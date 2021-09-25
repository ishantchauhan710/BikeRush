package com.ishant.bikerush.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.ishant.bikerush.R
import com.ishant.bikerush.other.Constants.ACTION_PAUSE_SERVICE
import com.ishant.bikerush.other.Constants.ACTION_SHOW_TRACKING_ACTIVITY
import com.ishant.bikerush.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.ishant.bikerush.other.Constants.ACTION_STOP_SERVICE
import com.ishant.bikerush.other.Constants.FASTEST_LOCATION_INTERVAL
import com.ishant.bikerush.other.Constants.LOCATION_UPDATE_INTERVAL
import com.ishant.bikerush.other.Constants.NOTIFICATION_CHANNEL_ID
import com.ishant.bikerush.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.ishant.bikerush.other.Constants.NOTIFICATION_ID
import com.ishant.bikerush.other.TrackingUtility
import com.ishant.bikerush.ui.BikeRushActivity
import com.ishant.bikerush.ui.TrackingActivity
import timber.log.Timber

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackingService: LifecycleService() {

    // This variable will help us know when to start and when to pause / resume our service
    var isFirstJourney = true

    // This will provide us the current location of user
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>() // This is the path where user has travelled
    }

    // This function is called whenever our service is created
    override fun onCreate() {
        super.onCreate()

        postInitialValues() // Function to post empty values to our live data. (We created this function at bottom).

        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            updateLocationTracking(it) // Function to update location of user when tracking is set to true. (We created this function at bottom).
        })
    }

    // This function is called whenever a command is received
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstJourney) {
                        startForegroundService() // We created this function at bottom
                        isFirstJourney = false
                        //Toast.makeText(this,"Service Started",Toast.LENGTH_SHORT).show()
                    } else {
                        startForegroundService()
                        //Toast.makeText(this,"Service Resumed",Toast.LENGTH_SHORT).show()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {  }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun pauseService() {
        isTracking.postValue(false)
    }

    // Function to create notification channel to provide metadata to notification
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID,NOTIFICATION_CHANNEL_NAME,IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    // Function to start our foreground service
    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager) // Notification channel created using the function we created above
        }

        // Building the actual notification that will be displayed
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_bike)
            .setContentTitle("Bike Rush")
            .setContentText("00:00:00")
            .setContentIntent(getPendingIntent())

        addEmptyPolyline() // A function to add empty polyline when the tracking is paused and then resumed at a different location. (We created this function at bottom).
        isTracking.postValue(true)

        startForeground(NOTIFICATION_ID,notificationBuilder.build()) // Start our service as a foreground service
    }

    // This is the activity where our service belongs to
    private fun getPendingIntent() = PendingIntent.getActivities(this,0,
        arrayOf(Intent(this,TrackingActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_ACTIVITY
        }),FLAG_UPDATE_CURRENT)

    // Function to post empty valus to our live data members
    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    // Function to add an empty polyline to our data members when there is a pause and resume distance gap between two locations
    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    // Function to add the location points of user
    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude,location.longitude) // Get latitudes and longitudes of user's current location
            pathPoints.value?.apply {
                last().add(pos) // Add the position to end of our pathPoints variable
                pathPoints.postValue(this)
            }
        }
    }

    // This callback will be called whenever location of user changes
    private val locationCallback = object: LocationCallback() {

        // When a new location is received
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if(isTracking.value!!) {
                result?.locations?.let { locations ->
                    for(location in locations) {
                        addPathPoint(location) // The resulted location will be added to our pathPoints variable
                        //Timber.d("Current User Location: ${location.latitude} ${location.longitude}")
                        Toast.makeText(this@TrackingService,"Current User Location: ${location.latitude} ${location.longitude}",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

    // The Actual Function to get the current location of user
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if(isTracking) { // When tracking is set to true
            if(TrackingUtility.hasLocationPermissions(this)) { // We created this function in TrackingUtility.kt. This function checks for location permissions.
                val request = LocationRequest().apply { // Location request instance created
                    interval = LOCATION_UPDATE_INTERVAL // Variable defined in Constants.kt
                    fastestInterval = FASTEST_LOCATION_INTERVAL // Variable defined in Constants.kt
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(request,locationCallback, Looper.getMainLooper()) // Get the current location of user using the fused location provider client
            } else {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }
    }

}