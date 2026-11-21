package com.restcycle.app.platform.time

import android.content.Context
import android.text.format.DateFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.Clock
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AndroidTimeFormatProvider(
    context: Context,
    private val clock: Clock? = null
) {
    private val appContext: Context = context.applicationContext

    private fun activeClock(): Clock = clock ?: Clock.systemDefaultZone()

    fun is24HourFormat(): Boolean = DateFormat.is24HourFormat(appContext)

    fun currentTime(): LocalTime =
        LocalTime.now(activeClock()).truncatedTo(ChronoUnit.MINUTES)

    fun minuteTickFlow(): Flow<LocalTime> = flow {
        while (currentCoroutineContext().isActive) {
            val rawNow = LocalTime.now(activeClock())
            emit(rawNow.truncatedTo(ChronoUnit.MINUTES))
            delay(millisUntilNextMinute(rawNow))
        }
    }.distinctUntilChanged()

    internal companion object {
        private const val NANOS_PER_MINUTE = 60_000_000_000L
        private const val NANOS_PER_SECOND = 1_000_000_000L
        private const val NANOS_PER_MILLI = 1_000_000L

        fun millisUntilNextMinute(now: LocalTime): Long {
            val nanosIntoMinute = now.second * NANOS_PER_SECOND + now.nano
            val remainingNanos = NANOS_PER_MINUTE - nanosIntoMinute
            val remainingMillis = (remainingNanos + NANOS_PER_MILLI - 1L) / NANOS_PER_MILLI
            return remainingMillis.coerceAtLeast(1L)
        }
    }
}
