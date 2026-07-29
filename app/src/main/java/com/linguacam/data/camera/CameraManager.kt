package com.linguacam.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.linguacam.data.repository.OcrRepository
import com.linguacam.data.repository.RecognizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Gestisce il ciclo di vita della fotocamera e l'analisi dei frame in tempo reale.
 *
 * Step 2:
 * - Conversione ImageProxy (YUV_420_888) -> Bitmap ARGB_8888 via NV21 + YuvImage
 * - Rispetto di rowStride / pixelStride per i tre piani YUV
 * - Rotazione del bitmap secondo imageProxy.imageInfo.rotationDegrees
 * - Fallback FRONT camera se BACK non disponibile
 * - processFrame: tolleranza a frame non-YUV (non crash)
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val ocrRepository: OcrRepository,
    private val coroutineScope: CoroutineScope
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var previewView: PreviewView? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    var onTextRecognized: ((RecognizedText) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun startCamera(previewView: PreviewView) {
        this.previewView = previewView

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                Timber.d("Camera inizializzata con successo")
            } catch (e: Exception) {
                Timber.e(e, "Errore nell'inizializzazione della camera")
                onError?.invoke("Errore camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()
            .also {
                it.setSurfaceProvider(previewView?.surfaceProvider)
            }

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processFrame(imageProxy)
                }
            }

        // Tentativo primario: BACK camera
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            Timber.d("Camera use cases legati con successo (BACK)")
        } catch (e: Exception) {
            // Fallback: FRONT camera
            Timber.w(e, "BACK camera non disponibile, provo FRONT")
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
                Timber.d("Camera use cases legati con successo (FRONT fallback)")
            } catch (e2: Exception) {
                Timber.e(e2, "Errore anche nel fallback FRONT")
                onError?.invoke("Errore binding camera: ${e2.message}")
            }
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val bitmap = try {
                imageProxyToBitmap(imageProxy)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "frame ignorato per formato non supportato")
                return
            }

            // Esegue il riconoscimento del testo in background
            coroutineScope.launch(Dispatchers.Default) {
                val result = ocrRepository.recognizeText(bitmap)
                result.onSuccess { recognizedText ->
                    if (recognizedText.fullText.isNotEmpty()) {
                        onTextRecognized?.invoke(recognizedText)
                    }
                }.onFailure { error ->
                    Timber.e(error, "Errore nel riconoscimento del testo")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Errore nel processing del frame")
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Converte ImageProxy (formato YUV_420_888 standard di CameraX ImageAnalysis)
     * in un Bitmap ARGB_8888 leggibile da ML Kit.
     *
     * Strategia:
     * - Estrae i tre piani Y, U, V dai ByteBuffer rispettando rowStride/pixelStride.
     * - Costruisce un array NV21 richiesto da android.graphics.YuvImage.
     * - Decodifica JPEG -> Bitmap.
     * - Ruota il bitmap secondo imageInfo.rotationDegrees.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        require(imageProxy.format == ImageFormat.YUV_420_888) {
            "Formato non supportato: ${imageProxy.format}; atteso YUV_420_888"
        }

        val width = imageProxy.width
        val height = imageProxy.height
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]

        val yBuffer = yPlane.buffer.apply { rewind() }
        val uBuffer = uPlane.buffer.apply { rewind() }
        val vBuffer = vPlane.buffer.apply { rewind() }

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val ySize = width * height
        val nv21 = ByteArray(ySize + (ySize / 2))

        // Copia il piano Y con gestione rowStride/pixelStride
        var index = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                nv21[index++] = yBuffer.get(row * yRowStride + col * yPixelStride)
            }
        }

        // Copia U e V interleaved (NV21: VU VU VU ...)
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uByte = uBuffer.get(row * uRowStride + col * uPixelStride)
                val vByte = vBuffer.get(row * vRowStride + col * vPixelStride)
                nv21[index++] = uByte
                nv21[index++] = vByte
            }
        }

        // NV21 -> JPEG -> Bitmap
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, output)
        val jpegBytes = output.toByteArray()
        var bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: error("Decodifica JPEG fallita")

        // Rotazione secondo orientamento device
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            bitmap = rotated
        }
        return bitmap
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            Timber.d("Camera fermata")
        } catch (e: Exception) {
            Timber.e(e, "Errore nello stop della camera")
        }
    }
}
