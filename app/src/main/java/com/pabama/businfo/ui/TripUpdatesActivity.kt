package com.pabama.businfo.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
import com.pabama.businfo.data.entity.MovementLogEntity
import com.pabama.businfo.databinding.ActivityTripUpdatesBinding
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory
import androidx.core.widget.doAfterTextChanged
import com.pabama.businfo.data.entity.BusEntity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class TripUpdatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripUpdatesBinding
    private var allArrivalLogs: List<MovementLogEntity> = emptyList()

    private var availableBuses: List<BusEntity> = emptyList()

    private var selectedTerminal: String? = null
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTripUpdatesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup bottom navigation
        NavigationHelper.setupPassengerBottomNav(this, "UPDATES")

        viewModel.allBuses.observe(this) { buses ->
            availableBuses = buses.orEmpty()
            filterArrivals()
        }

        /// Observe Live Arrivals
        viewModel.arrivals.observe(this) { arrivals ->
            allArrivalLogs = arrivals
            filterArrivals()
        }

        binding.inputSearchArrivals.doAfterTextChanged {
            filterArrivals()
        }

        binding.btnFilterTerminal.setOnClickListener {
            showTerminalFilterDialog()
        }

        binding.txtActiveTerminalFilter.setOnClickListener {
            selectedTerminal = null
            updateTerminalFilterLabel()
            filterArrivals()
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupPassengerBottomNav(this, "UPDATES")
        filterArrivals()
    }

    private fun showTerminalFilterDialog() {

        val routeTerminals = availableBuses
            .flatMap { bus ->
                bus.routeDescription
                    .split("→", "->", "—")
                    .map { it.trim() }
            }
        val logTerminals = allArrivalLogs
            .map { it.terminal.ifBlank { it.originOrDestination }.trim() }
        val terminals = (routeTerminals + logTerminals)
            .filter {
                it.isNotBlank() &&
                        !it.equals("Terminal", ignoreCase = true) &&
                        !it.equals("Destination", ignoreCase = true)
            }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sorted()

        if (terminals.isEmpty()) {
            Toast.makeText(
                this,
                "No terminals available.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val options = listOf("All terminals") + terminals

        var selectedIndex = options.indexOf(selectedTerminal)
            .coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Filter by Terminal")
            .setSingleChoiceItems(
                options.toTypedArray(),
                selectedIndex
            ) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Apply") { _, _ ->

                if (selectedIndex == 0) {
                    selectedTerminal = null
                } else {
                    selectedTerminal = options[selectedIndex]
                }
                updateTerminalFilterLabel()
                filterArrivals()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear Filter") { _, _ ->
                selectedTerminal = null
                updateTerminalFilterLabel()
                filterArrivals()
            }
            .show()
    }

    private fun updateTerminalFilterLabel() {

        val terminal = selectedTerminal

        binding.txtActiveTerminalFilter.visibility =
            if (terminal == null) View.GONE else View.VISIBLE

        binding.txtActiveTerminalFilter.text =
            terminal?.let { "$it  ×" } ?: ""
    }
    private fun getBusDestination(plate: String): String {

        val bus = availableBuses.firstOrNull {
            it.plateNumber.trim().equals(
                plate.trim(),
                ignoreCase = true
            )
        } ?: return "Not specified"

        val stops = bus.routeDescription
            .split("→", "->", "—")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val hasValidRoute = stops.size >= 2 &&
                stops.none {
                    it.equals("Terminal", ignoreCase = true) ||
                            it.equals("Destination", ignoreCase = true)
                }

        return if (hasValidRoute) {
            stops.last()
        } else {
            "Not assigned"
        }
    }

    private fun filterArrivals() {

        val query = binding.inputSearchArrivals.text
            .toString()
            .trim()

        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }

        val today = dateFormat.format(Date())

        val timeFormat = SimpleDateFormat(
            "hh:mm a",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
            isLenient = false
        }

        // Display only today's arrivals
        val todayArrivals = allArrivalLogs.filter { log ->
            log.recordedAt > 0 &&
                    dateFormat.format(Date(log.recordedAt)) == today
        }

        // Latest arrival first
        val sortedArrivals = todayArrivals.sortedWith(
            compareByDescending<MovementLogEntity> { log ->
                runCatching {
                    timeFormat.parse(log.movementTime.trim())?.time
                        ?: Long.MIN_VALUE
                }.getOrDefault(Long.MIN_VALUE)
            }.thenByDescending { it.recordedAt }
        )

        // Keep only the latest arrival of each bus
        val uniqueArrivals = sortedArrivals.distinctBy {
            it.busPlate.trim().uppercase(Locale.ROOT)
        }

        // Apply terminal filter based on latest recorded arrival terminal
        val terminalFiltered = if (selectedTerminal.isNullOrBlank()) {
            uniqueArrivals
        } else {
            uniqueArrivals.filter { log ->
                log.terminal.equals(selectedTerminal, ignoreCase = true) ||
                        log.originOrDestination.contains(selectedTerminal!!, ignoreCase = true)
            }
        }

        // Apply search
        val filteredArrivals = terminalFiltered.filter { log ->

            log.busPlate.contains(query, ignoreCase = true) ||
                    log.originOrDestination.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    getBusDestination(log.busPlate).contains(
                        query,
                        ignoreCase = true
                    )
        }

        renderMovements(
            binding.containerArrivalsList,
            filteredArrivals,
            true
        )
    }

    private fun renderMovements(
        container: LinearLayout,
        logs: List<MovementLogEntity>,
        isArrival: Boolean
    ) {
        container.removeAllViews()

        if (logs.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = if (binding.inputSearchArrivals.text.isNullOrBlank()) {
                    "No bus arrivals recorded today."
                } else {
                    "No matching bus arrivals found today."
                }
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 32, 0, 32)
            }
            container.addView(emptyText)
            return
        }

        for (log in logs) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 36, 40, 36)
                background = getDrawable(R.drawable.bg_card)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 24
                }
                layoutParams = params
            }

            val plateText = TextView(this).apply {
                text = log.busPlate
                textSize = 17f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val routeText = TextView(this).apply {
                text = log.terminal.ifBlank { log.originOrDestination }
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                }
                layoutParams = p
            }

            val statusText = TextView(this).apply {
                text = "ARRIVED"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.status_available))

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                }
            }

            val destinationText = TextView(this).apply {
                text = "To ${getBusDestination(log.busPlate)}"
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                }
            }

            val timeText = TextView(this).apply {
                text = log.movementTime
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                }
            }

            card.addView(plateText)
            card.addView(statusText)
            card.addView(routeText)
            card.addView(destinationText)
            card.addView(timeText)

            card.isClickable = true
            card.isFocusable = true

            card.setOnClickListener {
                val intent = Intent(this, TripDetailActivity::class.java)
                intent.putExtra("BUS_PLATE", log.busPlate)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            container.addView(card)
        }
    }
}
