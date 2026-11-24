package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.restcycle.app.platform.audio.AmbientSoundMode
import com.restcycle.app.platform.audio.AmbientSoundPackManager
import com.restcycle.app.platform.audio.AmbientSoundPlaybackState
import com.restcycle.app.platform.audio.AmbientSoundService
import com.restcycle.app.platform.audio.SoundMixPreset
import com.restcycle.app.platform.audio.SoundPackStatus
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientSoundDialog(
    playbackState: AmbientSoundPlaybackState,
    onSelectSound: (AmbientSoundMode) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectTimerMinutes: (Int) -> Unit,
    onDismiss: () -> Unit,
    packStatuses: Map<AmbientSoundMode, SoundPackStatus> = emptyMap(),
    onDownloadPack: (AmbientSoundMode) -> Unit = {},
    onCancelDownload: (AmbientSoundMode) -> Unit = {},
    onDeletePack: (AmbientSoundMode) -> Unit = {},
    onLayerVolumeChange: (AmbientSoundMode, Float) -> Unit = { _, _ -> },
    savedMixes: List<SoundMixPreset> = emptyList(),
    onApplyMix: (Map<AmbientSoundMode, Float>) -> Unit = {},
    onSaveCurrentMix: (String, Map<AmbientSoundMode, Float>) -> Unit = { _, _ -> },
    onDeleteSavedMix: (String) -> Unit = {}
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val activeModes = AmbientSoundMode.entries.filter { it != AmbientSoundMode.OFF }
    val activeLayers = playbackState.activeLayers.ifEmpty {
        if (playbackState.mode != AmbientSoundMode.OFF) mapOf(playbackState.mode to 0.80f) else emptyMap()
    }
    val totalDownloadedBytes = AmbientSoundPackManager.totalDownloadedBytes(packStatuses)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cozyStormLabel = stringResource(R.string.mixer_preset_cozy_storm)
    val forestNightLabel = stringResource(R.string.mixer_preset_forest_night)
    val builtInMixes = listOf(
        SoundMixPreset(
            id = "builtin_cozy_storm",
            name = cozyStormLabel,
            layers = mapOf(
                AmbientSoundMode.STREAM_RAIN to 0.78f,
                AmbientSoundMode.CAMPFIRE to 0.45f
            )
        ),
        SoundMixPreset(
            id = "builtin_forest_night",
            name = forestNightLabel,
            layers = mapOf(
                AmbientSoundMode.NIGHT_CRICKETS to 0.75f,
                AmbientSoundMode.AMBIENT_PAD to 0.40f
            )
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.cardSurface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = palette.borderSubtle,
                width = 36.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
                .testTag(RestCycleTestTags.AMBIENT_SOUND_DIALOG),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = RestCycleIcons.AudioWaveform,
                            contentDescription = null,
                            tint = palette.accentText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.sound_dialog_title),
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeLayers.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.accent.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = stringResource(R.string.mixer_active_count, activeLayers.size),
                                    color = palette.accentText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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

                // Saved & Curated Mixes Strip
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.mixer_presets_label),
                        color = palette.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeLayers.size >= 2) {
                            val saveLabel = activeLayers.keys.joinToString(" + ") {
                                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }.take(8)
                            }
                            val saveShape = RoundedCornerShape(50)
                            Surface(
                                shape = saveShape,
                                color = palette.accent.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, palette.accent),
                                modifier = Modifier
                                    .clip(saveShape)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onSaveCurrentMix(saveLabel, activeLayers)
                                    }
                            ) {
                                Text(
                                    text = stringResource(R.string.mixer_save_current),
                                    color = palette.accentText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        savedMixes.forEach { preset ->
                            val isActiveMix = activeLayers.keys == preset.layers.keys
                            val chipShape = RoundedCornerShape(50)
                            Surface(
                                shape = chipShape,
                                color = if (isActiveMix) palette.accent.copy(alpha = 0.20f) else palette.pillContainer,
                                border = BorderStroke(1.dp, if (isActiveMix) palette.accent else palette.borderSubtle),
                                modifier = Modifier
                                    .clip(chipShape)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onApplyMix(preset.layers)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = if (isActiveMix) palette.accentText else palette.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = RestCycleIcons.Close,
                                        contentDescription = null,
                                        tint = palette.textSecondary,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onDeleteSavedMix(preset.id)
                                            }
                                    )
                                }
                            }
                        }
                        builtInMixes.forEach { preset ->
                            val isActiveMix = activeLayers.keys == preset.layers.keys
                            val chipShape = RoundedCornerShape(50)
                            Surface(
                                shape = chipShape,
                                color = if (isActiveMix) palette.accent.copy(alpha = 0.20f) else palette.pillContainer,
                                border = BorderStroke(1.dp, if (isActiveMix) palette.accent else palette.borderSubtle),
                                modifier = Modifier
                                    .clip(chipShape)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onApplyMix(preset.layers)
                                    }
                            ) {
                                Text(
                                    text = preset.name,
                                    color = if (isActiveMix) palette.accentText else palette.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Sound selection rows with multi-track mixing, per-track slider & FLAC pack controls
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    activeModes.forEach { mode ->
                        val layerVol = activeLayers[mode]
                        SoundPackOptionRow(
                            mode = mode,
                            selected = layerVol != null,
                            layerVolume = layerVol ?: 0.80f,
                            packStatus = packStatuses[mode] ?: SoundPackStatus.NotDownloaded,
                            onSelect = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelectSound(mode)
                            },
                            onLayerVolumeChange = { vol -> onLayerVolumeChange(mode, vol) },
                            onDownload = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onDownloadPack(mode)
                            },
                            onCancelDownload = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onCancelDownload(mode)
                            },
                            onDelete = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onDeletePack(mode)
                            }
                        )
                    }
                    SoundOffOptionTile(
                        selected = activeLayers.isEmpty(),
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
                        RollingOdometerText(
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
                            val timerInteraction = remember { MutableInteractionSource() }
                            val timerScale = rememberPressScale(timerInteraction, pressedScale = 0.91f)
                            Surface(
                                shape = shape,
                                color = if (selected) palette.accent.copy(alpha = 0.22f) else palette.pillContainer,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) palette.accent else palette.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = timerScale
                                        scaleY = timerScale
                                    }
                                    .clip(shape)
                                    .clickable(
                                        interactionSource = timerInteraction,
                                        indication = null,
                                        role = Role.RadioButton
                                    ) {
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

                if (totalDownloadedBytes > 0L) {
                    Text(
                        text = stringResource(
                            R.string.sound_pack_storage_summary,
                            AmbientSoundPackManager.formatFileSize(totalDownloadedBytes)
                        ),
                        style = TabularTextStyles.cardDuration.copy(fontSize = 11.sp),
                        color = palette.accentText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = RestCycleIcons.Moon,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.sound_background_hint),
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
        }
    }
}

@Composable
private fun SoundPackOptionRow(
    mode: AmbientSoundMode,
    selected: Boolean,
    layerVolume: Float,
    packStatus: SoundPackStatus,
    onSelect: () -> Unit,
    onLayerVolumeChange: (Float) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    val shape = RoundedCornerShape(12.dp)
    val contentColor = if (selected) palette.accentText else palette.textPrimary
    val rowInteraction = remember { MutableInteractionSource() }
    val rowScale = rememberPressScale(rowInteraction, pressedScale = 0.97f)

    Surface(
        shape = shape,
        color = if (selected) palette.accent.copy(alpha = 0.20f) else palette.pillContainer,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.accent else palette.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = rowInteraction,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = onSelect
                        )
                        .padding(vertical = 2.dp)
                        .testTag(RestCycleTestTags.ambientSoundOption(mode.name)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (selected) {
                        LiveEqualizerIcon(
                            color = contentColor,
                            iconSize = 16.dp
                        )
                    } else {
                        Icon(
                            imageVector = RestCycleIcons.forAmbientSound(mode),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(mode.labelResId),
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = when (packStatus) {
                            SoundPackStatus.NotDownloaded -> stringResource(R.string.sound_pack_preview)
                            is SoundPackStatus.Downloading -> stringResource(
                                R.string.sound_pack_downloading,
                                packStatus.progressPercent
                            )
                            is SoundPackStatus.Downloaded -> stringResource(
                                R.string.sound_pack_offline_size,
                                AmbientSoundPackManager.formatFileSize(packStatus.sizeBytes)
                            )
                            SoundPackStatus.Error -> stringResource(R.string.sound_pack_error)
                        }
                        Text(
                            text = subtitle,
                            style = TabularTextStyles.cardDuration.copy(fontSize = 10.5.sp),
                            color = if (packStatus is SoundPackStatus.Downloaded) {
                                palette.accentText
                            } else {
                                palette.textSecondary
                            },
                            maxLines = 1
                        )
                    }
                }

                when (packStatus) {
                    SoundPackStatus.NotDownloaded, SoundPackStatus.Error -> {
                        SoundPackActionIconButton(
                            icon = RestCycleIcons.ImportDownload,
                            contentDescription = stringResource(R.string.sound_pack_download_cd),
                            tint = palette.textPrimary,
                            tag = RestCycleTestTags.soundPackDownloadBtn(mode.name),
                            onClick = onDownload
                        )
                    }

                    is SoundPackStatus.Downloading -> {
                        SoundPackActionIconButton(
                            icon = RestCycleIcons.Close,
                            contentDescription = stringResource(R.string.sound_pack_cancel_cd),
                            tint = palette.textSecondary,
                            tag = RestCycleTestTags.soundPackCancelBtn(mode.name),
                            onClick = onCancelDownload
                        )
                    }

                    is SoundPackStatus.Downloaded -> {
                        SoundPackActionIconButton(
                            icon = RestCycleIcons.Trash,
                            contentDescription = stringResource(R.string.sound_pack_delete_cd),
                            tint = palette.textSecondary,
                            tag = RestCycleTestTags.soundPackDeleteBtn(mode.name),
                            onClick = onDelete
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = selected,
                enter = expandVertically(tween(180)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(120))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = layerVolume,
                        onValueChange = onLayerVolumeChange,
                        valueRange = 0.10f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.borderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    )
                    Text(
                        text = "${(layerVolume * 100f).roundToInt()}%",
                        style = TabularTextStyles.cardDuration.copy(fontSize = 11.sp),
                        color = palette.accentText
                    )
                }
            }

            if (packStatus is SoundPackStatus.Downloading) {
                if (packStatus.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = { (packStatus.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = palette.accent,
                        trackColor = palette.borderSubtle.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                } else {
                    LinearProgressIndicator(
                        color = palette.accent,
                        trackColor = palette.borderSubtle.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundPackActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit
) {
    val palette = AtmosphereTheme.palette
    Surface(
        shape = CircleShape,
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SoundOffOptionTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val shape = RoundedCornerShape(12.dp)
    val contentColor = if (selected) palette.accentText else palette.textPrimary
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
            .testTag(RestCycleTestTags.ambientSoundOption(AmbientSoundMode.OFF.name))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = RestCycleIcons.VolumeOff,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(AmbientSoundMode.OFF.labelResId),
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
