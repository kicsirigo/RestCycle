package com.restcycle.app.unit

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.DefaultAppContainer
import com.restcycle.app.data.preferences.DataStoreLatencyPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

class LatencyPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createDataStore(
        file: File,
        scope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope
    ) { file }

    @Test
    fun defaults_whenStoreEmpty() = runTest {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        val repo = DataStoreLatencyPreferencesRepository(createDataStore(file, backgroundScope))

        assertEquals(15, repo.sleepLatencyMinutes.first())
        assertEquals(10, repo.napLatencyMinutes.first())
    }

    @Test
    fun setSleepLatency_validBoundariesAndNominal() = runTest {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        val repo = DataStoreLatencyPreferencesRepository(createDataStore(file, backgroundScope))

        repo.setSleepLatency(0)
        assertEquals(0, repo.sleepLatencyMinutes.first())

        repo.setSleepLatency(22)
        assertEquals(22, repo.sleepLatencyMinutes.first())

        repo.setSleepLatency(60)
        assertEquals(60, repo.sleepLatencyMinutes.first())
    }

    @Test
    fun setNapLatency_validBoundariesAndNominal() = runTest {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        val repo = DataStoreLatencyPreferencesRepository(createDataStore(file, backgroundScope))

        repo.setNapLatency(0)
        assertEquals(0, repo.napLatencyMinutes.first())

        repo.setNapLatency(12)
        assertEquals(12, repo.napLatencyMinutes.first())

        repo.setNapLatency(30)
        assertEquals(30, repo.napLatencyMinutes.first())
    }

    @Test
    fun setSleepLatency_clampsOutOfBoundsOnWrite() = runTest {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        val dataStore = createDataStore(file, backgroundScope)
        val repo = DataStoreLatencyPreferencesRepository(dataStore)
        val key = intPreferencesKey("sleep_latency_minutes")

        repo.setSleepLatency(-5)
        assertEquals(0, repo.sleepLatencyMinutes.first())
        assertEquals(0, dataStore.data.first()[key])

        repo.setSleepLatency(120)
        assertEquals(60, repo.sleepLatencyMinutes.first())
        assertEquals(60, dataStore.data.first()[key])
    }

    @Test
    fun setNapLatency_clampsOutOfBoundsOnWrite() = runTest {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        val dataStore = createDataStore(file, backgroundScope)
        val repo = DataStoreLatencyPreferencesRepository(dataStore)
        val key = intPreferencesKey("nap_latency_minutes")

        repo.setNapLatency(-1)
        assertEquals(0, repo.napLatencyMinutes.first())
        assertEquals(0, dataStore.data.first()[key])

        repo.setNapLatency(45)
        assertEquals(30, repo.napLatencyMinutes.first())
        assertEquals(30, dataStore.data.first()[key])
    }

    @Test
    fun readClamping_clampsPreExistingOutOfRangeData() = runTest {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        val dataStore = createDataStore(file, backgroundScope)
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("sleep_latency_minutes")] = 99
            prefs[intPreferencesKey("nap_latency_minutes")] = -3
        }
        val repo = DataStoreLatencyPreferencesRepository(dataStore)

        assertEquals(60, repo.sleepLatencyMinutes.first())
        assertEquals(0, repo.napLatencyMinutes.first())
    }

    @Test
    fun persistsAcrossRepositoryInstances() = runTest {
        val file = File(tempFolder.newFolder(), "persisted.preferences_pb")

        val job1 = Job()
        val scope1 = CoroutineScope(coroutineContext + job1)
        val repo1 = DataStoreLatencyPreferencesRepository(createDataStore(file, scope1))
        repo1.setSleepLatency(25)
        repo1.setNapLatency(5)
        job1.cancelAndJoin()

        val repo2 = DataStoreLatencyPreferencesRepository(createDataStore(file, backgroundScope))
        assertEquals(25, repo2.sleepLatencyMinutes.first())
        assertEquals(5, repo2.napLatencyMinutes.first())
    }

    @Test
    fun corruptedFile_recoversToDefaults() = runTest {
        val file = File(tempFolder.newFolder(), "corrupted.preferences_pb")
        file.writeBytes(byteArrayOf(0x7F, 0x1A, 0x00, 0x55, 0x7E))

        val repo = DataStoreLatencyPreferencesRepository(createDataStore(file, backgroundScope))
        assertEquals(15, repo.sleepLatencyMinutes.first())
        assertEquals(10, repo.napLatencyMinutes.first())
    }

    @Test
    fun ioExceptionOnRead_fallsBackToDefaults() = runTest {
        val ioFailingStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                throw IOException("Disk error")
            }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                emptyPreferences()
        }
        val ioRepo = DataStoreLatencyPreferencesRepository(ioFailingStore)
        assertEquals(15, ioRepo.sleepLatencyMinutes.first())
        assertEquals(10, ioRepo.napLatencyMinutes.first())

        val illegalStateStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                throw IllegalStateException("Unexpected bug")
            }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                emptyPreferences()
        }
        val nonIoRepo = DataStoreLatencyPreferencesRepository(illegalStateStore)
        try {
            nonIoRepo.sleepLatencyMinutes.first()
            fail("Expected IllegalStateException to be rethrown")
        } catch (_: IllegalStateException) {
            // Expected
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun distinctUntilChanged_suppressesUnrelatedKeyUpdates() = runTest {
        val file = File(tempFolder.newFolder(), "distinct.preferences_pb")
        val repo = DataStoreLatencyPreferencesRepository(createDataStore(file, backgroundScope))

        val collected = mutableListOf<Int>()
        val initialEmitted = CompletableDeferred<Unit>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.sleepLatencyMinutes
                .onEach {
                    collected.add(it)
                    initialEmitted.complete(Unit)
                }
                .take(2)
                .collect()
        }

        initialEmitted.await()
        assertEquals(listOf(15), collected)

        repo.setNapLatency(18)
        assertEquals(listOf(15), collected)

        repo.setSleepLatency(24)
        collectJob.join()
        assertEquals(listOf(15, 24), collected)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DefaultAppContainerDataStoreIsolationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class IsolatedFilesContext(
        base: Context,
        private val customFilesDir: File
    ) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = customFilesDir
    }

    @Test
    fun separateDefaultAppContainers_doNotLeakOrThrowAcrossFreshFilesDirs() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dir1 = tempFolder.newFolder("sandbox1")
        val dir2 = tempFolder.newFolder("sandbox2")

        val container1 = DefaultAppContainer(IsolatedFilesContext(app, dir1))
        val container2 = DefaultAppContainer(IsolatedFilesContext(app, dir2))

        try {
            container1.latencyPreferencesRepository.setSleepLatency(42)
            assertEquals(42, container1.latencyPreferencesRepository.sleepLatencyMinutes.first())
            // Container2 points to its own filesDir and remains isolated at default 15 without throwing
            assertEquals(15, container2.latencyPreferencesRepository.sleepLatencyMinutes.first())
        } finally {
            container1.close()
            container2.close()
        }
    }
}
