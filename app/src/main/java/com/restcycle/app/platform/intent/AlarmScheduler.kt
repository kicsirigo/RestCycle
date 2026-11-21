package com.restcycle.app.platform.intent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.restcycle.app.ui.theme.findActivity
import java.time.LocalTime

class AlarmScheduler {

    fun buildSetAlarmIntent(time: LocalTime, message: String): Intent =
        Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, time.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, time.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }

    fun buildSetTimerIntent(durationSeconds: Int, message: String): Intent =
        Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds.coerceAtLeast(0))
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }

    fun launchSetAlarm(context: Context, time: LocalTime, message: String): Boolean =
        launchIntent(context, buildSetAlarmIntent(time, message))

    fun launchSetTimer(context: Context, durationSeconds: Int, message: String): Boolean =
        launchIntent(context, buildSetTimerIntent(durationSeconds, message))

    private fun launchIntent(context: Context, intent: Intent): Boolean {
        if (context.findActivity() == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
