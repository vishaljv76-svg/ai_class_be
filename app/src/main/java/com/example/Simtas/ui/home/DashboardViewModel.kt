package com.example.Simtas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [DashboardScreen] adhering to MVVM architecture with real API dashboard stats.
 */
class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val data = DashboardRepository.getDashboardData()
            if (data != null) {
                _uiState.update { current ->
                    current.copy(
                        userName = data.userName,
                        dateText = data.dateText,
                        userInitials = data.userInitials,
                        totalNotes = data.totalNotes,
                        subjectsCount = data.subjectsCount,
                        quizzesCount = data.quizzesCount,
                        recentNotes = data.recentNotes,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectTab(tab: HomeBottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onRecordLectureClicked() {
        // Record lecture action
    }

    fun onUploadAudioClicked() {
        // Upload audio action
    }

    fun onMyNotesClicked() {
        // Open my notes action
    }

    fun onAiQuizClicked() {
        // Open AI quiz action
    }

    fun onNoteItemClicked(noteId: String) {
        // Open specific note
    }
}
