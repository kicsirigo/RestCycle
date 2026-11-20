package com.restcycle.app.platform.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.restcycle.app.MainActivity
import com.restcycle.app.R
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.ui.util.formatClockTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class RestCycleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun refreshAllWidgets(
            context: Context,
            sleepLatencyMinutes: Int = SleepCycleCalculator.DEFAULT_SLEEP_LATENCY_MINUTES,
            is24HourFormat: Boolean = DateFormat.is24HourFormat(context)
        ) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, RestCycleWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            updateWidgets(
                context = context,
                appWidgetManager = manager,
                appWidgetIds = ids,
                sleepLatencyMinutes = sleepLatencyMinutes,
                is24HourFormat = is24HourFormat
            )
        }

        internal fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            sleepLatencyMinutes: Int = SleepCycleCalculator.DEFAULT_SLEEP_LATENCY_MINUTES,
            is24HourFormat: Boolean = DateFormat.is24HourFormat(context),
            now: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
        ) {
            val wake5 = now.plusMinutes(sleepLatencyMinutes.toLong() + 5L * 90L)
            val wake6 = now.plusMinutes(sleepLatencyMinutes.toLong() + 6L * 90L)
            val text5 = context.getString(
                R.string.widget_cycle_format,
                5,
                formatClockTime(wake5, is24HourFormat)
            )
            val text6 = context.getString(
                R.string.widget_cycle_format,
                6,
                formatClockTime(wake6, is24HourFormat)
            )

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                WIDGET_REQUEST_CODE,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_restcycle).apply {
                    setTextViewText(R.id.widget_cycle_5, text5)
                    setTextViewText(R.id.widget_cycle_6, text6)
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private const val WIDGET_REQUEST_CODE = 4102
    }
}
