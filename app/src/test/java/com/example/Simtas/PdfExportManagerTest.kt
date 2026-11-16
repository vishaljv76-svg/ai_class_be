package com.example.Simtas

import android.graphics.Paint
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.home.RecentNoteIconType
import com.example.Simtas.ui.mynotes.NoteListItem
import com.example.Simtas.utils.PdfExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PdfExportManager] data formatting, mapping, and sanitation.
 */
class PdfExportManagerTest {

    @Test
    fun testSanitizeFileName_specialCharactersAndWhitespace() {
        val raw1 = "Database Normalization (1NF, 2NF & 3NF)!"
        val clean1 = PdfExportManager.sanitizeFileName(raw1)
        assertEquals("Database_Normalization_1NF_2NF_3NF", clean1)

        val raw2 = "   ---???***///   "
        val clean2 = PdfExportManager.sanitizeFileName(raw2)
        assertEquals("Note", clean2)

        val raw3 = ""
        val clean3 = PdfExportManager.sanitizeFileName(raw3)
        assertEquals("Note", clean3)
    }

    @Test
    fun testCreatePdfDataFromDetailedNotes_normalLecture() {
        val state = DetailedNotesUiState(
            courseCode = "CS301",
            tagText = "AI notes",
            lectureTitle = "Database Normalization",
            metadataText = "Prof. Sarah Chen · 21 Oct 2026",
            section1Title = "01 Why normalize?",
            section1Content = "Normalization organizes relations to minimize repeated data.",
            section2Title = "02 The first three normal forms",
            section2Bullets = listOf(
                "1NF — Atomic values in each cell.",
                "2NF — No partial dependency.",
                "3NF — No transitive dependency."
            ),
            calloutTitle = "Functional dependency: X → Y",
            calloutContent = "Each value of X determines exactly one value of Y.",
            section3Title = "03 Worked example",
            section3Content1 = "Decompose student enrollment table into 2NF relations.",
            footerText = "Study Notes · Database Systems"
        )

        val data = PdfExportManager.createPdfDataFromDetailedNotes(state)

        assertEquals("Database Normalization", data.title)
        assertEquals("CS301", data.subject)
        assertEquals("AI notes", data.tag)
        assertEquals("01 Why normalize?", data.section1Title)
        assertEquals("Normalization organizes relations to minimize repeated data.", data.section1Content)
        assertEquals(3, data.section2Bullets.size)
        assertEquals("Functional dependency: X → Y", data.calloutTitle)
        assertEquals("Decompose student enrollment table into 2NF relations.", data.section3Content1)
    }

    @Test
    fun testCreatePdfDataFromDetailedNotes_aiQuestionAnswerNote() {
        val state = DetailedNotesUiState(
            courseCode = "Computer Science",
            tagText = "AI Q&A",
            lectureTitle = "What is a B-Tree and how is it balanced?",
            metadataText = "AI Study Copilot · 21 Oct 2026",
            section1Title = "01 Question Asked",
            section1Content = "What is a B-Tree and how is it balanced?",
            section2Title = "02 AI Answer",
            section2Bullets = listOf("A B-Tree is a self-balancing search tree used in databases and file systems."),
            calloutTitle = "Conversation Reference",
            calloutContent = "Topic: Data Structures · Saved to My Notes",
            section3Title = "03 Complete AI Explanation",
            section3Content1 = "A B-Tree maintains sorted data and allows searches, sequential access, insertions, and deletions in logarithmic time.",
            footerText = "AI Study Copilot"
        )

        val data = PdfExportManager.createPdfDataFromDetailedNotes(state)

        assertEquals("What is a B-Tree and how is it balanced?", data.title)
        assertEquals("AI Q&A", data.tag)
        assertEquals("What is a B-Tree and how is it balanced?", data.question)
        assertNotNull(data.answer)
        assertTrue(data.answer!!.contains("A B-Tree maintains sorted data"))
        assertEquals("Conversation Reference", data.calloutTitle)
    }

    @Test
    fun testCreatePdfDataFromNoteItem_standalone() {
        val note = NoteListItem(
            id = "note_101",
            title = "Eigenvalues & Eigenvectors",
            subject = "Mathematics",
            dateText = "20 Oct",
            durationText = "36 min",
            readTimeText = "5 min read",
            iconType = RecentNoteIconType.Math,
            summary = "Properties of linear transformations and characteristic polynomials.",
            tag = "AI notes"
        )

        val data = PdfExportManager.createPdfDataFromNoteItem(note)

        assertEquals("Eigenvalues & Eigenvectors", data.title)
        assertEquals("Mathematics", data.subject)
        assertTrue(data.dateText.contains("20 Oct"))
        assertEquals("Properties of linear transformations and characteristic polynomials.", data.section1Content)
        assertEquals(1, data.section2Bullets.size)
    }

    @Test
    fun testEmptyNoteGracefulHandling() {
        val emptyState = DetailedNotesUiState(
            courseCode = "",
            tagText = "",
            lectureTitle = "",
            metadataText = "",
            section1Title = "",
            section1Content = "",
            section2Title = "",
            section2Bullets = emptyList(),
            calloutTitle = "",
            calloutContent = "",
            section3Title = "",
            section3Content1 = "",
            footerText = ""
        )

        val data = PdfExportManager.createPdfDataFromDetailedNotes(emptyState)

        assertEquals("Lecture Notes", data.title)
        assertEquals("Computer Science", data.subject)
        assertNotNull(data.dateText)
        assertTrue(data.dateText.isNotBlank())
    }
}
