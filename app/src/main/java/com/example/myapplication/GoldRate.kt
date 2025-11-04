package com.example.myapplication

data class GoldRateResponse(
    val record: GoldRateRecord? = null
)

data class GoldRateRecord(
    val value: String = ""
) {
    fun getGoldPrice(): Double {
        return try {
            value.replace(",", ".").toDouble()
        } catch (e: Exception) {
            5000.0
        }
    }
}