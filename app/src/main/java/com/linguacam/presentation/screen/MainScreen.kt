package com.linguacam.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linguacam.domain.model.Language
import com.linguacam.presentation.viewmodel.MainUiState
import com.linguacam.presentation.viewmodel.MainViewModel
import timber.log.Timber

/**
 * Schermata principale dell'app LinguaCam.
 * Mostra l'interfaccia di traduzione con selezione lingue, overlay e pulsante preferiti.
 *
 * Design: Material 3 con animazioni fluide
 * Funzionalità: Selezione lingue, traduzione, overlay, preferiti
 *
 * Step 2026-07-29 (Context-Morph — P0 integration):
 * - Aggiunti callbacks [onOpenCamera], [onOpenFavorites], [onOpenProPlan] per la NavGraph.
 * - TopAppBar contiene ora: Settings + Favorites (star) + ProPlan (corona).
 * - FAB "Camera" visibile e accessibile (icona + label) per primary action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenCamera: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenProPlan: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLanguageSelector by remember { mutableStateOf(false) }
    var isSelectingSource by remember { mutableStateOf(true) }

    Timber.d("MainScreen rendering")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LinguaCam",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onOpenProPlan) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = "Upgrade a Pro",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onOpenFavorites) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Preferiti",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { Timber.d("Settings clicked") }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Impostazioni",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCamera,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                icon = {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null
                    )
                },
                text = { Text("Camera") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Language Selection Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Source Language
                    LanguageSelector(
                        label = "Da:",
                        selectedLanguage = uiState.sourceLanguage,
                        onLanguageSelected = { language ->
                            viewModel.setSourceLanguage(language)
                            showLanguageSelector = false
                        },
                        isOpen = showLanguageSelector && isSelectingSource,
                        onOpenChange = {
                            showLanguageSelector = it
                            isSelectingSource = true
                        },
                        availableLanguages = uiState.availableLanguages
                    )

                    // Swap Languages Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val temp = uiState.sourceLanguage
                                viewModel.setSourceLanguage(uiState.targetLanguage)
                                viewModel.setTargetLanguage(temp)
                            }
                        ) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = "Scambia lingue",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Target Language
                    LanguageSelector(
                        label = "A:",
                        selectedLanguage = uiState.targetLanguage,
                        onLanguageSelected = { language ->
                            viewModel.setTargetLanguage(language)
                            showLanguageSelector = false
                        },
                        isOpen = showLanguageSelector && !isSelectingSource,
                        onOpenChange = {
                            showLanguageSelector = it
                            isSelectingSource = false
                        },
                        availableLanguages = uiState.availableLanguages
                    )
                }
            }

            // Translation Result Section
            if (uiState.lastTranslation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Original Text
                        Text(
                            text = "Originale:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.lastTranslation!!.originalText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Translated Text
                        Text(
                            text = "Traduzione:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.lastTranslation!!.translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Confidence Score
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Confidenza: ${(uiState.lastTranslation!!.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = uiState.lastTranslation!!.confidence,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Favorite Button
                        FavoriteButton(
                            isFavorite = uiState.isFavorite,
                            isSaving = uiState.isSavingFavorite,
                            onFavoriteClick = {
                                if (uiState.isFavorite) {
                                    viewModel.removeFavorite()
                                } else {
                                    viewModel.saveFavorite()
                                }
                            }
                        )
                    }
                }
            }

            // Loading State
            if (uiState.isTranslating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Traduzione in corso...",
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Error State
            if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Favorite Message
            AnimatedVisibility(
                visible = uiState.favoriteMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState.favoriteMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = uiState.favoriteMessage!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Status Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                StatusIndicator(
                    isOnline = !uiState.isOfflineMode,
                    isCameraPermissionGranted = uiState.isCameraPermissionGranted
                )
            }
        }
    }
}

/**
 * Componente per la selezione della lingua.
 */
@Composable
private fun LanguageSelector(
    label: String,
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    availableLanguages: List<Language>
) {
    Column {
        Button(
            onClick = { onOpenChange(!isOpen) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
            Spacer(modifier = Modifier.width(8.dp))
            Text(selectedLanguage.name)
        }

        if (isOpen) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    availableLanguages.forEach { language ->
                        TextButton(
                            onClick = { onLanguageSelected(language) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(language.name)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pulsante Preferiti con animazione stella.
 */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    isSaving: Boolean,
    onFavoriteClick: () -> Unit
) {
    Button(
        onClick = onFavoriteClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
        )
    ) {
        Icon(
            imageVector = if (isFavorite) {
                Icons.Filled.Star
            } else {
                Icons.Outlined.Star
            },
            contentDescription = "Preferiti",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = if (isFavorite) {
                "Rimosso dai preferiti"
            } else {
                "Aggiungi ai preferiti"
            }
        )
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Indicatore di stato (online/offline, permessi).
 */
@Composable
private fun StatusIndicator(
    isOnline: Boolean,
    isCameraPermissionGranted: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Online Status
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isOnline) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        ) {
            Text(
                text = if (isOnline) "Online" else "Offline",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Camera Permission Status
        if (!isCameraPermissionGranted) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = "Camera non autorizzata",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}
