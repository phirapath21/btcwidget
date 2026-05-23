package com.example.btcwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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
import android.util.TypedValue
import java.util.Locale

class BtcHalvingWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_MODE = "com.example.btcwidget.ACTION_NEXT_MODE_HALVING"
        const val ACTION_REFRESH = "com.example.btcwidget.ACTION_REFRESH"

        fun updateWidgetLayout(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            data: PriceData
        ) {
            val theme = getWidgetTheme(context)
            val mode = getWidgetMode(context, appWidgetId)

            val views = RemoteViews(context.packageName, R.layout.btc_widget_halving)

            // Dynamic Bounds
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).let { if (it == 0) 110 else it }
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).let { if (it == 0) 40 else it }

            // Dynamic Progress Bar length
            val barWidth = when {
                width < 150 -> 10
                width < 250 -> 15
                else -> 25
            }

            // Dynamic Font Sizing & Constraints
            data class LayoutSizes(val valSize: Float, val barSize: Float, val subSize: Float, val showFooter: Boolean)
            val sizes = if (height < 60) {
                LayoutSizes((width * 0.12f).coerceIn(16f, 26f), (width * 0.07f).coerceIn(9f, 12f), 8f, false)
            } else {
                LayoutSizes((width * 0.14f).coerceIn(20f, 36f), (width * 0.08f).coerceIn(10f, 14f), (width * 0.05f).coerceIn(8.5f, 11f), true)
            }

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
                5 -> { // Terminal Amber
                    bgResId = R.drawable.widget_background_amber
                    labelColor = 0xFF805800.toInt()
                    valueColor = 0xFFFFB000.toInt()
                    subTextColor = 0xFF805800.toInt()
                }
                6 -> { // Cyberpunk
                    bgResId = R.drawable.widget_background_cyberpunk
                    labelColor = 0xFF00F5FF.toInt()
                    valueColor = 0xFFFF007F.toInt()
                    subTextColor = 0xFF00F5FF.toInt()
                }
                7 -> { // Midnight Blue
                    bgResId = R.drawable.widget_background_midnight
                    labelColor = 0xFF64748B.toInt()
                    valueColor = 0xFF38BDF8.toInt()
                    subTextColor = 0xFF64748B.toInt()
                }
                8 -> { // Cypherpunk
                    bgResId = R.drawable.widget_background_cypherpunk
                    labelColor = 0xFF880000.toInt()
                    valueColor = 0xFFFF0000.toInt()
                    subTextColor = 0xFF880000.toInt()
                }
                9 -> { // Orange Pill
                    bgResId = R.drawable.widget_background_orangepill
                    labelColor = 0xFFA66210.toInt()
                    valueColor = 0xFFF7931A.toInt()
                    subTextColor = 0xFFA66210.toInt()
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
            views.setTextColor(R.id.txt_halving_label_top, labelColor)
            views.setTextColor(R.id.txt_halving_label_bottom, labelColor)
            views.setTextColor(R.id.txt_halving_value, valueColor)
            views.setTextColor(R.id.txt_halving_progress_bar, valueColor)
            views.setTextColor(R.id.txt_halving_sub, subTextColor)
            views.setInt(R.id.lbl_divider, "setBackgroundColor", labelColor)

            // Computations
            val epochBlocksCompleted = data.blockHeight % 210000
            val fraction = epochBlocksCompleted.toDouble() / 210000.0
            val epochProgressPercent = fraction * 100.0
            val blocksRemaining = 210000 - epochBlocksCompleted
            val remainingDays = blocksRemaining / 144.0 // 144 blocks per day (10 min avg)

            // Generate progress bar [██████░░░░░]
            val filledCount = (fraction * barWidth).toInt().coerceIn(0, barWidth)
            val emptyCount = barWidth - filledCount
            val barText = "[" + "█".repeat(filledCount) + "░".repeat(emptyCount) + "]"

            val labelTop: String
            val labelBottom: String
            val valueText: String
            val footerText: String

            when (mode) {
                1 -> { // Estimated days remaining
                    labelTop = "HALV"
                    labelBottom = "ETA"
                    valueText = String.format(Locale.US, "%,.0fd", remainingDays)
                    footerText = String.format(Locale.US, "%.1f%% progress • %,d blks remaining", epochProgressPercent, blocksRemaining)
                }
                2 -> { // Blocks remaining
                    labelTop = "HALV"
                    labelBottom = "BLKS"
                    valueText = String.format(Locale.US, "%,d", blocksRemaining)
                    footerText = String.format(Locale.US, "%.1f%% progress • %,.0f days remaining", epochProgressPercent, remainingDays)
                }
                else -> { // Mode 0: Epoch progress percentage
                    labelTop = "HALV"
                    labelBottom = "PCT"
                    valueText = String.format(Locale.US, "%.2f%%", epochProgressPercent)
                    footerText = String.format(Locale.US, "%,d blks • %,.0f days remaining", blocksRemaining, remainingDays)
                }
            }

            views.setTextViewText(R.id.txt_halving_label_top, labelTop)
            views.setTextViewText(R.id.txt_halving_label_bottom, labelBottom)
            views.setTextViewText(R.id.txt_halving_value, valueText)
            views.setTextViewText(R.id.txt_halving_progress_bar, barText)
            views.setTextViewText(R.id.txt_halving_sub, footerText.lowercase(Locale.US))

            // Apply responsive text sizes
            views.setTextViewTextSize(R.id.txt_halving_value, TypedValue.COMPLEX_UNIT_SP, sizes.valSize)
            views.setTextViewTextSize(R.id.txt_halving_progress_bar, TypedValue.COMPLEX_UNIT_SP, sizes.barSize)
            views.setTextViewTextSize(R.id.txt_halving_sub, TypedValue.COMPLEX_UNIT_SP, sizes.subSize)

            // Dynamic visibility for subtext/footer
            views.setViewVisibility(R.id.txt_halving_sub, if (sizes.showFooter) View.VISIBLE else View.GONE)

            // Tap layout to cycle display mode
            views.setOnClickPendingIntent(R.id.center_layout, getPendingSelfIntent(context, appWidgetId, ACTION_NEXT_MODE))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingSelfIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, BtcHalvingWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val requestCode = appWidgetId * 40 + when (action) {
                ACTION_NEXT_MODE -> 2
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
            return prefs.getInt("halving_mode_$appWidgetId", 0)
        }

        private fun saveWidgetMode(context: Context, appWidgetId: Int, mode: Int) {
            val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("halving_mode_$appWidgetId", mode).apply()
        }

        private fun getWidgetTheme(context: Context): Int {
            val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
            return prefs.getInt("blockclock_theme", 0)
        }

        fun triggerVibration(context: Context) {
            val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("haptic_enabled", true)) {
                return
            }
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
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = PriceRepository.getFromPrefs(context) ?: return
        for (id in appWidgetIds) {
            updateWidgetLayout(context, appWidgetManager, id, data)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_NEXT_MODE) {
            triggerVibration(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val currentMode = getWidgetMode(context, appWidgetId)
                val nextMode = (currentMode + 1) % 3
                saveWidgetMode(context, appWidgetId, nextMode)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val data = PriceRepository.getFromPrefs(context)
                if (data != null) {
                    updateWidgetLayout(context, appWidgetManager, appWidgetId, data)
                }
            }
        } else if (action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = android.content.ComponentName(context, BtcHalvingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val data = PriceRepository.getFromPrefs(context) ?: return
            for (id in appWidgetIds) {
                updateWidgetLayout(context, appWidgetManager, id, data)
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

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetAlarmManager.scheduleAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetAlarmManager.checkAndCancelAlarmIfNeeded(context)
    }
}
