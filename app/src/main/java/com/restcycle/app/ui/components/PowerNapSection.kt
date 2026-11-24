package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.restcycle.app.domain.model.SleepOption
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.util.formatClockTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PowerNapPresetCard(
    option: SleepOption,
    isHighlighted: Boolean,
    is24HourFormat: Boolean,
    onSelect: () -> Unit,
    onSetAlarm: (SleepOption) -> Unit,
    onSetTimer: (SleepOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val formattedTime = formatClockTime(option.targetTime, is24HourFormat)
    val presetTitle = stringResource(id = option.labelResId)
    val setAlarmCd = stringResource(R.string.cd_set_alarm_for_time, formattedTime)
    val setTimerCd = stringResource(R.string.cd_set_timer_for_nap, presetTitle)

    val descriptionRes = when (option.id) {
        SleepCycleCalculator.NAP_QUICK_ID -> R.string.nap_quick_desc
        SleepCycleCalculator.NAP_CLASSIC_ID -> R.string.nap_classic_desc
        else -> R.string.nap_full_desc
    }
    val durationText = stringResource(R.string.duration_minutes_only, option.durationMinutes)
    val highlightFraction by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "napCardHighlight"
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = presetTitle,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        if (option.id == SleepCycleCalculator.NAP_CLASSIC_ID) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.accent.copy(alpha = 0.20f),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.badge_suggested),
                                    color = palette.accentText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(id = descriptionRes),
                        color = palette.textSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    RollingOdometerText(
                        text = formattedTime,
                        style = TabularTextStyles.cardTargetTime.copy(fontSize = 20.sp, lineHeight = 24.sp),
                        color = palette.textPrimary
                    )
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        Text(
                            text = durationText,
                            style = TabularTextStyles.cardDuration,
                            color = palette.textSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        option.countdownMinutesFromNow?.let { mins ->
                            Text(
                                text = stringResource(R.string.countdown_in_minutes, mins.toInt()),
                                style = TabularTextStyles.cardCountdown,
                                color = palette.accentText,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSetAlarm(option)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = buttonContainerColor,
                            contentColor = buttonContentColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 32.dp)
                            .testTag(RestCycleTestTags.setAlarmButton(option.id))
                            .semantics {
                                contentDescription = setAlarmCd
                            }
                    ) {
                        Icon(
                            imageVector = RestCycleIcons.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.action_set_alarm),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSetTimer(option)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 32.dp)
                            .testTag(RestCycleTestTags.setTimerButton(option.id))
                            .semantics {
                                contentDescription = setTimerCd
                            }
                    ) {
                        Icon(
                            imageVector = RestCycleIcons.Timer,
                            contentDescription = null,
                            tint = palette.accentText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.action_set_timer),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PowerNapInertiaTooltip(modifier: Modifier = Modifier) {
    val palette = AtmosphereTheme.palette
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(RestCycleTestTags.INERTIA_TOOLTIP)
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(14.dp),
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = RestCycleIcons.Info,
                contentDescription = null,
                tint = palette.accentText,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(15.dp)
            )
            Text(
                text = stringResource(id = R.string.nap_inertia_tooltip),
                color = palette.textSecondary,
                fontSize = 11.5.sp,
                lineHeight = 14.5.sp
            )
        }
    }
}
