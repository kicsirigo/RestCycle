package com.restcycle.app.platform.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.restcycle.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundMode(val labelResId: Int, val emoji: String) {
    OFF(R.string.ambient_sound_off, "🔇"),
    BROWN_NOISE(R.string.ambient_sound_brown, "🌊"),
    STREAM_RAIN(R.string.ambient_sound_rain, "🌧️"),
    CAMPFIRE(R.string.ambient_sound_campfire, "🔥");

    fun next(): AmbientSoundMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

class AmbientSoundController {
    private var playbackJob: Job? = null

    fun start(
        scope: CoroutineScope,
        mode: AmbientSoundMode,
        fadeOutMinutes: Int = 20,
        onAutoStopped: () -> Unit = {}
    ) {
        stop()
        if (mode == AmbientSoundMode.OFF) return

        val sampleRate = 22050
        val totalFadeSeconds = (fadeOutMinutes.coerceAtLeast(10)) * 60
        val totalSamples = sampleRate.toLong() * totalFadeSeconds

        playbackJob = scope.launch(Dispatchers.Default) {
            var track: AudioTrack? = null
            try {
                val minBufBytes = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferFrames = (if (minBufBytes > 0) minBufBytes / 2 else sampleRate / 10).coerceAtLeast(2048)
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferFrames * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()

                val pcm = ShortArray(bufferFrames)
                val rng = Random(System.nanoTime())
                var brownLast = 0f
                var pinkB0 = 0f
                var pinkB1 = 0f
                var pinkB2 = 0f
                var lpFire = 0f
                var phase = 0.0
                var writtenSamples = 0L

                while (isActive && writtenSamples < totalSamples) {
                    for (i in 0 until bufferFrames) {
                        val fadeProgress = (writtenSamples.toFloat() / totalSamples.toFloat()).coerceIn(0f, 1f)
                        val envelope = 1f - fadeProgress * fadeProgress
                        val white = rng.nextFloat() * 2f - 1f

                        val sample = when (mode) {
                            AmbientSoundMode.BROWN_NOISE -> {
                                brownLast = (brownLast + 0.032f * white) / 1.032f
                                (brownLast * 2.8f).coerceIn(-0.9f, 0.9f)
                            }
                            AmbientSoundMode.STREAM_RAIN -> {
                                pinkB0 = 0.997f * pinkB0 + white * 0.029f
                                pinkB1 = 0.985f * pinkB1 + white * 0.032f
                                pinkB2 = 0.950f * pinkB2 + white * 0.048f
                                val lfo = 0.72f + 0.28f * sin(phase).toFloat()
                                phase += 2.0 * PI * 0.16 / sampleRate
                                ((pinkB0 + pinkB1 + pinkB2) * 0.45f * lfo).coerceIn(-0.85f, 0.85f)
                            }
                            AmbientSoundMode.CAMPFIRE -> {
                                lpFire = lpFire * 0.97f + white * 0.03f
                                val crackle = if (rng.nextFloat() < 0.0022f) (rng.nextFloat() * 2f - 1f) * 0.55f else 0f
                                (lpFire * 2.2f + crackle).coerceIn(-0.85f, 0.85f)
                            }
                            AmbientSoundMode.OFF -> 0f
                        }

                        pcm[i] = (sample * envelope * Short.MAX_VALUE * 0.35f).toInt().toShort()
                        writtenSamples++
                    }
                    track.write(pcm, 0, bufferFrames)
                }
                if (isActive) {
                    onAutoStopped()
                }
            } catch (_: Throwable) {
                // Gracefully no-op in headless/Robolectric environments without audio hardware
            } finally {
                runCatching {
                    track?.stop()
                    track?.release()
                }
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
    }
}
