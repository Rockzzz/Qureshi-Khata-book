package com.example.tabelahisabapp.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern Design System - Spacing Scale
 * Based on 8px base unit for consistent spacing throughout the app
 */
object Spacing {
    // Base units
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    
    // Specific use cases
    val screenPadding: Dp = 20.dp // Screen margins
    val cardPadding: Dp = 16.dp // Card padding
    val cardSpacing: Dp = 12.dp // Spacing between cards
    val sectionSpacing: Dp = 24.dp // Spacing between sections
    
    // Component sizes
    val minTouchTarget: Dp = 48.dp // Minimum touch target size for accessibility
    val fabSize: Dp = 64.dp // Floating Action Button size
    val iconSize: Dp = 24.dp // Standard icon size
    val largeIconSize: Dp = 32.dp // Large icon size
    
    // Border radius
    val radiusSmall: Dp = 8.dp
    val radiusMedium: Dp = 12.dp
    val radiusLarge: Dp = 16.dp
    val radiusXLarge: Dp = 24.dp
    val radiusCircle: Dp = 999.dp // For circular elements
}
