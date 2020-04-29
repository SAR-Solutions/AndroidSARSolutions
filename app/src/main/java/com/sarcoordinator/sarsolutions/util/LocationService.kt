package com.sarcoordinator.sarsolutions.util

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.sarcoordinator.sarsolutions.BaseApplication.Companion.CHANNEL_ID
import com.sarcoordinator.sarsolutions.BuildConfig
import com.sarcoordinator.sarsolutions.MainActivity
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.api.Case
import com.sarcoordinator.sarsolutions.api.Repository
import com.sarcoordinator.sarsolutions.api.Shift
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
        START_SHIFT, PUT_END_TIME, PUT_LOCATIONS, GET_SHIFT_ID
    }

    private var mTestMode: Boolean = false
    private lateinit var mCase: Case
    private var offlineMode: Boolean = false
    private val shiftId = MutableLiveData<String>()
    fun getShiftId(): LiveData<String> = shiftId

    private val serviceInfoText = MutableLiveData<String>()
    fun getServiceInfo(): LiveData<String> = serviceInfoText

    private val shiftEndedWithError = MutableLiveData<ShiftErrors>()
    fun hasShiftEndedWithError(): LiveData<ShiftErrors> = shiftEndedWithError

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val UPDATE_INTERVAL = 4000L
    private val FASTEST_INTERVAL = 2000L

    private lateinit var addPathsJob: CompletableJob
    private var addPathsJobIsSyncing = false

    private val locationList: MutableLiveData<ArrayList<Location>> = MutableLiveData()
    fun getAllLocations(): LiveData<ArrayList<Location>> = locationList

    // Locations from this pointer till the end of the list will be synced
    private var locationSyncListPointer = 0

    fun getListOfUnsyncedLocations(): List<Location> {
        return locationList.value!!.subList(locationSyncListPointer, locationList.value!!.size)
            .toList()
    }

    private var mEndTime: String? = null
    fun getEndTime(): String? = mEndTime

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

                locationList.value!!.add(location)
                locationList.notifyObserver()

                serviceInfoText.value = "Last updated at \n" + Calendar.getInstance().time
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(1, getNotification(serviceInfoText.value.toString()))

                Timber.d(
                    "List size before sync: ${locationList.value!!.size}\n" +
                            "Pointer: $locationSyncListPointer"
                )

                // Start addPathsjob if 10 or more locations exist and job isn't running
                if (!addPathsJobIsSyncing && getListOfUnsyncedLocations().size >= 10) {
                    val newPointerValue = locationList.value!!.size - 1
                    addPathsJobIsSyncing = true
                    val locationsToSync = getListOfUnsyncedLocations()
                    CoroutineScope(IO + addPathsJob).launch {
                        try {
                            Timber.d(
                                "Syncing list size: ${locationsToSync.size}\n" +
                                        "Pointer: $locationSyncListPointer"
                            )
                            if (mTestMode) {
                                // Emulate network call
                                Timber.d("Emulating syncing shifts")
                                delay(2000)
                            } else {
                                Repository.putLocations(shiftId.value!!, mTestMode, locationsToSync)
                            }
                            locationSyncListPointer = newPointerValue

                            Timber.d(
                                "List size after sync: ${locationList.value!!.size}\n" +
                                        "Pointer: $locationSyncListPointer"
                            )
                        } catch (exception: Exception) {
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

        val intentCase: Case? = intent.extras!!.getSerializable(case) as Case?
        if (intentCase != null) {
            mCase = intentCase
        } else {
            offlineMode = true
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationList.postValue(ArrayList())

        // Get and set mShiftId
        CoroutineScope(IO).launch {
            val shift = Shift(
                Calendar.getInstance().time.toString(),
                BuildConfig.VERSION_NAME
            )

            try {
                if (mTestMode) {
                    // Emulate network call
                    Timber.d("Emulating getting shift id")
                    delay(1000)
                    shiftId.postValue("Test Shift Id ${Calendar.getInstance().timeInMillis}")
                } else {
                    shiftId.postValue(Repository.postStartShift(shift, mCase.id, mTestMode).shiftId)
                }
                Timber.d("Location service started")
                serviceInfoText.postValue(getString(R.string.started_shift))
            } catch (e: Exception) {
                Timber.e(getString(R.string.error_starting))
                withContext(Main) {
                    serviceInfoText.value = getString(R.string.error_starting)
                    shiftEndedWithError.postValue(ShiftErrors.START_SHIFT)
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        completeShift().invokeOnCompletion {
            onDestroy()
        }
    }

    /*
     * Removed location update callback
     * End shift, set endTime and sync remaining points
     */
    fun completeShift(): Job {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)

        serviceInfoText.postValue("Syncing shift data with server")

        return CoroutineScope(IO).launch {
            // Nothing to do, if shift didn't start
            if (shiftEndedWithError.value == ShiftErrors.START_SHIFT)
                return@launch

            val endTime = Calendar.getInstance().time.toString()

            var networkRetries = 0

            while (shiftId.value == null) {
                if(networkRetries > 10) {
                    shiftEndedWithError.postValue(ShiftErrors.GET_SHIFT_ID)
                    return@launch
                }
                Timber.d(
                    "Shift didn't start and no points were recorded\n" +
                            "Waiting to get shift and token id" +
                            "Retry number: $networkRetries"
                )
                networkRetries++
                delay(1000)
            }

            // Sync remaining points
            if (getListOfUnsyncedLocations().isNotEmpty()) {
                try {
                    if (mTestMode) {
                        // Emulate network call
                        Timber.d("Emulating syncing remaining locations with error")
                        delay(1000)
                        throw Exception("Emulating failed location sync")
                    } else {
                        Repository.putLocations(
                            shiftId.value!!,
                            mTestMode,
                            getListOfUnsyncedLocations()
                        )
                    }
                    locationSyncListPointer = locationList.value!!.size
                    Timber.d("Final location sync pointer: $locationSyncListPointer for list size: ${locationList.value?.size}")
                } catch (exception: Exception) {
                    Timber.e("Error with network call putting locations\n$exception")
                    shiftEndedWithError.postValue(ShiftErrors.PUT_LOCATIONS)
                }
            }

            // Post endtime
            try {
                if (mTestMode) {
                    // Emulate network call
                    Timber.d("Emulating syncing remaining locations with error")
                    delay(2000)
                    throw Exception("Emulating failed end time sync")
                } else {
                    Repository.putEndTime(
                        shiftId.value!!,
                        mTestMode,
                        endTime
                    )
                }
            } catch (exception: Exception) {
                Timber.e("Error with network call putting end time\n$exception")
                mEndTime = endTime
                shiftEndedWithError.postValue(ShiftErrors.PUT_END_TIME)
            }
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
