package com.restcycle.app.unit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.platform.audio.AmbientSoundPackManager
import com.restcycle.app.platform.audio.SoundPackStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AmbientSoundPackManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AmbientSoundMode.entries.forEach { mode ->
            if (mode != AmbientSoundMode.OFF) {
                AmbientSoundPackManager.deletePack(context, mode)
            }
        }
        AmbientSoundPackManager.refreshStatuses(context)
    }

    @Test
    fun packUrlFor_returnsGitHubReleaseUrlForActiveSoundsAndNullForOff() {
        assertNull(AmbientSoundPackManager.packUrlFor(AmbientSoundMode.OFF))
        assertEquals(
            "${AmbientSoundPackManager.PACKS_BASE_URL}/thunderstorm_full.flac",
            AmbientSoundPackManager.packUrlFor(AmbientSoundMode.STREAM_RAIN)
        )
        AmbientSoundMode.entries.filter { it != AmbientSoundMode.OFF }.forEach { mode ->
            val url = AmbientSoundPackManager.packUrlFor(mode)
            assertNotNull(url)
            assertTrue("Expected .flac pack for $mode", url!!.endsWith(".flac"))
        }
    }

    @Test
    fun downloadRefreshAndDeletePack_manageLocalStorageCleanly() {
        val payload = ByteArray(64 * 1024) { (it % 97).toByte() }
        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            repeat(2) {
                runCatching {
                    server.accept().use { socket ->
                        val reqLine = socket.getInputStream().bufferedReader().readLine().orEmpty()
                        val out = socket.getOutputStream()
                        if (reqLine.contains("/sound-packs-v1/")) {
                            val resp = "HTTP/1.1 302 Found\r\nLocation: /cdn/thunderstorm_full.flac\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            out.write(resp.toByteArray(Charsets.US_ASCII))
                        } else {
                            val header = "HTTP/1.1 200 OK\r\nContent-Type: audio/flac\r\nContent-Length: ${payload.size}\r\nConnection: close\r\n\r\n"
                            out.write(header.toByteArray(Charsets.US_ASCII))
                            out.write(payload)
                        }
                        out.flush()
                    }
                }
            }
        }

        try {
            val port = server.localPort
            val progressList = mutableListOf<Int>()
            val downloaded = runBlocking {
                AmbientSoundPackManager.downloadPackSync(
                    context = context,
                    mode = AmbientSoundMode.STREAM_RAIN,
                    downloadUrl = "http://127.0.0.1:$port/sound-packs-v1/thunderstorm_full.flac"
                ) { pct, _, _ ->
                    progressList += pct
                }
            }

            assertNotNull(downloaded)
            assertTrue(downloaded!!.exists())
            assertEquals(payload.size.toLong(), downloaded.length())
            assertEquals(100, progressList.last())

            AmbientSoundPackManager.refreshStatuses(context)
            val status = AmbientSoundPackManager.statuses.value[AmbientSoundMode.STREAM_RAIN]
            assertTrue(status is SoundPackStatus.Downloaded)
            assertEquals(payload.size.toLong(), AmbientSoundPackManager.totalDownloadedBytes())
            assertNotNull(AmbientSoundPackManager.resolveDownloadedFile(context, AmbientSoundMode.STREAM_RAIN))

            val deleted = AmbientSoundPackManager.deletePack(context, AmbientSoundMode.STREAM_RAIN)
            assertTrue(deleted)
            assertNull(AmbientSoundPackManager.resolveDownloadedFile(context, AmbientSoundMode.STREAM_RAIN))
            assertEquals(0L, AmbientSoundPackManager.totalDownloadedBytes())
            assertEquals(
                SoundPackStatus.NotDownloaded,
                AmbientSoundPackManager.statuses.value[AmbientSoundMode.STREAM_RAIN]
            )
        } finally {
            runCatching { server.close() }
            serverThread.join(1000)
        }
    }
}
