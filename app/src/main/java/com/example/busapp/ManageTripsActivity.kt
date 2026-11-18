
package com.example.busapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManageTripsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_trips)

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

        // Edit trip records
        findViewById<View>(R.id.btnEditTrip1).setOnClickListener {
            showEditTripDialog(1)
        }

        findViewById<View>(R.id.btnEditTrip2).setOnClickListener {
            showEditTripDialog(2)
        }

        findViewById<View>(R.id.btnEditTrip3).setOnClickListener {
            showEditTripDialog(3)
        }

        findViewById<View>(R.id.btnEditTrip4).setOnClickListener {
            showEditTripDialog(4)
        }
    }

    private fun showEditTripDialog(tripNumber: Int) {

        val busId = when (tripNumber) {
            1 -> R.id.txtTrip1Bus
            2 -> R.id.txtTrip2Bus
            3 -> R.id.txtTrip3Bus
            else -> R.id.txtTrip4Bus
        }

        val detailsId = when (tripNumber) {
            1 -> R.id.txtTrip1Details
            2 -> R.id.txtTrip2Details
            3 -> R.id.txtTrip3Details
            else -> R.id.txtTrip4Details
        }

        val statusId = when (tripNumber) {
            1 -> R.id.txtTrip1Status
            2 -> R.id.txtTrip2Status
            3 -> R.id.txtTrip3Status
            else -> R.id.txtTrip4Status
        }

        val busView = findViewById<TextView>(busId)
        val detailsView = findViewById<TextView>(detailsId)
        val statusView = findViewById<TextView>(statusId)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 0)

        val inputBus = EditText(this)
        inputBus.hint = "Bus Plate Number"
        inputBus.setText(busView.text)
        layout.addView(inputBus)

        val inputRoute = EditText(this)
        inputRoute.hint = "Route"
        inputRoute.setText(detailsView.text)
        layout.addView(inputRoute)

        val statusSpinner = Spinner(this)

        val statuses = arrayOf(
            "AVAILABLE",
            "EN ROUTE",
            "DEPARTED",
            "UNAVAILABLE"
        )

        val adapter = ArrayAdapter(
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
            .setTitle("Edit Trip $tripNumber")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->

                val bus = inputBus.text.toString().trim()
                val route = inputRoute.text.toString().trim()

                if (bus.isEmpty() || route.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Please complete all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    busView.text = bus
                    detailsView.text = route
                    statusView.text = statusSpinner.selectedItem.toString()

                    Toast.makeText(
                        this,
                        "Trip information updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}
