package com.ishant.bikerush.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ishant.bikerush.R
import com.ishant.bikerush.databinding.ActivityTrackingBinding
import com.ishant.bikerush.db.Journey
import com.ishant.bikerush.other.Constants.ACTION_PAUSE_SERVICE
import com.ishant.bikerush.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.ishant.bikerush.other.Constants.ACTION_STOP_SERVICE
import com.ishant.bikerush.other.Constants.MAP_ZOOM
import com.ishant.bikerush.other.Constants.POLYLINE_COLOR
import com.ishant.bikerush.other.Constants.POLYLINE_WIDTH
import com.ishant.bikerush.other.TrackingUtility
import com.ishant.bikerush.services.Polyline
import com.ishant.bikerush.services.TrackingService
import com.ishant.bikerush.ui.viewmodels.BikeRushViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.round
import java.util.*

@AndroidEntryPoint
class TrackingActivity : AppCompatActivity() {

    // View Binding Variable
    lateinit var binding: ActivityTrackingBinding

    // Google Map Instance
    private var map: GoogleMap? = null

    // Whether we are tracking journey or not
    private var isTracking = false

    // List of all the distance lines in our journey
    private var pathPoints = mutableListOf<Polyline>()

    // Time duration of our service set to 0 initially
    private var curTimeInSeconds = 0L

    // Total distance travelled by user in Km
    private var distance = 0f

    // Average speed of user in Km/h
    private var avgSpeed = 0

    val viewModel: BikeRushViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        subscribeToObservers()

        // Set up mapView
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync {
            map = it // Get map asynchronously and assign the result to our map (google map instance) we created on top
            addAllPolylines()
        }

        binding.btnStartService.setOnClickListener {
            // Start/Resume and Pause our service
            toggleJourney()
        }

        binding.btnSaveJourney.setOnClickListener {
            zoomToSeeWholeTrack()
            endJourneyAndSaveToDb()
        }

    }

    // When back button is pressed, show the dialog box to cancel journey
    override fun onBackPressed() {
        //super.onBackPressed()
        showCancelTrackingDialog()
    }


    // We will use this function to zoom the map such that we see the whole track and take the screenshot of it
    private fun zoomToSeeWholeTrack() {
        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints) {
            for(pos in polyline) {
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                (binding.mapView.height*0.05f).toInt()
            )
        )

    }

    // Function to end our journey and save it to database
    private fun endJourneyAndSaveToDb() {
        map?.snapshot { bmp ->
            var distance = 0
            for(polyline in pathPoints) {
                // We created this function to calculate length of polyline in meters and then we convert it to km
                distance += (TrackingUtility.calculatePolylineLength(polyline).toInt())/1000
            }

            // Current date and time
            val curDate = Calendar.getInstance().timeInMillis

            // Our journey object to be saved in database
            val journey = Journey(curDate,avgSpeed.toLong(),distance,curTimeInSeconds,bmp)

            // Save our journey object to database
            viewModel.insertJourney(journey)

            // We created this function to stop the journey tracking service and end the activity
            stopJourney()
            Toast.makeText(applicationContext,"Journey saved successfully",Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Function to add the last or latest polyline on our map
    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size>1) { // If our distance line is not empty and last polyline contains a start and end point
            val preLastLatLng = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLng = pathPoints.last().last()

            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)

            // Inbuilt function of google maps to add a polyline
            map?.addPolyline(polylineOptions)

        }
    }

    // Function to add all polylines to our map
    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    // Function to move the camera to user
    private fun moveCameraToUser() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera( // Enable animation
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(), // Current position of user
                    MAP_ZOOM // Zoom scale defined in Constants.kt
                )
            )
        }
    }

    // If tracking is set to true in our service, we set our activity's tracking variable to true and vice versa
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnStartService.text = "Start"
        } else {
            binding.btnStartService.text = "Stop"
        }
    }

    // Function to start/resume and pause our service
    private fun toggleJourney() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    // Function to observe all the live data members of our service
    private fun subscribeToObservers() {

        // Update our activity's tracking variable according to the tracking variable of our service
        TrackingService.isTracking.observe(this, Observer {
            updateTracking(it)
        })

        // Whenever a new polyline is added in our service, we receive it in activity, add it to our pathPoints list, update it on our map and move the camera towards that polyline
        TrackingService.pathPoints.observe(this, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
            val distTrack = it
            distance = TrackingUtility.calculateLengthofPolylines(distTrack)
            binding.tvDistance.text = "${round((distance/1000f)*10)/10} km" // Show distance in km


        })

        // We get the time elapsed since our service was started, convert it to hh:mm:ss using the function we created in TrackingUtility.kt and display in textview as it changes
        TrackingService.timeRunInSeconds.observe(this, Observer {
            curTimeInSeconds = it
            val formattedTime = TrackingUtility.getFormattedStopwatchTime(curTimeInSeconds)
            binding.tvTime.text = formattedTime // Show time in hh:mm:ss

            // s=d/t
            avgSpeed = round(((distance/curTimeInSeconds)*(3600/1000))*10)/10 // Show speed in km/h
            binding.tvSpeed.text = "$avgSpeed kmh"

            if(curTimeInSeconds>0L) {
                binding.btnSaveJourney.visibility = View.VISIBLE
            }

        })

    }

    // Function to show the cancel tracking dialog when user presses back button
    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Cancel the Journey")
            .setMessage("Are you sure you want to cancel this journey")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _,_ -> stopJourney() }
            .setNegativeButton("No") { d,_ -> d.cancel() }
            .create()

        dialog.show()
    }

    // Function to stop our journey
    private fun stopJourney() {
        sendCommandToService(ACTION_STOP_SERVICE)
        finish()
    }

    // This function will start our service and send an action to it
    private fun sendCommandToService(action: String) = Intent(this,TrackingService::class.java).also {
        it.action = action
        startService(it)
    }


    // Following are the functions to handle the lifecycle of our map. Removing these functions may cause the app to crash.
    override fun onResume() {
        super.onResume()
        binding.mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView?.onSaveInstanceState(outState)
    }


}