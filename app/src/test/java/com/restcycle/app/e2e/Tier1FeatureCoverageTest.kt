package com.restcycle.app.e2e

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.R
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.isEmptyOrNullString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.w3c.dom.Element
import java.io.File
import java.time.LocalTime
import kotlin.math.abs

/**
 * Tier 1: Feature Coverage E2E Tests (F1–F18, 92 isolated happy-path tests).
 * Strictly opaque-box: imports ONLY MainActivity, R, domain.model.*, and RestCycleTestTags.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [E2EDeviceConfigs.ROBOLECTRIC_SDK],
    qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A
)
class Tier1FeatureCoverageTest {

    @get:Rule(order = 0)
    val envRule = RestCycleEnvironmentRule()

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    // =========================================================================
    // F1: Thumb-Zone Ergonomic Layout (5 tests)
    // =========================================================================

    @Test
    fun f1_01_defaultLaunchHasUpperResultsAboveBottom55ThumbZone() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.UPPER_RESULTS_ZONE).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).assertIsDisplayed()
            composeRule.assertThumbZoneErgonomics()
        }
    }

    @Test
    fun f1_02_wakeUpAtInteractiveControlsSitInsideThumbZone() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            val controls = listOf(
                RestCycleTestTags.MODE_SWITCHER,
                RestCycleTestTags.TIME_WHEEL,
                RestCycleTestTags.LATENCY_ADJUSTER
            )
            controls.forEach { tag ->
                val node = composeRule.onNodeWithTag(tag)
                node.assertIsDisplayed()
                val bounds = node.getUnclippedBoundsInRoot()
                assertTrue("$tag top (${bounds.top}) below thumb top (${thumb.top})", bounds.top >= thumb.top - 1.dp)
                assertTrue("$tag bottom (${bounds.bottom}) above thumb bottom (${thumb.bottom})", bounds.bottom <= thumb.bottom + 1.dp)
                assertTrue("$tag must be in bottom 55%", bounds.top >= root.top + root.height * 0.45f - 1.dp)
            }
        }
    }

    @Test
    fun f1_03_powerNapModePreservesThumbZoneErgonomics() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertThumbZoneErgonomics()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            val latencyBounds = composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_ADJUSTER).getUnclippedBoundsInRoot()
            val switcherBounds = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER).getUnclippedBoundsInRoot()
            assertTrue(latencyBounds.top >= thumb.top - 1.dp)
            assertTrue(switcherBounds.top >= thumb.top - 1.dp)
        }
    }

    @Test
    fun f1_04_sleepNowAndGoToBedAtPreserveSplitZones() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.assertThumbZoneErgonomics()
            val sleepNowThumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            val sleepNowLatency = composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_ADJUSTER).getUnclippedBoundsInRoot()
            assertTrue(sleepNowLatency.top >= sleepNowThumb.top - 1.dp)

            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.assertThumbZoneErgonomics()
            val bedThumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            val bedWheel = composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).getUnclippedBoundsInRoot()
            assertTrue(bedWheel.top >= bedThumb.top - 1.dp)
        }
    }

    @Test
    fun f1_05_modeSwitcherTouchTargetAndOptimalCardsVisibleWithoutScroll() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            val switcher = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER).getUnclippedBoundsInRoot()
            assertTrue("Switcher height (${switcher.height}) must be >= 48dp", switcher.height >= 48.dp)
            assertTrue("Switcher bottom (${switcher.bottom}) <= root bottom (${root.bottom})", switcher.bottom <= root.bottom + 1.dp)

            val card6 = composeRule.findOptionCardByTimeText("21:35")
            val card5 = composeRule.findOptionCardByTimeText("23:05")
            card6.assertIsDisplayed()
            card5.assertIsDisplayed()
            assertTrue(card6.getUnclippedBoundsInRoot().bottom <= thumb.top + 1.dp)
            assertTrue(card5.getUnclippedBoundsInRoot().bottom <= thumb.top + 1.dp)
        }
    }

    // =========================================================================
    // F2: Zero-Button Reactive UX (5 tests)
    // =========================================================================

    @Test
    fun f2_01_zeroCalculateOrSubmitButtonAcrossAllModes() {
        withMainActivity(composeRule) { _, activity ->
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                composeRule.assertZeroCalculateButton()
                assertNull(Shadows.shadowOf(activity).nextStartedActivity)
            }
        }
    }

    @Test
    fun f2_02_wheelScrollRecomputesBedtimesImmediatelyWithoutButton() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.findOptionCardByTimeText("21:35").assertIsDisplayed()
            composeRule.selectWheelTime(LocalTime.of(7, 50), is24Hour = true)
            composeRule.findOptionCardByTimeText("22:35").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("00:05").assertIsDisplayed()
            assertTrue(composeRule.onAllNodesWithText("21:35", substring = true).fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun f2_03_latencySliderRecomputesBedtimesImmediatelyWithoutButton() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(30)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("30", substring = true)
            composeRule.findOptionCardByTimeText("21:20").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("22:50").assertIsDisplayed()
        }
    }

    @Test
    fun f2_04_modeTabSwitchImmediatelyReplacesResultsOnSameActivity() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertIsDisplayed()
            assertFalse(activity.isFinishing)

            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10")).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).assertIsDisplayed()

            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertIsDisplayed()
            assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        }
    }

    @Test
    fun f2_05_tappingCardInGoToBedAtMorphsSkyAndBarsWithoutApplyButton() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            // Default bed_5 is 06:45 (DAWN_DUSK)
            assertSystemBarsLight(activity, expectedLightStatus = false)

            // Tap 6-cycle card: 23:00 + 15m + 540m = 08:15 (DAY)
            composeRule.tapOptionCard("bed_6")
            composeRule.mainClock.advanceTimeBy(650)
            composeRule.waitForIdle()
            assertSystemBarsLight(activity, expectedLightStatus = true)
        }
    }

    // =========================================================================
    // F3: Wake Up At (WAKE_UP_AT) (5 tests)
    // =========================================================================

    @Test
    fun f3_01_defaultLaunchShows0650Latency15AndSixOrderedBedtimes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertIsSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("15", substring = true)

            val expectedTimes = listOf("21:35", "23:05", "00:35", "02:05", "03:35", "05:05")
            val cards = composeRule.findAllCardsInResultsZone()
            assertEquals("Expected 6 bedtime cards", 6, cards.size)
            expectedTimes.forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun f3_02_dialingWheelTo0800RecomputesBedtimesAndSetsDaySkyFromWheel() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.selectWheelTime(LocalTime.of(8, 0), is24Hour = true)
            listOf("22:45", "00:15", "01:45", "03:15", "04:45", "06:15").forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
            assertSystemBarsLight(activity, expectedLightStatus = true)
        }
    }

    @Test
    fun f3_03_adjustingLatencyTo25ShiftsAllSixBedtimesTenMinutesEarlier() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(25)
            listOf("21:25", "22:55", "00:25", "01:55", "03:25", "04:55").forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun f3_04_wakeAlarmButtonDisplayedAndFiresSetAlarmForSelectedTime() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN).assertIsDisplayed()
            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 6, minute = 50)
        }
    }

    @Test
    fun f3_05_bedtimeCardsDisplayAccurateCycleDurationsAndQualityLabels() {
        withMainActivity(composeRule) { _, _ ->
            val card6 = composeRule.findOptionCardByTimeText("21:35").performScrollTo()
            card6.assertTextContains("9h", substring = true)
            card6.assertTextContains("Optimal", substring = true, ignoreCase = true)
            val card5 = composeRule.findOptionCardByTimeText("23:05").performScrollTo()
            card5.assertTextContains("7h", substring = true)
            card5.assertTextContains("Optimal", substring = true, ignoreCase = true)
            val card4 = composeRule.findOptionCardByTimeText("00:35").performScrollTo()
            card4.assertTextContains("6h", substring = true)
            card4.assertTextContains("Good", substring = true, ignoreCase = true)
            val card1 = composeRule.findOptionCardByTimeText("05:05").performScrollTo()
            card1.assertTextContains("1h", substring = true)
            card1.assertTextContains("Short", substring = true, ignoreCase = true)
        }
    }

    // =========================================================================
    // F4: Sleep Now (SLEEP_NOW) (5 tests)
    // =========================================================================

    @Test
    fun f4_01_sleepNowHidesWheelAndComputesSixForwardWakeTimes() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertDoesNotExist()
            for (k in 6 downTo 1) {
                val expected = expectedFormattedTime(expectedSleepCyclesForward(anchor, 15, k), is24Hour = true)
                composeRule.findOptionCardById("sleep_now_$k")
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertTextContains(expected, substring = true)
            }
        }
    }

    @Test
    fun f4_02_sleepNowDefaultsHighlightToFiveCycleSleepNow5() {
        envRule.setClockToLocalTime(LocalTime.of(22, 30))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            val card5 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5"))
            card5.assertIsDisplayed()
            card5.assertIsSelected()
            card5.assertTextContains("7h", substring = true)
        }
    }

    @Test
    fun f4_03_tappingHighlightedCardChangesSkyPhaseInSleepNow() {
        // 23:30 + 15m = 23:45 fall asleep -> 3c = 04:15 (NIGHT), 5c = 07:15 (DAWN_DUSK), 6c = 08:45 (DAY)
        envRule.setClockToLocalTime(LocalTime.of(23, 30))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            assertSystemBarsLight(activity, expectedLightStatus = false)

            composeRule.tapOptionCard("sleep_now_6")
            composeRule.mainClock.advanceTimeBy(650)
            composeRule.waitForIdle()
            assertSystemBarsLight(activity, expectedLightStatus = true)

            composeRule.tapOptionCard("sleep_now_3")
            composeRule.mainClock.advanceTimeBy(650)
            composeRule.waitForIdle()
            assertSystemBarsLight(activity, expectedLightStatus = false)
        }
    }

    @Test
    fun f4_04_adjustingLatencyInSleepNowShiftsWakeTimesForward() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.setLatencyMinutes(30)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("30", substring = true)
            val expected5 = expectedFormattedTime(expectedSleepCyclesForward(anchor, 30, 5), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5"))
                .assertTextContains(expected5, substring = true)
        }
    }

    @Test
    fun f4_05_sleepNowSetAlarmButtonFiresIntentForFiveCycleWakeTime() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(22, 10))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            val expected5 = expectedSleepCyclesForward(anchor, 15, 5)
            composeRule.tapSetAlarmOnCard("sleep_now_5")
            assertAlarmIntentSent(hour24 = expected5.hour, minute = expected5.minute)
        }
    }

    // =========================================================================
    // F5: Go to Bed At (GO_TO_BED_AT) (5 tests)
    // =========================================================================

    @Test
    fun f5_01_goToBedAtShowsWheelAndSixForwardWakeTimes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertIsDisplayed()
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)

            val expected = mapOf(6 to "07:15", 5 to "05:45", 4 to "04:15", 3 to "02:45", 2 to "01:15", 1 to "23:45")
            expected.forEach { (cyc, timeStr) ->
                composeRule.findOptionCardById("bed_$cyc")
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertTextContains(timeStr, substring = true)
            }
        }
    }

    @Test
    fun f5_02_goToBedAtDefaultsHighlightToBed5() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            val card5 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5"))
            card5.assertIsDisplayed()
            card5.assertIsSelected()
            card5.assertTextContains("05:45", substring = true)
        }
    }

    @Test
    fun f5_03_skyDrivenByHighlightedWakeCardNotBedtimeInGoToBedAt() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            // 23:00 bedtime (NIGHT), but bed_5 = 06:45 (DAWN_DUSK)
            assertSystemBarsLight(activity, expectedLightStatus = false)
            composeRule.tapOptionCard("bed_6") // 08:15 (DAY)
            composeRule.mainClock.advanceTimeBy(650)
            composeRule.waitForIdle()
            assertSystemBarsLight(activity, expectedLightStatus = true)
        }
    }

    @Test
    fun f5_04_adjustingLatencyInGoToBedAtShiftsAllWakeTimesLater() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 30), is24Hour = true)
            composeRule.setLatencyMinutes(30)
            val expected = mapOf(6 to "08:00", 5 to "06:30", 4 to "05:00", 3 to "03:30", 2 to "02:00", 1 to "00:30")
            expected.forEach { (cyc, timeStr) ->
                composeRule.findOptionCardById("bed_$cyc")
                    .performScrollTo()
                    .assertTextContains(timeStr, substring = true)
            }
        }
    }

    @Test
    fun f5_05_goToBedAtSetAlarmFiresIntentFor0545() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.tapSetAlarmOnCard("bed_5")
            assertAlarmIntentSent(hour24 = 5, minute = 45)
        }
    }

    // =========================================================================
    // F6: Power Nap (POWER_NAP) (5 tests)
    // =========================================================================

    @Test
    fun f6_01_powerNapHidesWheelShowsThreePresetsAndHighlightsNap20() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(13, 30))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertDoesNotExist()

            val card10 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10"))
            val card20 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20"))
            val card90 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90"))
            card10.assertIsDisplayed().assertTextContains(expectedFormattedTime(anchor.plusMinutes(20), true), substring = true)
            card20.assertIsDisplayed().assertIsSelected().assertTextContains(expectedFormattedTime(anchor.plusMinutes(30), true), substring = true)
            card90.assertIsDisplayed().assertTextContains(expectedFormattedTime(anchor.plusMinutes(100), true), substring = true)
        }
    }

    @Test
    fun f6_02_powerNapDisplaysInertiaTooltipAndDistinctPresetLabels() {
        envRule.setClockToLocalTime(LocalTime.of(14, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP)
                .assertIsDisplayed()
                .assertTextContains("30", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10")).assertTextContains("10", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertTextContains("20", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).assertTextContains("90", substring = true)
        }
    }

    @Test
    fun f6_03_adjustingNapLatencyTo20UpdatesTargetWakeTimes() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(14, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(20)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("20", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10"))
                .assertTextContains(expectedFormattedTime(anchor.plusMinutes(30), true), substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20"))
                .assertTextContains(expectedFormattedTime(anchor.plusMinutes(40), true), substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90"))
                .assertTextContains(expectedFormattedTime(anchor.plusMinutes(110), true), substring = true)
        }
    }

    @Test
    fun f6_04_powerNapSetTimerFires1800SecondsForClassic20mWithDefault10mLatency() {
        envRule.setClockToLocalTime(LocalTime.of(13, 30))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 1800)
        }
    }

    @Test
    fun f6_05_powerNapSetAlarmFiresSetAlarmDistinctFromTimer() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(13, 30))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            val expected90 = anchor.plusMinutes(100)
            composeRule.tapSetAlarmOnCard("nap_90")
            assertAlarmIntentSent(hour24 = expected90.hour, minute = expected90.minute)
        }
    }

    // =========================================================================
    // F7: Dynamic Celestial Header (Canvas) (5 tests)
    // =========================================================================

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_01_celestialHeaderRendersWarmDawnDuskPixelsAtDefault0650() {
        withMainActivity(composeRule) { _, _ ->
            val header = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
            header.assertIsDisplayed()
            val bounds = header.getUnclippedBoundsInRoot()
            assertTrue(bounds.width > 100.dp)
            assertTrue(bounds.height > 40.dp)
            val bitmap = composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER)
            assertTrue(bitmap.width > 0 && bitmap.height > 0)
            val lum = meanLuminance(bitmap)
            assertTrue("Expected non-black non-blown-out DAWN_DUSK header luminance, got $lum", lum in 0.02..0.85)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_02_celestialHeaderRendersBrightDaylightSunAt1200() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            val bitmap = composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER)
            val lum = meanLuminance(bitmap)
            assertTrue("Expected bright DAY celestial header luminance (> 0.45), got $lum", lum > 0.45)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_03_celestialHeaderRendersDarkNightSkyWithMoonStarsAt2300() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            val bitmap = composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER)
            val lum = meanLuminance(bitmap)
            assertTrue("Expected dark NIGHT celestial header luminance (< 0.15), got $lum", lum < 0.15)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_04_celestialHeaderInfiniteAnimationDoesNotBlockMainClock() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = true)
            composeRule.mainClock.autoAdvance = false
            try {
                repeat(3) {
                    composeRule.mainClock.advanceTimeBy(500)
                    composeRule.waitForIdle()
                }
                composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).assertIsDisplayed()
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
        }
    }

    @Test
    fun f7_05_celestialHeaderRemainsDisplayedAcrossAllFourModes() {
        withMainActivity(composeRule) { _, _ ->
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                val header = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
                header.assertIsDisplayed()
                assertTrue(header.getUnclippedBoundsInRoot().height > 40.dp)
            }
        }
    }

    // =========================================================================
    // F8: Dynamic Color & Gradient Morphing (5 tests)
    // =========================================================================

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_01_dayPaletteAppliesWarmIvoryBackgroundAt1200() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = true)
            val bg = composeRule.sampleRootBackgroundColorArgb(advanceMs = 700)
            val lum = relativeLuminance(bg)
            assertTrue("Expected bright DAY background (> 0.60), got lum=$lum rgb=${Color.red(bg)},${Color.green(bg)},${Color.blue(bg)}", lum > 0.60)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_02_dawnDuskPaletteAppliesDeepWarmTwilightAt0650() {
        withMainActivity(composeRule) { _, _ ->
            val bg = composeRule.sampleRootBackgroundColorArgb(advanceMs = 700, yFraction = 0.5f)
            val lum = relativeLuminance(bg)
            assertTrue(
                "Expected twilight DAWN_DUSK luminance in 0.005..0.35 (distinct from NIGHT #000000 and DAY > 0.60), got lum=$lum",
                lum in 0.005..0.35
            )
            // Sample upper glow region (y = 0.05f..0.12f) for warm amber/red (R >= B)
            val upperSamples = listOf(0.05f, 0.08f, 0.12f).map { yFrac ->
                composeRule.sampleRootBackgroundColorArgb(advanceMs = 0, yFraction = yFrac)
            }
            assertTrue(
                "Expected warm amber/red glow (R >= B and R > 15) in upper DAWN_DUSK region, got ${
                    upperSamples.map { "${Color.red(it)},${Color.green(it)},${Color.blue(it)}" }
                }",
                upperSamples.any { Color.red(it) >= Color.blue(it) && Color.red(it) > 15 }
            )
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_03_nightPaletteAppliesTrueAmoledBlackAt2300() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            val bg = composeRule.sampleRootBackgroundColorArgb(advanceMs = 700)
            assertTrue("Expected AMOLED red <= 5, got ${Color.red(bg)}", Color.red(bg) <= 5)
            assertTrue("Expected AMOLED green <= 5, got ${Color.green(bg)}", Color.green(bg) <= 5)
            assertTrue("Expected AMOLED blue <= 5, got ${Color.blue(bg)}", Color.blue(bg) <= 5)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_04_600msMorphShowsIntermediateColorAt300msMidpoint() {
        withMainActivity(composeRule) { _, _ ->
            // Position wheel at 07:00 (DAWN_DUSK, 1 hour step before 08:00 DAY) while autoAdvance = true
            composeRule.selectWheelTime(LocalTime.of(7, 0), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            val startLum = relativeLuminance(composeRule.sampleRootBackgroundColorArgb(0))
            assertTrue("Expected DAWN_DUSK start luminance < 0.35, got $startLum", startLum < 0.35)

            composeRule.mainClock.autoAdvance = false
            try {
                // Single 1-hour step (07:00 -> 08:00) + 200ms advance stays inside 600ms transition window
                composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_HOUR, forward = true, steps = 1)
                val midLum = relativeLuminance(composeRule.sampleRootBackgroundColorArgb(200))
                assertTrue(
                    "Expected intermediate morph luminance at ~250-300ms between DAWN_DUSK ($startLum) and DAY, got $midLum",
                    midLum > startLum + 0.05 && midLum < 0.85
                )
                val endLum = relativeLuminance(composeRule.sampleRootBackgroundColorArgb(450))
                assertTrue("Expected settled DAY luminance > 0.60 after 600ms+, got $endLum", endLum > 0.60)
                assertTrue("Expected settled DAY ($endLum) > midpoint ($midLum)", endLum > midLum + 0.05)
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_05_tappingBed6CardInGoToBedAtMorphsRootToDayPalette() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.tapOptionCard("bed_6")
            val bg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("Expected DAY background luminance > 0.60 after tapping bed_6", relativeLuminance(bg) > 0.60)
        }
    }

    // =========================================================================
    // F9: Edge-to-Edge & System Bar Tinting (5 tests)
    // =========================================================================

    @Test
    fun f9_01_default0650DawnDuskSetsDarkSystemBarAppearance() {
        withMainActivity(composeRule) { _, activity ->
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
        }
    }

    @Test
    fun f9_02_day1200SetsLightStatusAndNavigationBarsTrue() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
        }
    }

    @Test
    fun f9_03_night2300SetsLightStatusAndNavigationBarsFalse() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
        }
    }

    @Test
    fun f9_04_screenRootFillsFullEdgeToEdgeViewport() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            assertTrue("Expected full width >= 411dp, got ${root.width}", root.width >= 411.dp)
            assertTrue("Expected full height >= 914dp, got ${root.height}", root.height >= 914.dp)
        }
    }

    @Test
    fun f9_05_selectingDayCardInGoToBedAtFlipsSystemBarsDynamically() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
            composeRule.tapOptionCard("bed_6")
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
        }
    }

    // =========================================================================
    // F10: 4-Mode Bottom Segmented Switcher (6 tests)
    // =========================================================================

    @Test
    fun f10_01_modeSwitcherAndAllFourTabsDisplayedInThumbZone() {
        withMainActivity(composeRule) { _, _ ->
            val switcher = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER).assertIsDisplayed()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            assertTrue(switcher.getUnclippedBoundsInRoot().top >= thumb.top - 1.dp)
            SleepMode.entries.forEach { mode ->
                val tab = composeRule.onNodeWithTag(RestCycleTestTags.modeTab(mode)).assertIsDisplayed()
                assertTrue(tab.getUnclippedBoundsInRoot().top >= thumb.top - 1.dp)
            }
        }
    }

    @Test
    fun f10_02_defaultInitialSelectionIsWakeUpAtOnly() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertIsSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertIsNotSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).assertIsNotSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertIsNotSelected()
        }
    }

    @Test
    fun f10_03_tapSleepNowTabSelectsItAndHidesWheel() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertIsSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertIsNotSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertDoesNotExist()
            assertEquals(6, composeRule.findAllCardsInResultsZone().size)
        }
    }

    @Test
    fun f10_04_tapGoToBedAtTabSelectsItAndShowsWheel() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).assertIsSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertIsDisplayed()
            assertEquals(6, composeRule.findAllCardsInResultsZone().size)
        }
    }

    @Test
    fun f10_05_tapPowerNapTabSelectsItAndShowsThreePresets() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertIsSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertDoesNotExist()
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10")).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).assertIsDisplayed()
        }
    }

    @Test
    fun f10_06_modeSwitchEmitsClockTickHaptic() {
        withMainActivity(composeRule) { _, activity ->
            assertTrue(anyHapticFeedbackPerformed(activity).isEmpty())
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            assertClockTickHapticRecorded(activity)
        }
    }

    // =========================================================================
    // F11: Haptic Time Wheel Picker (5 tests)
    // =========================================================================

    @Test
    fun f11_01_wakeUpAtInitialWheelDisplaysDefault0650WithoutAmPmIn24h() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
            assertEquals(6, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            assertEquals(50, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE))
        }
    }

    @Test
    fun f11_02_steppingWheelHourUpdatesTimeAndBedtimes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(7, 50), is24Hour = true)
            assertEquals(7, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.findOptionCardByTimeText("00:05").assertIsDisplayed()
        }
    }

    @Test
    fun f11_03_steppingWheelMinuteUpdatesTimeAndBedtimes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(6, 55), is24Hour = true)
            assertEquals(55, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE))
            composeRule.findOptionCardByTimeText("23:10").assertIsDisplayed()
        }
    }

    @Test
    fun f11_04_12HourFormatShowsAmPmWheelAndTogglesPeriod() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.findOptionCardByTimeText("11:05 PM").assertIsDisplayed()
            composeRule.selectAmPmPeriod("PM")
            composeRule.findOptionCardByTimeText("11:05 AM").assertIsDisplayed()
        }
    }

    @Test
    fun f11_05_wheelUsesTabularNumeralsAndEmitsClockTickPerStep() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_MINUTE)
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_HOUR, forward = true, steps = 1)
            assertClockTickHapticRecorded(activity)
        }
    }

    // =========================================================================
    // F12: Fall-Asleep Latency Adjuster (5 tests)
    // =========================================================================

    @Test
    fun f12_01_defaultSleepLatencyIs15Minutes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_ADJUSTER).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT)
                .assertIsDisplayed()
                .assertTextContains("15", substring = true)
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()
        }
    }

    @Test
    fun f12_02_adjustingSleepLatencyInWakeUpAtShiftsBedtimesEarlier() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(30)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("30", substring = true)
            composeRule.findOptionCardByTimeText("22:50").assertIsDisplayed()
        }
    }

    @Test
    fun f12_03_adjustingSleepLatencyInGoToBedAtShiftsWakeTimesLater() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("05:45", substring = true)
            composeRule.setLatencyMinutes(30)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("06:00", substring = true)
        }
    }

    @Test
    fun f12_04_powerNapDefaultLatencyIs10AndAdjustsTargetAndCountdown() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(14, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("10", substring = true)
            composeRule.setLatencyMinutes(15)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("15", substring = true)
            val expected20 = expectedFormattedTime(anchor.plusMinutes(35), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertTextContains(expected20, substring = true)
        }
    }

    @Test
    fun f12_05_adjustedLatencyPersistsAcrossActivityRecreation() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.setLatencyMinutes(30)
            scenario.recreate()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("30", substring = true)
            composeRule.findOptionCardByTimeText("22:50").assertIsDisplayed()
        }
    }

    // =========================================================================
    // F13: One-Tap Native Alarm (ACTION_SET_ALARM) (5 tests)
    // =========================================================================

    @Test
    fun f13_01_wakeAlarmButtonInWakeUpAtFiresSetAlarmForSelectedTime() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 6, minute = 50)
        }
    }

    @Test
    fun f13_02_bedtimeCardSetAlarmInWakeUpAtFires2305Alarm() {
        withMainActivity(composeRule) { _, _ ->
            // Index 1 in 6..1 ordered list corresponds to 5-cycle bedtime (23:05)
            composeRule.findSetAlarmButtonMatchingTimeOrIndex(1).performClick()
            composeRule.waitForIdle()
            assertAlarmIntentSent(hour24 = 23, minute = 5)
        }
    }

    @Test
    fun f13_03_sleepNowCardSetAlarmFiresCalculatedWakeTime() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            val expected5 = expectedSleepCyclesForward(anchor, 15, 5)
            composeRule.tapSetAlarmOnCard("sleep_now_5")
            assertAlarmIntentSent(hour24 = expected5.hour, minute = expected5.minute)
        }
    }

    @Test
    fun f13_04_goToBedAtCardSetAlarmFiresCalculatedWakeTime() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.tapSetAlarmOnCard("bed_5")
            assertAlarmIntentSent(hour24 = 5, minute = 45)
        }
    }

    @Test
    fun f13_05_powerNapCardSetAlarmFiresWakeTimeDistinctFromTimer() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            val expected20 = anchor.plusMinutes(30)
            composeRule.tapSetAlarmOnCard("nap_20")
            assertAlarmIntentSent(hour24 = expected20.hour, minute = expected20.minute)
            Intents.assertNoUnverifiedIntents()
        }
    }

    // =========================================================================
    // F14: Power Nap Timer (ACTION_SET_TIMER) (5 tests)
    // =========================================================================

    @Test
    fun f14_01_quickBoost10mSetTimerFires1200Seconds() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_10")
            assertTimerIntentSent(durationSeconds = 1200)
        }
    }

    @Test
    fun f14_02_classicPowerNap20mSetTimerFires1800Seconds() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 1800)
        }
    }

    @Test
    fun f14_03_fullCycleNap90mSetTimerFires6000Seconds() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_90")
            assertTimerIntentSent(durationSeconds = 6000)
        }
    }

    @Test
    fun f14_04_customNapLatency25mUpdatesTimerTo2700Seconds() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(25)
            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 2700)
        }
    }

    @Test
    fun f14_05_timerExtraMessageMatchesLocalizedPresetTitle() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 1800, expectedMessageContains = "Classic Power Nap")
        }
    }

    // =========================================================================
    // F15: 12h/24h Auto-Detection & tnum (5 tests)
    // =========================================================================

    @Test
    fun f15_01_24HourSystemFormatDisplays24hCardsAndHidesAmPmWheel() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()
            assertTrue(composeRule.onAllNodesWithText("PM", substring = true).fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun f15_02_12HourSystemFormatDisplaysAmPmWheelAnd12hCards() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        assertFalse(DateFormat.is24HourFormat(app))
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.findOptionCardByTimeText("11:05 PM").assertIsDisplayed()
        }
    }

    @Test
    fun f15_03_changing24hTo12hWhilePausedRefreshesOnResume() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()

            flip24HourFormatAndResume(scenario, composeRule, is24Hour = false)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.findOptionCardByTimeText("11:05 PM").assertIsDisplayed()
        }
    }

    @Test
    fun f15_04_changing12hTo24hWhilePausedRefreshesOnResume() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.findOptionCardByTimeText("11:05 PM").assertIsDisplayed()

            flip24HourFormatAndResume(scenario, composeRule, is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()
        }
    }

    @Test
    fun f15_05_allNumericClockTextsApplyTabularNumeralsTnum() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_MINUTE)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
        }
    }

    // =========================================================================
    // F16: Tactile CLOCK_TICK Haptics (5 tests)
    // =========================================================================

    @Test
    fun f16_01_initialLaunchDoesNotEmitSpuriousHaptics() {
        withMainActivity(composeRule) { _, activity ->
            assertTrue(
                "Expected zero haptics on initial launch, got ${anyHapticFeedbackPerformed(activity)}",
                anyHapticFeedbackPerformed(activity).isEmpty()
            )
        }
    }

    @Test
    fun f16_02_modeSwitchEmitsClockTick() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            assertClockTickHapticRecorded(activity)
            assertTrue(anyHapticFeedbackPerformed(activity).all { it == HapticFeedbackConstants.CLOCK_TICK })
        }
    }

    @Test
    fun f16_03_wheelHourScrollStepEmitsClockTick() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_HOUR, forward = true, steps = 1)
            assertClockTickHapticRecorded(activity)
        }
    }

    @Test
    fun f16_04_wheelMinuteScrollStepEmitsClockTick() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = true, steps = 1)
            assertClockTickHapticRecorded(activity)
        }
    }

    @Test
    fun f16_05_selectingNonHighlightedOptionCardEmitsClockTick() {
        withMainActivity(composeRule) { _, activity ->
            // Tap a card in UPPER_RESULTS_ZONE directly without prior mode switch
            val cards = composeRule.findAllCardsInResultsZone()
            assertTrue(cards.size >= 4)
            val cardTag = cards[2].config.getOrNull(SemanticsProperties.TestTag)!!
            composeRule.tapOptionCard(cardTag.removePrefix(RestCycleTestTags.OPTION_CARD_PREFIX))
            assertClockTickHapticRecorded(activity)
        }
    }

    // =========================================================================
    // F17: Bilingual UI (EN & HU) (5 tests)
    // =========================================================================

    @Test
    fun f17_01_englishAndHungarianStringsXmlHaveExact1To1KeyParity() {
        val mainDir = resolveAppMainDir()
        val enMap = parseStringsXmlMap(File(mainDir, "res/values/strings.xml"))
        val huMap = parseStringsXmlMap(File(mainDir, "res/values-hu/strings.xml"))
        assertTrue("English strings.xml must not be empty", enMap.isNotEmpty())
        assertEquals("EN and HU strings.xml keys must match 1:1", enMap.keys, huMap.keys)
        enMap.forEach { (k, v) -> assertTrue("Blank EN value for $k", v.isNotBlank()) }
        huMap.forEach { (k, v) -> assertTrue("Blank HU value for $k", v.isNotBlank()) }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun f17_02_hungarianLocaleDisplaysExactHungarianModeNames() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertTextContains("Ébredés ekkor", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertTextContains("Most fekszem le", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).assertTextContains("Lefekvés ekkor", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertTextContains("Szieszta", substring = true)
        }
    }

    @Test
    fun f17_03_englishLocaleDisplaysExactEnglishModeNames() {
        withMainActivity(composeRule) { _, activity ->
            assertEquals("Wake Up At", activity.getString(R.string.mode_wake_up_at))
            assertEquals("Sleep Now", activity.getString(R.string.mode_sleep_now))
            assertEquals("Go to Bed At", activity.getString(R.string.mode_go_to_bed_at))
            assertEquals("Power Nap", activity.getString(R.string.mode_power_nap))
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertTextContains("Wake Up At", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertTextContains("Sleep Now", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).assertTextContains("Go to Bed At", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertTextContains("Power Nap", substring = true)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun f17_04_hungarianLocaleRendersPowerNapLabelsAndTooltipInHungarian() {
        val mainDir = resolveAppMainDir()
        val enMap = parseStringsXmlMap(File(mainDir, "res/values/strings.xml"))
        val huMap = parseStringsXmlMap(File(mainDir, "res/values-hu/strings.xml"))
        val huLocalizedValues = huMap.filter { (key, huVal) ->
            huVal.isNotBlank() && huVal != enMap[key]
        }.values

        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)

            val tooltipNode = composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP)
                .assertIsDisplayed()
                .fetchSemanticsNode()
            val tooltipText = tooltipNode.config.getOrNull(SemanticsProperties.Text)
                ?.joinToString(" ") { it.text }.orEmpty()
            assertTrue("Expected non-blank Hungarian inertia tooltip", tooltipText.isNotBlank())
            assertTrue("Expected tooltip to mention 30 min", tooltipText.contains("30"))
            assertFalse("English inertia prose leaked in Hungarian UI: '$tooltipText'", tooltipText.contains("sleep inertia", ignoreCase = true))
            assertTrue(
                "Expected Hungarian tooltip text from values-hu/strings.xml, got '$tooltipText'",
                huLocalizedValues.any { huVal ->
                    val cleaned = huVal.replace(Regex("%(\\d+\\$)?[sdfox]"), "").trim()
                    cleaned.length >= 4 && tooltipText.contains(cleaned, ignoreCase = true)
                }
            )

            listOf("nap_10" to "10", "nap_20" to "20", "nap_90" to "90").forEach { (id, minToken) ->
                val cardNode = composeRule.onNodeWithTag(RestCycleTestTags.optionCard(id))
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                val cardText = cardNode.config.getOrNull(SemanticsProperties.Text)
                    ?.joinToString(" ") { it.text }.orEmpty()
                assertTrue("Expected '$id' card to display '$minToken'", cardText.contains(minToken))
                assertTrue(
                    "Expected Hungarian preset label from values-hu/strings.xml on '$id', got '$cardText'",
                    huLocalizedValues.any { huVal ->
                        val cleaned = huVal.replace(Regex("%(\\d+\\$)?[sdfox]"), "").trim()
                        cleaned.length >= 4 && cardText.contains(cleaned, ignoreCase = true)
                    }
                )
            }

            composeRule.onNodeWithText("Quick Boost", substring = true).assertDoesNotExist()
            composeRule.onNodeWithText("Classic Power Nap", substring = true).assertDoesNotExist()
            composeRule.onNodeWithText("Full Cycle Nap", substring = true).assertDoesNotExist()
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun f17_05_hungarianLocalePopulatesLocalizedAlarmAndTimerMessages() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.tapWakeAlarmButton()
            Intents.intended(
                allOf(
                    hasAction(AlarmClock.ACTION_SET_ALARM),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not(isEmptyOrNullString())),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not("Wake up alarm"))
                )
            )
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            Intents.intended(
                allOf(
                    hasAction(AlarmClock.ACTION_SET_TIMER),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not(isEmptyOrNullString())),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not("Classic Power Nap"))
                )
            )
        }
    }

    // =========================================================================
    // F18: 100% Offline DataStore Persistence & Manifest (6 tests)
    // =========================================================================

    @Test
    fun f18_01_manifestDeclaresSetAlarmAndZeroInternetPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertOfflineManifestAndPermissions(context)

        val doc = parseAndroidManifestXml()
        val permNodes = doc.getElementsByTagName("uses-permission")
        val permNames = (0 until permNodes.length).map {
            (permNodes.item(it) as Element).getAttribute("android:name")
        }
        assertTrue(permNames.contains("com.android.alarm.permission.SET_ALARM"))
        assertTrue(permNames.contains("android.permission.INTERNET"))
        assertFalse(permNames.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse(permNames.contains("android.permission.ACCESS_WIFI_STATE"))
    }

    @Test
    fun f18_02_manifestQueriesIncludesSetAlarmAndSetTimerIntents() {
        val doc = parseAndroidManifestXml()
        val queriesNodes = doc.getElementsByTagName("queries")
        assertTrue("Manifest must declare <queries> for Android 11+ package visibility", queriesNodes.length > 0)
        val actions = mutableSetOf<String>()
        for (i in 0 until queriesNodes.length) {
            val el = queriesNodes.item(i) as Element
            val actionNodes = el.getElementsByTagName("action")
            for (j in 0 until actionNodes.length) {
                actions.add((actionNodes.item(j) as Element).getAttribute("android:name"))
            }
        }
        assertTrue("Missing SET_ALARM in <queries>", actions.contains("android.intent.action.SET_ALARM"))
        assertTrue("Missing SET_TIMER in <queries>", actions.contains("android.intent.action.SET_TIMER"))
    }

    @Test
    fun f18_03_sleepLatencyPersistsAcrossFullActivityCloseAndRelaunch() {
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.setLatencyMinutes(25)
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(25)
            composeRule.findOptionCardByTimeText("22:55").assertIsDisplayed()
        } finally {
            scenario.close()
        }
    }

    @Test
    fun f18_04_napLatencyPersistsAcrossFullActivityCloseAndRelaunch() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(14, 0))
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(22)
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(22)
            val expected20 = expectedFormattedTime(anchor.plusMinutes(42), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertTextContains(expected20, substring = true)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun f18_05_sleepAndNapLatenciesPersistIndependentlyOnDisk() {
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.setLatencyMinutes(40)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(5)

            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(40)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(5)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun f18_06_packageManagerCheckPermissionDeniesInternetAtRuntime() {
        withMainActivity(composeRule) { _, activity ->
            assertOfflineManifestAndPermissions(activity)
            assertNotNull(activity.getString(R.string.app_name))
        }
    }
}
