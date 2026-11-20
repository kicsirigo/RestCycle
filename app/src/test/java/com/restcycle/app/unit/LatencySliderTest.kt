package com.restcycle.app.unit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.R
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.LatencySlider
import com.restcycle.app.ui.theme.AtmosphereTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LatencySliderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun latencySlider_exposesExactRangesAndSupportsSingleMinuteResolution() {
        var minutes by mutableIntStateOf(15)
        var maxMinutes by mutableIntStateOf(60)
        var labelRes by mutableStateOf(R.string.latency_sleep_label)

        composeRule.setContent {
            AtmosphereTheme(skyPhase = SkyPhase.DAY) {
                LatencySlider(
                    labelResId = labelRes,
                    minutes = minutes,
                    maxMinutes = maxMinutes,
                    onLatencyChange = { minutes = it }
                )
            }
        }

        composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(15f, 0f..60f)
            )
        )
        composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT)
            .assertTextContains("15", substring = true)

        composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER)
            .performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action(22f)
            }
        composeRule.waitForIdle()
        assertEquals(22, minutes)
        composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT)
            .assertTextContains("22", substring = true)

        // Power Nap range (0..30)
        composeRule.runOnUiThread {
            maxMinutes = 30
            minutes = 10
            labelRes = R.string.latency_nap_label
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(10f, 0f..30f)
            )
        )
    }
}
