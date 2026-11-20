package com.restcycle.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
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
    val themeStyle: Flow<AppThemeStyle>
        get() = flowOf(AppThemeStyle.DYNAMIC)
    val timeFormatPreference: Flow<TimeFormatPreference>
        get() = flowOf(TimeFormatPreference.SYSTEM)
    val appLanguage: Flow<AppLanguage>
        get() = flowOf(AppLanguage.SYSTEM)
    suspend fun setSleepLatency(minutes: Int)
    suspend fun setNapLatency(minutes: Int)
    suspend fun setThemeStyle(style: AppThemeStyle) {}
    suspend fun setTimeFormatPreference(pref: TimeFormatPreference) {}
    suspend fun setAppLanguage(language: AppLanguage) {}

    companion object {
        const val PREFS_NAME = "restcycle_prefs"
        const val DEFAULT_SLEEP_LATENCY_MINUTES = 15
        const val MIN_SLEEP_LATENCY_MINUTES = 0
        const val MAX_SLEEP_LATENCY_MINUTES = 60

        const val DEFAULT_NAP_LATENCY_MINUTES = 10
        const val MIN_NAP_LATENCY_MINUTES = 0
        const val MAX_NAP_LATENCY_MINUTES = 30
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
    }

    private val safeData: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    override val sleepLatencyMinutes: Flow<Int> = safeData
        .map { prefs ->
            (prefs[Keys.SLEEP_LATENCY_MINUTES]
                ?: LatencyPreferencesRepository.DEFAULT_SLEEP_LATENCY_MINUTES)
                .coerceIn(
                    LatencyPreferencesRepository.MIN_SLEEP_LATENCY_MINUTES,
                    LatencyPreferencesRepository.MAX_SLEEP_LATENCY_MINUTES
                )
        }
        .distinctUntilChanged()

    override val napLatencyMinutes: Flow<Int> = safeData
        .map { prefs ->
            (prefs[Keys.NAP_LATENCY_MINUTES]
                ?: LatencyPreferencesRepository.DEFAULT_NAP_LATENCY_MINUTES)
                .coerceIn(
                    LatencyPreferencesRepository.MIN_NAP_LATENCY_MINUTES,
                    LatencyPreferencesRepository.MAX_NAP_LATENCY_MINUTES
                )
        }
        .distinctUntilChanged()

    override val themeStyle: Flow<AppThemeStyle> = safeData
        .map { prefs ->
            val raw = prefs[Keys.APP_THEME_STYLE]
            AppThemeStyle.entries.firstOrNull { it.name == raw } ?: AppThemeStyle.DYNAMIC
        }
        .distinctUntilChanged()

    override val timeFormatPreference: Flow<TimeFormatPreference> = safeData
        .map { prefs ->
            val raw = prefs[Keys.TIME_FORMAT_PREFERENCE]
            TimeFormatPreference.entries.firstOrNull { it.name == raw } ?: TimeFormatPreference.SYSTEM
        }
        .distinctUntilChanged()

    override val appLanguage: Flow<AppLanguage> = safeData
        .map { prefs ->
            val raw = prefs[Keys.APP_LANGUAGE]
            AppLanguage.entries.firstOrNull { it.name == raw } ?: AppLanguage.SYSTEM
        }
        .distinctUntilChanged()

    override suspend fun setSleepLatency(minutes: Int) {
        val clamped = minutes.coerceIn(
            LatencyPreferencesRepository.MIN_SLEEP_LATENCY_MINUTES,
            LatencyPreferencesRepository.MAX_SLEEP_LATENCY_MINUTES
        )
        dataStore.edit { prefs ->
            prefs[Keys.SLEEP_LATENCY_MINUTES] = clamped
        }
    }

    override suspend fun setNapLatency(minutes: Int) {
        val clamped = minutes.coerceIn(
            LatencyPreferencesRepository.MIN_NAP_LATENCY_MINUTES,
            LatencyPreferencesRepository.MAX_NAP_LATENCY_MINUTES
        )
        dataStore.edit { prefs ->
            prefs[Keys.NAP_LATENCY_MINUTES] = clamped
        }
    }

    override suspend fun setThemeStyle(style: AppThemeStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_THEME_STYLE] = style.name
        }
    }

    override suspend fun setTimeFormatPreference(pref: TimeFormatPreference) {
        dataStore.edit { prefs ->
            prefs[Keys.TIME_FORMAT_PREFERENCE] = pref.name
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.name
        }
    }

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
