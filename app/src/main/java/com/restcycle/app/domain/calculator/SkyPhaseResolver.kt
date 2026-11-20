package com.restcycle.app.domain.calculator

import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepOption
import java.time.LocalTime

class SkyPhaseResolver(
    private val calculator: SleepCycleCalculator = SleepCycleCalculator()
) {

    fun resolvePhase(time: LocalTime): SkyPhase {
        val minuteOfDay = time.hour * 60 + time.minute
        return when {
            minuteOfDay in DAY_START_MINUTE until DUSK_START_MINUTE -> SkyPhase.DAY
            minuteOfDay in DAWN_START_MINUTE until DAY_START_MINUTE -> SkyPhase.DAWN_DUSK
            minuteOfDay in DUSK_START_MINUTE until NIGHT_START_MINUTE -> SkyPhase.DAWN_DUSK
            else -> SkyPhase.NIGHT
        }
    }

    fun resolveActiveSkyPhase(
        mode: SleepMode,
        selectedTime: LocalTime,
        options: List<SleepOption>,
        highlightedOptionId: String?
    ): SkyPhase {
        if (mode == SleepMode.WAKE_UP_AT) {
            return resolvePhase(selectedTime)
        }
        val highlighted = highlightedOptionId?.let { id ->
            options.firstOrNull { it.id == id }
        }
        val fallbackId = calculator.defaultHighlightedOptionId(mode, options)
        val activeOption = highlighted
            ?: fallbackId?.let { id -> options.firstOrNull { it.id == id } }
            ?: options.firstOrNull()
        val activeTime = activeOption?.targetTime ?: selectedTime
        return resolvePhase(activeTime)
    }

    companion object {
        const val DAWN_START_MINUTE = 5 * 60 + 30    // 05:30 (330)
        const val DAY_START_MINUTE = 8 * 60          // 08:00 (480)
        const val DUSK_START_MINUTE = 17 * 60 + 30   // 17:30 (1050)
        const val NIGHT_START_MINUTE = 20 * 60 + 30  // 20:30 (1230)
    }
}
