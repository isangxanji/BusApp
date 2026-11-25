package com.pabama.businfo.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.PopupMenu
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.data.SessionManager
import com.pabama.businfo.databinding.ActivityMainBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory
import android.view.View
import com.pabama.businfo.data.entity.BusEntity
import com.pabama.businfo.data.entity.MovementLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    private lateinit var sessionManager: SessionManager
    private var featuredBusPlate: String = ""
    private var recentBusPlate: String = ""
    private var availableBuses: List<BusEntity> = emptyList()
    private var allArrivalLogs: List<MovementLogEntity> = emptyList()
    private fun openInstantly(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)

        // If staff is logged in and not explicitly viewing passenger mode, route directly to Staff / Admin dashboard
        val isViewingPassengerExplicitly = intent.getBooleanExtra("VIEW_PASSENGER", false)
        if (sessionManager.isLoggedIn() && !isViewingPassengerExplicitly) {
            if (sessionManager.isAdmin()) {
                openInstantly(Intent(this, AdminDashboardActivity::class.java))
            } else {
                openInstantly(Intent(this, StaffDashboardActivity::class.java))
            }
            finish()
            overridePendingTransition(0, 0)
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup passenger bottom navigation
        NavigationHelper.setupPassengerBottomNav(this, "HOME")

        // Staff and Admin dropdown menu
        binding.btnOpenStaffDashboard.setOnClickListener { view ->

            val popupMenu = PopupMenu(this, view)

            if (sessionManager.isLoggedIn()) {

                if (sessionManager.isAdmin()) {
                    popupMenu.menu.add("Return to Admin Dashboard")
                } else {
                    popupMenu.menu.add("Return to Staff Dashboard")
                }

            } else {
                popupMenu.menu.add("Staff Login")
                popupMenu.menu.add("Admin Login")
            }

            popupMenu.setOnMenuItemClickListener { item ->

                when (item.title.toString()) {

                    "Staff Login" -> {
                        openInstantly(
                            Intent(this, StaffLoginActivity::class.java)
                        )
                        true
                    }

                    "Admin Login" -> {
                        openInstantly(
                            Intent(this, StaffLoginActivity::class.java)
                        )
                        true
                    }

                    "Return to Staff Dashboard" -> {
                        openInstantly(
                            Intent(this, StaffDashboardActivity::class.java)
                        )
                        true
                    }

                    "Return to Admin Dashboard" -> {
                        openInstantly(
                            Intent(this, AdminDashboardActivity::class.java)
                        )
                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()
        }
        // See All Buses button
        binding.btnSeeAllBuses.setOnClickListener {
            openInstantly(Intent(this, BusInformationActivity::class.java))
        }

        // Featured Bus Card click
        binding.cardBus.setOnClickListener {
            if (featuredBusPlate.isNotBlank()) {
                val intent = Intent(this, TripDetailActivity::class.java).apply {
                    putExtra("BUS_PLATE", featuredBusPlate)
                }
                openInstantly(intent)
            } else {
                android.widget.Toast.makeText(this, "No active bus available to view details.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // See All Updates button & Card click
        binding.btnSeeAllUpdates.setOnClickListener {
            openInstantly(Intent(this, TripUpdatesActivity::class.java))
        }
        binding.cardRecentUpdate.setOnClickListener {

            if (recentBusPlate.isNotBlank()) {

                val intent = Intent(
                    this, TripDetailActivity::class.java
                ).apply {
                    putExtra("BUS_PLATE", recentBusPlate)
                }

                openInstantly(intent)
            }
        }

        // Observe Dynamic Metric Counts from Room DB
        viewModel.availableCount.observe(this) { count ->
            binding.txtCountAvailable.text = (count ?: 0).toString()
        }

        viewModel.arrivalsCount.observe(this) { count ->
            binding.txtCountArrivals.text = (count ?: 0).toString()
        }

        // Observe Available Buses
        viewModel.allBuses.observe(this) { buses ->

            availableBuses = buses.orEmpty()
            updateRecentArrival()

            val firstAvailable = buses?.firstOrNull {
                it.status.equals("AVAILABLE", ignoreCase = true)
            }

            if (firstAvailable != null) {

                featuredBusPlate = firstAvailable.plateNumber

                binding.txtMainBusPlate.text =
                    firstAvailable.plateNumber

                binding.txtMainBusDetails.text =
                    "${firstAvailable.destination} • ${firstAvailable.busType}"

                val stops = firstAvailable.routeDescription
                    .split("→", "->", "—")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (stops.size >= 2 &&
                    !stops.first().equals("Terminal", ignoreCase = true) &&
                    !stops.last().equals("Destination", ignoreCase = true)
                ) {
                    binding.txtMainBusRoute.text =
                        "Route: ${stops.first()} → ${stops.last()}"

                    binding.txtMainBusRoute.visibility = View.VISIBLE
                } else {
                    binding.txtMainBusRoute.visibility = View.GONE
                }

                StatusBadge.setStatus(
                    binding.txtMainBusStatus,
                    firstAvailable.status
                )

                binding.txtMainBusViewDetails.visibility = View.VISIBLE
                binding.cardBus.isClickable = true

            } else {

                featuredBusPlate = ""

                binding.txtMainBusPlate.text = "No buses available"

                binding.txtMainBusDetails.text =
                    "Check again for available buses."

                binding.txtMainBusRoute.visibility = View.GONE
                binding.txtMainBusViewDetails.visibility = View.GONE

                StatusBadge.setStatus(
                    binding.txtMainBusStatus,
                    "UNAVAILABLE"
                )

                binding.cardBus.isClickable = false
            }
        }

        /// Observe today's arrival records
        viewModel.arrivals.observe(this) { arrivals ->
            allArrivalLogs = arrivals.orEmpty()
            updateRecentArrival()
        }
    }

    private fun updateRecentArrival() {

        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd", Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }

        val today = dateFormat.format(Date())

        val timeFormat = SimpleDateFormat(
            "hh:mm a", Locale.US
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }

        val latest = allArrivalLogs
            .filter {
                it.recordedAt > 0L &&
                        dateFormat.format(Date(it.recordedAt)) == today
            }
            .sortedWith(
                compareByDescending<MovementLogEntity> { log ->
                    try {
                        timeFormat.parse(
                            log.movementTime.trim()
                        )?.time ?: Long.MIN_VALUE
                    } catch (e: Exception) {
                        Long.MIN_VALUE
                    }
                }.thenByDescending { it.recordedAt }
            )
            .firstOrNull()

        if (latest != null) {

            recentBusPlate = latest.busPlate

            val bus = availableBuses.firstOrNull {
                it.plateNumber.trim().equals(
                    latest.busPlate.trim(),
                    ignoreCase = true
                )
            }

            val destination = bus?.destination
                ?.takeIf { it.isNotBlank() }
                ?: "Not specified"

            binding.txtRecentUpdateTitle.text =
                latest.busPlate

            binding.txtRecentUpdateStatus.text = "ARRIVED"

            binding.txtRecentUpdateDesc.text =
                latest.originOrDestination

            binding.txtRecentUpdateDestination.text =
                "To $destination"

            binding.txtRecentUpdateTime.text =
                latest.movementTime

            binding.txtRecentUpdateStatus.visibility = View.VISIBLE
            binding.txtRecentUpdateDestination.visibility = View.VISIBLE
            binding.txtRecentUpdateTime.visibility = View.VISIBLE

            binding.cardRecentUpdate.isClickable = true

        } else {

            recentBusPlate = ""

            binding.txtRecentUpdateTitle.text =
                "No recent updates"

            binding.txtRecentUpdateDesc.text =
                "No bus arrivals recorded today."

            binding.txtRecentUpdateStatus.visibility = View.GONE
            binding.txtRecentUpdateDestination.visibility = View.GONE
            binding.txtRecentUpdateTime.visibility = View.GONE

            binding.cardRecentUpdate.isClickable = false
        }
    }

    override fun onResume() {
        super.onResume()

        if (::binding.isInitialized) {
            NavigationHelper.setupPassengerBottomNav(this, "HOME")
            updateRecentArrival()
        }
    }
}
