package com.restcycle.app.platform.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object BedtimeReminderScheduler {
    const val EXTRA_BEDTIME_FORMATTED = "extra_bedtime_formatted"
    const val LEAD_TIME_MINUTES = 20L
    private const val REMINDER_WINDOW_MS = 120_000L
    private const val REMINDER_REQUEST_CODE = 5209

    fun computeTriggerEpochMillis(
        bedtime: LocalTime,
        leadTimeMinutes: Long = LEAD_TIME_MINUTES,
        now: ZonedDateTime = ZonedDateTime.now(),
        zoneId: ZoneId = now.zone
    ): Long {
        val reminderLocalTime = bedtime.minusMinutes(leadTimeMinutes)
        var candidate = ZonedDateTime.of(LocalDate.from(now), reminderLocalTime, zoneId)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun scheduleReminder(
        context: Context,
        bedtime: LocalTime,
        formattedBedtime: String
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        val triggerAtMillis = computeTriggerEpochMillis(bedtime)
        val pendingIntent = buildPendingIntent(context, formattedBedtime)
        return try {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                REMINDER_WINDOW_MS,
                pendingIntent
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildPendingIntent(context, "")
        runCatching {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(context: Context, formattedBedtime: String): PendingIntent {
        val intent = Intent(context, BedtimeReminderReceiver::class.java).apply {
            putExtra(EXTRA_BEDTIME_FORMATTED, formattedBedtime)
        }
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
