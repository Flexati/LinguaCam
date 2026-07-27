package com.lingolens.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.LinkedHashMap

/**
 * Repository per il riconoscimento del testo (OCR) via ML Kit.
 *
 * Step 4 — script-aware:
 * - Mappa ogni lingua ISO al TextRecognizer appropriato.
 *   Supportati: LATIN, CHINESE, JAPANESE (KOREAN e DEVANAGARI rimandati a v1.1).
 * - Cache LRU dei TextRecognizer per evitare allocazioni multiple.
 * - Russo e Greco: nessun modulo ML Kit dedicato → fallback LATIN (qualità ridotta accettata).
 * - Arabo/Hindi (non-LATIN non-latin-europee): fallback LATIN in v1.
 *   Per il futuro v1.1 aggiungeremo DEVANAGARI e KOREAN.
 */
data class RecognizedText(
    val fullText: String,
    val blocks: List<TextBlock>
)

data class TextBlock(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 1f
)

class OcrRepository {

    private val MAX_CACHED_RECOGNIZERS = 5

    /** Cache LRU di TextRecognizer istanziati per script. */
    private val recognizers: LinkedHashMap<String, TextRecognizer> =
        object : LinkedHashMap<String, TextRecognizer>(
            MAX_CACHED_RECOGNIZERS, 0.75f, true
        ) {
            override fun removeEldestEntry(
                eldest: Map.Entry<String, TextRecognizer>?
            ): Boolean {
                if (size > MAX_CACHED_RECOGNIZERS && eldest != null) {
                    eldest.value.close()
                    return true
                }
                return false
            }
        }

    /** Script attualmente attivo. Modificato da setActiveScriptForLanguage. */
    @Volatile
    private var activeScript: RecognizerScript = RecognizerScript.LATIN

    fun setActiveScriptForLanguage(languageCode: String) {
        val newScript = scriptForIso(languageCode)
        activeScript = newScript
        Timber.d("setActiveScriptForLanguage($languageCode) -> $newScript")
    }

    /**
     * Riconosce il testo in un'immagine, scegliendo automaticamente il recognizer
     * in base allo script attivo.
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<RecognizedText> =
        withContext(Dispatchers.Default) {
            return@withContext try {
                Timber.d("OCR start: ${bitmap.width}x${bitmap.height}, script=$activeScript")

                val recognizer = getOrCreateRecognizer(activeScript)
                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(image).await()

                val fullText = visionText.text
                val blocks = mutableListOf<TextBlock>()

                visionText.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        val boundingBox = line.boundingBox
                        if (boundingBox != null) {
                            blocks.add(
                                TextBlock(
                                    text = line.text,
                                    left = boundingBox.left.toFloat(),
                                    top = boundingBox.top.toFloat(),
                                    right = boundingBox.right.toFloat(),
                                    bottom = boundingBox.bottom.toFloat()
                                )
                            )
                        }
                    }
                }

                Timber.d("OCR done: ${blocks.size} blocchi")
                Result.success(RecognizedText(fullText, blocks))
            } catch (e: Exception) {
                Timber.e(e, "OCR error")
                Result.failure(e)
            }
        }

    /**
     * Restituisce o istanzia un TextRecognizer per lo script richiesto.
     * La cache tiene fino a MAX_CACHED_RECOGNIZERS istanze.
     */
    private fun getOrCreateRecognizer(script: RecognizerScript): TextRecognizer =
        synchronized(recognizers) {
            recognizers.getOrPut(script.tag) { createRecognizer(script) }
        }

    private fun createRecognizer(script: RecognizerScript): TextRecognizer {
        return when (script) {
            RecognizerScript.LATIN -> {
                // modulo text-recognition (LATIN) - già dichiarato
                TextRecognition.getClient(
                    com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                )
            }
            RecognizerScript.CHINESE -> {
                // modulo opzionale: com.google.mlkit:text-recognition-chinese
                TextRecognition.getClient(
                    com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions.Builder().build()
                )
            }
            RecognizerScript.JAPANESE -> {
                // modulo opzionale: com.google.mlkit:text-recognition-japanese
                TextRecognition.getClient(
                    com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions.Builder().build()
                )
            }
            RecognizerScript.KOREAN,
            RecognizerScript.DEVANAGARI -> {
                // Non abilitati in v1.0 (dipendenze extra non aggiunte). Fallback LATIN.
                Timber.w("Script $script non abilitato in v1.0, fallback LATIN")
                TextRecognition.getClient(
                    com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                )
            }
        }
    }

    /**
     * Mappa ISO 639-1 -> script ML Kit richiesto.
     * Lingue non mappate cadono su LATIN.
     */
    private fun scriptForIso(code: String): RecognizerScript {
        return when (code.lowercase()) {
            "ja" -> RecognizerScript.JAPANESE
            "zh" -> RecognizerScript.CHINESE
            "ko" -> RecognizerScript.KOREAN
            "hi" -> RecognizerScript.DEVANAGARI
            "it", "en", "es", "fr", "de", "pt",
            "nl", "pl", "sv", "da", "fi", "cs",
            "tr", "ru", "el", "ar" -> RecognizerScript.LATIN
            else -> RecognizerScript.LATIN
        }
    }

    /**
     * Rilascia tutti i TextRecognizer cached. Da chiamare in onCleared() o onDispose().
     */
    fun release() {
        synchronized(recognizers) {
            recognizers.values.forEach { it.close() }
            recognizers.clear()
        }
    }

    /** Enum degli script supportati. */
    private enum class RecognizerScript(val tag: String) {
        LATIN("latin"),
        CHINESE("chinese"),
        JAPANESE("japanese"),
        KOREAN("korean"),
        DEVANAGARI("devanagari")
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        addOnCanceledListener { continuation.cancel() }
    }
}
