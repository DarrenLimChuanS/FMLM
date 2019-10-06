import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

// number of coordinates per vertex in this array
const val COORDS_PER_VERTEX = 3
var CubeCoords = floatArrayOf(
    -0.5f,  0.5f, -0.5f,      // top left  0
    -0.5f, -0.5f, -0.5f,      // bottom left  1
    0.5f, -0.5f, -0.5f,      // bottom right  2
    0.5f,  0.5f, -0.5f,       // top right  3
    -0.5f,  0.5f, 0.5f,      // top left  4
    -0.5f, -0.5f, 0.5f,      // bottom left  5
    0.5f, -0.5f, 0.5f,      // bottom right  6
    0.5f,  0.5f, 0.5f       // top right  7
)

var SquareCoords = floatArrayOf(     // in counterclockwise order:
    -0.5f,  0.5f, 0.0f,      // top left
    -0.5f, -0.5f, 0.0f,      // bottom left
    0.5f, -0.5f, 0.0f,      // bottom right
    0.5f,  0.5f, 0.0f       // top right
)

class Cube {
    private val drawOrder = shortArrayOf(
        0, 3, 2, 0, 2, 1,
        4, 5, 6, 4, 6, 7,
        4, 0, 1, 4, 1, 5,
        3, 7, 2, 7, 6, 2,
        5, 1, 2, 5, 2, 6,
        2, 0, 4, 2, 4, 7) // order to draw vertices
    private val drawOrder2 = shortArrayOf(
        0, 1, 2, 0,2,3)

    /*private val vertexShaderCode =
        "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}"*/

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    //shader program
    private var sProgram : Int = 0

    //for drawing
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexCount: Int = SquareCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private val color = floatArrayOf(1.0f,0.5f,0.0f,1.0f)

    private val MVPMatrixHandle = FloatArray(16)

    //view matrixes
    private val vertexShaderCode =
    // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0



    fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    init{
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        sProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }

    }

    // initialize vertex byte buffer for shape coordinates
    private val vertexBuffer: FloatBuffer =
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(CubeCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(CubeCoords)
                position(0)
            }
        }

    // initialize byte buffer for the draw list
    private val drawListBuffer: ShortBuffer =
        // (# of coordinate values * 2 bytes per short)
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }

    fun draw(mvpMatrix: FloatArray) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(sProgram);
        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(sProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandle);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            positionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, vertexBuffer);
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(sProgram, "vColor")
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0)
        // get handle to shape's transformation matrix
        val tMVPMatrixHandle  = GLES20.glGetUniformLocation(sProgram, "uMVPMatrix")
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(tMVPMatrixHandle , 1, false, mvpMatrix, 0)

        // Draw the square
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    fun draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(sProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(sProgram, "vPosition").also {

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(sProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }


}