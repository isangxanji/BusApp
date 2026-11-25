package com.pabama.businfo.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
import com.pabama.businfo.databinding.ActivityRecordBusMovementBinding
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.View
import android.widget.AdapterView
import com.pabama.businfo.data.entity.BusEntity

class RecordBusMovementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordBusMovementBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    private val busPlates = mutableListOf<String>()
    private val timeOptions = mutableListOf<String>()
    private lateinit var busAdapter: ArrayAdapter<String>
    private lateinit var timeAdapter: ArrayAdapter<String>

    private var availableBuses: List<BusEntity> = emptyList()
    private var previousSelectedBus = ""

    private val terminalOptions = mutableListOf<String>()

    private lateinit var terminalAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecordBusMovementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val initialLeft = binding.main.paddingLeft
        val initialTop = binding.main.paddingTop
        val initialRight = binding.main.paddingRight
        val initialBottom = binding.main.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )

            insets
        }

        // Setup staff bottom navigation
        NavigationHelper.setupStaffBottomNav(this, "RECORD")

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // Setup Bus Spinner with Black Font layout
        busAdapter = ArrayAdapter(this, R.layout.item_spinner_selected, busPlates).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerBus.adapter = busAdapter

        // Setup terminal dropdown
        terminalAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            terminalOptions
        ).apply {
            setDropDownViewResource(
                R.layout.item_spinner_dropdown
            )
        }

        binding.spinnerTerminal.adapter = terminalAdapter

// Detect selected bus changes
        binding.spinnerBus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateSelectedBusInformation()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    updateSelectedBusInformation()
                }
            }

        // Update terminal label based on movement type
        binding.radioMovementType.setOnCheckedChangeListener { _, checkedId ->

            binding.txtTerminalLabel.text =
                if (checkedId == R.id.radioArrival) {
                    "Arrival Terminal"
                } else {
                    "Departure Terminal"
                }
        }

        // Populate Bus Spinner from Room DB
        viewModel.allBuses.observe(this) { buses ->

            availableBuses = buses.orEmpty()

            busPlates.clear()

            if (availableBuses.isEmpty()) {

                busPlates.add("No buses available")

                binding.btnSaveMovement.isEnabled = false
                binding.btnReviewMovement.isEnabled = false

            } else {

                busPlates.add("Select bus")

                busPlates.addAll(
                    availableBuses.map { it.plateNumber }
                )

                binding.btnSaveMovement.isEnabled = true
                binding.btnReviewMovement.isEnabled = true
            }

            busAdapter.notifyDataSetChanged()

            updateSelectedBusInformation()
        }

        // Setup Time Dropdown with Black Font layout
        setupTimeDropdown()

        // Custom Time Picker button
        binding.btnPickCustomTime.setOnClickListener {
            openClockPicker()
        }

        // Direct Save Movement Button
        binding.btnSaveMovement.setOnClickListener {
            performSaveMovement()
        }

        // Review Movement Button (with Confirmation Dialog)
        binding.btnReviewMovement.setOnClickListener {
            val movementType = getSelectedMovementType()
            val terminal = getSelectedTerminal()
            val selectedBus = binding.spinnerBus.selectedItem?.toString() ?: ""

            val time = getSelectedTime()
            val notes = binding.inputMovementNotes.text?.toString()?.trim() ?: ""

            if (selectedBus.isEmpty() ||
                selectedBus == "No buses available" ||
                selectedBus == "Select bus"
            ) {
                Toast.makeText(this, "Please add a bus in Manage Buses before recording movement.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidTimeFormat(time)) {
                Toast.makeText(this, "Please choose a valid time format (e.g. 10:30 AM).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (terminal.isEmpty()) {

                Toast.makeText(
                    this,
                    "Please select a terminal.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val reviewMessage = """
                Movement Type: $movementType

                Bus: $selectedBus

                Origin: ${binding.txtBusOrigin.text}

                $movementType Terminal: $terminal

                Final Destination: ${binding.txtBusDestination.text}

                Time: $time

                Notes: ${if (notes.isEmpty()) "None" else notes}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Review Bus Movement")
                .setMessage(reviewMessage)
                .setNegativeButton("Edit", null)
                .setPositiveButton("Confirm & Save") { _, _ ->
                    performSaveMovement()
                }
                .show()
        }
    }

    private fun updateSelectedBusInformation() {

        val selectedPlate =
            binding.spinnerBus.selectedItem?.toString() ?: ""

        val previousTerminal =
            binding.spinnerTerminal.selectedItem?.toString().orEmpty()

        val isSameBus = selectedPlate.equals(
            previousSelectedBus,
            ignoreCase = true
        )

        previousSelectedBus = selectedPlate

        val selectedBus = availableBuses.firstOrNull {
            it.plateNumber.equals(
                selectedPlate,
                ignoreCase = true
            )
        }


        terminalOptions.clear()
        terminalOptions.add("Select terminal")

        if (selectedBus == null) {

            binding.layoutSelectedBusInfo.visibility = View.GONE

            binding.spinnerTerminal.isEnabled = false

            terminalAdapter.notifyDataSetChanged()
            binding.spinnerTerminal.setSelection(0)

            return
        }

        // Extract stops from the existing route
        val routeStops = selectedBus.routeDescription
            .split("→", "->", "—")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val hasValidRoute = routeStops.size >= 2 &&
                routeStops.none {
                    it.equals("Terminal", ignoreCase = true) ||
                            it.equals("Destination", ignoreCase = true)
                } &&
                !routeStops.first().equals(
                    routeStops.last(),
                    ignoreCase = true
                )

        if (!hasValidRoute) {

            binding.layoutSelectedBusInfo.visibility = View.VISIBLE

            binding.txtBusOrigin.text = "Not assigned"
            binding.txtBusDestination.text = "Not assigned"
            binding.txtBusRoute.text =
                "Assign a route in Manage Trips first."

            binding.spinnerTerminal.isEnabled = false

            terminalAdapter.notifyDataSetChanged()
            binding.spinnerTerminal.setSelection(0)

            return
        }

        // Display selected bus information
        binding.layoutSelectedBusInfo.visibility = View.VISIBLE

        binding.txtBusOrigin.text =
            if (routeStops.size >= 2) {
                routeStops.first()
            } else {
                "Not specified"
            }

        binding.txtBusDestination.text =
            if (routeStops.size >= 2) {
                routeStops.last()
            } else {
                selectedBus.destination
            }

        binding.txtBusRoute.text =
            if (routeStops.isNotEmpty()) {
                routeStops.joinToString(" → ")
            } else {
                "Route not specified"
            }

        // Populate terminal dropdown
        if (routeStops.size >= 2) {

            terminalOptions.addAll(
                routeStops.distinct()
            )

            binding.spinnerTerminal.isEnabled = true

        } else {

            binding.spinnerTerminal.isEnabled = false
        }

        terminalAdapter.notifyDataSetChanged()

        val previousIndex = terminalOptions.indexOf(previousTerminal)

        if (isSameBus && previousIndex > 0) {
            binding.spinnerTerminal.setSelection(previousIndex)
        } else {
            binding.spinnerTerminal.setSelection(0)
        }
    }

    private fun setupTimeDropdown() {
        timeOptions.clear()

        // 1. Current Time at index 0
        val currentTimeFormatted = SimpleDateFormat("hh:mm a", Locale.US).format(Date()).uppercase()
        timeOptions.add("$currentTimeFormatted (Current Time)")

        // 2. Standard 30-minute operational intervals from 05:00 AM to 11:30 PM
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        while (!cal.after(endCal)) {
            val formatted = timeFormat.format(cal.time).uppercase()
            if (!timeOptions.any { it.startsWith(formatted) }) {
                timeOptions.add(formatted)
            }
            cal.add(Calendar.MINUTE, 30)
        }

        timeAdapter = ArrayAdapter(this, R.layout.item_spinner_selected, timeOptions).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerTime.adapter = timeAdapter
        binding.spinnerTime.setSelection(0)
    }

    private fun openClockPicker() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hourOfDay, minute ->
            val pickedCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            val formattedTime = SimpleDateFormat("hh:mm a", Locale.US).format(pickedCal.time).uppercase()

            // Insert custom time into options and select it
            if (!timeOptions.contains(formattedTime)) {
                timeOptions.add(1, formattedTime)
                timeAdapter.notifyDataSetChanged()
                binding.spinnerTime.setSelection(1)
            } else {
                binding.spinnerTime.setSelection(timeOptions.indexOf(formattedTime))
            }
            Toast.makeText(this, "Time selected: $formattedTime", Toast.LENGTH_SHORT).show()
        }, currentHour, currentMinute, false).show()
    }

    private fun getSelectedTime(): String {
        val raw = binding.spinnerTime.selectedItem?.toString()?.trim() ?: ""
        return raw.replace("(Current Time)", "").trim()
    }

    private fun isValidTimeFormat(time: String): Boolean {
        val regex = Regex("^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM)$", RegexOption.IGNORE_CASE)
        return regex.matches(time)
    }

    private fun getSelectedMovementType(): String {
        return if (binding.radioGroupMovement.checkedRadioButtonId == R.id.radioArrival) {
            "Arrival"
        } else {
            "Departure"
        }
    }

    private fun getSelectedTerminal(): String {

        val terminal = binding.spinnerTerminal
            .selectedItem?.toString()?.trim() ?: ""

        return if (
            terminal.isEmpty() ||
            terminal == "Select terminal" ||
            !binding.spinnerTerminal.isEnabled
        ) {
            ""
        } else {
            terminal
        }
    }

    private fun performSaveMovement() {

        val selectedBus =
            binding.spinnerBus.selectedItem?.toString() ?: ""

        val terminal = getSelectedTerminal()

        val time = getSelectedTime()

        if (
            selectedBus.isEmpty() ||
            selectedBus == "No buses available" ||
            selectedBus == "Select bus"
        ) {
            Toast.makeText(
                this,
                "Please select a bus.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (terminal.isEmpty()) {
            Toast.makeText(
                this,
                "Please select a terminal.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!isValidTimeFormat(time)) {
            Toast.makeText(
                this,
                "Please select a valid movement time.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Persist movement record to Room SQLite and Firebase
        val movementType = getSelectedMovementType()
        val notes = binding.inputMovementNotes.text?.toString()?.trim() ?: ""

        viewModel.recordMovement(movementType, selectedBus, terminal, time, notes) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    "Bus movement saved successfully ($terminal at $time)!",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Failed to save movement record.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Helper property to safely access the RadioGroup by ID
    private val ActivityRecordBusMovementBinding.radioGroupMovement: android.widget.RadioGroup
        get() = this.radioMovementType

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupStaffBottomNav(this, "RECORD")
    }
}
