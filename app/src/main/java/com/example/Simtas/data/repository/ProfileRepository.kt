package com.example.Simtas.data.repository

import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import org.json.JSONObject

data class ProfileData(
    val userName: String,
    val userEmail: String,
    val userInitials: String,
    val courseText: String,
    val departmentText: String,
    val semesterBadgeText: String,
    val yearBadgeText: String,
    val hasProfile: Boolean,
    val profileImage: String? = null
)

object ProfileRepository {

    suspend fun fetchProfile(): ProfileData? {
        val userId = SessionManager.getUserId()
        val response = ApiClient.get("profile/profile_fetch.php?user_id=$userId")

        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val user = json.optJSONObject("user")
            val profile = json.optJSONObject("profile")

            val cached = SessionManager.getUserProfile(userId)
            val name = user?.optString("name")?.takeIf { it.isNotBlank() }
                ?: cached?.get("name")?.takeIf { it.isNotBlank() }
                ?: SessionManager.getUserName()
            val email = user?.optString("email")?.takeIf { it.isNotBlank() }
                ?: cached?.get("email")?.takeIf { it.isNotBlank() }
                ?: SessionManager.getUserEmail()
            val initials = user?.optString("initials")?.takeIf { it.isNotBlank() }
                ?: SessionManager.computeInitials(name)

            val course = profile?.optString("course")?.takeIf { it.isNotBlank() }
                ?: cached?.get("course")?.takeIf { it.isNotBlank() }
                ?: ""
            val dept = profile?.optString("department")?.takeIf { it.isNotBlank() }
                ?: cached?.get("department")?.takeIf { it.isNotBlank() }
                ?: ""
            val semBadge = profile?.optString("semester_badge")?.takeIf { it.isNotBlank() }
                ?: cached?.get("semester_badge")?.takeIf { it.isNotBlank() }
                ?: ""
            val yearBadge = profile?.optString("year_badge")?.takeIf { it.isNotBlank() }
                ?: cached?.get("year_badge")?.takeIf { it.isNotBlank() }
                ?: ""
            val photoUrl = profile?.optString("profile_image")?.takeIf { it.isNotBlank() && it != "null" }
                ?: user?.optString("profile_image")?.takeIf { it.isNotBlank() && it != "null" }
                ?: cached?.get("photo_path")?.takeIf { it.isNotBlank() }

            return ProfileData(
                userName = name,
                userEmail = email,
                userInitials = initials,
                courseText = course,
                departmentText = dept,
                semesterBadgeText = semBadge,
                yearBadgeText = yearBadge,
                hasProfile = json.optBoolean("has_profile", false),
                profileImage = photoUrl
            )
        }
        return null
    }

    suspend fun uploadProfilePhoto(base64Image: String): Boolean {
        val payload = JSONObject().apply {
            put("userId", SessionManager.getUserId())
            put("image_base64", base64Image)
        }.toString()

        val response = ApiClient.post("profile/profile_upload_photo.php", payload)
        return response.isSuccess
    }

    suspend fun saveOrUpdateProfile(
        name: String,
        course: String,
        department: String,
        semester: String
    ): Boolean {
        val payload = JSONObject().apply {
            put("userId", SessionManager.getUserId())
            put("studentName", name)
            put("selectedCourse", course)
            put("selectedDepartment", department)
            put("selectedSemester", semester)
        }.toString()

        val response = ApiClient.post("profile/profile_edit.php", payload)
        if (response.isSuccess) {
            val uId = SessionManager.getUserId()
            SessionManager.updateUserName(null, name)
            SessionManager.saveUserProfile(
                context = null,
                userId = uId,
                course = course,
                department = department,
                semesterBadge = if (semester.isNotBlank()) "Semester $semester" else "",
                yearBadge = "",
                name = name
            )
            return true
        }
        return false
    }

    suspend fun addProfileOnboarding(
        course: String,
        department: String,
        semester: String,
        name: String? = null
    ): Boolean {
        val uId = SessionManager.getUserId()
        val payload = JSONObject().apply {
            put("userId", uId)
            put("selectedCourse", course)
            put("selectedDepartment", department)
            put("selectedSemester", semester)
            if (!name.isNullOrEmpty()) {
                put("studentName", name)
            }
        }.toString()

        val response = ApiClient.post("profile/profile_add.php", payload)
        if (response.isSuccess && uId > 0) {
            if (!name.isNullOrBlank()) {
                SessionManager.updateUserName(null, name)
            }
            SessionManager.saveUserProfile(
                context = null,
                userId = uId,
                course = course,
                department = department,
                semesterBadge = if (semester.isNotBlank()) "Semester $semester" else "",
                yearBadge = "",
                name = name ?: SessionManager.getUserName()
            )
        }
        return response.isSuccess
    }
}

