package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.restcycle.app.R
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class BreathingPhase(val durationSeconds: Int, val labelResId: Int) {
    INHALE(4, R.string.breathing_phase_inhale),
    HOLD(7, R.string.breathing_phase_hold),
    EXHALE(8, R.string.breathing_phase_exhale)
}

@Composable
fun BreathingExerciseDialog(
    onDismiss: () -> Unit,
    ambientSoundMode: AmbientSoundMode = AmbientSoundMode.OFF,
    onCycleAmbientSound: (() -> Unit)? = null
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current

    var isRunning by remember { mutableStateOf(true) }
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var remainingSeconds by remember { mutableIntStateOf(BreathingPhase.INHALE.durationSeconds) }
    var cycleCount by remember { mutableIntStateOf(1) }
    val orbScale = remember { Animatable(0.45f) }

    LaunchedEffect(isRunning, currentPhase) {
        if (!isRunning) return@LaunchedEffect
        val targetScale = when (currentPhase) {
            BreathingPhase.INHALE -> 1.0f
            BreathingPhase.HOLD -> 1.0f
            BreathingPhase.EXHALE -> 0.45f
        }
        val animDurationMs = remainingSeconds * 1000
        val animJob = launch {
            orbScale.animateTo(
                targetValue = targetScale,
                animationSpec = tween(
                    durationMillis = animDurationMs.coerceAtLeast(100),
                    easing = if (currentPhase == BreathingPhase.HOLD) LinearEasing else FastOutSlowInEasing
                )
            )
        }
        while (remainingSeconds > 0 && isRunning) {
            delay(1000L)
            remainingSeconds -= 1
        }
        animJob.cancel()
        if (isRunning && remainingSeconds <= 0) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val nextPhase = when (currentPhase) {
                BreathingPhase.INHALE -> BreathingPhase.HOLD
                BreathingPhase.HOLD -> BreathingPhase.EXHALE
                BreathingPhase.EXHALE -> {
                    cycleCount += 1
                    BreathingPhase.INHALE
                }
            }
            currentPhase = nextPhase
            remainingSeconds = nextPhase.durationSeconds
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RestCycleTestTags.BREATHING_DIALOG),
            shape = RoundedCornerShape(26.dp),
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.borderSubtle),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.breathing_title),
                        color = palette.textPrimary,
                        fontSize = 17.sp,
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

                Text(
                    text = stringResource(R.string.breathing_subtitle),
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val accentColor = palette.accent
                    Canvas(modifier = Modifier.size(190.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension / 2f
                        val scale = orbScale.value.coerceIn(0.35f, 1.0f)

                        // Outer soft halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.34f),
                                    accentColor.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = maxRadius * (0.62f + 0.38f * scale)
                            ),
                            radius = maxRadius * (0.62f + 0.38f * scale),
                            center = center
                        )
                        // Mid ring
                        drawCircle(
                            color = accentColor.copy(alpha = 0.22f),
                            radius = maxRadius * (0.48f + 0.34f * scale),
                            center = center
                        )
                        // Core breathing orb
                        drawCircle(
                            color = accentColor.copy(alpha = 0.42f),
                            radius = maxRadius * (0.36f + 0.28f * scale),
                            center = center
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(currentPhase.labelResId),
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = remainingSeconds.coerceAtLeast(1).toString(),
                            style = TabularTextStyles.cardTargetTime,
                            color = palette.accentText
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.breathing_cycle_count, cycleCount),
                    style = TabularTextStyles.cardDuration,
                    color = palette.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            isRunning = !isRunning
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.pillContainer,
                            contentColor = palette.textPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(
                                if (isRunning) R.string.breathing_pause else R.string.breathing_resume
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (onCycleAmbientSound != null) {
                        val soundActive = ambientSoundMode != AmbientSoundMode.OFF
                        val soundText = stringResource(
                            if (soundActive) ambientSoundMode.labelResId else R.string.ambient_sound_button
                        )
                        val soundIcon = if (soundActive) {
                            RestCycleIcons.forAmbientSound(ambientSoundMode)
                        } else {
                            RestCycleIcons.AudioWaveform
                        }
                        val soundTint = if (soundActive) palette.onAccent else palette.textPrimary
                        FilledTonalButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onCycleAmbientSound()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (soundActive) palette.accent else palette.pillContainer,
                                contentColor = soundTint
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = soundIcon,
                                    contentDescription = null,
                                    tint = soundTint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = soundText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
