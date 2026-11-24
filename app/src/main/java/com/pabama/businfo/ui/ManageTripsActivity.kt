package com.pabama.businfo.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
import com.pabama.businfo.data.entity.TripEntity
import com.pabama.businfo.databinding.ActivityManageTripsBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class ManageTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTripsBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityManageTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup staff bottom navigation
        NavigationHelper.setupStaffBottomNav(this, "TRIPS")

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Add Trip button
        binding.btnAddTrip.setOnClickListener {
            showAddTripDialog()
        }

        // Observe trips from Room DB and render dynamically
        viewModel.allTrips.observe(this) { trips ->
            renderTripRecords(trips)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupStaffBottomNav(this, "TRIPS")
    }

    private fun renderTripRecords(trips: List<TripEntity>) {
        binding.containerTripRecords.removeAllViews()

        if (trips.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No trip routes currently recorded."
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 32, 0, 32)
            }
            binding.containerTripRecords.addView(emptyText)
            return
        }

        for (trip in trips) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 32, 40, 32)
                background = getDrawable(R.drawable.bg_card)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 20
                }
                layoutParams = params
            }

            val busText = TextView(this).apply {
                text = trip.busPlate
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val routeText = TextView(this).apply {
                text = trip.route
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
                layoutParams = p
            }

            // Bottom row: Status badge on left, Edit button on right
            val bottomRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                layoutParams = p
            }

            val statusText = TextView(this).apply {
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(20, 6, 20, 6)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = p
            }
            StatusBadge.setStatus(statusText, trip.status)

            val btnEdit = TextView(this).apply {
                text = "Edit"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.primary))
                background = getDrawable(R.color.accent_light)
                setPadding(28, 12, 28, 12)
                setOnClickListener {
                    showEditTripDialog(trip)
                }
            }

            bottomRow.addView(statusText)
            bottomRow.addView(btnEdit)

            card.addView(busText)
            card.addView(routeText)
            card.addView(bottomRow)

            binding.containerTripRecords.addView(card)
        }
    }

    private fun showAddTripDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val inputBusPlate = EditText(this).apply {
            hint = "Bus Plate (e.g. KBA 1008)"
            setSingleLine(true)
        }
        val inputRoute = EditText(this).apply {
            hint = "Route (e.g. Agora Terminal → Valencia)"
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }

        layout.addView(inputBusPlate)
        layout.addView(inputRoute)

        AlertDialog.Builder(this)
            .setTitle("Add New Trip Route")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val plate = inputBusPlate.text.toString().trim()
                val route = inputRoute.text.toString().trim()

                if (plate.isEmpty() || route.isEmpty()) {
                    Toast.makeText(this, "Bus plate and route description are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addTrip(plate, route, "AVAILABLE") { success ->
                    if (success) {
                        Toast.makeText(this, "Trip for $plate added successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to add trip route.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showEditTripDialog(trip: TripEntity) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        val inputBus = EditText(this).apply {
            hint = "Bus Plate"
            setText(trip.busPlate)
            setSingleLine(true)
        }
        layout.addView(inputBus)

        val inputRoute = EditText(this).apply {
            hint = "Route Description"
            setText(trip.route)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }
        layout.addView(inputRoute)

        val statusSpinner = Spinner(this).apply {
            background = getDrawable(R.drawable.bg_spinner)
            setPadding(24, 16, 24, 16)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
            layoutParams = p
        }
        val statuses = arrayOf("AVAILABLE", "EN ROUTE", "DEPARTED", "UNAVAILABLE")
        val adapter = ArrayAdapter(this, R.layout.item_spinner_selected, statuses).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        statusSpinner.adapter = adapter
        val statusIdx = statuses.indexOfFirst { it.equals(trip.status, ignoreCase = true) }
        if (statusIdx >= 0) statusSpinner.setSelection(statusIdx)
        layout.addView(statusSpinner)

        AlertDialog.Builder(this)
            .setTitle("Edit Trip ${trip.busPlate}")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Trip")
                    .setMessage("Are you sure you want to delete the trip route for ${trip.busPlate}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteTrip(trip) { success ->
                            if (success) {
                                Toast.makeText(this, "Trip route deleted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to delete trip.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
            .setPositiveButton("Save") { _, _ ->
                val bus = inputBus.text.toString().trim()
                val route = inputRoute.text.toString().trim()
                val status = statusSpinner.selectedItem.toString()

                if (bus.isEmpty() || route.isEmpty()) {
                    Toast.makeText(this, "Bus plate and route are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updated = trip.copy(
                    busPlate = bus,
                    route = route,
                    status = status
                )
                viewModel.updateTrip(updated) { success ->
                    if (success) {
                        Toast.makeText(this, "Trip route updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to update trip.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}
