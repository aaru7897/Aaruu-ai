package com.example.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DeviceActionHandler {
    private const val TAG = "DeviceActionHandler"

    private val KNOWN_APP_PACKAGES = mapOf(
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "youtube" to "com.google.android.youtube",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "camera" to "camera_intent",
        "gallery" to "gallery_intent",
        "photos" to "com.google.android.apps.photos",
        "spotify" to "com.spotify.music",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "alarm" to "com.google.android.deskclock",
        "gmail" to "com.google.android.gm",
        "mail" to "com.google.android.gm",
        "settings" to "settings_intent",
        "calendar" to "com.google.android.calendar",
        "notes" to "com.google.android.keep",
        "play store" to "com.android.vending"
    )

    fun execute(context: Context, action: DeviceAction): ActionExecutionResult {
        return try {
            when (action) {
                is DeviceAction.YouTubeSearch -> handleYouTubeSearch(context, action)
                is DeviceAction.SendWhatsApp -> handleSendWhatsApp(context, action)
                is DeviceAction.OpenInstagram -> handleOpenInstagram(context, action)
                is DeviceAction.SetTimer -> handleSetTimer(context, action)
                is DeviceAction.SetAlarm -> handleSetAlarm(context, action)
                is DeviceAction.SaveNoteOrReminder -> ActionExecutionResult(
                    success = true,
                    feedbackMessage = if (action.isReminder) {
                        "Boss, reminder save ho gaya hai Memory Vault mein: \"${action.content}\""
                    } else {
                        "Boss, note save kar liya hai Memory Vault mein: \"${action.content}\""
                    },
                    appOpened = "Memory Vault"
                )
                is DeviceAction.OpenApp -> handleOpenApp(context, action)
                is DeviceAction.WebSearch -> handleWebSearch(context, action)
                is DeviceAction.MakeCall -> handleMakeCall(context, action)
                is DeviceAction.OpenMap -> handleOpenMap(context, action)
                is DeviceAction.AddMemory -> ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Boss, maine yeh yaad rakh liya: ${action.content}"
                )
                is DeviceAction.ControlCommand -> ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Command executed: ${action.command}"
                )
                DeviceAction.None -> ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Ready Boss!"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute action: $action", e)
            ActionExecutionResult(
                success = false,
                feedbackMessage = "Boss, woh action complete nahi ho paya: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun handleYouTubeSearch(context: Context, action: DeviceAction.YouTubeSearch): ActionExecutionResult {
        val query = action.query.trim().ifBlank { "music" }
        val encodedQuery = Uri.encode(query)

        val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val hasYouTube = context.packageManager
            .queryIntentActivities(searchIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()

        return if (hasYouTube) {
            context.startActivity(searchIntent)
            ActionExecutionResult(
                success = true,
                feedbackMessage = "Boss, YouTube par \"$query\" search kar diya hai!",
                appOpened = "YouTube"
            )
        } else {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ActionExecutionResult(
                success = true,
                feedbackMessage = "Boss, YouTube browser mein \"$query\" khol diya hai.",
                appOpened = "YouTube Web"
            )
        }
    }

    private fun handleOpenInstagram(context: Context, action: DeviceAction.OpenInstagram): ActionExecutionResult {
        val cleanProfile = action.profileUsername?.trim()?.removePrefix("@").orEmpty()
        return try {
            if (cleanProfile.isNotBlank()) {
                val appUri = Uri.parse("http://instagram.com/_u/$cleanProfile")
                val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
                    setPackage("com.instagram.android")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val hasIgApp = context.packageManager
                    .queryIntentActivities(appIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    .isNotEmpty()

                if (hasIgApp) {
                    context.startActivity(appIntent)
                    ActionExecutionResult(
                        success = true,
                        feedbackMessage = "Boss, Instagram par @$cleanProfile ki profile khol di hai.",
                        appOpened = "Instagram"
                    )
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.instagram.com/$cleanProfile/")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                    ActionExecutionResult(
                        success = true,
                        feedbackMessage = "Instagram app nahi mila Boss, browser mein @$cleanProfile khol diya hai.",
                        appOpened = "Browser"
                    )
                }
            } else {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    ActionExecutionResult(
                        success = true,
                        feedbackMessage = "Boss, Instagram khol diya hai!",
                        appOpened = "Instagram"
                    )
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.instagram.com/")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                    ActionExecutionResult(
                        success = true,
                        feedbackMessage = "Instagram app installed nahi hai Boss, Instagram Web khol diya hai.",
                        appOpened = "Instagram Web"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Instagram launch error", e)
            ActionExecutionResult(
                success = false,
                feedbackMessage = "Boss, Instagram open nahi ho paya: ${e.message}"
            )
        }
    }

    private fun handleSetTimer(context: Context, action: DeviceAction.SetTimer): ActionExecutionResult {
        val durationSec = action.seconds.coerceAtLeast(1)
        val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSec)
            action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            val resolved = context.packageManager
                .queryIntentActivities(timerIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .isNotEmpty()
            if (resolved) {
                context.startActivity(timerIntent)
                val mins = durationSec / 60
                val secs = durationSec % 60
                val timeDesc = if (mins > 0) "$mins min ${if (secs > 0) "$secs sec" else ""}".trim() else "$secs seconds"
                ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Boss, Clock app mein $timeDesc ka timer set karne ke liye khol diya hai.",
                    appOpened = "Clock"
                )
            } else {
                ActionExecutionResult(
                    success = false,
                    feedbackMessage = "Boss, is device par koi compatible Clock/Timer app nahi mila jo timer intent handle kar sake."
                )
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, "Boss, timer start nahi ho saka: ${e.message}")
        }
    }

    private fun handleSendWhatsApp(context: Context, action: DeviceAction.SendWhatsApp): ActionExecutionResult {
        val encodedMessage = try {
            URLEncoder.encode(action.messageText, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            action.messageText
        }

        val recipientNumber = action.phoneNumber?.filter { it.isDigit() || it == '+' }

        val intent = if (!recipientNumber.isNullOrBlank()) {
            val url = "https://api.whatsapp.com/send?phone=$recipientNumber&text=$encodedMessage"
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, action.messageText)
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return try {
            val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (activities.isNotEmpty()) {
                context.startActivity(intent)
                val targetDesc = action.contactName ?: action.phoneNumber ?: "WhatsApp"
                ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Boss, $targetDesc ke liye WhatsApp message ready kar diya hai. Aap review karke send dabao.",
                    appOpened = "WhatsApp"
                )
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Boss, WhatsApp app installed nahi hai, isliye browser mein WhatsApp Web khola hai.",
                    appOpened = "Browser"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp dispatch failed", e)
            ActionExecutionResult(
                success = false,
                feedbackMessage = "Failed to launch WhatsApp: ${e.message}"
            )
        }
    }

    private fun handleOpenApp(context: Context, action: DeviceAction.OpenApp): ActionExecutionResult {
        val appQuery = action.appName.lowercase().trim()

        if (appQuery == "instagram" || appQuery.startsWith("instagram")) {
            return handleOpenInstagram(context, DeviceAction.OpenInstagram(null))
        }

        if (appQuery == "camera" || appQuery == "कैमरा") {
            val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                return ActionExecutionResult(true, "Boss, Camera khol diya hai.", "Camera")
            }
        }

        if (appQuery == "settings" || appQuery == "सेटिंग्स") {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return ActionExecutionResult(true, "Boss, Phone Settings khol di hain.", "Settings")
        }

        val targetPackage = action.packageName
            ?: KNOWN_APP_PACKAGES[appQuery]
            ?: findPackageByQuery(context, appQuery)

        if (targetPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Boss, ${action.appName} open kar diya hai.",
                    appOpened = action.appName
                )
            }
        }

        return if (!action.fallbackUrl.isNullOrBlank()) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(action.fallbackUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ActionExecutionResult(true, "Opened ${action.appName} in browser.", action.appName)
        } else {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://search?q=${Uri.encode(action.appName)}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(playStoreIntent)
                ActionExecutionResult(
                    success = false,
                    feedbackMessage = "Boss, ${action.appName} aapke phone mein installed nahi mila. Play Store search khol diya hai.",
                    appOpened = "Play Store"
                )
            } catch (e: Exception) {
                val webStoreIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=${Uri.encode(action.appName)}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webStoreIntent)
                ActionExecutionResult(
                    success = false,
                    feedbackMessage = "Boss, ${action.appName} installed nahi hai. Browser mein Play Store khol diya hai.",
                    appOpened = "Browser"
                )
            }
        }
    }

    private fun handleWebSearch(context: Context, action: DeviceAction.WebSearch): ActionExecutionResult {
        val query = action.query.trim()
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
            } else {
                val encodedQuery = Uri.encode(query)
                val url = "https://www.google.com/search?q=$encodedQuery"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            ActionExecutionResult(
                success = true,
                feedbackMessage = "Boss, Google par \"$query\" search kar diya hai.",
                appOpened = "Web Search"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                success = false,
                feedbackMessage = "Could not start search: ${e.message}"
            )
        }
    }

    private fun handleMakeCall(context: Context, action: DeviceAction.MakeCall): ActionExecutionResult {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(dialIntent)
            val recipient = action.contactName ?: action.phoneNumber
            ActionExecutionResult(
                success = true,
                feedbackMessage = "Boss, $recipient ke liye phone dialer khol diya hai.",
                appOpened = "Phone"
            )
        } catch (e: Exception) {
            ActionExecutionResult(false, "Could not open dialer: ${e.message}")
        }
    }

    private fun handleSetAlarm(context: Context, action: DeviceAction.SetAlarm): ActionExecutionResult {
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            action.hour?.let { putExtra(AlarmClock.EXTRA_HOUR, it) }
            action.minute?.let { putExtra(AlarmClock.EXTRA_MINUTES, it) }
            action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            val resolved = context.packageManager
                .queryIntentActivities(alarmIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .isNotEmpty()
            if (resolved) {
                context.startActivity(alarmIntent)
                val timeText = if (action.hour != null) "${action.hour}:${String.format("%02d", action.minute ?: 0)}" else "new time"
                ActionExecutionResult(
                    success = true,
                    feedbackMessage = "Boss, Clock app mein $timeText ka alarm set karne ke liye khol diya hai.",
                    appOpened = "Clock"
                )
            } else {
                ActionExecutionResult(
                    success = false,
                    feedbackMessage = "Boss, is device par koi Alarm app nahi mila jo alarm intent support karta ho."
                )
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, "Could not set alarm: ${e.message}")
        }
    }

    private fun handleOpenMap(context: Context, action: DeviceAction.OpenMap): ActionExecutionResult {
        val encodedDest = Uri.encode(action.destination)
        val mapUri = Uri.parse("geo:0,0?q=$encodedDest")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(mapIntent)
            ActionExecutionResult(
                success = true,
                feedbackMessage = "Boss, ${action.destination} ke liye Maps directions khol diye hain.",
                appOpened = "Maps"
            )
        } catch (e: Exception) {
            val webMap = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedDest")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webMap)
            ActionExecutionResult(true, "Opening Google Maps for ${action.destination}.", "Maps")
        }
    }

    private fun findPackageByQuery(context: Context, query: String): String? {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                return app.packageName
            }
        }
        return null
    }
}
