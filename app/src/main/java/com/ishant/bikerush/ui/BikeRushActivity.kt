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

class BikeRushActivity : AppCompatActivity() {

    lateinit var binding: ActivityBikerushBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBikerushBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.menu.getItem(1).isEnabled = false

        val navController = findNavController(R.id.fragment)
        binding.bottomNavigationView.setupWithNavController(navController)

        binding.btnNewJourney.setOnClickListener {
            val intent = Intent(this,TrackingActivity::class.java)
            startActivity(intent)
        }


    }

    private fun floatToDp(float: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, float, Resources.getSystem().displayMetrics)
    }

}