package com.restcycle.app.unit

import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.calculator.SleepOptionLabels
import com.restcycle.app.domain.model.CycleQuality
import com.restcycle.app.domain.model.SleepMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class SleepCycleCalculatorTest {

    private val calculator = SleepCycleCalculator()

    private val testLabels = SleepOptionLabels(
        optimal = 101,
        good = 102,
        short = 103,
        napQuick = 201,
        napClassic = 202,
        napFull = 203
    )

    @Test
    fun wakeUpAt_default0650_latency15_returns6OrderedBedtimes() {
        val options = calculator.calculateOptions(
            mode = SleepMode.WAKE_UP_AT,
            anchorTime = LocalTime.of(6, 50),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 99,
            labels = testLabels
        )

        assertEquals(
            listOf("wake_6", "wake_5", "wake_4", "wake_3"),
            options.map { it.id }
        )
        assertEquals(
            listOf(
                LocalTime.of(21, 35),
                LocalTime.of(23, 5),
                LocalTime.of(0, 35),
                LocalTime.of(2, 5)
            ),
            options.map { it.targetTime }
        )
        assertEquals(listOf(540, 450, 360, 270), options.map { it.durationMinutes })
        assertEquals(listOf(6, 5, 4, 3), options.map { it.cycles })
        assertEquals(
            listOf(
                CycleQuality.OPTIMAL,
                CycleQuality.OPTIMAL,
                CycleQuality.GOOD,
                CycleQuality.SHORT
            ),
            options.map { it.quality }
        )
        assertEquals(listOf(101, 101, 102, 103), options.map { it.labelResId })
        options.forEach { assertNull(it.countdownMinutesFromNow) }
    }

    @Test
    fun wakeUpAt_midnight0000_wrapsBackwardAcrossMidnight() {
        val options = calculator.calculateOptions(
            mode = SleepMode.WAKE_UP_AT,
            anchorTime = LocalTime.MIDNIGHT,
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(
            listOf(
                LocalTime.of(14, 45),
                LocalTime.of(16, 15),
                LocalTime.of(17, 45),
                LocalTime.of(19, 15)
            ),
            options.map { it.targetTime }
        )
    }

    @Test
    fun wakeUpAt_2359_latency15_exactMinutes() {
        val options = calculator.calculateOptions(
            mode = SleepMode.WAKE_UP_AT,
            anchorTime = LocalTime.of(23, 59),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(
            listOf(
                LocalTime.of(14, 44),
                LocalTime.of(16, 14),
                LocalTime.of(17, 44),
                LocalTime.of(19, 14)
            ),
            options.map { it.targetTime }
        )
    }

    @Test
    fun wakeUpAt_latencyBoundsAndClamping() {
        val zeroLatency = calculator.calculateOptions(
            SleepMode.WAKE_UP_AT,
            LocalTime.of(6, 50),
            sleepLatencyMinutes = 0,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        val negativeLatency = calculator.calculateOptions(
            SleepMode.WAKE_UP_AT,
            LocalTime.of(6, 50),
            sleepLatencyMinutes = -10,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            listOf(
                LocalTime.of(21, 50),
                LocalTime.of(23, 20),
                LocalTime.of(0, 50),
                LocalTime.of(2, 20)
            ),
            zeroLatency.map { it.targetTime }
        )
        assertEquals(zeroLatency, negativeLatency)

        val maxLatency = calculator.calculateOptions(
            SleepMode.WAKE_UP_AT,
            LocalTime.of(6, 50),
            sleepLatencyMinutes = 60,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        val overMaxLatency = calculator.calculateOptions(
            SleepMode.WAKE_UP_AT,
            LocalTime.of(6, 50),
            sleepLatencyMinutes = 120,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            listOf(
                LocalTime.of(20, 50),
                LocalTime.of(22, 20),
                LocalTime.of(23, 50),
                LocalTime.of(1, 20)
            ),
            maxLatency.map { it.targetTime }
        )
        assertEquals(maxLatency, overMaxLatency)
    }

    @Test
    fun sleepNow_2200_latency15_computesTimesAndCountdowns() {
        val options = calculator.calculateOptions(
            mode = SleepMode.SLEEP_NOW,
            anchorTime = LocalTime.of(22, 0),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 30,
            labels = testLabels
        )

        assertEquals(
            listOf("sleep_now_6", "sleep_now_5", "sleep_now_4", "sleep_now_3"),
            options.map { it.id }
        )
        assertEquals(
            listOf(
                LocalTime.of(7, 15),
                LocalTime.of(5, 45),
                LocalTime.of(4, 15),
                LocalTime.of(2, 45)
            ),
            options.map { it.targetTime }
        )
        assertEquals(listOf(555L, 465L, 375L, 285L), options.map { it.countdownMinutesFromNow })
    }

    @Test
    fun sleepNow_midnightWrap2350_latency15() {
        val options = calculator.calculateOptions(
            mode = SleepMode.SLEEP_NOW,
            anchorTime = LocalTime.of(23, 50),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(
            listOf(
                LocalTime.of(9, 5),
                LocalTime.of(7, 35),
                LocalTime.of(6, 5),
                LocalTime.of(4, 35)
            ),
            options.map { it.targetTime }
        )
        assertEquals(listOf(555L, 465L, 375L, 285L), options.map { it.countdownMinutesFromNow })
    }

    @Test
    fun sleepNow_latencyClamping_updatesTargetAndCountdown() {
        val low = calculator.calculateOptions(
            SleepMode.SLEEP_NOW,
            LocalTime.of(22, 0),
            sleepLatencyMinutes = -5,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            listOf(
                LocalTime.of(7, 0),
                LocalTime.of(5, 30),
                LocalTime.of(4, 0),
                LocalTime.of(2, 30)
            ),
            low.map { it.targetTime }
        )
        assertEquals(listOf(540L, 450L, 360L, 270L), low.map { it.countdownMinutesFromNow })

        val high = calculator.calculateOptions(
            SleepMode.SLEEP_NOW,
            LocalTime.of(22, 0),
            sleepLatencyMinutes = 90,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            listOf(
                LocalTime.of(8, 0),
                LocalTime.of(6, 30),
                LocalTime.of(5, 0),
                LocalTime.of(3, 30)
            ),
            high.map { it.targetTime }
        )
        assertEquals(listOf(600L, 510L, 420L, 330L), high.map { it.countdownMinutesFromNow })
    }

    @Test
    fun goToBedAt_2200_latency15_computesSixForwardTimesWithNullCountdown() {
        val options = calculator.calculateOptions(
            mode = SleepMode.GO_TO_BED_AT,
            anchorTime = LocalTime.of(22, 0),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(
            listOf("bed_6", "bed_5", "bed_4", "bed_3"),
            options.map { it.id }
        )
        assertEquals(
            listOf(
                LocalTime.of(7, 15),
                LocalTime.of(5, 45),
                LocalTime.of(4, 15),
                LocalTime.of(2, 45)
            ),
            options.map { it.targetTime }
        )
        options.forEach { assertNull(it.countdownMinutesFromNow) }
    }

    @Test
    fun goToBedAt_midnight0000And2359_wrapCleanly() {
        val atMidnight = calculator.calculateOptions(
            SleepMode.GO_TO_BED_AT,
            LocalTime.MIDNIGHT,
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            listOf(
                LocalTime.of(9, 15),
                LocalTime.of(7, 45),
                LocalTime.of(6, 15),
                LocalTime.of(4, 45)
            ),
            atMidnight.map { it.targetTime }
        )

        val at2359 = calculator.calculateOptions(
            SleepMode.GO_TO_BED_AT,
            LocalTime.of(23, 59),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(
            listOf(
                LocalTime.of(9, 14),
                LocalTime.of(7, 44),
                LocalTime.of(6, 14),
                LocalTime.of(4, 44)
            ),
            at2359.map { it.targetTime }
        )
    }

    @Test
    fun goToBedAt_latencyClamping_0To60() {
        val low = calculator.calculateOptions(
            SleepMode.GO_TO_BED_AT,
            LocalTime.of(22, 30),
            sleepLatencyMinutes = -1,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(LocalTime.of(6, 0), low.first { it.id == "bed_5" }.targetTime)

        val high = calculator.calculateOptions(
            SleepMode.GO_TO_BED_AT,
            LocalTime.of(22, 30),
            sleepLatencyMinutes = 75,
            napLatencyMinutes = 10,
            labels = testLabels
        )
        assertEquals(LocalTime.of(7, 0), high.first { it.id == "bed_5" }.targetTime)
    }

    @Test
    fun powerNap_1330_defaultNapLatency10_returnsThreePresetsInOrder() {
        val options = calculator.calculateOptions(
            mode = SleepMode.POWER_NAP,
            anchorTime = LocalTime.of(13, 30),
            sleepLatencyMinutes = 45,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(listOf("nap_10", "nap_20", "nap_90"), options.map { it.id })
        assertEquals(
            listOf(
                LocalTime.of(13, 50),
                LocalTime.of(14, 0),
                LocalTime.of(15, 10)
            ),
            options.map { it.targetTime }
        )
        assertEquals(listOf(null, null, 1), options.map { it.cycles })
        assertEquals(listOf(10, 20, 90), options.map { it.durationMinutes })
        assertEquals(
            listOf(CycleQuality.POWER_NAP, CycleQuality.POWER_NAP, CycleQuality.POWER_NAP),
            options.map { it.quality }
        )
        assertEquals(listOf(201, 202, 203), options.map { it.labelResId })
        assertEquals(listOf(20L, 30L, 100L), options.map { it.countdownMinutesFromNow })
    }

    @Test
    fun powerNap_midnightWrap2330_napLatency10() {
        val options = calculator.calculateOptions(
            mode = SleepMode.POWER_NAP,
            anchorTime = LocalTime.of(23, 30),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 10,
            labels = testLabels
        )

        assertEquals(
            listOf(
                LocalTime.of(23, 50),
                LocalTime.MIDNIGHT,
                LocalTime.of(1, 10)
            ),
            options.map { it.targetTime }
        )
        assertEquals(listOf(20L, 30L, 100L), options.map { it.countdownMinutesFromNow })
    }

    @Test
    fun powerNap_napLatencyClamping_0To30Never60() {
        val negative = calculator.calculateOptions(
            SleepMode.POWER_NAP,
            LocalTime.of(13, 0),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = -5,
            labels = testLabels
        )
        val zero = calculator.calculateOptions(
            SleepMode.POWER_NAP,
            LocalTime.of(13, 0),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 0,
            labels = testLabels
        )
        assertEquals(zero, negative)
        assertEquals(listOf(10L, 20L, 90L), zero.map { it.countdownMinutesFromNow })

        val thirty = calculator.calculateOptions(
            SleepMode.POWER_NAP,
            LocalTime.of(13, 0),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 30,
            labels = testLabels
        )
        val sixty = calculator.calculateOptions(
            SleepMode.POWER_NAP,
            LocalTime.of(13, 0),
            sleepLatencyMinutes = 15,
            napLatencyMinutes = 60,
            labels = testLabels
        )
        assertEquals(thirty, sixty)
        assertEquals(listOf(40L, 50L, 120L), sixty.map { it.countdownMinutesFromNow })
        assertEquals(LocalTime.of(15, 0), sixty.last().targetTime)
    }

    @Test
    fun qualityForCycles_allThresholds() {
        assertEquals(CycleQuality.OPTIMAL, calculator.qualityForCycles(6))
        assertEquals(CycleQuality.OPTIMAL, calculator.qualityForCycles(5))
        assertEquals(CycleQuality.GOOD, calculator.qualityForCycles(4))
        assertEquals(CycleQuality.SHORT, calculator.qualityForCycles(3))
        assertEquals(CycleQuality.SHORT, calculator.qualityForCycles(2))
        assertEquals(CycleQuality.SHORT, calculator.qualityForCycles(1))
    }

    @Test
    fun defaultHighlightedOptionId_allModes() {
        val wakeOptions = calculator.calculateOptions(SleepMode.WAKE_UP_AT, LocalTime.of(6, 50), 15, 10, testLabels)
        val nowOptions = calculator.calculateOptions(SleepMode.SLEEP_NOW, LocalTime.of(22, 0), 15, 10, testLabels)
        val bedOptions = calculator.calculateOptions(SleepMode.GO_TO_BED_AT, LocalTime.of(22, 30), 15, 10, testLabels)
        val napOptions = calculator.calculateOptions(SleepMode.POWER_NAP, LocalTime.of(13, 30), 15, 10, testLabels)

        assertEquals("wake_5", calculator.defaultHighlightedOptionId(SleepMode.WAKE_UP_AT, wakeOptions))
        assertEquals("sleep_now_5", calculator.defaultHighlightedOptionId(SleepMode.SLEEP_NOW, nowOptions))
        assertEquals("bed_5", calculator.defaultHighlightedOptionId(SleepMode.GO_TO_BED_AT, bedOptions))
        assertEquals("nap_20", calculator.defaultHighlightedOptionId(SleepMode.POWER_NAP, napOptions))
    }

    @Test
    fun defaultHighlightedOptionId_emptyAndPartialLists() {
        assertNull(calculator.defaultHighlightedOptionId(SleepMode.WAKE_UP_AT, emptyList()))

        val allBed = calculator.calculateOptions(SleepMode.GO_TO_BED_AT, LocalTime.of(22, 30), 15, 10, testLabels)
        val withoutBed5 = allBed.filterNot { it.id == "bed_5" }
        assertEquals("bed_6", calculator.defaultHighlightedOptionId(SleepMode.GO_TO_BED_AT, withoutBed5))
    }

    @Test
    fun sleepLatencyIsolation_sleepModesIgnoreNapLatency() {
        listOf(SleepMode.WAKE_UP_AT, SleepMode.SLEEP_NOW, SleepMode.GO_TO_BED_AT).forEach { mode ->
            val with0 = calculator.calculateOptions(mode, LocalTime.of(22, 0), 15, 0, testLabels)
            val with15 = calculator.calculateOptions(mode, LocalTime.of(22, 0), 15, 15, testLabels)
            val with30 = calculator.calculateOptions(mode, LocalTime.of(22, 0), 15, 30, testLabels)
            assertEquals(with0, with15)
            assertEquals(with15, with30)
        }
    }
}
