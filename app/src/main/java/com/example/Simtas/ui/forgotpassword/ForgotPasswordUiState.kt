package com.example.Simtas.ui.forgotpassword

/**
 * UI State for the Forgot Password / Reset Password Screen following MVVM architecture.
 */
data class ForgotPasswordUiState(
    val email: String = "alex.morgan@campus.edu",
    val isLoading: Boolean = false,
    val isResetEmailSent: Boolean = false,
    val infoMessage: String = "The reset link expires in 30 minutes. Check your spam folder if you don't see it."
)
