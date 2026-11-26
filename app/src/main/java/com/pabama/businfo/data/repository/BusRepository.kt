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
    val completedTripsCount: LiveData<Int> = tripDataLoc.getCompletedTripsCount()
    val totalTripsCount: LiveData<Int> = tripDataLoc.getTotalTripsCount()
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
        val cleanId = staffId.trim()
        val cleanPwd = password.trim()
        val local = staffDataLoc.authenticate(cleanId, cleanPwd)
        if (local != null) return@withContext local

        // Fallback to live Firebase Firestore check if local cache hasn't synced yet
        val remote = FirebaseSyncManager.authenticateRemote(cleanId, cleanPwd)
        if (remote != null) {
            if (staffDataLoc.getStaffById(remote.staffId) == null) {
                staffDataLoc.insertStaff(remote)
            }
        }
        remote
    }

    suspend fun addStaff(staffId: String, name: String, password: String): Long = withContext(Dispatchers.IO) {
        val cleanId = staffId.trim()
        if (staffDataLoc.getStaffById(cleanId) != null) {
            throw IllegalStateException("Staff ID '$cleanId' already exists.")
        }
        val staff = StaffEntity(
            staffId = cleanId,
            name = name.trim().capitalizeWords(),
            password = password.trim(),
            isActive = true,
            lastUpdatedAt = System.currentTimeMillis()
        )
        val id = staffDataLoc.insertStaff(staff)
        FirebaseSyncManager.syncStaff(staff)
        id
    }

    suspend fun updateStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        val updated = staff.copy(lastUpdatedAt = System.currentTimeMillis())
        staffDataLoc.updateStaff(updated)
        FirebaseSyncManager.syncStaff(updated)
    }

    suspend fun deleteStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        staffDataLoc.deleteStaff(staff)
        FirebaseSyncManager.deleteStaff(staff.staffId)
    }

    suspend fun resetDatabaseToDefaults(database: AppDatabase) = withContext(Dispatchers.IO) {
        AppDatabase.resetToDefaultData(database)
    }

    // Bus registration using plate number and bus type only
    suspend fun addBus(plateNumber: String, busType: String): Long = withContext(Dispatchers.IO) {
        addBus(plateNumber, "", busType)
    }

    suspend fun addBus(plateNumber: String, destination: String, busType: String): Long = withContext(Dispatchers.IO) {
        val cleanPlate = plateNumber.trim().uppercase(java.util.Locale.ROOT)
        val cleanDest = destination.trim().capitalizeWords()
        val cleanType = busType.trim().ifEmpty { "Regular Bus" }.capitalizeWords()
        val now = System.currentTimeMillis()

        val existing = busDataLoc.getBusByPlate(cleanPlate)
        if (existing != null) {
            val updated = existing.copy(
                busType = cleanType,
                destination = if (cleanDest.isNotEmpty()) cleanDest else existing.destination.capitalizeWords(),
                lastUpdatedAt = now
            )
            busDataLoc.updateBus(updated)
            FirebaseSyncManager.syncBus(updated)
            return@withContext existing.id.toLong()
        }

        val bus = BusEntity(
            plateNumber = cleanPlate,
            destination = cleanDest,
            busType = cleanType,
            routeDescription = "",
            status = "AVAILABLE",
            lastUpdatedAt = now
        )
        val id = busDataLoc.insertBus(bus)
        FirebaseSyncManager.syncBus(bus)
        id
    }

    suspend fun updateBus(bus: BusEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updatedBus = bus.copy(
            plateNumber = bus.plateNumber.trim().uppercase(java.util.Locale.ROOT),
            destination = bus.destination.trim().capitalizeWords(),
            busType = bus.busType.trim().capitalizeWords(),
            routeDescription = bus.routeDescription.capitalizeWords(),
            lastUpdatedAt = now
        )
        busDataLoc.updateBus(updatedBus)

        // Keep active non-completed trip synchronized with updated bus status/route
        val trips = tripDataLoc.getAllTripsSync()
        val activeTrip = trips.lastOrNull {
            it.busPlate.equals(bus.plateNumber, ignoreCase = true) &&
                !it.status.equals("COMPLETED", ignoreCase = true)
        }
        if (activeTrip != null) {
            val syncedTrip = activeTrip.copy(
                status = bus.status,
                route = bus.routeDescription.ifBlank { activeTrip.route },
                lastUpdatedAt = now
            )
            tripDataLoc.updateTrip(syncedTrip)
            FirebaseSyncManager.syncTrip(syncedTrip)
        }
        FirebaseSyncManager.syncBus(updatedBus)
    }

    suspend fun deleteBus(bus: BusEntity) = withContext(Dispatchers.IO) {
        busDataLoc.deleteBus(bus)
        val trips = tripDataLoc.getAllTripsSync()
        val matchingTrips = trips.filter { it.busPlate.equals(bus.plateNumber, ignoreCase = true) }
        for (t in matchingTrips) {
            tripDataLoc.deleteTrip(t)
            FirebaseSyncManager.deleteTrip(t)
        }
        FirebaseSyncManager.deleteBus(bus.plateNumber)
    }

    suspend fun getAllTripsSync(): List<TripEntity> = withContext(Dispatchers.IO) {
        tripDataLoc.getAllTripsSync()
    }

    // Route assignment through Manage Trips (canonical trip record synced to Firebase & bus)
    suspend fun addTrip(busPlate: String, route: String, status: String = "AVAILABLE"): Long = withContext(Dispatchers.IO) {
        val cleanPlate = busPlate.trim().uppercase(java.util.Locale.ROOT)
        val cleanRoute = route.trim().capitalizeWords()
        val cleanStatus = status.trim().ifEmpty { "AVAILABLE" }
        val now = System.currentTimeMillis()

        val stops = cleanRoute.split("→", "->", "—", "to", "To").map { it.trim().capitalizeWords() }.filter { it.isNotEmpty() }
        val dest = stops.lastOrNull().orEmpty()

        val firebaseId = FirebaseSyncManager.buildTripFirebaseId(cleanPlate, now)
        val trip = TripEntity(
            firebaseId = firebaseId,
            busPlate = cleanPlate,
            route = cleanRoute,
            status = cleanStatus,
            lastUpdatedAt = now
        )
        val tripId = tripDataLoc.insertTrip(trip)
        FirebaseSyncManager.syncTrip(trip)

        // Keep bus synchronized with assigned route and destination without duplicating trips
        val existingBus = busDataLoc.getBusByPlate(cleanPlate)
        if (existingBus != null) {
            val updated = existingBus.copy(
                destination = dest,
                routeDescription = cleanRoute,
                status = cleanStatus,
                lastUpdatedAt = now
            )
            busDataLoc.updateBus(updated)
            FirebaseSyncManager.syncBus(updated)
        } else {
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
        val firebaseId = trip.firebaseId.ifBlank { FirebaseSyncManager.buildTripFirebaseId(trip.busPlate, now) }
        val cleanRoute = trip.route.trim().capitalizeWords()
        val updatedTrip = trip.copy(
            firebaseId = firebaseId,
            busPlate = trip.busPlate.trim().uppercase(java.util.Locale.ROOT),
            route = cleanRoute,
            lastUpdatedAt = now
        )
        tripDataLoc.updateTrip(updatedTrip)
        FirebaseSyncManager.syncTrip(updatedTrip)

        val stops = cleanRoute.split("→", "->", "—", "to", "To").map { it.trim() }.filter { it.isNotEmpty() }
        val dest = stops.lastOrNull().orEmpty()

        val bus = busDataLoc.getBusByPlate(trip.busPlate)
        if (bus != null) {
            val busStatus = if (trip.status.equals("COMPLETED", ignoreCase = true)) "AVAILABLE" else trip.status
            val updated = bus.copy(
                destination = if (dest.isNotEmpty()) dest else bus.destination.capitalizeWords(),
                status = busStatus,
                routeDescription = cleanRoute,
                lastUpdatedAt = now
            )
            busDataLoc.updateBus(updated)
            FirebaseSyncManager.syncBus(updated)
        }
    }

    suspend fun deleteTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        tripDataLoc.deleteTrip(trip)
        FirebaseSyncManager.deleteTrip(trip)
    }

    // Saving actual arrival terminal and counting completed trips only on final destination
    suspend fun recordMovement(
        movementType: String,
        busPlate: String,
        terminal: String,
        movementTime: String,
        notes: String?
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanPlate = busPlate.trim().uppercase(java.util.Locale.ROOT)
        val cleanTerminal = terminal.trim().capitalizeWords()
        val bus = busDataLoc.getBusByPlate(cleanPlate)

        val trips = tripDataLoc.getAllTripsSync()
        val activeTrip = trips.lastOrNull {
            it.busPlate.equals(cleanPlate, ignoreCase = true) &&
                !it.status.equals("COMPLETED", ignoreCase = true)
        } ?: trips.lastOrNull { it.busPlate.equals(cleanPlate, ignoreCase = true) }

        val activeRoute = activeTrip?.route?.takeIf { it.isNotBlank() } ?: bus?.routeDescription.orEmpty()
        val routeStops = activeRoute
            .split("→", "->", "—", "to", "To")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val finalDestination = if (routeStops.size >= 2) routeStops.last() else bus?.destination.orEmpty()

        val isArrival = movementType.equals("Arrival", ignoreCase = true)
        val reachedFinal = isArrival && finalDestination.isNotBlank() &&
            cleanTerminal.equals(finalDestination, ignoreCase = true)

        val targetBusStatus = if (isArrival) "AVAILABLE" else "EN ROUTE"
        val targetTripStatus = when {
            reachedFinal -> "COMPLETED"
            isArrival -> "EN ROUTE" // Intermediate terminal arrival keeps trip active until final destination
            else -> "EN ROUTE"
        }

        val log = MovementLogEntity(
            movementType = movementType.trim(),
            busPlate = cleanPlate,
            terminal = cleanTerminal,
            originOrDestination = cleanTerminal,
            movementTime = movementTime.trim(),
            notes = notes?.trim()?.ifEmpty { null },
            recordedAt = now
        )
        val logId = movementLogDataLoc.insertLog(log)

        if (bus != null) {
            val updatedBus = bus.copy(status = targetBusStatus, lastUpdatedAt = now)
            busDataLoc.updateBus(updatedBus)
            FirebaseSyncManager.syncBus(updatedBus)
        }
        if (activeTrip != null) {
            val firebaseId = activeTrip.firebaseId.ifBlank { FirebaseSyncManager.buildTripFirebaseId(cleanPlate, now) }
            val updatedTrip = activeTrip.copy(firebaseId = firebaseId, status = targetTripStatus, lastUpdatedAt = now)
            tripDataLoc.updateTrip(updatedTrip)
            FirebaseSyncManager.syncTrip(updatedTrip)
        }

        FirebaseSyncManager.syncMovementLog(log)
        logId
    }

    suspend fun recordMovement(
        movementType: String,
        busPlate: String,
        movementTime: String,
        notes: String?
    ): Long = withContext(Dispatchers.IO) {
        recordMovement(movementType, busPlate, "", movementTime, notes)
    }
}
