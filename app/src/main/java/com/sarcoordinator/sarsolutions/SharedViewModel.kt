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
import com.sarcoordinator.sarsolutions.models.ShiftReport
import com.sarcoordinator.sarsolutions.models.Vehicle
import com.sarcoordinator.sarsolutions.util.LocationService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

// Viewmodel is shared between all fragments and parent activity
class SharedViewModel : ViewModel() {

    var isShiftReportSubmitted = true
    var vehicleList = ArrayList<VehicleCardContent>()

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

    fun addVehicle() {
        vehicleList.add(
            VehicleCardContent(
                name = "Vehicle ${vehicleList.size + 1}",
                isCountyVehicle = false,
                isPersonalVehicle = false,
                milesTraveled = null,
                vehicleType = 0
            )
        )
    }

    fun updateAtPosition(position: Int, vehicle: VehicleCardContent) {
        vehicleList[position] = vehicle
    }

    fun submitShiftReport(caseId: String, searchDuration: String, vehicleTypeArray: List<String>) {
        val report = ShiftReport(
            searchDuration = searchDuration,
            vehicles = vehicleListToObjects(vehicleTypeArray)
        )
        viewModelScope.launch(IO) {
            Repository.postShiftReport(caseId, report)
        }
        vehicleList.clear()
        isShiftReportSubmitted = true
    }

    // Convert vehicleList to list of Vehicle objects
    private fun vehicleListToObjects(vehicleTypeArray: List<String>): List<Vehicle> {
        val list = ArrayList<Vehicle>()
        vehicleList.forEach { vehicle ->
            list.add(
                Vehicle(
                    isCountyVehicle = vehicle.isCountyVehicle,
                    isPersonalVehicle = vehicle.isPersonalVehicle,
                    type = vehicleTypeArray[vehicle.vehicleType],
                    milesTraveled = vehicle.milesTraveled.toString()
                )
            )
        }
        return list
    }

    data class VehicleCardContent(
        var name: String,
        var isCountyVehicle: Boolean,
        var isPersonalVehicle: Boolean,
        var milesTraveled: String?,
        var vehicleType: Int
    )
}