package com.restcycle.app.unit

import com.restcycle.app.domain.model.CycleQuality
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepOption
import com.restcycle.app.domain.model.SleepUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SleepContractsTest {

    @Test
    fun sleepMode_hasExactFourEntries() {
        assertEquals(
            listOf(
                SleepMode.WAKE_UP_AT,
                SleepMode.SLEEP_NOW,
                SleepMode.GO_TO_BED_AT,
                SleepMode.POWER_NAP
            ),
            SleepMode.entries
        )
    }

    @Test
    fun skyPhase_hasExactThreeEntries() {
        assertEquals(
            listOf(
                SkyPhase.DAY,
                SkyPhase.DAWN_DUSK,
                SkyPhase.NIGHT
            ),
            SkyPhase.entries
        )
    }

    @Test
    fun cycleQuality_hasExactFourEntries() {
        assertEquals(
            listOf(
                CycleQuality.OPTIMAL,
                CycleQuality.GOOD,
                CycleQuality.SHORT,
                CycleQuality.POWER_NAP
            ),
            CycleQuality.entries
        )
    }

    @Test
    fun sleepUiState_defaultsMatchSpecification() {
        val state = SleepUiState()
        assertEquals(SleepMode.WAKE_UP_AT, state.mode)
        assertEquals(LocalTime.of(6, 50), state.selectedTime)
        assertEquals(15, state.sleepLatencyMinutes)
        assertEquals(10, state.napLatencyMinutes)
        assertEquals(emptyList<SleepOption>(), state.options)
        assertNull(state.highlightedOptionId)
        assertEquals(SkyPhase.DAWN_DUSK, state.activeSkyPhase)
        assertTrue(state.is24HourFormat)
    }

    @Test
    fun sleepOption_defaultsAndPropertiesMatchSpecification() {
        val shortNap = SleepOption(
            id = "nap_10",
            targetTime = LocalTime.of(14, 20),
            cycles = null,
            durationMinutes = 10,
            quality = CycleQuality.POWER_NAP,
            labelResId = 101
        )
        assertEquals("nap_10", shortNap.id)
        assertEquals(LocalTime.of(14, 20), shortNap.targetTime)
        assertNull(shortNap.cycles)
        assertEquals(10, shortNap.durationMinutes)
        assertEquals(CycleQuality.POWER_NAP, shortNap.quality)
        assertEquals(101, shortNap.labelResId)
        assertNull(shortNap.countdownMinutesFromNow)

        val timedSleep = shortNap.copy(
            id = "sleep_now_5",
            cycles = 5,
            durationMinutes = 450,
            quality = CycleQuality.OPTIMAL,
            countdownMinutesFromNow = 465L
        )
        assertEquals(5, timedSleep.cycles)
        assertEquals(465L, timedSleep.countdownMinutesFromNow)
    }
}
