// Theme.kt
package de.softopus.hotklick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Farbpalette aus der Web-App
object HotKlickColors {
    val Primary = Color(0xFF2196F3)
    val PrimaryDark = Color(0xFF1976D2)
    val Secondary = Color(0xFFFF9800)
    val Success = Color(0xFF4CAF50)
    val Danger = Color(0xFFF44336)
    val Warning = Color(0xFFFFC107)
    val Dark = Color(0xFF212121)
    val Light = Color(0xFFF5F5F5)
    val Gray = Color(0xFF757575)
    val White = Color(0xFFFFFFFF)
    val Border = Color(0xFFE0E0E0)
}

// Light Color Scheme (Web-App Style)
private val LightColorScheme = lightColorScheme(
    primary = HotKlickColors.Primary,
    onPrimary = HotKlickColors.White,
    primaryContainer = HotKlickColors.PrimaryDark,
    onPrimaryContainer = HotKlickColors.White,

    secondary = HotKlickColors.Secondary,
    onSecondary = HotKlickColors.White,

    tertiary = HotKlickColors.Success,
    onTertiary = HotKlickColors.White,

    error = HotKlickColors.Danger,
    onError = HotKlickColors.White,

    background = HotKlickColors.Light,
    onBackground = HotKlickColors.Dark,

    surface = HotKlickColors.White,
    onSurface = HotKlickColors.Dark,

    surfaceVariant = HotKlickColors.Light,
    onSurfaceVariant = HotKlickColors.Gray,

    outline = HotKlickColors.Border,
    outlineVariant = HotKlickColors.Gray.copy(alpha = 0.3f)
)

@Composable
fun DrawPointTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}