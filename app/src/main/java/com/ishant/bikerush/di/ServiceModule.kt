package com.ishant.bikerush.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.ishant.bikerush.R
import com.ishant.bikerush.other.Constants
import com.ishant.bikerush.ui.TrackingActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

// This file contains all the functions we need as long as our application lives
@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    // We need this client to access user location
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context) = FusedLocationProviderClient(context)

    @ServiceScoped
    @Provides
    // This is the activity where our service belongs to
    fun providePendingIntent(@ApplicationContext context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, TrackingActivity::class.java).also {
            it.action = Constants.ACTION_SHOW_TRACKING_ACTIVITY
        }, PendingIntent.FLAG_UPDATE_CURRENT
    )

    @ServiceScoped
    @Provides
    // Building the actual notification that will be displayed
    fun provideBaseNotificationBuilder(@ApplicationContext context: Context, pendingIntent: PendingIntent) = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_bike)
        .setContentTitle("Bike Rush")
        .setContentText("00:00:00")
        .setContentIntent(pendingIntent)

}