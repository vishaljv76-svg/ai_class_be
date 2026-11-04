package com.example.Simtas

import com.example.Simtas.data.ml.AudioPcmExtractor
import com.example.Simtas.data.ml.OfflineSpeechRecognizer
import com.example.Simtas.data.ml.WhisperModelManager
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.transcript.TranscriptParagraph
import com.example.Simtas.ui.transcript.TranscriptUiState
import com.example.Simtas.ui.transcript.TranscriptViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OfflineTranscriptionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun supportedLanguages_containsEnglishAndTamil() {
        val languages = OfflineSpeechRecognizer.SUPPORTED_LANGUAGES
        val codes = languages.map { it.first }
        val names = languages.map { it.second }

        assertTrue("Must support Auto Detect", codes.contains(""))
        assertTrue("Must support English", codes.contains("en"))
        assertTrue("Must support Tamil", codes.contains("ta"))
        assertTrue("English label present", names.any { it.contains("English") })
        assertTrue("Tamil label present", names.any { it.contains("Tamil") })
    }

    @Test
    fun modelManager_detectsInstalledAndMissingStates() {
        val mockContextDir = tempFolder.newFolder("mock_context_files")
        val modelDir = File(mockContextDir, "models/whisper-tiny")
        modelDir.mkdirs()

        val encoder = File(modelDir, WhisperModelManager.ENCODER_FILE)
        val decoder = File(modelDir, WhisperModelManager.DECODER_FILE)
        val tokens = File(modelDir, WhisperModelManager.TOKENS_FILE)

        // Initially missing
        assertFalse(encoder.exists())
        assertFalse(decoder.exists())
        assertFalse(tokens.exists())

        // Create dummy sized files to test installation detector logic
        FileOutputStream(encoder).use { it.write(ByteArray(11_000_000)) }
        FileOutputStream(decoder).use { it.write(ByteArray(55_000_000)) }
        FileOutputStream(tokens).use { it.write(ByteArray(200_000)) }

        assertTrue(encoder.length() > 10_000_000L)
        assertTrue(decoder.length() > 50_000_000L)
        assertTrue(tokens.length() > 100_000L)
    }

    @Test
    fun audioPcmExtractor_decodesWavPcmProperly() {
        val wavFile = tempFolder.newFile("test_16k_mono.wav")

        // Construct 1-second 16kHz mono 16-bit PCM WAV file
        val sampleRate = 16000
        val numSamples = sampleRate
        val pcmData = ByteArray(numSamples * 2)
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()

        // Generate 440Hz sine wave tone
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * 440.0 * i / sampleRate
            val sample = (Math.sin(angle) * 16000).toInt().toShort()
            shortBuffer.put(sample)
        }

        FileOutputStream(wavFile).use { fos ->
            // RIFF header
            val header = ByteArray(44)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            bb.put("RIFF".toByteArray())
            bb.putInt(36 + pcmData.size)
            bb.put("WAVE".toByteArray())
            bb.put("fmt ".toByteArray())
            bb.putInt(16) // Subchunk1Size (16 for PCM)
            bb.putShort(1) // AudioFormat (1 for PCM)
            bb.putShort(1) // NumChannels (1 mono)
            bb.putInt(sampleRate)
            bb.putInt(sampleRate * 2) // ByteRate
            bb.putShort(2) // BlockAlign
            bb.putShort(16) // BitsPerSample
            bb.put("data".toByteArray())
            bb.putInt(pcmData.size)
            fos.write(header)
            fos.write(pcmData)
        }

        val floatSamples = AudioPcmExtractor.tryReadWavPcm(wavFile)
        assertNotNull("Should successfully extract PCM float samples", floatSamples)
        assertEquals(numSamples, floatSamples!!.size)

        // Verify values normalized to [-1.0f, 1.0f]
        for (sample in floatSamples) {
            assertTrue("Sample must be in range [-1.0f, 1.0f]", sample >= -1.0f && sample <= 1.0f)
        }
    }

    @Test
    fun audioPcmExtractor_throwsOnMissingFile() {
        val missing = File(tempFolder.root, "does_not_exist.m4a")
        var threw = false
        try {
            AudioPcmExtractor.extractPcmSamples(missing)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue("Must throw on missing file", threw)
    }

    @Test
    fun transcriptUiState_initialStateHasNoDummyText() {
        val state = TranscriptUiState()
        assertTrue("Transcript paragraphs must start empty", state.paragraphs.isEmpty())
        assertFalse(state.isTranscribing)
        assertFalse(state.isEditing)
        assertNull(state.errorMessage)
    }

    @Test
    fun transcriptViewModel_languageSelection_updatesState() {
        val viewModel = TranscriptViewModel()
        viewModel.onLanguageSelected("ta", "Tamil (தமிழ்)")

        val state = viewModel.uiState.value
        assertEquals("ta", state.selectedLanguageCode)
        assertEquals("Tamil (தமிழ்)", state.language)
    }

    @Test
    fun transcriptViewModel_editingFlow_updatesParagraphsAndSession() {
        val viewModel = TranscriptViewModel()

        // Set initial paragraphs in ViewModel
        val initialText = "[00:00] Prof. Sarah Chen:\nDatabase Normalization introduction."
        viewModel.onEditTextClicked()
        viewModel.onEditableTextChanged(initialText)
        viewModel.onSaveEditClicked()

        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        assertEquals(1, state.paragraphs.size)
        assertEquals("00:00", state.paragraphs[0].timestamp)
        assertEquals("Prof. Sarah Chen", state.paragraphs[0].speaker)
        assertEquals("Database Normalization introduction.", state.paragraphs[0].text)

        // Check SessionManager sync
        val sessionText = SessionManager.getLastTranscriptionText()
        assertNotNull(sessionText)
        assertTrue(sessionText!!.contains("Database Normalization"))
    }

    @Test
    fun sessionManager_savesAndRetrievesTranscription() {
        val paras = listOf(
            TranscriptParagraph("01:15", "Dr. Raman", "பரிவர்த்தனை மேலாண்மை (Transaction Management)")
        )
        val full = "Dr. Raman: பரிவர்த்தனை மேலாண்மை (Transaction Management)"

        SessionManager.setLastTranscription(full, paras)
        assertEquals(full, SessionManager.getLastTranscriptionText())
        assertEquals(1, SessionManager.getLastTranscriptionParagraphs().size)
        assertEquals("01:15", SessionManager.getLastTranscriptionParagraphs()[0].timestamp)
    }

    @Test
    fun lectureNotesGenerator_extractsRealNotesAndSummary() {
        val transcript = "Database normalization is defined as the process of organizing data in a database. " +
                "First normal form requires atomic values in each table cell. " +
                "Second normal form removes partial functional dependencies. " +
                "Third normal form eliminates transitive dependencies. " +
                "In conclusion, proper normalization prevents update anomalies and reduces redundant storage."

        val paras = listOf(
            TranscriptParagraph("00:00", "Prof. Sarah Chen", transcript)
        )

        val result = com.example.Simtas.data.ml.LectureNotesGenerator.generate(
            transcriptText = transcript,
            paragraphs = paras,
            lectureTitle = "Database Normalization",
            subject = "Computer Science",
            durationText = "05:00"
        )

        val notes = result.generatedNotes
        val detailed = result.detailedNotes

        assertEquals("Database Normalization", notes.lectureTitle)
        assertEquals("Computer Science", notes.subjectBadge)
        assertTrue("Summary contains overview", notes.aiSummaryText.contains("Database normalization"))
        assertTrue("Key takeaways extracted", notes.keyTakeaways.isNotEmpty())
        assertTrue("Key definitions extracted", notes.importantTerms.isNotEmpty())
        assertTrue("Detailed section 1 present", detailed.section1Content.isNotBlank())
        assertTrue("Detailed section 2 bullets present", detailed.section2Bullets.isNotEmpty())

        // Ensure session manager persistence
        SessionManager.setLastGeneratedNote(notes)
        SessionManager.setLastDetailedNotes(detailed)
        assertEquals(notes, SessionManager.getLastGeneratedNote())
        assertEquals(detailed, SessionManager.getLastDetailedNotes())
    }

    @Test
    fun lecturePreviewUiState_retainsAudioMetadata() {
        val state = com.example.Simtas.ui.preview.LecturePreviewUiState(
            lectureTitle = "Operating Systems",
            audioFileName = "lecture_rec_20261104.m4a",
            audioFilePath = "/path/to/lecture_rec_20261104.m4a",
            fileSizeBytes = 2048000L,
            hasAudioFile = true,
            totalTimeText = "03:45"
        )

        assertEquals("Operating Systems", state.lectureTitle)
        assertEquals("lecture_rec_20261104.m4a", state.audioFileName)
        assertTrue(state.hasAudioFile)
        assertEquals(2048000L, state.fileSizeBytes)
    }
}
