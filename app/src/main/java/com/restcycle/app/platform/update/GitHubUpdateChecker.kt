package com.restcycle.app.platform.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ReleaseUpdateInfo(
    val latestVersion: String,
    val tagName: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String
)

enum class InstallLaunchResult {
    LAUNCHED_INSTALLER,
    OPENED_PERMISSION_SETTINGS,
    FAILED
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateUiState
    data class Downloading(
        val info: ReleaseUpdateInfo,
        val progressPercent: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = -1L
    ) : UpdateUiState

    data class ReadyToInstall(
        val info: ReleaseUpdateInfo,
        val apkFilePath: String,
        val needsInstallPermission: Boolean = false
    ) : UpdateUiState

    data object Error : UpdateUiState
}

/**
 * Checks GitHub Releases for a newer version of RestCycle, downloads the signed release APK
 * inside the app with live progress & cancellation, and launches Android's package installer
 * directly via FileProvider.
 */
object GitHubUpdateChecker {
    const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/kicsirigo/RestCycle/releases/latest"
    const val FALLBACK_APK_URL =
        "https://github.com/kicsirigo/RestCycle/releases/latest/download/RestCycle-release.apk"
    const val FALLBACK_RELEASE_URL =
        "https://github.com/kicsirigo/RestCycle/releases/latest"
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val UPDATES_CACHE_DIR = "updates"
    private const val MAX_REDIRECTS = 5

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

    fun formatBytesMegabytes(bytes: Long): String {
        val mb = bytes.coerceAtLeast(0L) / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
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

    /**
     * Downloads the release APK into app-private cache (`cacheDir/updates/`) with redirect
     * handling, cancellation cleanup, and throttled progress updates.
     */
    suspend fun downloadUpdateWithProgress(
        context: Context,
        info: ReleaseUpdateInfo,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, UPDATES_CACHE_DIR).apply { mkdirs() }
        updatesDir.listFiles()?.forEach { old -> runCatching { old.delete() } }

        val finalFile = File(updatesDir, "RestCycle-v${info.latestVersion}.apk")
        val partFile = File(updatesDir, "RestCycle-v${info.latestVersion}.apk.part")

        var connection: HttpURLConnection? = null
        try {
            connection = openFollowingRedirects(info.apkDownloadUrl) ?: return@withContext null
            val totalBytes = connection.contentLengthLong
            var downloaded = 0L
            var lastReportedPct = -1
            var lastReportedBytes = 0L

            onProgress(0, 0L, totalBytes)

            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val pct = if (totalBytes > 0L) {
                            ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        if (pct != lastReportedPct || (downloaded - lastReportedBytes) >= 64 * 1024L) {
                            lastReportedPct = pct
                            lastReportedBytes = downloaded
                            onProgress(pct, downloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (downloaded <= 0L || !partFile.exists()) {
                partFile.delete()
                return@withContext null
            }
            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            onProgress(100, downloaded, if (totalBytes > 0L) totalBytes else downloaded)
            finalFile
        } catch (ce: CancellationException) {
            runCatching { partFile.delete() }
            throw ce
        } catch (_: Exception) {
            runCatching { partFile.delete() }
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun openFollowingRedirects(startUrl: String): HttpURLConnection? {
        var currentUrl = startUrl
        repeat(MAX_REDIRECTS + 1) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "RestCycle-Android")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) return null
                currentUrl = URL(URL(currentUrl), location).toString()
            } else if (code == HttpURLConnection.HTTP_OK) {
                return conn
            } else {
                conn.disconnect()
                return null
            }
        }
        return null
    }

    fun buildInstallIntent(context: Context, apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): InstallLaunchResult {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return InstallLaunchResult.FAILED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return try {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                InstallLaunchResult.OPENED_PERMISSION_SETTINGS
            } catch (_: Exception) {
                InstallLaunchResult.FAILED
            }
        }
        return try {
            context.startActivity(buildInstallIntent(context, apkFile))
            InstallLaunchResult.LAUNCHED_INSTALLER
        } catch (_: Exception) {
            InstallLaunchResult.FAILED
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
                    .setMimeType(APK_MIME_TYPE)
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
