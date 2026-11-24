package com.restcycle.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive spring press-scale helper: compresses smoothly when pressed
 * and springs back with subtle organic bounce on release.
 */
@Composable
fun rememberPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.93f,
    restingScale: Float = 1f
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else restingScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expressivePressScale"
    )
    return scale
}

/**
 * Precision rolling odometer text for clock times (e.g. "22:35", "06:15 AM").
 * Animates each changing character vertically while exposing the complete string
 * in semantics for TalkBack and UI tests.
 */
@Composable
fun RollingOdometerText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clearAndSetSemantics {
            this.text = AnnotatedString(text)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    val direction = if (targetState >= initialState) 1 else -1
                    (slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { height -> (height * 0.55f * direction).toInt() } + fadeIn(tween(140)))
                        .togetherWith(
                            slideOutVertically(
                                animationSpec = tween(130, easing = FastOutSlowInEasing)
                            ) { height -> (-height * 0.55f * direction).toInt() } + fadeOut(tween(110))
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "odometerChar_$index"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = style,
                    color = color,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Animated 3-bar living audio equalizer icon for the header Sound button when an
 * ambient soundscape is playing.
 */
@Composable
fun LiveEqualizerIcon(
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 19.dp
) {
    val transition = rememberInfiniteTransition(label = "liveEqualizer")
    val bar1 by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 610, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar3"
    )

    Canvas(modifier = modifier.size(iconSize)) {
        val barWidth = size.width * 0.20f
        val gap = size.width * 0.14f
        val totalW = barWidth * 3f + gap * 2f
        val startX = (size.width - totalW) * 0.5f
        val maxH = size.height * 0.78f
        val bottomY = size.height * 0.89f
        val radius = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)
        val heights = floatArrayOf(bar1, bar2, bar3)

        heights.forEachIndexed { idx, frac ->
            val h = (maxH * frac).coerceAtLeast(barWidth)
            drawRoundRect(
                color = color,
                topLeft = Offset(x = startX + idx * (barWidth + gap), y = bottomY - h),
                size = Size(width = barWidth, height = h),
                cornerRadius = radius
            )
        }
    }
}
