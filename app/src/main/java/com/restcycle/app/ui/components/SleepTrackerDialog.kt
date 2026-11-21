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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.restcycle.app.R
import com.restcycle.app.domain.model.SleepQualityMood
import com.restcycle.app.domain.model.SleepSessionRecord
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.util.formatClockTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SleepTrackerDialog(
    activeSleepStartMillis: Long,
    sleepHistory: List<SleepSessionRecord>,
    is24HourFormat: Boolean,
    onStartSleep: () -> Unit,
    onCompleteSleepWithMood: (SleepQualityMood) -> Unit,
    onCancelSleep: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val isSleeping = activeSleepStartMillis > 0L

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
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
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                if (isSleeping) {
                    val startTime = Instant.ofEpochMilli(activeSleepStartMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                    val formattedStart = formatClockTime(startTime, is24HourFormat)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.sleep_sleeping_since, formattedStart),
                            style = TabularTextStyles.cardDuration,
                            color = palette.accentText
                        )
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
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onCancelSleep()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_cancel_session),
                                fontSize = 12.sp
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

                // 7-Day Sleep History Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sleep_history_title),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (sleepHistory.isNotEmpty()) {
                        val avgCycles = sleepHistory.map { it.cycles }.average()
                        val avgText = String.format(Locale.US, "%.1f", avgCycles)
                        Text(
                            text = stringResource(R.string.sleep_history_avg_cycles, avgText),
                            style = TabularTextStyles.cardDuration,
                            color = palette.accentText
                        )
                    }
                }

                if (sleepHistory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.sleep_history_empty),
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sleepHistory.forEach { record ->
                            SleepHistoryRow(record = record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepHistoryRow(record: SleepSessionRecord) {
    val palette = AtmosphereTheme.palette
    val dateLabel = Instant.ofEpochMilli(record.endEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MM/dd"))
    val hours = record.durationMinutes / 60
    val mins = record.durationMinutes % 60
    val durationText = stringResource(
        R.string.duration_hours_minutes,
        hours,
        String.format(Locale.US, "%02d", mins)
    )

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.mood.emoji,
                    fontSize = 16.sp
                )
                Column {
                    Text(
                        text = "$dateLabel • ${stringResource(record.mood.labelResId)}",
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.sleep_history_entry_meta, record.cycles, durationText),
                        style = TabularTextStyles.cardDuration,
                        color = palette.textSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                val filled = record.cycles.coerceIn(1, 6)
                for (i in 1..6) {
                    Box(
                        modifier = Modifier
                            .size(width = 7.dp, height = 10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i <= filled) palette.accent else palette.borderSubtle.copy(alpha = 0.45f)
                            )
                    )
                }
            }
        }
    }
}
