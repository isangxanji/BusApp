package com.pabama.businfo.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pabama.businfo.PabamaApplication
import com.pabama.businfo.R
import com.pabama.businfo.data.entity.BusEntity
import com.pabama.businfo.databinding.ActivityBusInformationBinding
import com.pabama.businfo.ui.components.StatusBadge
import com.pabama.businfo.ui.viewmodel.MainViewModel
import com.pabama.businfo.ui.viewmodel.MainViewModelFactory

class BusInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusInformationBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PabamaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBusInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup bottom navigation
        NavigationHelper.setupPassengerBottomNav(this, "BUSES")

        // Search Input listener
        binding.inputSearchBus.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observe Dynamic Buses from Room DB
        viewModel.searchResults.observe(this) { buses ->
            renderBusList(buses)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupPassengerBottomNav(this, "BUSES")
    }

    private fun renderBusList(buses: List<BusEntity>) {
        binding.busListContainer.removeAllViews()

        if (buses.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No buses found matching your search."
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 32, 0, 32)
            }
            binding.busListContainer.addView(emptyText)
            return
        }

        for (bus in buses) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 36, 40, 36)
                background = getDrawable(R.drawable.bg_card)
                isClickable = true
                isFocusable = true

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 24
                }
                layoutParams = params

                setOnClickListener {
                    val intent = Intent(this@BusInformationActivity, TripDetailActivity::class.java).apply {
                        putExtra("BUS_PLATE", bus.plateNumber)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }

            val plateText = TextView(this).apply {
                text = bus.plateNumber
                textSize = 17f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val detailsText = TextView(this).apply {
                text = if (bus.destination.isNotBlank()) "${bus.destination} • ${bus.busType}" else bus.busType
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
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 8, 24, 8)
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 20
                }
                layoutParams = p
            }
            StatusBadge.setStatus(statusText, bus.status)

            card.addView(plateText)
            card.addView(detailsText)

            // Display the bus route
            if (bus.routeDescription.isNotBlank()) {
                val routeText = TextView(this).apply {
                    val stops = bus.routeDescription
                        .split("→", "->", "—")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    text = when {
                        stops.size >= 2 ->
                            "Route: ${stops.first()} to ${stops.last()}"

                        stops.size == 1 ->
                            "Route: ${stops.first()}"

                        else -> "Route not specified"
                    }
                    textSize = 13f
                    setTextColor(getColor(R.color.text_secondary))
                    setPadding(0, 12, 0, 0)
                }

                card.addView(routeText)
            }

            card.addView(statusText)

            // View details label
            val viewDetailsText = TextView(this).apply {
                text = "View Bus Details >"
                textSize = 12f
                setTextColor(getColor(R.color.accent))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 20, 0, 0)
            }

            card.addView(viewDetailsText)

            binding.busListContainer.addView(card)
        }
    }
}
