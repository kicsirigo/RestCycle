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

    const val OPTION_CARD_PREFIX = "OPTION_CARD_"
    const val SET_ALARM_BTN_PREFIX = "SET_ALARM_BTN_"
    const val SET_TIMER_BTN_PREFIX = "SET_TIMER_BTN_"

    fun optionCard(id: String): String = "$OPTION_CARD_PREFIX$id"
    fun setAlarmButton(id: String): String = "$SET_ALARM_BTN_PREFIX$id"
    fun setTimerButton(id: String): String = "$SET_TIMER_BTN_PREFIX$id"

    fun modeTab(mode: SleepMode): String = when (mode) {
        SleepMode.WAKE_UP_AT -> MODE_TAB_WAKE_UP_AT
        SleepMode.SLEEP_NOW -> MODE_TAB_SLEEP_NOW
        SleepMode.GO_TO_BED_AT -> MODE_TAB_GO_TO_BED_AT
        SleepMode.POWER_NAP -> MODE_TAB_POWER_NAP
    }
}
