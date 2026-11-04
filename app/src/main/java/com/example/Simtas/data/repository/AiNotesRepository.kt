package com.example.Simtas.data.repository

import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.notes.GeneratedNotesUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class GeneratedNotesResult(
    val isSuccess: Boolean,
    val noteId: String? = null,
    val transcriptionId: Int? = null,
    val audioId: Int? = null,
    val notesUiState: GeneratedNotesUiState? = null,
    val message: String? = null,
    val errorCode: String? = null
)

data class SaveNoteResult(
    val isSuccess: Boolean,
    val noteId: String? = null,
    val message: String? = null
)

object AiNotesRepository {

    /**
     * Generates structured study notes from a lecture transcription via `notes/generate_ai_notes.php`.
     */
    suspend fun generateNotes(
        transcriptionId: Int? = SessionManager.getLastTranscriptionId(),
        audioId: Int? = SessionManager.getLastUploadedAudioId(),
        lectureTitle: String? = SessionManager.getLastUploadedAudioTitle(),
        subject: String? = null,
        saveToDb: Boolean = false
    ): GeneratedNotesResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            if (transcriptionId != null && transcriptionId > 0) {
                put("transcription_id", transcriptionId)
            }
            if (audioId != null && audioId > 0) {
                put("audio_id", audioId)
            }
            if (!lectureTitle.isNullOrBlank()) {
                put("lecture_title", lectureTitle)
            }
            if (!subject.isNullOrBlank()) {
                put("subject", subject)
            }
            put("save_to_db", saveToDb)
            put("userId", SessionManager.getUserId())
        }.toString()

        val response = ApiClient.post("notes/generate_ai_notes.php", payload)

        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val noteObj = json.optJSONObject("note")
            val noteId = json.optString("note_id").ifBlank { noteObj?.optString("id") }
            val tid = json.optInt("transcription_id", transcriptionId ?: 0)
            val aid = json.optInt("audio_id", audioId ?: 0)

            val title = noteObj?.optString("title")
                ?: noteObj?.optString("lectureTitle")
                ?: lectureTitle ?: "Lecture Study Notes"

            val subj = noteObj?.optString("subject")
                ?: noteObj?.optString("subjectBadge")
                ?: subject ?: "Computer Science"

            val meta = noteObj?.optString("metadataText")
                ?: "Prof. Sarah Chen · CS301"

            val summary = noteObj?.optString("summary")
                ?: noteObj?.optString("aiSummaryText")
                ?: ""

            val takeaways = mutableListOf<String>()
            val takeawaysArray = noteObj?.optJSONArray("keyTakeaways")
            if (takeawaysArray != null) {
                for (i in 0 until takeawaysArray.length()) {
                    takeaways.add(takeawaysArray.optString(i))
                }
            }

            val terms = mutableListOf<String>()
            val termsArray = noteObj?.optJSONArray("importantTerms")
            if (termsArray != null) {
                for (i in 0 until termsArray.length()) {
                    terms.add(termsArray.optString(i))
                }
            }

            val generatedState = GeneratedNotesUiState(
                subjectBadge = subj,
                lectureTitle = title,
                metadataText = meta,
                aiSummaryText = summary,
                keyTakeaways = if (takeaways.isNotEmpty()) takeaways else listOf("Core foundational concepts from lecture."),
                importantTerms = if (terms.isNotEmpty()) terms else listOf("Key concepts", "Definitions"),
                isSaved = saveToDb,
                isLoading = false
            )

            SessionManager.setLastGeneratedNote(generatedState)
            if (tid > 0) SessionManager.setLastTranscriptionId(tid)

            GeneratedNotesResult(
                isSuccess = true,
                noteId = noteId,
                transcriptionId = tid,
                audioId = aid,
                notesUiState = generatedState,
                message = json.optString("message", "Study notes generated successfully")
            )
        } else {
            val json = response.jsonObject
            val errCode = json?.optString("error_code")
            val msg = json?.optString("message") ?: response.errorMessage ?: "Failed to generate notes"

            GeneratedNotesResult(
                isSuccess = false,
                message = msg,
                errorCode = errCode
            )
        }
    }

    /**
     * Saves generated study notes directly to MySQL via `notes/save_generated_note.php`.
     */
    suspend fun saveGeneratedNote(
        transcriptionId: Int? = SessionManager.getLastTranscriptionId(),
        uiState: GeneratedNotesUiState
    ): SaveNoteResult = withContext(Dispatchers.IO) {
        val tid = transcriptionId ?: SessionManager.getLastTranscriptionId()

        // If transcription ID is present, save via linked save_generated_note.php
        if (tid != null && tid > 0) {
            val payload = JSONObject().apply {
                put("transcription_id", tid)
                put("userId", SessionManager.getUserId())
                put("title", uiState.lectureTitle)
                put("subject", uiState.subjectBadge)
                put("summary", uiState.aiSummaryText)
                put("keyTakeaways", JSONArray(uiState.keyTakeaways))
                put("importantTerms", JSONArray(uiState.importantTerms))
            }.toString()

            val response = ApiClient.post("notes/save_generated_note.php", payload)
            if (response.isSuccess && response.jsonObject != null) {
                val json = response.jsonObject
                val noteId = json.optString("note_id")
                return@withContext SaveNoteResult(
                    isSuccess = true,
                    noteId = noteId,
                    message = json.optString("message", "Generated notes saved successfully")
                )
            }
        }

        // Direct fallback via notes/create_note.php
        val fallbackNoteId = NotesRepositoryImpl.createNote(
            title = uiState.lectureTitle,
            subject = uiState.subjectBadge,
            summary = uiState.aiSummaryText,
            keyTakeaways = uiState.keyTakeaways,
            importantTerms = uiState.importantTerms
        )

        if (fallbackNoteId != null) {
            SaveNoteResult(
                isSuccess = true,
                noteId = fallbackNoteId,
                message = "Note saved to database successfully"
            )
        } else {
            SaveNoteResult(
                isSuccess = false,
                message = "Unable to save note to database"
            )
        }
    }
}

