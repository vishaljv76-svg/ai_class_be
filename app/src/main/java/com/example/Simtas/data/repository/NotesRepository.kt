package com.example.Simtas.data.repository

import com.example.Simtas.data.model.NoteModel
import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.home.RecentNoteIconType
import com.example.Simtas.ui.mynotes.NoteListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

data class NotesFetchResult(
    val totalNotes: Int,
    val subjectsCount: Int,
    val statsSubtitle: String,
    val backupStatusText: String,
    val notes: List<NoteListItem>
)

/**
 * Repository interface for managing notes data.
 * Adheres to MVVM architecture with real-time PHP/MySQL integration.
 */
interface NotesRepository {
    fun getNotes(): Flow<List<NoteModel>>
}

/**
 * Default implementation of [NotesRepository] integrated with PHP REST API.
 */
class NotesRepositoryImpl : NotesRepository {

    override fun getNotes(): Flow<List<NoteModel>> = flow {
        val result = fetchNotes()
        val models = result.notes.map { item ->
            NoteModel(
                id = item.id,
                title = item.title,
                summary = "${item.subject} · ${item.durationText}",
                fullText = item.title,
                timestamp = System.currentTimeMillis()
            )
        }
        emit(models)
    }

    companion object {

        suspend fun fetchNotes(search: String = "", category: String = ""): NotesFetchResult {
            val userId = SessionManager.getUserId()
            var path = "notes/list_notes.php?user_id=$userId"
            if (search.isNotBlank()) {
                path += "&search=" + java.net.URLEncoder.encode(search, "UTF-8")
            }
            if (category.isNotBlank() && category != "All") {
                path += "&category=" + java.net.URLEncoder.encode(category, "UTF-8")
            }

            val response = ApiClient.get(path)
            if (response.isSuccess && response.jsonObject != null) {
                val json = response.jsonObject
                val totalNotes = json.optInt("total_notes", 0)
                val subjectsCount = json.optInt("subjects_count", 0)
                val subtitle = json.optString("stats_subtitle", "$totalNotes notes · $subjectsCount subjects")
                val backup = json.optString("backup_status_text", "All notes backed up · Updated just now")

                val notesArray = json.optJSONArray("notes") ?: JSONArray()
                val list = mutableListOf<NoteListItem>()

                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.optJSONObject(i) ?: continue
                    val iconStr = obj.optString("iconType", "Database")
                    val iconType = if (iconStr.equals("Math", ignoreCase = true)) {
                        RecentNoteIconType.Math
                    } else {
                        RecentNoteIconType.Database
                    }

                    list.add(
                        NoteListItem(
                            id = obj.optString("id", i.toString()),
                            title = obj.optString("title", "Untitled Note"),
                            subject = obj.optString("subject", "Computer Science"),
                            dateText = obj.optString("dateText", "Today"),
                            durationText = obj.optString("durationText", "40 min"),
                            readTimeText = obj.optString("readTimeText", "5 min read"),
                            iconType = iconType
                        )
                    )
                }

                return NotesFetchResult(
                    totalNotes = totalNotes,
                    subjectsCount = subjectsCount,
                    statsSubtitle = subtitle,
                    backupStatusText = backup,
                    notes = list
                )
            }

            // Safe fallback if network fails
            return NotesFetchResult(
                totalNotes = 0,
                subjectsCount = 0,
                statsSubtitle = "0 notes · 0 subjects",
                backupStatusText = "Connecting to server...",
                notes = emptyList()
            )
        }

        suspend fun fetchNoteById(noteId: String): DetailedNotesUiState? {
            val response = ApiClient.get("notes/get_note.php?id=$noteId")
            if (response.isSuccess && response.jsonObject != null) {
                val obj = response.jsonObject.optJSONObject("note") ?: return null

                val bulletsArray = obj.optJSONArray("section2Bullets")
                val bulletsList = mutableListOf<String>()
                if (bulletsArray != null) {
                    for (i in 0 until bulletsArray.length()) {
                        bulletsList.add(bulletsArray.optString(i))
                    }
                }
                if (bulletsList.isEmpty()) {
                    bulletsList.addAll(listOf(
                        "1NF — Atomic values in each cell; no repeating groups.",
                        "2NF — In 1NF, and every non-key attribute depends on the whole candidate key.",
                        "3NF — In 2NF, with no transitive dependency between non-key attributes."
                    ))
                }

                return DetailedNotesUiState(
                    courseCode = obj.optString("courseCode", "CS301"),
                    tagText = obj.optString("tagText", "AI notes"),
                    lectureTitle = obj.optString("lectureTitle", "Database Normalization"),
                    metadataText = obj.optString("metadataText", "Prof. Sarah Chen · 21 Oct 2026 · 6 min read"),
                    section1Title = obj.optString("section1Title", "01 Why normalize?"),
                    section1Content = obj.optString("section1Content", "Normalization organizes relations to minimize repeated data."),
                    section2Title = obj.optString("section2Title", "02 Key normal forms"),
                    section2Bullets = bulletsList,
                    calloutTitle = obj.optString("calloutTitle", "Functional dependency: X → Y"),
                    calloutContent = obj.optString("calloutContent", "Each value of X determines exactly one value of Y."),
                    section3Title = obj.optString("section3Title", "03 Worked example"),
                    section3Content1 = obj.optString("section3Content1", "Sample relational schema decomposition."),
                    section3Content2 = obj.optString("section3Content2", "Resulting tables satisfy 3NF consistency requirements."),
                    footerText = obj.optString("footerText", "Excerpt · Next: In-depth practice"),
                    isBookmarked = obj.optBoolean("isBookmarked", false),
                    isLoading = false
                )
            }
            return null
        }

        suspend fun createNote(
            title: String,
            subject: String,
            summary: String,
            keyTakeaways: List<String> = emptyList(),
            importantTerms: List<String> = emptyList()
        ): String? {
            val payload = JSONObject().apply {
                put("userId", SessionManager.getUserId())
                put("title", title)
                put("subject", subject)
                put("summary", summary)
                put("keyTakeaways", JSONArray(keyTakeaways))
                put("importantTerms", JSONArray(importantTerms))
            }.toString()

            val response = ApiClient.post("notes/create_note.php", payload)
            if (response.isSuccess && response.jsonObject != null) {
                return response.jsonObject.optString("note_id")
            }
            return null
        }

        suspend fun deleteNote(noteId: String): Boolean {
            val response = ApiClient.post("notes/delete_note.php?id=$noteId", "{}")
            return response.isSuccess
        }

        suspend fun toggleBookmark(noteId: String, isBookmarked: Boolean): Boolean {
            val payload = JSONObject().apply {
                put("id", noteId)
                put("isBookmarked", isBookmarked)
            }.toString()
            val response = ApiClient.post("notes/update_note.php", payload)
            return response.isSuccess
        }
    }
}
