package com.example.qahwamanager.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedNameTest {

    private fun pick(language: String, ar: String = "أتاي", fr: String = "Thé à la menthe") =
        localizedName(name = "Mint tea", nameAr = ar, nameFr = fr, language = language)

    @Test
    fun picksArabicForArabicLocale() {
        assertEquals("أتاي", pick("ar"))
    }

    @Test
    fun picksFrenchForFrenchLocale() {
        assertEquals("Thé à la menthe", pick("fr"))
    }

    @Test
    fun picksEnglishForOtherLocales() {
        assertEquals("Mint tea", pick("en"))
        assertEquals("Mint tea", pick("es"))
    }

    @Test
    fun fallsBackToEnglishWhenTranslationIsBlank() {
        assertEquals("Mint tea", pick("ar", ar = " "))
        assertEquals("Mint tea", pick("fr", fr = ""))
    }
}
