package com.ishant.bikerush.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

// This class defines a single journey of our app
@Entity(tableName = "journey")
data class Journey(
    val dateCreated: Long = 0L,
    val speed: Float = 0f, // In km/h
    val distance: Int = 0, // In meters
    val duration: Long = 0L, // In milliseconds
    val img: Bitmap ?= null
) {
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null // id is declared inside the body of class, not inside the constructor so that we won't have to assign it every time we create an instance of this class
}