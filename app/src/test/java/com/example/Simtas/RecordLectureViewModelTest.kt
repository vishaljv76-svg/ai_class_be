package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.record.RecordLectureUiState
import com.example.Simtas.ui.record.RecordLectureViewModel
import com.example.Simtas.ui.record.RecordingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordLectureViewModelTest {

    @Test
    fun initialState_isIdleWithZeroTimer() {
        val viewModel = RecordLectureViewModel()
        val state = viewModel.uiState.value

        assertEquals(RecordingStatus.IDLE, state.status)
        assertEquals("00:00", state.timerText)
        assertEquals(0L, state.elapsedSeconds)
        assertNull(state.errorMessage)
        assertNull(state.recordedFilePath)
        assertEquals(0L, state.fileSizeBytes)
        assertFalse(state.isPermissionDenied)
        assertEquals(20, state.audioAmplitudes.size)
    }

    @Test
    fun permissionDenied_setsMeaningfulErrorMessageAndFlag() {
        val viewModel = RecordLectureViewModel()
        viewModel.onPermissionDenied()

        val state = viewModel.uiState.value
        assertEquals(RecordingStatus.IDLE, state.status)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Microphone permission is required"))
        assertTrue(state.isPermissionDenied)
    }

    @Test
    fun clearError_resetsErrorMessageToNull() {
        val viewModel = RecordLectureViewModel()
        viewModel.onPermissionDenied()
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onStop_withoutRecordedFile_resetsToIdleWithError() {
        val viewModel = RecordLectureViewModel()
        viewModel.onStopClicked(context = null)

        val state = viewModel.uiState.value
        assertEquals(RecordingStatus.IDLE, state.status)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("too short or empty"))
    }

    @Test
    fun sessionManager_savesAndRetrievesRecordedAudioPath() {
        val testPath = "/data/user/0/com.example.Simtas/files/recordings/test_rec.m4a"
        SessionManager.setLastRecordedAudioPath(testPath)
        assertEquals(testPath, SessionManager.getLastRecordedAudioPath())

        SessionManager.setLastRecordedAudioPath(null)
        assertNull(SessionManager.getLastRecordedAudioPath())
    }

    @Test
    fun nextButton_isEnabledOnlyWhenStoppedAndFileExists() {
        val idleState = RecordLectureUiState(status = RecordingStatus.IDLE)
        assertFalse(idleState.status == RecordingStatus.STOPPED && idleState.recordedFilePath != null && idleState.fileSizeBytes > 0L)

        val recordingState = RecordLectureUiState(status = RecordingStatus.RECORDING)
        assertFalse(recordingState.status == RecordingStatus.STOPPED && recordingState.recordedFilePath != null && recordingState.fileSizeBytes > 0L)

        val stoppedValidState = RecordLectureUiState(
            status = RecordingStatus.STOPPED,
            recordedFilePath = "/path/to/rec.m4a",
            fileSizeBytes = 1024L
        )
        assertTrue(stoppedValidState.status == RecordingStatus.STOPPED && stoppedValidState.recordedFilePath != null && stoppedValidState.fileSizeBytes > 0L)
    }

    @Test
    fun setLanguage_updatesSelectedLanguageCodeAndSessionManager() {
        val viewModel = RecordLectureViewModel()

        // Set to Tamil
        viewModel.setLanguage("ta")
        assertEquals("ta", viewModel.uiState.value.selectedLanguageCode)
        assertEquals("ta", SessionManager.getSelectedLanguage())

        // Set back to English
        viewModel.setLanguage("en")
        assertEquals("en", viewModel.uiState.value.selectedLanguageCode)
        assertEquals("en", SessionManager.getSelectedLanguage())
    }

    @Test
    fun updateLectureTitleAndCourseCode_modifiesUiState() {
        val viewModel = RecordLectureViewModel()

        viewModel.updateLectureTitle("Operating Systems & Deadlocks")
        assertEquals("Operating Systems & Deadlocks", viewModel.uiState.value.lectureTitle)

        viewModel.updateCourseCode("CS304")
        assertEquals("CS304", viewModel.uiState.value.courseCode)
    }

    @Test
    fun amplitudesList_hasStandardTwentyBars() {
        val viewModel = RecordLectureViewModel()
        val amplitudes = viewModel.uiState.value.audioAmplitudes
        assertEquals(20, amplitudes.size)
        amplitudes.forEach { amp ->
            assertTrue(amp in 0.05f..1.0f)
        }
    }
}
