package com.ishant.bikerush.services

import android.annotation.SuppressLint
import android.app.Notification
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    // This variable will help us know when to start and when to pause / resume our service
    var isFirstJourney = true

    // Timer variables

    // Timer enabled or not
    private var isTimerEnabled = false

    // When our setTimer() function is called, we will store the current time in this variable
    private var timeStarted = 0L // Time when our service was started

    // This is the time of one single lap that happens when setTimer() is called and paused
    private var lapTime = 0L

    // This is the total time our journey has been running
    private var timeRun = 0L

    // This variable will tell whether our service was killed or not
    private var serviceKilled = false

    @Inject   // This will provide us the current location of user
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject // This is the base notification that will contain title, time and icon
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    // This is the current notification that will contain the action text and action to be performed (pause or resume)
    lateinit var curNotificationBuilder: NotificationCompat.Builder


    companion object {
        val isTracking = MutableLiveData<Boolean>() // Whether we want to track our user or not
        val pathPoints =
            MutableLiveData<Polylines>() // This is the list of paths or lines where user has travelled
        val timeRunInSeconds =
            MutableLiveData<Long>() // Total time elapsed since our service was started or resumed
    }

    // This function is called whenever our service is created
    override fun onCreate() {
        super.onCreate()

        postInitialValues() // Function to post empty values to our live data. (We created this function at bottom).

        // Initially we set curNotificationBuilder to baseNotificationBuilder to avoid lateinit not initialized exception
        curNotificationBuilder = baseNotificationBuilder

        isTracking.observe(this, Observer {
            // Function to get location of user when tracking is set to true and save it to pathPoints variable. (We created this function at bottom).
            updateLocationTracking(it)

            // Function to update the notification whenever we are tracking. (We created this function at bottom).
            updateNotificationTrackingState(it)
        })
    }

    // This function is called whenever a command is received
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstJourney) {
                        // Function to start this service. (We created this function at bottom).
                        startForegroundService()
                        isFirstJourney = false

                        //Toast.makeText(this,"Service Started",Toast.LENGTH_SHORT).show()
                    } else {
                        // When we resume our service, we only want to continue the timer instead of restarting entire service.
                        startTimer()

                        //Toast.makeText(this,"Service Resumed",Toast.LENGTH_SHORT).show()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService() // Function to pause our service. (We created this function at bottom).
                }
                ACTION_STOP_SERVICE -> {
                    killService() // Function to stop or end our service. (We created this function at bottom).
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    // Function to kill our service
    private fun killService() {
        serviceKilled = true
        isFirstJourney = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    // Function to pause our service
    private fun pauseService() {
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    // Function to create notification channel to provide metadata to notification
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    // Function to start our foreground service
    private fun startForegroundService() {

        // Get notification manager service and create notification channel to store notification metadata if android version >= Oreo
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager) // Notification channel created using the function we created above
        }

        // As our service is starting, we set the isTracking value to true and also start the stopwatch timer
        isTracking.postValue(true)

        // Function to start the stopwatch. (We created this function).
        startTimer()

        // Start our service as a foreground service
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        // As time changes and our service is running, we update our notification's content
        timeRunInSeconds.observe(this, Observer {
            if(!serviceKilled) {
                val notification = curNotificationBuilder.setContentText(TrackingUtility.getFormattedStopwatchTime(it))
                notificationManager.notify(NOTIFICATION_ID,notification.build())
            }
        })

    }

    // Function to update our notification
    private fun updateNotificationTrackingState(isTracking: Boolean) {

        // Set notification action text
        val notificationActionText = if(isTracking) "Pause" else "Resume"

        // Set intent with action according to isTracking variable
        val pendingIntent = if(isTracking) {
            val pauseIntent = Intent(this,TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this,1,pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this,TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this,2,resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // This piece of code helps in clearing the previous actions
        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder,ArrayList<NotificationCompat.Action>())
        }

        // When our service is running, we notify the notification with the required data
        if(!serviceKilled) {
            curNotificationBuilder = baseNotificationBuilder.addAction(R.drawable.ic_bike,notificationActionText,pendingIntent)
            notificationManager.notify(NOTIFICATION_ID,curNotificationBuilder.build())
        }

    }


    // Function to initialize / post empty valus to our live data members
    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
    }

    // Function to add an empty polyline to our data members when there is a pause and resume distance gap between two locations
    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    // Function to add the location points of user
    private fun addPathPoint(location: Location?) {
        location?.let {
            // Get latitudes and longitudes of user's current location
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                // Add the position to end of our pathPoints variable
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    // This callback will be called whenever location of user changes
    private val locationCallback = object : LocationCallback() {

        // When a new location is received
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                result?.locations?.let { locations ->
                    for (location in locations) {
                        // The resulted location will be added to our pathPoints variable
                        addPathPoint(location)

                        //Timber.d("Current User Location: ${location.latitude} ${location.longitude}")
                        /*Toast.makeText(
                            this@TrackingService,
                            "Current User Location: ${location.latitude} ${location.longitude}",
                            Toast.LENGTH_SHORT
                        ).show()*/
                    }
                }
            }
        }

    }

    // The Actual Function to get the current location of user
    @SuppressLint("MissingPermission") // Since we used easy permissions library, we can use @SupressLint to hide this warning
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) { // When tracking is set to true
            if (TrackingUtility.hasLocationPermissions(this)) { // This function checks for location permissions. (We created this function in TrackingUtility.kt).
                val request = LocationRequest().apply { // Location request instance created
                    interval = LOCATION_UPDATE_INTERVAL // Variable defined in Constants.kt
                    fastestInterval = FASTEST_LOCATION_INTERVAL // Variable defined in Constants.kt
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                ) // Get the current location of user using the fused location provider client
            } else {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // Function to start the stopwatch / timer
    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInSeconds.postValue((timeRun + lapTime)/1000)
                delay(1000)
            }
            // Add the lap time to total time
            timeRun += lapTime
        }
    }



}