package com.pabama.businfo.data.repository

import androidx.lifecycle.LiveData
import com.pabama.businfo.data.AppDatabase
import com.pabama.businfo.data.dataloc.*
import com.pabama.businfo.data.entity.*
import com.pabama.businfo.data.remote.FirebaseSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusRepository(
    private val busDataLoc: BusDataLoc,
    private val movementLogDataLoc: MovementLogDataLoc,
    private val staffDataLoc: StaffDataLoc,
    private val tripDataLoc: TripDataLoc
) {
    val allBuses: LiveData<List<BusEntity>> = busDataLoc.getAllBuses()
    val availableCount: LiveData<Int> = busDataLoc.getAvailableCount()
    val enRouteCount: LiveData<Int> = busDataLoc.getEnRouteCount()
    val arrivals: LiveData<List<MovementLogEntity>> = movementLogDataLoc.getArrivals()
    val departures: LiveData<List<MovementLogEntity>> = movementLogDataLoc.getDepartures()
    val arrivalsCount: LiveData<Int> = movementLogDataLoc.getArrivalsCount()
    val departuresCount: LiveData<Int> = movementLogDataLoc.getDeparturesCount()
    val totalMovementLogsCount: LiveData<Int> = movementLogDataLoc.getTotalMovementLogsCount()
    val latestArrival: LiveData<MovementLogEntity?> = movementLogDataLoc.getLatestArrival()
    val allTrips: LiveData<List<TripEntity>> = tripDataLoc.getAllTrips()
    val allStaff: LiveData<List<StaffEntity>> = staffDataLoc.getAllStaff()
    val staffCount: LiveData<Int> = staffDataLoc.getStaffCount()

    fun searchBuses(query: String): LiveData<List<BusEntity>> {
        return if (query.isBlank()) {
            busDataLoc.getAllBuses()
        } else {
            busDataLoc.searchBuses(query.trim())
        }
    }

    suspend fun getAllBusesSync(): List<BusEntity> = withContext(Dispatchers.IO) {
        busDataLoc.getAllBusesSync()
    }

    suspend fun getBusByPlate(plateNumber: String): BusEntity? = withContext(Dispatchers.IO) {
        busDataLoc.getBusByPlate(plateNumber.trim())
    }

    fun getBusById(id: Int): LiveData<BusEntity?> = busDataLoc.getBusById(id)

    suspend fun authenticateStaff(staffId: String, password: String): StaffEntity? = withContext(Dispatchers.IO) {
        staffDataLoc.authenticate(staffId.trim(), password.trim())
    }

    suspend fun addStaff(staffId: String, name: String, password: String): Long = withContext(Dispatchers.IO) {
        val staff = StaffEntity(
            staffId = staffId.trim(),
            name = name.trim(),
            password = password.trim(),
            isActive = true
        )
        staffDataLoc.insertStaff(staff)
    }

    suspend fun updateStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        staffDataLoc.updateStaff(staff)
    }

    suspend fun deleteStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        staffDataLoc.deleteStaff(staff)
    }

    suspend fun resetDatabaseToDefaults(database: AppDatabase) = withContext(Dispatchers.IO) {
        AppDatabase.resetToDefaultData(database)
    }

    suspend fun addBus(plateNumber: String, destination: String, busType: String): Long = withContext(Dispatchers.IO) {
        val cleanPlate = plateNumber.trim()
        val cleanDest = destination.trim()
        val cleanType = busType.trim().ifEmpty { "Regular Bus" }
        val route = "Agora Terminal → $cleanDest"
        val now = System.currentTimeMillis()
        val bus = BusEntity(
            plateNumber = cleanPlate,
            destination = cleanDest,
            busType = cleanType,
            routeDescription = route,
            status = "AVAILABLE",
            lastUpdatedAt = now
        )
        val id = busDataLoc.insertBus(bus)
        // Maintain local synchronization: insert corresponding trip
        tripDataLoc.insertTrip(
            TripEntity(
                busPlate = cleanPlate,
                route = route,
                status = "AVAILABLE",
                lastUpdatedAt = now
            )
        )
        // Cloud Sync: push newly created bus to Firebase Firestore
        FirebaseSyncManager.syncBus(bus)
        id
    }

    suspend fun updateBus(bus: BusEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updatedBus = bus.copy(lastUpdatedAt = now)
        busDataLoc.updateBus(updatedBus)
        // Synchronize with trip records
        val trips = tripDataLoc.getAllTripsSync()
        val matchingTrip = trips.firstOrNull { it.busPlate.equals(bus.plateNumber, ignoreCase = true) }
        if (matchingTrip != null) {
            tripDataLoc.updateTrip(
                matchingTrip.copy(
                    status = bus.status,
                    route = bus.routeDescription,
                    lastUpdatedAt = now
                )
            )
        }
        // Cloud Sync: push updated bus to Firebase Firestore
        FirebaseSyncManager.syncBus(updatedBus)
    }

    suspend fun deleteBus(bus: BusEntity) = withContext(Dispatchers.IO) {
        busDataLoc.deleteBus(bus)
        val trips = tripDataLoc.getAllTripsSync()
        val matchingTrips = trips.filter { it.busPlate.equals(bus.plateNumber, ignoreCase = true) }
        for (t in matchingTrips) {
            tripDataLoc.deleteTrip(t)
        }
        // Cloud Sync: remove deleted bus from Firebase Firestore
        FirebaseSyncManager.deleteBus(bus.plateNumber)
    }

    suspend fun getAllTripsSync(): List<TripEntity> = withContext(Dispatchers.IO) {
        tripDataLoc.getAllTripsSync()
    }

    suspend fun addTrip(busPlate: String, route: String, status: String = "AVAILABLE"): Long = withContext(Dispatchers.IO) {
        val cleanPlate = busPlate.trim()
        val cleanRoute = route.trim()
        val cleanStatus = status.trim().ifEmpty { "AVAILABLE" }
        val now = System.currentTimeMillis()
        val trip = TripEntity(
            busPlate = cleanPlate,
            route = cleanRoute,
            status = cleanStatus,
            lastUpdatedAt = now
        )
        val tripId = tripDataLoc.insertTrip(trip)

        // Keep bus synchronized
        val existingBus = busDataLoc.getBusByPlate(cleanPlate)
        if (existingBus != null) {
            val updated = existingBus.copy(
                routeDescription = cleanRoute,
                status = cleanStatus,
                lastUpdatedAt = now
            )
            busDataLoc.updateBus(updated)
            FirebaseSyncManager.syncBus(updated)
        } else {
            val dest = cleanRoute.split("→", "->", "—").lastOrNull()?.trim() ?: "Mindanao"
            val newBus = BusEntity(
                plateNumber = cleanPlate,
                destination = dest,
                busType = "Regular Bus",
                routeDescription = cleanRoute,
                status = cleanStatus,
                lastUpdatedAt = now
            )
            busDataLoc.insertBus(newBus)
            FirebaseSyncManager.syncBus(newBus)
        }
        tripId
    }

    suspend fun updateTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        tripDataLoc.updateTrip(trip.copy(lastUpdatedAt = now))
        // Synchronize with bus records
        val bus = busDataLoc.getBusByPlate(trip.busPlate)
        if (bus != null) {
            val updated = bus.copy(
                status = trip.status,
                routeDescription = trip.route,
                lastUpdatedAt = now
            )
            busDataLoc.updateBus(updated)
            FirebaseSyncManager.syncBus(updated)
        }
    }

    suspend fun deleteTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        tripDataLoc.deleteTrip(trip)
    }

    suspend fun recordMovement(
        movementType: String,
        busPlate: String,
        movementTime: String,
        notes: String?
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanPlate = busPlate.trim()
        val bus = busDataLoc.getBusByPlate(cleanPlate)
        val originOrDest = if (movementType.equals("Arrival", ignoreCase = true)) {
            "From ${bus?.destination ?: "Terminal"}"
        } else {
            "To ${bus?.destination ?: "Terminal"}"
        }

        val log = MovementLogEntity(
            movementType = movementType.trim(),
            busPlate = cleanPlate,
            originOrDestination = originOrDest,
            movementTime = movementTime.trim(),
            notes = notes?.trim()?.ifEmpty { null },
            recordedAt = now
        )
        val logId = movementLogDataLoc.insertLog(log)

        // Automatically update both bus and trip status to eliminate out-of-sync redundancy
        val updatedStatus = if (movementType.equals("Arrival", ignoreCase = true)) {
            "AVAILABLE"
        } else {
            "EN ROUTE"
        }

        if (bus != null) {
            val updatedBus = bus.copy(status = updatedStatus, lastUpdatedAt = now)
            busDataLoc.updateBus(updatedBus)
            FirebaseSyncManager.syncBus(updatedBus)
        }
        val trips = tripDataLoc.getAllTripsSync()
        val matchingTrip = trips.firstOrNull { it.busPlate.equals(cleanPlate, ignoreCase = true) }
        if (matchingTrip != null) {
            tripDataLoc.updateTrip(matchingTrip.copy(status = updatedStatus, lastUpdatedAt = now))
        }

        // Cloud Sync: push movement log to Firebase Firestore
        FirebaseSyncManager.syncMovementLog(log)

        logId
    }
}
