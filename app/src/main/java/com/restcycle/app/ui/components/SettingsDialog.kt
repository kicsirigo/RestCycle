package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restcycle.app.R
import com.restcycle.app.platform.update.GitHubUpdateChecker
import com.restcycle.app.platform.update.ReleaseUpdateInfo
import com.restcycle.app.platform.update.UpdateUiState
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppLanguage
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.SolarizedDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.SolarizedDayAtmospherePalette
import com.restcycle.app.ui.theme.SolarizedNightAtmospherePalette
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
import com.restcycle.app.ui.theme.TimeFormatPreference
import com.restcycle.app.ui.theme.TokyoNightDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.TokyoNightDayAtmospherePalette
import com.restcycle.app.ui.theme.TokyoNightNightAtmospherePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    themeStyle: AppThemeStyle,
    timeFormatPreference: TimeFormatPreference,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onSelectTimeFormat: (TimeFormatPreference) -> Unit,
    onDismiss: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    pureAmoledNight: Boolean = false,
    onTogglePureAmoledNight: (Boolean) -> Unit = {},
    caffeineCutoffHours: Int = 8,
    onSelectCaffeineCutoffHours: (Int) -> Unit = {},
    bedtimeReminderEnabled: Boolean = false,
    onToggleBedtimeReminder: (Boolean) -> Unit = {},
    installedVersionName: String = "1.8.4",
    updateUiState: UpdateUiState = UpdateUiState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadUpdate: (ReleaseUpdateInfo) -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onInstallUpdate: (ReleaseUpdateInfo, String) -> Unit = { _, _ -> },
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {}
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current

    LaunchedEffect(Unit) {
        if (updateUiState is UpdateUiState.Idle) onCheckForUpdates()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .testTag(RestCycleTestTags.SETTINGS_DIALOG),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
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

                // Time Format
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionTitle(stringResource(R.string.settings_time_format_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TimeFormatPreference.SYSTEM to R.string.settings_time_format_system,
                            TimeFormatPreference.HOURS_24 to R.string.settings_time_format_24h,
                            TimeFormatPreference.HOURS_12 to R.string.settings_time_format_12h
                        ).forEach { (pref, labelRes) ->
                            val selected = timeFormatPreference == pref
                            SettingsOptionPill(
                                label = stringResource(labelRes),
                                selected = selected,
                                fontSize = 12.sp,
                                verticalPadding = 9.dp,
                                onClick = { if (!selected) onSelectTimeFormat(pref) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Theme + AMOLED & Reminder Toggles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionTitle(stringResource(R.string.settings_theme_label))
                    THEME_OPTIONS.chunked(2).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowThemes.forEach { (style, labelRes, dots) ->
                                val selected = themeStyle == style
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(role = Role.RadioButton) {
                                            if (!selected) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onSelectTheme(style)
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) palette.cardSurfaceHighlighted else palette.pillContainer,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) palette.accent else palette.borderSubtle
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 9.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(labelRes),
                                            color = palette.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            dots.forEach { swatch ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(swatch)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowThemes.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsOptionPill(
                            label = stringResource(R.string.settings_amoled_night),
                            leadingIcon = RestCycleIcons.Contrast,
                            selected = pureAmoledNight,
                            role = Role.Switch,
                            onClick = { onTogglePureAmoledNight(!pureAmoledNight) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(RestCycleTestTags.AMOLED_NIGHT_TOGGLE)
                        )
                        SettingsOptionPill(
                            label = stringResource(R.string.settings_reminder_toggle),
                            leadingIcon = RestCycleIcons.Bell,
                            selected = bedtimeReminderEnabled,
                            role = Role.Switch,
                            onClick = { onToggleBedtimeReminder(!bedtimeReminderEnabled) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(RestCycleTestTags.BEDTIME_REMINDER_TOGGLE)
                        )
                    }
                }

                // Caffeine Cutoff
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionTitle(stringResource(R.string.settings_caffeine_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 6, 8, 10).forEach { hours ->
                            val selected = caffeineCutoffHours == hours
                            val label = if (hours == 0) {
                                stringResource(R.string.settings_caffeine_off)
                            } else {
                                stringResource(R.string.settings_caffeine_hours, hours)
                            }
                            SettingsOptionPill(
                                label = label,
                                selected = selected,
                                onClick = { if (!selected) onSelectCaffeineCutoffHours(hours) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(RestCycleTestTags.caffeineOption(hours))
                            )
                        }
                    }
                }

                // Language
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionTitle(stringResource(R.string.settings_language_label))
                    AppLanguage.entries.chunked(2).forEach { rowLanguages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLanguages.forEach { lang ->
                                val selected = appLanguage == lang
                                val baseLabel = if (lang == AppLanguage.SYSTEM) {
                                    stringResource(R.string.settings_language_auto)
                                } else {
                                    lang.nativeLabel
                                }
                                SettingsOptionPill(
                                    label = baseLabel,
                                    leadingIcon = if (lang == AppLanguage.SYSTEM) RestCycleIcons.Globe else null,
                                    selected = selected,
                                    fontSize = 12.sp,
                                    onClick = { if (!selected) onSelectLanguage(lang) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(RestCycleTestTags.languageOption(lang.name))
                                )
                            }
                        }
                    }
                }

                // Updates
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsSectionTitle(stringResource(R.string.settings_update_label))
                        Text(
                            text = "v$installedVersionName",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    when (updateUiState) {
                        is UpdateUiState.Downloading -> {
                            val sizeSummary = if (updateUiState.totalBytes > 0L) {
                                "${GitHubUpdateChecker.formatBytesMegabytes(updateUiState.downloadedBytes)} / ${
                                    GitHubUpdateChecker.formatBytesMegabytes(updateUiState.totalBytes)
                                }"
                            } else if (updateUiState.downloadedBytes > 0L) {
                                GitHubUpdateChecker.formatBytesMegabytes(updateUiState.downloadedBytes)
                            } else {
                                ""
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.settings_update_downloading_progress,
                                        "v${updateUiState.info.latestVersion}",
                                        updateUiState.progressPercent
                                    ),
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (sizeSummary.isNotEmpty()) {
                                    Text(
                                        text = sizeSummary,
                                        color = palette.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (updateUiState.totalBytes > 0L) {
                                LinearProgressIndicator(
                                    progress = { (updateUiState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    color = palette.accent,
                                    trackColor = palette.borderSubtle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .testTag(RestCycleTestTags.UPDATE_PROGRESS_BAR)
                                )
                            } else {
                                LinearProgressIndicator(
                                    color = palette.accent,
                                    trackColor = palette.borderSubtle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .testTag(RestCycleTestTags.UPDATE_PROGRESS_BAR)
                                )
                            }
                            SettingsOptionPill(
                                label = stringResource(R.string.settings_update_cancel),
                                selected = false,
                                role = Role.Button,
                                fontSize = 12.sp,
                                verticalPadding = 8.dp,
                                onClick = onCancelDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(RestCycleTestTags.UPDATE_CANCEL_BTN)
                            )
                        }

                        is UpdateUiState.ReadyToInstall -> {
                            SettingsOptionPill(
                                label = stringResource(
                                    R.string.settings_update_install_now,
                                    "v${updateUiState.info.latestVersion}"
                                ),
                                leadingIcon = RestCycleIcons.ImportDownload,
                                selected = true,
                                role = Role.Button,
                                fontSize = 12.sp,
                                verticalPadding = 9.dp,
                                onClick = {
                                    onInstallUpdate(updateUiState.info, updateUiState.apkFilePath)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(RestCycleTestTags.UPDATE_ACTION_BTN)
                            )
                            if (updateUiState.needsInstallPermission) {
                                Text(
                                    text = stringResource(R.string.settings_update_permission_hint),
                                    color = palette.textSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        else -> {
                            val updateButtonText = when (updateUiState) {
                                UpdateUiState.Idle -> stringResource(R.string.settings_update_check)
                                UpdateUiState.Checking -> stringResource(R.string.settings_update_checking)
                                is UpdateUiState.UpToDate -> stringResource(
                                    R.string.settings_update_up_to_date,
                                    "v$installedVersionName"
                                )
                                is UpdateUiState.UpdateAvailable -> stringResource(
                                    R.string.settings_update_available_download,
                                    "v${updateUiState.info.latestVersion}"
                                )
                                UpdateUiState.Error -> stringResource(R.string.settings_update_error_retry)
                                else -> stringResource(R.string.settings_update_check)
                            }
                            SettingsOptionPill(
                                label = updateButtonText,
                                leadingIcon = if (updateUiState is UpdateUiState.UpdateAvailable) {
                                    RestCycleIcons.ImportDownload
                                } else {
                                    null
                                },
                                selected = updateUiState is UpdateUiState.UpdateAvailable,
                                enabled = updateUiState !is UpdateUiState.Checking,
                                role = Role.Button,
                                fontSize = 12.sp,
                                verticalPadding = 9.dp,
                                onClick = {
                                    when (updateUiState) {
                                        is UpdateUiState.UpdateAvailable -> onDownloadUpdate(updateUiState.info)
                                        else -> onCheckForUpdates()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(RestCycleTestTags.UPDATE_ACTION_BTN)
                            )
                        }
                    }
                }

                // Sleep data backup (#5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionTitle(stringResource(R.string.settings_backup_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsOptionPill(
                            label = stringResource(R.string.settings_backup_export),
                            leadingIcon = RestCycleIcons.ExportUpload,
                            selected = false,
                            role = Role.Button,
                            fontSize = 12.sp,
                            verticalPadding = 9.dp,
                            onClick = onExportBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(RestCycleTestTags.BACKUP_EXPORT_BTN)
                        )
                        SettingsOptionPill(
                            label = stringResource(R.string.settings_backup_import),
                            leadingIcon = RestCycleIcons.ImportDownload,
                            selected = false,
                            role = Role.Button,
                            fontSize = 12.sp,
                            verticalPadding = 9.dp,
                            onClick = onImportBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(RestCycleTestTags.BACKUP_IMPORT_BTN)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.65f))
                Text(
                    text = stringResource(R.string.settings_made_by),
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
        }
    }
}

private val THEME_OPTIONS = listOf(
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

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = AtmosphereTheme.palette.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsOptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    role: Role = Role.RadioButton,
    fontSize: TextUnit = 11.sp,
    verticalPadding: Dp = 7.dp
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val contentColor = if (selected) palette.onAccent else palette.textPrimary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = role) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) palette.accent else palette.pillContainer,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.accent else palette.borderSubtle
        )
    ) {
        if (leadingIcon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = verticalPadding)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = fontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = verticalPadding)
            )
        }
    }
}
