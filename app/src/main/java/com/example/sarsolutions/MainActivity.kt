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
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.listener.PermissionRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

@Suppress("LABEL_NAME_CLASH")
class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val fireBaseService = RetrofitFactory.makeFirebaseRetrofitService()
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = FirebaseFirestore.getInstance()

       db.collection("Shift")
           .get()
           .addOnSuccessListener { result ->
               for (document in result) {
                   Toast.makeText(this, "${document.id} ==> ${document.data}", Toast.LENGTH_LONG).show()
               }
           }

        start_button.setOnClickListener {
            if(!isSearching) {
                isSearching = true
                start_button.text = "Stop"
            } else {
                isSearching = false
                start_button.text = "Start"
            }
            val geoLocations = listOf(GeoLocation("123", "321"), GeoLocation("12", "420"))
            val shift = Shift("YlNtlx3VTh6rAv6KC9dU", "oKrbMcbPVJ6yiErezkX3", Calendar.getInstance().time.toString(), geoLocations)
            db.collection("Shift")
                .add(shift)
                .addOnSuccessListener { docRef ->
                    Log.d("SAR", "Added document with id ${docRef.id}")
                }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                    locStuff();
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {/* ... */
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {/* ... */
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
                    // ...
                    location_id.text = location.longitude.toString()
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
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
