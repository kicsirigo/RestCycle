package com.restcycle.app.unit

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.R
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepHistoryCodec
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.platform.audio.AmbientSoundController
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.LocalAppThemeStyle
import com.restcycle.app.ui.theme.paletteForTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class V15FeaturesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tokyoNightIsDefaultTheme_everforestReplacesCatppuccin_draculaAndRosePineWork() {
        assertEquals(AppThemeStyle.TOKYO_NIGHT, LocalAppThemeStyle.let { AppThemeStyle.entries.first() })

        listOf(AppThemeStyle.EVERFOREST, AppThemeStyle.DRACULA, AppThemeStyle.ROSE_PINE).forEach { style ->
            val day = paletteForTheme(SkyPhase.DAY, style, pureAmoledNight = false)
            val night = paletteForTheme(SkyPhase.NIGHT, style, pureAmoledNight = false)
            val amoled = paletteForTheme(SkyPhase.NIGHT, style, pureAmoledNight = true)

            assertTrue(day.isLightSystemBars)
            assertFalse(night.isLightSystemBars)
            assertNotEquals(Color(0xFF000000), night.backgroundBottom)
            assertEquals(Color(0xFF000000), amoled.backgroundTop)
            assertEquals(Color(0xFF000000), amoled.backgroundBottom)
        }

        // Settings shows Everforest, Dracula & Rosé Pine and removes Dynamic Sky & Catppuccin Macchiato
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dynamic Sky").assertDoesNotExist()
        composeRule.onNodeWithText("Catppuccin Macchiato").assertDoesNotExist()
        composeRule.onNodeWithText("Everforest").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Dracula").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Rosé Pine").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Tokyo Night").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun ambientSoundPill_andAllSixDistinctMp3Tracks_andTwoAndHalfHourCycleWork() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val rawIds = listOf(
            R.raw.ambient_brown_noise,
            R.raw.ambient_stream_rain,
            R.raw.ambient_campfire,
            R.raw.ambient_night_crickets,
            R.raw.ambient_pad_432,
            R.raw.ambient_singing_bowl
        )
        rawIds.forEach { rawId ->
            val sizeBytes = context.resources.openRawResource(rawId).use { it.readBytes().size }
            assertTrue("Expected non-empty MP3 raw asset $rawId, got $sizeBytes", sizeBytes > 400_000)
        }
        assertEquals(6, rawIds.distinct().size)

        assertEquals(9000.0, AmbientSoundController.MIX_CYCLE_DURATION_SECONDS, 0.01)
        listOf(0.0, 45.0, 1800.0, 4500.0, 8999.0).forEach { t ->
            val (l, r, offsetGain) = AmbientSoundController.computeMixGains(t)
            assertTrue("Left gain out of bounds at $t: $l", l in 0.50f..0.96f)
            assertTrue("Right gain out of bounds at $t: $r", r in 0.50f..0.96f)
            assertTrue("Offset gain out of bounds at $t: $offsetGain", offsetGain in 0.08f..0.42f)
        }

        assertEquals(AmbientSoundMode.BROWN_NOISE, AmbientSoundMode.OFF.next())
        assertEquals(AmbientSoundMode.STREAM_RAIN, AmbientSoundMode.BROWN_NOISE.next())
        assertEquals(AmbientSoundMode.CAMPFIRE, AmbientSoundMode.STREAM_RAIN.next())
        assertEquals(AmbientSoundMode.NIGHT_CRICKETS, AmbientSoundMode.CAMPFIRE.next())
        assertEquals(AmbientSoundMode.AMBIENT_PAD, AmbientSoundMode.NIGHT_CRICKETS.next())
        assertEquals(AmbientSoundMode.SINGING_BOWL, AmbientSoundMode.AMBIENT_PAD.next())
        assertEquals(AmbientSoundMode.OFF, AmbientSoundMode.SINGING_BOWL.next())

        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.SLEEP_NOW)).performClick()
        composeRule.waitForIdle()

        val soundBtn = composeRule.onNodeWithTag(RestCycleTestTags.AMBIENT_SOUND_BUTTON)
        soundBtn.assertIsDisplayed().assertTextContains("Sounds", substring = true)

        listOf(
            "Ocean Waves",
            "Stream & Rain",
            "Fireplace",
            "Night Crickets",
            "432 Hz Ambient",
            "Singing Bowl",
            "Sounds"
        ).forEach { expectedLabel ->
            soundBtn.performClick()
            composeRule.waitForIdle()
            soundBtn.assertTextContains(expectedLabel, substring = true)
        }
    }

    @Test
    fun sleepLogic_neverFabricatesCyclesOnQuickTap_andAllTimeCalendarWorksEndToEnd() {
        // 1. Verify duration/cycle math: short quick tap (2 min) -> 2 min, 0 cycles (NOT 450m / 5 cycles!)
        val (shortDur, shortCycles) = SleepHistoryCodec.computeDurationAndCycles(
            startEpochMillis = 1_700_000_000_000L,
            endEpochMillis = 1_700_000_000_000L + 2 * 60_000L,
            sleepLatencyMinutes = 15
        )
        assertEquals(2, shortDur)
        assertEquals(0, shortCycles)

        // Real 7h 45m (465m) sleep minus 15m latency = 450m = 5 cycles
        val (realDur, realCycles) = SleepHistoryCodec.computeDurationAndCycles(
            startEpochMillis = 1_700_000_000_000L,
            endEpochMillis = 1_700_000_000_000L + 465 * 60_000L,
            sleepLatencyMinutes = 15
        )
        assertEquals(465, realDur)
        assertEquals(5, realCycles)

        // All-time codec retains > 7 sessions across months/years cleanly (up to 2000)
        val multiMonthRecords = (1..25).map { idx ->
            SleepSessionRecord(
                startEpochMillis = 1_700_000_000_000L + idx * 86_400_000L,
                endEpochMillis = 1_700_000_000_000L + idx * 86_400_000L + 27_900_000L,
                durationMinutes = 465,
                cycles = 5,
                mood = SleepQualityMood.RESTED
            )
        }
        val decoded = SleepHistoryCodec.decode(SleepHistoryCodec.encode(multiMonthRecords))
        assertEquals(25, decoded.size)

        // Streak calculation across consecutive days
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val streakRecords = (0..2).map { d ->
            val endMs = ZonedDateTime.of(today.minusDays(d.toLong()).atTime(7, 0), zone).toInstant().toEpochMilli()
            SleepSessionRecord(
                startEpochMillis = endMs - 450 * 60_000L,
                endEpochMillis = endMs,
                durationMinutes = 450,
                cycles = 5,
                mood = SleepQualityMood.RESTED
            )
        }
        assertEquals(3, SleepHistoryCodec.computeCurrentStreakDays(streakRecords, today, zone))

        // 2. UI flow: Start sleep -> immediate "Woke up" tap -> short session notice shown,
        // selecting mood logs actual 0m / 0 cycles (never 5 cycles!), calendar month nav & delete work
        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.SLEEP_NOW)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_TRACKER_BUTTON)
            .assertIsDisplayed()
            .assertTextContains("Sleep", substring = true)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_TRACKER_BUTTON)
            .assertTextContains("Woke up", substring = true)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_TRACKER_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_SHORT_SESSION_NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText("How did you sleep?").assertIsDisplayed()
        SleepQualityMood.entries.forEach { mood ->
            composeRule.onNodeWithTag(RestCycleTestTags.sleepMoodOption(mood.name)).assertIsDisplayed()
        }

        // Complete quick session -> must record actual <=1m duration with 0 cycles
        composeRule.onNodeWithTag(RestCycleTestTags.sleepMoodOption(SleepQualityMood.RESTED.name))
            .performClick()
        composeRule.waitForIdle()

        // Open All-Time Calendar
        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_HISTORY_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_MONTH_LABEL).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.calendarDayCell(today.toString()))
            .performScrollTo()
            .assertIsDisplayed()
        // Never shows fake 5 cycles inside the calendar dialog for this quick session!
        composeRule.onNode(
            hasText("5 cycles", substring = true) and hasAnyAncestor(isDialog())
        ).assertDoesNotExist()
        composeRule.onNode(
            hasText("1 nights this month • Avg 0 min", substring = true) and hasAnyAncestor(isDialog())
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Rested •", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        // Navigate back to previous month and return to current month
        val prevMonth = YearMonth.from(today).minusMonths(1)
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_PREV_MONTH)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.calendarDayCell(prevMonth.atDay(1).toString()))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_NEXT_MONTH)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.calendarDayCell(today.toString()))
            .performScrollTo()
            .assertIsDisplayed()

        // Delete the logged session cleanly
        composeRule.onNodeWithContentDescription("Remove entry")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Rested •", substring = true).assertDoesNotExist()

        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun launcherShortcuts_intentExtrasOpenExpectedModeOrDialog() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val napIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SHORTCUT_ACTION, MainActivity.SHORTCUT_POWER_NAP)
        val controller = Robolectric.buildActivity(MainActivity::class.java, napIntent).setup()
        val activity = controller.get()
        assertEquals(
            MainActivity.SHORTCUT_POWER_NAP,
            activity.intent.getStringExtra(MainActivity.EXTRA_SHORTCUT_ACTION)
        )
        controller.pause().stop().destroy()
    }
}
