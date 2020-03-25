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
import com.sarcoordinator.sarsolutions.util.CacheDatabase
import com.sarcoordinator.sarsolutions.util.LocalCacheRepository
import com.sarcoordinator.sarsolutions.util.LocationService
import com.sarcoordinator.sarsolutions.util.notifyObserver
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

// Viewmodel is shared between all fragments and parent activity
class SharedViewModel(application: Application) : AndroidViewModel(application) {

    var vehicleList = ArrayList<VehicleCardContent>()
    var lastUpdatedText: String? = null
    var isShiftActive = false
    var isUploadTaskActive = false
    lateinit var currentImagePath: String
    private val binder = MutableLiveData<LocationService.LocalBinder>()
    private val cacheRepo: LocalCacheRepository =
        LocalCacheRepository(CacheDatabase.getDatabase(application).casesDao())

    // Number of failed shift syncs in progress
    var numberOfSyncsInProgress: Int = 0

    // To keep track of vehicle names
    var numberOfVehicles: Int = 0
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
            Timber.d("Connected to service")
            binder.postValue(iBinder as LocationService.LocalBinder)
            isShiftActive = true
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

    // List of cases
    private val cases = MutableLiveData<ArrayList<Case>>()
    fun getCases(): LiveData<ArrayList<Case>> {
        return cases
    }

    // List of image paths for the current case
    private val currentCaseImageList = MutableLiveData<ArrayList<String>>()
    fun getImageList(filesList: List<File>? = null): LiveData<ArrayList<String>> {
        filesList?.let { files ->
            viewModelScope.launch {
                val imageList = ArrayList<String>()
                files.sortedBy { file -> file.name }.forEach { file ->
                    imageList.add(file.absolutePath)
                }
                currentCaseImageList.postValue(imageList)
            }
        }
        return currentCaseImageList
    }

    fun getNetworkExceptionObservable(): LiveData<String> {
        return netWorkExceptionText
    }

    fun clearNetworkExceptions() {
        netWorkExceptionText.value = null
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
        currentCaseImageList.value = ArrayList<String>()
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

    /************************************************ Shift report **********************************************************/

    fun addVehicle() {
        numberOfVehicles++
        vehicleList.add(
            VehicleCardContent(
                name = "Vehicle $numberOfVehicles",
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
        shiftId: String,
        searchDuration: String,
        vehicleTypeArray: List<String>
    ): Job {
        val report = ShiftReport(
            searchDuration = searchDuration,
            vehicles = vehicleListToObjects(vehicleTypeArray)
        )
        return viewModelScope.launch(IO + networkException) {
            try {
                Repository.postShiftReport(shiftId, report)
                vehicleList.clear()
            } catch (e: Exception) {
                netWorkExceptionText.postValue(e.toString())
            }
        }.apply { start() }
    }

    fun completeShiftReportSubmission() {
        isShiftActive = false
    }

    fun addImagePathToList(imagePath: String) {
        currentCaseImageList.value!!.add(imagePath)
        currentCaseImageList.notifyObserver()
    }

    /************************************************ Cache database **********************************************************/

    fun getAllShiftReports(): LiveData<List<LocationsInShiftReport>> =
        cacheRepo.allShiftReports

    fun addLocationsToCache(locations: List<LocationPoint>, shiftId: String) {
        viewModelScope.launch(Default) {
            val list = ArrayList<CacheLocation>()
            locations.forEach { location ->
                list.add(
                    CacheLocation(
                        0,
                        shiftId,
                        location.latitude,
                        location.longitude
                    )
                )
            }
            withContext(IO) {
                cacheRepo.insertLocationList(shiftId, list)
            }
        }
    }

    fun addEndTimeToCache(endTime: String, shiftId: String) {
        viewModelScope.launch(IO) {
            cacheRepo.insertShiftReport(
                CacheShiftReport(
                    shiftId = shiftId,
                    caseName = currentCase.value!!.caseName,
                    endTime = endTime,
                    cacheTime = Calendar.getInstance().time.toString()
                )
            )
        }
    }

    fun addShiftReportToCache(searchDuration: String, shiftId: String): Job {
        Timber.i("Adding shift report to cache with id $shiftId")
        return viewModelScope.launch(IO) {

            // Insert shift Reports
            cacheRepo.insertShiftReport(
                CacheShiftReport(
                    shiftId = shiftId,
                    caseName = currentCase.value!!.caseName,
                    searchDuration = searchDuration,
                    cacheTime = Calendar.getInstance().time.toString()
                )
            )

            // Insert vehicles
            cacheRepo.insertVehicleList(vehicleListToCacheObjects(shiftId), shiftId)
        }
    }

    // Post locations to backend and clear form cache
    fun submitShiftReportFromCache(
        cachedShiftReport: LocationsInShiftReport,
        vehicleTypeArray: List<String>
    ): Job {
        numberOfSyncsInProgress++
        return viewModelScope.launch(IO) {
            val shiftId = cachedShiftReport.shiftReport.shiftId
            try {
                // Api calls to submit shift reports
                cachedShiftReport.locationList?.let {
                    Repository.putLocations(shiftId, false, cacheLocListToAPILocList(it))
                }
                cachedShiftReport.shiftReport.endTime?.let {
                    Repository.putEndTime(shiftId, false, it)
                }
                cachedShiftReport.vehicleList?.let {
                    Repository.postShiftReport(
                        shiftId,
                        ShiftReport(
                            cachedShiftReport.shiftReport.searchDuration!!,
                            cacheVehicleToVehicleObjects(it, vehicleTypeArray)
                        )
                    )
                }

                // Delete posted cached objects
                cacheRepo.deleteCachedReport(cachedShiftReport)
            } catch (exception: Exception) {
                Timber.e("Failed to add locations to $shiftId")
            }
        }.apply {
            invokeOnCompletion {
                numberOfSyncsInProgress--
            }
        }
    }

    /************************************************ Helpers **********************************************************/

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

    private fun vehicleListToCacheObjects(shiftId: String): List<CacheVehicle> {
        val list = ArrayList<CacheVehicle>()
        vehicleList.forEachIndexed { index, vehicleCardContent ->
            list.add(
                CacheVehicle(
                    0,
                    shiftId,
                    index,
                    vehicleCardContent.isCountyVehicle,
                    vehicleCardContent.isPersonalVehicle,
                    vehicleCardContent.vehicleType,
                    vehicleCardContent.milesTraveled!!
                )
            )
        }
        return list
    }

    private fun cacheVehicleToVehicleObjects(
        cachedList: List<CacheVehicle>,
        vehicleTypeArray: List<String>
    ): List<Vehicle> {
        val list = ArrayList<Vehicle>()
        cachedList.forEach {
            list.add(
                Vehicle(
                    isCountyVehicle = it.isCountyVehicle,
                    isPersonalVehicle = it.isPersonalVehicle,
                    type = vehicleTypeArray[it.type],
                    milesTraveled = it.milesTraveled
                )
            )
        }
        return list
    }

    private fun cacheLocListToAPILocList(list: List<CacheLocation>): List<LocationPoint> {
        val apiList = ArrayList<LocationPoint>()
        list.forEach {
            apiList.add(LocationPoint(it.latitude, it.longitude))
        }
        return apiList
    }

    data class VehicleCardContent(
        var name: String,
        var isCountyVehicle: Boolean,
        var isPersonalVehicle: Boolean,
        var milesTraveled: String?,
        var vehicleType: Int
    )
}