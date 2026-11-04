package com.example.Simtas.data.repository

import android.content.Context
import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import org.json.JSONObject

data class AuthResult(
    val isSuccess: Boolean,
    val message: String,
    val userId: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val hasProfile: Boolean = false
)

object AuthRepository {

    suspend fun signup(context: Context?, name: String, email: String, pass: String): AuthResult {
        val payload = JSONObject().apply {
            put("studentName", name)
            put("email", email)
            put("password", pass)
            put("confirmPassword", pass)
        }.toString()

        val response = ApiClient.post("auth/signup.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val token = json.optString("token")
            val userId = json.optInt("user_id", 1)
            val userObj = json.optJSONObject("user")
            val userName = userObj?.optString("name", name) ?: name
            val userEmail = userObj?.optString("email", email) ?: email

            if (token.isNotEmpty()) {
                SessionManager.saveSession(context, token, userId, userName, userEmail)
            }

            return AuthResult(
                isSuccess = true,
                message = json.optString("message", "Signup successful"),
                userId = userId,
                name = userName,
                email = userEmail,
                hasProfile = false
            )
        } else {
            return AuthResult(
                isSuccess = false,
                message = response.errorMessage ?: "Account creation failed"
            )
        }
    }

    suspend fun login(context: Context?, email: String, pass: String): AuthResult {
        val payload = JSONObject().apply {
            put("email", email)
            put("password", pass)
        }.toString()

        val response = ApiClient.post("auth/login.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val token = json.optString("token")
            val userObj = json.optJSONObject("user")
            val userId = userObj?.optInt("id", 1) ?: 1
            val userName = userObj?.optString("name", "Alex") ?: "Alex"
            val userEmail = userObj?.optString("email", email) ?: email
            val hasProfile = json.optBoolean("has_profile", false)

            if (token.isNotEmpty()) {
                SessionManager.saveSession(context, token, userId, userName, userEmail)
            }

            return AuthResult(
                isSuccess = true,
                message = json.optString("message", "Login successful"),
                userId = userId,
                name = userName,
                email = userEmail,
                hasProfile = hasProfile
            )
        } else {
            return AuthResult(
                isSuccess = false,
                message = response.errorMessage ?: "Login failed"
            )
        }
    }

    suspend fun forgotPassword(email: String): Pair<Boolean, String> {
        val payload = JSONObject().apply { put("email", email) }.toString()
        val response = ApiClient.post("auth/forgot_password.php", payload)
        return if (response.isSuccess) {
            val msg = response.jsonObject?.optString("message") ?: "Password reset instructions sent"
            Pair(true, msg)
        } else {
            Pair(false, response.errorMessage ?: "Failed to send reset link")
        }
    }

    suspend fun validateSession(context: Context? = null): Boolean {
        val token = SessionManager.getToken()
        if (token.isNullOrEmpty()) {
            return false
        }

        val response = ApiClient.get("auth/validate_session.php", token = token)
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val userObj = json.optJSONObject("user")
            val userId = userObj?.optInt("id") ?: json.optInt("user_id", SessionManager.getUserId())
            val userName = userObj?.optString("name") ?: SessionManager.getUserName()
            val userEmail = userObj?.optString("email") ?: SessionManager.getUserEmail()

            // Keep persistent session fresh
            SessionManager.saveSession(context, token, userId, userName, userEmail)
            return true
        } else {
            // Token is invalid, expired, or revoked
            SessionManager.clearSession(context)
            return false
        }
    }

    suspend fun logout(context: Context?) {
        try {
            ApiClient.post("auth/logout.php", "{}")
        } catch (_: Exception) {}
        SessionManager.clearSession(context)
    }
}

