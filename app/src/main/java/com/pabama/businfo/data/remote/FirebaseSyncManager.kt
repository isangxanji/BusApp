package com.pabama.businfo.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.pabama.businfo.data.AppDatabase
import com.pabama.businfo.data.entity.BusEntity
import com.pabama.businfo.data.entity.MovementLogEntity
import com.pabama.businfo.data.entity.StaffEntity
import com.pabama.businfo.data.entity.TripEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Manages real-time synchronization between the local Room SQLite cache
 * and Google Firebase Cloud Firestore (Spark Free Tier).
 *
 * All data (buses, trips, movement logs, and staff accounts) is sourced from
 * Firebase Firestore and deduplicated by canonical keys to prevent duplicate rows.
 */
object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val COLLECTION_BUSES = "buses"
    private const val COLLECTION_TRIPS = "trips"
    private const val COLLECTION_LOGS = "movement_logs"
    private const val COLLECTION_STAFF = "staff"

    private var firestore: FirebaseFirestore? = null
    private var busListener: ListenerRegistration? = null
    private var tripListener: ListenerRegistration? = null
    private var logListener: ListenerRegistration? = null
    private var staffListener: ListenerRegistration? = null
    private var isInitialized = false

    fun init(context: Context, database: AppDatabase, scope: CoroutineScope) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firestore = FirebaseFirestore.getInstance()
            isInitialized = true
            Log.d(TAG, "Firebase Cloud Firestore initialized successfully.")

            startListening(database, scope)
            ensureBootstrapAdminIfEmpty(database, scope)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped: ${e.message}")
        }
    }

    private fun ensureBootstrapAdminIfEmpty(database: AppDatabase, scope: CoroutineScope) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection(COLLECTION_STAFF).limit(1).get().await()
                if (snapshot.isEmpty && database.staffDataLoc().getAllStaffSync().isEmpty()) {
                    val bootstrapAdmin = StaffEntity(
                        staffId = "ADMIN001",
                        password = "admin123",
                        name = "System Administrator",
                        isActive = true,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                    database.staffDataLoc().insertStaff(bootstrapAdmin)
                    syncStaff(bootstrapAdmin)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Staff bootstrap check skipped: ${e.message}")
            }
        }
    }

    private fun startListening(database: AppDatabase, scope: CoroutineScope) {
        val db = firestore ?: return

        // 1. Real-time Buses synchronization (canonical source for fleet)
        try {
            busListener = db.collection(COLLECTION_BUSES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Bus listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val plateNumber = doc.getString("plateNumber") ?: doc.id
                            val destination = doc.getString("destination") ?: ""
                            val busType = doc.getString("busType") ?: "Regular Bus"
                            val routeDescription = doc.getString("routeDescription") ?: ""
                            val status = doc.getString("status") ?: "AVAILABLE"
                            val lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: System.currentTimeMillis()

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val localBus = database.busDataLoc().getBusByPlate(plateNumber)
                                    if (localBus == null) {
                                        database.busDataLoc().insertBus(
                                            BusEntity(
                                                plateNumber = plateNumber,
                                                destination = destination,
                                                busType = busType,
                                                routeDescription = routeDescription,
                                                status = status,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    } else if (lastUpdatedAt >= localBus.lastUpdatedAt) {
                                        database.busDataLoc().updateBus(
                                            localBus.copy(
                                                destination = destination,
                                                busType = busType,
                                                routeDescription = routeDescription,
                                                status = status,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val localBus = database.busDataLoc().getBusByPlate(plateNumber)
                                    if (localBus != null) {
                                        database.busDataLoc().deleteBus(localBus)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching bus listener: ${e.message}")
        }

        // 2. Real-time Trips synchronization (deduplicated via firebaseId)
        try {
            tripListener = db.collection(COLLECTION_TRIPS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Trip listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val firebaseId = doc.id
                            val busPlate = doc.getString("busPlate") ?: ""
                            val route = doc.getString("route") ?: ""
                            val status = doc.getString("status") ?: "AVAILABLE"
                            val lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: System.currentTimeMillis()

                            if (busPlate.isBlank()) continue

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val existing = database.tripDataLoc().getTripByFirebaseId(firebaseId)
                                    if (existing == null) {
                                        database.tripDataLoc().insertTrip(
                                            TripEntity(
                                                firebaseId = firebaseId,
                                                busPlate = busPlate,
                                                route = route,
                                                status = status,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    } else if (lastUpdatedAt >= existing.lastUpdatedAt) {
                                        database.tripDataLoc().updateTrip(
                                            existing.copy(
                                                busPlate = busPlate,
                                                route = route,
                                                status = status,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = database.tripDataLoc().getTripByFirebaseId(firebaseId)
                                    if (existing != null) {
                                        database.tripDataLoc().deleteTrip(existing)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching trip listener: ${e.message}")
        }

        // 3. Real-time Movement Logs synchronization (deduplicated by busPlate + recordedAt)
        try {
            logListener = db.collection(COLLECTION_LOGS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Movement log listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED || dc.type == DocumentChange.Type.MODIFIED) {
                                val doc = dc.document
                                val movementType = doc.getString("movementType") ?: "Arrival"
                                val busPlate = doc.getString("busPlate") ?: ""
                                val terminal = doc.getString("terminal") ?: ""
                                val originOrDest = doc.getString("originOrDestination") ?: terminal
                                val movementTime = doc.getString("movementTime") ?: ""
                                val notes = doc.getString("notes")?.takeIf { it.isNotBlank() }
                                val recordedAt = doc.getLong("recordedAt") ?: System.currentTimeMillis()

                                if (busPlate.isNotBlank()) {
                                    val existingLog = database.movementLogDataLoc().getLogByPlateAndTime(busPlate, recordedAt)
                                    if (existingLog == null) {
                                        database.movementLogDataLoc().insertLog(
                                            MovementLogEntity(
                                                movementType = movementType,
                                                busPlate = busPlate,
                                                terminal = terminal,
                                                originOrDestination = originOrDest,
                                                movementTime = movementTime,
                                                notes = notes,
                                                recordedAt = recordedAt
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching movement log listener: ${e.message}")
        }

        // 4. Real-time Staff synchronization (deduplicated by staffId)
        try {
            staffListener = db.collection(COLLECTION_STAFF)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Staff listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val staffId = doc.getString("staffId") ?: doc.id
                            val password = doc.getString("password") ?: ""
                            val name = doc.getString("name") ?: ""
                            val isActive = doc.getBoolean("isActive") ?: true
                            val lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: System.currentTimeMillis()

                            if (staffId.isBlank()) continue

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val existing = database.staffDataLoc().getStaffById(staffId)
                                    if (existing == null) {
                                        database.staffDataLoc().insertStaff(
                                            StaffEntity(
                                                staffId = staffId,
                                                password = password,
                                                name = name,
                                                isActive = isActive,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    } else if (lastUpdatedAt >= existing.lastUpdatedAt) {
                                        database.staffDataLoc().updateStaff(
                                            existing.copy(
                                                password = password,
                                                name = name,
                                                isActive = isActive,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = database.staffDataLoc().getStaffById(staffId)
                                    if (existing != null) {
                                        database.staffDataLoc().deleteStaff(existing)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching staff listener: ${e.message}")
        }
    }

    suspend fun authenticateRemote(staffId: String, password: String): StaffEntity? = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext null
        try {
            val docId = sanitizeKey(staffId.uppercase(Locale.ROOT))
            val doc = db.collection(COLLECTION_STAFF).document(docId).get().await()
            if (doc.exists()) {
                val remotePwd = doc.getString("password") ?: ""
                val isActive = doc.getBoolean("isActive") ?: true
                if (isActive && remotePwd == password) {
                    return@withContext StaffEntity(
                        staffId = doc.getString("staffId") ?: staffId,
                        password = remotePwd,
                        name = doc.getString("name") ?: staffId,
                        isActive = true,
                        lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote staff auth check failed: ${e.message}")
        }
        null
    }

    suspend fun syncBus(bus: BusEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = sanitizeKey(bus.plateNumber.uppercase(Locale.ROOT))
            val data = mapOf(
                "plateNumber" to bus.plateNumber,
                "destination" to bus.destination,
                "busType" to bus.busType,
                "routeDescription" to bus.routeDescription,
                "status" to bus.status,
                "lastUpdatedAt" to bus.lastUpdatedAt
            )
            db.collection(COLLECTION_BUSES).document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync bus to Firestore: ${e.message}")
        }
    }

    suspend fun deleteBus(plateNumber: String) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = sanitizeKey(plateNumber.uppercase(Locale.ROOT))
            db.collection(COLLECTION_BUSES).document(docId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete bus from Firestore: ${e.message}")
        }
    }

    suspend fun syncTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = trip.firebaseId.ifBlank { "${sanitizeKey(trip.busPlate.uppercase(Locale.ROOT))}_${trip.lastUpdatedAt}" }
            val data = mapOf(
                "busPlate" to trip.busPlate,
                "route" to trip.route,
                "status" to trip.status,
                "lastUpdatedAt" to trip.lastUpdatedAt
            )
            db.collection(COLLECTION_TRIPS).document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync trip to Firestore: ${e.message}")
        }
    }

    suspend fun deleteTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = trip.firebaseId.ifBlank { return@withContext }
            db.collection(COLLECTION_TRIPS).document(docId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete trip from Firestore: ${e.message}")
        }
    }

    suspend fun syncMovementLog(log: MovementLogEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = "${sanitizeKey(log.busPlate.uppercase(Locale.ROOT))}_${log.recordedAt}"
            val data = mapOf(
                "movementType" to log.movementType,
                "busPlate" to log.busPlate,
                "terminal" to log.terminal,
                "originOrDestination" to log.originOrDestination,
                "movementTime" to log.movementTime,
                "notes" to (log.notes ?: ""),
                "recordedAt" to log.recordedAt
            )
            db.collection(COLLECTION_LOGS).document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync movement log to Firestore: ${e.message}")
        }
    }

    suspend fun syncStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = sanitizeKey(staff.staffId.uppercase(Locale.ROOT))
            val data = mapOf(
                "staffId" to staff.staffId,
                "password" to staff.password,
                "name" to staff.name,
                "isActive" to staff.isActive,
                "lastUpdatedAt" to staff.lastUpdatedAt
            )
            db.collection(COLLECTION_STAFF).document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync staff to Firestore: ${e.message}")
        }
    }

    suspend fun deleteStaff(staffId: String) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = sanitizeKey(staffId.uppercase(Locale.ROOT))
            db.collection(COLLECTION_STAFF).document(docId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete staff from Firestore: ${e.message}")
        }
    }

    fun buildTripFirebaseId(busPlate: String, timestamp: Long): String {
        return "${sanitizeKey(busPlate.uppercase(Locale.ROOT))}_$timestamp"
    }

    private fun sanitizeKey(key: String): String {
        return key.trim().replace("/", "-").replace("\\", "-").replace(" ", "_")
    }

    fun stopListening() {
        busListener?.remove()
        tripListener?.remove()
        logListener?.remove()
        staffListener?.remove()
        busListener = null
        tripListener = null
        logListener = null
        staffListener = null
    }
}
