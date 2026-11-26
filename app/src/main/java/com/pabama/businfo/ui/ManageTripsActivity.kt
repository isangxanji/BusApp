package com.pabama.businfo.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import com.pabama.businfo.data.entity.TripEntity
import com.pabama.businfo.databinding.ActivityManageTripsBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory
import android.view.View
import android.widget.ScrollView
import com.pabama.businfo.data.entity.BusEntity
import android.widget.AdapterView

class ManageTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTripsBinding

    private var availableBuses: List<BusEntity> = emptyList()
    private val predefinedRoutes = listOf(
        listOf(
            "Kibawe",
            "Don Carlos",
            "Maramag",
            "Valencia",
            "Malaybalay"
        ),
        listOf(
            "Kibawe",
            "Don Carlos",
            "Maramag",
            "Valencia",
            "Malaybalay",
            "Cagayan de Oro"
        )
    )

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityManageTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup staff bottom navigation
        NavigationHelper.setupStaffBottomNav(this, "TRIPS")

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // Add Trip button
        binding.btnAddTrip.setOnClickListener {
            showAddTripDialog()
        }

        // Load registered buses
        viewModel.allBuses.observe(this) { buses ->
            availableBuses = buses.orEmpty()
                .sortedBy { it.plateNumber }
        }

        // Observe trips from Room DB and render dynamically
        viewModel.allTrips.observe(this) { trips ->
            renderTripRecords(trips)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupStaffBottomNav(this, "TRIPS")
    }

    private fun renderTripRecords(trips: List<TripEntity>) {
        binding.containerTripRecords.removeAllViews()

        if (trips.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No trip routes currently recorded."
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 32, 0, 32)
            }
            binding.containerTripRecords.addView(emptyText)
            return
        }

        for (trip in trips) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 32, 40, 32)
                background = getDrawable(R.drawable.bg_card)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 20
                }
                layoutParams = params
            }

            val busText = TextView(this).apply {
                text = trip.busPlate
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val routeStops = trip.route
                .split("→", "->", "—")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val routeText = TextView(this).apply {
                text = when {
                    routeStops.size >= 2 ->
                        "Route: ${routeStops.first()} to ${routeStops.last()}"

                    routeStops.size == 1 ->
                        "Route: ${routeStops.first()}"

                    else -> "Route not specified"
                }

                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 12, 0, 0)
            }

            // Bottom row: Status badge on left, Edit button on right
            val bottomRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                layoutParams = p
            }

            val statusText = TextView(this).apply {
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(20, 6, 20, 6)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = p
            }
            StatusBadge.setStatus(statusText, trip.status)

            val btnEdit = TextView(this).apply {
                text = "Edit"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.primary))
                background = getDrawable(R.color.accent_light)
                setPadding(28, 12, 28, 12)
                setOnClickListener {
                    showEditTripDialog(trip)
                }
            }

            bottomRow.addView(statusText)
            bottomRow.addView(btnEdit)

            card.addView(busText)
            card.addView(routeText)

            if (routeStops.size > 2) {

                val stopsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = View.GONE
                    setPadding(12, 12, 0, 12)
                }

                routeStops.drop(1).dropLast(1).forEach { stop ->

                    val stopText = TextView(this).apply {
                        text = "○  $stop"
                        textSize = 13f
                        setTextColor(getColor(R.color.text_secondary))
                        setPadding(0, 8, 0, 8)
                    }

                    stopsContainer.addView(stopText)
                }

                val btnViewStops = TextView(this).apply {
                    text = "View Intermediate Stops  ▼"
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(getColor(R.color.accent))
                    setPadding(0, 20, 0, 8)

                    setOnClickListener {
                        if (stopsContainer.visibility == View.GONE) {
                            stopsContainer.visibility = View.VISIBLE
                            text = "Hide Intermediate Stops  ▲"
                        } else {
                            stopsContainer.visibility = View.GONE
                            text = "View Intermediate Stops  ▼"
                        }
                    }
                }

                card.addView(btnViewStops)
                card.addView(stopsContainer)
            }

            card.addView(bottomRow)

            binding.containerTripRecords.addView(card)
        }
    }

    private fun getPredefinedRoute(
        origin: String,
        destination: String
    ): List<String> {

        for (route in predefinedRoutes) {

            if (
                route.first().equals(origin, ignoreCase = true) &&
                route.last().equals(destination, ignoreCase = true)
            ) {
                return route
            }

            if (
                route.last().equals(origin, ignoreCase = true) &&
                route.first().equals(destination, ignoreCase = true)
            ) {
                return route.reversed()
            }
        }

        return emptyList()
    }

    private data class RouteInputs(
        val origin: Spinner,
        val destination: Spinner,
        val stops: MutableList<EditText>
    ) {
        fun getCompleteRoute(): String {

            val intermediateStops = stops
                .map { it.text.toString().trim() }
                .filter { it.isNotEmpty() }

            return (
                    listOf(origin.selectedItem.toString()) +
                            intermediateStops +
                            listOf(destination.selectedItem.toString())
                    ).joinToString(" to ")
        }

        fun isValid(): Boolean {

            if (origin.selectedItemPosition <= 0 ||
                destination.selectedItemPosition <= 0
            ) {
                return false
            }

            val routeTerminals =
                listOf(origin.selectedItem.toString().trim()) +
                        stops.map { it.text.toString().trim() } +
                        listOf(destination.selectedItem.toString().trim())

            // Prevent empty terminal names
            if (routeTerminals.any { it.isBlank() }) {
                return false
            }

            // Prevent invalid route separators in terminal names
            if (routeTerminals.any {
                    it.contains("→") ||
                            it.contains("->") ||
                            it.contains("—")
                }) {
                return false
            }

            // Prevent duplicate terminals
            val normalizedTerminals = routeTerminals.map {
                it.lowercase(java.util.Locale.ROOT)
            }

            return normalizedTerminals.size ==
                    normalizedTerminals.distinct().size
        }
    }

    private fun createRouteInputs(
        layout: LinearLayout,
        existingRoute: String = ""
    ): RouteInputs {

        val routeStops = existingRoute
            .split("→", "->", "—")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val terminals = (
                predefinedRoutes.flatMap {
                    listOf(it.first(), it.last())
                } +
                        if (routeStops.size >= 2) {
                            listOf(routeStops.first(), routeStops.last())
                        } else {
                            emptyList()
                        }
                ).distinct().sorted()

        val terminalOptions = listOf("Select terminal") + terminals

        fun createTerminalSpinner(
            label: String,
            selectedTerminal: String = ""
        ): Spinner {

            val fieldLabel = TextView(this).apply {
                text = label
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.dialog_text))
                setPadding(0, 24, 0, 12)
            }

            layout.addView(fieldLabel)

            val spinner = Spinner(this).apply {
                background = getDrawable(R.drawable.bg_spinner)
                setPadding(24, 16, 24, 16)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val adapter = ArrayAdapter(
                this,
                R.layout.item_spinner_selected,
                terminalOptions
            ).apply {
                setDropDownViewResource(
                    R.layout.item_spinner_dropdown
                )
            }

            spinner.adapter = adapter

            val selectedIndex = terminalOptions.indexOfFirst {
                it.equals(selectedTerminal, ignoreCase = true)
            }

            spinner.setSelection(selectedIndex.coerceAtLeast(0))

            layout.addView(spinner)

            return spinner
        }

        // Origin dropdown
        val inputOrigin = createTerminalSpinner(
            "Origin",
            if (routeStops.size >= 2) routeStops.first() else ""
        )

        // Destination dropdown
        val inputDestination = createTerminalSpinner(
            "Destination",
            if (routeStops.size >= 2) routeStops.last() else ""
        )

        // Intermediate stops heading
        val stopsLabel = TextView(this).apply {
            text = "Route Preview"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_text))
            setPadding(0, 24, 0, 12)
        }

        layout.addView(stopsLabel)
        val originPreview = TextView(this).apply {
            text = "●  Select origin"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_text))
            setPadding(0, 16, 0, 16)
        }

        layout.addView(originPreview)

        // Container for intermediate stops
        val stopsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        layout.addView(stopsContainer)
        val destinationPreview = TextView(this).apply {
            text = "●  Select destination"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_text))
            setPadding(0, 16, 0, 16)
        }

        layout.addView(destinationPreview)

        val stopInputs = mutableListOf<EditText>()

        // Function to add a stop
        fun addStopField(
            existingStop: String = "",
            position: Int = stopInputs.size
        ) {

            val stopRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val inputStop = EditText(this).apply {
                hint = "Terminal or stop"
                setText(existingStop)
                setTextColor(getColor(R.color.dialog_text))
                setHintTextColor(getColor(R.color.dialog_hint))
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSingleLine(true)

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val btnRemove = TextView(this).apply {
                text = "✕"
                textSize = 20f
                setTextColor(getColor(R.color.dialog_delete))
                setPadding(24, 16, 16, 16)

                setOnClickListener {
                    stopInputs.remove(inputStop)
                    stopsContainer.removeView(stopRow)
                }
            }

            val insertPosition = position.coerceIn(0, stopInputs.size)

            stopInputs.add(insertPosition, inputStop)

            stopRow.addView(inputStop)
            stopRow.addView(btnRemove)

            stopsContainer.addView(stopRow, insertPosition)
        }

        // Load existing intermediate stops
        if (routeStops.size > 2) {
            routeStops.drop(1).dropLast(1).forEach { stop ->
                addStopField(stop)
            }
        }

        fun updateEndpointLabels() {

            originPreview.text =
                if (inputOrigin.selectedItemPosition > 0) {
                    "●  ${inputOrigin.selectedItem} (Origin)"
                } else {
                    "●  Select origin"
                }

            destinationPreview.text =
                if (inputDestination.selectedItemPosition > 0) {
                    "●  ${inputDestination.selectedItem} (Destination)"
                } else {
                    "●  Select destination"
                }
        }

        updateEndpointLabels()

        var previousOrigin = inputOrigin.selectedItem.toString()
        var previousDestination = inputDestination.selectedItem.toString()

        fun updateDestinationOptions() {

            val origin = inputOrigin.selectedItem.toString()
            val currentDestination =
                inputDestination.selectedItem?.toString().orEmpty()

            val validDestinations = predefinedRoutes.mapNotNull { route ->

                when {
                    route.first().equals(origin, ignoreCase = true) ->
                        route.last()

                    route.last().equals(origin, ignoreCase = true) ->
                        route.first()

                    else -> null
                }
            }.toMutableList()

            // Preserve existing custom routes when editing
            if (routeStops.size >= 2 &&
                routeStops.first().equals(origin, ignoreCase = true)
            ) {
                validDestinations.add(routeStops.last())
            }

            val options = listOf("Select terminal") +
                    validDestinations.distinct().sorted()

            val adapter = ArrayAdapter(
                this,
                R.layout.item_spinner_selected,
                options
            ).apply {
                setDropDownViewResource(
                    R.layout.item_spinner_dropdown
                )
            }

            inputDestination.adapter = adapter

            val selectedIndex = options.indexOf(currentDestination)

            inputDestination.setSelection(
                when {
                    selectedIndex > 0 -> selectedIndex
                    options.size == 2 -> 1
                    else -> 0
                }
            )
        }

        fun updateRoutePreview() {
            updateEndpointLabels()
            val origin = inputOrigin.selectedItem.toString()
            val destination = inputDestination.selectedItem.toString()

            // Preserve existing stops when opening Edit Trip
            if (origin == previousOrigin &&
                destination == previousDestination
            ) {
                return
            }

            previousOrigin = origin
            previousDestination = destination

            // Clear previous intermediate stops
            stopInputs.clear()
            stopsContainer.removeAllViews()

            if (inputOrigin.selectedItemPosition <= 0 ||
                inputDestination.selectedItemPosition <= 0 ||
                origin == destination
            ) {
                return
            }

            val route = getPredefinedRoute(origin, destination)

            if (route.size > 2) {
                route.drop(1).dropLast(1).forEach { stop ->
                    addStopField(stop)
                }
            }
        }

        val routeSelectionListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateRoutePreview()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    // No action needed
                }
            }

        updateDestinationOptions()

        inputOrigin.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateDestinationOptions()
                    updateRoutePreview()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    // No action needed
                }
            }

        inputDestination.onItemSelectedListener = routeSelectionListener


        // Add Stop button
        val btnAddStop = TextView(this).apply {
            text = "+ Add Stop"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_action))
            setPadding(0, 20, 0, 20)

            setOnClickListener {

                if (inputOrigin.selectedItemPosition <= 0 ||
                    inputDestination.selectedItemPosition <= 0 ||
                    inputOrigin.selectedItem == inputDestination.selectedItem
                ) {
                    Toast.makeText(
                        this@ManageTripsActivity,
                        "Select a valid origin and destination first.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val positions = listOf(
                    "After ${inputOrigin.selectedItem}"
                ) + stopInputs.mapIndexed { index, field ->
                    val name = field.text.toString().trim()

                    "After ${name.ifBlank { "Stop ${index + 1}" }}"
                }

                AlertDialog.Builder(this@ManageTripsActivity)
                    .setTitle("Insert Terminal Stop")
                    .setItems(positions.toTypedArray()) { _, which ->
                        addStopField(position = which)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        layout.addView(btnAddStop)

        return RouteInputs(
            origin = inputOrigin,
            destination = inputDestination,
            stops = stopInputs
        )
    }

    private fun showAddTripDialog() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        // Select registered bus
        val busLabel = TextView(this).apply {
            text = "Select Bus"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_text))
            setPadding(0, 0, 0, 12)
        }

        layout.addView(busLabel)

        val busOptions = listOf("Select bus") +
                availableBuses.map { it.plateNumber }

        val busSpinner = Spinner(this).apply {
            background = getDrawable(R.drawable.bg_spinner)
            setPadding(24, 16, 24, 16)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val busAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            busOptions
        ).apply {
            setDropDownViewResource(
                R.layout.item_spinner_dropdown
            )
        }

        busSpinner.adapter = busAdapter
        layout.addView(busSpinner)

// Display selected bus type
        val busTypeText = TextView(this).apply {
            text = "Bus Type: —"
            textSize = 13f
            setTextColor(getColor(R.color.dialog_hint))
            setPadding(0, 16, 0, 0)
        }

        layout.addView(busTypeText)

        busSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedPlate = busOptions[position]

                    val selectedBus = availableBuses.find {
                        it.plateNumber == selectedPlate
                    }

                    busTypeText.text = if (selectedBus != null) {
                        "Bus Type: ${selectedBus.busType}"
                    } else {
                        "Bus Type: —"
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    busTypeText.text = "Bus Type: —"
                }
            }

        // Origin, destination and intermediate stops
        val routeInputs = createRouteInputs(layout)

        // Make the dialog scrollable
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            addView(layout)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Trip Route")
            .setView(scrollView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {

            val btnAdd = dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            btnAdd.setOnClickListener {

                val plate = if (busSpinner.selectedItemPosition > 0) {
                    busSpinner.selectedItem.toString().trim()
                } else {
                    ""
                }

                // Validate required fields
                if (plate.isEmpty() ||
                    availableBuses.none { it.plateNumber == plate } ||
                    !routeInputs.isValid()
                ) {

                    Toast.makeText(
                        this,
                        "Select a bus and valid route. Check for empty or duplicate terminals.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                // Generate the complete route
                val route = routeInputs.getCompleteRoute()

                // Prevent repeated submissions
                btnAdd.isEnabled = false

                // Use the existing ViewModel function
                viewModel.addTrip(
                    plate,
                    route,
                    "AVAILABLE"
                ) { success ->

                    btnAdd.isEnabled = true

                    if (success) {

                        Toast.makeText(
                            this,
                            "Trip for $plate added successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()

                    } else {

                        Toast.makeText(
                            this,
                            "Failed to add trip route.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showEditTripDialog(trip: TripEntity) {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        // Display assigned bus
        val busLabel = TextView(this).apply {
            text = "Assigned Bus"
            textSize = 14f
            setTextColor(getColor(R.color.dialog_hint))
        }

        layout.addView(busLabel)

        val busPlateText = TextView(this).apply {
            text = trip.busPlate
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_text))
            setPadding(0, 12, 0, 8)
        }

        layout.addView(busPlateText)

        val assignedBus = availableBuses.find {
            it.plateNumber.equals(
                trip.busPlate,
                ignoreCase = true
            )
        }

        val busTypeText = TextView(this).apply {
            text = "Bus Type: ${assignedBus?.busType ?: "Not specified"}"
            textSize = 13f
            setTextColor(getColor(R.color.dialog_hint))
        }

        layout.addView(busTypeText)


        // Load existing route information
        val routeInputs = createRouteInputs(
            layout,
            trip.route
        )

        // Status label
        val statusLabel = TextView(this).apply {
            text = "Trip Status"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.dialog_text))
            setPadding(0, 20, 0, 12)
        }

        layout.addView(statusLabel)

        // Status spinner
        val statusSpinner = Spinner(this).apply {
            background = getDrawable(R.drawable.bg_spinner)
            setPadding(24, 16, 24, 16)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val statuses = arrayOf(
            "AVAILABLE",
            "EN ROUTE",
            "DEPARTED",
            "COMPLETED",
            "UNAVAILABLE"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            statuses
        ).apply {
            setDropDownViewResource(
                R.layout.item_spinner_dropdown
            )
        }

        statusSpinner.adapter = adapter

        val statusIdx = statuses.indexOfFirst {
            it.equals(trip.status, ignoreCase = true)
        }

        if (statusIdx >= 0) {
            statusSpinner.setSelection(statusIdx)
        }

        layout.addView(statusSpinner)

        // Scrollable dialog
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            addView(layout)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Trip ${trip.busPlate}")
            .setView(scrollView)
            .setNegativeButton("Cancel", null)

            // Delete trip
            .setNeutralButton("Delete") { _, _ ->

                AlertDialog.Builder(this)
                    .setTitle("Delete Trip")
                    .setMessage(
                        "Are you sure you want to delete " +
                                "the trip route for ${trip.busPlate}?"
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->

                        viewModel.deleteTrip(trip) { success ->

                            if (success) {
                                Toast.makeText(
                                    this,
                                    "Trip route deleted.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to delete trip.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .show()
            }

            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {

            val btnSave = dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            btnSave.setOnClickListener {

                val bus = trip.busPlate

                val status = statusSpinner
                    .selectedItem.toString()

                // Validate inputs
                if (bus.isEmpty() ||
                    availableBuses.none {
                        it.plateNumber.equals(bus, ignoreCase = true)
                    } ||
                    !routeInputs.isValid()
                ) {

                    Toast.makeText(
                        this,
                        "Bus plate, origin and destination are required.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                // Generate complete route
                val route = routeInputs.getCompleteRoute()

                // Prepare updated trip
                val updated = trip.copy(
                    busPlate = bus,
                    route = route,
                    status = status
                )

                // Prevent repeated submissions
                btnSave.isEnabled = false

                // Use existing update function
                viewModel.updateTrip(updated) { success ->

                    btnSave.isEnabled = true

                    if (success) {

                        Toast.makeText(
                            this,
                            "Trip route updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()

                    } else {

                        Toast.makeText(
                            this,
                            "Failed to update trip.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }
}
