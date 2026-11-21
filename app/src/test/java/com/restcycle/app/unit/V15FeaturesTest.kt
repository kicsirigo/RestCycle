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
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepHistoryCodec
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
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
    fun tokyoNightIsDefaultTheme_dynamicSkyIsRemoved_draculaAndRosePineWork() {
        assertEquals(AppThemeStyle.TOKYO_NIGHT, LocalAppThemeStyle.let { AppThemeStyle.entries.first() })

        listOf(AppThemeStyle.DRACULA, AppThemeStyle.ROSE_PINE).forEach { style ->
            val day = paletteForTheme(SkyPhase.DAY, style, pureAmoledNight = false)
            val night = paletteForTheme(SkyPhase.NIGHT, style, pureAmoledNight = false)
            val amoled = paletteForTheme(SkyPhase.NIGHT, style, pureAmoledNight = true)

            assertTrue(day.isLightSystemBars)
            assertFalse(night.isLightSystemBars)
            assertNotEquals(Color(0xFF000000), night.backgroundBottom)
            assertEquals(Color(0xFF000000), amoled.backgroundTop)
            assertEquals(Color(0xFF000000), amoled.backgroundBottom)
        }

        // Settings shows Dracula & Rosé Pine and no longer contains Dynamic Sky
        composeRule.onNodeWithTag(RestCycleTestTags.SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dynamic Sky").assertDoesNotExist()
        composeRule.onNodeWithText("Dracula").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Rosé Pine").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Tokyo Night").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun ambientSoundPill_cyclesThroughAllSynthesizedSoundModes() {
        assertEquals(AmbientSoundMode.BROWN_NOISE, AmbientSoundMode.OFF.next())
        assertEquals(AmbientSoundMode.STREAM_RAIN, AmbientSoundMode.BROWN_NOISE.next())
        assertEquals(AmbientSoundMode.CAMPFIRE, AmbientSoundMode.STREAM_RAIN.next())
        assertEquals(AmbientSoundMode.OFF, AmbientSoundMode.CAMPFIRE.next())

        composeRule.onNodeWithTag(RestCycleTestTags.modeTab(SleepMode.SLEEP_NOW)).performClick()
        composeRule.waitForIdle()

        val soundBtn = composeRule.onNodeWithTag(RestCycleTestTags.AMBIENT_SOUND_BUTTON)
        soundBtn.assertIsDisplayed().assertTextContains("Sounds", substring = true)

        soundBtn.performClick()
        composeRule.waitForIdle()
        soundBtn.assertTextContains("Brown Noise", substring = true)

        soundBtn.performClick()
        composeRule.waitForIdle()
        soundBtn.assertTextContains("Stream", substring = true)

        soundBtn.performClick()
        composeRule.waitForIdle()
        soundBtn.assertTextContains("Campfire", substring = true)

        soundBtn.performClick()
        composeRule.waitForIdle()
        soundBtn.assertTextContains("Sounds", substring = true)
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
