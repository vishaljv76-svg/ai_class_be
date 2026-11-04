package com.example.Simtas.ui.login

/**
 * UI State for the Login Screen following MVVM architecture.
 */
data class LoginUiState(
    val email: String = "alex.morgan@campus.edu",
    val password: String = "••••••••••••",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)
