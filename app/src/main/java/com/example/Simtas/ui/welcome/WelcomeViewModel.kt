package com.example.Simtas.ui.welcome

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for [WelcomeScreen] in accordance with MVVM architecture.
 */
class WelcomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onGetStartedClicked() {
        _uiState.update { it.copy(isGetStartedClicked = true) }
    }

    fun onLoginClicked() {
        // Handle login navigation in future flow
    }

    fun onSignUpClicked() {
        // Handle sign up navigation in future flow
    }
}
