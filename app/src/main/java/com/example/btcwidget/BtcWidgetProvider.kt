package com.example.btcwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class BtcWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_MODE = "com.example.btcwidget.ACTION_NEXT_MODE"
        const val ACTION_REFRESH = "com.example.btcwidget.ACTION_REFRESH"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Trigger a background refresh to get the latest data immediately when onUpdate is called
        triggerRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_NEXT_MODE) {
            triggerVibration(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val currentMode = getWidgetMode(context, appWidgetId)
                val nextMode = getNextActiveMode(context, currentMode)
                saveWidgetMode(context, appWidgetId, nextMode)

                // Instantly update the widget view using cached data
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val data = PriceRepository.getFromPrefs(context)
                if (data != null) {
                    updateWidgetLayout(context, appWidgetManager, appWidgetId, data)
                }
            }
        } else if (action == ACTION_REFRESH) {
            triggerVibration(context)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = PriceRepository.fetchPriceData(context)
                    if (data != null) {
                        PriceRepository.saveToPrefs(context, data)
                        updateAllWidgets(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors if permission or hardware fails
        }
    }

    private fun getNextActiveMode(context: Context, currentMode: Int): Int {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        for (i in 1..10) {
            val candidate = (currentMode + i) % 10
            if (prefs.getBoolean("mode_enabled_$candidate", true)) {
                return candidate
            }
        }
        return currentMode
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val data = PriceRepository.getFromPrefs(context) ?: return
        updateWidgetLayout(context, appWidgetManager, appWidgetId, data)
    }

    private fun triggerRefresh(context: Context) {
        val intent = Intent(context, BtcWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, BtcWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        val data = PriceRepository.getFromPrefs(context) ?: return

        for (appWidgetId in appWidgetIds) {
            updateWidgetLayout(context, appWidgetManager, appWidgetId, data)
        }
    }

    private fun updateWidgetLayout(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        data: PriceData
    ) {
        val mode = getWidgetMode(context, appWidgetId)
        val theme = getWidgetTheme(context)
        val views = RemoteViews(context.packageName, R.layout.btc_widget_blockclock)

        // Theme colors and background resource mapping
        val bgResId: Int
        val labelColor: Int
        val valueColor: Int
        val subTextColor: Int

        when (theme) {
            1 -> { // E-Ink Light
                bgResId = R.drawable.widget_background_light
                labelColor = 0xFF71717A.toInt()
                valueColor = 0xFF18181B.toInt()
                subTextColor = 0xFF71717A.toInt()
            }
            2 -> { // Bitcoin Orange
                bgResId = R.drawable.widget_background_orange
                labelColor = 0xFF78350F.toInt()
                valueColor = 0xFF121214.toInt()
                subTextColor = 0xFF78350F.toInt()
            }
            3 -> { // Matrix Green
                bgResId = R.drawable.widget_background_green
                labelColor = 0xFF047857.toInt()
                valueColor = 0xFF10B981.toInt()
                subTextColor = 0xFF047857.toInt()
            }
            4 -> { // Coinkite Gold
                bgResId = R.drawable.widget_background_gold
                labelColor = 0xFF8A6D3B.toInt()
                valueColor = 0xFFC5A059.toInt()
                subTextColor = 0xFF8A6D3B.toInt()
            }
            else -> { // E-Ink Dark (Default)
                bgResId = R.drawable.widget_background_dark
                labelColor = 0xFF8E8E93.toInt()
                valueColor = 0xFFE4E4E7.toInt()
                subTextColor = 0xFF8E8E93.toInt()
            }
        }

        // Apply theme styles to views
        views.setInt(R.id.widget_root, "setBackgroundResource", bgResId)
        views.setTextColor(R.id.txt_blockclock_label_top, labelColor)
        views.setTextColor(R.id.txt_blockclock_label_bottom, labelColor)
        views.setTextColor(R.id.txt_blockclock_value, valueColor)
        views.setTextColor(R.id.txt_blockclock_suffix, labelColor)
        views.setTextColor(R.id.txt_blockclock_sub, subTextColor)
        views.setInt(R.id.btn_refresh, "setColorFilter", subTextColor)
        views.setInt(R.id.lbl_divider, "setBackgroundColor", labelColor)

        // Bind data based on active mode
        val currency = "USD"
        val symbol = "$"

        val labelTopText: String
        val labelBottomText: String
        val valueText: String
        val subText: String
        val showLabel: Boolean
        val showSubText: Boolean
        val showLbl_divider: Boolean
        var suffixText = ""
        var showSuffix = false

        when (mode) {
            0 -> {
                labelTopText = "BTC"
                labelBottomText = currency
                valueText = String.format(Locale.US, "%s%.0f", symbol, data.btcUsd)
                subText = "Market price of BTC"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            1 -> {
                labelTopText = "SATS"
                labelBottomText = "1$currency"
                valueText = String.format(Locale.US, "%d", data.moscowTime)
                subText = "1$symbol per Satoshis"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            2 -> {
                labelTopText = ""
                labelBottomText = ""
                valueText = String.format(Locale.US, "%.0f", data.blockHeight.toDouble())
                subText = "Bitcoin block height"
                showLabel = false
                showSubText = true
                showLbl_divider = false
            }
            3 -> {
                labelTopText = "MSCW"
                labelBottomText = "TIME"
                valueText = String.format(Locale.US, "%d", data.moscowTime)
                subText = ""
                showLabel = true
                showSubText = false
                showLbl_divider = true
            }
            4 -> {
                labelTopText = "MSTR"
                labelBottomText = "BTC"
                valueText = String.format(Locale.US, "%.0f", data.mstrBtcHeld)
                subText = "Strategy Inc. - BTC held"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            5 -> {
                labelTopText = "MSTR"
                labelBottomText = "BTC"
                valueText = String.format(Locale.US, "%.5f", data.mstrBtc)
                subText = "Strategy Inc. - BTC per share"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            6 -> {
                labelTopText = "MSTR"
                labelBottomText = currency
                valueText = String.format(Locale.US, "%s%.0f", symbol, data.mstrUsd)
                subText = "Strategy Inc. - Share price"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            7 -> {
                labelTopText = "BTC"
                labelBottomText = ""
                val circInMillions = data.btcCirculation / 1_000_000.0
                valueText = String.format(Locale.US, "%.3f", circInMillions)
                suffixText = "MIL"
                showSuffix = true
                subText = "Bitcoin in circulation"
                showLabel = true
                showSubText = true
                showLbl_divider = false
            }
            8 -> { // Halving Countdown
                labelTopText = "HALV"
                labelBottomText = "BLKS"
                val halvingRemaining = 210000 - (data.blockHeight % 210000)
                valueText = String.format(Locale.US, "%,d", halvingRemaining)
                subText = "Blocks to next halving"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            9 -> { // Mempool Priority Fees
                labelTopText = "FEES"
                labelBottomText = "sat/vB"
                valueText = String.format(Locale.US, "%d", data.feeFastest)
                subText = String.format(Locale.US, "Mid: %d / Low: %d sat/vB", data.feeHalfHour, data.feeHour)
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
            else -> {
                labelTopText = "BTC"
                labelBottomText = currency
                valueText = String.format(Locale.US, "%s%.0f", symbol, data.btcUsd)
                subText = "Market price of BTC"
                showLabel = true
                showSubText = true
                showLbl_divider = true
            }
        }

        // Set label texts
        views.setTextViewText(R.id.txt_blockclock_label_top, labelTopText)
        views.setTextViewText(R.id.txt_blockclock_label_bottom, labelBottomText)
        
        // Toggle stacked label container visibility
        if (showLabel) {
            views.setViewVisibility(R.id.lbl_stacked_container, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.lbl_stacked_container, View.GONE)
        }

        // Toggle subtext container visibility
        if (showSubText) {
            views.setViewVisibility(R.id.txt_blockclock_sub, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.txt_blockclock_sub, View.GONE)
        }

        // Toggle lbl divider visilibity
        if (showLbl_divider) {
            views.setViewVisibility(R.id.lbl_divider, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.lbl_divider, View.GONE)
        }

        views.setTextViewText(R.id.txt_blockclock_value, valueText)
        views.setTextViewText(R.id.txt_blockclock_sub, subText)

        // Set suffix text and visibility
        views.setTextViewText(R.id.txt_blockclock_suffix, suffixText)
        if (showSuffix) {
            views.setViewVisibility(R.id.lbl_suffix_container, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.lbl_suffix_container, View.GONE)
        }

        // Show/hide the offline warning symbol based on whether lastFetchSuccessful is false
        if (!data.lastFetchSuccessful) {
            views.setViewVisibility(R.id.txt_warning, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.txt_warning, View.GONE)
        }

        // Bind navigation click intents (Tapping on central layout cycles mode)
        views.setOnClickPendingIntent(R.id.center_layout, getPendingSelfIntent(context, appWidgetId, ACTION_NEXT_MODE))
        // Bind refresh click intent
        views.setOnClickPendingIntent(R.id.btn_refresh, getPendingSelfIntent(context, appWidgetId, ACTION_REFRESH))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingSelfIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, BtcWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val requestCode = appWidgetId * 10 + when (action) {
            ACTION_NEXT_MODE -> 2
            ACTION_REFRESH -> 3
            else -> 0
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun getWidgetMode(context: Context, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("mode_$appWidgetId", 0) // Default to mode 0 (BTC/USD)
    }

    private fun saveWidgetMode(context: Context, appWidgetId: Int, mode: Int) {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("mode_$appWidgetId", mode).apply()
    }

    private fun getWidgetTheme(context: Context): Int {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("blockclock_theme", 0) // Default to 0 (E-Ink Dark)
    }
}
