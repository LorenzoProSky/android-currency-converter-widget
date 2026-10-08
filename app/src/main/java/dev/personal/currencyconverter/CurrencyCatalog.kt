package dev.personal.currencyconverter

object CurrencyCatalog {

    data class Currency(val code: String, val displayName: String, val flag: String)

    val ALL: List<Currency> = listOf(
        Currency("AUD", "Australian Dollar", "🇦🇺"),
        Currency("BRL", "Brazilian Real", "🇧🇷"),
        Currency("CAD", "Canadian Dollar", "🇨🇦"),
        Currency("CHF", "Swiss Franc", "🇨🇭"),
        Currency("CNY", "Chinese Yuan", "🇨🇳"),
        Currency("CZK", "Czech Koruna", "🇨🇿"),
        Currency("DKK", "Danish Krone", "🇩🇰"),
        Currency("EUR", "Euro", "🇪🇺"),
        Currency("GBP", "British Pound", "🇬🇧"),
        Currency("HKD", "Hong Kong Dollar", "🇭🇰"),
        Currency("HUF", "Hungarian Forint", "🇭🇺"),
        Currency("IDR", "Indonesian Rupiah", "🇮🇩"),
        Currency("ILS", "Israeli Shekel", "🇮🇱"),
        Currency("INR", "Indian Rupee", "🇮🇳"),
        Currency("ISK", "Icelandic Krona", "🇮🇸"),
        Currency("JPY", "Japanese Yen", "🇯🇵"),
        Currency("KRW", "South Korean Won", "🇰🇷"),
        Currency("MXN", "Mexican Peso", "🇲🇽"),
        Currency("MYR", "Malaysian Ringgit", "🇲🇾"),
        Currency("NOK", "Norwegian Krone", "🇳🇴"),
        Currency("NZD", "New Zealand Dollar", "🇳🇿"),
        Currency("PHP", "Philippine Peso", "🇵🇭"),
        Currency("PLN", "Polish Zloty", "🇵🇱"),
        Currency("RON", "Romanian Leu", "🇷🇴"),
        Currency("SEK", "Swedish Krona", "🇸🇪"),
        Currency("SGD", "Singapore Dollar", "🇸🇬"),
        Currency("THB", "Thai Baht", "🇹🇭"),
        Currency("TRY", "Turkish Lira", "🇹🇷"),
        Currency("USD", "US Dollar", "🇺🇸"),
        Currency("ZAR", "South African Rand", "🇿🇦")
    )

    const val MIN_SELECTABLE = 2
    const val MAX_SELECTABLE = 10
}