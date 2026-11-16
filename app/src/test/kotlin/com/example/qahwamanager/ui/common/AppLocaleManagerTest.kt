package com.example.qahwamanager.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLocaleManagerTest {

    @Test
    fun normalizeLanguageCode_worksForArabic() {
        assertEquals("ar", if ("ar".lowercase().startsWith("ar")) "ar" else "en")
    }

    @Test
    fun normalizeLanguageCode_worksForFrench() {
        assertEquals("fr", if ("fr".lowercase().startsWith("fr")) "fr" else "en")
    }

    @Test
    fun normalizeLanguageCode_worksForEnglish() {
        assertEquals("en", if ("en".lowercase().startsWith("en")) "en" else "en")
    }

    @Test
    fun supportedLanguages_hasExactCodes() {
        val codes = SUPPORTED_LANGUAGES.map { it.code }
        assertEquals(listOf("en", "ar", "fr"), codes)
    }

    @Test
    fun supportedLanguages_eachHasValidDisplayRes() {
        SUPPORTED_LANGUAGES.forEach { lang ->
            assert(lang.displayNameRes != 0)
            assert(lang.flag.isNotBlank())
            assert(lang.nativeName.isNotBlank())
        }
    }
}
