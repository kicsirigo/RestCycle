package com.restcycle.app.unit

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.restcycle.app.platform.update.GitHubUpdateChecker
import com.restcycle.app.platform.update.ReleaseUpdateInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GitHubUpdateCheckerTest {

    @Test
    fun normalizeVersion_stripsPrefixesAndSuffixes() {
        assertEquals("1.2.0", GitHubUpdateChecker.normalizeVersion("v1.2.0"))
        assertEquals("1.2.0", GitHubUpdateChecker.normalizeVersion(" V1.2.0-rc1 "))
        assertEquals("2.0", GitHubUpdateChecker.normalizeVersion("2.0+build42"))
    }

    @Test
    fun isNewerVersion_handlesPatchMinorMajorAndEqual() {
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.1.1", "v1.2.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.2.0", "1.2.1"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.2.0", "2.0.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.2", "1.2.1"))

        assertFalse(GitHubUpdateChecker.isNewerVersion("1.2.0", "v1.2.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("1.2.0", "1.1.1"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("1.2.0", "invalid"))
    }

    @Test
    fun parseReleaseJson_prefersReleaseApkAssetOverDebugApk() {
        val sampleJson = """
            {
              "tag_name": "v1.2.0",
              "html_url": "https://github.com/kicsirigo/RestCycle/releases/tag/v1.2.0",
              "assets": [
                {
                  "name": "RestCycle-debug.apk",
                  "browser_download_url": "https://github.com/kicsirigo/RestCycle/releases/download/v1.2.0/RestCycle-debug.apk"
                },
                {
                  "name": "RestCycle-release.apk",
                  "browser_download_url": "https://github.com/kicsirigo/RestCycle/releases/download/v1.2.0/RestCycle-release.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = GitHubUpdateChecker.parseReleaseJson(sampleJson)
        assertNotNull(parsed)
        assertEquals("1.2.0", parsed!!.latestVersion)
        assertEquals("v1.2.0", parsed.tagName)
        assertEquals(
            "https://github.com/kicsirigo/RestCycle/releases/download/v1.2.0/RestCycle-release.apk",
            parsed.apkDownloadUrl
        )
        assertEquals(
            "https://github.com/kicsirigo/RestCycle/releases/tag/v1.2.0",
            parsed.releasePageUrl
        )
    }

    @Test
    fun parseReleaseJson_fallsBackWhenAssetsMissingOrMalformed() {
        val noAssetsJson = """
            {
              "tag_name": "v1.3.0"
            }
        """.trimIndent()

        val parsed = GitHubUpdateChecker.parseReleaseJson(noAssetsJson)
        assertNotNull(parsed)
        assertEquals("1.3.0", parsed!!.latestVersion)
        assertEquals(GitHubUpdateChecker.FALLBACK_APK_URL, parsed.apkDownloadUrl)
        assertEquals(GitHubUpdateChecker.FALLBACK_RELEASE_URL, parsed.releasePageUrl)

        assertNull(GitHubUpdateChecker.parseReleaseJson("{}"))
        assertNull(GitHubUpdateChecker.parseReleaseJson("not json"))
    }

    @Test
    fun formatBytesMegabytes_formatsCleanDecimalMegabytes() {
        assertEquals("0.0 MB", GitHubUpdateChecker.formatBytesMegabytes(0L))
        assertEquals("1.0 MB", GitHubUpdateChecker.formatBytesMegabytes(1024L * 1024L))
        assertEquals("9.5 MB", GitHubUpdateChecker.formatBytesMegabytes((9.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun downloadUpdateWithProgress_streamsApkFollowingRedirectAndBuildsFileProviderIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val payload = ByteArray(48 * 1024) { (it % 127).toByte() }
        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            repeat(2) {
                runCatching {
                    server.accept().use { socket ->
                        val requestLine = socket.getInputStream().bufferedReader().readLine().orEmpty()
                        val out = socket.getOutputStream()
                        if (requestLine.contains("/latest.apk")) {
                            val resp = "HTTP/1.1 302 Found\r\nLocation: /cdn/RestCycle-release.apk\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            out.write(resp.toByteArray(Charsets.US_ASCII))
                        } else {
                            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: ${payload.size}\r\nConnection: close\r\n\r\n"
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
            val info = ReleaseUpdateInfo(
                latestVersion = "1.7.3",
                tagName = "v1.7.3",
                apkDownloadUrl = "http://127.0.0.1:$port/latest.apk",
                releasePageUrl = GitHubUpdateChecker.FALLBACK_RELEASE_URL
            )
            val progressReports = mutableListOf<Int>()
            val downloadedFile: File? = runBlocking {
                GitHubUpdateChecker.downloadUpdateWithProgress(context, info) { pct, _, _ ->
                    progressReports += pct
                }
            }
            assertNotNull(downloadedFile)
            assertTrue(downloadedFile!!.exists())
            assertEquals(payload.size.toLong(), downloadedFile.length())
            assertTrue(progressReports.isNotEmpty())
            assertEquals(100, progressReports.last())

            val installIntent = GitHubUpdateChecker.buildInstallIntent(context, downloadedFile)
            assertEquals(Intent.ACTION_VIEW, installIntent.action)
            assertEquals(GitHubUpdateChecker.APK_MIME_TYPE, installIntent.type)
            assertNotNull(installIntent.data)
            assertTrue(
                "Expected FileProvider content URI",
                installIntent.data.toString().startsWith("content://${context.packageName}.fileprovider/updates/")
            )
            assertTrue((installIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        } finally {
            runCatching { server.close() }
            serverThread.join(1000)
        }
    }
}
