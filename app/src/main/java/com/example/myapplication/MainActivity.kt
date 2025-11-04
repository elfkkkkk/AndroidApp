package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// WorkManager imports
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ЗАПУСКАЕМ WORKMANAGER ДЛЯ ОБНОВЛЕНИЯ КУРСА ЗОЛОТА
        setupGoldRateWorkManager()

        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    private fun setupGoldRateWorkManager() {
        // Создаем ограничения - обновляем только при наличии интернета
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Создаем периодическую задачу - обновляем каждые 2 часа
        val goldRateWorkRequest = PeriodicWorkRequestBuilder<GoldRateService>(
            2, TimeUnit.HOURS, // Интервал повторения
            15, TimeUnit.MINUTES // Flex интервал (можно выполнить за 15 минут до следующего интервала)
        ).setConstraints(constraints).build()

        // Запускаем уникальную периодическую задачу
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "goldRateUpdate",
            ExistingPeriodicWorkPolicy.KEEP, // Если задача уже есть - сохраняем ее
            goldRateWorkRequest
        )

        // Также запускаем немедленное обновление при старте приложения
        startImmediateGoldRateUpdate()
    }

    private fun startImmediateGoldRateUpdate() {
        val immediateWorkRequest = PeriodicWorkRequestBuilder<GoldRateService>(
            1, TimeUnit.HOURS // Этот параметр не важен для немедленного запуска
        ).build()

        WorkManager.getInstance(this).enqueue(immediateWorkRequest)
    }
}

@Composable
fun GameLaunchButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, PlayerSelectionActivity::class.java)
            context.startActivity(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            text = "🎮 НАЧАТЬ ИГРУ",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Правила игры", "Авторы", "Настройки", "Рекорды")
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, RegistrationActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .height(60.dp),
                ) {
                    Text(
                        text = "Зарегистрироваться",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                GameLaunchButton()
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> RulesScreen()
                1 -> AuthorsScreen()
                2 -> SettingsScreen()
                3 -> RecordsScreen()
            }
        }
    }
}

@Composable
fun RulesScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/rules.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun AuthorsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.author_photo),
            contentDescription = "Фото автора",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Автор проекта",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Perminova Ekaterina",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Студент 4 курса",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val workManager = remember { WorkManager.getInstance(context) }

    var gameSpeed by remember { mutableIntStateOf(settingsRepository.getGameSpeed()) }
    var maxCockroaches by remember { mutableIntStateOf(settingsRepository.getMaxCockroaches()) }
    var bonusInterval by remember { mutableIntStateOf(settingsRepository.getBonusInterval()) }
    var roundDuration by remember { mutableIntStateOf(settingsRepository.getRoundDuration()) }

    val currentGoldPrice = remember { mutableStateOf(GoldRateService.getCurrentGoldPrice()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Настройки игры",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(text = "Скорость игры: $gameSpeed", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = gameSpeed.toFloat(),
            onValueChange = { gameSpeed = it.toInt(); settingsRepository.saveGameSpeed(gameSpeed) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Text(text = "Макс. тараканов: $maxCockroaches", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = maxCockroaches.toFloat(),
            onValueChange = { maxCockroaches = it.toInt(); settingsRepository.saveMaxCockroaches(maxCockroaches) },
            valueRange = 1f..20f,
            steps = 18,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Text(text = "Интервал бонусов: $bonusInterval сек", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = bonusInterval.toFloat(),
            onValueChange = { bonusInterval = it.toInt(); settingsRepository.saveBonusInterval(bonusInterval) },
            valueRange = 5f..30f,
            steps = 24,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Text(text = "Длительность раунда: $roundDuration сек", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = roundDuration.toFloat(),
            onValueChange = { roundDuration = it.toInt(); settingsRepository.saveRoundDuration(roundDuration) },
            valueRange = 30f..120f,
            steps = 8,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Курс золота", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Текущий курс:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${String.format("%.2f", currentGoldPrice.value)} руб/г",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        val updateRequest = androidx.work.OneTimeWorkRequestBuilder<GoldRateService>().build()
                        workManager.enqueue(updateRequest)
                        currentGoldPrice.value = GoldRateService.getCurrentGoldPrice()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Обновить курс")
                }
            }
        }

        Button(
            onClick = {
                gameSpeed = 5
                maxCockroaches = 10
                bonusInterval = 10
                roundDuration = 60
                settingsRepository.saveGameSpeed(gameSpeed)
                settingsRepository.saveMaxCockroaches(maxCockroaches)
                settingsRepository.saveBonusInterval(bonusInterval)
                settingsRepository.saveRoundDuration(roundDuration)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Сбросить настройки")
        }
    }
}
@Composable
fun RecordsScreen() {
    val context = LocalContext.current
    val gameRepository = remember { GameRepository(AppDatabase.getInstance(context)) }
    var topScores by remember { mutableStateOf<List<ScoreWithPlayerInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        gameRepository.getTopScoresWithPlayerInfo().collectLatest { scores ->
            topScores = scores
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Топ 10 рекордов",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (topScores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Рекордов пока нет",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Сыграйте в игру чтобы установить первый рекорд!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topScores) { score ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = score.playerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Курс: ${score.playerCourse}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Сложность: ${score.difficulty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Дата: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(score.gameDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${score.score} очков",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        MainScreen()
    }
}