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

    /**
     * Fetches real quiz questions for a quiz ID or associated lecture note.
     */
    suspend fun getQuiz(
        quizId: Int? = SessionManager.getLastQuizId(),
        noteId: Int? = SessionManager.getLastNoteId()
    ): QuizDetailResult = withContext(Dispatchers.IO) {
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
