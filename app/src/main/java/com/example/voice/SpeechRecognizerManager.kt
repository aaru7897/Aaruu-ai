package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onFinalResult: (String) -> Unit,
    private val onErrorCallback: ((String?) -> Unit)? = null
) {
    private val tag = "SpeechRecognizerManager"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var activeLanguageCode: String = "hi" // "hi", "hinglish", "en"

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        _isAvailable.value = available
        if (!available) {
            Log.w(tag, "Speech recognition service is not available on this device")
            _errorMessage.value = "Speech recognition service unavailable on this device."
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to create SpeechRecognizer", e)
            _errorMessage.value = "Could not initialize SpeechRecognizer: ${e.localizedMessage}"
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _errorMessage.value = null
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _soundLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _soundLevel.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _soundLevel.value = 0f
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> null // Normal silence
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null // Normal timeout
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice network interrupted. Check connection."
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required Boss!"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy."
                    else -> null
                }
                Log.d(tag, "SpeechRecognizer error code: $error ($message)")
                if (message != null) {
                    _errorMessage.value = message
                }
                onErrorCallback?.invoke(message)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _soundLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalSpeech = matches?.firstOrNull()?.trim()
                if (!finalSpeech.isNullOrBlank()) {
                    _partialText.value = finalSpeech
                    onFinalResult(finalSpeech)
                } else {
                    onErrorCallback?.invoke(null)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val liveText = matches?.firstOrNull()
                if (!liveText.isNullOrBlank()) {
                    _partialText.value = liveText
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (_isPaused.value) {
            Log.d(tag, "Listening is paused by user; skipping startListening")
            return
        }

        if (speechRecognizer == null) {
            initRecognizer()
        }

        val targetLocaleTag = when (activeLanguageCode) {
            "hi" -> "hi-IN"
            "hinglish" -> "en-IN"
            "en" -> "en-US"
            else -> "hi-IN"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocaleTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocaleTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            _partialText.value = ""
            _errorMessage.value = null
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(tag, "startListening error", e)
            _isListening.value = false
            _errorMessage.value = "Failed to start microphone: ${e.message}"
        }
    }

    fun pauseListening() {
        _isPaused.value = true
        stopListening()
    }

    fun resumeListening() {
        _isPaused.value = false
        startListening()
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            _isListening.value = false
            _soundLevel.value = 0f
        } catch (e: Exception) {
            Log.e(tag, "stopListening error", e)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun destroy() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
        } catch (e: Exception) {
            Log.e(tag, "destroy error", e)
        }
    }
}
