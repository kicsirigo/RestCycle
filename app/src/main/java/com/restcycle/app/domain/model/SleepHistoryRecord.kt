package com.restcycle.app.domain.model

import com.restcycle.app.R

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
    const val MAX_HISTORY_RECORDS = 7
    const val DEFAULT_FALLBACK_SLEEP_MINUTES = 450

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
                val duration = parts[2].toIntOrNull()?.coerceIn(1, 24 * 60) ?: return@mapNotNull null
                val cycles = parts[3].toIntOrNull()?.coerceIn(1, 10) ?: return@mapNotNull null
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
