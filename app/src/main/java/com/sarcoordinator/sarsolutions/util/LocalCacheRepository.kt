package com.sarcoordinator.sarsolutions.util

import androidx.lifecycle.LiveData
import com.sarcoordinator.sarsolutions.models.CaseDao
import com.sarcoordinator.sarsolutions.models.RoomLocation

class LocalCacheRepository(private val caseDao: CaseDao) {

    val allLocationsCaseIds: LiveData<List<RoomLocation>> = caseDao.getAllLocationCaseIds()

    fun insertLocationList(locationList: List<RoomLocation>) =
        caseDao.insertLocationList(locationList)

    fun getAllLocationsForCase(caseId: String) =
        caseDao.getAllLocationsForCase(caseId)

    fun deleteLocations(locationList: List<RoomLocation>) =
        caseDao.deleteLocations(locationList)
}