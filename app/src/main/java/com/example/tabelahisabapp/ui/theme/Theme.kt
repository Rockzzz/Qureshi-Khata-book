package com.example.tabelahisabapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.tabelahisabapp.data.preferences.ThemePreferences

private val DarkColorScheme = darkColorScheme(
    primary = Purple600,
    onPrimary = CardBackground,
    secondary = Pink500,
    onSecondary = CardBackground,
    tertiary = InfoBlue,
    background = TextPrimary,
    surface = Color(0xFF1F2937), // Dark surface
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = Purple600,
    onPrimary = CardBackground,
    secondary = Pink500,
    onSecondary = CardBackground,
    tertiary = InfoBlue,
    background = BackgroundGray,
    surface = CardBackground,
    error = DangerRed,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextSecondary
)

// Local composition for theme mode
val LocalThemeMode = staticCompositionLocalOf<String> { "auto" }

@Composable
fun TabelaHisabAppTheme(
    themePreferences: ThemePreferences? = null,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeMode = if (themePreferences != null) {
        themePreferences.themeMode.collectAsState(initial = "auto").value
    } else {
        "auto"
    }
    
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDarkTheme // "auto"
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
