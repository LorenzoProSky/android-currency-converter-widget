package dev.personal.currencyconverter

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.edit

object RatesRepository {

    // EUR is the base currency (implicitly 1.0). This lets us convert every currency uniformly
    private val TARGET_SYMBOLS = listOf("USD", "PLN", "CHF", "GBP", "INR")
    private const val PREFS_NAME = "currency_widget_prefs"
    private const val KEY_RATES_JSON = "rates_json"
    private const val KEY_TIMESTAMP = "rates_timestamp"
    private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 60 minutes

    private suspend fun fetchFromNetwork(): Map<String, Double>? = withContext(Dispatchers.IO) {
        try {
            val symbols = TARGET_SYMBOLS.joinToString(",")
            val url = URL("https://api.frankfurter.dev/v1/latest?base=EUR&symbols=$symbols")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val ratesJson = JSONObject(body).getJSONObject("rates")

            val result = mutableMapOf("EUR" to 1.0)
            ratesJson.keys().forEach { key -> result[key] = ratesJson.getDouble(key) }
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun readCache(context: Context): Pair<Map<String, Double>, Long>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RATES_JSON, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)

        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Double>()
            obj.keys().forEach { key -> map[key] = obj.getDouble(key) }
            map to timestamp
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(context: Context, rates: Map<String, Double>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        rates.forEach { (key, value) -> obj.put(key, value) }
        prefs.edit {
            putString(KEY_RATES_JSON, obj.toString())
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }
    }

    /** Returns the best available EUR-pivoted rates map. */
    suspend fun getRates(context: Context, forceRefresh: Boolean = false): Map<String, Double> {
        val cached = readCache(context)
        val isFresh = cached != null && (System.currentTimeMillis() - cached.second) < CACHE_DURATION_MS

        if (isFresh && !forceRefresh) {
            return cached.first
        }

        val fresh = fetchFromNetwork()
        if (fresh != null) {
            writeCache(context, fresh)
            return fresh
        }

        return cached?.first ?: mapOf("EUR" to 1.0)
    }

    /** Converts using EUR as the pivot currency: source -> EUR -> target. */
    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double {
        if (from == to) return amount
        val rateFrom = rates[from] ?: return amount
        val rateTo = rates[to] ?: return amount
        val amountInEur = amount / rateFrom
        return amountInEur * rateTo
    }
}