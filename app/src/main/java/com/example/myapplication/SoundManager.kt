package com.example.myapplication

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class SoundManager private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SoundManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun playInsectScream() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.insect_scream).apply {
                setOnCompletionListener {
                    release()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка воспроизведения звука: ${e.message}")
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}