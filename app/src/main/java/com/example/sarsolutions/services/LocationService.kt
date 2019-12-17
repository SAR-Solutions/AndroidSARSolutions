package com.example.sarsolutions.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.example.sarsolutions.BuildConfig
import com.example.sarsolutions.MainActivity
import com.example.sarsolutions.MainFragment
import com.example.sarsolutions.MyApplication.Companion.CHANNEL_ID
import com.example.sarsolutions.R
import com.example.sarsolutions.models.Shift
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import timber.log.Timber
import java.util.*


class LocationService : Service() {

    companion object {
        val isTestMode = "TEST_MODE"
    }

    private var testMode: Boolean = false

    object Status {
        var isRunning: Boolean = false
    }

    var lastUpdated = MutableLiveData<String>()

    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val UPDATE_INTERVAL = 4000L
    private val FASTEST_INTERVAL = 2000L
    private val binder = LocalBinder()

    private val locationList: ArrayList<GeoPoint> = ArrayList()
    private var locationCallback: LocationCallback =  object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            super.onLocationResult(locationResult)
            locationResult ?: return
            for (location in locationResult.locations) {

                if(location.accuracy > 15) // Remove outliers for bad data points
                    continue
                // Don't record if already exists
                if (existsInList(GeoPoint(location.latitude, location.longitude)))
                    continue
                locationList.add(GeoPoint(location.latitude, location.longitude))
                lastUpdated.postValue( "Last updated at \n" + Calendar.getInstance().time)
                Timber.d(lastUpdated.value)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null)
            throw Exception("Service needs to be called with specified intent fields")
        testMode = intent.getBooleanExtra(isTestMode, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.loc_notification_title))
                .setContentText("CaseId YlNtlx3VTh6rAv6KC9dU")
                .setSmallIcon(R.drawable.ic_location)
                .build()

        // Id must not be 0
        // Ref: https://developer.android.com/guide/components/services.html#kotlin
        startForeground(1, notification)

        Timber.d("Location service started")
        getLocation()
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        syncUserShift()
        stopForeground(true)
        stopSelf()
    }

    // Add user shift to database
    private fun syncUserShift() {
        val shift = Shift(
                "YlNtlx3VTh6rAv6KC9dU",
                "oKrbMcbPVJ6yiErezkX3",
                Calendar.getInstance().time.toString(),
                BuildConfig.VERSION_NAME,
                locationList
        )

        db.collection(if (testMode) "TestShift" else "Shift")
                .add(shift)
                .addOnSuccessListener { docRef -> Timber.i("Added document with id ${docRef.id}") }
    }

    private fun getLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Stop if permissions aren't granted
        if(ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Location permissions not granted, stopping service.")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper())
    }

    // Checks for given point in locationList
    private fun existsInList(newPoint: GeoPoint) : Boolean {
        locationList.forEach { point ->
            if (point.compareTo(newPoint) == 0)
                return true
        }
        return false
    }


    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }
}