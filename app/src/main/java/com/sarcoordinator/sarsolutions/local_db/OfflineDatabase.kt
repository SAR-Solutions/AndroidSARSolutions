package com.sarcoordinator.sarsolutions.local_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.auth.FirebaseAuth

@Database(
    entities = [OfflineShift::class, OfflineShiftReport::class, OfflineLocation::class, OfflineVehicle::class],
    version = 1
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun OfflineCaseDao(): OfflineCaseDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        fun getDatabase(context: Context): OfflineDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null)
                return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    FirebaseAuth.getInstance().currentUser!!.uid.plus("_offline")
                ).build()
                INSTANCE = instance
                return instance
            }
        }

        fun removeDatabaseInstance() {
            INSTANCE = null
        }
    }
}