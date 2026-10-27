package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.viewmodel.AssistantVoiceState
import kotlin.math.sin

@Composable
fun AaruVoiceOrb(
    state: AssistantVoiceState,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Continuous wave rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Ripple wave animation for speaking / listening
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val (coreColors, statusText, statusSubtext) = when (state) {
        AssistantVoiceState.IDLE -> Triple(
            listOf(CyanGlow, IndigoAccent, VioletGlow),
            "Tap to talk to Aaru",
            "Full voice control & memory"
        )
        AssistantVoiceState.LISTENING -> Triple(
            listOf(VioletGlow, Color(0xFFEC4899), CyanBright),
            "Aaru is listening...",
            "Speak your command or thought"
        )
        AssistantVoiceState.PROCESSING -> Triple(
            listOf(AmberGlow, IndigoAccent, CyanGlow),
            "Thinking...",
            "Consulting personal memory"
        )
        AssistantVoiceState.SPEAKING -> Triple(
            listOf(CyanBright, IndigoAccent, VioletGlow),
            "Aaru speaking...",
            "Tap orb to interrupt"
        )
        AssistantVoiceState.PAUSED -> Triple(
            listOf(AmberGlow, IndigoAccent, CyanGlow),
            "Listening paused",
            "Tap to resume"
        )
        AssistantVoiceState.ERROR -> Triple(
            listOf(Color(0xFFF87171), AmberGlow, VioletGlow),
            "Attention needed",
            "Tap to retry"
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("aaru_voice_orb"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.58f

                // Audio level boost (0..1)
                val boost = if (state == AssistantVoiceState.LISTENING) {
                    audioLevel.coerceIn(0f, 1f) * 20f
                } else if (state == AssistantVoiceState.SPEAKING) {
                    0.25f * 15f
                } else {
                    0f
                }

                val currentRadius = (baseRadius * pulseScale) + boost

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColors.first().copy(alpha = 0.35f),
                            coreColors.getOrElse(1) { IndigoAccent }.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width / 2f
                    ),
                    radius = size.width / 2f,
                    center = center
                )

                // Expanding ripple waves when active
                if (state == AssistantVoiceState.LISTENING || state == AssistantVoiceState.SPEAKING) {
                    val rippleR = currentRadius + (rippleProgress * 32f)
                    val rippleAlpha = (1f - rippleProgress).coerceIn(0f, 1f) * 0.6f
                    drawCircle(
                        color = coreColors.first().copy(alpha = rippleAlpha),
                        radius = rippleR,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Orbiting wave bars
                val waveCount = 12
                val waveOrbitRadius = currentRadius + 14f
                for (i in 0 until waveCount) {
                    val angleDeg = (i * (360f / waveCount)) + rotationAngle
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val barHeight = 6f + (sin(angleRad * 2 + rotationAngle * 0.05).toFloat() * 6f) + (boost * 0.6f)
                    val startX = center.x + (waveOrbitRadius * kotlin.math.cos(angleRad)).toFloat()
                    val startY = center.y + (waveOrbitRadius * sin(angleRad)).toFloat()
                    val endX = center.x + ((waveOrbitRadius + barHeight) * kotlin.math.cos(angleRad)).toFloat()
                    val endY = center.y + ((waveOrbitRadius + barHeight) * sin(angleRad)).toFloat()

                    drawLine(
                        color = coreColors[i % coreColors.size].copy(alpha = 0.85f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Core radiant orb with gradient
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = coreColors,
                        start = Offset(center.x - currentRadius, center.y - currentRadius),
                        end = Offset(center.x + currentRadius, center.y + currentRadius)
                    ),
                    radius = currentRadius,
                    center = center
                )

                // Inner specular highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(center.x - (currentRadius * 0.3f), center.y - (currentRadius * 0.35f)),
                        radius = currentRadius * 0.6f
                    ),
                    radius = currentRadius * 0.55f,
                    center = Offset(center.x - (currentRadius * 0.25f), center.y - (currentRadius * 0.28f))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = statusText,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = statusSubtext,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}
