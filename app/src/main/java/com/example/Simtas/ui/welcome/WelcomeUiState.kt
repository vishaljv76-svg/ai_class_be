package com.example.Simtas.ui.welcome

/**
 * UI State for the Welcome/Onboarding Screen following MVVM architecture.
 */
data class WelcomeUiState(
    val title: String = "Be in class.\nNot in your notebook.",
    val subtitle: String = "AI-powered lecture notes that help you listen, understand and remember more.",
    val isLoading: Boolean = false,
    val isGetStartedClicked: Boolean = false
)
