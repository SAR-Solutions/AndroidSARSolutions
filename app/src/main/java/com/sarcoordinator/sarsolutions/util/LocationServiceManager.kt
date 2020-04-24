package com.sarcoordinator.sarsolutions.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.sarcoordinator.sarsolutions.SharedViewModel
import com.sarcoordinator.sarsolutions.models.Case
import timber.log.Timber

object LocationServiceManager {

    private lateinit var activity: AppCompatActivity

    private lateinit var serviceIntent: Intent
    private var service: LocationService? = null

    private val mIsServiceRunning = MutableLiveData<Boolean>().apply {
        this.value = false
    }

    fun getServiceStatusObservable(): LiveData<Boolean> = mIsServiceRunning
    fun getServiceStatus(): Boolean = mIsServiceRunning.value!!

    private val viewModel: SharedViewModel by lazy {
        ViewModelProvider(activity)[SharedViewModel::class.java]
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
            mIsServiceRunning.postValue(true)
            service = (iBinder as LocationService.LocalBinder).getService()
            observeService()
            Timber.d("Connected to service")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            unbindService()
            Timber.d("Disconnected from service")
        }
    }

    private var instance: LocationServiceManager? = null

    fun getInstance(activity: AppCompatActivity): LocationServiceManager {
        if (instance == null) {
            this.activity = activity
            serviceIntent = Intent(LocationServiceManager.activity, LocationService::class.java)
            instance = this
        }

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
        unbindService()
        activity.stopService(serviceIntent)
    }

    private fun observeService() {
        service?.let {
            it.getShiftId().observeForever {
                Timber.d("Shift id is $it")
            }
            it.getServiceInfo().observeForever {
                Timber.d("Shift info: $it")
            }
        }
    }

    fun bindService() {
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
}