/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ocr

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.walshtech.receiptwrangler.data.ReceiptCategory
import com.walshtech.receiptwrangler.data.ReceiptDraft
import com.walshtech.receiptwrangler.data.ReceiptStatus
import com.walshtech.receiptwrangler.data.UserLane
import com.walshtech.receiptwrangler.data.defaultProjectName
import com.walshtech.receiptwrangler.data.defaultReportNameForLane
import com.walshtech.receiptwrangler.data.todayIso
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class ReceiptOcrEngine(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val totalPattern = Pattern.compile("(?i)(grand total|balance due|amount due|amount|total)\\D{0,12}(\\d+[.,]\\d{2})")
    private val subtotalPattern = Pattern.compile("(?i)(sub\\s?total|subtotal)\\D{0,12}(\\d+[.,]\\d{2})")
    private val taxPattern = Pattern.compile("(?i)(tax|vat)\\D{0,8}(\\d+[.,]\\d{2})")
    private val amountPattern = Pattern.compile("(\\d+[.,]\\d{2})")
    private val datePatterns = listOf(
        Pattern.compile("(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2})"),
        Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/]20\\d{2})"),
        Pattern.compile("([A-Za-z]{3,9}\\s+\\d{1,2},\\s+20\\d{2})")
    )

    suspend fun importAndParse(uri: Uri, resolver: ContentResolver, lane: UserLane, projectName: String): ReceiptDraft {
        val localCopy = copyIntoPrivateStorage(uri, resolver)
        return reparseLocalFile(localCopy, uri.toString(), lane, projectName)
    }

    suspend fun reparseLocalFile(file: File, sourceUri: String, lane: UserLane, projectName: String = defaultProjectName()): ReceiptDraft {
        val text = if (file.extension.equals("pdf", true)) ocrPdf(file) else ocrBitmapFile(file)
        return draftFromText(text, sourceUri = sourceUri, localCopyPath = file.absolutePath, lane = lane, projectName = projectName)
    }

    private suspend fun ocrBitmapFile(file: File): String {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return ""
        val baseText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
        if (!shouldRunEmergencyPass(baseText)) return baseText

        val variants = listOf(
            bitmap,
            enhanceForEmergency(bitmap, contrast = 1.4f),
            enhanceForEmergency(bitmap, contrast = 1.8f),
            rotate(bitmap, 90f),
            rotate(bitmap, 270f),
            enhanceForEmergency(rotate(bitmap, 90f), contrast = 1.6f)
        )

        var best = baseText
        var bestScore = textQualityScore(baseText)
        variants.forEach { candidateBitmap ->
            val candidateText = recognizer.process(InputImage.fromBitmap(candidateBitmap, 0)).await().text
            val score = textQualityScore(candidateText)
            if (score > bestScore) {
                bestScore = score
                best = candidateText
            }
        }
        return best
    }

    private fun shouldRunEmergencyPass(text: String): Boolean {
        val compact = text.replace("\n", " ").trim()
        if (compact.length < 50) return true
        val digitCount = compact.count { it.isDigit() }
        return digitCount < 4
    }

    private fun textQualityScore(text: String): Int {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return 0
        val lineCount = cleaned.lines().count { it.isNotBlank() }
        val digitCount = cleaned.count { it.isDigit() }
        val alphaCount = cleaned.count { it.isLetter() }
        val amountHits = amountPattern.matcher(cleaned).toSequence().count()
        return cleaned.length + (lineCount * 8) + (digitCount * 3) + (alphaCount / 4) + (amountHits * 25)
    }

    private fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun enhanceForEmergency(source: Bitmap, contrast: Float): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private suspend fun ocrPdf(file: File): String {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = minOf(renderer.pageCount, 2)
        val parts = mutableListOf<String>()
        repeat(pageCount) { idx ->
            renderer.openPage(idx).use { page ->
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                parts += recognizer.process(InputImage.fromBitmap(bmp, 0)).await().text
            }
        }
        renderer.close()
        pfd.close()
        return parts.joinToString("\n")
    }

    private fun copyIntoPrivateStorage(uri: Uri, resolver: ContentResolver): File {
        val dir = File(context.filesDir, "receipt_sources").apply { mkdirs() }
        val ext = when {
            (resolver.getType(uri) ?: "").contains("pdf") -> "pdf"
            else -> "jpg"
        }
        val out = File(dir, "rw_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext")
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    fun draftFromText(text: String, sourceUri: String, localCopyPath: String, lane: UserLane, projectName: String = defaultProjectName()): ReceiptDraft {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = detectMerchant(lines)
        val total = detectTotal(text)
        val tax = detectTax(text)
        val subtotal = detectSubtotal(text, total, tax)
        val detectedDate = detectDate(text)
        val date = detectedDate.ifBlank { todayIso() }
        val category = inferCategory(text)
        val payment = inferPaymentMethod(text)
        val amountCount = amountPattern.matcher(text).toSequence().count()
        val confidence = computeConfidence(
            hasMerchant = merchant.isNotBlank(),
            hasTotal = total.isNotBlank(),
            hasDate = detectedDate.isNotBlank(),
            hasTax = tax.isNotBlank(),
            hasPayment = payment.isNotBlank(),
            amountCount = amountCount,
            textLength = text.length,
            categorized = category != ReceiptCategory.OTHER
        )
        val needs = buildList {
            if (merchant.isBlank()) add("missing merchant")
            if (total.isBlank()) add("missing total")
            if (detectedDate.isBlank()) add("missing date")
            if (confidence < 65) add("low OCR confidence")
        }
        val suggestedTag = inferTag(text)
        val suggestedReport = inferReportName(lane, date)
        return ReceiptDraft(
            projectName = projectName,
            merchant = merchant,
            dateIso = date,
            total = total,
            tax = tax,
            subtotal = subtotal,
            category = category,
            lane = lane,
            status = if (confidence >= 70) ReceiptStatus.READY else ReceiptStatus.REVIEW,
            notes = if (needs.isEmpty()) "" else "OCR assist: ${needs.joinToString(", ")}",
            tag = suggestedTag,
            reportName = suggestedReport,
            paymentMethod = payment,
            reimbursable = lane.defaultReimbursable,
            billable = lane.defaultBillable,
            confidence = confidence,
            sourceUri = sourceUri,
            localCopyPath = localCopyPath,
            ocrText = text.take(8000)
        )
    }

    private fun detectMerchant(lines: List<String>): String {
        val cleaned = lines.map { it.replace(Regex("[^A-Za-z0-9 &.'-]"), " ").trim() }.filter { it.length >= 3 }
        return cleaned.firstOrNull { line ->
            val lower = line.lowercase(Locale.US)
            lower !in listOf("receipt", "invoice", "thank you", "total", "subtotal", "tax")
        }?.take(48).orEmpty()
    }

    private fun detectTotal(text: String): String {
        val matcher = totalPattern.matcher(text)
        if (matcher.find()) return matcher.group(2)?.replace(',', '.') ?: ""
        val amounts = amountPattern.matcher(text)
        val values = mutableListOf<Double>()
        while (amounts.find()) {
            amounts.group(1)?.replace(',', '.')?.toDoubleOrNull()?.let { value ->
                if (value in 0.01..100000.0) values += value
            }
        }
        return values.maxOrNull()?.let { "%.2f".format(Locale.US, it) }.orEmpty()
    }

    private fun detectTax(text: String): String {
        val matcher = taxPattern.matcher(text)
        return if (matcher.find()) matcher.group(2)?.replace(',', '.') ?: "" else ""
    }

    private fun detectSubtotal(text: String, total: String, tax: String): String {
        val matcher = subtotalPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(2)?.replace(',', '.') ?: ""
        }
        if (total.isNotBlank() && tax.isNotBlank()) {
            val result = (total.replace(',', '.').toDoubleOrNull() ?: 0.0) - (tax.replace(',', '.').toDoubleOrNull() ?: 0.0)
            if (result > 0) return "%.2f".format(Locale.US, result)
        }
        return total
    }

    private fun detectDate(text: String): String {
        for (pattern in datePatterns) {
            val m = pattern.matcher(text)
            if (m.find()) return normalizeDate(m.group(1).orEmpty())
        }
        return ""
    }

    private fun normalizeDate(raw: String): String {
        val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy", "M/d/yyyy", "MM-dd-yyyy", "M-d-yyyy", "MMM d, yyyy", "MMMM d, yyyy")
        for (format in formats) {
            runCatching {
                val parser = SimpleDateFormat(format, Locale.US).apply { isLenient = false }
                val date = parser.parse(raw) ?: return@runCatching
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
            }
        }
        return raw
    }

    private fun inferPaymentMethod(text: String): String {
        val t = text.lowercase(Locale.US)
        return when {
            "visa" in t -> "Visa"
            "mastercard" in t || "master card" in t -> "Mastercard"
            "amex" in t || "american express" in t -> "Amex"
            "discover" in t -> "Discover"
            "cash" in t -> "Cash"
            "debit" in t -> "Debit"
            "credit" in t -> "Credit"
            "apple pay" in t -> "Apple Pay"
            "google pay" in t -> "Google Pay"
            "tap" in t || "contactless" in t -> "Contactless"
            else -> ""
        }
    }

    private fun inferTag(text: String): String {
        val t = text.lowercase(Locale.US)
        return when {
            listOf("airport", "airlines", "hotel", "uber", "lyft").any { it in t } -> "travel"
            listOf("client", "invoice", "project").any { it in t } -> "client"
            listOf("meal", "restaurant", "cafe").any { it in t } -> "meal"
            else -> ""
        }
    }

    private fun inferReportName(lane: UserLane, dateIso: String): String {
        return defaultReportNameForLane(lane, dateIso)
    }

    private fun computeConfidence(
        hasMerchant: Boolean,
        hasTotal: Boolean,
        hasDate: Boolean,
        hasTax: Boolean,
        hasPayment: Boolean,
        amountCount: Int,
        textLength: Int,
        categorized: Boolean
    ): Int {
        val base = listOf(
            if (hasMerchant) 26 else 0,
            if (hasTotal) 32 else 0,
            if (hasDate) 20 else 0,
            if (hasTax) 6 else 0,
            if (hasPayment) 4 else 0,
            if (categorized) 4 else 0,
            if (textLength > 60) 4 else 0,
            if (amountCount in 2..24) 4 else 0
        ).sum()
        return base.coerceIn(0, 100)
    }

    private fun java.util.regex.Matcher.toSequence(): Sequence<java.util.regex.Matcher> = sequence {
        while (find()) yield(this@toSequence)
    }

    private fun inferCategory(text: String): ReceiptCategory {
        val t = text.lowercase(Locale.US)
        return when {
            listOf("uber", "lyft", "taxi", "parking", "toll", "train", "metro").any { it in t } -> ReceiptCategory.GROUND_TRAVEL
            listOf("hotel", "inn", "marriott", "hilton", "hyatt", "motel").any { it in t } -> ReceiptCategory.LODGING
            listOf("airlines", "delta", "alaska", "united", "southwest", "jetblue").any { it in t } -> ReceiptCategory.AIR_TRAVEL
            listOf("restaurant", "cafe", "coffee", "bar", "grill", "bistro").any { it in t } -> ReceiptCategory.MEALS
            listOf("office depot", "staples", "supply", "hardware", "store").any { it in t } -> ReceiptCategory.SUPPLIES
            listOf("adobe", "microsoft", "openai", "google", "software", "subscription").any { it in t } -> ReceiptCategory.SOFTWARE
            listOf("shell", "chevron", "76", "exxon", "fuel", "gas").any { it in t } -> ReceiptCategory.FUEL
            listOf("tax", "vat", "fee", "surcharge").any { it in t } -> ReceiptCategory.TAX
            else -> ReceiptCategory.OTHER
        }
    }
}
