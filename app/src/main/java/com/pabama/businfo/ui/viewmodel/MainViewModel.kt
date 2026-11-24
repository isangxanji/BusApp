package com.pabama.businfo.ui.viewmodel

import androidx.lifecycle.*
import com.pabama.businfo.data.entity.BusEntity
import com.pabama.businfo.data.entity.MovementLogEntity
import com.pabama.businfo.data.entity.StaffEntity
import com.pabama.businfo.data.entity.TripEntity
import com.pabama.businfo.data.repository.BusRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: BusRepository) : ViewModel() {

    val allBuses: LiveData<List<BusEntity>> = repository.allBuses
    val availableCount: LiveData<Int> = repository.availableCount
    val enRouteCount: LiveData<Int> = repository.enRouteCount
    val arrivalsCount: LiveData<Int> = repository.arrivalsCount
    val departuresCount: LiveData<Int> = repository.departuresCount
    val arrivals: LiveData<List<MovementLogEntity>> = repository.arrivals
    val departures: LiveData<List<MovementLogEntity>> = repository.departures
    val latestArrival: LiveData<MovementLogEntity?> = repository.latestArrival
    val allTrips: LiveData<List<TripEntity>> = repository.allTrips
    val allStaff: LiveData<List<StaffEntity>> = repository.allStaff
    val staffCount: LiveData<Int> = repository.staffCount
    val totalMovementLogsCount: LiveData<Int> = repository.totalMovementLogsCount

    private val searchQuery = MutableLiveData<String>("")
    val searchResults: LiveData<List<BusEntity>> = searchQuery.switchMap { query ->
        repository.searchBuses(query)
    }

    private val _currentStaff = MutableLiveData<StaffEntity?>(null)
    val currentStaff: LiveData<StaffEntity?> = _currentStaff

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun login(staffId: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val staff = repository.authenticateStaff(staffId, password)
            if (staff != null) {
                _currentStaff.value = staff
                onResult(true, "Welcome back, ${staff.name}!")
            } else {
                onResult(false, "Invalid Staff ID or password.")
            }
        }
    }

    fun logout() {
        _currentStaff.value = null
    }

    suspend fun getBusByPlate(plate: String): BusEntity? = repository.getBusByPlate(plate)

    suspend fun getAllBusesSync(): List<BusEntity> = repository.getAllBusesSync()

    fun addBus(plate: String, destination: String, type: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addBus(plate, destination, type)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun updateBus(bus: BusEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateBus(bus)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun deleteBus(bus: BusEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteBus(bus)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun addTrip(busPlate: String, route: String, status: String = "AVAILABLE", onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addTrip(busPlate, route, status)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun updateTrip(trip: TripEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateTrip(trip)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun deleteTrip(trip: TripEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteTrip(trip)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun addStaff(staffId: String, name: String, password: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addStaff(staffId, name, password)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun updateStaff(staff: StaffEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateStaff(staff)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun toggleStaffActive(staff: StaffEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateStaff(staff.copy(isActive = !staff.isActive))
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun deleteStaff(staff: StaffEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteStaff(staff)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun resetDatabaseToDefaults(database: com.pabama.businfo.data.AppDatabase, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.resetDatabaseToDefaults(database)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun recordMovement(
        type: String,
        plate: String,
        time: String,
        notes: String?,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.recordMovement(type, plate, time, notes)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }
}

class MainViewModelFactory(private val repository: BusRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
