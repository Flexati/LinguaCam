package com.lingolens.domain.model

/**
 * Modello che rappresenta una lingua supportata da LingoLens.
 * 
 * @param code Codice ISO 639-1 della lingua (es. "it", "en", "es")
 * @param name Nome della lingua in italiano
 * @param nativeName Nome della lingua nella lingua stessa
 * @param isInstalled Se il modello ML Kit è installato localmente
 * @param downloadSize Dimensione del modello in MB (approssimativa)
 * @param isDownloading Se il download è in corso
 * @param downloadProgress Progresso del download (0-100)
 */
data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val isInstalled: Boolean = false,
    val downloadSize: Float = 0f,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0
) {
    companion object {
        // Lingue supportate da ML Kit Translation (core set per MVP)
        val SUPPORTED_LANGUAGES = listOf(
            Language("it", "Italiano", "Italiano"),
            Language("en", "Inglese", "English"),
            Language("es", "Spagnolo", "Español"),
            Language("fr", "Francese", "Français"),
            Language("de", "Tedesco", "Deutsch"),
            Language("pt", "Portoghese", "Português"),
            Language("ru", "Russo", "Русский"),
            Language("ja", "Giapponese", "日本語"),
            Language("zh", "Cinese", "中文"),
            Language("ko", "Coreano", "한국어"),
            Language("ar", "Arabo", "العربية"),
            Language("hi", "Hindi", "हिन्दी"),
            Language("tr", "Turco", "Türkçe"),
            Language("nl", "Olandese", "Nederlands"),
            Language("pl", "Polacco", "Polski"),
            Language("sv", "Svedese", "Svenska"),
            Language("da", "Danese", "Dansk"),
            Language("fi", "Finlandese", "Suomi"),
            Language("el", "Greco", "Ελληνικά"),
            Language("cs", "Ceco", "Čeština")
        )
    }
}
