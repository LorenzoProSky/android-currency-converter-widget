package dev.personal.currencyconverter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import java.util.Locale

import dev.personal.currencyconverter.WidgetActions.ACTION_CYCLE_SOURCE
import dev.personal.currencyconverter.WidgetActions.ACTION_CYCLE_TARGET
import dev.personal.currencyconverter.WidgetActions.ACTION_KEYPAD
import dev.personal.currencyconverter.WidgetActions.ACTION_SWAP
import dev.personal.currencyconverter.WidgetActions.EXTRA_KEY
import dev.personal.currencyconverter.WidgetActions.KNOWN_ACTIONS
import dev.personal.currencyconverter.WidgetActions.digitKeyIds

class CurrencyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        RatesRepository.refreshCacheAsync(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && intent.action in KNOWN_ACTIONS) {
            return // malformed - ignore
        }

        try {
            when (intent.action) {
                ACTION_CYCLE_SOURCE -> handleCycle(context, appWidgetId, isSource = true)
                ACTION_CYCLE_TARGET -> handleCycle(context, appWidgetId, isSource = false)
                ACTION_SWAP -> handleSwap(context, appWidgetId)
                ACTION_KEYPAD -> {
                    val key = intent.getStringExtra(EXTRA_KEY)
                    if (key != null) handleKeypad(context, appWidgetId, key)
                }
                else -> super.onReceive(context, intent)
            }
        } catch (e: Exception) {
            Log.e("CurrencyWidgetProvider", "onReceive failed for action=${intent.action}", e)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetState.clear(context, id)
        }
    }

    private fun handleCycle(context: Context, appWidgetId: Int, isSource: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (isSource) {
            val current = WidgetState.getSource(context, appWidgetId)
            WidgetState.setSource(context, appWidgetId, WidgetState.nextCurrency(current))
            updateSourceAndResult(context, appWidgetManager, appWidgetId)
        } else {
            val current = WidgetState.getTarget(context, appWidgetId)
            WidgetState.setTarget(context, appWidgetId, WidgetState.nextCurrency(current))
            updateTargetAndResult(context, appWidgetManager, appWidgetId)
        }
        RatesRepository.refreshCacheAsync(context)
    }

    private fun handleSwap(context: Context, appWidgetId: Int) {
        val oldSource = WidgetState.getSource(context, appWidgetId)
        val oldTarget = WidgetState.getTarget(context, appWidgetId)
        WidgetState.setSource(context, appWidgetId, oldTarget)
        WidgetState.setTarget(context, appWidgetId, oldSource)

        updateAfterSwap(context, AppWidgetManager.getInstance(context), appWidgetId)
        RatesRepository.refreshCacheAsync(context)
    }

    private fun handleKeypad(context: Context, appWidgetId: Int, key: String) {
        val current = WidgetState.getAmount(context, appWidgetId)
        val updated = when (key) {
            "CLEAR" -> WidgetState.clearAmount()
            "DOT" -> WidgetState.appendDot(current)
            else -> WidgetState.appendDigit(current, key)
        }
        WidgetState.setAmount(context, appWidgetId, updated)
        updateAmountAndResult(context, AppWidgetManager.getInstance(context), appWidgetId)
        RatesRepository.refreshCacheAsync(context)
    }

    companion object {
        private data class WidgetContent(
            val stringAmount: String,
            val resultDisplay: String,
            val source: String,
            val target: String
        )

        private fun computeContent(context: Context, appWidgetId: Int): WidgetContent {
            val stringAmount = WidgetState.getAmount(context, appWidgetId)
            val amount = WidgetState.amountStringToDouble(stringAmount)
            val source = WidgetState.getSource(context, appWidgetId)
            val target = WidgetState.getTarget(context, appWidgetId)
            val result = RatesRepository.convert(amount, source, target, context)
            return WidgetContent(
                stringAmount = stringAmount,
                resultDisplay = result?.let { formatResultForDisplay(it) } ?: "NO DATA",
                source = source,
                target = target
            )
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val content = computeContent(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.currency_widget)

            views.setTextViewText(R.id.amountText, content.stringAmount)
            views.setTextViewText(R.id.resultText, content.resultDisplay)
            views.setTextViewText(R.id.sourceCurrencyButton, "${content.source} ▾")
            views.setTextViewText(R.id.targetCurrencyButton, "${content.target} ▾")

            views.setOnClickPendingIntent(
                R.id.sourceCurrencyButton, buildPendingIntent(context, appWidgetId, ACTION_CYCLE_SOURCE)
            )
            views.setOnClickPendingIntent(
                R.id.targetCurrencyButton, buildPendingIntent(context, appWidgetId, ACTION_CYCLE_TARGET)
            )
            views.setOnClickPendingIntent(
                R.id.swapButton, buildPendingIntent(context, appWidgetId, ACTION_SWAP)
            )

            digitKeyIds.forEach { (digit, viewId) ->
                views.setOnClickPendingIntent(
                    viewId, buildPendingIntent(context, appWidgetId, ACTION_KEYPAD, digit)
                )
            }
            views.setOnClickPendingIntent(
                R.id.keyDot, buildPendingIntent(context, appWidgetId, ACTION_KEYPAD, "DOT")
            )
            views.setContentDescription(R.id.swapButton, "Swap currencies")
            views.setOnClickPendingIntent(
                R.id.keyClearAll, buildPendingIntent(context, appWidgetId, ACTION_KEYPAD, "CLEAR")
            )
            views.setContentDescription(R.id.keyClearAll, "Clear amount")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAmountAndResult(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val content = computeContent(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.amountText, content.stringAmount)
            views.setTextViewText(R.id.resultText, content.resultDisplay)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        fun updateSourceAndResult(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val content = computeContent(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.sourceCurrencyButton, "${content.source} ▾")
            views.setTextViewText(R.id.resultText, content.resultDisplay)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        fun updateTargetAndResult(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val content = computeContent(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.targetCurrencyButton, "${content.target} ▾")
            views.setTextViewText(R.id.resultText, content.resultDisplay)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        fun updateAfterSwap(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val content = computeContent(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.sourceCurrencyButton, "${content.source} ▾")
            views.setTextViewText(R.id.targetCurrencyButton, "${content.target} ▾")
            views.setTextViewText(R.id.resultText, content.resultDisplay)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        fun updateResult(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val content = computeContent(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.currency_widget)
            views.setTextViewText(R.id.resultText, content.resultDisplay)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        private fun buildPendingIntent(
            context: Context,
            appWidgetId: Int,
            action: String,
            extraKey: String? = null
        ): PendingIntent {
            val intent = Intent(context, CurrencyWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                if (extraKey != null) putExtra(EXTRA_KEY, extraKey)
            }
            // Unique per (widget, action, key)
            val requestCode = "$appWidgetId-$action-$extraKey".hashCode()
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private val SCALE_TIERS = listOf(
            1_000_000.0 to "M",
            1_000_000_000.0 to "B",
            1_000_000_000_000.0 to "T"
        )

        private fun formatResultForDisplay(amount: Double): String {
            if (amount < 1_000_000) {
                return String.format(Locale.US, "%,.2f", amount)
            }

            for (i in SCALE_TIERS.indices) {
                val (divisor, suffix) = SCALE_TIERS[i]
                val scaled = amount / divisor
                val rounded = Math.round(scaled * 100) / 100.0
                if (rounded < 1000.0 || i == SCALE_TIERS.lastIndex) {
                    return String.format(Locale.US, "%.3f%s", scaled, suffix)
                }
            }
            error("SCALE_TIERS must not be empty") // unreachable
        }
    }
}