
package com.example.busapp

import android.os.Bundle
import android.content.Intent
import android.widget.TextView
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AlertDialog
import android.widget.EditText

class ManageBusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_bus)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Back to Staff Dashboard
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Add New Bus
        findViewById<View>(R.id.btnAddBus).setOnClickListener {
            showBusDialog()
        }

        // Edit existing buses
        findViewById<View>(R.id.btnEditBus1).setOnClickListener {
            showEditBusDialog(1)
        }

        findViewById<View>(R.id.btnEditBus2).setOnClickListener {
            showEditBusDialog(2)
        }

        findViewById<View>(R.id.btnEditBus3).setOnClickListener {
            showEditBusDialog(3)
        }

        findViewById<View>(R.id.btnEditBus4).setOnClickListener {
            showEditBusDialog(4)
        }

        findViewById<View>(R.id.btnEditBus5).setOnClickListener {
            showEditBusDialog(5)
        }

        // Staff Bottom Navigation
        val btnDashboard = findViewById<TextView>(R.id.btnStaffDashboard)
        val btnRecord = findViewById<TextView>(R.id.btnStaffRecord)
        val btnBuses = findViewById<TextView>(R.id.btnStaffBuses)
        val btnTrips = findViewById<TextView>(R.id.btnStaffTrips)

        // Highlight Buses as the active screen
        btnBuses.setTextColor(getColor(R.color.accent))

        btnDashboard.setOnClickListener {
            startActivity(Intent(this, StaffDashboardActivity::class.java))
        }

        btnRecord.setOnClickListener {
            startActivity(Intent(this, RecordBusMovementActivity::class.java))
        }

        btnBuses.setOnClickListener {
            // Already on Manage Buses
        }

        btnTrips.setOnClickListener {
            startActivity(Intent(this, ManageTripsActivity::class.java))
        }
    }


    private fun showBusDialog() {
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 0)

        val inputPlate = EditText(this)
        inputPlate.hint = "Plate Number"
        layout.addView(inputPlate)

        val inputDestination = EditText(this)
        inputDestination.hint = "Destination"
        layout.addView(inputDestination)

        val inputType = EditText(this)
        inputType.hint = "Bus Type (e.g. Regular Bus)"
        layout.addView(inputType)

        AlertDialog.Builder(this)
            .setTitle("Add New Bus")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->

                val plate = inputPlate.text.toString().trim()
                val destination = inputDestination.text.toString().trim()
                val type = inputType.text.toString().trim()

                if (plate.isEmpty() || destination.isEmpty() || type.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Please complete all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    addBusCard(plate, destination, type)

                    Toast.makeText(
                        this,
                        "Bus $plate added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun showEditBusDialog(busNumber: Int) {

        val plateId = when (busNumber) {
            1 -> R.id.txtBus1Plate
            2 -> R.id.txtBus2Plate
            3 -> R.id.txtBus3Plate
            4 -> R.id.txtBus4Plate
            else -> R.id.txtBus5Plate
        }

        val detailsId = when (busNumber) {
            1 -> R.id.txtBus1Details
            2 -> R.id.txtBus2Details
            3 -> R.id.txtBus3Details
            4 -> R.id.txtBus4Details
            else -> R.id.txtBus5Details
        }

        val statusId = when (busNumber) {
            1 -> R.id.txtBus1Status
            2 -> R.id.txtBus2Status
            3 -> R.id.txtBus3Status
            4 -> R.id.txtBus4Status
            else -> R.id.txtBus5Status
        }

        val plateView = findViewById<android.widget.TextView>(plateId)
        val detailsView = findViewById<android.widget.TextView>(detailsId)
        val statusView = findViewById<android.widget.TextView>(statusId)

        val currentDetails = detailsView.text.toString().split(" • ")

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 0)

        val inputPlate = EditText(this)
        inputPlate.hint = "Plate Number"
        inputPlate.setText(plateView.text)
        layout.addView(inputPlate)

        val inputDestination = EditText(this)
        inputDestination.hint = "Destination"
        inputDestination.setText(currentDetails.getOrElse(0) { "" })
        layout.addView(inputDestination)

        val inputType = EditText(this)
        inputType.hint = "Bus Type"
        inputType.setText(currentDetails.getOrElse(1) { "" })
        layout.addView(inputType)

        val statusSpinner = android.widget.Spinner(this)

        val statuses = arrayOf(
            "AVAILABLE",
            "EN ROUTE",
            "DEPARTED",
            "UNAVAILABLE"
        )

        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statuses
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        statusSpinner.adapter = adapter

        val currentStatus = statusView.text.toString()
        val statusPosition = statuses.indexOf(currentStatus)

        if (statusPosition >= 0) {
            statusSpinner.setSelection(statusPosition)
        }

        layout.addView(statusSpinner)

        AlertDialog.Builder(this)
            .setTitle("Edit Bus $busNumber")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->

                val plate = inputPlate.text.toString().trim()
                val destination = inputDestination.text.toString().trim()
                val type = inputType.text.toString().trim()

                if (plate.isEmpty() || destination.isEmpty() || type.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Please complete all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    plateView.text = plate
                    detailsView.text = "$destination • $type"
                    statusView.text = statusSpinner.selectedItem.toString()

                    Toast.makeText(
                        this,
                        "Bus information updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }


    private fun addBusCard(plate: String, destination: String, type: String) {

        val container = findViewById<android.widget.LinearLayout>(
            R.id.busListContainer
        )

        // Create the bus card
        val card = android.widget.LinearLayout(this)
        card.orientation = android.widget.LinearLayout.VERTICAL
        card.setPadding(40, 32, 40, 32)
        card.background = getDrawable(R.drawable.bg_card)

        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.topMargin = 24
        card.layoutParams = params

        // Plate number
        val plateText = android.widget.TextView(this)
        plateText.text = plate
        plateText.textSize = 18f
        plateText.setTextColor(getColor(R.color.text_primary))
        plateText.setTypeface(null, android.graphics.Typeface.BOLD)

        // Destination and bus type
        val detailsText = android.widget.TextView(this)
        detailsText.text = "$destination • $type"
        detailsText.textSize = 14f
        detailsText.setTextColor(getColor(R.color.text_secondary))

        // Default status
        val statusText = android.widget.TextView(this)
        statusText.text = "AVAILABLE"
        statusText.textSize = 12f
        statusText.setTextColor(getColor(R.color.status_available))
        statusText.setTypeface(null, android.graphics.Typeface.BOLD)

        card.addView(plateText)
        card.addView(detailsText)
        card.addView(statusText)

        // Display the new card
        container.addView(card)
    }


}
