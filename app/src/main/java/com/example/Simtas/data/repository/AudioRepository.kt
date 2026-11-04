package com.example.Simtas.data.repository

import android.content.Context
import android.net.Uri
import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.transcript.TranscriptParagraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AudioUploadResult(
    val isSuccess: Boolean,
    val audioId: Int? = null,
    val filePath: String? = null,
    val message: String? = null
)

data class TranscriptionResult(
    val isSuccess: Boolean,
    val transcriptionId: Int? = null,
    val audioId: Int? = null,
    val lectureTitle: String = "",
    val language: String = "English",
    val durationAndConfidence: String = "",
    val fullTranscript: String = "",
    val paragraphs: List<TranscriptParagraph> = emptyList(),
    val message: String? = null
)

object AudioRepository {

    /**
     * Uploads an audio file via multipart form-data to `audio/upload_audio.php`.
     * Streams the file in chunks with real-time progress callbacks.
     */
    suspend fun uploadAudio(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        title: String = "",
        duration: String = "",
        onProgress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): AudioUploadResult = withContext(Dispatchers.IO) {
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            return@withContext AudioUploadResult(
                isSuccess = false,
                message = "Could not open selected audio file: ${e.localizedMessage ?: "File inaccessible"}"
            )
        }

        if (inputStream == null) {
            return@withContext AudioUploadResult(
                isSuccess = false,
                message = "Unable to read audio file from device storage."
            )
        }

        val fields = mutableMapOf<String, String>()
        val userId = SessionManager.getUserId()
        fields["userId"] = userId.toString()
        if (title.isNotBlank()) fields["title"] = title
        if (duration.isNotBlank()) fields["duration"] = duration

        val response = ApiClient.uploadMultipart(
            path = "audio/upload_audio.php",
            inputStream = inputStream,
            fileName = fileName,
            fileSize = fileSize,
            formFields = fields,
            onProgress = onProgress
        )

        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val audioId = json.optInt("audio_id", 0).takeIf { it > 0 }
                ?: json.optJSONObject("audio")?.optInt("id", 0)?.takeIf { it > 0 }

            val filePath = json.optJSONObject("audio")?.optString("filePath")
                ?: json.optString("file_path")

            if (audioId != null && audioId > 0) {
                SessionManager.setLastUploadedAudio(audioId, title.ifBlank { fileName }, duration)
            }

            AudioUploadResult(
                isSuccess = true,
                audioId = audioId,
                filePath = filePath,
                message = json.optString("message", "Audio uploaded successfully")
            )
        } else {
            AudioUploadResult(
                isSuccess = false,
                message = response.errorMessage ?: "Audio upload failed on server"
            )
        }
    }

    /**
     * Fetches saved transcription for an audio file from `audio/get_transcription.php`.
     */
    suspend fun getTranscription(audioId: Int): TranscriptionResult = withContext(Dispatchers.IO) {
        val response = ApiClient.get("audio/get_transcription.php?audio_id=$audioId")
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val transObj = json.optJSONObject("transcription") ?: return@withContext TranscriptionResult(
                isSuccess = false,
                message = "Transcription object missing in server response"
            )

            val id = transObj.optInt("id")
            val lectureTitle = transObj.optString("lectureTitle", "Lecture")
            val language = transObj.optString("language", "English")
            val durationAndConfidence = transObj.optString("durationAndConfidence", "42:18 · 98% confidence")
            val fullTranscript = transObj.optString("fullTranscript", "")

            val paragraphsList = mutableListOf<TranscriptParagraph>()
            val paragraphsArray = transObj.optJSONArray("paragraphs") ?: JSONArray()
            for (i in 0 until paragraphsArray.length()) {
                val p = paragraphsArray.optJSONObject(i) ?: continue
                paragraphsList.add(
                    TranscriptParagraph(
                        timestamp = p.optString("timestamp", "00:00"),
                        speaker = p.optString("speaker", "Speaker"),
                        text = p.optString("text", ""),
                        isHighlighted = i == 0
                    )
                )
            }

            TranscriptionResult(
                isSuccess = true,
                transcriptionId = id,
                audioId = audioId,
                lectureTitle = lectureTitle,
                language = language,
                durationAndConfidence = durationAndConfidence,
                fullTranscript = fullTranscript,
                paragraphs = paragraphsList
            )
        } else {
            TranscriptionResult(
                isSuccess = false,
                message = response.errorMessage ?: "No transcription found"
            )
        }
    }

    /**
     * Triggers live speech-to-text audio transcription via `audio/transcribe_audio.php`.
     */
    suspend fun transcribeAudio(audioId: Int): TranscriptionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("audio_id", audioId)
            put("userId", SessionManager.getUserId())
        }.toString()

        val response = ApiClient.post("audio/transcribe_audio.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val transObj = json.optJSONObject("transcription")
            val id = json.optInt("transcription_id", 0)

            val lectureTitle = transObj?.optString("lectureTitle")
                ?: json.optString("lecture_title", "Lecture")
            val durationAndConfidence = transObj?.optString("durationAndConfidence")
                ?: "42:18 · 98% confidence"
            val fullTranscript = transObj?.optString("fullTranscript")
                ?: json.optString("full_transcript", "")

            val paragraphsList = mutableListOf<TranscriptParagraph>()
            val paragraphsArray = transObj?.optJSONArray("paragraphs") ?: JSONArray()
            for (i in 0 until paragraphsArray.length()) {
                val p = paragraphsArray.optJSONObject(i) ?: continue
                paragraphsList.add(
                    TranscriptParagraph(
                        timestamp = p.optString("timestamp", "00:00"),
                        speaker = p.optString("speaker", "Speaker"),
                        text = p.optString("text", "")
                    )
                )
            }

            TranscriptionResult(
                isSuccess = true,
                transcriptionId = id,
                audioId = audioId,
                lectureTitle = lectureTitle,
                durationAndConfidence = durationAndConfidence,
                fullTranscript = fullTranscript,
                paragraphs = paragraphsList,
                message = json.optString("message")
            )
        } else {
            TranscriptionResult(
                isSuccess = false,
                message = response.errorMessage ?: "Transcription service error"
            )
        }
    }

    /**
     * Saves transcription text and structured paragraphs directly via `audio/save_transcription.php`.
     */
    suspend fun saveTranscription(
        audioId: Int,
        lectureTitle: String,
        fullTranscript: String,
        paragraphs: List<TranscriptParagraph>,
        duration: String = "42:18",
        language: String = "English"
    ): Boolean = withContext(Dispatchers.IO) {
        val parasArray = JSONArray()
        for (p in paragraphs) {
            parasArray.put(JSONObject().apply {
                put("timestamp", p.timestamp)
                put("speaker", p.speaker)
                put("text", p.text)
            })
        }

        val payload = JSONObject().apply {
            put("userId", SessionManager.getUserId())
            put("audio_id", audioId)
            put("lecture_title", lectureTitle)
            put("full_transcript", fullTranscript)
            put("paragraphs", parasArray)
            put("duration", duration)
            put("language", language)
        }.toString()

        val response = ApiClient.post("audio/save_transcription.php", payload)
        response.isSuccess
    }
}

