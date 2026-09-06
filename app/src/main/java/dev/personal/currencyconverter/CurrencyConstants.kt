package dev.personal.currencyconverter

object CurrencyConstants {
    const val DEFAULT_SOURCE = "EUR"
    const val DEFAULT_TARGET = "USD"
    val ALL_CURRENCIES = listOf("EUR", "USD", "PLN", "CHF", "GBP", "INR")
    val TARGET_SYMBOLS = listOf("USD", "PLN", "CHF", "GBP", "INR") // EUR is the base currency

    const val EXCHANGE_RATE_URL = "https://api.frankfurter.dev/v1/latest"
}