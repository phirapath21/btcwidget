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
import kotlinx.coroutines.delay
import java.util.Locale

class BtcDualWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_MODE_LEFT = "com.example.btcwidget.ACTION_NEXT_MODE_LEFT"
        const val ACTION_NEXT_MODE_RIGHT = "com.example.btcwidget.ACTION_NEXT_MODE_RIGHT"
        const val ACTION_REFRESH = "com.example.btcwidget.ACTION_REFRESH"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        triggerRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_NEXT_MODE_LEFT || action == ACTION_NEXT_MODE_RIGHT) {
            triggerVibration(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val isLeft = action == ACTION_NEXT_MODE_LEFT
                val currentMode = getWidgetMode(context, appWidgetId, isLeft)
                val nextMode = getNextActiveMode(context, currentMode)
                saveWidgetMode(context, appWidgetId, isLeft, nextMode)

                // Instantly update layout
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val data = PriceRepository.getFromPrefs(context)
                if (data != null) {
                    updateWidgetLayout(context, appWidgetManager, appWidgetId, data)
                }
            }
        } else if (action == ACTION_REFRESH) {
            triggerVibration(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                CoroutineScope(Dispatchers.Main).launch {
                    for (angle in 0..360 step 30) {
                        val views = RemoteViews(context.packageName, R.layout.btc_widget_dual)
                        views.setFloat(R.id.btn_refresh_left, "setRotation", angle.toFloat())
                        views.setFloat(R.id.btn_refresh_right, "setRotation", angle.toFloat())
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        delay(40)
                    }
                    val views = RemoteViews(context.packageName, R.layout.btc_widget_dual)
                    views.setFloat(R.id.btn_refresh_left, "setRotation", 0f)
                    views.setFloat(R.id.btn_refresh_right, "setRotation", 0f)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                }
            }
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
        val intent = Intent(context, BtcDualWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, BtcDualWidgetProvider::class.java)
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
        val leftMode = getWidgetMode(context, appWidgetId, true)
        val rightMode = getWidgetMode(context, appWidgetId, false)
        val theme = getWidgetTheme(context)
        
        val views = RemoteViews(context.packageName, R.layout.btc_widget_dual)

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

        // Apply theme backgrounds to left & right panel roots
        views.setInt(R.id.left_panel, "setBackgroundResource", bgResId)
        views.setInt(R.id.right_panel, "setBackgroundResource", bgResId)

        // Update left panel
        updatePanel(context, views, true, leftMode, data, theme, labelColor, valueColor, subTextColor)

        // Update right panel
        updatePanel(context, views, false, rightMode, data, theme, labelColor, valueColor, subTextColor)

        // Bind navigation click intents for left/right cycling
        views.setOnClickPendingIntent(R.id.center_layout_left, getPendingSelfIntent(context, appWidgetId, ACTION_NEXT_MODE_LEFT))
        views.setOnClickPendingIntent(R.id.center_layout_right, getPendingSelfIntent(context, appWidgetId, ACTION_NEXT_MODE_RIGHT))

        // Bind refresh click intents on both panels
        views.setOnClickPendingIntent(R.id.btn_refresh_left, getPendingSelfIntent(context, appWidgetId, ACTION_REFRESH))
        views.setOnClickPendingIntent(R.id.btn_refresh_right, getPendingSelfIntent(context, appWidgetId, ACTION_REFRESH))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updatePanel(
        context: Context,
        views: RemoteViews,
        isLeft: Boolean,
        mode: Int,
        data: PriceData,
        theme: Int,
        labelColor: Int,
        valueColor: Int,
        subTextColor: Int
    ) {
        val labelTopId = if (isLeft) R.id.txt_blockclock_label_top_left else R.id.txt_blockclock_label_top_right
        val labelBottomId = if (isLeft) R.id.txt_blockclock_label_bottom_left else R.id.txt_blockclock_label_bottom_right
        val valueId = if (isLeft) R.id.txt_blockclock_value_left else R.id.txt_blockclock_value_right
        val suffixId = if (isLeft) R.id.txt_blockclock_suffix_left else R.id.txt_blockclock_suffix_right
        val subId = if (isLeft) R.id.txt_blockclock_sub_left else R.id.txt_blockclock_sub_right
        val dividerId = if (isLeft) R.id.lbl_divider_left else R.id.lbl_divider_right
        val labelContainerId = if (isLeft) R.id.lbl_stacked_container_left else R.id.lbl_stacked_container_right
        val suffixContainerId = if (isLeft) R.id.lbl_suffix_container_left else R.id.lbl_suffix_container_right
        val warningId = if (isLeft) R.id.txt_warning_left else R.id.txt_warning_right

        // Set colors
        views.setTextColor(labelTopId, labelColor)
        views.setTextColor(labelBottomId, labelColor)
        views.setTextColor(valueId, valueColor)
        views.setTextColor(suffixId, labelColor)
        views.setTextColor(subId, subTextColor)
        views.setInt(dividerId, "setBackgroundColor", labelColor)
        views.setInt(if (isLeft) R.id.btn_refresh_left else R.id.btn_refresh_right, "setColorFilter", subTextColor)

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

        views.setTextViewText(labelTopId, labelTopText)
        views.setTextViewText(labelBottomId, labelBottomText)
        views.setViewVisibility(labelContainerId, if (showLabel) View.VISIBLE else View.GONE)
        views.setViewVisibility(subId, if (showSubText) View.VISIBLE else View.GONE)
        views.setViewVisibility(dividerId, if (showLbl_divider) View.VISIBLE else View.GONE)
        views.setTextViewText(valueId, valueText)
        views.setTextViewText(subId, subText)
        views.setTextViewText(suffixId, suffixText)
        views.setViewVisibility(suffixContainerId, if (showSuffix) View.VISIBLE else View.GONE)

        // Toggle warning visibility based on lastFetchSuccessful
        views.setViewVisibility(warningId, if (!data.lastFetchSuccessful) View.VISIBLE else View.GONE)
    }

    private fun getPendingSelfIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, BtcDualWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val requestCode = appWidgetId * 20 + when (action) {
            ACTION_NEXT_MODE_LEFT -> 2
            ACTION_NEXT_MODE_RIGHT -> 3
            ACTION_REFRESH -> 4
            else -> 0
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun getWidgetMode(context: Context, appWidgetId: Int, isLeft: Boolean): Int {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        val key = if (isLeft) "mode_left_$appWidgetId" else "mode_right_$appWidgetId"
        return prefs.getInt(key, if (isLeft) 0 else 1)
    }

    private fun saveWidgetMode(context: Context, appWidgetId: Int, isLeft: Boolean, mode: Int) {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        val key = if (isLeft) "mode_left_$appWidgetId" else "mode_right_$appWidgetId"
        prefs.edit().putInt(key, mode).apply()
    }

    private fun getWidgetTheme(context: Context): Int {
        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("blockclock_theme", 0) // Default to 0 (E-Ink Dark)
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
            // Ignore
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
}
