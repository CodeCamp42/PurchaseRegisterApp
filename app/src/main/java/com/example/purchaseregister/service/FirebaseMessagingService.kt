package com.example.purchaseregister.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.purchaseregister.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Verificar si el mensaje contiene una notificación
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Factura procesada",
                message = notification.body ?: "Los detalles de la factura están listos"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Este método se llama cuando se genera un nuevo token
        // Aquí debes enviar este token a tu backend
        sendTokenToBackend(token)
    }

    private fun sendTokenToBackend(token: String) {
        // TODO: Implementar llamada a tu backend para guardar el token
        // Asociar este token con el usuario actualmente logueado
        println("📱 Nuevo token FCM: $token")
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "facturas_channel"

        // Crear canal de notificación (necesario para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Facturas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de facturas procesadas"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Crear intent para abrir la app cuando se toque la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Cambia por tu propio icono
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Mostrar la notificación
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}