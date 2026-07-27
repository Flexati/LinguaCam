package com.lingolens.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test puro JVM per il contratto di [ModelNotDownloadedException].
 *
 * Step 11: assicura che la UI possa filtrare questa eccezione per mostrare
 * un messaggio "scarica il modello di XX per tradurre".
 */
class TranslationRepositoryTest {

    @Test
    fun `ModelNotDownloadedException carries language code`() {
        val ex = ModelNotDownloadedException(languageCode = "ja")
        assertEquals("ja", ex.languageCode)
        assertTrue(ex.message!!.contains("ja"))
    }

    @Test
    fun `ModelNotDownloadedException is a Kotlin Exception`() {
        val ex: Throwable = ModelNotDownloadedException(languageCode = "en")
        assertTrue("must be Exception", ex is Exception)
        assertTrue("must be RuntimeException-like", ex is RuntimeException ||
            ex.message != null)
    }
}
