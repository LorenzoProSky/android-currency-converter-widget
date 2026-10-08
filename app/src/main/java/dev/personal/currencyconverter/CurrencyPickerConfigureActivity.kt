package dev.personal.currencyconverter

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class CurrencyPickerConfigureActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val checkboxes = mutableMapOf<String, CheckBox>()
    private lateinit var countLabel: TextView
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_currency_picker)
        countLabel = findViewById(R.id.countLabel)
        confirmButton = findViewById(R.id.confirmButton)
        val list = findViewById<LinearLayout>(R.id.currencyList)

        // Brand-new widget -> nothing stored -> falls back to EUR/USD.
        // Reconfiguring an existing widget -> shows its actual current selection.
        val preselected = WidgetState.getCurrencies(this, appWidgetId).toSet()

        CurrencyCatalog.ALL.forEach { currency ->
            val box = CheckBox(this).apply {
                "${currency.flag}  ${currency.code} — ${currency.displayName}".also { text = it }
                textSize = 16f
                setTextColor(getColor(R.color.black))
                setPadding(0, 24, 0, 24)
                buttonTintList = ColorStateList.valueOf(getColor(R.color.accent))
                isChecked = currency.code in preselected
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked && selectedCount() > CurrencyCatalog.MAX_SELECTABLE) {
                        this.isChecked = false
                        Toast.makeText(
                            this@CurrencyPickerConfigureActivity,
                            "You can select up to ${CurrencyCatalog.MAX_SELECTABLE} currencies",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnCheckedChangeListener
                    }
                    updateCountLabel()
                }
            }
            checkboxes[currency.code] = box
            list.addView(box)
        }

        confirmButton.setOnClickListener { confirm() }
        updateCountLabel()
    }

    private fun selectedCount(): Int = checkboxes.values.count { it.isChecked }

    private fun updateCountLabel() {
        val count = selectedCount()
        "$count / ${CurrencyCatalog.MAX_SELECTABLE} selected".also { countLabel.text = it }
        confirmButton.isEnabled = count >= CurrencyCatalog.MIN_SELECTABLE
    }

    private fun confirm() {
        val selected = checkboxes.filterValues { it.isChecked }.keys.toList()
        if (selected.size < CurrencyCatalog.MIN_SELECTABLE) return

        WidgetState.setCurrencies(this, appWidgetId, selected)

        val existingSource = WidgetState.getSource(this, appWidgetId)
        val existingTarget = WidgetState.getTarget(this, appWidgetId)
        if (existingSource !in selected || existingTarget !in selected) {
            val source = if ("EUR" in selected) "EUR" else selected[0]
            val target = if ("USD" in selected && "USD" != source) "USD" else selected.first { it != source }
            WidgetState.setSource(this, appWidgetId, source)
            WidgetState.setTarget(this, appWidgetId, target)
        }

        CurrencyWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), appWidgetId)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}