package com.example.Simtas

import com.example.Simtas.ui.chat.MarkdownElement
import com.example.Simtas.ui.chat.buildInlineAnnotatedString
import com.example.Simtas.ui.chat.parseMarkdownContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 5A (Smart Answer Markdown Formatting) and
 * Phase 5B (Intelligent Mermaid Diagram Rendering).
 */
class MarkdownRendererTest {

    @Test
    fun testHeadings_parsedWithCorrectLevels() {
        val markdown = """
            # Heading Level 1
            ## Heading Level 2
            ### Heading Level 3
            #### Heading Level 4
        """.trimIndent()

        val elements = parseMarkdownContent(markdown)
        assertEquals(4, elements.size)

        assertTrue(elements[0] is MarkdownElement.Heading)
        assertEquals(1, (elements[0] as MarkdownElement.Heading).level)
        assertEquals("Heading Level 1", (elements[0] as MarkdownElement.Heading).text)

        assertTrue(elements[1] is MarkdownElement.Heading)
        assertEquals(2, (elements[1] as MarkdownElement.Heading).level)
        assertEquals("Heading Level 2", (elements[1] as MarkdownElement.Heading).text)

        assertTrue(elements[2] is MarkdownElement.Heading)
        assertEquals(3, (elements[2] as MarkdownElement.Heading).level)
        assertEquals("Heading Level 3", (elements[2] as MarkdownElement.Heading).text)

        assertTrue(elements[3] is MarkdownElement.Heading)
        assertEquals(4, (elements[3] as MarkdownElement.Heading).level)
        assertEquals("Heading Level 4", (elements[3] as MarkdownElement.Heading).text)
    }

    @Test
    fun testCodeBlock_parsedWithLanguageAndCode() {
        val markdown = """
            ```python
            def binary_search(arr, target):
                low = 0
                high = len(arr) - 1
                return -1
            ```
        """.trimIndent()

        val elements = parseMarkdownContent(markdown)
        assertEquals(1, elements.size)
        assertTrue(elements[0] is MarkdownElement.CodeBlock)

        val code = elements[0] as MarkdownElement.CodeBlock
        assertEquals("python", code.language)
        assertTrue(code.code.contains("def binary_search"))
        assertTrue(code.code.contains("return -1"))
    }

    @Test
    fun testMermaidDiagram_parsedWithExplanation() {
        val markdown = """
            ```mermaid
            flowchart TD
                Sun["Sunlight"] --> Leaf["Chloroplasts"]
                Water["Water"] --> Leaf
                Leaf --> Glucose["Glucose"]
            ```
            *Photosynthesis converts solar energy, water, and CO2 into glucose and oxygen.*
        """.trimIndent()

        val elements = parseMarkdownContent(markdown)
        assertEquals(1, elements.size)
        assertTrue(elements[0] is MarkdownElement.DiagramBlock)

        val diagram = elements[0] as MarkdownElement.DiagramBlock
        assertEquals("mermaid", diagram.type)
        assertTrue(diagram.code.contains("flowchart TD"))
        assertTrue(diagram.code.contains("Sun[\"Sunlight\"]"))
        assertNotNull(diagram.explanation)
        assertTrue(diagram.explanation!!.contains("Photosynthesis converts solar energy"))
    }

    @Test
    fun testTable_parsedHeadersAndRows() {
        val markdown = """
            | Normal Form | Rule | Anomaly Prevented |
            |-------------|------|-------------------|
            | 1NF | Atomic values | Repeating groups |
            | 2NF | No partial dependencies | Update anomalies |
            | 3NF | No transitive dependencies | Deletion anomalies |
        """.trimIndent()

        val elements = parseMarkdownContent(markdown)
        assertEquals(1, elements.size)
        assertTrue(elements[0] is MarkdownElement.TableBlock)

        val table = elements[0] as MarkdownElement.TableBlock
        assertEquals(3, table.headers.size)
        assertEquals("Normal Form", table.headers[0])
        assertEquals("Rule", table.headers[1])
        assertEquals("Anomaly Prevented", table.headers[2])

        assertEquals(3, table.rows.size)
        assertEquals("1NF", table.rows[0][0])
        assertEquals("2NF", table.rows[1][0])
        assertEquals("3NF", table.rows[2][0])
    }

    @Test
    fun testLists_bulletAndNumberedLists() {
        val markdown = """
            * First step in normalization
            * Second step: identify functional dependencies
            * Third step: verify candidate keys
        """.trimIndent()

        val elements = parseMarkdownContent(markdown)
        assertEquals(1, elements.size)
        assertTrue(elements[0] is MarkdownElement.BulletList)
        val bulletList = elements[0] as MarkdownElement.BulletList
        assertEquals(3, bulletList.items.size)
        assertEquals("First step in normalization", bulletList.items[0])

        val numberedMarkdown = """
            1. Calculate mid = (low + high) / 2
            2. Compare target with arr[mid]
            3. Adjust low or high boundary
        """.trimIndent()

        val numElements = parseMarkdownContent(numberedMarkdown)
        assertEquals(1, numElements.size)
        assertTrue(numElements[0] is MarkdownElement.NumberedList)
        val numList = numElements[0] as MarkdownElement.NumberedList
        assertEquals(3, numList.items.size)
        assertEquals(1, numList.startIndex)
        assertEquals("Calculate mid = (low + high) / 2", numList.items[0])
    }

    @Test
    fun testInlineFormatting_stripsRawSymbols() {
        val annotated = buildInlineAnnotatedString("This is **bold** and *italic* with `code`.", isDark = false)
        val text = annotated.text

        // Verify raw markdown characters are not in the rendered text
        assertFalse("Must not contain ** symbols", text.contains("**"))
        assertFalse("Must not contain ` backticks", text.contains("`"))
        assertEquals("This is bold and italic with  code .", text)
    }

    @Test
    fun testMathFormatting_inlineFormula() {
        val annotated = buildInlineAnnotatedString("Einstein formulated ${'$'}E = mc^2${'$'} in relativity.", isDark = false)
        val text = annotated.text
        assertFalse("Must strip dollar signs", text.contains("$"))
        assertEquals("Einstein formulated E = mc^2 in relativity.", text)
    }
}

