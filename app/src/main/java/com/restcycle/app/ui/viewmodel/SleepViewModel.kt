package com.restcycle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restcycle.app.R
import com.restcycle.app.data.preferences.LatencyPreferencesRepository
import com.restcycle.app.domain.calculator.SkyPhaseResolver
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.calculator.SleepOptionLabels
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepUiState
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
    internal val themeWritesInFlight = AtomicInteger(0)
    internal val timeFormatWritesInFlight = AtomicInteger(0)
    internal val languageWritesInFlight = AtomicInteger(0)

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    private val _themeStyle = MutableStateFlow(AppThemeStyle.DYNAMIC)
    val themeStyle: StateFlow<AppThemeStyle> = _themeStyle.asStateFlow()

    private val _timeFormatPreference = MutableStateFlow(TimeFormatPreference.SYSTEM)
    val timeFormatPreference: StateFlow<TimeFormatPreference> = _timeFormatPreference.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sleepLatencyMinutes
                .distinctUntilChanged()
                .collect { stored ->
                    if (sleepWritesInFlight.get() == 0) {
                        val clamped = stored.coerceIn(MIN_LATENCY, MAX_SLEEP_LATENCY)
                        if (clamped != sleepLatencyMinutes) {
                            sleepLatencyMinutes = clamped
                            emitState()
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.napLatencyMinutes
                .distinctUntilChanged()
                .collect { stored ->
                    if (napWritesInFlight.get() == 0) {
                        val clamped = stored.coerceIn(MIN_LATENCY, MAX_NAP_LATENCY)
                        if (clamped != napLatencyMinutes) {
                            napLatencyMinutes = clamped
                            emitState()
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.themeStyle
                .distinctUntilChanged()
                .collect { stored ->
                    if (themeWritesInFlight.get() == 0 && stored != _themeStyle.value) {
                        _themeStyle.value = stored
                    }
                }
        }

        viewModelScope.launch {
            repository.timeFormatPreference
                .distinctUntilChanged()
                .collect { stored ->
                    if (timeFormatWritesInFlight.get() == 0 && stored != currentTimeFormatPref) {
                        currentTimeFormatPref = stored
                        _timeFormatPreference.value = stored
                        val effective24h = resolveIs24Hour(systemIs24HourFormat, stored)
                        if (effective24h != is24HourFormat) {
                            is24HourFormat = effective24h
                            emitState()
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.appLanguage
                .distinctUntilChanged()
                .collect { stored ->
                    if (languageWritesInFlight.get() == 0 && stored != _appLanguage.value) {
                        _appLanguage.value = stored
                    }
                }
        }

        viewModelScope.launch {
            clockTicker.collect { tickedTime ->
                val truncated = tickedTime.truncatedTo(ChronoUnit.MINUTES)
                if (truncated != currentClockTime) {
                    currentClockTime = truncated
                    if (currentMode == SleepMode.SLEEP_NOW || currentMode == SleepMode.POWER_NAP) {
                        emitState()
                    }
                }
            }
        }
    }

    fun selectMode(mode: SleepMode) {
        currentClockTime = nowProvider().truncatedTo(ChronoUnit.MINUTES)
        if (currentMode == mode) {
            emitState()
            return
        }
        currentMode = mode
        explicitHighlightedOptionId = null
        emitState()
    }

    fun selectTime(time: LocalTime) {
        val truncated = time.truncatedTo(ChronoUnit.MINUTES)
        when (currentMode) {
            SleepMode.WAKE_UP_AT -> {
                if (truncated == wakeUpSelectedTime) return
                wakeUpSelectedTime = truncated
            }
            SleepMode.GO_TO_BED_AT -> {
                if (truncated == goToBedSelectedTime) return
                goToBedSelectedTime = truncated
            }
            SleepMode.SLEEP_NOW, SleepMode.POWER_NAP -> return
        }
        emitState()
    }

    fun updateSleepLatency(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_LATENCY, MAX_SLEEP_LATENCY)
        if (clamped == sleepLatencyMinutes) return
        sleepLatencyMinutes = clamped
        emitState()
        sleepWritesInFlight.incrementAndGet()
        viewModelScope.launch {
            try {
                repository.setSleepLatency(clamped)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                sleepWritesInFlight.decrementAndGet()
            }
        }
    }

    fun updateNapLatency(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_LATENCY, MAX_NAP_LATENCY)
        if (clamped == napLatencyMinutes) return
        napLatencyMinutes = clamped
        emitState()
        napWritesInFlight.incrementAndGet()
        viewModelScope.launch {
            try {
                repository.setNapLatency(clamped)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                napWritesInFlight.decrementAndGet()
            }
        }
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
        themeWritesInFlight.incrementAndGet()
        viewModelScope.launch {
            try {
                repository.setThemeStyle(style)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                themeWritesInFlight.decrementAndGet()
            }
        }
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
        timeFormatWritesInFlight.incrementAndGet()
        viewModelScope.launch {
            try {
                repository.setTimeFormatPreference(pref)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                timeFormatWritesInFlight.decrementAndGet()
            }
        }
    }

    fun selectAppLanguage(language: AppLanguage) {
        if (_appLanguage.value == language) return
        _appLanguage.value = language
        languageWritesInFlight.incrementAndGet()
        viewModelScope.launch {
            try {
                repository.setAppLanguage(language)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                languageWritesInFlight.decrementAndGet()
            }
        }
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
                val now = nowProvider()
                val nanosIntoMinute = now.second * 1_000_000_000L + now.nano
                val remainingNanos = 60_000_000_000L - nanosIntoMinute
                val delayMs = ((remainingNanos + 999_999L) / 1_000_000L).coerceIn(1L, 60_000L)
                delay(delayMs)
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
