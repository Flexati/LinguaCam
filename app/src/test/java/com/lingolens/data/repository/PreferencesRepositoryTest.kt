package com.lingolens.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test per PreferencesRepository.
 * 
 * Verifica:
 * - Salvataggio preferenze
 * - Lettura preferenze
 * - Reset preferenze
 * - Primo avvio tracking
 */
@ExperimentalCoroutinesApi
class PreferencesRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var repository: PreferencesRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = PreferencesRepository(context)
    }
    
    @After
    fun tearDown() = runTest {
        repository.resetPreferences()
    }
    
    @Test
    fun testIsFirstLaunchInitially() = runTest {
        // Al primo avvio, isFirstLaunch dovrebbe essere true
        val isFirstLaunch = repository.isFirstLaunch.first()
        assertTrue(isFirstLaunch)
    }
    
    @Test
    fun testMarkOnboardingCompleted() = runTest {
        // Marca onboarding come completato
        repository.markOnboardingCompleted()
        
        val isCompleted = repository.isOnboardingCompleted.first()
        assertTrue(isCompleted)
        
        val isFirstLaunch = repository.isFirstLaunch.first()
        assertFalse(isFirstLaunch)
    }
    
    @Test
    fun testResetPreferences() = runTest {
        // Marca come completato, poi resetta
        repository.markOnboardingCompleted()
        repository.resetPreferences()
        
        val isCompleted = repository.isOnboardingCompleted.first()
        assertFalse(isCompleted)
        
        val isFirstLaunch = repository.isFirstLaunch.first()
        assertTrue(isFirstLaunch)
    }
    
    @Test
    fun testPersistenceAcrossInstances() = runTest {
        // Verifica che le preferenze persistono tra istanze
        repository.markOnboardingCompleted()
        
        // Crea una nuova istanza
        val newRepository = PreferencesRepository(context)
        
        val isCompleted = newRepository.isOnboardingCompleted.first()
        assertTrue(isCompleted)
    }
}
