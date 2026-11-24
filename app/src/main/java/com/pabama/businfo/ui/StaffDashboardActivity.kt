package com.pabama.businfo.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
import com.pabama.businfo.data.SessionManager
import com.pabama.businfo.data.entity.BusEntity
import com.pabama.businfo.databinding.ActivityStaffDashboardBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class StaffDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffDashboardBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStaffDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup staff bottom navigation
        NavigationHelper.setupStaffBottomNav(this, "DASHBOARD")

        // Display personalized welcome text
        val staffName = sessionManager.getStaffName() ?: "Staff"
        binding.txtStaffWelcome.text = "Welcome back, $staffName!"

        // Check if Admin to show Admin Portal button
        if (sessionManager.isAdmin()) {
            binding.btnOpenAdminPanel.visibility = View.VISIBLE
            binding.btnOpenAdminPanel.setOnClickListener {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
        } else {
            binding.btnOpenAdminPanel.visibility = View.GONE
        }

        // Passenger View Button
        binding.btnPassengerView.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Log Out Button
        binding.btnStaffLogout.setOnClickListener {
            sessionManager.clearSession()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Quick Action Buttons
        binding.btnRecordMovement.setOnClickListener {
            startActivity(Intent(this, RecordBusMovementActivity::class.java))
        }

        binding.btnManageBuses.setOnClickListener {
            startActivity(Intent(this, ManageBusActivity::class.java))
        }

        binding.btnManageTrips.setOnClickListener {
            startActivity(Intent(this, ManageTripsActivity::class.java))
        }

        // Observe Dynamic Counts
        viewModel.availableCount.observe(this) { count ->
            binding.txtStaffCountAvailable.text = (count ?: 0).toString()
        }

        viewModel.arrivalsCount.observe(this) { count ->
            binding.txtStaffCountArrivals.text = (count ?: 0).toString()
        }

        viewModel.departuresCount.observe(this) { count ->
            binding.txtStaffCountDepartures.text = (count ?: 0).toString()
        }

        // Observe Current Buses
        viewModel.allBuses.observe(this) { buses ->
            renderCurrentBuses(buses)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupStaffBottomNav(this, "DASHBOARD")
        if (sessionManager.isAdmin()) {
            binding.btnOpenAdminPanel.visibility = View.VISIBLE
        }
    }

    private fun renderCurrentBuses(buses: List<BusEntity>) {
        binding.containerStaffBuses.removeAllViews()

        if (buses.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No active buses recorded. Tap Manage Buses to add one."
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 24, 0, 24)
            }
            binding.containerStaffBuses.addView(emptyText)
            return
        }

        for (bus in buses.take(5)) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 36, 40, 36)
                background = getDrawable(R.drawable.bg_card)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 20
                }
                layoutParams = params
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@StaffDashboardActivity, ManageBusActivity::class.java))
                }
            }

            val plateText = TextView(this).apply {
                text = bus.plateNumber
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val detailsText = TextView(this).apply {
                text = "${bus.destination} • ${bus.busType}"
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
                layoutParams = p
            }

            val statusText = TextView(this).apply {
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(20, 6, 20, 6)
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                layoutParams = p
            }
            StatusBadge.setStatus(statusText, bus.status)

            card.addView(plateText)
            card.addView(detailsText)
            card.addView(statusText)

            binding.containerStaffBuses.addView(card)
        }
    }
}
