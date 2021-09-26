package com.ishant.bikerush.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.db.williamchart.slidertooltip.SliderTooltip
import com.ishant.bikerush.R
import com.ishant.bikerush.databinding.FragmentStatisticsBinding
import com.ishant.bikerush.other.TrackingUtility
import com.ishant.bikerush.ui.viewmodels.BikeRushViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    val viewModel: BikeRushViewModel by viewModels()
    lateinit var binding: FragmentStatisticsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStatisticsBinding.bind(view)

        // Setting chart view visiblity to gone to avoid not initialized exception. Will set it to visible when we have atleast 2 or more journies in our database list
        binding.lineChart.visibility = View.GONE

        // Line chart gradient color
        binding.lineChart.gradientFillColors =
            intArrayOf(
                Color.parseColor("#8190339B"),
                Color.TRANSPARENT
            )

        // Line chart animation effect duration
        binding.lineChart.animation.duration = 1000L

        // Line chart Tooltip color
        binding.lineChart.tooltip =
            SliderTooltip().also {
                it.color = Color.WHITE
            }

        viewModel.journeyList.observe(viewLifecycleOwner, Observer { journeyList ->

            if (journeyList != null && journeyList.size>=2) {

                binding.lineChart.visibility = View.VISIBLE

                // Create an empty mutable list of a pair of string and float
                val lineSet = mutableListOf<Pair<String,Float>>()

                // Add distance and duration as pairs into our list in order to display the chart
                for (journey in journeyList) {
                    lineSet.add(Pair("${journey.distance} Km",journey.duration.toFloat()))
                }

                // When a portion in our chart is clicked, set the text view details according to it's journey data
                binding.lineChart.onDataPointTouchListener = { index, _, _ ->

                    // Get the journey from our list where chart's clicked portion journey's duration matches in our mobile database journey list's journey's duration
                    val jrny = journeyList.find { it.duration.toLong() == lineSet[index].second.toLong() }

                    // If we found any result, show that in respective textviews
                    jrny?.let { journey ->
                        binding.tvSpeed.text = "${journey.speed} kmh"
                        binding.tvDistance.text = "${journey.distance} km"
                        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                        binding.tvTime.text = dateFormat.format(journey.dateCreated)
                        binding.tvDuration.text = TrackingUtility.getFormattedStopwatchTime(journey.duration)
                    }
                }
                binding.lineChart.animate(lineSet)
            }
        })
    }
}