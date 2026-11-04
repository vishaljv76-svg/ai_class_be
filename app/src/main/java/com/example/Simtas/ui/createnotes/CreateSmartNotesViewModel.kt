package com.example.Simtas.ui.createnotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AiNotesRepository
import com.example.Simtas.ui.home.HomeBottomTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [CreateSmartNotesScreen] adhering to MVVM architecture with real AI notes generation.
 */
class CreateSmartNotesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSmartNotesUiState())
    val uiState: StateFlow<CreateSmartNotesUiState> = _uiState.asStateFlow()

    init {
        startNotesGeneration()
    }

    fun startNotesGeneration() {
        val audioTitle = SessionManager.getLastUploadedAudioTitle() ?: "Database Normalization"
        _uiState.update {
            it.copy(
                subtitleText = "Turning $audioTitle into notes that make sense.",
                currentStepText = "Analyzing lecture transcript",
                progressPercent = 45,
                progressStatusText = "Connecting ideas · Extracting key concepts",
                isGenerating = true,
                isGenerationComplete = false,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val transcriptionId = SessionManager.getLastTranscriptionId()
            val audioId = SessionManager.getLastUploadedAudioId()

            _uiState.update {
                it.copy(
                    currentStepText = "Structuring your study notes",
                    progressPercent = 75,
                    progressStatusText = "Generating summary & takeaways"
                )
            }

            val result = AiNotesRepository.generateNotes(
                transcriptionId = transcriptionId,
                audioId = audioId,
                lectureTitle = audioTitle,
                saveToDb = false
            )

            if (result.isSuccess && result.notesUiState != null) {
                SessionManager.setLastGeneratedNote(result.notesUiState)
                _uiState.update {
                    it.copy(
                        currentStepText = "Notes ready!",
                        progressPercent = 100,
                        progressStatusText = "All sections organized and ready to review",
                        isGenerating = false,
                        isGenerationComplete = true
                    )
                }
            } else {
                // Generate real notes & summary from actual transcript
                val transcriptText = SessionManager.getLastTranscriptionText() ?: ""
                val paragraphs = SessionManager.getLastTranscriptionParagraphs()
                val duration = SessionManager.getLastUploadedAudioDuration() ?: "Recorded Lecture"

                val extractive = com.example.Simtas.data.ml.LectureNotesGenerator.generate(
                    transcriptText = transcriptText,
                    paragraphs = paragraphs,
                    lectureTitle = audioTitle,
                    subject = "Computer Science",
                    durationText = duration
                )

                SessionManager.setLastGeneratedNote(extractive.generatedNotes)
                SessionManager.setLastDetailedNotes(extractive.detailedNotes)

                _uiState.update {
                    it.copy(
                        currentStepText = "Notes ready!",
                        progressPercent = 100,
                        progressStatusText = "Extracted lecture summary & takeaways ready to review",
                        isGenerating = false,
                        isGenerationComplete = true,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun selectTab(tab: HomeBottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
