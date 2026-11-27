package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.R
import com.restcycle.app.domain.calculator.SleepCycleCalculator
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.EverforestDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.EverforestDayAtmospherePalette
import com.restcycle.app.ui.theme.EverforestNightAtmospherePalette
import com.restcycle.app.ui.theme.GruvboxDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.GruvboxDayAtmospherePalette
import com.restcycle.app.ui.theme.GruvboxNightAtmospherePalette
import com.restcycle.app.ui.theme.NordDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.NordDayAtmospherePalette
import com.restcycle.app.ui.theme.NordNightAtmospherePalette
import com.restcycle.app.ui.theme.RosePineDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.RosePineDayAtmospherePalette
import com.restcycle.app.ui.theme.RosePineNightAtmospherePalette
import com.restcycle.app.ui.theme.SolarizedDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.SolarizedDayAtmospherePalette
import com.restcycle.app.ui.theme.SolarizedNightAtmospherePalette
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.theme.TokyoNightDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.TokyoNightDayAtmospherePalette
import com.restcycle.app.ui.theme.TokyoNightNightAtmospherePalette
import com.restcycle.app.ui.util.formatClockTime
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ONBOARDING_PAGE_COUNT = 4

@Composable
fun OnboardingOverlay(
    sleepLatencyMinutes: Int,
    themeStyle: AppThemeStyle,
    pureAmoledNight: Boolean,
    caffeineCutoffHours: Int,
    bedtimeReminderEnabled: Boolean,
    is24HourFormat: Boolean,
    onUpdateSleepLatency: (Int) -> Unit,
    onSelectThemeStyle: (AppThemeStyle) -> Unit,
    onTogglePureAmoledNight: (Boolean) -> Unit,
    onSelectCaffeineCutoffHours: (Int) -> Unit,
    onToggleBedtimeReminder: (Boolean) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { ONBOARDING_PAGE_COUNT })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.backgroundTop,
                        palette.backgroundBottom
                    )
                )
            )
            .testTag(RestCycleTestTags.ONBOARDING_ROOT)
    ) {
        // Subtle ambient sky glow in upper viewport
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.backgroundGlow.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.16f),
                    radius = size.width * 0.78f
                ),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.5f, size.height * 0.16f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            // Top Bar: Step pill + Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = palette.pillContainer,
                    border = BorderStroke(1.dp, palette.borderSubtle)
                ) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_step_label,
                            pagerState.currentPage + 1,
                            ONBOARDING_PAGE_COUNT
                        ),
                        style = TabularTextStyles.headerLiveClock,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onFinish()
                        }
                        .testTag(RestCycleTestTags.ONBOARDING_SKIP_BTN)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> OnboardingStepCycles()
                        1 -> OnboardingStepLatency(
                            sleepLatencyMinutes = sleepLatencyMinutes,
                            is24HourFormat = is24HourFormat,
                            onUpdateSleepLatency = onUpdateSleepLatency
                        )
                        2 -> OnboardingStepThemes(
                            themeStyle = themeStyle,
                            pureAmoledNight = pureAmoledNight,
                            onSelectThemeStyle = onSelectThemeStyle,
                            onTogglePureAmoledNight = onTogglePureAmoledNight
                        )
                        3 -> OnboardingStepHabits(
                            caffeineCutoffHours = caffeineCutoffHours,
                            bedtimeReminderEnabled = bedtimeReminderEnabled,
                            onSelectCaffeineCutoffHours = onSelectCaffeineCutoffHours,
                            onToggleBedtimeReminder = onToggleBedtimeReminder
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Pill-Dot Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(ONBOARDING_PAGE_COUNT) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 26.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "onboarding_dot_width_$index"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) palette.accent else palette.borderSubtle.copy(alpha = 0.85f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Navigation Dock
            val isFirstPage = pagerState.currentPage == 0
            val isLastPage = pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = !isFirstPage,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150))
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    val backScale = rememberPressScale(backInteraction, pressedScale = 0.95f)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.pillContainer,
                        border = BorderStroke(1.dp, palette.borderSubtle),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = backScale
                                scaleY = backScale
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = backInteraction,
                                indication = null,
                                role = Role.Button
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                scope.launch {
                                    pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                }
                            }
                            .testTag(RestCycleTestTags.ONBOARDING_BACK_BTN)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp)
                        )
                    }
                }

                val ctaInteraction = remember { MutableInteractionSource() }
                val ctaScale = rememberPressScale(ctaInteraction, pressedScale = 0.96f)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.accent,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = ctaScale
                            scaleY = ctaScale
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = ctaInteraction,
                            indication = null,
                            role = Role.Button
                        ) {
                            if (isLastPage) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onFinish()
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1).coerceAtMost(ONBOARDING_PAGE_COUNT - 1)
                                    )
                                }
                            }
                        }
                        .testTag(
                            if (isLastPage) RestCycleTestTags.ONBOARDING_FINISH_BTN
                            else RestCycleTestTags.ONBOARDING_NEXT_BTN
                        )
                ) {
                    Text(
                        text = stringResource(
                            if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
                        ),
                        color = palette.onAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepCycles() {
    val palette = AtmosphereTheme.palette
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_wave")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Animated 90-minute Sleep Cycle Wave Hero
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.cardSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 22.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h * 0.56f
                val amp = h * 0.34f
                val cycles = 3.5f

                val wavePath = Path()
                val fillPath = Path()
                val steps = 80
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val x = t * w
                    val y = midY - sin(t * cycles * 2f * PI.toFloat() + phaseShift * 0.25f) * amp
                    if (i == 0) {
                        wavePath.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        wavePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(w, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    path = wavePath,
                    color = palette.accent,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight 3 optimal wake-up peaks
                listOf(0.22f, 0.50f, 0.78f).forEach { peakT ->
                    val px = peakT * w
                    val py = midY - sin(peakT * cycles * 2f * PI.toFloat() + phaseShift * 0.25f) * amp
                    drawCircle(
                        color = palette.accent.copy(alpha = 0.28f),
                        radius = 10.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = palette.accent,
                        radius = 4.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Icon(
                    imageVector = RestCycleIcons.Moon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "90 min REM",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(26.dp))

    Text(
        text = stringResource(R.string.onboarding_step1_title),
        color = palette.textPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.onboarding_step1_body),
        color = palette.textSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(22.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OnboardingFeatureRow(
            icon = RestCycleIcons.Alarm,
            text = stringResource(R.string.onboarding_step1_pill_modes)
        )
        OnboardingFeatureRow(
            icon = RestCycleIcons.AudioWaveform,
            text = stringResource(R.string.onboarding_step1_pill_sounds)
        )
        OnboardingFeatureRow(
            icon = RestCycleIcons.Calendar,
            text = stringResource(R.string.onboarding_step1_pill_journal)
        )
    }
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, text: String) {
    val palette = AtmosphereTheme.palette
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.cardSurface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OnboardingStepLatency(
    sleepLatencyMinutes: Int,
    is24HourFormat: Boolean,
    onUpdateSleepLatency: (Int) -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val presets = listOf(5, 10, 15, 20, 30)

    // Hero badge with live odometer readout
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.cardSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 16.dp)
        ) {
            Icon(
                imageVector = RestCycleIcons.Moon,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            RollingOdometerText(
                text = stringResource(R.string.latency_minutes_value, sleepLatencyMinutes),
                color = palette.textPrimary,
                style = TabularTextStyles.headerLiveClock.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = stringResource(R.string.onboarding_step2_title),
        color = palette.textPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_step2_body),
        color = palette.textSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Quick preset chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            val selected = sleepLatencyMinutes == preset
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) palette.accent else palette.pillContainer,
                border = BorderStroke(1.dp, if (selected) palette.accent else palette.borderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.RadioButton) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onUpdateSleepLatency(preset)
                    }
            ) {
                Text(
                    text = "${preset}m",
                    style = TabularTextStyles.headerLiveClock,
                    color = if (selected) palette.onAccent else palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Slider(
        value = sleepLatencyMinutes.toFloat(),
        onValueChange = { raw ->
            val rounded = raw.roundToInt().coerceIn(
                SleepCycleCalculator.MIN_SLEEP_LATENCY_MINUTES,
                SleepCycleCalculator.MAX_SLEEP_LATENCY_MINUTES
            )
            if (rounded != sleepLatencyMinutes) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onUpdateSleepLatency(rounded)
            }
        },
        valueRange = SleepCycleCalculator.MIN_SLEEP_LATENCY_MINUTES.toFloat()..SleepCycleCalculator.MAX_SLEEP_LATENCY_MINUTES.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = palette.accent,
            activeTrackColor = palette.accent,
            inactiveTrackColor = palette.borderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Live calculation preview pill
    val sampleWake = LocalTime.of(7, 0)
    val sampleBed = sampleWake.minusMinutes((5L * 90L) + sleepLatencyMinutes.toLong())
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = RestCycleIcons.Sparkles,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(
                    R.string.onboarding_step2_preview,
                    formatClockTime(sampleWake, is24HourFormat),
                    formatClockTime(sampleBed, is24HourFormat)
                ),
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OnboardingStepThemes(
    themeStyle: AppThemeStyle,
    pureAmoledNight: Boolean,
    onSelectThemeStyle: (AppThemeStyle) -> Unit,
    onTogglePureAmoledNight: (Boolean) -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current

    Text(
        text = stringResource(R.string.onboarding_step3_title),
        color = palette.textPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_step3_body),
        color = palette.textSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(22.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ONBOARDING_THEMES.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (style, labelRes, dots) ->
                    val selected = themeStyle == style
                    val interaction = remember { MutableInteractionSource() }
                    val pressScale = rememberPressScale(interaction, pressedScale = 0.96f)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) palette.cardSurfaceHighlighted else palette.cardSurface.copy(alpha = 0.85f),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) palette.accent else palette.borderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.RadioButton
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelectThemeStyle(style)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                dots.forEach { dotColor ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Pure AMOLED Black toggle card
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pureAmoledNight) palette.accent.copy(alpha = 0.16f) else palette.cardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            width = 1.dp,
            color = if (pureAmoledNight) palette.accent else palette.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Switch) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onTogglePureAmoledNight(!pureAmoledNight)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = RestCycleIcons.Contrast,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.settings_amoled_night),
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Switch(
                checked = pureAmoledNight,
                onCheckedChange = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onTogglePureAmoledNight(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.onAccent,
                    checkedTrackColor = palette.accent
                )
            )
        }
    }
}

@Composable
private fun OnboardingStepHabits(
    caffeineCutoffHours: Int,
    bedtimeReminderEnabled: Boolean,
    onSelectCaffeineCutoffHours: (Int) -> Unit,
    onToggleBedtimeReminder: (Boolean) -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_habits")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "habits_glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = glowPulse
                scaleY = glowPulse
            }
            .clip(CircleShape)
            .background(palette.accent.copy(alpha = 0.16f))
    ) {
        Icon(
            imageVector = RestCycleIcons.Bell,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(34.dp)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.onboarding_step4_title),
        color = palette.textPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_step4_body),
        color = palette.textSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(22.dp))

    // Caffeine Cutoff Selector Card
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.cardSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = RestCycleIcons.Coffee,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.settings_caffeine_label),
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to stringResource(R.string.settings_caffeine_off),
                    6 to stringResource(R.string.settings_caffeine_hours, 6),
                    8 to stringResource(R.string.settings_caffeine_hours, 8),
                    10 to stringResource(R.string.settings_caffeine_hours, 10)
                ).forEach { (hours, label) ->
                    val selected = caffeineCutoffHours == hours
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.accent else palette.pillContainer,
                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelectCaffeineCutoffHours(hours)
                            }
                    ) {
                        Text(
                            text = label,
                            color = if (selected) palette.onAccent else palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Bedtime Reminder Card
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (bedtimeReminderEnabled) palette.accent.copy(alpha = 0.16f) else palette.cardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            width = 1.dp,
            color = if (bedtimeReminderEnabled) palette.accent else palette.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Switch) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onToggleBedtimeReminder(!bedtimeReminderEnabled)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = RestCycleIcons.Bell,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_reminder_toggle),
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.onboarding_step4_reminder_desc),
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = bedtimeReminderEnabled,
                onCheckedChange = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onToggleBedtimeReminder(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.onAccent,
                    checkedTrackColor = palette.accent
                )
            )
        }
    }
}

private val ONBOARDING_THEMES = listOf(
    Triple(
        AppThemeStyle.TOKYO_NIGHT,
        R.string.settings_theme_tokyo_night,
        listOf(TokyoNightDayAtmospherePalette.accent, TokyoNightDawnDuskAtmospherePalette.backgroundGlow, TokyoNightNightAtmospherePalette.accent)
    ),
    Triple(
        AppThemeStyle.NORD,
        R.string.settings_theme_nord,
        listOf(NordDayAtmospherePalette.accent, NordDawnDuskAtmospherePalette.backgroundGlow, NordNightAtmospherePalette.accent)
    ),
    Triple(
        AppThemeStyle.EVERFOREST,
        R.string.settings_theme_everforest,
        listOf(EverforestDayAtmospherePalette.accent, EverforestDawnDuskAtmospherePalette.backgroundGlow, EverforestNightAtmospherePalette.accent)
    ),
    Triple(
        AppThemeStyle.GRUVBOX,
        R.string.settings_theme_gruvbox,
        listOf(GruvboxDayAtmospherePalette.accent, GruvboxDawnDuskAtmospherePalette.backgroundGlow, GruvboxNightAtmospherePalette.accent)
    ),
    Triple(
        AppThemeStyle.SOLARIZED,
        R.string.settings_theme_solarized,
        listOf(SolarizedDayAtmospherePalette.backgroundGlow, SolarizedDawnDuskAtmospherePalette.backgroundGlow, SolarizedNightAtmospherePalette.accent)
    ),
    Triple(
        AppThemeStyle.ROSE_PINE,
        R.string.settings_theme_rose_pine,
        listOf(RosePineDayAtmospherePalette.accent, RosePineDawnDuskAtmospherePalette.backgroundGlow, RosePineNightAtmospherePalette.accent)
    )
)
