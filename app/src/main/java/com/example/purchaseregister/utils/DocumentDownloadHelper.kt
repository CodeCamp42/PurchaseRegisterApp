package com.example.purchaseregister.utils

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentDownloadHelper {
    fun saveBase64ToFile(
        context: Context,
        base64Content: String,
        fileName: String
    ): File? {
        return try {
            val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(decodedBytes)
            }

            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = android.net.Uri.fromFile(file)
            context.sendBroadcast(mediaScanIntent)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun processDownloadResponse(
        context: Context,
        fileName: String,
        fileContentBase64: String,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val file = saveBase64ToFile(context, fileContentBase64, fileName)

            if (file != null) {
                onSuccess(file.absolutePath)
                Toast.makeText(
                    context,
                    "✅ Archivo guardado: $fileName",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                onError("Error al guardar el archivo")
            }
        } catch (e: Exception) {
            onError("Error al procesar descarga: ${e.message}")
        }
    }

    fun getExtensionFromType(documentType: String): String {
        return when (documentType.lowercase()) {
            "pdf" -> "pdf"
            "xml" -> "xml"
            "cdr" -> "zip"
            else -> "dat"
        }
    }

    fun generateFileName(
        baseName: String,
        documentType: String
    ): String {
        val extension = getExtensionFromType(documentType)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        return "${baseName}_${documentType.uppercase()}_$timestamp.$extension"
    }
}