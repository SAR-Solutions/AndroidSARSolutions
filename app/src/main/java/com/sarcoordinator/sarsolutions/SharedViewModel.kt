package com.sarcoordinator.sarsolutions

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sarcoordinator.sarsolutions.api.Repository
import com.sarcoordinator.sarsolutions.models.*
import com.sarcoordinator.sarsolutions.util.CasesRoomDatabase
import com.sarcoordinator.sarsolutions.util.LocalCacheRepository
import com.sarcoordinator.sarsolutions.util.LocationService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

// Viewmodel is shared between all fragments and parent activity
class SharedViewModel(application: Application) : AndroidViewModel(application) {

    var vehicleList = ArrayList<VehicleCardContent>()

    lateinit var lastUpdatedText: String
    private val binder = MutableLiveData<LocationService.LocalBinder>()

    private val mIsShiftActive = MutableLiveData<Boolean>()
    val isShiftActive: LiveData<Boolean> = mIsShiftActive

    private val cacheRepo: LocalCacheRepository =
        LocalCacheRepository(CasesRoomDatabase.getDatabase(application).casesDao())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
            Timber.d("Connected to service")
            binder.postValue(iBinder as LocationService.LocalBinder)
            mIsShiftActive.value = true
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

    fun clearNetworkExceptions() {
        netWorkExceptionText.value = null
    }


    private val cases = MutableLiveData<ArrayList<Case>>()
    fun getCases(): LiveData<ArrayList<Case>> {
        return cases
    }

    fun refreshCases() {
        viewModelScope.launch(IO + networkException) {
            val result = ArrayList<Case>()
            try {
                Repository.getCases().forEach { case ->
                    result.add(case)
                }
                cases.postValue(result)
            } catch (e: Exception) {
                netWorkExceptionText.postValue(e.toString())
            }
        }
    }

    val currentCase = MutableLiveData<Case>()

    fun getCaseDetails(caseId: String): LiveData<Case> {
        viewModelScope.launch(IO + networkException) {
            try {
                currentCase.postValue(Repository.getCaseDetail(caseId).also {
                    it.id = caseId
                })
            } catch (e: Exception) {
                netWorkExceptionText.postValue(e.toString())
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
        binder.value = null
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

    // Update vehicle object at position in vehicleList
    fun updateVehicleAtPosition(position: Int, vehicle: VehicleCardContent) {
        vehicleList[position] = vehicle
    }

    fun submitShiftReport(
        caseId: String,
        searchDuration: String,
        vehicleTypeArray: List<String>
    ): Job {
        val report = ShiftReport(
            searchDuration = searchDuration,
            vehicles = vehicleListToObjects(vehicleTypeArray)
        )
        return viewModelScope.launch(IO + networkException) {
            try {
                Repository.postShiftReport(caseId, report)
                vehicleList.clear()
            } catch (e: Exception) {
                netWorkExceptionText.postValue(e.toString())
            }
        }
    }

    fun completeShiftReportSubmission() {
        mIsShiftActive.value = false
    }

    fun addLocationsToCache(locations: List<LocationPoint>) {
        viewModelScope.launch(Default) {
            val list = ArrayList<RoomLocation>()
            locations.forEach { location ->
                list.add(
                    RoomLocation(
                        currentCase.value!!.id,
                        location.latitude,
                        location.longitude
                    )
                )
            }
            withContext(IO) {
                cacheRepo.insertLocationList(list)
            }
        }
    }

    fun getAllLocationCaseIdsFromCache(): LiveData<List<String>> {
        return cacheRepo.allLocationsCaseIds
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