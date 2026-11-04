package com.example.Simtas.ui.splash

/**
 * UI State for the Splash Screen following MVVM pattern.
 */
data class SplashUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false
)
