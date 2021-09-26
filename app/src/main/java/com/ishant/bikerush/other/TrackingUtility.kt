package com.ishant.bikerush.other

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.ishant.bikerush.services.Polyline
import com.ishant.bikerush.services.Polylines
import pub.devrel.easypermissions.EasyPermissions

object TrackingUtility {

    // Function to check if user has provided location permissions or not
    fun hasLocationPermissions(context: Context) =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    // Functions to convert total seconds to hh:mm:ss format
    fun getFormattedStopwatchTime(sec: Long): String {
        // 7200 seconds
        // 120 minutes
        // 2 hours

        val hours = sec /3600
        val minutes = sec /60%60
        val seconds = sec %60

        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds"

    }

    // Function to get length of one polyline
    fun calculatePolylineLength(polyline: Polyline): Float {
        var distance = 0f
        for(i in 0..polyline.size-2) {
            val result = FloatArray(1)
            val pos1 = polyline[i]
            val pos2 = polyline[i+1]
            Location.distanceBetween(pos1.latitude,pos1.longitude,pos2.latitude,pos2.longitude,result)
            distance += result[0]
        }
        return distance
    }

    // Function to calculate sum of length of multiple polylines
    fun calculateLengthofPolylines(polylines: Polylines): Float {
        var totalDistance = 0f
        for(i in 0..polylines.size-1) {
            totalDistance += calculatePolylineLength(polylines[i])
        }
        return totalDistance
    }



}