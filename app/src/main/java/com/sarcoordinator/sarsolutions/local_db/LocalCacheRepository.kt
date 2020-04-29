package com.sarcoordinator.sarsolutions.local_db

import androidx.lifecycle.LiveData

class LocalCacheRepository(private val caseDao: CaseDao) {

    val allShiftReports: LiveData<List<LocationsInShiftReport>> = caseDao.getShiftReports()

    suspend fun insertLocationList(shiftId: String, locationList: List<CacheLocation>) {
        // Only make new case if one doesn't exist
        val case = caseDao.getShiftReportById(shiftId)
        if (case == null)
            caseDao.insertShiftReport(
                CacheShiftReport(
                    shiftId
                )
            )
        caseDao.insertLocationList(locationList)
    }

    suspend fun insertShiftReport(shiftReport: CacheShiftReport) {
        // Make new case if it doesn't exist
        val case = caseDao.getShiftReportById(shiftReport.shiftId)
        // Update current case
        if (case?.endTime != null)
            shiftReport.endTime = case.endTime
        caseDao.insertShiftReport(shiftReport)
    }

    suspend fun insertVehicleList(vehicles: List<CacheVehicle>, shiftId: String) {
        val case = caseDao.getShiftReportById(shiftId)
        if (case == null)
            caseDao.insertShiftReport(
                CacheShiftReport(
                    shiftId
                )
            )
        caseDao.insertVehicles(vehicles)
    }

    suspend fun deleteCachedReport(report: LocationsInShiftReport) {
        caseDao.deleteLocations(report.locationList)
        caseDao.deleteVehicles(report.vehicleList)
        caseDao.deleteShiftReport(report.shiftReport)
    }

}