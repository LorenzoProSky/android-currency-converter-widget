package dev.personal.currencyconverter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && intent.action in KNOWN_ACTIONS) {
            return // malformed - ignore
        }

        when (intent.action) {
            ACTION_CYCLE_SOURCE -> handleCycle(context, appWidgetId, isSource = true)
            ACTION_CYCLE_TARGET -> handleCycle(context, appWidgetId, isSource = false)
            ACTION_SWAP -> handleSwap(context, appWidgetId)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetState.clear(context, id)
        }
    }

    private fun handleCycle(context: Context, appWidgetId: Int, isSource: Boolean) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isSource) {
                    val current = WidgetState.getSource(context, appWidgetId)
                    WidgetState.setSource(context, appWidgetId, WidgetState.nextCurrency(current))
                } else {
                    val current = WidgetState.getTarget(context, appWidgetId)
                    WidgetState.setTarget(context, appWidgetId, WidgetState.nextCurrency(current))
                }
                updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSwap(context: Context, appWidgetId: Int) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val oldSource = WidgetState.getSource(context, appWidgetId)
                val oldTarget = WidgetState.getTarget(context, appWidgetId)
                WidgetState.setSource(context, appWidgetId, oldTarget)
                WidgetState.setTarget(context, appWidgetId, oldSource)
                updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_CYCLE_SOURCE = "dev.personal.currencyconverter.ACTION_CYCLE_SOURCE"
        private const val ACTION_CYCLE_TARGET = "dev.personal.currencyconverter.ACTION_CYCLE_TARGET"
        private const val ACTION_SWAP = "dev.personal.currencyconverter.ACTION_SWAP"
        private val KNOWN_ACTIONS = setOf(ACTION_CYCLE_SOURCE, ACTION_CYCLE_TARGET, ACTION_SWAP)

        suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forceRefresh: Boolean = false
        ) {
            val amount = 100.0
            val source = WidgetState.getSource(context, appWidgetId)
            val target = WidgetState.getTarget(context, appWidgetId)

            val rates = RatesRepository.getRates(context, forceRefresh)
            val result = RatesRepository.convert(amount, source, target, rates)

            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.amountText, amount.toInt().toString())
            views.setTextViewText(R.id.sourceCurrencyButton, "$source ▾")
            views.setTextViewText(R.id.resultText, String.format(Locale.US, "≈ %.2f", result))
            views.setTextViewText(R.id.targetCurrencyButton, "$target ▾")

            views.setOnClickPendingIntent(
                R.id.sourceCurrencyButton,
                buildActionPendingIntent(context, appWidgetId, ACTION_CYCLE_SOURCE)
            )
            views.setOnClickPendingIntent(
                R.id.targetCurrencyButton,
                buildActionPendingIntent(context, appWidgetId, ACTION_CYCLE_TARGET)
            )
            views.setOnClickPendingIntent(
                R.id.swapButton,
                buildActionPendingIntent(context, appWidgetId, ACTION_SWAP)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildActionPendingIntent(
            context: Context,
            appWidgetId: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, CurrencyWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val requestCode = "$appWidgetId-$action".hashCode()
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}