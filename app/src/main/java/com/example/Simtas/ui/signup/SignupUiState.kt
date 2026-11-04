package com.example.Simtas.ui.signup

/**
 * UI State for the Create Account (Sign Up) Screen following MVVM architecture.
 */
data class SignupUiState(
    val studentName: String = "Alex Morgan",
    val email: String = "alex.morgan@campus.edu",
    val password: String = "••••••••••••",
    val confirmPassword: String = "••••••••••••",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isPasswordValid: Boolean = true,
    val passwordValidationText: String = "12 characters · includes a number and symbol",
    val isLoading: Boolean = false,
    val isSignupSuccess: Boolean = false,
    val errorMessage: String? = null
)
