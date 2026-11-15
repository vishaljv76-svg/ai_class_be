package com.example.Simtas.data.repository

import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.quizquestion.QuizOptionItem
import com.example.Simtas.ui.quizresults.AnswerReviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RealQuizQuestion(
    val id: Int,
    val questionNumber: Int,
    val questionText: String,
    val topic: String,
    val topicText: String,
    val instructionText: String,
    val modeText: String,
    val options: List<QuizOptionItem>,
    val correctOption: String,
    val explanation: String
)

data class QuizDetailResult(
    val isSuccess: Boolean,
    val quizId: Int? = null,
    val title: String = "",
    val subject: String = "",
    val totalQuestions: Int = 0,
    val difficulty: String = "Moderate",
    val questions: List<RealQuizQuestion> = emptyList(),
    val message: String? = null
)

data class QuizSubmissionResult(
    val isSuccess: Boolean,
    val scorePercentage: Int = 0,
    val correctCount: Int = 0,
    val totalQuestions: Int = 0,
    val feedbackHeader: String = "",
    val feedbackText: String = "",
    val reviewItems: List<AnswerReviewItem> = emptyList(),
    val message: String? = null
)

object QuizRepository {

    @Volatile
    var cachedActiveQuiz: QuizDetailResult? = null

    /**
     * Generates a new quiz derived strictly from a specific note or all saved My Notes.
     */
    suspend fun generateQuiz(
        noteId: Int? = null,
        noteKey: String? = null,
        numberOfQuestions: Int = 5,
        difficulty: String = "Moderate"
    ): QuizDetailResult = withContext(Dispatchers.IO) {
        val userId = SessionManager.getUserId()
        val payload = JSONObject().apply {
            put("userId", userId)
            if (noteId != null && noteId > 0) {
                put("note_id", noteId)
            } else {
                put("scope", "all")
            }
            put("numberOfQuestions", numberOfQuestions)
            put("difficulty", difficulty)
        }.toString()

        val response = ApiClient.post("quiz/generate_quiz.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val root = response.jsonObject
            if (root.optBoolean("success", false)) {
                val qId = root.optInt("quiz_id", 0)
                if (qId > 0) {
                    SessionManager.setLastQuizId(qId)
                }

                val title = root.optString("title", "AI Quiz")
                val subject = root.optString("subject", "My Study Notes")
                val totalQ = root.optInt("total_questions", numberOfQuestions)

                val questionsList = mutableListOf<RealQuizQuestion>()
                val qArray = root.optJSONArray("questions")
                if (qArray != null) {
                    for (i in 0 until qArray.length()) {
                        val q = qArray.optJSONObject(i) ?: continue
                        val optList = mutableListOf<QuizOptionItem>()
                        val opts = q.optJSONArray("options")
                        if (opts != null) {
                            for (j in 0 until opts.length()) {
                                val opt = opts.optJSONObject(j) ?: continue
                                optList.add(
                                    QuizOptionItem(
                                        optionKey = opt.optString("optionKey", "A"),
                                        text = opt.optString("text", "")
                                    )
                                )
                            }
                        }

                        questionsList.add(
                            RealQuizQuestion(
                                id = q.optInt("id", i + 1),
                                questionNumber = q.optInt("questionNumber", q.optInt("question_number", i + 1)),
                                questionText = q.optString("questionText", q.optString("question_text", "Question ${i + 1}")),
                                topic = q.optString("topic", subject),
                                topicText = q.optString("topicText", "$subject · $difficulty"),
                                instructionText = q.optString("instructionText", "Choose one answer."),
                                modeText = q.optString("modeText", "$difficulty · MCQ"),
                                options = optList,
                                correctOption = q.optString("correctOption", q.optString("correct_option", "A")),
                                explanation = q.optString("explanation", "")
                            )
                        )
                    }
                }

                val result = QuizDetailResult(
                    isSuccess = true,
                    quizId = qId,
                    title = title,
                    subject = subject,
                    totalQuestions = totalQ,
                    difficulty = difficulty,
                    questions = questionsList
                )
                cachedActiveQuiz = result
                return@withContext result
            }
        }

        // Offline / Network-fallback: Generate quiz directly from local saved My Notes
        val offlineCandidateNotes = if (!noteKey.isNullOrBlank()) {
            val single = com.example.Simtas.data.local.LectureNotesStorage.getSavedLecture(noteKey)
            if (single != null) listOf(single) else emptyList()
        } else if (noteId != null && noteId > 0) {
            val single = com.example.Simtas.data.local.LectureNotesStorage.getSavedLecture(noteId.toString())
            if (single != null) listOf(single) else emptyList()
        } else {
            val list = com.example.Simtas.data.local.LectureNotesStorage.getAllNotesForList(userId)
            list.mapNotNull { com.example.Simtas.data.local.LectureNotesStorage.getSavedLecture(it.id) }
        }

        if (offlineCandidateNotes.isNotEmpty()) {
            return@withContext generateOfflineQuizFromNotes(offlineCandidateNotes, numberOfQuestions, difficulty)
        }

        QuizDetailResult(
            isSuccess = false,
            message = response.errorMessage ?: "Failed to generate quiz"
        )
    }

    private fun generateOfflineQuizFromNotes(
        notes: List<com.example.Simtas.data.local.SavedLectureItem>,
        numberOfQuestions: Int,
        difficulty: String
    ): QuizDetailResult {
        if (notes.isEmpty()) {
            return QuizDetailResult(
                isSuccess = false,
                message = "No study notes found to generate quiz questions."
            )
        }

        val questionsList = mutableListOf<RealQuizQuestion>()
        var qCounter = 1

        for (item in notes) {
            if (questionsList.size >= numberOfQuestions) break

            val title = item.title
            val answer = item.detailedNotes.section2Bullets.firstOrNull()
                ?: item.detailedNotes.section1Content
                ?: item.generatedNotes.aiSummaryText
            val subject = item.subject.ifBlank { "Study Notes" }

            val questionText = if (title.endsWith("?")) {
                title
            } else {
                "According to your notes on \"$title\", what is the primary concept or function?"
            }

            val correctText = if (answer.length > 120) answer.take(117) + "..." else answer
            val distractor1 = "It is unrelated to $title and does not apply to this concept."
            val distractor2 = "It is solely used for relational database normalization."
            val distractor3 = "It has been deprecated and replaced by random search techniques."

            val options = listOf(
                QuizOptionItem("A", correctText),
                QuizOptionItem("B", distractor1),
                QuizOptionItem("C", distractor2),
                QuizOptionItem("D", distractor3)
            )

            questionsList.add(
                RealQuizQuestion(
                    id = qCounter,
                    questionNumber = qCounter,
                    questionText = questionText,
                    topic = subject,
                    topicText = "$subject · $difficulty",
                    instructionText = "Choose the most accurate answer based on your saved notes.",
                    modeText = "$difficulty · MCQ",
                    options = options,
                    correctOption = "A",
                    explanation = "Directly based on your saved note: $answer"
                )
            )
            qCounter++
        }

        val primaryNote = notes.first()
        val result = QuizDetailResult(
            isSuccess = true,
            quizId = 999000 + (1..999).random(),
            title = if (notes.size == 1) "${primaryNote.title} Quiz" else "My Notes Review Quiz",
            subject = primaryNote.subject.ifBlank { "My Study Notes" },
            totalQuestions = questionsList.size,
            difficulty = difficulty,
            questions = questionsList
        )
        cachedActiveQuiz = result
        return result
    }

    /**
     * Fetches real quiz questions for a quiz ID or associated lecture note.
     */
    suspend fun getQuiz(
        quizId: Int? = SessionManager.getLastQuizId(),
        noteId: Int? = SessionManager.getLastNoteId()
    ): QuizDetailResult = withContext(Dispatchers.IO) {
        val cached = cachedActiveQuiz
        if (cached != null && cached.questions.isNotEmpty() && (quizId == null || quizId <= 0 || cached.quizId == quizId)) {
            return@withContext cached
        }
        val queryParams = mutableListOf<String>()
        if (quizId != null && quizId > 0) queryParams.add("quiz_id=$quizId")
        if (noteId != null && noteId > 0) queryParams.add("note_id=$noteId")
        queryParams.add("userId=${SessionManager.getUserId()}")

        val endpoint = "quiz/get_quiz.php?" + queryParams.joinToString("&")
        val response = ApiClient.get(endpoint)

        if (response.isSuccess && response.jsonObject != null) {
            val root = response.jsonObject
            val quizObj = root.optJSONObject("quiz")
            val qId = quizObj?.optInt("id", quizId ?: 0) ?: quizId
            if (qId != null && qId > 0) {
                SessionManager.setLastQuizId(qId)
            }

            val title = quizObj?.optString("title", "Lecture Quiz") ?: "Lecture Quiz"
            val subject = quizObj?.optString("subject", "Computer Science") ?: "Computer Science"
            val diff = quizObj?.optString("difficulty", "Moderate") ?: "Moderate"
            val totalQ = quizObj?.optInt("totalQuestions", 5) ?: 5

            val questionsList = mutableListOf<RealQuizQuestion>()
            val qArray = root.optJSONArray("questions")
            if (qArray != null) {
                for (i in 0 until qArray.length()) {
                    val q = qArray.optJSONObject(i) ?: continue
                    val optList = mutableListOf<QuizOptionItem>()
                    val opts = q.optJSONArray("options")
                    if (opts != null) {
                        for (j in 0 until opts.length()) {
                            val opt = opts.optJSONObject(j) ?: continue
                            optList.add(
                                QuizOptionItem(
                                    optionKey = opt.optString("optionKey", "A"),
                                    text = opt.optString("text", "")
                                )
                            )
                        }
                    }

                    questionsList.add(
                        RealQuizQuestion(
                            id = q.optInt("id", i + 1),
                            questionNumber = q.optInt("questionNumber", i + 1),
                            questionText = q.optString("questionText", "Question ${i + 1}"),
                            topic = q.optString("topic", subject),
                            topicText = q.optString("topicText", "$subject · $diff"),
                            instructionText = q.optString("instructionText", "Choose one answer."),
                            modeText = q.optString("modeText", "$diff · MCQ"),
                            options = optList,
                            correctOption = q.optString("correctOption", "A"),
                            explanation = q.optString("explanation", "")
                        )
                    )
                }
            }

            QuizDetailResult(
                isSuccess = true,
                quizId = qId,
                title = title,
                subject = subject,
                totalQuestions = totalQ,
                difficulty = diff,
                questions = questionsList
            )
        } else {
            QuizDetailResult(
                isSuccess = false,
                message = response.errorMessage ?: "Failed to fetch quiz"
            )
        }
    }

    /**
     * Submits selected quiz answers and returns real score and feedback.
     */
    suspend fun submitQuiz(
        quizId: Int,
        answers: Map<Int, String>
    ): QuizSubmissionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("quiz_id", quizId)
            put("userId", SessionManager.getUserId())
            val answersObj = JSONObject()
            for ((qNum, chosen) in answers) {
                answersObj.put(qNum.toString(), chosen)
            }
            put("answers", answersObj)
        }.toString()

        val response = ApiClient.post("quiz/submit_quiz.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val score = json.optInt("score_percentage", 0)
            val correct = json.optInt("correct_count", 0)
            val total = json.optInt("total_questions", answers.size)
            val header = json.optString("feedback_header", "Quiz Completed")
            val feedback = json.optString("feedback_text", "You finished the quiz.")

            val reviewItems = mutableListOf<AnswerReviewItem>()
            val rArray = json.optJSONArray("review_items")
            if (rArray != null) {
                for (i in 0 until rArray.length()) {
                    val r = rArray.optJSONObject(i) ?: continue
                    reviewItems.add(
                        AnswerReviewItem(
                            questionNumber = r.optString("questionNumber", "Q${i + 1}"),
                            questionTopic = r.optString("questionTopic", "Topic"),
                            isCorrect = r.optBoolean("isCorrect", false),
                            userAnswerText = r.optString("userAnswerText").takeIf { it.isNotBlank() },
                            correctAnswerText = r.optString("correctAnswerText").takeIf { it.isNotBlank() },
                            explanationText = r.optString("explanationText", "")
                        )
                    )
                }
            }

            QuizSubmissionResult(
                isSuccess = true,
                scorePercentage = score,
                correctCount = correct,
                totalQuestions = total,
                feedbackHeader = header,
                feedbackText = feedback,
                reviewItems = reviewItems
            )
        } else {
            QuizSubmissionResult(
                isSuccess = false,
                message = response.errorMessage ?: "Failed to submit quiz"
            )
        }
    }
}

