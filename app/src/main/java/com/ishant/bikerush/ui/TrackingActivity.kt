package com.ishant.bikerush.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions
import com.ishant.bikerush.R
import com.ishant.bikerush.databinding.ActivityTrackingBinding
import com.ishant.bikerush.other.Constants.ACTION_PAUSE_SERVICE
import com.ishant.bikerush.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.ishant.bikerush.other.Constants.MAP_ZOOM
import com.ishant.bikerush.other.Constants.POLYLINE_COLOR
import com.ishant.bikerush.other.Constants.POLYLINE_WIDTH
import com.ishant.bikerush.services.Polyline
import com.ishant.bikerush.services.Polylines
import com.ishant.bikerush.services.TrackingService

class TrackingActivity : AppCompatActivity() {

    lateinit var binding: ActivityTrackingBinding

    // Google Map Instance
    private var map: GoogleMap? = null

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

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
            //sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            toggleRun()
        }



    }

    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size>1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLng = pathPoints.last().last()

            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)

            map?.addPolyline(polylineOptions)

        }
    }

    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun moveCameraToUser() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnStartService.text = "Tracking is false"
        } else {
            binding.btnStartService.text = "Tracking is true"
        }
    }

    private fun toggleRun() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(this, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(this, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()

        })

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