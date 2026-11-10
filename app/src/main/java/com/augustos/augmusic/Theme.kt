package com.augustos.augmusic

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal const val DEFAULT_ACCENT: Int = 0xFFB7F36B.toInt()

internal val ACCENT_SWATCHES: List<Int> = listOf(
    0xFFB7F36B, 0xFF5EEAD4, 0xFF60A5FA, 0xFFA78BFA, 0xFFF472B6,
    0xFFFB923C, 0xFFFACC15, 0xFFF87171, 0xFF34D399, 0xFF94A3B8
).map { it.toInt() }

/** Paleta semântica do WMusic. Todas as telas leem daqui (via WC), nunca de cores fixas. */
@Immutable
internal class WColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentFg: Color,
    val accentSoft: Color,
    val gradTop: Color,
    val danger: Color,
    val useGradients: Boolean
)

internal fun onColorFor(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF10120F) else Color.White

internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

internal fun buildColors(dark: Boolean, accent: Color, gradients: Boolean): WColors =
    if (dark) {
        val bg = lerp(Color(0xFF0C0E0B), accent, 0.035f)
        WColors(
            dark = true,
            bg = bg,
            surface = lerp(Color(0xFF161915), accent, 0.06f),
            surfaceHigh = lerp(Color(0xFF20241E), accent, 0.09f),
            outline = Color.White.copy(alpha = 0.10f),
            text = Color(0xFFF2F4EF),
            muted = Color(0xFFA3AA9E),
            accent = accent,
            onAccent = onColorFor(accent),
            accentFg = accent,
            accentSoft = accent.copy(alpha = 0.16f),
            gradTop = lerp(bg, accent, 0.20f),
            danger = Color(0xFFFF6B6B),
            useGradients = gradients
        )
    } else {
        val bg = lerp(Color(0xFFF7F8F4), accent, 0.04f)
        WColors(
            dark = false,
            bg = bg,
            surface = Color.White,
            surfaceHigh = lerp(Color(0xFFEEF0EA), accent, 0.08f),
            outline = Color.Black.copy(alpha = 0.10f),
            text = Color(0xFF151813),
            muted = Color(0xFF5E655A),
            accent = accent,
            onAccent = onColorFor(accent),
            accentFg = if (accent.luminance() > 0.35f) lerp(accent, Color.Black, 0.5f) else accent,
            accentSoft = accent.copy(alpha = 0.20f),
            gradTop = lerp(bg, accent, 0.30f),
            danger = Color(0xFFC62828),
            useGradients = gradients
        )
    }

internal val LocalWColors = staticCompositionLocalOf { buildColors(true, Color(DEFAULT_ACCENT), true) }

/** Atalho: `val c = WC` dentro de qualquer composable. */
internal val WC: WColors
    @Composable @ReadOnlyComposable get() = LocalWColors.current

private val WTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp)
)

@Composable
internal fun isDarkFor(settings: AppSettings): Boolean {
    val system = isSystemInDarkTheme()
    return when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> system
    }
}

@Composable
internal fun WMusicTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = isDarkFor(settings)
    val context = LocalContext.current
    // "Cores adaptativas": usa a cor do papel de parede (Material You) no Android 12+.
    val accent = if (settings.adaptiveColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else Color(settings.accentColor)
    val colors = remember(dark, accent, settings.gradients) { buildColors(dark, accent, settings.gradients) }
    val scheme = remember(colors) {
        if (dark) darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bg, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh, onSurfaceVariant = colors.muted,
            outline = colors.muted, outlineVariant = colors.outline, error = colors.danger,
            secondaryContainer = colors.accentSoft, onSecondaryContainer = colors.accentFg,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surfaceHigh,
            surfaceContainerHighest = colors.surfaceHigh
        ) else lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bg, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh, onSurfaceVariant = colors.muted,
            outline = colors.muted, outlineVariant = colors.outline, error = colors.danger,
            secondaryContainer = colors.accentSoft, onSecondaryContainer = colors.accentFg,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surfaceHigh,
            surfaceContainerHighest = colors.surfaceHigh
        )
    }
    CompositionLocalProvider(LocalWColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = WTypography, content = content)
    }
}
