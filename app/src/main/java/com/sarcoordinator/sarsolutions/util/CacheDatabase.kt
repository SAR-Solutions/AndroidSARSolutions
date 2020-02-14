package com.sarcoordinator.sarsolutions.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sarcoordinator.sarsolutions.models.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE RoomLocation RENAME TO CacheLocation")
        database.execSQL("ALTER TABLE RoomEndTime RENAME TO CacheEndTime")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE 'CacheShiftReport' ('shiftId' TEXT NOT NULL, 'searchDuration' TEXT NOT NULL," +
                    "PRIMARY KEY('shiftId'))"
        )
        database.execSQL(
            "CREATE TABLE 'CacheVehicle' ('shiftId' TEXT NOT NULL, 'vehicleNumber' INTEGER NOT NULL, " +
                    "'isCountyVehicle' INTEGER NOT NULL, 'isPersonalVehicle' INTEGER NOT NULL, 'type' TEXT NOT NULL, " +
                    "'milesTraveled' TEXT NOT NULL, PRIMARY KEY('shiftId', 'vehicleNumber'))"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val cursor: Cursor = database.query("SELECT * FROM 'CacheVehicle'")
        cursor.use {
            if (cursor.moveToFirst()) {
                // Get all values from database
                val values = ContentValues()
                values.put("shiftId", cursor.getString(cursor.getColumnIndex("shiftId")))
                values.put("vehicleNumber", cursor.getInt(cursor.getColumnIndex("vehicleNumber")))
                values.put(
                    "isCountyVehicle",
                    cursor.getInt(cursor.getColumnIndex("isCountyVehicle"))
                )
                values.put(
                    "isPersonalVehicle",
                    cursor.getInt(cursor.getColumnIndex("isPersonalVehicle"))
                )
                values.put("type", cursor.getInt(cursor.getColumnIndex("type")))
                values.put(
                    "milesTraveled",
                    cursor.getString(cursor.getColumnIndex("milesTraveled"))
                )
            } else {
                database.execSQL("DROP TABLE IF EXISTS 'CacheVehicle'")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'CacheVehicle' (" +
                            "'shiftId' TEXT NOT NULL, 'vehicleNumber' INTEGER NOT NULL, " +
                            "'isCountyVehicle' INTEGER NOT NULL, 'isPersonalVehicle' INTEGER NOT NULL, 'type' INT NOT NULL, " +
                            "'milesTraveled' TEXT NOT NULL, PRIMARY KEY('shiftId', 'vehicleNumber'))"
                )
            }
        }
    }
}

@Database(
    entities = [CacheLocation::class, CacheEndTime::class, CacheShiftReport::class, CacheVehicle::class],
    version = 4
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
                    "cases_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}