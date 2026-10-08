package dev.personal.currencyconverter

object CurrencyConstants {
    const val DEFAULT_SOURCE = "EUR"
    const val DEFAULT_TARGET = "USD"
    val DEFAULT_CURRENCIES = listOf("EUR", "USD")
    const val MAX_AMOUNT_LENGTH = 9 // caps input amount at 999,999,999

    const val EXCHANGE_RATE_URL = "https://api.frankfurter.dev/v1/latest"
}