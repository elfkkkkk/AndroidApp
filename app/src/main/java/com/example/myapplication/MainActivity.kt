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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Правила игры", "Авторы", "Настройки")
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Button(
                onClick = {
                    val intent = Intent(context, RegistrationActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Зарегистрироваться")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Панель вкладок
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }

            // Контент вкладок
            when (selectedTabIndex) {
                0 -> RulesScreen()
                1 -> AuthorsScreen()
                2 -> SettingsScreen()
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
        // Фото автора
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

    // Загружаем сохраненные настройки при запуске
    var gameSpeed by remember { mutableIntStateOf(settingsRepository.getGameSpeed()) }
    var maxCockroaches by remember { mutableIntStateOf(settingsRepository.getMaxCockroaches()) }
    var bonusInterval by remember { mutableIntStateOf(settingsRepository.getBonusInterval()) }
    var roundDuration by remember { mutableIntStateOf(settingsRepository.getRoundDuration()) }

    // Функция для сохранения всех настроек
    fun saveAllSettings() {
        settingsRepository.saveGameSpeed(gameSpeed)
        settingsRepository.saveMaxCockroaches(maxCockroaches)
        settingsRepository.saveBonusInterval(bonusInterval)
        settingsRepository.saveRoundDuration(roundDuration)
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

        // Скорость игры
        Text(
            text = "Скорость игры: $gameSpeed",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = gameSpeed.toFloat(),
            onValueChange = {
                gameSpeed = it.toInt()
                settingsRepository.saveGameSpeed(gameSpeed) // Сохраняем при изменении
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Максимальное количество тараканов
        Text(
            text = "Максимальное количество тараканов: $maxCockroaches",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = maxCockroaches.toFloat(),
            onValueChange = {
                maxCockroaches = it.toInt()
                settingsRepository.saveMaxCockroaches(maxCockroaches) // Сохраняем при изменении
            },
            valueRange = 1f..20f,
            steps = 18,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Интервал появления бонусов
        Text(
            text = "Интервал появления бонусов: $bonusInterval сек",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = bonusInterval.toFloat(),
            onValueChange = {
                bonusInterval = it.toInt()
                settingsRepository.saveBonusInterval(bonusInterval) // Сохраняем при изменении
            },
            valueRange = 5f..30f,
            steps = 24,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Длительность раунда
        Text(
            text = "Длительность раунда: $roundDuration сек",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = roundDuration.toFloat(),
            onValueChange = {
                roundDuration = it.toInt()
                settingsRepository.saveRoundDuration(roundDuration) // Сохраняем при изменении
            },
            valueRange = 30f..120f,
            steps = 8,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Кнопка сброса настроек
        Button(
            onClick = {
                // Сбрасываем на значения по умолчанию
                gameSpeed = 5
                maxCockroaches = 10
                bonusInterval = 10
                roundDuration = 60
                saveAllSettings() // Сохраняем сброшенные значения
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Сбросить настройки")
        }

        // Информация о сохранении
        Text(
            text = "Настройки сохраняются автоматически",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        MainScreen()
    }
}