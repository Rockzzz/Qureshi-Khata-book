package com.example.tabelahisabapp.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Modern Design System - Shapes
 * Consistent rounded corners for cards, buttons, and other components
 */
object AppShapes {
    val small: CornerBasedShape = RoundedCornerShape(8.dp)
    val medium: CornerBasedShape = RoundedCornerShape(12.dp)
    val large: CornerBasedShape = RoundedCornerShape(16.dp)
    val extraLarge: CornerBasedShape = RoundedCornerShape(24.dp)
    
    // Component-specific shapes
    val card: CornerBasedShape = large // 16dp for cards
    val button: CornerBasedShape = medium // 12dp for buttons
    val textField: CornerBasedShape = medium // 12dp for input fields
    val dialog: CornerBasedShape = extraLarge // 24dp for dialogs/modals
}
