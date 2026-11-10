package com.example.Simtas.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.Simtas.ui.notes.GeneratedNotesUiState

/**
 * Manages user authentication session persistence in SharedPreferences.
 */
object SessionManager {
    private const val PREF_NAME = "smartnotes_ai_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_PROFILE_PHOTO_PATH = "profile_photo_path"

    private var inMemoryToken: String? = null
    private var inMemoryUserId: Int = 1
    private var inMemoryUserName: String = "Alex Morgan"
    private var inMemoryUserEmail: String = "alex.morgan@campus.edu"
    private var inMemoryPhotoPath: String? = null
    private var appContext: Context? = null
    private var inMemoryLastAudioId: Int? = null
    private var inMemoryLastAudioTitle: String? = null
    private var inMemoryLastAudioDuration: String? = null
    private var inMemoryLastRecordedAudioPath: String? = null
    private var inMemoryLastTranscriptText: String? = null
    private var inMemoryLastTranscriptParagraphs: List<com.example.Simtas.ui.transcript.TranscriptParagraph> = emptyList()
    private var inMemoryLastTranscriptionId: Int? = null
    private var inMemoryLastGeneratedNote: GeneratedNotesUiState? = null
    private var inMemoryLastDetailedNotes: com.example.Simtas.ui.detailednotes.DetailedNotesUiState? = null
    private var inMemoryLastJobId: Int? = null
    private var inMemoryLastNoteId: Int? = null
    private var inMemoryLastQuizId: Int? = null
    private var inMemoryDiscoveredResources: List<com.example.Simtas.ui.notes.DiscoveredResourceItem> = emptyList()
    private var inMemoryDetectedQAs: List<com.example.Simtas.ui.notes.DetectedQuestionAnswerItem> = emptyList()
    private var inMemoryLastQuizResult: com.example.Simtas.ui.quizresults.QuizResultsUiState? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        com.example.Simtas.data.local.LectureNotesStorage.init(context)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        inMemoryToken = prefs.getString(KEY_AUTH_TOKEN, null)
        inMemoryUserId = prefs.getInt(KEY_USER_ID, 1)
        inMemoryUserName = prefs.getString(KEY_USER_NAME, "Alex Morgan") ?: "Alex Morgan"
        inMemoryUserEmail = prefs.getString(KEY_USER_EMAIL, "alex.morgan@campus.edu") ?: "alex.morgan@campus.edu"
        inMemoryPhotoPath = prefs.getString(KEY_PROFILE_PHOTO_PATH, null)
    }

    fun saveSession(context: Context? = null, token: String, userId: Int, name: String, email: String) {
        inMemoryToken = token
        inMemoryUserId = userId
        inMemoryUserName = name
        inMemoryUserEmail = email

        val ctx = context ?: appContext
        ctx?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putString(KEY_AUTH_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun updateUserName(context: Context? = null, name: String) {
        inMemoryUserName = name
        val ctx = context ?: appContext
        ctx?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putString(KEY_USER_NAME, name)
            apply()
        }
    }

    fun getToken(): String? = inMemoryToken

    fun getUserId(): Int = inMemoryUserId

    fun getUserName(): String = inMemoryUserName

    fun getUserEmail(): String = inMemoryUserEmail

    fun getProfilePhotoPath(): String? = inMemoryPhotoPath

    fun saveProfilePhotoPath(context: Context? = null, path: String?) {
        inMemoryPhotoPath = path
        val ctx = context ?: appContext
        ctx?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            if (path == null) remove(KEY_PROFILE_PHOTO_PATH) else putString(KEY_PROFILE_PHOTO_PATH, path)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = !inMemoryToken.isNullOrEmpty()

    fun setLastUploadedAudio(audioId: Int, title: String? = null, duration: String? = null) {
        inMemoryLastAudioId = audioId
        inMemoryLastAudioTitle = title
        inMemoryLastAudioDuration = duration
    }

    fun getLastUploadedAudioId(): Int? = inMemoryLastAudioId

    fun getLastUploadedAudioTitle(): String? = inMemoryLastAudioTitle

    fun getLastUploadedAudioDuration(): String? = inMemoryLastAudioDuration

    fun setLastRecordedAudioPath(path: String?) {
        inMemoryLastRecordedAudioPath = path
    }

    fun getLastRecordedAudioPath(): String? = inMemoryLastRecordedAudioPath

    fun setLastTranscriptionId(transcriptionId: Int) {
        inMemoryLastTranscriptionId = transcriptionId
    }

    fun getLastTranscriptionId(): Int? = inMemoryLastTranscriptionId

    fun setLastTranscription(fullText: String?, paragraphs: List<com.example.Simtas.ui.transcript.TranscriptParagraph> = emptyList()) {
        inMemoryLastTranscriptText = fullText
        inMemoryLastTranscriptParagraphs = paragraphs
    }

    fun getLastTranscriptionText(): String? = inMemoryLastTranscriptText

    fun getLastTranscriptionParagraphs(): List<com.example.Simtas.ui.transcript.TranscriptParagraph> = inMemoryLastTranscriptParagraphs

    fun setLastGeneratedNote(note: GeneratedNotesUiState?) {
        inMemoryLastGeneratedNote = note
        if (note?.noteId != null) {
            inMemoryLastNoteId = note.noteId
        }
        if (note?.quizId != null) {
            inMemoryLastQuizId = note.quizId
        }
        if (note != null && note.educationalResources.isNotEmpty()) {
            inMemoryDiscoveredResources = note.educationalResources
        }
    }

    fun getLastGeneratedNote(): GeneratedNotesUiState? = inMemoryLastGeneratedNote

    fun setLastDetailedNotes(notes: com.example.Simtas.ui.detailednotes.DetailedNotesUiState?) {
        inMemoryLastDetailedNotes = notes
    }

    fun getLastDetailedNotes(): com.example.Simtas.ui.detailednotes.DetailedNotesUiState? = inMemoryLastDetailedNotes

    fun setLastJobId(jobId: Int?) {
        inMemoryLastJobId = jobId
    }

    fun getLastJobId(): Int? = inMemoryLastJobId

    fun setLastNoteId(noteId: Int?) {
        inMemoryLastNoteId = noteId
    }

    fun getLastNoteId(): Int? = inMemoryLastNoteId

    fun setLastQuizId(quizId: Int?) {
        inMemoryLastQuizId = quizId
    }

    fun getLastQuizId(): Int? = inMemoryLastQuizId

    fun setDiscoveredResources(resources: List<com.example.Simtas.ui.notes.DiscoveredResourceItem>) {
        inMemoryDiscoveredResources = resources
    }

    fun setDetectedQuestionsAndAnswers(list: List<com.example.Simtas.ui.notes.DetectedQuestionAnswerItem>) {
        inMemoryDetectedQAs = list
    }

    fun getDetectedQuestionsAndAnswers(): List<com.example.Simtas.ui.notes.DetectedQuestionAnswerItem> = inMemoryDetectedQAs

    fun getDiscoveredResources(): List<com.example.Simtas.ui.notes.DiscoveredResourceItem> = inMemoryDiscoveredResources

    fun setLastQuizResult(result: com.example.Simtas.ui.quizresults.QuizResultsUiState?) {
        inMemoryLastQuizResult = result
    }

    fun getLastQuizResult(): com.example.Simtas.ui.quizresults.QuizResultsUiState? = inMemoryLastQuizResult

    /**
     * Clears all per-lecture temporary state so that a new recording session
     * begins completely isolated with zero artifact bleed.
     */
    fun clearSessionLectureData() {
        inMemoryLastRecordedAudioPath = null
        inMemoryLastTranscriptText = null
        inMemoryLastTranscriptParagraphs = emptyList()
        inMemoryLastTranscriptionId = null
        inMemoryLastGeneratedNote = null
        inMemoryLastDetailedNotes = null
        inMemoryDetectedQAs = emptyList()
        inMemoryDiscoveredResources = emptyList()
        inMemoryLastQuizResult = null
        inMemoryLastJobId = null
        inMemoryLastNoteId = null
        inMemoryLastQuizId = null
        inMemoryLastAudioTitle = null
        inMemoryLastAudioDuration = null
    }

    fun clearSession(context: Context? = null) {
        inMemoryToken = null
        inMemoryUserId = 1
        inMemoryUserName = "Alex Morgan"
        inMemoryUserEmail = "alex.morgan@campus.edu"
        inMemoryPhotoPath = null
        inMemoryLastAudioId = null
        inMemoryLastAudioTitle = null
        inMemoryLastAudioDuration = null
        inMemoryLastTranscriptionId = null
        inMemoryLastGeneratedNote = null
        inMemoryLastJobId = null
        inMemoryLastNoteId = null
        inMemoryLastQuizId = null
        inMemoryDiscoveredResources = emptyList()
        inMemoryDetectedQAs = emptyList()
        inMemoryLastQuizResult = null

        val ctx = context ?: appContext
        ctx?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }
}

