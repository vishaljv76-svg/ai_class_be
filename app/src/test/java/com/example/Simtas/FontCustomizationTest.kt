package com.example.Simtas

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.Simtas.ui.theme.AppFontStyle
import com.example.Simtas.ui.theme.FontManager
import com.example.Simtas.ui.theme.getTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FontCustomizationTest {

    @Test
    fun testExactlyThreeFontStylesAvailable() {
        val styles = AppFontStyle.values()
        assertEquals("There must be exactly 3 selectable font styles", 3, styles.size)
        assertEquals(AppFontStyle.OldSerif, styles[0])
        assertEquals(AppFontStyle.BoldSansSerif, styles[1])
        assertEquals(AppFontStyle.ScriptCursive, styles[2])
    }

    @Test
    fun testFontFamilyMappings() {
        assertEquals(FontFamily.Serif, AppFontStyle.OldSerif.fontFamily)
        assertEquals(FontFamily.SansSerif, AppFontStyle.BoldSansSerif.fontFamily)
        assertEquals(FontFamily.Cursive, AppFontStyle.ScriptCursive.fontFamily)
    }

    @Test
    fun testSamplePreviewTextIsHelloWorld() {
        AppFontStyle.values().forEach { style ->
            assertEquals("Hello World", style.samplePreview)
        }
    }

    @Test
    fun testDefaultFontStyleIsBoldSansSerif() {
        assertEquals(AppFontStyle.BoldSansSerif, FontManager.currentFontStyle)
    }

    @Test
    fun testTypographyGenerationForAllThreeStyles() {
        AppFontStyle.values().forEach { style ->
            val typography = getTypography(style)
            assertNotNull(typography)
            assertEquals(style.fontFamily, typography.bodyLarge.fontFamily)
            assertEquals(style.fontFamily, typography.headlineLarge.fontFamily)
            assertEquals(style.fontFamily, typography.titleLarge.fontFamily)
            assertEquals(style.fontFamily, typography.labelSmall.fontFamily)
        }
    }
}
