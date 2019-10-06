package com.example.fmlm.fragment.routing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import androidx.lifecycle.ViewModelProviders
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkCallingOrSelfPermission

import com.example.fmlm.R
import com.google.android.gms.location.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class RoutingComponentFragment : Fragment() {
    lateinit var nameTextBox: TextInputEditText
    // Map
    private lateinit var mapView: MapView
    lateinit var startMarker:Marker
    lateinit var endMarker:Marker
    private lateinit var geoCoder: GeocoderNominatim
    var roadOverlay :Polyline? = null

    // Text Input
    private lateinit var textInputDestination: TextInputLayout

    // GPS
    private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
    private val PERMISSION_REQUEST = 10
    lateinit var locationManager : LocationManager
    private var currentLocation: Location? = null
    private var currentLocationNetwork: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var hasGPS = false
    private var hasNetwork = false

    // Current Destination
    private var curDestination:GeoPoint = GeoPoint(0.0,0.0)
    lateinit var curDestName:String

    companion object {
        fun newInstance() = RoutingComponentFragment()
    }

    private lateinit var viewModel: RoutingComponentViewModel

    inner class MapPinOverlay: org.osmdroid.events.MapEventsReceiver {
        override fun longPressHelper(p: GeoPoint?): Boolean {
            return false
        }

        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {

            setPin(p!!)
            return false
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.routing_component_fragment, container, false)
        nameTextBox = v.findViewById(R.id.TextInputEditText)

//        //supressing the restrictions
//        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)

        // Map fragment
        mapView = v.findViewById(R.id.map)
        val mReceive = MapPinOverlay()
        startMarker = Marker(mapView)
        endMarker = Marker(mapView)
        mapView.overlays.add(startMarker)
        mapView.overlays.add(endMarker)
        mapView.overlays.add(MapEventsOverlay(mReceive))
        setUpMap()

        // Get Text Input
        textInputDestination = v.findViewById(R.id.text_input_destination)

        //listener
        val button: Button = v.findViewById(R.id.button_confirm)
        button.setOnClickListener{
            buttonPressSearch()
        }



//        if (ContextCompat.checkSelfPermission( getContext()!!,Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
//        {
//            requestPermissions(
//                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
//                1)//request code 1 for coarse location
//        }
//        if (ContextCompat.checkSelfPermission( getContext()!!,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
//        {
//            requestPermissions(
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                2)//request code 1 for coarse location
//        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkPermission(permissions)){

            }
            else{
                requestPermissions(permissions,PERMISSION_REQUEST)
            }
        }
        getLocation()

        return v
    }

    fun setPin(p: GeoPoint)
    {
        //Log.e("map pin", "pinned")
        Log.e("map pin", p.longitude.toString())
        Log.e("map pin", p.latitude.toString())
        curDestination = p
        val handler = Handler();
        val r = setRoutes()
        handler.postDelayed(r, 0)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){

        // Set up LocationManager and Listener
        locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.e("first update location", "first updating location (gps)")
        val listener = MyListener()
        if(hasGPS || hasNetwork)
        {
            //can use gps
            if(hasGPS){
                Log.e("WE HAVE GPS","GPS")
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5.0f, listener)
                val localGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(localGPS != null) {
                    currentLocation = localGPS
                    Log.e("gps loc lat", currentLocation!!.latitude.toString())
                    Log.e("gps loc long", currentLocation!!.longitude.toString())
                }
            }
            if(hasNetwork){
                Log.e("WE HAVE GPS","GPS")
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5.0f, object:
                    LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if(location != null)
                        currentLocationNetwork = location
                        val handler = Handler()
                        val r = updateRoutes()
                        handler.postDelayed(r, 0)
                        //updateRoute()
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    }

                    override fun onProviderEnabled(provider: String?) {
                    }

                    override fun onProviderDisabled(provider: String?) {
                    }

                })
                val localNetworl = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if(localNetworl != null) {
                    currentLocationNetwork = localNetworl
                    Log.e("gps loc lat", currentLocationNetwork!!.latitude.toString())
                    Log.e("gps loc long", currentLocationNetwork!!.longitude.toString())
                }
            }

            if(currentLocation != null && currentLocationNetwork != null){
                if(currentLocation!!.accuracy > currentLocationNetwork!!.accuracy){
                    //take network
                    currentLocation = currentLocationNetwork
                    Log.e("gps loc lat", currentLocation!!.latitude.toString())
                    Log.e("gps loc long", currentLocation!!.longitude.toString())
                }

            }
        }
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, listener)
        //currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    }

    private fun checkPermission(permissionArray:Array<String>):Boolean{
        for(i in permissionArray){
            if(checkCallingOrSelfPermission (activity!!.applicationContext,i) == PermissionChecker.PERMISSION_DENIED)
                return false
        }
        return true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RoutingComponentViewModel::class.java)

        // Create Geocoder
        geoCoder = GeocoderNominatim("FMLM/1.0")

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Check if permission already granted
        if (Build.VERSION.SDK_INT > 23 &&
            ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getLocationPermission()
        } else {

        }

        val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(activity!!, permissions, 0)

        // Initialize the osmdroid configuration
        Configuration.getInstance()
            .load(
                activity?.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(activity?.applicationContext)
            )

//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!.applicationContext)
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                locationResult ?: return
////                for (location in locationResult.locations){
////                }
//            }
//        }

    }

    inner class updateRoutes:Runnable{
        override fun run(){
            android.os.Process.setThreadPriority((android.os.Process.THREAD_PRIORITY_BACKGROUND))
            //cTask.setImageDecodeThread(Thread.currentThread())
            updateRoute()
        }
    }

    open inner class setRoutes:Runnable{
        override fun run(){
            Process.setThreadPriority((Process.THREAD_PRIORITY_BACKGROUND))
            //cTask.setImageDecodeThread(Thread.currentThread())
            navigateRoute()
        }
    }

    inner class MyListener : LocationListener {
        override fun onLocationChanged(location: Location?) {
            Log.e("update location (gps)", "updating location (gps)")

            currentLocation = location!!
            val waypoints = getWayPoints()
            if(waypoints.isNotEmpty())
                drawRoute(waypoints)
            val handler = Handler()
            val r = updateRoutes()
            handler.postDelayed(r, 0)
            //updateRoute()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
        override fun onProviderEnabled(provider: String?) {
        }
        override fun onProviderDisabled(provider: String?) {}
    }

//    fun forceUpdateLocation(){
//        val myLoc= MyListener()
//        val locationManager= activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0.1f,myLoc)
//    }

    fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        //startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission( getContext()!!,Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
        {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1)//request code 1 for coarse location
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            null /* Looper */)
    }


    override fun onPause() {
        super.onPause()
        mapView.onPause()
        //stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


//    class DemoTask : AsyncTask<Void, Void, Void>() {
//        override fun doInBackground(vararg params: Void?): Void? {
//            setUpMap()
//            return void?;
//        }
//    }

    private fun setUpMap() {
        Log.e("SetUpMap", "EnterMap")
        // Hardcoded Bedok MRT
        val start = GeoPoint(1.3240, 103.9302)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(start)
        mapView.setMultiTouchControls(true)
/*
        geoCoder = GeocoderNominatim("FMLM/1.0")

        // Place marker at start point
        val startMarker = Marker(mapView)
        startMarker.position = start
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(startMarker)
        startMarker.title = "Bedok MRT"

        // Place marker at end point
        //val endPoint = GeoPoint(1.3467526, 103.93257659999995)
        val endPoint = getCoord("Temasek Polytechnic")
        val endMarker = Marker(mapView)
        endMarker.position = endPoint
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(endMarker)

        // Routing
        val waypoints = java.util.ArrayList<GeoPoint>()
        waypoints.add(start)
        waypoints.add(endPoint)

        // Get road from osrm
        var roadManager : OSRMRoadManager = OSRMRoadManager(activity)
        roadManager.setService("http://47.74.218.117:8000/route/v1/walking/")
        //roadManager.setService("http://47.74.218.117:8000/route/v1/driving/")
        val road = roadManager.getRoad(waypoints)

        // Draw Polylines along the routes
        val roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay.width = 15.0f
        mapView.overlays.add(roadOverlay)
*/
        Log.e("SetUpMap", "ExitMap")
    }

    fun getCoord(name:String):GeoPoint{
        val addresses = getAddressList(name)
        if(addresses.size < 1)
            return GeoPoint(0.0,0.0)
        return GeoPoint(addresses.first().latitude, addresses.first().longitude)
    }

    fun buttonPressSearch(){
        val destName = nameTextBox.getText()
        val destPoint= getCoord(destName.toString())
        curDestination = destPoint
        curDestName = destName.toString()
        Log.e("buttonSearch", nameTextBox.getText().toString())
        if(destPoint == GeoPoint(0.0,0.0))
        {
            Log.e("buttonSearch", "Location Not Found")
            Toast.makeText(activity, "Address not found!", Toast.LENGTH_SHORT).show()
            return
        }
        Log.e("buttonSearch",destPoint.toString())
        val handler = Handler();
        val r = setRoutes()
        handler.postDelayed(r, 0);
        //navigateRoute()
    }

    fun updateRoute(){

        Log.e("update route (gps)", "updating route (gps)")
        val start = GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude)
        //mapView.overlays.clear()

        geoCoder = GeocoderNominatim("FMLM/1.0")

        // Place marker at start point
        startMarker.position = start
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Current Location"

        if(curDestination == GeoPoint(0.0,0.0))
            return
        // Place marker at end point
        endMarker.position = curDestination
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Routing
        val waypoints = java.util.ArrayList<GeoPoint>()
        waypoints.clear()
        waypoints.add(start)
        waypoints.add(curDestination)

        // Get road from osrm
        var roadManager : OSRMRoadManager = OSRMRoadManager(activity)
        roadManager.setService("http://47.74.218.117:8000/route/v1/walking/")
        //roadManager.setService("http://47.74.218.117:8000/route/v1/driving/")
        val road = roadManager.getRoad(waypoints)

        // Draw Polylines along the routes
        addRoad(road)
    }

    fun addRoad(r: Road){

        if(roadOverlay != null) {
            if (mapView.overlays.contains(roadOverlay))
                mapView.overlays.remove(roadOverlay)
        }
        roadOverlay = RoadManager.buildRoadOverlay(r)
        mapView.overlays.add(roadOverlay)
    }

    fun navigateRoute(){

        val start = GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude)
        Log.e("gps loc", currentLocation!!.latitude.toString())
        Log.e("gps loc", currentLocation!!.longitude.toString())
//        mapView.setTileSource(TileSourceFactory.MAPNIK)
//        mapView.controller.setZoom(15.0)
//        mapView.controller.setCenter(start)
//        mapView.setMultiTouchControls(true)
        //mapView.overlays.clear()

        geoCoder = GeocoderNominatim("FMLM/1.0")

        // Place marker at start point
        //val startMarker = Marker(mapView)
        startMarker.position = start
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        //mapView.overlays.add(startMarker)
        //startMarker.title = "Bedok MRT"

        // Place marker at end point
        //val endMarker = Marker(mapView)
        endMarker.position = curDestination
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
       // mapView.overlays.add(endMarker)

        // Routing
        val waypoints = java.util.ArrayList<GeoPoint>()
        waypoints.clear()
        waypoints.add(start)
        waypoints.add(curDestination)

        // Get road from osrm
        var roadManager : OSRMRoadManager = OSRMRoadManager(activity)
        roadManager.setService("http://47.74.218.117:8000/route/v1/walking/")
        //roadManager.setService("http://47.74.218.117:8000/route/v1/driving/")
        val road = roadManager.getRoad(waypoints)

        val locationBundle =  geoCoder.getFromLocation(curDestination.latitude,curDestination.longitude,1).get(0)
        //var roadDestName = locationBundle.getAddressLine(0)
        var roadDestName = locationBundle.extras.get("display_name").toString()
        val separated = roadDestName.split(",")
//        val max = locationBundle.maxAddressLineIndex
//        var i = 1
//        while(i != max)
//        {
//            Log.e("adding", locationBundle.getAddressLine(i))
//            roadDestName += locationBundle.getAddressLine(i)
//            ++i
//        }
        Log.e("gps loc name", ""+locationBundle.extras.get("display_name").toString())
        textInputDestination.editText!!.setText(separated[0], TextView.BufferType.EDITABLE)

        // Draw Polylines along the routes
        addRoad(road)
    }

    private fun drawRoute(waypoints: ArrayList<GeoPoint>) {
        // Get road from osrm
        val roadManager: RoadManager = OSRMRoadManager(activity)
        val road = roadManager.getRoad(waypoints)

        // Draw Polylines along the routes
        addRoad(road)
    }

    private fun placeMarker(position: GeoPoint, title: String) {
        val marker = Marker(mapView)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        mapView.overlays.add(marker)
    }

    fun getLocationPermission() {
        var permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(activity!!, permissions, 0)
        permissions = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        ActivityCompat.requestPermissions(activity!!, permissions, 0)

        Toast.makeText(activity, "Required for GPS", Toast.LENGTH_LONG).show()
    }

    private fun getWayPoints(): ArrayList<GeoPoint> {
        val waypoints = ArrayList<GeoPoint>()

        // Get input strings
        val destinationInputText = textInputDestination.editText?.text?.trim().toString()

        // Input Checks
        if(destinationInputText.isEmpty()) {
            textInputDestination.error = "Please enter destination!"
        }
        else {
            val locationGeoResults: List<Address> = geoCoder.getFromLocation(currentLocation!!.latitude, currentLocation!!.longitude, 1)
            val destinationGeoResults: List<Address> = geoCoder.getFromLocationName(destinationInputText, 1)

            // Check validity of inputs
            var destinationIsValid = true
            var locationIsValid = true
            if(locationGeoResults.isEmpty()) {
                locationIsValid = false
            }
            if (destinationGeoResults.isEmpty()) {
                textInputDestination.error = "Invalid address!"
                destinationIsValid = false
            }
            if(!locationIsValid || !destinationIsValid)
                return waypoints

            // Both inputs are valid
            // Clear error message
            textInputDestination.error = null

            val startPoint = GeoPoint(locationGeoResults.first().latitude, locationGeoResults.first().longitude)
            val endPoint = GeoPoint(destinationGeoResults.first().latitude, destinationGeoResults.first().longitude)

            // Place marker
            //mapView.overlays.clear()
            //placeMarker(startPoint, "Start")
            //placeMarker(endPoint, "End")

            waypoints.add(startPoint)
            waypoints.add(endPoint)
        }

        return waypoints
    }

    fun getAddressList(locationName: String): List<Address> {
        var geoResults: List<Address> = emptyList()
        try {
            Log.e("geoResult", "try get geocoder")
            geoResults = geoCoder.getFromLocationName(locationName, 1)
            Log.e("geoResult", (geoResults.size).toString() )
            if (geoResults.isEmpty())
                Toast.makeText(activity, "Address not found!", Toast.LENGTH_SHORT).show()
            else {
                val address: Address = geoResults.first()
                val boundingbox: BoundingBox? = address.extras.getParcelable("BoundingBox");
                mapView.zoomToBoundingBox(boundingbox, true)
            }
        } catch (e: Exception) {
            Log.e("geo exceptions", e.toString() )
            Toast.makeText(activity, "Geocoding error !", Toast.LENGTH_LONG).show()
        }
        return geoResults
    }

    @Suppress("UNUSED_PARAMETER")
    fun confirmInput(v: View) {
        // Clear previous overlay
        mapView.overlays.clear()
        val waypoints = getWayPoints()

        if (waypoints.isNotEmpty())
            drawRoute(waypoints)
    }
}


