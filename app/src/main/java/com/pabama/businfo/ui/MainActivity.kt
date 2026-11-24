package com.pabama.businfo.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.data.SessionManager
import com.pabama.businfo.databinding.ActivityMainBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    private lateinit var sessionManager: SessionManager
    private var featuredBusPlate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)

        // If staff is logged in and not explicitly viewing passenger mode, route directly to Staff / Admin dashboard
        val isViewingPassengerExplicitly = intent.getBooleanExtra("VIEW_PASSENGER", false)
        if (sessionManager.isLoggedIn() && !isViewingPassengerExplicitly) {
            if (sessionManager.isAdmin()) {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else {
                startActivity(Intent(this, StaffDashboardActivity::class.java))
            }
            finish()
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

        // Staff Dashboard button
        if (sessionManager.isLoggedIn()) {
            binding.btnOpenStaffDashboard.text = if (sessionManager.isAdmin()) {
                "Return to Admin Portal →"
            } else {
                "Return to Staff Dashboard →"
            }
            binding.btnOpenStaffDashboard.setOnClickListener {
                if (sessionManager.isAdmin()) {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, StaffDashboardActivity::class.java))
                }
            }
        } else {
            binding.btnOpenStaffDashboard.setOnClickListener {
                startActivity(Intent(this, StaffLoginActivity::class.java))
            }
        }

        // See All Buses button
        binding.btnSeeAllBuses.setOnClickListener {
            startActivity(Intent(this, BusInformationActivity::class.java))
        }

        // Featured Bus Card click
        binding.cardBus.setOnClickListener {
            if (featuredBusPlate.isNotBlank()) {
                val intent = Intent(this, TripDetailActivity::class.java).apply {
                    putExtra("BUS_PLATE", featuredBusPlate)
                }
                startActivity(intent)
            } else {
                android.widget.Toast.makeText(this, "No active bus available to view details.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // See All Updates button & Card click
        binding.btnSeeAllUpdates.setOnClickListener {
            startActivity(Intent(this, TripUpdatesActivity::class.java))
        }
        binding.cardRecentUpdate.setOnClickListener {
            startActivity(Intent(this, TripUpdatesActivity::class.java))
        }

        // Observe Dynamic Metric Counts from Room DB
        viewModel.availableCount.observe(this) { count ->
            binding.txtCountAvailable.text = (count ?: 0).toString()
        }

        viewModel.arrivalsCount.observe(this) { count ->
            binding.txtCountArrivals.text = (count ?: 0).toString()
        }

        viewModel.departuresCount.observe(this) { count ->
            binding.txtCountDepartures.text = (count ?: 0).toString()
        }

        // Observe Available Buses to feature the primary available bus
        viewModel.allBuses.observe(this) { buses ->
            if (!buses.isNullOrEmpty()) {
                val firstAvailable = buses.firstOrNull { it.status.equals("AVAILABLE", ignoreCase = true) }
                    ?: buses.first()
                featuredBusPlate = firstAvailable.plateNumber
                binding.txtMainBusPlate.text = firstAvailable.plateNumber
                binding.txtMainBusDetails.text = "${firstAvailable.destination}  •  ${firstAvailable.busType}"
                StatusBadge.setStatus(binding.txtMainBusStatus, firstAvailable.status)
            } else {
                featuredBusPlate = ""
                binding.txtMainBusPlate.text = "No buses available"
                binding.txtMainBusDetails.text = "No active fleet records in terminal"
                StatusBadge.setStatus(binding.txtMainBusStatus, "UNAVAILABLE")
            }
        }

        // Observe Recent Update for display
        viewModel.latestArrival.observe(this) { latest ->
            if (latest != null) {
                binding.txtRecentUpdateTitle.text = "Bus ${latest.busPlate} ${latest.movementType.lowercase()}"
                binding.txtRecentUpdateDesc.text = "${latest.originOrDestination} at ${latest.movementTime}."
            } else {
                binding.txtRecentUpdateTitle.text = "No recent updates"
                binding.txtRecentUpdateDesc.text = "Recorded terminal movements will appear here."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupPassengerBottomNav(this, "HOME")
        if (sessionManager.isLoggedIn()) {
            binding.btnOpenStaffDashboard.text = if (sessionManager.isAdmin()) {
                "Return to Admin Portal →"
            } else {
                "Return to Staff Dashboard →"
            }
        }
    }
}
