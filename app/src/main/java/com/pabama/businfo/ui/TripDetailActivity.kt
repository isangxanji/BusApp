package com.pabama.businfo.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
import com.pabama.businfo.databinding.ActivityTripDetailBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import com.pabama.businfo.data.entity.MovementLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTripDetailBinding.inflate(layoutInflater)
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

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // Expand or collapse Route History
        binding.btnToggleRouteHistory.setOnClickListener {

            val isExpanded =
                binding.containerRouteHistory.visibility == View.VISIBLE

            if (isExpanded) {
                binding.containerRouteHistory.visibility = View.GONE
                binding.btnToggleRouteHistory.text = "Route History   ▼"
            } else {
                binding.containerRouteHistory.visibility = View.VISIBLE
                binding.btnToggleRouteHistory.text = "Route History   ▲"
            }
        }

        val busPlate = intent.getStringExtra("BUS_PLATE") ?: ""

        // Load route history for the selected bus
        viewModel.arrivals.observe(this) { arrivals ->

            val busHistory = arrivals.filter { log ->
                busPlate.isNotBlank() &&
                        log.busPlate.trim().equals(
                            busPlate.trim(),
                            ignoreCase = true
                        )
            }.sortedByDescending { it.recordedAt }

            renderRouteHistory(busHistory)
        }

        // Load bus details from Room DB
        lifecycleScope.launch {
            val bus = if (busPlate.isNotBlank()) viewModel.getBusByPlate(busPlate) else null
            binding.containerRouteWaypoints.removeAllViews()

            if (bus != null) {
                binding.txtDetailBusPlate.text = bus.plateNumber
                binding.txtDetailBusType.text = bus.busType
                StatusBadge.setStatus(binding.txtDetailBusStatus, bus.status)
                binding.txtDetailDestination.text = bus.destination

                // Dynamically display matching route stops
                val stops = bus.routeDescription.split("→", "->", "—")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (stops.isNotEmpty()) {
                    for (i in stops.indices) {
                        val stopView = TextView(this@TripDetailActivity).apply {
                            text = when {
                                stops.size == 1 -> "●  ${stops[i]}"
                                i == 0 -> "●  ${stops[i]} (Origin)"
                                i == stops.lastIndex -> "●  ${stops[i]} (Destination)"
                                else -> "○  ${stops[i]}"
                            }
                            setTextColor(getColor(R.color.text_primary))
                            textSize = 15f
                        }
                        binding.containerRouteWaypoints.addView(stopView)

                        if (i < stops.size - 1) {
                            val lineView = TextView(this@TripDetailActivity).apply {
                                text = "│"
                                setTextColor(getColor(R.color.accent))
                                setPadding(14, 4, 14, 4)
                                textSize = 17f
                            }
                            binding.containerRouteWaypoints.addView(lineView)
                        }
                    }
                } else {
                    val noStops = TextView(this@TripDetailActivity).apply {
                        text = "No route stops listed."
                        setTextColor(getColor(R.color.text_secondary))
                        textSize = 14f
                    }
                    binding.containerRouteWaypoints.addView(noStops)
                }
            } else {
                binding.txtDetailBusPlate.text = if (busPlate.isNotBlank()) busPlate else "Bus Not Found"
                binding.txtDetailBusType.text = "-"
                binding.txtDetailDestination.text = "No route details available"
                StatusBadge.setStatus(binding.txtDetailBusStatus, "UNAVAILABLE")
                val emptyMsg = TextView(this@TripDetailActivity).apply {
                    text = "No trip or route stops found for this bus."
                    setTextColor(getColor(R.color.text_secondary))
                    textSize = 14f
                }
                binding.containerRouteWaypoints.addView(emptyMsg)
            }
        }
    }

    private fun renderRouteHistory(logs: List<MovementLogEntity>) {

        binding.containerRouteHistory.removeAllViews()

        if (logs.isEmpty()) {

            val emptyText = TextView(this).apply {
                text = "No route history recorded yet."
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
            }

            binding.containerRouteHistory.addView(emptyText)
            return
        }

        val dateFormat = SimpleDateFormat(
            "MMMM dd, yyyy",
            Locale.US
        )

        logs.forEachIndexed { index, log ->

            // Latest recorded arrival label
            if (index == 0) {

                val latestLabel = TextView(this).apply {
                    text = "LATEST RECORDED ARRIVAL"
                    textSize = 11f
                    setTypeface(
                        null,
                        android.graphics.Typeface.BOLD
                    )
                    setTextColor(getColor(R.color.status_available))
                    setPadding(0, 16, 0, 4)
                }

                binding.containerRouteHistory.addView(latestLabel)
            }

            // Existing location information
            val locationText = TextView(this).apply {
                text = log.originOrDestination
                textSize = 15f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
                setPadding(0, 12, 0, 4)
            }

            // Manually selected arrival time
            val timeText = TextView(this).apply {
                text = "Arrived • ${log.movementTime}"
                textSize = 13f
                setTextColor(getColor(R.color.status_available))
                setPadding(0, 0, 0, 4)
            }

            // Automatically recorded date
            val recordedDate = if (log.recordedAt > 0L) {
                dateFormat.format(Date(log.recordedAt))
            } else {
                "Date unavailable"
            }

            val dateText = TextView(this).apply {
                text = "Recorded on: $recordedDate"
                textSize = 12f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 0, 0, 16)
            }

            binding.containerRouteHistory.addView(locationText)
            binding.containerRouteHistory.addView(timeText)
            binding.containerRouteHistory.addView(dateText)

            // Divider between records
            if (index < logs.lastIndex) {

                val divider = View(this).apply {

                    layoutParams =
                        android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )

                    setBackgroundColor(getColor(R.color.border))
                }

                binding.containerRouteHistory.addView(divider)
            }
        }
    }
}
