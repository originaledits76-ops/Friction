package com.example.features.auth

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.core.widgets.GlassCard
import com.example.core.widgets.PremiumBackground
import com.example.core.widgets.ResponsiveText
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

    // Entrance animation state
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        // Subtle ambient background
        PremiumBackground(style = BackgroundStyle.SETTINGS)

        AnimatedVisibility(
            visible = startAnimation,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(600, easing = EaseOutCubic)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Brand Emblem Header
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    FrictionPrimary.copy(alpha = 0.25f),
                                    FrictionSecondary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.width * 0.7f
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Friction Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                }

                // Typography Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ResponsiveText(
                        text = "Take Back Your Time.",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    ResponsiveText(
                        text = "Build healthier screen habits, one decision at a time.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Compact Benefit Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BenefitCardItem(
                        icon = Icons.Default.Tune,
                        title = "Set boundaries",
                        description = "Control distracting apps with meaningful friction."
                    )

                    BenefitCardItem(
                        icon = Icons.Default.Bolt,
                        title = "Build consistency",
                        description = "Track your progress, streaks and XP."
                    )

                    BenefitCardItem(
                        icon = Icons.Default.Psychology,
                        title = "Stay focused",
                        description = "Turn mindless scrolling into intentional choices."
                    )
                }

                // Liquid Glass Authentication Card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = DarkSurface.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Google Sign In (Primary)
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = FrictionPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            ResponsiveText(
                                text = "Google keeps your progress safely connected to your account.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Divider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.08f)
                            )
                            ResponsiveText(
                                text = "or",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.08f)
                            )
                        }

                        // Guest Sign In (Secondary)
                        FrictionButton(
                            text = "Continue as Guest",
                            onClick = {
                                viewModel.loginAsGuest()
                            },
                            isLoading = isLoading,
                            isSecondary = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )

                        ResponsiveText(
                            text = "Guest mode saves progress on this device. You can link Google anytime later.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        ResponsiveText(
                            text = "By continuing, you agree to Friction's Terms & Privacy Policy.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Error Dialog
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
 * Reusable Benefit Card Item
 */
@Composable
private fun BenefitCardItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color.White.copy(alpha = 0.04f),
        borderColor = Color.White.copy(alpha = 0.09f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FrictionPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FrictionPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                ResponsiveText(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                ResponsiveText(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
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
