package com.ishant.bikerush.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishant.bikerush.db.Journey
import com.ishant.bikerush.repositories.BikeRushRepository
import kotlinx.coroutines.launch

class BikeRushViewModel @ViewModelInject constructor(val bikeRushRepository: BikeRushRepository): ViewModel() {

    fun insertJourney(journey: Journey) = viewModelScope.launch {
        bikeRushRepository.upsertJourney(journey)
    }

    fun deleteJourney(journey: Journey) = viewModelScope.launch {
        bikeRushRepository.deleteJourney(journey)
    }

    val journeyList = bikeRushRepository.getAllJourneys("dateCreated")

}