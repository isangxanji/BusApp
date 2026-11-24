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
import com.pabama.businfo.data.entity.BusEntity
import com.pabama.businfo.databinding.ActivityManageBusBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class ManageBusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBusBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityManageBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup staff bottom navigation
        NavigationHelper.setupStaffBottomNav(this, "BUSES")

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Add New Bus
        binding.btnAddBus.setOnClickListener {
            showAddBusDialog()
        }

        // Observe all buses from Room DB and render dynamically
        viewModel.allBuses.observe(this) { buses ->
            renderBusRecords(buses)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupStaffBottomNav(this, "BUSES")
    }

    private fun renderBusRecords(buses: List<BusEntity>) {
        binding.containerBusRecords.removeAllViews()

        if (buses.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No buses currently recorded."
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 32, 0, 32)
            }
            binding.containerBusRecords.addView(emptyText)
            return
        }

        for (bus in buses) {
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
            StatusBadge.setStatus(statusText, bus.status)

            val btnEdit = TextView(this).apply {
                text = "Edit"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.primary))
                background = getDrawable(R.color.accent_light)
                setPadding(28, 12, 28, 12)
                setOnClickListener {
                    showEditBusDialog(bus)
                }
            }

            bottomRow.addView(statusText)
            bottomRow.addView(btnEdit)

            card.addView(plateText)
            card.addView(detailsText)
            card.addView(bottomRow)

            binding.containerBusRecords.addView(card)
        }
    }

    private fun showAddBusDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val inputPlate = EditText(this).apply {
            hint = "Plate Number (e.g. KBA 1010)"
            setSingleLine(true)
        }
        val inputDestination = EditText(this).apply {
            hint = "Destination (e.g. Valencia)"
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }
        val inputType = EditText(this).apply {
            hint = "Bus Type (e.g. Regular Bus)"
            setText("Regular Bus")
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }

        layout.addView(inputPlate)
        layout.addView(inputDestination)
        layout.addView(inputType)

        AlertDialog.Builder(this)
            .setTitle("Add New Bus")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val plate = inputPlate.text.toString().trim()
                val destination = inputDestination.text.toString().trim()
                val type = inputType.text.toString().trim()

                if (plate.isEmpty() || destination.isEmpty()) {
                    Toast.makeText(this, "Plate number and destination are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addBus(plate, destination, type) { success ->
                    if (success) {
                        Toast.makeText(this, "Bus $plate added successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to add bus.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showEditBusDialog(bus: BusEntity) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        val inputPlate = EditText(this).apply {
            hint = "Plate Number"
            setText(bus.plateNumber)
            setSingleLine(true)
        }
        layout.addView(inputPlate)

        val inputDestination = EditText(this).apply {
            hint = "Destination"
            setText(bus.destination)
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }
        layout.addView(inputDestination)

        val inputType = EditText(this).apply {
            hint = "Bus Type"
            setText(bus.busType)
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }
        layout.addView(inputType)

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
        val statusIdx = statuses.indexOfFirst { it.equals(bus.status, ignoreCase = true) }
        if (statusIdx >= 0) statusSpinner.setSelection(statusIdx)
        layout.addView(statusSpinner)

        AlertDialog.Builder(this)
            .setTitle("Edit Bus ${bus.plateNumber}")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Bus")
                    .setMessage("Are you sure you want to delete bus ${bus.plateNumber}? Any corresponding trip route will also be removed.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteBus(bus) { success ->
                            if (success) {
                                Toast.makeText(this, "Bus ${bus.plateNumber} deleted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to delete bus.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
            .setPositiveButton("Save") { _, _ ->
                val plate = inputPlate.text.toString().trim()
                val destination = inputDestination.text.toString().trim()
                val type = inputType.text.toString().trim()
                val status = statusSpinner.selectedItem.toString()

                if (plate.isEmpty() || destination.isEmpty()) {
                    Toast.makeText(this, "Plate number and destination are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updated = bus.copy(
                    plateNumber = plate,
                    destination = destination,
                    busType = type,
                    status = status
                )
                viewModel.updateBus(updated) { success ->
                    if (success) {
                        Toast.makeText(this, "Bus $plate updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to update bus.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}
