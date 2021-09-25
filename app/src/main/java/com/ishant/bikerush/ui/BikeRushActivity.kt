package com.ishant.bikerush.ui

import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.ishant.bikerush.R
import com.ishant.bikerush.databinding.ActivityBikerushBinding
import com.ishant.bikerush.other.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BikeRushActivity : AppCompatActivity() {

    // View Binding Variable
    lateinit var binding: ActivityBikerushBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBikerushBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // This is the function we created at bottom
        startTrackingActivityIfNeeded(intent)

        // Bottom Navigation Setup
        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.menu.getItem(1).isEnabled = false

        // Connecting bottom navigation view with navController
        val navController = findNavController(R.id.fragment)
        binding.bottomNavigationView.setupWithNavController(navController)

        // When fab button is clicked, start Tracking Activity
        binding.btnNewJourney.setOnClickListener {
            val intent = Intent(this,TrackingActivity::class.java)
            startActivity(intent)
        }


    }

    // In case our service is running and user closes app and clicks on notification, we want the tracking activity to be started. We can do it using this function
    private fun startTrackingActivityIfNeeded(intent: Intent?) {
        if(intent?.action== Constants.ACTION_SHOW_TRACKING_ACTIVITY) {
            val trackingActivityIntent = Intent(this,TrackingActivity::class.java)
            startActivity(trackingActivityIntent)
        }
    }

}