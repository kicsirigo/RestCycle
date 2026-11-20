package com.restcycle.app.e2e

import android.graphics.Color
import android.provider.AlarmClock
import android.view.HapticFeedbackConstants
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * Tier 4: Real-World End-User Scenario E2E Tests (10 scenarios, >= 9 required).
 * Strictly opaque-box: imports ONLY MainActivity, R, domain.model.*, and RestCycleTestTags.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [E2EDeviceConfigs.ROBOLECTRIC_SDK],
    qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A
)
class Tier4RealWorldScenariosTest {

    @get:Rule(order = 0)
    val envRule = RestCycleEnvironmentRule()

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t4_01_nothingPhone2aNightShiftBedtimePlanning() {
        envRule.setClockToLocalTime(LocalTime.of(19, 0))
        withMainActivity(composeRule) { _, activity ->
            composeRule.assertThumbZoneErgonomics()
            // Default 06:50 DAWN_DUSK -> 21:35 (6c), 23:05 (5c), 00:35 (4c)
            composeRule.findOptionCardByTimeText("21:35").assertIsDisplayed()
            composeRule.findOptionCardByTimeText("23:05").assertIsDisplayed()
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)

            // Dial wheel to 05:15 (NIGHT) -> 5-cycle bedtime = 05:15 - 7h30m - 15m = 21:30
            composeRule.selectWheelTime(LocalTime.of(5, 15), is24Hour = true)
            assertClockTickHapticRecorded(activity)
            val nightBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(Color.red(nightBg) <= 5 && Color.green(nightBg) <= 5 && Color.blue(nightBg) <= 5)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)

            composeRule.findOptionCardByTimeText("21:30").assertIsDisplayed()
            composeRule.findSetAlarmButtonMatchingTimeOrIndex(1).performClick()
            composeRule.waitForIdle()
            assertAlarmIntentSent(hour24 = 21, minute = 30)

            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 5, minute = 15)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_GALAXY_A56)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t4_02_samsungGalaxyA56Midday20mPowerNap() {
        envRule.setClockToLocalTime(LocalTime.of(13, 30))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertThumbZoneErgonomics()
            val dayBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(relativeLuminance(dayBg) > 0.60)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)

            composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP)
                .assertIsDisplayed()
                .assertTextContains("30", substring = true)

            composeRule.setLatencyMinutes(15)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
            // 13:30 + 15m + 20m = 14:05
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).assertTextContains("14:05", substring = true)

            composeRule.tapSetTimerOnCard("nap_20")
            assertTimerIntentSent(durationSeconds = 2100)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t4_03_spontaneousLateNightSleepNowFlow() {
        envRule.setClockToLocalTime(LocalTime.of(23, 45))
        withMainActivity(composeRule) { _, activity ->
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            assertClockTickHapticRecorded(activity)
            // 23:45 + 15m + 450m = 07:30 (DAWN_DUSK)
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("07:30", substring = true)
            assertSystemBarsLight(activity, expectedLightStatus = false, expectedLightNav = false)

            envRule.advanceClockMinutes(composeRule, 2) // 23:47 -> 5c = 07:32, 6c = 09:02
            composeRule.onNodeWithTag(RestCycleTestTags.optionCard("sleep_now_5")).assertTextContains("07:32", substring = true)

            composeRule.tapOptionCard("sleep_now_6")
            // Robolectric ShadowView retains only a single sticky haptic slot per View in the same Activity;
            // verify only CLOCK_TICK has been recorded (card tap haptic is verified in isolation in Tier 1 & Tier 2).
            assertTrue(anyHapticFeedbackPerformed(activity).all { it == HapticFeedbackConstants.CLOCK_TICK })
            val dayBg = composeRule.sampleRootBackgroundColorArgb(650)
            assertTrue(relativeLuminance(dayBg) > 0.60)
            assertSystemBarsLight(activity, expectedLightStatus = true, expectedLightNav = true)

            composeRule.tapSetAlarmOnCard("sleep_now_6")
            assertAlarmIntentSent(hour24 = 9, minute = 2)
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_GALAXY_A56_HU)
    fun t4_04_hungarianTraveler12h24hAndLocaleSwitch() {
        withMainActivity(composeRule) { scenario, _ ->
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertTextContains("Ébredés ekkor", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertTextContains("Most fekszem le", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_GO_TO_BED_AT).assertTextContains("Lefekvés ekkor", substring = true)
            composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertTextContains("Szieszta", substring = true)

            flip24HourFormatAndResume(scenario, composeRule, is24Hour = false)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertIsDisplayed()
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)

            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(9, 0), is24Hour = false)
            // 09:00 + 15m + 450m = 16:45 (4:45 PM)
            composeRule.tapSetAlarmOnCard("bed_5")
            Intents.intended(
                allOf(
                    hasAction(AlarmClock.ACTION_SET_ALARM),
                    hasExtra(AlarmClock.EXTRA_HOUR, 16),
                    hasExtra(AlarmClock.EXTRA_MINUTES, 45),
                    hasExtra(AlarmClock.EXTRA_SKIP_UI, false),
                    hasExtra(AlarmClock.EXTRA_MESSAGE, not(isEmptyOrNullString()))
                )
            )

            flip24HourFormatAndResume(scenario, composeRule, is24Hour = true)
            composeRule.onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM).assertDoesNotExist()
        }
    }

    @Test
    fun t4_05_plannedEarlyBedtimePlusDataStoreColdRestartPersistence() {
        var scenario = launchMainActivity(composeRule)
        try {
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(21, 30), is24Hour = true)
            composeRule.setLatencyMinutes(20)
            // 21:30 + 20m = 21:50 -> 4c 03:50, 5c 05:20 (NIGHT), 6c 06:50 (DAWN_DUSK)
            composeRule.findOptionCardById("bed_4").performScrollTo().assertTextContains("03:50", substring = true)
            composeRule.findOptionCardById("bed_5").performScrollTo().assertTextContains("05:20", substring = true)
            composeRule.findOptionCardById("bed_6").performScrollTo().assertTextContains("06:50", substring = true)
            composeRule.tapOptionCard("bed_6")

            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(20)
            composeRule.switchMode(SleepMode.GO_TO_BED_AT)
            composeRule.selectWheelTime(LocalTime.of(21, 30), is24Hour = true)
            composeRule.findOptionCardById("bed_5").assertTextContains("05:20", substring = true)
        } finally {
            scenario.close()
        }
    }

    @Test
    @Config(qualifiers = E2EDeviceConfigs.QUALIFIER_GALAXY_A56)
    @MissingClockApp
    fun t4_06_fullCycle90mRecoveryNapWithCustomLatencyAndMissingClockApp() {
        envRule.setClockToLocalTime(LocalTime.of(15, 0))
        var scenario = launchMainActivity(composeRule)
        try {
            lateinit var activity: com.restcycle.app.MainActivity
            scenario.onActivity { activity = it }
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(25)
            // 15:00 + 25m + 90m = 16:55 (115m)
            composeRule.findOptionCardById("nap_90").assertTextContains("16:55", substring = true)

            composeRule.tapSetAlarmOnCard("nap_90")
            assertMissingClockFallbackShown(composeRule, activity)
            composeRule.tapSetTimerOnCard("nap_90")
            assertMissingClockFallbackShown(composeRule, activity)

            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(25)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun t4_07_multiModeExplorationZeroCalculateButtonAndDualLatencyPersistence() {
        var scenario = launchMainActivity(composeRule)
        try {
            lateinit var activity: com.restcycle.app.MainActivity
            scenario.onActivity { activity = it }
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            assertClockTickHapticRecorded(activity)
            composeRule.assertZeroCalculateButton()
            listOf(SleepMode.GO_TO_BED_AT, SleepMode.POWER_NAP, SleepMode.WAKE_UP_AT).forEach { mode ->
                composeRule.switchMode(mode)
                assertTrue(anyHapticFeedbackPerformed(activity).all { it == HapticFeedbackConstants.CLOCK_TICK })
                composeRule.assertZeroCalculateButton()
            }

            composeRule.setLatencyMinutes(30)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.setLatencyMinutes(5)

            scenario = relaunchMainActivityPreservingDataStore(scenario, composeRule)
            composeRule.assertLatencyValueEquals(30)
            composeRule.switchMode(SleepMode.POWER_NAP)
            composeRule.assertLatencyValueEquals(5)
        } finally {
            scenario.close()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun t4_08_dawnToDayToNightCommuterScheduleAcrossAllEightSkyBoundaries() {
        withMainActivity(composeRule) { _, activity ->
            val expectedBoundaries = listOf(
                LocalTime.of(5, 29) to false,  // NIGHT
                LocalTime.of(5, 30) to false,  // DAWN_DUSK
                LocalTime.of(7, 59) to false,  // DAWN_DUSK
                LocalTime.of(8, 0) to true,    // DAY
                LocalTime.of(17, 29) to true,  // DAY
                LocalTime.of(17, 30) to false, // DAWN_DUSK
                LocalTime.of(20, 29) to false, // DAWN_DUSK
                LocalTime.of(20, 30) to false  // NIGHT
            )
            expectedBoundaries.forEach { (time, expectedLightBars) ->
                composeRule.selectWheelTime(time, is24Hour = true)
                val bg = composeRule.sampleRootBackgroundColorArgb(650)
                assertSystemBarsLight(activity, expectedLightStatus = expectedLightBars, expectedLightNav = expectedLightBars)
                composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).assertIsDisplayed()
                if (expectedLightBars) {
                    assertTrue("Expected bright DAY bg at $time", relativeLuminance(bg) > 0.60)
                } else if (time == LocalTime.of(5, 29) || time == LocalTime.of(20, 30)) {
                    assertTrue("Expected AMOLED NIGHT bg at $time", Color.red(bg) <= 5 && Color.green(bg) <= 5 && Color.blue(bg) <= 5)
                } else {
                    assertTrue("Expected twilight DAWN_DUSK bg at $time", relativeLuminance(bg) < 0.50)
                }
            }
        }
    }

    @Test
    fun t4_09_offlinePrivacyBilingualCompletenessAndWakeAlarmEndToEnd() {
        withMainActivity(composeRule) { _, activity ->
            assertOfflineManifestAndPermissions(activity)
            val doc = parseAndroidManifestXml()
            val queries = doc.getElementsByTagName("queries")
            assertTrue(queries.length > 0)
            val actions = (0 until queries.length).flatMap { i ->
                val list = (queries.item(i) as Element).getElementsByTagName("action")
                (0 until list.length).map { j -> (list.item(j) as Element).getAttribute("android:name") }
            }.toSet()
            assertTrue(actions.contains("android.intent.action.SET_ALARM"))
            assertTrue(actions.contains("android.intent.action.SET_TIMER"))

            val mainDir = resolveAppMainDir()
            val enMap = parseStringsXmlMap(File(mainDir, "res/values/strings.xml"))
            val huMap = parseStringsXmlMap(File(mainDir, "res/values-hu/strings.xml"))
            assertEquals(enMap.keys, huMap.keys)

            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.WHEEL_HOUR)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)

            composeRule.tapWakeAlarmButton()
            assertAlarmIntentSent(hour24 = 6, minute = 50)
        }
    }

    @Test
    @Config(sdk = [E2EDeviceConfigs.ROBOLECTRIC_SDK_ANDROID_15], qualifiers = E2EDeviceConfigs.QUALIFIER_NOTHING_PHONE_2A)
    fun t4_10_android15CompatibilityAndMidnightCrossingSleepNowTicker() {
        envRule.setClockToLocalTime(LocalTime.of(23, 55))
        withMainActivity(composeRule) { _, _ ->
            composeRule.setLatencyMinutes(20)
            composeRule.switchMode(SleepMode.SLEEP_NOW)
            // 23:55 + 20m = 00:15 -> 1c 01:45, 5c 07:45, 6c 09:15
            composeRule.findOptionCardById("sleep_now_5").assertTextContains("07:45", substring = true)
            composeRule.findOptionCardById("sleep_now_1").performScrollTo().assertTextContains("01:45", substring = true)

            envRule.advanceClockMinutes(composeRule, 6) // 00:01 -> 5c 07:51, 1c 01:51
            composeRule.findOptionCardById("sleep_now_5").performScrollTo().assertTextContains("07:51", substring = true)
            composeRule.findOptionCardById("sleep_now_1").performScrollTo().assertTextContains("01:51", substring = true)
            composeRule.assertAllNumericTextsUnderTagHaveTnum(RestCycleTestTags.UPPER_RESULTS_ZONE)
        }
    }
}
