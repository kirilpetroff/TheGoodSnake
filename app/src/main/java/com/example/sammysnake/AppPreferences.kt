package com.example.sammysnake

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Пази настройките на играча (управление, език, цвят на змията) между стартиранията. */
object AppPreferences {
    private const val PREFS_NAME = "sammy_snake_prefs"
    private const val KEY_CONTROL_TYPE = "control_type"
    private const val KEY_SNAKE_COLOR_INDEX = "snake_color_index"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_MUSIC_VOLUME_PERCENT = "music_volume_percent"
    private const val DEFAULT_MUSIC_VOLUME_PERCENT = 25

    fun getControlType(context: Context): ControlType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_CONTROL_TYPE, ControlType.BUTTONS.name)
        return runCatching { ControlType.valueOf(name ?: ControlType.BUTTONS.name) }.getOrDefault(ControlType.BUTTONS)
    }

    fun setControlType(context: Context, controlType: ControlType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CONTROL_TYPE, controlType.name)
            .apply()
    }

    fun getSnakeColorIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val index = prefs.getInt(KEY_SNAKE_COLOR_INDEX, 0)
        return index.coerceIn(0, SnakeColorPalette.presets.size - 1)
    }

    fun setSnakeColorIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SNAKE_COLOR_INDEX, index)
            .apply()
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "bg") ?: "bg"
    }

    fun setLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, languageTag)
            .apply()
        applyLanguage(languageTag)
    }

    /** Връща силата на музиката като дял от 0f до 1f. */
    fun getMusicVolume(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val percent = prefs.getInt(KEY_MUSIC_VOLUME_PERCENT, DEFAULT_MUSIC_VOLUME_PERCENT)
        return percent.coerceIn(0, 100) / 100f
    }

    /** Приема стойност от 0 до 100. */
    fun setMusicVolume(context: Context, percent: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MUSIC_VOLUME_PERCENT, percent.coerceIn(0, 100))
            .apply()
    }

    /** Прилага запазения език - извиква се рано в onCreate на всеки екран. */
    fun applyLanguage(context: Context) {
        applyLanguage(getLanguage(context))
    }

    private fun applyLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
