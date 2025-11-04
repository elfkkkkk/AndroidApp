package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class GoldRateAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        startImmediateGoldRateUpdate(context)
    }

    override fun onEnabled(context: Context) {
        setupGoldRateWorkManager(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("goldRateUpdate")
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_gold_rate_layout)
        val goldPrice = GoldRateService.getCurrentGoldPrice()
        val priceText = "Золото: ${"%.2f".format(goldPrice)} руб/г"

        views.setTextViewText(R.id.goldPriceText, priceText)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setupGoldRateWorkManager(context: Context) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val goldRateWorkRequest = PeriodicWorkRequestBuilder<GoldRateService>(
            2, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "goldRateUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            goldRateWorkRequest
        )
    }

    private fun startImmediateGoldRateUpdate(context: Context) {
        val immediateWorkRequest = OneTimeWorkRequestBuilder<GoldRateService>()
            .build()
        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
    }
}