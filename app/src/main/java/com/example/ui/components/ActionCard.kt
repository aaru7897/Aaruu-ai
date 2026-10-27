package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.DeviceAction
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldStatus
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow

@Composable
fun ActionCard(
    action: DeviceAction,
    onExecute: (DeviceAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (action is DeviceAction.None || action is DeviceAction.ControlCommand) return

    // Allow Boss to review & edit WhatsApp message before sending (Section 7 & 8)
    var editableWaMessage by remember(action) {
        mutableStateOf((action as? DeviceAction.SendWhatsApp)?.messageText.orEmpty())
    }

    val (title, description, icon, iconColor) = when (action) {
        is DeviceAction.YouTubeSearch -> Quadruple(
            "YouTube Search / यूट्यूब सर्च",
            "Search & play on YouTube: \"${action.query}\"",
            Icons.Default.PlayArrow,
            Color(0xFFEF4444)
        )
        is DeviceAction.SendWhatsApp -> Quadruple(
            "Review WhatsApp Message / मैसेज रिव्यू",
            "To: ${action.contactName ?: action.phoneNumber ?: "Select Contact in WhatsApp"}",
            Icons.Default.Send,
            EmeraldStatus
        )
        is DeviceAction.OpenInstagram -> Quadruple(
            "Instagram / इंस्टाग्राम",
            if (!action.profileUsername.isNullOrBlank()) "Open profile: @${action.profileUsername}" else "Launch Instagram App",
            Icons.Default.PhotoCamera,
            Color(0xFFEC4899)
        )
        is DeviceAction.SetTimer -> Quadruple(
            "Set Timer / टाइमर",
            "Timer for ${action.seconds} seconds (${action.label ?: "Aaru Timer"})",
            Icons.Default.Timer,
            CyanBright
        )
        is DeviceAction.SetAlarm -> Quadruple(
            "Set Alarm / अलार्म",
            "Alarm for ${action.hour ?: 7}:${String.format("%02d", action.minute ?: 0)} - ${action.label ?: "Alarm"}",
            Icons.Default.Alarm,
            AmberGlow
        )
        is DeviceAction.SaveNoteOrReminder -> Quadruple(
            if (action.isReminder) "Reminder Saved / रिमाइंडर" else "Note Saved / नोट",
            "\"${action.content}\"",
            Icons.Default.EditNote,
            VioletGlow
        )
        is DeviceAction.OpenApp -> Quadruple(
            "Open Application / ऐप खोलें",
            "Launch ${action.appName} on your device",
            Icons.Default.Apps,
            CyanBright
        )
        is DeviceAction.WebSearch -> Quadruple(
            "Google Search / वेब सर्च",
            "Search for: \"${action.query}\"",
            Icons.Default.Search,
            AmberGlow
        )
        is DeviceAction.MakeCall -> Quadruple(
            "Phone Dialer / कॉल",
            "Open dialer for ${action.contactName ?: action.phoneNumber}",
            Icons.Default.Call,
            EmeraldStatus
        )
        is DeviceAction.OpenMap -> Quadruple(
            "Directions & Maps / नेविगेशन",
            "Navigate to ${action.destination}",
            Icons.Default.Map,
            VioletGlow
        )
        is DeviceAction.AddMemory -> Quadruple(
            "Save to Memory / याद रखें",
            "Remember: ${action.content}",
            Icons.Default.Psychology,
            VioletGlow
        )
        else -> Quadruple("", "", Icons.Default.Apps, CyanGlow)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCardElevated, RoundedCornerShape(18.dp))
            .border(1.5.dp, iconColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("action_confirmation_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        if (action is DeviceAction.SendWhatsApp) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = editableWaMessage,
                onValueChange = { editableWaMessage = it },
                label = { Text("Review / Edit Message before sending", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = EmeraldStatus
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                modifier = Modifier.testTag("dismiss_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    val finalAction = if (action is DeviceAction.SendWhatsApp) {
                        action.copy(messageText = editableWaMessage)
                    } else {
                        action
                    }
                    onExecute(finalAction)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("execute_action_button")
            ) {
                Text("Confirm & Run / चलाएं", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
