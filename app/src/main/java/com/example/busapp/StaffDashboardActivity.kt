
package com.example.busapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StaffDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_staff_dashboard)

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

        // Record Bus Movement
        findViewById<View>(R.id.btnRecordMovement).setOnClickListener {
            startActivity(Intent(this, RecordBusMovementActivity::class.java))
        }

        // Manage Buses
        findViewById<View>(R.id.btnManageBuses).setOnClickListener {
            startActivity(Intent(this, ManageBusActivity::class.java))
        }

        // Manage Trips
        findViewById<View>(R.id.btnManageTrips).setOnClickListener {
            startActivity(Intent(this, ManageTripsActivity::class.java))
        }

        // Staff Bottom Navigation
        val btnDashboard = findViewById<android.widget.TextView>(R.id.btnStaffDashboard)
        val btnRecord = findViewById<android.widget.TextView>(R.id.btnStaffRecord)
        val btnBuses = findViewById<android.widget.TextView>(R.id.btnStaffBuses)
        val btnTrips = findViewById<android.widget.TextView>(R.id.btnStaffTrips)

// Highlight Dashboard as the active screen
        btnDashboard.setTextColor(getColor(R.color.accent))

        btnDashboard.setOnClickListener {
            // Already on the Staff Dashboard
        }

        btnRecord.setOnClickListener {
            startActivity(Intent(this, RecordBusMovementActivity::class.java))
        }

        btnBuses.setOnClickListener {
            startActivity(Intent(this, ManageBusActivity::class.java))
        }

        btnTrips.setOnClickListener {
            startActivity(Intent(this, ManageTripsActivity::class.java))
        }

    }
}
