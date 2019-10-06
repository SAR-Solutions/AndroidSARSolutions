package com.example.sarsolutions

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.location.LocationSettingsRequest
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.location.LocationManager
import android.content.Context.LOCATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import android.Manifest.permission
import com.karumi.dexter.Dexter
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.listener.PermissionRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

@Suppress("LABEL_NAME_CLASH")
class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val locationList : ArrayList<GeoPoint> = ArrayList<GeoPoint>()
    private var isSearching = false
    private var currentShiftId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = FirebaseFirestore.getInstance()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {

                    start_button.setOnClickListener {
                        // Change button state/functionality
                        if(!isSearching) { // Start searching
                            isSearching = true
                            start_button.text = "Stop"
                            locStuff()
                        } else { // Stop searching and update Loc
                            fusedLocationClient.removeLocationUpdates(locationCallback)
                            // TODO: Stop handler from updating
                            val shift = Shift("YlNtlx3VTh6rAv6KC9dU", "oKrbMcbPVJ6yiErezkX3", Calendar.getInstance().time.toString(), locationList)

                            /** NOTE: currentShiftId in this context is always going to be null for the demo. This code is here for the future **/

                            if(currentShiftId == null) { // Add if no current shift id exists
                                db.collection("Shift")
                                    .add(shift)
                                    .addOnSuccessListener { docRef ->
                                        Log.d("SAR", "Added document with id ${docRef.id}")
                                    }
                            }
                            else { // Update and merge if current shift id exists
                                db.collection("Shift")
                                    .document(currentShiftId!!)
                                    .set(shift, SetOptions.merge())
                            }
                            isSearching = false
                            start_button.text = "Start"
                            currentShiftId = null
                        }
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    // TODO: Add permissions check
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                }
            }).check()
    }

    private fun locStuff() {
        locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(p0: LocationAvailability?) {
                super.onLocationAvailability(p0)
            }

            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult ?: return
                for (location in locationResult.locations){

                    // Update UI with location data
                    locationList.add(GeoPoint(location.latitude, location.longitude))
                    location_id.text = "Last updated at \n" + Calendar.getInstance().time.toString()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 5000
        } // update every 5 seconds
    }

    override fun onStop() {
        super.onStop()
        if(isSearching) {
            // TODO: Update fields
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // Runs every 10 minutes to sync locations with the database
    private fun databaseSyncer() {
        val handler = Handler()
        handler.postDelayed(Runnable {
            TODO("Sync locations with database")
        }, 100000)
    }
}
