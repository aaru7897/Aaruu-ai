package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldStatus
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.viewmodel.AssistantVoiceState
import kotlin.math.sin

@Composable
fun CentralPulsingMicButton(
    voiceState: AssistantVoiceState,
    audioLevel: Float,
    partialSpeech: String,
    isHindi: Boolean,
    userNickname: String = "Boss",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_core_transition")

    val ripple1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )

    val ripple2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, delayMillis = 675, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )

    val hudRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val (glowColor, statusTitle, statusSubtitle) = when (voiceState) {
        AssistantVoiceState.IDLE -> Triple(
            CyanBright,
            if (isHindi) "Haan $userNickname, bolo ❤️ (Tap Mic)" else "Ready $userNickname • Tap Core to Speak",
            if (isHindi) "कहें: 'Aaru, suno!' या 'YouTube kholo'" else "Say: \"Aaru, suno!\" or \"Aaru search music on YouTube\""
        )
        AssistantVoiceState.LISTENING -> Triple(
            CyanGlow,
            if (isHindi) "आरू सुन रही है, $userNickname..." else "Aaru is listening, $userNickname...",
            if (isHindi) "हिंदी, हिंग्लिश या इंग्लिश में बोलें" else "Speak in Hindi, Hinglish, or English"
        )
        AssistantVoiceState.PAUSED -> Triple(
            AmberGlow,
            if (isHindi) "माइक पॉज़ है (Paused)" else "Listening Paused",
            if (isHindi) "दोबारा शुरू करने के लिए Resume दबाएं" else "Tap Resume or Core to listen again"
        )
        AssistantVoiceState.PROCESSING -> Triple(
            AmberGlow,
            if (isHindi) "सोच रही हूँ, $userNickname..." else "Processing AI Brain...",
            if (isHindi) "जवाब और फोन कमांड तैयार हो रहा है" else "Generating response & checking memory"
        )
        AssistantVoiceState.SPEAKING -> Triple(
            EmeraldStatus,
            if (isHindi) "आरू बोल रही है..." else "Aaru is speaking...",
            if (isHindi) "रोकने के लिए कोर या Stop दबाएं" else "Tap Core or Stop Speaking to interrupt"
        )
        AssistantVoiceState.ERROR -> Triple(
            RoseAlert,
            if (isHindi) "माइक या नेटवर्क रुकावट" else "Voice State Attention",
            if (isHindi) "दोबारा कोशिश करने के लिए टैप करें" else "Tap Core to retry"
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("central_pulsing_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(164.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = 44.dp.toPx()

                // Ambient JARVIS glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (voiceState == AssistantVoiceState.LISTENING) 0.38f else 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 78.dp.toPx()
                    ),
                    radius = 78.dp.toPx(),
                    center = center
                )

                // Rotating futuristic JARVIS HUD ring segments
                val hudRadius = 60.dp.toPx()
                val arcSize = Size(hudRadius * 2, hudRadius * 2)
                val topLeft = Offset(center.x - hudRadius, center.y - hudRadius)
                for (i in 0 until 4) {
                    drawArc(
                        color = glowColor.copy(alpha = 0.55f),
                        startAngle = hudRotation + (i * 90f),
                        sweepAngle = 48f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Expanding shockwaves when listening or speaking
                if (voiceState == AssistantVoiceState.LISTENING || voiceState == AssistantVoiceState.SPEAKING) {
                    val r1 = baseRadius + (ripple1 * 34.dp.toPx()) + (audioLevel * 12.dp.toPx())
                    val alpha1 = (1f - ripple1).coerceIn(0f, 1f) * 0.75f
                    drawCircle(
                        color = glowColor.copy(alpha = alpha1),
                        radius = r1,
                        center = center,
                        style = Stroke(width = 2.8.dp.toPx())
                    )

                    val r2 = baseRadius + (ripple2 * 34.dp.toPx())
                    val alpha2 = (1f - ripple2).coerceIn(0f, 1f) * 0.5f
                    drawCircle(
                        color = glowColor.copy(alpha = alpha2),
                        radius = r2,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Core Circular AI Orb Button
            val dynamicScale = if (voiceState == AssistantVoiceState.LISTENING || voiceState == AssistantVoiceState.SPEAKING) {
                pulseScale + (audioLevel * 0.08f)
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .size(86.dp * dynamicScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = when (voiceState) {
                                AssistantVoiceState.IDLE -> listOf(CyanBright, IndigoAccent)
                                AssistantVoiceState.LISTENING -> listOf(CyanBright, Color(0xFF00E5FF))
                                AssistantVoiceState.PAUSED -> listOf(AmberGlow, SurfaceCard)
                                AssistantVoiceState.PROCESSING -> listOf(AmberGlow, VioletGlow)
                                AssistantVoiceState.SPEAKING -> listOf(EmeraldStatus, CyanGlow)
                                AssistantVoiceState.ERROR -> listOf(RoseAlert, AmberGlow)
                            }
                        )
                    )
                    .border(
                        width = 2.5.dp,
                        color = Color.White.copy(alpha = 0.65f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (voiceState) {
                        AssistantVoiceState.SPEAKING -> Icons.Default.Stop
                        AssistantVoiceState.LISTENING -> Icons.Default.Hearing
                        AssistantVoiceState.PAUSED -> Icons.Default.Pause
                        AssistantVoiceState.ERROR -> Icons.Default.ErrorOutline
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "JARVIS AI Core Microphone",
                    tint = ObsidianBg,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Audio-reactive Waveform Bars
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(22.dp)
                .padding(vertical = 2.dp)
        ) {
            val barCount = 13
            for (i in 0 until barCount) {
                val active = voiceState == AssistantVoiceState.LISTENING || voiceState == AssistantVoiceState.SPEAKING
                val phase = i * 0.45
                val h = if (active) {
                    (6f + (sin(phase + ripple1 * 6.28).toFloat() * 8f) + (audioLevel * 12f)).coerceIn(4f, 20f)
                } else {
                    4f
                }
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) glowColor else glowColor.copy(alpha = 0.3f))
                )
                if (i < barCount - 1) Spacer(modifier = Modifier.width(3.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusTitle,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = statusSubtitle,
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        // Live Recognized Speech Transcript Pill
        if (partialSpeech.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(SurfaceCard, RoundedCornerShape(12.dp))
                    .border(1.dp, glowColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("live_speech_transcript")
            ) {
                Text(
                    text = "🎤 \"$partialSpeech\"",
                    color = CyanBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
