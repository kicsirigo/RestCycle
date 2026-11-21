package com.restcycle.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.restcycle.app.data.preferences.DataStoreLatencyPreferencesRepository
import com.restcycle.app.data.preferences.LatencyPreferencesRepository
import com.restcycle.app.domain.calculator.SkyPhaseResolver
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.platform.intent.AlarmScheduler
import com.restcycle.app.platform.time.AndroidTimeFormatProvider
import com.restcycle.app.ui.viewmodel.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class DefaultAppContainer(context: Context) {
    private val appContext: Context = context.applicationContext
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val latencyPreferencesRepository: LatencyPreferencesRepository by lazy {
        DataStoreLatencyPreferencesRepository(
            DataStoreLatencyPreferencesRepository.createDataStore(
                context = appContext,
                scope = applicationScope
            )
        )
    }

    val sleepCycleCalculator: SleepCycleCalculator = SleepCycleCalculator()
    val skyPhaseResolver: SkyPhaseResolver = SkyPhaseResolver(sleepCycleCalculator)
    val alarmScheduler: AlarmScheduler = AlarmScheduler()
    val timeFormatProvider: AndroidTimeFormatProvider = AndroidTimeFormatProvider(appContext)

    val sleepViewModelFactory: ViewModelProvider.Factory
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
    private var _container: DefaultAppContainer? = null

    var container: DefaultAppContainer
        get() = _container ?: DefaultAppContainer(this).also { _container = it }
        internal set(value) {
            if (_container !== value) _container?.close()
            _container = value
        }

    override fun onCreate() {
        super.onCreate()
        if (_container == null) {
            _container = DefaultAppContainer(this)
        }
    }

    override fun onTerminate() {
        _container?.close()
        super.onTerminate()
    }
}
