
package com.example.busapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RecordBusMovementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_bus_movement)

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

        val btnBack = findViewById<View>(R.id.btnBack)
        val btnReview = findViewById<View>(R.id.btnReviewMovement)

        val radioGroup = findViewById<RadioGroup>(R.id.radioMovementType)
        val spinnerBus = findViewById<Spinner>(R.id.spinnerBus)
        val inputTime = findViewById<EditText>(R.id.inputMovementTime)
        val inputNotes = findViewById<EditText>(R.id.inputMovementNotes)

        // Temporary bus list
        val buses = arrayOf("PXR 4412")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            buses
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerBus.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        btnReview.setOnClickListener {

            val movementType = if (radioGroup.checkedRadioButtonId == R.id.radioArrival) {
                "Arrival"
            } else {
                "Departure"
            }

            val selectedBus = spinnerBus.selectedItem.toString()
            val time = inputTime.text.toString().trim()
            val notes = inputNotes.text.toString().trim()

            if (time.isEmpty()) {
                inputTime.error = "Please enter the time"
                inputTime.requestFocus()
                return@setOnClickListener
            }

            val reviewMessage = """
                Movement Type: $movementType

                Bus: $selectedBus

                Time: $time

                Notes: ${if (notes.isEmpty()) "None" else notes}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Review Bus Movement")
                .setMessage(reviewMessage)
                .setNegativeButton("Edit", null)
                .setPositiveButton("Save") { _, _ ->

                    val preferences = getSharedPreferences(
                        "bus_records",
                        MODE_PRIVATE
                    )

                    val oldRecords = preferences.getString("movements", "") ?: ""

                    val newRecord = "$movementType | $selectedBus | $time | $notes"

                    val updatedRecords = if (oldRecords.isEmpty()) {
                        newRecord
                    } else {
                        "$oldRecords\n$newRecord"
                    }

                    preferences.edit()
                        .putString("movements", updatedRecords)
                        .apply()

                    Toast.makeText(
                        this,
                        "Bus movement saved successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    inputTime.text.clear()
                    inputNotes.text.clear()
                    radioGroup.check(R.id.radioArrival)
                }
                .show()

        }

        // Staff Bottom Navigation
        val btnDashboard = findViewById<android.widget.TextView>(R.id.btnStaffDashboard)
        val btnRecord = findViewById<android.widget.TextView>(R.id.btnStaffRecord)
        val btnBuses = findViewById<android.widget.TextView>(R.id.btnStaffBuses)
        val btnTrips = findViewById<android.widget.TextView>(R.id.btnStaffTrips)

        // Highlight Record as the active screen
        btnRecord.setTextColor(getColor(R.color.accent))

        btnDashboard.setOnClickListener {
            startActivity(Intent(this, StaffDashboardActivity::class.java))
        }

        btnRecord.setOnClickListener {
            // Already on the Record screen
        }

        btnBuses.setOnClickListener {
            startActivity(Intent(this, ManageBusActivity::class.java))
        }

        btnTrips.setOnClickListener {
            startActivity(Intent(this, ManageTripsActivity::class.java))
        }
    }
}
