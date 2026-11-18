
package com.example.busapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StaffLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_staff_login)

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

        val inputStaffId = findViewById<EditText>(R.id.inputStaffId)
        val inputPassword = findViewById<EditText>(R.id.inputStaffPassword)
        val btnLogin = findViewById<Button>(R.id.btnStaffLogin)
        val btnBack = findViewById<TextView>(R.id.btnBackToHome)

        btnLogin.setOnClickListener {

            val staffId = inputStaffId.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (staffId.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter your Staff ID and password.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Temporary demo credentials only
            if (staffId == "STAFF001" && password == "staff123") {

                val intent = Intent(this, StaffDashboardActivity::class.java)
                intent.putExtra("STAFF_ID", staffId)
                startActivity(intent)
                finish()

            } else {
                Toast.makeText(
                    this,
                    "Invalid Staff ID or password.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
