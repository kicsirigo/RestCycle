package com.restcycle.app.unit

import com.restcycle.app.domain.model.SleepFactorTag
import com.restcycle.app.domain.model.SleepHistoryCodec
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.platform.audio.SoundMixCodec
import com.restcycle.app.platform.audio.SoundMixPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class V19FeaturesTest {

    @Test
    fun soundMixCodec_roundTripAndCapsAtThreeLayers() {
        val presets = listOf(
            SoundMixPreset(
                id = "mix_1",
                name = "Cozy Storm",
                layers = linkedMapOf(
                    AmbientSoundMode.STREAM_RAIN to 0.80f,
                    AmbientSoundMode.CAMPFIRE to 0.45f,
                    AmbientSoundMode.NIGHT_CRICKETS to 0.30f,
                    AmbientSoundMode.AMBIENT_PAD to 0.25f
                )
            )
        )
        val encoded = SoundMixCodec.encode(presets)
        val decoded = SoundMixCodec.decode(encoded)

        assertEquals(1, decoded.size)
        assertEquals("Cozy Storm", decoded.first().name)
        assertEquals(3, decoded.first().layers.size)
        assertEquals(0.80f, decoded.first().layers[AmbientSoundMode.STREAM_RAIN] ?: 0f, 0.01f)
        assertEquals(0.45f, decoded.first().layers[AmbientSoundMode.CAMPFIRE] ?: 0f, 0.01f)
        assertEquals(0.30f, decoded.first().layers[AmbientSoundMode.NIGHT_CRICKETS] ?: 0f, 0.01f)
    }

    @Test
    fun sleepHistoryCodec_tagsRoundTripAndBackwardCompat() {
        // Legacy 5-field record without tags must still decode cleanly
        val legacy = SleepHistoryCodec.decode("1700000000000,1700027000000,450,5,RESTED")
        assertEquals(1, legacy.size)
        assertTrue(legacy.first().tags.isEmpty())

        // Tagged record round-trip in compact codec and JSON backup
        val tagged = listOf(
            SleepSessionRecord(
                startEpochMillis = 1700000000000L,
                endEpochMillis = 1700027000000L,
                durationMinutes = 450,
                cycles = 5,
                mood = SleepQualityMood.ENERGIZED,
                tags = setOf(SleepFactorTag.EXERCISE, SleepFactorTag.LATE_CAFFEINE)
            )
        )
        val decoded = SleepHistoryCodec.decode(SleepHistoryCodec.encode(tagged))
        assertEquals(setOf(SleepFactorTag.EXERCISE, SleepFactorTag.LATE_CAFFEINE), decoded.first().tags)

        val json = SleepHistoryCodec.exportToJson(tagged, exportedAtEpochMillis = 1700030000000L)
        val imported = SleepHistoryCodec.importFromJson(json)
        assertNotNull(imported)
        assertEquals(setOf(SleepFactorTag.EXERCISE, SleepFactorTag.LATE_CAFFEINE), imported!!.first().tags)
    }

    @Test
    fun weeklySummary_computesSevenDaysDebtAndCircularConsistency() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 24)
        // Night 1: bed 23:50 -> wake 06:50 (420 min, 30 min debt vs 450 target)
        val start1 = ZonedDateTime.of(today.minusDays(2), LocalTime.of(23, 50), zone).toInstant().toEpochMilli()
        val end1 = ZonedDateTime.of(today.minusDays(1), LocalTime.of(6, 50), zone).toInstant().toEpochMilli()
        // Night 2: bed 00:05 -> wake 06:35 (390 min, 60 min debt vs 450 target)
        val start2 = ZonedDateTime.of(today, LocalTime.of(0, 5), zone).toInstant().toEpochMilli()
        val end2 = ZonedDateTime.of(today, LocalTime.of(6, 35), zone).toInstant().toEpochMilli()

        val records = listOf(
            SleepSessionRecord(start1, end1, 420, 4, SleepQualityMood.RESTED),
            SleepSessionRecord(start2, end2, 390, 4, SleepQualityMood.FAIR)
        )

        val summary = SleepHistoryCodec.computeWeeklySummary(records, today = today, zoneId = zone)
        assertEquals(7, summary.days.size)
        assertEquals(2, summary.loggedNightsCount)
        assertEquals(90, summary.sleepDebtMinutes)
        // 23:50 and 00:05 are only 15 minutes apart across midnight -> high circular consistency!
        assertNotNull(summary.consistencyPercent)
        assertTrue("Expected high circular bedtime consistency >= 95%", summary.consistencyPercent!! >= 95)
    }

    @Test
    fun factorInsight_identifiesStrongestPositiveAndNegativeCorrelation() {
        val positiveRecords = listOf(
            SleepSessionRecord(1000L, 28000000L, 450, 5, SleepQualityMood.ENERGIZED, setOf(SleepFactorTag.EXERCISE)),
            SleepSessionRecord(30000000L, 58000000L, 450, 5, SleepQualityMood.RESTED, setOf(SleepFactorTag.EXERCISE))
        )
        val posInsight = SleepHistoryCodec.computeFactorInsight(positiveRecords)
        assertNotNull(posInsight)
        assertEquals(SleepFactorTag.EXERCISE, posInsight!!.tag)
        assertEquals(100, posInsight.refreshedPercent)
        assertTrue(posInsight.isPositive)

        val negativeRecords = listOf(
            SleepSessionRecord(1000L, 28000000L, 360, 4, SleepQualityMood.EXHAUSTED, setOf(SleepFactorTag.LATE_CAFFEINE)),
            SleepSessionRecord(30000000L, 58000000L, 360, 4, SleepQualityMood.FAIR, setOf(SleepFactorTag.LATE_CAFFEINE))
        )
        val negInsight = SleepHistoryCodec.computeFactorInsight(negativeRecords)
        assertNotNull(negInsight)
        assertEquals(SleepFactorTag.LATE_CAFFEINE, negInsight!!.tag)
        assertEquals(0, negInsight.refreshedPercent)
        assertFalse(negInsight.isPositive)
    }
}
