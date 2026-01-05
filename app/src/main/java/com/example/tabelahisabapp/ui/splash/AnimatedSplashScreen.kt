package com.example.tabelahisabapp.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabelahisabapp.R
import kotlinx.coroutines.delay

// Premium gradient colors for splash
private val SplashGradientStart = Color(0xFF1E1E1E)
private val SplashGradientEnd = Color(0xFF2D2D2D)
private val AccentGold = Color(0xFFD4A574)
private val AccentPurple = Color(0xFF8B5CF6)

/**
 * Premium animated splash screen with loading animation
 */
@Composable
fun AnimatedSplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    // Logo scale animation
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutBack
        ),
        label = "scale"
    )
    
    // Logo pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Progress animation
    LaunchedEffect(Unit) {
        startAnimation = true
        // Animate progress bar
        while (progress < 1f) {
            progress += 0.02f
            delay(30)
        }
        delay(200) // Short pause after loading complete
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SplashGradientStart, SplashGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated App Logo - Using launcher foreground (same as app drawer)
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale * pulse)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name with gradient text effect
            Text(
                text = "Qureshi Khata Book",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your Business Partner",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Animated Progress Bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AccentGold, AccentPurple)
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading text
            Text(
                text = "Loading...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        // Bottom branding
        Text(
            text = "Made with ❤️",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
