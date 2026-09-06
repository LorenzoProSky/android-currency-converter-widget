package dev.personal.currencyconverter

import android.content.Context
import dev.personal.currencyconverter.CurrencyConstants.ALL_CURRENCIES
import dev.personal.currencyconverter.CurrencyConstants.DEFAULT_SOURCE
import dev.personal.currencyconverter.CurrencyConstants.DEFAULT_TARGET
import androidx.core.content.edit

object WidgetState {

    private const val PREFS_NAME = "currency_widget_state"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSource(context: Context, appWidgetId: Int): String =
        prefs(context).getString("source_$appWidgetId", DEFAULT_SOURCE) ?: DEFAULT_SOURCE

    fun getTarget(context: Context, appWidgetId: Int): String =
        prefs(context).getString("target_$appWidgetId", DEFAULT_TARGET) ?: DEFAULT_TARGET

    fun setSource(context: Context, appWidgetId: Int, currency: String) {
        prefs(context).edit { putString("source_$appWidgetId", currency) }
    }

    fun setTarget(context: Context, appWidgetId: Int, currency: String) {
        prefs(context).edit { putString("target_$appWidgetId", currency) }
    }

    fun nextCurrency(current: String): String {
        val index = ALL_CURRENCIES.indexOf(current)
        return ALL_CURRENCIES[(index + 1) % ALL_CURRENCIES.size]
    }

    fun clear(context: Context, appWidgetId: Int) {
        prefs(context).edit {
            remove("source_$appWidgetId")
                .remove("target_$appWidgetId")
        }
    }
}