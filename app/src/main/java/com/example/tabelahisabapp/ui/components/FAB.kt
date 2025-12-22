package com.example.tabelahisabapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tabelahisabapp.ui.theme.*

/**
 * Modern Floating Action Button with gradient background
 */
@Composable
fun GradientFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Mic,
    contentDescription: String = "Voice Input",
    size: Dp = Spacing.fabSize
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        containerColor = Color.Transparent,
        content = {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                        ),
                        shape = CircleShape
                    )
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = CardBackground,
                        modifier = Modifier.size(Spacing.largeIconSize)
                    )
                }
            }
        }
    )
}

/**
 * Speed Dial FAB with expandable options
 */
@Composable
fun SpeedDialFAB(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddFarm: () -> Unit,
    onAddTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(200),
        label = "fab_rotation"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Backdrop scrim when expanded
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggle() }
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed dial options
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                exit = fadeOut(tween(100)) + scaleOut(tween(100))
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Trade option
                    SpeedDialItem(
                        label = "Add Trade",
                        icon = Icons.Default.ShowChart,
                        onClick = {
                            onToggle()
                            onAddTrade()
                        }
                    )
                    
                    // Add Farm option
                    SpeedDialItem(
                        label = "Add Farm",
                        icon = Icons.Default.Agriculture,
                        onClick = {
                            onToggle()
                            onAddFarm()
                        }
                    )
                }
            }
            
            // Main FAB
            FloatingActionButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(Spacing.fabSize)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                containerColor = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .size(Spacing.fabSize)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (expanded) "Close" else "Add",
                        tint = CardBackground,
                        modifier = Modifier
                            .size(Spacing.largeIconSize)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardBackground,
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        
        // Mini FAB
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = CardBackground,
            contentColor = Purple600,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
