package com.sarcoordinator.sarsolutions.local_db

class OfflineRepository(private val offlineCaseDao: OfflineCaseDao) {

    fun getAllShifts() = offlineCaseDao.getOfflineShifts()

    suspend fun insertOfflineShift(shift: OfflineShift) = offlineCaseDao.insertShift(shift)

    suspend fun deleteShift(shift: OfflineShift) {
        offlineCaseDao.deleteVehicles(shift.id)
        offlineCaseDao.deleteLocationList(shift.id)
        offlineCaseDao.deleteShiftReport(shift.id)
        offlineCaseDao.deleteShift(shift)
    }

    suspend fun insertLocationList(locationList: List<OfflineLocation>) =
        offlineCaseDao.insertLocationList(locationList)

    suspend fun insertShiftReport(shiftReport: OfflineShiftReport) =
        offlineCaseDao.insertShiftReport(shiftReport)

    suspend fun insertVehicles(vehicles: List<OfflineVehicle>) =
        offlineCaseDao.insertVehicles(vehicles)

    suspend fun getAllLocationsInShift(shiftId: Int) =
        offlineCaseDao.getAllLocationsForShift(shiftId)

    suspend fun getShiftReport(shiftId: Int) = offlineCaseDao.getShiftReportForShift(shiftId)

    suspend fun getVehiclesInShift(shiftId: Int) = offlineCaseDao.getVehiclesForShift(shiftId)

}