package com.sarcoordinator.sarsolutions.util

import androidx.lifecycle.LiveData
import com.sarcoordinator.sarsolutions.models.*

class LocalCacheRepository(private val caseDao: CaseDao) {

    val allLocationsShiftIds: LiveData<List<CacheLocation>> = caseDao.getAllLocationShiftIds()

    suspend fun insertLocationList(locationList: List<CacheLocation>) =
        caseDao.insertLocationList(locationList)

    suspend fun getAllLocationsForShift(shiftId: String) =
        caseDao.getAllLocationsForShift(shiftId)

    suspend fun deleteLocations(locationList: List<CacheLocation>) =
        caseDao.deleteLocations(locationList)

    suspend fun getEndTimeForShift(shiftId: String) =
        caseDao.getEndTimeForShift(shiftId)

    suspend fun insertEndTime(endTime: CacheEndTime) =
        caseDao.insertEndTime(endTime)

    suspend fun insertShiftReport(
        shiftReport: CacheShiftReport,
        vehicles: List<CacheVehicle>?
    ) {
        caseDao.insertShiftReport(shiftReport)
        caseDao.insertAllVehicles(vehicles)
    }
}