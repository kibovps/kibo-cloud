package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ArchivaDarkScheme = darkColorScheme(
    primary = SkyPrimary,
    onPrimary = Color(0xFF0B1F4D),
    primaryContainer = SkyContainerDark,
    onPrimaryContainer = SkyOnContainerDark,
    secondary = AmberLight,
    onSecondary = Color(0xFF3E2200),
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark,
    tertiary = TealLight,
    tertiaryContainer = TealContainerDark,
    background = ObsidianBg,
    onBackground = InkLight,
    surface = ObsidianSurface,
    onSurface = InkLight,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = InkMutedLight,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianHigh,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
)

private val ArchivaLightScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = CobaltOnContainerLight,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberOnContainerLight,
    tertiary = TealTertiary,
    tertiaryContainer = TealContainerLight,
    background = AlabasterBg,
    onBackground = InkDark,
    surface = CrispSurface,
    onSurface = InkDark,
    surfaceVariant = WarmStoneContainer,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = CrispSurface,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = WarmStoneContainer,
    surfaceContainerHigh = WarmStoneHigh,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
)

val ArchivaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ArchivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ArchivaDarkScheme
        else -> ArchivaLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArchivaTypography,
        shapes = ArchivaShapes,
        content = content,
    )
}
