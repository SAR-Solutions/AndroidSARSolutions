package com.sarcoordinator.sarsolutions.local_db

import androidx.room.*
import java.io.Serializable

@Entity
data class OfflineShift(
    val caseName: String
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

@Entity
data class OfflineLocation(
    val shiftId: Int,
    val latitude: Double,
    val longitude: Double
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

@Entity
data class OfflineShiftReport(
    @PrimaryKey val shiftId: Int,
    val searchDuration: String,
    var endTime: String
) : Serializable

@Entity
data class OfflineVehicle(
    val shiftId: Int,
    val vehicleNumber: Int,
    val isCountyVehicle: Boolean,
    val isPersonalVehicle: Boolean,
    val type: Int,
    val milesTraveled: String
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

@Dao
interface OfflineCaseDao {

    @Query("SELECT * FROM OfflineShift")
    fun getOfflineShifts(): List<OfflineShift>

    @Query("SELECT * FROM OfflineLocation WHERE shiftId == :shiftId")
    suspend fun getAllLocationsForShift(shiftId: Int): List<OfflineLocation>

    @Query("SELECT * FROM OfflineShiftReport WHERE shiftId == :shiftId LIMIT 1")
    suspend fun getShiftReportForShift(shiftId: Int): OfflineShiftReport

    @Query("SELECT * FROM OfflineVehicle WHERE shiftId == :shiftId")
    suspend fun getVehiclesForShift(shiftId: Int): List<OfflineVehicle>

    @Query("DELETE FROM OfflineLocation WHERE shiftId == :shiftId")
    suspend fun deleteLocationList(shiftId: Int)

    @Query("DELETE FROM OfflineShiftReport WHERE shiftId == :shiftId")
    suspend fun deleteShiftReport(shiftId: Int)

    @Query("DELETE FROM OfflineVehicle WHERE shiftId == :shiftId")
    suspend fun deleteVehicles(shiftId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: OfflineShift)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationList(locationList: List<OfflineLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftReport(shiftReport: OfflineShiftReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<OfflineVehicle>)

    @Delete
    suspend fun deleteShift(shift: OfflineShift)
}