package com.restcycle.app.ui.screen

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.R
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepOption
import com.restcycle.app.domain.model.SleepUiState
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.CelestialHeader
import com.restcycle.app.ui.components.CelestialLandscapeScene
import com.restcycle.app.ui.components.HapticTimeWheel
import com.restcycle.app.ui.components.LatencySlider
import com.restcycle.app.ui.components.ModeSegmentedBar
import com.restcycle.app.ui.components.PowerNapInertiaTooltip
import com.restcycle.app.ui.components.PowerNapPresetCard
import com.restcycle.app.ui.components.RestCycleIcons
import com.restcycle.app.ui.components.SettingsDialog
import com.restcycle.app.ui.components.SleepOptionCard
import com.restcycle.app.ui.theme.AppLanguage
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.theme.TimeFormatPreference
import com.restcycle.app.ui.util.formatClockTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale

@Composable
fun RestCycleScreen(
    uiState: SleepUiState,
    currentClockTime: LocalTime,
    onSelectMode: (SleepMode) -> Unit,
    onSelectTime: (LocalTime) -> Unit,
    onUpdateSleepLatency: (Int) -> Unit,
    onUpdateNapLatency: (Int) -> Unit,
    onHighlightOption: (String) -> Unit,
    onLaunchAlarm: (time: LocalTime, message: String) -> Boolean,
    onLaunchTimer: (durationSeconds: Int, message: String) -> Boolean,
    modifier: Modifier = Modifier,
    themeStyle: AppThemeStyle = AppThemeStyle.DYNAMIC,
    timeFormatPreference: TimeFormatPreference = TimeFormatPreference.SYSTEM,
    onSelectThemeStyle: (AppThemeStyle) -> Unit = {},
    onSelectTimeFormatPreference: (TimeFormatPreference) -> Unit = {},
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onSelectAppLanguage: (AppLanguage) -> Unit = {}
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, baseConfiguration, appLanguage) {
        val tag = appLanguage.languageTag
        if (tag == null) {
            baseContext
        } else {
            val locale = Locale.forLanguageTag(tag)
            val config = Configuration(baseConfiguration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            baseContext.createConfigurationContext(config)
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalResources provides localizedContext.resources
    ) {
        RestCycleScreenContent(
            uiState = uiState,
            currentClockTime = currentClockTime,
            onSelectMode = onSelectMode,
            onSelectTime = onSelectTime,
            onUpdateSleepLatency = onUpdateSleepLatency,
            onUpdateNapLatency = onUpdateNapLatency,
            onHighlightOption = onHighlightOption,
            onLaunchAlarm = onLaunchAlarm,
            onLaunchTimer = onLaunchTimer,
            modifier = modifier,
            themeStyle = themeStyle,
            timeFormatPreference = timeFormatPreference,
            onSelectThemeStyle = onSelectThemeStyle,
            onSelectTimeFormatPreference = onSelectTimeFormatPreference,
            appLanguage = appLanguage,
            onSelectAppLanguage = onSelectAppLanguage
        )
    }
}

@Composable
private fun RestCycleScreenContent(
    uiState: SleepUiState,
    currentClockTime: LocalTime,
    onSelectMode: (SleepMode) -> Unit,
    onSelectTime: (LocalTime) -> Unit,
    onUpdateSleepLatency: (Int) -> Unit,
    onUpdateNapLatency: (Int) -> Unit,
    onHighlightOption: (String) -> Unit,
    onLaunchAlarm: (time: LocalTime, message: String) -> Boolean,
    onLaunchTimer: (durationSeconds: Int, message: String) -> Boolean,
    modifier: Modifier,
    themeStyle: AppThemeStyle,
    timeFormatPreference: TimeFormatPreference,
    onSelectThemeStyle: (AppThemeStyle) -> Unit,
    onSelectTimeFormatPreference: (TimeFormatPreference) -> Unit,
    appLanguage: AppLanguage,
    onSelectAppLanguage: (AppLanguage) -> Unit
) {
    val palette = AtmosphereTheme.palette
    val resources = LocalResources.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val clockUnavailableMessage = stringResource(R.string.clock_app_unavailable)

    fun notifyMissingClockIfFailed(launched: Boolean) {
        if (!launched) {
            snackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob = coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = clockUnavailableMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val skyGradient = Brush.verticalGradient(
        0.00f to palette.backgroundGlow,
        0.20f to palette.backgroundTop,
        0.50f to palette.backgroundMid,
        1.00f to palette.backgroundBottom
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skyGradient)
            .testTag(RestCycleTestTags.SCREEN_ROOT)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp)
        ) {
            // Upper ~46% Zone: Celestial Header + Scrollable Result Cards
            Column(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                CelestialHeader(
                    skyPhase = uiState.activeSkyPhase,
                    mode = uiState.mode,
                    onOpenSettings = { showSettings = true }
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (uiState.mode == SleepMode.POWER_NAP) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .verticalScroll(rememberScrollState())
                            .testTag(RestCycleTestTags.UPPER_RESULTS_ZONE)
                            .padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.options.forEach { option ->
                            PowerNapPresetCard(
                                option = option,
                                isHighlighted = option.id == uiState.highlightedOptionId,
                                is24HourFormat = uiState.is24HourFormat,
                                onSelect = { onHighlightOption(option.id) },
                                onSetAlarm = { targetOption ->
                                    val alarmMessage = resources.getString(R.string.alarm_label_nap)
                                    notifyMissingClockIfFailed(
                                        onLaunchAlarm(targetOption.targetTime, alarmMessage)
                                    )
                                },
                                onSetTimer = { targetOption ->
                                    val durationMinutes = targetOption.countdownMinutesFromNow
                                        ?: (uiState.napLatencyMinutes + targetOption.durationMinutes).toLong()
                                    val durationSeconds = (durationMinutes * 60L).toInt()
                                    val timerMessage = resources.getString(targetOption.labelResId)
                                    notifyMissingClockIfFailed(
                                        onLaunchTimer(durationSeconds, timerMessage)
                                    )
                                }
                            )
                        }
                        PowerNapInertiaTooltip()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .testTag(RestCycleTestTags.UPPER_RESULTS_ZONE),
                        contentPadding = PaddingValues(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.options, key = { it.id }) { option ->
                            SleepOptionCard(
                                option = option,
                                isHighlighted = option.id == uiState.highlightedOptionId,
                                is24HourFormat = uiState.is24HourFormat,
                                onSelect = { onHighlightOption(option.id) },
                                onSetAlarm = { targetOption ->
                                    val alarmMessage = resources.getString(
                                        alarmMessageResForMode(uiState.mode)
                                    )
                                    notifyMissingClockIfFailed(
                                        onLaunchAlarm(targetOption.targetTime, alarmMessage)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Bottom ~54% Thumb-Zone Controls (strictly transparent over root sky gradient)
            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .testTag(RestCycleTestTags.THUMB_CONTROLS_ZONE)
                    .padding(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top context action / status pill inside thumb zone
                when (uiState.mode) {
                    SleepMode.WAKE_UP_AT -> {
                        val formattedWake = formatClockTime(uiState.selectedTime, uiState.is24HourFormat)
                        FilledTonalButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                val message = resources.getString(R.string.alarm_label_wakeup)
                                notifyMissingClockIfFailed(onLaunchAlarm(uiState.selectedTime, message))
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = palette.pillContainer,
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .testTag(RestCycleTestTags.WAKE_ALARM_BTN)
                        ) {
                            Icon(
                                imageVector = RestCycleIcons.Alarm,
                                contentDescription = null,
                                tint = palette.accentText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_wake_alarm),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedWake,
                                style = TabularTextStyles.headerLiveClock,
                                color = palette.accentText
                            )
                        }
                    }

                    SleepMode.GO_TO_BED_AT -> {
                        val formattedBedtime = formatClockTime(uiState.selectedTime, uiState.is24HourFormat)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.pillContainer,
                            border = BorderStroke(1.dp, palette.borderSubtle)
                        ) {
                            Text(
                                text = stringResource(R.string.planned_bedtime_label, formattedBedtime),
                                style = TabularTextStyles.headerLiveClock,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> {
                        val formattedNow = formatClockTime(currentClockTime, uiState.is24HourFormat)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.pillContainer,
                            border = BorderStroke(1.dp, palette.borderSubtle)
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_now_current_time_label, formattedNow),
                                style = TabularTextStyles.headerLiveClock,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Middle thumb area: HapticTimeWheel in wheel modes, reactive scenic landscape in live modes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.mode == SleepMode.WAKE_UP_AT || uiState.mode == SleepMode.GO_TO_BED_AT) {
                        HapticTimeWheel(
                            selectedTime = uiState.selectedTime,
                            is24HourFormat = uiState.is24HourFormat,
                            onTimeChange = onSelectTime
                        )
                    } else {
                        val hintRes = if (uiState.mode == SleepMode.SLEEP_NOW) {
                            R.string.sleep_now_hint
                        } else {
                            R.string.power_nap_hint
                        }
                        CelestialLandscapeScene(
                            skyPhase = uiState.activeSkyPhase,
                            hintText = stringResource(id = hintRes),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Latency slider + Bottom 4-mode segmented switcher
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.mode == SleepMode.POWER_NAP) {
                        LatencySlider(
                            labelResId = R.string.latency_nap_label,
                            minutes = uiState.napLatencyMinutes,
                            maxMinutes = SleepCycleCalculator.MAX_NAP_LATENCY_MINUTES,
                            onLatencyChange = onUpdateNapLatency
                        )
                    } else {
                        LatencySlider(
                            labelResId = R.string.latency_sleep_label,
                            minutes = uiState.sleepLatencyMinutes,
                            maxMinutes = SleepCycleCalculator.MAX_SLEEP_LATENCY_MINUTES,
                            onLatencyChange = onUpdateSleepLatency
                        )
                    }

                    ModeSegmentedBar(
                        selectedMode = uiState.mode,
                        onModeSelected = onSelectMode
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 64.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = palette.cardSurfaceHighlighted,
                contentColor = palette.textPrimary
            )
        }

        if (showSettings) {
            SettingsDialog(
                themeStyle = themeStyle,
                timeFormatPreference = timeFormatPreference,
                onSelectTheme = onSelectThemeStyle,
                onSelectTimeFormat = onSelectTimeFormatPreference,
                onDismiss = { showSettings = false },
                appLanguage = appLanguage,
                onSelectLanguage = onSelectAppLanguage
            )
        }
    }
}

private fun alarmMessageResForMode(mode: SleepMode): Int = when (mode) {
    SleepMode.WAKE_UP_AT -> R.string.alarm_label_bedtime
    SleepMode.SLEEP_NOW, SleepMode.GO_TO_BED_AT -> R.string.alarm_label_wakeup
    SleepMode.POWER_NAP -> R.string.alarm_label_nap
}
