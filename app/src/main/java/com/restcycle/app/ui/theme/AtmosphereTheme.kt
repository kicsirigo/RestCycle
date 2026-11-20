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

val DayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFE8E0CE),
    backgroundTop = Color(0xFFE2DDD0),
    backgroundMid = Color(0xFFDCE0D8),
    backgroundBottom = Color(0xFFD5DDD8),
    cardSurface = Color(0xFFEDE8DC),
    cardSurfaceHighlighted = Color(0xFFE6DEC8),
    accent = Color(0xFFF59E0B),
    accentText = Color(0xFF8C3D05),
    onAccent = Color(0xFF1A1C22),
    textPrimary = Color(0xFF1A1C22),
    textSecondary = Color(0xFF4A505C),
    borderSubtle = Color(0xFFC8C5B8),
    pillContainer = Color(0xFFD8D4C6),
    isLightSystemBars = true
)

val DawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFD96B27),
    backgroundTop = Color(0xFF4A2525),
    backgroundMid = Color(0xFF291A24),
    backgroundBottom = Color(0xFF1F1523),
    cardSurface = Color(0xFF261B20),
    cardSurfaceHighlighted = Color(0xFF362426),
    accent = Color(0xFFFF8C42),
    accentText = Color(0xFFFF8C42),
    onAccent = Color(0xFF1A1C22),
    textPrimary = Color(0xFFF2E4D4),
    textSecondary = Color(0xFFD6BFB0),
    borderSubtle = Color(0xFF4D3434),
    pillContainer = Color(0xFF2B1E23),
    isLightSystemBars = false
)

val NightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF000000),
    backgroundTop = Color(0xFF000000),
    backgroundMid = Color(0xFF000000),
    backgroundBottom = Color(0xFF000000),
    cardSurface = Color(0xFF11131A),
    cardSurfaceHighlighted = Color(0xFF1A1E29),
    accent = Color(0xFFE6C687),
    accentText = Color(0xFFE6C687),
    onAccent = Color(0xFF1A1C22),
    textPrimary = Color(0xFFE4E0D8),
    textSecondary = Color(0xFFA7A49E),
    borderSubtle = Color(0xFF232734),
    pillContainer = Color(0xFF141720),
    isLightSystemBars = false
)

enum class AppThemeStyle {
    DYNAMIC,
    NORD,
    CATPPUCCIN_MACCHIATO
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

val NordAtmospherePalette = NordNightAtmospherePalette

val CatppuccinDayAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAY,
    backgroundGlow = Color(0xFFECEFF5),
    backgroundTop = Color(0xFFE6E9EF),
    backgroundMid = Color(0xFFE1E5ED),
    backgroundBottom = Color(0xFFDCE0E8),
    cardSurface = Color(0xFFEFF1F5),
    cardSurfaceHighlighted = Color(0xFFE6E9EF),
    accent = Color(0xFF8839EF),
    accentText = Color(0xFF722ED1),
    onAccent = Color(0xFFEFF1F5),
    textPrimary = Color(0xFF4C4F69),
    textSecondary = Color(0xFF5C5F77),
    borderSubtle = Color(0xFFCCD0DA),
    pillContainer = Color(0xFFDCE0E8),
    isLightSystemBars = true
)

val CatppuccinDawnDuskAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.DAWN_DUSK,
    backgroundGlow = Color(0xFFF5A97F),
    backgroundTop = Color(0xFF3B3952),
    backgroundMid = Color(0xFF2B2D42),
    backgroundBottom = Color(0xFF24273A),
    cardSurface = Color(0xFF363A4F),
    cardSurfaceHighlighted = Color(0xFF494D64),
    accent = Color(0xFFF5A97F),
    accentText = Color(0xFFF5A97F),
    onAccent = Color(0xFF24273A),
    textPrimary = Color(0xFFCAD3F5),
    textSecondary = Color(0xFFB8C0E0),
    borderSubtle = Color(0xFF5B6078),
    pillContainer = Color(0xFF30344A),
    isLightSystemBars = false
)

val CatppuccinNightAtmospherePalette = AtmospherePalette(
    skyPhase = SkyPhase.NIGHT,
    backgroundGlow = Color(0xFF1E2030),
    backgroundTop = Color(0xFF181926),
    backgroundMid = Color(0xFF151622),
    backgroundBottom = Color(0xFF12131D),
    cardSurface = Color(0xFF24273A),
    cardSurfaceHighlighted = Color(0xFF363A4F),
    accent = Color(0xFFC6A0F6),
    accentText = Color(0xFFC6A0F6),
    onAccent = Color(0xFF12131D),
    textPrimary = Color(0xFFCAD3F5),
    textSecondary = Color(0xFFA5ADCB),
    borderSubtle = Color(0xFF494D64),
    pillContainer = Color(0xFF202336),
    isLightSystemBars = false
)

val CatppuccinMacchiatoAtmospherePalette = CatppuccinNightAtmospherePalette

fun paletteForSkyPhase(skyPhase: SkyPhase): AtmospherePalette = when (skyPhase) {
    SkyPhase.DAY -> DayAtmospherePalette
    SkyPhase.DAWN_DUSK -> DawnDuskAtmospherePalette
    SkyPhase.NIGHT -> NightAtmospherePalette
}

fun paletteForTheme(
    skyPhase: SkyPhase,
    themeStyle: AppThemeStyle = AppThemeStyle.DYNAMIC
): AtmospherePalette = when (themeStyle) {
    AppThemeStyle.DYNAMIC -> paletteForSkyPhase(skyPhase)
    AppThemeStyle.NORD -> when (skyPhase) {
        SkyPhase.DAY -> NordDayAtmospherePalette
        SkyPhase.DAWN_DUSK -> NordDawnDuskAtmospherePalette
        SkyPhase.NIGHT -> NordNightAtmospherePalette
    }
    AppThemeStyle.CATPPUCCIN_MACCHIATO -> when (skyPhase) {
        SkyPhase.DAY -> CatppuccinDayAtmospherePalette
        SkyPhase.DAWN_DUSK -> CatppuccinDawnDuskAtmospherePalette
        SkyPhase.NIGHT -> CatppuccinNightAtmospherePalette
    }
}

val LocalAtmospherePalette = compositionLocalOf { DayAtmospherePalette }
val LocalAppThemeStyle = compositionLocalOf { AppThemeStyle.DYNAMIC }

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
    themeStyle: AppThemeStyle = AppThemeStyle.DYNAMIC,
    content: @Composable () -> Unit
) {
    val target = paletteForTheme(skyPhase, themeStyle)
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

private tailrec fun Context?.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
