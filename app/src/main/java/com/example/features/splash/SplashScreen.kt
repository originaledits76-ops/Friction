package com.example.features.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

// Simple representation of floating particles
data class Particle(
    val xPercent: Float,
    var yPercent: Float,
    val speed: Float,
    val radiusDp: Float,
    val alpha: Float
)

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // 1. Create a state sequence for Splash Screen animations
    var splashState by remember { mutableStateOf(0) } // 0: Start, 1: Logo, 2: Tagline, 3: Settle/Zoom, 4: Out

    // 2. Continuous animation variables
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    // Constant slow float multiplier for particles
    val floatMultiplier by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_float"
    )

    // Setup random particles
    val particles = remember {
        List(25) {
            Particle(
                xPercent = Random.nextFloat(),
                yPercent = Random.nextFloat(),
                speed = 0.05f + Random.nextFloat() * 0.08f,
                radiusDp = 2f + Random.nextFloat() * 4f,
                alpha = 0.15f + Random.nextFloat() * 0.3f
            )
        }
    }

    // 3. Orchestrate steps
    LaunchedEffect(Unit) {
        // Step 0: Background loads immediately
        delay(300)
        
        // Step 1: Logo fades in and bounces
        splashState = 1
        delay(1000)
        
        // Step 2: Tagline fades up & tiny line expands
        splashState = 2
        delay(1000)
        
        // Step 3: Subtle zoom down and brightening
        splashState = 3
        delay(600)
        
        // Step 4: Dispatch completion
        splashState = 4
        onSplashComplete()
    }

    // 4. M3-aligned coordinated animators
    val backgroundIntensity by animateFloatAsState(
        targetValue = if (splashState >= 3) 0.05f else 0.12f,
        animationSpec = tween(700, easing = EaseInOutQuad),
        label = "bg_gradient_intensity"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (splashState >= 1) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutQuad),
        label = "logo_alpha"
    )

    val logoScale by animateFloatAsState(
        targetValue = when (splashState) {
            0 -> 0.3f
            1, 2 -> 1.0f
            else -> 0.94f // Scales down slightly on settle
        },
        animationSpec = if (splashState == 1) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            tween(600, easing = EaseInOutQuad)
        },
        label = "logo_scale"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue = if (splashState >= 2) 1f else 0f,
        animationSpec = tween(700, easing = EaseOutSine),
        label = "tagline_alpha"
    )

    val taglineOffsetY by animateDpAsState(
        targetValue = if (splashState >= 2) 0.dp else 16.dp,
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "tagline_offset"
    )

    val lineProgress by animateFloatAsState(
        targetValue = if (splashState >= 2) 1f else 0f,
        animationSpec = tween(900, easing = EaseInOutSine),
        label = "line_progress"
    )

    // Visual composition
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // A. Subtle Brand Green Radial/Linear Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FrictionPrimary.copy(alpha = backgroundIntensity),
                            DarkBackground
                        ),
                        center = Offset.Unspecified,
                        radius = 1200f
                    )
                )
        )

        // B. Drifting Brand Particles (Dynamic Canvas)
        val density = LocalDensity.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                // Calculate moving Y coordinate based on floatMultiplier
                val currentYPercent = (p.yPercent - (floatMultiplier * p.speed * 0.05f)) % 1.0f
                val finalYPercent = if (currentYPercent < 0) currentYPercent + 1.0f else currentYPercent

                val pxX = p.xPercent * width
                val pxY = finalYPercent * height
                val radiusPx = with(density) { p.radiusDp.dp.toPx() }

                drawCircle(
                    color = FrictionPrimary.copy(alpha = p.alpha),
                    radius = radiusPx,
                    center = Offset(pxX, pxY)
                )
            }
        }

        // C. Logo & Tagline Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // 1. Friction Animated Geometric Logo & Glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Subtle green blur glow behind logo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .blur(20.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    FrictionPrimary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // High-fidelity official Friction Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Friction Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Product Name "Friction"
            Text(
                text = "Friction",
                style = MaterialTheme.typography.displayMedium,
                color = FrictionText,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .scale(logoScale)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Tagline "Transform your life."
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .alpha(taglineAlpha)
                    .offset(y = taglineOffsetY),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Transform your life.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = FrictionSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Coordinated growing line underneath tagline
                    Canvas(
                        modifier = Modifier
                            .width(80.dp)
                            .height(2.5.dp)
                    ) {
                        val lineWidth = size.width * lineProgress
                        val startX = (size.width - lineWidth) / 2
                        drawRoundRect(
                            color = FrictionPrimary,
                            topLeft = Offset(startX, 0f),
                            size = Size(lineWidth, size.height),
                            cornerRadius = CornerRadius(1.5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
