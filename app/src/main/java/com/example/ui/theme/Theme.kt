package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeFittDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DeFittPureBlack,
    primaryContainer = NeonGreenContainer,
    onPrimaryContainer = NeonGreenSoft,
    secondary = CodGold,
    onSecondary = DeFittPureBlack,
    secondaryContainer = CodGoldContainer,
    onSecondaryContainer = CodGold,
    tertiary = InfoCyan,
    onTertiary = DeFittPureBlack,
    background = DeFittBlack,
    onBackground = TextWhite,
    surface = DeFittSurface,
    onSurface = TextWhite,
    surfaceVariant = DeFittSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DeFittSurfaceElevated,
    surfaceContainerHigh = DeFittSurfaceHigh,
    outline = DeFittOutline,
    error = DangerRed,
    onError = TextWhite
)

val DeFittShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DeFittTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DeFittDarkColorScheme,
        typography = DeFittTypography,
        shapes = DeFittShapes,
        content = content
    )
}
