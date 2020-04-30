package com.sarcoordinator.sarsolutions.local_db

import androidx.room.*
import java.io.Serializable

@Entity
data class OfflineShift(
    val caseName: String
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Entity
data class OfflineLocation(
    val shiftId: Long,
    val latitude: Double,
    val longitude: Double
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Entity
data class OfflineShiftReport(
    @PrimaryKey val shiftId: Long,
    val searchDuration: String,
    var endTime: String
) : Serializable

@Entity
data class OfflineVehicle(
    val shiftId: Long,
    val vehicleNumber: Int,
    val isCountyVehicle: Boolean,
    val isPersonalVehicle: Boolean,
    val type: Int,
    val milesTraveled: String
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Dao
interface OfflineCaseDao {

    @Query("SELECT * FROM OfflineShift")
    fun getOfflineShifts(): List<OfflineShift>

    @Query("SELECT * FROM OfflineLocation WHERE shiftId == :shiftId")
    suspend fun getAllLocationsForShift(shiftId: Long): List<OfflineLocation>

    @Query("SELECT * FROM OfflineShiftReport WHERE shiftId == :shiftId LIMIT 1")
    suspend fun getShiftReportForShift(shiftId: Long): OfflineShiftReport

    @Query("SELECT * FROM OfflineVehicle WHERE shiftId == :shiftId")
    suspend fun getVehiclesForShift(shiftId: Long): List<OfflineVehicle>

    @Query("DELETE FROM OfflineLocation WHERE shiftId == :shiftId")
    suspend fun deleteLocationList(shiftId: Long)

    @Query("DELETE FROM OfflineShiftReport WHERE shiftId == :shiftId")
    suspend fun deleteShiftReport(shiftId: Long)

    @Query("DELETE FROM OfflineVehicle WHERE shiftId == :shiftId")
    suspend fun deleteVehicles(shiftId: Long)

    @Query("DELETE FROM OfflineShift WHERE id == :shiftId")
    suspend fun deleteShift(shiftId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: OfflineShift): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationList(locationList: List<OfflineLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftReport(shiftReport: OfflineShiftReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<OfflineVehicle>)
}