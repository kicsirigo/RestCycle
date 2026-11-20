package com.restcycle.app.unit

import android.app.Application
import android.content.Intent
import android.provider.AlarmClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.R
import com.restcycle.app.RestCycleApplication
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.model.CycleQuality
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepOption
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.PowerNapPresetCard
import com.restcycle.app.ui.components.SleepOptionCard
import com.restcycle.app.ui.theme.AtmosphereTheme
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class SleepOptionCardAndPowerNapTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUpIntents() {
        Intents.init()
    }

    @After
    fun tearDownIntents() {
        Intents.release()
    }

    @Test
    fun tappingOptionCardHighlightsWithoutFiringAlarmAndSetAlarmFiresIntent() {
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_5")).assertIsSelected()
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_6")).assertIsNotSelected()

        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_6")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_6")).assertIsSelected()
        assertTrue(Intents.getIntents().isEmpty())

        val decorView = composeRule.activity.window.decorView
        val haptics = collectHapticFeedbacks(decorView)
        assertTrue(
            "Expected CLOCK_TICK haptic on a Compose view, found $haptics",
            haptics.contains(HapticFeedbackConstants.CLOCK_TICK)
        )

        // Tap dedicated Wake Alarm button (only in WAKE_UP_AT)
        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        Intents.intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_ALARM),
                hasExtra(AlarmClock.EXTRA_HOUR, 6),
                hasExtra(AlarmClock.EXTRA_MINUTES, 50),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, false)
            )
        )
    }

    @Test
    fun powerNap_rendersStackedCardsInertiaTooltipAndTimerIntent_andHandlesMissingClockAppNonBlocking() {
        val app = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        Shadows.shadowOf(app as Application).checkActivities(true)

        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN).assertDoesNotExist()
        composeRule.onNodeWithTag(RestCycleTestTags.INERTIA_TOOLTIP).assertIsDisplayed()

        val c10 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_10")).getUnclippedBoundsInRoot()
        val c20 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_20")).getUnclippedBoundsInRoot()
        val c90 = composeRule.onNodeWithTag(RestCycleTestTags.optionCard("nap_90")).getUnclippedBoundsInRoot()
        assertTrue(c10.bottom <= c20.top + 1.dp)
        assertTrue(c20.bottom <= c90.top + 1.dp)

        // Verify missing clock app fallback shows non-blocking Snackbar and keeps Activity alive
        composeRule.onNodeWithTag(RestCycleTestTags.setAlarmButton("nap_20")).performClick()
        composeRule.waitForIdle()

        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(app.getString(R.string.clock_app_unavailable)).assertIsDisplayed()

        // Tapping Timer button immediately after Alarm button works without dismissing a modal dialog
        composeRule.onNodeWithTag(RestCycleTestTags.setTimerButton("nap_20")).performClick()
        composeRule.waitForIdle()

        Intents.intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_TIMER),
                hasExtra(AlarmClock.EXTRA_LENGTH, (10 + 20) * 60),
                hasExtra(AlarmClock.EXTRA_MESSAGE, app.getString(R.string.nap_classic_title)),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, false)
            )
        )

        // Verify duration formatting has 00-padded minutes on 6-cycle card when back in WAKE_UP_AT
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.optionCard("wake_6"))
            .assertTextContains("9h 00m", substring = true)
    }

    private fun collectHapticFeedbacks(root: android.view.View): List<Int> {
        val result = mutableListOf<Int>()
        val perf = Shadows.shadowOf(root).lastHapticFeedbackPerformed()
        if (perf != -1) result.add(perf)
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                result.addAll(collectHapticFeedbacks(root.getChildAt(i)))
            }
        }
        return result
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "hu-w360dp-h640dp-xxhdpi")
class NarrowHungarianLargeFontCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sleepNowAndPowerNapCards_hungarian360dpFontScale130_wrapCleanlyWithoutTruncation() {
        val sleepNowOption = SleepOption(
            id = "sleep_now_6",
            targetTime = LocalTime.of(7, 45),
            cycles = 6,
            durationMinutes = 540,
            quality = CycleQuality.OPTIMAL,
            labelResId = R.string.quality_optimal,
            countdownMinutesFromNow = 555L
        )
        val napClassicOption = SleepOption(
            id = SleepCycleCalculator.NAP_CLASSIC_ID,
            targetTime = LocalTime.of(14, 30),
            cycles = null,
            durationMinutes = 20,
            quality = CycleQuality.POWER_NAP,
            labelResId = R.string.nap_classic_title,
            countdownMinutesFromNow = 30L
        )
        val napFullOption = SleepOption(
            id = SleepCycleCalculator.NAP_FULL_ID,
            targetTime = LocalTime.of(15, 40),
            cycles = 1,
            durationMinutes = 90,
            quality = CycleQuality.POWER_NAP,
            labelResId = R.string.nap_full_title,
            countdownMinutesFromNow = 100L
        )

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = 1.3f)
            ) {
                AtmosphereTheme(skyPhase = SkyPhase.DAY) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SleepOptionCard(
                            option = sleepNowOption,
                            isHighlighted = true,
                            is24HourFormat = false,
                            onSelect = {},
                            onSetAlarm = {}
                        )
                        PowerNapPresetCard(
                            option = napClassicOption,
                            isHighlighted = true,
                            is24HourFormat = false,
                            onSelect = {},
                            onSetAlarm = {},
                            onSetTimer = {}
                        )
                        PowerNapPresetCard(
                            option = napFullOption,
                            isHighlighted = false,
                            is24HourFormat = false,
                            onSelect = {},
                            onSetAlarm = {},
                            onSetTimer = {}
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("6 ciklus", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("9ó 00p", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("555 perc múlva", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Klasszikus szieszta", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ajánlott", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("100 perc múlva", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.setAlarmButton("sleep_now_6")).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.setTimerButton(SleepCycleCalculator.NAP_CLASSIC_ID))
            .assertIsDisplayed()
    }
}
