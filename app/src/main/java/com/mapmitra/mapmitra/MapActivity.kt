package com.mapmitra.mapmitra

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibrationEffect.createOneShot
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken.newInstance
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapmitra.mapmitra.directionhelpers.FetchURL
import com.mapmitra.mapmitra.directionhelpers.TaskLoadedCallback
import java.io.IOException


class MapActivity : AppCompatActivity(), OnMapReadyCallback,TaskLoadedCallback{
    private var requestNum = 0
    private var mMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var placesClient: PlacesClient? = null
    private var vibrate: Vibrator? = null
    private var mLastKnownLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var mapView: View? = null
    private var searchView: SearchView? = null
    private val defaultZoom = 25f
    private var currentPolyline: Polyline? = null
    private var latlngdest:LatLng = LatLng(0.0, 0.0)

    private lateinit var firestoreDB : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        searchView = findViewById(R.id.sv_location)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?


        val addMarker = findViewById<FloatingActionButton>(R.id.markerAdd)


        firestoreDB = FirebaseFirestore.getInstance()
        vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator



        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        mapView = mapFragment.view
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MapActivity)
        Places.initialize(this@MapActivity, getString(R.string.google_map_api_key))
        placesClient = Places.createClient(this)
        newInstance()
        addMarker.setOnClickListener { addMarker() }

    }

    private fun showDirection() {


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_Logout) {
            Log.i("MapActivity", "User Wants to Logout")
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this@MapActivity, LoginActivity::class.java))
            finish()
        }
        if (item.itemId == R.id.menu_about_us) {
            startActivity(Intent(this@MapActivity, AboutUs::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.isMyLocationEnabled = true
        mMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
        mMap!!.isTrafficEnabled = true
        mMap!!.uiSettings.isZoomControlsEnabled = true
        mMap!!.uiSettings.isMyLocationButtonEnabled = true
        mMap!!.uiSettings.isMapToolbarEnabled = true
        mMap!!.uiSettings.isCompassEnabled = true
        mMap!!.uiSettings.setAllGesturesEnabled(true)

        val btnDirection = findViewById<FloatingActionButton>(R.id.btnDirection)
        btnDirection.isEnabled = false

        getdeviceLocation()
        showMarkers()
        if (mapView != null && mapView!!.findViewById<View?>("1".toInt()) != null) {
            val locationButton = (mapView!!.findViewById<View>("1".toInt()).parent as View).findViewById<View>("2".toInt())
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 40, 160)
        }

        // check if GPS is enabled or not & then request user to enable it
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 500
        locationRequest.fastestInterval = 100
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this@MapActivity)
        val task = settingsClient.checkLocationSettings(builder.build())
        task.addOnSuccessListener(this@MapActivity) {
            getdeviceLocation()
            if (mLastKnownLocation != null) {
                checkObstacles()
            }
        }
        task.addOnFailureListener(this@MapActivity) { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapActivity, 51)
                } catch (e1: SendIntentException) {
                    e1.printStackTrace()
                }
            }
        }
        if (mLastKnownLocation != null) {
            checkObstacles()
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val location = query
                var addressList: List<Address>? = null
                if ( location != "") {
                    val geocoder = Geocoder(this@MapActivity)
                    try {
                        addressList = geocoder.getFromLocationName(location, 1)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    val address = addressList!![0]
                    val latlngdest = LatLng(address.latitude, address.longitude)
                    btnDirection?.isEnabled = true
                    btnDirection?.setOnClickListener{
                        var ltLng:LatLng
                        ltLng = LatLng(mLastKnownLocation!!.latitude,mLastKnownLocation!!.longitude)
                        if (latlngdest.latitude!=0.0 && latlngdest.longitude!=0.0){
                            Toast.makeText(this@MapActivity,"Destination Not Empty",Toast.LENGTH_SHORT).show()
                            Log.e("Direction","Destination Not Empty @ ${latlngdest.latitude},${latlngdest.longitude}")
                            Log.e("Direction","Destination Not Empty @ ${ltLng.latitude},${ltLng.longitude}")
                            FetchURL(this@MapActivity).execute(getUrl(ltLng, latlngdest, "driving"), "driving")
                        }
                    }
                    mMap!!.addMarker(MarkerOptions().position(latlngdest).title(location))
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latlngdest, 15f))
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
    }

    private fun checkObstacles() {
        getdeviceLocation()
        val lat = mLastKnownLocation!!.latitude
        val lon = mLastKnownLocation!!.longitude
        val obsList = firestoreDB.collection("Obstacles")
        obsList.addSnapshotListener { snapshot, exception ->
            if(exception != null || snapshot == null) {
                Log.e("MapActivity","Exception while checking obstacles",exception)
                return@addSnapshotListener
            }

            val obstaclesList = snapshot.toObjects(Obstacles::class.java)

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setMessage("Obstacle at 50 mtrs ahead....")
            alertDialogBuilder.setPositiveButton("Ok") { _, _ -> Toast.makeText(this@MapActivity, "Thank You!!", Toast.LENGTH_LONG).show() }
                    .setNegativeButton("Cancel") { _, _ -> Toast.makeText(this@MapActivity, "Thank You!!", Toast.LENGTH_LONG).show() }
            val alertDialog = alertDialogBuilder.create()
            for (obstacle in obstaclesList) {
                if (((obstacle.lattitude - lat) <= 0.00050) || ((obstacle.longitude - lon )<= 0.00050)) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrate!!.vibrate(createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE))
                    }

                    alertDialog.show()
                    Toast.makeText(this@MapActivity, "\n Be Alert \n Obstacle Ahead\n", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    private fun showMarkers() {

        val obsList = firestoreDB.collection("Obstacles")
        obsList.addSnapshotListener { snapshot, exception ->
            if(exception != null || snapshot == null) {
                Log.e("MapActivity","Exception while quering Posts",exception)
                return@addSnapshotListener
            }
            val obstaclesList = snapshot.toObjects(Obstacles::class.java)

           var latLng:LatLng
            for (obstacle in obstaclesList) {

                latLng = LatLng(obstacle.lattitude,obstacle.longitude)
                mMap!!.addMarker(MarkerOptions()
                        .position(latLng)
                        .alpha(0.5.toFloat())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
            }
        }
    }

    private fun addMarker() {

        getdeviceLocation()
        val lat = mLastKnownLocation!!.latitude
        val lon = mLastKnownLocation!!.longitude
        val obstacles = Obstacles(lat,lon)
        requestNum += 1
        when {
            requestNum == 10 -> {

                firestoreDB.collection("Obstacles").add(obstacles)
                        .addOnSuccessListener {
                            Toast.makeText(this@MapActivity, "Location Added in Firebase Successfully..", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this@MapActivity, "Failure Adding to Database ", Toast.LENGTH_SHORT).show()
                            Log.e("Firebase","Failure Adding to Database",it)
                        }
                Toast.makeText(this@MapActivity, "Location Added Successfully..", Toast.LENGTH_SHORT).show()
                showMarkers()
                Log.i("Markers", "New Marker Added.@ $lat,$lon")
            }
            requestNum >= 10 -> {
                Toast.makeText(this@MapActivity, "Marker Already Added..", Toast.LENGTH_SHORT).show()
                showMarkers()
            }
            else -> {
                Toast.makeText(this@MapActivity, "Request Sent Successfully..", Toast.LENGTH_SHORT).show()
                showMarkers()
                Log.i("Markers", "New Marker Request number $requestNum.@ $lat,$lon")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getdeviceLocation(){
            mFusedLocationProviderClient!!.lastLocation
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            mLastKnownLocation = task.result
                            if (mLastKnownLocation != null) {
                                val latlng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
                                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,defaultZoom))
                            } else {
                                val locationRequest = LocationRequest.create()
                                locationRequest.interval = 1000
                                locationRequest.fastestInterval = 500
                                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                                locationCallback = object : LocationCallback() {
                                    override fun onLocationResult(locationResult: LocationResult) {
                                        super.onLocationResult(locationResult)

                                        val latlng = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)

                                        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,defaultZoom))
                                        mFusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
                                    }
                                }
                                mFusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback, null)
                            }
                        } else {
                            Toast.makeText(this@MapActivity, "unable to get Last Location", Toast.LENGTH_SHORT).show()
                        }
                    }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 51) {
            if (resultCode == Activity.RESULT_OK) {
                getdeviceLocation()
                checkObstacles()
            }
        }
    }

    private fun getUrl(origin: LatLng, dest: LatLng, directionMode: String): String? {
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
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_map_api_key)
    }

    override fun onTaskDone(vararg values: Any?) {
        if (currentPolyline != null) currentPolyline?.remove()
        currentPolyline = mMap!!.addPolyline(values[0] as PolylineOptions?)
    }


}