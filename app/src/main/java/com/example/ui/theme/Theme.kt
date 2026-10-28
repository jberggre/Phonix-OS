package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PhonixDarkColorScheme = darkColorScheme(
    primary = PhonixBlueLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFCCE5FF),
    secondary = PhonixCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004E58),
    onSecondaryContainer = Color(0xFFBCEBF2),
    tertiary = PhonixGreen,
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF00522E),
    onTertiaryContainer = Color(0xFF8FF7B7),
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate600
)

private val PhonixLightColorScheme = lightColorScheme(
    primary = PhonixBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE5FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = PhonixCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCEBF2),
    onSecondaryContainer = Color(0xFF002024),
    tertiary = PhonixGreen,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate700,
    outline = Slate400
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek technical dark theme for server/OS environment
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PhonixDarkColorScheme
        else -> PhonixLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
