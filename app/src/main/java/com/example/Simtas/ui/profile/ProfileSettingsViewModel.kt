package com.example.Simtas.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.ApiConfig
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AuthRepository
import com.example.Simtas.data.repository.ProfileRepository
import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.theme.AppFontStyle
import com.example.Simtas.ui.theme.AppThemeMode
import com.example.Simtas.ui.theme.FontManager
import com.example.Simtas.ui.theme.ThemeManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for [ProfileSettingsScreen] adhering to MVVM architecture with real API synchronization.
 */
class ProfileSettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSettingsUiState())
    val uiState: StateFlow<ProfileSettingsUiState> = _uiState.asStateFlow()

    init {
        val currentMode = ThemeManager.currentThemeMode
        val currentFont = FontManager.currentFontStyle
        val currentUserId = SessionManager.getUserId()
        val cached = SessionManager.getUserProfile(currentUserId)
        val initialName = cached?.get("name") ?: SessionManager.getUserName()
        val initialEmail = cached?.get("email") ?: SessionManager.getUserEmail()
        _uiState.update {
            it.copy(
                userName = initialName,
                userEmail = initialEmail,
                userInitials = SessionManager.computeInitials(initialName),
                courseText = cached?.get("course") ?: "",
                departmentText = cached?.get("department") ?: "",
                semesterBadgeText = cached?.get("semester_badge") ?: "",
                yearBadgeText = cached?.get("year_badge") ?: "",
                selectedThemeMode = currentMode,
                themeModeText = currentMode.displayName,
                selectedFontStyle = currentFont,
                fontStyleText = currentFont.displayName
            )
        }
        loadProfile()
    }

    fun loadProfile(context: Context? = null) {
        val requestUserId = SessionManager.getUserId()
        val requestToken = SessionManager.getToken()
        if (requestUserId <= 0) return

        // Load locally cached photo first for instant display
        val localPath = SessionManager.getProfilePhotoPath(requestUserId)
        if (!localPath.isNullOrEmpty()) {
            val file = File(localPath)
            if (file.exists()) {
                try {
                    val localBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (localBitmap != null) {
                        _uiState.update { it.copy(profilePhotoBitmap = localBitmap) }
                    }
                } catch (_: Exception) {}
            }
        }

        viewModelScope.launch {
            val profile = ProfileRepository.fetchProfile()
            // Strictly guard against stale responses overwriting currently authenticated user identity
            if (SessionManager.getUserId() != requestUserId || SessionManager.getToken() != requestToken) {
                return@launch
            }

            if (profile != null) {
                SessionManager.saveUserProfile(
                    context = context,
                    userId = requestUserId,
                    course = profile.courseText,
                    department = profile.departmentText,
                    semesterBadge = profile.semesterBadgeText,
                    yearBadge = profile.yearBadgeText,
                    name = profile.userName,
                    email = profile.userEmail,
                    photoPath = profile.profileImage
                )

                _uiState.update { current ->
                    current.copy(
                        userName = profile.userName,
                        userEmail = profile.userEmail,
                        userInitials = profile.userInitials,
                        courseText = profile.courseText,
                        departmentText = profile.departmentText,
                        semesterBadgeText = profile.semesterBadgeText,
                        yearBadgeText = profile.yearBadgeText
                    )
                }

                // If no local bitmap exists but backend has a profile photo, fetch it
                if (_uiState.value.profilePhotoBitmap == null && !profile.profileImage.isNullOrEmpty()) {
                    try {
                        withContext(Dispatchers.IO) {
                            val fullUrl = if (profile.profileImage.startsWith("http://") || profile.profileImage.startsWith("https://")) {
                                profile.profileImage
                            } else {
                                val base = ApiConfig.BASE_URL.trimEnd('/')
                                val end = profile.profileImage.trimStart('/')
                                "$base/$end"
                            }
                            val url = java.net.URL(fullUrl)
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            conn.setRequestProperty("ngrok-skip-browser-warning", "true")
                            if (conn.responseCode in 200..299) {
                                val remoteBitmap = BitmapFactory.decodeStream(conn.inputStream)
                                conn.disconnect()
                                if (remoteBitmap != null) {
                                    if (SessionManager.getUserId() == requestUserId) {
                                        _uiState.update { it.copy(profilePhotoBitmap = remoteBitmap) }
                                    }
                                    if (context != null) {
                                        val profileDir = File(context.filesDir, "profile_photos")
                                        if (!profileDir.exists()) profileDir.mkdirs()
                                        val file = File(profileDir, "profile_${requestUserId}.jpg")
                                        FileOutputStream(file).use { out ->
                                            remoteBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                        }
                                        SessionManager.saveProfilePhotoPath(context, file.absolutePath, userId = requestUserId)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } else {
                // Use session details if available
                val name = SessionManager.getUserName()
                val email = SessionManager.getUserEmail()
                _uiState.update { current ->
                    current.copy(
                        userName = name,
                        userEmail = email,
                        userInitials = SessionManager.computeInitials(name)
                    )
                }
            }
        }
    }

    fun onPhotoSelected(context: Context, uri: Uri) {
        _uiState.update { it.copy(isUploadingPhoto = true, photoErrorMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Decode & downsample bitmap
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(context, uri, 512, 512)
                }

                if (bitmap == null) {
                    _uiState.update { it.copy(isUploadingPhoto = false, photoErrorMessage = "Unable to process selected image") }
                    return@launch
                }

                // 2. Immediately update UI & persist locally (Guarantees local persistence!)
                _uiState.update {
                    it.copy(
                        profilePhotoBitmap = bitmap,
                        isUploadingPhoto = false,
                        photoErrorMessage = null
                    )
                }

                withContext(Dispatchers.IO) {
                    val profileDir = File(context.filesDir, "profile_photos")
                    if (!profileDir.exists()) profileDir.mkdirs()
                    val uId = SessionManager.getUserId()
                    val file = File(profileDir, "profile_${uId}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    SessionManager.saveProfilePhotoPath(context, file.absolutePath, userId = uId)
                }

                // 3. Sync to backend MySQL asynchronously
                withContext(Dispatchers.IO) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    ProfileRepository.uploadProfilePhoto(base64)
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        photoErrorMessage = e.localizedMessage ?: "Failed to save photo"
                    )
                }
            }
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun toggleStudyReminders(enabled: Boolean) {
        _uiState.update { it.copy(isStudyRemindersEnabled = enabled) }
    }

    fun toggleNotesReadyAlerts(enabled: Boolean) {
        _uiState.update { it.copy(isNotesReadyAlertsEnabled = enabled) }
    }

    fun onThemeClicked() {
        _uiState.update { it.copy(isThemeDialogVisible = true) }
    }

    fun onDismissThemeDialog() {
        _uiState.update { it.copy(isThemeDialogVisible = false) }
    }

    fun onThemeSelected(context: Context?, mode: AppThemeMode) {
        ThemeManager.setThemeMode(context, mode)
        _uiState.update {
            it.copy(
                selectedThemeMode = mode,
                themeModeText = mode.displayName,
                isThemeDialogVisible = false
            )
        }
    }

    fun onFontClicked() {
        _uiState.update { it.copy(isFontDialogVisible = true) }
    }

    fun onDismissFontDialog() {
        _uiState.update { it.copy(isFontDialogVisible = false) }
    }

    fun onFontSelected(context: Context?, style: AppFontStyle) {
        FontManager.setFontStyle(context, style)
        _uiState.update {
            it.copy(
                selectedFontStyle = style,
                fontStyleText = style.displayName,
                isFontDialogVisible = false
            )
        }
    }

    fun selectTab(tab: HomeBottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onLogoutClicked(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            AuthRepository.logout(null)
            onSuccess()
        }
    }

    fun onEditProfileClicked() {
        _uiState.update { current ->
            current.copy(
                isEditing = true,
                editName = current.userName,
                editCourse = current.courseText,
                editDepartment = current.departmentText,
                editSemester = current.semesterBadgeText
            )
        }
    }

    fun onCancelEditClicked() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun onEditNameChange(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun onEditCourseChange(course: String) {
        _uiState.update { it.copy(editCourse = course) }
    }

    fun onEditDepartmentChange(department: String) {
        _uiState.update { it.copy(editDepartment = department) }
    }

    fun onEditSemesterChange(semester: String) {
        _uiState.update { it.copy(editSemester = semester) }
    }

    fun onSaveChangesClicked() {
        val current = _uiState.value
        val trimmedName = current.editName.trim().ifEmpty { current.userName }
        val trimmedCourse = current.editCourse.trim().ifEmpty { current.courseText }
        val trimmedDept = current.editDepartment.trim().ifEmpty { current.departmentText }
        val trimmedSem = current.editSemester.trim().ifEmpty { current.semesterBadgeText }

        val initials = computeInitials(trimmedName)
        val yearBadge = computeYearBadge(trimmedSem, current.yearBadgeText)

        // Optimistically update UI
        _uiState.update {
            it.copy(
                userName = trimmedName,
                userInitials = initials,
                courseText = trimmedCourse,
                departmentText = trimmedDept,
                semesterBadgeText = trimmedSem,
                yearBadgeText = yearBadge,
                isEditing = false
            )
        }

        // Asynchronously persist to MySQL backend
        viewModelScope.launch {
            ProfileRepository.saveOrUpdateProfile(
                name = trimmedName,
                course = trimmedCourse,
                department = trimmedDept,
                semester = trimmedSem
            )
        }
    }

    private fun computeInitials(name: String): String {
        val parts = name.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        return initials.ifEmpty { "U" }
    }

    private fun computeYearBadge(semesterStr: String, fallback: String): String {
        val digits = "\\d+".toRegex().find(semesterStr)?.value?.toIntOrNull()
        return if (digits != null) {
            val year = kotlin.math.ceil(digits / 2.0).toInt()
            when {
                year >= 4 -> "Final year"
                year == 1 -> "1st year"
                year == 2 -> "2nd year"
                year == 3 -> "3rd year"
                else -> "Year $year"
            }
        } else {
            fallback
        }
    }
}
