package dev.personal.currencyconverter

import android.app.Application
import androidx.work.Configuration

class CurrencyConverterApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}