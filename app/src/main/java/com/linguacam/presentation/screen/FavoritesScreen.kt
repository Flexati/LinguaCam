package com.linguacam.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linguacam.domain.model.FavoriteTranslation
import com.linguacam.presentation.viewmodel.FavoritesViewModel

/**
 * Schermata per visualizzare e gestire i preferiti.
 *
 * Funzionalità:
 * - Visualizzazione lista preferiti ordinata per data
 * - Visualizzazione dettagli preferito
 * - Aggiunta/rimozione dai preferiti
 * - Modifica note
 * - Cancellazione batch
 *
 * Design: Material 3 con animazioni fluide
 */
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onFavoriteSelected: (FavoriteTranslation) -> Unit = {},
    onOpenCamera: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        FavoritesHeader(
            favoriteCount = state.favoriteCount,
            onClearAll = { viewModel.clearAllFavorites() }
        )

        // Error Message
        AnimatedVisibility(visible = state.error != null) {
            ErrorBanner(
                message = state.error ?: "",
                onDismiss = { viewModel.clearError() }
            )
        }

        // Content
        if (state.isLoading) {
            LoadingIndicator()
        } else if (state.favorites.isEmpty()) {
            EmptyFavoritesMessage(onOpenCamera = onOpenCamera)
        } else {
            FavoritesList(
                favorites = state.favorites,
                onFavoriteClick = { favorite ->
                    viewModel.selectFavorite(favorite)
                    onFavoriteSelected(favorite)
                },
                onRemoveFavorite = { favoriteId ->
                    viewModel.removeFavorite(favoriteId)
                }
            )
        }

        // Detail Panel
        AnimatedVisibility(visible = state.selectedFavorite != null) {
            state.selectedFavorite?.let { favorite ->
                FavoriteDetailPanel(
                    favorite = favorite,
                    isEditingNotes = state.isEditingNotes,
                    onEditNotes = { viewModel.enableNotesEditing() },
                    onSaveNotes = { newNotes ->
                        viewModel.updateFavoriteNotes(favorite.id, newNotes)
                    },
                    onClose = { viewModel.deselectFavorite() }
                )
            }
        }
    }
}

/**
 * Header della schermata preferiti.
 */
@Composable
private fun FavoritesHeader(
    favoriteCount: Int,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Preferiti",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$favoriteCount traduzioni salvate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (favoriteCount > 0) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Cancella tutti",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Lista dei preferiti.
 */
@Composable
private fun FavoritesList(
    favorites: List<FavoriteTranslation>,
    onFavoriteClick: (FavoriteTranslation) -> Unit,
    onRemoveFavorite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(favorites, key = { it.id }) { favorite ->
            FavoriteItem(
                favorite = favorite,
                onClick = { onFavoriteClick(favorite) },
                onRemove = { onRemoveFavorite(favorite.id) }
            )
        }
    }
}

/**
 * Item singolo nella lista preferiti.
 */
@Composable
private fun FavoriteItem(
    favorite: FavoriteTranslation,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = favorite.getListItemText(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(
                    text = favorite.getListItemSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (favorite.notes.isNotEmpty()) {
                    Text(
                        text = "Note: ${favorite.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Rimuovi",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Pannello dettagli preferito.
 */
@Composable
private fun FavoriteDetailPanel(
    favorite: FavoriteTranslation,
    isEditingNotes: Boolean,
    onEditNotes: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onClose: () -> Unit
) {
    var notesText by remember { mutableStateOf(favorite.notes) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dettagli",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = "Chiudi"
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Original Text
            Text(
                text = "Originale:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = favorite.originalText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Translated Text
            Text(
                text = "Traduzione:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = favorite.translatedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Notes Section
            Text(
                text = "Note:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (isEditingNotes) {
                TextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Aggiungi note...") },
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSaveNotes(notesText) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Salva")
                    }
                }
            } else {
                Text(
                    text = favorite.notes.ifEmpty { "Nessuna nota" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (favorite.notes.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = onEditNotes,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Modifica",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Modifica Note")
                }
            }
        }
    }
}

/**
 * Messaggio quando non ci sono preferiti.
 */
@Composable
private fun EmptyFavoritesMessage(onOpenCamera: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Nessun preferito",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Salva le traduzioni più utili per un accesso rapido offline",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(
            onClick = onOpenCamera,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apri fotocamera")
        }
    }
}

/**
 * Indicatore di caricamento.
 */
@Composable
private fun LoadingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = "Caricamento preferiti...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/**
 * Banner per messaggi di errore.
 */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Chiudi"
                )
            }
        }
    }
}
