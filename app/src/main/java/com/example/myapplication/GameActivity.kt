package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class GameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var gameContainer: FrameLayout
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView

    private var score = 0
    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var roundDuration = 60
    private var bonusInterval = 15

    private var playerId: Long = -1L
    private var playerName: String = ""
    private lateinit var gameRepository: GameRepository

    private val activeCockroaches = mutableListOf<ImageView>()
    private val activeBonuses = mutableListOf<ImageView>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var bonusTimer: CountDownTimer

    // Система бонусов и акселерометра
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isBonusActive = false
    private var bonusActiveUntil: Long = 0
    private val BONUS_DURATION = 5000L
    private var hasAccelerometer = false

    // Переменные для обработки акселерометра
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val SHAKE_THRESHOLD = 800f // Чувствительность к тряске

    // Звук
    private var soundManager: SoundManager? = null

    // СИСТЕМА ЗОЛОТОГО ТАРАКАНА
    private var goldCockroachInterval = 20 // Интервал появления золотого таракана в секундах
    private var goldCockroach: ImageView? = null
    private val goldCockroachDrawable = R.drawable.gold_cockroach // Добавьте gold_cockroach.png в drawable

    private val insectDrawables = listOf(
        R.drawable.cockroach1,
        R.drawable.cockroach2,
        R.drawable.beetle1,
        R.drawable.beetle2
    )

    private val bonusDrawable = R.drawable.bonus

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameSpeed = intent.getIntExtra("game_speed", 5)
        maxCockroaches = intent.getIntExtra("max_cockroaches", 10)
        val roundDurationSeconds = intent.getIntExtra("round_duration", 60)
        roundDuration = roundDurationSeconds * 1000
        bonusInterval = intent.getIntExtra("bonus_interval", 15)

        playerId = intent.getLongExtra("player_id", -1L)
        playerName = intent.getStringExtra("player_name") ?: "Unknown"

        Log.d("GameActivity", "Настройки игры:")
        Log.d("GameActivity", " - Скорость: $gameSpeed")
        Log.d("GameActivity", " - Макс. тараканов: $maxCockroaches")
        Log.d("GameActivity", " - Длительность раунда: ${roundDuration / 1000} сек")
        Log.d("GameActivity", " - Интервал бонусов: $bonusInterval сек")
        Log.d("GameActivity", " - Игрок: $playerName (ID: $playerId)")

        // Инициализация
        gameRepository = GameRepository(AppDatabase.getInstance(this))
        soundManager = SoundManager.getInstance(this)

        // Инициализация сенсоров
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        hasAccelerometer = accelerometer != null

        if (!hasAccelerometer) {
            Log.w("GameActivity", "Акселерометр не доступен на этом устройстве")
        } else {
            Log.d("GameActivity", "Акселерометр инициализирован")
        }

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

        // Запускаем таймер бонусов
        startBonusTimer()

        // ЗАПУСКАЕМ ТАЙМЕР ЗОЛОТОГО ТАРАКАНА
        startGoldCockroachTimer()

        startInsectSpawning()
    }

    private fun startGoldCockroachTimer() {
        Log.d("GameActivity", "Запуск таймера золотого таракана. Интервал: $goldCockroachInterval сек")

        val goldRunnable = object : Runnable {
            override fun run() {
                if (goldCockroach == null) {
                    spawnGoldCockroach()
                }
                // Повторяем каждые 20 секунд
                handler.postDelayed(this, goldCockroachInterval * 1000L)
            }
        }

        // Первый золотой таракан через 20 секунд
        handler.postDelayed(goldRunnable, goldCockroachInterval * 1000L)
    }

    private fun spawnGoldCockroach() {
        // Удаляем предыдущего золотого таракана если есть
        goldCockroach?.let {
            gameContainer.removeView(it)
        }

        val goldCockroachView = ImageView(this).apply {
            setImageResource(goldCockroachDrawable)
            layoutParams = FrameLayout.LayoutParams(150, 150) // Чуть больше обычного

            val containerWidth = gameContainer.width
            val containerHeight = gameContainer.height

            if (containerWidth > 0 && containerHeight > 0) {
                x = Random.nextFloat() * (containerWidth - 150)
                y = Random.nextFloat() * (containerHeight - 150)
            } else {
                x = 200f
                y = 200f
            }

            // Анимация блеска для золотого таракана
            ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.2f, 1f).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
            ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.2f, 1f).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                start()
            }

            // Анимация вращения
            ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply {
                duration = 3000
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }

        goldCockroachView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                killGoldCockroach(goldCockroachView)
                true
            } else {
                false
            }
        }

        gameContainer.addView(goldCockroachView)
        goldCockroach = goldCockroachView

        Log.d("GameActivity", "Золотой таракан появился!")
    }

    private fun killGoldCockroach(cockroach: ImageView) {
        val goldPrice = GoldRateService.getCurrentGoldPrice()
        // Начисляем очки пропорционально курсу золота (например, курс * 10)
        val pointsEarned = (goldPrice / 10).toInt()
        score += pointsEarned

        Log.d("GameActivity", "Золотой таракан пойман! +$pointsEarned очков (курс: $goldPrice)")

        // Показываем сообщение о начисленных очках
        showGoldBonusMessage(pointsEarned)
        updateScore()

        // Анимация исчезновения с эффектом "золотого взрыва"
        ObjectAnimator.ofFloat(cockroach, "scaleX", 1f, 2f, 0f).apply {
            duration = 500
            start()
        }
        ObjectAnimator.ofFloat(cockroach, "scaleY", 1f, 2f, 0f).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    gameContainer.removeView(cockroach)
                    goldCockroach = null
                }
            })
            start()
        }
    }

    private fun showGoldBonusMessage(points: Int) {
        val bonusText = TextView(this).apply {
            text = "ЗОЛОТО!\n+$points очков!"
            setTextColor(0xFFFFFF00.toInt())
            textSize = 18f
            setBackgroundColor(0x80000000.toInt())
            setPadding(20, 10, 20, 10)
            gravity = android.view.Gravity.CENTER

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }

        gameContainer.addView(bonusText)

        // Анимация появления и исчезновения
        bonusText.alpha = 0f
        bonusText.animate()
            .alpha(1f)
            .setDuration(500)
            .withEndAction {
                bonusText.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setStartDelay(1000)
                    .withEndAction {
                        gameContainer.removeView(bonusText)
                    }
                    .start()
            }
            .start()
    }

    private fun startBonusTimer() {
        bonusTimer = object : CountDownTimer(roundDuration.toLong(), bonusInterval * 1000L) {
            private var firstTick = true

            override fun onTick(millisUntilFinished: Long) {
                if (firstTick) {
                    // Пропускаем первый тик (он происходит сразу)
                    firstTick = false
                    return
                }

                // Спавним бонус только если сейчас нет активного бонуса
                if (!isBonusActive && activeBonuses.isEmpty()) {
                    spawnBonus()
                }
            }

            override fun onFinish() {
                // Не спавним бонус в конце игры
            }
        }.start()
    }

    private fun spawnBonus() {
        val bonusView = ImageView(this).apply {
            setImageResource(bonusDrawable)
            layoutParams = FrameLayout.LayoutParams(100, 100)

            val containerWidth = gameContainer.width
            val containerHeight = gameContainer.height

            if (containerWidth > 0 && containerHeight > 0) {
                x = Random.nextFloat() * (containerWidth - 100)
                y = Random.nextFloat() * (containerHeight - 100)
            } else {
                x = 200f
                y = 200f
            }

            // Анимация пульсации
            ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.3f, 1f).apply {
                duration = 800
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
            ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.3f, 1f).apply {
                duration = 800
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }

        bonusView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                activateBonus(bonusView)
                true
            } else {
                false
            }
        }

        gameContainer.addView(bonusView)
        activeBonuses.add(bonusView)

        Log.d("GameActivity", "Бонус появился на позиции (${bonusView.x}, ${bonusView.y})")
    }

    private fun activateBonus(bonusView: ImageView) {
        soundManager?.playInsectScream()

        gameContainer.removeView(bonusView)
        activeBonuses.remove(bonusView)

        if (hasAccelerometer) {
            isBonusActive = true
            bonusActiveUntil = System.currentTimeMillis() + BONUS_DURATION

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)

            showBonusActivatedMessage()

            Log.d("GameActivity", "Бонус активирован! Длительность: ${BONUS_DURATION}мс")

            // Автоматическое отключение бонуса через BONUS_DURATION
            handler.postDelayed({
                if (isBonusActive) {
                    deactivateBonus()
                }
            }, BONUS_DURATION)
        } else {
            // Если акселерометра нет, просто показываем сообщение
            showNoAccelerometerMessage()
        }

        // Обновляем UI
        updateScore()
    }

    private fun showBonusActivatedMessage() {
        val bonusText = TextView(this).apply {
            text = "БОНУС АКТИВИРОВАН!\nНаклоняйте телефон!"
            setTextColor(0xFFFFFF00.toInt())
            textSize = 16f
            setBackgroundColor(0x80000000.toInt())
            setPadding(20, 10, 20, 10)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
            }
        }

        gameContainer.addView(bonusText)

        handler.postDelayed({
            gameContainer.removeView(bonusText)
        }, 2000)
    }

    private fun showNoAccelerometerMessage() {
        val messageText = TextView(this).apply {
            text = "Акселерометр не доступен\nБонус не работает"
            setTextColor(0xFFFF0000.toInt())
            textSize = 14f
            setBackgroundColor(0x80000000.toInt())
            setPadding(20, 10, 20, 10)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
            }
        }

        gameContainer.addView(messageText)

        handler.postDelayed({
            gameContainer.removeView(messageText)
        }, 2000)
    }

    private fun deactivateBonus() {
        isBonusActive = false
        sensorManager.unregisterListener(this)

        // Показываем сообщение о завершении бонуса
        showBonusDeactivatedMessage()

        updateScore()
        Log.d("GameActivity", "Бонус деактивирован")
    }

    private fun showBonusDeactivatedMessage() {
        val bonusText = TextView(this).apply {
            text = "Бонус завершен"
            setTextColor(0xFFFFA500.toInt())
            textSize = 14f
            setBackgroundColor(0x80000000.toInt())
            setPadding(20, 10, 20, 10)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
            }
        }

        gameContainer.addView(bonusText)

        handler.postDelayed({
            gameContainer.removeView(bonusText)
        }, 1500)
    }

    // Улучшенный обработчик данных акселерометра
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isBonusActive || !hasAccelerometer) return

        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()

                // Фильтруем слишком частые обновления (не чаще чем раз в 50мс)
                if ((currentTime - lastUpdate) > 50) {
                    val timeDiff = currentTime - lastUpdate
                    lastUpdate = currentTime

                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]

                    // Рассчитываем изменение положения
                    val speed = abs(x + y + z - lastX - lastY - lastZ) / timeDiff * 10000

                    // Если изменение достаточно большое - применяем наклон
                    if (speed > SHAKE_THRESHOLD) {
                        Log.d("Accelerometer", "Обнаружено движение: speed=$speed")
                        applyTiltToInsects(x, y)
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }
    }

    private fun applyTiltToInsects(tiltX: Float, tiltY: Float) {
        // Усиливаем эффект наклона с учетом чувствительности
        val forceMultiplier = 25f + (gameSpeed * 2) // Зависит от сложности игры
        val effectiveForceX = tiltX * forceMultiplier
        val effectiveForceY = tiltY * forceMultiplier

        Log.d("Accelerometer", "Применяем наклон: X=$effectiveForceX, Y=$effectiveForceY")

        // Применяем движение ко всем активным насекомым
        activeCockroaches.forEach { insect ->
            // Плавно перемещаем насекомое в направлении наклона
            var newX = insect.x - effectiveForceX
            var newY = insect.y - effectiveForceY

            // Проверяем границы экрана
            val maxX = (gameContainer.width - insect.width).toFloat()
            val maxY = (gameContainer.height - insect.height).toFloat()

            newX = newX.coerceIn(0f, maxX)
            newY = newY.coerceIn(0f, maxY)

            // Применяем новую позицию с анимацией для плавности
            insect.animate()
                .x(newX)
                .y(newY)
                .setDuration(100)
                .start()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Логируем изменение точности сенсора
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_LOW ->
                Log.d("Accelerometer", "Точность сенсора: НИЗКАЯ")
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
                Log.d("Accelerometer", "Точность сенсора: СРЕДНЯЯ")
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
                Log.d("Accelerometer", "Точность сенсора: ВЫСОКАЯ")
            SensorManager.SENSOR_STATUS_UNRELIABLE ->
                Log.d("Accelerometer", "Точность сенсора: НЕНАДЕЖНАЯ")
        }
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
        return interval.coerceAtLeast(500) // Минимальный интервал 500мс
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
                    if (!isBonusActive) {
                        moveInsect(insect)
                    }
                    handler.postDelayed(this, 50)
                }
            }
        }
        handler.post(moveRunnable)
    }

    private fun moveInsect(insect: ImageView) {
        val speed = gameSpeed * 2 + Random.nextInt(-2, 3)

        val directionX = when (Random.nextInt(0, 3)) {
            0 -> -1 // влево
            1 -> 0  // нет движения по X
            else -> 1 // вправо
        }

        val directionY = when (Random.nextInt(0, 4)) {
            0 -> -1 // вверх
            1 -> 0  // нет движения по Y
            else -> 1 // вниз (более вероятно)
        }

        val containerWidth = gameContainer.width
        val containerHeight = gameContainer.height

        var newX = insect.x + directionX * speed
        var newY = insect.y + directionY * speed

        if (newX < 0) {
            newX = 0f
        } else if (newX > containerWidth - insect.width) {
            newX = (containerWidth - insect.width).toFloat()
        }

        if (newY < 0) {
            newY = 0f
        } else if (newY > containerHeight - insect.height) {
            newY = (containerHeight - insect.height).toFloat()
        }

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
        val bonusStatus = if (isBonusActive) " | БОНУС АКТИВЕН!" else ""
        scoreTextView.text = "Игрок: $playerName | Очки: $score$bonusStatus"
    }

    private fun endGame() {
        Log.d("GameActivity", "Завершение игры...")
        handler.removeCallbacksAndMessages(null)
        countDownTimer.cancel()
        bonusTimer.cancel()

        if (isBonusActive) {
            deactivateBonus()
        }

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
                } catch (e: Exception) {
                    Log.e("GameActivity", "Ошибка сохранения результата: ${e.message}")
                }
            }
        }

        activeCockroaches.forEach { insect ->
            gameContainer.removeView(insect)
        }
        activeCockroaches.clear()

        activeBonuses.forEach { bonus ->
            gameContainer.removeView(bonus)
        }
        activeBonuses.clear()

        // ОЧИЩАЕМ ЗОЛОТОГО ТАРАКАНА
        goldCockroach?.let {
            gameContainer.removeView(it)
            goldCockroach = null
        }

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

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (isBonusActive && hasAccelerometer) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("GameActivity", "onDestroy: очистка ресурсов")
        handler.removeCallbacksAndMessages(null)
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        if (::bonusTimer.isInitialized) {
            bonusTimer.cancel()
        }

        sensorManager.unregisterListener(this)

        activeCockroaches.clear()
        activeBonuses.clear()

        // Очищаем золотого таракана
        goldCockroach?.let {
            gameContainer.removeView(it)
            goldCockroach = null
        }
    }
}