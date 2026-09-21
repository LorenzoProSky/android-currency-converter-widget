package dev.personal.currencyconverter

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RatesRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val success = RatesRepository.fetchFromNetworkAndCache(applicationContext)
        if (!success) {
            return Result.retry()
        }

        // Redraw all currently-placed widget instances
        val manager = AppWidgetManager.getInstance(applicationContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(applicationContext, CurrencyWidgetProvider::class.java)
        )
        for (id in ids) {
            // Rates-only -> Partial update
            CurrencyWidgetProvider.updateResult(applicationContext, manager, id)
        }

        return Result.success()
    }
}