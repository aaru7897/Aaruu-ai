package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AssistantViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val voiceModeEnabled by viewModel.voiceModeEnabled.collectAsState()
    val continuousModeEnabled by viewModel.continuousModeEnabled.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val backgroundServiceActive by viewModel.backgroundServiceActive.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val brainConfig by viewModel.brainConfig.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val allMemories by viewModel.allMemories.collectAsState()

    var nicknameInput by remember(brainConfig.userNickname) { mutableStateOf(brainConfig.userNickname) }
    var customApiUrl by remember(brainConfig.customEndpointUrl) { mutableStateOf(brainConfig.customEndpointUrl) }
    var customApiKey by remember(brainConfig.customApiKey) { mutableStateOf(brainConfig.customApiKey) }
    var customModelName by remember(brainConfig.modelName) { mutableStateOf(brainConfig.modelName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Voice, Brain & Wake-Word Config",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "आवाज़, एआई मॉडल, वेक-वर्ड और बैकग्राउंड कंट्रोल",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Background Service & "Aaru, suno!" Wake-Word Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (backgroundServiceActive)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (backgroundServiceActive) Color(0xFF34D399) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Wake-Word Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (backgroundServiceActive)
                                    "ACTIVE • Say \"Aaru, suno!\" or \"Aaru search music on YouTube\""
                                else
                                    "STOPPED • Turn on to keep Aaru listening via Foreground Notification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = backgroundServiceActive,
                            onCheckedChange = { viewModel.toggleBackgroundService() },
                            modifier = Modifier.testTag("background_service_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Wake Phrase (\"Aaru, suno!\" / \"Aaru\")",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "When ON, background mode responds only when speech includes \"Aaru\" or \"आरू\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Android Background & Battery Notice: Android requires a visible persistent notification while microphone listening is active in the background. You can stop Aaru anytime from the notification's 'Stop Aaru' button or the Core screen. For uninterrupted background operation on OEM phones (Xiaomi/Samsung/Realme), set Battery Usage to 'Unrestricted' in App Info.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Voice-to-Voice & Speech Synthesis Controls (Voice Selection, Speed, Pitch)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice-to-Voice & TTS Customization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Speak All Responses (आवाज़ में जवाब)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aaru automatically speaks every response aloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = voiceModeEnabled,
                            onCheckedChange = { viewModel.toggleVoiceMode() },
                            modifier = Modifier.testTag("voice_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continuous Conversation Loop (लगातार बातचीत)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically resumes listening 450ms after Aaru finishes speaking",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = continuousModeEnabled,
                            onCheckedChange = { viewModel.toggleContinuousMode() },
                            modifier = Modifier.testTag("continuous_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Select Voice Profile (आवाज़ चुनें - Female / Male / Accent)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val voiceList = if (availableVoices.isEmpty()) {
                            listOf(
                                Triple("default_indian_female", "Aaru (Pleasant Female • hi-IN)", true)
                            )
                        } else {
                            availableVoices.take(6).map { Triple(it.name, it.displayName, it.isFemalePreferred) }
                        }

                        voiceList.forEach { (vName, vLabel, _) ->
                            val isSelected = selectedVoiceName == vName
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectVoice(vName) },
                                label = { Text(vLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Speaking Speed (बोलने की गति)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${(speechRate * 100).roundToInt() / 100f}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.6f..1.6f,
                        modifier = Modifier.testTag("speech_rate_slider")
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Voice Pitch & Warmth (आवाज़ की टोन)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${(speechPitch * 100).roundToInt() / 100f}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = { viewModel.setSpeechPitch(it) },
                        valueRange = 0.7f..1.4f,
                        modifier = Modifier.testTag("speech_pitch_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.speakText(
                                "Haan ${brainConfig.userNickname}, meri aawaaz aur speed ab bilkul set hai! Batao aaj kya karna hai?"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Aaru's Voice Output")
                    }
                }
            }
        }

        // 3. Identity, Nickname, Language & Response Length Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Identity, Language & Response Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text("Your Nickname (Default: Boss)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nickname_input")
                        )
                        Button(
                            onClick = {
                                val updated = nicknameInput.trim().ifBlank { "Boss" }
                                viewModel.updateBrainConfig(brainConfig.copy(userNickname = updated))
                            }
                        ) {
                            Text("Save")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Primary Speech & Conversation Language",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "hi" to "हिंदी (Hindi)",
                            "hinglish" to "Hinglish",
                            "en" to "English"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = selectedLanguage == code,
                                onClick = { viewModel.setLanguage(code) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_lang_$code")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Response Length (जवाब की लंबाई)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "SHORT" to "Short (Fast Voice)",
                            "BALANCED" to "Balanced",
                            "DETAILED" to "Detailed"
                        ).forEach { (lenCode, lenLabel) ->
                            FilterChip(
                                selected = brainConfig.responseLength == lenCode,
                                onClick = {
                                    viewModel.updateBrainConfig(brainConfig.copy(responseLength = lenCode))
                                },
                                label = { Text(lenLabel) }
                            )
                        }
                    }
                }
            }
        }

        // 4. AI Brain Provider & Model Configuration (Gemini / Groq / OpenAI-Compatible)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI Brain Provider & Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Switch between Firebase Gemini AI Logic or Custom Groq/OpenAI Endpoint",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = brainConfig.provider == "FIREBASE_GEMINI",
                            onClick = {
                                viewModel.updateBrainConfig(
                                    brainConfig.copy(
                                        provider = "FIREBASE_GEMINI",
                                        modelName = "gemini-2.5-flash"
                                    )
                                )
                            },
                            label = { Text("Firebase Gemini (Recommended)") }
                        )
                        FilterChip(
                            selected = brainConfig.provider == "GROQ_COMPATIBLE",
                            onClick = {
                                viewModel.updateBrainConfig(
                                    brainConfig.copy(
                                        provider = "GROQ_COMPATIBLE",
                                        modelName = "llama-3.3-70b-versatile"
                                    )
                                )
                            },
                            label = { Text("Groq / OpenAI REST API") }
                        )
                    }

                    if (brainConfig.provider == "FIREBASE_GEMINI") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select Gemini Model:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "gemini-2.5-flash" to "Gemini 2.5 Flash (Balanced)",
                                "gemini-2.5-flash-lite" to "Gemini 2.5 Flash-Lite (Ultra Fast)"
                            ).forEach { (modelId, label) ->
                                FilterChip(
                                    selected = brainConfig.modelName == modelId,
                                    onClick = {
                                        viewModel.updateBrainConfig(brainConfig.copy(modelName = modelId))
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customApiUrl,
                            onValueChange = { customApiUrl = it },
                            label = { Text("Endpoint URL (Groq / OpenAI / OpenRouter)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customModelName,
                            onValueChange = { customModelName = it },
                            label = { Text("Model ID (e.g., llama-3.3-70b-versatile)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            label = { Text("API Key (Stored in session memory only)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateBrainConfig(
                                    brainConfig.copy(
                                        customEndpointUrl = customApiUrl.trim(),
                                        modelName = customModelName.trim(),
                                        customApiKey = customApiKey.trim()
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Custom API Configuration")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Security Note: Custom API keys entered here are held in local runtime memory and are never logged or transmitted to third parties.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Data & Conversation Reset Controls
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conversation & Memory Reset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stored Memories: ${allMemories.size} items in Room SQLite Vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.startNewConversation() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_chat_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Chat")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearAllMemories() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_memories_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Memories")
                        }
                    }
                }
            }
        }
    }
}
