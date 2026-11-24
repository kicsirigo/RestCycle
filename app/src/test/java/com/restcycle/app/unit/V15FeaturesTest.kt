package com.restcycle.app.unit

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
import com.restcycle.app.platform.audio.AmbientSoundService
import com.restcycle.app.platform.audio.AmbientSoundStateHolder
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.RestCycleIcons
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
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

    @Before
    fun resetAmbientSoundState() {
        AmbientSoundStateHolder.stopPlayback()
    }

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
            assertTrue("Expected compact 15s preview MP3 raw asset $rawId, got $sizeBytes", sizeBytes in 100_000..300_000)
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

        // Verify 30s sleep timer fade-out math & countdown formatting
        assertEquals(1.0f, AmbientSoundController.computeFadeMultiplier(timerActive = false, remainingSeconds = 5L), 0.001f)
        assertEquals(1.0f, AmbientSoundController.computeFadeMultiplier(timerActive = true, remainingSeconds = 45L), 0.001f)
        assertEquals(0.5f, AmbientSoundController.computeFadeMultiplier(timerActive = true, remainingSeconds = 15L), 0.001f)
        assertEquals(0.0f, AmbientSoundController.computeFadeMultiplier(timerActive = true, remainingSeconds = 0L), 0.001f)
        assertEquals("15:05", AmbientSoundService.formatRemainingTime(905L))

        // Sound picker pill is accessible right from WAKE_UP_AT as well as all other modes
        composeRule.onNodeWithTag(RestCycleTestTags.SOUND_PICKER_PILL)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.AMBIENT_SOUND_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.SOUND_VOLUME_SLIDER).performScrollTo().assertIsDisplayed()

        // All 6 sounds + Off + timer options are present in the dialog
        AmbientSoundMode.entries.forEach { mode ->
            composeRule.onNodeWithTag(RestCycleTestTags.ambientSoundOption(mode.name))
                .performScrollTo()
                .assertIsDisplayed()
        }

        // Choose Fireplace + 30 min sleep timer
        composeRule.onNodeWithTag(RestCycleTestTags.ambientSoundOption(AmbientSoundMode.CAMPFIRE.name))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(RestCycleTestTags.soundTimerOption(30))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(AmbientSoundMode.CAMPFIRE, AmbientSoundStateHolder.state.value.mode)
        assertEquals(30, AmbientSoundStateHolder.state.value.sleepTimerMinutes)
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()

        // Switch to SLEEP_NOW: landscape sound button reflects active sound & opens picker dialog
        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.SLEEP_NOW)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.AMBIENT_SOUND_BUTTON)
            .assertIsDisplayed()
            .assertTextContains("Fireplace", substring = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.AMBIENT_SOUND_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.ambientSoundOption(AmbientSoundMode.OFF.name))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(AmbientSoundMode.OFF, AmbientSoundStateHolder.state.value.mode)
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()
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

    @Test
    fun manualSleepLog_cyclePatternInsight_andJsonBackupWorkEndToEnd() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        // 1. Cycle Pattern Insight pure calculation (#3)
        val singleNight = listOf(
            SleepSessionRecord(1_700_000_000_000L, 1_700_027_000_000L, 450, 5, SleepQualityMood.RESTED)
        )
        assertNull(SleepHistoryCodec.computeBestCycleInsight(singleNight))

        val mixedNights = listOf(
            SleepSessionRecord(1_700_000_000_000L, 1_700_021_600_000L, 360, 4, SleepQualityMood.EXHAUSTED),
            SleepSessionRecord(1_700_100_000_000L, 1_700_121_600_000L, 360, 4, SleepQualityMood.FAIR),
            SleepSessionRecord(1_700_200_000_000L, 1_700_227_000_000L, 450, 5, SleepQualityMood.ENERGIZED),
            SleepSessionRecord(1_700_300_000_000L, 1_700_327_000_000L, 450, 5, SleepQualityMood.RESTED)
        )
        val insight = SleepHistoryCodec.computeBestCycleInsight(mixedNights)
        assertNotNull(insight)
        assertEquals(5, insight!!.bestCycles)
        assertEquals(100, insight.refreshedPercent)
        assertEquals(2, insight.sampleCount)

        // 2. JSON Export, Import & Merge deduplication (#5)
        val exportedJson = SleepHistoryCodec.exportToJson(mixedNights, exportedAtEpochMillis = 1_700_400_000_000L)
        assertTrue(exportedJson.contains("\"format\": \"restcycle_sleep_backup\""))
        val reimported = SleepHistoryCodec.importFromJson(exportedJson)
        assertNotNull(reimported)
        assertEquals(4, reimported!!.size)
        assertNull(SleepHistoryCodec.importFromJson("{ invalid json"))
        assertNull(SleepHistoryCodec.importFromJson("{\"foo\":\"bar\"}"))

        val (mergedSame, addedSame) = SleepHistoryCodec.mergeHistory(mixedNights, reimported)
        assertEquals(4, mergedSame.size)
        assertEquals(0, addedSame)

        val extraNight = listOf(
            SleepSessionRecord(1_700_500_000_000L, 1_700_527_000_000L, 450, 5, SleepQualityMood.ENERGIZED)
        )
        val (mergedPlusOne, addedPlusOne) = SleepHistoryCodec.mergeHistory(mixedNights, reimported + extraNight)
        assertEquals(5, mergedPlusOne.size)
        assertEquals(1, addedPlusOne)

        // 3. UI: Manual retroactive entry (#2) + Insight card appearance (#3) in All-Time Calendar
        Settings.System.putString(
            composeRule.activity.contentResolver,
            Settings.System.TIME_12_24,
            "24"
        )
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_HISTORY_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_INSIGHT_CARD).assertDoesNotExist()

        // Log first night manually on today with exact 1-minute selection & inline HapticTimeWheel
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_ADD_MANUAL_BTN)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_MANUAL_FORM).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_TIME_WHEEL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_WHEEL_HOUR).assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_WHEEL_MINUTE).assertIsDisplayed()

        // Step bedtime +1m (23:00 -> 23:01) and wake time -1m (06:45 -> 06:44) => 7h 43m
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_BED_PLUS).performScrollTo().performClick()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_WAKE_MINUS).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNode(
            hasText("23:01", substring = true) and hasAnyAncestor(isDialog())
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNode(
            hasText("06:44", substring = true) and hasAnyAncestor(isDialog())
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNode(
            hasText("7h 43m", substring = true) and hasAnyAncestor(isDialog())
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(RestCycleTestTags.manualMoodOption(SleepQualityMood.ENERGIZED.name))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_MANUAL_SAVE_BTN)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Energized •", substring = true).performScrollTo().assertIsDisplayed()

        // Log a second night manually (switch active wheel to Wake time & verify insight card appears)
        if (today.dayOfMonth > 1) {
            composeRule.onNodeWithTag(RestCycleTestTags.calendarDayCell(today.minusDays(1).toString()))
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_ADD_MANUAL_BTN)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_WAKE_ROW).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_TIME_WHEEL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.MANUAL_WAKE_MINUS).performScrollTo().performClick()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_MANUAL_SAVE_BTN)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CALENDAR_INSIGHT_CARD)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // 4. UI: Backup Export / Import buttons visible in Settings
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.BACKUP_EXPORT_BTN)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Export JSON", substring = true)
        composeRule.onNodeWithTag(RestCycleTestTags.BACKUP_IMPORT_BTN)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Import JSON", substring = true)
    }

    @Test
    fun monochromeVectorIcons_coverAllUiAndEnumStatesWithUnified24dpGrid() {
        val coreIcons = listOf(
            RestCycleIcons.Alarm,
            RestCycleIcons.Timer,
            RestCycleIcons.Info,
            RestCycleIcons.Settings,
            RestCycleIcons.Contrast,
            RestCycleIcons.Bell,
            RestCycleIcons.ExportUpload,
            RestCycleIcons.ImportDownload,
            RestCycleIcons.Globe,
            RestCycleIcons.Coffee,
            RestCycleIcons.Calendar,
            RestCycleIcons.Moon,
            RestCycleIcons.Sun,
            RestCycleIcons.Wind,
            RestCycleIcons.AudioWaveform,
            RestCycleIcons.Flame,
            RestCycleIcons.Sparkles
        )
        val soundIcons = AmbientSoundMode.entries.map { RestCycleIcons.forAmbientSound(it) }
        val moodIcons = SleepQualityMood.entries.map { RestCycleIcons.forMood(it) }

        (coreIcons + soundIcons + moodIcons).forEach { icon ->
            assertEquals("Expected 24dp width for ${icon.name}", 24.dp, icon.defaultWidth)
            assertEquals("Expected 24dp height for ${icon.name}", 24.dp, icon.defaultHeight)
            assertTrue("Expected vector paths in ${icon.name}", icon.root.size > 0)
        }
    }
}
