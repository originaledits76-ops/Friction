package com.example.core.widgets

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

fun Modifier.glassCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.04f),
    borderColor: Color = Color.White.copy(alpha = 0.10f)
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(1.dp, borderColor, shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.04f),
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Surface(
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

enum class BackgroundStyle {
    DASHBOARD, ANALYTICS, FRICTION_ENGINE, LEADERBOARD, SETTINGS
}

@Composable
fun PremiumBackground(
    style: BackgroundStyle = BackgroundStyle.DASHBOARD
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base background fill
        drawRect(color = DarkBackground)

        when (style) {
            BackgroundStyle.DASHBOARD -> {
                // Soft blurred circles and large translucent gradients
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionPrimary.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.15f),
                        radius = w * 0.8f
                    ),
                    radius = w * 0.8f,
                    center = Offset(w * 0.85f, h * 0.15f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionSecondary.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.85f),
                        radius = w * 0.75f
                    ),
                    radius = w * 0.75f,
                    center = Offset(w * 0.15f, h * 0.85f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionAccent.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.5f
                    ),
                    radius = w * 0.5f,
                    center = Offset(w * 0.5f, h * 0.45f)
                )
            }
            BackgroundStyle.ANALYTICS -> {
                // Subtle graph curves and grid patterns
                val path = Path().apply {
                    moveTo(0f, h * 0.65f)
                    quadraticTo(w * 0.3f, h * 0.4f, w * 0.6f, h * 0.55f)
                    quadraticTo(w * 0.85f, h * 0.7f, w, h * 0.35f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(FrictionPrimary.copy(alpha = 0.18f), Color.Transparent),
                        startY = h * 0.35f,
                        endY = h
                    )
                )
                val gridSpacing = 60.dp.toPx()
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionSecondary.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(w * 0.2f, h * 0.2f),
                        radius = w * 0.6f
                    ),
                    radius = w * 0.6f,
                    center = Offset(w * 0.2f, h * 0.2f)
                )
            }
            BackgroundStyle.FRICTION_ENGINE -> {
                // Soft concentric circles and floating abstract blobs
                for (i in 1..5) {
                    drawCircle(
                        color = FrictionAccent.copy(alpha = 0.06f * (6 - i)),
                        radius = w * 0.22f * i,
                        center = Offset(w * 0.5f, h * 0.38f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionPrimary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * 0.8f, h * 0.7f),
                        radius = w * 0.6f
                    ),
                    radius = w * 0.6f,
                    center = Offset(w * 0.8f, h * 0.7f)
                )
            }
            BackgroundStyle.LEADERBOARD -> {
                // Smooth diagonal gradients and glowing particles
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            DarkBackground,
                            FrictionPrimary.copy(alpha = 0.22f),
                            FrictionSecondary.copy(alpha = 0.15f),
                            DarkBackground
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                // Particle accents
                val particles = listOf(
                    Offset(w * 0.15f, h * 0.2f),
                    Offset(w * 0.8f, h * 0.35f),
                    Offset(w * 0.3f, h * 0.65f),
                    Offset(w * 0.75f, h * 0.8f),
                    Offset(w * 0.5f, h * 0.15f)
                )
                particles.forEach { p ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = 4.dp.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = FrictionPrimary.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = p
                    )
                }
            }
            BackgroundStyle.SETTINGS -> {
                // Minimal geometric pattern with soft gradient highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionPrimary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(w * 0.5f, 0f),
                        radius = w * 0.8f
                    ),
                    radius = w * 0.8f,
                    center = Offset(w * 0.5f, 0f)
                )
                val gridSize = 48.dp.toPx()
                var x = 0f
                while (x < w) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += gridSize
                }
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += gridSize
                }
            }
        }
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    containerColor: Color = DarkCardBg,
    borderColor: Color = Color(0x18FFFFFF),
    elevation: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x99000000),
                spotColor = Color(0xFF000000)
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(content = content)
    }
}

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x80000000),
                spotColor = Color(0x80000000)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16191D),
        border = BorderStroke(1.dp, Color(0x20FFFFFF))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = FrictionPrimary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = FrictionPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun FrictionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isSecondary: Boolean = false,
    isGhost: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Premium spring-based scale animation (target 0.97 on press)
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.8f, // comfortable bounce
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val view = LocalView.current

    Surface(
        onClick = {
            if (!isLoading && enabled) {
                try {
                    // Soft tactile haptic response
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (e: Exception) {
                    // Fail gracefully
                }
                onClick()
            }
        },
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .heightIn(min = 48.dp), // Touch target compliance (minimum 48dp)
        shape = RoundedCornerShape(16.dp), // Premium 16px corner radius
        color = when {
            !enabled -> DarkSurface
            isGhost -> Color.Transparent
            isSecondary -> DarkCardBg
            else -> FrictionPrimary
        },
        border = when {
            !enabled -> BorderStroke(1.dp, Color(0x10FFFFFF))
            isGhost -> null
            isSecondary -> BorderStroke(1.dp, Color(0x15FFFFFF))
            else -> null
        },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = if (isSecondary || isGhost) FrictionPrimary else Color(0xFF111315),
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        icon()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        !enabled -> TextSecondary
                        isGhost -> FrictionPrimary
                        isSecondary -> TextPrimary
                        else -> Color(0xFF111315) // Deep contrast text on Accent Green primary CTA
                    },
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * A beautiful custom mindfulness illustration representing "Mindful Choice" rather than scrolling.
 * Draws an elegant person standing in negative space looking at a blooming glowing tree (life)
 * instead of a glowing distraction screen, adapted for a gorgeous dark aesthetic.
 */
@Composable
fun FrictionIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "illustration")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Radial glow brush for premium dark ambiance
        val greenGrad = Brush.radialGradient(
            colors = listOf(FrictionPrimary.copy(alpha = 0.25f), FrictionPrimary.copy(alpha = 0.0f)),
            center = Offset(width * 0.65f, height * 0.4f),
            radius = width * 0.45f * pulseScale
        )

        // Draw glowing aura behind the tree
        drawCircle(
            brush = greenGrad,
            center = Offset(width * 0.65f, height * 0.4f),
            radius = width * 0.45f * pulseScale
        )

        // Draw grounding hill with sophisticated DarkSurface color
        val groundPath = Path().apply {
            moveTo(0f, height * 0.85f)
            quadraticTo(width * 0.35f, height * 0.8f, width * 0.7f, height * 0.87f)
            quadraticTo(width * 0.9f, height * 0.89f, width, height * 0.82f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = groundPath,
            color = DarkSurface
        )

        // Draw the trunk of the Tree of Life
        val trunkPath = Path().apply {
            moveTo(width * 0.65f, height * 0.83f)
            cubicTo(
                width * 0.63f, height * 0.65f,
                width * 0.62f, height * 0.55f,
                width * 0.64f, height * 0.45f
            )
            quadraticTo(width * 0.52f, height * 0.35f, width * 0.48f, height * 0.33f)
            moveTo(width * 0.64f, height * 0.45f)
            quadraticTo(width * 0.75f, height * 0.37f, width * 0.82f, height * 0.36f)
            moveTo(width * 0.64f, height * 0.45f)
            lineTo(width * 0.65f, height * 0.32f)
        }
        drawPath(
            path = trunkPath,
            color = FrictionPrimary,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw blooming circles on the tree (leaves/fruits representing productive activities)
        drawCircle(
            color = FrictionPrimary,
            radius = 20.dp.toPx() * pulseScale,
            center = Offset(width * 0.48f, height * 0.33f + floatOffset)
        )
        drawCircle(
            color = FrictionSecondary,
            radius = 24.dp.toPx() * (2f - pulseScale),
            center = Offset(width * 0.65f, height * 0.28f - floatOffset)
        )
        drawCircle(
            color = FrictionAccent, // Accent gold fruits
            radius = 10.dp.toPx() * pulseScale,
            center = Offset(width * 0.58f, height * 0.36f + floatOffset)
        )
        drawCircle(
            color = Color(0xFF4ADE80),
            radius = 18.dp.toPx() * pulseScale,
            center = Offset(width * 0.82f, height * 0.36f + floatOffset)
        )

        // Draw elegant human figure standing on the dark ground
        translate(left = width * 0.25f, top = height * 0.62f + floatOffset * 0.3f) {
            // Head
            drawCircle(
                color = TextPrimary,
                radius = 6.dp.toPx(),
                center = Offset(0f, 0f)
            )
            // Torso / Body
            val personBody = Path().apply {
                moveTo(0f, 7.dp.toPx())
                cubicTo(
                    -3.dp.toPx(), 18.dp.toPx(),
                    -5.dp.toPx(), 36.dp.toPx(),
                    -2.dp.toPx(), 50.dp.toPx()
                )
                lineTo(2.dp.toPx(), 50.dp.toPx())
                cubicTo(
                    4.dp.toPx(), 36.dp.toPx(),
                    3.dp.toPx(), 18.dp.toPx(),
                    0f, 7.dp.toPx()
                )
                close()
            }
            drawPath(path = personBody, color = TextPrimary)
        }
    }
}

/**
 * A highly polished, zen-like pulsing spinner representing Focus and Calming flow.
 */
@Composable
fun FrictionSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            // Pulsing background circle
            drawCircle(
                color = FrictionPrimary.copy(alpha = 0.15f * (1.5f - scalePulse)),
                radius = 28.dp.toPx() * scalePulse
            )
            
            // Rotating arc outline representing elegant friction loops
            drawArc(
                color = FrictionPrimary,
                startAngle = rotation,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Static elegant secondary dot
            drawCircle(
                color = FrictionAccent,
                radius = 4.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2)
            )
        }
    }
}

