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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.restcycle.app.R
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AppLanguage
import com.restcycle.app.ui.theme.AppThemeStyle
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.CatppuccinDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.CatppuccinDayAtmospherePalette
import com.restcycle.app.ui.theme.CatppuccinNightAtmospherePalette
import com.restcycle.app.ui.theme.DawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.DayAtmospherePalette
import com.restcycle.app.ui.theme.NightAtmospherePalette
import com.restcycle.app.ui.theme.NordDawnDuskAtmospherePalette
import com.restcycle.app.ui.theme.NordDayAtmospherePalette
import com.restcycle.app.ui.theme.NordNightAtmospherePalette
import com.restcycle.app.ui.theme.TimeFormatPreference

@Composable
fun SettingsDialog(
    themeStyle: AppThemeStyle,
    timeFormatPreference: TimeFormatPreference,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onSelectTimeFormat: (TimeFormatPreference) -> Unit,
    onDismiss: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onSelectLanguage: (AppLanguage) -> Unit = {}
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RestCycleTestTags.SETTINGS_DIALOG),
            shape = RoundedCornerShape(24.dp),
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.borderSubtle),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
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

                // Time Format Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_time_format_label),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val formatOptions = listOf(
                            TimeFormatPreference.SYSTEM to R.string.settings_time_format_system,
                            TimeFormatPreference.HOURS_24 to R.string.settings_time_format_24h,
                            TimeFormatPreference.HOURS_12 to R.string.settings_time_format_12h
                        )
                        formatOptions.forEach { (pref, labelRes) ->
                            val selected = timeFormatPreference == pref
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(role = Role.RadioButton) {
                                        if (!selected) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            onSelectTimeFormat(pref)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) palette.accent else palette.pillContainer,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) palette.accent else palette.borderSubtle
                                )
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    color = if (selected) palette.onAccent else palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // Theme Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val themeOptions = listOf(
                        Triple(
                            AppThemeStyle.DYNAMIC,
                            R.string.settings_theme_dynamic,
                            listOf(
                                DayAtmospherePalette.accent,
                                DawnDuskAtmospherePalette.backgroundGlow,
                                NightAtmospherePalette.backgroundBottom
                            )
                        ),
                        Triple(
                            AppThemeStyle.NORD,
                            R.string.settings_theme_nord,
                            listOf(
                                NordDayAtmospherePalette.accent,
                                NordDawnDuskAtmospherePalette.backgroundGlow,
                                NordNightAtmospherePalette.accent
                            )
                        ),
                        Triple(
                            AppThemeStyle.CATPPUCCIN_MACCHIATO,
                            R.string.settings_theme_catppuccin_macchiato,
                            listOf(
                                CatppuccinDayAtmospherePalette.accent,
                                CatppuccinDawnDuskAtmospherePalette.backgroundGlow,
                                CatppuccinNightAtmospherePalette.accent
                            )
                        )
                    )

                    themeOptions.forEach { (style, labelRes, dots) ->
                        val selected = themeStyle == style
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    dots.forEach { swatch ->
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Language Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language_label),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    AppLanguage.entries.chunked(2).forEach { rowLanguages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLanguages.forEach { lang ->
                                val selected = appLanguage == lang
                                val label = if (lang == AppLanguage.SYSTEM) {
                                    stringResource(R.string.settings_language_auto)
                                } else {
                                    lang.nativeLabel
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag(RestCycleTestTags.languageOption(lang.name))
                                        .clickable(role = Role.RadioButton) {
                                            if (!selected) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onSelectLanguage(lang)
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) palette.accent else palette.pillContainer,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) palette.accent else palette.borderSubtle
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) palette.onAccent else palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
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
}
