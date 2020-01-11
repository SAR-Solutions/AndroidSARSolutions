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
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.BuildConfig
import com.sarcoordinator.sarsolutions.MainActivity
import com.sarcoordinator.sarsolutions.MyApplication.Companion.CHANNEL_ID
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.api.Repository
import com.sarcoordinator.sarsolutions.models.LocationPoint
import com.sarcoordinator.sarsolutions.models.Shift
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class LocationService : Service() {

    companion object {
        const val isTestMode = "TEST_MODE"
    }

    private var mTestMode: Boolean = false
    private lateinit var mShiftId: String
    private lateinit var mIdToken: String

    private val lastUpdated = MutableLiveData<String>()
    fun getLastUpdated(): LiveData<String> = lastUpdated

    private val user = FirebaseAuth.getInstance().currentUser
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val UPDATE_INTERVAL = 4000L
    private val FASTEST_INTERVAL = 2000L

    private lateinit var addPathsJob: CompletableJob
    private var addPathsJobIsSyncing = false

    private val locationList: ArrayList<LocationPoint> = ArrayList()
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            super.onLocationResult(locationResult)

            locationResult ?: return
            if (!::mIdToken.isInitialized || !::mShiftId.isInitialized)
                return

            for (location in locationResult.locations) {

                if (location.accuracy > 20) // Remove outliers for bad data points
                    continue
                // Don't record if already exists
                if (locationList.contains(LocationPoint(location.latitude, location.longitude)))
                    continue
                locationList.add(LocationPoint(location.latitude, location.longitude))
                lastUpdated.postValue("Last updated at \n" + Calendar.getInstance().time)
                Timber.d(lastUpdated.value)

                // Start addPathsjob if 10 or more locations exist and job isn't running
                if (locationList.size >= 10 && !addPathsJobIsSyncing) {
                    addPathsJobIsSyncing = true
                    val tempList = ArrayList<LocationPoint>()
                    tempList.addAll(locationList)
                    locationList.clear()
                    CoroutineScope(IO + addPathsJob).launch {
                        Repository.putLocations(mIdToken, mShiftId, mTestMode, tempList)
                    }.invokeOnCompletion {
                        addPathsJobIsSyncing = false
                    }
                }
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
        if (intent == null)
            throw Exception("Service needs to be called with specified intent fields")
        mTestMode = intent.extras!!.getBoolean(isTestMode, false)
        intent.getBooleanExtra(isTestMode, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create shift; get and set mShiftId, mIdToken
        user?.getIdToken(true)?.addOnSuccessListener {
            mIdToken = it.token!!
            val shift = Shift(
                Calendar.getInstance().time.toString(),
                BuildConfig.VERSION_NAME
            )
            CoroutineScope(IO).launch {
                mShiftId = Repository
                    .postStartShift(mIdToken, shift, "YlNtlx3VTh6rAv6KC9dU", mTestMode).shiftId
                lastUpdated.postValue(getString(R.string.started_shift))
            }
        }

        if (!::addPathsJob.isInitialized) {
            addPathsJob = Job()
        }

        // Start service notification intent

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

        Timber.d("Location service started")
        getLocation()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        completeShift()
        stopForeground(true)
        stopSelf()
    }

    // End shift; set endTime and sync remaining points
    private fun completeShift() {
        if (locationList.isNotEmpty()) {
            // Sync remaining points
            CoroutineScope(IO).launch {
                Repository.putLocations(mIdToken, mShiftId, mTestMode, locationList)
            }
        }

        // Post endtime
        CoroutineScope(IO).launch {
            Repository.putEndTime(
                mIdToken,
                mShiftId,
                mTestMode,
                Calendar.getInstance().time.toString()
            )
        }
    }

    // Add user shift to database
//    private fun syncUserShift() {
//        if(!locationList.isNullOrEmpty()) {
//            val shift = Shift(
//                startTime,
//                Calendar.getInstance().time.toString(),
//                BuildConfig.VERSION_NAME,
//                locationList
//            )
//
//            CoroutineScope(IO).launch {
//                Repository.postShift(shift)
//            }
//
//            user?.getIdToken(true)?.addOnCompleteListener { task ->
//                if(task.isSuccessful) {
//                    val idToken = task.result?.token
//                    Timber.d("User token is: $idToken")
//                }
//            }
//
//            return
//            //TODO: Replace with api call
//            db.collection(if (testMode) "TestShift" else "Shift")
//                .add(shift)
//                .addOnSuccessListener { docRef ->
//                    Timber.i(
//                        "Added to ${if (testMode) "TestShift" else "Shift"}" +
//                                " with id ${docRef.id}"
//                    )
//                }
//        }
//    }

    private fun getLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Stop if permissions aren't granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("Location permissions not granted, stopping service.")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
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