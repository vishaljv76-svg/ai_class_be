package com.example.Simtas.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [StudentOnboardingScreen] adhering to MVVM architecture with real API integration.
 */
class StudentOnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudentOnboardingUiState())
    val uiState: StateFlow<StudentOnboardingUiState> = _uiState.asStateFlow()

    fun onStudentNameChanged(name: String) {
        _uiState.update { it.copy(studentName = name) }
    }

    fun onCourseSelected(course: String) {
        _uiState.update { it.copy(selectedCourse = course) }
    }

    fun onDepartmentSelected(department: String) {
        _uiState.update { it.copy(selectedDepartment = department) }
    }

    fun onSemesterSelected(semester: String) {
        _uiState.update { it.copy(selectedSemester = semester) }
    }

    fun onContinueClicked(onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            ProfileRepository.addProfileOnboarding(
                course = _uiState.value.selectedCourse,
                department = _uiState.value.selectedDepartment,
                semester = _uiState.value.selectedSemester,
                name = _uiState.value.studentName
            )
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }
}
