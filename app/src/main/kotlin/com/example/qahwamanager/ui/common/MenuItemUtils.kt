package com.example.qahwamanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.MenuItem
import java.util.Locale

/**
 * Returns the localized name of a [MenuItem] inside a Composable,
 * automatically observing the active [LocalConfiguration] locale.
 */
@Composable
fun MenuItem.getLocalizedName(): String {
    val config = LocalConfiguration.current
    val locale = if (!config.locales.isEmpty) config.locales[0] else Locale.getDefault()
    return getLocalizedName(locale)
}

/**
 * Returns the localized name of a [MenuItem] for a given [Locale],
 * gracefully falling back to Arabic, French, or English.
 */
fun MenuItem.getLocalizedName(locale: Locale): String {
    val lang = locale.language.lowercase()
    return when {
        lang.startsWith("ar") && nameAr.isNotBlank() -> nameAr
        lang.startsWith("fr") && nameFr.isNotBlank() -> nameFr
        name.isNotBlank() -> name
        nameFr.isNotBlank() -> nameFr
        else -> nameAr
    }
}

/**
 * Returns localized category title.
 */
@Composable
fun getLocalizedCategoryName(category: String): String {
    return when (category.lowercase()) {
        "coffee" -> stringResource(R.string.category_coffee)
        "tea" -> stringResource(R.string.category_tea)
        "food" -> stringResource(R.string.category_food)
        "juice" -> stringResource(R.string.category_juice)
        else -> category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
