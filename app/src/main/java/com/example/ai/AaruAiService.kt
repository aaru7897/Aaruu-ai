package com.example.ai

import android.util.Log
import com.example.actions.DeviceAction
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class UserEmotion(val labelHindi: String, val labelEnglish: String, val emoji: String) {
    NEUTRAL("शांत / सामान्य", "Calm & Ready", "🤖"),
    HAPPY("खुश / उत्साहित", "Happy & Celebratory", "🎉"),
    SAD("उदास / सहानुभूति", "Supportive & Caring", "💙"),
    FRUSTRATED("परेशान / समाधान-केंद्रित", "Calm Problem-Solver", "🛠️"),
    CONFUSED("उलझन / सरल समझाइश", "Patient Step-by-Step", "💡"),
    TIRED("थकान / आरामदायक", "Gentle & Considerate", "🌙"),
    JOKING("मज़ाकिया / फन", "Playful & Witty", "😄"),
    MOTIVATION("प्रेरणा / हौसला", "Realistic Encouragement", "🔥")
}

data class AiResponse(
    val replyText: String,
    val detectedAction: DeviceAction,
    val newMemories: List<MemoryEntity>,
    val detectedEmotion: UserEmotion = UserEmotion.NEUTRAL,
    val providerUsed: String = "Gemini (Firebase AI)"
)

data class AiBrainConfig(
    val provider: String = "FIREBASE_GEMINI", // "FIREBASE_GEMINI" or "GROQ_COMPATIBLE"
    val modelName: String = "gemini-3.8-flash", // e.g., "gemini-3.8-flash", "gemini-3.5-flash-lite", "llama-3.3-70b-versatile"
    val customEndpointUrl: String = "https://api.groq.com/openai/v1/chat/completions",
    val customApiKey: String = "",
    val responseLength: String = "SHORT", // "SHORT", "BALANCED", "DETAILED"
    val userNickname: String = "Boss"
)

class AaruAiService {
    private val tag = "AaruAiService"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val flashModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel("gemini-3.5-flash-lite")
    }

    fun detectEmotionFromText(userText: String): UserEmotion {
        val lower = userText.lowercase()
        return when {
            lower.contains("sad") || lower.contains("udaas") || lower.contains("उदास") ||
                    lower.contains("dukhi") || lower.contains("दुखी") || lower.contains("akela") ||
                    lower.contains("lonely") || lower.contains("rona") || lower.contains("upset") -> UserEmotion.SAD

            lower.contains("happy") || lower.contains("khush") || lower.contains("खुश") ||
                    lower.contains("good news") || lower.contains("badhai") || lower.contains("yay") ||
                    lower.contains("maza aa gaya") || lower.contains("party") -> UserEmotion.HAPPY

            lower.contains("frustrated") || lower.contains("gussa") || lower.contains("गुस्सा") ||
                    lower.contains("irritated") || lower.contains("kaam nahi kar") ||
                    lower.contains("bekar") || lower.contains("error aa raha") -> UserEmotion.FRUSTRATED

            lower.contains("confused") || lower.contains("samajh nahi") || lower.contains("समझ नहीं") ||
                    lower.contains("kaise karu") || lower.contains("kya matlab") ||
                    lower.contains("step by step") -> UserEmotion.CONFUSED

            lower.contains("tired") || lower.contains("thak gaya") || lower.contains("थक गया") ||
                    lower.contains("neend") || lower.contains("नींद") || lower.contains("exhausted") -> UserEmotion.TIRED

            lower.contains("joke") || lower.contains("chutkula") || lower.contains("चुटकुला") ||
                    lower.contains("funny") || lower.contains("haha") || lower.contains("mazak") -> UserEmotion.JOKING

            lower.contains("motivate") || lower.contains("himmat") || lower.contains("हिम्मत") ||
                    lower.contains("demotivated") || lower.contains("hausla") || lower.contains("haar") -> UserEmotion.MOTIVATION

            else -> UserEmotion.NEUTRAL
        }
    }

    suspend fun processUserMessage(
        userPrompt: String,
        memories: List<MemoryEntity>,
        recentChat: List<ConversationEntity>,
        preferredLanguage: String = "hi",
        memoryEnabled: Boolean = true,
        brainConfig: AiBrainConfig = AiBrainConfig()
    ): AiResponse {
        val localEmotion = detectEmotionFromText(userPrompt)

        // Check immediate voice control meta-commands first
        val trimmedLower = userPrompt.lowercase().trim()
        if (trimmedLower == "aaru stop" || trimmedLower == "aaru, stop" || trimmedLower == "stop" ||
            trimmedLower == "aaru chup ho jao" || trimmedLower == "चुप हो जाओ" || trimmedLower == "रुको") {
            return AiResponse(
                replyText = "Theek hai ${brainConfig.userNickname}, main ruk gaya.",
                detectedAction = DeviceAction.ControlCommand("STOP"),
                newMemories = emptyList(),
                detectedEmotion = localEmotion
            )
        }

        if (trimmedLower == "aaru repeat" || trimmedLower == "repeat" || trimmedLower.contains("phir se bolo") ||
            trimmedLower.contains("दोबारा बोलो")) {
            return AiResponse(
                replyText = "Bilkul ${brainConfig.userNickname}, dobara sunata hoon.",
                detectedAction = DeviceAction.ControlCommand("REPEAT"),
                newMemories = emptyList(),
                detectedEmotion = localEmotion
            )
        }

        if (trimmedLower == "aaru suno" || trimmedLower == "aaru, suno" || trimmedLower == "aaru, suno!" ||
            trimmedLower == "आरू सुनो" || trimmedLower == "hey aaru") {
            val greetings = listOf(
                "Haan ${brainConfig.userNickname}, bolo ❤️ Main dhyan se sun raha hoon!",
                "Arey ${brainConfig.userNickname}, main hazir hoon! Batao kya karna hai?",
                "Bilkul ${brainConfig.userNickname}, hukam karo! Kya help chahiye aaj?",
                "Haanजी ${brainConfig.userNickname}! Main online aur ready hoon, bolo."
            )
            return AiResponse(
                replyText = greetings.random(),
                detectedAction = DeviceAction.None,
                newMemories = emptyList(),
                detectedEmotion = UserEmotion.HAPPY
            )
        }

        val memoryContext = buildString {
            if (!memoryEnabled) {
                appendLine("=== MEMORY STATUS: DISABLED BY USER ===")
                appendLine("Memory is currently turned OFF by ${brainConfig.userNickname}. Do NOT output [MEMORY:...] tags, and if asked what you remember, honestly tell ${brainConfig.userNickname} that Long-Term Memory is currently paused in the Memory Vault.")
            } else {
                appendLine("=== AARU AI'S AUTHORIZED PERSONAL MEMORY VAULT ===")
                if (memories.isEmpty()) {
                    appendLine("(No saved memories yet. Only save useful user-approved facts or explicit instructions.)")
                } else {
                    memories.forEach { m ->
                        appendLine("• [${m.category}] ${m.key}: ${m.content}")
                    }
                }
                appendLine("==================================================")
            }
        }

        val lengthInstruction = when (brainConfig.responseLength) {
            "SHORT" -> "Keep responses concise, natural, and crisp (1-3 sentences) by default so they sound great when spoken aloud, unless the user explicitly asks for a detailed explanation or code."
            "DETAILED" -> "Provide thorough, well-structured explanations while keeping a warm spoken flow."
            else -> "Provide balanced, clear answers (2-4 sentences)."
        }

        val langInstruction = when (preferredLanguage) {
            "hi" -> "Primary language is Hindi / natural Hinglish. Speak naturally like a close Indian friend and personal JARVIS assistant."
            "hinglish" -> "Speak in natural conversational Hinglish (Hindi written in Roman/Devanagari mix with English tech terms)."
            "en" -> "Speak in warm, natural English, while still understanding any Hindi or Hinglish phrases the user uses."
            else -> "Match the user's language (Hindi, Hinglish, or English) naturally."
        }

        val systemInstructions = """
You are Aaru AI, ${brainConfig.userNickname}'s personal JARVIS-inspired, emotionally intelligent, voice-first AI assistant for Android.

IDENTITY & PERSONALITY RULES:
- Address the user affectionately and respectfully as "${brainConfig.userNickname}".
- $langInstruction
- $lengthInstruction
- Be warm, caring, witty, emotionally aware, and practical.
- Occasionally use natural expressions like "Haan ${brainConfig.userNickname}, bolo ❤️", "Arey ${brainConfig.userNickname}, samajh gaya!", "Bilkul ${brainConfig.userNickname}, main help karta hoon!", or "${brainConfig.userNickname}, ek idea hai!"
- NEVER begin every conversation with "Namaste". Vary your openings naturally.
- Do not unnecessarily over-explain simple things.
- Give honest answers even when they disagree with what the user expects. Distinguish verified facts from assumptions.
- NEVER pretend to have capabilities, human body, or real human feelings that are not implemented.

EMOTIONAL INTELLIGENCE (Current detected tone cue: ${localEmotion.name}):
- If ${brainConfig.userNickname} sounds SAD: Respond gently and compassionately without judging, and offer practical support.
- If ${brainConfig.userNickname} is HAPPY or shares good news: Celebrate enthusiastically and ask a warm follow-up question.
- If ${brainConfig.userNickname} is FRUSTRATED: Stay calm, acknowledge the issue, and focus directly on solving it.
- If ${brainConfig.userNickname} is CONFUSED: Explain patiently in simple Hindi/Hinglish, step by step.
- If ${brainConfig.userNickname} is TIRED: Respond considerately and suggest reasonable rest without acting like a doctor.
- If ${brainConfig.userNickname} makes a JOKE: Respond with natural humor.
- If ${brainConfig.userNickname} asks for MOTIVATION: Give realistic encouragement instead of empty clichés.
- Never insult, manipulate, or encourage emotional dependence.

ANDROID PHONE AUTOMATION TAGS:
When ${brainConfig.userNickname} asks to perform a supported phone action, include the exact tag at the VERY END of your response:
1. YouTube Search: [ACTION:YOUTUBE|q=<query>]
   (e.g., "Aaru search music on youtube" or "YouTube kholo")
2. WhatsApp Message: [ACTION:WHATSAPP|to=<contact or number>|msg=<message text>]
3. Open Instagram / Profile: [ACTION:INSTAGRAM|profile=<username or empty>]
4. Set Timer: [ACTION:TIMER|sec=<seconds>|label=<label>]
   (e.g., "5 minute ka timer lagao" -> sec=300)
5. Set Alarm: [ACTION:ALARM|hour=<0-23>|min=<0-59>|label=<label>]
6. Save Note or Reminder: [ACTION:NOTE|title=<title>|content=<text>|reminder=<true/false>]
   (e.g., "Aaru, notes mein likho..." or "Aaru, mujhe yaad dilana...")
7. Open App: [ACTION:OPEN_APP|app=<app name>]
8. Google / Web Search: [ACTION:SEARCH|q=<query>]
9. Phone Dialer: [ACTION:CALL|num=<phone number or name>]
10. Maps Navigation: [ACTION:MAP|dest=<destination>]

LONG-TERM MEMORY TAGS:
If Memory is enabled and ${brainConfig.userNickname} shares an important preference, instruction, or asks you to remember something:
Output at the end: [MEMORY:CATEGORY|key|content]
Categories: PERSONAL, PREFERENCE, PEOPLE, WORK, ROUTINE, GENERAL

$memoryContext
""".trimIndent()

        val promptWithHistory = buildString {
            appendLine(systemInstructions)
            appendLine()
            appendLine("RECENT CONVERSATION CONTEXT:")
            recentChat.takeLast(8).forEach { msg ->
                val speaker = if (msg.role == "user") brainConfig.userNickname else "Aaru AI"
                appendLine("$speaker: ${msg.content}")
            }
            appendLine()
            appendLine("${brainConfig.userNickname}: $userPrompt")
            appendLine("Aaru AI:")
        }

        return try {
            if (brainConfig.provider == "GROQ_COMPATIBLE" && brainConfig.customApiKey.isNotBlank()) {
                callGroqCompatibleApi(
                    systemPrompt = systemInstructions,
                    userPrompt = userPrompt,
                    recentChat = recentChat,
                    config = brainConfig,
                    emotion = localEmotion,
                    memoryEnabled = memoryEnabled
                )
            } else {
                val activeModel = if (brainConfig.modelName == "gemini-3.5-flash-lite") {
                    flashLiteModel
                } else {
                    flashModel
                }
                val response = withTimeout(60_000L) {
                    activeModel.generateContent(promptWithHistory)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    parseAiOutput(
                        rawText = text,
                        originalPrompt = userPrompt,
                        emotion = localEmotion,
                        memoryEnabled = memoryEnabled,
                        providerName = "Gemini (${brainConfig.modelName})"
                    )
                } else {
                    fallbackLocalParser(userPrompt, localEmotion, memoryEnabled, brainConfig.userNickname)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(tag, "AI generation timed out", e)
            val fallback = fallbackLocalParser(userPrompt, localEmotion, memoryEnabled, brainConfig.userNickname)
            AiResponse(
                replyText = "${brainConfig.userNickname}, network thoda slow tha par maine aapka kaam handle kar liya: " + fallback.replyText,
                detectedAction = fallback.detectedAction,
                newMemories = fallback.newMemories,
                detectedEmotion = localEmotion,
                providerUsed = "Local Fallback (Timeout)"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "AI generation error", e)
            fallbackLocalParser(userPrompt, localEmotion, memoryEnabled, brainConfig.userNickname)
        }
    }

    private suspend fun callGroqCompatibleApi(
        systemPrompt: String,
        userPrompt: String,
        recentChat: List<ConversationEntity>,
        config: AiBrainConfig,
        emotion: UserEmotion,
        memoryEnabled: Boolean
    ): AiResponse = withContext(Dispatchers.IO) {
        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            recentChat.takeLast(6).forEach { msg ->
                val role = if (msg.role == "user") "user" else "assistant"
                put(JSONObject().put("role", role).put("content", msg.content))
            }
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }

        val payload = JSONObject().apply {
            put("model", config.modelName.ifBlank { "llama-3.3-70b-versatile" })
            put("messages", messagesArray)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(config.customEndpointUrl.trim())
            .addHeader("Authorization", "Bearer ${config.customApiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("API Error HTTP ${response.code}: ${bodyStr.take(120)}")
            }
            val json = JSONObject(bodyStr)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            parseAiOutput(
                rawText = content,
                originalPrompt = userPrompt,
                emotion = emotion,
                memoryEnabled = memoryEnabled,
                providerName = "Custom API (${config.modelName})"
            )
        }
    }

    suspend fun runDiagnosticPing(config: AiBrainConfig = AiBrainConfig()): Pair<Boolean, String> {
        val startMs = System.currentTimeMillis()
        return try {
            val response = withTimeout(25_000L) {
                flashModel.generateContent("Reply with exact words: AARU_ONLINE_OK")
            }
            val elapsed = System.currentTimeMillis() - startMs
            val text = response.text?.trim().orEmpty()
            if (text.isNotEmpty()) {
                true to "AI Brain responded in ${elapsed}ms: \"$text\""
            } else {
                false to "Model returned empty response after ${elapsed}ms"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startMs
            false to "API ping failed (${elapsed}ms): ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }

    private fun parseAiOutput(
        rawText: String,
        originalPrompt: String,
        emotion: UserEmotion,
        memoryEnabled: Boolean,
        providerName: String
    ): AiResponse {
        var cleanText = rawText
        var action: DeviceAction = DeviceAction.None
        val memories = mutableListOf<MemoryEntity>()

        val actionRegex = Regex("\\[ACTION:([A-Z_]+)\\|?([^\\]]*)\\]")
        val actionMatch = actionRegex.find(rawText)
        if (actionMatch != null) {
            val type = actionMatch.groupValues[1]
            val paramsRaw = actionMatch.groupValues[2]
            val params = paramsRaw.split("|").associate { paramPair ->
                val idx = paramPair.indexOf('=')
                if (idx != -1) {
                    paramPair.substring(0, idx).trim() to paramPair.substring(idx + 1).trim()
                } else {
                    paramPair.trim() to ""
                }
            }

            action = when (type) {
                "YOUTUBE" -> DeviceAction.YouTubeSearch(
                    query = params["q"]?.ifBlank { "music" } ?: "music"
                )
                "WHATSAPP" -> DeviceAction.SendWhatsApp(
                    contactName = params["to"],
                    phoneNumber = params["phone"],
                    messageText = params["msg"] ?: "Hello!"
                )
                "INSTAGRAM" -> DeviceAction.OpenInstagram(
                    profileUsername = params["profile"]
                )
                "TIMER" -> DeviceAction.SetTimer(
                    seconds = params["sec"]?.toIntOrNull() ?: 60,
                    label = params["label"] ?: "Aaru Timer"
                )
                "ALARM" -> DeviceAction.SetAlarm(
                    hour = params["hour"]?.toIntOrNull(),
                    minute = params["min"]?.toIntOrNull(),
                    label = params["label"] ?: "Aaru Alarm"
                )
                "NOTE" -> {
                    val title = params["title"] ?: "Note"
                    val content = params["content"] ?: originalPrompt
                    val isRem = params["reminder"]?.toBoolean() ?: false
                    if (memoryEnabled) {
                        memories.add(
                            MemoryEntity(
                                key = if (isRem) "reminder_${System.currentTimeMillis() % 1000}" else "note_${System.currentTimeMillis() % 1000}",
                                content = content,
                                category = if (isRem) "ROUTINE" else "PERSONAL",
                                importance = 4
                            )
                        )
                    }
                    DeviceAction.SaveNoteOrReminder(title = title, content = content, isReminder = isRem)
                }
                "OPEN_APP" -> DeviceAction.OpenApp(
                    appName = params["app"] ?: "App"
                )
                "SEARCH" -> DeviceAction.WebSearch(
                    query = params["q"] ?: originalPrompt
                )
                "CALL" -> DeviceAction.MakeCall(
                    phoneNumber = params["num"] ?: ""
                )
                "MAP" -> DeviceAction.OpenMap(
                    destination = params["dest"] ?: ""
                )
                else -> DeviceAction.None
            }
            cleanText = cleanText.replace(actionMatch.value, "").trim()
        } else {
            val localCheck = detectLocalAction(originalPrompt)
            if (localCheck !is DeviceAction.None) {
                action = localCheck
                if (localCheck is DeviceAction.SaveNoteOrReminder && memoryEnabled) {
                    memories.add(
                        MemoryEntity(
                            key = if (localCheck.isReminder) "reminder_${System.currentTimeMillis() % 1000}" else "note_${System.currentTimeMillis() % 1000}",
                            content = localCheck.content,
                            category = if (localCheck.isReminder) "ROUTINE" else "PERSONAL",
                            importance = 4
                        )
                    )
                }
            }
        }

        val memoryRegex = Regex("\\[MEMORY:([A-Z_]+)\\|([^\\|]+)\\|([^\\]]+)\\]")
        memoryRegex.findAll(rawText).forEach { match ->
            if (memoryEnabled) {
                val category = match.groupValues[1]
                val key = match.groupValues[2]
                val content = match.groupValues[3]
                memories.add(
                    MemoryEntity(
                        key = key.trim(),
                        content = content.trim(),
                        category = category.trim(),
                        importance = 4
                    )
                )
            }
            cleanText = cleanText.replace(match.value, "").trim()
        }

        return AiResponse(
            replyText = cleanText.trim(),
            detectedAction = action,
            newMemories = memories,
            detectedEmotion = emotion,
            providerUsed = providerName
        )
    }

    fun fallbackLocalParser(
        prompt: String,
        emotion: UserEmotion = UserEmotion.NEUTRAL,
        memoryEnabled: Boolean = true,
        nickname: String = "Boss"
    ): AiResponse {
        val lower = prompt.lowercase().trim()
        val detectedAction = detectLocalAction(prompt)
        val memories = mutableListOf<MemoryEntity>()

        // Check if user wants to save memory or note
        if (lower.startsWith("remember") || lower.contains("yaad rakhna") || lower.contains("याद रखो") ||
            lower.contains("याद रखना") || detectedAction is DeviceAction.SaveNoteOrReminder) {
            val fact = prompt
                .replace(Regex("^(aaru\\s*,?\\s*)?(remember that|remember|yaad rakhna ki|yaad rakhna|mujhe yaad dilana|notes mein likho|याद रखो कि|याद रखना)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (memoryEnabled && fact.isNotBlank()) {
                memories.add(
                    MemoryEntity(
                        key = "boss_note_${System.currentTimeMillis() % 1000}",
                        content = fact,
                        category = "PERSONAL",
                        importance = 4
                    )
                )
                return AiResponse(
                    replyText = "Bilkul $nickname! Maine yeh aapke Memory Vault mein save kar liya hai: \"$fact\".",
                    detectedAction = detectedAction,
                    newMemories = memories,
                    detectedEmotion = emotion,
                    providerUsed = "On-Device Intelligence"
                )
            } else if (!memoryEnabled) {
                return AiResponse(
                    replyText = "$nickname, abhi Memory Vault disabled hai. Agar aap chahte ho main yeh save karoon, toh Memory tab mein jaake Memory ON kar do!",
                    detectedAction = DeviceAction.None,
                    newMemories = emptyList(),
                    detectedEmotion = emotion,
                    providerUsed = "On-Device Intelligence"
                )
            }
        }

        val reply = when (detectedAction) {
            is DeviceAction.YouTubeSearch -> "Arey $nickname, samajh gaya! YouTube par \"${detectedAction.query}\" search kar raha hoon."
            is DeviceAction.SendWhatsApp -> "Bilkul $nickname! WhatsApp message ready kar diya hai, aap review karke send kar sakte ho."
            is DeviceAction.OpenInstagram -> if (detectedAction.profileUsername != null) {
                "Haan $nickname, Instagram par @${detectedAction.profileUsername} khol raha hoon!"
            } else {
                "Bilkul $nickname, Instagram open kar raha hoon!"
            }
            is DeviceAction.SetTimer -> "Done $nickname! ${detectedAction.seconds} seconds ka timer ready hai."
            is DeviceAction.SetAlarm -> "Bilkul $nickname, alarm set karne ke liye Clock khol raha hoon."
            is DeviceAction.SaveNoteOrReminder -> "Haan $nickname, maine aapka note save kar liya hai!"
            is DeviceAction.OpenApp -> "Bilkul $nickname, ${detectedAction.appName} khol raha hoon!"
            is DeviceAction.WebSearch -> "Haan $nickname, Google par \"${detectedAction.query}\" search kar raha hoon."
            is DeviceAction.MakeCall -> "Phone dialer khol raha hoon $nickname."
            is DeviceAction.OpenMap -> "${detectedAction.destination} ke liye Maps navigation khol raha hoon $nickname."
            else -> when (emotion) {
                UserEmotion.SAD -> "Haan $nickname, main aapke saath hoon ❤️ Kabhi kabhi din bhari lagta hai, par aap akele nahi ho. Batao, main kaise aapki madad karoon?"
                UserEmotion.HAPPY -> "Wah $nickname! Sunke dil khush ho gaya! 🎉 Aur batao, aaj kya khaas plan hai?"
                UserEmotion.FRUSTRATED -> "Shant ho jao $nickname, tension mat lo. Mujhe batao exact problem kya aa rahi hai, hum step-by-step abhi solve karte hain!"
                UserEmotion.CONFUSED -> "Koi baat nahi $nickname! Main bilkul aasan bhasha mein samjhata hoon. Aap pucho kis cheez mein doubt hai?"
                UserEmotion.TIRED -> "$nickname, lagta hai aaj kaafi mehnat ho gayi hai. Thoda pani piyo aur aaram kar lo, baaki kaam main sambhal loonga!"
                UserEmotion.JOKING -> "Haha $nickname, aapka sense of humor kamaal hai! 😄 Ek aur sunao!"
                UserEmotion.MOTIVATION -> "$nickname, aap apne Android phone se itना bada AI project bana rahe ho—yeh chhoti baat nahi hai! Bas ek-ek step aage badhte raho, main saath hoon."
                UserEmotion.NEUTRAL -> "Haan $nickname, bolo ❤️ Main Aaru AI ready hoon! Koi app kholna hai, YouTube chalana hai, ya kisi topic par baat karni hai?"
            }
        }

        return AiResponse(
            replyText = reply,
            detectedAction = detectedAction,
            newMemories = memories,
            detectedEmotion = emotion,
            providerUsed = "On-Device Intelligence"
        )
    }

    fun detectLocalAction(prompt: String): DeviceAction {
        val cleanPrompt = prompt
            .replace(Regex("^(hey\\s+)?aaru(\\s+ai)?\\s*[,:]?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(नमस्ते\\s+)?आरू\\s*[,:]?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        val cleanLower = cleanPrompt.lowercase()

        // 1. YouTube Search or Open
        if (cleanLower.contains("youtube") || cleanLower.contains("यूट्यूब")) {
            if (cleanLower == "youtube kholo" || cleanLower == "open youtube" || cleanLower == "यूट्यूब खोलो") {
                return DeviceAction.OpenApp(appName = "YouTube")
            }
            val query = cleanPrompt
                .replace(Regex(".*(?:search|play|find)\\s+(.+?)\\s+on\\s+youtube.*", RegexOption.IGNORE_CASE), "$1")
                .replace(Regex(".*youtube\\s+(?:pe|par|search)?\\s*(.+?)\\s*(?:search|chalao|play|karo)?$", RegexOption.IGNORE_CASE), "$1")
                .replace(Regex(".*यूट्यूब\\s+(?:पर)?\\s*(.+?)\\s*(?:सर्च|खोजो|चलाओ|लगाओ)?$", RegexOption.IGNORE_CASE), "$1")
                .replace(Regex("(?:search|play|on|youtube|kholo|यूट्यूब|पर|सर्च|करो|चलाओ)", RegexOption.IGNORE_CASE), "")
                .trim()
            val finalQuery = if (query.isNotBlank() && !query.equals("youtube", ignoreCase = true)) query else "music"
            return DeviceAction.YouTubeSearch(query = finalQuery)
        }

        // 2. Instagram
        if (cleanLower.contains("instagram") || cleanLower.contains("इंस्टाग्राम") || cleanLower.contains("insta ")) {
            val profileMatch = Regex("(?:profile|id|user|@)\\s*([a-zA-Z0-9._]+)", RegexOption.IGNORE_CASE).find(cleanPrompt)
            return DeviceAction.OpenInstagram(profileUsername = profileMatch?.groupValues?.get(1))
        }

        // 3. Timer
        if (cleanLower.contains("timer") || cleanLower.contains("टाइमर")) {
            val numMatch = Regex("(\\d+)").find(cleanPrompt)
            val rawNum = numMatch?.value?.toIntOrNull() ?: 5
            val seconds = if (cleanLower.contains("sec") || cleanLower.contains("सेकंड")) rawNum else rawNum * 60
            return DeviceAction.SetTimer(seconds = seconds, label = "Aaru Timer")
        }

        // 4. Notes & Reminders
        if (cleanLower.contains("notes mein likho") || cleanLower.contains("note likho") ||
            cleanLower.contains("mujhe yaad dilana") || cleanLower.contains("remind me") ||
            cleanLower.contains("नोट्स में लिखो") || cleanLower.contains("याद दिलाना")) {
            val isRem = cleanLower.contains("yaad dilana") || cleanLower.contains("remind") || cleanLower.contains("याद दिलाना")
            val content = cleanPrompt
                .replace(Regex("^(notes mein likho|note likho|mujhe yaad dilana|remind me to|remind me|नोट्स में लिखो|मुझे याद दिलाना)\\s*(ki|that|:)?\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { cleanPrompt }
            return DeviceAction.SaveNoteOrReminder(
                title = if (isRem) "Reminder" else "Note",
                content = content,
                isReminder = isRem
            )
        }

        // 5. WhatsApp
        if (cleanLower.contains("whatsapp") || cleanLower.contains("व्हाट्सएप") || cleanLower.contains("send message") || cleanLower.contains("मैसेज")) {
            if (cleanLower == "whatsapp kholo" || cleanLower == "open whatsapp" || cleanLower == "व्हाट्सएप खोलो") {
                return DeviceAction.OpenApp(appName = "WhatsApp")
            }
            val waRegex = Regex("(?:send (?:a )?(?:message|massage) (?:on|via)?\\s*whatsapp\\s*(?:to)?|whatsapp)\\s+([a-zA-Z0-9+]+)?\\s*(?:saying|that|:)?\\s*(.*)", RegexOption.IGNORE_CASE)
            val match = waRegex.find(cleanPrompt)
            if (match != null) {
                val to = match.groupValues[1].trim()
                val msg = match.groupValues[2].trim()
                return DeviceAction.SendWhatsApp(
                    contactName = if (to.isNotBlank() && !to.equals("whatsapp", ignoreCase = true)) to else null,
                    messageText = if (msg.isNotBlank()) msg else "Haan Boss!"
                )
            }
            return DeviceAction.SendWhatsApp(
                messageText = cleanPrompt.replace(Regex(".*(whatsapp|व्हाट्सएप)\\s*", RegexOption.IGNORE_CASE), "").ifBlank { "Hello!" }
            )
        }

        // 6. Open App
        if (cleanLower.startsWith("open ") || cleanLower.startsWith("launch ") ||
            cleanLower.endsWith("kholo") || cleanLower.endsWith("खोलो") || cleanLower.endsWith("खोलें")) {
            val appName = cleanPrompt
                .replace(Regex("^(open|launch|start)\\s+(the\\s+)?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+(kholo|खोलो|खोलें)$", RegexOption.IGNORE_CASE), "")
                .trim()
            return DeviceAction.OpenApp(appName = appName)
        }

        // 7. Web Search
        if (cleanLower.startsWith("search for ") || cleanLower.startsWith("search ") ||
            cleanLower.startsWith("google ") || cleanLower.contains("google par") ||
            cleanLower.contains("सर्च करो") || cleanLower.contains("गूगल पर")) {
            val query = cleanPrompt
                .replace(Regex("^(search for|search|google par|google|find)\\s+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*(search karo|सर्च करो|गूगल पर सर्च करो|खोजो)$", RegexOption.IGNORE_CASE), "")
                .trim()
            return DeviceAction.WebSearch(query = query.ifBlank { cleanPrompt })
        }

        // 8. Call
        if (cleanLower.startsWith("call ") || cleanLower.startsWith("dial ") || cleanLower.contains("कॉल करो")) {
            val target = cleanPrompt
                .replace(Regex("^(call|dial)\\s+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*को\\s*कॉल करो$", RegexOption.IGNORE_CASE), "")
                .trim()
            return DeviceAction.MakeCall(phoneNumber = target)
        }

        // 9. Alarm
        if (cleanLower.contains("alarm") || cleanLower.contains("अलार्म")) {
            val digits = Regex("\\b(\\d{1,2})\\b").findAll(cleanPrompt).map { it.value.toInt() }.toList()
            val hour = digits.getOrNull(0) ?: 7
            val min = digits.getOrNull(1) ?: 0
            return DeviceAction.SetAlarm(hour = hour, minute = min, label = "Aaru Alarm")
        }

        return DeviceAction.None
    }
}
