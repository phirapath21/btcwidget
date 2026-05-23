package com.example.btcwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

object WidgetAlarmManager {
    private const val TAG = "WidgetAlarmManager"
    private const val REQUEST_CODE = 9999

    fun scheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available")
            return
        }

        val prefs = context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("refresh_interval", 30) // Default to 30 minutes
        val intervalMillis = intervalMinutes * 60 * 1000L

        val intent = Intent(context, BtcWidgetProvider::class.java).apply {
            action = BtcWidgetProvider.ACTION_REFRESH
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

        // Cancel existing alarm
        alarmManager.cancel(pendingIntent)

        val triggerAtMillis = SystemClock.elapsedRealtime() + intervalMillis

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled alarm repeating every $intervalMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BtcWidgetProvider::class.java).apply {
            action = BtcWidgetProvider.ACTION_REFRESH
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled background alarm")
    }

    fun checkAndCancelAlarmIfNeeded(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        val singleIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BtcWidgetProvider::class.java))
        val quoteIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BtcQuoteWidgetProvider::class.java))
        val halvingIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BtcHalvingWidgetProvider::class.java))

        if (singleIds.isEmpty() && quoteIds.isEmpty() && halvingIds.isEmpty()) {
            cancelAlarm(context)
        }
    }
}
