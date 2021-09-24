package com.ishant.bikerush.di

import android.content.Context
import androidx.room.Room
import com.ishant.bikerush.db.JourneyDatabase
import com.ishant.bikerush.other.Constants.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

// This file contains all the functions we need as long as our application lives
@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideJourneyDatabaseInstance(@ApplicationContext context: Context) =
        Room.databaseBuilder(context,JourneyDatabase::class.java,DATABASE_NAME).build()

    @Singleton
    @Provides
    fun provideJourneyDao(db: JourneyDatabase) = db.getJourneyDao()
}