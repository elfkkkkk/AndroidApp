package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE)

    // Ключи для настроек
    companion object {
        private const val KEY_GAME_SPEED = "game_speed"
        private const val KEY_MAX_COCKROACHES = "max_cockroaches"
        private const val KEY_BONUS_INTERVAL = "bonus_interval"
        private const val KEY_ROUND_DURATION = "round_duration"
    }

    // Сохранение настроек
    fun saveGameSpeed(speed: Int) {
        sharedPreferences.edit().putInt(KEY_GAME_SPEED, speed).apply()
    }

    fun saveMaxCockroaches(count: Int) {
        sharedPreferences.edit().putInt(KEY_MAX_COCKROACHES, count).apply()
    }

    fun saveBonusInterval(interval: Int) {
        sharedPreferences.edit().putInt(KEY_BONUS_INTERVAL, interval).apply()
    }

    fun saveRoundDuration(duration: Int) {
        sharedPreferences.edit().putInt(KEY_ROUND_DURATION, duration).apply()
    }

    // Загрузка настроек
    fun getGameSpeed(): Int {
        return sharedPreferences.getInt(KEY_GAME_SPEED, 5) // Значение по умолчанию: 5
    }

    fun getMaxCockroaches(): Int {
        return sharedPreferences.getInt(KEY_MAX_COCKROACHES, 10) // Значение по умолчанию: 10
    }

    fun getBonusInterval(): Int {
        return sharedPreferences.getInt(KEY_BONUS_INTERVAL, 10) // Значение по умолчанию: 10
    }

    fun getRoundDuration(): Int {
        return sharedPreferences.getInt(KEY_ROUND_DURATION, 60) // Значение по умолчанию: 60
    }

    // Получение всех настроек
    fun getAllSettings(): GameSettings {
        return GameSettings(
            gameSpeed = getGameSpeed(),
            maxCockroaches = getMaxCockroaches(),
            bonusInterval = getBonusInterval(),
            roundDuration = getRoundDuration()
        )
    }
}

// Модель для хранения всех настроек
data class GameSettings(
    val gameSpeed: Int,
    val maxCockroaches: Int,
    val bonusInterval: Int,
    val roundDuration: Int
)