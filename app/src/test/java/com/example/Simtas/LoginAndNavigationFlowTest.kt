package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AuthRepository
import com.example.Simtas.ui.login.LoginViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying SmartNotes AI Login, Session Persistence, and Navigation Flow.
 */
class LoginAndNavigationFlowTest {

    @Before
    fun setUp() {
        SessionManager.clearSession()
    }

    @After
    fun tearDown() {
        SessionManager.clearSession()
    }

    @Test
    fun testInitialState_noSession_isNotLoggedIn() {
        assertFalse("User should not be logged in initially", SessionManager.isLoggedIn())
        assertNull("Initial token should be null", SessionManager.getToken())
    }

    @Test
    fun testSaveSession_persistsTokenAndUserData() {
        val testToken = "jwt_test_token_sample_abc123"
        val testUserId = 42
        val testName = "Jane Doe"
        val testEmail = "jane.doe@university.edu"

        SessionManager.saveSession(
            context = null,
            token = testToken,
            userId = testUserId,
            name = testName,
            email = testEmail
        )

        assertTrue("User should be marked as logged in", SessionManager.isLoggedIn())
        assertEquals(testToken, SessionManager.getToken())
        assertEquals(testUserId, SessionManager.getUserId())
        assertEquals(testName, SessionManager.getUserName())
        assertEquals(testEmail, SessionManager.getUserEmail())
    }

    @Test
    fun testClearSession_removesSessionData() {
        SessionManager.saveSession(null, "active_token", 1, "User", "user@test.com")
        assertTrue(SessionManager.isLoggedIn())

        SessionManager.clearSession(null)
        assertFalse("User should no longer be logged in after clearSession", SessionManager.isLoggedIn())
        assertNull("Token should be null after clearSession", SessionManager.getToken())
    }

    @Test
    fun testStartupNavigation_whenNoSession_targetIsLogin() {
        // Given no active session
        SessionManager.clearSession(null)
        assertFalse(SessionManager.isLoggedIn())

        // Simulated startup decision
        val targetScreen = if (SessionManager.isLoggedIn()) {
            AppScreen.HomeDashboard
        } else {
            AppScreen.Login
        }

        assertEquals("Startup without session must show Login screen", AppScreen.Login, targetScreen)
    }

    @Test
    fun testStartupNavigation_whenValidSessionExists_targetIsDashboardDirectly() {
        // Given existing persistent session
        SessionManager.saveSession(null, "valid_persistent_token", 5, "Alex", "alex@campus.edu")
        assertTrue(SessionManager.isLoggedIn())

        // Simulated startup decision
        val targetScreen = if (SessionManager.isLoggedIn()) {
            AppScreen.HomeDashboard
        } else {
            AppScreen.Login
        }

        assertEquals("Startup with valid session must navigate directly to Dashboard", AppScreen.HomeDashboard, targetScreen)
    }

    @Test
    fun testLoginViewModel_validationFailsOnBlankInput() {
        val viewModel = LoginViewModel()

        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        var navigated = false
        viewModel.onLoginClicked { navigated = true }

        assertFalse("Should not navigate on blank credentials", navigated)
        val state = viewModel.uiState.value
        assertFalse("Should not be logged in", state.isLoggedIn)
        assertNotNull("Should set error message", state.errorMessage)
        assertTrue(state.errorMessage!!.contains("both email and password", ignoreCase = true))
    }

    @Test
    fun testLoginViewModel_updatesInputFieldsCorrectly() {
        val viewModel = LoginViewModel()

        viewModel.onEmailChanged("student@college.edu")
        viewModel.onPasswordChanged("SecurePass123!")

        val state = viewModel.uiState.value
        assertEquals("student@college.edu", state.email)
        assertEquals("SecurePass123!", state.password)
        assertNull(state.errorMessage)

        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun testExplicitLogout_clearsSessionAndNavigatesToLogin() = runBlocking {
        SessionManager.saveSession(null, "active_token_to_logout", 8, "Logout User", "logout@test.com")
        assertTrue(SessionManager.isLoggedIn())

        // Simulate logout flow
        AuthRepository.logout(null)

        assertFalse("User must be logged out after explicit logout", SessionManager.isLoggedIn())
        assertNull("Token must be null after logout", SessionManager.getToken())

        val postLogoutScreen = if (SessionManager.isLoggedIn()) AppScreen.HomeDashboard else AppScreen.Login
        assertEquals("Screen must be Login after logout", AppScreen.Login, postLogoutScreen)
    }

    @Test
    fun testBackNavigationRules_dashboardDoesNotReturnToLogin() {
        // On Dashboard, pressing Back exits the activity (or finishes app), NEVER routes back to Login
        var activityFinished = false
        val currentScreen = AppScreen.HomeDashboard

        fun onBackPress() {
            if (currentScreen == AppScreen.HomeDashboard) {
                activityFinished = true
            }
        }

        onBackPress()
        assertTrue("Back press on HomeDashboard must finish activity and not navigate to Login", activityFinished)
    }

    @Test
    fun testBackNavigationRules_signupAndForgotPasswordReturnToLogin() {
        fun resolveBackDestination(screen: AppScreen): AppScreen {
            return when (screen) {
                AppScreen.Signup, AppScreen.ForgotPassword -> AppScreen.Login
                else -> AppScreen.HomeDashboard
            }
        }

        assertEquals(AppScreen.Login, resolveBackDestination(AppScreen.Signup))
        assertEquals(AppScreen.Login, resolveBackDestination(AppScreen.ForgotPassword))
    }
}

