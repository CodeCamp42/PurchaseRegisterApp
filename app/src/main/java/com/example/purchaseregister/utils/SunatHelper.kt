package com.example.purchaseregister.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import android.util.Base64

// --- PERSISTENCIA SUNAT ---
object SunatPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_RUC = "sunat_ruc"
    private const val KEY_USER = "sunat_usuario"
    private const val KEY_TOKEN = "sunat_token"
    private const val KEY_COOKIES = "sunat_cookies"
    private const val KEY_CLAVE_SOL = "sunat_clave_sol"

    fun saveClaveSol(context: Context, claveSol: String) {
        try {
            // Cifrado simple Base64 de Android
            val encrypted = Base64.encodeToString(claveSol.toByteArray(), Base64.NO_WRAP)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CLAVE_SOL, encrypted).apply()
            println("🔐 Clave SOL guardada (cifrada)")
        } catch (e: Exception) {
            // Fallback: guardar sin cifrar (no recomendado en producción)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CLAVE_SOL, claveSol).apply()
            println("⚠️ Clave SOL guardada sin cifrar")
        }
    }

    // Obtener clave SOL descifrada
    fun getClaveSol(context: Context): String? {
        val encrypted = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CLAVE_SOL, null)

        return encrypted?.let {
            try {
                String(Base64.decode(it, Base64.NO_WRAP))
            } catch (e: Exception) {
                // Si no es Base64, devolver como está (texto plano)
                it
            }
        }
    }

    fun saveRuc(context: Context, ruc: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_RUC, ruc).apply()
    }

    fun saveUser(context: Context, usuario: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER, usuario).apply()
    }

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveCookies(context: Context, cookies: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_COOKIES, cookies).apply()
        println("🍪 Cookies guardadas (${cookies.length} chars)")
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    fun getRuc(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RUC, null)
    }

    fun getUser(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)
    }

    fun getCookies(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COOKIES, null)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RUC)
            .remove(KEY_USER)
            .remove(KEY_TOKEN)
            .remove(KEY_COOKIES)
            .apply()
        println("🧹 [SunatPrefs] Todas las credenciales limpiadas")
    }
}

// --- DIÁLOGO WEBVIEW SUNAT ---
@Composable
fun SunatLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Red)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                SunatWebView(onLoginSuccess = onLoginSuccess)
            }
        }
    )
}

@Composable
fun SunatWebView(onLoginSuccess: () -> Unit) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    procesarCookiesYTokens(context, view, url, onLoginSuccess)
                }
            }

            loadUrl(SUNAT_LOGIN_URL)
        }
    })
}

private fun procesarCookiesYTokens(
    context: Context,
    view: WebView?,
    url: String?,
    onLoginSuccess: () -> Unit
) {
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(url ?: "")

    if (!cookies.isNullOrEmpty()) {
        SunatPrefs.saveCookies(context, cookies)
        println("🍪 [SunatHelper] Cookies guardadas: ${cookies.take(100)}...")
    }

    val token = extractTokenFromCookies(cookies)
    token?.let {
        SunatPrefs.saveToken(context, it)
        println("🔑 [SunatHelper] Token guardado: ${it.take(20)}...")
    }

    view?.evaluateJavascript(EXTRACT_USER_SCRIPT) { result ->
        procesarResultadoJavaScript(context, result, onLoginSuccess)
    }
}

private fun extractTokenFromCookies(cookies: String?): String? {
    return cookies?.split(";")
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("ITMENUSESSION=") }
        ?.split("=")
        ?.get(1)
}

private fun procesarResultadoJavaScript(
    context: Context,
    result: String?,
    onLoginSuccess: () -> Unit
) {
    if (result != null && result != "null") {
        try {
            val clean = result
                .removePrefix("\"")
                .removeSuffix("\"")
                .replace("\\\"", "\"")

            val json = JSONObject(clean)
            val ruc = json.optString("ruc", null)
            val usuario = json.optString("usuario", null)

            if (esRucValido(ruc) && !usuario.isNullOrEmpty()) {
                SunatPrefs.saveRuc(context, ruc)
                SunatPrefs.saveUser(context, usuario)
                onLoginSuccess()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun esRucValido(ruc: String?): Boolean {
    return !ruc.isNullOrEmpty() && ruc.length == 11
}

// Constantes
private const val SUNAT_LOGIN_URL =
    "https://api-seguridad.sunat.gob.pe/v1/clientessol/4f3b88b3-d9d6-402a-b85d-6a0bc857746a/oauth2/loginMenuSol?lang=es-PE&showDni=true&showLanguages=false&originalUrl=https://e-menu.sunat.gob.pe/cl-ti-itmenu/AutenticaMenuInternet.htm&state=rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAADZXhlcHQABnBhcmFtc3QASyomKiYvY2wtdGktaXRtZW51L01lbnVJbnRlcm5ldC5odG0mYjY0ZDI2YThiNWFmMDkxOTIzYjIzYjY0MDdhMWMxZGI0MWU3MzNhNnQABGV4ZWNweA=="

private const val EXTRACT_USER_SCRIPT = """
    (function() {
        const menuButton = [...document.querySelectorAll('*')]
            .find(el => el.innerText && el.innerText.includes('Bienvenido'));
        if (menuButton) menuButton.click();

        const items = document.querySelectorAll("ul.dropdown-menu li.dropdown-header strong");
        let ruc = null, usuario = null;

        items.forEach(el => {
            const text = el.innerText.trim();
            if (text.startsWith("RUC:")) ruc = text.replace("RUC:", "").trim();
            if (text.startsWith("Usuario:")) usuario = text.replace("Usuario:", "").trim();
        });

        return JSON.stringify({ ruc, usuario });
    })();
"""