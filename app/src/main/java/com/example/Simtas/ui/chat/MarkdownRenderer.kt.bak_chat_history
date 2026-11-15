package com.example.Simtas.ui.chat

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.OuterRingBorder
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.ThemeManager

// --- Sealed Class for Structured Markdown Elements ---

sealed class MarkdownElement {
    data class Heading(val level: Int, val text: String) : MarkdownElement()
    data class Paragraph(val text: String) : MarkdownElement()
    data class BulletList(val items: List<String>) : MarkdownElement()
    data class NumberedList(val items: List<String>, val startIndex: Int = 1) : MarkdownElement()
    data class CodeBlock(val language: String, val code: String) : MarkdownElement()
    data class DiagramBlock(val type: String, val code: String, val explanation: String? = null) : MarkdownElement()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownElement()
    data class Blockquote(val text: String) : MarkdownElement()
}

/**
 * Main Markdown and Visual Diagram renderer for SmartNotes AI.
 * Parses Gemini responses and renders clean typography, code blocks with syntax highlighting,
 * responsive tables, and interactive educational Mermaid diagrams with explanations.
 */
@Composable
fun MarkdownViewer(
    content: String,
    modifier: Modifier = Modifier,
    onCopyText: ((String) -> Unit)? = null
) {
    val elements = remember(content) { parseMarkdownContent(content) }
    val isDark = ThemeManager.isDark()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        elements.forEach { element ->
            when (element) {
                is MarkdownElement.Heading -> RenderHeading(element, isDark)
                is MarkdownElement.Paragraph -> RenderParagraph(element, isDark)
                is MarkdownElement.BulletList -> RenderBulletList(element, isDark)
                is MarkdownElement.NumberedList -> RenderNumberedList(element, isDark)
                is MarkdownElement.CodeBlock -> RenderCodeBlock(element, isDark, onCopyText)
                is MarkdownElement.DiagramBlock -> RenderDiagram(element, isDark)
                is MarkdownElement.TableBlock -> RenderTable(element, isDark)
                is MarkdownElement.Blockquote -> RenderBlockquote(element, isDark)
            }
        }
    }
}

// --- Render Heading ---

@Composable
private fun RenderHeading(heading: MarkdownElement.Heading, isDark: Boolean) {
    val fontSize = when (heading.level) {
        1 -> 20.sp
        2 -> 17.sp
        3 -> 15.sp
        else -> 14.sp
    }
    val fontWeight = when (heading.level) {
        1 -> FontWeight.Bold
        2 -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }
    val textColor = when (heading.level) {
        1, 2 -> if (isDark) Color(0xFFEDE9FE) else PurplePrimary
        else -> TextDarkPrimary
    }

    Text(
        text = buildInlineAnnotatedString(heading.text, isDark),
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = textColor,
        lineHeight = (fontSize.value + 6).sp,
        modifier = Modifier.padding(top = if (heading.level <= 2) 4.dp else 2.dp)
    )
}

// --- Render Paragraph ---

@Composable
private fun RenderParagraph(paragraph: MarkdownElement.Paragraph, isDark: Boolean) {
    SelectionContainer {
        Text(
            text = buildInlineAnnotatedString(paragraph.text, isDark),
            fontSize = 14.sp,
            color = TextDarkPrimary,
            lineHeight = 22.sp
        )
    }
}

// --- Render Bullet List ---

@Composable
private fun RenderBulletList(list: MarkdownElement.BulletList, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        list.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 10.dp, start = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildInlineAnnotatedString(item, isDark),
                        fontSize = 14.sp,
                        color = TextDarkPrimary,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

// --- Render Numbered List ---

@Composable
private fun RenderNumberedList(list: MarkdownElement.NumberedList, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        list.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${list.startIndex + index}.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    modifier = Modifier
                        .padding(top = 1.dp, end = 8.dp, start = 2.dp)
                        .width(22.dp)
                )
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildInlineAnnotatedString(item, isDark),
                        fontSize = 14.sp,
                        color = TextDarkPrimary,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

// --- Render Code Block with Syntax Highlighting & Copy Button ---

@Composable
private fun RenderCodeBlock(
    codeBlock: MarkdownElement.CodeBlock,
    isDark: Boolean,
    onCopyText: ((String) -> Unit)?
) {
    val context = LocalContext.current
    val langLabel = if (codeBlock.language.isNotBlank()) codeBlock.language.uppercase() else "CODE"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF313244), RoundedCornerShape(12.dp))
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181825))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = langLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA6ADC8),
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (onCopyText != null) {
                                onCopyText(codeBlock.code)
                            } else {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(langLabel, codeBlock.code))
                                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCopyIcon(Color(0xFFCDD6F4))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy Code",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCDD6F4)
                    )
                }
            }

            // Syntax Highlighted Monospace Code Area with Horizontal Scroll
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = buildSyntaxHighlightedCode(codeBlock.code, codeBlock.language),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

// --- Render Table Block ---

@Composable
private fun RenderTable(table: MarkdownElement.TableBlock, isDark: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OuterRingBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(if (isDark) Color(0xFF261F42) else Color(0xFFEDE9FE))
                    .padding(vertical = 8.dp)
            ) {
                table.headers.forEach { header ->
                    Text(
                        text = header.trim(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFEDE9FE) else PurplePrimary,
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 10.dp)
                    )
                }
            }

            HorizontalDivider(color = OuterRingBorder, thickness = 1.dp)

            // Data Rows
            table.rows.forEachIndexed { rowIndex, row ->
                val rowBg = if (rowIndex % 2 == 1) {
                    if (isDark) Color(0xFF1E1A30).copy(alpha = 0.5f) else Color(0xFFFAF8FF)
                } else Color.Transparent

                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .padding(vertical = 8.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = buildInlineAnnotatedString(cell.trim(), isDark),
                            fontSize = 12.sp,
                            color = TextDarkPrimary,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .width(130.dp)
                                .padding(horizontal = 10.dp)
                        )
                    }
                }
                if (rowIndex < table.rows.size - 1) {
                    HorizontalDivider(color = OuterRingBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// --- Render Blockquote ---

@Composable
private fun RenderBlockquote(blockquote: MarkdownElement.Blockquote, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF241E38) else Color(0xFFF6F3FF))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PurplePrimary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildInlineAnnotatedString(blockquote.text, isDark),
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = TextDarkPrimary,
            lineHeight = 19.sp
        )
    }
}

// --- Render Visual Mermaid Diagram ---

@Composable
fun RenderDiagram(diagram: MarkdownElement.DiagramBlock, isDark: Boolean) {
    var isWebViewLoading by remember { mutableStateOf(true) }
    var hasWebViewError by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OuterRingBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Diagram Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF241E38) else Color(0xFFF3EFFF))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawSparkleStarIcon(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Visual Diagram (${diagram.type.uppercase()})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }

                Text(
                    text = "Pinch to Zoom · Pan",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            // Visual Diagram Container (WebView Mermaid or Native Vector Fallback)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 380.dp)
                    .background(if (isDark) Color(0xFF13111C) else Color(0xFFFAFAFE)),
                contentAlignment = Alignment.Center
            ) {
                if (hasWebViewError) {
                    // Native Compose Vector Diagram Fallback
                    NativeVectorDiagramFallback(diagram.code, isDark)
                } else {
                    MermaidWebView(
                        mermaidCode = diagram.code,
                        isDark = isDark,
                        onLoadingChanged = { isWebViewLoading = it },
                        onError = { hasWebViewError = true }
                    )

                    if (isWebViewLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PurplePrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Diagram Educational Explanation Callout
            if (!diagram.explanation.isNullOrBlank()) {
                HorizontalDivider(color = OuterRingBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawLightbulbIcon(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = diagram.explanation,
                        fontSize = 12.sp,
                        color = TextDarkPrimary,
                        lineHeight = 17.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

// --- Mermaid WebView Component ---

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MermaidWebView(
    mermaidCode: String,
    isDark: Boolean,
    onLoadingChanged: (Boolean) -> Unit,
    onError: () -> Unit
) {
    val cleanCode = remember(mermaidCode) {
        mermaidCode.replace("\\", "\\\\").replace("`", "\\`")
    }

    val htmlContent = remember(cleanCode, isDark) {
        val themeStr = if (isDark) "dark" else "default"
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=4.0, user-scalable=yes">
            <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 12px;
                    background: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                    box-sizing: border-box;
                    font-family: -apple-system, Roboto, sans-serif;
                }
                .mermaid {
                    width: 100%;
                    overflow-x: auto;
                    display: flex;
                    justify-content: center;
                }
                svg {
                    max-width: 100% !important;
                    height: auto !important;
                }
            </style>
        </head>
        <body>
            <div class="mermaid">
                $cleanCode
            </div>
            <script>
                try {
                    mermaid.initialize({
                        startOnLoad: true,
                        theme: '$themeStr',
                        securityLevel: 'loose',
                        flowchart: { useMaxWidth: true, htmlLabels: true }
                    });
                } catch(e) {
                    console.error("Mermaid init error:", e);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(0) // Transparent
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChanged(false)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        onError()
                    }
                }
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 380.dp)
    )
}

// --- Native Compose Vector Diagram Fallback (100% Offline) ---

@Composable
private fun NativeVectorDiagramFallback(mermaidCode: String, isDark: Boolean) {
    val nodes = remember(mermaidCode) { parseMermaidNodes(mermaidCode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        nodes.forEachIndexed { index, node ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF261F42) else Color(0xFFEDE9FE))
                    .border(1.dp, PurplePrimary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = node,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFEDE9FE) else PurplePrimary
                )
            }

            if (index < nodes.size - 1) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawDownArrow(PurplePrimary)
                }
            }
        }
    }
}

private fun parseMermaidNodes(mermaidCode: String): List<String> {
    val results = mutableListOf<String>()
    val lines = mermaidCode.lines()
    val pattern = Regex("\\[[\"']?(.*?)[\"']?\\]|\\([\"']?(.*?)[\"']?\\)|\\{[\"']?(.*?)[\"']?\\}")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("graph") || trimmed.startsWith("flowchart") || trimmed.startsWith("%%")) continue

        val matches = pattern.findAll(trimmed)
        for (m in matches) {
            val content = m.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: m.value
            val cleaned = content.replace("\"", "").replace("'", "").trim()
            if (cleaned.isNotBlank() && !results.contains(cleaned)) {
                results.add(cleaned)
            }
        }
    }
    return if (results.isNotEmpty()) results else listOf("Process Start", "Analysis & Execution", "Output Generated")
}

// --- Inline Markdown Parser (Bold, Italic, Code, Math) ---

fun buildInlineAnnotatedString(text: String, isDark: Boolean): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Bold Italic: ***text***
            if (i + 5 < len && text.startsWith("***", i)) {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 3
                    continue
                }
            }

            // Bold: **text** or __text__
            if (i + 3 < len && (text.startsWith("**", i) || text.startsWith("__", i))) {
                val delim = text.substring(i, i + 2)
                val end = text.indexOf(delim, i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Inline Code: `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val codeContent = text.substring(i + 1, end)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = if (isDark) Color(0xFF2E2648) else Color(0xFFEDE9FE),
                            color = PurplePrimary
                        )
                    ) {
                        append(" $codeContent ")
                    }
                    i = end + 1
                    continue
                }
            }

            // Italic: *text* or _text_
            if ((text[i] == '*' || text[i] == '_') && i + 1 < len && !Character.isWhitespace(text[i + 1])) {
                val delim = text[i]
                val end = text.indexOf(delim, i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            // Inline Math: $math$
            if (text[i] == '$' && i + 1 < len && text[i + 1] != '$') {
                val end = text.indexOf('$', i + 1)
                if (end != -1 && end > i + 1) {
                    val mathContent = text.substring(i + 1, end)
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            color = PurplePrimary
                        )
                    ) {
                        append(mathContent)
                    }
                    i = end + 1
                    continue
                }
            }

            // Plain text
            append(text[i])
            i++
        }
    }
}

// --- Syntax Highlighting Helper ---

private fun buildSyntaxHighlightedCode(code: String, language: String): AnnotatedString {
    val keywords = setOf(
        "def", "return", "if", "elif", "else", "while", "for", "in", "import", "from", "as",
        "class", "try", "except", "finally", "with", "lambda", "yield", "async", "await",
        "val", "var", "fun", "null", "true", "false", "this", "super", "package", "public",
        "private", "protected", "override", "data", "sealed", "SELECT", "FROM", "WHERE",
        "INSERT", "UPDATE", "DELETE", "JOIN", "GROUP", "BY", "ORDER", "TABLE", "CREATE"
    )

    return buildAnnotatedString {
        val lines = code.lines()
        lines.forEachIndexed { lineIdx, line ->
            var col = 0
            val len = line.length

            while (col < len) {
                // Comments (#, //, --)
                if ((line.startsWith("#", col) && language.lowercase() == "python") ||
                    line.startsWith("//", col) || line.startsWith("--", col)) {
                    withStyle(SpanStyle(color = Color(0xFF6C7086), fontStyle = FontStyle.Italic)) {
                        append(line.substring(col))
                    }
                    break
                }

                // Strings ("..." or '...')
                if (line[col] == '"' || line[col] == '\'') {
                    val quote = line[col]
                    val endQuote = line.indexOf(quote, col + 1)
                    if (endQuote != -1) {
                        withStyle(SpanStyle(color = Color(0xFFA6E3A1))) {
                            append(line.substring(col, endQuote + 1))
                        }
                        col = endQuote + 1
                        continue
                    }
                }

                // Identifiers / Words
                if (Character.isJavaIdentifierStart(line[col])) {
                    val start = col
                    while (col < len && Character.isJavaIdentifierPart(line[col])) {
                        col++
                    }
                    val word = line.substring(start, col)
                    if (keywords.contains(word) || keywords.contains(word.uppercase())) {
                        withStyle(SpanStyle(color = Color(0xFFCBA6F7), fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = Color(0xFFCDD6F4))) {
                            append(word)
                        }
                    }
                    continue
                }

                // Numbers
                if (Character.isDigit(line[col])) {
                    val start = col
                    while (col < len && (Character.isDigit(line[col]) || line[col] == '.')) {
                        col++
                    }
                    withStyle(SpanStyle(color = Color(0xFFFAB387))) {
                        append(line.substring(start, col))
                    }
                    continue
                }

                // Other symbols / operators
                withStyle(SpanStyle(color = Color(0xFF89DCEB))) {
                    append(line[col])
                }
                col++
            }

            if (lineIdx < lines.size - 1) {
                append("\n")
            }
        }
    }
}

// --- Content Parser (Splits Raw Markdown into Elements) ---

fun parseMarkdownContent(rawContent: String): List<MarkdownElement> {
    val elements = mutableListOf<MarkdownElement>()
    val lines = rawContent.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trimEnd()

        // 1. Code Block or Mermaid Diagram
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim().lowercase()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trimStart().startsWith("```")) {
                i++ // Skip closing ```
            }

            val codeBody = codeLines.joinToString("\n")
            if (lang == "mermaid") {
                // Check if explanation follows on the next line
                var explanation: String? = null
                if (i < lines.size && lines[i].isNotBlank()) {
                    val nextLine = lines[i].trim()
                    if (nextLine.startsWith("*") || nextLine.startsWith("Explanation:") || nextLine.startsWith("Note:")) {
                        explanation = nextLine.removePrefix("*").removeSuffix("*").removePrefix("Explanation:").trim()
                        i++
                    }
                }
                elements.add(MarkdownElement.DiagramBlock(type = "mermaid", code = codeBody, explanation = explanation))
            } else {
                elements.add(MarkdownElement.CodeBlock(language = lang, code = codeBody))
            }
            continue
        }

        // 2. Table Block (| Col 1 | Col 2 |)
        if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                tableLines.add(lines[i].trim())
                i++
            }

            if (tableLines.size >= 2) {
                val headerLine = tableLines[0]
                val headers = headerLine.split("|").map { it.trim() }.filter { it.isNotBlank() }

                val rows = mutableListOf<List<String>>()
                for (tIdx in 1 until tableLines.size) {
                    val rLine = tableLines[tIdx]
                    // Skip delimiter line |---|---|
                    if (rLine.contains("---") || rLine.contains("-|-")) continue
                    val cells = rLine.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    if (cells.isNotEmpty()) {
                        rows.add(cells)
                    }
                }
                elements.add(MarkdownElement.TableBlock(headers = headers, rows = rows))
            }
            continue
        }

        // 3. Headings (#, ##, ###, ####)
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headingText = line.drop(level).trim()
            if (level in 1..4 && headingText.isNotBlank()) {
                elements.add(MarkdownElement.Heading(level = level, text = headingText))
                i++
                continue
            }
        }

        // 4. Bullet Lists (* , - , + )
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") || line.trimStart().startsWith("+ ")) {
            val items = mutableListOf<String>()
            while (i < lines.size && (lines[i].trimStart().startsWith("- ") || lines[i].trimStart().startsWith("* ") || lines[i].trimStart().startsWith("+ "))) {
                val itemText = lines[i].trimStart().drop(2).trim()
                items.add(itemText)
                i++
            }
            elements.add(MarkdownElement.BulletList(items))
            continue
        }

        // 5. Numbered Lists (1. , 2. )
        val numMatch = Regex("^\\s*(\\d+)\\.\\s+(.*)").find(line)
        if (numMatch != null) {
            val items = mutableListOf<String>()
            val startIdx = numMatch.groupValues[1].toIntOrNull() ?: 1
            while (i < lines.size) {
                val match = Regex("^\\s*(\\d+)\\.\\s+(.*)").find(lines[i]) ?: break
                items.add(match.groupValues[2].trim())
                i++
            }
            elements.add(MarkdownElement.NumberedList(items = items, startIndex = startIdx))
            continue
        }

        // 6. Blockquote (> ...)
        if (line.trimStart().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                quoteLines.add(lines[i].trimStart().removePrefix(">").trim())
                i++
            }
            elements.add(MarkdownElement.Blockquote(quoteLines.joinToString(" ")))
            continue
        }

        // 7. Regular Paragraph
        if (line.isNotBlank()) {
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() &&
                !lines[i].trimStart().startsWith("```") &&
                !lines[i].trimStart().startsWith("#") &&
                !lines[i].trimStart().startsWith("- ") &&
                !lines[i].trimStart().startsWith("* ") &&
                !lines[i].trimStart().startsWith(">") &&
                !Regex("^\\s*(\\d+)\\.\\s+").containsMatchIn(lines[i])) {
                paragraphLines.add(lines[i].trim())
                i++
            }
            elements.add(MarkdownElement.Paragraph(paragraphLines.joinToString(" ")))
            continue
        }

        i++
    }

    return elements
}

// --- Vector Canvas Icons Helpers ---

private fun DrawScope.drawCopyIcon(color: Color) {
    val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.6f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.12f, size.height * 0.32f),
        size = Size(size.width * 0.6f, size.height * 0.6f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
}

private fun DrawScope.drawSparkleStarIcon(color: Color) {
    val path = Path().apply {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = size.width / 2.2f
        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawLightbulbIcon(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.32f,
        center = Offset(size.width * 0.5f, size.height * 0.38f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.38f, size.height * 0.72f),
        end = Offset(size.width * 0.62f, size.height * 0.72f),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.42f, size.height * 0.85f),
        end = Offset(size.width * 0.58f, size.height * 0.85f),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDownArrow(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.1f),
        end = Offset(size.width * 0.5f, size.height * 0.85f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    val path = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.55f)
        lineTo(size.width * 0.5f, size.height * 0.85f)
        lineTo(size.width * 0.75f, size.height * 0.55f)
    }
    drawPath(path = path, color = color, style = stroke)
}

