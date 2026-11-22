package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.R
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.ui.RestCycleTestTags
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

private val EverforestDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFDFA000),
    celestialDisc = Color(0xFFF57D26),
    farMountain = Color(0xFF939F91),
    midMountain = Color(0xFF7FBBB3),
    midMountainHighlight = Color(0xFFA7C080),
    foothill = Color(0xFF5D7A58),
    treeBack = Color(0xFF486446),
    treeFront = Color(0xFF354F38),
    lakeTop = Color(0xFF7FBBB3),
    lakeBottom = Color(0xFF3A94C5),
    streamColor = Color(0xFF83C092),
    waterShimmer = Color(0xFFFDF6E3)
)

private val EverforestDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFE69875),
    celestialDisc = Color(0xFFDBBC7F),
    farMountain = Color(0xFF4F5B58),
    midMountain = Color(0xFF3D484D),
    midMountainHighlight = Color(0xFFE69875),
    foothill = Color(0xFF2D353B),
    treeBack = Color(0xFF262D32),
    treeFront = Color(0xFF1E2326),
    lakeTop = Color(0xFF51706D),
    lakeBottom = Color(0xFF272E33),
    streamColor = Color(0xFF83C092),
    waterShimmer = Color(0xFFDBBC7F)
)

private val EverforestNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFA7C080),
    celestialDisc = Color(0xFFD3C6AA),
    farMountain = Color(0xFF323C41),
    midMountain = Color(0xFF272E33),
    midMountainHighlight = Color(0xFF5C6A72),
    foothill = Color(0xFF1E2326),
    treeBack = Color(0xFF191E21),
    treeFront = Color(0xFF131719),
    lakeTop = Color(0xFF384B55),
    lakeBottom = Color(0xFF161A1D),
    streamColor = Color(0xFF7FBBB3),
    waterShimmer = Color(0xFFA7C080)
)

private val TokyoNightDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFF2E7DE9),
    celestialDisc = Color(0xFF8C6C3E),
    farMountain = Color(0xFF9699A3),
    midMountain = Color(0xFF6172B0),
    midMountainHighlight = Color(0xFF7AA2F7),
    foothill = Color(0xFF485E30),
    treeBack = Color(0xFF3A4C28),
    treeFront = Color(0xFF2A391E),
    lakeTop = Color(0xFF5A8DEE),
    lakeBottom = Color(0xFF3D59A1),
    streamColor = Color(0xFF7DCFFF),
    waterShimmer = Color(0xFFE6E7ED)
)

private val TokyoNightDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFFF9E64),
    celestialDisc = Color(0xFFE0AF68),
    farMountain = Color(0xFF414868),
    midMountain = Color(0xFF292E42),
    midMountainHighlight = Color(0xFFFF9E64),
    foothill = Color(0xFF1F2335),
    treeBack = Color(0xFF1A1B26),
    treeFront = Color(0xFF13141C),
    lakeTop = Color(0xFF3D59A1),
    lakeBottom = Color(0xFF1A1B26),
    streamColor = Color(0xFF7DCFFF),
    waterShimmer = Color(0xFFFF9E64)
)

private val TokyoNightNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFF7AA2F7),
    celestialDisc = Color(0xFFC0CAF5),
    farMountain = Color(0xFF292E42),
    midMountain = Color(0xFF1F2335),
    midMountainHighlight = Color(0xFF7AA2F7),
    foothill = Color(0xFF16161E),
    treeBack = Color(0xFF13141C),
    treeFront = Color(0xFF0D0E14),
    lakeTop = Color(0xFF283457),
    lakeBottom = Color(0xFF101118),
    streamColor = Color(0xFF7AA2F7),
    waterShimmer = Color(0xFF7DCFFF)
)

private val GruvboxDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFD65D0E),
    celestialDisc = Color(0xFFD79921),
    farMountain = Color(0xFFA89984),
    midMountain = Color(0xFF7C6F64),
    midMountainHighlight = Color(0xFFD79921),
    foothill = Color(0xFF79740E),
    treeBack = Color(0xFF625E0B),
    treeFront = Color(0xFF4B4808),
    lakeTop = Color(0xFF458588),
    lakeBottom = Color(0xFF076678),
    streamColor = Color(0xFF689D6A),
    waterShimmer = Color(0xFFFBF1C7)
)

private val GruvboxDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFFE8019),
    celestialDisc = Color(0xFFFABD2F),
    farMountain = Color(0xFF665C54),
    midMountain = Color(0xFF504945),
    midMountainHighlight = Color(0xFFFE8019),
    foothill = Color(0xFF3C3836),
    treeBack = Color(0xFF282828),
    treeFront = Color(0xFF1D2021),
    lakeTop = Color(0xFF83A598),
    lakeBottom = Color(0xFF282828),
    streamColor = Color(0xFFFE8019),
    waterShimmer = Color(0xFFFABD2F)
)

private val GruvboxNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFB8BB26),
    celestialDisc = Color(0xFFEBDBB2),
    farMountain = Color(0xFF504945),
    midMountain = Color(0xFF3C3836),
    midMountainHighlight = Color(0xFF83A598),
    foothill = Color(0xFF282828),
    treeBack = Color(0xFF222222),
    treeFront = Color(0xFF181A1B),
    lakeTop = Color(0xFF458588),
    lakeBottom = Color(0xFF181A1B),
    streamColor = Color(0xFF8EC07C),
    waterShimmer = Color(0xFFB8BB26)
)

private val DraculaDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFF9580FF),
    celestialDisc = Color(0xFFFFB86C),
    farMountain = Color(0xFF8C86B8),
    midMountain = Color(0xFF6272A4),
    midMountainHighlight = Color(0xFFBD93F9),
    foothill = Color(0xFF436B63),
    treeBack = Color(0xFF345650),
    treeFront = Color(0xFF25413D),
    lakeTop = Color(0xFF8BE9FD),
    lakeBottom = Color(0xFF6272A4),
    streamColor = Color(0xFF8BE9FD),
    waterShimmer = Color(0xFFF8F8F2)
)

private val DraculaDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFFF79C6),
    celestialDisc = Color(0xFFFFB86C),
    farMountain = Color(0xFF564B75),
    midMountain = Color(0xFF44475A),
    midMountainHighlight = Color(0xFFFF79C6),
    foothill = Color(0xFF282A36),
    treeBack = Color(0xFF232530),
    treeFront = Color(0xFF191A24),
    lakeTop = Color(0xFF6272A4),
    lakeBottom = Color(0xFF282A36),
    streamColor = Color(0xFFBD93F9),
    waterShimmer = Color(0xFFFFB86C)
)

private val DraculaNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFBD93F9),
    celestialDisc = Color(0xFFF8F8F2),
    farMountain = Color(0xFF383A4A),
    midMountain = Color(0xFF282A36),
    midMountainHighlight = Color(0xFF6272A4),
    foothill = Color(0xFF1E1F29),
    treeBack = Color(0xFF191A22),
    treeFront = Color(0xFF12131A),
    lakeTop = Color(0xFF44475A),
    lakeBottom = Color(0xFF12131A),
    streamColor = Color(0xFF8BE9FD),
    waterShimmer = Color(0xFFBD93F9)
)

private val RosePineDayLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFEA9D34),
    celestialDisc = Color(0xFFD7827E),
    farMountain = Color(0xFFB4A8BA),
    midMountain = Color(0xFF907AA9),
    midMountainHighlight = Color(0xFFD7827E),
    foothill = Color(0xFF56949F),
    treeBack = Color(0xFF3E7884),
    treeFront = Color(0xFF286983),
    lakeTop = Color(0xFF9CCFD8),
    lakeBottom = Color(0xFF56949F),
    streamColor = Color(0xFF9CCFD8),
    waterShimmer = Color(0xFFFFFAF3)
)

private val RosePineDawnDuskLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFEA9A97),
    celestialDisc = Color(0xFFF6C177),
    farMountain = Color(0xFF524F67),
    midMountain = Color(0xFF393552),
    midMountainHighlight = Color(0xFFEA9A97),
    foothill = Color(0xFF2A273F),
    treeBack = Color(0xFF232136),
    treeFront = Color(0xFF191724),
    lakeTop = Color(0xFF3E8FB0),
    lakeBottom = Color(0xFF232136),
    streamColor = Color(0xFF9CCFD8),
    waterShimmer = Color(0xFFF6C177)
)

private val RosePineNightLandscapeColors = LandscapeColors(
    celestialGlow = Color(0xFFEBBCBA),
    celestialDisc = Color(0xFFE0DEF4),
    farMountain = Color(0xFF393552),
    midMountain = Color(0xFF26233A),
    midMountainHighlight = Color(0xFFC4A7E7),
    foothill = Color(0xFF1F1D2E),
    treeBack = Color(0xFF191724),
    treeFront = Color(0xFF100E18),
    lakeTop = Color(0xFF31748F),
    lakeBottom = Color(0xFF100E18),
    streamColor = Color(0xFF9CCFD8),
    waterShimmer = Color(0xFFEBBCBA)
)

private fun landscapeColorsFor(
    skyPhase: SkyPhase,
    themeStyle: AppThemeStyle
): LandscapeColors = when (themeStyle) {
    AppThemeStyle.TOKYO_NIGHT -> when (skyPhase) {
        SkyPhase.DAY -> TokyoNightDayLandscapeColors
        SkyPhase.DAWN_DUSK -> TokyoNightDawnDuskLandscapeColors
        SkyPhase.NIGHT -> TokyoNightNightLandscapeColors
    }
    AppThemeStyle.NORD -> when (skyPhase) {
        SkyPhase.DAY -> NordDayLandscapeColors
        SkyPhase.DAWN_DUSK -> NordDawnDuskLandscapeColors
        SkyPhase.NIGHT -> NordNightLandscapeColors
    }
    AppThemeStyle.EVERFOREST -> when (skyPhase) {
        SkyPhase.DAY -> EverforestDayLandscapeColors
        SkyPhase.DAWN_DUSK -> EverforestDawnDuskLandscapeColors
        SkyPhase.NIGHT -> EverforestNightLandscapeColors
    }
    AppThemeStyle.GRUVBOX -> when (skyPhase) {
        SkyPhase.DAY -> GruvboxDayLandscapeColors
        SkyPhase.DAWN_DUSK -> GruvboxDawnDuskLandscapeColors
        SkyPhase.NIGHT -> GruvboxNightLandscapeColors
    }
    AppThemeStyle.DRACULA -> when (skyPhase) {
        SkyPhase.DAY -> DraculaDayLandscapeColors
        SkyPhase.DAWN_DUSK -> DraculaDawnDuskLandscapeColors
        SkyPhase.NIGHT -> DraculaNightLandscapeColors
    }
    AppThemeStyle.ROSE_PINE -> when (skyPhase) {
        SkyPhase.DAY -> RosePineDayLandscapeColors
        SkyPhase.DAWN_DUSK -> RosePineDawnDuskLandscapeColors
        SkyPhase.NIGHT -> RosePineNightLandscapeColors
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
    modifier: Modifier = Modifier,
    onOpenBreathing: (() -> Unit)? = null,
    ambientSoundMode: AmbientSoundMode = AmbientSoundMode.OFF,
    onCycleAmbientSound: (() -> Unit)? = null
) {
    val palette = AtmosphereTheme.palette
    val themeStyle = AtmosphereTheme.themeStyle
    val view = LocalView.current
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

        if (onCycleAmbientSound != null) {
            val soundActive = ambientSoundMode != AmbientSoundMode.OFF
            val soundLabel = stringResource(
                if (soundActive) ambientSoundMode.labelResId else R.string.ambient_sound_button
            )
            val soundIcon = if (soundActive) {
                RestCycleIcons.forAmbientSound(ambientSoundMode)
            } else {
                RestCycleIcons.AudioWaveform
            }
            val soundTint = if (soundActive) palette.accentText else palette.textPrimary
            val shape = RoundedCornerShape(50)
            Surface(
                shape = shape,
                color = if (soundActive) {
                    palette.accent.copy(alpha = 0.24f)
                } else {
                    palette.pillContainer.copy(alpha = 0.85f)
                },
                border = BorderStroke(1.dp, if (soundActive) palette.accent else palette.borderSubtle),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 4.dp)
                    .clip(shape)
                    .testTag(RestCycleTestTags.AMBIENT_SOUND_BUTTON)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onCycleAmbientSound()
                    }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = soundIcon,
                        contentDescription = null,
                        tint = soundTint,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = soundLabel,
                        color = soundTint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        if (onOpenBreathing != null) {
            val shape = RoundedCornerShape(50)
            Surface(
                shape = shape,
                color = palette.pillContainer.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, palette.borderSubtle),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .clip(shape)
                    .testTag(RestCycleTestTags.BREATHE_BUTTON)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onOpenBreathing()
                    }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = RestCycleIcons.Wind,
                        contentDescription = null,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.action_breathe),
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
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
