package com.example.Simtas.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The three selectable Global Font Styles in SmartNotes AI:
 * 1. Old Serif: Classic, formal, traditional serif typography.
 * 2. Bold Sans-Serif: Clean, modern, heavy lettering.
 * 3. Script / Cursive: Elegant handwritten-style typography.
 */
enum class AppFontStyle(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val samplePreview: String = "Hello World"
) {
    OldSerif(
        id = "old_serif",
        displayName = "Old Serif",
        subtitle = "Classic, formal & traditional serif typography"
    ),
    BoldSansSerif(
        id = "bold_sans_serif",
        displayName = "Bold Sans-Serif",
        subtitle = "Clean, modern & heavy lettering"
    ),
    ScriptCursive(
        id = "script_cursive",
        displayName = "Script / Cursive",
        subtitle = "Elegant handwritten-style typography"
    );

    val fontFamily: FontFamily
        get() = when (this) {
            OldSerif -> FontFamily.Serif
            BoldSansSerif -> FontFamily.SansSerif
            ScriptCursive -> FontFamily.Cursive
        }

    val previewWeight: FontWeight
        get() = when (this) {
            OldSerif -> FontWeight.Medium
            BoldSansSerif -> FontWeight.ExtraBold
            ScriptCursive -> FontWeight.Normal
        }
}

/**
 * Centralized Font Style Manager handling real-time typography switching,
 * persistent SharedPreferences storage, and safe offline defaults.
 */
object FontManager {
    private const val PREFS_NAME = "smartnotes_font_prefs"
    private const val KEY_FONT_STYLE = "selected_font_style"

    private var appContext: Context? = null
    // Safe default font style for first-time users is BoldSansSerif
    private val _fontStyle = MutableStateFlow(AppFontStyle.BoldSansSerif)
    val fontStyle: StateFlow<AppFontStyle> = _fontStyle.asStateFlow()

    val currentFontStyle: AppFontStyle
        get() = _fontStyle.value

    /**
     * Initializes FontManager from application context, restoring saved preference.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FONT_STYLE, AppFontStyle.BoldSansSerif.name)
        val style = try {
            AppFontStyle.valueOf(saved ?: AppFontStyle.BoldSansSerif.name)
        } catch (_: Exception) {
            AppFontStyle.BoldSansSerif
        }
        _fontStyle.value = style
    }

    /**
     * Sets and persistently saves the active font style.
     */
    fun setFontStyle(context: Context?, style: AppFontStyle) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        _fontStyle.value = style
        val targetContext = context ?: appContext
        targetContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putString(KEY_FONT_STYLE, style.name)
            apply()
        }
    }
}
