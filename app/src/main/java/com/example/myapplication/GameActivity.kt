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
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var gameContainer: FrameLayout
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView

    private var score = 0
    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var roundDuration = 60

    private var playerId: Long = -1L
    private var playerName: String = ""
    private lateinit var gameRepository: GameRepository

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

        gameSpeed = intent.getIntExtra("game_speed", 5)
        maxCockroaches = intent.getIntExtra("max_cockroaches", 10)


        val roundDurationSeconds = intent.getIntExtra("round_duration", 60)
        roundDuration = roundDurationSeconds * 1000

        playerId = intent.getLongExtra("player_id", -1L)
        playerName = intent.getStringExtra("player_name") ?: "Unknown"

        Log.d("GameActivity", "Настройки игры:")
        Log.d("GameActivity", " - Скорость: $gameSpeed")
        Log.d("GameActivity", " - Макс. тараканов: $maxCockroaches")
        Log.d("GameActivity", " - Длительность раунда: ${roundDuration / 1000} сек")
        Log.d("GameActivity", " - Игрок: $playerName (ID: $playerId)")

        gameRepository = GameRepository(AppDatabase.getInstance(this))

        gameContainer = findViewById(R.id.gameContainer)
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)

        updateScore()

        startGame()
    }

    private fun startGame() {
        score = 0
        updateScore()

        Log.d("GameActivity", "Запуск игры на ${roundDuration / 1000} секунд")

        countDownTimer = object : CountDownTimer(roundDuration.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerTextView.text = "Время: $secondsLeft сек"
            }

            override fun onFinish() {
                Log.d("GameActivity", "Время вышло! Финальный счет: $score")
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
        val interval = (2000 - (gameSpeed * 150)).toLong()
        Log.d("GameActivity", "Интервал появления: $interval мс")
        return interval
    }

    private fun spawnInsect() {
        val insectView = ImageView(this).apply {
            val randomInsect = insectDrawables.random()
            setImageResource(randomInsect)
            layoutParams = FrameLayout.LayoutParams(120, 120)

            val containerWidth = gameContainer.width
            val containerHeight = gameContainer.height

            if (containerWidth > 0 && containerHeight > 0) {
                x = Random.nextFloat() * (containerWidth - 120)
                y = Random.nextFloat() * (containerHeight - 120)
            } else {
                x = 100f
                y = 100f
            }

            alpha = 0f
            animate().alpha(1f).duration = 500
        }

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

        Log.d("GameActivity", "Появилось насекомое. Всего: ${activeCockroaches.size}")
    }

    private fun startInsectMovement(insect: ImageView) {
        val moveRunnable = object : Runnable {
            override fun run() {
                if (activeCockroaches.contains(insect)) {
                    moveInsect(insect)
                    handler.postDelayed(this, 50)
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

        if (newX < 0) newX = 0f
        if (newX > containerWidth - insect.width) newX = (containerWidth - insect.width).toFloat()
        if (newY < 0) newY = 0f
        if (newY > containerHeight - insect.height) newY = (containerHeight - insect.height).toFloat()

        insect.x = newX
        insect.y = newY
    }

    private fun killInsect(insect: ImageView, isHit: Boolean) {
        if (isHit) {
            score += 10
            Log.d("GameActivity", "Попадание! +10 очков. Текущий счет: $score")
        } else {
            score -= 5
            if (score < 0) score = 0
            Log.d("GameActivity", "Промах! -5 очков. Текущий счет: $score")
        }
        updateScore()

        ObjectAnimator.ofFloat(insect, "scaleX", 1f, 0f).apply {
            duration = 200
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    gameContainer.removeView(insect)
                    activeCockroaches.remove(insect)
                    Log.d("GameActivity", "Насекомое уничтожено. Осталось: ${activeCockroaches.size}")
                }
            })
            start()
        }
        ObjectAnimator.ofFloat(insect, "scaleY", 1f, 0f).setDuration(200).start()
    }

    private fun updateScore() {
        scoreTextView.text = "Игрок: $playerName | Очки: $score"
    }

    private fun endGame() {
        Log.d("GameActivity", "Завершение игры...")
        handler.removeCallbacksAndMessages(null)
        countDownTimer.cancel()

        CoroutineScope(Dispatchers.IO).launch {
            if (playerId != -1L) {
                try {
                    val scoreEntity = ScoreEntity(
                        playerId = playerId,
                        score = score,
                        difficulty = gameSpeed
                    )
                    val scoreId = gameRepository.insertScore(scoreEntity)
                    Log.d("GameActivity", "Результат сохранен в БД. ID записи: $scoreId")

                    val topScores = gameRepository.getTopScoresWithPlayerInfo()
                    topScores.collect { scores ->
                        Log.d("GameActivity", "Топ рекордов в БД: ${scores.size} записей")
                        scores.forEachIndexed { index, scoreRecord ->
                            Log.d("GameActivity", "  ${index + 1}. ${scoreRecord.playerName}: ${scoreRecord.score} очков")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GameActivity", "Ошибка сохранения результата: ${e.message}")
                }
            } else {
                Log.w("GameActivity", "Не удалось сохранить результат: ID игрока невалиден")
            }
        }

        activeCockroaches.forEach { insect ->
            gameContainer.removeView(insect)
        }
        activeCockroaches.clear()

        showGameOverDialog()
    }

    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("Игрок: $playerName\nВаши очки: $score\nДлительность: ${roundDuration / 1000} сек")
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("GameActivity", "onDestroy: очистка ресурсов")
        handler.removeCallbacksAndMessages(null)
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        activeCockroaches.clear()
    }
}