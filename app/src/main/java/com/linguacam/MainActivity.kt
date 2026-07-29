package com.linguacam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.linguacam.data.repository.PreferencesRepository
import com.linguacam.presentation.LocalBillingRepository
import com.linguacam.presentation.LinguaCamNavGraph
import com.linguacam.ui.theme.LinguaCamTheme

/**
 * Entry point di LinguaCam.
 *
 * Step 2026-07-29 — Context-Morph (P0 integration):
 * - Legge `PreferencesRepository.isOnboardingCompleted` (Flow) per decidere
 *   la start destination della NavGraph: "onboarding" oppure "home".
 * - Mostra uno splash minimo (CircularProgressIndicator) durante la prima lettura.
 * - Delega la gestione di tutte le rotte a `LinguaCamNavGraph`.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingRepo = (application as LinguaCamApp).billingRepository
        val preferencesRepo = PreferencesRepository(applicationContext)

        setContent {
            LinguaCamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalBillingRepository provides billingRepo) {
                        // Osserva se l'onboarding è già stato completato.
                        val isOnboardingCompleted by preferencesRepo.isOnboardingCompleted
                            .collectAsState(initial = null)

                        if (isOnboardingCompleted == null) {
                            // Prima lettura in corso (DataStore async): splash leggero.
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // false → onboarding, true → home.
                            val startDestination = if (isOnboardingCompleted == true) {
                                "home"
                            } else {
                                "onboarding"
                            }
                            LinguaCamRoot(
                                startDestination = startDestination,
                                appContext = applicationContext,
                                billingRepository = billingRepo
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wrapper per montare la NavGraph con il NavController corretto.
 * Estratto per ridurre la complessità cognitiva di MainActivity.
 */
@Composable
private fun LinguaCamRoot(
    startDestination: String,
    appContext: android.content.Context,
    billingRepository: com.linguacam.data.repository.BillingRepositoryAPI
) {
    val navController = rememberNavController()
    LinguaCamNavGraph(
        navController = navController,
        appContext = appContext,
        billingRepository = billingRepository,
        startDestination = startDestination
    )
}
