package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GoldRateService(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GoldRateService"
        private var currentGoldPrice: Double = 0.0

        fun getCurrentGoldPrice(): Double = if (currentGoldPrice > 0) currentGoldPrice else 10432.09

        private fun setCurrentGoldPrice(price: Double) {
            currentGoldPrice = price
            Log.d(TAG, "Курс золота установлен: $currentGoldPrice")
        }

        fun notifyWidgetUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, GoldRateAppWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                if (appWidgetIds.isNotEmpty()) {
                    appWidgetIds.forEach { appWidgetId ->
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }
                    Log.d(TAG, "Виджеты обновлены")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении виджетов: ${e.message}")
            }
        }

        // обновл текста на экране
        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = android.widget.RemoteViews(context.packageName, R.layout.widget_gold_rate_layout)

            val goldPrice = getCurrentGoldPrice()
            val priceText = "Золото: ${String.format("%.2f", goldPrice)} руб/г"

            views.setTextViewText(R.id.goldPriceText, priceText)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Запуск обновления курса золота...")

        return@withContext try {
            val realPrice = fetchRealGoldPrice() //
            if (realPrice > 0) {
                setCurrentGoldPrice(realPrice)
                Log.d(TAG, "Успешно получен реальный курс: $realPrice")
            } else {
                val fallbackPrice = if (currentGoldPrice > 0) currentGoldPrice                                            else 10432.09
                setCurrentGoldPrice(fallbackPrice)
                Log.d(TAG, "Используется сохраненный курс: $fallbackPrice")
            }

            notifyWidgetUpdate(applicationContext)
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка: ${e.message}")
            notifyWidgetUpdate(applicationContext)
            Result.success()
        }
    }

    private suspend fun fetchRealGoldPrice(): Double {
        return try {
            val api = createRetrofit()
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = Date()
            val dateReq1 = dateFormat.format(today)
            val dateReq2 = dateFormat.format(today)

            Log.d(TAG, "Запрос курса за период: $dateReq1 - $dateReq2")

            val response = api.getGoldRate(dateReq1, dateReq2).execute()

            if (response.isSuccessful) {
                val metallData = response.body()
                if (metallData != null && metallData.records.isNotEmpty()) {
                    val record = metallData.records.first()
                    val price = record.getGoldPrice() // извлекает число из объекта

                    if (price > 0) {
                        Log.d(TAG, "Успешно распарсен курс: $price")
                        price
                    } else {
                        Log.e(TAG, "Нулевая цена в записи")
                        0.0
                    }
                } else {
                    Log.e(TAG, "Пустой ответ или нет записей")
                    0.0
                }
            } else {
                Log.e(TAG, "Ошибка HTTP: ${response.code()} - ${response.message()}")
                0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запроса к ЦБ: ${e.message}")
            0.0
        }
    }

    private fun createRetrofit(): CbrApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://www.cbr.ru/")
            .client(client)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(CbrApi::class.java)
    }
}