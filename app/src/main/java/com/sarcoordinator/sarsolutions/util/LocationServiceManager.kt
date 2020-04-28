package com.sarcoordinator.sarsolutions.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sarcoordinator.sarsolutions.BuildConfig
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.models.Case
import timber.log.Timber

object LocationServiceManager {

    private var instance: LocationServiceManager? = null
    private lateinit var activity: AppCompatActivity

    private lateinit var serviceIntent: Intent
    private var service: LocationService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
            service = (iBinder as LocationService.LocalBinder).getService()
            mIsServiceRunning.postValue(true)
            Timber.d("Connected to service")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            unbindService()
            Timber.d("Disconnected from service")
        }
    }

    private val mIsServiceRunning = MutableLiveData<Boolean>().apply { this.value = false }
    fun getServiceStatusObservable(): LiveData<Boolean> = mIsServiceRunning
    fun getServiceStatus(): Boolean = mIsServiceRunning.value!!

    private val mIsShiftComplete = MutableLiveData<Boolean>()

    fun getInstance(activity: AppCompatActivity): LocationServiceManager {
        if (instance == null) {
            this.activity = activity
            serviceIntent = Intent(LocationServiceManager.activity, LocationService::class.java)
            instance = this
        }

        // If service is already running, rebind service
        if (mIsServiceRunning.value!!)
            bindService()

        return instance!!
    }

    /**
     * Returns status of location service permission
     * -1 -> In progress
     * 0  -> Permission denied
     * 1  -> Permission granted
     */
    private val mLocationPermissionStatus = MutableLiveData<Int>()
    fun startLocationService(isTestMode: Boolean, currentCase: Case): LiveData<Int> {

        mLocationPermissionStatus.value = -1

        // Check for location permission
        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    mLocationPermissionStatus.postValue(1)

                    mIsShiftComplete.postValue(false)
                    serviceIntent.putExtra(
                        LocationService.isTestMode,
                        isTestMode
                    )

                    serviceIntent.putExtra(
                        LocationService.case,
                        currentCase
                    )

                    ContextCompat.startForegroundService(
                        activity.applicationContext,
                        serviceIntent
                    )
                    bindService()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    mLocationPermissionStatus.postValue(0)

                    if (response.isPermanentlyDenied)
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("Permission Denied")
                            .setMessage("Location permission is needed to use this feature")
                            .setNegativeButton(activity.getString(R.string.cancel), null)
                            .setPositiveButton(activity.getString(R.string.go_to_settings)) { _, _ ->
                                // Open settings
                                val settingsIntent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                                )
                                activity.startActivity(settingsIntent)
                            }
                            .show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Location Permission Required")
                        .setMessage("Location permission is needed to use this feature")
                        .setPositiveButton(activity.getString(R.string.ok)) { _, _ -> token.continuePermissionRequest() }
                        .setOnCancelListener { token.cancelPermissionRequest() }
                        .show()
                }

            })
            .withErrorListener {
                mLocationPermissionStatus.postValue(0)
                Timber.e("Unexpected error requesting location permission")
                Toast.makeText(activity, "Unexpected error, try again", Toast.LENGTH_LONG)
                    .show()
            }
            .onSameThread()
            .check()
        return mLocationPermissionStatus
    }

    fun stopLocationService(): LiveData<Boolean> {
        service?.completeShift()?.invokeOnCompletion {
            unbindService()
            activity.stopService(serviceIntent)
            mIsShiftComplete.postValue(true)
        }
        return mIsShiftComplete
    }

    private fun bindService() {
        activity.applicationContext.bindService(
            serviceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbindService() {
        try {
            activity.applicationContext.unbindService(serviceConnection)
        } catch (exception: Exception) {
            Timber.e("Error unbinding service: $exception")
        }
    }

    /********************************** Service info observables *********************************************/
    /************************* Should only be called once shift is started **********************************/
    fun getShiftIdObservable(): LiveData<String> = service!!.getShiftId()
    fun getShiftInfoObservable(): LiveData<String> = service!!.getServiceInfo()
    fun getShiftErrorsObservable(): LiveData<LocationService.ShiftErrors> =
        service!!.hasShiftEndedWithError()

    fun getLocationListObservable(): LiveData<ArrayList<Location>> = service!!.getAllLocations()
    fun getUnsyncedLocationLists(): List<Location> = service!!.getListOfUnsyncedLocations()
    fun getEndTime(): String = service!!.getEndTime() ?: "Error getting end time"
}