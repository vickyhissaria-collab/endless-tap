package com.atlas.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AtlasMode { Command, Reading }

data class AtlasPalette(
    val mode: AtlasMode,
    val canvas: Color,
    val canvas2: Color,
    val canvas3: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val border: Color,
    val borderStrong: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val text4: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentDeep: Color,
    val second: Color,
    val secondSoft: Color,
    val secondDeep: Color
) {
    val isDark: Boolean get() = mode == AtlasMode.Command
}

val CommandPalette = AtlasPalette(
    mode = AtlasMode.Command,
    canvas = Void, canvas2 = Void2, canvas3 = Void3,
    surface = Glass, surfaceStrong = GlassStrong,
    border = GlassBorder, borderStrong = GlassBorderStrong,
    text1 = Light1, text2 = Light2, text3 = Light3, text4 = Light4,
    accent = Gold, accentSoft = GoldSoft, accentDeep = GoldDeep,
    second = Purple, secondSoft = PurpleSoft, secondDeep = PurpleDeep
)

val ReadingPalette = AtlasPalette(
    mode = AtlasMode.Reading,
    canvas = Paper, canvas2 = Paper2, canvas3 = Paper3,
    surface = Color(0xCCFFFFFF), surfaceStrong = Color(0xFFFFFFFF),
    border = Color(0x141A140C), borderStrong = Color(0x1F1A140C),
    text1 = Ink1, text2 = Ink2, text3 = Ink3, text4 = Ink4,
    accent = PurpleDeep, accentSoft = Color(0x1A6A4CC8),
    accentDeep = Color(0xFF4D35A0),
    second = GoldDeep, secondSoft = Color(0x1FA07D2F), secondDeep = Color(0xFF7E5F1F)
)

val LocalAtlasPalette = compositionLocalOf { CommandPalette }

@Composable
@ReadOnlyComposable
fun atlasPalette(): AtlasPalette = LocalAtlasPalette.current

@Composable
fun AtlasTheme(
    mode: AtlasMode = AtlasMode.Command,
    content: @Composable () -> Unit
) {
    val palette = if (mode == AtlasMode.Command) CommandPalette else ReadingPalette

    val colorScheme = if (mode == AtlasMode.Command) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.canvas,
            secondary = palette.second,
            onSecondary = palette.canvas,
            background = palette.canvas,
            onBackground = palette.text1,
            surface = palette.canvas2,
            onSurface = palette.text1,
            surfaceVariant = palette.canvas3,
            onSurfaceVariant = palette.text2,
            outline = palette.borderStrong
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.canvas,
            secondary = palette.second,
            onSecondary = palette.canvas,
            background = palette.canvas,
            onBackground = palette.text1,
            surface = palette.canvas2,
            onSurface = palette.text1,
            surfaceVariant = palette.canvas3,
            onSurfaceVariant = palette.text2,
            outline = palette.borderStrong
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            val darkIcons = palette.canvas.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }

    CompositionLocalProvider(LocalAtlasPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AtlasTypography,
            content = content
        )
    }
}
