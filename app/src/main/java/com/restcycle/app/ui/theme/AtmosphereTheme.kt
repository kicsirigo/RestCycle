package com.restcycle.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.restcycle.app.domain.model.SkyPhase

@Immutable
data class AtmospherePalette(
    val skyPhase: SkyPhase,
    val backgroundGlow: Color,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val cardSurface: Color,
    val cardSurfaceHighlighted: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderSubtle: Color,
    val pillContainer: Color,
    val isLightSystemBars: Boolean
)

enum class AppThemeStyle {
    TOKYO_NIGHT,
    NORD,
    EVERFOREST,
    GRUVBOX,
    DRACULA,
    ROSE_PINE
}

enum class TimeFormatPreference {
    SYSTEM,
    HOURS_24,
    HOURS_12
}

enum class AppLanguage(val languageTag: String?, val nativeLabel: String) {
    SYSTEM(null, "Auto"),
    ENGLISH("en", "English"),
    HUNGARIAN("hu", "Magyar"),
    SPANISH("es", "Español"),
    ROMANIAN("ro", "Română"),
    POLISH("pl", "Polski"),
    PORTUGUESE("pt-PT", "Português"),
    BRAZILIAN_PORTUGUESE("pt-BR", "Português (BR)")
}

val NordDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFE5E9F0),
    backgroundTop = Color(0xFFE0E5ED),
    backgroundMid = Color(0xFFD8DEE9),
    backgroundBottom = Color(0xFFCFD7E3),
    cardSurface = Color(0xFFECEFF4),
    cardSurfaceHighlighted = Color(0xFFE2E7F0),
    accent = Color(0xFF5E81AC),
    accentText = Color(0xFF3B5B84),
    onAccent = Color(0xFFECEFF4),
    textPrimary = Color(0xFF2E3440),
    textSecondary = Color(0xFF4C566A),
    borderSubtle = Color(0xFFC2CAD8),
    pillContainer = Color(0xFFD8DEE9),
    isLightSystemBars = true
)

val NordDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFD08770),
    backgroundTop = Color(0xFF434C5E),
    backgroundMid = Color(0xFF353D4D),
    backgroundBottom = Color(0xFF2E3440),
    cardSurface = Color(0xFF3B4252),
    cardSurfaceHighlighted = Color(0xFF4C566A),
    accent = Color(0xFFEBCB8B),
    accentText = Color(0xFFEBCB8B),
    onAccent = Color(0xFF2E3440),
    textPrimary = Color(0xFFECEFF4),
    textSecondary = Color(0xFFD8DEE9),
    borderSubtle = Color(0xFF5E687E),
    pillContainer = Color(0xFF3B4252),
    isLightSystemBars = false
)

val NordNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF242933),
    backgroundTop = Color(0xFF242933),
    backgroundMid = Color(0xFF1F242D),
    backgroundBottom = Color(0xFF1D212A),
    cardSurface = Color(0xFF2E3440),
    cardSurfaceHighlighted = Color(0xFF3B4252),
    accent = Color(0xFF88C0D0),
    accentText = Color(0xFF88C0D0),
    onAccent = Color(0xFF1D212A),
    textPrimary = Color(0xFFECEFF4),
    textSecondary = Color(0xFFD8DEE9),
    borderSubtle = Color(0xFF434C5E),
    pillContainer = Color(0xFF2B313C),
    isLightSystemBars = false
)

val EverforestDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFFDF6E3),
    backgroundTop = Color(0xFFF4F0D9),
    backgroundMid = Color(0xFFEFEBD4),
    backgroundBottom = Color(0xFFE6E2CC),
    cardSurface = Color(0xFFF8F5E4),
    cardSurfaceHighlighted = Color(0xFFEDEADA),
    accent = Color(0xFF8DA101),
    accentText = Color(0xFF536A00),
    onAccent = Color(0xFFFDF6E3),
    textPrimary = Color(0xFF5C6A72),
    textSecondary = Color(0xFF708089),
    borderSubtle = Color(0xFFBEC5B2),
    pillContainer = Color(0xFFE5E1CB),
    isLightSystemBars = true
)

val EverforestDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFE69875),
    backgroundTop = Color(0xFF423D33),
    backgroundMid = Color(0xFF323936),
    backgroundBottom = Color(0xFF272E33),
    cardSurface = Color(0xFF2E383C),
    cardSurfaceHighlighted = Color(0xFF374145),
    accent = Color(0xFFDBBC7F),
    accentText = Color(0xFFDBBC7F),
    onAccent = Color(0xFF272E33),
    textPrimary = Color(0xFFD3C6AA),
    textSecondary = Color(0xFFB9C0AB),
    borderSubtle = Color(0xFF4F5B58),
    pillContainer = Color(0xFF2B3438),
    isLightSystemBars = false
)

val EverforestNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF232A2E),
    backgroundTop = Color(0xFF1E2326),
    backgroundMid = Color(0xFF1A1F22),
    backgroundBottom = Color(0xFF161A1D),
    cardSurface = Color(0xFF272E33),
    cardSurfaceHighlighted = Color(0xFF323C41),
    accent = Color(0xFFA7C080),
    accentText = Color(0xFFA7C080),
    onAccent = Color(0xFF1E2326),
    textPrimary = Color(0xFFD3C6AA),
    textSecondary = Color(0xFF9DA9A0),
    borderSubtle = Color(0xFF414B50),
    pillContainer = Color(0xFF232A2E),
    isLightSystemBars = false
)

val TokyoNightDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFECEEF5),
    backgroundTop = Color(0xFFE6E8F2),
    backgroundMid = Color(0xFFDFE2EE),
    backgroundBottom = Color(0xFFD8DCEB),
    cardSurface = Color(0xFFF0F2F8),
    cardSurfaceHighlighted = Color(0xFFE5E8F4),
    accent = Color(0xFF2E7DE9),
    accentText = Color(0xFF1B52A6),
    onAccent = Color(0xFFF4F6FB),
    textPrimary = Color(0xFF24283B),
    textSecondary = Color(0xFF4A5068),
    borderSubtle = Color(0xFFBDC1D4),
    pillContainer = Color(0xFFDCE0EE),
    isLightSystemBars = true
)

val TokyoNightDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFFF9E64),
    backgroundTop = Color(0xFF3E2A3D),
    backgroundMid = Color(0xFF22243B),
    backgroundBottom = Color(0xFF1A1B26),
    cardSurface = Color(0xFF24283B),
    cardSurfaceHighlighted = Color(0xFF323750),
    accent = Color(0xFFFF9E64),
    accentText = Color(0xFFFF9E64),
    onAccent = Color(0xFF1A1B26),
    textPrimary = Color(0xFFE5E9F8),
    textSecondary = Color(0xFFD0D7F2),
    borderSubtle = Color(0xFF414868),
    pillContainer = Color(0xFF22263A),
    isLightSystemBars = false
)

val TokyoNightNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF030305),
    backgroundTop = Color(0xFF020204),
    backgroundMid = Color(0xFF010103),
    backgroundBottom = Color(0xFF010102),
    cardSurface = Color(0xFF1F2335),
    cardSurfaceHighlighted = Color(0xFF292E42),
    accent = Color(0xFF7AA2F7),
    accentText = Color(0xFF7AA2F7),
    onAccent = Color(0xFF101118),
    textPrimary = Color(0xFFC0CAF5),
    textSecondary = Color(0xFF9AA5CE),
    borderSubtle = Color(0xFF3B4261),
    pillContainer = Color(0xFF1B1E2E),
    isLightSystemBars = false
)

val GruvboxDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFF2E5BC),
    backgroundTop = Color(0xFFEBDBB2),
    backgroundMid = Color(0xFFE3D1A4),
    backgroundBottom = Color(0xFFD5C4A1),
    cardSurface = Color(0xFFF4E8C6),
    cardSurfaceHighlighted = Color(0xFFEAD9AE),
    accent = Color(0xFFD65D0E),
    accentText = Color(0xFFAF3A03),
    onAccent = Color(0xFFFBF1C7),
    textPrimary = Color(0xFF3C3836),
    textSecondary = Color(0xFF665C54),
    borderSubtle = Color(0xFFBDAE93),
    pillContainer = Color(0xFFE0CFA4),
    isLightSystemBars = true
)

val GruvboxDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFFE8019),
    backgroundTop = Color(0xFF4B2D24),
    backgroundMid = Color(0xFF362723),
    backgroundBottom = Color(0xFF282828),
    cardSurface = Color(0xFF3C3836),
    cardSurfaceHighlighted = Color(0xFF504945),
    accent = Color(0xFFFABD2F),
    accentText = Color(0xFFFABD2F),
    onAccent = Color(0xFF282828),
    textPrimary = Color(0xFFEBDBB2),
    textSecondary = Color(0xFFD5C4A1),
    borderSubtle = Color(0xFF665C54),
    pillContainer = Color(0xFF32302F),
    isLightSystemBars = false
)

val GruvboxNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF282828),
    backgroundTop = Color(0xFF222222),
    backgroundMid = Color(0xFF1D2021),
    backgroundBottom = Color(0xFF181A1B),
    cardSurface = Color(0xFF282828),
    cardSurfaceHighlighted = Color(0xFF3C3836),
    accent = Color(0xFFB8BB26),
    accentText = Color(0xFFB8BB26),
    onAccent = Color(0xFF1D2021),
    textPrimary = Color(0xFFEBDBB2),
    textSecondary = Color(0xFFBDAE93),
    borderSubtle = Color(0xFF504945),
    pillContainer = Color(0xFF252627),
    isLightSystemBars = false
)

val DraculaDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFF8F8F2),
    backgroundTop = Color(0xFFF1F0FA),
    backgroundMid = Color(0xFFE9E7F6),
    backgroundBottom = Color(0xFFE0DDF2),
    cardSurface = Color(0xFFFAF9FF),
    cardSurfaceHighlighted = Color(0xFFEEEBF9),
    accent = Color(0xFF9580FF),
    accentText = Color(0xFF6B3FA0),
    onAccent = Color(0xFFF8F8F2),
    textPrimary = Color(0xFF282A36),
    textSecondary = Color(0xFF4E5269),
    borderSubtle = Color(0xFFC5C0DC),
    pillContainer = Color(0xFFE5E1F4),
    isLightSystemBars = true
)

val DraculaDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFFF79C6),
    backgroundTop = Color(0xFF472A4D),
    backgroundMid = Color(0xFF322A46),
    backgroundBottom = Color(0xFF282A36),
    cardSurface = Color(0xFF343746),
    cardSurfaceHighlighted = Color(0xFF44475A),
    accent = Color(0xFFFFB86C),
    accentText = Color(0xFFFFB86C),
    onAccent = Color(0xFF282A36),
    textPrimary = Color(0xFFF8F8F2),
    textSecondary = Color(0xFFDADAE8),
    borderSubtle = Color(0xFF6272A4),
    pillContainer = Color(0xFF303341),
    isLightSystemBars = false
)

val DraculaNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF21222C),
    backgroundTop = Color(0xFF1B1C25),
    backgroundMid = Color(0xFF16171F),
    backgroundBottom = Color(0xFF12131A),
    cardSurface = Color(0xFF282A36),
    cardSurfaceHighlighted = Color(0xFF383A4A),
    accent = Color(0xFFBD93F9),
    accentText = Color(0xFFBD93F9),
    onAccent = Color(0xFF12131A),
    textPrimary = Color(0xFFF8F8F2),
    textSecondary = Color(0xFFB6C2E2),
    borderSubtle = Color(0xFF44475A),
    pillContainer = Color(0xFF232530),
    isLightSystemBars = false
)

val RosePineDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFFAF4ED),
    backgroundTop = Color(0xFFF4EDE4),
    backgroundMid = Color(0xFFEFE6DC),
    backgroundBottom = Color(0xFFE8DDD2),
    cardSurface = Color(0xFFFFFAF3),
    cardSurfaceHighlighted = Color(0xFFF2E9E1),
    accent = Color(0xFFD7827E),
    accentText = Color(0xFF9E3F5B),
    onAccent = Color(0xFFFFFAF3),
    textPrimary = Color(0xFF575279),
    textSecondary = Color(0xFF6E6A86),
    borderSubtle = Color(0xFFD4C9C0),
    pillContainer = Color(0xFFEDE2D8),
    isLightSystemBars = true
)

val RosePineDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFEA9A97),
    backgroundTop = Color(0xFF3E2D45),
    backgroundMid = Color(0xFF2B263E),
    backgroundBottom = Color(0xFF232136),
    cardSurface = Color(0xFF2A273F),
    cardSurfaceHighlighted = Color(0xFF393552),
    accent = Color(0xFFF6C177),
    accentText = Color(0xFFF6C177),
    onAccent = Color(0xFF232136),
    textPrimary = Color(0xFFE0DEF4),
    textSecondary = Color(0xFFC4C1DE),
    borderSubtle = Color(0xFF56526E),
    pillContainer = Color(0xFF27243B),
    isLightSystemBars = false
)

val RosePineNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF191724),
    backgroundTop = Color(0xFF161420),
    backgroundMid = Color(0xFF13111C),
    backgroundBottom = Color(0xFF100E18),
    cardSurface = Color(0xFF1F1D2E),
    cardSurfaceHighlighted = Color(0xFF26233A),
    accent = Color(0xFFEBBCBA),
    accentText = Color(0xFFEBBCBA),
    onAccent = Color(0xFF100E18),
    textPrimary = Color(0xFFE0DEF4),
    textSecondary = Color(0xFFABA6C7),
    borderSubtle = Color(0xFF403D52),
    pillContainer = Color(0xFF1B1929),
    isLightSystemBars = false
)

fun paletteForTheme(
    skyPhase: SkyPhase,
    themeStyle: AppThemeStyle = AppThemeStyle.TOKYO_NIGHT,
    pureAmoledNight: Boolean = false
): AtmospherePalette {
    val base = when (themeStyle) {
        AppThemeStyle.TOKYO_NIGHT -> when (skyPhase) {
            SkyPhase.DAY -> TokyoNightDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> TokyoNightDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> TokyoNightNightAtmospherePalette
        }
        AppThemeStyle.NORD -> when (skyPhase) {
            SkyPhase.DAY -> NordDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> NordDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> NordNightAtmospherePalette
        }
        AppThemeStyle.EVERFOREST -> when (skyPhase) {
            SkyPhase.DAY -> EverforestDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> EverforestDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> EverforestNightAtmospherePalette
        }
        AppThemeStyle.GRUVBOX -> when (skyPhase) {
            SkyPhase.DAY -> GruvboxDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> GruvboxDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> GruvboxNightAtmospherePalette
        }
        AppThemeStyle.DRACULA -> when (skyPhase) {
            SkyPhase.DAY -> DraculaDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> DraculaDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> DraculaNightAtmospherePalette
        }
        AppThemeStyle.ROSE_PINE -> when (skyPhase) {
            SkyPhase.DAY -> RosePineDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> RosePineDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> RosePineNightAtmospherePalette
        }
    }
    return if (pureAmoledNight && skyPhase == SkyPhase.NIGHT) {
        base.copy(
            backgroundGlow = Color(0xFF000000),
            backgroundTop = Color(0xFF000000),
            backgroundMid = Color(0xFF000000),
            backgroundBottom = Color(0xFF000000)
        )
    } else {
        base
    }
}

val LocalAtmospherePalette = compositionLocalOf { TokyoNightDayAtmospherePalette }
val LocalAppThemeStyle = compositionLocalOf { AppThemeStyle.TOKYO_NIGHT }

object AtmosphereTheme {
    val palette: AtmospherePalette
        @Composable
        get() = LocalAtmospherePalette.current

    val themeStyle: AppThemeStyle
        @Composable
        get() = LocalAppThemeStyle.current
}

const val ATMOSPHERE_TRANSITION_DURATION_MS = 600

@Composable
fun AtmosphereTheme(
    skyPhase: SkyPhase,
    themeStyle: AppThemeStyle = AppThemeStyle.TOKYO_NIGHT,
    pureAmoledNight: Boolean = false,
    content: @Composable () -> Unit
) {
    val target = paletteForTheme(skyPhase, themeStyle, pureAmoledNight)
    val animSpec = tween<Color>(durationMillis = ATMOSPHERE_TRANSITION_DURATION_MS)

    val backgroundGlow by animateColorAsState(target.backgroundGlow, animSpec, label = "bgGlow")
    val backgroundTop by animateColorAsState(target.backgroundTop, animSpec, label = "bgTop")
    val backgroundMid by animateColorAsState(target.backgroundMid, animSpec, label = "bgMid")
    val backgroundBottom by animateColorAsState(target.backgroundBottom, animSpec, label = "bgBottom")
    val cardSurface by animateColorAsState(target.cardSurface, animSpec, label = "cardSurface")
    val cardSurfaceHighlighted by animateColorAsState(
        target.cardSurfaceHighlighted,
        animSpec,
        label = "cardSurfaceHighlighted"
    )
    val accent by animateColorAsState(target.accent, animSpec, label = "accent")
    val accentText by animateColorAsState(target.accentText, animSpec, label = "accentText")
    val onAccent by animateColorAsState(target.onAccent, animSpec, label = "onAccent")
    val textPrimary by animateColorAsState(target.textPrimary, animSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, animSpec, label = "textSecondary")
    val borderSubtle by animateColorAsState(target.borderSubtle, animSpec, label = "borderSubtle")
    val pillContainer by animateColorAsState(target.pillContainer, animSpec, label = "pillContainer")

    val animatedPalette = AtmospherePalette(
        skyPhase = skyPhase,
        backgroundGlow = backgroundGlow,
        backgroundTop = backgroundTop,
        backgroundMid = backgroundMid,
        backgroundBottom = backgroundBottom,
        cardSurface = cardSurface,
        cardSurfaceHighlighted = cardSurfaceHighlighted,
        accent = accent,
        accentText = accentText,
        onAccent = onAccent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        borderSubtle = borderSubtle,
        pillContainer = pillContainer,
        isLightSystemBars = target.isLightSystemBars
    )

    val colorScheme = if (target.isLightSystemBars) {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = backgroundTop,
            onBackground = textPrimary,
            surface = cardSurface,
            onSurface = textPrimary,
            surfaceVariant = pillContainer,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = backgroundBottom,
            onBackground = textPrimary,
            surface = cardSurface,
            onSurface = textPrimary,
            surfaceVariant = pillContainer,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle
        )
    }

    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLightBars = target.isLightSystemBars

    fun syncSystemBars() {
        if (view.isInEditMode) return
        val window = view.context.findActivity()?.window ?: return
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = isLightBars
        controller.isAppearanceLightNavigationBars = isLightBars
    }

    LaunchedEffect(view, isLightBars) {
        syncSystemBars()
    }

    DisposableEffect(lifecycleOwner, view, isLightBars) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncSystemBars()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(
        LocalAtmospherePalette provides animatedPalette,
        LocalAppThemeStyle provides themeStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RestCycleTypography,
            content = content
        )
    }
}

internal tailrec fun Context?.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
