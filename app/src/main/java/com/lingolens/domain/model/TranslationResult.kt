package com.lingolens.domain.model

/**
 * Modello che rappresenta il risultato di una traduzione.
 * 
 * @param originalText Il testo originale riconosciuto dalla fotocamera
 * @param translatedText Il testo tradotto
 * @param sourceLanguage Codice della lingua di origine
 * @param targetLanguage Codice della lingua di destinazione
 * @param confidence Livello di confidenza del riconoscimento (0-1)
 * @param timestamp Timestamp della traduzione
 */
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val confidence: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)
