package com.sarcoordinator.sarsolutions

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.sarcoordinator.sarsolutions.api.Repository
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.services.LocationService
import kotlinx.coroutines.Dispatchers.IO
import timber.log.Timber

// Viewmodel is shared between all fragments and parent activity
class SharedViewModel : ViewModel() {

    lateinit var lastUpdatedText: String
    private val binder = MutableLiveData<LocationService.LocalBinder>()
    lateinit var mAuthToken: String
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

    val cases: LiveData<ArrayList<Case>> = liveData(IO) {
        val result = ArrayList<Case>()
        Repository.getCases().caseIds.forEach { id ->
            result.add(Repository.getCaseDetail(id, mAuthToken).also { case ->
                case.id = id
            })
        }
        emit(result)
    }

    fun mAuthTokenExists(): Boolean {
        if (!::mAuthToken.isInitialized)
            return false
        return true
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