package com.restcycle.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.restcycle.app.R
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.ATMOSPHERE_TRANSITION_DURATION_MS
import com.restcycle.app.ui.theme.AtmosphereTheme
import kotlin.math.cos
import kotlin.math.sin

private data class StarSpec(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val invertedPhase: Boolean
)

private val NIGHT_STARS = listOf(
    StarSpec(0.14f, 0.22f, 1.3f, false),
    StarSpec(0.28f, 0.14f, 1.7f, true),
    StarSpec(0.42f, 0.30f, 1.1f, false),
    StarSpec(0.76f, 0.18f, 1.8f, true),
    StarSpec(0.88f, 0.34f, 1.3f, false),
    StarSpec(0.20f, 0.72f, 1.2f, true),
    StarSpec(0.82f, 0.70f, 1.5f, false),
    StarSpec(0.68f, 0.84f, 1.1f, true),
    StarSpec(0.32f, 0.82f, 1.4f, false)
)

@Composable
fun CelestialHeader(
    skyPhase: SkyPhase,
    mode: SleepMode,
    modifier: Modifier = Modifier,
    ambientSoundMode: AmbientSoundMode = AmbientSoundMode.OFF,
    onOpenSoundPicker: (() -> Unit)? = null,
    onOpenSleepHistory: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val phaseAnimSpec = tween<Float>(durationMillis = ATMOSPHERE_TRANSITION_DURATION_MS)
    val dayAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.DAY) 1f else 0f,
        animationSpec = phaseAnimSpec,
        label = "dayAlpha"
    )
    val dawnAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.DAWN_DUSK) 1f else 0f,
        animationSpec = phaseAnimSpec,
        label = "dawnAlpha"
    )
    val nightAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.NIGHT) 1f else 0f,
        animationSpec = phaseAnimSpec,
        label = "nightAlpha"
    )
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.DAWN_DUSK) 0.68f else 0f,
        animationSpec = phaseAnimSpec,
        label = "headerTextScrimAlpha"
    )
    val textScrimColor = palette.backgroundBottom.copy(alpha = headerScrimAlpha)

    val infiniteTransition = rememberInfiniteTransition(label = "celestial_motion")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40_000, easing = LinearEasing)
        ),
        label = "sunRotation"
    )
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    val celestialDescription = when (skyPhase) {
        SkyPhase.DAY -> stringResource(R.string.cd_celestial_day)
        SkyPhase.DAWN_DUSK -> stringResource(R.string.cd_celestial_dawn_dusk)
        SkyPhase.NIGHT -> stringResource(R.string.cd_celestial_night)
    }
    val skyPhaseLabel = when (skyPhase) {
        SkyPhase.DAY -> stringResource(R.string.sky_phase_day)
        SkyPhase.DAWN_DUSK -> stringResource(R.string.sky_phase_dawn_dusk)
        SkyPhase.NIGHT -> stringResource(R.string.sky_phase_night)
    }
    val modeSubtitle = when (mode) {
        SleepMode.WAKE_UP_AT -> stringResource(R.string.header_subtitle_wake_up_at)
        SleepMode.SLEEP_NOW -> stringResource(R.string.header_subtitle_sleep_now)
        SleepMode.GO_TO_BED_AT -> stringResource(R.string.header_subtitle_go_to_bed_at)
        SleepMode.POWER_NAP -> stringResource(R.string.header_subtitle_power_nap)
    }

    val outerMoonPath = remember { Path() }
    val cutoutMoonPath = remember { Path() }
    val crescentMoonPath = remember { Path() }
    val horizonPath = remember { Path() }
    val groundFillPath = remember { Path() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp, max = 82.dp)
            .background(Color.Transparent)
            .testTag(RestCycleTestTags.CELESTIAL_HEADER)
            .semantics {
                contentDescription = celestialDescription
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
        Canvas(
            modifier = Modifier.size(50.dp)
        ) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val minDim = size.minDimension

            if (dayAlpha > 0.01f) {
                val sunRadius = minDim * 0.21f * sunPulse
                val haloRadius = minDim * 0.48f * sunPulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.28f * dayAlpha),
                            Color(0xFFFDE68A).copy(alpha = 0.10f * dayAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = haloRadius
                    ),
                    radius = haloRadius,
                    center = center
                )

                rotate(degrees = sunRotation, pivot = center) {
                    val rayCount = 12
                    val innerR = minDim * 0.27f
                    val outerR = minDim * 0.39f * sunPulse
                    for (i in 0 until rayCount) {
                        val angleRad = Math.toRadians((i * (360.0 / rayCount)))
                        val cosA = cos(angleRad).toFloat()
                        val sinA = sin(angleRad).toFloat()
                        drawLine(
                            color = Color(0xFFF59E0B).copy(alpha = 0.90f * dayAlpha),
                            start = Offset(center.x + innerR * cosA, center.y + innerR * sinA),
                            end = Offset(center.x + outerR * cosA, center.y + outerR * sinA),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = dayAlpha),
                    radius = sunRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFDE68A).copy(alpha = 0.72f * dayAlpha),
                    radius = sunRadius * 0.62f,
                    center = Offset(center.x - sunRadius * 0.14f, center.y - sunRadius * 0.14f)
                )
            }

            if (dawnAlpha > 0.01f) {
                val horizonY = size.height * 0.64f
                val sunCenter = Offset(size.width * 0.5f, horizonY - minDim * 0.03f)
                val hazeRadius = minDim * 0.48f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD96B27).copy(alpha = 0.48f * dawnAlpha),
                            Color(0xFFFF8C42).copy(alpha = 0.20f * dawnAlpha),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = hazeRadius
                    ),
                    radius = hazeRadius,
                    center = sunCenter
                )

                clipRect(top = 0f, left = 0f, right = size.width, bottom = horizonY) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFCF8B).copy(alpha = dawnAlpha),
                                Color(0xFFFF8C42).copy(alpha = dawnAlpha),
                                Color(0xFFD96B27).copy(alpha = 0.85f * dawnAlpha)
                            ),
                            center = sunCenter,
                            radius = minDim * 0.24f
                        ),
                        radius = minDim * 0.22f * sunPulse,
                        center = sunCenter
                    )
                }

                groundFillPath.reset()
                groundFillPath.moveTo(0f, horizonY + minDim * 0.03f)
                groundFillPath.quadraticTo(
                    size.width * 0.5f,
                    horizonY - minDim * 0.08f,
                    size.width,
                    horizonY + minDim * 0.03f
                )
                groundFillPath.lineTo(size.width, size.height)
                groundFillPath.lineTo(0f, size.height)
                groundFillPath.close()
                drawPath(
                    path = groundFillPath,
                    color = Color(0xFF1F1523).copy(alpha = 0.82f * dawnAlpha)
                )

                horizonPath.reset()
                horizonPath.moveTo(minDim * 0.08f, horizonY + minDim * 0.03f)
                horizonPath.quadraticTo(
                    size.width * 0.5f,
                    horizonY - minDim * 0.08f,
                    size.width - minDim * 0.08f,
                    horizonY + minDim * 0.03f
                )
                drawPath(
                    path = horizonPath,
                    color = Color(0xFFFF8C42).copy(alpha = 0.92f * dawnAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            if (nightAlpha > 0.01f) {
                for (star in NIGHT_STARS) {
                    val alphaFactor = if (star.invertedPhase) 1.35f - starTwinkle else starTwinkle
                    val clampedAlpha = (alphaFactor * nightAlpha).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFFF2EFE9).copy(alpha = clampedAlpha),
                        radius = star.radiusDp.dp.toPx(),
                        center = Offset(size.width * star.xFraction, size.height * star.yFraction)
                    )
                }

                val moonRadius = minDim * 0.23f
                val moonCenter = Offset(size.width * 0.50f, size.height * 0.48f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE6C687).copy(alpha = 0.22f * nightAlpha),
                            Color.Transparent
                        ),
                        center = moonCenter,
                        radius = moonRadius * 1.9f
                    ),
                    radius = moonRadius * 1.9f,
                    center = moonCenter
                )

                outerMoonPath.reset()
                outerMoonPath.addOval(
                    Rect(
                        left = moonCenter.x - moonRadius,
                        top = moonCenter.y - moonRadius,
                        right = moonCenter.x + moonRadius,
                        bottom = moonCenter.y + moonRadius
                    )
                )
                cutoutMoonPath.reset()
                val cutOffsetX = moonRadius * 0.52f
                val cutOffsetY = -moonRadius * 0.28f
                val cutRadius = moonRadius * 0.88f
                cutoutMoonPath.addOval(
                    Rect(
                        left = moonCenter.x + cutOffsetX - cutRadius,
                        top = moonCenter.y + cutOffsetY - cutRadius,
                        right = moonCenter.x + cutOffsetX + cutRadius,
                        bottom = moonCenter.y + cutOffsetY + cutRadius
                    )
                )
                crescentMoonPath.reset()
                crescentMoonPath.op(outerMoonPath, cutoutMoonPath, PathOperation.Difference)
                drawPath(
                    path = crescentMoonPath,
                    color = Color(0xFFE6C687).copy(alpha = nightAlpha)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .background(color = textScrimColor, shape = RoundedCornerShape(14.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$skyPhaseLabel · $modeSubtitle",
                color = palette.textSecondary,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        }

        if (onOpenSoundPicker != null || onOpenSleepHistory != null || onOpenSettings != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenSoundPicker != null) {
                    val soundActive = ambientSoundMode != AmbientSoundMode.OFF
                    val soundInteraction = remember { MutableInteractionSource() }
                    val soundScale = rememberPressScale(soundInteraction, pressedScale = 0.89f)
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = soundScale
                                scaleY = soundScale
                            }
                            .clip(CircleShape)
                            .testTag(RestCycleTestTags.SOUND_PICKER_PILL)
                            .clickable(
                                interactionSource = soundInteraction,
                                indication = null,
                                role = Role.Button
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onOpenSoundPicker()
                            },
                        shape = CircleShape,
                        color = if (soundActive) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
                        border = BorderStroke(
                            width = if (soundActive) 1.4.dp else 1.dp,
                            color = if (soundActive) palette.accent else palette.borderSubtle
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (soundActive) {
                                LiveEqualizerIcon(
                                    color = palette.accentText,
                                    iconSize = 19.dp
                                )
                            } else {
                                Icon(
                                    imageVector = RestCycleIcons.AudioWaveform,
                                    contentDescription = stringResource(R.string.sound_dialog_title),
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
                if (onOpenSleepHistory != null) {
                    val calInteraction = remember { MutableInteractionSource() }
                    val calScale = rememberPressScale(calInteraction, pressedScale = 0.89f)
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = calScale
                                scaleY = calScale
                            }
                            .clip(CircleShape)
                            .testTag(RestCycleTestTags.SLEEP_HISTORY_BUTTON)
                            .clickable(
                                interactionSource = calInteraction,
                                indication = null,
                                role = Role.Button
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onOpenSleepHistory()
                            },
                        shape = CircleShape,
                        color = palette.pillContainer,
                        border = BorderStroke(1.dp, palette.borderSubtle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = RestCycleIcons.Calendar,
                                contentDescription = stringResource(R.string.sleep_history_title),
                                tint = palette.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
                if (onOpenSettings != null) {
                    val settingsInteraction = remember { MutableInteractionSource() }
                    val settingsScale = rememberPressScale(settingsInteraction, pressedScale = 0.89f)
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = settingsScale
                                scaleY = settingsScale
                            }
                            .clip(CircleShape)
                            .testTag(RestCycleTestTags.SETTINGS_BUTTON)
                            .clickable(
                                interactionSource = settingsInteraction,
                                indication = null,
                                role = Role.Button
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onOpenSettings()
                            },
                        shape = CircleShape,
                        color = palette.pillContainer,
                        border = BorderStroke(1.dp, palette.borderSubtle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = RestCycleIcons.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = palette.accentText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
