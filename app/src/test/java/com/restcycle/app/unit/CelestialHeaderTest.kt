package com.restcycle.app.unit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.R
import com.restcycle.app.RestCycleApplication
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.CelestialHeader
import com.restcycle.app.ui.theme.AtmosphereTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class CelestialHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun celestialHeader_displaysTagAndLocalizedContentDescriptionPerSkyPhase() {
        val context = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        var phase by mutableStateOf(SkyPhase.DAY)

        composeRule.setContent {
            AtmosphereTheme(skyPhase = phase) {
                CelestialHeader(skyPhase = phase, mode = SleepMode.WAKE_UP_AT)
            }
        }

        composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
            .assertIsDisplayed()
            .assertContentDescriptionContains(context.getString(R.string.cd_celestial_day))

        composeRule.runOnUiThread { phase = SkyPhase.DAWN_DUSK }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
            .assertContentDescriptionContains(context.getString(R.string.cd_celestial_dawn_dusk))

        composeRule.runOnUiThread { phase = SkyPhase.NIGHT }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER)
            .assertContentDescriptionContains(context.getString(R.string.cd_celestial_night))
    }
}
