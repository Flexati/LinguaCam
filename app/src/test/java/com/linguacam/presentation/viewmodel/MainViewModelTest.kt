package com.linguacam.presentation.viewmodel

import com.linguacam.data.repository.FavoritesRepository
import com.linguacam.data.repository.LanguageModelRepository
import com.linguacam.data.repository.ModelDownloadState
import com.linguacam.data.repository.OcrRepository
import com.linguacam.data.repository.RecognizedText
import com.linguacam.data.repository.TextBlock
import com.linguacam.data.repository.TranslationRepository
import com.linguacam.domain.model.FavoriteTranslation
import com.linguacam.domain.model.Language
import com.linguacam.domain.model.TranslationResult
import com.linguacam.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test unitari per MainViewModel contro le FIRME REALI del codice attuale.
 *
 * Step 11:
 * - i repository sono mockati con Mockito (classi concrete, non interface)
 * - niente Robolectric, niente Context
 * - verifica metodi REALI del MainViewModel: setSourceLanguage/setTargetLanguage/swapLanguages/
 *   setTranslationResult/clearTranslationResult/setOnlineStatus/translateText/saveFavorite/...
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var langRepo: LanguageModelRepository
    private lateinit var transRepo: TranslationRepository
    private lateinit var ocrRepo: OcrRepository
    private lateinit var favRepo: FavoritesRepository

    private val installedFlow = MutableStateFlow(setOf("it", "en"))
    private val downloadFlow = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    private val favoritesFlow = MutableStateFlow<List<FavoriteTranslation>>(emptyList())
    private val favoritesCountFlow = MutableStateFlow(0)

    @Before
    fun setup() {
        installedFlow.value = setOf("it", "en")
        downloadFlow.value = ModelDownloadState.Idle
        favoritesFlow.value = emptyList()
        favoritesCountFlow.value = 0

        langRepo = org.mockito.kotlin.mock {
            org.mockito.kotlin.whenever(it.installedLanguageCodes).thenReturn(installedFlow)
            org.mockito.kotlin.whenever(it.downloadState).thenReturn(downloadFlow)
            org.mockito.kotlin.whenever(refreshInstalledLanguages()).thenAnswer { installedFlow.value }
            org.mockito.kotlin.whenever(isLanguageInstalled(org.mockito.kotlin.any())).thenAnswer { invocation ->
                val code = invocation.arguments[0] as String
                installedFlow.value.contains(code)
            }
            org.mockito.kotlin.whenever(canInstallLanguageOnFreePlan())
                .thenAnswer { installedFlow.value.size < 2 }
        }

        transRepo = org.mockito.kotlin.mock {
            org.mockito.kotlin.whenever(translate(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any()))
                .thenAnswer { invocation ->
                    val text = invocation.arguments[0] as String
                    val src = invocation.arguments[1] as String
                    val tgt = invocation.arguments[2] as String
                    Result.success(TranslationResult(text, "TR($text)", 1.0f, src, tgt))
                }
        }

        ocrRepo = org.mockito.kotlin.mock()

        favRepo = org.mockito.kotlin.mock {
            org.mockito.kotlin.whenever(favorites).thenReturn(favoritesFlow)
            org.mockito.kotlin.whenever(getFavorites()).thenReturn(favoritesFlow)
            org.mockito.kotlin.whenever(getFavoritesCount()).thenReturn(favoritesCountFlow)
        }
    }

    private fun newVm() = MainViewModel(langRepo, transRepo, ocrRepo, favRepo)

    @Test
    fun `initial state has source it and target en, no translation`() = runTest {
        val vm = newVm()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertNotNull(state)
        assertEquals("it", state.sourceLanguage.code)
        assertEquals("en", state.targetLanguage.code)
        assertNull(state.lastTranslation)
        assertFalse(state.isTranslating)
    }

    @Test
    fun `setSourceLanguage updates uiState`() = runTest {
        val vm = newVm()
        vm.setSourceLanguage(Language("es", "Spagnolo", "Espa\u00f1ol"))
        assertEquals("es", vm.uiState.value.sourceLanguage.code)
    }

    @Test
    fun `setTargetLanguage updates uiState`() = runTest {
        val vm = newVm()
        vm.setTargetLanguage(Language("fr", "Francese", "Fran\u00e7ais"))
        assertEquals("fr", vm.uiState.value.targetLanguage.code)
    }

    @Test
    fun `swapLanguages swaps source and target`() = runTest {
        val vm = newVm()
        val before = vm.uiState.value
        vm.swapLanguages()
        val after = vm.uiState.value
        assertEquals(before.targetLanguage.code, after.sourceLanguage.code)
        assertEquals(before.sourceLanguage.code, after.targetLanguage.code)
    }

    @Test
    fun `setTranslationResult stores result in lastTranslation`() = runTest {
        val vm = newVm()
        vm.setTranslationResult(
            TranslationResult("Ciao", "Hello", 0.95f, "it", "en")
        )
        val state = vm.uiState.value
        assertNotNull(state.lastTranslation)
        assertEquals("Hello", state.lastTranslation!!.translatedText)
    }

    @Test
    fun `clearTranslationResult nulls lastTranslation`() = runTest {
        val vm = newVm()
        vm.setTranslationResult(TranslationResult("a", "b", 1f, "it", "en"))
        vm.clearTranslationResult()
        assertNull(vm.uiState.value.lastTranslation)
    }

    @Test
    fun `setOnlineStatus toggles offlineMode`() = runTest {
        val vm = newVm()
        vm.setOnlineStatus(false)
        assertTrue(vm.uiState.value.isOfflineMode)
        vm.setOnlineStatus(true)
        assertFalse(vm.uiState.value.isOfflineMode)
    }

    @Test
    fun `translateText on success updates lastTranslation`() = runTest {
        val vm = newVm()
        vm.translateText("Ciao")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertNotNull(state.lastTranslation)
        assertFalse(state.isTranslating)
    }
}

/**
 * Smoke test per [RecognizedText] / [TextBlock] data classes.
 * Step 11: garantisce che il contratto di ritorno di OcrRepository sia valido.
 */
class RecognizedTextTest {

    @Test
    fun `RecognizedText carries blocks`() {
        val blocks = listOf(
            TextBlock("Ciao", 10f, 20f, 100f, 50f),
            TextBlock("Mondo", 110f, 20f, 200f, 50f)
        )
        val r = RecognizedText("Ciao Mondo", blocks)
        assertEquals("Ciao Mondo", r.fullText)
        assertEquals(2, r.blocks.size)
        assertEquals("Ciao", r.blocks[0].text)
        assertEquals(10f, r.blocks[0].left)
    }
}
