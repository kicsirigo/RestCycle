package com.restcycle.app.unit

import com.restcycle.app.data.preferences.LatencyPreferencesRepository
import com.restcycle.app.domain.calculator.SkyPhaseResolver
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.calculator.SleepOptionLabels
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.viewmodel.SleepViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {

    class MainDispatcherRule(
        val dispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeLatencyPreferencesRepository(
        initialSleep: Int = LatencyPreferencesRepository.DEFAULT_SLEEP_LATENCY_MINUTES,
        initialNap: Int = LatencyPreferencesRepository.DEFAULT_NAP_LATENCY_MINUTES,
        initialWakeMinutes: Int = LatencyPreferencesRepository.DEFAULT_WAKE_UP_TIME_MINUTES,
        initialBedMinutes: Int = LatencyPreferencesRepository.DEFAULT_GO_TO_BED_TIME_MINUTES,
        initialLastMode: SleepMode = SleepMode.WAKE_UP_AT
    ) : LatencyPreferencesRepository {
        private val _sleep = MutableStateFlow(initialSleep)
        private val _nap = MutableStateFlow(initialNap)
        private val _wakeMinutes = MutableStateFlow(initialWakeMinutes)
        private val _bedMinutes = MutableStateFlow(initialBedMinutes)
        private val _lastMode = MutableStateFlow(initialLastMode)

        override val sleepLatencyMinutes: Flow<Int> = _sleep.asStateFlow()
        override val napLatencyMinutes: Flow<Int> = _nap.asStateFlow()
        override val wakeUpTimeMinutes: Flow<Int> = _wakeMinutes.asStateFlow()
        override val goToBedTimeMinutes: Flow<Int> = _bedMinutes.asStateFlow()
        override val lastWheelMode: Flow<SleepMode> = _lastMode.asStateFlow()

        val recordedSleepWrites = mutableListOf<Int>()
        val recordedNapWrites = mutableListOf<Int>()
        val recordedWakeWrites = mutableListOf<Int>()
        val recordedBedWrites = mutableListOf<Int>()
        val recordedModeWrites = mutableListOf<SleepMode>()
        var sleepWriteGate: CompletableDeferred<Unit>? = null
        var sleepWriteException: Throwable? = null
        var napWriteException: Throwable? = null

        override suspend fun setSleepLatency(minutes: Int) {
            sleepWriteGate?.await()
            sleepWriteException?.let { throw it }
            val clamped = minutes.coerceIn(
                LatencyPreferencesRepository.MIN_SLEEP_LATENCY_MINUTES,
                LatencyPreferencesRepository.MAX_SLEEP_LATENCY_MINUTES
            )
            recordedSleepWrites.add(clamped)
            _sleep.value = clamped
        }

        override suspend fun setNapLatency(minutes: Int) {
            napWriteException?.let { throw it }
            val clamped = minutes.coerceIn(
                LatencyPreferencesRepository.MIN_NAP_LATENCY_MINUTES,
                LatencyPreferencesRepository.MAX_NAP_LATENCY_MINUTES
            )
            recordedNapWrites.add(clamped)
            _nap.value = clamped
        }

        override suspend fun setWakeUpTimeMinutes(minutesOfDay: Int) {
            val clamped = minutesOfDay.coerceIn(
                LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY,
                LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY
            )
            recordedWakeWrites.add(clamped)
            _wakeMinutes.value = clamped
        }

        override suspend fun setGoToBedTimeMinutes(minutesOfDay: Int) {
            val clamped = minutesOfDay.coerceIn(
                LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY,
                LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY
            )
            recordedBedWrites.add(clamped)
            _bedMinutes.value = clamped
        }

        override suspend fun setLastWheelMode(mode: SleepMode) {
            recordedModeWrites.add(mode)
            _lastMode.value = mode
        }

        fun emitSleepFromStore(minutes: Int) {
            _sleep.value = minutes
        }

        fun emitNapFromStore(minutes: Int) {
            _nap.value = minutes
        }
    }

    private val testLabels = SleepOptionLabels(
        optimal = 101,
        good = 102,
        short = 103,
        napQuick = 201,
        napClassic = 202,
        napFull = 203
    )

    private var fakeNow: LocalTime = LocalTime.of(22, 0)

    private fun createViewModel(
        repo: FakeLatencyPreferencesRepository = FakeLatencyPreferencesRepository(),
        initialIs24HourFormat: Boolean = true,
        clockTicker: Flow<LocalTime> = emptyFlow()
    ): SleepViewModel = SleepViewModel(
        repository = repo,
        calculator = SleepCycleCalculator(),
        skyPhaseResolver = SkyPhaseResolver(),
        labels = testLabels,
        initialIs24HourFormat = initialIs24HourFormat,
        nowProvider = { fakeNow },
        clockTicker = clockTicker
    )

    @Test
    fun initialState_isWakeUpAt0650WithDefaultLatencyAndSixOptions() = runTest {
        fakeNow = LocalTime.of(22, 0)
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(SleepMode.WAKE_UP_AT, state.mode)
        assertEquals(LocalTime.of(6, 50), state.selectedTime)
        assertEquals(15, state.sleepLatencyMinutes)
        assertEquals(10, state.napLatencyMinutes)
        assertEquals(6, state.options.size)
        assertEquals(
            listOf("wake_6", "wake_5", "wake_4", "wake_3", "wake_2", "wake_1"),
            state.options.map { it.id }
        )
        assertEquals("wake_5", state.highlightedOptionId)
        assertEquals(SkyPhase.DAWN_DUSK, state.activeSkyPhase)
        assertTrue(state.is24HourFormat)
    }

    @Test
    fun init_loadsNonDefaultPersistedLatenciesFromRepository() = runTest {
        val repo = FakeLatencyPreferencesRepository(initialSleep = 35, initialNap = 22)
        val viewModel = createViewModel(repo = repo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(35, state.sleepLatencyMinutes)
        assertEquals(22, state.napLatencyMinutes)
        assertEquals(
            LocalTime.of(22, 45),
            state.options.first { it.id == "wake_5" }.targetTime
        )
    }

    @Test
    fun selectTime_updatesWakeUpOptionsAndDrivesSkyPhaseFromWheel() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectTime(LocalTime.of(8, 0))
        assertEquals(SkyPhase.DAY, viewModel.uiState.value.activeSkyPhase)
        assertEquals(
            LocalTime.of(0, 15),
            viewModel.uiState.value.options.first { it.id == "wake_5" }.targetTime
        )

        viewModel.selectTime(LocalTime.of(20, 30))
        assertEquals(SkyPhase.NIGHT, viewModel.uiState.value.activeSkyPhase)
    }

    @Test
    fun selectTime_ignoresSecondsAndNanos() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectTime(LocalTime.of(7, 15, 42, 999))
        assertEquals(LocalTime.of(7, 15), viewModel.uiState.value.selectedTime)
    }

    @Test
    fun selectMode_sleepNow_usesLiveClockDefaultsHighlightAndSky() = runTest {
        fakeNow = LocalTime.of(23, 30)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.SLEEP_NOW)

        val state = viewModel.uiState.value
        assertEquals(SleepMode.SLEEP_NOW, state.mode)
        assertEquals(6, state.options.size)
        assertEquals("sleep_now_5", state.highlightedOptionId)
        assertEquals(
            LocalTime.of(7, 15),
            state.options.first { it.id == "sleep_now_5" }.targetTime
        )
        assertEquals(SkyPhase.DAWN_DUSK, state.activeSkyPhase)
    }

    @Test
    fun selectMode_goToBedAt_computesForwardOptionsAndDefaultsBed5() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.GO_TO_BED_AT)

        val state = viewModel.uiState.value
        assertEquals(SleepMode.GO_TO_BED_AT, state.mode)
        assertEquals(6, state.options.size)
        assertEquals("bed_5", state.highlightedOptionId)
        assertEquals(LocalTime.of(22, 30), state.selectedTime)
        assertEquals(
            LocalTime.of(6, 15),
            state.options.first { it.id == "bed_5" }.targetTime
        )
        assertEquals(SkyPhase.DAWN_DUSK, state.activeSkyPhase)
    }

    @Test
    fun selectMode_powerNap_returnsThreePresetsDefaultsNap20AndUsesClock() = runTest {
        fakeNow = LocalTime.of(13, 30)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.POWER_NAP)

        val state = viewModel.uiState.value
        assertEquals(SleepMode.POWER_NAP, state.mode)
        assertEquals(listOf("nap_10", "nap_20", "nap_90"), state.options.map { it.id })
        assertEquals("nap_20", state.highlightedOptionId)
        assertEquals(
            LocalTime.of(14, 0),
            state.options.first { it.id == "nap_20" }.targetTime
        )
        assertEquals(SkyPhase.DAY, state.activeSkyPhase)
    }

    @Test
    fun selectMode_resetsExplicitHighlightToModeDefault() = runTest {
        fakeNow = LocalTime.of(22, 0)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.SLEEP_NOW)
        viewModel.highlightOption("sleep_now_4")
        assertEquals("sleep_now_4", viewModel.uiState.value.highlightedOptionId)

        viewModel.selectMode(SleepMode.POWER_NAP)
        assertEquals("nap_20", viewModel.uiState.value.highlightedOptionId)

        viewModel.selectMode(SleepMode.SLEEP_NOW)
        assertEquals("sleep_now_5", viewModel.uiState.value.highlightedOptionId)
    }

    @Test
    fun selectMode_preservesSeparateWheelTimesForWakeUpAndGoToBed() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(LocalTime.of(6, 50), viewModel.uiState.value.selectedTime)

        viewModel.selectMode(SleepMode.GO_TO_BED_AT)
        viewModel.selectTime(LocalTime.of(23, 0))
        assertEquals(LocalTime.of(23, 0), viewModel.uiState.value.selectedTime)

        viewModel.selectMode(SleepMode.WAKE_UP_AT)
        assertEquals(LocalTime.of(6, 50), viewModel.uiState.value.selectedTime)
        assertEquals(SkyPhase.DAWN_DUSK, viewModel.uiState.value.activeSkyPhase)

        viewModel.selectMode(SleepMode.GO_TO_BED_AT)
        assertEquals(LocalTime.of(23, 0), viewModel.uiState.value.selectedTime)
    }

    @Test
    fun highlightOption_morphsSkyInForwardAndNapModes_butNotWakeUpAt() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WAKE_UP_AT: wheel 08:00 (DAY), wake_6 is 22:45 (NIGHT) -> stays DAY
        viewModel.selectTime(LocalTime.of(8, 0))
        viewModel.highlightOption("wake_6")
        assertEquals("wake_6", viewModel.uiState.value.highlightedOptionId)
        assertEquals(SkyPhase.DAY, viewModel.uiState.value.activeSkyPhase)

        // GO_TO_BED_AT: 23:00 -> bed_5 is 06:45 (DAWN_DUSK), bed_6 is 08:15 (DAY)
        viewModel.selectMode(SleepMode.GO_TO_BED_AT)
        viewModel.selectTime(LocalTime.of(23, 0))
        assertEquals(SkyPhase.DAWN_DUSK, viewModel.uiState.value.activeSkyPhase)
        viewModel.highlightOption("bed_6")
        assertEquals(SkyPhase.DAY, viewModel.uiState.value.activeSkyPhase)

        // POWER_NAP: 16:40 -> nap_20 is 17:10 (DAY), nap_90 is 18:20 (DAWN_DUSK)
        fakeNow = LocalTime.of(16, 40)
        viewModel.selectMode(SleepMode.POWER_NAP)
        assertEquals(SkyPhase.DAY, viewModel.uiState.value.activeSkyPhase)
        viewModel.highlightOption("nap_90")
        assertEquals(SkyPhase.DAWN_DUSK, viewModel.uiState.value.activeSkyPhase)
    }

    @Test
    fun highlightOption_ignoresUnknownOptionId() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.highlightOption("non_existent_id")
        assertEquals("wake_5", viewModel.uiState.value.highlightedOptionId)
    }

    @Test
    fun selectTime_preservesExplicitHighlightInGoToBedAt() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.GO_TO_BED_AT)
        viewModel.selectTime(LocalTime.of(22, 0))
        viewModel.highlightOption("bed_4")

        viewModel.selectTime(LocalTime.of(1, 45))

        val state = viewModel.uiState.value
        assertEquals("bed_4", state.highlightedOptionId)
        assertEquals(LocalTime.of(8, 0), state.options.first { it.id == "bed_4" }.targetTime)
        assertEquals(SkyPhase.DAY, state.activeSkyPhase)
    }

    @Test
    fun updateSleepLatency_clamps0To60UpdatesImmediatelyAndPersists() = runTest {
        val repo = FakeLatencyPreferencesRepository()
        val viewModel = createViewModel(repo = repo)
        advanceUntilIdle()

        viewModel.updateSleepLatency(-10)
        assertEquals(0, viewModel.uiState.value.sleepLatencyMinutes)
        assertEquals(10, viewModel.uiState.value.napLatencyMinutes)

        viewModel.updateSleepLatency(95)
        assertEquals(60, viewModel.uiState.value.sleepLatencyMinutes)

        viewModel.updateSleepLatency(25)
        assertEquals(25, viewModel.uiState.value.sleepLatencyMinutes)

        advanceUntilIdle()
        assertEquals(listOf(0, 60, 25), repo.recordedSleepWrites)
        assertEquals(10, viewModel.uiState.value.napLatencyMinutes)
    }

    @Test
    fun updateNapLatency_clamps0To30UpdatesImmediatelyAndPersists() = runTest {
        fakeNow = LocalTime.of(13, 0)
        val repo = FakeLatencyPreferencesRepository()
        val viewModel = createViewModel(repo = repo)
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.POWER_NAP)

        viewModel.updateNapLatency(-5)
        assertEquals(0, viewModel.uiState.value.napLatencyMinutes)

        viewModel.updateNapLatency(45)
        assertEquals(30, viewModel.uiState.value.napLatencyMinutes)

        viewModel.updateNapLatency(18)
        assertEquals(18, viewModel.uiState.value.napLatencyMinutes)
        assertEquals(38L, viewModel.uiState.value.options.first { it.id == "nap_20" }.countdownMinutesFromNow)

        advanceUntilIdle()
        assertEquals(listOf(0, 30, 18), repo.recordedNapWrites)
        assertEquals(15, viewModel.uiState.value.sleepLatencyMinutes)
    }

    @Test
    fun updateSleepLatency_rapidUpdatesDoNotFlickerBackToStaleRepoValue() = runTest {
        val repo = FakeLatencyPreferencesRepository(initialSleep = 15)
        val gate = CompletableDeferred<Unit>()
        repo.sleepWriteGate = gate
        val viewModel = createViewModel(repo = repo)

        viewModel.updateSleepLatency(20)
        viewModel.updateSleepLatency(22)
        viewModel.updateSleepLatency(25)

        // Run initial collector emission (15) while writes are suspended on gate
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertEquals(25, viewModel.uiState.value.sleepLatencyMinutes)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(25, viewModel.uiState.value.sleepLatencyMinutes)
    }

    @Test
    fun clockTicker_updatesSleepNowAndPowerNap_doesNotRecomputeWakeUpAt() = runTest {
        fakeNow = LocalTime.of(22, 0)
        val clockFlow = MutableSharedFlow<LocalTime>()
        val viewModel = createViewModel(clockTicker = clockFlow)
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.SLEEP_NOW)
        clockFlow.emit(LocalTime.of(22, 5))
        advanceUntilIdle()
        assertEquals(
            LocalTime.of(5, 50),
            viewModel.uiState.value.options.first { it.id == "sleep_now_5" }.targetTime
        )

        fakeNow = LocalTime.of(14, 0)
        viewModel.selectMode(SleepMode.POWER_NAP)
        clockFlow.emit(LocalTime.of(14, 5))
        advanceUntilIdle()
        assertEquals(
            LocalTime.of(14, 35),
            viewModel.uiState.value.options.first { it.id == "nap_20" }.targetTime
        )

        viewModel.selectMode(SleepMode.WAKE_UP_AT)
        val beforeTick = viewModel.uiState.value.options
        clockFlow.emit(LocalTime.of(23, 0))
        advanceUntilIdle()
        assertEquals(beforeTick, viewModel.uiState.value.options)
    }

    @Test
    fun refreshClockNow_updatesLiveModesImmediatelyOnResume() = runTest {
        fakeNow = LocalTime.of(22, 0)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectMode(SleepMode.SLEEP_NOW)
        assertEquals(
            LocalTime.of(5, 45),
            viewModel.uiState.value.options.first { it.id == "sleep_now_5" }.targetTime
        )

        fakeNow = LocalTime.of(22, 12)
        viewModel.refreshClockNow()
        assertEquals(
            LocalTime.of(5, 57),
            viewModel.uiState.value.options.first { it.id == "sleep_now_5" }.targetTime
        )
    }

    @Test
    fun refreshTimeFormat_andFactory_workAsContracted() = runTest {
        val repo = FakeLatencyPreferencesRepository()
        val factory = SleepViewModel.factory(
            repository = repo,
            labels = testLabels,
            initialIs24HourFormat = true,
            nowProvider = { fakeNow },
            clockTicker = emptyFlow()
        )
        val viewModel = factory.create(SleepViewModel::class.java)
        assertNotNull(viewModel)
        assertTrue(viewModel.uiState.value.is24HourFormat)

        viewModel.selectMode(SleepMode.GO_TO_BED_AT)
        viewModel.selectTime(LocalTime.of(23, 15))
        viewModel.highlightOption("bed_4")

        viewModel.refreshTimeFormat(false)
        assertFalse(viewModel.uiState.value.is24HourFormat)
        assertEquals(SleepMode.GO_TO_BED_AT, viewModel.uiState.value.mode)
        assertEquals(LocalTime.of(23, 15), viewModel.uiState.value.selectedTime)
        assertEquals("bed_4", viewModel.uiState.value.highlightedOptionId)

        viewModel.refreshTimeFormat(true)
        assertTrue(viewModel.uiState.value.is24HourFormat)

        class OtherViewModel : androidx.lifecycle.ViewModel()
        try {
            factory.create(OtherViewModel::class.java)
            fail("Expected IllegalArgumentException for unknown ViewModel class")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun updateSleepLatency_repositoryIOException_doesNotCrashRecoversInFlightAndCollectsSubsequentEmissions() = runTest {
        val repo = FakeLatencyPreferencesRepository(initialSleep = 15)
        val viewModel = createViewModel(repo = repo)
        advanceUntilIdle()

        repo.sleepWriteException = IOException("Simulated disk full")
        viewModel.updateSleepLatency(30)
        advanceUntilIdle()

        assertEquals(0, viewModel.sleepWritesInFlight.get())
        assertEquals(30, viewModel.uiState.value.sleepLatencyMinutes)

        repo.sleepWriteException = null
        repo.emitSleepFromStore(42)
        advanceUntilIdle()

        assertEquals(42, viewModel.uiState.value.sleepLatencyMinutes)
    }

    @Test
    fun updateNapLatency_repositoryIOException_doesNotCrashRecoversInFlightAndCollectsSubsequentEmissions() = runTest {
        val repo = FakeLatencyPreferencesRepository(initialNap = 10)
        val viewModel = createViewModel(repo = repo)
        advanceUntilIdle()

        repo.napWriteException = IOException("Simulated disk full")
        viewModel.updateNapLatency(25)
        advanceUntilIdle()

        assertEquals(0, viewModel.napWritesInFlight.get())
        assertEquals(25, viewModel.uiState.value.napLatencyMinutes)

        repo.napWriteException = null
        repo.emitNapFromStore(18)
        advanceUntilIdle()

        assertEquals(18, viewModel.uiState.value.napLatencyMinutes)
    }

    @Test
    fun init_restoresPersistedWakeBedTimesAndLastWheelMode() = runTest {
        val repo = FakeLatencyPreferencesRepository(
            initialWakeMinutes = 7 * 60 + 30,
            initialBedMinutes = 23 * 60 + 15,
            initialLastMode = SleepMode.GO_TO_BED_AT
        )
        val viewModel = createViewModel(repo = repo)
        advanceUntilIdle()

        assertEquals(SleepMode.GO_TO_BED_AT, viewModel.uiState.value.mode)
        assertEquals(LocalTime.of(23, 15), viewModel.uiState.value.selectedTime)

        viewModel.selectMode(SleepMode.WAKE_UP_AT)
        assertEquals(LocalTime.of(7, 30), viewModel.uiState.value.selectedTime)
    }

    @Test
    fun selectTimeAndSelectMode_persistWheelTimesAndScheduledModeOnly() = runTest {
        val repo = FakeLatencyPreferencesRepository()
        val viewModel = createViewModel(repo = repo)
        advanceUntilIdle()

        viewModel.selectTime(LocalTime.of(7, 0))
        viewModel.selectMode(SleepMode.GO_TO_BED_AT)
        viewModel.selectTime(LocalTime.of(22, 15))
        viewModel.selectMode(SleepMode.POWER_NAP)
        viewModel.selectMode(SleepMode.SLEEP_NOW)
        advanceUntilIdle()

        assertEquals(listOf(7 * 60), repo.recordedWakeWrites)
        assertEquals(listOf(22 * 60 + 15), repo.recordedBedWrites)
        assertEquals(listOf(SleepMode.GO_TO_BED_AT), repo.recordedModeWrites)
    }
}
