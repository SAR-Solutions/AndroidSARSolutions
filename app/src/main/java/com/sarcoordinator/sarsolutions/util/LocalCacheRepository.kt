package com.sarcoordinator.sarsolutions.util

import androidx.lifecycle.LiveData
import com.sarcoordinator.sarsolutions.models.CaseDao
import com.sarcoordinator.sarsolutions.models.RoomEndTime
import com.sarcoordinator.sarsolutions.models.RoomLocation

class LocalCacheRepository(private val caseDao: CaseDao) {

    val allLocationsCaseIds: LiveData<List<RoomLocation>> = caseDao.getAllLocationShiftIds()

    suspend fun insertLocationList(locationList: List<RoomLocation>) =
        caseDao.insertLocationList(locationList)

    suspend fun getAllLocationsForShift(shiftId: String) =
        caseDao.getAllLocationsForShift(shiftId)

    suspend fun deleteLocations(locationList: List<RoomLocation>) =
        caseDao.deleteLocations(locationList)

    suspend fun getEndTimeForShift(shiftId: String) =
        caseDao.getEndTimeForShift(shiftId)

    suspend fun insertEndTime(endTime: RoomEndTime) =
        caseDao.insertEndTime(endTime)
}