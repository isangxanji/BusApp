
package com.example.busapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BusInformationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bus_information)

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

        // Open Trip Details when PXR 4412 is tapped
        findViewById<android.view.View>(R.id.cardBus1).setOnClickListener {
            val intent = Intent(this, TripDetailActivity::class.java)
            startActivity(intent)
        }

        // Passenger Bottom Navigation
        val btnHome = findViewById<android.widget.TextView>(R.id.btnHome)
        val btnBuses = findViewById<android.widget.TextView>(R.id.btnBuses)
        val btnUpdates = findViewById<android.widget.TextView>(R.id.btnUpdates)
        val btnAbout = findViewById<android.widget.TextView>(R.id.btnAbout)

// Highlight Buses as the active screen
        btnBuses.setTextColor(getColor(R.color.accent))

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnBuses.setOnClickListener {
            // Already on the Bus Information screen
        }

        btnUpdates.setOnClickListener {
            startActivity(Intent(this, TripUpdatesActivity::class.java))
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
