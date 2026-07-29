package com.linguacam.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linguacam.domain.model.FavoriteTranslation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

/**
 * Repository per gestire le traduzioni preferite.
 *
 * Responsabilità:
 * - Salvare traduzioni nei preferiti
 * - Recuperare lista preferiti
 * - Rimuovere dai preferiti
 * - Aggiornare note
 * - Persistenza con DataStore
 *
 * Architettura: Data Layer
 */
class FavoritesRepository(private val context: Context) {

    private val favoritesKey = stringPreferencesKey("favorites_list")
    private val json = Json

    /**
     * Salva una traduzione nei preferiti.
     *
     * @param originalText Testo originale
     * @param translatedText Testo tradotto
     * @param sourceLanguage Lingua di origine
     * @param targetLanguage Lingua di destinazione
     * @param confidence Confidence score
     * @param notes Note opzionali
     */
    suspend fun saveFavorite(
        originalText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String,
        confidence: Float = 1.0f,
        notes: String = ""
    ) {
        try {
            val favorite = FavoriteTranslation(
                id = UUID.randomUUID().toString(),
                originalText = originalText,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                confidence = confidence,
                savedAt = System.currentTimeMillis(),
                notes = notes,
                isFavorite = true
            )

            context.favoritesDataStore.edit { preferences ->
                val currentList = getFavoritesList(preferences)
                val updatedList = currentList + favorite
                preferences[favoritesKey] = json.encodeToString(updatedList)
            }
        } catch (e: Exception) {
            throw Exception("Errore nel salvataggio del preferito: ${e.message}")
        }
    }

    /**
     * Recupera la lista di tutti i preferiti.
     *
     * @return Flow di lista preferiti, ordinata per data decrescente
     */
    fun getFavorites(): Flow<List<FavoriteTranslation>> {
        return context.favoritesDataStore.data.map { preferences ->
            try {
                val favoritesList = getFavoritesList(preferences)
                // Ordina per data decrescente (più recenti prima)
                favoritesList.sortedByDescending { it.savedAt }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Rimuove un preferito dalla lista.
     *
     * @param favoriteId ID del preferito da rimuovere
     */
    suspend fun removeFavorite(favoriteId: String) {
        try {
            context.favoritesDataStore.edit { preferences ->
                val currentList = getFavoritesList(preferences)
                val updatedList = currentList.filter { it.id != favoriteId }
                preferences[favoritesKey] = json.encodeToString(updatedList)
            }
        } catch (e: Exception) {
            throw Exception("Errore nella rimozione del preferito: ${e.message}")
        }
    }

    /**
     * Aggiorna le note di un preferito.
     *
     * @param favoriteId ID del preferito
     * @param notes Nuove note
     */
    suspend fun updateFavoriteNotes(favoriteId: String, notes: String) {
        try {
            context.favoritesDataStore.edit { preferences ->
                val currentList = getFavoritesList(preferences)
                val updatedList = currentList.map { favorite ->
                    if (favorite.id == favoriteId) {
                        favorite.copy(notes = notes)
                    } else {
                        favorite
                    }
                }
                preferences[favoritesKey] = json.encodeToString(updatedList)
            }
        } catch (e: Exception) {
            throw Exception("Errore nell'aggiornamento delle note: ${e.message}")
        }
    }

    /**
     * Verifica se una traduzione è già nei preferiti.
     *
     * @param originalText Testo originale
     * @param translatedText Testo tradotto
     * @return true se è già nei preferiti, false altrimenti
     */
    suspend fun isFavorite(originalText: String, translatedText: String): Boolean {
        return try {
            val preferences = context.favoritesDataStore.data.first()
            val favorites = getFavoritesList(preferences)
            favorites.any { it.originalText == originalText && it.translatedText == translatedText }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cancella tutti i preferiti.
     */
    suspend fun clearAllFavorites() {
        try {
            context.favoritesDataStore.edit { preferences ->
                preferences[favoritesKey] = json.encodeToString(emptyList<FavoriteTranslation>())
            }
        } catch (e: Exception) {
            throw Exception("Errore nella cancellazione dei preferiti: ${e.message}")
        }
    }

    /**
     * Conta il numero totale di preferiti.
     *
     * @return Flow del conteggio
     */
    fun getFavoritesCount(): Flow<Int> {
        return context.favoritesDataStore.data.map { preferences ->
            try {
                getFavoritesList(preferences).size
            } catch (e: Exception) {
                0
            }
        }
    }

    /**
     * Esporta i preferiti in formato JSON.
     *
     * @return Stringa JSON con tutti i preferiti
     */
    suspend fun exportFavoritesAsJson(): String {
        return try {
            val preferences = context.favoritesDataStore.data.first()
            val favorites = getFavoritesList(preferences)
            json.encodeToString(favorites)
        } catch (e: Exception) {
            "[]"
        }
    }

    /**
     * Importa preferiti da JSON.
     *
     * @param jsonString Stringa JSON con i preferiti
     */
    suspend fun importFavoritesFromJson(jsonString: String) {
        try {
            val importedFavorites = json.decodeFromString<List<FavoriteTranslation>>(jsonString)
            context.favoritesDataStore.edit { preferences ->
                val currentList = getFavoritesList(preferences)
                val mergedList = (currentList + importedFavorites).distinctBy { it.id }
                preferences[favoritesKey] = json.encodeToString(mergedList)
            }
        } catch (e: Exception) {
            throw Exception("Errore nell'importazione dei preferiti: ${e.message}")
        }
    }

    /**
     * Funzione helper per estrarre la lista di preferiti dalle preferenze.
     */
    private fun getFavoritesList(preferences: Preferences): List<FavoriteTranslation> {
        return try {
            val jsonString = preferences[favoritesKey] ?: return emptyList()
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
