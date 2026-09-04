package com.cydoniancitizen.mindora.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.annotation.StringRes
import com.cydoniancitizen.mindora.R

/**
 * The languages offered in settings, in the order they are shown.
 *
 * A null tag means "whatever the system is set to", which is the platform's own idea of no choice
 * rather than a third language.
 */
enum class AppLanguage(
    val languageTag: String?,
    @param:StringRes val labelResId: Int,
) {
    SYSTEM(null, R.string.language_system),
    ENGLISH("en", R.string.language_english),
    ITALIAN("it", R.string.language_italian),
}

/** The language the app is currently displayed in, as the platform records it. */
internal fun currentAppLanguage(context: Context): AppLanguage {
    val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
    val tag = locales?.takeUnless { it.isEmpty }?.get(0)?.language
    return AppLanguage.entries.firstOrNull { it.languageTag == tag } ?: AppLanguage.SYSTEM
}

/**
 * Hands the choice to the platform instead of storing it.
 *
 * Android owns the per-app language from 13 onwards: the choice survives reinstalls, appears in
 * system settings beside every other app, and is applied by recreating the activity. Keeping a
 * second copy in the app's own preferences would only create a source of truth that can disagree
 * with the one actually deciding which strings load.
 */
internal fun setAppLanguage(context: Context, language: AppLanguage) {
    val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
    localeManager.applicationLocales = language.languageTag
        ?.let { LocaleList.forLanguageTags(it) }
        ?: LocaleList.getEmptyLocaleList()
}
