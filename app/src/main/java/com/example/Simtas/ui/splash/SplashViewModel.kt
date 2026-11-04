package com.example.Simtas.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [SplashScreen] in accordance with MVVM architecture.
 * Manages UI state and business logic without any external dependencies.
 */
class SplashViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        // Example initialization logic (e.g., pre-loading resources or timer)
        startSplashTimer()
    }

    private fun startSplashTimer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Splash screen delay / initialization complete setup
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }
}
