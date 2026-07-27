package com.lingolens.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Repository che gestisce le preferenze dell'app utilizzando DataStore.
 * 
 * Responsabilità:
 * - Tracciare se l'onboarding è stato completato
 * - Salvare preferenze utente
 * - Gestire lo stato di primo avvio
 */

private const val PREFERENCES_NAME = "lingolens_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME
)

class PreferencesRepository(private val context: Context) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }
    
    /**
     * Flow che emette true se l'onboarding è stato completato.
     */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }
    
    /**
     * Flow che emette true se è il primo avvio dell'app.
     */
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH] ?: true
    }
    
    /**
     * Marca l'onboarding come completato.
     */
    suspend fun markOnboardingCompleted() {
        try {
            context.dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETED] = true
                preferences[FIRST_LAUNCH] = false
            }
            Timber.d("Onboarding marked as completed")
        } catch (e: Exception) {
            Timber.e(e, "Errore nel salvataggio delle preferenze")
        }
    }
    
    /**
     * Resetta le preferenze (per testing).
     */
    suspend fun resetPreferences() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.d("Preferences reset")
        } catch (e: Exception) {
            Timber.e(e, "Errore nel reset delle preferenze")
        }
    }
}
