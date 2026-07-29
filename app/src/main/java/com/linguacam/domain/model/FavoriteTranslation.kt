package com.linguacam.domain.model

import java.util.Date
import kotlinx.serialization.Serializable

/**
 * Modello dati per una traduzione salvata nei preferiti.
 *
 * Rappresenta una traduzione che l'utente ha scelto di salvare per accesso rapido offline.
 *
 * @property id Identificatore unico (UUID)
 * @property originalText Testo originale riconosciuto
 * @property translatedText Testo tradotto
 * @property sourceLanguage Codice lingua di origine (es: "it")
 * @property targetLanguage Codice lingua di destinazione (es: "en")
 * @property confidence Confidence score del riconoscimento (0.0 - 1.0)
 * @property savedAt Timestamp di quando è stato salvato
 * @property notes Note opzionali dell'utente
 * @property isFavorite Flag per indicare se è un preferito
 */
@Serializable
data class FavoriteTranslation(
    val id: String = "",
    val originalText: String = "",
    val translatedText: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val confidence: Float = 0f,
    val savedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isFavorite: Boolean = true
) {
    /**
     * Restituisce una descrizione leggibile della traduzione.
     *
     * Formato: "Italiano → Inglese: Ciao → Hello"
     */
    fun getDisplayText(sourceLanguageName: String, targetLanguageName: String): String {
        return "$sourceLanguageName → $targetLanguageName: $originalText → $translatedText"
    }

    /**
     * Restituisce la data formattata di salvataggio.
     */
    fun getFormattedDate(): String {
        val date = Date(savedAt)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }

    /**
     * Restituisce il testo da visualizzare nella lista.
     */
    fun getListItemText(): String {
        return "$originalText → $translatedText"
    }

    /**
     * Restituisce il sottotitolo da visualizzare nella lista.
     */
    fun getListItemSubtitle(): String {
        return "$sourceLanguage → $targetLanguage • ${getFormattedDate()}"
    }
}
