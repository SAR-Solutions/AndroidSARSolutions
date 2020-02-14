package com.sarcoordinator.sarsolutions.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class CacheEndTime(
    @PrimaryKey val shiftId: String,
    val caseName: String,
    val endTime: String,
    val cacheTime: String
)

@Entity(primaryKeys = ["shiftId", "latitude", "longitude"])
data class CacheLocation(
    val shiftId: String,
    val caseName: String,
    val latitude: Double,
    val longitude: Double,
    val cacheTime: String
)

@Entity
data class CacheShiftReport(
    @PrimaryKey val shiftId: String,
    val searchDuration: String
)

@Entity(primaryKeys = ["shiftId", "vehicleNumber"])
data class CacheVehicle(
    val shiftId: String,
    val vehicleNumber: Int,
    val isCountyVehicle: Boolean,
    val isPersonalVehicle: Boolean,
    val type: Int,
    val milesTraveled: String
)

@Dao
interface CaseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocationList(locationList: List<CacheLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndTime(endTime: CacheEndTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftReport(shiftId: CacheShiftReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVehicles(vehicle: List<CacheVehicle>?)

    @Delete
    suspend fun deleteLocations(locations: List<CacheLocation>)

    @Query("SELECT * FROM CacheLocation GROUP BY shiftId")
    fun getAllLocationShiftIds(): LiveData<List<CacheLocation>>

    @Query("SELECT * FROM CacheLocation WHERE shiftId = :shiftId")
    suspend fun getAllLocationsForShift(shiftId: String): List<CacheLocation>

    @Query("SELECT * FROM CacheEndTime WHERE shiftId = :shiftId")
    suspend fun getEndTimeForShift(shiftId: String): CacheEndTime?

    @Query("SELECT * FROM CacheLocation WHERE shiftId = :shiftId")
    fun testLocationList(shiftId: String): List<CacheLocation>

}
