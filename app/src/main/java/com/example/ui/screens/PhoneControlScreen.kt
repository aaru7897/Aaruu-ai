package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.actions.DeviceAction
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneControlScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val actionFeedback by viewModel.actionFeedback.collectAsState()

    var youtubeQuery by remember { mutableStateOf("") }
    var whatsappPhone by remember { mutableStateOf("") }
    var whatsappMessage by remember { mutableStateOf("") }
    var instagramUsername by remember { mutableStateOf("") }
    var timerMinutes by remember { mutableStateOf("5") }
    var timerLabel by remember { mutableStateOf("Boss Break") }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var googleQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("phone_control_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Phone Automation Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "फोन कंट्रोल • Verified Android intents for YouTube, WhatsApp, Instagram, Timers & Notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!actionFeedback.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionFeedback ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 1. YouTube Search & Music Automation Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                tint = Color(0xFFFF4444)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "YouTube Video & Music Search",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Say \"Aaru YouTube par Arijit Singh songs search karo\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = youtubeQuery,
                        onValueChange = { youtubeQuery = it },
                        label = { Text("Song, artist, or topic (गाना या वीडियो)") },
                        placeholder = { Text("e.g., Lo-fi Bollywood beats") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_query_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Arijit Singh Hits", "90s Hindi Classics", "CarryMinati", "Coke Studio").forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.clickable {
                                    viewModel.executePendingAction(
                                        DeviceAction.YouTubeSearch(preset),
                                        context
                                    )
                                }
                            ) {
                                Text(
                                    text = "▶ $preset",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val q = youtubeQuery.ifBlank { "Trending Hindi Music" }
                            viewModel.executePendingAction(DeviceAction.YouTubeSearch(q), context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_launch_button")
                    ) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search on YouTube (यूट्यूब पर खोजें)")
                    }
                }
            }
        }

        // 2. WhatsApp Message Composer Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color(0xFF25D366)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WhatsApp Smart Messenger",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pre-fills message safely in WhatsApp for Boss's confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = whatsappPhone,
                        onValueChange = { whatsappPhone = it },
                        label = { Text("Phone Number (Optional, e.g. +919876543210)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_phone_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = whatsappMessage,
                        onValueChange = { whatsappMessage = it },
                        label = { Text("Message (संदेश)") },
                        placeholder = { Text("Namaste! Main 10 minute mein pahunch raha hoon.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_message_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (whatsappMessage.isNotBlank()) {
                                viewModel.executePendingAction(
                                    DeviceAction.SendWhatsApp(
                                        phoneNumber = whatsappPhone.trim(),
                                        contactName = whatsappPhone.ifBlank { "Contact" },
                                        messageText = whatsappMessage.trim()
                                    ),
                                    context
                                )
                            }
                        },
                        enabled = whatsappMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color(0xFF04210F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Prepare in WhatsApp", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Instagram Opener & Profile Search Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1306C).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFFE1306C)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Instagram App & Profile Opener",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Open Instagram feed or jump directly to any public profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = instagramUsername,
                        onValueChange = { instagramUsername = it },
                        label = { Text("Instagram Username (Optional, e.g. virat.kohli)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("instagram_username_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.executePendingAction(
                                DeviceAction.OpenInstagram(profileUsername = instagramUsername.trim().removePrefix("@")),
                                context
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE1306C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("instagram_open_button")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (instagramUsername.isBlank()) "Open Instagram App"
                            else "Open @${instagramUsername.trim().removePrefix("@")} on Instagram"
                        )
                    }
                }
            }
        }

        // 4. Timer & Alarm Automation Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Timers & Morning Alarms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Say \"Aaru 10 minute ka timer laga do\" or \"Subah 7 baje alarm lagao\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timerMinutes,
                            onValueChange = { timerMinutes = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Minutes") },
                            singleLine = true,
                            modifier = Modifier.weight(0.4f)
                        )
                        OutlinedTextField(
                            value = timerLabel,
                            onValueChange = { timerLabel = it },
                            label = { Text("Timer Label") },
                            singleLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val mins = timerMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 5
                                viewModel.executePendingAction(
                                    DeviceAction.SetTimer(
                                        seconds = mins * 60,
                                        label = timerLabel.ifBlank { "Aaru Timer" }
                                    ),
                                    context
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_timer_button")
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Timer")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.executePendingAction(
                                    DeviceAction.SetAlarm(hour = 7, minute = 0, label = "Morning Alarm - Boss"),
                                    context
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_alarm_button")
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("7:00 AM Alarm")
                        }
                    }
                }
            }
        }

        // 5. Quick Notes & Reminders Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Quick Notes & Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saves to Aaru's Memory Vault & opens your Notes app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note / Reminder Title") },
                        placeholder = { Text("e.g., Grocery List / Project Deadline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Note Details") },
                        placeholder = { Text("Call client at 4 PM and review PDF...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_content_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (noteContent.isNotBlank()) {
                                val title = noteTitle.ifBlank { "Boss Reminder" }
                                viewModel.addNewMemory(
                                    key = title,
                                    content = noteContent.trim(),
                                    category = "REMINDER",
                                    importance = 4
                                )
                                viewModel.executePendingAction(
                                    DeviceAction.SaveNoteOrReminder(title = title, content = noteContent.trim()),
                                    context
                                )
                                noteTitle = ""
                                noteContent = ""
                            }
                        },
                        enabled = noteContent.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_note_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Note & Reminder")
                    }
                }
            }
        }

        // 6. Quick App Launcher Grid
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "One-Tap App Launcher (ऐप लॉन्चर)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val quickApps = listOf(
                        Triple("WhatsApp", Icons.AutoMirrored.Filled.Send, Color(0xFF25D366)),
                        Triple("YouTube", Icons.Default.PlayCircleFilled, Color(0xFFFF4444)),
                        Triple("Instagram", Icons.Default.PhotoCamera, Color(0xFFE1306C)),
                        Triple("Camera", Icons.Default.CameraAlt, Color(0xFF38BDF8)),
                        Triple("Maps", Icons.Default.Explore, Color(0xFF34D399)),
                        Triple("Settings", Icons.Default.Settings, Color(0xFFA78BFA)),
                        Triple("Phone", Icons.Default.Phone, Color(0xFF60A5FA)),
                        Triple("Chrome", Icons.Default.Public, Color(0xFFFBBF24)),
                        Triple("Spotify", Icons.Default.MusicNote, Color(0xFF1DB954))
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickApps.forEach { (appName, icon, tintColor) ->
                            AppLaunchButton(
                                appName = appName,
                                icon = icon,
                                tintColor = tintColor,
                                onClick = {
                                    viewModel.executePendingAction(
                                        DeviceAction.OpenApp(appName),
                                        context
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 7. Google Search Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Search & Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = googleQuery,
                        onValueChange = { googleQuery = it },
                        label = { Text("Search Google or Maps") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_query_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (googleQuery.isNotBlank()) {
                                    viewModel.executePendingAction(
                                        DeviceAction.WebSearch(googleQuery.trim()),
                                        context
                                    )
                                }
                            },
                            enabled = googleQuery.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Search")
                        }
                        OutlinedButton(
                            onClick = {
                                if (googleQuery.isNotBlank()) {
                                    viewModel.executePendingAction(
                                        DeviceAction.OpenMap(googleQuery.trim()),
                                        context
                                    )
                                }
                            },
                            enabled = googleQuery.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Maps")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLaunchButton(
    appName: String,
    icon: ImageVector,
    tintColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("launch_app_${appName.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = appName,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
