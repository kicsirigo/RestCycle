package com.restcycle.app.e2e

import android.content.Context
import android.graphics.Color
import android.provider.AlarmClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import org.w3c.dom.Element
import java.io.File
import java.time.LocalTime
import kotlin.math.abs

/**
 * Tier 2: Boundary, Corner & Negative Case E2E Tests (F1–F18, 91 tests).
 * Strictly opaque-box: imports ONLY MainActivity, R, domain.model.*, and RestCycleTestTags.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [E2EDeviceConfigs.ROBOLECTRIC_SDK],
    qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A
)
class Tier2BoundaryCornerTest {

    @get:Rule(order = 0)
    val envRule = RestCycleEnvironmentRule()

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    // =========================================================================
    // F1: Thumb-Zone Ergonomic Layout — Tier 2 (5 tests)
    // =========================================================================

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_COMPACT)
    fun f1_t2_01_compactViewportMaintainsNonOverlappingSplitZonesAcrossAllModes() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                val upper = composeRule.onNodeWithTag(RestCycleTestTags.UPPER_RESULTS_ZONE).getUnclippedBoundsInRoot()
                val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
                assertTrue(upper.width > 0.dp && upper.height > 0.dp)
                assertTrue(thumb.width > 0.dp && thumb.height > 0.dp)
                assertTrue(upper.bottom <= thumb.top + 1.dp)
                SleepMode.entries.forEach { tabMode ->
                    val tabBounds = composeRule.onNodeWithTag(RestCycleTestTags.modeTab(tabMode)).getUnclippedBoundsInRoot()
                    assertTrue(tabBounds.right <= root.right + 1.dp)
                }
            }
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A, fontScale = 1.3f)
    fun f1_t2_02_largeAccessibilityFontScale130PreservesBoundsWithoutClipping() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            val header = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).getUnclippedBoundsInRoot()
            listOf(
                RestCycleTestTags.MODE_SWITCHER,
                RestCycleTestTags.TIME_WHEEL,
                RestCycleTestTags.LATENCY_ADJUSTER
            ).forEach { tag ->
                val b = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
                assertTrue(b.width > 0.dp && b.height > 0.dp)
                assertTrue(b.top >= header.bottom - 1.dp)
                assertTrue(b.left >= root.left - 1.dp && b.right <= root.right + 1.dp)
            }
            composeRule.switchMode(SleepMode.POWER_NAP)
            val tooltip = composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).getUnclippedBoundsInRoot()
            assertTrue(tooltip.width > 0.dp && tooltip.height > 0.dp)
            assertTrue(tooltip.right <= root.right + 1.dp)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_TABLET)
    fun f1_t2_03_tabletViewportAnchorsThumbZoneInLowerHalf() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            assertTrue(thumb.top >= root.top + root.height * 0.42f)
            assertTrue(thumb.bottom >= root.bottom - 2.dp)
        }
    }

    @Test
    fun f1_t2_04_12HourAmPmColumnFitsNonOverlappingInsideTimeWheelAndThumbZone() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { _, _ ->
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            val hour = composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).getUnclippedBoundsInRoot()
            val min = composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).getUnclippedBoundsInRoot()
            val amPm = composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).getUnclippedBoundsInRoot()
            assertTrue(hour.right <= min.left + 1.dp)
            assertTrue(min.right <= amPm.left + 1.dp)
            assertTrue(amPm.top >= thumb.top - 1.dp && amPm.bottom <= thumb.bottom + 1.dp)
        }
    }

    @Test
    fun f1_t2_05_scrollingResultsZoneDoesNotDisplaceStationaryThumbZone() {
        withMainActivity(composeRule) { _, _ ->
            val thumbBefore = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            composeRule.findOptionCardByTimeText("05:05").performScrollTo().assertIsDisplayed()
            val thumbAfter = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            assertEquals(thumbBefore.top, thumbAfter.top)
            assertEquals(thumbBefore.bottom, thumbAfter.bottom)
        }
    }

    // =========================================================================
    // F2: Zero-Button Reactive UX — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f2_t2_01_rapidModeSwitchingSettlesCleanlyOnFinalMode() {
        withMainActivity(composeRule) { _, _ ->
            listOf(
                SleepMode.SLEEP_NOW,
                SleepMode.GO_TO_BED_AT,
                SleepMode.POWER_NAP,
                SleepMode.WAKE_UP_AT
            ).forEach { mode ->
                composeRule.onNodeWithTag(RestCycleTestTags.modeTab(mode)).performClick()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).assertDoesNotExist()
            composeRule.findOptionCardByTimeText("21:35").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()
        }
    }

    @Test
    fun f2_t2_02_rapidWheelScrubFollowedByLatencyChangeConvergesWithoutStaleState() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = true, steps = 3)
            composeRule.selectWheelTime(LocalTime.of(7, 0), is24Hour = true)
            composeRule.setLatencyMinutes(20)
            // 07:00 - 7h30m - 20m = 23:10
            composeRule.findOptionCardByTimeText("23:10").assertIsDisplayed()
        }
    }

    @Test
    fun f2_t2_03_selectingNonHighlightedCardsDoesNotSpuriouslyFireAlarmIntent() {
        envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.tapOptionCard("sleep_now_4")
            composeRule.tapOptionCard("sleep_now_6")
            composeRule.tapOptionCard("sleep_now_5")
            Intents.assertNoUnverifiedIntents()
        }
    }

    @Test
    fun f2_t2_04_modifyingNapLatencyDoesNotCorruptWakeUpAtSleepLatency() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(20)
            composeRule.switchMode(SleepMode.WAKE_UP_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("15", substring = true)
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()
        }
    }

    @Test
    fun f2_t2_05_activityRecreationImmediatelyRendersReactiveCalculations() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.setLatencyMinutes(20)
            scenario.recreate()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("20", substring = true)
            composeRule.findOptionCardByTimeText("23:00").assertIsDisplayed()
        }
    }

    // =========================================================================
    // F3: Wake Up At (WAKE_UP_AT) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f3_t2_01_midnight0000WrapsAllSixBedtimesCleanlyToPreviousDay() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.selectWheelTime(LocalTime.of(0, 0), is24Hour = true)
            listOf("14:45", "16:15", "17:45", "19:15", "20:45", "22:15").forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
            assertSystemBarsLight(activity, expectedLightStatus = false)
        }
    }

    @Test
    fun f3_t2_02_maxMinute2359ComputesExactMinuteBedtimes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(23, 59), is24Hour = true)
            listOf("14:44", "16:14", "17:44", "19:14", "20:44", "22:14").forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun f3_t2_03_zeroSleepLatencyProducesExact90MinMultiples() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(0)
            composeRule.assertLatencyValueEquals(0)
            listOf("21:50", "23:20", "00:50", "02:20", "03:50", "05:20").forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun f3_t2_04_max60MinSleepLatencySubtractsFullHour() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(60)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("60", substring = true)
            listOf("20:50", "22:20", "23:50", "01:20", "02:50", "04:20").forEach { t ->
                composeRule.findOptionCardByTimeText(t).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun f3_t2_05_tappingNightBedtimeCardInWakeUpAtKeepsWheelDrivenDaySky() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.selectWheelTime(LocalTime.of(8, 0), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = true)
            // Tap 5-cycle bedtime 00:15 (NIGHT time) -> sky MUST stay DAY (driven by wheel 08:00)
            composeRule.findOptionCardByTimeText("00:15").performClick()
            composeRule.waitForIdle()
            assertSystemBarsLight(activity, expectedLightStatus = true)
        }
    }

    // =========================================================================
    // F4: Sleep Now (SLEEP_NOW) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f4_t2_01_midnightRolloverProducesValidNextMorningTimes() {
        envRule.setClockToLocalTime(LocalTime.of(23, 50))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            // 23:50 + 15m = 00:05 fall asleep -> 1c 01:35, 5c 07:35, 6c 09:05
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_6")).assertTextContains("09:05", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("07:35", substring = true)
            composeRule.findOptionCardById("sleep_now_1").performScrollTo().assertTextContains("01:35", substring = true)
        }
    }

    @Test
    fun f4_t2_02_extremeLatencyBounds0And60InSleepNow() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.setLatencyMinutes(0)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5"))
                .assertTextContains(expectedFormattedTime(anchor.plusMinutes(450), true), substring = true)
            composeRule.setLatencyMinutes(60)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5"))
                .assertTextContains(expectedFormattedTime(anchor.plusMinutes(510), true), substring = true)
        }
    }

    @Test
    fun f4_t2_03_switchingOutAndBackToSleepNowRestoresDefaultSleepNow5() {
        envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.tapOptionCard("sleep_now_4")
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_4")).assertIsSelected()

            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertIsSelected()
        }
    }

    @Test
    fun f4_t2_04_sleepNowTickerAdvancesWakeTimesByOneMinute() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(22, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            val before = expectedFormattedTime(expectedSleepCyclesForward(anchor, 15, 5), true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains(before, substring = true)

            envRule.advanceClockMinutes(composeRule, 1)
            val after = expectedFormattedTime(expectedSleepCyclesForward(anchor.plusMinutes(1), 15, 5), true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains(after, substring = true)
        }
    }

    @Test
    fun f4_t2_05_nightWallClockWithDayWakeCardRendersDaySkyInSleepNow() {
        // 01:00 (NIGHT) + 15m + 450m = 08:45 (DAY)
        envRule.setClockToLocalTime(LocalTime.of(1, 0))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            assertSystemBarsLight(activity, expectedLightStatus = true)
        }
    }

    // =========================================================================
    // F5: Go to Bed At (GO_TO_BED_AT) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f5_t2_01_lateNight2359BedtimeWrapsCleanlyAcrossMidnight() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(23, 59), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_6")).assertTextContains("09:14", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("07:44", substring = true)
            composeRule.findOptionCardById("bed_1").performScrollTo().assertTextContains("01:44", substring = true)
        }
    }

    @Test
    fun f5_t2_02_exactMidnight0000BedtimeComputesAllSixWakeTimes() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(0, 0), is24Hour = true)
            val expected = mapOf(6 to "09:15", 5 to "07:45", 4 to "06:15", 3 to "04:45", 2 to "03:15", 1 to "01:45")
            expected.forEach { (cyc, t) ->
                composeRule.findOptionCardById("bed_$cyc")
                    .performScrollTo()
                    .assertTextContains(t, substring = true)
            }
        }
    }

    @Test
    fun f5_t2_03_latencyBounds0And60At2200CrossDawnBoundary() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.setLatencyMinutes(0)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("05:30", substring = true)
            composeRule.setLatencyMinutes(60)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("06:30", substring = true)
            composeRule.findOptionCardById("bed_4").performScrollTo().assertTextContains("05:00", substring = true)
        }
    }

    @Test
    fun f5_t2_04_scrollingBedtimeWhileNonDefaultCardHighlightedUpdatesSky() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.tapOptionCard("bed_4") // 04:15 (NIGHT)
            assertSystemBarsLight(activity, expectedLightStatus = false)

            // Scroll bedtime to 01:45 -> 01:45 + 15m + 360m = 08:00 (DAY)
            composeRule.selectWheelTime(LocalTime.of(1, 45), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_4")).assertTextContains("08:00", substring = true)
            assertSystemBarsLight(activity, expectedLightStatus = true)
        }
    }

    @Test
    fun f5_t2_05_wakeAlarmButtonHiddenInGoToBedAtMode() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN).assertDoesNotExist()
        }
    }

    // =========================================================================
    // F6: Power Nap (POWER_NAP) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f6_t2_01_powerNapLatencyRangeIsClamped0To30Never60() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            val sliderNode = composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER).fetchSemanticsNode()
            val range = sliderNode.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.range
            assertEquals(0f, range?.start)
            assertEquals(30f, range?.endInclusive)

            composeRule.setLatencyMinutes(0)
            composeRule.tapSetTimerOnCard("nap_10")
            assertTimerIntentSent(durationSeconds = 600)

            composeRule.setLatencyMinutes(30)
            composeRule.tapSetTimerOnCard("nap_90")
            assertTimerIntentSent(durationSeconds = 7200)
        }
    }

    @Test
    fun f6_t2_02_sleepLatency45DoesNotOverwriteNapLatencyDefault10() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(45)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("10", substring = true)
            composeRule.switchMode(SleepMode.WAKE_UP_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("45", substring = true)
        }
    }

    @Test
    fun f6_t2_03_selectingDifferentNapPresetMorphsSkyBetweenDayAndDawnDusk() {
        // 16:40 + 10m + 20m = 17:10 (DAY); 16:40 + 10m + 90m = 18:20 (DAWN_DUSK)
        envRule.setClockToLocalTime(LocalTime.of(16, 40))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            assertSystemBarsLight(activity, expectedLightStatus = true)
            composeRule.tapOptionCard("nap_90")
            assertSystemBarsLight(activity, expectedLightStatus = false)
        }
    }

    @Test
    fun f6_t2_04_lateNightPowerNapCrossesMidnightCleanly() {
        envRule.setClockToLocalTime(LocalTime.of(23, 30))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10")).assertTextContains("23:50", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertTextContains("00:00", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).assertTextContains("01:10", substring = true)
        }
    }

    @Test
    fun f6_t2_05_inertiaTooltipAndTimerButtonsAbsentInAllSleepModes() {
        withMainActivity(composeRule) { _, _ ->
            listOf(SleepMode.WAKE_UP_AT, SleepMode.SLEEP_NOW, SleepMode.GO_TO_BED_AT).forEach { mode ->
                composeRule.switchMode(mode)
                composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).assertDoesNotExist()
                val timerButtons = composeRule.onAllNodes(
                    SemanticsMatcher("TimerBtn") { n ->
                        n.config.getOrNull(SemanticsProperties.TestTag)
                            ?.startsWith(RestCycleTestTags.SET_TIMER_BTN_PREFIX) == true
                    }
                ).fetchSemanticsNodes()
                assertTrue("No timer buttons should exist in $mode", timerButtons.isEmpty())
            }
        }
    }

    // =========================================================================
    // F7: Dynamic Celestial Header (Canvas) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_t2_01_headerTransitionsSmoothlyAcross0529To0530Boundary() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(5, 29), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(700)
            ShadowLooper.idleMainLooper()
            composeRule.waitForIdle()
            val beforeHeaderLum = meanLuminance(composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER))
            val beforeBgLum = relativeLuminance(composeRule.sampleRootBackgroundColorArgb(0))

            composeRule.mainClock.autoAdvance = false
            try {
                // Single 1-minute step (05:29 -> 05:30)
                composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = true, steps = 1)
                composeRule.mainClock.advanceTimeBy(200)
                ShadowLooper.idleMainLooper()
                composeRule.waitForIdle()
                composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).assertIsDisplayed()

                composeRule.mainClock.advanceTimeBy(450)
                ShadowLooper.idleMainLooper()
                composeRule.waitForIdle()
                val header = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
                header.assertIsDisplayed()
                assertTrue(header.getUnclippedBoundsInRoot().height > 40.dp)

                val afterHeaderLum = meanLuminance(composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER))
                val afterBgLum = relativeLuminance(composeRule.sampleRootBackgroundColorArgb(0))
                assertTrue(
                    "Expected visual/luminance shift across 05:29 -> 05:30 (header: $beforeHeaderLum -> $afterHeaderLum, bg: $beforeBgLum -> $afterBgLum)",
                    abs(afterHeaderLum - beforeHeaderLum) > 0.005 || afterBgLum > beforeBgLum + 0.01
                )
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_t2_02_rapidWheelTogglesAcrossAllPhasesSettleWithoutCanvasCrash() {
        withMainActivity(composeRule) { _, _ ->
            listOf(
                LocalTime.of(5, 29),
                LocalTime.of(8, 0),
                LocalTime.of(17, 30),
                LocalTime.of(20, 30)
            ).forEach { t ->
                composeRule.selectWheelTime(t, is24Hour = true)
            }
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            val lum = meanLuminance(composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER))
            assertTrue("Expected settled NIGHT header luminance < 0.15, got $lum", lum < 0.15)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_COMPACT)
    fun f7_t2_03_compactViewportClipsHeaderCleanlyAboveResultsAndInsideRoot() {
        withMainActivity(composeRule) { _, _ ->
            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            val header = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).getUnclippedBoundsInRoot()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            assertTrue(header.left >= root.left - 1.dp && header.right <= root.right + 1.dp)
            assertTrue(header.bottom <= thumb.top + 1.dp)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_t2_04_nightTwinklingStarsAnimationPreservesAmoledBlackBackdrop() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(2000)
            composeRule.waitForIdle()
            val bg = composeRule.sampleRootBackgroundColorArgb(0)
            assertTrue(Color.red(bg) <= 5 && Color.green(bg) <= 5 && Color.blue(bg) <= 5)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f7_t2_05_headerResumesCleanlyAfterLifecycleStopAndStart() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = true)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.waitForIdle()
            val header = composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
            header.assertIsDisplayed()
            assertTrue(meanLuminance(composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER)) > 0.35)
        }
    }

    // =========================================================================
    // F8: Dynamic Color & Gradient Morphing — Tier 2 (5 tests)
    // =========================================================================

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_t2_01_morningBoundaries0529To0530And0759To0800() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(5, 29), is24Hour = true)
            val nightBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("05:29 must be NIGHT (#000000)", Color.red(nightBg) <= 5 && Color.blue(nightBg) <= 5)

            composeRule.selectWheelTime(LocalTime.of(5, 30), is24Hour = true)
            val dawnBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("05:30 must transition out of NIGHT", Color.red(dawnBg) > 5 || Color.green(dawnBg) > 5 || Color.blue(dawnBg) > 5)

            composeRule.selectWheelTime(LocalTime.of(7, 59), is24Hour = true)
            val lateDawnBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("07:59 must still be DAWN_DUSK (lum < 0.5)", relativeLuminance(lateDawnBg) < 0.50)

            composeRule.selectWheelTime(LocalTime.of(8, 0), is24Hour = true)
            val dayBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("08:00 must be DAY (lum > 0.60)", relativeLuminance(dayBg) > 0.60)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_t2_02_eveningBoundaries1729To1730And2029To2030() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(17, 29), is24Hour = true)
            assertTrue("17:29 must be DAY", relativeLuminance(composeRule.sampleRootBackgroundColorArgb(700)) > 0.60)

            composeRule.selectWheelTime(LocalTime.of(17, 30), is24Hour = true)
            assertTrue("17:30 must be DAWN_DUSK", relativeLuminance(composeRule.sampleRootBackgroundColorArgb(700)) < 0.50)

            composeRule.selectWheelTime(LocalTime.of(20, 29), is24Hour = true)
            val duskBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("20:29 must still be DAWN_DUSK (not #000000)", Color.red(duskBg) > 5 || Color.blue(duskBg) > 5)

            composeRule.selectWheelTime(LocalTime.of(20, 30), is24Hour = true)
            val nightBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("20:30 must be pure NIGHT #000000", Color.red(nightBg) <= 5 && Color.green(nightBg) <= 5 && Color.blue(nightBg) <= 5)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_t2_03_midFlightMorphReversalNightToDayBackToNightSettlesPureBlack() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(5, 29), is24Hour = true)
            composeRule.mainClock.advanceTimeBy(700)
            ShadowLooper.idleMainLooper()
            composeRule.waitForIdle()

            composeRule.mainClock.autoAdvance = false
            try {
                // Single 1-minute step into DAWN_DUSK (05:29 -> 05:30), advance ~150ms (~250ms total < 600ms), then reverse 1 step back to NIGHT
                composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = true, steps = 1)
                composeRule.mainClock.advanceTimeBy(150)
                ShadowLooper.idleMainLooper()
                composeRule.waitForIdle()
                composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = false, steps = 1)
                val finalBg = composeRule.sampleRootBackgroundColorArgb(700)
                assertTrue(
                    "Expected pure AMOLED black after reversal back to 05:29, got rgb=${Color.red(finalBg)},${Color.green(finalBg)},${Color.blue(finalBg)}",
                    Color.red(finalBg) <= 5 && Color.green(finalBg) <= 5 && Color.blue(finalBg) <= 5
                )
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_t2_04_nightCardObsidianSurfaceRemainsDistinctFromAmoledBackground() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            val bg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue(Color.red(bg) <= 5 && Color.green(bg) <= 5 && Color.blue(bg) <= 5)
            val cardBitmap = composeRule.captureTagBitmap(
                composeRule.findAllCardsInResultsZone().first().config.getOrNull(SemanticsProperties.TestTag)!!
            )
            assertTrue("Card mean luminance should be > background in NIGHT", meanLuminance(cardBitmap) > 0.02)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun f8_t2_05_switchingModeFromDayCardToWakeUpAtMorphsBackToDawnDusk() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            composeRule.tapOptionCard("bed_6") // 08:15 DAY
            assertTrue(relativeLuminance(composeRule.sampleRootBackgroundColorArgb(700)) > 0.60)

            composeRule.switchMode(SleepMode.WAKE_UP_AT) // default 06:50 DAWN_DUSK
            val dawnBg = composeRule.sampleRootBackgroundColorArgb(700)
            assertTrue("Must morph back to DAWN_DUSK", relativeLuminance(dawnBg) < 0.50)
        }
    }

    // =========================================================================
    // F9: Edge-to-Edge & System Bar Tinting — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f9_t2_01_systemBarsFlipAtExact0800And1730DayBoundaries() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.selectWheelTime(LocalTime.of(7, 59), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
            composeRule.selectWheelTime(LocalTime.of(8, 0), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
            composeRule.selectWheelTime(LocalTime.of(17, 29), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
            composeRule.selectWheelTime(LocalTime.of(17, 30), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
        }
    }

    @Test
    fun f9_t2_02_nightAndDawnBoundariesNeverFlashLightSystemBars() {
        withMainActivity(composeRule) { _, activity ->
            listOf(
                LocalTime.of(5, 29),
                LocalTime.of(5, 30),
                LocalTime.of(20, 29),
                LocalTime.of(20, 30)
            ).forEach { t ->
                composeRule.selectWheelTime(t, is24Hour = true)
                assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
            }
        }
    }

    @Test
    fun f9_t2_03_systemBarLightFlagsSurviveStopAndResumeInDayPhase() {
        withMainActivity(composeRule) { scenario, activity ->
            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = true)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.waitForIdle()
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
        }
    }

    @Test
    fun f9_t2_04_windowInsetsPaddingKeepsModeSwitcherAboveNavigationBar() {
        withMainActivity(composeRule) { _, activity ->
            val insets = WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.statusBars(),
                    androidx.core.graphics.Insets.of(0, 48, 0, 0)
                )
                .setInsets(
                    WindowInsetsCompat.Type.navigationBars(),
                    androidx.core.graphics.Insets.of(0, 0, 0, 64)
                )
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            composeRule.waitForIdle()

            val root = composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
            val switcher = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER).getUnclippedBoundsInRoot()
            assertEquals(0.dp, root.top)
            assertTrue(switcher.bottom <= root.bottom)
        }
    }

    @Test
    fun f9_t2_05_systemBarsSyncAcrossModeSwitchBetweenDawnWakeAndDayPowerNap() {
        envRule.setClockToLocalTime(LocalTime.of(13, 30))
        withMainActivity(composeRule) { _, activity ->
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
            composeRule.switchMode(SleepMode.POWER_NAP)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
            composeRule.switchMode(SleepMode.WAKE_UP_AT)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
        }
    }

    // =========================================================================
    // F10: 4-Mode Bottom Segmented Switcher — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f10_t2_01_retappingAlreadySelectedTabIsIdempotentAndPreservesWheelTime() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(7, 10), is24Hour = true)
            composeRule.switchMode(SleepMode.WAKE_UP_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertIsSelected()
            assertEquals(7, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            assertEquals(10, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE))
        }
    }

    @Test
    fun f10_t2_02_rapidSequentialSwitchingSettlesOnPowerNapWithThreePresetsOnly() {
        envRule.setClockToLocalTime(LocalTime.of(14, 0))
        withMainActivity(composeRule) { _, _ ->
            listOf(
                SleepMode.POWER_NAP,
                SleepMode.SLEEP_NOW,
                SleepMode.GO_TO_BED_AT,
                SleepMode.WAKE_UP_AT,
                SleepMode.POWER_NAP
            ).forEach { composeRule.onNodeWithTag(RestCycleTestTags.modeTab(it)).performClick() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertIsSelected()
            assertEquals(3, composeRule.findAllCardsInResultsZone().size)
        }
    }

    @Test
    fun f10_t2_03_modeSwitchPreservesDistinctMax60AndMin0Latencies() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(60)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(0)
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("60", substring = true)
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("60", substring = true)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(0)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_COMPACT)
    fun f10_t2_04_modeTabsNonOverlappingAndClickableOnCompactViewport() {
        withMainActivity(composeRule) { _, _ ->
            val bounds = SleepMode.entries.map { mode ->
                val tab = composeRule.onNodeWithTag(RestCycleTestTags.modeTab(mode)).assertIsDisplayed()
                tab.getUnclippedBoundsInRoot()
            }
            bounds.forEach { b ->
                assertTrue(b.width > 0.dp && b.height >= 40.dp)
            }
            for (i in 0 until bounds.size - 1) {
                assertTrue("Tabs $i and ${i + 1} must not overlap", bounds[i].right <= bounds[i + 1].left + 1.dp)
            }
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertIsSelected()
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A, fontScale = 1.3f)
    fun f10_t2_05_modeTabsRemainDisplayedWithinSwitcherAtFontScale130() {
        withMainActivity(composeRule) { _, _ ->
            val switcher = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER).getUnclippedBoundsInRoot()
            SleepMode.entries.forEach { mode ->
                val tab = composeRule.onNodeWithTag(RestCycleTestTags.modeTab(mode)).assertIsDisplayed()
                val b = tab.getUnclippedBoundsInRoot()
                assertTrue(b.left >= switcher.left - 1.dp && b.right <= switcher.right + 1.dp)
            }
        }
    }

    // =========================================================================
    // F11: Haptic Time Wheel Picker — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f11_t2_01_hourWheelWrapsCleanly23To00And00To23() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(23, 0), is24Hour = true)
            assertEquals(23, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_HOUR, forward = true, steps = 1)
            assertEquals(0, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.findOptionCardByTimeText("16:15").assertIsDisplayed()

            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_HOUR, forward = false, steps = 1)
            assertEquals(23, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.findOptionCardByTimeText("15:15").assertIsDisplayed()
        }
    }

    @Test
    fun f11_t2_02_minuteWheelWrapsCleanly59To00And00To59() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(6, 59), is24Hour = true)
            assertEquals(59, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE))
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = true, steps = 1)
            assertEquals(0, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE))
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_MINUTE, forward = false, steps = 1)
            assertEquals(59, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE))
        }
    }

    @Test
    fun f11_t2_03_12HourNoonVsMidnightDisplays12Never00AndSendsCorrect24hExtra() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(0, 0), is24Hour = false)
            assertEquals(12, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 0, minute = 0)

            composeRule.selectWheelTime(LocalTime.of(12, 0), is24Hour = false)
            assertEquals(12, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 12, minute = 0)
        }
    }

    @Test
    fun f11_t2_04_rapidFlingSnapsToValidIntegerMinute() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_MINUTE).performTouchInput {
                swipeUp(startY = bottom - 5f, endY = top + 5f, durationMillis = 50)
            }
            composeRule.mainClock.advanceTimeBy(1000)
            ShadowLooper.idleMainLooper()
            composeRule.waitForIdle()
            val settled = composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE)
            assertTrue(
                "Minute must snap to 0..59 and move away from default 50, was $settled",
                settled != null && settled in 0..59 && settled != 50
            )
        }
    }

    @Test
    fun f11_t2_05_wheelHiddenInSleepNowAndPowerNapThenRestoredInGoToBedAt() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertDoesNotExist()
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertDoesNotExist()
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).assertIsDisplayed()
        }
    }

    // =========================================================================
    // F12: Fall-Asleep Latency Adjuster — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f12_t2_01_sleepLatencyLowerBoundZeroNoUnderflow() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(0)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER).performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
            composeRule.assertLatencyValueEquals(0)
            composeRule.findOptionCardByTimeText("23:20").assertIsDisplayed()
        }
    }

    @Test
    fun f12_t2_02_sleepLatencyUpperBound60NoOverflow() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(60)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER).performTouchInput { swipeRight() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("60", substring = true)
            composeRule.findOptionCardByTimeText("22:20").assertIsDisplayed()
        }
    }

    @Test
    fun f12_t2_03_napLatencyLowerBoundZeroYields600SecondTimer() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(0)
            composeRule.assertLatencyValueEquals(0)
            composeRule.tapSetTimerOnCard("nap_10")
            assertTimerIntentSent(durationSeconds = 600)
        }
    }

    @Test
    fun f12_t2_04_napLatencyUpperBound30Yields7200SecondTimer() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(30)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("30", substring = true)
            composeRule.tapSetTimerOnCard("nap_90")
            assertTimerIntentSent(durationSeconds = 7200)
        }
    }

    @Test
    fun f12_t2_05_rapidLatencyScrubbingConvergesAndPersistsFinalValue() {
        var scenario = launchMainActivity(composeRule)
        try {
            listOf(0, 60, 15, 45).forEach { composeRule.setLatencyMinutes(it) }
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(45)
        } finally {
            scenario.close()
        }
    }

    // =========================================================================
    // F13: One-Tap Native Alarm (ACTION_SET_ALARM) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f13_t2_01_12HourPmTimeProduces24HourExtraHour() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(14, 15), is24Hour = false)
            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 14, minute = 15)
        }
    }

    @Test
    fun f13_t2_02_midnight0005ProducesExtraHourZeroNot24() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(0, 5), is24Hour = true)
            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 0, minute = 5)
        }
    }

    @Test
    @MissingClockApp
    fun f13_t2_03_missingClockAppActivityNotFoundHandledGracefullyOnAlarm() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.tapWakeAlarmButton()
            assertMissingClockFallbackShown(composeRule, activity)
        }
    }

    @Test
    fun f13_t2_04_setAlarmOnNonHighlighted3CycleCardUsesExactCardTime() {
        withMainActivity(composeRule) { _, _ ->
            // In WAKE_UP_AT (6..1 order): index 3 = 3-cycle card (02:05)
            composeRule.findSetAlarmButtonMatchingTimeOrIndex(3).performScrollTo().performClick()
            composeRule.waitForIdle()
            assertAlarmIntentSent(hour24 = 2, minute = 5)
        }
    }

    @Test
    fun f13_t2_05_doubleTapWakeAlarmDoesNotCorruptExtrasOrCrash() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN).performClick()
            composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN).performClick()
            composeRule.waitForIdle()
            val recorded = Intents.getIntents().filter { it.action == AlarmClock.ACTION_SET_ALARM }
            assertEquals(2, recorded.size)
            recorded.forEach { intent ->
                assertEquals(6, intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
                assertEquals(50, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
                assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
            }
        }
    }

    // =========================================================================
    // F14: Power Nap Timer (ACTION_SET_TIMER) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f14_t2_01_zeroNapLatency10mNapProducesExact600Seconds() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(0)
            composeRule.tapSetTimerOnCard("nap_10")
            assertTimerIntentSent(durationSeconds = 600)
        }
    }

    @Test
    fun f14_t2_02_maxNapLatency30m90mNapProducesExact7200Seconds() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(30)
            composeRule.tapSetTimerOnCard("nap_90")
            assertTimerIntentSent(durationSeconds = 7200)
        }
    }

    @Test
    @MissingClockApp
    fun f14_t2_03_missingTimerAppActivityNotFoundHandledGracefully() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            assertMissingClockFallbackShown(composeRule, activity)
        }
    }

    @Test
    fun f14_t2_04_timerButtonsAbsentOutsidePowerNapMode() {
        withMainActivity(composeRule) { _, _ ->
            listOf(SleepMode.WAKE_UP_AT, SleepMode.SLEEP_NOW, SleepMode.GO_TO_BED_AT).forEach { mode ->
                composeRule.switchMode(mode)
                val timers = composeRule.onAllNodes(
                    SemanticsMatcher("TimerBtn") { n ->
                        n.config.getOrNull(SemanticsProperties.TestTag)
                            ?.startsWith(RestCycleTestTags.SET_TIMER_BTN_PREFIX) == true
                    }
                ).fetchSemanticsNodes()
                assertTrue(timers.isEmpty())
            }
        }
    }

    @Test
    fun f14_t2_05_powerNapTimerAndAlarmButtonsDoNotCrossFireActions() {
        val anchor = envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 1800)

            val expected20 = anchor.plusMinutes(30)
            composeRule.tapSetAlarmOnCard("nap_20")
            assertAlarmIntentSent(hour24 = expected20.hour, minute = expected20.minute)
            Intents.assertNoUnverifiedIntents()
        }
    }

    // =========================================================================
    // F15: 12h/24h Auto-Detection & tnum — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f15_t2_01_midnight0015Formats1215AmIn12hAnd0015In24h() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(15, 0), is24Hour = true)
            // 15:00 + 15m + 540m (6c) = 00:15
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_6")).assertTextContains("00:15", substring = true)

            flip24HourFormatAndResume(scenario, composeRule, is24Hour = false)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_6")).assertTextContains("12:15 AM", substring = true)
        }
    }

    @Test
    fun f15_t2_02_onResumeFormatSwitchPreservesModeTimeAndLatency() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.setLatencyMinutes(25)
            flip24HourFormatAndResume(scenario, composeRule, is24Hour = false)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).assertIsSelected()
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("25", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_EN_US)
    fun f15_t2_03_nullTime1224SettingFallsBackToLocaleDefaultWithoutCrash() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, null)
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
        }
    }

    @Test
    fun f15_t2_04_tnumVerifiedAcrossAllFourModesIncludingNapCountdowns() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, _ ->
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                // Check OPTION_CARD_* inside UPPER_RESULTS_ZONE (excluding prose INERTIA_TOOLTIP) + LATENCY_VALUE_TEXT + TIME_WHEEL
                composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
                composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.LATENCY_VALUE_TEXT)
                if (mode == SleepMode.WAKE_UP_AT || mode == SleepMode.GO_TO_BED_AT) {
                    composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
                    composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_MINUTE)
                }
            }
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU, fontScale = 1.3f)
    fun f15_t2_05_tnumHoldsUnderHungarianLocaleAndFontScale130() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_MINUTE)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
        }
    }

    // =========================================================================
    // F16: Tactile CLOCK_TICK Haptics — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f16_t2_01_retappingAlreadySelectedModeTabDoesNotEmitSwitchTick() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).performClick()
            composeRule.waitForIdle()
            assertTrue(
                "Retapping already-selected tab should not emit haptic, got ${anyHapticFeedbackPerformed(activity)}",
                anyHapticFeedbackPerformed(activity).isEmpty()
            )
        }
    }

    @Test
    fun f16_t2_02_hapticsDisabledInSystemSettingsDoesNotCrashOnInteractions() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        withMainActivity(composeRule) { _, _ ->
            composeRule.stepWheelComponent(RestCycleTestTags.WHEEL_HOUR, forward = true, steps = 1)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapOptionCard("nap_90")
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).assertIsSelected()
        }
    }

    @Test
    fun f16_t2_03_rapidMultiStepWheelScrollEmitsOnlyClockTickConstant() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_HOUR).performTouchInput {
                swipeUp(durationMillis = 80)
            }
            composeRule.waitForIdle()
            val haptics = anyHapticFeedbackPerformed(activity)
            assertTrue(haptics.isNotEmpty())
            assertTrue(haptics.all { it == HapticFeedbackConstants.CLOCK_TICK })
        }
    }

    @Test
    fun f16_t2_04_tappingPowerNapCardEmitsClockTickAndSelectsCard() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapOptionCard("nap_90")
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).assertIsSelected()
            assertClockTickHapticRecorded(activity)
        }
    }

    @Test
    fun f16_t2_05_hapticConstantIsSpecificallyClockTickNotLongPressOrContextClick() {
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            val haptics = anyHapticFeedbackPerformed(activity)
            assertTrue(haptics.contains(HapticFeedbackConstants.CLOCK_TICK))
            assertFalse(haptics.contains(HapticFeedbackConstants.LONG_PRESS))
            assertFalse(haptics.contains(HapticFeedbackConstants.CONTEXT_CLICK))
        }
    }

    // =========================================================================
    // F17: Bilingual UI (EN & HU) — Tier 2 (5 tests)
    // =========================================================================

    @Test
    fun f17_t2_01_allFormatPlaceholdersMatchExactlyBetweenEnAndHu() {
        val mainDir = resolveAppMainDir()
        val enMap = parseStringsXmlMap(File(mainDir, "res/values/strings.xml"))
        val huMap = parseStringsXmlMap(File(mainDir, "res/values-hu/strings.xml"))
        val placeholderRegex = Regex("%(\\d+\\$)?[sdfox]")
        enMap.forEach { (key, enVal) ->
            val huVal = huMap[key] ?: error("Missing HU key: $key")
            val enPlaceholders = placeholderRegex.findAll(enVal).map { it.value }.toList()
            val huPlaceholders = placeholderRegex.findAll(huVal).map { it.value }.toList()
            assertEquals("Format placeholders mismatch for key '$key'", enPlaceholders, huPlaceholders)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun f17_t2_02_hungarianDiacriticsRenderWithoutReplacementGlyphs() {
        withMainActivity(composeRule) { _, _ ->
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                val allTextNodes = composeRule.onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
                    useUnmergedTree = true
                ).fetchSemanticsNodes()
                allTextNodes.forEach { node ->
                    val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()
                    assertFalse("Replacement glyph U+FFFD found in '$text'", text.contains('\uFFFD'))
                }
            }
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_COMPACT_HU, fontScale = 1.3f)
    fun f17_t2_03_hungarianLabelsDoNotClipAtCompactViewportAnd130FontScale() {
        withMainActivity(composeRule) { _, _ ->
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                val tabBounds = composeRule.onNodeWithTag(RestCycleTestTags.modeTab(mode)).getUnclippedBoundsInRoot()
                assertTrue(tabBounds.width > 0.dp && tabBounds.height > 0.dp)
                val latencyBounds = composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).getUnclippedBoundsInRoot()
                assertTrue(latencyBounds.width > 0.dp && latencyBounds.height > 0.dp)
                if (mode == SleepMode.POWER_NAP) {
                    val tooltipBounds = composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).getUnclippedBoundsInRoot()
                    assertTrue(tooltipBounds.width > 0.dp && tooltipBounds.height > 0.dp)
                }
            }
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun f17_t2_04_hungarianLocaleWith12HourOverrideFormatsTimesWithoutCrash() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertTextContains("Ébredés ekkor", substring = true)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun f17_t2_05_noHardcodedEnglishModeNamesLeakInHungarianUi() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT)
                .assertIsDisplayed()
                .assertTextContains("Ébredés ekkor", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW)
                .assertIsDisplayed()
                .assertTextContains("Most fekszem le", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT)
                .assertIsDisplayed()
                .assertTextContains("Lefekvés ekkor", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP)
                .assertIsDisplayed()
                .assertTextContains("Szieszta", substring = true)
            listOf("Wake Up At", "Sleep Now", "Go to Bed At", "Power Nap").forEach { enLabel ->
                composeRule.onNodeWithText(enLabel).assertDoesNotExist()
            }
        }
    }

    // =========================================================================
    // F18: 100% Offline DataStore Persistence & Manifest — Tier 2 (6 tests)
    // =========================================================================

    @Test
    fun f18_t2_01_freshInstallWithNoDataStoreFileUsesDefaults15And10() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        File(app.filesDir, "datastore").deleteRecursively()
        withMainActivity(composeRule) { _, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("15", substring = true)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("10", substring = true)
        }
    }

    @Test
    fun f18_t2_02_zeroLatencyBoundaryPersistsAsZeroNotResetToDefault() {
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.setLatencyMinutes(0)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(0)
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(0)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(0)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun f18_t2_03_maxLatencyBoundaries60And30PersistAcrossRelaunch() {
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.setLatencyMinutes(60)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(30)
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(60)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(30)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun f18_t2_04_multipleSequentialOverwritesPersistOnlyFinalValue() {
        var scenario = launchMainActivity(composeRule)
        try {
            listOf(15, 45, 12, 52).forEach { composeRule.setLatencyMinutes(it) }
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(52)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun f18_t2_05_manifestContainsNoIntrusiveOrNetworkPermissions() {
        val doc = parseAndroidManifestXml()
        val permNodes = doc.getElementsByTagName("uses-permission")
        val perms = (0 until permNodes.length).map { (permNodes.item(it) as Element).getAttribute("android:name") }
        listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WAKE_LOCK"
        ).forEach { forbidden ->
            assertFalse("Forbidden permission in manifest: $forbidden", perms.contains(forbidden))
        }
    }

    @Test
    fun f18_t2_06_manifestExportsMainActivityWithLauncherIntentFilter() {
        val doc = parseAndroidManifestXml()
        val activities = doc.getElementsByTagName("activity")
        assertTrue(activities.length > 0)
        val mainActivityEl = (0 until activities.length)
            .map { activities.item(it) as Element }
            .firstOrNull {
                val name = it.getAttribute("android:name")
                name == ".MainActivity" || name == "com.restcycle.app.MainActivity"
            }
        assertTrue("MainActivity not declared in AndroidManifest.xml", mainActivityEl != null)
        assertEquals("true", mainActivityEl!!.getAttribute("android:exported"))

        val actions = mainActivityEl.getElementsByTagName("action")
        val categories = mainActivityEl.getElementsByTagName("category")
        val actionNames = (0 until actions.length).map { (actions.item(it) as Element).getAttribute("android:name") }
        val categoryNames = (0 until categories.length).map { (categories.item(it) as Element).getAttribute("android:name") }
        assertTrue(actionNames.contains("android.intent.action.MAIN"))
        assertTrue(categoryNames.contains("android.intent.category.LAUNCHER"))
    }
}
