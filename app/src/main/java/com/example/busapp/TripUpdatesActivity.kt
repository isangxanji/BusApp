
package com.example.busapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TripUpdatesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trip_updates)

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

        val btnArrivals = findViewById<View>(R.id.btnArrivals)
        val btnDepartures = findViewById<View>(R.id.btnDepartures)

        val layoutArrivals = findViewById<View>(R.id.layoutArrivals)
        val layoutDepartures = findViewById<View>(R.id.layoutDepartures)

        // Show Arrivals by default
        layoutArrivals.visibility = View.VISIBLE
        layoutDepartures.visibility = View.GONE

        // Arrivals button
        btnArrivals.setOnClickListener {
            layoutArrivals.visibility = View.VISIBLE
            layoutDepartures.visibility = View.GONE
        }

        // Departures button
        btnDepartures.setOnClickListener {
            layoutArrivals.visibility = View.GONE
            layoutDepartures.visibility = View.VISIBLE
        }

        // Passenger Bottom Navigation
        val btnHome = findViewById<android.widget.TextView>(R.id.btnHome)
        val btnBuses = findViewById<android.widget.TextView>(R.id.btnBuses)
        val btnUpdates = findViewById<android.widget.TextView>(R.id.btnUpdates)
        val btnAbout = findViewById<android.widget.TextView>(R.id.btnAbout)

        // Highlight Updates as the active screen
        btnUpdates.setTextColor(getColor(R.color.accent))

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnBuses.setOnClickListener {
            startActivity(Intent(this, BusInformationActivity::class.java))
        }

        btnUpdates.setOnClickListener {
            // Already on the Updates screen
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
