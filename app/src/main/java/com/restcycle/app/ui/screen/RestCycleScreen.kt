package com.restcycle.app.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restcycle.app.R
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepOption
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.domain.model.SleepUiState
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.platform.audio.AmbientSoundService
import com.restcycle.app.platform.audio.AmbientSoundStateHolder
import com.restcycle.app.platform.reminder.BedtimeReminderScheduler
import com.restcycle.app.platform.update.GitHubUpdateChecker
import com.restcycle.app.platform.update.UpdateUiState
import com.restcycle.app.platform.widget.RestCycleWidgetProvider
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.components.AmbientSoundDialog
import com.restcycle.app.ui.components.BreathingExerciseDialog
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
import com.restcycle.app.ui.components.SleepTrackerDialog
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
    themeStyle: AppThemeStyle = AppThemeStyle.TOKYO_NIGHT,
    timeFormatPreference: TimeFormatPreference = TimeFormatPreference.SYSTEM,
    onSelectThemeStyle: (AppThemeStyle) -> Unit = {},
    onSelectTimeFormatPreference: (TimeFormatPreference) -> Unit = {},
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onSelectAppLanguage: (AppLanguage) -> Unit = {},
    pureAmoledNight: Boolean = false,
    onTogglePureAmoledNight: (Boolean) -> Unit = {},
    caffeineCutoffHours: Int = 8,
    onSelectCaffeineCutoffHours: (Int) -> Unit = {},
    bedtimeReminderEnabled: Boolean = false,
    onToggleBedtimeReminder: (Boolean) -> Unit = {},
    activeSleepStartMillis: Long = 0L,
    sleepHistory: List<SleepSessionRecord> = emptyList(),
    onStartSleepSession: () -> Unit = {},
    onCompleteSleepSession: (SleepQualityMood) -> Unit = {},
    onCancelSleepSession: () -> Unit = {},
    onDeleteSleepRecord: (SleepSessionRecord) -> Unit = {},
    onAddManualSleep: (startEpochMillis: Long, endEpochMillis: Long, mood: SleepQualityMood) -> Unit = { _, _, _ -> },
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    backupStatusEvent: Pair<Int, String>? = null,
    openBreathingTrigger: Int = 0
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
        val palette = AtmosphereTheme.palette
        val context = LocalContext.current
        val resources = LocalResources.current
        val view = LocalView.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var snackbarJob by remember { mutableStateOf<Job?>(null) }
        var showSettings by remember { mutableStateOf(false) }
        var showBreathing by remember { mutableStateOf(false) }
        var showSleepTracker by remember { mutableStateOf(false) }
        var showSoundPicker by remember { mutableStateOf(false) }
        val ambientPlaybackState by AmbientSoundStateHolder.state.collectAsStateWithLifecycle()
        val ambientSoundMode = ambientPlaybackState.mode
        var updateUiState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
        val installedVersionName = remember(context) { resolveInstalledVersionName(context) }
        val clockUnavailableMessage = stringResource(R.string.clock_app_unavailable)

        LaunchedEffect(backupStatusEvent?.first) {
            val msg = backupStatusEvent?.second ?: return@LaunchedEffect
            snackbarJob?.cancel()
            snackbarJob = coroutineScope.launch {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }

        LaunchedEffect(openBreathingTrigger) {
            if (openBreathingTrigger > 0) {
                showBreathing = true
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val latestActiveSleepStart by rememberUpdatedState(activeSleepStartMillis)
        var pausedWhileSleeping by remember { mutableStateOf(false) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (latestActiveSleepStart > 0L) {
                            pausedWhileSleeping = true
                        }
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        val activeStart = latestActiveSleepStart
                        if (pausedWhileSleeping &&
                            activeStart > 0L &&
                            (System.currentTimeMillis() - activeStart) >= AUTO_WAKE_PROMPT_MIN_ELAPSED_MS
                        ) {
                            showSleepTracker = true
                        }
                        pausedWhileSleeping = false
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val activeBedtimeForHelpers: LocalTime? = when (uiState.mode) {
            SleepMode.WAKE_UP_AT -> uiState.options.firstOrNull { it.id == uiState.highlightedOptionId }?.targetTime
                ?: uiState.options.firstOrNull()?.targetTime
            SleepMode.GO_TO_BED_AT -> uiState.selectedTime
            SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> null
        }

        LaunchedEffect(bedtimeReminderEnabled, activeBedtimeForHelpers, uiState.is24HourFormat) {
            if (bedtimeReminderEnabled && activeBedtimeForHelpers != null) {
                val formatted = formatClockTime(activeBedtimeForHelpers, uiState.is24HourFormat)
                BedtimeReminderScheduler.scheduleReminder(
                    context = baseContext,
                    bedtime = activeBedtimeForHelpers,
                    formattedBedtime = formatted
                )
            } else if (!bedtimeReminderEnabled) {
                BedtimeReminderScheduler.cancelReminder(baseContext)
            }
        }

        LaunchedEffect(uiState.sleepLatencyMinutes, uiState.is24HourFormat) {
            runCatching {
                RestCycleWidgetProvider.refreshAllWidgets(
                    context = baseContext,
                    sleepLatencyMinutes = uiState.sleepLatencyMinutes,
                    is24HourFormat = uiState.is24HourFormat
                )
            }
        }

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
                            val untilBedtimeMinutes = if (uiState.mode == SleepMode.WAKE_UP_AT) {
                                val nowMin = currentClockTime.hour * 60 + currentClockTime.minute
                                val targetMin = option.targetTime.hour * 60 + option.targetTime.minute
                                ((targetMin - nowMin + 1440) % 1440).toLong()
                            } else {
                                null
                            }
                            SleepOptionCard(
                                option = option,
                                isHighlighted = option.id == uiState.highlightedOptionId,
                                is24HourFormat = uiState.is24HourFormat,
                                untilBedtimeMinutes = untilBedtimeMinutes,
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
                // Top context action / status pill (+ Caffeine Cutoff, Sleep Tracker & History pills) inside thumb zone
                val isSleeping = activeSleepStartMillis > 0L
                val onSleepActionClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (isSleeping) {
                        showSleepTracker = true
                    } else {
                        onStartSleepSession()
                    }
                }
                when (uiState.mode) {
                    SleepMode.WAKE_UP_AT -> {
                        val formattedWake = formatClockTime(uiState.selectedTime, uiState.is24HourFormat)
                        val caffeineCutoffTime = activeBedtimeForHelpers
                            ?.takeIf { caffeineCutoffHours > 0 }
                            ?.minusHours(caffeineCutoffHours.toLong())
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    val message = resources.getString(R.string.alarm_label_wakeup)
                                    notifyMissingClockIfFailed(onLaunchAlarm(uiState.selectedTime, message))
                                },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedWake,
                                    style = TabularTextStyles.headerLiveClock,
                                    color = palette.accentText
                                )
                            }
                            if (caffeineCutoffTime != null) {
                                CaffeineCutoffPill(
                                    cutoffTime = caffeineCutoffTime,
                                    is24HourFormat = uiState.is24HourFormat
                                )
                            }
                            SleepSessionPill(
                                isSleeping = isSleeping,
                                compact = true,
                                onClick = onSleepActionClick
                            )
                            SleepHistoryPill(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showSleepTracker = true
                                }
                            )
                            SoundPickerPill(
                                ambientSoundMode = ambientSoundMode,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showSoundPicker = true
                                }
                            )
                        }
                    }

                    SleepMode.GO_TO_BED_AT -> {
                        val formattedBedtime = formatClockTime(uiState.selectedTime, uiState.is24HourFormat)
                        val caffeineCutoffTime = uiState.selectedTime
                            .takeIf { caffeineCutoffHours > 0 }
                            ?.minusHours(caffeineCutoffHours.toLong())
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.pillContainer,
                                border = BorderStroke(1.dp, palette.borderSubtle)
                            ) {
                                Text(
                                    text = stringResource(R.string.planned_bedtime_label, formattedBedtime),
                                    style = TabularTextStyles.headerLiveClock,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            if (caffeineCutoffTime != null) {
                                CaffeineCutoffPill(
                                    cutoffTime = caffeineCutoffTime,
                                    is24HourFormat = uiState.is24HourFormat
                                )
                            }
                            SleepSessionPill(
                                isSleeping = isSleeping,
                                compact = true,
                                onClick = onSleepActionClick
                            )
                            SleepHistoryPill(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showSleepTracker = true
                                }
                            )
                            SoundPickerPill(
                                ambientSoundMode = ambientSoundMode,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showSoundPicker = true
                                }
                            )
                        }
                    }

                    SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> {
                        val formattedNow = formatClockTime(currentClockTime, uiState.is24HourFormat)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.pillContainer,
                                border = BorderStroke(1.dp, palette.borderSubtle)
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_now_current_time_label, formattedNow),
                                    style = TabularTextStyles.headerLiveClock,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            SleepSessionPill(
                                isSleeping = isSleeping,
                                compact = false,
                                onClick = onSleepActionClick
                            )
                            SleepHistoryPill(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showSleepTracker = true
                                }
                            )
                            SoundPickerPill(
                                ambientSoundMode = ambientSoundMode,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showSoundPicker = true
                                }
                            )
                        }
                    }
                }

                // Middle thumb area: HapticTimeWheel + Quick Chips in wheel modes, reactive scenic landscape in live modes
                val modeTransition = remember { Animatable(1f) }
                var previousMiddleMode by remember { mutableStateOf(uiState.mode) }
                LaunchedEffect(uiState.mode) {
                    if (previousMiddleMode != uiState.mode) {
                        previousMiddleMode = uiState.mode
                        modeTransition.snapTo(0f)
                        modeTransition.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = modeTransition.value
                            alpha = 0.25f + 0.75f * p
                            val s = 0.96f + 0.04f * p
                            scaleX = s
                            scaleY = s
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.mode == SleepMode.WAKE_UP_AT || uiState.mode == SleepMode.GO_TO_BED_AT) {
                        val compactWheel = maxHeight < 170.dp
                        val wheelItemHeight = if (compactWheel) 34.dp else 42.dp
                        val presets = if (uiState.mode == SleepMode.WAKE_UP_AT) {
                            WAKE_UP_PRESET_TIMES
                        } else {
                            GO_TO_BED_PRESET_TIMES
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (compactWheel) 4.dp else 8.dp)
                        ) {
                            HapticTimeWheel(
                                selectedTime = uiState.selectedTime,
                                is24HourFormat = uiState.is24HourFormat,
                                onTimeChange = onSelectTime,
                                itemHeight = wheelItemHeight
                            )
                            QuickTimePresetRow(
                                presets = presets,
                                selectedTime = uiState.selectedTime,
                                is24HourFormat = uiState.is24HourFormat,
                                onSelectPreset = { preset ->
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onSelectTime(preset)
                                }
                            )
                        }
                    } else {
                        val hintRes = if (uiState.mode == SleepMode.SLEEP_NOW) {
                            R.string.sleep_now_hint
                        } else {
                            R.string.power_nap_hint
                        }
                        CelestialLandscapeScene(
                            skyPhase = uiState.activeSkyPhase,
                            hintText = stringResource(id = hintRes),
                            onOpenBreathing = { showBreathing = true },
                            ambientSoundMode = ambientSoundMode,
                            onCycleAmbientSound = { showSoundPicker = true },
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
                onSelectLanguage = onSelectAppLanguage,
                pureAmoledNight = pureAmoledNight,
                onTogglePureAmoledNight = onTogglePureAmoledNight,
                caffeineCutoffHours = caffeineCutoffHours,
                onSelectCaffeineCutoffHours = onSelectCaffeineCutoffHours,
                bedtimeReminderEnabled = bedtimeReminderEnabled,
                onToggleBedtimeReminder = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            baseContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            (baseContext as? Activity)?.let { activity ->
                                ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    REQ_POST_NOTIFICATIONS
                                )
                            }
                        }
                    }
                    onToggleBedtimeReminder(enabled)
                },
                installedVersionName = installedVersionName,
                updateUiState = updateUiState,
                onCheckForUpdates = {
                    if (updateUiState !is UpdateUiState.Checking) {
                        updateUiState = UpdateUiState.Checking
                        coroutineScope.launch {
                            updateUiState = GitHubUpdateChecker.checkForUpdate(installedVersionName)
                        }
                    }
                },
                onDownloadUpdate = { releaseInfo ->
                    updateUiState = UpdateUiState.Downloading(releaseInfo)
                    GitHubUpdateChecker.downloadUpdateApk(context, releaseInfo)
                },
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup
            )
        }

        if (showBreathing) {
            BreathingExerciseDialog(
                onDismiss = { showBreathing = false },
                ambientSoundMode = ambientSoundMode,
                onCycleAmbientSound = { showSoundPicker = true }
            )
        }

        if (showSoundPicker) {
            AmbientSoundDialog(
                playbackState = ambientPlaybackState,
                onSelectSound = { mode ->
                    if (mode == AmbientSoundMode.OFF) {
                        AmbientSoundService.stop(baseContext)
                    } else {
                        AmbientSoundService.play(
                            context = baseContext,
                            mode = mode,
                            volume = ambientPlaybackState.volume,
                            timerMinutes = ambientPlaybackState.sleepTimerMinutes
                        )
                    }
                },
                onVolumeChange = { vol ->
                    AmbientSoundService.updateVolume(baseContext, vol)
                },
                onSelectTimerMinutes = { mins ->
                    AmbientSoundService.updateTimer(baseContext, mins)
                },
                onDismiss = { showSoundPicker = false }
            )
        }

        if (showSleepTracker) {
            SleepTrackerDialog(
                activeSleepStartMillis = activeSleepStartMillis,
                sleepHistory = sleepHistory,
                is24HourFormat = uiState.is24HourFormat,
                onStartSleep = {
                    onStartSleepSession()
                },
                onCompleteSleepWithMood = { mood ->
                    onCompleteSleepSession(mood)
                    showSleepTracker = false
                },
                onCancelSleep = {
                    onCancelSleepSession()
                },
                onDeleteRecord = onDeleteSleepRecord,
                sleepLatencyMinutes = uiState.sleepLatencyMinutes,
                onAddManualSleep = onAddManualSleep,
                onDismiss = { showSleepTracker = false }
            )
        }
    }
    }
}

private const val REQ_POST_NOTIFICATIONS = 5211
private const val AUTO_WAKE_PROMPT_MIN_ELAPSED_MS = 20L * 60_000L

@Composable
private fun SleepSessionPill(
    isSleeping: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val icon = if (isSleeping) RestCycleIcons.Sun else RestCycleIcons.Moon
    val actionLabel = stringResource(if (isSleeping) R.string.action_woke_up else R.string.action_start_sleep)
    val contentColor = if (isSleeping) palette.onAccent else palette.textPrimary
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (isSleeping) palette.accent else palette.pillContainer,
        border = BorderStroke(1.dp, if (isSleeping) palette.accent else palette.borderSubtle),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(RestCycleTestTags.SLEEP_TRACKER_BUTTON)
    ) {
        if (compact) {
            Icon(
                imageVector = icon,
                contentDescription = actionLabel,
                tint = contentColor,
                modifier = Modifier
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .size(15.dp)
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SleepHistoryPill(
    onClick: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(RestCycleTestTags.SLEEP_HISTORY_BUTTON)
    ) {
        Icon(
            imageVector = RestCycleIcons.Calendar,
            contentDescription = stringResource(R.string.sleep_history_title),
            tint = palette.textPrimary,
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .size(15.dp)
        )
    }
}

@Composable
private fun SoundPickerPill(
    ambientSoundMode: AmbientSoundMode,
    onClick: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val active = ambientSoundMode != AmbientSoundMode.OFF
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (active) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
        border = BorderStroke(1.dp, if (active) palette.accent else palette.borderSubtle),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(RestCycleTestTags.SOUND_PICKER_PILL)
    ) {
        Icon(
            imageVector = if (active) RestCycleIcons.forAmbientSound(ambientSoundMode) else RestCycleIcons.AudioWaveform,
            contentDescription = stringResource(R.string.sound_dialog_title),
            tint = if (active) palette.accentText else palette.textPrimary,
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .size(15.dp)
        )
    }
}

@Composable
private fun CaffeineCutoffPill(
    cutoffTime: LocalTime,
    is24HourFormat: Boolean
) {
    val palette = AtmosphereTheme.palette
    val formatted = formatClockTime(cutoffTime, is24HourFormat)
    Surface(
        shape = RoundedCornerShape(50),
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .semantics(mergeDescendants = true) {}
            .testTag(RestCycleTestTags.CAFFEINE_CUTOFF_PILL)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = RestCycleIcons.Coffee,
                contentDescription = stringResource(R.string.settings_caffeine_label),
                tint = palette.textSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stringResource(R.string.caffeine_cutoff_label, formatted),
                style = TabularTextStyles.headerLiveClock,
                color = palette.textSecondary,
                maxLines = 1
            )
        }
    }
}

private val WAKE_UP_PRESET_TIMES = listOf(
    LocalTime.of(6, 0),
    LocalTime.of(6, 30),
    LocalTime.of(7, 0),
    LocalTime.of(7, 30)
)

private val GO_TO_BED_PRESET_TIMES = listOf(
    LocalTime.of(22, 0),
    LocalTime.of(22, 30),
    LocalTime.of(23, 0),
    LocalTime.of(0, 0)
)

@Composable
private fun QuickTimePresetRow(
    presets: List<LocalTime>,
    selectedTime: LocalTime,
    is24HourFormat: Boolean,
    onSelectPreset: (LocalTime) -> Unit
) {
    val palette = AtmosphereTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(RestCycleTestTags.QUICK_TIME_CHIPS_ROW),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val isSelected = selectedTime == preset
            val key = String.format(Locale.US, "%02d%02d", preset.hour, preset.minute)
            val label = formatClockTime(preset, is24HourFormat)
            val shape = RoundedCornerShape(50)
            Surface(
                shape = shape,
                color = if (isSelected) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) palette.accent else palette.borderSubtle
                ),
                modifier = Modifier
                    .clip(shape)
                    .clickable { onSelectPreset(preset) }
                    .testTag(RestCycleTestTags.quickTimeChip(key))
            ) {
                Text(
                    text = label,
                    style = TabularTextStyles.cardDuration,
                    color = if (isSelected) palette.accentText else palette.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                )
            }
        }
    }
}

private fun alarmMessageResForMode(mode: SleepMode): Int = when (mode) {
    SleepMode.WAKE_UP_AT -> R.string.alarm_label_bedtime
    SleepMode.SLEEP_NOW, SleepMode.GO_TO_BED_AT -> R.string.alarm_label_wakeup
    SleepMode.POWER_NAP -> R.string.alarm_label_nap
}

private fun resolveInstalledVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "1.7.2"
    } catch (_: Exception) {
        "1.7.2"
    }
}
