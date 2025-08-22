package com.fredz.optimanglesolaire.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {

    private const val PREFS = "i18n_prefs"
    private const val KEY_LANG = "language_tag"

    /** Applique la langue sauvegardée (appelé dans OptiApp.onCreate). */
    fun applySaved(context: Context) {
        val saved = getSavedTag(context)
        if (!saved.isNullOrBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
        }
    }

    /** Change la langue de l’app et la sauvegarde. */
    fun setLanguage(context: Context, languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        saveTag(context, languageTag)
    }

    /** Retourne la langue actuellement appliquée (ex: "fr", "en-GB"). */
    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val first = locales[0]
        return first?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()
    }

    /** Liste des langues supportées aujourd’hui (tu pourras en rajouter plus tard). */
    val supported: List<String> = listOf(
        "fr", // Français (par défaut)
        "en", // Anglais
        "de", // Allemand
        "es"  // Espagnol
        // "zh", "ja", "hi", ... quand tu ajouteras les ressources
    )

    // --- Privé ---

    private fun saveTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, tag)
            .apply()
    }

    private fun getSavedTag(context: Context): String? {
        // Si AppCompat connaît déjà une locale, on la prend (utile après redémarrage)
        AppCompatDelegate.getApplicationLocales()[0]?.let { return it.toLanguageTag() }
        // Sinon, on tombe sur la valeur prefs
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
    }
}
