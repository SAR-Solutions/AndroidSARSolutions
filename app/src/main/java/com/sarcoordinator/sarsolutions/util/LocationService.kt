package com.sarcoordinator.sarsolutions.util

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import com.sarcoordinator.sarsolutions.BuildConfig
import com.sarcoordinator.sarsolutions.MainActivity
import com.sarcoordinator.sarsolutions.MyApplication.Companion.CHANNEL_ID
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.api.Repository
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.models.LocationPoint
import com.sarcoordinator.sarsolutions.models.Shift
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class LocationService : Service() {

    companion object {
        const val isTestMode = "TEST_MODE"
        const val case = "CASE"
    }

    enum class ShiftErrors {
        START_SHIFT, PUT_END_TIME, PUT_LOCATIONS
    }

    private var mTestMode: Boolean = false
    private lateinit var mCase: Case
    private val shiftId = MutableLiveData<String>()
    fun getShiftId(): LiveData<String> = shiftId

    private val serviceInfoText = MutableLiveData<String>()
    fun getServiceInfo(): LiveData<String> = serviceInfoText

    private val shiftEndedWithError = MutableLiveData<ShiftErrors>()
    fun hasShiftEndedWithError(): LiveData<ShiftErrors> = shiftEndedWithError

    private val isServiceSyncRunning = MutableLiveData<Boolean>()
    fun isServiceSyncRunning(): LiveData<Boolean> = isServiceSyncRunning


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val UPDATE_INTERVAL = 4000L
    private val FASTEST_INTERVAL = 2000L

    private lateinit var addPathsJob: CompletableJob
    private var addPathsJobIsSyncing = false

    private val locationList: ArrayList<LocationPoint> = ArrayList()
    private val pendingSyncList: ArrayList<LocationPoint> = ArrayList()
    fun getSyncList(): ArrayList<LocationPoint> = pendingSyncList

    private var mEndTime: String? = null
    fun getEndTime(): String? = mEndTime

    init {
        isServiceSyncRunning.value = true
    }

    /*
    * Makes network call to post data
    * Handles adding location to locationList or pendingSyncList
    */
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            super.onLocationResult(locationResult)

            locationResult ?: return
            if (shiftId.value == null)
                return

            for (location in locationResult.locations) {

                if (location.accuracy > 20) // Remove outliers for bad data points
                    continue
                // Don't record if already exists
                if (locationList.contains(LocationPoint(location.latitude, location.longitude)))
                    continue
                locationList.add(LocationPoint(location.latitude, location.longitude))
                serviceInfoText.value = "Last updated at \n" + Calendar.getInstance().time
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(1, getNotification(serviceInfoText.value.toString()))

                // Start addPathsjob if 10 or more locations exist and job isn't running
                if (locationList.size >= 10 && !addPathsJobIsSyncing) {
                    addPathsJobIsSyncing = true
                    val tempList = ArrayList<LocationPoint>()
                    tempList.addAll(locationList)
                    locationList.clear()
                    CoroutineScope(IO + addPathsJob).launch {
                        try {
                            Repository.putLocations(shiftId.value!!, mTestMode, tempList)
                        } catch (exception: Exception) {
                            pendingSyncList.addAll(tempList)
                            Timber.e("No internet connection found, added paths to pendingSyncList")
                        }
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

        // Intent and argument checks
        if (intent == null)
            throw Exception("Service needs to be called with specified intent fields")

        mTestMode = intent.extras!!.getBoolean(isTestMode, false)
        mCase = intent.extras!!.getSerializable(case) as Case

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get and set mShiftId
        CoroutineScope(IO).launch {
            val shift = Shift(
                Calendar.getInstance().time.toString(),
                BuildConfig.VERSION_NAME
            )

            try {
                shiftId.postValue(Repository.postStartShift(shift, mCase.id, mTestMode).shiftId)
                Timber.d("Location service started")
                serviceInfoText.postValue(getString(R.string.started_shift))
            } catch (e: Exception) {
                Timber.e(getString(R.string.error_starting))
                withContext(Main) {
                    serviceInfoText.value = getString(R.string.error_starting)
                    shiftEndedWithError.value = ShiftErrors.START_SHIFT
                }
                onDestroy()
            }
        }

        if (!::addPathsJob.isInitialized) {
            addPathsJob = Job()
        }

        // Start service notification intent
        // Id must NOT be 0
        // Ref: https://developer.android.com/guide/components/services.html#kotlin
        startForeground(1, getNotification(getString(R.string.starting_location_service)))

        getLocation()
        return START_NOT_STICKY
    }

    fun getNotification(title: String): Notification {
        val resultIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val resultPendingIntent = PendingIntent.getActivity(
            this, 0, resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notification is needed for >= API 26 for foreground services
        return NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(
                "Case ID: ${mCase.id}\n" +
                        "Case Date: ${GlobalUtil.convertEpochToDate(mCase.date)}"
            )
            setSmallIcon(R.drawable.ic_location)
            setContentIntent(resultPendingIntent)
            setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(
                        "Case ID: ${mCase.id}\n" +
                                "Case Date: ${GlobalUtil.convertEpochToDate(mCase.date)}"
                    )
            )
        }.build()
    }

    override fun onDestroy() {
        stopForeground(true)
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    /*
     * Removed location update callback
     * End shift, set endTime and sync remaining points
     */
    fun completeShift() {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)

        serviceInfoText.postValue("Syncing shift data with server")

        CoroutineScope(IO).launch {
            // Nothing to do, if shift didn't start
            if (shiftEndedWithError.value == ShiftErrors.START_SHIFT)
                return@launch

            val endTime = Calendar.getInstance().time.toString()

            while (shiftId.value == null) {
                Timber.d(
                    "Shift didn't start and no points were recorded\n" +
                            "Waiting to get shift and token id"
                )
                delay(1000)
            }

            // Sync remaining points
            if (locationList.isNotEmpty() || pendingSyncList.isNotEmpty()) {
                pendingSyncList.addAll(locationList)
                try {
                    Repository.putLocations(shiftId.value!!, mTestMode, pendingSyncList)
                } catch (exception: Exception) {
                    Timber.e("Error with network call putting locations\n$exception")
                    shiftEndedWithError.postValue(ShiftErrors.PUT_LOCATIONS)
                }
            }

            // Post endtime
            try {
                Repository.putEndTime(
                    shiftId.value!!,
                    mTestMode,
                    endTime
                )
            } catch (exception: Exception) {
                Timber.e("Error with network call putting end time\n$exception")
                mEndTime = endTime
                shiftEndedWithError.postValue(ShiftErrors.PUT_END_TIME)
            }
            isServiceSyncRunning.postValue(false)
        }
    }

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
}