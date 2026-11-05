package com.example.Simtas.data.repository

import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.home.DashboardUiState
import com.example.Simtas.ui.home.RecentNoteIconType
import com.example.Simtas.ui.home.RecentNoteItem
import org.json.JSONArray

object DashboardRepository {

    suspend fun getDashboardData(): DashboardUiState? {
        val userId = SessionManager.getUserId()
        val response = ApiClient.get("dashboard/dashboard_stats.php?user_id=$userId")

        if (response.isSuccess && response.jsonObject != null) {
            val json = response.jsonObject
            val name = json.optString("userName", SessionManager.getUserName())
            val initials = json.optString("userInitials", "AM")
            val date = json.optString("dateText", "Today")
            val totalNotes = json.optInt("totalNotes", 0)
            val subjectsCount = json.optInt("subjectsCount", 0)
            val quizzesCount = json.optInt("quizzesCount", 8)

            val recentArray = json.optJSONArray("recentNotes") ?: JSONArray()
            val recentList = mutableListOf<RecentNoteItem>()

            for (i in 0 until recentArray.length()) {
                val obj = recentArray.optJSONObject(i) ?: continue
                val iconStr = obj.optString("iconType", "Database")
                val iconType = if (iconStr.equals("Math", ignoreCase = true)) {
                    RecentNoteIconType.Math
                } else {
                    RecentNoteIconType.Database
                }

                recentList.add(
                    RecentNoteItem(
                        id = obj.optString("id", i.toString()),
                        title = obj.optString("title", "Lecture Notes"),
                        subject = obj.optString("subject", "Computer Science"),
                        date = obj.optString("date", "Today"),
                        duration = obj.optString("duration", "40 min"),
                        iconType = iconType
                    )
                )
            }

            val localRecent = com.example.Simtas.data.local.LectureNotesStorage.getAllRecentNotes()
            for (lr in localRecent) {
                if (recentList.none { it.id == lr.id || it.title.equals(lr.title, ignoreCase = true) }) {
                    recentList.add(0, lr)
                }
            }

            return DashboardUiState(
                userName = name,
                dateText = date,
                userInitials = initials,
                totalNotes = if (totalNotes > 0) totalNotes else recentList.size,
                subjectsCount = if (subjectsCount > 0) subjectsCount else recentList.map { it.subject }.distinct().size,
                quizzesCount = quizzesCount,
                recentNotes = recentList,
                isLoading = false
            )
        }

        val localRecent = com.example.Simtas.data.local.LectureNotesStorage.getAllRecentNotes()
        if (localRecent.isNotEmpty()) {
            return DashboardUiState(
                userName = SessionManager.getUserName(),
                dateText = "Today",
                userInitials = "AM",
                totalNotes = localRecent.size,
                subjectsCount = localRecent.map { it.subject }.distinct().size,
                quizzesCount = 0,
                recentNotes = localRecent,
                isLoading = false
            )
        }
        return null
    }
}

