package com.example.sarsolutions

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.sarsolutions.api.Repository
import com.example.sarsolutions.models.Case
import com.example.sarsolutions.services.LocationService
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class CasesViewModel : ViewModel() {

    var isTestingEnabled = false
    lateinit var lastUpdatedText: String
    private val binder = MutableLiveData<LocationService.LocalBinder>()
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
            Timber.d("Connected to service")
            binder.postValue(iBinder as LocationService.LocalBinder)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Timber.d("Disconnected from service")
            binder.postValue(null)
        }
    }

    val cases: LiveData<ArrayList<Case>> = liveData(Dispatchers.IO) {
        val result = ArrayList<Case>()
        Repository.getCases().caseIds.forEach { id ->
            result.add(Repository.getCaseDetail(id))
        }
        emit(result)
    }

    fun getBinder(): LiveData<LocationService.LocalBinder> {
        return binder
    }

    fun getServiceConnection(): ServiceConnection {
        return serviceConnection
    }

    fun removeService() {
        binder.postValue(null)
    }
}