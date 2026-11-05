package com.example.Simtas.ui.upload

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.AudioRepository
import com.example.Simtas.data.remote.SessionManager
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for [UploadLectureScreen] adhering to MVVM architecture.
 */
class UploadLectureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UploadLectureUiState())
    val uiState: StateFlow<UploadLectureUiState> = _uiState.asStateFlow()

    private var uploadJob: Job? = null

    fun onAudioFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // 1. Preserve persistable read permission across sessions via Storage Access Framework
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: SecurityException) {
                    // Ignored if persistable flag is not granted by provider
                }

                // 2. Query file metadata (name & size) on IO dispatcher
                var fileName = ""
                var fileSize = 0L

                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIdx != -1) {
                                    fileName = cursor.getString(nameIdx) ?: ""
                                }
                                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                                if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
                                    fileSize = cursor.getLong(sizeIdx)
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    if (fileName.isBlank()) {
                        fileName = uri.lastPathSegment ?: "audio_lecture.mp3"
                    }

                    if (fileSize <= 0L) {
                        try {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                                fileSize = pfd.statSize
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 3. Validate file type (audio/* and common audio extensions)
                val mimeType = try {
                    context.contentResolver.getType(uri)
                } catch (_: Exception) {
                    null
                }

                val lowerName = fileName.lowercase(Locale.ROOT)
                val isAudioExt = lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                        lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") ||
                        lowerName.endsWith(".flac") || lowerName.endsWith(".ogg") ||
                        lowerName.endsWith(".wma") || lowerName.endsWith(".opus") ||
                        lowerName.endsWith(".amr") || lowerName.endsWith(".3gp")

                val isAudioMime = mimeType?.startsWith("audio/") == true ||
                        mimeType == "application/ogg" ||
                        mimeType == "application/x-flac"

                if (!isAudioMime && !isAudioExt && !mimeType.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Please select a valid audio file (MP3, WAV, M4A, etc.)"
                        )
                    }
                    return@launch
                }

                // Validate 100 MB max size
                val maxSizeBytes = 100L * 1024 * 1024
                if (fileSize > maxSizeBytes) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Selected file exceeds the 100 MB limit."
                        )
                    }
                    return@launch
                }

                // 4. Extract audio duration via MediaMetadataRetriever
                var durationMs = 0L
                withContext(Dispatchers.IO) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = durStr?.toLongOrNull() ?: 0L
                        retriever.release()
                    } catch (_: Exception) {}
                }

                val sizeText = formatFileSize(fileSize)
                val durationText = formatDuration(durationMs)
                val fileDetails = when {
                    durationText.isNotEmpty() && sizeText.isNotEmpty() -> "$durationText · $sizeText"
                    durationText.isNotEmpty() -> durationText
                    sizeText.isNotEmpty() -> sizeText
                    else -> "Audio ready"
                }

                // 5. Connect to upload workflow with real file metadata and progress
                uploadJob?.cancel()
                _uiState.update {
                    it.copy(
                        selectedFileName = fileName,
                        fileDurationAndSize = fileDetails,
                        selectedFileUri = uri,
                        isFileSelected = true,
                        isUploading = true,
                        isUploadComplete = false,
                        uploadProgressPercent = 0,
                        uploadedBytesText = if (sizeText.isNotEmpty()) "Uploading · 0.0 of $sizeText" else "Uploading...",
                        errorMessage = null
                    )
                }

                // Cache a local copy so Lecture Preview and Offline Speech Recognizer can access it directly
                withContext(Dispatchers.IO) {
                    try {
                        val recDir = java.io.File(context.filesDir, "recordings")
                        if (!recDir.exists()) recDir.mkdirs()
                        val localCopy = java.io.File(recDir, fileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(localCopy).use { out -> input.copyTo(out) }
                        }
                        if (localCopy.exists() && localCopy.length() > 0L) {
                            SessionManager.setLastRecordedAudioPath(localCopy.absolutePath)
                            SessionManager.setLastUploadedAudio(
                                audioId = SessionManager.getLastUploadedAudioId() ?: 0,
                                title = fileName.substringBeforeLast('.'),
                                duration = durationText
                            )
                        }
                    } catch (_: Exception) {}
                }

                uploadJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val uploadResult = AudioRepository.uploadAudio(
                            context = context,
                            uri = uri,
                            fileName = fileName,
                            fileSize = fileSize,
                            title = fileName.substringBeforeLast('.'),
                            duration = durationText,
                            onProgress = { bytesSent, totalBytes ->
                                val percent = if (totalBytes > 0) ((bytesSent * 100) / totalBytes).toInt().coerceIn(0, 99) else 0
                                val uploadedStr = formatFileSize(bytesSent)
                                val progressText = if (sizeText.isNotEmpty()) "Uploading · $uploadedStr of $sizeText" else "Uploading · $percent%"
                                _uiState.update {
                                    it.copy(
                                        uploadProgressPercent = percent,
                                        uploadedBytesText = progressText,
                                        isUploading = true,
                                        isUploadComplete = false
                                    )
                                }
                            }
                        )

                        if (uploadResult.isSuccess && uploadResult.audioId != null) {
                            _uiState.update {
                                it.copy(
                                    uploadProgressPercent = 100,
                                    uploadedBytesText = if (sizeText.isNotEmpty()) "Upload complete · $sizeText" else "Upload complete",
                                    isUploading = false,
                                    isUploadComplete = true,
                                    uploadedAudioId = uploadResult.audioId,
                                    errorMessage = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    isUploadComplete = false,
                                    errorMessage = uploadResult.message ?: "Upload failed on server"
                                )
                            }
                        }
                    } catch (_: CancellationException) {
                        // Cancelled safely
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                isUploadComplete = false,
                                errorMessage = "Upload failed: ${e.localizedMessage ?: "Unknown error"}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to process audio file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onPickerCancelled() {
        // Picker was cancelled by user - safely preserve current selection
    }

    fun onPickerError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun onBrowseFilesClicked() {
        // Preserved for direct invocation compatibility
    }

    fun onCancelUploadClicked() {
        uploadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedFileName = "",
                fileDurationAndSize = "",
                uploadProgressPercent = 0,
                uploadedBytesText = "",
                isUploading = false,
                isUploadComplete = false,
                isFileSelected = false,
                selectedFileUri = null,
                errorMessage = null
            )
        }
    }

    fun onContinueClicked() {
        // Process uploaded file
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return ""
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return if (hours > 0) {
            val remMinutes = minutes % 60
            String.format(Locale.US, "%02d:%02d:%02d duration", hours, remMinutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d duration", minutes, seconds)
        }
    }
}
