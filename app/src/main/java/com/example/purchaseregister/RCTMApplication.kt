package com.example.purchaseregister

import android.app.Application
import com.example.purchaseregister.api.RetrofitClient

class RCTMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar RetrofitClient con el contexto de la aplicación
        RetrofitClient.init(this)
    }
}