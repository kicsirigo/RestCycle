package com.restcycle.app.domain.model

import com.restcycle.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class SleepCycleInsight(
    val bestCycles: Int,
    val refreshedPercent: Int,
    val sampleCount: Int
)

enum class SleepFactorTag(val labelResId: Int) {
    LATE_CAFFEINE(R.string.sleep_factor_caffeine),
    EXERCISE(R.string.sleep_factor_exercise),
    STRESS(R.string.sleep_factor_stress),
    LATE_MEAL(R.string.sleep_factor_late_meal),
    SCREEN_TIME(R.string.sleep_factor_screen_time),
    ALCOHOL(R.string.sleep_factor_alcohol)
}

data class DailySleepPoint(
    val date: LocalDate,
    val totalMinutes: Int,
    val bestMood: SleepQualityMood?
)

data class WeeklySleepSummary(
    val days: List<DailySleepPoint>,
    val sleepDebtMinutes: Int,
    val consistencyPercent: Int?,
    val loggedNightsCount: Int
)

data class SleepFactorInsight(
    val tag: SleepFactorTag,
    val refreshedPercent: Int,
    val sampleCount: Int,
    val isPositive: Boolean
)

enum class SleepQualityMood(val score: Int, val labelResId: Int) {
    EXHAUSTED(1, R.string.sleep_mood_exhausted),
    FAIR(2, R.string.sleep_mood_fair),
    RESTED(3, R.string.sleep_mood_rested),
    ENERGIZED(4, R.string.sleep_mood_energized)
}

data class SleepSessionRecord(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val durationMinutes: Int,
    val cycles: Int,
    val mood: SleepQualityMood,
    val tags: Set<SleepFactorTag> = emptySet()
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
            val base = "${record.startEpochMillis},${record.endEpochMillis},${record.durationMinutes},${record.cycles},${record.mood.name}"
            if (record.tags.isEmpty()) {
                base
            } else {
                "$base,${record.tags.joinToString("|") { it.name }}"
            }
        }
    }

    fun decode(raw: String?): List<SleepSessionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size < 5) return@mapNotNull null
                val start = parts[0].toLongOrNull() ?: return@mapNotNull null
                val end = parts[1].toLongOrNull() ?: return@mapNotNull null
                val duration = parts[2].toIntOrNull()?.coerceIn(0, 24 * 60) ?: return@mapNotNull null
                val cycles = parts[3].toIntOrNull()?.coerceIn(0, 12) ?: return@mapNotNull null
                val mood = SleepQualityMood.entries.firstOrNull { it.name == parts[4] } ?: return@mapNotNull null
                val tags = if (parts.size >= 6 && parts[5].isNotBlank()) {
                    parts[5].split("|").mapNotNullTo(LinkedHashSet()) { tagName ->
                        SleepFactorTag.entries.firstOrNull { it.name == tagName }
                    }
                } else {
                    emptySet()
                }
                SleepSessionRecord(
                    startEpochMillis = start,
                    endEpochMillis = end,
                    durationMinutes = duration,
                    cycles = cycles,
                    mood = mood,
                    tags = tags
                )
            }
            .take(MAX_HISTORY_RECORDS)
    }

    /**
     * Computes a rolling 7-day sleep chart, weekly sleep debt (against [targetMinutes] per logged night),
     * and circular bedtime consistency percentage across the last 14 days.
     */
    fun computeWeeklySummary(
        records: List<SleepSessionRecord>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        targetMinutes: Int = 450
    ): WeeklySleepSummary {
        val byDate = records.groupBy { record ->
            Instant.ofEpochMilli(record.endEpochMillis).atZone(zoneId).toLocalDate()
        }
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayRecords = byDate[date].orEmpty()
            DailySleepPoint(
                date = date,
                totalMinutes = dayRecords.sumOf { it.durationMinutes }.coerceAtMost(24 * 60),
                bestMood = dayRecords.maxByOrNull { it.mood.score }?.mood
            )
        }
        val loggedDays = days.filter { it.totalMinutes > 0 }
        val debtMinutes = loggedDays.sumOf { (targetMinutes - it.totalMinutes) }.coerceAtLeast(0)

        val windowStart = today.minusDays(13)
        val recentBedMinutes = records
            .filter { record ->
                val d = Instant.ofEpochMilli(record.endEpochMillis).atZone(zoneId).toLocalDate()
                !d.isBefore(windowStart) && !d.isAfter(today)
            }
            .map { record ->
                val startTime = Instant.ofEpochMilli(record.startEpochMillis).atZone(zoneId).toLocalTime()
                startTime.hour * 60 + startTime.minute
            }

        val consistency = if (recentBedMinutes.size >= 2) {
            val angles = recentBedMinutes.map { (it / 1440.0) * 2.0 * PI }
            val meanSin = angles.sumOf { sin(it) } / angles.size
            val meanCos = angles.sumOf { cos(it) } / angles.size
            val meanAngle = atan2(meanSin, meanCos)
            val sqDiffs = angles.map { a ->
                var diff = a - meanAngle
                while (diff > PI) diff -= 2.0 * PI
                while (diff < -PI) diff += 2.0 * PI
                val mins = (diff / (2.0 * PI)) * 1440.0
                mins * mins
            }
            val stdDevMinutes = sqrt(sqDiffs.sum() / sqDiffs.size)
            // <= 15 min std-dev = 100%, 150+ min std-dev = 20%
            val normalized = (100.0 - ((stdDevMinutes - 15.0).coerceAtLeast(0.0) * (80.0 / 135.0)))
            normalized.roundToInt().coerceIn(20, 100)
        } else {
            null
        }

        return WeeklySleepSummary(
            days = days,
            sleepDebtMinutes = debtMinutes,
            consistencyPercent = consistency,
            loggedNightsCount = loggedDays.size
        )
    }

    /**
     * Finds the sleep factor tag with the clearest positive or negative impact on refreshed wake-ups.
     * Requires at least 2 logged nights with the same tag.
     */
    fun computeFactorInsight(records: List<SleepSessionRecord>): SleepFactorInsight? {
        val candidates = SleepFactorTag.entries.mapNotNull { tag ->
            val tagged = records.filter { tag in it.tags }
            if (tagged.size < 2) return@mapNotNull null
            val refreshed = tagged.count {
                it.mood == SleepQualityMood.RESTED || it.mood == SleepQualityMood.ENERGIZED
            }
            val pct = ((refreshed * 100.0) / tagged.size).roundToInt().coerceIn(0, 100)
            SleepFactorInsight(
                tag = tag,
                refreshedPercent = pct,
                sampleCount = tagged.size,
                isPositive = pct >= 60
            )
        }
        return candidates.maxWithOrNull(
            compareBy<SleepFactorInsight> { abs(it.refreshedPercent - 50) }
                .thenBy { it.sampleCount }
        )
    }

    /**
     * Finds the cycle count at which the user wakes up rested/energized most consistently.
     * Requires at least 2 logged nights with >= 1 cycle to avoid noisy single-session bias.
     */
    fun computeBestCycleInsight(records: List<SleepSessionRecord>): SleepCycleInsight? {
        val eligible = records.filter { it.cycles >= 1 }
        if (eligible.size < 2) return null

        val grouped = eligible.groupBy { it.cycles }
        val bestEntry = grouped.entries.maxWithOrNull(
            compareBy<Map.Entry<Int, List<SleepSessionRecord>>> { (_, group) ->
                group.count { it.mood == SleepQualityMood.RESTED || it.mood == SleepQualityMood.ENERGIZED }.toDouble() / group.size
            }.thenBy { (_, group) ->
                group.sumOf { it.mood.score }.toDouble() / group.size
            }.thenBy { (_, group) ->
                group.size
            }.thenBy { (cycles, _) ->
                -abs(cycles - 5)
            }
        ) ?: return null

        val group = bestEntry.value
        val refreshedCount = group.count {
            it.mood == SleepQualityMood.RESTED || it.mood == SleepQualityMood.ENERGIZED
        }
        val refreshedPercent = ((refreshedCount * 100.0) / group.size).roundToInt().coerceIn(0, 100)
        return SleepCycleInsight(
            bestCycles = bestEntry.key,
            refreshedPercent = refreshedPercent,
            sampleCount = group.size
        )
    }

    fun exportToJson(
        records: List<SleepSessionRecord>,
        exportedAtEpochMillis: Long = System.currentTimeMillis()
    ): String {
        val limited = records.take(MAX_HISTORY_RECORDS)
        return buildString {
            appendLine("{")
            appendLine("  \"format\": \"$BACKUP_FORMAT\",")
            appendLine("  \"version\": 1,")
            appendLine("  \"exportedAtEpochMillis\": $exportedAtEpochMillis,")
            append("  \"records\": [")
            if (limited.isEmpty()) {
                appendLine("]")
            } else {
                appendLine()
                limited.forEachIndexed { index, record ->
                    appendLine("    {")
                    appendLine("      \"startEpochMillis\": ${record.startEpochMillis},")
                    appendLine("      \"endEpochMillis\": ${record.endEpochMillis},")
                    appendLine("      \"durationMinutes\": ${record.durationMinutes},")
                    appendLine("      \"cycles\": ${record.cycles},")
                    if (record.tags.isEmpty()) {
                        appendLine("      \"mood\": \"${record.mood.name}\"")
                    } else {
                        appendLine("      \"mood\": \"${record.mood.name}\",")
                        val tagsJson = record.tags.joinToString(", ") { "\"${it.name}\"" }
                        appendLine("      \"tags\": [$tagsJson]")
                    }
                    append("    }")
                    if (index < limited.lastIndex) append(",")
                    appendLine()
                }
                appendLine("  ]")
            }
            append("}")
        }
    }

    fun importFromJson(rawJson: String): List<SleepSessionRecord>? {
        if (rawJson.isBlank()) return null
        val root = MiniJsonParser(rawJson).parseRootObject() ?: return null
        val format = root["format"] as? String
        if (format != null && format != BACKUP_FORMAT) return null

        val recordsRaw = root["records"] as? List<*> ?: return null
        val parsed = ArrayList<SleepSessionRecord>(recordsRaw.size)
        for (item in recordsRaw) {
            val obj = item as? Map<*, *> ?: return null
            val start = (obj["startEpochMillis"] as? Number)?.toLong() ?: return null
            val end = (obj["endEpochMillis"] as? Number)?.toLong() ?: return null
            if (start <= 0L || end < start) return null
            val duration = (obj["durationMinutes"] as? Number)?.toInt()?.coerceIn(0, 24 * 60) ?: return null
            val cycles = (obj["cycles"] as? Number)?.toInt()?.coerceIn(0, 12) ?: return null
            val moodName = obj["mood"] as? String ?: return null
            val mood = SleepQualityMood.entries.firstOrNull { it.name == moodName } ?: return null
            val tagsRaw = obj["tags"] as? List<*>
            val tags = tagsRaw
                ?.mapNotNullTo(LinkedHashSet()) { rawTag ->
                    (rawTag as? String)?.let { name -> SleepFactorTag.entries.firstOrNull { it.name == name } }
                }
                ?: emptySet()
            parsed += SleepSessionRecord(
                startEpochMillis = start,
                endEpochMillis = end,
                durationMinutes = duration,
                cycles = cycles,
                mood = mood,
                tags = tags
            )
        }
        return parsed
            .distinctBy { it.startEpochMillis to it.endEpochMillis }
            .sortedByDescending { it.endEpochMillis }
            .take(MAX_HISTORY_RECORDS)
    }

    fun mergeHistory(
        existing: List<SleepSessionRecord>,
        imported: List<SleepSessionRecord>
    ): Pair<List<SleepSessionRecord>, Int> {
        val seenKeys = HashSet<Pair<Long, Long>>(existing.size + imported.size)
        val merged = ArrayList<SleepSessionRecord>(existing.size + imported.size)
        for (record in existing) {
            val key = record.startEpochMillis to record.endEpochMillis
            if (seenKeys.add(key)) {
                merged += record
            }
        }
        var addedCount = 0
        for (record in imported) {
            val key = record.startEpochMillis to record.endEpochMillis
            if (seenKeys.add(key)) {
                merged += record
                addedCount++
            }
        }
        val sorted = merged
            .sortedByDescending { it.endEpochMillis }
            .take(MAX_HISTORY_RECORDS)
        return sorted to addedCount
    }

    private const val BACKUP_FORMAT = "restcycle_sleep_backup"

    private class MiniJsonParser(private val src: String) {
        private var pos = 0

        fun parseRootObject(): Map<String, Any?>? {
            skipWhitespace()
            if (pos >= src.length || src[pos] != '{') return null
            val obj = parseObject() ?: return null
            skipWhitespace()
            return if (pos == src.length) obj else null
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) return INVALID
            return when (src[pos]) {
                '{' -> parseObject() ?: INVALID
                '[' -> parseArray() ?: INVALID
                '"' -> parseString() ?: INVALID
                't' -> if (src.startsWith("true", pos)) { pos += 4; true } else INVALID
                'f' -> if (src.startsWith("false", pos)) { pos += 5; false } else INVALID
                'n' -> if (src.startsWith("null", pos)) { pos += 4; null } else INVALID
                '-', in '0'..'9' -> parseNumber() ?: INVALID
                else -> INVALID
            }
        }

        private fun parseObject(): Map<String, Any?>? {
            if (src[pos] != '{') return null
            pos++
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWhitespace()
                if (pos >= src.length || src[pos] != '"') return null
                val key = parseString() ?: return null
                skipWhitespace()
                if (pos >= src.length || src[pos] != ':') return null
                pos++
                val value = parseValue()
                if (value === INVALID) return null
                map[key] = value
                skipWhitespace()
                if (pos >= src.length) return null
                when (src[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> return null
                }
            }
            return null
        }

        private fun parseArray(): List<Any?>? {
            if (src[pos] != '[') return null
            pos++
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                val value = parseValue()
                if (value === INVALID) return null
                list += value
                skipWhitespace()
                if (pos >= src.length) return null
                when (src[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> return null
                }
            }
            return null
        }

        private fun parseString(): String? {
            if (src[pos] != '"') return null
            pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                when (val ch = src[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) return null
                        when (val esc = src[pos++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) return null
                                val code = src.substring(pos, pos + 4).toIntOrNull(16) ?: return null
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> return null
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            return null
        }

        private fun parseNumber(): Number? {
            val start = pos
            if (src[pos] == '-') pos++
            if (pos >= src.length || src[pos] !in '0'..'9') return null
            while (pos < src.length && src[pos] in '0'..'9') pos++
            var isFloat = false
            if (pos < src.length && src[pos] == '.') {
                isFloat = true
                pos++
                if (pos >= src.length || src[pos] !in '0'..'9') return null
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                isFloat = true
                pos++
                if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
                if (pos >= src.length || src[pos] !in '0'..'9') return null
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            val token = src.substring(start, pos)
            return if (isFloat) token.toDoubleOrNull() else token.toLongOrNull()
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private companion object {
            val INVALID = Any()
        }
    }
}
