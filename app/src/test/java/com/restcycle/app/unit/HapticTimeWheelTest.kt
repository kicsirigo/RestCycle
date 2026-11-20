package com.restcycle.app.unit

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.HapticTimeWheel
import com.restcycle.app.ui.theme.AtmosphereTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class HapticTimeWheelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasStateDescription(expected: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)

    @Test
    fun wheel24h_drag44dpIncrementsAndDecrementsWithCircularWrapAndHaptic() {
        var time by mutableStateOf(LocalTime.of(23, 59))
        var is24h by mutableStateOf(true)
        lateinit var hostView: View

        composeRule.setContent {
            hostView = LocalView.current
            AtmosphereTheme(skyPhase = SkyPhase.NIGHT) {
                HapticTimeWheel(
                    selectedTime = time,
                    is24HourFormat = is24h,
                    onTimeChange = { time = it }
                )
            }
        }

        composeRule.waitForIdle()
        val shadowView = Shadows.shadowOf(hostView)
        assertEquals(-1, shadowView.lastHapticFeedbackPerformed())

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("23"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).assert(hasStateDescription("59"))

        // Upward drag on hour wraps 23 -> 00
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY + stepPx / 2f))
            moveTo(Offset(centerX, centerY - stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(0, time.hour)
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, shadowView.lastHapticFeedbackPerformed())

        // Downward drag on hour wraps 00 -> 23
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY - stepPx / 2f))
            moveTo(Offset(centerX, centerY + stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(23, time.hour)

        // Upward drag on minute wraps 59 -> 00
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY + stepPx / 2f))
            moveTo(Offset(centerX, centerY - stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(0, time.minute)
    }

    @Test
    fun wheel12h_displays12AtMidnightAndNoonWithAmPmToRightOfMinute() {
        var time by mutableStateOf(LocalTime.of(0, 15))
        lateinit var hostView: View

        composeRule.setContent {
            hostView = LocalView.current
            AtmosphereTheme(skyPhase = SkyPhase.DAY) {
                HapticTimeWheel(
                    selectedTime = time,
                    is24HourFormat = false,
                    onTimeChange = { time = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("12"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("AM"))

        val minuteBounds = composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE)
            .getUnclippedBoundsInRoot()
        val amPmBounds = composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM)
            .getUnclippedBoundsInRoot()
        assertTrue(minuteBounds.right <= amPmBounds.left + 1.dp)

        // External update (e.g. mode switch) syncs silently without firing haptic
        val priorHaptic = Shadows.shadowOf(hostView).lastHapticFeedbackPerformed()
        composeRule.runOnUiThread { time = LocalTime.of(12, 30) }
        composeRule.waitForIdle()
        assertEquals(priorHaptic, Shadows.shadowOf(hostView).lastHapticFeedbackPerformed())
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("12"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("PM"))

        // Verify tabular numerals on wheel digit nodes
        val digitNode = composeRule.onAllNodesWithText("12").onFirst().fetchSemanticsNode()
        val layoutResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        digitNode.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult]
            .action?.invoke(layoutResults)
        assertTrue(layoutResults.isNotEmpty())
        assertEquals("tnum", layoutResults.first().layoutInput.style.fontFeatureSettings)
    }

    @Test
    fun wheelMinute_rapidFlingSnapsToValidValueAwayFromInitial() {
        var time by mutableStateOf(LocalTime.of(6, 50))

        composeRule.setContent {
            AtmosphereTheme(skyPhase = SkyPhase.DAWN_DUSK) {
                HapticTimeWheel(
                    selectedTime = time,
                    is24HourFormat = true,
                    onTimeChange = { time = it }
                )
            }
        }

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).performTouchInput {
            swipeUp(durationMillis = 50L)
        }
        composeRule.waitForIdle()
        assertNotEquals(50, time.minute)
        assertTrue(time.minute in 0..59)
    }

    @Test
    fun wheel_24hTo12hSwitch_0130_preservesHourAndAmWithoutHaptic() {
        assert24hTo12hSwitchPreservesTime(LocalTime.of(1, 30), "1", "AM")
    }

    @Test
    fun wheel_24hTo12hSwitch_0550_preservesHourAndAmWithoutHaptic() {
        assert24hTo12hSwitchPreservesTime(LocalTime.of(5, 50), "5", "AM")
    }

    @Test
    fun wheel_24hTo12hSwitch_1415_preservesHourAndPmWithoutHaptic() {
        assert24hTo12hSwitchPreservesTime(LocalTime.of(14, 15), "2", "PM")
    }

    @Test
    fun wheel_24hTo12hSwitch_2230_preservesHourAndPmWithoutHaptic() {
        assert24hTo12hSwitchPreservesTime(LocalTime.of(22, 30), "10", "PM")
    }

    private fun assert24hTo12hSwitchPreservesTime(
        initialTime: LocalTime,
        expectedHour12: String,
        expectedAmPm: String
    ) {
        var time by mutableStateOf(initialTime)
        var is24h by mutableStateOf(true)
        lateinit var hostView: View

        composeRule.setContent {
            hostView = LocalView.current
            AtmosphereTheme(skyPhase = SkyPhase.NIGHT) {
                HapticTimeWheel(
                    selectedTime = time,
                    is24HourFormat = is24h,
                    onTimeChange = { time = it }
                )
            }
        }

        composeRule.waitForIdle()
        val shadowView = Shadows.shadowOf(hostView)
        assertEquals(-1, shadowView.lastHapticFeedbackPerformed())
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()

        // Switch 24h -> 12h
        composeRule.runOnUiThread { is24h = false }
        composeRule.waitForIdle()

        assertEquals(
            "Time corrupted on 24h->12h switch for $initialTime",
            initialTime,
            time
        )
        assertEquals(
            "Unexpected CLOCK_TICK on 24h->12h switch for $initialTime",
            -1,
            shadowView.lastHapticFeedbackPerformed()
        )
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR)
            .assert(hasStateDescription(expectedHour12))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM)
            .assertIsDisplayed()
            .assert(hasStateDescription(expectedAmPm))

        // Verify wheel still has full headroom to step +1h after 24h->12h switch
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY + stepPx / 2f))
            moveTo(Offset(centerX, centerY - stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(initialTime.plusHours(1), time)
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, shadowView.lastHapticFeedbackPerformed())
    }

    @Test
    fun wheel12h_scrollingAcross11And12AutomaticallyTogglesAmAndPm() {
        var time by mutableStateOf(LocalTime.of(11, 0))

        composeRule.setContent {
            AtmosphereTheme(skyPhase = SkyPhase.DAY) {
                HapticTimeWheel(
                    selectedTime = time,
                    is24HourFormat = false,
                    onTimeChange = { time = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("11"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("AM"))

        // 11 AM -> 12 PM
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY + stepPx / 2f))
            moveTo(Offset(centerX, centerY - stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(LocalTime.of(12, 0), time)
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("12"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("PM"))

        // 12 PM -> 1 PM (no toggle between 12 and 1)
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY + stepPx / 2f))
            moveTo(Offset(centerX, centerY - stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(LocalTime.of(13, 0), time)
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("1"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("PM"))

        // 1 PM -> 12 PM -> 11 AM
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY - stepPx / 2f))
            moveTo(Offset(centerX, centerY + stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(LocalTime.of(12, 0), time)
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("PM"))

        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
            val stepPx = 44.dp.toPx()
            down(Offset(centerX, centerY - stepPx / 2f))
            moveTo(Offset(centerX, centerY + stepPx / 2f), delayMillis = 80L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(LocalTime.of(11, 0), time)
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assert(hasStateDescription("11"))
        composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assert(hasStateDescription("AM"))
    }
}
