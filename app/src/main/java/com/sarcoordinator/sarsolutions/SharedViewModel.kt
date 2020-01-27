package com.sarcoordinator.sarsolutions

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarcoordinator.sarsolutions.api.Repository
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.services.LocationService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Observe coroutineFailureText to get notified on network failure
    private val networkException = CoroutineExceptionHandler { coroutineContext, throwable ->
        Timber.e(throwable)
        netWorkExceptionText.postValue("Failed network call, try again.")
    }

    private val netWorkExceptionText = MutableLiveData<String>()

    fun getNetworkExceptionObservable(): LiveData<String> {
        return netWorkExceptionText
    }


    private val cases = MutableLiveData<ArrayList<Case>>()
    fun getCases(): LiveData<ArrayList<Case>> {
        return cases
    }

    fun refreshCases() {
        viewModelScope.launch(IO + networkException) {
            val result = ArrayList<Case>()
            Repository.getCases().forEach { case ->
                result.add(case)
            }
            cases.postValue(result)
        }
    }

    val currentCase = MutableLiveData<Case>()

    fun getCaseDetails(caseId: String): LiveData<Case> {
        viewModelScope.launch {
            withContext(IO) {
                currentCase.postValue(Repository.getCaseDetail(caseId).also {
                    it.id = caseId
                })
            }
        }
        return currentCase
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