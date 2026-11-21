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
    val primaryRawResId: Int?,
    val secondaryRawResId: Int?
) {
    OFF(R.string.ambient_sound_off, "🔇", null, null),
    BROWN_NOISE(R.string.ambient_sound_brown, "🌊", R.raw.ambient_brown_noise, R.raw.ambient_stream_rain),
    STREAM_RAIN(R.string.ambient_sound_rain, "🌧️", R.raw.ambient_stream_rain, R.raw.ambient_brown_noise),
    CAMPFIRE(R.string.ambient_sound_campfire, "🔥", R.raw.ambient_campfire, R.raw.ambient_stream_rain);

    fun next(): AmbientSoundMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

/**
 * Plays seamless 60s stereo `.mp3` base tracks via Android [MediaPlayer] and dynamically
 * cross-blends primary and complementary layers across a 2.5-hour (150-minute) macro mix cycle
 * that repeats cleanly without noticeable loops or high-frequency hiss.
 */
class AmbientSoundController {
    private var mixJob: Job? = null
    private var primaryPlayer: MediaPlayer? = null
    private var secondaryPlayer: MediaPlayer? = null

    fun start(
        context: Context,
        scope: CoroutineScope,
        mode: AmbientSoundMode
    ) {
        stop()
        val primaryResId = mode.primaryRawResId ?: return
        val secondaryResId = mode.secondaryRawResId ?: primaryResId
        val appContext = context.applicationContext

        mixJob = scope.launch(Dispatchers.Default) {
            var p1: MediaPlayer? = null
            var p2: MediaPlayer? = null
            try {
                p1 = createLoopingPlayer(appContext, primaryResId, initialVolume = 0.78f)
                p2 = createLoopingPlayer(appContext, secondaryResId, initialVolume = 0.14f)
                primaryPlayer = p1
                secondaryPlayer = p2

                p1?.start()
                p2?.start()

                var elapsedSeconds = 0.0
                while (isActive) {
                    val (leftGain, rightGain, underGain) = computeMixGains(elapsedSeconds)
                    p1?.setVolume(leftGain, rightGain)
                    p2?.setVolume(underGain, underGain * 0.92f)

                    delay(MIX_STEP_MS)
                    elapsedSeconds = (elapsedSeconds + MIX_STEP_MS / 1000.0) % MIX_CYCLE_DURATION_SECONDS
                }
            } catch (_: Throwable) {
                // Gracefully ignore on environments without hardware media codecs
            } finally {
                releasePlayers(p1, p2)
                primaryPlayer = null
                secondaryPlayer = null
            }
        }
    }

    fun stop() {
        mixJob?.cancel()
        mixJob = null
        releasePlayers(primaryPlayer, secondaryPlayer)
        primaryPlayer = null
        secondaryPlayer = null
    }

    private fun createLoopingPlayer(
        context: Context,
        rawResId: Int,
        initialVolume: Float
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
        // 2.5 hours (150 minutes = 9000 seconds) per macro mix cycle before seamless repeat
        const val MIX_CYCLE_DURATION_SECONDS = 150 * 60.0
        private const val MIX_STEP_MS = 500L

        /**
         * Computes gently breathing stereo and layer-blend gains for [elapsedSeconds] inside
         * the 2.5-hour mix cycle using incommensurate slow LFO waves so transitions feel organic.
         */
        fun computeMixGains(elapsedSeconds: Double): Triple<Float, Float, Float> {
            val cyclePhase = 2.0 * PI * (elapsedSeconds % MIX_CYCLE_DURATION_SECONDS) / MIX_CYCLE_DURATION_SECONDS
            val waveSlow = sin(2.0 * PI * elapsedSeconds / 173.0)
            val wavePan = sin(2.0 * PI * elapsedSeconds / 89.0)
            val waveDeep = cos(cyclePhase * 3.0)

            val primaryBase = (0.76 + 0.12 * waveSlow + 0.06 * waveDeep).coerceIn(0.60, 0.94)
            val leftGain = (primaryBase * (1.0 + 0.06 * wavePan)).toFloat().coerceIn(0.50f, 0.96f)
            val rightGain = (primaryBase * (1.0 - 0.06 * wavePan)).toFloat().coerceIn(0.50f, 0.96f)
            val underGain = (0.20 + 0.11 * (-waveSlow) + 0.05 * cos(2.0 * PI * elapsedSeconds / 311.0))
                .toFloat()
                .coerceIn(0.08f, 0.38f)

            return Triple(leftGain, rightGain, underGain)
        }
    }
}
