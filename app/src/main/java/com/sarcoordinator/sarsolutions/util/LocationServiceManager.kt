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

class LocationServiceManager(private val activity: AppCompatActivity) {

    private var serviceIntent: Intent = Intent(activity, LocationService::class.java)

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
            Timber.d("Connected to service")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            unbindService()
            Timber.d("Disconnected from service")
        }
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

        ContextCompat.startForegroundService(activity, serviceIntent)
        bindService()
    }

    fun stopLocationService() {
        unbindService()
        activity.stopService(serviceIntent)
    }

    private fun bindService() {
        activity.bindService(
            serviceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun unbindService() {
        activity.unbindService(serviceConnection)
        mIsServiceRunning.postValue(false)
    }
}