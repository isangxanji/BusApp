package com.pabama.businfo.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
import com.pabama.businfo.data.entity.StaffEntity
import com.pabama.businfo.databinding.ActivityAdminDashboardBinding
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        // Security check
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Administrator access required.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StaffDashboardActivity::class.java))
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val adminName = sessionManager.getStaffName() ?: "Admin"
        binding.txtAdminSubtitle.text = "Logged in as $adminName (${sessionManager.getStaffId()})"

        // Navigation Actions
        binding.btnAdminLogout.setOnClickListener {
            sessionManager.clearSession()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnGoToStaffDashboard.setOnClickListener {
            startActivity(Intent(this, StaffDashboardActivity::class.java))
        }

        binding.btnAdminManageBuses.setOnClickListener {
            startActivity(Intent(this, ManageBusActivity::class.java))
        }

        binding.btnAdminManageTrips.setOnClickListener {
            startActivity(Intent(this, ManageTripsActivity::class.java))
        }

        binding.btnAdminRecordMovement.setOnClickListener {
            startActivity(Intent(this, RecordBusMovementActivity::class.java))
        }

        binding.btnAdminPassengerView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnAdminResetData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Data to Defaults")
                .setMessage("Are you sure you want to reset all buses, trips, movement logs, and staff back to default seed records? This will clear any testing entries.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset Everything") { _, _ ->
                    val app = application as PabamaApplication
                    viewModel.resetDatabaseToDefaults(app.database) { success ->
                        if (success) {
                            Toast.makeText(this, "Database reset to initial defaults successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to reset database.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }

        binding.btnAddStaffAccount.setOnClickListener {
            showAddStaffDialog()
        }

        // Live Overview Metrics
        viewModel.staffCount.observe(this) { count ->
            binding.txtAdminStaffCount.text = (count ?: 0).toString()
        }

        viewModel.allBuses.observe(this) { buses ->
            binding.txtAdminBusCount.text = (buses?.size ?: 0).toString()
        }

        viewModel.allTrips.observe(this) { trips ->
            binding.txtAdminTripCount.text = (trips?.size ?: 0).toString()
        }

        viewModel.totalMovementLogsCount.observe(this) { count ->
            binding.txtAdminMovementCount.text = (count ?: 0).toString()
        }

        // Observe and render Staff Accounts
        viewModel.allStaff.observe(this) { staffList ->
            renderStaffList(staffList)
        }
    }

    private fun renderStaffList(staffList: List<StaffEntity>) {
        binding.containerStaffAccounts.removeAllViews()

        if (staffList.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No staff accounts found."
                setTextColor(getColor(R.color.text_secondary))
                setPadding(16, 24, 16, 24)
            }
            binding.containerStaffAccounts.addView(emptyText)
            return
        }

        for (staff in staffList) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 28, 32, 28)
                background = getDrawable(R.drawable.bg_card)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                layoutParams = params
            }

            // Top row: Name + Role / Status badges
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val nameLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = p
            }

            val txtName = TextView(this).apply {
                text = staff.name
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val txtId = TextView(this).apply {
                text = "ID: ${staff.staffId}"
                textSize = 12f
                setTextColor(getColor(R.color.text_secondary))
            }
            nameLayout.addView(txtName)
            nameLayout.addView(txtId)

            val roleBadge = TextView(this).apply {
                val isAdminRole = staff.staffId.startsWith("ADMIN", ignoreCase = true)
                text = if (isAdminRole) "ADMIN" else "STAFF"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(18, 6, 18, 6)
                setTextColor(if (isAdminRole) Color.parseColor("#154C34") else Color.parseColor("#44474E"))

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f
                    setColor(if (isAdminRole) Color.parseColor("#D4EBD8") else Color.parseColor("#ECEBE8"))
                }
                background = bg
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 12
                }
                layoutParams = p
            }

            val statusBadge = TextView(this).apply {
                text = if (staff.isActive) "ACTIVE" else "INACTIVE"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(18, 6, 18, 6)
                setTextColor(if (staff.isActive) Color.parseColor("#218653") else Color.parseColor("#C74343"))

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f
                    setColor(if (staff.isActive) Color.parseColor("#E5F3EA") else Color.parseColor("#FCE8E8"))
                }
                background = bg
            }

            topRow.addView(nameLayout)
            topRow.addView(roleBadge)
            topRow.addView(statusBadge)

            // Bottom action buttons row
            val actionsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 20
                }
                layoutParams = p
            }

            val btnToggle = TextView(this).apply {
                text = if (staff.isActive) "Deactivate" else "Activate"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 12, 24, 12)
                setTextColor(if (staff.isActive) getColor(R.color.status_unavailable) else getColor(R.color.status_available))
                background = getDrawable(R.drawable.bg_card)
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16
                }
                layoutParams = p
                setOnClickListener {
                    // Prevent deactivating own account
                    if (staff.staffId.equals(sessionManager.getStaffId(), ignoreCase = true)) {
                        Toast.makeText(this@AdminDashboardActivity, "Cannot deactivate your own active session.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.toggleStaffActive(staff) {
                            Toast.makeText(this@AdminDashboardActivity, "Updated status for ${staff.name}.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val btnEditPwd = TextView(this).apply {
                text = "Change Password"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 12, 24, 12)
                setTextColor(getColor(R.color.primary))
                background = getDrawable(R.drawable.bg_card)
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16
                }
                layoutParams = p
                setOnClickListener {
                    showChangePasswordDialog(staff)
                }
            }

            val btnDelete = TextView(this).apply {
                text = "Delete"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 12, 24, 12)
                setTextColor(getColor(R.color.status_unavailable))
                background = getDrawable(R.drawable.bg_card)
                setOnClickListener {
                    if (staff.staffId.equals(sessionManager.getStaffId(), ignoreCase = true)) {
                        Toast.makeText(this@AdminDashboardActivity, "Cannot delete your own active session.", Toast.LENGTH_SHORT).show()
                    } else {
                        AlertDialog.Builder(this@AdminDashboardActivity)
                            .setTitle("Delete Account")
                            .setMessage("Are you sure you want to permanently delete account ${staff.staffId} (${staff.name})?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteStaff(staff) { success ->
                                    if (success) {
                                        Toast.makeText(this@AdminDashboardActivity, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@AdminDashboardActivity, "Failed to delete account.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }

            actionsRow.addView(btnToggle)
            actionsRow.addView(btnEditPwd)
            actionsRow.addView(btnDelete)

            card.addView(topRow)
            card.addView(actionsRow)

            binding.containerStaffAccounts.addView(card)
        }
    }

    private fun showAddStaffDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        val inputStaffId = EditText(this).apply {
            hint = "Staff ID (e.g. STAFF002 or ADMIN002)"
            setSingleLine(true)
        }

        val inputName = EditText(this).apply {
            hint = "Full Name"
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }

        val inputPassword = EditText(this).apply {
            hint = "Initial Password"
            setSingleLine(true)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layoutParams = p
        }

        layout.addView(inputStaffId)
        layout.addView(inputName)
        layout.addView(inputPassword)

        AlertDialog.Builder(this)
            .setTitle("Add Authorized Staff Account")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val id = inputStaffId.text.toString().trim()
                val name = inputName.text.toString().trim()
                val pwd = inputPassword.text.toString().trim()

                if (id.isEmpty() || name.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (viewModel.allStaff.value?.any { it.staffId.equals(id, ignoreCase = true) } == true) {
                    Toast.makeText(this, "An account with ID '$id' already exists.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addStaff(id, name, pwd) { success ->
                    if (success) {
                        Toast.makeText(this, "Staff account $id created successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to create staff account.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog(staff: StaffEntity) {
        val inputNewPwd = EditText(this).apply {
            hint = "Enter new password"
            setSingleLine(true)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password for ${staff.name}")
            .setView(inputNewPwd)
            .setPositiveButton("Update") { _, _ ->
                val newPwd = inputNewPwd.text.toString().trim()
                if (newPwd.isEmpty()) {
                    Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updateStaff(staff.copy(password = newPwd)) {
                    Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
