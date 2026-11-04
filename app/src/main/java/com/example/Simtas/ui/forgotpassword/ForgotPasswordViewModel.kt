package com.example.Simtas.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [ForgotPasswordScreen] adhering to MVVM architecture with real API integration.
 */
class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onResetPasswordClicked() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val (success, msg) = AuthRepository.forgotPassword(email)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isResetEmailSent = success,
                    infoMessage = msg
                )
            }
        }
    }
}
