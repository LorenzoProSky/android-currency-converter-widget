package dev.personal.currencyconverter

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

import dev.personal.currencyconverter.CurrencyConstants.ALL_CURRENCIES
import dev.personal.currencyconverter.CurrencyConstants.DEFAULT_SOURCE
import dev.personal.currencyconverter.CurrencyConstants.DEFAULT_TARGET
import dev.personal.currencyconverter.CurrencyConstants.MAX_AMOUNT_LENGTH

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

    fun getAmount(context: Context, appWidgetId: Int): String =
        prefs(context).getString("amount_$appWidgetId", "0") ?: "0"

    fun setAmount(context: Context, appWidgetId: Int, amount: String) {
        prefs(context).edit { putString("amount_$appWidgetId", amount) }
    }

    fun nextCurrency(current: String): String {
        val index = ALL_CURRENCIES.indexOf(current)
        return ALL_CURRENCIES[(index + 1) % ALL_CURRENCIES.size]
    }

    // Amount is the literal string being typed, e.g. "0", "52", "52.", "52.3"
    fun amountStringToDouble(amount: String): Double = amount.toDoubleOrNull() ?: 0.0

    fun amountDoubleToString(amount: Double): String =
        String.format(Locale.US, "%.2f", amount).take(MAX_AMOUNT_LENGTH)

    fun appendDigit(amount: String, digit: String): String {
        if (amount.length >= MAX_AMOUNT_LENGTH) return amount

        val dotIndex = amount.indexOf('.')
        if (dotIndex != -1) {
            val decimalDigitsSoFar = amount.length - dotIndex - 1
            if (decimalDigitsSoFar >= 2) return amount
        }

        return if (amount == "0") digit else amount + digit
    }

    // Only one decimal point allowed
    fun appendDot(amount: String): String =
        if (amount.contains('.')) amount else "$amount."

    fun clearAmount(): String = "0"

    fun clear(context: Context, appWidgetId: Int) {
        prefs(context).edit {
            remove("source_$appWidgetId")
                .remove("target_$appWidgetId")
                .remove("amount_$appWidgetId")
        }
    }
}