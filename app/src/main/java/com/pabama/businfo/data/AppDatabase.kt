package com.pabama.businfo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pabama.businfo.data.dataloc.*
import com.pabama.businfo.data.entity.*
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        BusEntity::class,
        MovementLogEntity::class,
        StaffEntity::class,
        TripEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun busDataLoc(): BusDataLoc
    abstract fun movementLogDataLoc(): MovementLogDataLoc
    abstract fun staffDataLoc(): StaffDataLoc
    abstract fun tripDataLoc(): TripDataLoc

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pabama_bus_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun resetToDefaultData(database: AppDatabase) {
            database.clearAllTables()
        }
    }
}
