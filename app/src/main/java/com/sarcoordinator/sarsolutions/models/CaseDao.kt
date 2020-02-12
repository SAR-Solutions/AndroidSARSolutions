package com.sarcoordinator.sarsolutions.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class RoomEndTime(
    @PrimaryKey val caseId: String,
    @ColumnInfo val endTime: String?
)

@Entity(primaryKeys = ["latitude", "longitude"])
data class RoomLocation(
    val caseId: String,
    @ColumnInfo val latitude: Double,
    @ColumnInfo val longitude: Double
)

@Dao
interface CaseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLocationList(locationList: List<RoomLocation>)

    @Query("SELECT * FROM RoomLocation")
    fun getAllLocations(): LiveData<List<RoomLocation>>

    @Query("SELECT DISTINCT caseId FROM RoomLocation")
    fun getAllLocationCaseIds(): LiveData<List<String>>

//    @Query("SELECT * FROM roomcase WHERE caseId IN (:caseId)")
//    fun getByCaseId(caseId: String): RoomCase
}
