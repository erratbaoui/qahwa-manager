package com.example.qahwamanager.ui.loyalty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeGeneratorTest {

    @Test
    fun encodeAndToSvg_producesValidMatrixAndFinderPatterns() {
        val url = "https://qahwa-manager-pos-2026.web.app/?cafe=481f6589-446e-4e97-a75d-f7cafe074788&table=5"
        val matrix = QrCodeGenerator.encode(url)

        // Version 5 (37x37) for 90-byte URL
        assertEquals(37, matrix.size)
        assertEquals(37, matrix[0].size)

        // Top-left 7x7 finder pattern outer corners and center must be dark
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])
        assertTrue(matrix[6][6])
        assertTrue(matrix[3][3])

        val svg = QrCodeGenerator.toSvg(url)
        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.contains("viewBox=\"0 0 41 41\""))
        assertTrue(svg.endsWith("</svg>"))
    }
}
