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
    SOLARIZED,
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
    backgroundGlow = Color(0xFF7EB4D6),
    backgroundTop = Color(0xFF4C7C9E),
    backgroundMid = Color(0xFF406B8B),
    backgroundBottom = Color(0xFF375E7B),
    cardSurface = Color(0xFF2B4C66),
    cardSurfaceHighlighted = Color(0xFF365D7C),
    accent = Color(0xFF88C0D0),
    accentText = Color(0xFFA3E0F2),
    onAccent = Color(0xFF1B3042),
    textPrimary = Color(0xFFF2F7FA),
    textSecondary = Color(0xFFCCE0EE),
    borderSubtle = Color(0xFF6998B8),
    pillContainer = Color(0xFF26455D),
    isLightSystemBars = false
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
    backgroundGlow = Color(0xFF68A878),
    backgroundTop = Color(0xFF376E4E),
    backgroundMid = Color(0xFF2E5F44),
    backgroundBottom = Color(0xFF27543B),
    cardSurface = Color(0xFF1F4531),
    cardSurfaceHighlighted = Color(0xFF29573E),
    accent = Color(0xFFA7C080),
    accentText = Color(0xFFC5E898),
    onAccent = Color(0xFF152E21),
    textPrimary = Color(0xFFF1F5EA),
    textSecondary = Color(0xFFC8DEC9),
    borderSubtle = Color(0xFF538A68),
    pillContainer = Color(0xFF1C3E2C),
    isLightSystemBars = false
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
    backgroundGlow = Color(0xFF4F9CF9),
    backgroundTop = Color(0xFF2C64B8),
    backgroundMid = Color(0xFF24539E),
    backgroundBottom = Color(0xFF1E4688),
    cardSurface = Color(0xFF1B3B73),
    cardSurfaceHighlighted = Color(0xFF244C91),
    accent = Color(0xFF7CE8FF),
    accentText = Color(0xFF7CE8FF),
    onAccent = Color(0xFF0F2347),
    textPrimary = Color(0xFFF2F6FF),
    textSecondary = Color(0xFFC7DBFA),
    borderSubtle = Color(0xFF4A7BD0),
    pillContainer = Color(0xFF19376B),
    isLightSystemBars = false
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
    backgroundGlow = Color(0xFFD98A48),
    backgroundTop = Color(0xFF8A4E24),
    backgroundMid = Color(0xFF77421E),
    backgroundBottom = Color(0xFF663818),
    cardSurface = Color(0xFF4F2C14),
    cardSurfaceHighlighted = Color(0xFF63381A),
    accent = Color(0xFFFABD2F),
    accentText = Color(0xFFFFD269),
    onAccent = Color(0xFF2B170A),
    textPrimary = Color(0xFFFDF3DB),
    textSecondary = Color(0xFFE8CFA8),
    borderSubtle = Color(0xFFA66A3E),
    pillContainer = Color(0xFF472711),
    isLightSystemBars = false
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

val SolarizedDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFF36A1AD),
    backgroundTop = Color(0xFF1B6D7A),
    backgroundMid = Color(0xFF155D6A),
    backgroundBottom = Color(0xFF114E5A),
    cardSurface = Color(0xFF0D424D),
    cardSurfaceHighlighted = Color(0xFF135361),
    accent = Color(0xFF2AA198),
    accentText = Color(0xFFF5D26B),
    onAccent = Color(0xFF07262D),
    textPrimary = Color(0xFFEEF8F8),
    textSecondary = Color(0xFFBFE2E6),
    borderSubtle = Color(0xFF328594),
    pillContainer = Color(0xFF0B3A44),
    isLightSystemBars = false
)

val SolarizedDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFCB4B16),
    backgroundTop = Color(0xFF1B424D),
    backgroundMid = Color(0xFF0F3540),
    backgroundBottom = Color(0xFF072B35),
    cardSurface = Color(0xFF0B3642),
    cardSurfaceHighlighted = Color(0xFF134756),
    accent = Color(0xFFDFB234),
    accentText = Color(0xFFF0CA5E),
    onAccent = Color(0xFF07262E),
    textPrimary = Color(0xFFEEF3F0),
    textSecondary = Color(0xFFB8CFD4),
    borderSubtle = Color(0xFF2A5F6D),
    pillContainer = Color(0xFF09303B),
    isLightSystemBars = false
)

val SolarizedNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF073642),
    backgroundTop = Color(0xFF042E39),
    backgroundMid = Color(0xFF022630),
    backgroundBottom = Color(0xFF001F27),
    cardSurface = Color(0xFF073642),
    cardSurfaceHighlighted = Color(0xFF0E4654),
    accent = Color(0xFF2AA198),
    accentText = Color(0xFF56CFC6),
    onAccent = Color(0xFF001F27),
    textPrimary = Color(0xFFE3ECEA),
    textSecondary = Color(0xFF9CB6BC),
    borderSubtle = Color(0xFF1C5463),
    pillContainer = Color(0xFF052D38),
    isLightSystemBars = false
)

val RosePineDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFC97A94),
    backgroundTop = Color(0xFF7D425E),
    backgroundMid = Color(0xFF6C3751),
    backgroundBottom = Color(0xFF5C2E46),
    cardSurface = Color(0xFF4A2439),
    cardSurfaceHighlighted = Color(0xFF5E2F49),
    accent = Color(0xFFF6C177),
    accentText = Color(0xFFFFD09B),
    onAccent = Color(0xFF2B1421),
    textPrimary = Color(0xFFFBF0F5),
    textSecondary = Color(0xFFE5C5D6),
    borderSubtle = Color(0xFF9C5C7B),
    pillContainer = Color(0xFF422033),
    isLightSystemBars = false
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
        AppThemeStyle.SOLARIZED -> when (skyPhase) {
            SkyPhase.DAY -> SolarizedDayAtmospherePalette
            SkyPhase.DAWN_DUSK -> SolarizedDawnDuskAtmospherePalette
            SkyPhase.NIGHT -> SolarizedNightAtmospherePalette
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
