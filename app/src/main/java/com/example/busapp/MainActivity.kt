
package com.example.busapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        // See All Buses
        findViewById<android.view.View>(R.id.btnSeeAllBuses).setOnClickListener {
            startActivity(Intent(this, BusInformationActivity::class.java))
        }

        // Bus Card
        findViewById<android.view.View>(R.id.cardBus).setOnClickListener {
            startActivity(Intent(this, TripDetailActivity::class.java))
        }

        // See All Updates
        findViewById<android.view.View>(R.id.btnSeeAllUpdates).setOnClickListener {
            startActivity(Intent(this, TripUpdatesActivity::class.java))
        }

        // Bottom Navigation
        findViewById<android.view.View>(R.id.btnHome).setOnClickListener {
            // Already on Home
        }

        findViewById<android.view.View>(R.id.btnBuses).setOnClickListener {
            startActivity(Intent(this, BusInformationActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnUpdates).setOnClickListener {
            startActivity(Intent(this, TripUpdatesActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Open Staff Dashboard
        findViewById<android.view.View>(R.id.btnOpenStaffDashboard).setOnClickListener {
            startActivity(Intent(this, StaffLoginActivity::class.java))
        }
    }
}
