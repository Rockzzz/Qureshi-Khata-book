package com.example.tabelahisabapp.ui.voice

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.tabelahisabapp.ui.theme.*

/**
 * Voice Recording Screen - Full screen modal with pulsing microphone animation
 * Connected to VoiceFlowViewModel for real speech recognition
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecordingScreen(
    viewModel: VoiceFlowViewModel = hiltViewModel(),
    onNavigateToConfirmation: () -> Unit,
    onNavigateToClarification: () -> Unit,
    onCancel: () -> Unit
) {
    val transcribedText by viewModel.transcribedText.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val voiceFlowState by viewModel.voiceFlowState.collectAsState()
    
    // Permission handling
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // Auto-start recording when permission granted
    LaunchedEffect(micPermission.status.isGranted) {
        if (micPermission.status.isGranted && !isRecording) {
            viewModel.startRecording()
        }
    }
    
    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!micPermission.status.isGranted) {
            micPermission.launchPermissionRequest()
        }
    }
    
    // Navigate based on state
    LaunchedEffect(voiceFlowState) {
        when (voiceFlowState) {
            is VoiceFlowState.Confirmation -> onNavigateToConfirmation()
            is VoiceFlowState.NeedsClarification -> onNavigateToClarification()
            else -> {}
        }
    }

    // Pulsing animation for the microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Format recording duration (MM:SS)
    val minutes = (recordingDuration / 1000) / 60
    val seconds = (recordingDuration / 1000) % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)

    // Full screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Close button at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = {
                    viewModel.cancelRecording()
                    onCancel()
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Pulsing microphone icon
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulse rings (only when recording)
                if (isRecording) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseScale + (index * 0.15f))
                                .alpha(pulseAlpha / (index + 1))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            GradientPurpleStart.copy(alpha = 0.5f),
                                            GradientPurpleEnd.copy(alpha = 0.2f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Main microphone icon
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Recording",
                            tint = CardBackground,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Status text
            Text(
                text = when (voiceFlowState) {
                    is VoiceFlowState.Recording -> "Sun Raha Hoon..."
                    is VoiceFlowState.Parsing -> "Samajh Raha Hoon..."
                    is VoiceFlowState.Error -> "Galti Ho Gayi"
                    else -> "Bolo, Main Sun Raha Hoon"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Recording duration
            if (isRecording) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            // Error message
            if (voiceFlowState is VoiceFlowState.Error) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = (voiceFlowState as VoiceFlowState.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Transcribed text box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = AppShapes.card
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.md),
                    contentAlignment = if (transcribedText.isEmpty()) Alignment.Center else Alignment.TopStart
                ) {
                    if (transcribedText.isEmpty()) {
                        Text(
                            text = "Aise Bolo:\n\"Aijaz se 50 hazar mila\"\n\"Gaffar bhai ko 1 lac diya\"\n\"Milk ka 200 rs kharcha\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = transcribedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Stop recording button (only when recording)
            if (isRecording) {
                Button(
                    onClick = { viewModel.stopRecording() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed
                    ),
                    shape = AppShapes.button
                ) {
                    Text(
                        text = "Band Karo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (!micPermission.status.isGranted) {
                // Permission button
                Button(
                    onClick = { micPermission.launchPermissionRequest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple600
                    ),
                    shape = AppShapes.button
                ) {
                    Text(
                        text = "Mic Permission Do",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Start recording button
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple600
                    ),
                    shape = AppShapes.button
                ) {
                    Text(
                        text = "Shuru Karo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Cancel text button
            TextButton(onClick = {
                viewModel.cancelRecording()
                onCancel()
            }) {
                Text(
                    text = "Rehne Do",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CardBackground
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}
