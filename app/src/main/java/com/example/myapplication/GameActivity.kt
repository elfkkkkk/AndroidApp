package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var gameContainer: FrameLayout
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView

    private var score = 0
    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var roundDuration = 60

    private val activeCockroaches = mutableListOf<ImageView>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var countDownTimer: CountDownTimer

    private val insectDrawables = listOf(
        R.drawable.cockroach1,
        R.drawable.cockroach2,
        R.drawable.beetle1,
        R.drawable.beetle2
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Получаем настройки из Intent
        gameSpeed = intent.getIntExtra("game_speed", 5)
        maxCockroaches = intent.getIntExtra("max_cockroaches", 10)
        roundDuration = intent.getIntExtra("round_duration", 60) * 1000 // в миллисекунды

        // Инициализация View
        gameContainer = findViewById(R.id.gameContainer)
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)

        startGame()
    }

    private fun startGame() {
        score = 0
        updateScore()

        countDownTimer = object : CountDownTimer(roundDuration.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerTextView.text = "Время: $secondsLeft сек"
            }

            override fun onFinish() {
                endGame()
            }
        }.start()

        startInsectSpawning()
    }

    private fun startInsectSpawning() {
        val spawnRunnable = object : Runnable {
            override fun run() {
                if (activeCockroaches.size < maxCockroaches) {
                    spawnInsect()
                }
                handler.postDelayed(this, calculateSpawnInterval())
            }
        }
        handler.post(spawnRunnable)
    }

    private fun calculateSpawnInterval(): Long {
        return (2000 - (gameSpeed * 150)).toLong()
    }

    private fun spawnInsect() {
        val insectView = ImageView(this).apply {
            val randomInsect = insectDrawables.random()
            setImageResource(randomInsect)
            layoutParams = FrameLayout.LayoutParams(120, 120)

            // Случайная позиция на экране
            val containerWidth = gameContainer.width
            val containerHeight = gameContainer.height

            if (containerWidth > 0 && containerHeight > 0) {
                x = Random.nextFloat() * (containerWidth - 120)
                y = Random.nextFloat() * (containerHeight - 120)
            } else {
                x = 100f
                y = 100f
            }

            // Анимация появления
            alpha = 0f
            animate().alpha(1f).duration = 500
        }

        // Добавляем обработчик касания
        insectView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                killInsect(insectView, true)
                true
            } else {
                false
            }
        }

        gameContainer.addView(insectView)
        activeCockroaches.add(insectView)

        startInsectMovement(insectView)
    }

    private fun startInsectMovement(insect: ImageView) {
        val moveRunnable = object : Runnable {
            override fun run() {
                if (activeCockroaches.contains(insect)) {
                    moveInsect(insect)
                    handler.postDelayed(this, 50) // Частота обновления движения
                }
            }
        }
        handler.post(moveRunnable)
    }

    private fun moveInsect(insect: ImageView) {
        val speed = gameSpeed * 2 + Random.nextInt(-2, 3)
        val directionX = Random.nextInt(-1, 2)
        val directionY = Random.nextInt(-1, 2)

        val containerWidth = gameContainer.width
        val containerHeight = gameContainer.height

        var newX = insect.x + directionX * speed
        var newY = insect.y + directionY * speed

        // Проверка границ экрана
        if (newX < 0) newX = 0f
        if (newX > containerWidth - insect.width) newX = (containerWidth - insect.width).toFloat()
        if (newY < 0) newY = 0f
        if (newY > containerHeight - insect.height) newY = (containerHeight - insect.height).toFloat()

        insect.x = newX
        insect.y = newY
    }

    private fun killInsect(insect: ImageView, isHit: Boolean) {
        if (isHit) {
            // Попадание - начисляем очки
            score += 10
            updateScore()
        } else {
            // Промах - штраф
            score -= 5
            if (score < 0) score = 0
            updateScore()
        }

        // Анимация уничтожения
        ObjectAnimator.ofFloat(insect, "scaleX", 1f, 0f).apply {
            duration = 200
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    gameContainer.removeView(insect)
                    activeCockroaches.remove(insect)
                }
            })
            start()
        }
        ObjectAnimator.ofFloat(insect, "scaleY", 1f, 0f).setDuration(200).start()
    }

    private fun updateScore() {
        scoreTextView.text = "Очки: $score"
    }

    private fun endGame() {
        handler.removeCallbacksAndMessages(null)
        countDownTimer.cancel()

        // Удаляем всех насекомых
        activeCockroaches.forEach { insect ->
            gameContainer.removeView(insect)
        }
        activeCockroaches.clear()

        showGameOverDialog()
    }

    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("Ваши очки: $score")
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }
}