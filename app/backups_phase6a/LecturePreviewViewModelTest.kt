package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.preview.LecturePreviewViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Screen 2: [LecturePreviewViewModel] verifying voice-to-text,
 * manual text input, combined input, language selection, and session persistence.
 */
class LecturePreviewViewModelTest {

    @Before
    fun setUp() {
        SessionManager.setLastTranscription(null, emptyList())
        SessionManager.setLastRecordedAudioPath(null)
    }

    @After
    fun tearDown() {
        SessionManager.setLastTranscription(null, emptyList())
        SessionManager.setLastRecordedAudioPath(null)
    }

    @Test
    fun testInitialState_defaultValues() {
        val viewModel = LecturePreviewViewModel()
        val state = viewModel.uiState.value

        assertEquals("Database Normalization", state.lectureTitle)
        assertEquals("en", state.selectedLanguageCode)
        assertFalse(state.isListening)
        assertFalse(state.isTranscribing)
        assertEquals("", state.recognizedText)
    }

    @Test
    fun testManualTextInput_updatesStateAndSessionManager() {
        val viewModel = LecturePreviewViewModel()
        val typedText = "Database normalization is used to organize data."

        viewModel.onRecognizedTextChanged(typedText)

        assertEquals(typedText, viewModel.uiState.value.recognizedText)
        assertEquals(typedText, SessionManager.getLastTranscriptionText())
        val paragraphs = SessionManager.getLastTranscriptionParagraphs()
        assertTrue("Paragraphs should not be empty", paragraphs.isNotEmpty())
        assertEquals(typedText, paragraphs[0].text)
    }

    @Test
    fun testCombinedVoiceAndManualText_preservesBothInputs() {
        val viewModel = LecturePreviewViewModel()

        // 1. First input (simulating recognized speech)
        val spokenSpeech = "Database normalization is used to organize data."
        viewModel.onRecognizedTextChanged(spokenSpeech)
        assertEquals(spokenSpeech, viewModel.uiState.value.recognizedText)

        // 2. Student manually adds a question
        val combined = "$spokenSpeech\nExplain this topic."
        viewModel.onRecognizedTextChanged(combined)

        assertEquals(combined, viewModel.uiState.value.recognizedText)
        assertEquals(combined, SessionManager.getLastTranscriptionText())
        val paragraphs = SessionManager.getLastTranscriptionParagraphs()
        assertEquals(2, paragraphs.size)
        assertEquals("Database normalization is used to organize data.", paragraphs[0].text)
        assertEquals("Explain this topic.", paragraphs[1].text)
    }

    @Test
    fun testClearText_resetsTextAndSession() {
        val viewModel = LecturePreviewViewModel()
        viewModel.onRecognizedTextChanged("Some content")
        assertEquals("Some content", viewModel.uiState.value.recognizedText)

        viewModel.onClearTextClicked()
        assertEquals("", viewModel.uiState.value.recognizedText)
        assertEquals("", SessionManager.getLastTranscriptionText())
        assertTrue(SessionManager.getLastTranscriptionParagraphs().isEmpty())
    }

    @Test
    fun testLanguageSelection_switchesBetweenEnglishAndTamil() {
        val viewModel = LecturePreviewViewModel()
        assertEquals("en", viewModel.uiState.value.selectedLanguageCode)

        viewModel.onLanguageSelected("ta")
        assertEquals("ta", viewModel.uiState.value.selectedLanguageCode)

        viewModel.onLanguageSelected("en")
        assertEquals("en", viewModel.uiState.value.selectedLanguageCode)
    }

    @Test
    fun testGenerateTranscriptClicked_validatesInput() {
        val viewModel = LecturePreviewViewModel()

        // Blank text and no file -> returns false with error
        val canProceedEmpty = viewModel.onGenerateTranscriptClicked()
        assertFalse(canProceedEmpty)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Valid text -> returns true and stores to session
        viewModel.onRecognizedTextChanged("Lecture on algorithms")
        val canProceedValid = viewModel.onGenerateTranscriptClicked()
        assertTrue(canProceedValid)
        assertEquals("Lecture on algorithms", SessionManager.getLastTranscriptionText())
    }

    @Test
    fun testReopeningScreen_restoresPreviouslySavedText() {
        // Given text was saved previously in session
        SessionManager.setLastTranscription(
            "Previously saved lecture note",
            emptyList()
        )

        // When a new LecturePreviewViewModel is created
        val newViewModel = LecturePreviewViewModel()

        // Then previous text is restored
        assertEquals("Previously saved lecture note", newViewModel.uiState.value.recognizedText)
    }
}

