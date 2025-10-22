package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val gender: String,
    val course: Int,
    val difficulty: Int,
    val birthDate: Long,
    val zodiacSign: String,
    val createdAt: Long = System.currentTimeMillis()
)