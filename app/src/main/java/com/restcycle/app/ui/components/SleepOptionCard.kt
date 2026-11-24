package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.R
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.domain.model.CycleQuality
import com.restcycle.app.domain.model.SleepOption
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.util.formatClockTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepOptionCard(
    option: SleepOption,
    isHighlighted: Boolean,
    is24HourFormat: Boolean,
    onSelect: () -> Unit,
    onSetAlarm: (SleepOption) -> Unit,
    modifier: Modifier = Modifier,
    untilBedtimeMinutes: Long? = null
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val formattedTime = formatClockTime(option.targetTime, is24HourFormat)
    val setAlarmCd = stringResource(R.string.cd_set_alarm_for_time, formattedTime)

    val hours = option.durationMinutes / 60
    val mins = option.durationMinutes % 60
    val durationText = if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, mins.toString().padStart(2, '0'))
    } else {
        stringResource(R.string.duration_minutes_only, mins)
    }

    val cyclesText = option.cycles?.let { count ->
        if (count == 1) {
            stringResource(R.string.cycles_count_single, count)
        } else {
            stringResource(R.string.cycles_count, count)
        }
    }

    val qualityBadgeColor = when (option.quality) {
        CycleQuality.OPTIMAL -> palette.accent
        CycleQuality.GOOD -> palette.accent.copy(alpha = 0.78f)
        CycleQuality.SHORT, CycleQuality.POWER_NAP -> palette.textSecondary
    }
    val isSuggested = option.cycles == SleepCycleCalculator.DEFAULT_SUGGESTED_CYCLES
    val highlightFraction by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "sleepCardHighlight"
    )
    val cardSurfaceColor = lerp(palette.cardSurface, palette.cardSurfaceHighlighted, highlightFraction)
    val cardBorderColor = lerp(palette.borderSubtle, palette.accent, highlightFraction)
    val buttonContainerColor = lerp(palette.pillContainer, palette.accent, highlightFraction)
    val buttonContentColor = lerp(palette.textPrimary, palette.onAccent, highlightFraction)

    val cardInteraction = remember { MutableInteractionSource() }
    val cardScale = rememberPressScale(
        interactionSource = cardInteraction,
        pressedScale = 0.97f,
        restingScale = if (isHighlighted) 1.012f else 1f
    )
    val alarmBtnInteraction = remember { MutableInteractionSource() }
    val alarmBtnScale = rememberPressScale(alarmBtnInteraction, pressedScale = 0.91f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .testTag(RestCycleTestTags.optionCard(option.id))
            .selectable(
                selected = isHighlighted,
                interactionSource = cardInteraction,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onSelect()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = cardSurfaceColor,
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            color = cardBorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RollingOdometerText(
                        text = formattedTime,
                        style = TabularTextStyles.cardTargetTime,
                        color = palette.textPrimary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = qualityBadgeColor.copy(alpha = 0.16f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = stringResource(id = option.labelResId),
                            color = if (option.quality == CycleQuality.SHORT) palette.textSecondary else palette.accentText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (isSuggested) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accent.copy(alpha = 0.22f),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = stringResource(id = R.string.badge_suggested),
                                color = palette.accentText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    option.cycles?.let { cycleCount ->
                        CycleCapsulesIndicator(
                            cycles = cycleCount,
                            activeColor = qualityBadgeColor,
                            inactiveColor = palette.textSecondary.copy(alpha = 0.22f),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .testTag(RestCycleTestTags.cycleDots(option.id))
                        )
                    }
                    if (cyclesText != null) {
                        Text(
                            text = cyclesText,
                            style = TabularTextStyles.cardDuration,
                            color = palette.textSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "•",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Text(
                        text = durationText,
                        style = TabularTextStyles.cardDuration,
                        color = palette.textSecondary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    val countdown = option.countdownMinutesFromNow
                    if (countdown != null) {
                        Text(
                            text = "•",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = stringResource(R.string.countdown_in_minutes, countdown.toInt()),
                            style = TabularTextStyles.cardCountdown,
                            color = palette.accentText,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    } else if (untilBedtimeMinutes != null) {
                        val untilLabel = when {
                            untilBedtimeMinutes <= 0L -> stringResource(R.string.bedtime_countdown_now)
                            untilBedtimeMinutes >= 60L -> stringResource(
                                R.string.bedtime_countdown_hm,
                                (untilBedtimeMinutes / 60L).toInt(),
                                (untilBedtimeMinutes % 60L).toInt()
                            )
                            else -> stringResource(R.string.bedtime_countdown_m, untilBedtimeMinutes.toInt())
                        }
                        Text(
                            text = "•",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = untilLabel,
                            style = TabularTextStyles.cardCountdown,
                            color = palette.accentText,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onSetAlarm(option)
                },
                interactionSource = alarmBtnInteraction,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .graphicsLayer {
                        scaleX = alarmBtnScale
                        scaleY = alarmBtnScale
                    }
                    .testTag(RestCycleTestTags.setAlarmButton(option.id))
                    .semantics {
                        contentDescription = setAlarmCd
                    }
            ) {
                Icon(
                    imageVector = RestCycleIcons.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_set_alarm),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CycleCapsulesIndicator(
    cycles: Int,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 36.dp, height = 6.dp)) {
        val totalSlots = 6
        val gapPx = 2.dp.toPx()
        val pillWidth = ((size.width - gapPx * (totalSlots - 1)) / totalSlots).coerceAtLeast(2f)
        val radius = size.height / 2f
        val corner = CornerRadius(radius, radius)
        for (i in 0 until totalSlots) {
            val left = i * (pillWidth + gapPx)
            drawRoundRect(
                color = if (i < cycles) activeColor else inactiveColor,
                topLeft = Offset(left, 0f),
                size = Size(pillWidth, size.height),
                cornerRadius = corner
            )
        }
    }
}
