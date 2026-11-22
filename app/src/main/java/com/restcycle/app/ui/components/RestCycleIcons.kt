package com.restcycle.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.platform.audio.AmbientSoundMode

object RestCycleIcons {
    private fun strokeIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

    private fun ImageVector.Builder.strokePath(block: PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }

    val Alarm: ImageVector by lazy {
        strokeIcon("RestCycleAlarm") {
            strokePath {
                moveTo(12f, 5f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 5f)
                close()
                moveTo(12f, 8.5f)
                lineTo(12f, 13f)
                lineTo(15f, 14.5f)
                moveTo(4.5f, 4.5f)
                lineTo(7f, 2.5f)
                moveTo(19.5f, 4.5f)
                lineTo(17f, 2.5f)
            }
        }
    }

    val Timer: ImageVector by lazy {
        strokeIcon("RestCycleTimer") {
            strokePath {
                moveTo(9.5f, 3f)
                lineTo(14.5f, 3f)
                moveTo(12f, 3f)
                lineTo(12f, 5.5f)
                moveTo(12f, 5.5f)
                arcTo(7.5f, 7.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 5.5f)
                close()
                moveTo(12f, 13f)
                lineTo(15.5f, 9.5f)
            }
        }
    }

    val Info: ImageVector by lazy {
        strokeIcon("RestCycleInfo") {
            strokePath {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 16.5f)
                moveTo(12f, 8f)
                lineTo(12f, 8.2f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("RestCycleSettings") {
            strokePath {
                moveTo(4f, 7f)
                lineTo(8f, 7f)
                moveTo(13f, 7f)
                lineTo(20f, 7f)
                moveTo(10.5f, 4.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.49f, 4.5f)
                close()

                moveTo(4f, 17f)
                lineTo(11f, 17f)
                moveTo(16f, 17f)
                lineTo(20f, 17f)
                moveTo(13.5f, 14.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13.49f, 14.5f)
                close()
            }
        }
    }

    val Contrast: ImageVector by lazy {
        strokeIcon("RestCycleContrast") {
            strokePath {
                moveTo(12f, 3.5f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3.5f)
                close()
            }
            path(fill = SolidColor(Color.Black), stroke = null) {
                moveTo(12f, 4f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 20f)
                close()
            }
        }
    }

    val Bell: ImageVector by lazy {
        strokeIcon("RestCycleBell") {
            strokePath {
                moveTo(6f, 9f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 9f)
                lineTo(18f, 13.5f)
                curveTo(18f, 15.2f, 19.3f, 16.4f, 20f, 17f)
                lineTo(4f, 17f)
                curveTo(4.7f, 16.4f, 6f, 15.2f, 6f, 13.5f)
                close()
                moveTo(10f, 17.5f)
                arcTo(2.2f, 2.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14f, 17.5f)
            }
        }
    }

    val ExportUpload: ImageVector by lazy {
        strokeIcon("RestCycleExportUpload") {
            strokePath {
                moveTo(12f, 4f)
                lineTo(12f, 15f)
                moveTo(7.5f, 8.5f)
                lineTo(12f, 4f)
                lineTo(16.5f, 8.5f)
                moveTo(4f, 15f)
                lineTo(4f, 18.5f)
                curveTo(4f, 19.5f, 4.5f, 20f, 5.5f, 20f)
                lineTo(18.5f, 20f)
                curveTo(19.5f, 20f, 20f, 19.5f, 20f, 18.5f)
                lineTo(20f, 15f)
            }
        }
    }

    val ImportDownload: ImageVector by lazy {
        strokeIcon("RestCycleImportDownload") {
            strokePath {
                moveTo(12f, 15f)
                lineTo(12f, 4f)
                moveTo(7.5f, 10.5f)
                lineTo(12f, 15f)
                lineTo(16.5f, 10.5f)
                moveTo(4f, 15f)
                lineTo(4f, 18.5f)
                curveTo(4f, 19.5f, 4.5f, 20f, 5.5f, 20f)
                lineTo(18.5f, 20f)
                curveTo(19.5f, 20f, 20f, 19.5f, 20f, 18.5f)
                lineTo(20f, 15f)
            }
        }
    }

    val Globe: ImageVector by lazy {
        strokeIcon("RestCycleGlobe") {
            strokePath {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
                moveTo(3f, 12f)
                lineTo(21f, 12f)
                moveTo(12f, 3f)
                curveTo(15f, 6.5f, 15f, 17.5f, 12f, 21f)
                moveTo(12f, 3f)
                curveTo(9f, 6.5f, 9f, 17.5f, 12f, 21f)
            }
        }
    }

    val Coffee: ImageVector by lazy {
        strokeIcon("RestCycleCoffee") {
            strokePath {
                moveTo(4.5f, 9f)
                lineTo(15.5f, 9f)
                lineTo(15.5f, 14.5f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4.5f, 14.5f)
                close()
                moveTo(15.5f, 10f)
                lineTo(17.5f, 10f)
                arcTo(2.2f, 2.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.5f, 14.4f)
                lineTo(15.5f, 14.4f)
                moveTo(8f, 4f)
                lineTo(8f, 6f)
                moveTo(12f, 3.5f)
                lineTo(12f, 5.8f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        strokeIcon("RestCycleCalendar") {
            strokePath {
                moveTo(5.5f, 5.5f)
                lineTo(18.5f, 5.5f)
                curveTo(19.5f, 5.5f, 20f, 6f, 20f, 7f)
                lineTo(20f, 18.5f)
                curveTo(20f, 19.5f, 19.5f, 20f, 18.5f, 20f)
                lineTo(5.5f, 20f)
                curveTo(4.5f, 20f, 4f, 19.5f, 4f, 18.5f)
                lineTo(4f, 7f)
                curveTo(4f, 6f, 4.5f, 5.5f, 5.5f, 5.5f)
                close()
                moveTo(4f, 10f)
                lineTo(20f, 10f)
                moveTo(8.5f, 3.5f)
                lineTo(8.5f, 6.5f)
                moveTo(15.5f, 3.5f)
                lineTo(15.5f, 6.5f)
            }
        }
    }

    val Moon: ImageVector by lazy {
        strokeIcon("RestCycleMoon") {
            strokePath {
                moveTo(20f, 12.8f)
                arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.2f, 4f)
                arcTo(6.4f, 6.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20f, 12.8f)
                close()
            }
        }
    }

    val Sun: ImageVector by lazy {
        strokeIcon("RestCycleSun") {
            strokePath {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 8f)
                close()
                moveTo(12f, 3f)
                lineTo(12f, 4.8f)
                moveTo(12f, 19.2f)
                lineTo(12f, 21f)
                moveTo(3f, 12f)
                lineTo(4.8f, 12f)
                moveTo(19.2f, 12f)
                lineTo(21f, 12f)
                moveTo(5.6f, 5.6f)
                lineTo(6.9f, 6.9f)
                moveTo(17.1f, 17.1f)
                lineTo(18.4f, 18.4f)
                moveTo(18.4f, 5.6f)
                lineTo(17.1f, 6.9f)
                moveTo(6.9f, 17.1f)
                lineTo(5.6f, 18.4f)
            }
        }
    }

    val Wind: ImageVector by lazy {
        strokeIcon("RestCycleWind") {
            strokePath {
                moveTo(3f, 8.5f)
                lineTo(14.5f, 8.5f)
                arcTo(2.25f, 2.25f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12.5f, 5.2f)
                moveTo(3f, 12.5f)
                lineTo(18.5f, 12.5f)
                arcTo(2.25f, 2.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16.5f, 15.8f)
                moveTo(3f, 16.5f)
                lineTo(11.5f, 16.5f)
            }
        }
    }

    val AudioWaveform: ImageVector by lazy {
        strokeIcon("RestCycleAudioWaveform") {
            strokePath {
                moveTo(4f, 10.5f)
                lineTo(4f, 13.5f)
                moveTo(8f, 6.5f)
                lineTo(8f, 17.5f)
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                moveTo(16f, 7.5f)
                lineTo(16f, 16.5f)
                moveTo(20f, 10f)
                lineTo(20f, 14f)
            }
        }
    }

    val VolumeOff: ImageVector by lazy {
        strokeIcon("RestCycleVolumeOff") {
            strokePath {
                moveTo(10f, 5.5f)
                lineTo(5.5f, 9f)
                lineTo(3f, 9f)
                lineTo(3f, 15f)
                lineTo(5.5f, 15f)
                lineTo(10f, 18.5f)
                close()
                moveTo(15f, 9.5f)
                lineTo(20f, 14.5f)
                moveTo(20f, 9.5f)
                lineTo(15f, 14.5f)
            }
        }
    }

    val Waves: ImageVector by lazy {
        strokeIcon("RestCycleWaves") {
            strokePath {
                moveTo(3f, 7.5f)
                curveTo(6f, 5.5f, 9f, 5.5f, 12f, 7.5f)
                curveTo(15f, 9.5f, 18f, 9.5f, 21f, 7.5f)
                moveTo(3f, 12.5f)
                curveTo(6f, 10.5f, 9f, 10.5f, 12f, 12.5f)
                curveTo(15f, 14.5f, 18f, 14.5f, 21f, 12.5f)
                moveTo(3f, 17.5f)
                curveTo(6f, 15.5f, 9f, 15.5f, 12f, 17.5f)
                curveTo(15f, 19.5f, 18f, 19.5f, 21f, 17.5f)
            }
        }
    }

    val CloudRain: ImageVector by lazy {
        strokeIcon("RestCycleCloudRain") {
            strokePath {
                moveTo(6.5f, 14.5f)
                lineTo(17f, 14.5f)
                arcTo(3.4f, 3.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16.5f, 7.8f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7.2f, 8.4f)
                arcTo(3.2f, 3.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.5f, 14.5f)
                close()
                moveTo(8.5f, 17.5f)
                lineTo(7.5f, 20f)
                moveTo(12.5f, 17.5f)
                lineTo(11.5f, 20f)
                moveTo(16.5f, 17.5f)
                lineTo(15.5f, 20f)
            }
        }
    }

    val Flame: ImageVector by lazy {
        strokeIcon("RestCycleFlame") {
            strokePath {
                moveTo(12f, 3f)
                curveTo(15f, 6.5f, 18f, 9.5f, 18f, 14f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6f, 14f)
                curveTo(6f, 10.5f, 8.5f, 7.5f, 9.5f, 6f)
                curveTo(9.8f, 7.8f, 10.8f, 9f, 11.5f, 8.2f)
                curveTo(11.8f, 6.5f, 12f, 4.5f, 12f, 3f)
                close()
            }
        }
    }

    val NightStars: ImageVector by lazy {
        strokeIcon("RestCycleNightStars") {
            strokePath {
                moveTo(15.5f, 16.5f)
                arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8.5f, 6.2f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15.5f, 16.5f)
                close()
                moveTo(17.5f, 5f)
                lineTo(17.5f, 8f)
                moveTo(16f, 6.5f)
                lineTo(19f, 6.5f)
                moveTo(19.5f, 10.5f)
                lineTo(19.5f, 12.5f)
                moveTo(18.5f, 11.5f)
                lineTo(20.5f, 11.5f)
            }
        }
    }

    val FrequencyWave: ImageVector by lazy {
        strokeIcon("RestCycleFrequencyWave") {
            strokePath {
                moveTo(3f, 12f)
                curveTo(5.5f, 5f, 8.5f, 5f, 12f, 12f)
                curveTo(15.5f, 19f, 18.5f, 19f, 21f, 12f)
            }
        }
    }

    val SingingBowl: ImageVector by lazy {
        strokeIcon("RestCycleSingingBowl") {
            strokePath {
                moveTo(4.5f, 11.5f)
                lineTo(19.5f, 11.5f)
                curveTo(19.5f, 16.5f, 16f, 19f, 12f, 19f)
                curveTo(8f, 19f, 4.5f, 16.5f, 4.5f, 11.5f)
                close()
                moveTo(9f, 19.5f)
                lineTo(15f, 19.5f)
                moveTo(9f, 7.5f)
                curveTo(10.8f, 6f, 13.2f, 6f, 15f, 7.5f)
                moveTo(7f, 5f)
                curveTo(10f, 3.2f, 14f, 3.2f, 17f, 5f)
            }
        }
    }

    val Sparkles: ImageVector by lazy {
        strokeIcon("RestCycleSparkles") {
            strokePath {
                moveTo(11f, 4f)
                curveTo(11f, 9f, 14f, 12f, 19f, 12f)
                curveTo(14f, 12f, 11f, 15f, 11f, 20f)
                curveTo(11f, 15f, 8f, 12f, 3f, 12f)
                curveTo(8f, 12f, 11f, 9f, 11f, 4f)
                close()
                moveTo(18.5f, 4f)
                lineTo(18.5f, 7f)
                moveTo(17f, 5.5f)
                lineTo(20f, 5.5f)
            }
        }
    }

    val MoodExhausted: ImageVector by lazy {
        strokeIcon("RestCycleMoodExhausted") {
            strokePath {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
                moveTo(8f, 9.5f)
                lineTo(10f, 10.5f)
                moveTo(16f, 9.5f)
                lineTo(14f, 10.5f)
                moveTo(9f, 16f)
                quadTo(12f, 13.5f, 15f, 16f)
            }
        }
    }

    val MoodFair: ImageVector by lazy {
        strokeIcon("RestCycleMoodFair") {
            strokePath {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
                moveTo(9f, 10f)
                lineTo(9f, 10.3f)
                moveTo(15f, 10f)
                lineTo(15f, 10.3f)
                moveTo(9f, 15f)
                lineTo(15f, 15f)
            }
        }
    }

    val MoodRested: ImageVector by lazy {
        strokeIcon("RestCycleMoodRested") {
            strokePath {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
                moveTo(9f, 10f)
                lineTo(9f, 10.3f)
                moveTo(15f, 10f)
                lineTo(15f, 10.3f)
                moveTo(8.5f, 14f)
                quadTo(12f, 17f, 15.5f, 14f)
            }
        }
    }

    val MoodEnergized: ImageVector by lazy {
        strokeIcon("RestCycleMoodEnergized") {
            strokePath {
                moveTo(13.5f, 3f)
                lineTo(6.5f, 13f)
                lineTo(12f, 13f)
                lineTo(10.5f, 21f)
                lineTo(17.5f, 11f)
                lineTo(12f, 11f)
                close()
            }
        }
    }

    fun forAmbientSound(mode: AmbientSoundMode): ImageVector = when (mode) {
        AmbientSoundMode.OFF -> VolumeOff
        AmbientSoundMode.BROWN_NOISE -> Waves
        AmbientSoundMode.STREAM_RAIN -> CloudRain
        AmbientSoundMode.CAMPFIRE -> Flame
        AmbientSoundMode.NIGHT_CRICKETS -> NightStars
        AmbientSoundMode.AMBIENT_PAD -> FrequencyWave
        AmbientSoundMode.SINGING_BOWL -> SingingBowl
    }

    fun forMood(mood: SleepQualityMood): ImageVector = when (mood) {
        SleepQualityMood.EXHAUSTED -> MoodExhausted
        SleepQualityMood.FAIR -> MoodFair
        SleepQualityMood.RESTED -> MoodRested
        SleepQualityMood.ENERGIZED -> MoodEnergized
    }
}
