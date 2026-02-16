package com.example.purchaseregister.utils

object CurrencyUtils {

    fun isDollarCurrency(currency: String): Boolean {
        return currency.contains("USD", ignoreCase = true) ||
                currency.contains("Dólar", ignoreCase = true) ||
                currency.contains("Dolar", ignoreCase = true) ||
                currency.contains("US$", ignoreCase = false) ||
                currency.contains("U$", ignoreCase = false)
    }

    fun isSolCurrency(currency: String): Boolean {
        return currency.contains("Soles", ignoreCase = true) ||
                currency.contains("SOL", ignoreCase = true) ||
                currency.contains("PEN", ignoreCase = true) ||
                currency.contains("S/", ignoreCase = false)
    }

    fun formatCurrency(currency: String): String {
        return when {
            isSolCurrency(currency) -> "Soles (PEN)"
            isDollarCurrency(currency) -> "Dólares (USD)"
            currency == "Soles" -> "Soles (PEN)"
            currency == "Dólares" -> "Dólares (USD)"
            currency == "Dolares" -> "Dólares (USD)"
            else -> currency
        }
    }
}