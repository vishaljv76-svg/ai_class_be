package com.example.Simtas.ui.profile

import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.theme.AppFontStyle
import com.example.Simtas.ui.theme.AppThemeMode

/**
 * UI State for Profile & Settings Screen following MVVM architecture.
 */
data class ProfileSettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userInitials: String = "",
    val courseText: String = "",
    val departmentText: String = "",
    val semesterBadgeText: String = "",
    val yearBadgeText: String = "",
    val isStudyRemindersEnabled: Boolean = true,
    val isNotesReadyAlertsEnabled: Boolean = true,
    val themeModeText: String = "Light",
    val selectedThemeMode: AppThemeMode = AppThemeMode.Light,
    val isThemeDialogVisible: Boolean = false,
    val fontStyleText: String = "Bold Sans-Serif",
    val selectedFontStyle: AppFontStyle = AppFontStyle.BoldSansSerif,
    val isFontDialogVisible: Boolean = false,
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
