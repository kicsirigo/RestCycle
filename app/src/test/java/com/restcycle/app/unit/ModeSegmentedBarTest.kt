package com.restcycle.app.unit

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.R
import com.restcycle.app.RestCycleApplication
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.ModeSegmentedBar
import com.restcycle.app.ui.theme.AtmosphereTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModeSegmentedBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun modeSegmentedBar_switchesModesAndEmitsClockTickOnlyOnNewSelection() {
        var selectedMode by mutableStateOf(SleepMode.WAKE_UP_AT)
        lateinit var hostView: android.view.View

        composeRule.setContent {
            hostView = LocalView.current
            AtmosphereTheme(skyPhase = SkyPhase.DAWN_DUSK) {
                ModeSegmentedBar(
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it }
                )
            }
        }

        composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).assertIsSelected()
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW).assertIsNotSelected()

        // No haptic on initial composition
        val shadowView = Shadows.shadowOf(hostView)
        assertEquals(-1, shadowView.lastHapticFeedbackPerformed())

        // Re-tapping active tab must not fire haptic
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_WAKE_UP_AT).performClick()
        composeRule.waitForIdle()
        assertEquals(-1, shadowView.lastHapticFeedbackPerformed())

        // Selecting a different tab fires CLOCK_TICK
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).performClick()
        composeRule.waitForIdle()
        assertEquals(SleepMode.POWER_NAP, selectedMode)
        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_POWER_NAP).assertIsSelected()
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, shadowView.lastHapticFeedbackPerformed())
    }

    @Test
    @Config(qualifiers = "hu-w360dp-h640dp-xhdpi")
    fun modeSegmentedBar_hungarianLabelsFitWithoutOverlapAt360dp() {
        val context = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        composeRule.setContent {
            AtmosphereTheme(skyPhase = SkyPhase.NIGHT) {
                ModeSegmentedBar(
                    selectedMode = SleepMode.SLEEP_NOW,
                    onModeSelected = {}
                )
            }
        }

        val tabs = SleepMode.entries.map { RestCycleTestTags.modeTab(it) }
        val switcherBounds = composeRule.onNodeWithTag(RestCycleTestTags.MODE_SWITCHER)
            .getUnclippedBoundsInRoot()

        val boundsList = tabs.map { tag ->
            composeRule.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(40.dp)
                .getUnclippedBoundsInRoot()
        }

        composeRule.onNodeWithTag(RestCycleTestTags.MODE_TAB_SLEEP_NOW)
            .assertTextContains(context.getString(R.string.mode_sleep_now))

        for (i in 0 until boundsList.size - 1) {
            assertTrue(
                "Tab $i right (${boundsList[i].right}) overlapped Tab ${i + 1} left (${boundsList[i + 1].left})",
                boundsList[i].right <= boundsList[i + 1].left + 1.dp
            )
        }
        boundsList.forEach { b ->
            assertTrue(b.left >= switcherBounds.left - 1.dp)
            assertTrue(b.right <= switcherBounds.right + 1.dp)
        }
    }

    @Test
    @Config(qualifiers = "hu-w360dp-h640dp-xxhdpi")
    fun modeSegmentedBar_hungarianAt360dpAndFontScale130_fitsWithoutOverflowOrTruncation() {
        val context = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = 1.3f)
            ) {
                AtmosphereTheme(skyPhase = SkyPhase.NIGHT) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ModeSegmentedBar(
                            selectedMode = SleepMode.SLEEP_NOW,
                            onModeSelected = {}
                        )
                    }
                }
            }
        }

        val tabs = SleepMode.entries.map { RestCycleTestTags.modeTab(it) }
        val boundsList = tabs.map { tag ->
            composeRule.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(40.dp)
                .getUnclippedBoundsInRoot()
        }
        for (i in 0 until boundsList.size - 1) {
            assertTrue(
                "Tab $i right (${boundsList[i].right}) overlapped Tab ${i + 1} left (${boundsList[i + 1].left})",
                boundsList[i].right <= boundsList[i + 1].left + 1.dp
            )
        }

        listOf(
            R.string.mode_wake_up_at,
            R.string.mode_sleep_now,
            R.string.mode_go_to_bed_at,
            R.string.mode_power_nap
        ).forEach { resId ->
            val label = context.getString(resId)
            val node = composeRule.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertTrue("Expected TextLayoutResult for '$label'", layouts.isNotEmpty())
            val layout = layouts.first()
            assertTrue(
                "Unexpected visual overflow/truncation for '$label' at fontScale=1.3f " +
                    "(didOverflowWidth=${layout.didOverflowWidth}, didOverflowHeight=${layout.didOverflowHeight}, " +
                    "lines=${layout.lineCount}, size=${layout.size}, maxW=${layout.layoutInput.constraints.maxWidth}, " +
                    "fontSize=${layout.layoutInput.style.fontSize}, fontScale=${layout.layoutInput.density.fontScale}, " +
                    "line0W=${layout.getLineRight(0) - layout.getLineLeft(0)}, ellipsized0=${layout.isLineEllipsized(0)})",
                !layout.hasVisualOverflow
            )
            assertTrue(
                "Expected <= 2 lines for '$label', got ${layout.lineCount}",
                layout.lineCount <= 2
            )
        }
    }
}
