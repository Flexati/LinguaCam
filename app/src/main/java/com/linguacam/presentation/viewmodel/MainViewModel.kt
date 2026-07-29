package com.linguacam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linguacam.data.repository.LanguageModelRepository
import com.linguacam.data.repository.TranslationRepository
import com.linguacam.data.repository.OcrRepository
import com.linguacam.data.repository.FavoritesRepository
import com.linguacam.domain.model.Language
import com.linguacam.domain.model.TranslationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel che gestisce lo stato della schermata principale di traduzione.
 */
data class MainUiState(
    val sourceLanguage: Language = Language("it", "Italiano", "Italiano"),
    val targetLanguage: Language = Language("en", "Inglese", "English"),
    val isTranslating: Boolean = false,
    val lastTranslation: TranslationResult? = null,
    val error: String? = null,
    val isCameraPermissionGranted: Boolean = false,
    val isOfflineMode: Boolean = false,
    val availableLanguages: List<Language> = Language.SUPPORTED_LANGUAGES,
    val isFavorite: Boolean = false,
    val isSavingFavorite: Boolean = false,
    val favoriteMessage: String? = null
)

class MainViewModel(
    private val languageModelRepository: LanguageModelRepository,
    private val translationRepository: TranslationRepository,
    private val ocrRepository: OcrRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        Timber.d("MainViewModel inizializzato")
        // Step 3: sincronizza i modelli effettivamente presenti sul device con lo StateFlow.
        viewModelScope.launch {
            languageModelRepository.refreshInstalledLanguages()
        }
    }

    /**
     * Pre-flight: verifica che il modello ML Kit per la lingua sorgente sia installato.
     * Se non lo è, tenta un download silente (senza bloccare la UI).
     */
    fun ensureSourceLanguageModelReady() {
        val src = _uiState.value.sourceLanguage.code
        if (!languageModelRepository.isLanguageInstalled(src)) {
            Timber.w("Modello non installato per $src — tentativo download silente")
            viewModelScope.launch {
                languageModelRepository.downloadLanguageModel(src)
            }
        }
    }
    
    /**
     * Imposta la lingua di origine.
     */
    fun setSourceLanguage(language: Language) {
        Timber.d("Lingua di origine impostata a: ${language.code}")
        _uiState.value = _uiState.value.copy(sourceLanguage = language)
    }
    
    /**
     * Imposta la lingua di destinazione.
     */
    fun setTargetLanguage(language: Language) {
        Timber.d("Lingua di destinazione impostata a: ${language.code}")
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }

    /**
     * Scambia lingua sorgente e lingua di destinazione (alias comodo UX).
     */
    fun swapLanguages() {
        val state = _uiState.value
        _uiState.value = state.copy(
            sourceLanguage = state.targetLanguage,
            targetLanguage = state.sourceLanguage
        )
        Timber.d("Lingue scambiate")
    }

    /**
     * Imposta direttamente il TranslationResult corrente
     * (utile per test, oppure per iniezione da processi esterni come OCR pipeline).
     */
    fun setTranslationResult(result: TranslationResult) {
        _uiState.value = _uiState.value.copy(
            lastTranslation = result,
            isTranslating = false,
            error = null
        )
    }

    /**
     * Pulisce la traduzione corrente.
     */
    fun clearTranslationResult() {
        _uiState.value = _uiState.value.copy(lastTranslation = null)
    }

    /**
     * Alias comodo: setOnlineStatus(bool) = setOfflineMode(!bool).
     * Mantenuto per retro-compat.
     */
    fun setOnlineStatus(isOnline: Boolean) {
        setOfflineMode(!isOnline)
    }
    
    /**
     * Traduce un testo.
     */
    fun translateText(text: String) {
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Testo vuoto")
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isTranslating = true, error = null)
                
                val state = _uiState.value
                val result = translationRepository.translate(
                    text,
                    state.sourceLanguage.code,
                    state.targetLanguage.code
                )
                
                result.onSuccess { translation ->
                    _uiState.value = _uiState.value.copy(
                        lastTranslation = translation,
                        isTranslating = false
                    )
                    // Verifica se è già nei preferiti
                    checkIfFavorite()
                    Timber.d("Traduzione riuscita: ${translation.translatedText}")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isTranslating = false,
                        error = error.message ?: "Errore sconosciuto"
                    )
                    Timber.e(error, "Errore durante la traduzione")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranslating = false,
                    error = e.message ?: "Errore sconosciuto"
                )
                Timber.e(e, "Errore nel ViewModel")
            }
        }
    }
    
    /**
     * Aggiorna lo stato del permesso della fotocamera.
     */
    fun setCameraPermissionGranted(granted: Boolean) {
        Timber.d("Permesso fotocamera: $granted")
        _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted)
    }
    
    /**
     * Aggiorna lo stato offline.
     */
    fun setOfflineMode(isOffline: Boolean) {
        Timber.d("Modalità offline: $isOffline")
        _uiState.value = _uiState.value.copy(isOfflineMode = isOffline)
    }
    
    /**
     * Scarica un modello linguistico.
     */
    fun downloadLanguageModel(languageCode: String) {
        viewModelScope.launch {
            try {
                Timber.d("Inizio download del modello: $languageCode")
                val result = languageModelRepository.downloadLanguageModel(languageCode)
                result.onSuccess {
                    Timber.d("Download completato: $languageCode")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Errore download: ${error.message}"
                    )
                    Timber.e(error, "Errore download modello")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore: ${e.message}"
                )
                Timber.e(e, "Errore nel download")
            }
        }
    }

    /**
     * Salva la traduzione corrente nei preferiti.
     */
    fun saveFavorite(notes: String = "") {
        val translation = _uiState.value.lastTranslation
        if (translation == null) {
            _uiState.value = _uiState.value.copy(
                error = "Nessuna traduzione da salvare"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSavingFavorite = true,
                    error = null,
                    favoriteMessage = null
                )

                favoritesRepository.saveFavorite(
                    originalText = translation.originalText,
                    translatedText = translation.translatedText,
                    sourceLanguage = _uiState.value.sourceLanguage.code,
                    targetLanguage = _uiState.value.targetLanguage.code,
                    confidence = translation.confidence,
                    notes = notes
                )

                _uiState.value = _uiState.value.copy(
                    isSavingFavorite = false,
                    isFavorite = true,
                    favoriteMessage = "Aggiunto ai preferiti ✓"
                )
                Timber.d("Traduzione salvata nei preferiti")

                // Nascondi il messaggio dopo 2 secondi
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(favoriteMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingFavorite = false,
                    error = "Errore nel salvataggio: ${e.message}"
                )
                Timber.e(e, "Errore nel salvataggio del preferito")
            }
        }
    }

    /**
     * Verifica se la traduzione corrente è nei preferiti.
     */
    fun checkIfFavorite() {
        val translation = _uiState.value.lastTranslation
        if (translation == null) {
            _uiState.value = _uiState.value.copy(isFavorite = false)
            return
        }

        viewModelScope.launch {
            try {
                val isFav = favoritesRepository.isFavorite(
                    translation.originalText,
                    translation.translatedText
                )
                _uiState.value = _uiState.value.copy(isFavorite = isFav)
            } catch (e: Exception) {
                Timber.e(e, "Errore nel controllo del preferito")
            }
        }
    }

    /**
     * Rimuove la traduzione corrente dai preferiti.
     */
    fun removeFavorite() {
        val translation = _uiState.value.lastTranslation
        if (translation == null) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSavingFavorite = true,
                    error = null,
                    favoriteMessage = null
                )

                // Trova l'ID del preferito usando first() per terminare il Flow
                val list = favoritesRepository.getFavorites().first()
                val favorite = list.find {
                    it.originalText == translation.originalText &&
                    it.translatedText == translation.translatedText
                }
                if (favorite != null) {
                    favoritesRepository.removeFavorite(favorite.id)
                    _uiState.value = _uiState.value.copy(
                        isSavingFavorite = false,
                        isFavorite = false,
                        favoriteMessage = "Rimosso dai preferiti"
                    )
                    Timber.d("Preferito rimosso")

                    // Nascondi il messaggio dopo 2 secondi
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = _uiState.value.copy(favoriteMessage = null)
                } else {
                    _uiState.value = _uiState.value.copy(isSavingFavorite = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingFavorite = false,
                    error = "Errore nella rimozione: ${e.message}"
                )
                Timber.e(e, "Errore nella rimozione del preferito")
            }
        }
    }

    /**
     * Cancella il messaggio di feedback.
     */
    fun clearFavoriteMessage() {
        _uiState.value = _uiState.value.copy(favoriteMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        Timber.d("MainViewModel cleared")
        translationRepository.release()
        ocrRepository.release()
    }
}
