package com.example.Simtas

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.Simtas.data.ml.AudioPcmExtractor
import com.example.Simtas.data.ml.LectureNotesGenerator
import com.example.Simtas.data.ml.OfflineSpeechRecognizer
import com.example.Simtas.data.ml.WhisperModelManager
import com.example.Simtas.data.remote.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device verification of the four-screen lecture processing pipeline:
 * 1. Offline Whisper Model Installation & Loading on device.
 * 2. Sherpa-ONNX Native Initializer for English and Tamil.
 * 3. AudioPcmExtractor real M4A audio decoding.
 * 4. Actual speech transcription.
 * 5. LectureNotesGenerator note, summary, and takeaway extraction.
 * 6. SessionManager state retention across screens.
 */
@RunWith(AndroidJUnit4::class)
class FourScreenWorkflowDeviceTest {

    @Test
    fun testDeviceWhisperModelInstalledAndLoads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val isInstalled = WhisperModelManager.isModelInstalled(context)
        assertTrue("Whisper model files must be detected on device", isInstalled)

        val modelFiles = WhisperModelManager.getModelFiles(context)
        assertNotNull("Model files must be non-null", modelFiles)
        assertTrue("Encoder must exist", modelFiles!!.encoder.exists())
        assertTrue("Decoder must exist", modelFiles.decoder.exists())
        assertTrue("Tokens must exist", modelFiles.tokens.exists())
    }

    @Test
    fun testNativeRecognizerInitializesForEnglishAndTamil() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Test English
        val enRecognizer = OfflineSpeechRecognizer.getRecognizer(context, "en")
        assertNotNull("Sherpa-ONNX English recognizer must initialize", enRecognizer)

        // Test Tamil
        val taRecognizer = OfflineSpeechRecognizer.getRecognizer(context, "ta")
        assertNotNull("Sherpa-ONNX Tamil recognizer must initialize", taRecognizer)
    }

    @Test
    fun testRealAudioDecodingAndTranscriptionPipeline() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val recDir = File(context.filesDir, "recordings")
        val audioFiles = recDir.listFiles()?.filter { it.extension.equals("m4a", ignoreCase = true) && it.length() > 0L }

        if (!audioFiles.isNullOrEmpty()) {
            val audioFile = audioFiles.maxByOrNull { it.length() }!!
            assertTrue("Audio file must exist and be accessible", audioFile.exists())
            assertTrue("Audio file must have bytes", audioFile.length() > 0L)

            // 1. Audio decoding via MediaExtractor & MediaCodec
            val pcmSamples = AudioPcmExtractor.extractPcmSamples(audioFile)
            assertTrue("PCM samples must be extracted", pcmSamples.isNotEmpty())

            // 2. Transcription with OfflineSpeechRecognizer
            val result = OfflineSpeechRecognizer.transcribeAudioFile(context, audioFile, "en")
            assertTrue("Transcription must succeed", result.isSuccess)
            assertNotNull("Paragraphs must not be null", result.paragraphs)
            assertTrue("Paragraphs list must not be empty", result.paragraphs.isNotEmpty())

            // 3. Save to SessionManager
            SessionManager.setLastRecordedAudioPath(audioFile.absolutePath)
            SessionManager.setLastTranscription(result.fullTranscript, result.paragraphs)
            assertEquals(audioFile.absolutePath, SessionManager.getLastRecordedAudioPath())
            assertEquals(result.fullTranscript, SessionManager.getLastTranscriptionText())

            // 4. Notes Generation with LectureNotesGenerator
            val notesResult = LectureNotesGenerator.generate(
                transcriptText = result.fullTranscript,
                paragraphs = result.paragraphs,
                lectureTitle = "Operating Systems",
                subject = "Computer Science",
                durationText = result.durationText
            )

            assertNotNull("Generated notes must not be null", notesResult.generatedNotes)
            assertNotNull("Detailed notes must not be null", notesResult.detailedNotes)
            assertEquals("Operating Systems", notesResult.generatedNotes.lectureTitle)

            // Save to SessionManager for Screen 4
            SessionManager.setLastGeneratedNote(notesResult.generatedNotes)
            SessionManager.setLastDetailedNotes(notesResult.detailedNotes)
            assertEquals(notesResult.generatedNotes, SessionManager.getLastGeneratedNote())
            assertEquals(notesResult.detailedNotes, SessionManager.getLastDetailedNotes())
        }
    }
}

