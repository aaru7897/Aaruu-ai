package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceOption(
    val name: String,
    val displayName: String,
    val localeTag: String,
    val isFemalePreferred: Boolean
)

class TextToSpeechManager(
    private val context: Context,
    private val onDoneSpeaking: (() -> Unit)? = null
) {
    private val tag = "TextToSpeechManager"
    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow("default_indian_female")
    val selectedVoiceName: StateFlow<String> = _selectedVoiceName.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.05f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private var lastSpokenText: String = ""

    private val hindiLocale = Locale("hi", "IN")
    private val indianEnglishLocale = Locale("en", "IN")

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    _isInitialized.value = true
                    val res = tts?.setLanguage(hindiLocale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val enRes = tts?.setLanguage(indianEnglishLocale)
                        if (enRes == TextToSpeech.LANG_MISSING_DATA || enRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.language = Locale.getDefault()
                        }
                    }

                    tts?.setPitch(_speechPitch.value)
                    tts?.setSpeechRate(_speechRate.value)

                    loadVoiceCatalog()

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            onDoneSpeaking?.invoke()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            onDoneSpeaking?.invoke()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                            onDoneSpeaking?.invoke()
                        }
                    })
                    Log.d(tag, "TTS initialized successfully")
                } else {
                    _isInitialized.value = false
                    Log.e(tag, "TTS initialization failed with status $status")
                }
            }
        } catch (e: Exception) {
            _isInitialized.value = false
            Log.e(tag, "Exception initializing TTS", e)
        }
    }

    private fun loadVoiceCatalog() {
        try {
            val engineVoices: Set<Voice>? = tts?.voices
            val options = mutableListOf(
                VoiceOption(
                    name = "default_indian_female",
                    displayName = "Aaru Natural Indian (Female Warm)",
                    localeTag = "hi-IN / en-IN",
                    isFemalePreferred = true
                ),
                VoiceOption(
                    name = "jarvis_crisp",
                    displayName = "JARVIS Crisp Assistant",
                    localeTag = "en-IN / hi-IN",
                    isFemalePreferred = false
                )
            )

            engineVoices?.filter {
                val lang = it.locale.language.lowercase()
                (lang == "hi" || lang == "en") && !it.isNetworkConnectionRequired
            }?.take(8)?.forEach { v ->
                val isFemale = v.name.lowercase().let { n ->
                    n.contains("female") || n.contains("f0") || n.contains("sfg") || n.contains("hed") || n.contains("hin-default")
                }
                val country = v.locale.country.ifBlank { "Global" }
                options.add(
                    VoiceOption(
                        name = v.name,
                        displayName = "${v.locale.displayLanguage} ($country) - ${v.name.takeLast(12)}",
                        localeTag = v.locale.toLanguageTag(),
                        isFemalePreferred = isFemale
                    )
                )
            }

            _availableVoices.value = options

            // Try to auto-select a pleasant Indian Hindi or English voice if present
            val preferred = engineVoices?.firstOrNull {
                it.locale.language == "hi" && it.locale.country == "IN"
            } ?: engineVoices?.firstOrNull {
                it.locale.language == "en" && it.locale.country == "IN"
            }
            if (preferred != null) {
                tts?.voice = preferred
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not enumerate TTS voices", e)
        }
    }

    fun selectVoice(voiceName: String) {
        _selectedVoiceName.value = voiceName
        when (voiceName) {
            "default_indian_female" -> {
                setPitch(1.08f)
                setSpeechRate(_speechRate.value)
            }
            "jarvis_crisp" -> {
                setPitch(0.92f)
                setSpeechRate(_speechRate.value)
            }
            else -> {
                try {
                    val match = tts?.voices?.firstOrNull { it.name == voiceName }
                    if (match != null) {
                        tts?.voice = match
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Voice selection failed", e)
                }
            }
        }
    }

    fun speak(text: String, preferredLanguage: String = "hi") {
        if (!_isInitialized.value || tts == null) {
            Log.w(tag, "TTS not yet initialized, re-initializing...")
            initTts()
            return
        }

        stop()

        val cleanText = text
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("\\[MEMORY:[^\\]]+\\]"), "")
            .replace(Regex("\\[EMOTION:[^\\]]+\\]"), "")
            .replace(Regex("[*#_`~]"), "")
            .replace("❤️", "")
            .trim()

        if (cleanText.isBlank()) return
        lastSpokenText = cleanText

        // Detect Devanagari script or Hindi preference
        val containsDevanagari = cleanText.any { it in '\u0900'..'\u097F' }
        try {
            if (containsDevanagari || preferredLanguage == "hi") {
                val avail = tts?.isLanguageAvailable(hindiLocale)
                if (avail != null && avail >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = hindiLocale
                } else {
                    tts?.language = indianEnglishLocale
                }
            } else {
                val avail = tts?.isLanguageAvailable(indianEnglishLocale)
                if (avail != null && avail >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = indianEnglishLocale
                } else {
                    tts?.language = Locale.getDefault()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Language switch error", e)
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val utteranceId = "aaru_${System.currentTimeMillis()}"
        _isSpeaking.value = true
        // Pad with a leading space and trailing period so TTS engines never clip first/last words
        val paddedSpeech = " $cleanText. "
        tts?.speak(paddedSpeech, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun repeatLast(preferredLanguage: String = "hi"): Boolean {
        if (lastSpokenText.isBlank()) return false
        speak(lastSpokenText, preferredLanguage)
        return true
    }

    fun getLastSpokenText(): String = lastSpokenText

    fun isHindiSupported(): Boolean {
        val res = tts?.isLanguageAvailable(hindiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return res >= TextToSpeech.LANG_AVAILABLE
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _speechPitch.value = clamped
        tts?.setPitch(clamped)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            _isInitialized.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}
