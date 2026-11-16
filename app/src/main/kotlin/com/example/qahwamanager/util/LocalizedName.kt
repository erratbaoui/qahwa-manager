package com.example.qahwamanager.util

/** Picks the name matching [language] ("ar", "fr", ...), falling back to the English [name]. */
fun localizedName(name: String, nameAr: String, nameFr: String, language: String): String {
    val translated = when (language) {
        "ar" -> nameAr
        "fr" -> nameFr
        else -> name
    }
    return translated.ifBlank { name }
}
