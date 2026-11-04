package com.example.Simtas.ui.theme

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported Theme Modes in SmartNotes AI.
 */
enum class AppThemeMode {
    Light,
    Dark,
    System;

    val displayName: String
        get() = when (this) {
            Light -> "Light Mode"
            Dark -> "Dark Mode"
            System -> "System Default"
        }
}

/**
 * Centralized Theme Manager handling real-time theme switching, system detection, and persistence.
 */
object ThemeManager {
    private const val PREFS_NAME = "smartnotes_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private var appContext: Context? = null
    private val _themeMode = MutableStateFlow(AppThemeMode.Light)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    val currentThemeMode: AppThemeMode
        get() = _themeMode.value

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.Light.name)
        val mode = try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.Light.name)
        } catch (_: Exception) {
            AppThemeMode.Light
        }
        _themeMode.value = mode
    }

    fun setThemeMode(context: Context?, mode: AppThemeMode) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        _themeMode.value = mode
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putString(KEY_THEME_MODE, mode.name)
            apply()
        }
    }

    fun isDark(): Boolean {
        return when (_themeMode.value) {
            AppThemeMode.Light -> false
            AppThemeMode.Dark -> true
            AppThemeMode.System -> isSystemNightMode()
        }
    }

    private fun isSystemNightMode(): Boolean {
        val ctx = appContext ?: return false
        val nightModeFlags = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
