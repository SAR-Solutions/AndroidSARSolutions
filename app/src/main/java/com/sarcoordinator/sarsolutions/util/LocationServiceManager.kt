package com.sarcoordinator.sarsolutions.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    fun startLocationService(isTestMode: Boolean, currentCase: Case) {
        serviceIntent.putExtra(
            LocationService.isTestMode,
            isTestMode
        )

        serviceIntent.putExtra(
            LocationService.case,
            currentCase
        )

        ContextCompat.startForegroundService(activity.applicationContext, serviceIntent)
        bindService()
    }

    fun stopLocationService() {
        service?.completeShift()?.invokeOnCompletion {
            unbindService()
            activity.stopService(serviceIntent)
        }
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
        mIsServiceRunning.postValue(false)
    }

    /********************************** Service info observables *********************************************/
    /************************* Should only be called once shift is started **********************************/
    fun getShiftIdObservable(): LiveData<String> = service!!.getShiftId()
    fun getShiftInfoObservable(): LiveData<String> = service!!.getServiceInfo()
    fun getShiftErrorsObservable(): LiveData<LocationService.ShiftErrors> =
        service!!.hasShiftEndedWithError()

    fun getLocationListObservable(): LiveData<ArrayList<Location>> = service!!.getAllLocations()
}