package com.ishant.bikerush.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ishant.bikerush.R
import com.ishant.bikerush.adapters.JourneyAdapter
import com.ishant.bikerush.databinding.FragmentJourneyBinding
import com.ishant.bikerush.db.Journey
import com.ishant.bikerush.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.ishant.bikerush.other.TrackingUtility
import com.ishant.bikerush.ui.viewmodels.BikeRushViewModel
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

// This fragment will display all the journeys tracked using our app
@AndroidEntryPoint
class JourneyFragment : Fragment(R.layout.fragment_journey), EasyPermissions.PermissionCallbacks {

    val viewModel: BikeRushViewModel by viewModels()

    lateinit var binding: FragmentJourneyBinding
    lateinit var adapter: JourneyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentJourneyBinding.bind(view)
        requestPermissions()

        adapter = JourneyAdapter()

        binding.rvJourney.adapter = adapter
        binding.rvJourney.layoutManager = LinearLayoutManager(requireContext())

        viewModel.journeyList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        adapter.setOnItemClickListener {
            deleteJourney(it)
        }

    }

    // Function to delete a journey
    private fun deleteJourney(journey: Journey) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this journey")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { d,_ ->
                viewModel.deleteJourney(journey)
                d.cancel()
            }
            .setNegativeButton("No") { d,_ -> d.cancel() }
            .create()

        dialog.show()
    }


    // Function to request location permissions
    private fun requestPermissions() {
        if(TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }

        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.O) {

            EasyPermissions.requestPermissions(this,
                "You need to accept location permissions to use this app",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        } else {
            EasyPermissions.requestPermissions(this,
            "You need to accept location permissions to use this app",
            REQUEST_CODE_LOCATION_PERMISSION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        }

    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // When any of the permission is permanently denied, open the android app settings
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }


}