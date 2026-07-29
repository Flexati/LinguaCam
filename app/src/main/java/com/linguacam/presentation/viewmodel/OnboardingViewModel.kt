package com.linguacam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linguacam.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel che gestisce lo stato del flusso onboarding.
 *
 * Esteso 2026-07-29 da Context-Morph: aggiunta dipendenza PreferencesRepository
 * per poter persistere lo stato di completamento onboarding.
 */
data class OnboardingState(
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val isCompleted: Boolean = false,
    val canSkip: Boolean = true
) {
    val progress: Float
        get() = (currentStep + 1) / totalSteps.toFloat()

    val isLastStep: Boolean
        get() = currentStep == totalSteps - 1
}

class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    /**
     * Passa al prossimo step dell'onboarding.
     */
    fun nextStep() {
        val currentState = _state.value
        if (currentState.currentStep < currentState.totalSteps - 1) {
            _state.value = currentState.copy(currentStep = currentState.currentStep + 1)
            Timber.d("Onboarding step: ${_state.value.currentStep + 1}/${currentState.totalSteps}")
        } else {
            completeOnboarding()
        }
    }

    /**
     * Torna al passo precedente.
     */
    fun previousStep() {
        val currentState = _state.value
        if (currentState.currentStep > 0) {
            _state.value = currentState.copy(currentStep = currentState.currentStep - 1)
            Timber.d("Onboarding step back: ${_state.value.currentStep + 1}/${currentState.totalSteps}")
        }
    }

    /**
     * Salta l'onboarding.
     */
    fun skipOnboarding() {
        Timber.d("Onboarding skipped")
        completeOnboarding()
    }

    /**
     * Completa l'onboarding e persiste lo stato.
     */
    private fun completeOnboarding() {
        _state.value = _state.value.copy(isCompleted = true)
        Timber.d("Onboarding completed")
        viewModelScope.launch {
            preferencesRepository.markOnboardingCompleted()
        }
    }

    /**
     * Resetta l'onboarding (per testing).
     */
    fun resetOnboarding() {
        _state.value = OnboardingState()
        Timber.d("Onboarding reset")
    }
}
