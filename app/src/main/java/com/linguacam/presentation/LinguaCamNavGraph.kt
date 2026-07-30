package com.linguacam.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.linguacam.data.repository.BillingRepositoryAPI
import com.linguacam.presentation.screen.CameraScreen
import com.linguacam.presentation.screen.FavoritesScreen
import com.linguacam.presentation.screen.MainScreen
import com.linguacam.presentation.screen.OnboardingScreen
import com.linguacam.presentation.screen.ProPlanScreen
import com.linguacam.presentation.viewmodel.FavoritesViewModel
import com.linguacam.presentation.viewmodel.FavoritesViewModelFactory
import com.linguacam.presentation.viewmodel.MainViewModel
import com.linguacam.presentation.viewmodel.MainViewModelFactory
import com.linguacam.presentation.viewmodel.OnboardingViewModel
import com.linguacam.presentation.viewmodel.OnboardingViewModelFactory
import android.content.Context

/**
 * NavGraph centrale di LinguaCam.
 *
 * Step 2026-07-29 — Context-Morph (P0 integration):
 * - 5 rotte: onboarding / home / camera / favorites / proplan
 * - Bottom navigation bar SOLO su home (tab 3: Home, Camera, Favorites).
 * - ProPlan accessibile dalla TopAppBar della home (corona), full-screen paywall.
 * - Onboarding full-screen (no bottom bar), Camera full-screen (no bottom bar).
 *
 * Contratto:
 * - Il chiamante (MainActivity) deve fornire:
 *   1) il NavController già avviato a onboarding oppure home
 *   2) il BillingRepositoryAPI via LocalBillingRepository
 *   3) il Context applicativo per le factory ViewModel
 *
 * Convenzione rotte:
 * - "onboarding"  → OnboardingScreen (4 step, full-screen, no bottom bar)
 * - "home"        → MainScreen (FAB camera + topbar corona/star/settings)
 * - "camera"      → CameraScreen (full-screen, no bottom bar)
 * - "favorites"   → FavoritesScreen (full-screen, no bottom bar)
 * - "proplan"     → ProPlanScreen (full-screen, no bottom bar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinguaCamNavGraph(
    navController: NavHostController,
    appContext: Context,
    billingRepository: BillingRepositoryAPI,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // ─── Onboarding ────────────────────────────────────────────────
        composable("onboarding") {
            // Full-screen: nessun Scaffold/bottom bar.
            val onboardingVm: OnboardingViewModel = viewModel(
                factory = OnboardingViewModelFactory(appContext)
            )
            val state by onboardingVm.state.collectAsState()

            OnboardingScreen(
                state = state,
                onNextStep = { onboardingVm.nextStep() },
                onPreviousStep = { onboardingVm.previousStep() },
                onSkip = {
                    onboardingVm.skipOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onComplete = {
                    // markOnboardingCompleted() viene già chiamato dentro completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // ─── Home ──────────────────────────────────────────────────────
        composable("home") {
            val mainVm: MainViewModel = viewModel(
                factory = MainViewModelFactory(appContext)
            )

            MainScreen(
                viewModel = mainVm,
                onOpenCamera = { navController.navigate("camera") },
                onOpenFavorites = { navController.navigate("favorites") },
                onOpenProPlan = { navController.navigate("proplan") }
            )
        }

        // ─── Camera (full-screen) ──────────────────────────────────────
        composable("camera") {
            val mainVm: MainViewModel = viewModel(
                factory = MainViewModelFactory(appContext)
            )
            // Stato MainUiState osservato per leggere le lingue correnti.
            val uiState by mainVm.uiState.collectAsState()

            CameraScreen(
                sourceLanguage = uiState.sourceLanguage,
                targetLanguage = uiState.targetLanguage,
                onTranslation = { /* hook per future pipeline */ },
                onClose = { navController.popBackStack() }
            )
        }

        // ─── Favorites (full-screen) ───────────────────────────────────
        composable("favorites") {
            val favoritesVm: FavoritesViewModel = viewModel(
                factory = FavoritesViewModelFactory(appContext)
            )

            Scaffold(
                topBar = {
                    androidx.compose.material3.TopAppBar(
                        title = {
                            Text(
                                "Preferiti",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(
                                onClick = { navController.popBackStack() }
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Home,
                                    contentDescription = "Home"
                                )
                            }
                        },
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            ) { innerPadding ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    FavoritesScreen(
                        viewModel = favoritesVm,
                        onOpenCamera = { navController.navigate("camera") }
                    )
                }
            }
        }

        // ─── Pro Plan (full-screen) ────────────────────────────────────
        composable("proplan") {
            ProPlanScreen(
                billing = billingRepository,
                onClose = { navController.popBackStack() },
                onPurchaseComplete = { navController.popBackStack() }
            )
        }
    }
}
