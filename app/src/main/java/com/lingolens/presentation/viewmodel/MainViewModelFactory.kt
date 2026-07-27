package com.lingolens.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lingolens.data.repository.FavoritesRepository
import com.lingolens.data.repository.LanguageModelRepository
import com.lingolens.data.repository.OcrRepository
import com.lingolens.data.repository.TranslationRepository

/**
 * Factory per istanziare MainViewModel con le sue dipendenze.
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
