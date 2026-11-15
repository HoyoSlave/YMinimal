package io.ymusic.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class DesignPalette(
    val isDark: Boolean,
    val Accent: Color,
    val InkStrong: Color,
    val Ink: Color,
    val InkMuted: Color,
    val InkSoft: Color,
    val InkSecondary: Color,
    val InkFaint: Color,
    val IconDots: Color,
    val OnInk: Color,
    val OnAccent: Color,
    val Placeholder: Color,
    val PlaceholderDim: Color,
    val PlaceholderIcon: Color,
    val StackNear: Color,
    val StackFar: Color,
    val StackTab: Color,
    val Outline: Color,
    val OutlineSoft: Color,
    val Divider: Color,
    val DividerMini: Color,
    val Surface: Color,
    val SurfaceDim: Color,
    val SurfaceElevated: Color,
    val SettingsCard: Color,
    val FieldPill: Color,
    val BrandGreen: Color,
) {
    val BadgeInk = Color(0xFF1D1B20)
    val BadgeScrim = Color(0xB8000000)
    val WatchedTrack = Color(0x66FFFFFF)
    val WatchedFill = Color(0xFF0B57D0)
}

val DefaultAccent = Color(0xFF0B57D0)

val LightDesignColors = DesignPalette(
    isDark = false,
    Accent = DefaultAccent,
    InkStrong = Color(0xFF1D1B20),
    Ink = Color(0xFF1D1B20),
    InkMuted = Color(0xFF49454F),
    InkSoft = Color(0xFF49454F),
    InkSecondary = Color(0xFF5F5B66),
    InkFaint = Color(0xFF79747E),
    IconDots = Color(0xFF79747E),
    OnInk = Color.White,
    OnAccent = Color.White,
    Placeholder = Color(0xFFE7E0EC),
    PlaceholderDim = Color(0xFFDDD5E4),
    PlaceholderIcon = Color(0xFF79747E),
    StackNear = Color(0xFFE7E0EC),
    StackFar = Color(0xFFF3EDF7),
    StackTab = Color(0xFFE7E0EC),
    Outline = Color(0xFF79747E),
    OutlineSoft = Color(0xFFCAC4D0),
    Divider = Color(0xFFE7E0EC),
    DividerMini = Color(0xFFE7E0EC),
    Surface = Color(0xFFFDFBFF),
    SurfaceDim = Color(0xFFF3EDF7),
    SurfaceElevated = Color(0xFFECE6F0),
    SettingsCard = Color(0xFFF3EDF7),
    FieldPill = Color(0xFFECE6F0),
    BrandGreen = Color(0xFF146C2E),
)

val DarkDesignColors = DesignPalette(
    isDark = true,
    Accent = DefaultAccent,
    InkStrong = Color(0xFFE6E0E9),
    Ink = Color(0xFFE6E0E9),
    InkMuted = Color(0xFFCAC4D0),
    InkSoft = Color(0xFFCAC4D0),
    InkSecondary = Color(0xFF938F99),
    InkFaint = Color(0xFF79747E),
    IconDots = Color(0xFF938F99),
    OnInk = Color(0xFF141218),
    OnAccent = Color.White,
    Placeholder = Color(0xFF2B2930),
    PlaceholderDim = Color(0xFF36343B),
    PlaceholderIcon = Color(0xFF938F99),
    StackNear = Color(0xFF2B2930),
    StackFar = Color(0xFF211F26),
    StackTab = Color(0xFF36343B),
    Outline = Color(0xFF938F99),
    OutlineSoft = Color(0xFF49454F),
    Divider = Color(0xFF2B2930),
    DividerMini = Color(0xFF25232A),
    Surface = Color(0xFF141218),
    SurfaceDim = Color(0xFF1D1B20),
    SurfaceElevated = Color(0xFF211F26),
    SettingsCard = Color(0xFF1D1B20),
    FieldPill = Color(0xFF2B2930),
    BrandGreen = Color(0xFF6DD58C),
)

val AmoledDesignColors = DarkDesignColors.copy(
    Surface = Color.Black,
    SurfaceDim = Color(0xFF0F0E13),
    SurfaceElevated = Color(0xFF17151C),
    SettingsCard = Color(0xFF0F0E13),
)

fun isDarkTheme(theme: String, systemDark: Boolean): Boolean = when (theme) {
    "light" -> false
    "dark", "amoled" -> true
    else -> systemDark
}

fun accentColorOf(hex: String): Color {
    val rgb = hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16) ?: return DefaultAccent
    return darkenedAccent(Color(0xFF000000 or rgb))
}

const val MaxAccentLuminance = 0.3f

fun darkenedAccent(color: Color): Color {
    if (color.luminance() <= MaxAccentLuminance) return color
    var low = 0f
    var high = 1f
    repeat(16) {
        val mid = (low + high) / 2f
        if (Color(color.red * mid, color.green * mid, color.blue * mid).luminance() <= MaxAccentLuminance) {
            low = mid
        } else {
            high = mid
        }
    }
    return Color(color.red * low, color.green * low, color.blue * low)
}

fun accentHexOf(color: Color): String {
    fun channel(v: Float) = (v * 255f + 0.5f).toInt().coerceIn(0, 255).toString(16).padStart(2, '0')
    return (channel(color.red) + channel(color.green) + channel(color.blue)).uppercase()
}

fun designPaletteFor(theme: String, systemDark: Boolean, accentHex: String = ""): DesignPalette {
    val base = when {
        theme == "amoled" -> AmoledDesignColors
        isDarkTheme(theme, systemDark) -> DarkDesignColors
        else -> LightDesignColors
    }
    val accent = accentColorOf(accentHex)
    return if (accent == base.Accent) base else base.copy(Accent = accent)
}

@Composable
fun rememberStockAndroidPalette(theme: String, systemDark: Boolean, accentHex: String = ""): DesignPalette {
    val dark = isDarkTheme(theme, systemDark)
    val dynamic = if (accentHex.isBlank()) platformDynamicPalette(dark) else null
    if (dynamic != null) {
        return if (theme == "amoled") {
            dynamic.copy(
                Surface = Color.Black,
                SurfaceDim = Color(0xFF0F0E13),
                SurfaceElevated = Color(0xFF17151C),
                SettingsCard = Color(0xFF0F0E13),
            )
        } else {
            dynamic
        }
    }
    return designPaletteFor(theme, systemDark, accentHex)
}

val LocalDesignColors = staticCompositionLocalOf { LightDesignColors }

val DesignColors: DesignPalette
    @Composable @ReadOnlyComposable
    get() = LocalDesignColors.current

internal val ChipTextSize = 13.5.sp

object DesignSpacing {
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space16 = 16.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
    val Space48 = 48.dp

    val MarginCompact = Space16
    val MarginExpanded = Space24

    val RailCollapsedWidth = 80.dp
    val RailExpandedWidth = 280.dp

    val Gutter = Space16
    val ListRowVertical = 10.dp
    val GridRowGap = Space16

    val SettingsRowHorizontal = 20.dp
    val SettingsRowVertical = 16.dp

    val SettingsCardInset = Space16
    val SettingsCardCorner = 24.dp
    val SettingsSectionGap = Space24

    val DialogRowHorizontal = Space24
    val DialogRowVertical = 14.dp

    val FormMaxWidth = 560.dp
}

class GridMetrics(val margin: Dp, val gutter: Dp) {
    val inset = gutter / 2
}

fun gridMetricsFor(windowWidth: Dp): GridMetrics {
    val expanded = windowWidth >= 840.dp
    return GridMetrics(
        margin = if (expanded) DesignSpacing.MarginExpanded else DesignSpacing.MarginCompact,
        gutter = if (expanded) DesignSpacing.Gutter else DesignSpacing.Space8,
    )
}

private fun schemeFor(c: DesignPalette) = with(c) {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    base.copy(
        primary = Accent,
        onPrimary = OnAccent,
        secondary = Ink,
        onSecondary = OnInk,
        tertiary = BrandGreen,
        background = Surface,
        onBackground = Ink,
        surface = Surface,
        onSurface = Ink,
        surfaceVariant = Placeholder,
        onSurfaceVariant = InkSecondary,
        primaryContainer = lerp(Surface, Accent, if (isDark) 0.32f else 0.16f),
        onPrimaryContainer = if (isDark) lerp(Accent, Color.White, 0.4f) else lerp(Accent, Color.Black, 0.45f),
        secondaryContainer = lerp(SurfaceElevated, Accent, if (isDark) 0.22f else 0.12f),
        onSecondaryContainer = InkStrong,
        surfaceContainer = SurfaceDim,
        surfaceContainerLow = SurfaceDim,
        surfaceContainerHigh = SurfaceElevated,
        surfaceContainerHighest = Placeholder,
        outline = Outline,
        outlineVariant = OutlineSoft,
        scrim = BadgeScrim,
    )
}

@Composable
fun YMusicTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    YMusicTheme(if (dark) DarkDesignColors else LightDesignColors, content)

@Composable
fun YMusicTheme(palette: DesignPalette, content: @Composable () -> Unit) {
    val scheme = remember(palette) { schemeFor(palette) }
    CompositionLocalProvider(LocalDesignColors provides palette) {
        SyncSystemBars(palette.isDark)
        MaterialTheme(colorScheme = scheme) {
            CompositionLocalProvider(LocalContentColor provides palette.Ink) {
                AccentRipple(palette.Accent, content)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AccentRipple(accent: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        androidx.compose.material3.LocalRippleConfiguration provides
            androidx.compose.material3.RippleConfiguration(color = accent),
        content = content,
    )
}
