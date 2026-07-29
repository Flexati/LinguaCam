package com.linguacam.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.linguacam.data.repository.FavoritesRepository
import com.linguacam.data.repository.LanguageModelRepository
import com.linguacam.data.repository.OcrRepository
import com.linguacam.data.repository.TranslationRepository

/**
 * Factory per istanziare MainViewModel con le sue dipendenze.
 *
 * Nota: per OnboardingViewModel e FavoritesViewModel esistono factory dedicate
 * (OnboardingViewModelFactory, FavoritesViewModelFactory) — questo file resta
 * responsabile SOLO di MainViewModel per chiarezza di responsabilità.
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                languageModelRepository = LanguageModelRepository(),
                translationRepository = TranslationRepository(),
                ocrRepository = OcrRepository(),
                favoritesRepository = FavoritesRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
