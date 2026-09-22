package com.example.data.safety

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Voice keyword detection service for emergency voice triggers.
 * Listens for keywords like "help", "emergency", "ajiya", "danger", "save me", "sos".
 */
class VoiceKeywordDetector(
    private val context: Context,
    private val onKeywordDetected: (keyword: String) -> Unit,
    private val onStatusUpdate: (status: String) -> Unit = {}
) {
    private val tag = "VoiceKeywordDetector"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main)

    val emergencyKeywords = listOf("help", "emergency", "ajiya", "danger", "save me", "sos", "attack")

    fun startListening(): Boolean {
        if (isListening) return true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(tag, "Speech recognition not available on this device.")
            onStatusUpdate("Voice recognition hardware unavailable")
            return false
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onStatusUpdate("Listening for emergency keywords (\"help\", \"emergency\", \"ajiya\")...")
            Log.i(tag, "Voice keyword detection started.")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognizer: ${e.message}")
            onStatusUpdate("Failed to initialize voice listener: ${e.message}")
            isListening = false
            return false
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
            isListening = false
            onStatusUpdate("Voice trigger paused")
            Log.i(tag, "Voice keyword detection stopped.")
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    /**
     * Allows manual or simulation testing of voice trigger keywords
     * in emulators or silent testing environments.
     */
    fun simulateVoiceInput(spokenText: String) {
        val lower = spokenText.lowercase(Locale.getDefault())
        val matched = emergencyKeywords.find { lower.contains(it) }
        if (matched != null) {
            onStatusUpdate("Keyword matched: \"$matched\"! Triggering SOS...")
            onKeywordDetected(matched)
        } else {
            onStatusUpdate("Heard: \"$spokenText\" (no emergency keyword matched)")
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onStatusUpdate("Voice detector active - Say \"Help\" or \"Emergency\"")
        }

        override fun onBeginningOfSpeech() {
            onStatusUpdate("Detecting speech...")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            onStatusUpdate("Processing speech...")
        }

        override fun onError(error: Int) {
            Log.w(tag, "Speech recognition error code: $error")
            // Automatically restart listening after temporary pause/timeout if enabled
            if (isListening) {
                scope.launch {
                    delay(1000)
                    restartListeningLoop()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            processMatches(matches)
            if (isListening) {
                scope.launch {
                    delay(500)
                    restartListeningLoop()
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            processMatches(matches)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processMatches(matches: List<String>?) {
        if (matches.isNullOrEmpty()) return
        for (phrase in matches) {
            val lower = phrase.lowercase(Locale.getDefault())
            val matchedKeyword = emergencyKeywords.find { lower.contains(it) }
            if (matchedKeyword != null) {
                Log.w(tag, "EMERGENCY KEYWORD MATCHED: $matchedKeyword in phrase: \"$phrase\"")
                onStatusUpdate("EMERGENCY KEYWORD \"$matchedKeyword\" DETECTED!")
                onKeywordDetected(matchedKeyword)
                return
            }
        }
    }

    private fun restartListeningLoop() {
        if (!isListening) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error restarting speech loop: ${e.message}")
        }
    }
}
