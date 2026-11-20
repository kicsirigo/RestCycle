package com.restcycle.app.platform.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseUpdateInfo(
    val latestVersion: String,
    val tagName: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateUiState
    data class Downloading(val info: ReleaseUpdateInfo) : UpdateUiState
    data object Error : UpdateUiState
}

/**
 * Checks GitHub Releases for a newer version of RestCycle and initiates user-requested
 * APK downloads via Android DownloadManager (with browser fallback). Never forces updates.
 */
object GitHubUpdateChecker {
    const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/kicsirigo/RestCycle/releases/latest"
    const val FALLBACK_APK_URL =
        "https://github.com/kicsirigo/RestCycle/releases/latest/download/RestCycle-release.apk"
    const val FALLBACK_RELEASE_URL =
        "https://github.com/kicsirigo/RestCycle/releases/latest"

    fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
            .trim()
    }

    fun isNewerVersion(installedVersion: String, remoteVersion: String): Boolean {
        val installedParts = parseVersionParts(installedVersion) ?: return false
        val remoteParts = parseVersionParts(remoteVersion) ?: return false
        val maxLen = maxOf(installedParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val inst = installedParts.getOrElse(i) { 0 }
            val rem = remoteParts.getOrElse(i) { 0 }
            if (rem != inst) {
                return rem > inst
            }
        }
        return false
    }

    private fun parseVersionParts(raw: String): List<Int>? {
        val normalized = normalizeVersion(raw)
        if (normalized.isEmpty()) return null
        val segments = normalized.split('.')
        val numbers = segments.map { it.toIntOrNull() ?: return null }
        return if (numbers.isEmpty()) null else numbers
    }

    fun parseReleaseJson(json: String): ReleaseUpdateInfo? {
        return try {
            val root = JSONObject(json)
            val tagName = root.optString("tag_name", "").trim()
            if (tagName.isEmpty()) return null
            val latestVersion = normalizeVersion(tagName)
            if (parseVersionParts(latestVersion) == null) return null

            val releasePageUrl = root.optString("html_url", "")
                .takeIf { it.startsWith("https://") }
                ?: FALLBACK_RELEASE_URL

            val assets = root.optJSONArray("assets")
            var releaseApkUrl: String? = null
            var anyApkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val url = asset.optString("browser_download_url", "")
                    if (!url.startsWith("https://")) continue
                    if (name.equals("RestCycle-release.apk", ignoreCase = true)) {
                        releaseApkUrl = url
                        break
                    }
                    if (anyApkUrl == null && name.endsWith(".apk", ignoreCase = true)) {
                        anyApkUrl = url
                    }
                }
            }

            ReleaseUpdateInfo(
                latestVersion = latestVersion,
                tagName = tagName,
                apkDownloadUrl = releaseApkUrl ?: anyApkUrl ?: FALLBACK_APK_URL,
                releasePageUrl = releasePageUrl
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkForUpdate(installedVersion: String): UpdateUiState = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "RestCycle-Android")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateUiState.Error
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseInfo = parseReleaseJson(body) ?: return@withContext UpdateUiState.Error
            if (isNewerVersion(installedVersion, releaseInfo.latestVersion)) {
                UpdateUiState.UpdateAvailable(releaseInfo)
            } else {
                UpdateUiState.UpToDate(normalizeVersion(installedVersion))
            }
        } catch (_: Exception) {
            UpdateUiState.Error
        } finally {
            connection?.disconnect()
        }
    }

    fun downloadUpdateApk(context: Context, info: ReleaseUpdateInfo): Boolean {
        try {
            val dm = context.getSystemService(DownloadManager::class.java)
            if (dm != null) {
                val fileName = "RestCycle-v${info.latestVersion}.apk"
                val request = DownloadManager.Request(Uri.parse(info.apkDownloadUrl))
                    .setTitle("RestCycle v${info.latestVersion}")
                    .setDescription(fileName)
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                dm.enqueue(request)
                return true
            }
        } catch (_: Exception) {
            // Fall through to browser download
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkDownloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
