package com.ishant.bikerush.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// This class creates the database of our journey into the device
// Since we are using dagger hilt, we don't need to create the createDatabase() function in this class. Instead, we can create that function in the modules.
@Database(entities = [Journey::class], version = 1)
@TypeConverters(Converters::class)
abstract class JourneyDatabase: RoomDatabase() {
    abstract fun getJourneyDao(): JourneyDao
}