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
import com.pabama.businfo.data.entity.TripEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages real-time synchronization between the local Room SQLite database
 * and Google Firebase Cloud Firestore (Spark Free Tier).
 *
 * Designed to provide:
 * 1. Offline-first resilience: local Room SQLite remains the single source of UI truth.
 * 2. Instant device-to-device synchronization: changes on one phone push to Firestore
 *    and reflect immediately on all connected passenger and staff devices.
 * 3. Zero data redundancy: uses canonical bus documents and avoids duplicate route data.
 */
object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val COLLECTION_BUSES = "buses"
    private const val COLLECTION_LOGS = "movement_logs"

    private var firestore: FirebaseFirestore? = null
    private var busListener: ListenerRegistration? = null
    private var logListener: ListenerRegistration? = null
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

            // Start listening for real-time cloud updates from other devices
            startListening(database, scope)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped or operating in local-only mode: ${e.message}")
        }
    }

    private fun startListening(database: AppDatabase, scope: CoroutineScope) {
        val db = firestore ?: return

        // 1. Real-time fleet synchronization (Buses & corresponding Trips)
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
                            val routeDescription = doc.getString("routeDescription") ?: "Agora Terminal → $destination"
                            val status = doc.getString("status") ?: "AVAILABLE"
                            val lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: System.currentTimeMillis()

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val localBus = database.busDataLoc().getBusByPlate(plateNumber)
                                    if (localBus == null) {
                                        // Insert new bus synchronized from another device
                                        val newBus = BusEntity(
                                            plateNumber = plateNumber,
                                            destination = destination,
                                            busType = busType,
                                            routeDescription = routeDescription,
                                            status = status,
                                            lastUpdatedAt = lastUpdatedAt
                                        )
                                        database.busDataLoc().insertBus(newBus)
                                        database.tripDataLoc().insertTrip(
                                            TripEntity(
                                                busPlate = plateNumber,
                                                route = routeDescription,
                                                status = status,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                    } else if (lastUpdatedAt >= localBus.lastUpdatedAt) {
                                        // Update local bus only if remote version is newer or equal
                                        database.busDataLoc().updateBus(
                                            localBus.copy(
                                                destination = destination,
                                                busType = busType,
                                                routeDescription = routeDescription,
                                                status = status,
                                                lastUpdatedAt = lastUpdatedAt
                                            )
                                        )
                                        val trips = database.tripDataLoc().getAllTripsSync()
                                        val matching = trips.firstOrNull { it.busPlate.equals(plateNumber, ignoreCase = true) }
                                        if (matching != null) {
                                            database.tripDataLoc().updateTrip(
                                                matching.copy(
                                                    route = routeDescription,
                                                    status = status,
                                                    lastUpdatedAt = lastUpdatedAt
                                                )
                                            )
                                        }
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val localBus = database.busDataLoc().getBusByPlate(plateNumber)
                                    if (localBus != null) {
                                        database.busDataLoc().deleteBus(localBus)
                                        val trips = database.tripDataLoc().getAllTripsSync()
                                        for (t in trips.filter { it.busPlate.equals(plateNumber, ignoreCase = true) }) {
                                            database.tripDataLoc().deleteTrip(t)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching bus snapshot listener: ${e.message}")
        }

        // 2. Real-time movement logs synchronization (Arrivals & Departures)
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
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val doc = dc.document
                                val movementType = doc.getString("movementType") ?: "Arrival"
                                val busPlate = doc.getString("busPlate") ?: ""
                                val originOrDest = doc.getString("originOrDestination") ?: ""
                                val movementTime = doc.getString("movementTime") ?: ""
                                val terminal = doc.getString("terminal") ?: ""
                                val notes = doc.getString("notes")
                                val recordedAt = doc.getLong("recordedAt") ?: System.currentTimeMillis()

                                if (busPlate.isNotBlank()) {
                                    val newLog = MovementLogEntity(
                                        movementType = movementType,
                                        busPlate = busPlate,
                                        originOrDestination = originOrDest,
                                        movementTime = movementTime,
                                        terminal = terminal,
                                        notes = notes,
                                        recordedAt = recordedAt
                                    )
                                    database.movementLogDataLoc().insertLog(newLog)

                                    // Keep bus & trip status synchronized
                                    val targetStatus = if (movementType.equals("Arrival", ignoreCase = true)) "AVAILABLE" else "EN ROUTE"
                                    val localBus = database.busDataLoc().getBusByPlate(busPlate)
                                    if (localBus != null && localBus.lastUpdatedAt <= recordedAt) {
                                        database.busDataLoc().updateBus(localBus.copy(status = targetStatus, lastUpdatedAt = recordedAt))
                                        val trips = database.tripDataLoc().getAllTripsSync()
                                        val matching = trips.firstOrNull { it.busPlate.equals(busPlate, ignoreCase = true) }
                                        if (matching != null) {
                                            database.tripDataLoc().updateTrip(matching.copy(status = targetStatus, lastUpdatedAt = recordedAt))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching movement log listener: ${e.message}")
        }
    }

    suspend fun syncBus(bus: BusEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = sanitizeKey(bus.plateNumber)
            val data = mapOf(
                "plateNumber" to bus.plateNumber,
                "destination" to bus.destination,
                "busType" to bus.busType,
                "routeDescription" to bus.routeDescription,
                "status" to bus.status,
                "lastUpdatedAt" to bus.lastUpdatedAt
            )
            db.collection(COLLECTION_BUSES).document(docId).set(data, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced bus ${bus.plateNumber} to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync bus to Firestore: ${e.message}")
        }
    }

    suspend fun deleteBus(plateNumber: String) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = sanitizeKey(plateNumber)
            db.collection(COLLECTION_BUSES).document(docId).delete().await()
            Log.d(TAG, "Successfully deleted bus $plateNumber from Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete bus from Firestore: ${e.message}")
        }
    }

    suspend fun syncMovementLog(log: MovementLogEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docId = "${sanitizeKey(log.busPlate)}_${log.recordedAt}"
            val data = mapOf(
                "movementType" to log.movementType,
                "busPlate" to log.busPlate,
                "originOrDestination" to log.originOrDestination,
                "movementTime" to log.movementTime,
                "terminal" to log.terminal,
                "notes" to (log.notes ?: ""),
                "recordedAt" to log.recordedAt
            )
            db.collection(COLLECTION_LOGS).document(docId).set(data, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced movement log for ${log.busPlate} to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync movement log to Firestore: ${e.message}")
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.trim().replace("/", "-").replace("\\", "-").replace(" ", "_")
    }

    fun stopListening() {
        busListener?.remove()
        logListener?.remove()
        busListener = null
        logListener = null
    }
}
