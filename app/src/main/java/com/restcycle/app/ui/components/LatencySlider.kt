package com.restcycle.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.R
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import kotlin.math.roundToInt

@Composable
fun LatencySlider(
    labelResId: Int,
    minutes: Int,
    maxMinutes: Int,
    onLatencyChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val clamped = minutes.coerceIn(0, maxMinutes)
    val animatedSliderValue by animateFloatAsState(
        targetValue = clamped.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
        label = "latencyThumb"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(RestCycleTestTags.LATENCY_ADJUSTER)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = labelResId),
                color = palette.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(id = R.string.latency_minutes_value, clamped),
                style = TabularTextStyles.sliderValue,
                color = palette.accentText,
                modifier = Modifier.testTag(RestCycleTestTags.LATENCY_VALUE_TEXT)
            )
        }

        Slider(
            value = animatedSliderValue,
            onValueChange = { raw ->
                val rounded = when {
                    raw <= 1.25f -> 0
                    raw >= maxMinutes - 1.25f -> maxMinutes
                    else -> raw.roundToInt().coerceIn(0, maxMinutes)
                }
                if (rounded != clamped) {
                    onLatencyChange(rounded)
                }
            },
            valueRange = 0f..maxMinutes.toFloat(),
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.borderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .testTag(RestCycleTestTags.LATENCY_SLIDER)
                .semantics {
                    setProgress { target ->
                        val exact = target.roundToInt().coerceIn(0, maxMinutes)
                        onLatencyChange(exact)
                        true
                    }
                }
        )
    }
}
