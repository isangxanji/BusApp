package com.pabama.businfo.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
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

        val isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        if (isAdmin) {
            binding.txtLoginHeader.text = "Admin Login"
            binding.txtLoginSubtitle.text = "Sign in using administrator credentials."
            binding.btnRegisterStaff.visibility = View.VISIBLE
            binding.btnRegisterStaff.text = "Register account"
            binding.btnRequestStaffAccount.visibility = View.GONE
        } else {
            binding.txtLoginHeader.text = "Staff Login"
            binding.txtLoginSubtitle.text = "Sign in using your authorized staff credentials."
            binding.btnRegisterStaff.visibility = View.GONE
            binding.btnRequestStaffAccount.visibility = View.VISIBLE
            binding.btnRequestStaffAccount.text = "Need an account? Submit request"
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

        binding.btnRequestStaffAccount.setOnClickListener {
            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:rotia.james12@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Staff Account Addition Request - Mindanao Bus Info")
                putExtra(Intent.EXTRA_TEXT, "Hello Admin,\n\nI would like to request a new staff account.\n\nFull Name: \nContact Number: \nPreferred Staff ID: \nRole (Inspector/Dispatcher/Conductor): \n\nThank you.")
            }
            try {
                startActivity(Intent.createChooser(mailIntent, "Submit Account Request"))
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found to submit request.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBackToHome.setOnClickListener {
            finish()
        }
    }

    private fun showRegisterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_register_staff, null)
        val inputId = dialogView.findViewById<EditText>(R.id.dialogInputId)
        val inputName = dialogView.findViewById<EditText>(R.id.dialogInputName)
        val inputPwd = dialogView.findViewById<EditText>(R.id.dialogInputPwd)

        AlertDialog.Builder(this)
            .setTitle("Register Account")
            .setView(dialogView)
            .setPositiveButton("Register") { _, _ ->
                val id = inputId.text.toString().trim()
                val name = inputName.text.toString().trim()
                val pwd = inputPwd.text.toString().trim()

                if (id.isEmpty() || name.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (id.startsWith("ADMIN", ignoreCase = true)) {
                    Toast.makeText(this, "Admin accounts must be created by an existing administrator.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                viewModel.addStaff(id, name, pwd) { success ->
                    if (success) {
                        Toast.makeText(this, "Account registered successfully! You can now log in.", Toast.LENGTH_LONG).show()
                        binding.inputStaffId.setText(id)
                        binding.inputStaffPassword.setText(pwd)
                    } else {
                        Toast.makeText(this, "Staff ID '$id' already exists or failed to register.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
