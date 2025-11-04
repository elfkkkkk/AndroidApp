package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible

class GoldRateWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var goldPriceText: TextView
    private lateinit var progressBar: ProgressBar

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_gold_rate, this, true)

        goldPriceText = view.findViewById(R.id.goldPriceText)
        progressBar = view.findViewById(R.id.progressBar)

        // Начальная настройка
        updateGoldPrice(0.0)
    }

    fun updateGoldPrice(price: Double) {
        goldPriceText.text = if (price > 0) {
            "Золото: ${String.format("%.2f", price)} руб/г"
        } else {
            "Загрузка..."
        }

        progressBar.isVisible = price == 0.0
        goldPriceText.isVisible = price != 0.0
    }

    fun showError() {
        goldPriceText.text = "Ошибка загрузки"
        progressBar.isVisible = false
        goldPriceText.isVisible = true
    }
}