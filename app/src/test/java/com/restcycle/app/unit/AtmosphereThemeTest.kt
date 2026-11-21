package com.restcycle.app.unit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.RestCycleTypography
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.theme.TokyoNightDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.TokyoNightDayAtmospherePalette
import com.restcycle.app.ui.theme.TokyoNightNightAtmospherePalette
import com.restcycle.app.ui.theme.paletteForTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AtmosphereThemeUnitTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun palettesMatchExactSpecificationTokens() {
        assertEquals(Color(0xFFECEEF5), TokyoNightDayAtmospherePalette.backgroundGlow)
        assertEquals(Color(0xFFD8DCEB), TokyoNightDayAtmospherePalette.backgroundBottom)
        assertEquals(Color(0xFFF0F2F8), TokyoNightDayAtmospherePalette.cardSurface)
        assertEquals(Color(0xFF2E7DE9), TokyoNightDayAtmospherePalette.accent)
        assertEquals(Color(0xFF24283B), TokyoNightDayAtmospherePalette.textPrimary)
        assertTrue(TokyoNightDayAtmospherePalette.isLightSystemBars)

        assertEquals(Color(0xFFFF9E64), TokyoNightDawnDuskAtmospherePalette.backgroundGlow)
        assertEquals(Color(0xFF3E2A3D), TokyoNightDawnDuskAtmospherePalette.backgroundTop)
        assertEquals(Color(0xFF1A1B26), TokyoNightDawnDuskAtmospherePalette.backgroundBottom)
        assertEquals(Color(0xFF24283B), TokyoNightDawnDuskAtmospherePalette.cardSurface)
        assertEquals(Color(0xFFFF9E64), TokyoNightDawnDuskAtmospherePalette.accent)
        assertEquals(Color(0xFFE5E9F8), TokyoNightDawnDuskAtmospherePalette.textPrimary)
        assertFalse(TokyoNightDawnDuskAtmospherePalette.isLightSystemBars)

        assertEquals(Color(0xFF020204), TokyoNightNightAtmospherePalette.backgroundTop)
        assertEquals(Color(0xFF010102), TokyoNightNightAtmospherePalette.backgroundBottom)
        assertEquals(Color(0xFF1F2335), TokyoNightNightAtmospherePalette.cardSurface)
        assertEquals(Color(0xFF7AA2F7), TokyoNightNightAtmospherePalette.accent)
        assertEquals(Color(0xFFC0CAF5), TokyoNightNightAtmospherePalette.textPrimary)
        assertFalse(TokyoNightNightAtmospherePalette.isLightSystemBars)
    }

    @Test
    fun tabularTextStylesAndTypographyEnforceTnum() {
        val numericStyles = listOf(
            TabularTextStyles.wheelDigitSelected,
            TabularTextStyles.wheelDigitUnselected,
            TabularTextStyles.cardTargetTime,
            TabularTextStyles.cardDuration,
            TabularTextStyles.cardCountdown,
            TabularTextStyles.sliderValue,
            TabularTextStyles.headerLiveClock,
            RestCycleTypography.displayLarge,
            RestCycleTypography.titleLarge,
            RestCycleTypography.bodyMedium,
            RestCycleTypography.labelLarge
        )
        numericStyles.forEach { style ->
            assertEquals("tnum", style.fontFeatureSettings)
        }
    }

    @Test
    fun atmosphereTheme_morphsColorsSmoothlyOver600ms() {
        var phase by mutableStateOf(SkyPhase.NIGHT)
        var sampledBg by mutableStateOf(Color.Black)

        composeRule.setContent {
            AtmosphereTheme(skyPhase = phase) {
                val bg = AtmosphereTheme.palette.backgroundBottom
                sampledBg = bg
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(TokyoNightNightAtmospherePalette.backgroundBottom.toArgb(), sampledBg.toArgb())

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            phase = SkyPhase.DAY
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()

        assertTrue(
            "Expected mid-morph red in 0.15..0.85, was ${sampledBg.red}",
            sampledBg.red in 0.15f..0.85f
        )

        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()
        assertEquals(TokyoNightDayAtmospherePalette.backgroundBottom.toArgb(), sampledBg.toArgb())
    }

    @Test
    fun accentTextAndDawnDuskHeaderSubtitle_meetWcagAAContrast() {
        fun contrastRatio(fg: Color, bg: Color): Float {
            val l1 = fg.luminance()
            val l2 = bg.luminance()
            val lighter = maxOf(l1, l2)
            val darker = minOf(l1, l2)
            return (lighter + 0.05f) / (darker + 0.05f)
        }

        // Default Tokyo Night DAY accentText achieves >= 4.5:1 across all DAY surfaces
        assertEquals(Color(0xFF2E7DE9), TokyoNightDayAtmospherePalette.accent)
        listOf(
            TokyoNightDayAtmospherePalette.cardSurface,
            TokyoNightDayAtmospherePalette.cardSurfaceHighlighted,
            TokyoNightDayAtmospherePalette.backgroundBottom,
            TokyoNightDayAtmospherePalette.pillContainer
        ).forEach { surface ->
            val cr = contrastRatio(TokyoNightDayAtmospherePalette.accentText, surface)
            assertTrue("Expected DAY accentText CR >= 4.5 on $surface, got $cr", cr >= 4.5f)
        }

        // DAWN_DUSK & NIGHT accentText >= 4.5:1 on card/pill surfaces
        listOf(
            TokyoNightDawnDuskAtmospherePalette.cardSurface,
            TokyoNightDawnDuskAtmospherePalette.cardSurfaceHighlighted,
            TokyoNightDawnDuskAtmospherePalette.pillContainer
        ).forEach { surface ->
            val cr = contrastRatio(TokyoNightDawnDuskAtmospherePalette.accentText, surface)
            assertTrue("Expected DAWN_DUSK accentText CR >= 4.5 on $surface, got $cr", cr >= 4.5f)
        }
        listOf(
            TokyoNightNightAtmospherePalette.cardSurface,
            TokyoNightNightAtmospherePalette.cardSurfaceHighlighted,
            TokyoNightNightAtmospherePalette.pillContainer
        ).forEach { surface ->
            val cr = contrastRatio(TokyoNightNightAtmospherePalette.accentText, surface)
            assertTrue("Expected NIGHT accentText CR >= 4.5 on $surface, got $cr", cr >= 4.5f)
        }

        // DAWN_DUSK CelestialHeader subtitle over translucent dark scrim (0.68f) atop top #FF9E64 glow
        val headerScrimBg = TokyoNightDawnDuskAtmospherePalette.backgroundBottom.copy(alpha = 0.68f)
            .compositeOver(TokyoNightDawnDuskAtmospherePalette.backgroundGlow)
        val subtitleCr = contrastRatio(TokyoNightDawnDuskAtmospherePalette.textSecondary, headerScrimBg)
        assertTrue("Expected DAWN_DUSK header subtitle CR >= 4.5, got $subtitleCr", subtitleCr >= 4.5f)
        val titleCr = contrastRatio(TokyoNightDawnDuskAtmospherePalette.textPrimary, headerScrimBg)
        assertTrue("Expected DAWN_DUSK header title CR >= 4.5, got $titleCr", titleCr >= 4.5f)
    }

    @Test
    fun allThemeStyles_adaptAcrossAllThreeSkyPhases() {
        AppThemeStyle.entries.forEach { style ->
            val day = paletteForTheme(SkyPhase.DAY, style)
            val dawn = paletteForTheme(SkyPhase.DAWN_DUSK, style)
            val night = paletteForTheme(SkyPhase.NIGHT, style)

            assertTrue("$style DAY should use light system bars", day.isLightSystemBars)
            assertFalse("$style DAWN_DUSK should use dark system bars", dawn.isLightSystemBars)
            assertFalse("$style NIGHT should use dark system bars", night.isLightSystemBars)

            assertNotEquals(day.backgroundBottom, dawn.backgroundBottom)
            assertNotEquals(dawn.backgroundBottom, night.backgroundBottom)
            assertNotEquals(day.accent, dawn.accent)
            assertNotEquals(dawn.accent, night.accent)
        }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class AtmosphereSystemBarsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun systemBars_updateLightAndDarkFlagsAcrossSkyPhasesAndLifecycle() {
        // Default WAKE_UP_AT 06:50 is DAWN_DUSK -> light bars = false
        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
        }

        // Highlighting 6-cycle in GO_TO_BED_AT (22:30 + 9h15m = 07:45 DAWN_DUSK; bed_6 vs bed_5)
        // Switching to GO_TO_BED_AT and tapping bed_6 (07:45) vs tapping wheel 08:00 -> DAY
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).performClick()
        composeRule.waitForIdle()
        // bed_1 is 00:15 (NIGHT) -> light bars = false
        composeRule.onNodeWithTag(RestCycleTestTags.UPPER_RESULTS_ZONE)
            .performScrollToNode(hasTestTag(RestCycleTestTags.optionCard("bed_1")))
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_1")).performClick()
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
        }

        // Switch back to WAKE_UP_AT and step twice via 44dp upward drag (06 -> 07 -> 08) -> DAY -> light bars = true
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).performClick()
        composeRule.waitForIdle()
        repeat(2) {
            stepWheelForward(composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR))
            composeRule.waitForIdle()
        }
        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
        }

        // Survives STARTED -> RESUMED lifecycle transition
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
        }
    }
}

private fun stepWheelForward(node: androidx.compose.ui.test.SemanticsNodeInteraction) {
    node.performTouchInput {
        val stepPx = 44.dp.toPx()
        down(androidx.compose.ui.geometry.Offset(centerX, centerY + stepPx / 2f))
        moveTo(androidx.compose.ui.geometry.Offset(centerX, centerY - stepPx / 2f), delayMillis = 80L)
        up()
    }
}
