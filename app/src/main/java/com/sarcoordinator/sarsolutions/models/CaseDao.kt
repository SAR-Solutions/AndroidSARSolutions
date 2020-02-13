package com.sarcoordinator.sarsolutions.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class RoomEndTime(
    @PrimaryKey val caseId: String,
    val caseName: String,
    val endTime: String,
    val cacheTime: String
)

@Entity(primaryKeys = ["caseId", "latitude", "longitude"])
data class RoomLocation(
    val caseId: String,
    val caseName: String,
    val latitude: Double,
    val longitude: Double,
    val cacheTime: String
)

@Dao
interface CaseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLocationList(locationList: List<RoomLocation>)

    @Query("SELECT * FROM RoomLocation")
    fun getAllLocations(): LiveData<List<RoomLocation>>

    @Query("SELECT * FROM RoomLocation GROUP BY caseId")
    fun getAllLocationCaseIds(): LiveData<List<RoomLocation>>

    @Query("SELECT * FROM RoomLocation WHERE caseId = :caseId")
    fun getAllLocationsForCase(caseId: String): LiveData<List<RoomLocation>>

    @Delete
    fun deleteLocations(locations: List<RoomLocation>)

//    @Query("SELECT * FROM roomcase WHERE caseId IN (:caseId)")
//    fun getByCaseId(caseId: String): RoomCase
}
