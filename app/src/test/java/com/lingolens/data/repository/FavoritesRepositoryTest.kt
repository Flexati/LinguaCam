package com.linguacam.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test unitari per FavoritesRepository.
 *
 * Verifica:
 * - Salvataggio preferiti
 * - Recupero preferiti
 * - Rimozione preferiti
 * - Aggiornamento note
 * - Persistenza
 */
@ExperimentalCoroutinesApi
class FavoritesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: FavoritesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FavoritesRepository(context)
    }

    @After
    fun tearDown() = runTest {
        repository.clearAllFavorites()
    }

    @Test
    fun testSaveFavorite() = runTest {
        // Salva un preferito
        repository.saveFavorite(
            originalText = "Ciao",
            translatedText = "Hello",
            sourceLanguage = "it",
            targetLanguage = "en",
            confidence = 0.95f,
            notes = "Saluto comune"
        )

        // Verifica che sia stato salvato
        val favorites = repository.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("Ciao", favorites[0].originalText)
        assertEquals("Hello", favorites[0].translatedText)
        assertEquals("Saluto comune", favorites[0].notes)
    }

    @Test
    fun testGetFavorites() = runTest {
        // Salva più preferiti
        repository.saveFavorite("Ciao", "Hello", "it", "en")
        repository.saveFavorite("Grazie", "Thank you", "it", "en")
        repository.saveFavorite("Arrivederci", "Goodbye", "it", "en")

        // Verifica che siano stati recuperati
        val favorites = repository.getFavorites().first()
        assertEquals(3, favorites.size)
    }

    @Test
    fun testRemoveFavorite() = runTest {
        // Salva un preferito
        repository.saveFavorite("Ciao", "Hello", "it", "en")
        var favorites = repository.getFavorites().first()
        val favoriteId = favorites[0].id

        // Rimuove il preferito
        repository.removeFavorite(favoriteId)

        // Verifica che sia stato rimosso
        favorites = repository.getFavorites().first()
        assertEquals(0, favorites.size)
    }

    @Test
    fun testUpdateFavoriteNotes() = runTest {
        // Salva un preferito
        repository.saveFavorite("Ciao", "Hello", "it", "en", notes = "Nota iniziale")
        var favorites = repository.getFavorites().first()
        val favoriteId = favorites[0].id

        // Aggiorna le note
        repository.updateFavoriteNotes(favoriteId, "Nota aggiornata")

        // Verifica l'aggiornamento
        favorites = repository.getFavorites().first()
        assertEquals("Nota aggiornata", favorites[0].notes)
    }

    @Test
    fun testIsFavorite() = runTest {
        // Salva un preferito
        repository.saveFavorite("Ciao", "Hello", "it", "en")

        // Verifica che sia nei preferiti
        assertTrue(repository.isFavorite("Ciao", "Hello"))
        assertFalse(repository.isFavorite("Arrivederci", "Goodbye"))
    }

    @Test
    fun testGetFavoritesCount() = runTest {
        // Inizialmente vuoto
        var count = repository.getFavoritesCount().first()
        assertEquals(0, count)

        // Salva preferiti
        repository.saveFavorite("Ciao", "Hello", "it", "en")
        repository.saveFavorite("Grazie", "Thank you", "it", "en")

        // Verifica il conteggio
        count = repository.getFavoritesCount().first()
        assertEquals(2, count)
    }

    @Test
    fun testClearAllFavorites() = runTest {
        // Salva preferiti
        repository.saveFavorite("Ciao", "Hello", "it", "en")
        repository.saveFavorite("Grazie", "Thank you", "it", "en")

        // Cancella tutti
        repository.clearAllFavorites()

        // Verifica che sia vuoto
        val favorites = repository.getFavorites().first()
        assertEquals(0, favorites.size)
    }

    @Test
    fun testFavoritesOrderedByDateDescending() = runTest {
        // Salva preferiti con ritardo
        repository.saveFavorite("Primo", "First", "it", "en")
        Thread.sleep(100)
        repository.saveFavorite("Secondo", "Second", "it", "en")
        Thread.sleep(100)
        repository.saveFavorite("Terzo", "Third", "it", "en")

        // Verifica che siano ordinati per data decrescente
        val favorites = repository.getFavorites().first()
        assertEquals("Terzo", favorites[0].originalText)
        assertEquals("Secondo", favorites[1].originalText)
        assertEquals("Primo", favorites[2].originalText)
    }

    @Test
    fun testExportFavoritesAsJson() = runTest {
        // Salva preferiti
        repository.saveFavorite("Ciao", "Hello", "it", "en")
        repository.saveFavorite("Grazie", "Thank you", "it", "en")

        // Esporta come JSON
        val json = repository.exportFavoritesAsJson()

        // Verifica che sia un JSON valido
        assertTrue(json.contains("Ciao"))
        assertTrue(json.contains("Hello"))
        assertTrue(json.contains("Grazie"))
        assertTrue(json.contains("Thank you"))
    }

    @Test
    fun testImportFavoritesFromJson() = runTest {
        // Salva un preferito
        repository.saveFavorite("Ciao", "Hello", "it", "en")

        // Esporta come JSON
        val json = repository.exportFavoritesAsJson()

        // Cancella tutto
        repository.clearAllFavorites()
        var favorites = repository.getFavorites().first()
        assertEquals(0, favorites.size)

        // Importa dal JSON
        repository.importFavoritesFromJson(json)

        // Verifica che sia stato importato
        favorites = repository.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("Ciao", favorites[0].originalText)
    }

    @Test
    fun testPersistenceAcrossInstances() = runTest {
        // Salva un preferito
        repository.saveFavorite("Ciao", "Hello", "it", "en")

        // Crea una nuova istanza
        val newRepository = FavoritesRepository(context)

        // Verifica che il preferito sia persistito
        val favorites = newRepository.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("Ciao", favorites[0].originalText)
    }
}
