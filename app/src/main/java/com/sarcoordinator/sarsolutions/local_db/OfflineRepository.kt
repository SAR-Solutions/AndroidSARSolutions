package com.sarcoordinator.sarsolutions.local_db

class OfflineRepository(private val offlineCaseDao: OfflineCaseDao) {

    fun getAllShifts() = offlineCaseDao.getOfflineShifts()

    suspend fun insertOfflineShift(shift: OfflineShift) = offlineCaseDao.insertShift(shift)

    suspend fun deleteShift(shiftId: Long) {
        offlineCaseDao.deleteVehicles(shiftId)
        offlineCaseDao.deleteLocationList(shiftId)
        offlineCaseDao.deleteShiftReport(shiftId)
        offlineCaseDao.deleteShift(shiftId)
    }

    suspend fun insertLocationList(locationList: List<OfflineLocation>) =
        offlineCaseDao.insertLocationList(locationList)

    suspend fun insertShiftReport(shiftReport: OfflineShiftReport) =
        offlineCaseDao.insertShiftReport(shiftReport)

    suspend fun insertVehicles(vehicles: List<OfflineVehicle>) =
        offlineCaseDao.insertVehicles(vehicles)

    suspend fun getAllLocationsInShift(shiftId: Long) =
        offlineCaseDao.getAllLocationsForShift(shiftId)

    suspend fun getShiftReport(shiftId: Long) = offlineCaseDao.getShiftReportForShift(shiftId)

    suspend fun getVehiclesInShift(shiftId: Long) = offlineCaseDao.getVehiclesForShift(shiftId)

}