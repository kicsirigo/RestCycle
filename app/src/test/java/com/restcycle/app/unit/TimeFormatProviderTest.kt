package com.restcycle.app.unit

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.platform.time.AndroidTimeFormatProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TimeFormatProviderTest {

    private class MutableClock(
        private var currentInstant: Instant,
        private val zoneId: ZoneId = ZoneOffset.UTC
    ) : Clock() {
        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)
        override fun instant(): Instant = currentInstant
        fun advanceSeconds(seconds: Long) {
            currentInstant = currentInstant.plusSeconds(seconds)
        }
    }

    @Test
    fun is24HourFormat_reflectsSystemSettingChanges() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val provider = AndroidTimeFormatProvider(app)

        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "24")
        assertTrue(provider.is24HourFormat())

        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        assertFalse(provider.is24HourFormat())

        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "24")
        assertTrue(provider.is24HourFormat())
    }

    @Test
    fun currentTime_truncatesSecondsAndNanos() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = Clock.fixed(Instant.parse("2026-11-19T22:15:47.987654321Z"), ZoneOffset.UTC)
        val provider = AndroidTimeFormatProvider(app, clock)

        assertEquals(LocalTime.of(22, 15), provider.currentTime())
    }

    @Test
    fun millisUntilNextMinute_roundsBoundaryValuesProperly() {
        assertEquals(60_000L, AndroidTimeFormatProvider.millisUntilNextMinute(LocalTime.of(12, 0, 0, 0)))
        assertEquals(15_000L, AndroidTimeFormatProvider.millisUntilNextMinute(LocalTime.of(12, 0, 45, 0)))
        assertEquals(1L, AndroidTimeFormatProvider.millisUntilNextMinute(LocalTime.of(12, 0, 59, 999_500_000)))
        assertEquals(2L, AndroidTimeFormatProvider.millisUntilNextMinute(LocalTime.of(12, 0, 59, 998_500_000)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun minuteTickFlow_emitsMinuteAlignedTicksAndDeduplicates() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = MutableClock(Instant.parse("2026-11-19T22:15:40Z"))
        val provider = AndroidTimeFormatProvider(app, clock)

        val emissions = mutableListOf<LocalTime>()
        val job = launch {
            provider.minuteTickFlow().take(3).toList(emissions)
        }

        runCurrent()
        assertEquals(listOf(LocalTime.of(22, 15)), emissions)

        // Advance past the remaining 20 seconds to 22:16:00
        clock.advanceSeconds(20)
        advanceTimeBy(20_000L)
        runCurrent()
        assertEquals(listOf(LocalTime.of(22, 15), LocalTime.of(22, 16)), emissions)

        // Advance another 60 seconds to 22:17:00
        clock.advanceSeconds(60)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(
            listOf(LocalTime.of(22, 15), LocalTime.of(22, 16), LocalTime.of(22, 17)),
            emissions
        )
        job.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun defaultClock_reflectsSubsequentTimeZoneSetDefaultChangesInCurrentTimeAndMinuteTickFlow() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val provider = AndroidTimeFormatProvider(app)

            val initialCurrent = provider.currentTime()
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"))
            val shiftedCurrent = provider.currentTime()
            val currentDeltaMinutes =
                ((shiftedCurrent.toSecondOfDay() / 60 - initialCurrent.toSecondOfDay() / 60) + 1440) % 1440
            assertTrue(
                "Expected +180..181 minute shift in currentTime() after TimeZone.setDefault, got $currentDeltaMinutes",
                currentDeltaMinutes in 180..181
            )

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val emissions = mutableListOf<LocalTime>()
            val job = launch {
                provider.minuteTickFlow().take(2).toList(emissions)
            }

            runCurrent()
            assertEquals(1, emissions.size)

            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"))
            advanceTimeBy(60_000L)
            runCurrent()

            assertEquals(2, emissions.size)
            val tickDeltaMinutes =
                ((emissions[1].toSecondOfDay() / 60 - emissions[0].toSecondOfDay() / 60) + 1440) % 1440
            assertTrue(
                "Expected +300..301 minute shift in minuteTickFlow() after TimeZone.setDefault, got $tickDeltaMinutes",
                tickDeltaMinutes in 300..301
            )
            job.cancel()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
