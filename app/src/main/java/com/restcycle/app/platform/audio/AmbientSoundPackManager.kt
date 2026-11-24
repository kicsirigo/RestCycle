package com.restcycle.app.platform.audio

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

sealed interface SoundPackStatus {
    data object NotDownloaded : SoundPackStatus
    data class Downloading(
        val progressPercent: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = -1L
    ) : SoundPackStatus

    data class Downloaded(
        val filePath: String,
        val sizeBytes: Long
    ) : SoundPackStatus

    data object Error : SoundPackStatus
}

/**
 * Manages on-demand downloading, local private storage (`filesDir/sound_packs/`),
 * cancellation, and deletion of full-length lossless FLAC ambient sound packs
 * hosted on the dedicated `sound-packs-v1` GitHub Release.
 */
object AmbientSoundPackManager {
    const val PACKS_BASE_URL =
        "https://github.com/kicsirigo/RestCycle/releases/download/sound-packs-v1"
    private const val SOUND_PACKS_DIR = "sound_packs"
    private const val MAX_REDIRECTS = 5

    private val _statuses = MutableStateFlow<Map<AmbientSoundMode, SoundPackStatus>>(emptyMap())
    val statuses: StateFlow<Map<AmbientSoundMode, SoundPackStatus>> = _statuses.asStateFlow()

    private val activeJobs = ConcurrentHashMap<AmbientSoundMode, Job>()

    fun packUrlFor(mode: AmbientSoundMode, baseUrl: String = PACKS_BASE_URL): String? {
        val fileName = mode.packFileName ?: return null
        return "${baseUrl.trimEnd('/')}/$fileName"
    }

    fun packFileFor(context: Context, mode: AmbientSoundMode): File? {
        val fileName = mode.packFileName ?: return null
        val dir = File(context.applicationContext.filesDir, SOUND_PACKS_DIR)
        return File(dir, fileName)
    }

    fun resolveDownloadedFile(context: Context, mode: AmbientSoundMode): File? {
        val file = packFileFor(context, mode) ?: return null
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun refreshStatuses(context: Context) {
        val appContext = context.applicationContext
        val current = _statuses.value
        val updated = LinkedHashMap<AmbientSoundMode, SoundPackStatus>()
        AmbientSoundMode.entries.forEach { mode ->
            if (mode == AmbientSoundMode.OFF) return@forEach
            val existing = current[mode]
            if (existing is SoundPackStatus.Downloading && activeJobs[mode]?.isActive == true) {
                updated[mode] = existing
            } else {
                val file = resolveDownloadedFile(appContext, mode)
                updated[mode] = if (file != null) {
                    SoundPackStatus.Downloaded(file.absolutePath, file.length())
                } else {
                    SoundPackStatus.NotDownloaded
                }
            }
        }
        _statuses.value = updated
    }

    fun totalDownloadedBytes(
        statusMap: Map<AmbientSoundMode, SoundPackStatus> = _statuses.value
    ): Long {
        return statusMap.values
            .filterIsInstance<SoundPackStatus.Downloaded>()
            .sumOf { it.sizeBytes }
    }

    fun formatFileSize(bytes: Long): String {
        val mb = bytes.coerceAtLeast(0L) / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun startDownload(
        context: Context,
        scope: CoroutineScope,
        mode: AmbientSoundMode,
        baseUrl: String = PACKS_BASE_URL,
        onCompleted: (File) -> Unit = {}
    ) {
        val url = packUrlFor(mode, baseUrl) ?: return
        val appContext = context.applicationContext
        activeJobs.remove(mode)?.cancel()
        setStatus(mode, SoundPackStatus.Downloading(progressPercent = 0))

        val job = scope.launch {
            val file = downloadPackSync(
                context = appContext,
                mode = mode,
                downloadUrl = url
            ) { pct, downloaded, total ->
                setStatus(
                    mode,
                    SoundPackStatus.Downloading(
                        progressPercent = pct,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                )
            }
            activeJobs.remove(mode)
            if (file != null) {
                setStatus(mode, SoundPackStatus.Downloaded(file.absolutePath, file.length()))
                onCompleted(file)
            } else {
                setStatus(mode, SoundPackStatus.Error)
            }
        }
        activeJobs[mode] = job
    }

    fun cancelDownload(context: Context, mode: AmbientSoundMode) {
        activeJobs.remove(mode)?.cancel()
        val finalFile = packFileFor(context, mode)
        if (finalFile != null) {
            runCatching { File("${finalFile.absolutePath}.part").delete() }
        }
        val existing = resolveDownloadedFile(context, mode)
        setStatus(
            mode,
            if (existing != null) {
                SoundPackStatus.Downloaded(existing.absolutePath, existing.length())
            } else {
                SoundPackStatus.NotDownloaded
            }
        )
    }

    fun deletePack(context: Context, mode: AmbientSoundMode): Boolean {
        activeJobs.remove(mode)?.cancel()
        val finalFile = packFileFor(context, mode) ?: return false
        runCatching { File("${finalFile.absolutePath}.part").delete() }
        val deleted = if (finalFile.exists()) finalFile.delete() else false
        setStatus(mode, SoundPackStatus.NotDownloaded)
        return deleted
    }

    suspend fun downloadPackSync(
        context: Context,
        mode: AmbientSoundMode,
        downloadUrl: String,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        val finalFile = packFileFor(context, mode) ?: return@withContext null
        finalFile.parentFile?.mkdirs()
        val partFile = File("${finalFile.absolutePath}.part")
        runCatching { partFile.delete() }

        var connection: HttpURLConnection? = null
        try {
            connection = openFollowingRedirects(downloadUrl) ?: return@withContext null
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
                        if (pct != lastReportedPct || (downloaded - lastReportedBytes) >= 128 * 1024L) {
                            lastReportedPct = pct
                            lastReportedBytes = downloaded
                            onProgress(pct, downloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (downloaded <= 0L || !partFile.exists()) {
                runCatching { partFile.delete() }
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
                connectTimeout = 15_000
                readTimeout = 30_000
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

    private fun setStatus(mode: AmbientSoundMode, status: SoundPackStatus) {
        _statuses.update { map ->
            map + (mode to status)
        }
    }
}
