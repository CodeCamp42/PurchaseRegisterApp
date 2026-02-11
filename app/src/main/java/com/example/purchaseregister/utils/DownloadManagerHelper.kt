package com.example.purchaseregister.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object DownloadManagerHelper {
    fun descargarDocumento(
        context: Context,
        numeroComprobante: String,
        tipo: String,
        baseUrl: String = "http://192.168.1.85:3043",
        onEnqueued: (Long) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val url = "$baseUrl/factura/descargar/$numeroComprobante/$tipo"
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val extension = when (tipo.lowercase()) {
                "pdf" -> "pdf"
                "xml" -> "xml"
                "cdr" -> "zip"
                else -> "dat"
            }

            val fileName = "${numeroComprobante}_${tipo.uppercase()}.$extension"
            val uri = Uri.parse(url)

            val request = DownloadManager.Request(uri)
                .setTitle("Descargando $tipo: $numeroComprobante")
                .setDescription("Descargando archivo $tipo de la factura")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)
            onEnqueued(downloadId)
        } catch (e: SecurityException) {
            onError("Error de permisos: ${e.message}")
        } catch (e: IllegalArgumentException) {
            onError("URL inválida: ${e.message}")
        } catch (e: Exception) {
            onError("Error al descargar: ${e.message}")
        }
    }
}