package com.pabama.businfo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pabama.businfo.data.dataloc.*
import com.pabama.businfo.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BusEntity::class,
        MovementLogEntity::class,
        StaffEntity::class,
        TripEntity::class
    ],
    version = 5,
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
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun resetToDefaultData(database: AppDatabase) {
            database.clearAllTables()
            populateInitialData(database)
        }

        suspend fun populateInitialData(database: AppDatabase) {
            // Seed only the authorized accounts required for login and administrative CRUD
            database.staffDataLoc().insertStaff(
                StaffEntity(
                    staffId = "STAFF001",
                    password = "staff123",
                    name = "Juan Dela Cruz"
                )
            )
            database.staffDataLoc().insertStaff(
                StaffEntity(
                    staffId = "ADMIN001",
                    password = "admin123",
                    name = "Maria Santos"
                )
            )
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }
    }
}
