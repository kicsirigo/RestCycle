package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restcycle.app.R
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.platform.audio.AmbientSoundPlaybackState
import com.restcycle.app.platform.audio.AmbientSoundService
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import kotlin.math.roundToInt

@Composable
fun AmbientSoundDialog(
    playbackState: AmbientSoundPlaybackState,
    onSelectSound: (AmbientSoundMode) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectTimerMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val activeModes = AmbientSoundMode.entries.filter { it != AmbientSoundMode.OFF }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.borderSubtle),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag(RestCycleTestTags.AMBIENT_SOUND_DIALOG)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎵 ${stringResource(R.string.sound_dialog_title)}",
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.accentText),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sound_close),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.65f))

                // Sound selection grid (2 columns x 3 rows) + Off pill
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeModes.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { mode ->
                                SoundModeOptionTile(
                                    mode = mode,
                                    selected = playbackState.mode == mode,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onSelectSound(mode)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    SoundModeOptionTile(
                        mode = AmbientSoundMode.OFF,
                        selected = playbackState.mode == AmbientSoundMode.OFF,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelectSound(AmbientSoundMode.OFF)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Volume Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sound_volume_label),
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(playbackState.volume * 100f).roundToInt()}%",
                            style = TabularTextStyles.cardDuration,
                            color = palette.accentText
                        )
                    }
                    Slider(
                        value = playbackState.volume,
                        onValueChange = onVolumeChange,
                        valueRange = AmbientSoundService.MIN_VOLUME..AmbientSoundService.MAX_VOLUME,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(RestCycleTestTags.SOUND_VOLUME_SLIDER)
                    )
                }

                // Sleep Timer Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sound_timer_label),
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (playbackState.isPlaying && playbackState.remainingSeconds > 0L) {
                            Text(
                                text = stringResource(
                                    R.string.sound_timer_remaining,
                                    AmbientSoundService.formatRemainingTime(playbackState.remainingSeconds)
                                ),
                                style = TabularTextStyles.cardDuration,
                                color = palette.accentText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AmbientSoundService.SLEEP_TIMER_OPTIONS.forEach { minutes ->
                            val selected = playbackState.sleepTimerMinutes == minutes
                            val label = if (minutes == 0) {
                                stringResource(R.string.sound_timer_endless)
                            } else {
                                stringResource(R.string.sound_timer_minutes, minutes)
                            }
                            val shape = RoundedCornerShape(10.dp)
                            Surface(
                                shape = shape,
                                color = if (selected) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) palette.accent else palette.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shape)
                                    .clickable(role = Role.RadioButton) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onSelectTimerMinutes(minutes)
                                    }
                                    .testTag(RestCycleTestTags.soundTimerOption(minutes))
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) palette.accentText else palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "🌙 ${stringResource(R.string.sound_background_hint)}",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SoundModeOptionTile(
    mode: AmbientSoundMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (selected) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.accent else palette.borderSubtle
        ),
        modifier = modifier
            .clip(shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(RestCycleTestTags.ambientSoundOption(mode.name))
    ) {
        Text(
            text = "${mode.emoji} ${stringResource(mode.labelResId)}",
            color = if (selected) palette.accentText else palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}
