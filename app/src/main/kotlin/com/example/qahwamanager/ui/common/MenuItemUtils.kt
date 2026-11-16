package com.example.qahwamanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.MenuItem
import java.util.Locale

/**
 * Returns the localized name of a [MenuItem] according to the current system/app locale,
 * gracefully falling back to English, French, or Arabic.
 */
fun MenuItem.getLocalizedName(locale: Locale = Locale.getDefault()): String {
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
