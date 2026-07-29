package com.linguacam.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.linguacam.data.repository.FavoritesRepository

/**
 * Factory per istanziare FavoritesViewModel con le sue dipendenze.
 *
 * Aggiunto 2026-07-29 da Context-Morph per integrazione P0-4 (NavGraph).
 */
class FavoritesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            return FavoritesViewModel(
                favoritesRepository = FavoritesRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
