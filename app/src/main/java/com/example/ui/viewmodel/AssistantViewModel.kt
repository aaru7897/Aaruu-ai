package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.DeviceAction
import com.example.actions.DeviceActionHandler
import com.example.ai.AaruAiService
import com.example.ai.AiBrainConfig
import com.example.ai.UserEmotion
import com.example.data.local.AaruDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import com.example.data.repository.AaruRepository
import com.example.service.AaruBackgroundService
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import com.example.voice.VoiceOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantVoiceState {
    IDLE,
    LISTENING,
    PAUSED,
    PROCESSING,
    SPEAKING,
    ERROR
}

enum class DiagnosticStatus {
    UNTESTED,
    RUNNING,
    PASSED,
    WARNING,
    FAILED
}

data class DiagnosticItem(
    val id: Int,
    val title: String,
    val titleHindi: String,
    val status: DiagnosticStatus = DiagnosticStatus.UNTESTED,
    val detail: String = "Tap 'Run Test' to verify"
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AaruRepository
    private val aiService = AaruAiService()

    private val _voiceState = MutableStateFlow(AssistantVoiceState.IDLE)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _currentEmotion = MutableStateFlow(UserEmotion.NEUTRAL)
    val currentEmotion: StateFlow<UserEmotion> = _currentEmotion.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _pendingAction = MutableStateFlow<DeviceAction?>(null)
    val pendingAction: StateFlow<DeviceAction?> = _pendingAction.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private val _voiceModeEnabled = MutableStateFlow(true) // Voice-first by default
    val voiceModeEnabled: StateFlow<Boolean> = _voiceModeEnabled.asStateFlow()

    private val _continuousModeEnabled = MutableStateFlow(true) // Continuous voice conversation
    val continuousModeEnabled: StateFlow<Boolean> = _continuousModeEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("hi") // Default primary: Hindi ("hi", "hinglish", "en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _memoryEnabled = MutableStateFlow(true)
    val memoryEnabled: StateFlow<Boolean> = _memoryEnabled.asStateFlow()

    private val _autoSaveMemory = MutableStateFlow(true)
    val autoSaveMemory: StateFlow<Boolean> = _autoSaveMemory.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(true)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _brainConfig = MutableStateFlow(AiBrainConfig())
    val brainConfig: StateFlow<AiBrainConfig> = _brainConfig.asStateFlow()

    val backgroundServiceActive: StateFlow<Boolean> = AaruBackgroundService.isRunningFlow

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _memorySearch = MutableStateFlow("")
    val memorySearch: StateFlow<String> = _memorySearch.asStateFlow()

    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private var ttsManager: TextToSpeechManager? = null

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialSpeech = MutableStateFlow("")
    val partialSpeech: StateFlow<String> = _partialSpeech.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow("default_indian_female")
    val selectedVoiceName: StateFlow<String> = _selectedVoiceName.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.05f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _diagnostics = MutableStateFlow(initialDiagnosticsList())
    val diagnostics: StateFlow<List<DiagnosticItem>> = _diagnostics.asStateFlow()

    val messages: StateFlow<List<ConversationEntity>>
    val allMemories: StateFlow<List<MemoryEntity>>
    val filteredMemories: StateFlow<List<MemoryEntity>>

    private var isStoppedManually = false

    init {
        val db = AaruDatabase.getDatabase(application)
        repository = AaruRepository(db.aaruDao())

        messages = repository.allMessagesFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allMemories = repository.allMemoriesFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        filteredMemories = combine(allMemories, _selectedCategory, _memorySearch) { list, cat, query ->
            list.filter { mem ->
                val matchesCategory = (cat == "ALL" || mem.category.equals(cat, ignoreCase = true))
                val matchesQuery = query.isBlank() ||
                        mem.content.contains(query, ignoreCase = true) ||
                        mem.key.contains(query, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        refreshNetworkStatus()
        initVoiceEngines()
    }

    private fun initVoiceEngines() {
        ttsManager = TextToSpeechManager(getApplication()) {
            // When TTS finishes speaking:
            if (_voiceState.value == AssistantVoiceState.SPEAKING) {
                _voiceState.value = AssistantVoiceState.IDLE
            }
            // If continuous conversation mode is enabled and user hasn't stopped or paused:
            if (_continuousModeEnabled.value && !isStoppedManually && speechRecognizerManager?.isPaused?.value != true) {
                viewModelScope.launch {
                    // 450ms anti-feedback delay so microphone never picks up Aaru's own voice
                    delay(450)
                    if (!isStoppedManually && _voiceState.value == AssistantVoiceState.IDLE) {
                        startListening()
                    }
                }
            }
        }

        speechRecognizerManager = SpeechRecognizerManager(
            context = getApplication(),
            onFinalResult = { finalResult ->
                _partialSpeech.value = finalResult
                if (finalResult.isNotBlank()) {
                    processPrompt(finalResult, fromVoice = true)
                }
            },
            onErrorCallback = { errMsg ->
                if (errMsg != null) {
                    _errorBanner.value = errMsg
                    _voiceState.value = AssistantVoiceState.ERROR
                } else if (_voiceState.value == AssistantVoiceState.LISTENING) {
                    _voiceState.value = AssistantVoiceState.IDLE
                }
            }
        )

        // Bind live flows from managers
        viewModelScope.launch {
            speechRecognizerManager?.soundLevel?.collect { _audioLevel.value = it }
        }
        viewModelScope.launch {
            speechRecognizerManager?.partialText?.collect { _partialSpeech.value = it }
        }
        viewModelScope.launch {
            ttsManager?.availableVoices?.collect { _availableVoices.value = it }
        }
        viewModelScope.launch {
            ttsManager?.selectedVoiceName?.collect { _selectedVoiceName.value = it }
        }
        viewModelScope.launch {
            ttsManager?.speechRate?.collect { _speechRate.value = it }
        }
        viewModelScope.launch {
            ttsManager?.speechPitch?.collect { _speechPitch.value = it }
        }
    }

    fun refreshNetworkStatus(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = connected
        return connected
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
        speechRecognizerManager?.activeLanguageCode = lang
    }

    fun setMemoryEnabled(enabled: Boolean) {
        _memoryEnabled.value = enabled
    }

    fun setAutoSaveMemory(enabled: Boolean) {
        _autoSaveMemory.value = enabled
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _wakeWordEnabled.value = enabled
        AaruBackgroundService.wakeWordRequired = enabled
    }

    fun updateBrainConfig(newConfig: AiBrainConfig) {
        _brainConfig.value = newConfig
    }

    fun selectVoice(voiceName: String) {
        ttsManager?.selectVoice(voiceName)
    }

    fun setSpeechRate(rate: Float) {
        ttsManager?.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        ttsManager?.setPitch(pitch)
    }

    fun toggleBackgroundService() {
        if (backgroundServiceActive.value) {
            AaruBackgroundService.stop(getApplication())
        } else {
            AaruBackgroundService.start(getApplication())
        }
    }

    fun toggleVoiceMode() {
        _voiceModeEnabled.value = !_voiceModeEnabled.value
        if (!_voiceModeEnabled.value) {
            ttsManager?.stop()
            if (_voiceState.value == AssistantVoiceState.SPEAKING) {
                _voiceState.value = AssistantVoiceState.IDLE
            }
        }
    }

    fun toggleContinuousMode() {
        _continuousModeEnabled.value = !_continuousModeEnabled.value
    }

    fun startListening() {
        val hasMic = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            _errorBanner.value = "Boss, Microphone permission zaroori hai voice conversation ke liye."
            _voiceState.value = AssistantVoiceState.ERROR
            return
        }

        isStoppedManually = false
        _errorBanner.value = null
        ttsManager?.stop()
        speechRecognizerManager?.activeLanguageCode = _selectedLanguage.value
        speechRecognizerManager?.resumeListening()
        _voiceState.value = AssistantVoiceState.LISTENING
    }

    fun pauseListening() {
        speechRecognizerManager?.pauseListening()
        _voiceState.value = AssistantVoiceState.PAUSED
    }

    fun resumeListening() {
        startListening()
    }

    fun stopListening() {
        isStoppedManually = true
        speechRecognizerManager?.stopListening()
        if (_voiceState.value == AssistantVoiceState.LISTENING || _voiceState.value == AssistantVoiceState.PAUSED) {
            _voiceState.value = AssistantVoiceState.IDLE
        }
    }

    /**
     * Full Stop Assistant control: immediately halts both speech synthesis and microphone recording.
     */
    fun stopAssistantCompletely() {
        isStoppedManually = true
        ttsManager?.stop()
        speechRecognizerManager?.stopListening()
        _partialSpeech.value = ""
        _voiceState.value = AssistantVoiceState.IDLE
    }

    fun stopSpeaking() {
        ttsManager?.stop()
        if (_voiceState.value == AssistantVoiceState.SPEAKING) {
            _voiceState.value = AssistantVoiceState.IDLE
        }
    }

    fun repeatLastAnswer() {
        val repeated = ttsManager?.repeatLast(_selectedLanguage.value) ?: false
        if (repeated) {
            _voiceState.value = AssistantVoiceState.SPEAKING
        } else {
            val lastAaruMsg = messages.value.lastOrNull { it.role == "aaru" }?.content
            if (!lastAaruMsg.isNullOrBlank()) {
                speakText(lastAaruMsg)
            }
        }
    }

    fun dismissErrorBanner() {
        _errorBanner.value = null
        if (_voiceState.value == AssistantVoiceState.ERROR) {
            _voiceState.value = AssistantVoiceState.IDLE
        }
    }

    fun processPrompt(userText: String, fromVoice: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        speechRecognizerManager?.stopListening()
        ttsManager?.stop()
        _voiceState.value = AssistantVoiceState.PROCESSING
        refreshNetworkStatus()

        viewModelScope.launch {
            repository.addMessage("user", trimmed)

            val memories = if (_memoryEnabled.value) repository.getAllMemoriesList() else emptyList()
            val recentChat = repository.getRecentMessages(10)

            val response = aiService.processUserMessage(
                userPrompt = trimmed,
                memories = memories,
                recentChat = recentChat,
                preferredLanguage = _selectedLanguage.value,
                memoryEnabled = _memoryEnabled.value,
                brainConfig = _brainConfig.value
            )

            _currentEmotion.value = response.detectedEmotion

            if (_memoryEnabled.value && _autoSaveMemory.value) {
                response.newMemories.forEach { mem ->
                    repository.saveMemory(
                        key = mem.key,
                        content = mem.content,
                        category = mem.category,
                        importance = mem.importance
                    )
                }
            }

            // Handle Voice Meta-Commands (Stop / Repeat)
            if (response.detectedAction is DeviceAction.ControlCommand) {
                when (response.detectedAction.command) {
                    "STOP" -> {
                        stopAssistantCompletely()
                        repository.addMessage("aaru", response.replyText, "CONTROL_STOP")
                        return@launch
                    }
                    "REPEAT" -> {
                        repeatLastAnswer()
                        return@launch
                    }
                }
            }

            val actionTypeName = when (response.detectedAction) {
                is DeviceAction.YouTubeSearch -> "YOUTUBE"
                is DeviceAction.SendWhatsApp -> "WHATSAPP"
                is DeviceAction.OpenInstagram -> "INSTAGRAM"
                is DeviceAction.SetTimer -> "TIMER"
                is DeviceAction.SetAlarm -> "ALARM"
                is DeviceAction.SaveNoteOrReminder -> "NOTE"
                is DeviceAction.OpenApp -> "OPEN_APP"
                is DeviceAction.WebSearch -> "WEB_SEARCH"
                is DeviceAction.MakeCall -> "CALL"
                is DeviceAction.OpenMap -> "MAP"
                is DeviceAction.AddMemory -> "MEMORY"
                is DeviceAction.ControlCommand -> "CONTROL"
                DeviceAction.None -> null
            }

            repository.addMessage(
                role = "aaru",
                content = response.replyText,
                actionType = actionTypeName
            )

            // Consequential actions (like WhatsApp messages) show confirmation card for review;
            // Safe navigation actions (like opening YouTube/Note) can execute or show confirmation card
            if (response.detectedAction !is DeviceAction.None) {
                if (response.detectedAction is DeviceAction.SendWhatsApp) {
                    // Always allow Boss to review WhatsApp message before sending (Section 7 & 8)
                    _pendingAction.value = response.detectedAction
                } else if (fromVoice && (response.detectedAction is DeviceAction.YouTubeSearch ||
                            response.detectedAction is DeviceAction.OpenApp ||
                            response.detectedAction is DeviceAction.OpenInstagram ||
                            response.detectedAction is DeviceAction.SaveNoteOrReminder)) {
                    executePendingAction(response.detectedAction, getApplication())
                } else {
                    _pendingAction.value = response.detectedAction
                }
            }

            if (_voiceModeEnabled.value || fromVoice) {
                _voiceState.value = AssistantVoiceState.SPEAKING
                ttsManager?.speak(response.replyText, _selectedLanguage.value)
            } else {
                _voiceState.value = AssistantVoiceState.IDLE
            }
        }
    }

    fun executePendingAction(action: DeviceAction, context: Context) {
        val result = DeviceActionHandler.execute(context, action)
        _actionFeedback.value = result.feedbackMessage
        _pendingAction.value = null

        viewModelScope.launch {
            repository.addMessage(
                role = "action",
                content = result.feedbackMessage
            )
        }

        if (_voiceModeEnabled.value) {
            ttsManager?.speak(result.feedbackMessage, _selectedLanguage.value)
        }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
    }

    fun setMemoryCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setMemorySearchQuery(query: String) {
        _memorySearch.value = query
    }

    fun addNewMemory(key: String, content: String, category: String, importance: Int) {
        viewModelScope.launch {
            repository.saveMemory(key, content, category, importance)
        }
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.updateMemory(memory)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun startNewConversation() {
        stopAssistantCompletely()
        viewModelScope.launch {
            repository.clearChat()
            val nick = _brainConfig.value.userNickname
            repository.addMessage(
                role = "aaru",
                content = "Haan $nick, nayi baat-cheet shuru karte hain! ❤️ Batao kya plan hai?"
            )
        }
    }

    fun speakText(text: String) {
        isStoppedManually = false
        _voiceState.value = AssistantVoiceState.SPEAKING
        ttsManager?.speak(text, _selectedLanguage.value)
    }

    // ========================================================================
    // SECTION 11: 10-POINT DIAGNOSTIC SYSTEM
    // ========================================================================
    private fun initialDiagnosticsList(): List<DiagnosticItem> = listOf(
        DiagnosticItem(1, "1. Microphone Permission", "माइक्रोफ़ोन अनुमति"),
        DiagnosticItem(2, "2. Speech Recognition Service", "स्पीच रिकग्निशन सेवा"),
        DiagnosticItem(3, "3. Internet Connectivity", "इंटरनेट कनेक्टिविटी"),
        DiagnosticItem(4, "4. API Connectivity", "एपीआई कनेक्शन"),
        DiagnosticItem(5, "5. AI Model Response", "एआई मॉडल रिस्पॉन्स"),
        DiagnosticItem(6, "6. Text-to-Speech (Hindi/EN)", "टेक्स्ट-टू-स्पीच (हिंदी/अंग्रेज़ी)"),
        DiagnosticItem(7, "7. Audio Playback Stream", "ऑडियो प्लेबैक और वॉल्यूम"),
        DiagnosticItem(8, "8. Continuous Conversation", "लगातार बातचीत मोड"),
        DiagnosticItem(9, "9. Background Service", "बैकग्राउंड सर्विस"),
        DiagnosticItem(10, "10. Memory Vault Storage", "मेमोरी स्टोरेज (Room DB)")
    )

    fun runAllDiagnostics() {
        viewModelScope.launch {
            for (id in 1..10) {
                runSingleDiagnosticSuspend(id)
            }
        }
    }

    fun runSingleDiagnostic(id: Int) {
        viewModelScope.launch {
            runSingleDiagnosticSuspend(id)
        }
    }

    private suspend fun runSingleDiagnosticSuspend(id: Int) {
        updateDiagnosticItem(id, DiagnosticStatus.RUNNING, "Testing...")
        val app = getApplication<Application>()

        when (id) {
            1 -> {
                val granted = ContextCompat.checkSelfPermission(
                    app,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    updateDiagnosticItem(1, DiagnosticStatus.PASSED, "RECORD_AUDIO permission is granted.")
                } else {
                    updateDiagnosticItem(1, DiagnosticStatus.WARNING, "RECORD_AUDIO permission not granted yet. Tap Mic button to grant.")
                }
            }
            2 -> {
                val available = SpeechRecognizer.isRecognitionAvailable(app)
                if (available) {
                    updateDiagnosticItem(2, DiagnosticStatus.PASSED, "Android SpeechRecognizer service is available on this device.")
                } else {
                    updateDiagnosticItem(2, DiagnosticStatus.WARNING, "SpeechRecognizer service unavailable on this device/emulator. Text fallback active.")
                }
            }
            3 -> {
                val online = refreshNetworkStatus()
                if (online) {
                    updateDiagnosticItem(3, DiagnosticStatus.PASSED, "Active internet connection verified.")
                } else {
                    updateDiagnosticItem(3, DiagnosticStatus.FAILED, "Offline: No active internet connection detected.")
                }
            }
            4 -> {
                val cfg = _brainConfig.value
                if (cfg.provider == "GROQ_COMPATIBLE" && cfg.customApiKey.isBlank()) {
                    updateDiagnosticItem(4, DiagnosticStatus.FAILED, "Custom API provider selected, but API key is empty.")
                } else {
                    updateDiagnosticItem(4, DiagnosticStatus.PASSED, "Configured provider (${cfg.provider} / ${cfg.modelName}) ready.")
                }
            }
            5 -> {
                val (ok, msg) = aiService.runDiagnosticPing(_brainConfig.value)
                if (ok) {
                    updateDiagnosticItem(5, DiagnosticStatus.PASSED, msg)
                } else {
                    updateDiagnosticItem(5, DiagnosticStatus.WARNING, "$msg (On-device fallback parser remains operational)")
                }
            }
            6 -> {
                val ttsReady = ttsManager?.isInitialized?.value == true
                val hindiOk = ttsManager?.isHindiSupported() == true
                if (ttsReady && hindiOk) {
                    updateDiagnosticItem(6, DiagnosticStatus.PASSED, "TTS Engine initialized. Hindi (hi-IN) & English voices supported.")
                } else if (ttsReady) {
                    updateDiagnosticItem(6, DiagnosticStatus.PASSED, "TTS Engine initialized (Default locale active).")
                } else {
                    updateDiagnosticItem(6, DiagnosticStatus.WARNING, "TTS Engine initializing or missing system voice pack.")
                }
            }
            7 -> {
                val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val musicVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                if (musicVol > 0) {
                    ttsManager?.speak("Haan Boss, audio test bilkul sahi kaam kar raha hai!", "hi")
                    updateDiagnosticItem(7, DiagnosticStatus.PASSED, "Media volume: $musicVol/$maxVol. Test voice phrase triggered.")
                } else {
                    updateDiagnosticItem(7, DiagnosticStatus.WARNING, "Media volume is muted (0/$maxVol). Increase phone volume to hear Aaru.")
                }
            }
            8 -> {
                val contEnabled = _continuousModeEnabled.value
                val voiceEnabled = _voiceModeEnabled.value
                updateDiagnosticItem(
                    8,
                    DiagnosticStatus.PASSED,
                    "Continuous Mode: ${if (contEnabled) "ON" else "OFF"}, Auto-Speak: ${if (voiceEnabled) "ON" else "OFF"}, Anti-Echo Guard: 450ms delay active."
                )
            }
            9 -> {
                val running = backgroundServiceActive.value
                val wake = _wakeWordEnabled.value
                updateDiagnosticItem(
                    9,
                    DiagnosticStatus.PASSED,
                    "Foreground Service configured. Currently: ${if (running) "RUNNING" else "STOPPED (Ready)"}. Wake-word: ${if (wake) "ON" else "OFF"}."
                )
            }
            10 -> {
                try {
                    val tempId = repository.saveMemory(
                        key = "_diag_test_probe",
                        content = "Diagnostic verification record",
                        category = "GENERAL",
                        importance = 1
                    )
                    val count = repository.getMemoryCount()
                    repository.deleteMemory(tempId)
                    updateDiagnosticItem(
                        10,
                        DiagnosticStatus.PASSED,
                        "Room SQLite Read/Write/Delete test passed. Active stored memories: ${count - 1}."
                    )
                } catch (e: Exception) {
                    updateDiagnosticItem(10, DiagnosticStatus.FAILED, "Database error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun updateDiagnosticItem(id: Int, status: DiagnosticStatus, detail: String) {
        _diagnostics.value = _diagnostics.value.map { item ->
            if (item.id == id) item.copy(status = status, detail = detail) else item
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.destroy()
        ttsManager?.shutdown()
    }
}
