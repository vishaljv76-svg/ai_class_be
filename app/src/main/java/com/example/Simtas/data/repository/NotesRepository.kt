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
                            iconType = iconType,
                            summary = obj.optString("summary", ""),
                            tag = obj.optString("tag", "AI notes")
                        )
                    )
                }

                // Merge local notes (avoiding duplicate IDs)
                val localNotes = com.example.Simtas.data.local.LectureNotesStorage.getAllNotesForList(userId)
                for (ln in localNotes) {
                    if (list.none { it.id == ln.id || it.title.equals(ln.title, ignoreCase = true) }) {
                        list.add(0, ln)
                    }
                }

                return NotesFetchResult(
                    totalNotes = list.size,
                    subjectsCount = list.map { it.subject }.distinct().size,
                    statsSubtitle = subtitle,
                    backupStatusText = backup,
                    notes = list
                )
            }

            // Safe fallback to local storage if network fails
            val localNotes = com.example.Simtas.data.local.LectureNotesStorage.getAllNotesForList(userId)
            val filtered = if (category.isNotBlank() && category != "All") {
                localNotes.filter { it.subject.equals(category, ignoreCase = true) }
            } else {
                localNotes
            }
            val searched = if (search.isNotBlank()) {
                filtered.filter { it.title.contains(search, ignoreCase = true) }
            } else {
                filtered
            }

            return NotesFetchResult(
                totalNotes = searched.size,
                subjectsCount = searched.map { it.subject }.distinct().size,
                statsSubtitle = "${searched.size} notes · ${searched.map { it.subject }.distinct().size} subjects",
                backupStatusText = "Offline mode · Loaded from device storage",
                notes = searched
            )
        }

        suspend fun fetchNoteById(noteId: String): DetailedNotesUiState? {
            val local = com.example.Simtas.data.local.LectureNotesStorage.getDetailedNotes(noteId)
            if (local != null) return local

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
                val noteTitle = obj.optString("lectureTitle", obj.optString("title", "Lecture Notes"))
                val noteSummary = obj.optString("summary", "")
                if (bulletsList.isEmpty()) {
                    if (noteSummary.isNotBlank()) {
                        bulletsList.add(noteSummary)
                    } else {
                        bulletsList.addAll(listOf(
                            "1NF — Atomic values in each cell; no repeating groups.",
                            "2NF — In 1NF, and every non-key attribute depends on the whole candidate key.",
                            "3NF — In 2NF, with no transitive dependency between non-key attributes."
                        ))
                    }
                }

                return DetailedNotesUiState(
                    courseCode = obj.optString("courseCode", "CS301"),
                    tagText = obj.optString("tagText", "AI notes"),
                    lectureTitle = noteTitle,
                    metadataText = obj.optString("metadataText", "Prof. Sarah Chen · 21 Oct 2026 · 6 min read"),
                    section1Title = obj.optString("section1Title", "01 Overview"),
                    section1Content = obj.optString("section1Content", if (noteSummary.isNotBlank()) noteSummary else "Study note content."),
                    section2Title = obj.optString("section2Title", "02 Key Concepts"),
                    section2Bullets = bulletsList,
                    calloutTitle = obj.optString("calloutTitle", "Key Concept"),
                    calloutContent = obj.optString("calloutContent", if (noteSummary.isNotBlank()) noteSummary else "Key principle."),
                    section3Title = obj.optString("section3Title", "03 Complete Explanation"),
                    section3Content1 = obj.optString("section3Content1", if (noteSummary.isNotBlank()) noteSummary else "Detailed note explanation."),
                    section3Content2 = obj.optString("section3Content2", ""),
                    footerText = obj.optString("footerText", "Study Notes"),
                    isBookmarked = obj.optBoolean("isBookmarked", false),
                    isLoading = false
                )
            }
            return null
        }

        private val savedQaDeduplicationMap = java.util.concurrent.ConcurrentHashMap<String, Long>()

        suspend fun saveAiQuestionAnswer(
            question: String,
            answer: String,
            subject: String = "Computer Science",
            lectureTitle: String = "AI Study Copilot",
            dateText: String = "Today",
            timeText: String = ""
        ): String? {
            val q = question.trim()
            val a = answer.trim()
            if (q.isBlank() || a.isBlank()) return null

            val userId = SessionManager.getUserId()
            val dedupKey = "$userId|$q|${a.take(64)}"
            val now = System.currentTimeMillis()
            val lastSaved = savedQaDeduplicationMap[dedupKey]
            if (lastSaved != null && now - lastSaved < 120_000L) {
                // Duplicate entry within 2 minutes: prevent duplicate save
                return null
            }
            savedQaDeduplicationMap[dedupKey] = now

            val localId = com.example.Simtas.data.local.LectureNotesStorage.saveAiQuestionAnswerNote(
                question = q,
                answer = a,
                subject = subject,
                lectureTitle = lectureTitle,
                dateText = dateText,
                userId = userId
            )

            val formattedTime = if (timeText.isNotBlank()) " · $timeText" else ""
            val payload = JSONObject().apply {
                put("userId", userId)
                put("title", q)
                put("subject", subject)
                put("tag", "AI Q&A")
                put("summary", a)
                put("full_content", a)
                put("section1Title", "01 Question Asked")
                put("section1Content", q)
                put("section2Title", "02 AI Answer")
                put("section2Bullets", JSONArray(listOf(a)))
                put("calloutTitle", "Conversation Reference")
                put("calloutContent", "Topic: $lectureTitle · $dateText$formattedTime")
                put("section3Title", "03 Complete Explanation")
                put("section3Content1", a)
                put("footerText", "AI Study Copilot · $dateText$formattedTime")
            }.toString()

            val response = ApiClient.post("notes/create_note.php", payload)
            if (response.isSuccess && response.jsonObject != null) {
                return response.jsonObject.optString("note_id", localId)
            }
            return localId
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
