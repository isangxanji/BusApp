package com.pabama.businfo.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
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

class TripUpdatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripUpdatesBinding
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

        // Arrivals / Departures toggle
        binding.btnArrivals.setOnClickListener {
            showArrivalsTab()
        }

        binding.btnDepartures.setOnClickListener {
            showDeparturesTab()
        }

        // Observe Live Arrivals
        viewModel.arrivals.observe(this) { arrivals ->
            renderMovements(binding.containerArrivalsList, arrivals, true)
        }

        // Observe Live Departures
        viewModel.departures.observe(this) { departures ->
            renderMovements(binding.containerDeparturesList, departures, false)
        }

        showArrivalsTab()
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupPassengerBottomNav(this, "UPDATES")
    }

    private fun showArrivalsTab() {
        val activeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(getColor(R.color.primary))
        }
        binding.btnArrivals.background = activeBg
        binding.btnArrivals.setTextColor(getColor(R.color.surface))

        binding.btnDepartures.setBackgroundColor(Color.TRANSPARENT)
        binding.btnDepartures.setTextColor(getColor(R.color.text_secondary))

        binding.layoutArrivals.visibility = View.VISIBLE
        binding.layoutDepartures.visibility = View.GONE
    }

    private fun showDeparturesTab() {
        val activeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(getColor(R.color.primary))
        }
        binding.btnDepartures.background = activeBg
        binding.btnDepartures.setTextColor(getColor(R.color.surface))

        binding.btnArrivals.setBackgroundColor(Color.TRANSPARENT)
        binding.btnArrivals.setTextColor(getColor(R.color.text_secondary))

        binding.layoutArrivals.visibility = View.GONE
        binding.layoutDepartures.visibility = View.VISIBLE
    }

    private fun renderMovements(
        container: LinearLayout,
        logs: List<MovementLogEntity>,
        isArrival: Boolean
    ) {
        container.removeAllViews()

        if (logs.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = if (isArrival) "No arrivals recorded yet." else "No departures recorded yet."
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
                    topMargin = 24
                }
                layoutParams = params
            }

            val plateText = TextView(this).apply {
                text = log.busPlate
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val routeText = TextView(this).apply {
                text = log.originOrDestination
                textSize = 14f
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
                text = "${if (isArrival) "Arrived" else "Departed"} • ${log.movementTime}"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(
                    if (isArrival) getColor(R.color.status_available) else getColor(R.color.status_departed)
                )
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                layoutParams = p
            }

            card.addView(plateText)
            card.addView(routeText)
            card.addView(statusText)

            if (!log.notes.isNullOrBlank()) {
                val notesText = TextView(this).apply {
                    text = "Note: ${log.notes}"
                    textSize = 12f
                    setTextColor(getColor(R.color.text_secondary))
                    val p = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8
                    }
                    layoutParams = p
                }
                card.addView(notesText)
            }

            container.addView(card)
        }
    }
}
