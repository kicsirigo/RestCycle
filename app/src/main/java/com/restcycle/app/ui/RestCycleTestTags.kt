package com.restcycle.app.ui

import com.restcycle.app.domain.model.SleepMode

object RestCycleTestTags {
    const val SCREEN_ROOT = "SCREEN_ROOT"
    const val CELESTIAL_HEADER = "CELESTIAL_HEADER"
    const val UPPER_RESULTS_ZONE = "UPPER_RESULTS_ZONE"
    const val THUMB_CONTROLS_ZONE = "THUMB_CONTROLS_ZONE"

    const val MODE_SWITCHER = "MODE_SWITCHER"
    const val MODE_TAB_WAKE_UP_AT = "MODE_TAB_WAKE_UP_AT"
    const val MODE_TAB_SLEEP_NOW = "MODE_TAB_SLEEP_NOW"
    const val MODE_TAB_GO_TO_BED_AT = "MODE_TAB_GO_TO_BED_AT"
    const val MODE_TAB_POWER_NAP = "MODE_TAB_POWER_NAP"

    const val TIME_WHEEL = "TIME_WHEEL"
    const val WHEEL_HOUR = "WHEEL_HOUR"
    const val WHEEL_MINUTE = "WHEEL_MINUTE"
    const val WHEEL_AM_PM = "WHEEL_AM_PM"

    const val LATENCY_ADJUSTER = "LATENCY_ADJUSTER"
    const val LATENCY_SLIDER = "LATENCY_SLIDER"
    const val LATENCY_VALUE_TEXT = "LATENCY_VALUE_TEXT"

    const val WAKE_ALARM_BTN = "WAKE_ALARM_BTN"
    const val INERTIA_TOOLTIP = "INERTIA_TOOLTIP"
    const val SETTINGS_BUTTON = "SETTINGS_BUTTON"
    const val SETTINGS_DIALOG = "SETTINGS_DIALOG"
    const val UPDATE_ACTION_BTN = "UPDATE_ACTION_BTN"
    const val BREATHE_BUTTON = "BREATHE_BUTTON"
    const val BREATHING_DIALOG = "BREATHING_DIALOG"
    const val AMBIENT_SOUND_BUTTON = "AMBIENT_SOUND_BUTTON"
    const val SLEEP_TRACKER_BUTTON = "SLEEP_TRACKER_BUTTON"
    const val SLEEP_HISTORY_BUTTON = "SLEEP_HISTORY_BUTTON"
    const val SLEEP_TRACKER_DIALOG = "SLEEP_TRACKER_DIALOG"
    const val START_SLEEP_DIALOG_BTN = "START_SLEEP_DIALOG_BTN"
    const val SLEEP_MOOD_PREFIX = "SLEEP_MOOD_"
    const val CAFFEINE_CUTOFF_PILL = "CAFFEINE_CUTOFF_PILL"
    const val AMOLED_NIGHT_TOGGLE = "AMOLED_NIGHT_TOGGLE"
    const val BEDTIME_REMINDER_TOGGLE = "BEDTIME_REMINDER_TOGGLE"
    const val CAFFEINE_OPTION_PREFIX = "CAFFEINE_OPTION_"

    fun caffeineOption(hours: Int): String = "$CAFFEINE_OPTION_PREFIX$hours"
    fun sleepMoodOption(moodName: String): String = "$SLEEP_MOOD_PREFIX$moodName"

    const val OPTION_CARD_PREFIX = "OPTION_CARD_"
    const val SET_ALARM_BTN_PREFIX = "SET_ALARM_BTN_"
    const val SET_TIMER_BTN_PREFIX = "SET_TIMER_BTN_"
    const val LANGUAGE_OPTION_PREFIX = "LANGUAGE_OPTION_"
    const val QUICK_TIME_CHIPS_ROW = "quick_time_chips_row"
    const val QUICK_TIME_CHIP_PREFIX = "quick_time_chip_"
    const val CYCLE_DOTS_PREFIX = "cycle_dots_"

    fun optionCard(id: String): String = "$OPTION_CARD_PREFIX$id"
    fun setAlarmButton(id: String): String = "$SET_ALARM_BTN_PREFIX$id"
    fun setTimerButton(id: String): String = "$SET_TIMER_BTN_PREFIX$id"
    fun languageOption(languageName: String): String = "$LANGUAGE_OPTION_PREFIX$languageName"
    fun quickTimeChip(timeKey: String): String = "$QUICK_TIME_CHIP_PREFIX$timeKey"
    fun cycleDots(optionId: String): String = "$CYCLE_DOTS_PREFIX$optionId"

    fun modeTab(mode: SleepMode): String = when (mode) {
        SleepMode.WAKE_UP_AT -> MODE_TAB_WAKE_UP_AT
        SleepMode.SLEEP_NOW -> MODE_TAB_SLEEP_NOW
        SleepMode.GO_TO_BED_AT -> MODE_TAB_GO_TO_BED_AT
        SleepMode.POWER_NAP -> MODE_TAB_POWER_NAP
    }
}
