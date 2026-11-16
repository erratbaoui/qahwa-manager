package com.example.qahwamanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every English string must exist in Arabic and French, with the same format placeholders,
 * so a missing or mistyped translation fails here instead of showing English (or crashing in
 * String.format) on a device.
 */
class StringResourcesTest {

    private val resDir = File("src/main/res")

    @Test
    fun everyStringIsTranslated_withMatchingPlaceholders() {
        val english = load("values")
        assertTrue("no strings found in ${resDir.absolutePath}", english.isNotEmpty())

        for (locale in listOf("values-ar", "values-fr")) {
            val translated = load(locale)
            val missing = english.keys.filter { it !in translated }
            assertEquals("$locale is missing translations", emptyList<String>(), missing)

            for ((name, resource) in english) {
                val expected = resource.texts.map(::placeholders).toSet()
                translated.getValue(name).texts.forEach { text ->
                    val actual = placeholders(text)
                    // A plural form may leave the count out (Arabic "one"/"two" do), but no
                    // translation may use a placeholder English doesn't supply.
                    val ok = if (resource.isPlural) expected.any { it.containsAll(actual) } else actual in expected
                    assertTrue("$locale/$name: placeholders $actual don't match $expected", ok)
                }
            }
        }
    }

    /** One text for a <string>, one per quantity for <plurals>. */
    private class Resource(val texts: List<String>, val isPlural: Boolean)

    /** Translatable resources of one values directory, by name. */
    private fun load(dir: String): Map<String, Resource> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "$dir/strings.xml"))
        val result = mutableMapOf<String, Resource>()
        val root = doc.documentElement
        root.elements("string")
            .filter { it.getAttribute("translatable") != "false" }
            .forEach { result[it.getAttribute("name")] = Resource(listOf(it.textContent), isPlural = false) }
        root.elements("plurals").forEach { plural ->
            result[plural.getAttribute("name")] =
                Resource(plural.elements("item").map { it.textContent }, isPlural = true)
        }
        return result
    }

    private fun Element.elements(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun placeholders(text: String): List<String> =
        Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]""").findAll(text).map { it.value }.toList().sorted()
}
