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
    val status: String = "AVAILABLE", // AVAILABLE, EN ROUTE, DEPARTED, UNAVAILABLE
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "movement_logs")
data class MovementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movementType: String, // "Arrival" or "Departure"
    val busPlate: String,
    val originOrDestination: String, // e.g. "Arrived at Valencia" or "From CDO to Valencia"
    val movementTime: String, // e.g. "10:30 AM"
    val terminal: String = "", // actual arrival or departure terminal
    val notes: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val staffId: String,
    val password: String,
    val name: String,
    val isActive: Boolean = true
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val busPlate: String,
    val route: String,
    val status: String = "AVAILABLE", // AVAILABLE, EN ROUTE, COMPLETED, UNAVAILABLE
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
