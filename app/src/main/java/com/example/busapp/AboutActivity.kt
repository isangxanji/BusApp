package com.example.busapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Passenger Bottom Navigation
        val btnHome = findViewById<android.widget.TextView>(R.id.btnHome)
        val btnBuses = findViewById<android.widget.TextView>(R.id.btnBuses)
        val btnUpdates = findViewById<android.widget.TextView>(R.id.btnUpdates)
        val btnAbout = findViewById<android.widget.TextView>(R.id.btnAbout)

        // Highlight About as the active screen
        btnAbout.setTextColor(getColor(R.color.accent))

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnBuses.setOnClickListener {
            startActivity(Intent(this, BusInformationActivity::class.java))
        }

        btnUpdates.setOnClickListener {
            startActivity(Intent(this, TripUpdatesActivity::class.java))
        }

        btnAbout.setOnClickListener {
            // Already on the About screen
        }
    }
}