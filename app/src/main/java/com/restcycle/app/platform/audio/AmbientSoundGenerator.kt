package com.restcycle.app.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import com.restcycle.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AmbientSoundMode(
    val labelResId: Int,
    val rawResId: Int?,
    val packFileName: String? = null,
    val useOffsetTextureLayer: Boolean = false
) {
    OFF(R.string.ambient_sound_off, null, null),
    BROWN_NOISE(R.string.ambient_sound_brown, R.raw.ambient_brown_noise, "ocean_waves_full.flac"),
    STREAM_RAIN(R.string.ambient_sound_rain, R.raw.ambient_stream_rain, "thunderstorm_full.flac"),
    CAMPFIRE(R.string.ambient_sound_campfire, R.raw.ambient_campfire, "fireplace_full.flac"),
    NIGHT_CRICKETS(R.string.ambient_sound_crickets, R.raw.ambient_night_crickets, "night_crickets_full.flac"),
    AMBIENT_PAD(R.string.ambient_sound_pad_432, R.raw.ambient_pad_432, "ambient_pad_432_full.flac"),
    SINGING_BOWL(R.string.ambient_sound_singing_bowl, R.raw.ambient_singing_bowl, "singing_bowl_full.flac");

    fun next(): AmbientSoundMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

data class AmbientSoundPlaybackState(
    val mode: AmbientSoundMode = AmbientSoundMode.OFF,
    val volume: Float = 0.85f,
    val sleepTimerMinutes: Int = 0,
    val remainingSeconds: Long = 0L,
    val isPreviewPlayback: Boolean = false,
    val activeLayers: Map<AmbientSoundMode, Float> = emptyMap()
) {
    val isPlaying: Boolean get() = activeLayers.isNotEmpty() || mode != AmbientSoundMode.OFF
}

/**
 * Shared reactive state for ambient playback controlled by [AmbientSoundService],
 * keeping UI dialogs and status pills in sync across all tabs and background lifecycle events.
 */
object AmbientSoundStateHolder {
    private val _state = MutableStateFlow(AmbientSoundPlaybackState())
    val state: StateFlow<AmbientSoundPlaybackState> = _state.asStateFlow()

    fun update(transform: (AmbientSoundPlaybackState) -> AmbientSoundPlaybackState) {
        _state.update(transform)
    }

    fun stopPlayback() {
        _state.update {
            it.copy(
                mode = AmbientSoundMode.OFF,
                remainingSeconds = 0L,
                isPreviewPlayback = false,
                activeLayers = emptyMap()
            )
        }
    }
}

/**
 * Multi-track ambient audio controller supporting up to [MAX_MIX_LAYERS] simultaneous
 * lossless FLAC (or 15s offline preview) tracks with independent per-track perceptual volume,
 * master volume scaling, PARTIAL_WAKE_LOCK, and smooth 30-second sleep timer fade-out.
 */
class AmbientSoundController {
    private var mixJob: Job? = null
    private val layerPlayers = ConcurrentHashMap<AmbientSoundMode, MediaPlayer>()
    private val layerVolumes = ConcurrentHashMap<AmbientSoundMode, Float>()

    @Volatile
    private var masterVolume: Float = 0.85f

    @Volatile
    private var fadeMultiplier: Float = 1.0f

    fun setVolume(volume: Float) {
        masterVolume = volume.coerceIn(0.0f, 1.0f)
        applyCurrentVolumes()
    }

    fun setFadeMultiplier(multiplier: Float) {
        fadeMultiplier = multiplier.coerceIn(0.0f, 1.0f)
        applyCurrentVolumes()
    }

    fun setLayerVolume(mode: AmbientSoundMode, volume: Float) {
        val clamped = volume.coerceIn(0.10f, 1.0f)
        layerVolumes[mode] = clamped
        val scale = (clamped * masterVolume * fadeMultiplier).coerceIn(0f, 1f)
        val gain = scale * scale
        runCatching {
            layerPlayers[mode]?.setVolume(gain, gain)
        }
    }

    fun syncLayers(
        context: Context,
        scope: CoroutineScope,
        layers: Map<AmbientSoundMode, Float>,
        initialMasterVolume: Float = masterVolume,
        onLayerPreviewCompleted: (AmbientSoundMode) -> Unit = {}
    ) {
        val target = layers
            .filterKeys { it != AmbientSoundMode.OFF && it.rawResId != null }
            .entries
            .take(MAX_MIX_LAYERS)
            .associate { it.key to it.value.coerceIn(0.10f, 1.0f) }

        if (target.isEmpty()) {
            stop()
            return
        }

        masterVolume = initialMasterVolume.coerceIn(0.0f, 1.0f)
        val appContext = context.applicationContext

        // Release any removed layers
        val toRemove = layerPlayers.keys.filter { it !in target.keys }
        toRemove.forEach { removedMode ->
            layerVolumes.remove(removedMode)
            layerPlayers.remove(removedMode)?.let { releasePlayer(it) }
        }

        // Update volumes for already-running layers
        target.forEach { (mode, vol) ->
            layerVolumes[mode] = vol
            if (layerPlayers.containsKey(mode)) {
                setLayerVolume(mode, vol)
            }
        }

        // Launch any newly added layers asynchronously without disturbing existing layers
        val toAdd = target.filterKeys { !layerPlayers.containsKey(it) }
        if (toAdd.isNotEmpty()) {
            scope.launch(Dispatchers.Default) {
                toAdd.forEach { (mode, vol) ->
                    val rawId = mode.rawResId ?: return@forEach
                    val downloadedFile = AmbientSoundPackManager.resolveDownloadedFile(appContext, mode)
                    val scale = (vol * masterVolume * fadeMultiplier).coerceIn(0f, 1f)
                    val gain = scale * scale
                    val player = if (downloadedFile != null) {
                        createFilePlayer(
                            context = appContext,
                            filePath = downloadedFile.absolutePath,
                            initialVolume = gain,
                            looping = true,
                            seekHalfway = false
                        )
                    } else {
                        createRawPlayer(
                            context = appContext,
                            rawResId = rawId,
                            initialVolume = gain,
                            looping = false,
                            seekHalfway = false
                        )?.apply {
                            setOnCompletionListener {
                                layerPlayers.remove(mode)?.let { releasePlayer(it) }
                                layerVolumes.remove(mode)
                                onLayerPreviewCompleted(mode)
                            }
                        }
                    }
                    if (player != null) {
                        layerPlayers.put(mode, player)?.let { old -> releasePlayer(old) }
                        layerVolumes[mode] = vol
                        runCatching {
                            player.start()
                            val currentScale = ((layerVolumes[mode] ?: vol) * masterVolume * fadeMultiplier).coerceIn(0f, 1f)
                            val currentGain = currentScale * currentScale
                            player.setVolume(currentGain, currentGain)
                        }
                    }
                }
            }
        }
    }

    fun start(
        context: Context,
        scope: CoroutineScope,
        mode: AmbientSoundMode,
        initialVolume: Float = 0.85f,
        onPreviewCompleted: () -> Unit = {}
    ) {
        stop()
        if (mode == AmbientSoundMode.OFF || mode.rawResId == null) return
        fadeMultiplier = 1.0f
        syncLayers(
            context = context,
            scope = scope,
            layers = mapOf(mode to DEFAULT_LAYER_VOLUME),
            initialMasterVolume = initialVolume,
            onLayerPreviewCompleted = { onPreviewCompleted() }
        )
    }

    fun stop() {
        mixJob?.cancel()
        mixJob = null
        val snapshot = layerPlayers.values.toList()
        layerPlayers.clear()
        layerVolumes.clear()
        snapshot.forEach { releasePlayer(it) }
    }

    private fun applyCurrentVolumes() {
        val master = (masterVolume * fadeMultiplier).coerceIn(0f, 1f)
        layerPlayers.forEach { (mode, player) ->
            val layerVol = layerVolumes[mode] ?: DEFAULT_LAYER_VOLUME
            val scale = (layerVol * master).coerceIn(0f, 1f)
            val gain = scale * scale
            runCatching {
                player.setVolume(gain, gain)
            }
        }
    }

    private fun createFilePlayer(
        context: Context,
        filePath: String,
        initialVolume: Float,
        looping: Boolean,
        seekHalfway: Boolean
    ): MediaPlayer? {
        return runCatching {
            MediaPlayer().apply {
                runCatching { setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK) }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(filePath)
                isLooping = looping
                prepare()
                if (seekHalfway && duration > 2000) {
                    seekTo(duration / 2)
                }
                setVolume(initialVolume, initialVolume)
            }
        }.getOrNull()
    }

    private fun createRawPlayer(
        context: Context,
        rawResId: Int,
        initialVolume: Float,
        looping: Boolean,
        seekHalfway: Boolean
    ): MediaPlayer? {
        return runCatching {
            MediaPlayer.create(context, rawResId)?.apply {
                runCatching { setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK) }
                isLooping = looping
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (seekHalfway && duration > 2000) {
                    seekTo(duration / 2)
                }
                setVolume(initialVolume, initialVolume)
            }
        }.getOrNull()
    }

    private fun releasePlayer(player: MediaPlayer?) {
        runCatching {
            if (player?.isPlaying == true) {
                player.stop()
            }
        }
        runCatching {
            player?.release()
        }
    }

    companion object {
        const val MAX_MIX_LAYERS = 3
        const val DEFAULT_LAYER_VOLUME = 0.80f
        // 2.5 hours (150 minutes = 9000 seconds) per macro stereo evolution cycle before repeat
        const val MIX_CYCLE_DURATION_SECONDS = 150 * 60.0
        const val FADE_OUT_DURATION_SECONDS = 30L
        private const val MIX_STEP_MS = 500L

        /**
         * Modulates primary and half-track time-offset layers of the same selected sound
         * over a 2.5-hour slow envelope so the base `.mp3` never sounds mechanically repetitive.
         */
        fun computeMixGains(elapsedSeconds: Double): Triple<Float, Float, Float> {
            val cyclePhase = 2.0 * PI * (elapsedSeconds % MIX_CYCLE_DURATION_SECONDS) / MIX_CYCLE_DURATION_SECONDS
            val waveSlow = sin(2.0 * PI * elapsedSeconds / 173.0)
            val wavePan = sin(2.0 * PI * elapsedSeconds / 89.0)
            val waveDeep = cos(cyclePhase * 3.0)

            val primaryBase = (0.74 + 0.14 * waveSlow + 0.06 * waveDeep).coerceIn(0.55, 0.94)
            val leftGain = (primaryBase * (1.0 + 0.06 * wavePan)).toFloat().coerceIn(0.50f, 0.96f)
            val rightGain = (primaryBase * (1.0 - 0.06 * wavePan)).toFloat().coerceIn(0.50f, 0.96f)
            val offsetGain = (0.24 + 0.12 * (-waveSlow) + 0.06 * cos(2.0 * PI * elapsedSeconds / 311.0))
                .toFloat()
                .coerceIn(0.08f, 0.42f)

            return Triple(leftGain, rightGain, offsetGain)
        }

        /**
         * Returns a smooth linear fade-out factor across the final 30 seconds of a sleep timer.
         */
        fun computeFadeMultiplier(
            timerActive: Boolean,
            remainingSeconds: Long,
            fadeSeconds: Long = FADE_OUT_DURATION_SECONDS
        ): Float {
            if (!timerActive) return 1.0f
            if (remainingSeconds <= 0L) return 0.0f
            if (remainingSeconds >= fadeSeconds) return 1.0f
            return (remainingSeconds.toFloat() / fadeSeconds.toFloat()).coerceIn(0.0f, 1.0f)
        }
    }
}
