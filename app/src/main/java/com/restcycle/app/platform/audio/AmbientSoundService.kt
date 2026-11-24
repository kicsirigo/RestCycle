package com.restcycle.app.platform.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.restcycle.app.MainActivity
import com.restcycle.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground mediaPlayback service that keeps ambient sleep audio playing seamlessly when the
 * user turns the screen off or switches apps, with an optional sleep timer and 30-second fade-out.
 */
class AmbientSoundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val controller = AmbientSoundController()
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            AmbientSoundStateHolder.state
                .map { it.volume }
                .distinctUntilChanged()
                .collect { controller.setVolume(it) }
        }
        serviceScope.launch {
            AmbientSoundStateHolder.state
                .map { it.activeLayers }
                .distinctUntilChanged()
                .collect { layers ->
                    layers.forEach { (mode, vol) ->
                        controller.setLayerVolume(mode, vol)
                    }
                }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_STOP
        when (action) {
            ACTION_STOP -> {
                stopPlaybackAndSelf()
                return START_NOT_STICKY
            }

            ACTION_PLAY -> {
                val modeName = intent?.getStringExtra(EXTRA_MODE_NAME)
                val mode = AmbientSoundMode.entries.firstOrNull { it.name == modeName }
                    ?: AmbientSoundMode.OFF
                if (mode == AmbientSoundMode.OFF) {
                    stopPlaybackAndSelf()
                    return START_NOT_STICKY
                }
                val volume = (intent?.getFloatExtra(EXTRA_VOLUME, DEFAULT_VOLUME) ?: DEFAULT_VOLUME)
                    .coerceIn(MIN_VOLUME, MAX_VOLUME)
                val timerMinutes = (intent?.getIntExtra(EXTRA_TIMER_MINUTES, 0) ?: 0)
                    .coerceAtLeast(0)

                val singleLayers = mapOf(mode to AmbientSoundController.DEFAULT_LAYER_VOLUME)
                syncMixPlayback(singleLayers, volume, timerMinutes)
            }

            ACTION_SYNC_MIX -> {
                val current = AmbientSoundStateHolder.state.value
                val layers = current.activeLayers
                if (layers.isEmpty()) {
                    stopPlaybackAndSelf()
                    return START_NOT_STICKY
                }
                syncMixPlayback(layers, current.volume, current.sleepTimerMinutes)
            }

            ACTION_UPDATE_VOLUME -> {
                val current = AmbientSoundStateHolder.state.value
                val volume = (intent?.getFloatExtra(EXTRA_VOLUME, current.volume) ?: current.volume)
                    .coerceIn(MIN_VOLUME, MAX_VOLUME)
                controller.setVolume(volume)
                AmbientSoundStateHolder.update { it.copy(volume = volume) }
                if (current.isPlaying) {
                    ensureNotificationChannel()
                    startForegroundCompat(buildNotification(current.mode, current.remainingSeconds))
                } else {
                    stopSelf()
                }
            }

            ACTION_UPDATE_TIMER -> {
                val current = AmbientSoundStateHolder.state.value
                if (!current.isPlaying) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val timerMinutes = (intent?.getIntExtra(EXTRA_TIMER_MINUTES, 0) ?: 0)
                    .coerceAtLeast(0)
                ensureNotificationChannel()
                val remaining = if (timerMinutes > 0) timerMinutes * 60L else 0L
                startForegroundCompat(buildNotification(current.mode, remaining))
                restartTimer(current.mode, timerMinutes)
            }
        }
        return START_NOT_STICKY
    }

    private fun syncMixPlayback(
        layers: Map<AmbientSoundMode, Float>,
        volume: Float,
        timerMinutes: Int
    ) {
        val primaryMode = layers.keys.firstOrNull() ?: run {
            stopPlaybackAndSelf()
            return
        }
        ensureNotificationChannel()
        val allPreview = layers.keys.all { AmbientSoundPackManager.resolveDownloadedFile(this, it) == null }
        val effectiveTimer = if (allPreview) 0 else timerMinutes
        val remaining = if (effectiveTimer > 0) effectiveTimer * 60L else 0L
        startForegroundCompat(buildNotification(primaryMode, remaining))

        AmbientSoundStateHolder.update {
            AmbientSoundPlaybackState(
                mode = primaryMode,
                volume = volume.coerceIn(MIN_VOLUME, MAX_VOLUME),
                sleepTimerMinutes = timerMinutes,
                remainingSeconds = remaining,
                isPreviewPlayback = allPreview,
                activeLayers = layers
            )
        }
        controller.syncLayers(
            context = this,
            scope = serviceScope,
            layers = layers,
            initialMasterVolume = volume,
            onLayerPreviewCompleted = { finishedMode ->
                val remainingLayers = AmbientSoundStateHolder.state.value.activeLayers - finishedMode
                if (remainingLayers.isEmpty()) {
                    stopPlaybackAndSelf()
                } else {
                    val nextPrimary = remainingLayers.keys.first()
                    AmbientSoundStateHolder.update {
                        it.copy(mode = nextPrimary, activeLayers = remainingLayers)
                    }
                    updateNotification(nextPrimary, AmbientSoundStateHolder.state.value.remainingSeconds)
                }
            }
        )
        restartTimer(primaryMode, effectiveTimer)
    }

    private fun restartTimer(mode: AmbientSoundMode, timerMinutes: Int) {
        timerJob?.cancel()
        timerJob = null
        controller.setFadeMultiplier(1.0f)

        if (timerMinutes <= 0) {
            AmbientSoundStateHolder.update {
                it.copy(sleepTimerMinutes = 0, remainingSeconds = 0L)
            }
            updateNotification(mode, 0L)
            return
        }

        val totalSeconds = timerMinutes * 60L
        AmbientSoundStateHolder.update {
            it.copy(sleepTimerMinutes = timerMinutes, remainingSeconds = totalSeconds)
        }
        updateNotification(mode, totalSeconds)

        timerJob = serviceScope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0L) {
                val fade = AmbientSoundController.computeFadeMultiplier(
                    timerActive = true,
                    remainingSeconds = remaining
                )
                controller.setFadeMultiplier(fade)
                AmbientSoundStateHolder.update { it.copy(remainingSeconds = remaining) }
                if (remaining % 15L == 0L || remaining == totalSeconds) {
                    updateNotification(mode, remaining)
                }
                delay(1000L)
                remaining--
            }
            if (isActive) {
                stopPlaybackAndSelf()
            }
        }
    }

    private fun stopPlaybackAndSelf() {
        timerJob?.cancel()
        timerJob = null
        controller.stop()
        AmbientSoundStateHolder.stopPlayback()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sound_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(mode: AmbientSoundMode, remainingSeconds: Long) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(mode, remainingSeconds))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(mode: AmbientSoundMode, remainingSeconds: Long): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, AmbientSoundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundLabel = getString(mode.labelResId)
        val title = getString(R.string.sound_notification_playing, soundLabel)
        val subText = if (remainingSeconds > 0L) {
            getString(R.string.sound_timer_remaining, formatRemainingTime(remainingSeconds))
        } else {
            getString(R.string.sound_background_hint)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, getString(R.string.sound_notification_stop), stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        timerJob = null
        controller.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "restcycle_ambient_sound"
        const val NOTIFICATION_ID = 5310

        const val ACTION_PLAY = "com.restcycle.app.action.AMBIENT_PLAY"
        const val ACTION_SYNC_MIX = "com.restcycle.app.action.AMBIENT_SYNC_MIX"
        const val ACTION_UPDATE_VOLUME = "com.restcycle.app.action.AMBIENT_VOLUME"
        const val ACTION_UPDATE_TIMER = "com.restcycle.app.action.AMBIENT_TIMER"
        const val ACTION_STOP = "com.restcycle.app.action.AMBIENT_STOP"

        const val EXTRA_MODE_NAME = "extra_mode_name"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_TIMER_MINUTES = "extra_timer_minutes"

        const val DEFAULT_VOLUME = 0.85f
        const val MIN_VOLUME = 0.0f
        const val MAX_VOLUME = 1.0f

        val SLEEP_TIMER_OPTIONS = listOf(0, 15, 30, 45, 60)

        fun formatRemainingTime(remainingSeconds: Long): String {
            val safeSeconds = remainingSeconds.coerceAtLeast(0L)
            val mins = safeSeconds / 60L
            val secs = safeSeconds % 60L
            return "%d:%02d".format(mins, secs)
        }

        fun play(
            context: Context,
            mode: AmbientSoundMode,
            volume: Float = AmbientSoundStateHolder.state.value.volume,
            timerMinutes: Int = AmbientSoundStateHolder.state.value.sleepTimerMinutes
        ) {
            if (mode == AmbientSoundMode.OFF) {
                stop(context)
                return
            }
            val clampedVolume = volume.coerceIn(MIN_VOLUME, MAX_VOLUME)
            val safeTimer = timerMinutes.coerceAtLeast(0)
            val isPreview = AmbientSoundPackManager.resolveDownloadedFile(context, mode) == null
            val remaining = if (!isPreview && safeTimer > 0) safeTimer * 60L else 0L

            AmbientSoundStateHolder.update {
                AmbientSoundPlaybackState(
                    mode = mode,
                    volume = clampedVolume,
                    sleepTimerMinutes = safeTimer,
                    remainingSeconds = remaining,
                    isPreviewPlayback = isPreview,
                    activeLayers = mapOf(mode to AmbientSoundController.DEFAULT_LAYER_VOLUME)
                )
            }

            val intent = Intent(context, AmbientSoundService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_MODE_NAME, mode.name)
                putExtra(EXTRA_VOLUME, clampedVolume)
                putExtra(EXTRA_TIMER_MINUTES, safeTimer)
            }
            runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            }
        }

        fun toggleLayer(context: Context, mode: AmbientSoundMode) {
            if (mode == AmbientSoundMode.OFF) {
                stop(context)
                return
            }
            val current = AmbientSoundStateHolder.state.value
            val updated = LinkedHashMap(current.activeLayers)
            if (updated.containsKey(mode)) {
                updated.remove(mode)
            } else {
                if (updated.size >= AmbientSoundController.MAX_MIX_LAYERS) {
                    val oldestKey = updated.keys.firstOrNull()
                    if (oldestKey != null) updated.remove(oldestKey)
                }
                updated[mode] = AmbientSoundController.DEFAULT_LAYER_VOLUME
            }
            applyMix(context, updated)
        }

        fun updateLayerVolume(mode: AmbientSoundMode, layerVolume: Float) {
            val clamped = layerVolume.coerceIn(0.10f, 1.0f)
            AmbientSoundStateHolder.update { current ->
                if (!current.activeLayers.containsKey(mode)) {
                    current
                } else {
                    val updated = LinkedHashMap(current.activeLayers)
                    updated[mode] = clamped
                    current.copy(activeLayers = updated)
                }
            }
        }

        fun applyMix(context: Context, layers: Map<AmbientSoundMode, Float>) {
            val cleaned = layers
                .filterKeys { it != AmbientSoundMode.OFF }
                .entries
                .take(AmbientSoundController.MAX_MIX_LAYERS)
                .associateTo(LinkedHashMap()) { it.key to it.value.coerceIn(0.10f, 1.0f) }

            if (cleaned.isEmpty()) {
                stop(context)
                return
            }
            val current = AmbientSoundStateHolder.state.value
            val primary = cleaned.keys.first()
            val allPreview = cleaned.keys.all { AmbientSoundPackManager.resolveDownloadedFile(context, it) == null }
            val safeTimer = current.sleepTimerMinutes.coerceAtLeast(0)
            val remaining = if (!allPreview && safeTimer > 0) safeTimer * 60L else 0L

            AmbientSoundStateHolder.update {
                it.copy(
                    mode = primary,
                    remainingSeconds = remaining,
                    isPreviewPlayback = allPreview,
                    activeLayers = cleaned
                )
            }

            val intent = Intent(context, AmbientSoundService::class.java).apply {
                action = ACTION_SYNC_MIX
            }
            runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            }
        }

        fun updateVolume(@Suppress("UNUSED_PARAMETER") context: Context, volume: Float) {
            val clamped = volume.coerceIn(MIN_VOLUME, MAX_VOLUME)
            AmbientSoundStateHolder.update { it.copy(volume = clamped) }
        }

        fun updateTimer(context: Context, timerMinutes: Int) {
            val safeTimer = timerMinutes.coerceAtLeast(0)
            val wasPlaying = AmbientSoundStateHolder.state.value.isPlaying
            val remaining = if (wasPlaying && safeTimer > 0) safeTimer * 60L else 0L
            AmbientSoundStateHolder.update {
                it.copy(sleepTimerMinutes = safeTimer, remainingSeconds = remaining)
            }
            if (!wasPlaying) return

            val intent = Intent(context, AmbientSoundService::class.java).apply {
                action = ACTION_UPDATE_TIMER
                putExtra(EXTRA_TIMER_MINUTES, safeTimer)
            }
            runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            }
        }

        fun stop(context: Context) {
            AmbientSoundStateHolder.stopPlayback()
            val intent = Intent(context, AmbientSoundService::class.java)
            runCatching {
                context.applicationContext.stopService(intent)
            }
        }
    }
}
