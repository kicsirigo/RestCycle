package com.restcycle.app.unit

import com.restcycle.app.platform.update.GitHubUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
