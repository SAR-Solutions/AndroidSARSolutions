package com.sarcoordinator.sarsolutions.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.sarcoordinator.sarsolutions.BuildConfig
import com.sarcoordinator.sarsolutions.MainActivity
import com.sarcoordinator.sarsolutions.MyApplication.Companion.CHANNEL_ID
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.models.Shift
import timber.log.Timber
import java.util.*


class LocationService : Service() {

    companion object {
        const val isTestMode = "TEST_MODE"
    }

    private var testMode: Boolean = false
    private lateinit var startTime: String

    private val lastUpdated = MutableLiveData<String>()
    fun getLastUpdated(): LiveData<String> = lastUpdated

    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val UPDATE_INTERVAL = 4000L
    private val FASTEST_INTERVAL = 2000L

    private val locationList: ArrayList<GeoPoint> = ArrayList()
    private var locationCallback: LocationCallback =  object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            super.onLocationResult(locationResult)
            locationResult ?: return
            for (location in locationResult.locations) {

                if (location.accuracy > 20) // Remove outliers for bad data points
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

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null)
            throw Exception("Service needs to be called with specified intent fields")
        testMode = intent.getBooleanExtra(isTestMode, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val resultIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("test", 1)
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val resultPendingIntent = PendingIntent.getActivity(
            this, 0, resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notification is needed for >= API 26 for foreground services
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle(getString(R.string.loc_notification_title))
            setContentText("CaseId YlNtlx3VTh6rAv6KC9dU")
            setSmallIcon(R.drawable.ic_location)
            setContentIntent(resultPendingIntent)
        }.build()

        // Id must NOT be 0
        // Ref: https://developer.android.com/guide/components/services.html#kotlin
        startForeground(1, notification)
        startTime = Calendar.getInstance().time.toString()

        Timber.d("Location service started")
        getLocation()
        return START_NOT_STICKY
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
            startTime,
                Calendar.getInstance().time.toString(),
                BuildConfig.VERSION_NAME,
                locationList
        )

        db.collection(if (testMode) "TestShift" else "Shift")
                .add(shift)
            .addOnSuccessListener { docRef ->
                Timber.i(
                    "Added to ${if (testMode) "TestShift" else "Shift"}" +
                            " with id ${docRef.id}"
                )
            }
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

    private fun activityRecognition() {
        val transitions = mutableListOf<ActivityTransition>()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_FOOT)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        val request = ActivityTransitionRequest(transitions)
//        val pendingIntent = PendingIntent.
//        val task = ActivityRecognition.getClient(baseContext)
//            .requestActivityTransitionUpdates(request, PendingIntent())
    }
}