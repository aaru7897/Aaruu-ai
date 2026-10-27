package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.ConversationEntity
import com.example.ui.components.ActionCard
import com.example.ui.components.CentralPulsingMicButton
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldStatus
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.viewmodel.AssistantViewModel
import com.example.ui.viewmodel.AssistantVoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val partialSpeech by viewModel.partialSpeech.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val voiceModeEnabled by viewModel.voiceModeEnabled.collectAsState()
    val continuousModeEnabled by viewModel.continuousModeEnabled.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val backgroundActive by viewModel.backgroundServiceActive.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()
    val brainConfig by viewModel.brainConfig.collectAsState()

    val isHindi = selectedLanguage == "hi" || selectedLanguage == "hinglish"

    var textInput by remember { mutableStateOf("") }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startListening()
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Online / Offline HUD Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) EmeraldStatus else RoseAlert)
                                .testTag("online_status_indicator")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AARU AI",
                                    color = CyanBright,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentEmotion.emoji} ${if (isHindi) currentEmotion.labelHindi else currentEmotion.labelEnglish}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = if (isOnline) {
                                    "Online • ${brainConfig.userNickname}'s Personal JARVIS"
                                } else {
                                    "Offline Mode • On-Device Brain Active"
                                },
                                color = if (isOnline) EmeraldStatus else AmberGlow,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Cycle Language: Hindi -> Hinglish -> English
                    FilterChip(
                        selected = true,
                        onClick = {
                            val nextLang = when (selectedLanguage) {
                                "hi" -> "hinglish"
                                "hinglish" -> "en"
                                else -> "hi"
                            }
                            viewModel.setLanguage(nextLang)
                        },
                        label = {
                            Text(
                                text = when (selectedLanguage) {
                                    "hi" -> "🇮🇳 हिन्दी"
                                    "hinglish" -> "🇮🇳 Hinglish"
                                    else -> "🇬🇧 EN"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurfaceCardElevated,
                            selectedLabelColor = CyanBright
                        ),
                        modifier = Modifier.testTag("language_switch_chip")
                    )

                    // Auto-Speak Toggle
                    IconButton(
                        onClick = { viewModel.toggleVoiceMode() },
                        modifier = Modifier.testTag("toggle_voice_mode_button")
                    ) {
                        Icon(
                            imageVector = if (voiceModeEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Spoken Responses",
                            tint = if (voiceModeEnabled) CyanBright else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // New Conversation
                    IconButton(
                        onClick = { viewModel.startNewConversation() },
                        modifier = Modifier.testTag("new_conversation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "New Conversation",
                            tint = CyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Clear Chat
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Visible Start/Stop Assistant & Background Service Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start / Stop Assistant Button
                val isAssistantActive = voiceState == AssistantVoiceState.LISTENING || voiceState == AssistantVoiceState.SPEAKING
                OutlinedButton(
                    onClick = {
                        if (isAssistantActive) {
                            viewModel.stopAssistantCompletely()
                        } else {
                            if (hasMicPermission) {
                                viewModel.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isAssistantActive) RoseAlert else EmeraldStatus
                    ),
                    modifier = Modifier.testTag("start_stop_assistant_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isAssistantActive) "Stop Assistant" else "Start Assistant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Background Service Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (backgroundActive) "BG Service: ON" else "BG Service: OFF",
                        color = if (backgroundActive) EmeraldStatus else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = backgroundActive,
                        onCheckedChange = { viewModel.toggleBackgroundService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldStatus,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceCard
                        ),
                        modifier = Modifier.testTag("toggle_background_service_switch")
                    )
                }
            }

            // Error / Permission Diagnostic Banner
            AnimatedVisibility(visible = errorBanner != null) {
                errorBanner?.let { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RoseAlert.copy(alpha = 0.18f))
                            .border(1.dp, RoseAlert.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚠️ $err",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissErrorBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextPrimary)
                        }
                    }
                }
            }

            // CENTRAL JARVIS AI CORE & VOICE CONTROL DECK
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CentralPulsingMicButton(
                    voiceState = voiceState,
                    audioLevel = audioLevel,
                    partialSpeech = partialSpeech,
                    isHindi = isHindi,
                    userNickname = brainConfig.userNickname,
                    onClick = {
                        if (voiceState == AssistantVoiceState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else if (voiceState == AssistantVoiceState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            if (hasMicPermission) {
                                viewModel.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Voice Control Deck: Stop Speaking | Repeat Answer | Pause/Resume | Continuous Loop
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop Speaking
                    SuggestionChip(
                        onClick = { viewModel.stopSpeaking() },
                        icon = {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                tint = RoseAlert,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text("Stop Speaking", fontSize = 11.sp, color = TextPrimary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceCard),
                        modifier = Modifier.testTag("stop_speaking_button")
                    )

                    // Repeat Last Answer
                    SuggestionChip(
                        onClick = { viewModel.repeatLastAnswer() },
                        icon = {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text("Repeat", fontSize = 11.sp, color = TextPrimary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceCard),
                        modifier = Modifier.testTag("repeat_answer_button")
                    )

                    // Pause / Resume Listening
                    val isPaused = voiceState == AssistantVoiceState.PAUSED
                    SuggestionChip(
                        onClick = {
                            if (isPaused || voiceState == AssistantVoiceState.IDLE) {
                                if (hasMicPermission) viewModel.resumeListening()
                                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.pauseListening()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = AmberGlow,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (isPaused) "Resume Mic" else "Pause Mic",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceCard),
                        modifier = Modifier.testTag("pause_resume_listening_button")
                    )

                    // Continuous Conversation Toggle
                    FilterChip(
                        selected = continuousModeEnabled,
                        onClick = { viewModel.toggleContinuousMode() },
                        label = {
                            Text(
                                text = if (continuousModeEnabled) "Loop: ON" else "Loop: OFF",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VioletGlow.copy(alpha = 0.28f),
                            selectedLabelColor = CyanBright,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("continuous_mode_chip")
                    )
                }
            }

            // Quick Hindi & Hinglish Voice Command Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "🎙️ Aaru, suno!",
                    "▶️ Aaru, search music on YouTube",
                    "📺 Aaru, YouTube kholo",
                    "⏱️ Aaru, 5 minute ka timer lagao",
                    "📝 Aaru, notes mein likho: Kal meeting hai",
                    "💬 Aaru, WhatsApp kholo",
                    "📸 Aaru, Instagram kholo",
                    "🌐 Aaru, is text ko English mein translate karo: मुझे कोडिंग पसंद है",
                    "🧠 Aaru, tumhe mere baare mein kya yaad hai?"
                )

                items(suggestions) { suggestion ->
                    val cleanPrompt = suggestion.substring(3).trim()
                    SuggestionChip(
                        onClick = { viewModel.processPrompt(cleanPrompt, fromVoice = false) },
                        label = { Text(suggestion, fontSize = 11.sp, color = TextPrimary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SurfaceCard
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = CyanGlow.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("suggestion_chip_${cleanPrompt.take(10)}")
                    )
                }
            }

            // Pending Action Confirmation Card (including WhatsApp message review)
            AnimatedVisibility(
                visible = pendingAction != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                pendingAction?.let { action ->
                    ActionCard(
                        action = action,
                        onExecute = { updatedAction -> viewModel.executePendingAction(updatedAction, context) },
                        onDismiss = { viewModel.dismissPendingAction() }
                    )
                }
            }

            // Conversation History Timeline
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        userNickname = brainConfig.userNickname,
                        onSpeakAgain = { viewModel.speakText(message.content) }
                    )
                }

                if (voiceState == AssistantVoiceState.PROCESSING) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(SurfaceCard, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanBright,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Aaru AI jawab taiyar kar rahi hai, ${brainConfig.userNickname}...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Bottom Input Bar with Mic & Text Fallback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            when (voiceState) {
                                AssistantVoiceState.LISTENING -> CyanBright
                                AssistantVoiceState.SPEAKING -> EmeraldStatus
                                else -> CyanGlow
                            }
                        )
                        .clickable {
                            if (voiceState == AssistantVoiceState.SPEAKING) {
                                viewModel.stopSpeaking()
                            } else if (voiceState == AssistantVoiceState.LISTENING) {
                                viewModel.stopListening()
                            } else {
                                if (hasMicPermission) {
                                    viewModel.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("bottom_voice_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceState == AssistantVoiceState.SPEAKING) Icons.Default.Stop
                        else if (voiceState == AssistantVoiceState.LISTENING) Icons.Default.Hearing
                        else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = ObsidianBg,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = if (voiceState == AssistantVoiceState.LISTENING) {
                                "Bolo ${brainConfig.userNickname}, main sun rahi hoon..."
                            } else {
                                "Bolo 'Aaru, suno!' ya yahan type karo..."
                            },
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_text_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.processPrompt(textInput, fromVoice = false)
                                textInput = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.processPrompt(textInput, fromVoice = false)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.testTag("assistant_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = CyanBright
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ConversationEntity,
    userNickname: String = "Boss",
    onSpeakAgain: () -> Unit
) {
    val isUser = message.role == "user"
    val isAction = message.role == "action"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) SurfaceCardElevated else if (isAction) Color(0xFF064E3B) else SurfaceCard
    val borderColor = if (isUser) CyanGlow.copy(alpha = 0.45f) else if (isAction) EmeraldStatus.copy(alpha = 0.55f) else VioletGlow.copy(alpha = 0.35f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .fillMaxWidth(if (isAction) 1f else 0.88f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isUser) "$userNickname (You)" else if (isAction) "⚡ Phone Automation" else "🤖 Aaru AI",
                        color = if (isUser) CyanBright else if (isAction) EmeraldStatus else VioletGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isUser && !isAction) {
                        IconButton(
                            onClick = onSpeakAgain,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak Aloud",
                                tint = CyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (!message.actionType.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Module: ${message.actionType}",
                        color = AmberGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
