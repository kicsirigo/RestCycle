package com.restcycle.app.unit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.platform.reminder.BedtimeReminderScheduler
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.paletteForTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class V14FeaturesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun newThemesAndPureAmoledNightOverrideWorkAcrossAllSkyPhases() {
        val tokyoDay = paletteForTheme(SkyPhase.DAY, AppThemeStyle.TOKYO_NIGHT, pureAmoledNight = false)
        val tokyoNight = paletteForTheme(SkyPhase.NIGHT, AppThemeStyle.TOKYO_NIGHT, pureAmoledNight = false)
        val tokyoAmoled = paletteForTheme(SkyPhase.NIGHT, AppThemeStyle.TOKYO_NIGHT, pureAmoledNight = true)

        assertTrue(tokyoDay.isLightSystemBars)
        assertNotEquals(Color(0xFF000000), tokyoNight.backgroundBottom)
        assertEquals(Color(0xFF000000), tokyoAmoled.backgroundTop)
        assertEquals(Color(0xFF000000), tokyoAmoled.backgroundBottom)

        val gruvboxDay = paletteForTheme(SkyPhase.DAY, AppThemeStyle.GRUVBOX, pureAmoledNight = false)
        val gruvboxAmoled = paletteForTheme(SkyPhase.NIGHT, AppThemeStyle.GRUVBOX, pureAmoledNight = true)
        assertTrue(gruvboxDay.isLightSystemBars)
        assertEquals(Color(0xFF000000), gruvboxAmoled.backgroundBottom)
    }

    @Test
    fun bedtimeReminderScheduler_computesFutureTriggerAcrossMidnight() {
        val zone = ZoneId.of("UTC")
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(21, 0), zone)

        // Bedtime 22:30 -> reminder at 22:10 same day
        val sameDayMs = BedtimeReminderScheduler.computeTriggerEpochMillis(
            bedtime = LocalTime.of(22, 30),
            leadTimeMinutes = 20L,
            now = now,
            zoneId = zone
        )
        val expectedSameDay = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(22, 10), zone)
            .toInstant().toEpochMilli()
        assertEquals(expectedSameDay, sameDayMs)

        // Bedtime 20:30 -> reminder at 20:10 has passed, rolls to next day
        val nextDayMs = BedtimeReminderScheduler.computeTriggerEpochMillis(
            bedtime = LocalTime.of(20, 30),
            leadTimeMinutes = 20L,
            now = now,
            zoneId = zone
        )
        val expectedNextDay = ZonedDateTime.of(LocalDate.of(2026, 11, 21), LocalTime.of(20, 10), zone)
            .toInstant().toEpochMilli()
        assertEquals(expectedNextDay, nextDayMs)
    }

    @Test
    fun caffeinePill_andBreathingDialog_andSettingsNewThemesWorkEndToEnd() {
        // Default WAKE_UP_AT 06:50 -> highlighted wake_5 bedtime is 23:05 -> -8h caffeine cutoff = 15:05
        composeRule.onNodeWithTag(RestCycleTestTags.CAFFEINE_CUTOFF_PILL)
            .assertIsDisplayed()
            .assertTextContains(":05", substring = true)
        composeRule.onNodeWithText("\u2615", substring = true).assertDoesNotExist()

        // Switch to SLEEP_NOW and open 4-7-8 Breathing Exercise
        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.SLEEP_NOW)).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(RestCycleTestTags.BREATHE_BUTTON)
            .assertIsDisplayed()
            .performClick()
        composeRule.mainClock.advanceTimeBy(150L)
        composeRule.onNodeWithTag(RestCycleTestTags.BREATHING_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.mainClock.advanceTimeBy(150L)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        // Open Settings and verify Tokyo Night, Gruvbox, AMOLED toggle, Reminder toggle, Caffeine selector
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tokyo Night").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Gruvbox").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(RestCycleTestTags.AMOLED_NIGHT_TOGGLE).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(RestCycleTestTags.BEDTIME_REMINDER_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.caffeineOption(0)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // Switching back to WAKE_UP_AT with caffeine cutoff = Off hides the caffeine pill
        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.WAKE_UP_AT)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CAFFEINE_CUTOFF_PILL).assertDoesNotExist()
    }
}
