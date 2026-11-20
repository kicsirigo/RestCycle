package com.restcycle.app.unit

import com.restcycle.app.domain.calculator.SkyPhaseResolver
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.calculator.SleepOptionLabels
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class SkyPhaseResolverTest {

    private val calculator = SleepCycleCalculator()
    private val resolver = SkyPhaseResolver(calculator)

    private val testLabels = SleepOptionLabels(
        optimal = 101,
        good = 102,
        short = 103,
        napQuick = 201,
        napClassic = 202,
        napFull = 203
    )

    @Test
    fun resolvePhase_allEightExactBoundaries() {
        assertEquals(SkyPhase.NIGHT, resolver.resolvePhase(LocalTime.of(0, 0)))
        assertEquals(SkyPhase.NIGHT, resolver.resolvePhase(LocalTime.of(5, 29)))
        assertEquals(SkyPhase.DAWN_DUSK, resolver.resolvePhase(LocalTime.of(5, 30)))
        assertEquals(SkyPhase.DAWN_DUSK, resolver.resolvePhase(LocalTime.of(7, 59)))
        assertEquals(SkyPhase.DAY, resolver.resolvePhase(LocalTime.of(8, 0)))
        assertEquals(SkyPhase.DAY, resolver.resolvePhase(LocalTime.of(17, 29)))
        assertEquals(SkyPhase.DAWN_DUSK, resolver.resolvePhase(LocalTime.of(17, 30)))
        assertEquals(SkyPhase.DAWN_DUSK, resolver.resolvePhase(LocalTime.of(20, 29)))
        assertEquals(SkyPhase.NIGHT, resolver.resolvePhase(LocalTime.of(20, 30)))
        assertEquals(SkyPhase.NIGHT, resolver.resolvePhase(LocalTime.of(23, 59)))
    }

    @Test
    fun resolvePhase_exhaustiveAll1440MinutesOfDay() {
        for (minuteOfDay in 0 until 1440) {
            val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
            val expected = when {
                minuteOfDay in 480..1049 -> SkyPhase.DAY
                minuteOfDay in 330..479 -> SkyPhase.DAWN_DUSK
                minuteOfDay in 1050..1229 -> SkyPhase.DAWN_DUSK
                else -> SkyPhase.NIGHT
            }
            assertEquals("Mismatch at $time", expected, resolver.resolvePhase(time))
        }
    }

    @Test
    fun resolvePhase_ignoresSubMinuteSecondsAndNanos() {
        assertEquals(SkyPhase.NIGHT, resolver.resolvePhase(LocalTime.of(5, 29, 59)))
        assertEquals(SkyPhase.DAWN_DUSK, resolver.resolvePhase(LocalTime.of(7, 59, 59)))
        assertEquals(SkyPhase.DAY, resolver.resolvePhase(LocalTime.of(17, 29, 59)))
        assertEquals(SkyPhase.DAWN_DUSK, resolver.resolvePhase(LocalTime.of(20, 29, 59)))
        assertEquals(SkyPhase.NIGHT, resolver.resolvePhase(LocalTime.of(23, 59, 59, 999_999_999)))
    }

    @Test
    fun resolveActiveSkyPhase_wakeUpAt_alwaysUsesSelectedTime() {
        val selected = LocalTime.of(8, 0)
        val options = calculator.calculateOptions(SleepMode.WAKE_UP_AT, selected, 15, 10, testLabels)
        // wake_5 targetTime is 00:15 (NIGHT), but WAKE_UP_AT stays DAY from selectedTime 08:00
        assertEquals(
            SkyPhase.DAY,
            resolver.resolveActiveSkyPhase(SleepMode.WAKE_UP_AT, selected, options, "wake_5")
        )
        assertEquals(
            SkyPhase.DAY,
            resolver.resolveActiveSkyPhase(SleepMode.WAKE_UP_AT, selected, options, "wake_6")
        )
    }

    @Test
    fun resolveActiveSkyPhase_wakeUpAt_nullHighlightOrEmptyOptions_usesSelectedTime() {
        assertEquals(
            SkyPhase.NIGHT,
            resolver.resolveActiveSkyPhase(
                mode = SleepMode.WAKE_UP_AT,
                selectedTime = LocalTime.of(23, 0),
                options = emptyList(),
                highlightedOptionId = null
            )
        )
    }

    @Test
    fun resolveActiveSkyPhase_sleepNow_usesHighlightedOptionTargetTime() {
        val options = calculator.calculateOptions(
            mode = SleepMode.SLEEP_NOW,
            anchorTime = LocalTime.of(23, 30),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            SkyPhase.DAWN_DUSK, // 07:15
            resolver.resolveActiveSkyPhase(SleepMode.SLEEP_NOW, LocalTime.of(6, 50), options, "sleep_now_5")
        )
        assertEquals(
            SkyPhase.DAY, // 08:45
            resolver.resolveActiveSkyPhase(SleepMode.SLEEP_NOW, LocalTime.of(6, 50), options, "sleep_now_6")
        )
        assertEquals(
            SkyPhase.NIGHT, // 04:15
            resolver.resolveActiveSkyPhase(SleepMode.SLEEP_NOW, LocalTime.of(6, 50), options, "sleep_now_3")
        )
    }

    @Test
    fun resolveActiveSkyPhase_goToBedAt_usesHighlightedCardNotBedtimeWheel() {
        val bedtime = LocalTime.of(23, 0) // NIGHT
        val options = calculator.calculateOptions(SleepMode.GO_TO_BED_AT, bedtime, 15, 10, testLabels)

        assertEquals(
            SkyPhase.DAWN_DUSK, // bed_5 = 06:45
            resolver.resolveActiveSkyPhase(SleepMode.GO_TO_BED_AT, bedtime, options, "bed_5")
        )
        assertEquals(
            SkyPhase.DAY, // bed_6 = 08:15
            resolver.resolveActiveSkyPhase(SleepMode.GO_TO_BED_AT, bedtime, options, "bed_6")
        )
        assertEquals(
            SkyPhase.NIGHT, // bed_1 = 00:45
            resolver.resolveActiveSkyPhase(SleepMode.GO_TO_BED_AT, bedtime, options, "bed_1")
        )
    }

    @Test
    fun resolveActiveSkyPhase_powerNap_usesHighlightedPreset() {
        val options = calculator.calculateOptions(
            mode = SleepMode.POWER_NAP,
            anchorTime = LocalTime.of(16, 40),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(
            SkyPhase.DAY, // nap_20 = 17:10
            resolver.resolveActiveSkyPhase(SleepMode.POWER_NAP, LocalTime.of(6, 50), options, "nap_20")
        )
        assertEquals(
            SkyPhase.DAWN_DUSK, // nap_90 = 18:20
            resolver.resolveActiveSkyPhase(SleepMode.POWER_NAP, LocalTime.of(6, 50), options, "nap_90")
        )
    }

    @Test
    fun resolveActiveSkyPhase_nullHighlightedId_fallsBackToDefaultSuggested() {
        val nowOptions = calculator.calculateOptions(SleepMode.SLEEP_NOW, LocalTime.of(23, 30), 15, 10, testLabels)
        // sleep_now_6 = 08:45 (DAY), sleep_now_5 = 07:15 (DAWN_DUSK) -> must choose sleep_now_5, NOT options.first()
        assertEquals(
            SkyPhase.DAWN_DUSK,
            resolver.resolveActiveSkyPhase(SleepMode.SLEEP_NOW, LocalTime.of(23, 30), nowOptions, null)
        )

        val bedOptions = calculator.calculateOptions(SleepMode.GO_TO_BED_AT, LocalTime.of(23, 0), 15, 10, testLabels)
        // bed_6 = 08:15 (DAY), bed_5 = 06:45 (DAWN_DUSK)
        assertEquals(
            SkyPhase.DAWN_DUSK,
            resolver.resolveActiveSkyPhase(SleepMode.GO_TO_BED_AT, LocalTime.of(23, 0), bedOptions, null)
        )

        val napOptions = calculator.calculateOptions(SleepMode.POWER_NAP, LocalTime.of(17, 5), 15, 10, testLabels)
        // nap_10 = 17:25 (DAY), nap_20 = 17:35 (DAWN_DUSK)
        assertEquals(
            SkyPhase.DAWN_DUSK,
            resolver.resolveActiveSkyPhase(SleepMode.POWER_NAP, LocalTime.of(12, 0), napOptions, null)
        )
    }

    @Test
    fun resolveActiveSkyPhase_unknownHighlightedId_fallsBackToDefaultSuggested() {
        val nowOptions = calculator.calculateOptions(SleepMode.SLEEP_NOW, LocalTime.of(23, 30), 15, 10, testLabels)
        assertEquals(
            SkyPhase.DAWN_DUSK,
            resolver.resolveActiveSkyPhase(SleepMode.SLEEP_NOW, LocalTime.of(23, 30), nowOptions, "nonexistent_id")
        )
    }

    @Test
    fun resolveActiveSkyPhase_emptyOptions_fallsBackToSelectedTime() {
        assertEquals(
            SkyPhase.DAY,
            resolver.resolveActiveSkyPhase(SleepMode.SLEEP_NOW, LocalTime.of(12, 0), emptyList(), null)
        )
        assertEquals(
            SkyPhase.DAY,
            resolver.resolveActiveSkyPhase(SleepMode.GO_TO_BED_AT, LocalTime.of(12, 0), emptyList(), "bed_5")
        )
        assertEquals(
            SkyPhase.DAY,
            resolver.resolveActiveSkyPhase(SleepMode.POWER_NAP, LocalTime.of(12, 0), emptyList(), "nap_20")
        )
    }
}
