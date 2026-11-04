package com.example.Simtas.data.ml

import android.content.Context
import com.example.Simtas.ui.transcript.TranscriptParagraph
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class OfflineTranscriptionResult(
    val isSuccess: Boolean,
    val fullTranscript: String = "",
    val paragraphs: List<TranscriptParagraph> = emptyList(),
    val detectedLanguage: String = "English",
    val durationText: String = "",
    val errorMessage: String? = null
)

/**
 * Offline Multilingual Speech-to-Text Recognizer powered by Sherpa-ONNX and Whisper.
 * Operates 100% on-device with zero server dependencies, supporting English, Tamil,
 * and 90+ additional languages.
 */
object OfflineSpeechRecognizer {

    // Supported Languages in Whisper Multilingual
    val SUPPORTED_LANGUAGES = listOf(
        "" to "Auto Detect",
        "en" to "English",
        "ta" to "Tamil (தமிழ்)",
        "hi" to "Hindi (हिन्दी)",
        "es" to "Spanish (Español)",
        "fr" to "French (Français)",
        "de" to "German (Deutsch)",
        "zh" to "Chinese (中文)",
        "te" to "Telugu (తెలుగు)",
        "kn" to "Kannada (ಕನ್ನಡ)",
        "ml" to "Malayalam (മലയാളം)",
        "mr" to "Marathi (मराठी)",
        "bn" to "Bengali (বাংলা)"
    )

    private var activeRecognizer: OfflineRecognizer? = null
    private var currentConfigKey: String? = null
    private val recognizerLock = Any()

    /**
     * Initializes or retrieves a cached OfflineRecognizer instance for the specified language.
     */
    fun getRecognizer(context: Context, languageCode: String): OfflineRecognizer {
        synchronized(recognizerLock) {
            val modelFiles = WhisperModelManager.getModelFiles(context)
                ?: throw IllegalStateException("Offline speech model is not installed. Please download the model files first.")

            val configKey = "${modelFiles.encoder.path}_${languageCode}"
            if (activeRecognizer != null && currentConfigKey == configKey) {
                return activeRecognizer!!
            }

            activeRecognizer?.release()

            val whisperConfig = OfflineWhisperModelConfig().apply {
                this.encoder = modelFiles.encoder.absolutePath
                this.decoder = modelFiles.decoder.absolutePath
                this.language = languageCode
                this.task = "transcribe"
                this.tailPaddings = 0
            }

            val modelConfig = OfflineModelConfig().apply {
                this.whisper = whisperConfig
                this.tokens = modelFiles.tokens.absolutePath
                this.numThreads = 2
                this.modelType = "whisper"
                this.debug = false
            }

            val recognizerConfig = OfflineRecognizerConfig().apply {
                this.modelConfig = modelConfig
            }

            val recognizer = OfflineRecognizer(context.assets, recognizerConfig)
            activeRecognizer = recognizer
            currentConfigKey = configKey
            return recognizer
        }
    }

    /**
     * Transcribes an audio file on device completely offline.
     */
    suspend fun transcribeAudioFile(
        context: Context,
        audioFile: File,
        languageCode: String = "",
        onProgress: ((percent: Int) -> Unit)? = null
    ): OfflineTranscriptionResult = withContext(Dispatchers.Default) {
        try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext OfflineTranscriptionResult(
                    isSuccess = false,
                    errorMessage = "Audio file is missing or has zero length."
                )
            }

            if (!WhisperModelManager.isModelInstalled(context)) {
                return@withContext OfflineTranscriptionResult(
                    isSuccess = false,
                    errorMessage = "Offline speech model is not installed. Please download the offline model first."
                )
            }

            onProgress?.invoke(5)

            // 1. Extract 16kHz mono float samples
            val samples = AudioPcmExtractor.extractPcmSamples(audioFile)
            if (samples.isEmpty()) {
                return@withContext OfflineTranscriptionResult(
                    isSuccess = false,
                    errorMessage = "No playable audio data could be decoded from the recording."
                )
            }

            onProgress?.invoke(20)

            // 2. Initialize Recognizer
            val recognizer = getRecognizer(context, languageCode)

            // 3. Process in 30-second windows (Whisper native window is 30s = 480,000 samples @ 16kHz)
            val windowSize = 16000 * 30
            val totalSamples = samples.size
            val numWindows = ((totalSamples + windowSize - 1) / windowSize).coerceAtLeast(1)

            val paragraphs = mutableListOf<TranscriptParagraph>()
            val fullTextBuilder = StringBuilder()
            var detectedLang = if (languageCode.isNotBlank()) languageCode else "English"

            for (w in 0 until numWindows) {
                if (!currentCoroutineContext().isActive) {
                    return@withContext OfflineTranscriptionResult(
                        isSuccess = false,
                        errorMessage = "Transcription was cancelled by user."
                    )
                }

                val start = w * windowSize
                val end = (start + windowSize).coerceAtMost(totalSamples)
                val windowSamples = samples.copyOfRange(start, end)

                val stream = recognizer.createStream()
                try {
                    stream.acceptWaveform(windowSamples, 16000)
                    recognizer.decode(stream)
                    val result = recognizer.getResult(stream)

                    val text = result.text.trim()
                    if (result.lang.isNotBlank()) {
                        detectedLang = result.lang
                    }

                    if (text.isNotBlank()) {
                        val startSec = (start / 16000)
                        val mins = startSec / 60
                        val secs = startSec % 60
                        val timestamp = String.format(Locale.US, "%02d:%02d", mins, secs)

                        paragraphs.add(
                            TranscriptParagraph(
                                timestamp = timestamp,
                                speaker = "Speaker",
                                text = text
                            )
                        )
                        if (fullTextBuilder.isNotEmpty()) fullTextBuilder.append(" ")
                        fullTextBuilder.append(text)
                    }
                } finally {
                    stream.release()
                }

                val currentPercent = 20 + ((w + 1) * 75 / numWindows)
                onProgress?.invoke(currentPercent.coerceIn(20, 95))
            }

            onProgress?.invoke(100)

            val totalSec = totalSamples / 16000
            val totalMins = totalSec / 60
            val totalRemainingSec = totalSec % 60
            val durationStr = String.format(Locale.US, "%02d:%02d", totalMins, totalRemainingSec)

            // Map language code to human-readable name
            val langDisplayName = SUPPORTED_LANGUAGES.find { it.first.equals(detectedLang, ignoreCase = true) }?.second
                ?: detectedLang.replaceFirstChar { it.uppercase() }

            if (paragraphs.isEmpty()) {
                paragraphs.add(
                    TranscriptParagraph(
                        timestamp = "00:00",
                        speaker = "Speaker",
                        text = "No clear speech detected in this recording."
                    )
                )
            }

            OfflineTranscriptionResult(
                isSuccess = true,
                fullTranscript = fullTextBuilder.toString(),
                paragraphs = paragraphs,
                detectedLanguage = langDisplayName,
                durationText = durationStr
            )

        } catch (e: Exception) {
            OfflineTranscriptionResult(
                isSuccess = false,
                errorMessage = "Offline speech recognition error: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    fun release() {
        synchronized(recognizerLock) {
            activeRecognizer?.release()
            activeRecognizer = null
            currentConfigKey = null
        }
    }
}
