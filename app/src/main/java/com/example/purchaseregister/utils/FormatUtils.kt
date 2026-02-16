package com.example.purchaseregister.utils

object FormatUtils {

    fun formatUnitOfMeasure(quantity: String, unit: String): String {
        val formattedUnit = when (unit.uppercase()) {
            "KILO", "KILOS", "KILOGRAMO", "KILOGRAMOS", "KG", "KGS" -> "Kg"
            "GRAMO", "GRAMOS", "GR", "GRS", "G" -> "Gr"
            "LITRO", "LITROS", "L", "LT", "LTS" -> "Lt"
            "UNIDAD", "UNIDADES", "UN", "UND", "UNDS" -> "UN"
            "METRO", "METROS", "M", "MT", "MTS" -> "M"
            "CENTIMETRO", "CENTIMETROS", "CM", "CMS" -> "Cm"
            "MILIMETRO", "MILIMETROS", "MM", "MMS" -> "Mm"
            "PAQUETE", "PAQUETES", "PQ", "PQT", "PQTS" -> "Pq"
            "CAJA", "CAJAS", "CJ", "CJA", "CJAS" -> "Bx"
            "GALON", "US GALON", "GALONES", "GAL", "GALS" -> "Gal"
            "CASE", "CS" -> "Cs"
            else -> if (unit.isNotBlank()) unit else ""
        }

        return if (formattedUnit.isNotBlank()) "$quantity $formattedUnit" else quantity
    }

    fun cleanAmount(text: String): String {
        return text.replace(Regex("[^0-9.]"), "")
    }
}