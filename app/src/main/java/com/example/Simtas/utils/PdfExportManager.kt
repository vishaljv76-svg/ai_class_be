package com.example.Simtas.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.mynotes.NoteListItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model representing formatted note content for PDF generation.
 */
data class NotePdfData(
    val title: String,
    val subject: String,
    val dateText: String,
    val tag: String = "AI notes",
    val section1Title: String = "01 Overview",
    val section1Content: String = "",
    val section2Title: String = "02 Key Concepts",
    val section2Bullets: List<String> = emptyList(),
    val calloutTitle: String? = null,
    val calloutContent: String? = null,
    val section3Title: String? = "03 Detailed Explanation",
    val section3Content1: String? = null,
    val section3Content2: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val footerText: String? = null
)

/**
 * Safe, scoped-storage compliant PDF export engine for SmartNotes AI.
 *
 * - Uses native Android SDK [PdfDocument]
 * - Renders multi-page documents with automatic page pagination
 * - Formats headings, bullet points, callout boxes, and code blocks
 * - Saves into device Downloads (MediaStore.Downloads on Android 10+ / API 29+)
 * - Provides seamless Open and Share intents via [FileProvider]
 */
object PdfExportManager {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2) // 511f
    private const val MAX_Y = 780f

    /**
     * Converts a [DetailedNotesUiState] into a [NotePdfData] model.
     */
    fun createPdfDataFromDetailedNotes(state: DetailedNotesUiState): NotePdfData {
        val isQa = state.tagText.contains("Q&A", ignoreCase = true) ||
                   state.section1Title.contains("Question", ignoreCase = true)
        return NotePdfData(
            title = state.lectureTitle.ifBlank { "Lecture Notes" },
            subject = state.courseCode.ifBlank { "Computer Science" },
            dateText = state.metadataText.ifBlank {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            },
            tag = state.tagText.ifBlank { if (isQa) "AI Q&A" else "AI notes" },
            section1Title = state.section1Title.ifBlank { if (isQa) "Question Asked" else "Overview" },
            section1Content = state.section1Content,
            section2Title = state.section2Title.ifBlank { if (isQa) "AI Answer" else "Key Concepts" },
            section2Bullets = state.section2Bullets,
            calloutTitle = state.calloutTitle.takeIf { !it.isNullOrBlank() },
            calloutContent = state.calloutContent.takeIf { !it.isNullOrBlank() },
            section3Title = state.section3Title.takeIf { !it.isNullOrBlank() },
            section3Content1 = state.section3Content1.takeIf { !it.isNullOrBlank() },
            section3Content2 = state.section3Content2.takeIf { !it.isNullOrBlank() },
            question = if (isQa) state.section1Content else null,
            answer = if (isQa) (state.section3Content1 ?: state.section2Bullets.firstOrNull()) else null,
            footerText = state.footerText.ifBlank { "SmartNotes AI · Study Notes" }
        )
    }

    /**
     * Converts a [NoteListItem] (with optional detailed state) into a [NotePdfData] model.
     */
    fun createPdfDataFromNoteItem(
        note: NoteListItem,
        detailedState: DetailedNotesUiState? = null
    ): NotePdfData {
        if (detailedState != null) {
            return createPdfDataFromDetailedNotes(detailedState)
        }

        val isQa = note.tag.contains("Q&A", ignoreCase = true)
        val fallbackSummary = note.summary.ifBlank { "Class notes for ${note.title}" }

        return NotePdfData(
            title = note.title.ifBlank { "Untitled Note" },
            subject = note.subject.ifBlank { "Computer Science" },
            dateText = "${note.dateText} · ${note.readTimeText}",
            tag = note.tag.ifBlank { if (isQa) "AI Q&A" else "AI notes" },
            section1Title = if (isQa) "Question Asked" else "01 Overview",
            section1Content = if (isQa) note.title else fallbackSummary,
            section2Title = if (isQa) "AI Answer" else "02 Key Takeaways",
            section2Bullets = if (isQa) listOf(fallbackSummary) else listOf(fallbackSummary),
            calloutTitle = if (isQa) "AI Study Copilot" else "Key Principle",
            calloutContent = fallbackSummary,
            section3Title = if (isQa) "Complete Explanation" else "03 Summary",
            section3Content1 = fallbackSummary,
            question = if (isQa) note.title else null,
            answer = if (isQa) fallbackSummary else null,
            footerText = "SmartNotes AI · ${note.subject} · ${note.dateText}"
        )
    }

    /**
     * Generates a multi-page PDF document and saves it into the device Downloads folder.
     *
     * @return Result containing [ExportResult] on success, or an exception on failure.
     */
    fun exportNoteToPdf(
        context: Context,
        data: NotePdfData
    ): Result<ExportResult> {
        val pdfDocument = PdfDocument()
        var currentPageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create())
        var canvas = page.canvas

        val brandColor = AndroidColor.rgb(108, 92, 231) // PurplePrimary
        val brandDark = AndroidColor.rgb(30, 27, 46)
        val textMuted = AndroidColor.rgb(120, 115, 145)
        val calloutBgColor = AndroidColor.rgb(241, 237, 254)
        val headerBandColor = AndroidColor.rgb(245, 243, 255)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Helper lambda to finish current page and start a new one
        fun newPage(): Pair<PdfDocument.Page, android.graphics.Canvas> {
            // Draw footer on the page being closed
            paint.color = textMuted
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val footerMsg = data.footerText ?: "SmartNotes AI · Study Smarter • Learn Better"
            canvas.drawText(footerMsg, MARGIN_X, 810f, paint)
            val pageNumText = "Page $currentPageNumber"
            val pageNumWidth = paint.measureText(pageNumText)
            canvas.drawText(pageNumText, PAGE_WIDTH - MARGIN_X - pageNumWidth, 810f, paint)

            pdfDocument.finishPage(page)

            currentPageNumber++
            val nextPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create())
            val nextCanvas = nextPage.canvas

            // Mini top banner on subsequent pages
            paint.color = headerBandColor
            nextCanvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 42f, paint)

            paint.color = brandColor
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            nextCanvas.drawText("SmartNotes AI", MARGIN_X, 26f, paint)

            paint.color = textMuted
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val titleSnippet = if (data.title.length > 40) data.title.take(37) + "..." else data.title
            nextCanvas.drawText("•   $titleSnippet", MARGIN_X + 80f, 26f, paint)

            page = nextPage
            canvas = nextCanvas
            return Pair(nextPage, nextCanvas)
        }

        var yPos = 0f

        try {
            // --- Page 1 Header Banner ---
            paint.color = headerBandColor
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 136f, paint)

            // Brand name
            paint.color = brandColor
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SmartNotes AI", MARGIN_X, 42f, paint)

            // Tag badge (e.g. AI notes / AI Q&A)
            val tagText = data.tag.uppercase(Locale.getDefault())
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val tagWidth = paint.measureText(tagText)
            val badgeLeft = PAGE_WIDTH - MARGIN_X - tagWidth - 16f
            val badgeRect = RectF(badgeLeft, 26f, PAGE_WIDTH - MARGIN_X, 46f)
            paint.color = calloutBgColor
            canvas.drawRoundRect(badgeRect, 10f, 10f, paint)
            paint.color = brandColor
            canvas.drawText(tagText, badgeLeft + 8f, 40f, paint)

            // Note Title (wrapped)
            paint.color = brandDark
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val titleLines = wrapTextToLines(data.title, paint, CONTENT_WIDTH)
            var titleY = 72f
            for (line in titleLines.take(2)) {
                canvas.drawText(line, MARGIN_X, titleY, paint)
                titleY += 20f
            }

            // Subject & Date metadata
            paint.color = textMuted
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Subject: ${data.subject}   •   ${data.dateText}", MARGIN_X, titleY + 6f, paint)

            yPos = 162f

            // --- Helper to check page bounds ---
            fun checkPageOverflow(neededHeight: Float) {
                if (yPos + neededHeight > MAX_Y) {
                    newPage()
                    yPos = 65f
                }
            }

            // --- Section 1: Overview / Question Asked ---
            if (data.section1Content.isNotBlank()) {
                checkPageOverflow(40f)

                // Section header
                paint.color = brandColor
                paint.textSize = 13f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(data.section1Title, MARGIN_X, yPos, paint)
                yPos += 18f

                // Body text
                paint.color = brandDark
                paint.textSize = 10.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val lines = wrapTextToLines(data.section1Content, paint, CONTENT_WIDTH)
                for (line in lines) {
                    checkPageOverflow(16f)
                    canvas.drawText(line, MARGIN_X, yPos, paint)
                    yPos += 15f
                }
                yPos += 12f
            }

            // --- Section 2: Key Concepts / AI Answer ---
            val bullets = data.section2Bullets.filter { it.isNotBlank() }
            if (bullets.isNotEmpty()) {
                checkPageOverflow(36f)

                paint.color = brandColor
                paint.textSize = 13f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(data.section2Title, MARGIN_X, yPos, paint)
                yPos += 18f

                paint.textSize = 10.5f
                for (bullet in bullets) {
                    val bulletLines = wrapTextToLines(bullet, paint, CONTENT_WIDTH - 16f)
                    checkPageOverflow(bulletLines.size * 15f + 8f)

                    // Draw bullet circle
                    paint.color = brandColor
                    canvas.drawCircle(MARGIN_X + 4f, yPos - 3.5f, 2.5f, paint)

                    // Draw bullet text
                    paint.color = brandDark
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    for (line in bulletLines) {
                        canvas.drawText(line, MARGIN_X + 14f, yPos, paint)
                        yPos += 15f
                    }
                    yPos += 4f
                }
                yPos += 8f
            }

            // --- Callout Box ---
            if (!data.calloutContent.isNullOrBlank()) {
                paint.textSize = 10f
                val calloutLines = wrapTextToLines(data.calloutContent, paint, CONTENT_WIDTH - 24f)
                val calloutBoxHeight = (calloutLines.size * 14f) + 36f

                checkPageOverflow(calloutBoxHeight + 10f)

                // Background box
                val calloutRect = RectF(MARGIN_X, yPos, MARGIN_X + CONTENT_WIDTH, yPos + calloutBoxHeight)
                paint.color = calloutBgColor
                canvas.drawRoundRect(calloutRect, 10f, 10f, paint)

                // Left highlight bar
                paint.color = brandColor
                canvas.drawRoundRect(RectF(MARGIN_X, yPos, MARGIN_X + 4f, yPos + calloutBoxHeight), 2f, 2f, paint)

                var cY = yPos + 18f
                if (!data.calloutTitle.isNullOrBlank()) {
                    paint.color = brandColor
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(data.calloutTitle, MARGIN_X + 14f, cY, paint)
                    cY += 16f
                }

                paint.color = brandDark
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                for (line in calloutLines) {
                    canvas.drawText(line, MARGIN_X + 14f, cY, paint)
                    cY += 14f
                }

                yPos += calloutBoxHeight + 14f
            }

            // --- Section 3: Detailed Explanation / Complete Content ---
            val hasSec3 = !data.section3Content1.isNullOrBlank() || !data.section3Content2.isNullOrBlank()
            if (hasSec3) {
                checkPageOverflow(36f)

                val sec3Title = data.section3Title ?: "03 Detailed Explanation"
                paint.color = brandColor
                paint.textSize = 13f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(sec3Title, MARGIN_X, yPos, paint)
                yPos += 18f

                val fullTextList = listOfNotNull(data.section3Content1, data.section3Content2)
                for (content in fullTextList) {
                    paint.color = brandDark
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val lines = wrapTextToLines(content, paint, CONTENT_WIDTH)
                    for (line in lines) {
                        checkPageOverflow(16f)
                        canvas.drawText(line, MARGIN_X, yPos, paint)
                        yPos += 15f
                    }
                    yPos += 8f
                }
            }

            // Finish the last page
            paint.color = textMuted
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val footerMsg = data.footerText ?: "SmartNotes AI · Study Smarter • Learn Better"
            canvas.drawText(footerMsg, MARGIN_X, 810f, paint)
            val pageNumText = "Page $currentPageNumber"
            val pageNumWidth = paint.measureText(pageNumText)
            canvas.drawText(pageNumText, PAGE_WIDTH - MARGIN_X - pageNumWidth, 810f, paint)

            pdfDocument.finishPage(page)

            // Save the document to Downloads
            val cleanTitle = sanitizeFileName(data.title).take(28)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SmartNotes_${cleanTitle}_$timestamp.pdf"

            val savedUri = writePdfToDownloads(context, pdfDocument, fileName)
                ?: return Result.failure(Exception("Failed to write PDF to Downloads storage."))

            return Result.success(ExportResult(savedUri, fileName, currentPageNumber))
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            try {
                pdfDocument.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Writes the PDF document to Downloads folder using MediaStore (Android 10+) or standard storage.
     */
    private fun writePdfToDownloads(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): Uri? {
        // Approach A: Android 10+ (API 29+) MediaStore scoped storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartNotes_AI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return uri
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Approach B: Public Downloads directory fallback (for API < 29 or if MediaStore fails)
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(publicDownloads, "SmartNotes_AI")
            if (!targetDir.exists()) targetDir.mkdirs()

            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Approach C: App-specific external files directory fallback
        try {
            val appDownloads = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SmartNotes_AI")
            if (!appDownloads.exists()) appDownloads.mkdirs()

            val file = File(appDownloads, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Splits a text block into lines that fit within [maxWidth] when drawn with [paint].
     */
    fun wrapTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()

        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("")
                continue
            }

            val words = paragraph.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    // Handle edge case where a single word is wider than maxWidth
                    if (paint.measureText(word) > maxWidth) {
                        var subWord = word
                        while (paint.measureText(subWord) > maxWidth && subWord.length > 1) {
                            var cut = subWord.length - 1
                            while (cut > 0 && paint.measureText(subWord.substring(0, cut)) > maxWidth) {
                                cut--
                            }
                            if (cut == 0) cut = 1
                            lines.add(subWord.substring(0, cut))
                            subWord = subWord.substring(cut)
                        }
                        currentLine = StringBuilder(subWord)
                    } else {
                        currentLine = StringBuilder(word)
                    }
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }

        return lines
    }

    /**
     * Cleans a string to be safely used as part of a file name.
     */
    fun sanitizeFileName(title: String): String {
        val clean = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
        return clean.ifBlank { "Note" }
    }

    /**
     * Opens a PDF file using an external PDF viewer intent.
     */
    fun openPdf(context: Context, uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer application found on device.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Shares a PDF file using Android's system share sheet.
     */
    fun sharePdf(context: Context, uri: Uri, title: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Exported notes: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Note PDF via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share menu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

/**
 * Result data class returned when a PDF is exported successfully.
 */
data class ExportResult(
    val uri: Uri,
    val fileName: String,
    val pageCount: Int
)
