package app.lasttime.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.lasttime.R

/** UI languages. [System] clears the override: the app follows the system language and falls back to English. */
enum class Language(val tag: String, @StringRes val label: Int) {
    System("", R.string.language_system),
    English("en", R.string.language_en),
    Spanish("es", R.string.language_es),
    German("de", R.string.language_de),
    French("fr", R.string.language_fr),
    Italian("it", R.string.language_it),
}

/**
 * Per-app language. Android 13+ stores it in the system; before that we keep it in preferences and re-apply it at
 * start-up, because AppCompat does not persist it without an activity.
 */
object AppLanguage {
    private const val PREFS = "language"
    private const val KEY = "tag"

    fun current(): Language {
        val lang = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: return Language.System
        return Language.entries.firstOrNull { it.tag.isNotEmpty() && it.tag == lang } ?: Language.System
    }

    fun set(c: Context, language: Language) {
        if (Build.VERSION.SDK_INT < 33) {
            c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    /** Call from Application.onCreate. */
    fun restore(c: Context) {
        if (Build.VERSION.SDK_INT >= 33) return
        val tag = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        if (tag.isNotEmpty()) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** A context whose resources use the chosen language, for widgets and workers (no-op on Android 13+). */
    fun localized(c: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return c
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return c
        val config = Configuration(c.resources.configuration)
        config.setLocale(locales.get(0))
        return c.createConfigurationContext(config)
    }
}
