package com.example.tabelahisabapp.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*

/**
 * Voice Clarification/Error Screen - Friendly error handling with options
 * Connected to VoiceFlowViewModel
 */
@Composable
fun VoiceClarificationScreen(
    viewModel: VoiceFlowViewModel = hiltViewModel(),
    onTryAgain: () -> Unit,
    onManualEntry: () -> Unit,
    onCancel: () -> Unit
) {
    val voiceFlowState by viewModel.voiceFlowState.collectAsState()
    
    val clarificationState = voiceFlowState as? VoiceFlowState.NeedsClarification ?: return
    
    Scaffold(
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                        )
                    )
                    .padding(horizontal = Spacing.screenPadding, vertical = Spacing.lg)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = "Need Clarification",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = CardBackground
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = "Let's get this right",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CardBackground.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.md, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Error Message Card
                TintedCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WarningOrange.copy(alpha = 0.1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = clarificationState.error,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = WarningOrange
                            )
                        }
                    }
                }

                // What I Heard Card
                WhiteCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LabelText(text = "What I Heard")
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "\"${clarificationState.transcribedText}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Suggestions
                if (clarificationState.suggestions.isNotEmpty()) {
                    WhiteCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 6.dp
                    ) {
                        Text(
                            text = "Did you mean one of these customers?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.md))
                        
                        // Suggestion buttons
                        clarificationState.suggestions.forEach { suggestion ->
                            Button(
                                onClick = { 
                                    viewModel.selectCustomerSuggestion(suggestion)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Purple600.copy(alpha = 0.1f),
                                    contentColor = Purple600
                                ),
                                shape = AppShapes.button
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Action Buttons
                Button(
                    onClick = {
                        viewModel.reset()
                        onTryAgain()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple600
                    ),
                    shape = AppShapes.button
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "Try Again with Voice",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Purple600
                    )
                ) {
                    Text(
                        text = "Enter Manually",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.reset()
                        onCancel()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
                
                // Help Text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = InfoBlue.copy(alpha = 0.05f)
                    ),
                    shape = AppShapes.card
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md)
                    ) {
                        Text(
                            text = "💡 Tips for better recognition:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = InfoBlue
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "• Speak clearly and slowly\n• Include full names\n• Mention the amount and purpose",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
