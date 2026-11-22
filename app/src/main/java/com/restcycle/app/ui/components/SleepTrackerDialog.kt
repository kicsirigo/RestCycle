package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restcycle.app.R
import com.restcycle.app.domain.model.SleepHistoryCodec
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.util.formatClockTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SleepTrackerDialog(
    activeSleepStartMillis: Long,
    sleepHistory: List<SleepSessionRecord>,
    is24HourFormat: Boolean,
    onStartSleep: () -> Unit,
    onCompleteSleepWithMood: (SleepQualityMood) -> Unit,
    onCancelSleep: () -> Unit,
    onDeleteRecord: (SleepSessionRecord) -> Unit = {},
    sleepLatencyMinutes: Int = 15,
    onAddManualSleep: (startEpochMillis: Long, endEpochMillis: Long, mood: SleepQualityMood) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val currentMonth = remember(today) { YearMonth.from(today) }

    var displayedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDate by remember { mutableStateOf(today) }

    val sessionsByDate = remember(sleepHistory, zoneId) {
        sleepHistory.groupBy { record ->
            Instant.ofEpochMilli(record.endEpochMillis).atZone(zoneId).toLocalDate()
        }
    }

    val isSleeping = activeSleepStartMillis > 0L

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag(RestCycleTestTags.SLEEP_TRACKER_DIALOG),
            shape = RoundedCornerShape(24.dp),
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.borderSubtle),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sleep_tracker_title),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.accentText)
                    ) {
                        Text(
                            text = stringResource(R.string.action_close),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Active Sleep Controls or Start Sleep Button
                if (isSleeping) {
                    val nowMillis = System.currentTimeMillis().coerceAtLeast(activeSleepStartMillis)
                    val elapsedMinutes = ((nowMillis - activeSleepStartMillis) / 60_000L).toInt().coerceAtLeast(0)
                    val startTime = Instant.ofEpochMilli(activeSleepStartMillis)
                        .atZone(zoneId)
                        .toLocalTime()
                    val formattedStart = formatClockTime(startTime, is24HourFormat)
                    val elapsedText = formatDurationCompact(elapsedMinutes)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_sleeping_since, formattedStart),
                                style = TabularTextStyles.cardDuration,
                                color = palette.accentText
                            )
                            Text(
                                text = stringResource(R.string.sleep_elapsed_label, elapsedText),
                                style = TabularTextStyles.cardDuration,
                                color = palette.textSecondary
                            )
                        }

                        if (elapsedMinutes < 15) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.accent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(RestCycleTestTags.CALENDAR_SHORT_SESSION_NOTICE)
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_short_session_warning, elapsedMinutes),
                                    color = palette.textPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.sleep_wakeup_question),
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        SleepQualityMood.entries.chunked(2).forEach { rowMoods ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMoods.forEach { mood ->
                                    val shape = RoundedCornerShape(14.dp)
                                    Surface(
                                        shape = shape,
                                        color = palette.pillContainer,
                                        border = BorderStroke(1.dp, palette.borderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shape)
                                            .testTag(RestCycleTestTags.sleepMoodOption(mood.name))
                                            .clickable(role = Role.Button) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onCompleteSleepWithMood(mood)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${mood.emoji} ${stringResource(mood.labelResId)}",
                                                color = palette.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onCancelSleep()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (elapsedMinutes < 15) palette.cardSurfaceHighlighted else palette.pillContainer,
                                contentColor = palette.textSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_cancel_session),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onStartSleep()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(RestCycleTestTags.START_SLEEP_DIALOG_BTN)
                    ) {
                        Text(
                            text = "🌙 ${stringResource(R.string.action_start_sleep)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.65f))

                // All-Time Stats Row
                val totalNights = sleepHistory.size
                val allTimeAvgMinutes = if (sleepHistory.isNotEmpty()) {
                    sleepHistory.map { it.durationMinutes }.average().roundToInt()
                } else {
                    0
                }
                val streakDays = remember(sleepHistory, today, zoneId) {
                    SleepHistoryCodec.computeCurrentStreakDays(sleepHistory, today, zoneId)
                }
                val avgDurationText = if (sleepHistory.isEmpty()) "—" else formatDurationCompact(allTimeAvgMinutes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarStatCard(
                        label = stringResource(R.string.calendar_stat_total_nights),
                        value = totalNights.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CalendarStatCard(
                        label = stringResource(R.string.calendar_stat_avg_sleep),
                        value = avgDurationText,
                        modifier = Modifier.weight(1f)
                    )
                    CalendarStatCard(
                        label = stringResource(R.string.calendar_stat_streak),
                        value = if (streakDays > 0) "🔥 $streakDays" else "0",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Personal Sleep Cycle Pattern Insight (#3)
                val cycleInsight = remember(sleepHistory) {
                    SleepHistoryCodec.computeBestCycleInsight(sleepHistory)
                }
                if (cycleInsight != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(RestCycleTestTags.CALENDAR_INSIGHT_CARD)
                    ) {
                        Text(
                            text = "✨ ${
                                stringResource(
                                    R.string.calendar_cycle_insight,
                                    cycleInsight.bestCycles,
                                    cycleInsight.refreshedPercent,
                                    cycleInsight.sampleCount
                                )
                            }",
                            color = palette.accentText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Month Navigation Bar
                val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
                val monthLabel = remember(displayedMonth, monthFormatter, locale) {
                    displayedMonth.atDay(1).format(monthFormatter)
                        .replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
                        }
                }
                val canGoNext = displayedMonth < currentMonth

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthNavButton(
                        symbol = "‹",
                        enabled = true,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            displayedMonth = displayedMonth.minusMonths(1)
                            selectedDate = if (displayedMonth == currentMonth) today else displayedMonth.atDay(1)
                        },
                        modifier = Modifier.testTag(RestCycleTestTags.CALENDAR_PREV_MONTH)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthLabel,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag(RestCycleTestTags.CALENDAR_MONTH_LABEL)
                        )
                        if (displayedMonth != currentMonth || selectedDate != today) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.accent.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        displayedMonth = currentMonth
                                        selectedDate = today
                                    }
                            ) {
                                Text(
                                    text = stringResource(R.string.calendar_today),
                                    color = palette.accentText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    MonthNavButton(
                        symbol = "›",
                        enabled = canGoNext,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (canGoNext) {
                                displayedMonth = displayedMonth.plusMonths(1)
                                selectedDate = if (displayedMonth == currentMonth) today else displayedMonth.atDay(1)
                            }
                        },
                        modifier = Modifier.testTag(RestCycleTestTags.CALENDAR_NEXT_MONTH)
                    )
                }

                // Weekday Headers (Mon..Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.entries.forEach { dow ->
                        val shortName = dow.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
                            .trimEnd('.')
                            .take(2)
                        Text(
                            text = shortName,
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // All-Time Month Calendar Grid
                val firstDayOfMonth = displayedMonth.atDay(1)
                val leadingEmpty = firstDayOfMonth.dayOfWeek.value - 1
                val daysInMonth = displayedMonth.lengthOfMonth()
                val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7
                val monthCells = (0 until totalCells).map { slot ->
                    val dayNum = slot - leadingEmpty + 1
                    if (dayNum in 1..daysInMonth) displayedMonth.atDay(dayNum) else null
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    monthCells.chunked(7).forEach { weekDates ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekDates.forEach { cellDate ->
                                if (cellDate == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    )
                                } else {
                                    val daySessions = sessionsByDate[cellDate].orEmpty()
                                    val latestMood = daySessions.firstOrNull()?.mood
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == today
                                    val isFuture = cellDate > today
                                    val cellShape = RoundedCornerShape(10.dp)

                                    Surface(
                                        shape = cellShape,
                                        color = when {
                                            isSelected -> palette.cardSurfaceHighlighted
                                            latestMood != null -> palette.accent.copy(alpha = 0.14f)
                                            else -> palette.pillContainer.copy(alpha = 0.55f)
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = when {
                                                isSelected -> palette.accent
                                                isToday -> palette.accent.copy(alpha = 0.55f)
                                                latestMood != null -> palette.accent.copy(alpha = 0.35f)
                                                else -> palette.borderSubtle.copy(alpha = 0.45f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .alpha(if (isFuture) 0.45f else 1f)
                                            .clip(cellShape)
                                            .testTag(RestCycleTestTags.calendarDayCell(cellDate.toString()))
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                selectedDate = cellDate
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = cellDate.dayOfMonth.toString(),
                                                style = TabularTextStyles.cardDuration.copy(fontSize = 11.sp),
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isToday) palette.accentText else palette.textPrimary
                                            )
                                            if (latestMood != null) {
                                                Text(
                                                    text = latestMood.emoji,
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp
                                                )
                                            } else if (isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.accent)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.65f))

                // Selected Day Details
                val selectedDateLabel = remember(selectedDate, locale) {
                    selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                }
                val selectedDayRecords = sessionsByDate[selectedDate].orEmpty()
                val canLogForSelectedDay = !selectedDate.isAfter(today)
                var showManualForm by remember { mutableStateOf(false) }
                var manualBedtime by remember { mutableStateOf(LocalTime.of(23, 0)) }
                var manualWaketime by remember { mutableStateOf(LocalTime.of(6, 45)) }
                var manualMood by remember { mutableStateOf(SleepQualityMood.RESTED) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDateLabel,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (canLogForSelectedDay) {
                        val pillShape = RoundedCornerShape(50)
                        Surface(
                            shape = pillShape,
                            color = if (showManualForm) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
                            border = BorderStroke(1.dp, if (showManualForm) palette.accent else palette.borderSubtle),
                            modifier = Modifier
                                .clip(pillShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showManualForm = !showManualForm
                                }
                                .testTag(RestCycleTestTags.CALENDAR_ADD_MANUAL_BTN)
                        ) {
                            Text(
                                text = stringResource(R.string.calendar_add_manual),
                                color = if (showManualForm) palette.accentText else palette.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (showManualForm && canLogForSelectedDay) {
                    val bedDate = if (manualBedtime.isAfter(manualWaketime)) {
                        selectedDate.minusDays(1)
                    } else {
                        selectedDate
                    }
                    val startEpochMillis = ZonedDateTime.of(bedDate, manualBedtime, zoneId).toInstant().toEpochMilli()
                    val endEpochMillis = ZonedDateTime.of(selectedDate, manualWaketime, zoneId).toInstant().toEpochMilli()
                    val (previewDuration, previewCycles) = SleepHistoryCodec.computeDurationAndCycles(
                        startEpochMillis = startEpochMillis,
                        endEpochMillis = endEpochMillis,
                        sleepLatencyMinutes = sleepLatencyMinutes
                    )
                    ManualSleepEntryCard(
                        bedtime = manualBedtime,
                        waketime = manualWaketime,
                        is24HourFormat = is24HourFormat,
                        selectedMood = manualMood,
                        previewDurationMinutes = previewDuration,
                        previewCycles = previewCycles,
                        onAdjustBedtime = { deltaMins ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            manualBedtime = manualBedtime.plusMinutes(deltaMins.toLong())
                        },
                        onAdjustWaketime = { deltaMins ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            manualWaketime = manualWaketime.plusMinutes(deltaMins.toLong())
                        },
                        onSelectMood = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            manualMood = it
                        },
                        onSave = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onAddManualSleep(startEpochMillis, endEpochMillis, manualMood)
                            showManualForm = false
                        }
                    )
                }

                if (selectedDayRecords.isEmpty()) {
                    Text(
                        text = if (sleepHistory.isEmpty()) {
                            stringResource(R.string.sleep_history_empty)
                        } else {
                            stringResource(R.string.calendar_no_entries_for_day)
                        },
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedDayRecords.forEach { record ->
                            SleepHistoryRow(
                                record = record,
                                is24HourFormat = is24HourFormat,
                                zoneId = zoneId,
                                onDelete = { onDeleteRecord(record) }
                            )
                        }
                    }
                }

                // Viewed Month Summary Footer
                val monthRecords = sleepHistory.filter { record ->
                    YearMonth.from(Instant.ofEpochMilli(record.endEpochMillis).atZone(zoneId).toLocalDate()) == displayedMonth
                }
                if (monthRecords.isNotEmpty()) {
                    val monthAvgMins = monthRecords.map { it.durationMinutes }.average().roundToInt()
                    Text(
                        text = stringResource(
                            R.string.calendar_month_summary,
                            monthRecords.size,
                            formatDurationCompact(monthAvgMins)
                        ),
                        style = TabularTextStyles.cardDuration.copy(fontSize = 11.sp),
                        color = palette.accentText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = TabularTextStyles.cardDuration.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = palette.accentText,
                maxLines = 1
            )
            Text(
                text = label,
                color = palette.textSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonthNavButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val shape = CircleShape
    Surface(
        shape = shape,
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SleepHistoryRow(
    record: SleepSessionRecord,
    is24HourFormat: Boolean,
    zoneId: ZoneId,
    onDelete: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val startClock = Instant.ofEpochMilli(record.startEpochMillis).atZone(zoneId).toLocalTime()
    val endClock = Instant.ofEpochMilli(record.endEpochMillis).atZone(zoneId).toLocalTime()
    val timeRange = "${formatClockTime(startClock, is24HourFormat)} → ${formatClockTime(endClock, is24HourFormat)}"
    val durationText = formatDurationCompact(record.durationMinutes)
    val metaText = if (record.cycles > 0) {
        stringResource(R.string.sleep_history_entry_meta, record.cycles, durationText)
    } else {
        durationText
    }
    val deleteDesc = stringResource(R.string.calendar_delete_entry)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.mood.emoji,
                    fontSize = 18.sp
                )
                Column {
                    Text(
                        text = "${stringResource(record.mood.labelResId)} • $timeRange",
                        style = TabularTextStyles.cardDuration,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metaText,
                        style = TabularTextStyles.cardDuration,
                        color = palette.textSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val filled = record.cycles.coerceIn(0, 6)
                    for (i in 1..6) {
                        Box(
                            modifier = Modifier
                                .size(width = 6.dp, height = 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i <= filled) palette.accent else palette.borderSubtle.copy(alpha = 0.45f)
                                )
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .semantics { contentDescription = deleteDesc }
                        .testTag(RestCycleTestTags.deleteSleepRecordBtn(record.endEpochMillis))
                        .clickable(role = Role.Button) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDurationCompact(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return if (hours == 0) {
        stringResource(R.string.duration_minutes_only, mins)
    } else {
        stringResource(R.string.duration_hours_minutes, hours, String.format(Locale.US, "%02d", mins))
    }
}

@Composable
private fun ManualSleepEntryCard(
    bedtime: LocalTime,
    waketime: LocalTime,
    is24HourFormat: Boolean,
    selectedMood: SleepQualityMood,
    previewDurationMinutes: Int,
    previewCycles: Int,
    onAdjustBedtime: (Int) -> Unit,
    onAdjustWaketime: (Int) -> Unit,
    onSelectMood: (SleepQualityMood) -> Unit,
    onSave: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RestCycleTestTags.CALENDAR_MANUAL_FORM)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ManualTimeStepperRow(
                label = stringResource(R.string.calendar_manual_bedtime),
                timeText = formatClockTime(bedtime, is24HourFormat),
                minusTag = RestCycleTestTags.MANUAL_BED_MINUS,
                plusTag = RestCycleTestTags.MANUAL_BED_PLUS,
                onMinus = { onAdjustBedtime(-15) },
                onPlus = { onAdjustBedtime(15) }
            )
            ManualTimeStepperRow(
                label = stringResource(R.string.calendar_manual_waketime),
                timeText = formatClockTime(waketime, is24HourFormat),
                minusTag = RestCycleTestTags.MANUAL_WAKE_MINUS,
                plusTag = RestCycleTestTags.MANUAL_WAKE_PLUS,
                onMinus = { onAdjustWaketime(-15) },
                onPlus = { onAdjustWaketime(15) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.calendar_manual_quality),
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SleepQualityMood.entries.forEach { mood ->
                        val isSelected = mood == selectedMood
                        val shape = RoundedCornerShape(10.dp)
                        Surface(
                            shape = shape,
                            color = if (isSelected) palette.accent.copy(alpha = 0.24f) else palette.cardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) palette.accent else palette.borderSubtle
                            ),
                            modifier = Modifier
                                .clip(shape)
                                .clickable(role = Role.RadioButton) { onSelectMood(mood) }
                                .testTag(RestCycleTestTags.manualMoodOption(mood.name))
                        ) {
                            Text(
                                text = mood.emoji,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.sleep_history_entry_meta,
                        previewCycles,
                        formatDurationCompact(previewDurationMinutes)
                    ),
                    style = TabularTextStyles.cardDuration,
                    color = palette.accentText
                )
                FilledTonalButton(
                    onClick = onSave,
                    enabled = previewDurationMinutes > 0,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.accent,
                        contentColor = palette.onAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag(RestCycleTestTags.CALENDAR_MANUAL_SAVE_BTN)
                ) {
                    Text(
                        text = stringResource(R.string.calendar_manual_save),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualTimeStepperRow(
    label: String,
    timeText: String,
    minusTag: String,
    plusTag: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.borderSubtle),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onMinus)
                    .testTag(minusTag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("−", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = timeText,
                style = TabularTextStyles.headerLiveClock,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = CircleShape,
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.borderSubtle),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onPlus)
                    .testTag(plusTag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
