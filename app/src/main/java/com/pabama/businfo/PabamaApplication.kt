package com.pabama.businfo

import android.app.Application
import com.pabama.businfo.data.AppDatabase
import com.pabama.businfo.data.remote.FirebaseSyncManager
import com.pabama.businfo.data.repository.BusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PabamaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        BusRepository(
            database.busDataLoc(),
            database.movementLogDataLoc(),
            database.staffDataLoc(),
            database.tripDataLoc()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase Cloud Firestore synchronization
        FirebaseSyncManager.init(this, database, applicationScope)
    }
}
