package com.restcycle.app.unit

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.data.preferences.LatencyPreferencesRepository
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.OnboardingOverlay
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.viewmodel.SleepViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class OnboardingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboarding_4StepInteractiveFlow_updatesPreferencesAndFinishes() {
        var latency by mutableIntStateOf(15)
        var theme by mutableStateOf(AppThemeStyle.TOKYO_NIGHT)
        var amoled by mutableStateOf(false)
        var caffeine by mutableIntStateOf(8)
        var reminder by mutableStateOf(false)
        var finished = false

        composeRule.setContent {
            AtmosphereTheme(
                skyPhase = SkyPhase.NIGHT,
                themeStyle = theme,
                pureAmoledNight = amoled
            ) {
                OnboardingOverlay(
                    sleepLatencyMinutes = latency,
                    themeStyle = theme,
                    pureAmoledNight = amoled,
                    caffeineCutoffHours = caffeine,
                    bedtimeReminderEnabled = reminder,
                    is24HourFormat = true,
                    onUpdateSleepLatency = { latency = it },
                    onSelectThemeStyle = { theme = it },
                    onTogglePureAmoledNight = { amoled = it },
                    onSelectCaffeineCutoffHours = { caffeine = it },
                    onToggleBedtimeReminder = { reminder = it },
                    onFinish = { finished = true }
                )
            }
        }

        // Step 1
        composeRule.onNodeWithTag(RestCycleTestTags.ONBOARDING_ROOT).assertIsDisplayed()
        composeRule.onNodeWithText("1 / 4").assertIsDisplayed()
        composeRule.onNodeWithTag(RestCycleTestTags.ONBOARDING_NEXT_BTN).performClick()
        composeRule.waitForIdle()

        // Step 2: Latency preset
        composeRule.onNodeWithText("2 / 4").assertIsDisplayed()
        composeRule.onNodeWithText("20m").performClick()
        composeRule.waitForIdle()
        assertEquals(20, latency)
        composeRule.onNodeWithTag(RestCycleTestTags.ONBOARDING_NEXT_BTN).performClick()
        composeRule.waitForIdle()

        // Step 3: Live theme picker
        composeRule.onNodeWithText("3 / 4").assertIsDisplayed()
        composeRule.onNodeWithText("Solarized").performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeStyle.SOLARIZED, theme)
        composeRule.onNodeWithTag(RestCycleTestTags.ONBOARDING_NEXT_BTN).performClick()
        composeRule.waitForIdle()

        // Step 4: Habits & Get Started
        composeRule.onNodeWithText("4 / 4").assertIsDisplayed()
        composeRule.onNodeWithText("10h").performClick()
        composeRule.waitForIdle()
        assertEquals(10, caffeine)

        composeRule.onNodeWithTag(RestCycleTestTags.ONBOARDING_FINISH_BTN).performClick()
        composeRule.waitForIdle()
        assertTrue(finished)
    }

    @Test
    fun onboarding_skipImmediatelyCallsOnFinish() {
        var finished = false
        composeRule.setContent {
            AtmosphereTheme(skyPhase = SkyPhase.NIGHT, themeStyle = AppThemeStyle.TOKYO_NIGHT) {
                OnboardingOverlay(
                    sleepLatencyMinutes = 15,
                    themeStyle = AppThemeStyle.TOKYO_NIGHT,
                    pureAmoledNight = false,
                    caffeineCutoffHours = 8,
                    bedtimeReminderEnabled = false,
                    is24HourFormat = true,
                    onUpdateSleepLatency = {},
                    onSelectThemeStyle = {},
                    onTogglePureAmoledNight = {},
                    onSelectCaffeineCutoffHours = {},
                    onToggleBedtimeReminder = {},
                    onFinish = { finished = true }
                )
            }
        }

        composeRule.onNodeWithTag(RestCycleTestTags.ONBOARDING_SKIP_BTN).performClick()
        composeRule.waitForIdle()
        assertTrue(finished)
    }

    @Test
    fun viewModel_completeOnboarding_persistsFlag() = runTest {
        val onboardingFlow = MutableStateFlow(false)
        val fakeRepo = object : LatencyPreferencesRepository {
            override val sleepLatencyMinutes: Flow<Int> = MutableStateFlow(15)
            override val napLatencyMinutes: Flow<Int> = MutableStateFlow(10)
            override val onboardingCompleted: Flow<Boolean> = onboardingFlow
            override suspend fun setSleepLatency(minutes: Int) {}
            override suspend fun setNapLatency(minutes: Int) {}
            override suspend fun setOnboardingCompleted(completed: Boolean) {
                onboardingFlow.value = completed
            }
        }

        val vm = SleepViewModel(repository = fakeRepo)
        assertFalse(onboardingFlow.value)
        vm.completeOnboarding()
        assertEquals(true, vm.onboardingCompleted.value)
    }
}
