package com.linguacam.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linguacam.data.repository.TextBlock
import timber.log.Timber

/**
 * Sistema di overlay che visualizza le traduzioni sopra il testo riconosciuto.
 *
 * Responsabilità:
 * - Posizionare correttamente il testo tradotto sopra quello originale
 * - Gestire la rotazione e l'inclinazione del testo
 * - Fornire feedback visivo durante la traduzione
 */

/**
 * Composable che renderizza l'overlay di traduzione.
 *
 * **Coordinate responsive** (2026-07-29 — Context-Morph):
 * - I [TextBlock] di ML Kit hanno coordinate in pixel relativi al frame analizzato.
 * - [containerWidth] e [containerHeight] sono le dimensioni REALI del Box che
 *   contiene l'overlay, espresse in pixel (px → poi convertite in dp).
 * - Le coordinate dell'overlay vengono calcolate come rapporto:
 *   `left = (block.left / frameWidth) * containerWidth`
 *   In questo modo l'overlay si scala correttamente su qualsiasi risoluzione
 *   camera/preview, eliminando i magic number hardcoded.
 *
 * @param textBlocks Lista dei blocchi di testo riconosciuti con coordinate in pixel
 * @param translatedTexts Mappa da testo originale a testo tradotto
 * @param containerWidth Larghezza del container in pixel (px)
 * @param containerHeight Altezza del container in pixel (px)
 * @param frameWidth Larghezza del frame a cui le coordinate di ML Kit si riferiscono.
 *                  Default 1280f per retrocompat con il vecchio contratto hardcoded.
 * @param frameHeight Altezza del frame a cui le coordinate di ML Kit si riferiscono.
 *                  Default 720f per retrocompat con il vecchio contratto hardcoded.
 */
@Composable
fun TranslationOverlay(
    textBlocks: List<TextBlock>,
    translatedTexts: Map<String, String>,
    containerWidth: Float,
    containerHeight: Float,
    frameWidth: Float = 1280f,
    frameHeight: Float = 720f
) {
    // Guardia: se le dimensioni non sono ancora note, non renderizzare nulla.
    if (containerWidth <= 0f || containerHeight <= 0f || frameWidth <= 0f || frameHeight <= 0f) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        textBlocks.forEach { block ->
            val translatedText = translatedTexts[block.text] ?: block.text

            // Coordinate RELATIVE al frame ML Kit, scalate al container reale.
            // Niente più magic number 1280×720 — funziona su qualsiasi preview.
            val leftDp = (block.left / frameWidth * containerWidth).dp
            val topDp = (block.top / frameHeight * containerHeight).dp
            val widthDp = ((block.right - block.left) / frameWidth * containerWidth).dp
            val heightDp = ((block.bottom - block.top) / frameHeight * containerHeight).dp

            Timber.d("Rendering overlay: '$translatedText' at (${leftDp.value}dp, ${topDp.value}dp)")

            Box(
                modifier = Modifier
                    .offset(leftDp, topDp)
                    .width(widthDp)
                    .height(heightDp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = translatedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Composable che mostra un indicatore di caricamento durante la traduzione.
 */
@Composable
fun TranslationLoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Traduzione in corso...",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Composable che mostra un messaggio di errore.
 */
@Composable
fun TranslationErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Composable che mostra un messaggio quando nessun testo è riconosciuto.
 */
@Composable
fun NoTextDetectedMessage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
                .fillMaxWidth(0.8f)
        ) {
            Text(
                "Nessun testo rilevato. Inquadra un cartello o un menu.",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Estensione per CircularProgressIndicator
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth
    )
}
