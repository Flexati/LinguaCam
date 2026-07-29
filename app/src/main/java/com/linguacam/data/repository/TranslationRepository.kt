package com.linguacam.data.repository

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.linguacam.domain.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository che gestisce la traduzione on-device utilizzando ML Kit.
 * 
 * Responsabilità:
 * - Creare e gestire le istanze di Translator per coppie di lingue
 * - Eseguire la traduzione in background thread
 * - Gestire gli errori di traduzione
 * - Rilasciare le risorse quando non più necessarie
 */
class TranslationRepository(
    private val languageModelRepository: LanguageModelRepository = LanguageModelRepository()
) {

    private val translators = mutableMapOf<String, Translator?>()

    /**
     * Traduce un testo da una lingua all'altra.
     *
     * @param text Il testo da tradurre
     * @param sourceLanguageCode Codice della lingua di origine (es. "it")
     * @param targetLanguageCode Codice della lingua di destinazione (es. "en")
     * @return TranslationResult con il testo tradotto
     */
    suspend fun translate(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): Result<TranslationResult> = withContext(Dispatchers.Default) {
        return@withContext try {
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Testo vuoto"))
            }

            // Step 3: verifica pre-flight che i modelli ML Kit per source e target
            // siano effettivamente scaricati. Senza questo check, translator.translate()
            // lancia IllegalStateException a runtime.
            if (!languageModelRepository.isLanguageInstalled(sourceLanguageCode)) {
                return@withContext Result.failure(
                    ModelNotDownloadedException(sourceLanguageCode)
                )
            }
            if (!languageModelRepository.isLanguageInstalled(targetLanguageCode)) {
                return@withContext Result.failure(
                    ModelNotDownloadedException(targetLanguageCode)
                )
            }

            Timber.d("Traduzione: '$text' da $sourceLanguageCode a $targetLanguageCode")
            
            // Ottiene o crea il translator per questa coppia di lingue
            val translator = getOrCreateTranslator(sourceLanguageCode, targetLanguageCode)
                ?: return@withContext Result.failure(
                    Exception("Impossibile creare il translator per $sourceLanguageCode -> $targetLanguageCode")
                )
            
            // Esegue la traduzione (operazione sincrona in ML Kit)
            val translatedText = translator.translate(text).await()
            
            val result = TranslationResult(
                originalText = text,
                translatedText = translatedText,
                sourceLanguage = sourceLanguageCode,
                targetLanguage = targetLanguageCode
            )
            
            Timber.d("Traduzione completata: '$translatedText'")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Errore durante la traduzione")
            Result.failure(e)
        }
    }
    
    /**
     * Ottiene o crea un translator per una coppia di lingue.
     */
    private fun getOrCreateTranslator(
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): Translator? {
        val key = "$sourceLanguageCode-$targetLanguageCode"

        // Cache hit: riusa Translator esistente (già non-null perché garantito al momento dell'inserimento).
        translators[key]?.let { return it }

        // Cache miss: crea nuovo Translator.
        val newTranslator: Translator? = try {
            val sourceLanguage: String? = mapMlKitLanguageCode(sourceLanguageCode)
            val targetLanguage: String? = mapMlKitLanguageCode(targetLanguageCode)

            if (sourceLanguage == null || targetLanguage == null) {
                Timber.w("Lingua non supportata: $sourceLanguageCode o $targetLanguageCode")
                null
            } else {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()
                Translation.getClient(options)
            }
        } catch (e: Exception) {
            Timber.e(e, "Errore nella creazione del translator")
            null
        }

        // Inserisci in cache SOLO se non-null (evita di cachare "fallimenti" che bloccherebbero retry).
        if (newTranslator != null) {
            translators[key] = newTranslator
        }
        return newTranslator
    }
    
    /**
     * Converte il codice ISO 639-1 al codice ML Kit.
     */
    private fun mapMlKitLanguageCode(code: String): String? {
        return when (code.lowercase()) {
            "it" -> TranslateLanguage.ITALIAN
            "en" -> TranslateLanguage.ENGLISH
            "es" -> TranslateLanguage.SPANISH
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "ru" -> TranslateLanguage.RUSSIAN
            "ja" -> TranslateLanguage.JAPANESE
            "zh" -> TranslateLanguage.CHINESE
            "ko" -> TranslateLanguage.KOREAN
            "ar" -> TranslateLanguage.ARABIC
            "hi" -> TranslateLanguage.HINDI
            "tr" -> TranslateLanguage.TURKISH
            "nl" -> TranslateLanguage.DUTCH
            "pl" -> TranslateLanguage.POLISH
            "sv" -> TranslateLanguage.SWEDISH
            "da" -> TranslateLanguage.DANISH
            "fi" -> TranslateLanguage.FINNISH
            "el" -> TranslateLanguage.GREEK
            "cs" -> TranslateLanguage.CZECH
            else -> null
        }
    }
    
    /**
     * Rilascia tutte le risorse dei translator.
     */
    fun release() {
        translators.values.forEach { translator ->
            translator.close()
        }
        translators.clear()
    }
}

// Estensione per convertire Task di ML Kit in suspend function
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine {
    continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}

/**
 * Eccezione restituita quando il modello ML Kit per la lingua data
 * non è stato scaricato. Distinta da generico IllegalStateException per
 * permettere alla UI di mostrare un messaggio specifico
 * ("Scarica il modello di XX per tradurre").
 */
class ModelNotDownloadedException(val languageCode: String) :
    Exception("Modello di traduzione non scaricato per: $languageCode")
