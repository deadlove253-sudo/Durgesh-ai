package com.example.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.ListeningState
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceEngineCapabilities(
  val speechRecognizerAvailable: Boolean = true,
  val ttsInitialized: Boolean = false,
  val hindiTtsSupported: Boolean = false,
  val ttsStatusNote: String = "हिंदी आवाज़ इंजन तैयार हो रहा है…",
)

class HindiVoiceEngine(
  private val context: Context,
  private val onSpeechRecognized: (String) -> Unit,
  private val onRecognitionError: (hindiMessage: String, englishMessage: String) -> Unit,
) {
  private val appContext = context.applicationContext
  private var speechRecognizer: SpeechRecognizer? = null
  private var textToSpeech: TextToSpeech? = null

  private val _listeningState = MutableStateFlow<ListeningState>(ListeningState.Idle)
  val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

  private val _capabilities =
    MutableStateFlow(
      VoiceEngineCapabilities(
        speechRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
      )
    )
  val capabilities: StateFlow<VoiceEngineCapabilities> = _capabilities.asStateFlow()

  init {
    initTextToSpeech()
  }

  private fun initTextToSpeech() {
    try {
      textToSpeech =
        TextToSpeech(appContext) { status ->
          if (status == TextToSpeech.SUCCESS) {
            val hindiLocale = Locale.forLanguageTag("hi-IN")
            val result = textToSpeech?.setLanguage(hindiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            val supported =
              result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            val note =
              if (supported) {
                "Hindi TTS (hi-IN) तैयार है"
              } else {
                "डिवाइस पर हिंदी TTS डेटा इंस्टॉल नहीं है — फ़ोन TTS सेटिंग्स से Hindi डाउनलोड करें।"
              }
            _capabilities.value =
              _capabilities.value.copy(
                ttsInitialized = true,
                hindiTtsSupported = supported,
                ttsStatusNote = note,
              )
            textToSpeech?.setOnUtteranceProgressListener(
              object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                  _listeningState.value = ListeningState.Speaking(utteranceId ?: "")
                }

                override fun onDone(utteranceId: String?) {
                  if (_listeningState.value is ListeningState.Speaking) {
                    _listeningState.value = ListeningState.Idle
                  }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                  if (_listeningState.value is ListeningState.Speaking) {
                    _listeningState.value = ListeningState.Idle
                  }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                  if (_listeningState.value is ListeningState.Speaking) {
                    _listeningState.value = ListeningState.Idle
                  }
                }
              }
            )
          } else {
            _capabilities.value =
              _capabilities.value.copy(
                ttsInitialized = false,
                hindiTtsSupported = false,
                ttsStatusNote = "TextToSpeech इंजन शुरू नहीं हो सका।",
              )
          }
        }
    } catch (e: Exception) {
      Log.w("HindiVoiceEngine", "TTS init failed", e)
      _capabilities.value =
        _capabilities.value.copy(
          ttsInitialized = false,
          hindiTtsSupported = false,
          ttsStatusNote = "TextToSpeech उपलब्ध नहीं है।",
        )
    }
  }

  fun startListening(preferOffline: Boolean) {
    if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
      _capabilities.value = _capabilities.value.copy(speechRecognizerAvailable = false)
      onRecognitionError(
        "आपके फोन पर Android Speech Recognition सेवा उपलब्ध नहीं है। आप नीचे टाइप करके कमांड दे सकते हैं।",
        "Android SpeechRecognizer service is not available on this device.",
      )
      return
    }

    stopSpeaking()
    destroyRecognizer()

    try {
      val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
      speechRecognizer = recognizer

      recognizer.setRecognitionListener(
        object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _listeningState.value = ListeningState.Listening(rmsDb = 0f, partialText = "")
          }

          override fun onBeginningOfSpeech() {}

          override fun onRmsChanged(rmsdB: Float) {
            val current = _listeningState.value
            if (current is ListeningState.Listening) {
              _listeningState.value = current.copy(rmsDb = rmsdB)
            }
          }

          override fun onBufferReceived(buffer: ByteArray?) {}

          override fun onEndOfSpeech() {
            _listeningState.value = ListeningState.Processing
          }

          override fun onError(error: Int) {
            _listeningState.value = ListeningState.Idle
            val (hindiMsg, engMsg) = mapSpeechError(error, preferOffline)
            onRecognitionError(hindiMsg, engMsg)
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            _listeningState.value = ListeningState.Idle
            if (best.isBlank()) {
              onRecognitionError(
                "आवाज़ साफ़ सुनाई नहीं दी। कृपया फिर से बोलें।",
                "No speech matched. Please try again.",
              )
            } else {
              onSpeechRecognized(best)
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val partial =
              partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            val current = _listeningState.value
            if (current is ListeningState.Listening && partial.isNotBlank()) {
              _listeningState.value = current.copy(partialText = partial)
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        }
      )

      val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
          if (preferOffline) {
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
          }
        }

      _listeningState.value = ListeningState.Listening(rmsDb = 0f, partialText = "")
      recognizer.startListening(intent)
    } catch (e: Exception) {
      _listeningState.value = ListeningState.Idle
      onRecognitionError(
        "वॉइस रिकग्निशन शुरू करने में समस्या आई: ${e.localizedMessage ?: "Unknown error"}",
        "Failed to start SpeechRecognizer.",
      )
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
    } catch (_: Exception) {}
    if (_listeningState.value is ListeningState.Listening) {
      _listeningState.value = ListeningState.Idle
    }
  }

  fun speak(text: String, speechRate: Float = 1.0f) {
    val tts = textToSpeech ?: return
    if (!_capabilities.value.ttsInitialized) return
    tts.setSpeechRate(speechRate.coerceIn(0.6f, 1.5f))
    val utteranceId = "durgesh_${System.currentTimeMillis()}"
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
  }

  fun stopSpeaking() {
    try {
      textToSpeech?.stop()
    } catch (_: Exception) {}
    if (_listeningState.value is ListeningState.Speaking) {
      _listeningState.value = ListeningState.Idle
    }
  }

  private fun destroyRecognizer() {
    try {
      speechRecognizer?.destroy()
    } catch (_: Exception) {}
    speechRecognizer = null
  }

  fun release() {
    destroyRecognizer()
    try {
      textToSpeech?.stop()
      textToSpeech?.shutdown()
    } catch (_: Exception) {}
    textToSpeech = null
  }

  private fun mapSpeechError(error: Int, preferOffline: Boolean): Pair<String, String> {
    return when (error) {
      SpeechRecognizer.ERROR_AUDIO ->
        "माइक्रोफ़ोन से ऑडियो रिकॉर्ड करने में समस्या आई।" to "Audio recording error."
      SpeechRecognizer.ERROR_CLIENT ->
        "वॉइस सेशन बंद किया गया।" to "Speech client stopped."
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
        "माइक्रोफ़ोन अनुमति (RECORD_AUDIO) आवश्यक है।" to "Microphone permission missing."
      SpeechRecognizer.ERROR_NETWORK,
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
        if (preferOffline) {
          "ऑफ़लाइन हिंदी वॉइस पैक नहीं मिला और नेटवर्क उपलब्ध नहीं है। सेटिंग्स से Offline Hindi Speech Pack डाउनलोड करें या नीचे टाइप करें।" to
            "Offline Hindi speech pack missing and network unavailable."
        } else {
          "वॉइस पहचानने के लिए इंटरनेट कनेक्शन नहीं है। सेटिंग्स से ऑफ़लाइन हिंदी पैक डाउनलोड करें या नीचे टाइप करके कमांड दें।" to
            "Network unavailable for online speech recognition."
        }
      SpeechRecognizer.ERROR_NO_MATCH ->
        "आपकी आवाज़ समझ नहीं आई। कृपया साफ़ हिंदी या Hinglish में दोबारा बोलें।" to
          "Could not match speech."
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
        "वॉइस इंजन अभी व्यस्त है। एक पल बाद फिर प्रयास करें।" to "Speech recognizer is busy."
      SpeechRecognizer.ERROR_SERVER ->
        "स्पीच सर्वर से जवाब नहीं मिला। ऑफ़लाइन कमांड नीचे टाइप करके चलाएँ।" to
          "Speech recognition server error."
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
        "कोई आवाज़ सुनाई नहीं दी। माइक दबाकर फिर से बोलें।" to "No speech input detected."
      SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
      SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        "हिंदी (hi-IN) वॉइस पैक इस डिवाइस पर उपलब्ध नहीं है। कृपया Google Voice Input सेटिंग्स से Hindi (India) डाउनलोड करें।" to
          "Hindi (hi-IN) speech model unavailable on device."
      else ->
        "वॉइस पहचानने में त्रुटि आई (कोड $error)।" to "SpeechRecognizer error code $error."
    }
  }
}
