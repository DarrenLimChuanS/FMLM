package com.example.fmlm.fragment.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.fmlm.R
import com.example.fmlm.common.helpers.CameraPermissionHelper
import com.example.fmlm.common.helpers.DisplayRotationHelper
import com.example.fmlm.common.helpers.SnackbarHelper
import com.example.fmlm.common.helpers.TapHelper
import com.example.fmlm.common.rendering.BackgroundRenderer
import com.example.fmlm.common.rendering.ObjectRenderer
import com.example.fmlm.common.rendering.PlaneRenderer
import com.example.fmlm.common.rendering.PointCloudRenderer
import com.example.fmlm.helloar.HelloArActivity
import com.example.fmlm.helloar.TrackingStateHelper
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import java.io.IOException
import java.util.ArrayList
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARComponentFragment : Fragment(), GLSurfaceView.Renderer {
    private lateinit var gLView: GLSurfaceView

    // Set to true ensures requestInstall() triggers installation if necessary.
    private var mUserRequestedInstall: Boolean = true

    private var arCoreSession: Session? = null
    private val messageSnackbarHelper = SnackbarHelper()
    private var displayRotationHelper: DisplayRotationHelper? = null
    private val TAG = HelloArActivity::class.java.simpleName
    private var tapHelper: TapHelper? = null

    private val backgroundRenderer = BackgroundRenderer()
    private val virtualObject = ObjectRenderer()
    private val virtualObjectShadow = ObjectRenderer()
    private val planeRenderer = PlaneRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    private val anchorMatrix = FloatArray(16)
    private val DEFAULT_COLOR = floatArrayOf(0f, 0f, 0f, 0f)

    private val SEARCHING_PLANE_MESSAGE = "Searching for surfaces..."

    // Anchors created from taps used for object placing with a given color.
    private class ColoredAnchor(val anchor: Anchor, val color: FloatArray)

    private val anchors = ArrayList<ColoredAnchor>()

    companion object {
        fun newInstance() = ARComponentFragment()
    }

    private lateinit var componentViewModel: ARComponentViewModel

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (arCoreSession == null) {
            return
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper?.updateSessionIfNeeded(arCoreSession)

        try {
            arCoreSession?.setCameraTextureName(backgroundRenderer.textureId)

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            val frame = arCoreSession?.update()
            val camera = frame?.camera

            // Handle one tap per frame.
            handleTap(frame!!, camera!!)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame)

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.trackingState == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(
                    activity, TrackingStateHelper.getTrackingFailureReasonString(camera)
                )
                return
            }

            // Get projection matrix.
            val projmtx = FloatArray(16)
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            val viewmtx = FloatArray(16)
            camera.getViewMatrix(viewmtx, 0)

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate?.getColorCorrection(colorCorrectionRgba, 0)

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            frame.acquirePointCloud().use { pointCloud ->
                pointCloudRenderer.update(pointCloud)
                pointCloudRenderer.draw(viewmtx, projmtx)
            }

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.
            if (hasTrackingPlane()) {
                messageSnackbarHelper.hide(activity)
            } else {
                messageSnackbarHelper.showMessage(activity, SEARCHING_PLANE_MESSAGE)
            }

            // Visualize planes.
            planeRenderer.drawPlanes(
                arCoreSession?.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projmtx
            )

            // Visualize anchors created by touch.
            val scaleFactor = 1.0f
            for (coloredAnchor in anchors) {
                if (coloredAnchor.anchor.trackingState != TrackingState.TRACKING) {
                    continue
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                coloredAnchor.anchor.pose.toMatrix(anchorMatrix, 0)

                // Update and draw the model and its shadow.
                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor)
                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor)
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color)
                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color)
            }

        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }

    }

    private fun hasTrackingPlane(): Boolean {
        for (plane in arCoreSession?.getAllTrackables(Plane::class.java)!!) {
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        val tap = tapHelper?.poll()
        if (tap != null && camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon
                val trackable = hit.trackable
                // Creates an anchor if a plane or an oriented point was hit.
                if ((trackable is Plane
                            && trackable.isPoseInPolygon(hit.hitPose)
                            && PlaneRenderer.calculateDistanceToPlane(
                        hit.hitPose,
                        camera.pose
                    ) > 0) || trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                ) {
                    // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size >= 20) {
                        anchors[0].anchor.detach()
                        anchors.removeAt(0)
                    }

                    // Assign a color to the object for rendering based on the trackable type
                    // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
                    // for AR_TRACKABLE_PLANE, it's green color.
                    var objColor: FloatArray = DEFAULT_COLOR
                    if (trackable is Point)
                        objColor = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)
                    else if (trackable is Plane)
                        objColor = floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)


                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(ColoredAnchor(hit.createAnchor(), objColor))
                    break
                }
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper?.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/activity)
            planeRenderer.createOnGlThread(/*context=*/activity, "models/trigrid.png")
            pointCloudRenderer.createOnGlThread(/*context=*/activity)

            virtualObject.createOnGlThread(/*context=*/activity, "models/SITFULL.obj", "models/SITFULL.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)

            virtualObjectShadow.createOnGlThread(
                /*context=*/ activity, "models/andy_shadow.obj", "models/andy_shadow.png"
            )
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow)
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ar_component_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        componentViewModel = ViewModelProviders.of(this).get(ARComponentViewModel::class.java)

        gLView = view!!.findViewById(R.id.surface_view)

        displayRotationHelper = DisplayRotationHelper(/*context=*/activity)

        // Set up tap listener.
        tapHelper = TapHelper(/*context=*/activity)
        gLView.setOnTouchListener(tapHelper)

        gLView.preserveEGLContextOnPause = true
        gLView.setEGLContextClientVersion(2)
        gLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
        gLView.setRenderer(this)
        gLView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        gLView.setWillNotDraw(false)

        mUserRequestedInstall = false
    }

    override fun onResume() {
        super.onResume()

        if (arCoreSession == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(activity, !mUserRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        mUserRequestedInstall = true
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {
                    }
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity)
                    return
                }

                // Create the session.
                arCoreSession = Session(/* context= */activity)

            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }

//            if (message != null) {
//                messageSnackbarHelper.showError(activity, message)
//                Log.e(TAG, "Exception creating session", exception)
//                return
//            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            arCoreSession?.resume()
        } catch (e: CameraNotAvailableException) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(activity, "Camera not available. Please restart the app.")
            arCoreSession = null
            return
        }

        gLView.onResume()
        displayRotationHelper?.onResume()
    }
}
