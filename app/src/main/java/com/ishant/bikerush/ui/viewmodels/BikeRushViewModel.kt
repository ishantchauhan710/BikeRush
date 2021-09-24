package com.ishant.bikerush.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.ishant.bikerush.repositories.BikeRushRepository

class BikeRushViewModel @ViewModelInject constructor(val bikeRushRepository: BikeRushRepository): ViewModel() {

}