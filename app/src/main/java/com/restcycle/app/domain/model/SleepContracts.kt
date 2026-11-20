package com.restcycle.app.domain.model

enum class SleepMode { WAKE_UP_AT, SLEEP_NOW, GO_TO_BED_AT, POWER_NAP }

enum class SkyPhase { DAY, DAWN_DUSK, NIGHT }

enum class CycleQuality { OPTIMAL, GOOD, SHORT, POWER_NAP }

data class SleepOption(
    val id: String,
    val targetTime: java.time.LocalTime,
    val cycles: Int?, // null for short power naps
    val durationMinutes: Int,
    val quality: CycleQuality,
    val labelResId: Int,
    val countdownMinutesFromNow: Long? = null
)

data class SleepUiState(
    val mode: SleepMode = SleepMode.WAKE_UP_AT,
    val selectedTime: java.time.LocalTime = java.time.LocalTime.of(6, 50),
    val sleepLatencyMinutes: Int = 15,
    val napLatencyMinutes: Int = 10,
    val options: List<SleepOption> = emptyList(),
    val highlightedOptionId: String? = null,
    val activeSkyPhase: SkyPhase = SkyPhase.DAWN_DUSK,
    val is24HourFormat: Boolean = true
)
