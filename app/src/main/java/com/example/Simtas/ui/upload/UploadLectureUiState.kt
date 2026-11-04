package com.example.Simtas.ui.upload

import android.net.Uri

/**
 * UI State for Upload Lecture Screen following MVVM architecture.
 */
data class UploadLectureUiState(
    val selectedFileName: String = "",
    val fileDurationAndSize: String = "",
    val uploadProgressPercent: Int = 0,
    val uploadedBytesText: String = "",
    val isUploading: Boolean = false,
    val isUploadComplete: Boolean = false,
    val isFileSelected: Boolean = false,
    val selectedFileUri: Uri? = null,
    val uploadedAudioId: Int? = null,
    val errorMessage: String? = null
)
