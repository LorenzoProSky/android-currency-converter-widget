package dev.personal.currencyconverter

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class CurrencyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    updateWidget(context, appWidgetManager, appWidgetId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forceRefresh: Boolean = false
        ) {
            val amount = 100.0
            val source = "EUR"
            val target = "USD"

            val rates = RatesRepository.getRates(context, forceRefresh)
            val result = RatesRepository.convert(amount, source, target, rates)

            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.sourceText, "${amount.toInt()} $source")
            views.setTextViewText(
                R.id.resultText,
                String.format(Locale.US, "≈ %.2f %s", result, target)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}