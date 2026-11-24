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

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        val busPlate = intent.getStringExtra("BUS_PLATE") ?: ""

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
                            text = "●  ${stops[i]}"
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
}
