package com.restcycle.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val TABULAR_NUMERALS = "tnum"

object TabularTextStyles {
    val wheelDigitSelected = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )

    val wheelDigitUnselected = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 23.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )

    val cardTargetTime = TextStyle(
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 27.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )

    val cardDuration = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )

    val cardCountdown = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )

    val sliderValue = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )

    val headerLiveClock = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )
}

val RestCycleTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 42.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    displayMedium = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    displaySmall = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    titleSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        fontFeatureSettings = TABULAR_NUMERALS
    )
)
