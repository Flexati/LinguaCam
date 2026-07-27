package com.lingolens.data.repository

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestisce i modelli linguistici on-device di ML Kit (Translator).
 *
 * Step 3 — sostituzione completa della precedente simulazione.
 * Usa RemoteModelManager (API reale ML Kit) per scaricare/verificare/cancellare i modelli.
 *
 * Espone:
 * - installedLanguageCodes (StateFlow): insieme dei codici ISO delle lingue effettivamente
 *   presenti sul device (verificato via RemoteModelManager).
 * - downloadState (StateFlow): stato corrente del download in corso.
 * - downloadLanguageModel / deleteLanguageModel / refreshInstalledLanguages: azioni concrete.
 */
class LanguageModelRepository {

    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()

    private val _installedLanguageCodes = MutableStateFlow<Set<String>>(emptySet())
    val installedLanguageCodes: StateFlow<Set<String>> = _installedLanguageCodes.asStateFlow()

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    /** Protegge da download concorrenti: solo uno alla volta. */
    private val activeDownload = AtomicReference<String?>(null)

    /**
     * Verifica effettivamente — interrogando RemoteModelManager — quali modelli
     * sono presenti sul device. Aggiorna lo StateFlow interno.
     */
    suspend fun refreshInstalledLanguages(): Set<String> = withContext(Dispatchers.IO) {
        val present = mutableSetOf<String>()
        for ((iso, mlKitLang) in SUPPORTED_MAP) {
            val model = Translation.getModel(mlKitLang)
            val downloaded = remoteModelManager.isModelDownloaded(model).await()
            if (downloaded) present += iso
        }
        _installedLanguageCodes.value = present
        Timber.d("Refresh installed: $present")
        present
    }

    /**
     * Scarica il modello per la lingua data.
     * Rispetta un singolo download alla volta (gli altri chiamano Result.failure).
     */
    suspend fun downloadLanguageModel(languageCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!activeDownload.compareAndSet(null, languageCode)) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Altro download già in corso: ${activeDownload.get()}"
                )
            )
        }
        try {
            _downloadState.value = ModelDownloadState.Downloading(0)
            val mlKit = SUPPORTED_MAP[languageCode]
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Lingua non supportata: $languageCode")
                )
            val model = Translation.getModel(mlKit)

            // Nessun vincolo di rete in v1.
            // Modello ML Kit ~15-25MB per lingua; in v1.1 valuteremo requireWifi().
            val conditions = DownloadConditions.Builder().build()

            remoteModelManager
                .download(model, conditions)
                .addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalBytes
                    val progress = if (total > 0) {
                        (taskSnapshot.bytesDownloaded * 100L / total).toInt()
                    } else 0
                    _downloadState.value = ModelDownloadState.Downloading(progress)
                }
                .addOnSuccessListener {
                    _installedLanguageCodes.value =
                        _installedLanguageCodes.value + languageCode
                    _downloadState.value = ModelDownloadState.Downloaded
                    Timber.d("Download model OK: $languageCode")
                }
                .addOnFailureListener { e ->
                    _downloadState.value = ModelDownloadState.Failed(
                        e.message ?: "Errore sconosciuto"
                    )
                    Timber.e(e, "Download model fallito: $languageCode")
                }
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "downloadLanguageModel error")
            _downloadState.value = ModelDownloadState.Failed(e.message ?: "Errore")
            Result.failure(e)
        } finally {
            activeDownload.set(null)
        }
    }

    suspend fun deleteLanguageModel(languageCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mlKit = SUPPORTED_MAP[languageCode]
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Lingua non supportata: $languageCode")
                )
            val model = Translation.getModel(mlKit)
            remoteModelManager.deleteDownloadedModel(model).await()
            _installedLanguageCodes.value =
                _installedLanguageCodes.value - languageCode
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "deleteLanguageModel error")
            Result.failure(e)
        }
    }

    fun isLanguageInstalled(languageCode: String): Boolean =
        _installedLanguageCodes.value.contains(languageCode)

    fun getInstalledLanguageCount(): Int = _installedLanguageCodes.value.size

    /**
     * Limite FREE_PLAN_MAX_LANGUAGES definito qui per retro-compatibilità con codice legacy.
     * Il valore reale è esposto dal BillingRepository (vedi step 1).
     */
    fun canInstallLanguageOnFreePlan(): Boolean =
        getInstalledLanguageCount() < FREE_PLAN_MAX_LANGUAGES

    companion object {
        private const val FREE_PLAN_MAX_LANGUAGES = 2

        /**
         * Mappa ISO 639-1 -> TranslateLanguage (ML Kit).
         * Tiene i 20 codici del MVP. Lingue non mappate restituiscono null
         * e il caller riceve un Result.failure.
         */
        private val SUPPORTED_MAP: Map<String, String> = mapOf(
            "it" to TranslateLanguage.ITALIAN,
            "en" to TranslateLanguage.ENGLISH,
            "es" to TranslateLanguage.SPANISH,
            "fr" to TranslateLanguage.FRENCH,
            "de" to TranslateLanguage.GERMAN,
            "pt" to TranslateLanguage.PORTUGUESE,
            "ru" to TranslateLanguage.RUSSIAN,
            "ja" to TranslateLanguage.JAPANESE,
            "zh" to TranslateLanguage.CHINESE,
            "ko" to TranslateLanguage.KOREAN,
            "ar" to TranslateLanguage.ARABIC,
            "hi" to TranslateLanguage.HINDI,
            "tr" to TranslateLanguage.TURKISH,
            "nl" to TranslateLanguage.DUTCH,
            "pl" to TranslateLanguage.POLISH,
            "sv" to TranslateLanguage.SWEDISH,
            "da" to TranslateLanguage.DANISH,
            "fi" to TranslateLanguage.FINISH,
            "el" to TranslateLanguage.GREEK,
            "cs" to TranslateLanguage.CZECH
        )
    }
}

private fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { e -> cont.resumeWithException(e) }
        addOnCanceledListener { cont.cancel() }
    }
