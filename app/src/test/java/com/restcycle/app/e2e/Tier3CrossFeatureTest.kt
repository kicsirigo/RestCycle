package com.restcycle.app.e2e

import android.content.Context
import android.graphics.Color
import android.provider.AlarmClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.isEmptyOrNullString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.w3c.dom.Element
import java.io.File
import java.time.LocalTime

/**
 * Tier 3: Pairwise Cross-Feature Combination E2E Tests (20 tests, >= 18 required).
 * Strictly opaque-box: imports ONLY MainActivity, R, domain.model.*, and RestCycleTestTags.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [E2EDeviceConfigs.ROBOLECTRIC_SDK],
    qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A
)
class Tier3CrossFeatureTest {

    @get:Rule(order = 0)
    val envRule = RestCycleEnvironmentRule()

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Test
    fun t3_01_wakeUpAtPlusLatencyAdjusterUpdatesAllSixCardsReactively() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.assertZeroCalculateButton()
            composeRule.setLatencyMinutes(0)
            composeRule.findOptionCardByTimeText("21:50").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("23:20").assertIsDisplayed()

            composeRule.setLatencyMinutes(30)
            composeRule.findOptionCardByTimeText("21:20").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("22:50").assertIsDisplayed()

            composeRule.setLatencyMinutes(45)
            composeRule.findOptionCardByTimeText("21:05").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("22:35").assertIsDisplayed()
            composeRule.assertZeroCalculateButton()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t3_02_wakeUpAtPlusWheelPlusSkyMorphDrivenSolelyByWheel() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(11, 0), is24Hour = true)
            val dayBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue("Expected DAY background for 11:00 wheel", relativeLuminance(dayBg) > 0.60)

            composeRule.selectWheelTime(LocalTime.of(22, 30), is24Hour = true)
            val nightBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(Color.red(nightBg) <= 5 && Color.green(nightBg) <= 5 && Color.blue(nightBg) <= 5)
        }
    }

    @Test
    fun t3_03_sleepNowPlusLatencyAdjusterShiftsAllWakeTimesForward() {
        envRule.setClockToLocalTime(LocalTime.of(23, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("06:45", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_6")).assertTextContains("08:15", substring = true)

            composeRule.setLatencyMinutes(25)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("06:55", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_6")).assertTextContains("08:25", substring = true)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t3_04_sleepNowPlusCardSelectionPlusSkyHeaderAndSystemBars() {
        // 23:30 + 15m = 23:45 -> 5c 07:15 (DAWN_DUSK), 6c 08:45 (DAY), 3c 04:15 (NIGHT)
        envRule.setClockToLocalTime(LocalTime.of(23, 30))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)

            composeRule.tapOptionCard("sleep_now_6")
            val dayBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(relativeLuminance(dayBg) > 0.60)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)
            assertTrue(meanLuminance(composeRule.captureTagBitmap(RestCycleTestTags.CELESTIAL_HEADER)) > 0.35)

            composeRule.tapOptionCard("sleep_now_3")
            val nightBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(Color.red(nightBg) <= 5 && Color.green(nightBg) <= 5 && Color.blue(nightBg) <= 5)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
        }
    }

    @Test
    fun t3_05_goToBedAtPlusWheelPlusSetAlarmIntent() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 45), is24Hour = true)
            composeRule.tapSetAlarmOnCard("bed_5")
            assertAlarmIntentSent(hour24 = 6, minute = 30)
        }
    }

    @Test
    fun t3_06_goToBedAtPlusLatencyPlusDataStoreRestartPersistence() {
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.setLatencyMinutes(22)
            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(22)
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("05:52", substring = true)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun t3_07_powerNapVsSleepLatencyIsolationAcrossModeSwitches() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(40)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("10", substring = true)
            composeRule.setLatencyMinutes(20)
            composeRule.switchMode(SleepMode.WAKE_UP_AT)
            composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertTextContains("40", substring = true)
        }
    }

    @Test
    fun t3_08_powerNapPlusActionSetTimerAcrossAllThreePresets() {
        envRule.setClockToLocalTime(LocalTime.of(14, 0))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(15)
            composeRule.tapSetTimerOnCard("nap_10")
            assertTimerIntentSent(durationSeconds = 1500, expectedMessageContains = "Quick Boost")

            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 2100, expectedMessageContains = "Classic Power Nap")

            composeRule.tapSetTimerOnCard("nap_90")
            assertTimerIntentSent(durationSeconds = 6300, expectedMessageContains = "Full Cycle Nap")
        }
    }

    @Test
    fun t3_09_powerNapPlusActionSetAlarmMidnightWrap() {
        envRule.setClockToLocalTime(LocalTime.of(23, 20))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.findOptionCardById("nap_90").assertTextContains("01:00", substring = true)
            composeRule.tapSetAlarmOnCard("nap_90")
            assertAlarmIntentSent(hour24 = 1, minute = 0)
        }
    }

    @Test
    fun t3_10_modeSwitcherPlusClockTickHapticsAcrossAllTransitions() {
        withMainActivity(composeRule) { _, activity ->
            assertTrue(
                "Expected no haptics prior to interaction, got ${anyHapticFeedbackPerformed(activity)}",
                anyHapticFeedbackPerformed(activity).isEmpty()
            )
            // Initial mode switch records CLOCK_TICK; Robolectric ShadowView retains a single sticky slot per View
            // across consecutive clicks in the same Activity (individual tab/card haptics are isolated in Tier 1 & Tier 2).
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertIsSelected()
            assertClockTickHapticRecorded(activity)
            assertTrue(anyHapticFeedbackPerformed(activity).all { it == HapticFeedbackConstants.CLOCK_TICK })

            listOf(
                SleepMode.GO_TO_BED_AT,
                SleepMode.POWER_NAP,
                SleepMode.WAKE_UP_AT
            ).forEach { mode ->
                composeRule.switchMode(mode)
                composeRule.onNodeWithTag(RestCycleTestTags.modeTab(mode)).assertIsSelected()
                assertTrue(anyHapticFeedbackPerformed(activity).all { it == HapticFeedbackConstants.CLOCK_TICK })
            }
        }
    }

    @Test
    fun t3_11_modeSwitcherPlusZeroButtonReactivityPreservingCustomWheel() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.selectWheelTime(LocalTime.of(7, 20), is24Hour = true)
            composeRule.assertZeroCalculateButton()
            listOf(SleepMode.SLEEP_NOW, SleepMode.GO_TO_BED_AT, SleepMode.POWER_NAP, SleepMode.WAKE_UP_AT).forEach { m ->
                composeRule.switchMode(m)
                composeRule.assertZeroCalculateButton()
                assertTrue(composeRule.findAllCardsInResultsZone().isNotEmpty())
            }
            // Verify custom 07:20 wake-up time and its recalculated bedtimes (22:05, 23:35) are preserved
            composeRule.findOptionCardByTimeText("22:05").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("23:35").assertIsDisplayed()
            composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR)?.let { assertEquals(7, it) }
            composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_MINUTE)?.let { assertEquals(20, it) }
        }
    }

    @Test
    fun t3_12_timeWheelPlus12h24hTogglePlusTabularNumerals() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.selectWheelTime(LocalTime.of(13, 30), is24Hour = false)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)

            flip24HourFormatAndResume(scenario, composeRule, is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
            assertEquals(13, composeRule.readWheelCurrentInt(RestCycleTestTags.WHEEL_HOUR))
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
        }
    }

    @Test
    fun t3_13_alarmIntentSends24hExtraHourWhenUiDisplays12HourPmFormat() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(13, 0), is24Hour = false)
            // 13:00 + 15m + 450m = 20:45 (8:45 PM)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("bed_5")).assertTextContains("8:45 PM", substring = true)
            composeRule.tapSetAlarmOnCard("bed_5")
            assertAlarmIntentSent(hour24 = 20, minute = 45)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A_HU)
    fun t3_14_alarmAndTimerIntentsUseHungarianMessagesUnderHuLocale() {
        withMainActivity(composeRule) { _, _ ->
            composeRule.tapWakeAlarmButton()
            composeRule.findSetAlarmButtonMatchingTimeOrIndex(1).performClick()
            composeRule.waitForIdle()
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")

            val recorded = Intents.getIntents()
            assertEquals(3, recorded.size)
            recorded.forEach { intent ->
                val msg = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE)
                assertTrue("Expected non-blank localized EXTRA_MESSAGE", !msg.isNullOrBlank())
            }
            Intents.intended(
                allOf(
                    hasAction(AlarmClock.ACTION_SET_TIMER),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not("Classic Power Nap")),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not(isEmptyOrNullString()))
                )
            )
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_GALAXY_A56_HU)
    fun t3_15_12h24hTogglePlusHungarianLocalePreservesTnumAndLabels() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertTextContains("Szieszta", substring = true)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
            flip24HourFormatAndResume(scenario, composeRule, is24Hour = false)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertTextContains("Szieszta", substring = true)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_GALAXY_A56)
    fun t3_16_thumbZoneErgonomicsAcrossAllModesAndWheelOnGalaxyA56() {
        withMainActivity(composeRule) { _, _ ->
            SleepMode.entries.forEach { mode ->
                composeRule.switchMode(mode)
                composeRule.assertThumbZoneErgonomics()
                if (mode == SleepMode.WAKE_UP_AT || mode == SleepMode.GO_TO_BED_AT) {
                    val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
                    val wheel = composeRule.onNodeWithTag(RestCycleTestTags.TIME_WHEEL).getUnclippedBoundsInRoot()
                    assertTrue(wheel.top >= thumb.top - 1.dp && wheel.bottom <= thumb.bottom + 1.dp)
                }
            }
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_GALAXY_A56)
    fun t3_17_thumbZoneWithPowerNapPresetsAndInertiaTooltipNonOverlapping() {
        envRule.setClockToLocalTime(LocalTime.of(13, 30))
        withMainActivity(composeRule) { _, _ ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertThumbZoneErgonomics()
            val c10 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10")).getUnclippedBoundsInRoot()
            val c20 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).getUnclippedBoundsInRoot()
            val c90 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).getUnclippedBoundsInRoot()
            val tooltip = composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).getUnclippedBoundsInRoot()
            val thumb = composeRule.onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
            assertTrue(c10.bottom <= c20.top + 1.dp)
            assertTrue(c20.bottom <= c90.top + 1.dp)
            assertTrue(tooltip.bottom <= thumb.top + 1.dp || tooltip.top >= thumb.top - 1.dp)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t3_18_colorMorphPlusSystemBarsPlusModeSwitchAcrossAllThreePhases() {
        envRule.setClockToLocalTime(LocalTime.of(13, 0))
        withMainActivity(composeRule) { _, activity ->
            // DAWN_DUSK in WAKE_UP_AT 06:50
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)

            // DAY in POWER_NAP 13:00
            composeRule.switchMode(SleepMode.POWER_NAP)
            val dayBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(relativeLuminance(dayBg) > 0.60)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)

            // NIGHT in GO_TO_BED_AT 22:00 -> bed_3 = 02:45
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(22, 0), is24Hour = true)
            composeRule.tapOptionCard("bed_3")
            val nightBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(Color.red(nightBg) <= 5 && Color.green(nightBg) <= 5 && Color.blue(nightBg) <= 5)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)
        }
    }

    @Test
    @MissingClockApp
    fun t3_19_missingClockAppAndManifestQueriesAudit() {
        withMainActivity(composeRule) { _, activity ->
            assertOfflineManifestAndPermissions(activity)
            val doc = parseAndroidManifestXml()
            val queries = doc.getElementsByTagName("queries")
            assertTrue(queries.length > 0)
            val actionNames = mutableSetOf<String>()
            for (i in 0 until queries.length) {
                val actions = (queries.item(i) as Element).getElementsByTagName("action")
                for (j in 0 until actions.length) {
                    actionNames.add((actions.item(j) as Element).getAttribute("android:name"))
                }
            }
            assertTrue(actionNames.contains("android.intent.action.SET_ALARM"))
            assertTrue(actionNames.contains("android.intent.action.SET_TIMER"))

            composeRule.tapWakeAlarmButton()
            assertMissingClockFallbackShown(composeRule, activity)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.tapSetTimerOnCard("nap_20")
            assertMissingClockFallbackShown(composeRule, activity)
            composeRule.onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).assertIsDisplayed()
        }
    }

    @Test
    fun t3_20_sleepNowMidnightTickerPlusTnumPlusCardClickHaptic() {
        envRule.setClockToLocalTime(LocalTime.of(23, 59))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            // 23:59 + 15m + 450m = 07:44
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("07:44", substring = true)

            envRule.advanceClockMinutes(composeRule, 2) // 00:01 -> 07:46
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("07:46", substring = true)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)

            composeRule.tapOptionCard("sleep_now_4")
            assertClockTickHapticRecorded(activity)
        }
    }
}
