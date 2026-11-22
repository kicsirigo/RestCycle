package com.restcycle.app.domain.model

import com.restcycle.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class SleepQualityMood(val score: Int, val emoji: String, val labelResId: Int) {
    EXHAUSTED(1, "😫", R.string.sleep_mood_exhausted),
    FAIR(2, "😐", R.string.sleep_mood_fair),
    RESTED(3, "🙂", R.string.sleep_mood_rested),
    ENERGIZED(4, "⚡", R.string.sleep_mood_energized)
}

data class SleepSessionRecord(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val durationMinutes: Int,
    val cycles: Int,
    val mood: SleepQualityMood
)

object SleepHistoryCodec {
    const val MAX_HISTORY_RECORDS = 2000
    const val CYCLE_LENGTH_MINUTES = 90.0
    const val MIN_MINUTES_FOR_FULL_CYCLE = 45

    fun computeDurationAndCycles(
        startEpochMillis: Long,
        endEpochMillis: Long,
        sleepLatencyMinutes: Int
    ): Pair<Int, Int> {
        val rawElapsedMinutes = ((endEpochMillis - startEpochMillis) / 60_000L)
            .toInt()
            .coerceIn(0, 20 * 60)
        val effectiveAsleepMinutes = (rawElapsedMinutes - sleepLatencyMinutes).coerceAtLeast(0)
        val cycles = if (rawElapsedMinutes < MIN_MINUTES_FOR_FULL_CYCLE) {
            0
        } else {
            (effectiveAsleepMinutes / CYCLE_LENGTH_MINUTES).roundToInt().coerceIn(0, 12)
        }
        return rawElapsedMinutes to cycles
    }

    fun computeCurrentStreakDays(
        records: List<SleepSessionRecord>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        if (records.isEmpty()) return 0
        val loggedDates = records.mapTo(HashSet()) { record ->
            Instant.ofEpochMilli(record.endEpochMillis).atZone(zoneId).toLocalDate()
        }
        var cursor = when {
            today in loggedDates -> today
            today.minusDays(1) in loggedDates -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (cursor in loggedDates) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun encode(records: List<SleepSessionRecord>): String {
        return records.take(MAX_HISTORY_RECORDS).joinToString(";") { record ->
            "${record.startEpochMillis},${record.endEpochMillis},${record.durationMinutes},${record.cycles},${record.mood.name}"
        }
    }

    fun decode(raw: String?): List<SleepSessionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size != 5) return@mapNotNull null
                val start = parts[0].toLongOrNull() ?: return@mapNotNull null
                val end = parts[1].toLongOrNull() ?: return@mapNotNull null
                val duration = parts[2].toIntOrNull()?.coerceIn(0, 24 * 60) ?: return@mapNotNull null
                val cycles = parts[3].toIntOrNull()?.coerceIn(0, 12) ?: return@mapNotNull null
                val mood = SleepQualityMood.entries.firstOrNull { it.name == parts[4] } ?: return@mapNotNull null
                SleepSessionRecord(
                    startEpochMillis = start,
                    endEpochMillis = end,
                    durationMinutes = duration,
                    cycles = cycles,
                    mood = mood
                )
            }
            .take(MAX_HISTORY_RECORDS)
    }
}
