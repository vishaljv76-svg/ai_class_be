package com.example.Simtas.data.local

import android.content.Context
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.home.RecentNoteIconType
import com.example.Simtas.ui.home.RecentNoteItem
import com.example.Simtas.ui.mynotes.NoteListItem
import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import com.example.Simtas.ui.notes.GeneratedNotesUiState
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class SavedLectureItem(
    val id: String,
    val title: String,
    val subject: String,
    val dateText: String,
    val durationText: String,
    val transcriptText: String,
    val generatedNotes: GeneratedNotesUiState,
    val detailedNotes: DetailedNotesUiState
)

/**
 * Dedicated Persistent Lecture & Study Notes Storage.
 * Ensures every lecture has its own separate saved study notes.
 *
 * Guarantees:
 * - Each lecture stores its full detailed study notes, generated summary, takeaways, and transcript.
 * - Reopening an old lecture loads its own saved notes (never overwritten by another lecture).
 * - App restart preserves all saved notes on-device via SharedPreferences.
 * - Dual-layer storage: In-memory map (for instant access and unit tests) + Persistent disk storage.
 */
object LectureNotesStorage {

    private const val PREFS_NAME = "simtas_saved_lectures"
    private const val KEY_LECTURE_IDS = "saved_lecture_ids"
    private const val KEY_PREFIX_LECTURE = "lecture_data_"

    private var appContext: Context? = null
    private val inMemoryLectures = ConcurrentHashMap<String, SavedLectureItem>()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadAllFromDisk()
    }

    /**
     * Saves a complete lecture bundle with separate study notes.
     */
    fun saveLecture(
        lectureId: String,
        title: String,
        subject: String,
        transcriptText: String,
        generatedNotes: GeneratedNotesUiState,
        detailedNotes: DetailedNotesUiState,
        durationText: String = "40 min",
        dateText: String = "Today"
    ): String {
        val id = if (lectureId.isNotBlank()) lectureId else (System.currentTimeMillis() / 1000).toString()

        val item = SavedLectureItem(
            id = id,
            title = title,
            subject = subject,
            dateText = dateText,
            durationText = durationText,
            transcriptText = transcriptText,
            generatedNotes = generatedNotes.copy(lectureTitle = title, subjectBadge = subject),
            detailedNotes = detailedNotes.copy(lectureTitle = title)
        )

        inMemoryLectures[id] = item
        saveToDisk(item)
        return id
    }

    /**
     * Retrieves the detailed study notes for a specific lecture ID or title.
     */
    fun getDetailedNotes(lectureId: String): DetailedNotesUiState? {
        val item = inMemoryLectures[lectureId] ?: inMemoryLectures.values.firstOrNull {
            it.title.equals(lectureId, ignoreCase = true)
        }
        return item?.detailedNotes
    }

    /**
     * Retrieves the generated study notes for a specific lecture ID or title.
     */
    fun getGeneratedNotes(lectureId: String): GeneratedNotesUiState? {
        val item = inMemoryLectures[lectureId] ?: inMemoryLectures.values.firstOrNull {
            it.title.equals(lectureId, ignoreCase = true)
        }
        return item?.generatedNotes
    }

    /**
     * Retrieves the transcript text for a specific lecture ID or title.
     */
    fun getTranscript(lectureId: String): String? {
        val item = inMemoryLectures[lectureId] ?: inMemoryLectures.values.firstOrNull {
            it.title.equals(lectureId, ignoreCase = true)
        }
        return item?.transcriptText
    }

    /**
     * Retrieves the entire saved lecture item by ID or title.
     */
    fun getSavedLecture(lectureId: String): SavedLectureItem? {
        return inMemoryLectures[lectureId] ?: inMemoryLectures.values.firstOrNull {
            it.title.equals(lectureId, ignoreCase = true)
        }
    }

    /**
     * Returns all saved lectures formatted for MyNotesScreen.
     */
    fun getAllNotesForList(): List<NoteListItem> {
        return inMemoryLectures.values.map { item ->
            val iconType = if (item.subject.equals("Mathematics", ignoreCase = true)) {
                RecentNoteIconType.Math
            } else {
                RecentNoteIconType.Database
            }
            NoteListItem(
                id = item.id,
                title = item.title,
                subject = item.subject,
                dateText = item.dateText,
                durationText = item.durationText,
                readTimeText = "5 min read",
                iconType = iconType
            )
        }
    }

    /**
     * Returns all saved lectures formatted for DashboardScreen.
     */
    fun getAllRecentNotes(): List<RecentNoteItem> {
        return inMemoryLectures.values.map { item ->
            val iconType = if (item.subject.equals("Mathematics", ignoreCase = true)) {
                RecentNoteIconType.Math
            } else {
                RecentNoteIconType.Database
            }
            RecentNoteItem(
                id = item.id,
                title = item.title,
                subject = item.subject,
                date = item.dateText,
                duration = item.durationText,
                iconType = iconType
            )
        }
    }

    /**
     * Deletes a lecture by ID.
     */
    fun deleteLecture(lectureId: String): Boolean {
        val removed = inMemoryLectures.remove(lectureId) != null
        val ctx = appContext ?: return removed
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idSet = prefs.getStringSet(KEY_LECTURE_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        idSet.remove(lectureId)
        prefs.edit()
            .putStringSet(KEY_LECTURE_IDS, idSet)
            .remove(KEY_PREFIX_LECTURE + lectureId)
            .apply()
        return removed
    }

    /**
     * Clears all in-memory lectures (useful for unit testing).
     */
    fun clearInMemory() {
        inMemoryLectures.clear()
    }

    // --- Private Disk Serialization ---

    private fun saveToDisk(item: SavedLectureItem) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = serializeLecture(item)

            val idSet = prefs.getStringSet(KEY_LECTURE_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            idSet.add(item.id)

            prefs.edit()
                .putStringSet(KEY_LECTURE_IDS, idSet)
                .putString(KEY_PREFIX_LECTURE + item.id, json.toString())
                .apply()
        } catch (_: Exception) {}
    }

    private fun loadAllFromDisk() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val idSet = prefs.getStringSet(KEY_LECTURE_IDS, emptySet()) ?: emptySet()
            for (id in idSet) {
                val jsonStr = prefs.getString(KEY_PREFIX_LECTURE + id, null) ?: continue
                val item = deserializeLecture(JSONObject(jsonStr))
                if (item != null) {
                    inMemoryLectures[item.id] = item
                }
            }
        } catch (_: Exception) {}
    }

    private fun serializeLecture(item: SavedLectureItem): JSONObject {
        val json = JSONObject()
        json.put("id", item.id)
        json.put("title", item.title)
        json.put("subject", item.subject)
        json.put("dateText", item.dateText)
        json.put("durationText", item.durationText)
        json.put("transcriptText", item.transcriptText)

        // Generated notes
        val gen = JSONObject()
        gen.put("subjectBadge", item.generatedNotes.subjectBadge)
        gen.put("lectureTitle", item.generatedNotes.lectureTitle)
        gen.put("metadataText", item.generatedNotes.metadataText)
        gen.put("aiSummaryText", item.generatedNotes.aiSummaryText)
        gen.put("keyTakeaways", JSONArray(item.generatedNotes.keyTakeaways))
        gen.put("importantTerms", JSONArray(item.generatedNotes.importantTerms))
        gen.put("mathQuestion", item.generatedNotes.mathQuestion)
        gen.put("mathAnswer", item.generatedNotes.mathAnswer)
        gen.put("mathExplanation", item.generatedNotes.mathExplanation)
        gen.put("hasMathSolution", item.generatedNotes.hasMathSolution)

        val qasArray = JSONArray()
        for (qa in item.generatedNotes.detectedQuestionsAndAnswers) {
            val qaObj = JSONObject()
            qaObj.put("question", qa.question)
            qaObj.put("answer", qa.answer)
            qaObj.put("explanation", qa.explanation)
            qaObj.put("status", qa.status)
            qaObj.put("isAnswered", qa.isAnswered)
            qasArray.put(qaObj)
        }
        gen.put("qas", qasArray)
        json.put("generatedNotes", gen)

        // Detailed notes
        val det = JSONObject()
        det.put("courseCode", item.detailedNotes.courseCode)
        det.put("tagText", item.detailedNotes.tagText)
        det.put("lectureTitle", item.detailedNotes.lectureTitle)
        det.put("metadataText", item.detailedNotes.metadataText)
        det.put("section1Title", item.detailedNotes.section1Title)
        det.put("section1Content", item.detailedNotes.section1Content)
        det.put("section2Title", item.detailedNotes.section2Title)
        det.put("section2Bullets", JSONArray(item.detailedNotes.section2Bullets))
        det.put("calloutTitle", item.detailedNotes.calloutTitle)
        det.put("calloutContent", item.detailedNotes.calloutContent)
        det.put("section3Title", item.detailedNotes.section3Title)
        det.put("section3Content1", item.detailedNotes.section3Content1)
        det.put("section3Content2", item.detailedNotes.section3Content2)
        det.put("footerText", item.detailedNotes.footerText)
        det.put("isBookmarked", item.detailedNotes.isBookmarked)
        json.put("detailedNotes", det)

        return json
    }

    private fun deserializeLecture(json: JSONObject): SavedLectureItem? {
        val id = json.optString("id")
        if (id.isBlank()) return null

        val title = json.optString("title", "Lecture Notes")
        val subject = json.optString("subject", "General Studies")
        val dateText = json.optString("dateText", "Today")
        val durationText = json.optString("durationText", "40 min")
        val transcript = json.optString("transcriptText", "")

        val genJson = json.optJSONObject("generatedNotes")
        val takeawaysList = mutableListOf<String>()
        val termsList = mutableListOf<String>()
        val qasList = mutableListOf<DetectedQuestionAnswerItem>()

        if (genJson != null) {
            val takeawaysArr = genJson.optJSONArray("keyTakeaways")
            if (takeawaysArr != null) {
                for (i in 0 until takeawaysArr.length()) takeawaysList.add(takeawaysArr.optString(i))
            }
            val termsArr = genJson.optJSONArray("importantTerms")
            if (termsArr != null) {
                for (i in 0 until termsArr.length()) termsList.add(termsArr.optString(i))
            }
            val qasArr = genJson.optJSONArray("qas")
            if (qasArr != null) {
                for (i in 0 until qasArr.length()) {
                    val qObj = qasArr.optJSONObject(i) ?: continue
                    qasList.add(
                        DetectedQuestionAnswerItem(
                            question = qObj.optString("question"),
                            answer = qObj.optString("answer"),
                            explanation = qObj.optString("explanation"),
                            status = qObj.optString("status"),
                            isAnswered = qObj.optBoolean("isAnswered", true)
                        )
                    )
                }
            }
        }

        val genNotes = GeneratedNotesUiState(
            subjectBadge = genJson?.optString("subjectBadge", subject) ?: subject,
            lectureTitle = genJson?.optString("lectureTitle", title) ?: title,
            metadataText = genJson?.optString("metadataText", "Lecture Notes · $durationText") ?: "Lecture Notes · $durationText",
            aiSummaryText = genJson?.optString("aiSummaryText", "") ?: "",
            keyTakeaways = takeawaysList,
            importantTerms = termsList,
            detectedQuestionsAndAnswers = qasList,
            mathQuestion = genJson?.optString("mathQuestion", "") ?: "",
            mathAnswer = genJson?.optString("mathAnswer", "") ?: "",
            mathExplanation = genJson?.optString("mathExplanation", "") ?: "",
            hasMathSolution = genJson?.optBoolean("hasMathSolution", false) ?: false,
            isSaved = true,
            isLoading = false
        )

        val detJson = json.optJSONObject("detailedNotes")
        val bulletsList = mutableListOf<String>()
        if (detJson != null) {
            val bArr = detJson.optJSONArray("section2Bullets")
            if (bArr != null) {
                for (i in 0 until bArr.length()) bulletsList.add(bArr.optString(i))
            }
        }

        val detNotes = DetailedNotesUiState(
            courseCode = detJson?.optString("courseCode", "CS301") ?: "CS301",
            tagText = detJson?.optString("tagText", "Lecture Notes") ?: "Lecture Notes",
            lectureTitle = detJson?.optString("lectureTitle", title) ?: title,
            metadataText = detJson?.optString("metadataText", "Extracted from lecture · $durationText") ?: "Extracted from lecture · $durationText",
            section1Title = detJson?.optString("section1Title", "01 Overview") ?: "01 Overview",
            section1Content = detJson?.optString("section1Content", "") ?: "",
            section2Title = detJson?.optString("section2Title", "02 Key Concepts & Takeaways") ?: "02 Key Concepts & Takeaways",
            section2Bullets = bulletsList,
            calloutTitle = detJson?.optString("calloutTitle", "Key Principle") ?: "Key Principle",
            calloutContent = detJson?.optString("calloutContent", "") ?: "",
            section3Title = detJson?.optString("section3Title", "03 Discussion") ?: "03 Discussion",
            section3Content1 = detJson?.optString("section3Content1", "") ?: "",
            section3Content2 = detJson?.optString("section3Content2", "") ?: "",
            footerText = detJson?.optString("footerText", "Study Notes") ?: "Study Notes",
            isBookmarked = detJson?.optBoolean("isBookmarked", false) ?: false,
            isLoading = false
        )

        return SavedLectureItem(
            id = id,
            title = title,
            subject = subject,
            dateText = dateText,
            durationText = durationText,
            transcriptText = transcript,
            generatedNotes = genNotes,
            detailedNotes = detNotes
        )
    }
}

