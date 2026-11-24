package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
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
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme

@Composable
fun ModeSegmentedBar(
    selectedMode: SleepMode,
    onModeSelected: (SleepMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val fontScale = LocalDensity.current.fontScale
    val tabFontSize = if (fontScale > 1.10f) {
        (10.5f * (1.10f / fontScale).coerceAtLeast(0.72f)).sp
    } else {
        10.5.sp
    }
    val tabLineHeight = (tabFontSize.value * 1.22f).sp

    val animatedTabIndex by animateFloatAsState(
        targetValue = selectedMode.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 420f),
        label = "modePillSlide"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(RestCycleTestTags.MODE_SWITCHER),
        shape = RoundedCornerShape(24.dp),
        color = palette.pillContainer,
        border = BorderStroke(1.dp, palette.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup()
                .drawBehind {
                    val tabCount = SleepMode.entries.size
                    val tabWidth = size.width / tabCount
                    drawRoundRect(
                        color = palette.accent,
                        topLeft = Offset(x = tabWidth * animatedTabIndex, y = 0f),
                        size = Size(width = tabWidth, height = size.height),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SleepMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val label = stringResource(id = mode.labelResId())
                val selectedFraction by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "modeTabSelected"
                )
                val tabTextColor = lerp(palette.textSecondary, palette.onAccent, selectedFraction)
                val tabScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.95f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
                    label = "modeTabScale"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .testTag(RestCycleTestTags.modeTab(mode))
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = {
                                if (!isSelected) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onModeSelected(mode)
                                }
                            }
                        )
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = tabTextColor,
                        fontSize = tabFontSize,
                        lineHeight = tabLineHeight,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = tabScale
                                scaleY = tabScale
                            }
                    )
                }
            }
        }
    }
}

private fun SleepMode.labelResId(): Int = when (this) {
    SleepMode.WAKE_UP_AT -> R.string.mode_wake_up_at
    SleepMode.SLEEP_NOW -> R.string.mode_sleep_now
    SleepMode.GO_TO_BED_AT -> R.string.mode_go_to_bed_at
    SleepMode.POWER_NAP -> R.string.mode_power_nap
}
