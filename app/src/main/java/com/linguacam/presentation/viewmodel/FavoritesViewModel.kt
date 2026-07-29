package com.linguacam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linguacam.data.repository.FavoritesRepository
import com.linguacam.domain.model.FavoriteTranslation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel per gestire lo stato dei preferiti.
 *
 * Responsabilità:
 * - Gestire lista preferiti
 * - Aggiungere/rimuovere dai preferiti
 * - Aggiornare note
 * - Gestire errori
 * - Esporre stato via StateFlow
 *
 * Architettura: Presentation Layer
 */
class FavoritesViewModel(private val favoritesRepository: FavoritesRepository) : ViewModel() {

    /**
     * Stato UI per i preferiti.
     */
    data class FavoritesState(
        val favorites: List<FavoriteTranslation> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val favoriteCount: Int = 0,
        val selectedFavorite: FavoriteTranslation? = null,
        val isEditingNotes: Boolean = false
    )

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        loadFavorites()
        loadFavoritesCount()
    }

    /**
     * Carica la lista di preferiti dal repository.
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                favoritesRepository.getFavorites().collect { favorites ->
                    _state.value = _state.value.copy(
                        favorites = favorites,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Errore nel caricamento dei preferiti: ${e.message}"
                )
            }
        }
    }

    /**
     * Carica il conteggio dei preferiti.
     */
    private fun loadFavoritesCount() {
        viewModelScope.launch {
            try {
                favoritesRepository.getFavoritesCount().collect { count ->
                    _state.value = _state.value.copy(favoriteCount = count)
                }
            } catch (e: Exception) {
                // Silent fail per il conteggio
            }
        }
    }

    /**
     * Salva una nuova traduzione nei preferiti.
     *
     * @param originalText Testo originale
     * @param translatedText Testo tradotto
     * @param sourceLanguage Lingua di origine
     * @param targetLanguage Lingua di destinazione
     * @param confidence Confidence score
     * @param notes Note opzionali
     */
    fun saveFavorite(
        originalText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String,
        confidence: Float = 1.0f,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                favoritesRepository.saveFavorite(
                    originalText = originalText,
                    translatedText = translatedText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    confidence = confidence,
                    notes = notes
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Errore nel salvataggio: ${e.message}"
                )
            }
        }
    }

    /**
     * Rimuove un preferito dalla lista.
     *
     * @param favoriteId ID del preferito da rimuovere
     */
    fun removeFavorite(favoriteId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                favoritesRepository.removeFavorite(favoriteId)
                _state.value = _state.value.copy(selectedFavorite = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Errore nella rimozione: ${e.message}"
                )
            }
        }
    }

    /**
     * Aggiorna le note di un preferito.
     *
     * @param favoriteId ID del preferito
     * @param notes Nuove note
     */
    fun updateFavoriteNotes(favoriteId: String, notes: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                favoritesRepository.updateFavoriteNotes(favoriteId, notes)
                _state.value = _state.value.copy(isEditingNotes = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Errore nell'aggiornamento: ${e.message}"
                )
            }
        }
    }

    /**
     * Seleziona un preferito per visualizzare i dettagli.
     *
     * @param favorite Preferito da selezionare
     */
    fun selectFavorite(favorite: FavoriteTranslation) {
        _state.value = _state.value.copy(selectedFavorite = favorite)
    }

    /**
     * Deseleziona il preferito corrente.
     */
    fun deselectFavorite() {
        _state.value = _state.value.copy(selectedFavorite = null)
    }

    /**
     * Abilita la modalità di editing delle note.
     */
    fun enableNotesEditing() {
        _state.value = _state.value.copy(isEditingNotes = true)
    }

    /**
     * Disabilita la modalità di editing delle note.
     */
    fun disableNotesEditing() {
        _state.value = _state.value.copy(isEditingNotes = false)
    }

    /**
     * Cancella tutti i preferiti.
     */
    fun clearAllFavorites() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                favoritesRepository.clearAllFavorites()
                _state.value = _state.value.copy(selectedFavorite = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Errore nella cancellazione: ${e.message}"
                )
            }
        }
    }

    /**
     * Esporta i preferiti in formato JSON.
     *
     * @return Stringa JSON con i preferiti
     */
    suspend fun exportFavorites(): String {
        return try {
            favoritesRepository.exportFavoritesAsJson()
        } catch (e: Exception) {
            "{}"
        }
    }

    /**
     * Importa preferiti da JSON.
     *
     * @param jsonString Stringa JSON
     */
    fun importFavorites(jsonString: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                favoritesRepository.importFavoritesFromJson(jsonString)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Errore nell'importazione: ${e.message}"
                )
            }
        }
    }

    /**
     * Cancella il messaggio di errore.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
