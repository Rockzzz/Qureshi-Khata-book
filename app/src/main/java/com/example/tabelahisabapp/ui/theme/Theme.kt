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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.tabelahisabapp.data.preferences.ThemePreferences

// ═══════════════════════════════════════════════════════════════════════════════
// DARK COLOR SCHEME - Premium Dark Mode
// ═══════════════════════════════════════════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = Purple300,                    // Lighter purple for dark mode
    onPrimary = DarkBackground,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple50,
    
    // Secondary colors
    secondary = AccentTeal,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSoftTeal,
    onSecondaryContainer = SoftTeal,
    
    // Tertiary colors
    tertiary = AccentBlue,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkSoftBlue,
    onTertiaryContainer = SoftBlue,
    
    // Background and surface
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    
    // Error colors
    error = AccentRed,
    onError = DarkBackground,
    errorContainer = DarkSoftRed,
    onErrorContainer = SoftRed,
    
    // Outline and borders
    outline = DarkBorder,
    outlineVariant = DarkSurfaceVariant,
    
    // Inverse colors
    inverseSurface = CardBackground,
    inverseOnSurface = TextPrimary,
    inversePrimary = Purple600,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f)
)

// ═══════════════════════════════════════════════════════════════════════════════
// LIGHT COLOR SCHEME - Premium Light Mode
// ═══════════════════════════════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Purple600,                    // Main brand color
    onPrimary = CardBackground,
    primaryContainer = Purple50,
    onPrimaryContainer = Purple700,
    
    // Secondary colors
    secondary = AccentTeal,
    onSecondary = CardBackground,
    secondaryContainer = SoftTeal,
    onSecondaryContainer = DarkSoftTeal,
    
    // Tertiary colors
    tertiary = AccentBlue,
    onTertiary = CardBackground,
    tertiaryContainer = SoftBlue,
    onTertiaryContainer = DarkSoftBlue,
    
    // Background and surface
    background = BackgroundGray,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextSecondary,
    
    // Error colors
    error = AccentRed,
    onError = CardBackground,
    errorContainer = SoftRed,
    onErrorContainer = DarkSoftRed,
    
    // Outline and borders
    outline = BorderGray,
    outlineVariant = Color(0xFFE2E8F0),    // Slate-200
    
    // Inverse colors
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = Purple300,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.3f)
)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COMPOSITION LOCALS
// ═══════════════════════════════════════════════════════════════════════════════

// Theme mode: "light", "dark", or "auto"
val LocalThemeMode = staticCompositionLocalOf<String> { "auto" }

// Whether we're in dark theme (computed value)
val LocalIsDarkTheme = compositionLocalOf { false }

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENDED COLOR PROVIDER
// For semantic colors that need to change based on theme
// ═══════════════════════════════════════════════════════════════════════════════
data class ExtendedColors(
    val isDark: Boolean,
    
    // Card backgrounds
    val cardBackground: Color,
    val cardBackgroundElevated: Color,
    
    // Soft card backgrounds
    val softGreen: Color,
    val softRed: Color,
    val softBlue: Color,
    val softPurple: Color,
    val softOrange: Color,
    
    // Text colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    
    // Navigation
    val navBarBackground: Color,
    val navBarSelected: Color,
    val navBarUnselected: Color,
    
    // Gradient colors
    val gradientStart: Color,
    val gradientEnd: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        isDark = false,
        cardBackground = CardBackground,
        cardBackgroundElevated = CardBackground,
        softGreen = SoftGreen,
        softRed = SoftRed,
        softBlue = SoftBlue,
        softPurple = SoftPurple,
        softOrange = SoftOrange,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary,
        navBarBackground = NavBarLight,
        navBarSelected = NavBarSelectedLight,
        navBarUnselected = NavBarUnselectedLight,
        gradientStart = GradientPurpleStart,
        gradientEnd = GradientPurpleEnd
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun TabelaHisabAppTheme(
    themePreferences: ThemePreferences? = null,
    dynamicColor: Boolean = false,  // Disabled by default for consistent branding
    content: @Composable () -> Unit
) {
    // Determine theme mode from preferences
    val themeMode = if (themePreferences != null) {
        themePreferences.themeMode.collectAsState(initial = "auto").value
    } else {
        "auto"
    }
    
    // Determine if we should use dark theme
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDarkTheme // "auto" follows system
    }
    
    // Select color scheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Extended colors based on theme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            isDark = true,
            cardBackground = DarkSurface,
            cardBackgroundElevated = DarkSurfaceVariant,
            softGreen = DarkSoftGreen,
            softRed = DarkSoftRed,
            softBlue = DarkSoftBlue,
            softPurple = DarkSoftPurple,
            softOrange = DarkSoftOrange,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textTertiary = DarkTextTertiary,
            navBarBackground = NavBarDark,
            navBarSelected = NavBarSelectedDark,
            navBarUnselected = NavBarUnselectedDark,
            gradientStart = DarkGradientPurpleStart,
            gradientEnd = DarkGradientPurpleEnd
        )
    } else {
        ExtendedColors(
            isDark = false,
            cardBackground = CardBackground,
            cardBackgroundElevated = CardBackground,
            softGreen = SoftGreen,
            softRed = SoftRed,
            softBlue = SoftBlue,
            softPurple = SoftPurple,
            softOrange = SoftOrange,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textTertiary = TextTertiary,
            navBarBackground = NavBarLight,
            navBarSelected = NavBarSelectedLight,
            navBarUnselected = NavBarUnselectedLight,
            gradientStart = GradientPurpleStart,
            gradientEnd = GradientPurpleEnd
        )
    }
    
    // Set status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar matches the gradient header
            window.statusBarColor = if (darkTheme) {
                DarkGradientPurpleStart.toArgb()
            } else {
                GradientPurpleStart.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // Provide theme values
    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalIsDarkTheme provides darkTheme,
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// THEME ACCESSOR OBJECT
// Easy access to extended colors from composables
// ═══════════════════════════════════════════════════════════════════════════════
object AppTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
    
    val isDark: Boolean
        @Composable
        get() = LocalIsDarkTheme.current
}
