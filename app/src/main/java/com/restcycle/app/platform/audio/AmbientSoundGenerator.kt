package com.restcycle.app.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AmbientSoundMode(
    val labelResId: Int,
    val emoji: String,
    val rawResId: Int?,
    val useOffsetTextureLayer: Boolean = false
) {
    OFF(R.string.ambient_sound_off, "🔇", null),
    BROWN_NOISE(R.string.ambient_sound_brown, "🌊", R.raw.ambient_brown_noise),
    STREAM_RAIN(R.string.ambient_sound_rain, "🌧️", R.raw.ambient_stream_rain),
    CAMPFIRE(R.string.ambient_sound_campfire, "🔥", R.raw.ambient_campfire),
    NIGHT_CRICKETS(R.string.ambient_sound_crickets, "🦗", R.raw.ambient_night_crickets),
    AMBIENT_PAD(R.string.ambient_sound_pad_432, "🎵", R.raw.ambient_pad_432),
    SINGING_BOWL(R.string.ambient_sound_singing_bowl, "🔔", R.raw.ambient_singing_bowl);

    fun next(): AmbientSoundMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

data class AmbientSoundPlaybackState(
    val mode: AmbientSoundMode = AmbientSoundMode.OFF,
    val volume: Float = 0.85f,
    val sleepTimerMinutes: Int = 0,
    val remainingSeconds: Long = 0L
) {
    val isPlaying: Boolean get() = mode != AmbientSoundMode.OFF
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
        _state.update { it.copy(mode = AmbientSoundMode.OFF, remainingSeconds = 0L) }
    }
}

/**
 * Plays a single selected ambient `.mp3` soundscape cleanly without mixing in unrelated sounds,
 * supporting dynamic volume scaling, optional half-track stereo evolution, and smooth fade-out.
 */
class AmbientSoundController {
    private var mixJob: Job? = null
    private var primaryPlayer: MediaPlayer? = null
    private var offsetSelfPlayer: MediaPlayer? = null

    @Volatile
    private var masterVolume: Float = 0.85f

    @Volatile
    private var fadeMultiplier: Float = 1.0f

    fun setVolume(volume: Float) {
        masterVolume = volume.coerceIn(0.1f, 1.0f)
        applyCurrentVolumes()
    }

    fun setFadeMultiplier(multiplier: Float) {
        fadeMultiplier = multiplier.coerceIn(0.0f, 1.0f)
        applyCurrentVolumes()
    }

    fun start(
        context: Context,
        scope: CoroutineScope,
        mode: AmbientSoundMode,
        initialVolume: Float = 0.85f
    ) {
        stop()
        val trackResId = mode.rawResId ?: return
        masterVolume = initialVolume.coerceIn(0.1f, 1.0f)
        fadeMultiplier = 1.0f
        val appContext = context.applicationContext

        mixJob = scope.launch(Dispatchers.Default) {
            var p1: MediaPlayer? = null
            var p2: MediaPlayer? = null
            try {
                val effective = (masterVolume * fadeMultiplier).coerceIn(0f, 1f)
                p1 = createLoopingPlayer(appContext, trackResId, initialVolume = 0.85f * effective, seekHalfway = false)
                p2 = if (mode.useOffsetTextureLayer) {
                    createLoopingPlayer(appContext, trackResId, initialVolume = 0.24f * effective, seekHalfway = true)
                } else {
                    null
                }
                primaryPlayer = p1
                offsetSelfPlayer = p2

                p1?.start()
                p2?.start()

                var elapsedSeconds = 0.0
                while (isActive) {
                    val (leftGain, rightGain, offsetGain) = computeMixGains(elapsedSeconds)
                    val scale = (masterVolume * fadeMultiplier).coerceIn(0f, 1f)
                    p1?.setVolume(leftGain * scale, rightGain * scale)
                    p2?.setVolume(offsetGain * scale, offsetGain * 0.90f * scale)

                    delay(MIX_STEP_MS)
                    elapsedSeconds = (elapsedSeconds + MIX_STEP_MS / 1000.0) % MIX_CYCLE_DURATION_SECONDS
                }
            } catch (_: Throwable) {
                // Gracefully ignore in headless/Robolectric test environments without audio hardware
            } finally {
                releasePlayers(p1, p2)
                primaryPlayer = null
                offsetSelfPlayer = null
            }
        }
    }

    fun stop() {
        mixJob?.cancel()
        mixJob = null
        releasePlayers(primaryPlayer, offsetSelfPlayer)
        primaryPlayer = null
        offsetSelfPlayer = null
    }

    private fun applyCurrentVolumes() {
        val scale = (masterVolume * fadeMultiplier).coerceIn(0f, 1f)
        runCatching {
            primaryPlayer?.setVolume(0.85f * scale, 0.85f * scale)
            offsetSelfPlayer?.setVolume(0.24f * scale, 0.22f * scale)
        }
    }

    private fun createLoopingPlayer(
        context: Context,
        rawResId: Int,
        initialVolume: Float,
        seekHalfway: Boolean
    ): MediaPlayer? {
        return runCatching {
            MediaPlayer.create(context, rawResId)?.apply {
                isLooping = true
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

    private fun releasePlayers(p1: MediaPlayer?, p2: MediaPlayer?) {
        listOf(p1, p2).forEach { player ->
            runCatching {
                if (player?.isPlaying == true) {
                    player.stop()
                }
            }
            runCatching {
                player?.release()
            }
        }
    }

    companion object {
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
