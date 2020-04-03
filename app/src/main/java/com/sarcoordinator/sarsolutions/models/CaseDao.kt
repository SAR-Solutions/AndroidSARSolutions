package com.sarcoordinator.sarsolutions.models

import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable

@Entity
data class CacheShiftReport(
    @PrimaryKey val shiftId: String,
    val caseName: String? = null,
    val searchDuration: String? = null,
    var endTime: String? = null,
    val cacheTime: String? = null
)

@Entity
data class CacheLocation(
    @PrimaryKey(autoGenerate = true)
    val locationId: Int,
    val shiftId: String,
    val latitude: Double,
    val longitude: Double
)

@Entity
data class CacheVehicle(
    @PrimaryKey(autoGenerate = true) val vehicleId: Int = 0,
    val shiftId: String,
    val vehicleNumber: Int,
    val isCountyVehicle: Boolean,
    val isPersonalVehicle: Boolean,
    val type: Int,
    val milesTraveled: String
)

data class LocationsInShiftReport(
    @Embedded val shiftReport: CacheShiftReport,

    @Relation(
        parentColumn = "shiftId",
        entityColumn = "shiftId",
        entity = CacheLocation::class
    )
    val locationList: List<CacheLocation>?,

    @Relation(
        parentColumn = "shiftId",
        entityColumn = "shiftId",
        entity = CacheVehicle::class
    )
    val vehicleList: List<CacheVehicle>?
): Serializable

@Dao
interface CaseDao {

    @Transaction
    @Query("SELECT * FROM CacheShiftReport")
    fun getShiftReports(): LiveData<List<LocationsInShiftReport>>

    @Query("SELECT * FROM CacheShiftReport WHERE shiftId == :shiftId")
    suspend fun getShiftReportById(shiftId: String): CacheShiftReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftReport(shiftReport: CacheShiftReport)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocationList(locationList: List<CacheLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<CacheVehicle>)

    @Delete
    suspend fun deleteShiftReport(shiftReport: CacheShiftReport)

    @Delete
    suspend fun deleteLocations(locationList: List<CacheLocation>?)

    @Delete
    suspend fun deleteVehicles(vehicles: List<CacheVehicle>?)

//
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insertLocationList(locationList: List<CacheLocation>)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertEndTime(endTime: CacheEndTime)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertShiftReport(shiftId: CacheShiftReport)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAllVehicles(vehicle: List<CacheVehicle>?)
//
//    @Delete
//    suspend fun deleteLocations(locations: List<CacheLocation>)
//
//    @Query("SELECT * FROM CacheLocation GROUP BY shiftId")
//    fun getAllLocationShiftIds(): LiveData<List<CacheLocation>>
//
//    @Query("SELECT * FROM CacheLocation WHERE shiftId = :shiftId")
//    suspend fun getAllLocationsForShift(shiftId: String): List<CacheLocation>
//
//    @Query("SELECT * FROM CacheEndTime WHERE shiftId = :shiftId")
//    suspend fun getEndTimeForShift(shiftId: String): CacheEndTime?
//
//    @Query("SELECT * FROM CacheLocation WHERE shiftId = :shiftId")
//    fun testLocationList(shiftId: String): List<CacheLocation>

}
