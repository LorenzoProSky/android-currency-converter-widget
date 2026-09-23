package dev.personal.currencyconverter

import android.content.Context
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

import dev.personal.currencyconverter.CurrencyConstants.DEFAULT_SOURCE
import dev.personal.currencyconverter.CurrencyConstants.EXCHANGE_RATE_URL
import dev.personal.currencyconverter.CurrencyConstants.TARGET_SYMBOLS

object RatesRepository {

    private const val PREFS_NAME = "currency_widget_prefs"
    private const val KEY_RATES_JSON = "rates_json"
    private const val KEY_TIMESTAMP = "rates_timestamp"
    private const val REFRESH_WORK_NAME = "refresh_rates"
    private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 60 minutes

    private suspend fun fetchFromNetwork(): Map<String, Double>? = withContext(Dispatchers.IO) {
        try {
            val symbols = TARGET_SYMBOLS.joinToString(",")
            val url = URL("$EXCHANGE_RATE_URL?base=$DEFAULT_SOURCE&symbols=$symbols")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val ratesJson = JSONObject(body).getJSONObject("rates")

            // EUR is the base currency (implicitly 1.0). This lets us convert every currency uniformly
            val result = mutableMapOf("EUR" to 1.0)
            ratesJson.keys().forEach { key -> result[key] = ratesJson.getDouble(key) }
            result
        } catch (e: Exception) {
            null
        }
    }

    @Volatile
    private var memoryCache: Pair<Map<String, Double>, Long>? = null

    private fun readCache(context: Context): Pair<Map<String, Double>, Long>? {
        memoryCache?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RATES_JSON, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)

        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Double>()
            obj.keys().forEach { key -> map[key] = obj.getDouble(key) }
            (map to timestamp).also { memoryCache = it }
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(context: Context, rates: Map<String, Double>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        rates.forEach { (key, value) -> obj.put(key, value) }
        val timestamp = System.currentTimeMillis()
        prefs.edit {
            putString(KEY_RATES_JSON, obj.toString())
                .putLong(KEY_TIMESTAMP, timestamp)
        }
        memoryCache = rates to timestamp
    }

    fun isCacheStale(context: Context): Boolean {
        val cached = readCache(context) ?: return true
        return (System.currentTimeMillis() - cached.second) >= CACHE_DURATION_MS
    }

    fun refreshCacheAsync(context: Context) {
        if (!isCacheStale(context)) return

        val request = OneTimeWorkRequestBuilder<RatesRefreshWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // ExistingWorkPolicy.KEEP = if a refresh is already pending or running -> no-op
        WorkManager.getInstance(context)
            .enqueueUniqueWork(REFRESH_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    suspend fun fetchFromNetworkAndCache(context: Context): Boolean {
        val fresh = fetchFromNetwork() ?: return false
        writeCache(context, fresh)
        return true
    }

    /**
     * Converts using EUR as the pivot currency: source -> EUR -> target.
     * Null = no cache yet -> caller shows "NO DATA"
     **/
    fun convert(amount: Double, from: String, to: String, context: Context): Double? {
        val rates = readCache(context)?.first ?: return null // No fallback

        if (from == to) return amount
        val rateFrom = rates[from] ?: return amount
        val rateTo = rates[to] ?: return amount
        val amountInEur = amount / rateFrom
        return amountInEur * rateTo
    }
}