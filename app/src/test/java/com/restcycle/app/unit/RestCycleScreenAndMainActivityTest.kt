package com.restcycle.app.unit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppLanguage
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RestCycleScreenAndMainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun thumbZoneProportions_andTransparentControls_andNoCalculateButton() {
        val rootBounds = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val headerBounds = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val upperBounds = composeRule.onNodeWithTag(RestCycleTestTags.UPPER_RESULTS_ZONE)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val thumbBounds = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val switcherBounds = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER)
            .getUnclippedBoundsInRoot()
        val wheelBounds = composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL)
            .getUnclippedBoundsInRoot()
        val sliderBounds = composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_ADJUSTER)
            .getUnclippedBoundsInRoot()

        assertEquals(0.dp, rootBounds.top)
        val rootHeight = rootBounds.bottom - rootBounds.top
        val thumbHeight = thumbBounds.bottom - thumbBounds.top
        assertTrue(thumbHeight >= rootHeight * 0.48f)
        assertTrue(thumbBounds.top >= rootBounds.top + rootHeight * 0.42f)
        assertTrue(headerBounds.bottom <= thumbBounds.top + 1.dp)
        assertTrue(upperBounds.bottom <= thumbBounds.top + 1.dp)

        listOf(switcherBounds, wheelBounds, sliderBounds).forEach { controlBounds ->
            assertTrue(controlBounds.top >= rootBounds.top + rootHeight * 0.45f - 1.dp)
        }

        // Both 6-cycle and 5-cycle options visible without scrolling on w412dp-h915dp
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_6")).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_5")).assertIsDisplayed()

        // Zero Calculate/Submit/Results buttons
        listOf("Calculate", "Számítás", "Számol", "Submit", "Confirm", "Results").forEach { forbidden ->
            val nodes = composeRule.onAllNodesWithText(forbidden, substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
            assertTrue("Unexpected forbidden text '$forbidden'", nodes.isEmpty())
        }

        // Verify THUMB_CONTROLS_ZONE leaves root DAWN_DUSK sky gradient visible at y = 0.5f and top glow at y = 0.08f
        lateinit var renderedBitmap: Bitmap
        composeRule.activityRule.scenario.onActivity { activity ->
            val decor = activity.window.decorView
            renderedBitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(renderedBitmap))
        }
        val topGlowPixel = renderedBitmap.getPixel(2, (renderedBitmap.height * 0.08f).toInt())
        val midPixel = renderedBitmap.getPixel(2, (renderedBitmap.height * 0.5f).toInt())
        val topR = android.graphics.Color.red(topGlowPixel)
        val topB = android.graphics.Color.blue(topGlowPixel)
        assertTrue("Expected warm top glow R >= B && R > 15, got R=$topR B=$topB", topR >= topB && topR > 15)

        val midLum = 0.2126 * android.graphics.Color.red(midPixel) / 255.0 +
            0.7152 * android.graphics.Color.green(midPixel) / 255.0 +
            0.0722 * android.graphics.Color.blue(midPixel) / 255.0
        assertTrue("Expected DAWN_DUSK mid luminance in 0.005..0.35, got $midLum", midLum in 0.005..0.35)
    }

    @Test
    fun mainActivity_refreshesTimeFormatOnResumeWhilePreservingState() {
        val resolver = composeRule.activity.contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5"))
            .assertTextContains("06:15", substring = true)

        Settings.System.putString(resolver, Settings.System.TIME_12_24, "12")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5"))
            .assertTextContains("6:15 AM", substring = true)
    }

    @Test
    fun mainActivity_24hTo12hSwitchOnResume_preservesLowHourAndPmHourWithoutCorruption() {
        val resolver = composeRule.activity.contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        // Step WAKE_UP_AT hour down from 06:50 to 05:50 in 24h mode
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY - stepPx / 2f))
            moveTo(Offset(centerX, centerY + stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN)
            .assertTextContains("05:50", substring = true)

        // Switch OS format 24h -> 12h and resume: 05:50 must stay 5:50 AM (not 12:50 AM)
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "12")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        val hasState = { expected: String ->
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)
        }
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasState("5"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM)
            .assertIsDisplayed()
            .assert(hasState("AM"))
        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN)
            .assertTextContains("5:50 AM", substring = true)

        // Also test PM bedtime 22:30 across 24h -> 12h on ON_RESUME
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasState("22"))

        Settings.System.putString(resolver, Settings.System.TIME_12_24, "12")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasState("10"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM)
            .assertIsDisplayed()
            .assert(hasState("PM"))
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5"))
            .assertTextContains("6:15 AM", substring = true)
    }

    @Test
    fun mainActivity_settingsDialog_supports12h24hThemesAndFooterCredit() {
        val resolver = composeRule.activity.contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("made by: kicsirigo").assertIsDisplayed()
        composeRule.onNodeWithText("Nord").assertIsDisplayed()
        composeRule.onNodeWithText("Everforest").assertIsDisplayed()

        // Switch to 12-hour format and Nord -> Everforest theme
        composeRule.onNodeWithText("12-hour").performClick()
        composeRule.onNodeWithText("Nord").performClick()
        composeRule.onNodeWithText("Everforest").performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN)
            .assertTextContains("6:50 AM", substring = true)

        // Switch explicitly to 24-hour, verify, and restore System + Tokyo Night cleanly
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("24-hour").performClick()
        composeRule.onNodeWithText("Tokyo Night").performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN)
            .assertTextContains("06:50", substring = true)

        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("System").performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
    }

    @Test
    fun mainActivity_settingsDialog_switchesLanguageReactivelyAndRestoresAuto() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("🇪🇸 Español").assertIsDisplayed()
        composeRule.onNodeWithText("🇭🇺 Magyar").assertIsDisplayed()

        // Switch to Spanish
        composeRule.onNodeWithTag(RestCycleTestTags.languageOption(AppLanguage.SPANISH.name))
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Despertar a las").assertIsDisplayed()

        // Switch to Brazilian Portuguese
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.languageOption(AppLanguage.BRAZILIAN_PORTUGUESE.name))
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Cochilo").assertIsDisplayed()

        // Restore Auto (System)
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.languageOption(AppLanguage.SYSTEM.name))
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Wake Up At").assertIsDisplayed()
    }

    @Test
    fun mainActivity_quickTimeChipsAndCycleCapsulesUpdateWheelAndCards() {
        val resolver = composeRule.activity.contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.QUICK_TIME_CHIPS_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.cycleDots("wake_6"), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.cycleDots("wake_5"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Tap 07:00 preset chip in WAKE_UP_AT
        composeRule.onNodeWithTag(RestCycleTestTags.quickTimeChip("0700"))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN)
            .assertTextContains("07:00", substring = true)

        // Switch to GO_TO_BED_AT and tap 23:00 preset chip
        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.GO_TO_BED_AT)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.quickTimeChip("2300"))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        val hasState = { expected: String ->
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)
        }
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasState("23"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).assert(hasState("00"))
    }
}
