package com.example.purchaseregister.service

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object MLKitOCRService {

    private lateinit var recognizer: TextRecognizer

    fun initialize() {
        if (!::recognizer.isInitialized) {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    fun close() {
        if (::recognizer.isInitialized) {
            recognizer.close()
        }
    }

    fun analyzeInvoice(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        initialize()

        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                try {
                    val textoExtraido = visionText.text
                    Log.d("MLKIT_OCR", "Texto extraído:\n$textoExtraido")

                    val jsonResult = IntelligentReceiptParser.parseReceipt(textoExtraido)

                    Log.d("MLKIT_OCR", "JSON generado: $jsonResult")
                    onSuccess(jsonResult)
                } catch (e: Exception) {
                    Log.e("MLKIT_OCR", "Error procesando texto: ${e.message}", e)
                    onError("Error procesando OCR: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MLKIT_OCR", "Error en OCR: ${e.message}", e)
                onError("Error en OCR: ${e.message}")
            }
    }
}