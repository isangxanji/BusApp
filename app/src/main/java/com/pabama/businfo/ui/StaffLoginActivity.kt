package com.pabama.businfo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.data.SessionManager
import com.pabama.businfo.databinding.ActivityStaffLoginBinding
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class StaffLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffLoginBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager.getInstance(this)

        // If already logged in, redirect directly to dashboard
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.isAdmin()) {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else {
                startActivity(Intent(this, StaffDashboardActivity::class.java))
            }
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityStaffLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.inputStaffPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.btnStaffLogin.performClick()
                true
            } else {
                false
            }
        }

        binding.btnStaffLogin.setOnClickListener {
            val staffId = binding.inputStaffId.text?.toString()?.trim() ?: ""
            val password = binding.inputStaffPassword.text?.toString()?.trim() ?: ""

            if (staffId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter your Staff ID and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Authenticate against Room database
            viewModel.login(staffId, password) { success, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (success) {
                    val staff = viewModel.currentStaff.value
                    val staffName = staff?.name ?: "Staff Member"
                    val role = if (staffId.startsWith("ADMIN", ignoreCase = true)) "ADMIN" else "STAFF"

                    sessionManager.saveSession(staffId, staffName, role)

                    if (sessionManager.isAdmin()) {
                        val intent = Intent(this, AdminDashboardActivity::class.java)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, StaffDashboardActivity::class.java).apply {
                            putExtra("STAFF_ID", staffId)
                        }
                        startActivity(intent)
                    }
                    finish()
                }
            }
        }

        binding.btnRegisterStaff.setOnClickListener {
            showRegisterDialog()
        }

        binding.btnBackToHome.setOnClickListener {
            finish()
        }
    }

    private fun showRegisterDialog() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        val inputId = android.widget.EditText(this).apply {
            hint = "Staff ID (e.g. STAFF002)"
            setSingleLine(true)
        }

        val inputName = android.widget.EditText(this).apply {
            hint = "Full Name"
            setSingleLine(true)
            val p = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }

        val inputPwd = android.widget.EditText(this).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            val p = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }

        layout.addView(inputId)
        layout.addView(inputName)
        layout.addView(inputPwd)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Register Staff Account")
            .setView(layout)
            .setPositiveButton("Register") { _, _ ->
                val id = inputId.text.toString().trim()
                val name = inputName.text.toString().trim()
                val pwd = inputPwd.text.toString().trim()

                if (id.isEmpty() || name.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addStaff(id, name, pwd) { success ->
                    if (success) {
                        Toast.makeText(this, "Account registered successfully! You can now log in.", Toast.LENGTH_LONG).show()
                        binding.inputStaffId.setText(id)
                        binding.inputStaffPassword.setText(pwd)
                    } else {
                        Toast.makeText(this, "Failed to register account.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
