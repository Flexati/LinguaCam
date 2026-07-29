package com.linguacam.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.linguacam.data.repository.PreferencesRepository

/**
 * Factory per istanziare OnboardingViewModel con PreferencesRepository.
 *
 * Aggiunto 2026-07-29 da Context-Morph per integrazione P0-3 (NavGraph onboarding).
 */
class OnboardingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            return OnboardingViewModel(
                preferencesRepository = PreferencesRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
