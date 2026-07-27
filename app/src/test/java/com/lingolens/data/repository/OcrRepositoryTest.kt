package com.lingolens.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test puro JVM per i data class esposti da [OcrRepository].
 *
 * Step 11: verificano la shape dei tipi di ritorno, utili per i test
 * di integrazione più a valle.
 */
class OcrRepositoryTest {

    @Test
    fun `RecognizedText carries full text and blocks`() {
        val r = RecognizedText(fullText = "Hello World", blocks = emptyList())
        assertEquals("Hello World", r.fullText)
        assertTrue(r.blocks.isEmpty())
    }

    @Test
    fun `TextBlock defaults confidence to 1f`() {
        val b = TextBlock(text = "Ciao", left = 0f, top = 0f, right = 100f, bottom = 30f)
        assertEquals("Ciao", b.text)
        assertEquals(0f, b.left)
        assertEquals(30f, b.bottom)
        assertEquals(1f, b.confidence)
    }

    @Test
    fun `TextBlock custom confidence preserved`() {
        val b = TextBlock(text = "X", left = 1f, top = 2f, right = 3f, bottom = 4f, confidence = 0.5f)
        assertEquals(0.5f, b.confidence)
    }
}
