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
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

class GoldRateService(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GoldRateService"
        private var currentGoldPrice: Double = 5000.0

        fun getCurrentGoldPrice(): Double = currentGoldPrice

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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении виджетов: ${e.message}")
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = android.widget.RemoteViews(context.packageName, R.layout.widget_gold_rate_layout)

            val goldPrice = getCurrentGoldPrice()
            val priceText = "Золото: ${String.format("%.2f", goldPrice)} руб/г"

            views.setTextViewText(R.id.goldPriceText, priceText)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val realPrice = fetchRealGoldPrice()
            setCurrentGoldPrice(realPrice)
            notifyWidgetUpdate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка: ${e.message}")
            notifyWidgetUpdate(applicationContext)
            Result.success()
        }
    }

    private suspend fun fetchRealGoldPrice(): Double {
        return try {
            val api = createRetrofit()
            val currentDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                .format(java.util.Date())
            val url = "scripts/xml_metall.asp?date_req1=$currentDate&date_req2=$currentDate"

            val response = api.getGoldRate(url).execute()

            if (response.isSuccessful) {
                parseGoldPriceFromXml(response.body()?.toString() ?: "")
            } else {
                5000.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запроса: ${e.message}")
            5000.0
        }
    }

    private fun parseGoldPriceFromXml(xmlContent: String): Double {
        return try {
            val buyPattern = "<Buy>([0-9]+[.,][0-9]+)</Buy>".toRegex()
            val matchResult = buyPattern.find(xmlContent)

            if (matchResult != null) {
                matchResult.groupValues[1].replace(",", ".").toDouble()
            } else {
                5000.0
            }
        } catch (e: Exception) {
            5000.0
        }
    }

    private fun createRetrofit(): CbrApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
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