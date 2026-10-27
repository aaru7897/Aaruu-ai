package com.example.actions

sealed interface DeviceAction {
    data class SendWhatsApp(
        val contactName: String? = null,
        val phoneNumber: String? = null,
        val messageText: String
    ) : DeviceAction

    data class YouTubeSearch(
        val query: String
    ) : DeviceAction

    data class OpenInstagram(
        val profileUsername: String? = null
    ) : DeviceAction

    data class SetTimer(
        val seconds: Int,
        val label: String? = null
    ) : DeviceAction

    data class SetAlarm(
        val hour: Int? = null,
        val minute: Int? = null,
        val label: String? = null
    ) : DeviceAction

    data class SaveNoteOrReminder(
        val title: String,
        val content: String,
        val isReminder: Boolean = false
    ) : DeviceAction

    data class OpenApp(
        val appName: String,
        val packageName: String? = null,
        val fallbackUrl: String? = null
    ) : DeviceAction

    data class WebSearch(
        val query: String,
        val searchEngine: String = "Google"
    ) : DeviceAction

    data class MakeCall(
        val phoneNumber: String,
        val contactName: String? = null
    ) : DeviceAction

    data class OpenMap(
        val destination: String
    ) : DeviceAction

    data class AddMemory(
        val key: String,
        val content: String,
        val category: String = "PERSONAL",
        val importance: Int = 4
    ) : DeviceAction

    data class ControlCommand(
        val command: String // "STOP", "REPEAT", "PAUSE", "RESUME"
    ) : DeviceAction

    data object None : DeviceAction
}

data class ActionExecutionResult(
    val success: Boolean,
    val feedbackMessage: String,
    val appOpened: String? = null
)
