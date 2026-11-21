package com.restcycle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restcycle.app.R
import com.restcycle.app.data.preferences.LatencyPreferencesRepository
import com.restcycle.app.domain.calculator.SkyPhaseResolver
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.calculator.SleepOptionLabels
import com.restcycle.app.domain.model.SleepHistoryCodec
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.domain.model.SleepUiState
import com.restcycle.app.platform.time.AndroidTimeFormatProvider
import com.restcycle.app.ui.theme.AppLanguage
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.TimeFormatPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

class SleepViewModel(
    private val repository: LatencyPreferencesRepository,
    private val calculator: SleepCycleCalculator = SleepCycleCalculator(),
    private val skyPhaseResolver: SkyPhaseResolver = SkyPhaseResolver(calculator),
    private val labels: SleepOptionLabels = defaultSleepOptionLabels(),
    initialIs24HourFormat: Boolean = true,
    private val nowProvider: () -> LocalTime = { LocalTime.now() },
    private val clockTicker: Flow<LocalTime> = minuteAlignedClockFlow(nowProvider)
) : ViewModel() {

    private var currentMode: SleepMode = SleepMode.WAKE_UP_AT
    private var wakeUpSelectedTime: LocalTime = DEFAULT_WAKE_UP_TIME
    private var goToBedSelectedTime: LocalTime = DEFAULT_GO_TO_BED_TIME
    private var sleepLatencyMinutes: Int = DEFAULT_SLEEP_LATENCY
    private var napLatencyMinutes: Int = DEFAULT_NAP_LATENCY
    private var currentClockTime: LocalTime = nowProvider().truncatedTo(ChronoUnit.MINUTES)
    private var explicitHighlightedOptionId: String? = null
    private var systemIs24HourFormat: Boolean = initialIs24HourFormat
    private var currentTimeFormatPref: TimeFormatPreference = TimeFormatPreference.SYSTEM
    private var is24HourFormat: Boolean = initialIs24HourFormat

    internal val sleepWritesInFlight = AtomicInteger(0)
    internal val napWritesInFlight = AtomicInteger(0)
    private val genericWritesInFlight = AtomicInteger(0)

    private var hasUserSelectedModeInSession = false
    private var hasUserSelectedWakeTimeInSession = false
    private var hasUserSelectedBedTimeInSession = false

    private val _clockTime = MutableStateFlow(currentClockTime)
    val clockTime: StateFlow<LocalTime> = _clockTime.asStateFlow()

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    private val _themeStyle = MutableStateFlow(AppThemeStyle.TOKYO_NIGHT)
    val themeStyle: StateFlow<AppThemeStyle> = _themeStyle.asStateFlow()

    private val _timeFormatPreference = MutableStateFlow(TimeFormatPreference.SYSTEM)
    val timeFormatPreference: StateFlow<TimeFormatPreference> = _timeFormatPreference.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _pureAmoledNight = MutableStateFlow(false)
    val pureAmoledNight: StateFlow<Boolean> = _pureAmoledNight.asStateFlow()

    private val _caffeineCutoffHours = MutableStateFlow(LatencyPreferencesRepository.DEFAULT_CAFFEINE_CUTOFF_HOURS)
    val caffeineCutoffHours: StateFlow<Int> = _caffeineCutoffHours.asStateFlow()

    private val _bedtimeReminderEnabled = MutableStateFlow(false)
    val bedtimeReminderEnabled: StateFlow<Boolean> = _bedtimeReminderEnabled.asStateFlow()

    private val _activeSleepStartMillis = MutableStateFlow(0L)
    val activeSleepStartMillis: StateFlow<Long> = _activeSleepStartMillis.asStateFlow()

    private val _sleepHistory = MutableStateFlow<List<SleepSessionRecord>>(emptyList())
    val sleepHistory: StateFlow<List<SleepSessionRecord>> = _sleepHistory.asStateFlow()

    init {
        collectPref(repository.sleepLatencyMinutes, sleepWritesInFlight) { stored ->
            val clamped = stored.coerceIn(MIN_LATENCY, MAX_SLEEP_LATENCY)
            if (clamped != sleepLatencyMinutes) {
                sleepLatencyMinutes = clamped
                emitState()
            }
        }
        collectPref(repository.napLatencyMinutes, napWritesInFlight) { stored ->
            val clamped = stored.coerceIn(MIN_LATENCY, MAX_NAP_LATENCY)
            if (clamped != napLatencyMinutes) {
                napLatencyMinutes = clamped
                emitState()
            }
        }
        collectPref(repository.wakeUpTimeMinutes, genericWritesInFlight) { storedMinutes ->
            if (!hasUserSelectedWakeTimeInSession) {
                val restored = minutesOfDayToTime(storedMinutes)
                if (restored != wakeUpSelectedTime) {
                    wakeUpSelectedTime = restored
                    emitState()
                }
            }
        }
        collectPref(repository.goToBedTimeMinutes, genericWritesInFlight) { storedMinutes ->
            if (!hasUserSelectedBedTimeInSession) {
                val restored = minutesOfDayToTime(storedMinutes)
                if (restored != goToBedSelectedTime) {
                    goToBedSelectedTime = restored
                    emitState()
                }
            }
        }
        collectPref(repository.lastWheelMode, genericWritesInFlight) { storedMode ->
            if (!hasUserSelectedModeInSession &&
                (storedMode == SleepMode.WAKE_UP_AT || storedMode == SleepMode.GO_TO_BED_AT) &&
                storedMode != currentMode
            ) {
                currentMode = storedMode
                explicitHighlightedOptionId = null
                emitState()
            }
        }
        collectPref(repository.themeStyle, genericWritesInFlight) { _themeStyle.value = it }
        collectPref(repository.appLanguage, genericWritesInFlight) { _appLanguage.value = it }
        collectPref(repository.pureAmoledNight, genericWritesInFlight) { _pureAmoledNight.value = it }
        collectPref(repository.caffeineCutoffHours, genericWritesInFlight) { _caffeineCutoffHours.value = it }
        collectPref(repository.bedtimeReminderEnabled, genericWritesInFlight) { _bedtimeReminderEnabled.value = it }
        collectPref(repository.activeSleepStartMillis, genericWritesInFlight) { _activeSleepStartMillis.value = it }
        collectPref(repository.sleepHistory, genericWritesInFlight) { _sleepHistory.value = it }
        collectPref(repository.timeFormatPreference, genericWritesInFlight) { stored ->
            if (stored != currentTimeFormatPref) {
                currentTimeFormatPref = stored
                _timeFormatPreference.value = stored
                val effective24h = resolveIs24Hour(systemIs24HourFormat, stored)
                if (effective24h != is24HourFormat) {
                    is24HourFormat = effective24h
                    emitState()
                }
            }
        }

        viewModelScope.launch {
            clockTicker.collect { tickedTime ->
                val truncated = tickedTime.truncatedTo(ChronoUnit.MINUTES)
                if (truncated != currentClockTime) {
                    currentClockTime = truncated
                    _clockTime.value = truncated
                    if (currentMode == SleepMode.SLEEP_NOW || currentMode == SleepMode.POWER_NAP) {
                        emitState()
                    }
                }
            }
        }
    }

    private fun <T> collectPref(flow: Flow<T>, counter: AtomicInteger, onValue: (T) -> Unit) {
        viewModelScope.launch {
            flow.distinctUntilChanged().collect { stored ->
                if (counter.get() == 0) onValue(stored)
            }
        }
    }

    private fun persistPref(counter: AtomicInteger = genericWritesInFlight, write: suspend () -> Unit) {
        counter.incrementAndGet()
        viewModelScope.launch {
            try {
                write()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                counter.decrementAndGet()
            }
        }
    }

    private fun minutesOfDayToTime(minutes: Int): LocalTime {
        val clamped = minutes.coerceIn(
            LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY,
            LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY
        )
        return LocalTime.of(clamped / 60, clamped % 60)
    }

    fun selectMode(mode: SleepMode) {
        hasUserSelectedModeInSession = true
        currentClockTime = nowProvider().truncatedTo(ChronoUnit.MINUTES)
        _clockTime.value = currentClockTime
        if (currentMode == mode) {
            emitState()
            return
        }
        currentMode = mode
        explicitHighlightedOptionId = null
        emitState()
        if (mode == SleepMode.WAKE_UP_AT || mode == SleepMode.GO_TO_BED_AT) {
            persistPref { repository.setLastWheelMode(mode) }
        }
    }

    fun selectTime(time: LocalTime) {
        val truncated = time.truncatedTo(ChronoUnit.MINUTES)
        val minutesOfDay = truncated.hour * 60 + truncated.minute
        when (currentMode) {
            SleepMode.WAKE_UP_AT -> {
                hasUserSelectedWakeTimeInSession = true
                if (truncated == wakeUpSelectedTime) return
                wakeUpSelectedTime = truncated
                emitState()
                persistPref { repository.setWakeUpTimeMinutes(minutesOfDay) }
            }
            SleepMode.GO_TO_BED_AT -> {
                hasUserSelectedBedTimeInSession = true
                if (truncated == goToBedSelectedTime) return
                goToBedSelectedTime = truncated
                emitState()
                persistPref { repository.setGoToBedTimeMinutes(minutesOfDay) }
            }
            SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> return
        }
    }

    fun updateSleepLatency(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_LATENCY, MAX_SLEEP_LATENCY)
        if (clamped == sleepLatencyMinutes) return
        sleepLatencyMinutes = clamped
        emitState()
        persistPref(sleepWritesInFlight) { repository.setSleepLatency(clamped) }
    }

    fun updateNapLatency(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_LATENCY, MAX_NAP_LATENCY)
        if (clamped == napLatencyMinutes) return
        napLatencyMinutes = clamped
        emitState()
        persistPref(napWritesInFlight) { repository.setNapLatency(clamped) }
    }

    fun highlightOption(optionId: String) {
        val currentOptions = _uiState.value.options
        if (currentOptions.none { it.id == optionId }) return
        if (explicitHighlightedOptionId == optionId && _uiState.value.highlightedOptionId == optionId) return
        explicitHighlightedOptionId = optionId
        emitState()
    }

    fun selectThemeStyle(style: AppThemeStyle) {
        if (_themeStyle.value == style) return
        _themeStyle.value = style
        persistPref { repository.setThemeStyle(style) }
    }

    fun selectTimeFormatPreference(pref: TimeFormatPreference) {
        if (currentTimeFormatPref == pref && _timeFormatPreference.value == pref) return
        currentTimeFormatPref = pref
        _timeFormatPreference.value = pref
        val effective24h = resolveIs24Hour(systemIs24HourFormat, pref)
        if (is24HourFormat != effective24h) {
            is24HourFormat = effective24h
            emitState()
        }
        persistPref { repository.setTimeFormatPreference(pref) }
    }

    fun selectAppLanguage(language: AppLanguage) {
        if (_appLanguage.value == language) return
        _appLanguage.value = language
        persistPref { repository.setAppLanguage(language) }
    }

    fun setPureAmoledNight(enabled: Boolean) {
        if (_pureAmoledNight.value == enabled) return
        _pureAmoledNight.value = enabled
        persistPref { repository.setPureAmoledNight(enabled) }
    }

    fun setCaffeineCutoffHours(hours: Int) {
        val clamped = hours.coerceIn(
            LatencyPreferencesRepository.MIN_CAFFEINE_CUTOFF_HOURS,
            LatencyPreferencesRepository.MAX_CAFFEINE_CUTOFF_HOURS
        )
        if (_caffeineCutoffHours.value == clamped) return
        _caffeineCutoffHours.value = clamped
        persistPref { repository.setCaffeineCutoffHours(clamped) }
    }

    fun setBedtimeReminderEnabled(enabled: Boolean) {
        if (_bedtimeReminderEnabled.value == enabled) return
        _bedtimeReminderEnabled.value = enabled
        persistPref { repository.setBedtimeReminderEnabled(enabled) }
    }

    fun startSleepSession(nowEpochMillis: Long = System.currentTimeMillis()) {
        val safeStart = nowEpochMillis.coerceAtLeast(1L)
        _activeSleepStartMillis.value = safeStart
        persistPref { repository.setActiveSleepStartMillis(safeStart) }
    }

    fun completeSleepSession(
        mood: SleepQualityMood,
        nowEpochMillis: Long = System.currentTimeMillis()
    ) {
        val existingStart = _activeSleepStartMillis.value
        val start = if (existingStart > 0L) {
            existingStart
        } else {
            nowEpochMillis - SleepHistoryCodec.DEFAULT_FALLBACK_SLEEP_MINUTES * 60_000L
        }
        val elapsedMinutes = ((nowEpochMillis - start) / 60_000L).toInt()
        val durationMinutes = if (elapsedMinutes < 15) {
            SleepHistoryCodec.DEFAULT_FALLBACK_SLEEP_MINUTES
        } else {
            elapsedMinutes.coerceAtMost(14 * 60)
        }
        val cycles = ((durationMinutes - sleepLatencyMinutes).coerceAtLeast(90) / 90.0)
            .roundToInt()
            .coerceIn(1, 7)
        val record = SleepSessionRecord(
            startEpochMillis = start,
            endEpochMillis = nowEpochMillis,
            durationMinutes = durationMinutes,
            cycles = cycles,
            mood = mood
        )
        val updated = (listOf(record) + _sleepHistory.value)
            .take(SleepHistoryCodec.MAX_HISTORY_RECORDS)
        _sleepHistory.value = updated
        _activeSleepStartMillis.value = 0L
        persistPref {
            repository.setActiveSleepStartMillis(0L)
            repository.setSleepHistory(updated)
        }
    }

    fun cancelSleepSession() {
        if (_activeSleepStartMillis.value == 0L) return
        _activeSleepStartMillis.value = 0L
        persistPref { repository.setActiveSleepStartMillis(0L) }
    }

    fun refreshTimeFormat(is24Hour: Boolean) {
        systemIs24HourFormat = is24Hour
        val effective24h = resolveIs24Hour(systemIs24HourFormat, currentTimeFormatPref)
        if (is24HourFormat == effective24h) return
        is24HourFormat = effective24h
        emitState()
    }

    private fun resolveIs24Hour(system24h: Boolean, pref: TimeFormatPreference): Boolean = when (pref) {
        TimeFormatPreference.SYSTEM -> system24h
        TimeFormatPreference.HOURS_24 -> true
        TimeFormatPreference.HOURS_12 -> false
    }

    fun refreshClockNow() {
        val fresh = nowProvider().truncatedTo(ChronoUnit.MINUTES)
        if (fresh != currentClockTime) {
            currentClockTime = fresh
            _clockTime.value = fresh
            if (currentMode == SleepMode.SLEEP_NOW || currentMode == SleepMode.POWER_NAP) {
                emitState()
            }
        }
    }

    private fun emitState() {
        _uiState.value = buildUiState()
    }

    private fun buildUiState(): SleepUiState {
        val activeSelectedTime = when (currentMode) {
            SleepMode.GO_TO_BED_AT -> goToBedSelectedTime
            SleepMode.WAKE_UP_AT, SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> wakeUpSelectedTime
        }
        val anchorTime = when (currentMode) {
            SleepMode.WAKE_UP_AT, SleepMode.GO_TO_BED_AT -> activeSelectedTime
            SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> currentClockTime
        }
        val options = calculator.calculateOptions(
            mode = currentMode,
            anchorTime = anchorTime,
            sleepLatencyMinutes = sleepLatencyMinutes,
            napLatencyMinutes = napLatencyMinutes,
            labels = labels
        )
        val resolvedHighlightId = explicitHighlightedOptionId
            ?.takeIf { id -> options.any { it.id == id } }
            ?: calculator.defaultHighlightedOptionId(currentMode, options)
        val activeSky = skyPhaseResolver.resolveActiveSkyPhase(
            mode = currentMode,
            selectedTime = activeSelectedTime,
            options = options,
            highlightedOptionId = resolvedHighlightId
        )
        return SleepUiState(
            mode = currentMode,
            selectedTime = activeSelectedTime,
            sleepLatencyMinutes = sleepLatencyMinutes,
            napLatencyMinutes = napLatencyMinutes,
            options = options,
            highlightedOptionId = resolvedHighlightId,
            activeSkyPhase = activeSky,
            is24HourFormat = is24HourFormat
        )
    }

    companion object {
        const val MIN_LATENCY = SleepCycleCalculator.MIN_SLEEP_LATENCY_MINUTES
        const val MAX_SLEEP_LATENCY = SleepCycleCalculator.MAX_SLEEP_LATENCY_MINUTES
        const val MAX_NAP_LATENCY = SleepCycleCalculator.MAX_NAP_LATENCY_MINUTES
        const val DEFAULT_SLEEP_LATENCY = SleepCycleCalculator.DEFAULT_SLEEP_LATENCY_MINUTES
        const val DEFAULT_NAP_LATENCY = SleepCycleCalculator.DEFAULT_NAP_LATENCY_MINUTES
        val DEFAULT_WAKE_UP_TIME: LocalTime = LocalTime.of(6, 50)
        val DEFAULT_GO_TO_BED_TIME: LocalTime = LocalTime.of(22, 30)

        fun defaultSleepOptionLabels(): SleepOptionLabels = SleepOptionLabels(
            optimal = R.string.quality_optimal,
            good = R.string.quality_good,
            short = R.string.quality_short,
            napQuick = R.string.nap_quick_title,
            napClassic = R.string.nap_classic_title,
            napFull = R.string.nap_full_title
        )

        fun minuteAlignedClockFlow(nowProvider: () -> LocalTime): Flow<LocalTime> = flow {
            while (coroutineContext.isActive) {
                delay(AndroidTimeFormatProvider.millisUntilNextMinute(nowProvider()).coerceAtMost(60_000L))
                emit(nowProvider().truncatedTo(ChronoUnit.MINUTES))
            }
        }

        fun factory(
            repository: LatencyPreferencesRepository,
            calculator: SleepCycleCalculator = SleepCycleCalculator(),
            skyPhaseResolver: SkyPhaseResolver = SkyPhaseResolver(calculator),
            labels: SleepOptionLabels = defaultSleepOptionLabels(),
            initialIs24HourFormat: Boolean = true,
            nowProvider: () -> LocalTime = { LocalTime.now() },
            clockTicker: Flow<LocalTime> = minuteAlignedClockFlow(nowProvider)
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SleepViewModel::class.java)) {
                    "Unknown ViewModel class: ${modelClass.name}"
                }
                return SleepViewModel(
                    repository = repository,
                    calculator = calculator,
                    skyPhaseResolver = skyPhaseResolver,
                    labels = labels,
                    initialIs24HourFormat = initialIs24HourFormat,
                    nowProvider = nowProvider,
                    clockTicker = clockTicker
                ) as T
            }
        }
    }
}
