package com.restcycle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.screen.RestCycleScreen
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.viewmodel.SleepViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SleepViewModel by viewModels {
        (application as RestCycleApplication).container.sleepViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as RestCycleApplication).container

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
            val timeFormatPref by viewModel.timeFormatPreference.collectAsStateWithLifecycle()
            val syncedClockTime = uiState.options.firstOrNull()
                ?.takeIf {
                    (uiState.mode == SleepMode.SLEEP_NOW || uiState.mode == SleepMode.POWER_NAP) &&
                        it.countdownMinutesFromNow != null
                }
                ?.let { option ->
                    option.targetTime.minusMinutes(option.countdownMinutesFromNow!!)
                }
                ?: container.timeFormatProvider.currentTime()
            AtmosphereTheme(
                skyPhase = uiState.activeSkyPhase,
                themeStyle = themeStyle
            ) {
                RestCycleScreen(
                    uiState = uiState,
                    currentClockTime = syncedClockTime,
                    onSelectMode = viewModel::selectMode,
                    onSelectTime = viewModel::selectTime,
                    onUpdateSleepLatency = viewModel::updateSleepLatency,
                    onUpdateNapLatency = viewModel::updateNapLatency,
                    onHighlightOption = viewModel::highlightOption,
                    onLaunchAlarm = { time, message ->
                        container.alarmScheduler.launchSetAlarm(this, time, message)
                    },
                    onLaunchTimer = { durationSeconds, message ->
                        container.alarmScheduler.launchSetTimer(this, durationSeconds, message)
                    },
                    themeStyle = themeStyle,
                    timeFormatPreference = timeFormatPref,
                    onSelectThemeStyle = viewModel::selectThemeStyle,
                    onSelectTimeFormatPreference = viewModel::selectTimeFormatPreference
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as RestCycleApplication).container
        viewModel.refreshTimeFormat(container.timeFormatProvider.is24HourFormat())
        viewModel.refreshClockNow()
    }
}
