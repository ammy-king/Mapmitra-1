package com.mapmitra.mapmitra

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationSettingsRequest.Builder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.mapmitra.mapmitra.directionhelpers.FetchURL
import com.mapmitra.mapmitra.directionhelpers.TaskLoadedCallback
import com.mapmitra.mapmitra.models.Obstacles
import kotlinx.android.synthetic.main.content_map.*
import java.io.IOException
import kotlin.math.abs

private const val TAG = "MapActivity"

class MapsActivity : FragmentActivity() , OnMapReadyCallback , TaskLoadedCallback {
    private var mMap: GoogleMap? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var placesClient: PlacesClient? = null
    var mLastKnownLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var mapView: View? = null
    val firestoreDB = FirebaseFirestore.getInstance()
    private var currentPolyline: Polyline? = null
    private var searchView: SearchView? = null
    var mapFragment: SupportMapFragment? = null
    var requestPH = 0
    var requestSP = 0
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    lateinit var description: String
    private val channelId = "com.mapmitra.mapmitra"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        title = "MapMitra"
        btnDirection.isEnabled = false

        val bottomNavigation: BottomNavigationView = findViewById(R.id.btm_nav)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            onBottomNavigationClicked(item)

        }


        searchView = findViewById(R.id.sv_location)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        mapView = mapFragment!!.view
        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@MapsActivity)
        Places.initialize(this@MapsActivity , getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
        AutocompleteSessionToken.newInstance()
        if (ActivityCompat.checkSelfPermission(
                this@MapsActivity ,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(this , "Request Not Found" , Toast.LENGTH_LONG).show()
        }


    }

    override fun onActivityResult(requestCode: Int , resultCode: Int , data: Intent?) {
        super.onActivityResult(requestCode , resultCode , data)
        if (requestCode == 51 && resultCode == Activity.RESULT_OK) {
            getCurrentLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.isMyLocationEnabled = true
        mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap!!.isTrafficEnabled = true
        mMap!!.uiSettings.isMyLocationButtonEnabled = false

        title = "MapMitra"

        // btnDirection.isEnabled = false

        if (mapView != null && mapView!!.findViewById<View?>("1".toInt()) != null) {
            val locationButton =
                (mapView!!.findViewById<View>("1".toInt()).parent as View).findViewById<View>("2".toInt())
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP , 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM , RelativeLayout.TRUE)
            layoutParams.setMargins(0 , 0 , 40 , 40)


        }

        // check if GPS is enabled or not & then request user to enable it
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder: Builder = Builder().addLocationRequest(locationRequest)
        val settingsClient: SettingsClient? = LocationServices.getSettingsClient(this@MapsActivity)
        val task: Task<LocationSettingsResponse> =
            settingsClient!!.checkLocationSettings(builder.build())
        task.addOnSuccessListener(this@MapsActivity) { getCurrentLocation() }
        task.addOnFailureListener(this@MapsActivity) { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapsActivity , 51)
                } catch (e1: SendIntentException) {
                    e1.printStackTrace()
                }
            }
        }




        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val location = searchView!!.query.toString()
                var addressList: List<Address>? = null
                if (true) {
                    val geocoder = Geocoder(this@MapsActivity)
                    try {
                        addressList = geocoder.getFromLocationName(location , 1)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    val address = addressList!![0]
                    val latLng =
                        LatLng(
                            address.latitude ,
                            address.longitude
                        )
                    mMap!!.addMarker(MarkerOptions().position(latLng).title(location))
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng , 14f))

                    btnDirection.isEnabled = true
                    btnDirection.setOnClickListener {
                        mFusedLocationProviderClient!!.lastLocation
                            .addOnSuccessListener { location ->
                                if (location != null) {
                                    var dest = LatLng(location.latitude , location.longitude)
                                    FetchURL(this@MapsActivity).execute(
                                        getUrl(
                                            dest ,
                                            latLng ,
                                            "driving"
                                        ) , "driving"
                                    )
                                    Toast.makeText(
                                        this@MapsActivity ,
                                        "Currently Not Available" ,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }


                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        btnDirection.isEnabled = false
        getCurrentLocation()
        callObstacles()
        callChecker()

    }

    private fun onBottomNavigationClicked(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.addMarker -> {
                val builder = AlertDialog.Builder(this@MapsActivity)
                builder.setTitle("Choose Type")
                    .setMessage("Which type of Obstacle do you want to add. ")
                    .setNegativeButton("SpeedBreaker") { _ , _ ->
                        addObstacle("SPeedBreaker")
                    }
                    .setPositiveButton("Pothole") { _ , _ ->
                        addObstacle("Pothole")
                    }
                    .show()
            }

            R.id.myLocation -> {
                getCurrentLocation()
            }

            R.id.profile -> {
                startActivity(Intent(this@MapsActivity , ProfileActivity::class.java))
            }


        }
        return true

    }

    private fun callObstacles() {
        showObstacles("Pothole")
        showObstacles("SpeedBreaker")

    }

    private fun showObstacles(type: String) {

        var collection = if (type == "Pothole") "Potholes" else "SpeedBreakers"

        firestoreDB.collection(collection)
            .addSnapshotListener { snapshot , exception ->
                if (exception != null || snapshot == null) {
                    Log.e(TAG , "Exception while fetching $collection" , exception)
                    return@addSnapshotListener
                }

                val obstaclesList = snapshot.toObjects(Obstacles::class.java)
                for (obstacle in obstaclesList) {
                    var position: LatLng = LatLng(obstacle.lattitude , obstacle.longitude)
                    if (type == "Pothole") {
                        mMap!!.addMarker(
                            MarkerOptions()
                                .position(position)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                        )
                    } else {
                        mMap!!.addMarker(
                            MarkerOptions()
                                .position(position)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }
            }

    }

    private fun callChecker() {
        checkObstacle("Pothole")
        checkObstacle("SpeedBreaker")
    }

    private fun checkObstacle(type: String) {
        mFusedLocationProviderClient!!.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    var lat = location.latitude
                    var lon = location.longitude

                    var collection = if (type == "Pothole") "Potholes" else "SpeedBreakers"

                    firestoreDB.collection(collection)
                        .addSnapshotListener { snapshot , exception ->
                            if (exception != null || snapshot == null) {
                                Log.e(TAG , "Exception while fetching $collection" , exception)
                                return@addSnapshotListener
                            }

                            val obstaclesList = snapshot.toObjects(Obstacles::class.java)

                            for (obstacle in obstaclesList) {
                                if ((abs(obstacle.lattitude - lat) < 0.00050) && abs(obstacle.longitude - lon) < 0.00050) {
                                    Toast.makeText(
                                        this ,
                                        "Obstacle Ahead\n Be Alert." ,
                                        Toast.LENGTH_LONG
                                    ).show()
                                    addNotification(type)

                                    val builder = AlertDialog.Builder(this@MapsActivity)
                                    builder.setTitle("Obstacle Ahead!!")
                                        .setMessage("Be Alert!! $type is Ahead!! ")
                                        .setPositiveButton("OK") { _ , _ -> }
                                        .show()
                                }
                            }

                        }

                }
            }
    }

    private fun addNotification(type: String) {
        val intent = Intent(this , MapsActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this , 0 , intent , PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId , "$type Ahead" , NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ,
                null
            )
            notificationChannel.enableVibration(true)
            notificationChannel.lockscreenVisibility
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this , channelId)
                .setContentTitle("$type Ahead")
                .setContentText("Be alert! $type is Ahead.")
                .setSmallIcon(R.drawable.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources , R.drawable.ic_launcher))
                .setContentIntent(pendingIntent)


        } else {
            builder = Notification.Builder(this)
                .setContentTitle("$type Ahead")
                .setContentText("Be alert! $type is Ahead.")
                .setSmallIcon(R.drawable.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources , R.drawable.ic_launcher))
                .setContentIntent(pendingIntent)
        }

        notificationManager.notify(1234 , builder.build())
    }

    private fun addObstacle(type: String) {

        var request = if (type == "SpeedBreaker") requestSP else requestPH
        var collection = if (type == "SpeedBreaker") "SpeedBreakers" else "Potholes"

        getCurrentLocation()
        var lat = mLastKnownLocation!!.latitude
        var lon = mLastKnownLocation!!.longitude
        var obstacle = Obstacles(lat , lon)

        if (request < 10) {
            request += 1
            Toast.makeText(this , "Request sent Successfully" , Toast.LENGTH_SHORT).show()
        } else if (request == 10) {
            firestoreDB.collection(collection).add(obstacle)
                .addOnSuccessListener {
                    Toast.makeText(this , "Marker Added Successfully. " , Toast.LENGTH_SHORT).show()
                    request = 0
                }.addOnFailureListener {
                    Toast.makeText(
                        this ,
                        "Problem Adding Marker, Try Again Later" ,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this , "Marker Already Added. " , Toast.LENGTH_SHORT).show()
        }

        if (type == "SpeedBreaker") {
            requestSP = request
        } else {
            requestPH = request
        }
    }

    private fun getCurrentLocation() {
        mFusedLocationProviderClient!!.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    mapFragment!!.getMapAsync {
                        mLastKnownLocation = location
                        mMap!!.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    mLastKnownLocation!!.latitude ,
                                    mLastKnownLocation!!.longitude
                                ) , 20f
                            )
                        )
                    }
                }
            }
    }

    private fun getUrl(origin: LatLng , dest: LatLng , directionMode: String): String? {
        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        // Mode
        val mode = "mode=$directionMode"
        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$mode"
        // Output format
        val output = "json"
        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=" + getString(
            R.string.google_maps_key
        )
    }

    override fun onTaskDone(vararg values: Any?) {
        if (currentPolyline != null) currentPolyline?.remove()
        currentPolyline = mMap!!.addPolyline(values[0] as PolylineOptions?)
    }

}



