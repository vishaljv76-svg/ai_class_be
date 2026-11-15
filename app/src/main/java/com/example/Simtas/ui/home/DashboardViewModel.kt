package com.example.Simtas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
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

    private val initialName = SessionManager.getUserName()
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            userName = initialName,
            userInitials = SessionManager.computeInitials(initialName)
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val requestUserId = SessionManager.getUserId()
        val requestToken = SessionManager.getToken()
        val currentName = SessionManager.getUserName()
        _uiState.update { 
            it.copy(
                userName = if (it.userName.isBlank()) currentName else it.userName,
                userInitials = if (it.userInitials.isBlank()) SessionManager.computeInitials(currentName) else it.userInitials,
                isLoading = true 
            ) 
        }
        viewModelScope.launch {
            val data = DashboardRepository.getDashboardData()
            // Discard stale response if user switched during async call
            if (SessionManager.getUserId() != requestUserId || SessionManager.getToken() != requestToken) {
                return@launch
            }
            if (data != null) {
                val resolvedName = if (data.userName.isNotBlank()) data.userName else SessionManager.getUserName()
                val resolvedInitials = if (data.userInitials.isNotBlank()) data.userInitials else SessionManager.computeInitials(resolvedName)
                _uiState.update { current ->
                    current.copy(
                        userName = resolvedName,
                        dateText = data.dateText,
                        userInitials = resolvedInitials,
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
