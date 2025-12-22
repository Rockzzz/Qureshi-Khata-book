package com.example.tabelahisabapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabelahisabapp.ui.theme.CardBackground

/**
 * Gradient avatar with initial letter
 */
@Composable
fun GradientAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    gradientStart: Color,
    gradientEnd: Color
) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = CardBackground,
            fontSize = (size.value * 0.4).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Get gradient colors based on name hash for consistency
 */
fun getAvatarGradient(name: String): Pair<Color, Color> {
    val gradients = listOf(
        Color(0xFFFB923C) to Color(0xFFF97316), // Orange
        Color(0xFF3B82F6) to Color(0xFF2563EB), // Blue
        Color(0xFF8B5CF6) to Color(0xFF7C3AED), // Purple
        Color(0xFF14B8A6) to Color(0xFF0D9488), // Teal
        Color(0xFFEC4899) to Color(0xFFDB2777), // Pink
        Color(0xFF10B981) to Color(0xFF059669)  // Green
    )
    
    val index = name.hashCode().mod(gradients.size).let { if (it < 0) it + gradients.size else it }
    return gradients[index]
}
