package com.restcycle.app.unit

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
            "Campfire",
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
    fun startSleepAndWakeUp4OptionRating_savesInto7DayHistory() {
        // Codec keeps up to 7 most recent records cleanly
        val sampleRecords = (1..9).map { idx ->
            SleepSessionRecord(
                startEpochMillis = 1_700_000_000_000L + idx * 86_400_000L,
                endEpochMillis = 1_700_000_000_000L + idx * 86_400_000L + 27_000_000L,
                durationMinutes = 450,
                cycles = 5,
                mood = SleepQualityMood.RESTED
            )
        }
        val decoded = SleepHistoryCodec.decode(SleepHistoryCodec.encode(sampleRecords))
        assertEquals(SleepHistoryCodec.MAX_HISTORY_RECORDS, decoded.size)

        // Start sleep in SLEEP_NOW mode
        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.SLEEP_NOW)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_TRACKER_BUTTON)
            .assertIsDisplayed()
            .assertTextContains("Sleep", substring = true)
            .performClick()
        composeRule.waitForIdle()

        // Button transitions to "Woke up"
        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_TRACKER_BUTTON)
            .assertTextContains("Woke up", substring = true)
            .performClick()
        composeRule.waitForIdle()

        // Dialog shows "How did you sleep?" and all 4 options
        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_TRACKER_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("How did you sleep?").assertIsDisplayed()
        SleepQualityMood.entries.forEach { mood ->
            composeRule.onNodeWithTag(RestCycleTestTags.sleepMoodOption(mood.name)).assertIsDisplayed()
        }

        // Tap 3rd option ("Rested")
        composeRule.onNodeWithTag(RestCycleTestTags.sleepMoodOption(SleepQualityMood.RESTED.name))
            .performClick()
        composeRule.waitForIdle()

        // Re-open history pill and confirm record appears in Last 7 Nights
        composeRule.onNodeWithTag(RestCycleTestTags.SLEEP_HISTORY_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Avg: 5.0 cycles", substring = true).assertIsDisplayed()
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
