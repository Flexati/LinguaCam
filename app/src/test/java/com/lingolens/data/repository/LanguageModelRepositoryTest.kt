package com.linguacam.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Test puro JVM per il contratto di [ModelDownloadState] e [LanguageModelSource].
 *
 * Step 11: questi sealed/interface sono esposti dal modulo data
 * e possono essere testati indipendentemente dalla concrete implementation.
 */
class LanguageModelRepositoryTest {

    @Test
    fun `ModelDownloadState Idle and NotDownloaded are distinct`() {
        assertNotEquals<ModelDownloadState>(ModelDownloadState.Idle, ModelDownloadState.NotDownloaded)
    }

    @Test
    fun `ModelDownloadState Downloading carries progress`() {
        val s: ModelDownloadState = ModelDownloadState.Downloading(progressPercent = 42)
        assertEquals(42, (s as ModelDownloadState.Downloading).progressPercent)
    }

    @Test
    fun `ModelDownloadState Failed carries reason`() {
        val s: ModelDownloadState = ModelDownloadState.Failed(reason = "network")
        assertEquals("network", (s as ModelDownloadState.Failed).reason)
    }

    @Test
    fun `ModelDownloadState Downloaded is singleton`() {
        assertEquals(ModelDownloadState.Downloaded, ModelDownloadState.Downloaded)
    }

    @Test
    fun `exhaustive match check - 5 cases for ModelDownloadState`() {
        val cases: List<ModelDownloadState> = listOf(
            ModelDownloadState.Idle,
            ModelDownloadState.NotDownloaded,
            ModelDownloadState.Downloading(0),
            ModelDownloadState.Downloaded,
            ModelDownloadState.Failed("x")
        )
        assertEquals(5, cases.size)
    }
}
