package com.ishant.bikerush.db

import androidx.lifecycle.LiveData
import androidx.room.*

// This interface contains all the functions or operations we will perform on the Journey.kt class
@Dao
interface JourneyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJourney(journey: Journey)

    @Delete
    suspend fun deleteJourney(journey: Journey)

    @Query("SELECT * FROM journey ORDER BY :orderByCriteria DESC")
    fun getAllJourneys(orderByCriteria: String): LiveData<List<Journey>>


}