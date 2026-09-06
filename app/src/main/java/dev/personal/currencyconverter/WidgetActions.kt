package dev.personal.currencyconverter

object WidgetActions {
    const val ACTION_CYCLE_SOURCE = "dev.personal.currencyconverter.ACTION_CYCLE_SOURCE"
    const val ACTION_CYCLE_TARGET = "dev.personal.currencyconverter.ACTION_CYCLE_TARGET"
    const val ACTION_SWAP = "dev.personal.currencyconverter.ACTION_SWAP"
    const val ACTION_KEYPAD = "dev.personal.currencyconverter.ACTION_KEYPAD"
    val KNOWN_ACTIONS =
        setOf(ACTION_CYCLE_SOURCE, ACTION_CYCLE_TARGET, ACTION_SWAP, ACTION_KEYPAD)

    const val EXTRA_KEY = "extra_key"
    val digitKeyIds = mapOf(
        "0" to R.id.key0, "1" to R.id.key1, "2" to R.id.key2, "3" to R.id.key3,
        "4" to R.id.key4, "5" to R.id.key5, "6" to R.id.key6, "7" to R.id.key7,
        "8" to R.id.key8, "9" to R.id.key9
    )
}