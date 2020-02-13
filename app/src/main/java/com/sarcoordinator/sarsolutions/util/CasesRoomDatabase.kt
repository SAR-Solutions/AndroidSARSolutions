package com.sarcoordinator.sarsolutions.util

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sarcoordinator.sarsolutions.models.CaseDao
import com.sarcoordinator.sarsolutions.models.RoomEndTime
import com.sarcoordinator.sarsolutions.models.RoomLocation


@Database(entities = [RoomLocation::class, RoomEndTime::class], version = 1)
abstract class CasesRoomDatabase : RoomDatabase() {
    abstract fun casesDao(): CaseDao

    // Singleton to prevent multiple instances of the database
    companion object {
        @Volatile
        private var INSTANCE: CasesRoomDatabase? = null

        fun getDatabase(context: Context): CasesRoomDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null)
                return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CasesRoomDatabase::class.java,
                    "cases_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}