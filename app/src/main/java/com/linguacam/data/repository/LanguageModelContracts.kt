package com.linguacam.data.repository

/**
 * Stato del download di un modello on-device ML Kit.
 * Esposto via StateFlow per la UI.
 */
sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    object NotDownloaded : ModelDownloadState()
    data class Downloading(val progressPercent: Int) : ModelDownloadState()
    object Downloaded : ModelDownloadState()
    data class Failed(val reason: String) : ModelDownloadState()
}

/**
 * Astrazione della sorgente modelli ML Kit.
 * Permette di testare LanguageModelRepository contro un fake o
 * di sostituire l'implementazione senza toccare la presentation.
 */
interface LanguageModelSource {
    suspend fun isDownloaded(languageCode: String): Boolean
    suspend fun download(languageCode: String): Result<Unit>
    suspend fun delete(languageCode: String): Result<Unit>
}
