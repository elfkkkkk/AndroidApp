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

        setupGoldRateWorkManager()
        startImmediateGoldRateUpdate()

        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    private fun setupGoldRateWorkManager() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val goldRateWorkRequest = androidx.work.PeriodicWorkRequestBuilder<GoldRateService>(
            2, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "goldRateUpdate",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            goldRateWorkRequest
        )
    }

    private fun startImmediateGoldRateUpdate() {
        val immediateWorkRequest = androidx.work.OneTimeWorkRequestBuilder<GoldRateService>()
            .build()
        androidx.work.WorkManager.getInstance(this).enqueue(immediateWorkRequest)
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
    val isLoading = remember { mutableStateOf(false) }

    fun updateGoldRate() {
        isLoading.value = true
        val updateRequest = androidx.work.OneTimeWorkRequestBuilder<GoldRateService>().build()
        workManager.enqueue(updateRequest)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            currentGoldPrice.value = GoldRateService.getCurrentGoldPrice()
            isLoading.value = false
        }, 3000)
    }

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

        Text(
            text = "Скорость игры: $gameSpeed",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = gameSpeed.toFloat(),
            onValueChange = {
                gameSpeed = it.toInt()
                settingsRepository.saveGameSpeed(gameSpeed)
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Максимальное количество тараканов: $maxCockroaches",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = maxCockroaches.toFloat(),
            onValueChange = {
                maxCockroaches = it.toInt()
                settingsRepository.saveMaxCockroaches(maxCockroaches)
            },
            valueRange = 1f..20f,
            steps = 18,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Интервал появления бонусов: $bonusInterval сек",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = bonusInterval.toFloat(),
            onValueChange = {
                bonusInterval = it.toInt()
                settingsRepository.saveBonusInterval(bonusInterval)
            },
            valueRange = 5f..30f,
            steps = 24,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Длительность раунда: $roundDuration сек",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = roundDuration.toFloat(),
            onValueChange = {
                roundDuration = it.toInt()
                settingsRepository.saveRoundDuration(roundDuration)
            },
            valueRange = 30f..120f,
            steps = 8,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Курс золота ЦБ РФ",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Текущий курс золота:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "${String.format("%.2f", currentGoldPrice.value)} руб/г",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { updateGoldRate() },
                    enabled = !isLoading.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    if (isLoading.value) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Обновление...")
                        }
                    } else {
                        Text("Обновить курс из ЦБ РФ")
                    }
                }

                Text(
                    text = "• Курс автоматически обновляется каждые 2 часа\n" +
                            "• Золотой таракан приносит очки пропорционально курсу",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Виджет курса золота",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Добавьте виджет на домашний экран:\n" +
                            "1. Нажмите и удерживайте пустое место\n" +
                            "2. Выберите 'Виджеты'\n" +
                            "3. Найдите 'MyApplication'\n" +
                            "4. Перетащите виджет на экран",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Сбросить настройки по умолчанию")
        }

        Text(
            text = "Настройки сохраняются автоматически",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
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