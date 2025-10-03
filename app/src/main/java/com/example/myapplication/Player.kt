package com.example.myapplication

data class Player(
    val fullName: String,
    val gender: String,
    val course: Int,           // Int для курса
    val difficulty: Int,       // Int для сложности
    val birthDate: Long,       // Long для даты
    val zodiacSign: String     // String для знака зодиака
)