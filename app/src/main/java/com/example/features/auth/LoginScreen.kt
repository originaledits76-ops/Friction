package com.example.features.auth

import com.example.core.widgets.ResponsiveText
import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.FrictionSpinner
import com.example.core.widgets.PremiumBackground
import com.example.data.repository.AuthStatus
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // React to authentication state updates
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthStatus.Loading -> {
                isLoading = true
            }
            is AuthStatus.Error -> {
                isLoading = false
                errorMessage = (uiState as AuthStatus.Error).message
            }
            else -> {
                isLoading = false
            }
        }
    }

    // Gentle float animation for mascot in top section
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        // Abstract subtle background
        PremiumBackground(style = BackgroundStyle.SETTINGS)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalHeight = maxHeight

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top 45% - Large mascot illustration centered horizontally & floating
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeight * 0.42f)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft glowing aura behind Flick mascot
                    Canvas(modifier = Modifier.size(220.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    FrictionPrimary.copy(alpha = 0.22f),
                                    FrictionSecondary.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.width * 0.65f
                        )
                    }

                    if (isLoading) {
                        FrictionSpinner()
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.mascot_hi),
                            contentDescription = "Flick Mascot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .offset(y = floatOffset.dp)
                                .fillMaxHeight(0.85f)
                                .aspectRatio(1f)
                        )
                    }
                }

                // Bottom 55% - Liquid Glass Container
                Surface(
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                    color = DarkSurface.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 28.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Greeting and Descriptions
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ResponsiveText(
                                text = "Hi, I'm Flick!",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            ResponsiveText(
                                text = "I'm here to help you build better habits and protect your focus.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = FrictionPrimary,
                                textAlign = TextAlign.Center
                            )

                            ResponsiveText(
                                text = "Friction introduces an intentional pause before opening distracting apps so you stay in control of your time and mind.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Authentication Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FrictionButton(
                                text = "Continue with Google",
                                onClick = {
                                    activity?.let { viewModel.loginWithGoogle(it) }
                                },
                                isLoading = isLoading,
                                icon = { GoogleIconDrawing() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )

                            FrictionButton(
                                text = "Continue as Guest",
                                onClick = {
                                    viewModel.loginAsGuest()
                                },
                                isLoading = isLoading,
                                isSecondary = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            ResponsiveText(
                                text = "By continuing, you agree to our Terms of Service & Privacy Policy.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Animated Dialog for Error State
        if (errorMessage != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { errorMessage = null }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FrictionError.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(FrictionError.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                drawLine(
                                    color = FrictionError,
                                    start = Offset(size.width / 2f, size.height * 0.25f),
                                    end = Offset(size.width / 2f, size.height * 0.65f),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawCircle(
                                    color = FrictionError,
                                    radius = 2.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height * 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ResponsiveText(
                            text = "Authentication Error",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = FrictionError,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ResponsiveText(
                            text = errorMessage ?: "Sign-in cancelled.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ResponsiveText(
                            text = "Please explore as Guest for full local experience.",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrictionSecondaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FrictionButton(
                                text = "Retry Sign-In",
                                onClick = {
                                    errorMessage = null
                                    activity?.let { viewModel.loginWithGoogle(it) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            FrictionButton(
                                text = "Continue as Guest",
                                onClick = {
                                    errorMessage = null
                                    viewModel.loginAsGuest()
                                },
                                isSecondary = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws the Google G Icon logo dynamically using Canvas
 */
@Composable
fun GoogleIconDrawing() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val redColor = Color(0xFFEA4335)
        val yellowColor = Color(0xFFFBBC05)
        val greenColor = Color(0xFF34A853)
        val blueColor = Color(0xFF4285F4)

        val strokeWidth = 3.5.dp.toPx()

        drawArc(
            color = redColor,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = yellowColor,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = greenColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = blueColor,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )

        drawLine(
            color = blueColor,
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(w * 0.95f, h * 0.5f),
            strokeWidth = strokeWidth
        )
    }
}


