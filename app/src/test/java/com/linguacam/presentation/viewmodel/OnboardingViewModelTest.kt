package com.linguacam.presentation.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.linguacam.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test unitari per OnboardingViewModel.
 *
 * Verifica:
 * - Navigazione tra step
 * - Progress calculation
 * - Skip functionality
 * - Completion state
 *
 * Aggiornato 2026-07-29 (Context-Morph — P0-3):
 * - OnboardingViewModel ora richiede PreferencesRepository nel costruttore.
 * - In test usiamo un PreferencesRepository reale con ApplicationProvider context
 *   (DataStore Preferences è disponibile anche in JVM unit-test).
 */
@ExperimentalCoroutinesApi
class OnboardingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var appContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appContext = ApplicationProvider.getApplicationContext()
        preferencesRepository = PreferencesRepository(appContext)
        viewModel = OnboardingViewModel(preferencesRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testInitialState() {
        // Verifica stato iniziale
        val state = viewModel.state.value
        assertEquals(state.currentStep, 0)
        assertEquals(state.totalSteps, 4)
        assertFalse(state.isCompleted)
        assertTrue(state.canSkip)
    }
    
    @Test
    fun testNextStep() = runTest {
        // Verifica avanzamento tra step
        viewModel.nextStep()
        var state = viewModel.state.value
        assertEquals(state.currentStep, 1)
        
        viewModel.nextStep()
        state = viewModel.state.value
        assertEquals(state.currentStep, 2)
    }
    
    @Test
    fun testPreviousStep() = runTest {
        // Verifica ritorno al passo precedente
        viewModel.nextStep()
        viewModel.nextStep()
        
        viewModel.previousStep()
        var state = viewModel.state.value
        assertEquals(state.currentStep, 1)
        
        viewModel.previousStep()
        state = viewModel.state.value
        assertEquals(state.currentStep, 0)
    }
    
    @Test
    fun testProgressCalculation() = runTest {
        // Verifica calcolo della progress bar
        var state = viewModel.state.value
        assertEquals(state.progress, 0.25f) // Step 0 di 4
        
        viewModel.nextStep()
        state = viewModel.state.value
        assertEquals(state.progress, 0.5f) // Step 1 di 4
        
        viewModel.nextStep()
        state = viewModel.state.value
        assertEquals(state.progress, 0.75f) // Step 2 di 4
    }
    
    @Test
    fun testIsLastStep() = runTest {
        // Verifica riconoscimento dell'ultimo step
        var state = viewModel.state.value
        assertFalse(state.isLastStep)
        
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        state = viewModel.state.value
        assertTrue(state.isLastStep)
    }
    
    @Test
    fun testSkipOnboarding() = runTest {
        // Verifica skip
        viewModel.skipOnboarding()
        val state = viewModel.state.value
        assertTrue(state.isCompleted)
    }
    
    @Test
    fun testCompleteOnboarding() = runTest {
        // Verifica completamento
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep() // Ultimo step, dovrebbe completare
        
        val state = viewModel.state.value
        assertTrue(state.isCompleted)
    }
    
    @Test
    fun testResetOnboarding() = runTest {
        // Verifica reset
        viewModel.skipOnboarding()
        viewModel.resetOnboarding()
        
        val state = viewModel.state.value
        assertEquals(state.currentStep, 0)
        assertFalse(state.isCompleted)
    }
    
    @Test
    fun testCannotGoBeforeFirstStep() = runTest {
        // Verifica che non si possa andare prima del primo step
        viewModel.previousStep()
        val state = viewModel.state.value
        assertEquals(state.currentStep, 0)
    }
    
    @Test
    fun testCannotGoAfterLastStep() = runTest {
        // Verifica che non si possa andare oltre l'ultimo step senza completare
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep() // Completa
        
        val state = viewModel.state.value
        assertTrue(state.isCompleted)
    }
}
