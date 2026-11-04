package com.example.Simtas.ui.profile

import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.theme.AppThemeMode

/**
 * UI State for Profile & Settings Screen following MVVM architecture.
 */
data class ProfileSettingsUiState(
    val userName: String = "Alex Morgan",
    val userEmail: String = "alex.morgan@campus.edu",
    val userInitials: String = "AM",
    val courseText: String = "B.Tech · Computer Science",
    val departmentText: String = "Computer Science & Engineering",
    val semesterBadgeText: String = "Semester 7",
    val yearBadgeText: String = "Final year",
    val isStudyRemindersEnabled: Boolean = true,
    val isNotesReadyAlertsEnabled: Boolean = true,
    val themeModeText: String = "Light",
    val selectedThemeMode: AppThemeMode = AppThemeMode.Light,
    val isThemeDialogVisible: Boolean = false,
    val appVersionText: String = "SmartNotes AI · v1.0.0",
    val selectedTab: HomeBottomTab = HomeBottomTab.Profile,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editCourse: String = "",
    val editDepartment: String = "",
    val editSemester: String = "",
    val profilePhotoBitmap: android.graphics.Bitmap? = null,
    val isUploadingPhoto: Boolean = false,
    val photoErrorMessage: String? = null
)
