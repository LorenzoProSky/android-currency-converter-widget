package dev.personal.currencyconverter

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class CurrencyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // A user can pin the same widget multiple times
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.sourceText, "100 EUR")
            views.setTextViewText(R.id.resultText, "≈ 108.50 USD")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}