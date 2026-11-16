package com.example.qahwamanager.ui.common

import com.example.qahwamanager.data.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MenuItemUtilsTest {

    private val sampleItem = MenuItem(
        id = 1,
        name = "Espresso",
        nameAr = "إسبريسو",
        nameFr = "Café Expresso",
        category = "coffee",
        price = 15.0,
    )

    @Test
    fun getLocalizedName_arabicLocale_returnsArabicName() {
        val result = sampleItem.getLocalizedName(Locale.forLanguageTag("ar"))
        assertEquals("إسبريسو", result)
    }

    @Test
    fun getLocalizedName_frenchLocale_returnsFrenchName() {
        val result = sampleItem.getLocalizedName(Locale.FRENCH)
        assertEquals("Café Expresso", result)
    }

    @Test
    fun getLocalizedName_englishLocale_returnsEnglishName() {
        val result = sampleItem.getLocalizedName(Locale.ENGLISH)
        assertEquals("Espresso", result)
    }

    @Test
    fun getLocalizedName_fallbackWhenMissingArabic() {
        val itemNoAr = sampleItem.copy(nameAr = "")
        val result = itemNoAr.getLocalizedName(Locale.forLanguageTag("ar"))
        assertEquals("Espresso", result)
    }
}
