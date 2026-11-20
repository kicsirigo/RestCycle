package com.restcycle.app.unit

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.R
import com.restcycle.app.RestCycleApplication
import com.restcycle.app.ui.theme.AppLanguage
import com.restcycle.app.ui.util.formatClockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LocalizationParityTest {

    @Test
    fun englishAndHungarianStringsResolveAllKeysAndFormatPlaceholders() {
        val context = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        val simpleIds = listOf(
            R.string.app_name,
            R.string.mode_wake_up_at,
            R.string.mode_sleep_now,
            R.string.mode_go_to_bed_at,
            R.string.mode_power_nap,
            R.string.quality_optimal,
            R.string.quality_good,
            R.string.quality_short,
            R.string.nap_quick_title,
            R.string.nap_classic_title,
            R.string.nap_full_title,
            R.string.nap_quick_desc,
            R.string.nap_classic_desc,
            R.string.nap_full_desc,
            R.string.badge_suggested,
            R.string.nap_inertia_tooltip,
            R.string.alarm_label_wakeup,
            R.string.alarm_label_bedtime,
            R.string.alarm_label_nap,
            R.string.action_set_alarm,
            R.string.action_set_timer,
            R.string.action_wake_alarm,
            R.string.clock_app_unavailable,
            R.string.wheel_am,
            R.string.wheel_pm,
            R.string.latency_sleep_label,
            R.string.latency_nap_label,
            R.string.header_subtitle_wake_up_at,
            R.string.header_subtitle_sleep_now,
            R.string.header_subtitle_go_to_bed_at,
            R.string.header_subtitle_power_nap,
            R.string.sky_phase_day,
            R.string.sky_phase_dawn_dusk,
            R.string.sky_phase_night,
            R.string.cd_celestial_day,
            R.string.cd_celestial_dawn_dusk,
            R.string.cd_celestial_night,
            R.string.cd_time_wheel_hours,
            R.string.cd_time_wheel_minutes,
            R.string.cd_time_wheel_am_pm,
            R.string.sleep_now_hint,
            R.string.power_nap_hint,
            R.string.settings_title,
            R.string.settings_time_format_label,
            R.string.settings_time_format_system,
            R.string.settings_time_format_24h,
            R.string.settings_time_format_12h,
            R.string.settings_theme_label,
            R.string.settings_theme_dynamic,
            R.string.settings_theme_nord,
            R.string.settings_theme_catppuccin_macchiato,
            R.string.settings_theme_tokyo_night,
            R.string.settings_theme_gruvbox,
            R.string.settings_amoled_night,
            R.string.settings_caffeine_label,
            R.string.settings_caffeine_off,
            R.string.settings_reminder_toggle,
            R.string.action_breathe,
            R.string.breathing_title,
            R.string.breathing_phase_inhale,
            R.string.breathing_phase_hold,
            R.string.breathing_phase_exhale,
            R.string.widget_name,
            R.string.reminder_notification_title,
            R.string.settings_language_label,
            R.string.settings_language_auto,
            R.string.settings_update_label,
            R.string.settings_update_check,
            R.string.settings_update_checking,
            R.string.settings_update_error_retry,
            R.string.settings_made_by,
            R.string.action_close
        )

        simpleIds.forEach { resId ->
            assertFalse(context.getString(resId).isBlank())
        }

        assertFalse(context.getString(R.string.cycles_count, 5).isBlank())
        assertFalse(context.getString(R.string.cycles_count_single, 1).isBlank())
        assertFalse(context.getString(R.string.duration_hours_minutes, 9, "00").isBlank())
        assertFalse(context.getString(R.string.duration_minutes_only, 20).isBlank())
        assertFalse(context.getString(R.string.countdown_in_minutes, 30).isBlank())
        assertFalse(context.getString(R.string.bedtime_countdown_now).isBlank())
        assertFalse(context.getString(R.string.bedtime_countdown_hm, 2, 15).isBlank())
        assertFalse(context.getString(R.string.bedtime_countdown_m, 40).isBlank())
        assertFalse(context.getString(R.string.latency_minutes_value, 15).isBlank())
        assertFalse(context.getString(R.string.sleep_now_current_time_label, "22:15").isBlank())
        assertFalse(context.getString(R.string.planned_bedtime_label, "22:30").isBlank())
        assertFalse(context.getString(R.string.cd_set_alarm_for_time, "06:50").isBlank())
        assertFalse(context.getString(R.string.cd_set_timer_for_nap, "Quick Boost").isBlank())
        assertFalse(context.getString(R.string.settings_update_up_to_date, "v1.2.0").isBlank())
        assertFalse(context.getString(R.string.settings_update_available_download, "v1.2.1").isBlank())
        assertFalse(context.getString(R.string.settings_update_downloading, "v1.2.1").isBlank())
    }

    @Test
    @Config(qualifiers = "hu")
    fun hungarianLocaleResolvesTranslationsAndFormats12hTimesWithUsAmPm() {
        val context = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        assertEquals("Ébredés ekkor", context.getString(R.string.mode_wake_up_at))
        assertEquals("Most fekszem le", context.getString(R.string.mode_sleep_now))
        assertEquals("Lefekvés ekkor", context.getString(R.string.mode_go_to_bed_at))
        assertEquals("Szieszta", context.getString(R.string.mode_power_nap))
        assertEquals("9ó 00p", context.getString(R.string.duration_hours_minutes, 9, "00"))

        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("hu-HU"))
            assertEquals("8:45 PM", formatClockTime(LocalTime.of(20, 45), is24HourFormat = false))
            assertEquals("12:15 AM", formatClockTime(LocalTime.of(0, 15), is24HourFormat = false))
            assertEquals("00:15", formatClockTime(LocalTime.of(0, 15), is24HourFormat = true))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun allSupportedLanguagesResolveAllStringsAndModeTitles() {
        val baseContext = ApplicationProvider.getApplicationContext<RestCycleApplication>()
        val simpleIds = listOf(
            R.string.app_name,
            R.string.mode_wake_up_at,
            R.string.mode_sleep_now,
            R.string.mode_go_to_bed_at,
            R.string.mode_power_nap,
            R.string.quality_optimal,
            R.string.quality_good,
            R.string.quality_short,
            R.string.nap_quick_title,
            R.string.nap_classic_title,
            R.string.nap_full_title,
            R.string.nap_quick_desc,
            R.string.nap_classic_desc,
            R.string.nap_full_desc,
            R.string.badge_suggested,
            R.string.nap_inertia_tooltip,
            R.string.alarm_label_wakeup,
            R.string.alarm_label_bedtime,
            R.string.alarm_label_nap,
            R.string.action_set_alarm,
            R.string.action_set_timer,
            R.string.action_wake_alarm,
            R.string.clock_app_unavailable,
            R.string.wheel_am,
            R.string.wheel_pm,
            R.string.latency_sleep_label,
            R.string.latency_nap_label,
            R.string.header_subtitle_wake_up_at,
            R.string.header_subtitle_sleep_now,
            R.string.header_subtitle_go_to_bed_at,
            R.string.header_subtitle_power_nap,
            R.string.sky_phase_day,
            R.string.sky_phase_dawn_dusk,
            R.string.sky_phase_night,
            R.string.cd_celestial_day,
            R.string.cd_celestial_dawn_dusk,
            R.string.cd_celestial_night,
            R.string.cd_time_wheel_hours,
            R.string.cd_time_wheel_minutes,
            R.string.cd_time_wheel_am_pm,
            R.string.sleep_now_hint,
            R.string.power_nap_hint,
            R.string.settings_title,
            R.string.settings_time_format_label,
            R.string.settings_time_format_system,
            R.string.settings_time_format_24h,
            R.string.settings_time_format_12h,
            R.string.settings_theme_label,
            R.string.settings_theme_dynamic,
            R.string.settings_theme_nord,
            R.string.settings_theme_catppuccin_macchiato,
            R.string.settings_theme_tokyo_night,
            R.string.settings_theme_gruvbox,
            R.string.settings_amoled_night,
            R.string.settings_caffeine_label,
            R.string.settings_caffeine_off,
            R.string.settings_reminder_toggle,
            R.string.action_breathe,
            R.string.breathing_title,
            R.string.breathing_phase_inhale,
            R.string.breathing_phase_hold,
            R.string.breathing_phase_exhale,
            R.string.widget_name,
            R.string.reminder_notification_title,
            R.string.settings_language_label,
            R.string.settings_language_auto,
            R.string.settings_update_label,
            R.string.settings_update_check,
            R.string.settings_update_checking,
            R.string.settings_update_error_retry,
            R.string.settings_made_by,
            R.string.action_close
        )

        val expectedWakeUpTitle = mapOf(
            AppLanguage.ENGLISH to "Wake Up At",
            AppLanguage.HUNGARIAN to "Ébredés ekkor",
            AppLanguage.SPANISH to "Despertar a las",
            AppLanguage.ROMANIAN to "Trezire la",
            AppLanguage.POLISH to "Pobudka o",
            AppLanguage.PORTUGUESE to "Acordar às",
            AppLanguage.BRAZILIAN_PORTUGUESE to "Acordar às"
        )

        val expectedNapTitle = mapOf(
            AppLanguage.ENGLISH to "Power Nap",
            AppLanguage.HUNGARIAN to "Szieszta",
            AppLanguage.SPANISH to "Siesta",
            AppLanguage.ROMANIAN to "Siestă",
            AppLanguage.POLISH to "Drzemka",
            AppLanguage.PORTUGUESE to "Sesta",
            AppLanguage.BRAZILIAN_PORTUGUESE to "Cochilo"
        )

        AppLanguage.entries.filter { it != AppLanguage.SYSTEM }.forEach { language ->
            val locale = Locale.forLanguageTag(language.languageTag!!)
            val config = Configuration(baseContext.resources.configuration).apply {
                setLocale(locale)
            }
            val localizedContext = baseContext.createConfigurationContext(config)

            simpleIds.forEach { resId ->
                assertFalse(
                    "Blank string for $language resId=$resId",
                    localizedContext.getString(resId).isBlank()
                )
            }

            assertEquals(expectedWakeUpTitle[language], localizedContext.getString(R.string.mode_wake_up_at))
            assertEquals(expectedNapTitle[language], localizedContext.getString(R.string.mode_power_nap))
            assertFalse(localizedContext.getString(R.string.cycles_count, 5).isBlank())
            assertFalse(localizedContext.getString(R.string.duration_hours_minutes, 9, "00").isBlank())
            assertFalse(localizedContext.getString(R.string.countdown_in_minutes, 30).isBlank())
            assertFalse(localizedContext.getString(R.string.bedtime_countdown_now).isBlank())
            assertFalse(localizedContext.getString(R.string.bedtime_countdown_hm, 2, 15).isBlank())
            assertFalse(localizedContext.getString(R.string.bedtime_countdown_m, 40).isBlank())
            assertFalse(localizedContext.getString(R.string.settings_update_up_to_date, "v1.2.0").isBlank())
            assertFalse(localizedContext.getString(R.string.settings_update_available_download, "v1.2.1").isBlank())
            assertFalse(localizedContext.getString(R.string.settings_update_downloading, "v1.2.1").isBlank())
        }
    }
}
