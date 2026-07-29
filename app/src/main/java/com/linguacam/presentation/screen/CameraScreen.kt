package com.linguacam.presentation.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.linguacam.data.camera.CameraManager
import com.linguacam.data.repository.OcrRepository
import com.linguacam.data.repository.RecognizedText
import com.linguacam.domain.model.Language
import com.linguacam.presentation.overlay.NoTextDetectedMessage
import com.linguacam.presentation.overlay.TranslationErrorMessage
import com.linguacam.presentation.overlay.TranslationLoadingIndicator
import com.linguacam.presentation.overlay.TranslationOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

/**
 * Schermata della fotocamera con overlay di traduzione in tempo reale.
 *
 * Step 2:
 * - CameraManager istanziato una sola volta (in LaunchedEffect) per evitare race con AndroidView.update
 * - Permesso fotocamera richiesto runtime (ActivityResultContracts.RequestPermission)
 * - cameraManager.startCamera() chiamato DOPO che il permesso è stato concesso
 *  e DOPO che il PreviewView è stato creato
 */
@Composable
fun CameraScreen(
    sourceLanguage: Language,
    targetLanguage: Language,
    onTranslation: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ocrRepository = remember { OcrRepository() }

    // Step 4: propaga il cambio lingua al OCR repository per cambiare script ML Kit.
    LaunchedEffect(sourceLanguage.code) {
        ocrRepository.setActiveScriptForLanguage(sourceLanguage.code)
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }
    var recognizedText by remember { mutableStateOf<RecognizedText?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var translatedTexts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var permissionGranted by remember { mutableStateOf(false) }

    Timber.d("CameraScreen rendering: $sourceLanguage -> $targetLanguage")

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            previewView?.let { pv -> cameraManager?.startCamera(pv) }
        } else {
            errorMessage = "Permesso fotocamera negato"
        }
    }

    // Inizializza il CameraManager (una sola volta) e richiedi il permesso.
    LaunchedEffect(Unit) {
        val manager = CameraManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            ocrRepository = ocrRepository,
            coroutineScope = CoroutineScope(Dispatchers.Main)
        )

        manager.onTextRecognized = { text ->
            recognizedText = text
            Timber.d("Testo riconosciuto: ${text.fullText}")
        }

        manager.onError = { error ->
            errorMessage = error
            Timber.e("Errore camera: $error")
        }

        cameraManager = manager
        // Richiesta permesso: quando arriva la risposta il launcher callback
        // provvede ad invocare startCamera(pv) se pv != null
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager?.stopCamera()
            ocrRepository.release()
            Timber.d("CameraScreen cleanup")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewView = this
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { /* nessun side-effect: manager già istanziato dal LaunchedEffect */ }
        )

        // Se il permesso arriva DOPO la creazione del PreviewView, avviamo la camera.
        LaunchedEffect(previewView, permissionGranted) {
            if (permissionGranted && previewView != null && cameraManager != null) {
                cameraManager?.startCamera(previewView!!)
            }
        }

        if (recognizedText != null && translatedTexts.isNotEmpty()) {
            TranslationOverlay(
                textBlocks = recognizedText!!.blocks,
                translatedTexts = translatedTexts,
                containerWidth = 1280,
                containerHeight = 720
            )
        }

        if (isTranslating) {
            TranslationLoadingIndicator()
        }

        if (errorMessage != null) {
            TranslationErrorMessage(errorMessage!!)
        }

        if (recognizedText == null && !isTranslating && errorMessage == null) {
            NoTextDetectedMessage()
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Da: ${sourceLanguage.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    "A: ${targetLanguage.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Chiudi",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (recognizedText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Testo riconosciuto (${recognizedText!!.blocks.size} blocchi):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        recognizedText!!.fullText.take(100) + if (recognizedText!!.fullText.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
