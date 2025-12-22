package com.example.tabelahisabapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tabelahisabapp.ui.theme.*

/**
 * Modern gradient card with customizable colors
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientStart: Color = GradientPurpleStart,
    gradientEnd: Color = GradientPurpleEnd,
    cornerRadius: Dp = Spacing.radiusLarge,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    
    Card(
        modifier = cardModifier.shadow(elevation, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                )
                .padding(Spacing.cardPadding)
        ) {
            content()
        }
    }
}

/**
 * Standard white card with shadow
 */
@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Spacing.radiusLarge,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    
    Card(
        modifier = cardModifier.shadow(elevation, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding)
        ) {
            content()
        }
    }
}

/**
 * Tinted card with custom background color
 */
@Composable
fun TintedCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BackgroundGray,
    cornerRadius: Dp = Spacing.radiusLarge,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    
    Card(
        modifier = cardModifier.shadow(elevation, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding)
        ) {
            content()
        }
    }
}

/**
 * Section header text
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = modifier
    )
}

/**
 * Large display number for financial amounts
 */
@Composable
fun DisplayAmount(
    amount: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Text(
        text = amount,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

/**
 * Label text for card sections
 */
@Composable
fun LabelText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = modifier
    )
}
