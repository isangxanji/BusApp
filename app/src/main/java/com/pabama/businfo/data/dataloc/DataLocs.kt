package com.pabama.businfo.data.dataloc

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pabama.businfo.data.entity.*

@Dao
interface BusDataLoc {
    @Query("SELECT * FROM buses ORDER BY id ASC")
    fun getAllBuses(): LiveData<List<BusEntity>>

    @Query("SELECT * FROM buses ORDER BY id ASC")
    fun getAllBusesSync(): List<BusEntity>

    @Query("SELECT * FROM buses WHERE plateNumber LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' OR busType LIKE '%' || :query || '%' ORDER BY id ASC")
    fun searchBuses(query: String): LiveData<List<BusEntity>>

    @Query("SELECT * FROM buses WHERE plateNumber = :plateNumber LIMIT 1")
    fun getBusByPlate(plateNumber: String): BusEntity?

    @Query("SELECT * FROM buses WHERE id = :id LIMIT 1")
    fun getBusById(id: Int): LiveData<BusEntity?>

    @Query("SELECT COUNT(*) FROM buses WHERE UPPER(status) = 'AVAILABLE'")
    fun getAvailableCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM buses WHERE UPPER(status) = 'EN ROUTE'")
    fun getEnRouteCount(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBus(bus: BusEntity): Long

    @Update
    fun updateBus(bus: BusEntity): Int

    @Delete
    fun deleteBus(bus: BusEntity): Int
}

@Dao
interface MovementLogDataLoc {
    @Query("SELECT * FROM movement_logs WHERE UPPER(movementType) = 'ARRIVAL' ORDER BY recordedAt DESC")
    fun getArrivals(): LiveData<List<MovementLogEntity>>

    @Query("SELECT * FROM movement_logs WHERE UPPER(movementType) = 'DEPARTURE' ORDER BY recordedAt DESC")
    fun getDepartures(): LiveData<List<MovementLogEntity>>

    @Query("SELECT * FROM movement_logs ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): LiveData<List<MovementLogEntity>>

    @Query("SELECT COUNT(*) FROM movement_logs")
    fun getTotalMovementLogsCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM movement_logs WHERE UPPER(movementType) = 'ARRIVAL'")
    fun getArrivalsCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM movement_logs WHERE UPPER(movementType) = 'DEPARTURE'")
    fun getDeparturesCount(): LiveData<Int>

    @Query("SELECT * FROM movement_logs WHERE UPPER(movementType) = 'ARRIVAL' ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestArrival(): LiveData<MovementLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLog(log: MovementLogEntity): Long
}

@Dao
interface StaffDataLoc {
    @Query("SELECT * FROM staff WHERE staffId = :staffId AND password = :password AND isActive = 1 LIMIT 1")
    fun authenticate(staffId: String, password: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE staffId = :staffId LIMIT 1")
    fun getStaffById(staffId: String): StaffEntity?

    @Query("SELECT * FROM staff ORDER BY id ASC")
    fun getAllStaff(): LiveData<List<StaffEntity>>

    @Query("SELECT COUNT(*) FROM staff")
    fun getStaffCount(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStaff(staff: StaffEntity): Long

    @Update
    fun updateStaff(staff: StaffEntity): Int

    @Delete
    fun deleteStaff(staff: StaffEntity): Int
}

@Dao
interface TripDataLoc {
    @Query("SELECT * FROM trips ORDER BY id ASC")
    fun getAllTrips(): LiveData<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY id ASC")
    fun getAllTripsSync(): List<TripEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE UPPER(status) = 'COMPLETED'")
    fun getCompletedTripsCount(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrip(trip: TripEntity): Long

    @Update
    fun updateTrip(trip: TripEntity): Int

    @Delete
    fun deleteTrip(trip: TripEntity): Int
}
