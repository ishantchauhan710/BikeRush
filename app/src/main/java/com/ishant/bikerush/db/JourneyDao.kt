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

    @Query("SELECT SUM(duration) FROM journey")
    fun getTotalDuration(): LiveData<Int>

    @Query("SELECT SUM(distance) FROM journey")
    fun getTotalDistance(): LiveData<Int>

    @Query("SELECT AVG(speed) FROM journey")
    fun getAvgSpeed(): LiveData<Float>




}