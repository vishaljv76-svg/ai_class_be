package com.example.Simtas.data.repository

import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.notes.DiscoveredResourceItem
import com.example.Simtas.ui.notes.GeneratedNotesUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class JobStartResult(
    val isSuccess: Boolean,
    val jobId: Int? = null,
    val audioId: Int? = null,
    val status: String = "PENDING",
    val progressPercent: Int = 0,
    val currentStep: String = "Queued",
    val message: String? = null
)

data class JobStatusResult(
    val isSuccess: Boolean,
    val jobId: Int? = null,
    val audioId: Int? = null,
    val transcriptionId: Int? = null,
    val noteId: Int? = null,
    val quizId: Int? = null,
    val status: String = "PENDING",
    val progressPercent: Int = 0,
    val currentStep: String = "Processing",
    val noteUiState: GeneratedNotesUiState? = null,
    val resources: List<DiscoveredResourceItem> = emptyList(),
    val errorMessage: String? = null
)

data class JobRetryResult(
    val isSuccess: Boolean,
    val jobId: Int? = null,
    val message: String? = null
)

object JobRepository {

    /**
     * Initiates the full automated lecture processing pipeline on backend.
     */
    suspend fun startLectureProcessing(audioId: Int, runSync: Boolean = false): JobStartResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("audio_id", audioId)
            put("run_sync", runSync)
            put("userId", SessionManager.getUserId())
        }.toString()

        val response = ApiClient.post("jobs/start_lecture_processing.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val jobId = json.optInt("job_id", 0).takeIf { it > 0 }
            if (jobId != null) {
                SessionManager.setLastJobId(jobId)
            }
            JobStartResult(
                isSuccess = true,
                jobId = jobId,
                audioId = json.optInt("audio_id", audioId),
                status = json.optString("status", "PENDING"),
                progressPercent = json.optInt("progress_percent", 5),
                currentStep = json.optString("current_step", "Queued for processing"),
                message = json.optString("message")
            )
        } else {
            JobStartResult(
                isSuccess = false,
                message = response.errorMessage ?: "Failed to start lecture processing"
            )
        }
    }

    /**
     * Fetches current processing stage, progress percentage, and results.
     */
    suspend fun getJobStatus(
        jobId: Int? = SessionManager.getLastJobId(),
        audioId: Int? = SessionManager.getLastUploadedAudioId(),
        advance: Boolean = false
    ): JobStatusResult = withContext(Dispatchers.IO) {
        val queryParams = mutableListOf<String>()
        if (jobId != null && jobId > 0) queryParams.add("job_id=$jobId")
        if (audioId != null && audioId > 0) queryParams.add("audio_id=$audioId")
        if (advance) queryParams.add("advance=1")
        queryParams.add("userId=${SessionManager.getUserId()}")

        val endpoint = "jobs/get_job_status.php?" + queryParams.joinToString("&")
        val response = ApiClient.get(endpoint)

        if (response.isSuccess && response.jsonObject != null) {
            val root = response.jsonObject
            val jobObj = root.optJSONObject("job")

            val id = jobObj?.optInt("id", jobId ?: 0) ?: jobId
            val aId = jobObj?.optInt("audio_id", audioId ?: 0) ?: audioId
            val tId = jobObj?.optInt("transcription_id", 0)?.takeIf { it > 0 }
            val nId = jobObj?.optInt("note_id", 0)?.takeIf { it > 0 }
            val qId = jobObj?.optInt("quiz_id", 0)?.takeIf { it > 0 }
            val status = jobObj?.optString("status", "PENDING") ?: "PENDING"
            val progress = jobObj?.optInt("progress_percent", 0) ?: 0
            val step = jobObj?.optString("current_step", "Processing") ?: "Processing"
            val error = jobObj?.optString("error_message")?.takeIf { it.isNotBlank() && it != "null" }

            if (id != null && id > 0) SessionManager.setLastJobId(id)
            if (tId != null) SessionManager.setLastTranscriptionId(tId)
            if (nId != null) SessionManager.setLastNoteId(nId)
            if (qId != null) SessionManager.setLastQuizId(qId)

            // Parse Discovered Resources
            val resources = mutableListOf<DiscoveredResourceItem>()
            val resArray = root.optJSONArray("resources")
            if (resArray != null) {
                for (i in 0 until resArray.length()) {
                    val r = resArray.optJSONObject(i) ?: continue
                    val resItem = DiscoveredResourceItem(
                        id = r.optInt("id", 0),
                        title = r.optString("title", "Educational Resource"),
                        url = r.optString("url", ""),
                        source = r.optString("source", "Web"),
                        topic = r.optString("topic").takeIf { it.isNotBlank() && it != "null" },
                        snippet = r.optString("snippet").takeIf { it.isNotBlank() && it != "null" },
                        isVerified = r.optInt("is_verified", 1) == 1
                    )
                    if (resItem.url.isNotBlank()) {
                        resources.add(resItem)
                    }
                }
            }

            if (resources.isNotEmpty()) {
                SessionManager.setDiscoveredResources(resources)
            }

            // Parse Note UI State if note data is returned
            var generatedState: GeneratedNotesUiState? = null
            val noteObj = root.optJSONObject("note")
            if (noteObj != null) {
                val title = noteObj.optString("title").ifBlank {
                    SessionManager.getLastUploadedAudioTitle() ?: "Database Normalization"
                }
                val subject = noteObj.optString("subject").ifBlank { "Computer Science" }
                val summary = noteObj.optString("summary")
                val courseCode = noteObj.optString("course_code").ifBlank { "CS301" }
                val duration = noteObj.optString("duration").ifBlank { "42 min" }

                val takeaways = mutableListOf<String>()
                val tArray = noteObj.optJSONArray("key_takeaways")
                if (tArray != null) {
                    for (i in 0 until tArray.length()) {
                        takeaways.add(tArray.optString(i))
                    }
                }

                val terms = mutableListOf<String>()
                val termsArray = noteObj.optJSONArray("important_terms")
                if (termsArray != null) {
                    for (i in 0 until termsArray.length()) {
                        terms.add(termsArray.optString(i))
                    }
                }

                generatedState = GeneratedNotesUiState(
                    noteId = nId,
                    quizId = qId,
                    subjectBadge = subject,
                    lectureTitle = title,
                    metadataText = "$duration · $courseCode · AI notes",
                    aiSummaryText = summary.ifBlank { "Key concepts and principles from the lecture." },
                    keyTakeaways = if (takeaways.isNotEmpty()) takeaways else listOf(
                        "Repeated data can cause update, insertion and deletion anomalies.",
                        "2NF removes partial dependencies on a composite key.",
                        "3NF keeps non-key attributes dependent only on the key."
                    ),
                    importantTerms = if (terms.isNotEmpty()) terms else listOf(
                        "Functional dependency", "Composite key", "Normal forms"
                    ),
                    educationalResources = resources,
                    isSaved = true
                )
                SessionManager.setLastGeneratedNote(generatedState)
            }

            JobStatusResult(
                isSuccess = true,
                jobId = id,
                audioId = aId,
                transcriptionId = tId,
                noteId = nId,
                quizId = qId,
                status = status,
                progressPercent = progress,
                currentStep = step,
                noteUiState = generatedState,
                resources = resources,
                errorMessage = error
            )
        } else {
            JobStatusResult(
                isSuccess = false,
                errorMessage = response.errorMessage ?: "Failed to fetch job status"
            )
        }
    }

    /**
     * Retries a processing job from the last failed stage.
     */
    suspend fun retryJob(jobId: Int, runSync: Boolean = false): JobRetryResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("job_id", jobId)
            put("run_sync", runSync)
            put("userId", SessionManager.getUserId())
        }.toString()

        val response = ApiClient.post("jobs/retry_job.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            JobRetryResult(
                isSuccess = true,
                jobId = jobId,
                message = response.jsonObject.optString("message")
            )
        } else {
            JobRetryResult(
                isSuccess = false,
                message = response.errorMessage ?: "Failed to retry job"
            )
        }
    }

    /**
     * Fetches educational resources for a note.
     */
    suspend fun getLectureResources(noteId: Int): List<DiscoveredResourceItem> = withContext(Dispatchers.IO) {
        val endpoint = "resources/get_lecture_resources.php?note_id=$noteId&userId=${SessionManager.getUserId()}"
        val response = ApiClient.get(endpoint)
        val list = mutableListOf<DiscoveredResourceItem>()

        if (response.isSuccess && response.jsonObject != null) {
            val arr = response.jsonObject.optJSONArray("resources")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i) ?: continue
                    list.add(
                        DiscoveredResourceItem(
                            id = r.optInt("id", 0),
                            title = r.optString("title", "Resource"),
                            url = r.optString("url", ""),
                            source = r.optString("source", "Web"),
                            topic = r.optString("topic").takeIf { it.isNotBlank() && it != "null" },
                            snippet = r.optString("snippet").takeIf { it.isNotBlank() && it != "null" },
                            isVerified = r.optInt("is_verified", 1) == 1
                        )
                    )
                }
            }
        }
        list
    }
}
