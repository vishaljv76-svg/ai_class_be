package com.example.Simtas.ui.chat

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
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
 * responsive tables, clickable online resources, and interactive educational Mermaid diagrams with explanations.
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

// --- Helper Composable for Clickable & Formatted Inline Markdown Text ---

@Composable
fun InlineMarkdownText(
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    color: Color = TextDarkPrimary,
    lineHeight: TextUnit = 22.sp
) {
    val context = LocalContext.current
    val annotated = remember(text, isDark) { buildInlineAnnotatedString(text, isDark) }
    val hasUrl = remember(annotated) { annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty() }

    if (hasUrl) {
        ClickableText(
            text = annotated,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = color,
                lineHeight = lineHeight
            ),
            modifier = modifier,
            onClick = { offset ->
                annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { link ->
                    try {
                        val parsedUri = Uri.parse(link.item)
                        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        )
    } else {
        Text(
            text = annotated,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            color = color,
            lineHeight = lineHeight,
            modifier = modifier
        )
    }
}

// --- Render Paragraph ---

@Composable
private fun RenderParagraph(paragraph: MarkdownElement.Paragraph, isDark: Boolean) {
    SelectionContainer {
        InlineMarkdownText(
            text = paragraph.text,
            isDark = isDark,
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
                    InlineMarkdownText(
                        text = item,
                        isDark = isDark,
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
                    InlineMarkdownText(
                        text = item,
                        isDark = isDark,
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
                        InlineMarkdownText(
                            text = cell.trim(),
                            isDark = isDark,
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
        InlineMarkdownText(
            text = blockquote.text,
            isDark = isDark,
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

            HorizontalDivider(color = OuterRingBorder, thickness = 1.dp)

            // Diagram Content Body (WebView with on-device fallback)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp)
                    .background(if (isDark) Color(0xFF1E1E2E) else Color(0xFFFAFAFC)),
                contentAlignment = Alignment.Center
            ) {
                if (!hasWebViewError) {
                    MermaidWebView(
                        diagramCode = diagram.code,
                        isDark = isDark,
                        onLoadingChange = { loading -> isWebViewLoading = loading },
                        onError = { hasWebViewError = true }
                    )
                }

                if (isWebViewLoading && !hasWebViewError) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = PurplePrimary,
                        strokeWidth = 2.5.dp
                    )
                }

                if (hasWebViewError) {
                    // Clean native fallback card when WebView unavailable
                    NativeDiagramFallback(diagram.code, isDark)
                }
            }

            // Educational Diagram Companion Explanation
            if (!diagram.explanation.isNullOrBlank()) {
                HorizontalDivider(color = OuterRingBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF231C38) else Color(0xFFF9F7FD))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(PurplePrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionContainer {
                        InlineMarkdownText(
                            text = diagram.explanation,
                            isDark = isDark,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextDarkPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Mermaid HTML WebView Component ---

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MermaidWebView(
    diagramCode: String,
    isDark: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: () -> Unit
) {
    val theme = if (isDark) "dark" else "default"
    val bgColor = if (isDark) "#1E1E2E" else "#FAFAFC"
    val textColor = if (isDark) "#CDD6F4" else "#1E1B4B"

    val sanitizedCode = diagramCode.replace("`", "")
        .replace("\\", "\\\\")
        .replace("\$", "\\$")

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
            <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 16px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    background-color: $bgColor;
                    color: $textColor;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    overflow: auto;
                }
                .mermaid {
                    width: 100%;
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
                $sanitizedCode
            </div>
            <script>
                try {
                    mermaid.initialize({
                        startOnLoad: true,
                        theme: '$theme',
                        securityLevel: 'loose',
                        flowchart: { useMaxWidth: true, htmlLabels: true, curve: 'basis' }
                    });
                } catch(e) {
                    console.error(e);
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0)

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChange(false)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        onLoadingChange(false)
                        onError()
                    }
                }
                loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxWidth().height(260.dp)
    )
}

// --- Native Canvas & Box Fallback for Diagrams ---

@Composable
private fun NativeDiagramFallback(code: String, isDark: Boolean) {
    val steps = remember(code) { extractDiagramNodes(code) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEachIndexed { index, step ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF2C2448) else Color(0xFFEDE9FE))
                    .border(1.dp, PurplePrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = step,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFEDE9FE) else PurplePrimary
                )
            }

            if (index < steps.size - 1) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = PurplePrimary,
                        start = Offset(w / 2, 0f),
                        end = Offset(w / 2, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

// --- Extract node labels from Mermaid syntax ---

private fun extractDiagramNodes(code: String): List<String> {
    val results = mutableListOf<String>()
    val lines = code.lines()

    val pattern = Regex("""\[(?:\"([^\"]+)\"|([^\]]+))\]""")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("graph") || trimmed.startsWith("flowchart") ||
            trimmed.startsWith("subgraph") || trimmed.startsWith("style") || trimmed == "end") {
            continue
        }

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

// --- Inline Markdown Parser (Bold, Italic, Code, Math, Links) ---

fun buildInlineAnnotatedString(text: String, isDark: Boolean): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Markdown Link: [text](url)
            if (text[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen).trim()
                        val start = length
                        append(linkText)
                        val end = length
                        addStyle(
                            SpanStyle(
                                color = if (isDark) Color(0xFF89B4FA) else Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            ),
                            start,
                            end
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = url,
                            start = start,
                            end = end
                        )
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Bare URL: https://... or http://...
            if ((text.startsWith("https://", i) || text.startsWith("http://", i))) {
                var urlEnd = i
                while (urlEnd < len && !Character.isWhitespace(text[urlEnd]) && text[urlEnd] != ')' && text[urlEnd] != ']' && text[urlEnd] != '>') {
                    urlEnd++
                }
                val rawUrl = text.substring(i, urlEnd).trimEnd('.', ',', ';', ':')
                val start = length
                append(rawUrl)
                val end = length
                addStyle(
                    SpanStyle(
                        color = if (isDark) Color(0xFF89B4FA) else Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = rawUrl,
                    start = start,
                    end = end
                )
                i = i + rawUrl.length
                continue
            }

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

                // Identifiers / Keywords
                if (line[col].isLetter() || line[col] == '_') {
                    val wordStart = col
                    while (col < len && (line[col].isLetterOrDigit() || line[col] == '_')) {
                        col++
                    }
                    val word = line.substring(wordStart, col)
                    if (keywords.contains(word)) {
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
                if (line[col].isDigit()) {
                    val numStart = col
                    while (col < len && (line[col].isDigit() || line[col] == '.')) {
                        col++
                    }
                    withStyle(SpanStyle(color = Color(0xFFFAB387))) {
                        append(line.substring(numStart, col))
                    }
                    continue
                }

                // Symbols & Operators
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

// --- Markdown Document Parser ---

fun parseMarkdownContent(rawText: String): List<MarkdownElement> {
    val elements = mutableListOf<MarkdownElement>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trimEnd()

        if (line.isBlank()) {
            i++
            continue
        }

        // Fenced Code or Diagram Block: ```lang ... ```
        if (line.trimStart().startsWith("```")) {
            val lang = line.trim().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // consume closing ```

            val fullBlock = codeLines.joinToString("\n")
            if (lang.lowercase() == "mermaid") {
                // Peek ahead for companion explanation: *explanation* or _explanation_
                var explanation: String? = null
                if (i < lines.size && lines[i].trim().isNotBlank()) {
                    val candidate = lines[i].trim()
                    if ((candidate.startsWith("*") && candidate.endsWith("*")) ||
                        (candidate.startsWith("_") && candidate.endsWith("_"))) {
                        explanation = candidate.removeSurrounding("*").removeSurrounding("_").trim()
                        i++
                    }
                }
                elements.add(MarkdownElement.DiagramBlock(type = "mermaid", code = fullBlock, explanation = explanation))
            } else {
                elements.add(MarkdownElement.CodeBlock(language = lang, code = fullBlock))
            }
            continue
        }

        // Headings: #, ##, ###, ####
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headingText = line.drop(level).trim()
            elements.add(MarkdownElement.Heading(level = level, text = headingText))
            i++
            continue
        }

        // Markdown Table: | col1 | col2 |
        if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                tableLines.add(lines[i].trim())
                i++
            }
            if (tableLines.size >= 2) {
                val headers = tableLines[0].split("|").filter { it.isNotBlank() }.map { it.trim() }
                val rows = mutableListOf<List<String>>()
                for (tIdx in 2 until tableLines.size) { // skip separator row at index 1
                    val rowCells = tableLines[tIdx].split("|").filter { it.isNotBlank() }.map { it.trim() }
                    rows.add(rowCells)
                }
                elements.add(MarkdownElement.TableBlock(headers = headers, rows = rows))
            }
            continue
        }

        // Blockquotes: > quote
        if (line.trim().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            elements.add(MarkdownElement.Blockquote(quoteLines.joinToString(" ")))
            continue
        }

        // Bullet Lists: *, -, +
        if (line.trim().startsWith("* ") || line.trim().startsWith("- ") || line.trim().startsWith("+ ")) {
            val bulletItems = mutableListOf<String>()
            while (i < lines.size && (lines[i].trim().startsWith("* ") || lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("+ "))) {
                val itemText = lines[i].trim().drop(2).trim()
                bulletItems.add(itemText)
                i++
            }
            elements.add(MarkdownElement.BulletList(items = bulletItems))
            continue
        }

        // Numbered Lists: 1. , 2.
        val numMatch = Regex("""^(\d+)\.\s+(.*)$""").find(line.trim())
        if (numMatch != null) {
            val startIndex = numMatch.groupValues[1].toIntOrNull() ?: 1
            val numItems = mutableListOf<String>()
            while (i < lines.size) {
                val m = Regex("""^(\d+)\.\s+(.*)$""").find(lines[i].trim()) ?: break
                numItems.add(m.groupValues[2].trim())
                i++
            }
            elements.add(MarkdownElement.NumberedList(items = numItems, startIndex = startIndex))
            continue
        }

        // Regular Paragraph
        val paraLines = mutableListOf<String>()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trim().startsWith("#") &&
            !lines[i].trim().startsWith("```") &&
            !lines[i].trim().startsWith("|") &&
            !lines[i].trim().startsWith(">") &&
            !lines[i].trim().startsWith("* ") &&
            !lines[i].trim().startsWith("- ") &&
            Regex("""^\d+\.\s+""").find(lines[i].trim()) == null
        ) {
            paraLines.add(lines[i].trim())
            i++
        }
        if (paraLines.isNotEmpty()) {
            elements.add(MarkdownElement.Paragraph(text = paraLines.joinToString(" ")))
        }
    }

    return elements
}

// --- Canvas Drawing Helpers for Markdown Card Elements ---

private fun DrawScope.drawCopyIcon(color: Color) {
    val strokeWidth = 1.5.dp.toPx()
    val w = size.width
    val h = size.height

    // Back card
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.28f, h * 0.1f),
        size = Size(w * 0.58f, h * 0.64f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )

    // Front card
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.12f, h * 0.26f),
        size = Size(w * 0.58f, h * 0.64f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawSparkleStarIcon(color: Color) {
    val cx = size.width / 2
    val cy = size.height / 2
    val r = size.width / 2

    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticBezierTo(cx, cy, cx + r, cy)
        quadraticBezierTo(cx, cy, cx, cy + r)
        quadraticBezierTo(cx, cy, cx - r, cy)
        quadraticBezierTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path, color = color)
}
