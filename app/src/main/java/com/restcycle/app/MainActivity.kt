package com.restcycle.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.platform.widget.RestCycleWidgetProvider
import com.restcycle.app.ui.screen.RestCycleScreen
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.viewmodel.SleepViewModel
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: SleepViewModel by viewModels {
        (application as RestCycleApplication).container.sleepViewModelFactory
    }
    private var openBreathingTrigger by mutableIntStateOf(0)
    private var backupSnackbarEvent by mutableStateOf<Pair<Int, String>?>(null)

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val json = viewModel.exportSleepHistoryJson()
        val count = viewModel.sleepHistory.value.size
        val saved = runCatching {
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(json)
                }
            } ?: error("Cannot open output stream")
        }.isSuccess
        val ctx = resolveLocalizedContext()
        val message = if (saved) {
            ctx.getString(R.string.backup_export_success, count)
        } else {
            ctx.getString(R.string.backup_error)
        }
        val nextId = (backupSnackbarEvent?.first ?: 0) + 1
        backupSnackbarEvent = nextId to message
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val counts = runCatching {
            val raw = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: error("Cannot open input stream")
            viewModel.importSleepHistoryJson(raw)
        }.getOrNull()
        val ctx = resolveLocalizedContext()
        val message = if (counts != null) {
            ctx.getString(R.string.backup_import_success, counts.first, counts.second)
        } else {
            ctx.getString(R.string.backup_error)
        }
        val nextId = (backupSnackbarEvent?.first ?: 0) + 1
        backupSnackbarEvent = nextId to message
    }

    private fun resolveLocalizedContext(): Context {
        val tag = viewModel.appLanguage.value.languageTag ?: return this
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleShortcutIntent(intent)

        val container = (application as RestCycleApplication).container

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
            val timeFormatPref by viewModel.timeFormatPreference.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val pureAmoledNight by viewModel.pureAmoledNight.collectAsStateWithLifecycle()
            val caffeineCutoffHours by viewModel.caffeineCutoffHours.collectAsStateWithLifecycle()
            val bedtimeReminderEnabled by viewModel.bedtimeReminderEnabled.collectAsStateWithLifecycle()
            val activeSleepStartMillis by viewModel.activeSleepStartMillis.collectAsStateWithLifecycle()
            val sleepHistory by viewModel.sleepHistory.collectAsStateWithLifecycle()
            val savedSoundMixes by viewModel.savedSoundMixes.collectAsStateWithLifecycle()
            val liveClockTime by viewModel.clockTime.collectAsStateWithLifecycle()
            val syncedClockTime = uiState.options.firstOrNull()
                ?.takeIf {
                    (uiState.mode == SleepMode.SLEEP_NOW || uiState.mode == SleepMode.POWER_NAP) &&
                        it.countdownMinutesFromNow != null
                }
                ?.let { option ->
                    option.targetTime.minusMinutes(option.countdownMinutesFromNow!!)
                }
                ?: liveClockTime
            AtmosphereTheme(
                skyPhase = uiState.activeSkyPhase,
                themeStyle = themeStyle,
                pureAmoledNight = pureAmoledNight
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
                    onSelectTimeFormatPreference = viewModel::selectTimeFormatPreference,
                    appLanguage = appLanguage,
                    onSelectAppLanguage = viewModel::selectAppLanguage,
                    pureAmoledNight = pureAmoledNight,
                    onTogglePureAmoledNight = viewModel::setPureAmoledNight,
                    caffeineCutoffHours = caffeineCutoffHours,
                    onSelectCaffeineCutoffHours = viewModel::setCaffeineCutoffHours,
                    bedtimeReminderEnabled = bedtimeReminderEnabled,
                    onToggleBedtimeReminder = viewModel::setBedtimeReminderEnabled,
                    activeSleepStartMillis = activeSleepStartMillis,
                    sleepHistory = sleepHistory,
                    onStartSleepSession = viewModel::startSleepSession,
                    onCompleteSleepSession = { mood -> viewModel.completeSleepSession(mood) },
                    onCompleteSleepSessionWithTags = { mood, tags -> viewModel.completeSleepSession(mood, tags) },
                    onCancelSleepSession = viewModel::cancelSleepSession,
                    onDeleteSleepRecord = viewModel::deleteSleepRecord,
                    onAddManualSleep = { start, end, mood -> viewModel.addManualSleepSession(start, end, mood) },
                    onAddManualSleepWithTags = { start, end, mood, tags ->
                        viewModel.addManualSleepSession(start, end, mood, tags)
                    },
                    savedSoundMixes = savedSoundMixes,
                    onSaveSoundMix = viewModel::saveSoundMix,
                    onDeleteSoundMix = viewModel::deleteSoundMix,
                    onExportBackup = {
                        exportBackupLauncher.launch("restcycle-backup-${LocalDate.now()}.json")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    backupStatusEvent = backupSnackbarEvent,
                    openBreathingTrigger = openBreathingTrigger
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_SHORTCUT_ACTION)) {
            SHORTCUT_SLEEP_NOW -> viewModel.selectMode(SleepMode.SLEEP_NOW)
            SHORTCUT_POWER_NAP -> viewModel.selectMode(SleepMode.POWER_NAP)
            SHORTCUT_BREATHE -> {
                viewModel.selectMode(SleepMode.SLEEP_NOW)
                openBreathingTrigger += 1
            }
        }
    }

    companion object {
        const val EXTRA_SHORTCUT_ACTION = "extra_shortcut_action"
        const val SHORTCUT_SLEEP_NOW = "SLEEP_NOW"
        const val SHORTCUT_POWER_NAP = "POWER_NAP"
        const val SHORTCUT_BREATHE = "BREATHE"
    }

    override fun onResume() {
        super.onResume()
        val container = (application as RestCycleApplication).container
        viewModel.refreshTimeFormat(container.timeFormatProvider.is24HourFormat())
        viewModel.refreshClockNow()
        val currentState = viewModel.uiState.value
        RestCycleWidgetProvider.refreshAllWidgets(
            context = this,
            sleepLatencyMinutes = currentState.sleepLatencyMinutes,
            is24HourFormat = currentState.is24HourFormat
        )
    }
}
