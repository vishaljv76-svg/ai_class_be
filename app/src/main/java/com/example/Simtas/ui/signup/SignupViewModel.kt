package com.example.Simtas.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [SignupScreen] adhering to MVVM architecture with real API registration.
 */
class SignupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onStudentNameChanged(name: String) {
        _uiState.update { it.copy(studentName = name, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        val isValid = password.length >= 6
        _uiState.update { 
            it.copy(
                password = password,
                isPasswordValid = isValid,
                errorMessage = null
            )
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onCreateAccountClicked(onSuccess: () -> Unit = {}) {
        val name = _uiState.value.studentName.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirm = _uiState.value.confirmPassword

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required") }
            return
        }

        if (password != confirm) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = AuthRepository.signup(null, name, email, password)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSignupSuccess = true) }
                onSuccess()
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
