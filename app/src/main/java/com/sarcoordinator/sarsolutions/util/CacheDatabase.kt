package com.sarcoordinator.sarsolutions.util

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.models.CacheLocation
import com.sarcoordinator.sarsolutions.models.CacheShiftReport
import com.sarcoordinator.sarsolutions.models.CacheVehicle
import com.sarcoordinator.sarsolutions.models.CaseDao

@Database(
    entities = [CacheShiftReport::class, CacheLocation::class, CacheVehicle::class],
    version = 1
)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun casesDao(): CaseDao

    // Singleton to prevent multiple instances of the database
    companion object {
        @Volatile
        private var INSTANCE: CacheDatabase? = null

        fun getDatabase(context: Context): CacheDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null)
                return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CacheDatabase::class.java,
                    FirebaseAuth.getInstance().currentUser!!.uid
                ).build()
                INSTANCE = instance
                return instance
            }
        }

        // Remove current database instance from memory
        fun removeDatabaseInstance() {
            INSTANCE = null
        }
    }
}