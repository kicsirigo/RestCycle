package com.restcycle.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.ui.theme.ATMOSPHERE_TRANSITION_DURATION_MS
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import kotlin.math.sin

private data class LandscapeColors(
    val celestialGlow: Color,
    val celestialDisc: Color,
    val farMountain: Color,
    val midMountain: Color,
    val midMountainHighlight: Color,
    val foothill: Color,
    val treeBack: Color,
    val treeFront: Color,
    val lakeTop: Color,
    val lakeBottom: Color,
    val streamColor: Color,
    val waterShimmer: Color
)

private val DayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFF3C66E),
    celestialDisc = Color(0xFFF6D67B),
    farMountain = Color(0xFF9EB0B3),
    midMountain = Color(0xFF728A8B),
    midMountainHighlight = Color(0xFF9FB8AD),
    foothill = Color(0xFF5D7A67),
    treeBack = Color(0xFF425F4D),
    treeFront = Color(0xFF314B3C),
    lakeTop = Color(0xFF7BA2B5),
    lakeBottom = Color(0xFF9AB8C2),
    streamColor = Color(0xFF8FB8C7),
    waterShimmer = Color(0xFFF6E8C3)
)

private val DawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFE68345),
    celestialDisc = Color(0xFFFFC178),
    farMountain = Color(0xFF5A313A),
    midMountain = Color(0xFF3E222D),
    midMountainHighlight = Color(0xFF8C493F),
    foothill = Color(0xFF281922),
    treeBack = Color(0xFF23151D),
    treeFront = Color(0xFF190E16),
    lakeTop = Color(0xFF6B3E3E),
    lakeBottom = Color(0xFF271724),
    streamColor = Color(0xFFB3694F),
    waterShimmer = Color(0xFFFFB56B)
)

private val NightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFE6C687),
    celestialDisc = Color(0xFFE6C687),
    farMountain = Color(0xFF192038),
    midMountain = Color(0xFF11172A),
    midMountainHighlight = Color(0xFF293352),
    foothill = Color(0xFF0A0F1D),
    treeBack = Color(0xFF090F1A),
    treeFront = Color(0xFF050810),
    lakeTop = Color(0xFF121F38),
    lakeBottom = Color(0xFF040710),
    streamColor = Color(0xFF233759),
    waterShimmer = Color(0xFFE6C687)
)

private val NordDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFEBCB8B),
    celestialDisc = Color(0xFFEBCB8B),
    farMountain = Color(0xFF81A1C1),
    midMountain = Color(0xFF5E81AC),
    midMountainHighlight = Color(0xFF88C0D0),
    foothill = Color(0xFF4C566A),
    treeBack = Color(0xFF434C5E),
    treeFront = Color(0xFF2E3440),
    lakeTop = Color(0xFF88C0D0),
    lakeBottom = Color(0xFF5E81AC),
    streamColor = Color(0xFF8FBCBB),
    waterShimmer = Color(0xFFECEFF4)
)

private val NordDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFD08770),
    celestialDisc = Color(0xFFEBCB8B),
    farMountain = Color(0xFF5E687E),
    midMountain = Color(0xFF434C5E),
    midMountainHighlight = Color(0xFFD08770),
    foothill = Color(0xFF2E3440),
    treeBack = Color(0xFF29303B),
    treeFront = Color(0xFF1F242E),
    lakeTop = Color(0xFF5E81AC),
    lakeBottom = Color(0xFF2E3440),
    streamColor = Color(0xFF88C0D0),
    waterShimmer = Color(0xFFEBCB8B)
)

private val NordNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFF88C0D0),
    celestialDisc = Color(0xFFD8DEE9),
    farMountain = Color(0xFF3B4252),
    midMountain = Color(0xFF2E3440),
    midMountainHighlight = Color(0xFF5E81AC),
    foothill = Color(0xFF242933),
    treeBack = Color(0xFF1E232C),
    treeFront = Color(0xFF171B22),
    lakeTop = Color(0xFF3B4C66),
    lakeBottom = Color(0xFF1D212A),
    streamColor = Color(0xFF5E81AC),
    waterShimmer = Color(0xFF88C0D0)
)

private val CatppuccinDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFDF8E1D),
    celestialDisc = Color(0xFFFE640B),
    farMountain = Color(0xFF9CA0B0),
    midMountain = Color(0xFF7287FD),
    midMountainHighlight = Color(0xFFB4BEFE),
    foothill = Color(0xFF40A02B),
    treeBack = Color(0xFF378828),
    treeFront = Color(0xFF28691E),
    lakeTop = Color(0xFF74C7EC),
    lakeBottom = Color(0xFF1E66F5),
    streamColor = Color(0xFF89DCEB),
    waterShimmer = Color(0xFFEFF1F5)
)

private val CatppuccinDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFF5A97F),
    celestialDisc = Color(0xFFEED49F),
    farMountain = Color(0xFF5B6078),
    midMountain = Color(0xFF494D64),
    midMountainHighlight = Color(0xFFF5A97F),
    foothill = Color(0xFF2C3047),
    treeBack = Color(0xFF24273A),
    treeFront = Color(0xFF1B1D2C),
    lakeTop = Color(0xFF6E738D),
    lakeBottom = Color(0xFF24273A),
    streamColor = Color(0xFF91D7E3),
    waterShimmer = Color(0xFFF5A97F)
)

private val CatppuccinNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFC6A0F6),
    celestialDisc = Color(0xFFB7BDF8),
    farMountain = Color(0xFF363A4F),
    midMountain = Color(0xFF24273A),
    midMountainHighlight = Color(0xFF8AADF4),
    foothill = Color(0xFF181926),
    treeBack = Color(0xFF161824),
    treeFront = Color(0xFF10111A),
    lakeTop = Color(0xFF3B4261),
    lakeBottom = Color(0xFF12131D),
    streamColor = Color(0xFF7DC4E4),
    waterShimmer = Color(0xFFC6A0F6)
)

private fun landscapeColorsFor(
    skyPhase: SkyPhase,
    themeStyle: AppThemeStyle
): LandscapeColors = when (themeStyle) {
    AppThemeStyle.NORD -> when (skyPhase) {
        SkyPhase.DAY -> NordDayLandscapeColors
        SkyPhase.DAWN_DUSK -> NordDawnDuskLandscapeColors
        SkyPhase.NIGHT -> NordNightLandscapeColors
    }
    AppThemeStyle.CATPPUCCIN_MACCHIATO -> when (skyPhase) {
        SkyPhase.DAY -> CatppuccinDayLandscapeColors
        SkyPhase.DAWN_DUSK -> CatppuccinDawnDuskLandscapeColors
        SkyPhase.NIGHT -> CatppuccinNightLandscapeColors
    }
    AppThemeStyle.DYNAMIC -> when (skyPhase) {
        SkyPhase.DAY -> DayLandscapeColors
        SkyPhase.DAWN_DUSK -> DawnDuskLandscapeColors
        SkyPhase.NIGHT -> NightLandscapeColors
    }
}

private data class PineSpec(
    val xFraction: Float,
    val baseYFraction: Float,
    val heightFraction: Float,
    val isFrontLayer: Boolean
)

private val SCENE_PINES = listOf(
    PineSpec(0.08f, 0.70f, 0.13f, false),
    PineSpec(0.13f, 0.72f, 0.18f, true),
    PineSpec(0.19f, 0.69f, 0.12f, false),
    PineSpec(0.24f, 0.71f, 0.15f, true),
    PineSpec(0.30f, 0.67f, 0.10f, false),
    PineSpec(0.36f, 0.64f, 0.08f, false),
    PineSpec(0.64f, 0.64f, 0.08f, false),
    PineSpec(0.70f, 0.66f, 0.11f, false),
    PineSpec(0.76f, 0.70f, 0.16f, true),
    PineSpec(0.82f, 0.68f, 0.12f, false),
    PineSpec(0.87f, 0.72f, 0.19f, true),
    PineSpec(0.92f, 0.70f, 0.13f, false)
)

private data class SceneStar(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val phaseOffset: Float
)

private val SCENE_STARS = listOf(
    SceneStar(0.16f, 0.18f, 1.1f, 0.0f),
    SceneStar(0.26f, 0.24f, 1.4f, 1.1f),
    SceneStar(0.36f, 0.14f, 1.0f, 2.0f),
    SceneStar(0.45f, 0.20f, 1.2f, 0.6f),
    SceneStar(0.62f, 0.15f, 1.3f, 2.7f),
    SceneStar(0.74f, 0.22f, 1.1f, 1.5f),
    SceneStar(0.84f, 0.17f, 1.4f, 0.4f),
    SceneStar(0.20f, 0.35f, 1.0f, 2.3f),
    SceneStar(0.80f, 0.32f, 1.1f, 1.9f)
)

@Composable
fun CelestialLandscapeScene(
    skyPhase: SkyPhase,
    hintText: String,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val themeStyle = AtmosphereTheme.themeStyle
    val target = landscapeColorsFor(skyPhase, themeStyle)
    val colorSpec = tween<Color>(durationMillis = ATMOSPHERE_TRANSITION_DURATION_MS)

    val celestialGlow by animateColorAsState(target.celestialGlow, colorSpec, label = "sceneCelGlow")
    val celestialDisc by animateColorAsState(target.celestialDisc, colorSpec, label = "sceneCelDisc")
    val farMountain by animateColorAsState(target.farMountain, colorSpec, label = "sceneFarMtn")
    val midMountain by animateColorAsState(target.midMountain, colorSpec, label = "sceneMidMtn")
    val midHighlight by animateColorAsState(target.midMountainHighlight, colorSpec, label = "sceneMidHi")
    val foothill by animateColorAsState(target.foothill, colorSpec, label = "sceneFoothill")
    val treeBack by animateColorAsState(target.treeBack, colorSpec, label = "sceneTreeBack")
    val treeFront by animateColorAsState(target.treeFront, colorSpec, label = "sceneTreeFront")
    val lakeTop by animateColorAsState(target.lakeTop, colorSpec, label = "sceneLakeTop")
    val lakeBottom by animateColorAsState(target.lakeBottom, colorSpec, label = "sceneLakeBottom")
    val streamColor by animateColorAsState(target.streamColor, colorSpec, label = "sceneStream")
    val waterShimmer by animateColorAsState(target.waterShimmer, colorSpec, label = "sceneShimmer")

    val nightAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.NIGHT) 1f else 0f,
        animationSpec = tween(durationMillis = ATMOSPHERE_TRANSITION_DURATION_MS),
        label = "sceneNightAlpha"
    )
    val dayAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.DAY) 1f else 0f,
        animationSpec = tween(durationMillis = ATMOSPHERE_TRANSITION_DURATION_MS),
        label = "sceneDayAlpha"
    )
    val dawnDuskAlpha by animateFloatAsState(
        targetValue = if (skyPhase == SkyPhase.DAWN_DUSK) 1f else 0f,
        animationSpec = tween(durationMillis = ATMOSPHERE_TRANSITION_DURATION_MS),
        label = "sceneDawnDuskAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "landscape_water_motion")
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waterRipplePhase"
    )
    val ambientProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "landscapeAmbientProgress"
    )

    val farMountainPath = remember { Path() }
    val midMountainPath = remember { Path() }
    val midHighlightLeftPath = remember { Path() }
    val midHighlightRightPath = remember { Path() }
    val leftShorePath = remember { Path() }
    val rightShorePath = remember { Path() }
    val streamPath = remember { Path() }
    val treePath = remember { Path() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val horizonY = h * 0.64f

            // 1. Subtle stars & occasional shooting star (night phase only)
            if (nightAlpha > 0.02f) {
                for (star in SCENE_STARS) {
                    val twinkle = 0.55f + 0.45f * sin(ripplePhase + star.phaseOffset)
                    drawCircle(
                        color = Color(0xFFE4E0D8).copy(alpha = (twinkle * nightAlpha).coerceIn(0f, 1f)),
                        radius = star.radiusDp.dp.toPx(),
                        center = Offset(w * star.xFraction, h * star.yFraction)
                    )
                }
                if (ambientProgress in 0.18f..0.34f) {
                    val streakT = (ambientProgress - 0.18f) / 0.16f
                    val fadeEnvelope = sin(streakT * Math.PI).toFloat().coerceIn(0f, 1f)
                    val headX = w * (0.22f + 0.34f * streakT)
                    val headY = h * (0.11f + 0.11f * streakT)
                    val tailX = headX - w * 0.085f
                    val tailY = headY - h * 0.032f
                    val meteorAlpha = (0.78f * fadeEnvelope * nightAlpha).coerceIn(0f, 1f)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFF4D6).copy(alpha = meteorAlpha)
                            ),
                            start = Offset(tailX, tailY),
                            end = Offset(headX, headY)
                        ),
                        start = Offset(tailX, tailY),
                        end = Offset(headX, headY),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFFFFFAEB).copy(alpha = meteorAlpha),
                        radius = 1.8.dp.toPx(),
                        center = Offset(headX, headY)
                    )
                }
            }

            // 1b. Softly drifting high-altitude cloud wisps (day phase)
            if (dayAlpha > 0.05f) {
                for (cIdx in 0..2) {
                    val baseOffset = (ambientProgress + cIdx * 0.33f) % 1.0f
                    val cloudCenterX = w * (0.12f + 0.76f * baseOffset)
                    val cloudCenterY = h * (0.18f + cIdx * 0.045f)
                    val cloudWidth = w * (0.18f - cIdx * 0.02f)
                    val cloudHeight = 7.dp.toPx()
                    val edgeAlpha = sin(baseOffset * Math.PI).toFloat().coerceIn(0f, 1f)
                    drawRoundRect(
                        color = waterShimmer.copy(alpha = (0.20f * dayAlpha * edgeAlpha).coerceIn(0f, 0.25f)),
                        topLeft = Offset(cloudCenterX - cloudWidth / 2f, cloudCenterY),
                        size = Size(cloudWidth, cloudHeight),
                        cornerRadius = CornerRadius(cloudHeight / 2f, cloudHeight / 2f)
                    )
                }
            }

            // 2. Soft celestial glow & sun/moon disc nestled above the central valley
            val celArcOffsetY = -0.04f * dayAlpha + 0.045f * dawnDuskAlpha
            val celCenter = Offset(w * 0.52f, h * (0.26f + celArcOffsetY))
            val celGlowRadius = minOf(w, h) * 0.40f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        celestialGlow.copy(alpha = 0.34f),
                        celestialGlow.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = celCenter,
                    radius = celGlowRadius
                ),
                radius = celGlowRadius,
                center = celCenter
            )
            drawCircle(
                color = celestialDisc.copy(alpha = 0.88f),
                radius = minOf(w, h) * 0.075f,
                center = celCenter
            )

            // 3. Far alpine mountain range
            farMountainPath.reset()
            farMountainPath.moveTo(0f, horizonY)
            farMountainPath.lineTo(0f, h * 0.46f)
            farMountainPath.lineTo(w * 0.18f, h * 0.29f)
            farMountainPath.lineTo(w * 0.35f, h * 0.44f)
            farMountainPath.lineTo(w * 0.50f, h * 0.28f)
            farMountainPath.lineTo(w * 0.67f, h * 0.44f)
            farMountainPath.lineTo(w * 0.84f, h * 0.26f)
            farMountainPath.lineTo(w, h * 0.42f)
            farMountainPath.lineTo(w, horizonY)
            farMountainPath.close()
            drawPath(path = farMountainPath, color = farMountain.copy(alpha = 0.78f))

            // 4. Mid mountain ridges framing the stream valley
            midMountainPath.reset()
            midMountainPath.moveTo(0f, horizonY)
            midMountainPath.lineTo(0f, h * 0.53f)
            midMountainPath.lineTo(w * 0.22f, h * 0.35f)
            midMountainPath.lineTo(w * 0.48f, h * 0.54f)
            midMountainPath.lineTo(w * 0.54f, h * 0.53f)
            midMountainPath.lineTo(w * 0.77f, h * 0.36f)
            midMountainPath.lineTo(w, h * 0.51f)
            midMountainPath.lineTo(w, horizonY)
            midMountainPath.close()
            drawPath(path = midMountainPath, color = midMountain.copy(alpha = 0.90f))

            // Ridge light facets
            midHighlightLeftPath.reset()
            midHighlightLeftPath.moveTo(w * 0.22f, h * 0.35f)
            midHighlightLeftPath.lineTo(w * 0.35f, h * 0.46f)
            midHighlightLeftPath.lineTo(w * 0.24f, h * 0.51f)
            midHighlightLeftPath.close()
            drawPath(path = midHighlightLeftPath, color = midHighlight.copy(alpha = 0.50f))

            midHighlightRightPath.reset()
            midHighlightRightPath.moveTo(w * 0.77f, h * 0.36f)
            midHighlightRightPath.lineTo(w * 0.65f, h * 0.47f)
            midHighlightRightPath.lineTo(w * 0.75f, h * 0.52f)
            midHighlightRightPath.close()
            drawPath(path = midHighlightRightPath, color = midHighlight.copy(alpha = 0.45f))

            // 5. Winding alpine stream descending into the lake
            streamPath.reset()
            streamPath.moveTo(w * 0.495f, h * 0.53f)
            streamPath.cubicTo(
                w * 0.54f, h * 0.57f,
                w * 0.44f, h * 0.60f,
                w * 0.53f, horizonY + h * 0.03f
            )
            streamPath.lineTo(w * 0.44f, horizonY + h * 0.03f)
            streamPath.cubicTo(
                w * 0.39f, h * 0.60f,
                w * 0.50f, h * 0.57f,
                w * 0.475f, h * 0.53f
            )
            streamPath.close()
            drawPath(path = streamPath, color = streamColor.copy(alpha = 0.90f))

            // 6. Lake surface gently fading out toward the bottom
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to lakeTop.copy(alpha = 0.85f),
                    0.65f to lakeBottom.copy(alpha = 0.55f),
                    1.0f to lakeBottom.copy(alpha = 0f),
                    startY = horizonY,
                    endY = h
                ),
                topLeft = Offset(0f, horizonY),
                size = Size(w, h - horizonY)
            )

            // 7. Left & right wooded shorelines curving toward the lake
            leftShorePath.reset()
            leftShorePath.moveTo(0f, horizonY - h * 0.04f)
            leftShorePath.quadraticTo(
                w * 0.24f, horizonY - h * 0.01f,
                w * 0.42f, horizonY + h * 0.05f
            )
            leftShorePath.quadraticTo(
                w * 0.20f, horizonY + h * 0.08f,
                0f, horizonY + h * 0.11f
            )
            leftShorePath.close()
            drawPath(path = leftShorePath, color = foothill.copy(alpha = 0.92f))

            rightShorePath.reset()
            rightShorePath.moveTo(w, horizonY - h * 0.05f)
            rightShorePath.quadraticTo(
                w * 0.74f, horizonY - h * 0.01f,
                w * 0.56f, horizonY + h * 0.05f
            )
            rightShorePath.quadraticTo(
                w * 0.78f, horizonY + h * 0.08f,
                w, horizonY + h * 0.11f
            )
            rightShorePath.close()
            drawPath(path = rightShorePath, color = foothill.copy(alpha = 0.92f))

            // 8. Pine forest silhouettes along both shores
            for (pine in SCENE_PINES) {
                drawPineTree(
                    treePath = treePath,
                    cx = w * pine.xFraction,
                    baseY = h * pine.baseYFraction,
                    treeHeight = h * pine.heightFraction,
                    color = (if (pine.isFrontLayer) treeFront else treeBack).copy(alpha = 0.92f)
                )
            }

            // 9. Animated water shimmer ripples across the lake
            val shimmerCenterX = w * 0.50f
            for (i in 0..4) {
                val rowY = h * (0.69f + i * 0.044f)
                val sway = sin(ripplePhase + i * 0.9f) * (w * 0.016f)
                val halfSpan = w * (0.13f - i * 0.015f).coerceAtLeast(0.03f)
                val shimmerAlpha = (0.55f - i * 0.08f).coerceIn(0.14f, 0.65f)
                drawLine(
                    color = waterShimmer.copy(alpha = shimmerAlpha),
                    start = Offset(shimmerCenterX - halfSpan + sway, rowY),
                    end = Offset(shimmerCenterX + halfSpan + sway, rowY),
                    strokeWidth = (1.8f - i * 0.18f).dp.toPx().coerceAtLeast(1.dp.toPx()),
                    cap = StrokeCap.Round
                )
            }

            // 9b. Gently drifting translucent valley mist during golden hour (DAWN_DUSK)
            if (dawnDuskAlpha > 0.05f) {
                for (mIdx in 0..1) {
                    val drift = sin(ripplePhase + mIdx * 2.1f) * (w * 0.04f)
                    val mistWidth = w * (0.48f - mIdx * 0.08f)
                    val mistHeight = 9.dp.toPx()
                    val mistY = horizonY - h * (0.025f - mIdx * 0.032f)
                    val mistX = w * 0.50f - mistWidth / 2f + drift
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.Transparent,
                            0.5f to celestialGlow.copy(alpha = 0.18f * dawnDuskAlpha),
                            1.0f to Color.Transparent,
                            startX = mistX,
                            endX = mistX + mistWidth
                        ),
                        topLeft = Offset(mistX, mistY),
                        size = Size(mistWidth, mistHeight),
                        cornerRadius = CornerRadius(mistHeight / 2f, mistHeight / 2f)
                    )
                }
            }

            // 10. Seamless edge feathering so the scene melts directly into the app background
            drawRect(
                brush = Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.14f to Color.Black,
                    0.78f to Color.Black,
                    1.00f to Color.Transparent,
                    startY = 0f,
                    endY = h
                ),
                blendMode = BlendMode.DstIn
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    0.00f to Color.Transparent,
                    0.10f to Color.Black,
                    0.90f to Color.Black,
                    1.00f to Color.Transparent,
                    startX = 0f,
                    endX = w
                ),
                blendMode = BlendMode.DstIn
            )
        }

        Text(
            text = hintText,
            color = palette.textSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 2.dp)
        )
    }
}

private fun DrawScope.drawPineTree(
    treePath: Path,
    cx: Float,
    baseY: Float,
    treeHeight: Float,
    color: Color
) {
    val halfW = treeHeight * 0.36f
    treePath.reset()
    treePath.moveTo(cx, baseY - treeHeight)
    treePath.lineTo(cx + halfW * 0.55f, baseY - treeHeight * 0.54f)
    treePath.lineTo(cx - halfW * 0.55f, baseY - treeHeight * 0.54f)
    treePath.close()

    treePath.moveTo(cx, baseY - treeHeight * 0.72f)
    treePath.lineTo(cx + halfW * 0.82f, baseY - treeHeight * 0.28f)
    treePath.lineTo(cx - halfW * 0.82f, baseY - treeHeight * 0.28f)
    treePath.close()

    treePath.moveTo(cx, baseY - treeHeight * 0.45f)
    treePath.lineTo(cx + halfW, baseY)
    treePath.lineTo(cx - halfW, baseY)
    treePath.close()
    drawPath(path = treePath, color = color)
}
