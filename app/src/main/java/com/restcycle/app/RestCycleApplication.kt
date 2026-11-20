package com.restcycle.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModelProvider
import com.restcycle.app.data.preferences.DataStoreLatencyPreferencesRepository
import com.restcycle.app.data.preferences.LatencyPreferencesRepository
import com.restcycle.app.domain.calculator.SkyPhaseResolver
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.platform.intent.AlarmScheduler
import com.restcycle.app.platform.time.AndroidTimeFormatProvider
import com.restcycle.app.platform.time.TimeFormatProvider
import com.restcycle.app.ui.viewmodel.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

interface AppContainer {
    val latencyPreferencesRepository: LatencyPreferencesRepository
    val sleepCycleCalculator: SleepCycleCalculator
    val skyPhaseResolver: SkyPhaseResolver
    val alarmScheduler: AlarmScheduler
    val timeFormatProvider: TimeFormatProvider
    val sleepViewModelFactory: ViewModelProvider.Factory
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext: Context = context.applicationContext
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dataStore: DataStore<Preferences> by lazy {
        DataStoreLatencyPreferencesRepository.createDataStore(
            context = appContext,
            scope = applicationScope
        )
    }

    override val latencyPreferencesRepository: LatencyPreferencesRepository by lazy {
        DataStoreLatencyPreferencesRepository(dataStore)
    }

    override val sleepCycleCalculator: SleepCycleCalculator by lazy {
        SleepCycleCalculator()
    }

    override val skyPhaseResolver: SkyPhaseResolver by lazy {
        SkyPhaseResolver(sleepCycleCalculator)
    }

    override val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler()
    }

    override val timeFormatProvider: TimeFormatProvider by lazy {
        AndroidTimeFormatProvider(appContext)
    }

    override val sleepViewModelFactory: ViewModelProvider.Factory
        get() = SleepViewModel.factory(
            repository = latencyPreferencesRepository,
            calculator = sleepCycleCalculator,
            skyPhaseResolver = skyPhaseResolver,
            initialIs24HourFormat = timeFormatProvider.is24HourFormat(),
            nowProvider = { timeFormatProvider.currentTime() },
            clockTicker = timeFormatProvider.minuteTickFlow()
        )

    internal fun close() {
        applicationScope.cancel()
    }
}

class RestCycleApplication : Application() {
    private var _container: AppContainer? = null

    var container: AppContainer
        get() = _container ?: DefaultAppContainer(this).also { _container = it }
        internal set(value) {
            if (_container !== value) {
                (_container as? DefaultAppContainer)?.close()
            }
            _container = value
        }

    override fun onCreate() {
        super.onCreate()
        if (_container == null) {
            _container = DefaultAppContainer(this)
        }
    }

    override fun onTerminate() {
        (_container as? DefaultAppContainer)?.close()
        super.onTerminate()
    }
}
