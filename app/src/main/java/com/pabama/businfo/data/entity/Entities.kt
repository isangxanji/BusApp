package com.pabama.businfo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buses")
data class BusEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plateNumber: String,
    val destination: String = "",
    val busType: String = "Regular Bus",
    val routeDescription: String = "",
    val status: String = "AVAILABLE", // AVAILABLE, EN ROUTE, COMPLETED, UNAVAILABLE
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "movement_logs")
data class MovementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movementType: String, // "Arrival" or "Departure"
    val busPlate: String,
    val terminal: String = "", // Actual recorded arrival or departure terminal
    val originOrDestination: String,
    val movementTime: String, // e.g. "10:30 AM"
    val notes: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val staffId: String,
    val password: String,
    val name: String,
    val isActive: Boolean = true,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseId: String = "",
    val busPlate: String,
    val route: String,
    val status: String = "AVAILABLE", // AVAILABLE, EN ROUTE, COMPLETED, UNAVAILABLE
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

fun String.capitalizeWords(): String {
    if (isBlank()) return this
    return this.split(" ").joinToString(" ") { word ->
        if (word.isEmpty()) {
            word
        } else if (word.contains("→")) {
            word.split("→").joinToString(" to ") { it.trim().capitalizeWords() }
        } else if (word.contains("->")) {
            word.split("->").joinToString(" to ") { it.trim().capitalizeWords() }
        } else {
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
        }
    }
}
