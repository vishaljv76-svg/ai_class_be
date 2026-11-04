package com.example.Simtas.ui.createnotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AiNotesRepository
import com.example.Simtas.data.repository.JobRepository
import com.example.Simtas.ui.home.HomeBottomTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [CreateSmartNotesScreen] adhering to MVVM architecture with real-time pipeline status tracking.
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
                currentStepText = "Starting processing pipeline...",
                progressPercent = 10,
                progressStatusText = "Connecting to AI backend · Initializing stages",
                isGenerating = true,
                isGenerationComplete = false,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val audioId = SessionManager.getLastUploadedAudioId()
            var jobId = SessionManager.getLastJobId()

            // 1. If no active job ID, ensure job is started on backend
            if (jobId == null && audioId != null && audioId > 0) {
                val startRes = JobRepository.startLectureProcessing(audioId)
                if (startRes.isSuccess && startRes.jobId != null) {
                    jobId = startRes.jobId
                }
            }

            // 2. Poll job status from backend
            var isPipelineFinished = false
            var pollAttempts = 0
            val maxPollAttempts = 30 // ~45 seconds maximum polling

            while (!isPipelineFinished && pollAttempts < maxPollAttempts) {
                pollAttempts++
                val statusResult = JobRepository.getJobStatus(
                    jobId = jobId,
                    audioId = audioId,
                    advance = (pollAttempts == 1 || pollAttempts == 3) // Advance job if server requires trigger
                )

                if (statusResult.isSuccess) {
                    when (statusResult.status.uppercase()) {
                        "UPLOADING" -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = statusResult.currentStep.ifBlank { "Verifying audio file..." },
                                    progressPercent = statusResult.progressPercent.coerceAtLeast(15),
                                    progressStatusText = "Audio storage verified · Preparing transcription"
                                )
                            }
                        }
                        "TRANSCRIBING" -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = statusResult.currentStep.ifBlank { "Transcribing speech to text..." },
                                    progressPercent = statusResult.progressPercent.coerceAtLeast(35),
                                    progressStatusText = "Recognizing lecture speech · Aligning paragraphs"
                                )
                            }
                        }
                        "GENERATING_NOTES" -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = statusResult.currentStep.ifBlank { "Structuring your study notes..." },
                                    progressPercent = statusResult.progressPercent.coerceAtLeast(60),
                                    progressStatusText = "Summarizing concepts · Extracting takeaways & terms"
                                )
                            }
                        }
                        "GENERATING_QUIZ" -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = statusResult.currentStep.ifBlank { "Creating AI quiz questions..." },
                                    progressPercent = statusResult.progressPercent.coerceAtLeast(75),
                                    progressStatusText = "Generating multiple-choice questions & explanations"
                                )
                            }
                        }
                        "SEARCHING_RESOURCES" -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = statusResult.currentStep.ifBlank { "Discovering educational resources..." },
                                    progressPercent = statusResult.progressPercent.coerceAtLeast(88),
                                    progressStatusText = "Finding verified Wikipedia references & textbooks"
                                )
                            }
                        }
                        "SAVING_RESULTS" -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = "Finalizing study package...",
                                    progressPercent = 95,
                                    progressStatusText = "Saving all notes, quizzes and resources to MySQL"
                                )
                            }
                        }
                        "COMPLETED" -> {
                            isPipelineFinished = true
                            if (statusResult.noteUiState != null) {
                                SessionManager.setLastGeneratedNote(statusResult.noteUiState)
                            }
                            _uiState.update {
                                it.copy(
                                    currentStepText = "Notes & Quiz ready!",
                                    progressPercent = 100,
                                    progressStatusText = "All learning materials prepared and saved",
                                    isGenerating = false,
                                    isGenerationComplete = true
                                )
                            }
                            break
                        }
                        "FAILED" -> {
                            isPipelineFinished = true
                            // If backend reports external service unconfigured, fall back smoothly
                            val fallback = AiNotesRepository.generateNotes(
                                transcriptionId = SessionManager.getLastTranscriptionId(),
                                audioId = audioId,
                                lectureTitle = audioTitle,
                                saveToDb = false
                            )
                            _uiState.update {
                                it.copy(
                                    currentStepText = "Notes structured",
                                    progressPercent = 100,
                                    progressStatusText = "Transcript organized · Ready to review and save",
                                    isGenerating = false,
                                    isGenerationComplete = true,
                                    errorMessage = statusResult.errorMessage ?: fallback.message
                                )
                            }
                            break
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    currentStepText = statusResult.currentStep,
                                    progressPercent = statusResult.progressPercent.coerceAtLeast(10)
                                )
                            }
                        }
                    }
                }

                if (!isPipelineFinished) {
                    delay(1500)
                }
            }

            // 3. Fallback safety if polling timed out
            if (!isPipelineFinished) {
                val fallback = AiNotesRepository.generateNotes(
                    transcriptionId = SessionManager.getLastTranscriptionId(),
                    audioId = audioId,
                    lectureTitle = audioTitle,
                    saveToDb = false
                )
                _uiState.update {
                    it.copy(
                        currentStepText = "Notes ready!",
                        progressPercent = 100,
                        progressStatusText = "Lecture notes ready to review",
                        isGenerating = false,
                        isGenerationComplete = true
                    )
                }
            }
        }
    }

    fun selectTab(tab: HomeBottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
