package com.example.purchaseregister.service

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object IntelligentReceiptParser {

    private const val TAG = "SmartParser"

    private fun detectBusinessType(text: String): String {
        val upperText = text.uppercase()
        return when {
            upperText.contains("MIFARMA") || upperText.contains("INKAFARMA") -> "PHARMACY"
            upperText.contains("PLAZA VEA") || upperText.contains("TOTTUS") || upperText.contains("WONG") -> "SUPERMARKET"
            upperText.contains("FALABELLA") || upperText.contains("RIPLEY") || upperText.contains("SAGA") -> "DEPARTMENT_STORE"
            upperText.contains("REPSOL") || upperText.contains("PRIMAX") -> "GAS_STATION"
            upperText.contains("RESTAURANT") || upperText.contains("CHIFA") -> "RESTAURANT"
            else -> "GENERIC"
        }
    }

    fun parseReceipt(text: String): String {
        val json = JSONObject()
        val lines = text.lines().filter { it.isNotBlank() }
        val businessType = detectBusinessType(text)

        Log.d(TAG, "Business type detected: $businessType")

        json.put("tipo_documento", detectDocumentType(text))
        json.put("ruc_provider", extractRUC(text))
        json.put("razon_social", extractBusinessName(lines))
        json.put("fecha", extractDate(text))
        json.put("moneda", "Soles")
        json.put("tipo_cambio", "")

        val seriesNumber = extractSeriesNumber(text)
        json.put("serie", seriesNumber.first)
        json.put("numero", seriesNumber.second)

        val products = when (businessType) {
            "PHARMACY" -> parsePharmacyProducts(lines, text)
            "SUPERMARKET" -> parseSupermarketProducts(lines, text)
            "DEPARTMENT_STORE" -> parseDepartmentStoreProducts(lines, text)
            else -> parseGenericProducts(lines, text)
        }
        json.put("productos", products)

        val totals = extractTotals(text, lines)
        json.put("costo_total", totals.first)
        json.put("igv", totals.second)
        json.put("importe_total", totals.third)

        return json.toString()
    }

    private fun parsePharmacyProducts(lines: List<String>, fullText: String): JSONArray {
        val productsArray = JSONArray()
        val allPrices = extractAllPrices(fullText)
        val totalAmount = extractTotalAmount(fullText)

        val productPrices = allPrices.filter {
            it != totalAmount && it.toDoubleOrNull()?.let { it < 1000 } == true
        }.toMutableList()

        Log.d(TAG, "Pharmacy - Found ${productPrices.size} product prices: $productPrices")

        var productIndex = 0

        for (i in lines.indices) {
            val line = lines[i].trim()

            val codePattern = Regex("""^(\d{6,})\s+(.+)$""")
            val codeMatch = codePattern.find(line)

            if (codeMatch != null && productIndex < productPrices.size) {
                var description = codeMatch.groupValues[2].trim()
                description = cleanDescription(description)

                var quantity = "1"
                for (j in i + 1 until minOf(i + 5, lines.size)) {
                    val nextLine = lines[j].trim()
                    if (nextLine.matches(Regex("^\\d+$")) && nextLine.toIntOrNull()?.let { it <= 99 } == true) {
                        quantity = nextLine
                        break
                    }
                }

                val price = productPrices[productIndex]
                if (description.isNotBlank() && price.toDoubleOrNull() != null) {
                    productsArray.put(createProductJson(description, quantity, price))
                    productIndex++
                }
            }
        }

        return productsArray
    }

    private fun parseSupermarketProducts(lines: List<String>, fullText: String): JSONArray {
        val productsArray = JSONArray()

        val patterns = listOf(
            Regex("""(.+?)\s+(\d+(?:[\.,]\d+)?)\s*(?:x|X|un|UN)?\s*[S/.\$]?\s*(\d+[\.,]\d+)"""),
            Regex("""(.+?)\s+(\d+(?:[\.,]\d+)?)\s+(\d+[\.,]\d+)""")
        )

        for (line in lines) {
            for (pattern in patterns) {
                pattern.find(line)?.let { match ->
                    var description = match.groupValues[1].trim()
                    val quantity = match.groupValues[2].replace(",", ".")
                    val price = match.groupValues[3].replace(",", ".")

                    description = cleanDescription(description)

                    if (description.isNotBlank() && price.toDoubleOrNull() != null) {
                        productsArray.put(createProductJson(description, quantity, price))
                        break
                    }
                }
            }
        }

        return productsArray
    }

    private fun parseDepartmentStoreProducts(lines: List<String>, fullText: String): JSONArray {
        val productsArray = JSONArray()

        var inProductSection = false
        var currentProduct: MutableMap<String, String>? = null

        for (line in lines) {
            val trimmedLine = line.trim()

            if (trimmedLine.contains(Regex("PRODUCTO|DESCRIPCIÓN|CANTIDAD|PRECIO", RegexOption.IGNORE_CASE))) {
                inProductSection = true
                continue
            }

            if (inProductSection && trimmedLine.contains(Regex("TOTAL|SUBTOTAL|IGV", RegexOption.IGNORE_CASE))) {
                inProductSection = false
                if (currentProduct != null) {
                    productsArray.put(createProductJson(
                        currentProduct["desc"] ?: "",
                        currentProduct["qty"] ?: "1",
                        currentProduct["price"] ?: "0"
                    ))
                    currentProduct = null
                }
            }

            if (inProductSection && trimmedLine.isNotBlank()) {
                if (currentProduct == null) {
                    currentProduct = mutableMapOf()
                    currentProduct["desc"] = trimmedLine
                } else if (currentProduct["qty"] == null && trimmedLine.matches(Regex("^\\d+$"))) {
                    currentProduct["qty"] = trimmedLine
                } else if (currentProduct["price"] == null && trimmedLine.matches(Regex("^\\d+[\\.\\,]?\\d*$"))) {
                    currentProduct["price"] = trimmedLine.replace(",", ".")
                    productsArray.put(createProductJson(
                        currentProduct["desc"] ?: "",
                        currentProduct["qty"] ?: "1",
                        currentProduct["price"] ?: "0"
                    ))
                    currentProduct = null
                }
            }
        }

        if (productsArray.length() == 0) {
            return parseGenericProducts(lines, fullText)
        }

        return productsArray
    }

    private fun parseGenericProducts(lines: List<String>, fullText: String): JSONArray {
        val productsArray = JSONArray()

        val pricePattern = Regex("""\b(\d+[\.,]\d{2})\b""")
        val allPrices = pricePattern.findAll(fullText)
            .map { it.groupValues[1].replace(",", ".") }
            .filter { it.toDoubleOrNull()?.let { it < 10000 } == true }
            .toList()

        val totalAmount = extractTotalAmount(fullText)
        val productPrices = allPrices.filter { it != totalAmount }.toMutableList()

        Log.d(TAG, "Generic - Found ${productPrices.size} potential prices")

        var productIndex = 0
        var descriptionBuilder = StringBuilder()

        for (line in lines) {
            val trimmedLine = line.trim()

            if (trimmedLine.matches(Regex("^\\d+$")) && descriptionBuilder.isNotEmpty() && productIndex < productPrices.size) {
                val description = descriptionBuilder.toString().trim()
                val price = productPrices[productIndex]

                if (description.isNotBlank() && price.toDoubleOrNull() != null) {
                    productsArray.put(createProductJson(description, trimmedLine, price))
                    productIndex++
                    descriptionBuilder.clear()
                }
                continue
            }

            val skipPatterns = listOf(
                "RUC", "FECHA", "BOLETA", "FACTURA", "CAJA", "CAJERO", "TOTAL",
                "IGV", "IMPORTE", "VUELTO", "SUBTOTAL", "MONEDA", "TIPO CAMBIO",
                "SON:", "TARJETA", "VISA", "TERMINAL", "VENDEDOR", "TURNO"
            )

            var shouldSkip = false
            for (skipPattern in skipPatterns) {
                if (trimmedLine.contains(Regex(skipPattern, RegexOption.IGNORE_CASE))) {
                    shouldSkip = true
                    break
                }
            }

            if (shouldSkip) {
                if (descriptionBuilder.isNotEmpty()) {
                    descriptionBuilder.clear()
                }
                continue
            }

            if (trimmedLine.isNotBlank() && !trimmedLine.matches(Regex("^\\d+$")) && productIndex < productPrices.size) {
                if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append(" ")
                descriptionBuilder.append(trimmedLine)
            }

            if (productIndex >= productPrices.size) break
        }

        if (productsArray.length() == 0 && productPrices.isNotEmpty()) {
            Log.d(TAG, "Generic fallback: line-by-line extraction")
            for (i in lines.indices) {
                val line = lines[i]
                if (line.matches(Regex(".*[A-Za-zÑñ].*")) &&
                    line.length > 10 &&
                    i + 1 < lines.size) {

                    val nextLine = lines[i + 1]
                    if (nextLine.matches(Regex("^\\d+$")) && productIndex < productPrices.size) {
                        productsArray.put(createProductJson(line, nextLine, productPrices[productIndex]))
                        productIndex++
                    }
                }
            }
        }

        return productsArray
    }

    private fun detectDocumentType(text: String): String {
        return when {
            text.contains(Regex("FACTURA", RegexOption.IGNORE_CASE)) -> "Factura"
            text.contains(Regex("BOLETA", RegexOption.IGNORE_CASE)) -> "Boleta"
            text.contains(Regex("NOTA DE VENTA", RegexOption.IGNORE_CASE)) -> "Nota de Venta"
            else -> "Boleta"
        }
    }

    private fun extractRUC(text: String): String {
        val pattern = Regex("""RUC\s*:?\s*(\d{11})""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun extractBusinessName(lines: List<String>): String {
        for (i in lines.indices) {
            val line = lines[i]

            if (line.contains(Regex("RUC\\s*:", RegexOption.IGNORE_CASE))) {
                val parts = line.split(Regex("RUC\\s*:", RegexOption.IGNORE_CASE))
                if (parts.isNotEmpty()) {
                    var name = parts[0].trim()
                    name = name.replace(Regex("[^A-Za-zÑñÁÉÍÓÚ\\s\\.&]"), "").trim()
                    if (name.isNotBlank() && name.length > 3 && !name.matches(Regex(".*\\d.*"))) {
                        return name.take(100)
                    }
                }

                for (j in maxOf(0, i - 3) until i) {
                    var name = lines[j].trim()
                    name = name.replace(Regex("[^A-Za-zÑñÁÉÍÓÚ\\s\\.&]"), "").trim()
                    if (name.isNotBlank() && name.length > 5 &&
                        name.contains(Regex("S\\.A\\.C\\.|S\\.R\\.L\\.|EIRL|SAC|SRL", RegexOption.IGNORE_CASE))) {
                        return name.take(100)
                    }
                }
            }

            if (line.contains(Regex("S\\.A\\.C\\.|S\\.R\\.L\\.|EIRL|SAC|SRL", RegexOption.IGNORE_CASE))) {
                var name = line.trim()
                name = name.replace(Regex("^\\d+\\s+"), "")
                name = name.replace(Regex("[^A-Za-zÑñÁÉÍÓÚ\\s\\.&]"), "").trim()
                if (name.isNotBlank() && name.length > 5) {
                    return name.take(100)
                }
            }
        }

        for (line in lines) {
            if (line.length in 10..80 &&
                !line.contains(Regex("\\d")) &&
                line.contains(Regex("[A-Z]{3,}"))) {
                return line.trim().take(100)
            }
        }

        return ""
    }

    private fun extractSeriesNumber(text: String): Pair<String, String> {
        val patterns = listOf(
            Regex("""BOLETA:?\s+([A-Z0-9]+)-(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""FACTURA:?\s+([A-Z0-9]+)-(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z0-9]{2,4})-(\d{6,12})""")
        )
        for (pattern in patterns) {
            pattern.find(text)?.let {
                return Pair(it.groupValues[1], it.groupValues[2])
            }
        }
        return Pair("", "")
    }

    private fun extractDate(text: String): String {
        val pattern = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""")
        pattern.find(text)?.let {
            val day = it.groupValues[1].padStart(2, '0')
            val month = it.groupValues[2].padStart(2, '0')
            val year = it.groupValues[3]
            return "$day/$month/$year"
        }
        return ""
    }

    private fun extractAllPrices(text: String): List<String> {
        val pattern = Regex("""(\d+[\.,]\d{2})""")
        return pattern.findAll(text)
            .map { it.groupValues[1].replace(",", ".") }
            .filter { it.toDoubleOrNull() != null }
            .toList()
    }

    private fun extractTotalAmount(text: String): String {
        val patterns = listOf(
            Regex("""IMPORTE\s*(?:FINAL|TOTAL)?:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""TOTAL\s*:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""TTAL\s*:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""SON:\s*[A-Z\s]+\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""TOTAL\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", ".")
                val amountDouble = amount.toDoubleOrNull()
                if (amountDouble != null && amountDouble > 0) {
                    Log.d(TAG, "Found total: $amount with pattern ${pattern.pattern}")
                    return amount
                }
            }
        }

        val allNumbers = mutableListOf<Double>()
        val numberPattern = Regex("""\b(\d+[\.,]\d{2})\b""")
        val matches = numberPattern.findAll(text)

        for (match in matches) {
            val numStr = match.groupValues[1].replace(",", ".")
            val num = numStr.toDoubleOrNull()
            if (num != null && num in 1.0..100000.0) {
                allNumbers.add(num)
            }
        }

        allNumbers.sortDescending()

        if (allNumbers.size > 0) {
            val largestNumber = allNumbers[0]
            Log.d(TAG, "Using largest number as total: $largestNumber")
            return "%.2f".format(largestNumber)
        }

        return ""
    }

    private fun extractTotals(text: String, lines: List<String>): Triple<String, String, String> {
        var subtotal = ""
        var igv = ""
        var totalAmount = extractTotalAmount(text)

        val igvPatterns = listOf(
            Regex("""IGV\s*:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""IGV\s*:?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""\*+\s*IGV\s*:?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""\*+\s*IGV\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""IGV\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
            Regex("""IGV\s*18%\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in igvPatterns) {
            pattern.find(text)?.let {
                igv = it.groupValues[1].replace(",", ".")
                Log.d(TAG, "Found IGV: $igv with pattern ${pattern.pattern}")
                break
            }
        }

        if (subtotal.isEmpty()) {
            val valorVentaPatterns = listOf(
                Regex("""VALOR\s*VENTA\s*:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
                Regex("""\*+\s*Valor Venta\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
                Regex("""VALOR\s*VENTA\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
                Regex("""COSTO\s*TOTAL\s*:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE),
                Regex("""SUBTOTAL\s*:?\s*S?/?\.?\s*(\d+[\.,]\d+)""", RegexOption.IGNORE_CASE)
            )

            for (pattern in valorVentaPatterns) {
                pattern.find(text)?.let {
                    subtotal = it.groupValues[1].replace(",", ".")
                    Log.d(TAG, "Found subtotal (Valor Venta): $subtotal")
                    break
                }
            }
        }

        if (totalAmount.isNotBlank()) {
            val totalNum = totalAmount.toDoubleOrNull() ?: 0.0

            if (igv.isNotBlank() && subtotal.isEmpty()) {
                val igvNum = igv.toDoubleOrNull() ?: 0.0
                subtotal = String.format("%.2f", totalNum - igvNum)
                Log.d(TAG, "Calculated subtotal from total - IGV: $subtotal")
            } else if (subtotal.isNotBlank() && igv.isEmpty()) {
                val subtotalNum = subtotal.toDoubleOrNull() ?: 0.0
                val calculatedIGV = totalNum - subtotalNum
                igv = String.format("%.2f", calculatedIGV)
                Log.d(TAG, "Calculated IGV from total - subtotal: $igv")
            } else if (subtotal.isEmpty() && igv.isEmpty()) {
                val baseWithoutIGV = totalNum / 1.18
                val calculatedIGV = totalNum - baseWithoutIGV
                subtotal = String.format("%.2f", baseWithoutIGV)
                igv = String.format("%.2f", calculatedIGV)
                Log.d(TAG, "Calculated both from total (assuming 18% IGV): subtotal=$subtotal, igv=$igv")
            }
        }

        Log.d(TAG, "Final totals - Subtotal: $subtotal, IGV: $igv, Total: $totalAmount")

        return Triple(subtotal, igv, totalAmount)
    }

    private fun cleanDescription(description: String): String {
        var cleaned = description
        cleaned = cleaned.replace(Regex("\\s+[A-Z]{2,}\\s+"), " ")
        cleaned = cleaned.replace(Regex("\\s+\\d+[A-Z]+\\s+"), " ")
        cleaned = cleaned.replace(Regex("\\s+MKT\\s+[A-Z]+\\s+"), " ")
        cleaned = cleaned.trim()
        if (cleaned.length > 80) cleaned = cleaned.substring(0, 80)
        return cleaned
    }

    private fun createProductJson(description: String, quantity: String, price: String): JSONObject {
        val product = JSONObject()
        product.put("descripcion", description.take(100))
        product.put("cantidad", quantity)
        product.put("unidad_medida", detectUnitOfMeasure(description))
        product.put("costo_unitario", price)
        return product
    }

    private fun detectUnitOfMeasure(description: String): String {
        val descUpper = description.uppercase()
        return when {
            descUpper.contains(Regex("KG|KILO|KILOGRAM")) -> "KILOGRAMOS"
            descUpper.contains(Regex("LT|LITRO")) -> "LITROS"
            descUpper.contains(Regex("SOB|SOBRE")) -> "SOBRES"
            descUpper.contains(Regex("UN|UNID|UNIDAD|TAB|COMP|CAPSULA|TABLET")) -> "UNIDADES"
            descUpper.contains(Regex("GR|GRAMO")) -> "GRAMOS"
            descUpper.contains(Regex("ML|MILILITRO")) -> "MILILITROS"
            else -> "UNIDADES"
        }
    }
}