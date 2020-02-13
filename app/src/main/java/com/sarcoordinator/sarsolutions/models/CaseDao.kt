package com.sarcoordinator.sarsolutions.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class RoomEndTime(
    @PrimaryKey val shiftId: String,
    val caseName: String,
    val endTime: String,
    val cacheTime: String
)

@Entity(primaryKeys = ["shiftId", "latitude", "longitude"])
data class RoomLocation(
    val shiftId: String,
    val caseName: String,
    val latitude: Double,
    val longitude: Double,
    val cacheTime: String
)

@Dao
interface CaseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocationList(locationList: List<RoomLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndTime(endTime: RoomEndTime)

    @Delete
    suspend fun deleteLocations(locations: List<RoomLocation>)

    @Query("SELECT * FROM RoomLocation GROUP BY shiftId")
    fun getAllLocationShiftIds(): LiveData<List<RoomLocation>>

    @Query("SELECT * FROM RoomLocation WHERE shiftId = :shiftId")
    suspend fun getAllLocationsForShift(shiftId: String): List<RoomLocation>

    @Query("SELECT * FROM RoomEndTime WHERE shiftId = :shiftId")
    suspend fun getEndTimeForShift(shiftId: String): RoomEndTime?

    @Query("SELECT * FROM RoomLocation WHERE shiftId = :shiftId")
    fun testLocationList(shiftId: String): List<RoomLocation>

}
