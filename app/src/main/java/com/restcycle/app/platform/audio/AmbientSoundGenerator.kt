package com.restcycle.app.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.restcycle.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    BROWN_NOISE(R.string.ambient_sound_brown, "🌊", R.raw.ambient_brown_noise, useOffsetTextureLayer = true),
    STREAM_RAIN(R.string.ambient_sound_rain, "🌧️", R.raw.ambient_stream_rain, useOffsetTextureLayer = true),
    CAMPFIRE(R.string.ambient_sound_campfire, "🔥", R.raw.ambient_campfire, useOffsetTextureLayer = true),
    NIGHT_CRICKETS(R.string.ambient_sound_crickets, "🦗", R.raw.ambient_night_crickets, useOffsetTextureLayer = true),
    AMBIENT_PAD(R.string.ambient_sound_pad_432, "🎵", R.raw.ambient_pad_432, useOffsetTextureLayer = false),
    SINGING_BOWL(R.string.ambient_sound_singing_bowl, "🔔", R.raw.ambient_singing_bowl, useOffsetTextureLayer = false);

    fun next(): AmbientSoundMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

/**
 * Plays a single selected ambient `.mp3` soundscape cleanly without mixing in unrelated sounds,
 * using dual phase-offset `MediaPlayer` instances of that same track across a 2.5-hour (150-minute)
 * slowly evolving organic stereo curve, repeating seamlessly indefinitely.
 */
class AmbientSoundController {
    private var mixJob: Job? = null
    private var primaryPlayer: MediaPlayer? = null
    private var offsetSelfPlayer: MediaPlayer? = null

    fun start(
        context: Context,
        scope: CoroutineScope,
        mode: AmbientSoundMode
    ) {
        stop()
        val trackResId = mode.rawResId ?: return
        val appContext = context.applicationContext

        mixJob = scope.launch(Dispatchers.Default) {
            var p1: MediaPlayer? = null
            var p2: MediaPlayer? = null
            try {
                p1 = createLoopingPlayer(appContext, trackResId, initialVolume = 0.85f, seekHalfway = false)
                p2 = if (mode.useOffsetTextureLayer) {
                    createLoopingPlayer(appContext, trackResId, initialVolume = 0.24f, seekHalfway = true)
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
                    p1?.setVolume(leftGain, rightGain)
                    p2?.setVolume(offsetGain, offsetGain * 0.90f)

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
        private const val MIX_STEP_MS = 500L

        /**
         * Modulates primary and half-track time-offset layers of the same selected sound
         * over a 2.5-hour slow envelope so the 60s base `.mp3` never sounds mechanically repetitive.
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
    }
}
