package com.restcycle.app.ui.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMAT_24H: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val FORMAT_12H: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

fun formatClockTime(time: LocalTime, is24HourFormat: Boolean): String =
    if (is24HourFormat) {
        time.format(FORMAT_24H)
    } else {
        time.format(FORMAT_12H)
    }
