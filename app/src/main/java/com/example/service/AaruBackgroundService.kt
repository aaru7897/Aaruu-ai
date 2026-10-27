package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.actions.DeviceAction
import com.example.actions.DeviceActionHandler
import com.example.ai.AaruAiService
import com.example.data.local.AaruDatabase
import com.example.data.repository.AaruRepository
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AaruBackgroundService : Service() {

    private val tag = "AaruBgService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var ttsManager: TextToSpeechManager? = null
    private val aiService = AaruAiService()
    private lateinit var repository: AaruRepository

    private var isListening = false
    private var shouldKeepListening = true

    companion object {
        const val CHANNEL_ID = "aaru_bg_assistant_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.service.START_LISTENING"
        const val ACTION_STOP = "com.example.service.STOP_LISTENING"

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        var isRunning: Boolean
            get() = _isRunningFlow.value
            private set(value) {
                _isRunningFlow.value = value
            }

        var wakeWordRequired: Boolean = true

        fun start(context: Context) {
            val intent = Intent(context, AaruBackgroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("AaruBgService", "Could not start foreground service", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AaruBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("AaruBgService", "Could not stop service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()

        val db = AaruDatabase.getDatabase(this)
        repository = AaruRepository(db.aaruDao())

        ttsManager = TextToSpeechManager(this) {
            // Wait 450ms after TTS finishes so Aaru never hears its own echo
            if (shouldKeepListening) {
                serviceScope.launch {
                    delay(450)
                    startBackgroundListening()
                }
            }
        }

        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shouldKeepListening = false
                isRunning = false
                stopBackgroundListening()
                ttsManager?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                shouldKeepListening = true
                isRunning = true
                val modeDesc = if (wakeWordRequired) "Wake-word active: Say 'Aaru, suno!'" else "Continuous background listening active"
                val notification = buildNotification("🎤 Mic Active • $modeDesc")
                startForeground(NOTIFICATION_ID, notification)
                startBackgroundListening()
            }
        }
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(tag, "SpeechRecognizer not available")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        if (shouldKeepListening && ttsManager?.isSpeaking?.value != true) {
                            serviceScope.launch {
                                delay(1200)
                                startBackgroundListening()
                            }
                        }
                    }

                    override fun onResults(results: android.os.Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        Log.d(tag, "Background heard: $text")

                        if (text.isNotBlank()) {
                            handleBackgroundHeardText(text)
                        } else if (shouldKeepListening) {
                            startBackgroundListening()
                        }
                    }

                    override fun onPartialResults(partialResults: android.os.Bundle?) {}
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to init SpeechRecognizer", e)
        }
    }

    private fun startBackgroundListening() {
        if (!shouldKeepListening || ttsManager?.isSpeaking?.value == true) return

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e(tag, "startListening error", e)
            isListening = false
        }
    }

    private fun stopBackgroundListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            isListening = false
        } catch (e: Exception) {
            Log.e(tag, "stopListening error", e)
        }
    }

    private fun handleBackgroundHeardText(spokenText: String) {
        val lower = spokenText.lowercase()

        val hasWakeWord = lower.contains("aaru") || spokenText.contains("आरू") ||
                lower.startsWith("search") || lower.startsWith("open") || lower.startsWith("play") ||
                spokenText.contains("यूट्यूब") || spokenText.contains("खोलो")

        if (wakeWordRequired && !hasWakeWord) {
            if (shouldKeepListening) {
                startBackgroundListening()
            }
            return
        }

        serviceScope.launch {
            updateNotification("⚡ Processing: \"$spokenText\"")
            val memories = repository.getAllMemoriesList()
            val recent = repository.getRecentMessages(6)

            val aiResponse = aiService.processUserMessage(spokenText, memories, recent, "hi")

            repository.addMessage("user", spokenText)
            repository.addMessage("aaru", aiResponse.replyText)

            when (val act = aiResponse.detectedAction) {
                is DeviceAction.ControlCommand -> {
                    if (act.command == "STOP") {
                        ttsManager?.stop()
                    }
                }
                is DeviceAction.SendWhatsApp -> {
                    // Ask Boss to review message or open WhatsApp pre-filled
                    val actionResult = DeviceActionHandler.execute(applicationContext, act)
                    ttsManager?.speak(actionResult.feedbackMessage, "hi")
                }
                !is DeviceAction.None -> {
                    val actionResult = DeviceActionHandler.execute(applicationContext, act)
                    ttsManager?.speak("${aiResponse.replyText} ${actionResult.feedbackMessage}", "hi")
                }
                else -> {
                    ttsManager?.speak(aiResponse.replyText, "hi")
                }
            }

            updateNotification("🎤 Mic Active • Say 'Aaru, suno!' or tap Stop")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aaru AI Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows microphone status when Aaru AI is running in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AaruBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aaru AI — Personal JARVIS Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Assistant", pendingStop)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        shouldKeepListening = false
        serviceScope.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        ttsManager?.shutdown()
        ttsManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
