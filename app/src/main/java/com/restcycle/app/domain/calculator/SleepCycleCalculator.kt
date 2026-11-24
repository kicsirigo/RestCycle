package com.restcycle.app.domain.calculator

import androidx.annotation.StringRes
import com.restcycle.app.domain.model.CycleQuality
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepOption
import java.time.LocalTime

data class SleepOptionLabels(
    @get:StringRes @param:StringRes val optimal: Int,
    @get:StringRes @param:StringRes val good: Int,
    @get:StringRes @param:StringRes val short: Int,
    @get:StringRes @param:StringRes val napQuick: Int,
    @get:StringRes @param:StringRes val napClassic: Int,
    @get:StringRes @param:StringRes val napFull: Int
)

class SleepCycleCalculator {

    private data class NapPreset(
        val id: String,
        val durationMinutes: Int,
        val cycles: Int?,
        val labelSelector: (SleepOptionLabels) -> Int
    )

    private val napPresets = listOf(
        NapPreset(NAP_QUICK_ID, 10, null) { it.napQuick },
        NapPreset(NAP_CLASSIC_ID, 20, null) { it.napClassic },
        NapPreset(NAP_FULL_ID, 90, 1) { it.napFull }
    )

    fun clampSleepLatency(minutes: Int): Int =
        minutes.coerceIn(MIN_SLEEP_LATENCY_MINUTES, MAX_SLEEP_LATENCY_MINUTES)

    fun clampNapLatency(minutes: Int): Int =
        minutes.coerceIn(MIN_NAP_LATENCY_MINUTES, MAX_NAP_LATENCY_MINUTES)

    fun qualityForCycles(cycles: Int): CycleQuality = when {
        cycles >= DEFAULT_SUGGESTED_CYCLES -> CycleQuality.OPTIMAL
        cycles == 4 -> CycleQuality.GOOD
        else -> CycleQuality.SHORT
    }

    fun calculateOptions(
        mode: SleepMode,
        anchorTime: LocalTime,
        sleepLatencyMinutes: Int,
        napLatencyMinutes: Int,
        labels: SleepOptionLabels
    ): List<SleepOption> = when (mode) {
        SleepMode.WAKE_UP_AT -> buildCycleOptions(
            prefix = "wake",
            anchorTime = anchorTime,
            latency = clampSleepLatency(sleepLatencyMinutes),
            forward = false,
            includeCountdown = false,
            labels = labels
        )
        SleepMode.SLEEP_NOW -> buildCycleOptions(
            prefix = "sleep_now",
            anchorTime = anchorTime,
            latency = clampSleepLatency(sleepLatencyMinutes),
            forward = true,
            includeCountdown = true,
            labels = labels
        )
        SleepMode.GO_TO_BED_AT -> buildCycleOptions(
            prefix = "bed",
            anchorTime = anchorTime,
            latency = clampSleepLatency(sleepLatencyMinutes),
            forward = true,
            includeCountdown = false,
            labels = labels
        )
        SleepMode.POWER_NAP -> {
            val latency = clampNapLatency(napLatencyMinutes)
            napPresets.map { preset ->
                val totalMinutes = (latency + preset.durationMinutes).toLong()
                SleepOption(
                    id = preset.id,
                    targetTime = anchorTime.plusMinutes(totalMinutes),
                    cycles = preset.cycles,
                    durationMinutes = preset.durationMinutes,
                    quality = CycleQuality.POWER_NAP,
                    labelResId = preset.labelSelector(labels),
                    countdownMinutesFromNow = totalMinutes
                )
            }
        }
    }

    private fun buildCycleOptions(
        prefix: String,
        anchorTime: LocalTime,
        latency: Int,
        forward: Boolean,
        includeCountdown: Boolean,
        labels: SleepOptionLabels
    ): List<SleepOption> = (MAX_CYCLES downTo MIN_CYCLES).map { cycles ->
        val sleepDuration = cycles * CYCLE_MINUTES
        val totalMinutes = sleepDuration.toLong() + latency
        val quality = qualityForCycles(cycles)
        SleepOption(
            id = "${prefix}_$cycles",
            targetTime = if (forward) anchorTime.plusMinutes(totalMinutes) else anchorTime.minusMinutes(totalMinutes),
            cycles = cycles,
            durationMinutes = sleepDuration,
            quality = quality,
            labelResId = labelForQuality(quality, labels),
            countdownMinutesFromNow = if (includeCountdown) totalMinutes else null
        )
    }

    fun defaultHighlightedOptionId(
        mode: SleepMode,
        options: List<SleepOption>
    ): String? {
        if (options.isEmpty()) return null
        val preferredId = when (mode) {
            SleepMode.WAKE_UP_AT -> "wake_$DEFAULT_SUGGESTED_CYCLES"
            SleepMode.SLEEP_NOW -> "sleep_now_$DEFAULT_SUGGESTED_CYCLES"
            SleepMode.GO_TO_BED_AT -> "bed_$DEFAULT_SUGGESTED_CYCLES"
            SleepMode.POWER_NAP -> NAP_CLASSIC_ID
        }
        return options.firstOrNull { it.id == preferredId }?.id
            ?: when (mode) {
                SleepMode.POWER_NAP -> options.firstOrNull { it.durationMinutes == 20 }?.id
                else -> options.firstOrNull { it.cycles == DEFAULT_SUGGESTED_CYCLES }?.id
            }
            ?: options.first().id
    }

    private fun labelForQuality(quality: CycleQuality, labels: SleepOptionLabels): Int = when (quality) {
        CycleQuality.OPTIMAL -> labels.optimal
        CycleQuality.GOOD -> labels.good
        CycleQuality.SHORT -> labels.short
        CycleQuality.POWER_NAP -> labels.napClassic
    }

    companion object {
        const val CYCLE_MINUTES = 90
        const val MIN_CYCLES = 3
        const val MAX_CYCLES = 6
        const val DEFAULT_SUGGESTED_CYCLES = 5

        const val MIN_SLEEP_LATENCY_MINUTES = 0
        const val MAX_SLEEP_LATENCY_MINUTES = 60
        const val DEFAULT_SLEEP_LATENCY_MINUTES = 15

        const val MIN_NAP_LATENCY_MINUTES = 0
        const val MAX_NAP_LATENCY_MINUTES = 30
        const val DEFAULT_NAP_LATENCY_MINUTES = 10

        const val NAP_QUICK_ID = "nap_10"
        const val NAP_CLASSIC_ID = "nap_20"
        const val NAP_FULL_ID = "nap_90"
    }
}
