package com.restcycle.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.model.SleepHistoryCodec
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.platform.audio.SoundMixCodec
import com.restcycle.app.platform.audio.SoundMixPreset
import com.restcycle.app.ui.theme.AppLanguage
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.TimeFormatPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException

interface LatencyPreferencesRepository {
    val sleepLatencyMinutes: Flow<Int>
    val napLatencyMinutes: Flow<Int>
    val themeStyle: Flow<AppThemeStyle> get() = flowOf(AppThemeStyle.TOKYO_NIGHT)
    val timeFormatPreference: Flow<TimeFormatPreference> get() = flowOf(TimeFormatPreference.SYSTEM)
    val appLanguage: Flow<AppLanguage> get() = flowOf(AppLanguage.SYSTEM)
    val wakeUpTimeMinutes: Flow<Int> get() = flowOf(DEFAULT_WAKE_UP_TIME_MINUTES)
    val goToBedTimeMinutes: Flow<Int> get() = flowOf(DEFAULT_GO_TO_BED_TIME_MINUTES)
    val lastWheelMode: Flow<SleepMode> get() = flowOf(SleepMode.WAKE_UP_AT)
    val pureAmoledNight: Flow<Boolean> get() = flowOf(false)
    val caffeineCutoffHours: Flow<Int> get() = flowOf(DEFAULT_CAFFEINE_CUTOFF_HOURS)
    val bedtimeReminderEnabled: Flow<Boolean> get() = flowOf(false)
    val activeSleepStartMillis: Flow<Long> get() = flowOf(0L)
    val sleepHistory: Flow<List<SleepSessionRecord>> get() = flowOf(emptyList())
    val savedSoundMixes: Flow<List<SoundMixPreset>> get() = flowOf(emptyList())

    suspend fun setSleepLatency(minutes: Int)
    suspend fun setNapLatency(minutes: Int)
    suspend fun setThemeStyle(style: AppThemeStyle) {}
    suspend fun setTimeFormatPreference(pref: TimeFormatPreference) {}
    suspend fun setAppLanguage(language: AppLanguage) {}
    suspend fun setWakeUpTimeMinutes(minutesOfDay: Int) {}
    suspend fun setGoToBedTimeMinutes(minutesOfDay: Int) {}
    suspend fun setLastWheelMode(mode: SleepMode) {}
    suspend fun setPureAmoledNight(enabled: Boolean) {}
    suspend fun setCaffeineCutoffHours(hours: Int) {}
    suspend fun setBedtimeReminderEnabled(enabled: Boolean) {}
    suspend fun setActiveSleepStartMillis(epochMillis: Long) {}
    suspend fun setSleepHistory(records: List<SleepSessionRecord>) {}
    suspend fun setSavedSoundMixes(presets: List<SoundMixPreset>) {}

    companion object {
        const val PREFS_NAME = "restcycle_prefs"
        const val DEFAULT_SLEEP_LATENCY_MINUTES = SleepCycleCalculator.DEFAULT_SLEEP_LATENCY_MINUTES
        const val MIN_SLEEP_LATENCY_MINUTES = SleepCycleCalculator.MIN_SLEEP_LATENCY_MINUTES
        const val MAX_SLEEP_LATENCY_MINUTES = SleepCycleCalculator.MAX_SLEEP_LATENCY_MINUTES

        const val DEFAULT_NAP_LATENCY_MINUTES = SleepCycleCalculator.DEFAULT_NAP_LATENCY_MINUTES
        const val MIN_NAP_LATENCY_MINUTES = SleepCycleCalculator.MIN_NAP_LATENCY_MINUTES
        const val MAX_NAP_LATENCY_MINUTES = SleepCycleCalculator.MAX_NAP_LATENCY_MINUTES

        const val DEFAULT_WAKE_UP_TIME_MINUTES = 6 * 60 + 50
        const val DEFAULT_GO_TO_BED_TIME_MINUTES = 22 * 60 + 30
        const val MIN_TIME_MINUTES_OF_DAY = 0
        const val MAX_TIME_MINUTES_OF_DAY = 23 * 60 + 59

        const val DEFAULT_CAFFEINE_CUTOFF_HOURS = 8
        const val MIN_CAFFEINE_CUTOFF_HOURS = 0
        const val MAX_CAFFEINE_CUTOFF_HOURS = 12
    }
}

class DataStoreLatencyPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : LatencyPreferencesRepository {

    private object Keys {
        val SLEEP_LATENCY_MINUTES = intPreferencesKey("sleep_latency_minutes")
        val NAP_LATENCY_MINUTES = intPreferencesKey("nap_latency_minutes")
        val APP_THEME_STYLE = stringPreferencesKey("app_theme_style")
        val TIME_FORMAT_PREFERENCE = stringPreferencesKey("time_format_preference")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val WAKE_UP_TIME_MINUTES = intPreferencesKey("wake_up_time_minutes")
        val GO_TO_BED_TIME_MINUTES = intPreferencesKey("go_to_bed_time_minutes")
        val LAST_WHEEL_MODE = stringPreferencesKey("last_wheel_mode")
        val PURE_AMOLED_NIGHT = booleanPreferencesKey("pure_amoled_night")
        val CAFFEINE_CUTOFF_HOURS = intPreferencesKey("caffeine_cutoff_hours")
        val BEDTIME_REMINDER_ENABLED = booleanPreferencesKey("bedtime_reminder_enabled")
        val ACTIVE_SLEEP_START_MILLIS = longPreferencesKey("active_sleep_start_millis")
        val SLEEP_HISTORY_RECORDS = stringPreferencesKey("sleep_history_records")
        val SAVED_SOUND_MIXES = stringPreferencesKey("saved_sound_mixes")
    }

    private val safeData: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    private fun <T> readPref(mapper: (Preferences) -> T): Flow<T> =
        safeData.map(mapper).distinctUntilChanged()

    private suspend fun <T> writePref(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    override val sleepLatencyMinutes: Flow<Int> = readPref { prefs ->
        (prefs[Keys.SLEEP_LATENCY_MINUTES] ?: LatencyPreferencesRepository.DEFAULT_SLEEP_LATENCY_MINUTES)
            .coerceIn(LatencyPreferencesRepository.MIN_SLEEP_LATENCY_MINUTES, LatencyPreferencesRepository.MAX_SLEEP_LATENCY_MINUTES)
    }

    override val napLatencyMinutes: Flow<Int> = readPref { prefs ->
        (prefs[Keys.NAP_LATENCY_MINUTES] ?: LatencyPreferencesRepository.DEFAULT_NAP_LATENCY_MINUTES)
            .coerceIn(LatencyPreferencesRepository.MIN_NAP_LATENCY_MINUTES, LatencyPreferencesRepository.MAX_NAP_LATENCY_MINUTES)
    }

    override val themeStyle: Flow<AppThemeStyle> = readPref { prefs ->
        AppThemeStyle.entries.firstOrNull { it.name == prefs[Keys.APP_THEME_STYLE] } ?: AppThemeStyle.TOKYO_NIGHT
    }

    override val timeFormatPreference: Flow<TimeFormatPreference> = readPref { prefs ->
        TimeFormatPreference.entries.firstOrNull { it.name == prefs[Keys.TIME_FORMAT_PREFERENCE] }
            ?: TimeFormatPreference.SYSTEM
    }

    override val appLanguage: Flow<AppLanguage> = readPref { prefs ->
        AppLanguage.entries.firstOrNull { it.name == prefs[Keys.APP_LANGUAGE] } ?: AppLanguage.SYSTEM
    }

    override val wakeUpTimeMinutes: Flow<Int> = readPref { prefs ->
        (prefs[Keys.WAKE_UP_TIME_MINUTES] ?: LatencyPreferencesRepository.DEFAULT_WAKE_UP_TIME_MINUTES)
            .coerceIn(LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY, LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY)
    }

    override val goToBedTimeMinutes: Flow<Int> = readPref { prefs ->
        (prefs[Keys.GO_TO_BED_TIME_MINUTES] ?: LatencyPreferencesRepository.DEFAULT_GO_TO_BED_TIME_MINUTES)
            .coerceIn(LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY, LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY)
    }

    override val lastWheelMode: Flow<SleepMode> = readPref { prefs ->
        if (prefs[Keys.LAST_WHEEL_MODE] == SleepMode.GO_TO_BED_AT.name) SleepMode.GO_TO_BED_AT else SleepMode.WAKE_UP_AT
    }

    override val pureAmoledNight: Flow<Boolean> = readPref { it[Keys.PURE_AMOLED_NIGHT] ?: false }

    override val caffeineCutoffHours: Flow<Int> = readPref { prefs ->
        (prefs[Keys.CAFFEINE_CUTOFF_HOURS] ?: LatencyPreferencesRepository.DEFAULT_CAFFEINE_CUTOFF_HOURS)
            .coerceIn(LatencyPreferencesRepository.MIN_CAFFEINE_CUTOFF_HOURS, LatencyPreferencesRepository.MAX_CAFFEINE_CUTOFF_HOURS)
    }

    override val bedtimeReminderEnabled: Flow<Boolean> = readPref { it[Keys.BEDTIME_REMINDER_ENABLED] ?: false }

    override val activeSleepStartMillis: Flow<Long> = readPref {
        (it[Keys.ACTIVE_SLEEP_START_MILLIS] ?: 0L).coerceAtLeast(0L)
    }

    override val sleepHistory: Flow<List<SleepSessionRecord>> = readPref {
        SleepHistoryCodec.decode(it[Keys.SLEEP_HISTORY_RECORDS])
    }

    override val savedSoundMixes: Flow<List<SoundMixPreset>> = readPref {
        SoundMixCodec.decode(it[Keys.SAVED_SOUND_MIXES])
    }

    override suspend fun setSleepLatency(minutes: Int) = writePref(
        Keys.SLEEP_LATENCY_MINUTES,
        minutes.coerceIn(LatencyPreferencesRepository.MIN_SLEEP_LATENCY_MINUTES, LatencyPreferencesRepository.MAX_SLEEP_LATENCY_MINUTES)
    )

    override suspend fun setNapLatency(minutes: Int) = writePref(
        Keys.NAP_LATENCY_MINUTES,
        minutes.coerceIn(LatencyPreferencesRepository.MIN_NAP_LATENCY_MINUTES, LatencyPreferencesRepository.MAX_NAP_LATENCY_MINUTES)
    )

    override suspend fun setThemeStyle(style: AppThemeStyle) = writePref(Keys.APP_THEME_STYLE, style.name)

    override suspend fun setTimeFormatPreference(pref: TimeFormatPreference) =
        writePref(Keys.TIME_FORMAT_PREFERENCE, pref.name)

    override suspend fun setAppLanguage(language: AppLanguage) = writePref(Keys.APP_LANGUAGE, language.name)

    override suspend fun setWakeUpTimeMinutes(minutesOfDay: Int) = writePref(
        Keys.WAKE_UP_TIME_MINUTES,
        minutesOfDay.coerceIn(LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY, LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY)
    )

    override suspend fun setGoToBedTimeMinutes(minutesOfDay: Int) = writePref(
        Keys.GO_TO_BED_TIME_MINUTES,
        minutesOfDay.coerceIn(LatencyPreferencesRepository.MIN_TIME_MINUTES_OF_DAY, LatencyPreferencesRepository.MAX_TIME_MINUTES_OF_DAY)
    )

    override suspend fun setLastWheelMode(mode: SleepMode) {
        if (mode == SleepMode.WAKE_UP_AT || mode == SleepMode.GO_TO_BED_AT) {
            writePref(Keys.LAST_WHEEL_MODE, mode.name)
        }
    }

    override suspend fun setPureAmoledNight(enabled: Boolean) = writePref(Keys.PURE_AMOLED_NIGHT, enabled)

    override suspend fun setCaffeineCutoffHours(hours: Int) = writePref(
        Keys.CAFFEINE_CUTOFF_HOURS,
        hours.coerceIn(LatencyPreferencesRepository.MIN_CAFFEINE_CUTOFF_HOURS, LatencyPreferencesRepository.MAX_CAFFEINE_CUTOFF_HOURS)
    )

    override suspend fun setBedtimeReminderEnabled(enabled: Boolean) =
        writePref(Keys.BEDTIME_REMINDER_ENABLED, enabled)

    override suspend fun setActiveSleepStartMillis(epochMillis: Long) =
        writePref(Keys.ACTIVE_SLEEP_START_MILLIS, epochMillis.coerceAtLeast(0L))

    override suspend fun setSleepHistory(records: List<SleepSessionRecord>) =
        writePref(Keys.SLEEP_HISTORY_RECORDS, SleepHistoryCodec.encode(records))

    override suspend fun setSavedSoundMixes(presets: List<SoundMixPreset>) =
        writePref(Keys.SAVED_SOUND_MIXES, SoundMixCodec.encode(presets))

    companion object {
        fun createDataStore(
            context: Context,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ): DataStore<Preferences> {
            val appContext = context.applicationContext
            return PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope
            ) {
                appContext.preferencesDataStoreFile(LatencyPreferencesRepository.PREFS_NAME)
            }
        }
    }
}
