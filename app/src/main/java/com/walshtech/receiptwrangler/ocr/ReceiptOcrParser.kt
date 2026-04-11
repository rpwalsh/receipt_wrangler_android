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
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.walshtech.receiptwrangler.data.ReceiptCategory
import com.walshtech.receiptwrangler.data.ReceiptDraft
import com.walshtech.receiptwrangler.data.UserLane
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class ReceiptOcrParser(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val totalPattern = Pattern.compile("(?i)(total|amount|balance due|grand total)\\D{0,8}(\\d+[.,]\\d{2})")
    private val genericAmountPattern = Pattern.compile("(\\d+[.,]\\d{2})")
    private val datePatterns = listOf(
        Pattern.compile("(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2})"),
        Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/]20\\d{2})")
    )

    suspend fun parseUri(uri: Uri, resolver: ContentResolver, lane: UserLane): ReceiptDraft {
        return if ((resolver.getType(uri) ?: "").contains("pdf")) {
            ReceiptDraft(
                merchant = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported PDF",
                dateIso = todayIso(),
                notes = "PDF imported. OCR not attempted in this build; attach pages manually if needed.",
                lane = lane,
                confidence = 15,
                sourceUri = uri.toString()
            )
        } else {
            val stream = resolver.openInputStream(uri)
            val bitmap = stream.use { BitmapFactory.decodeStream(it) }
            val image = InputImage.fromBitmap(bitmap, 0)
            val text = recognizer.process(image).await().text
            draftFromText(text, uri.toString(), lane)
        }
    }

    fun draftFromText(text: String, sourceUri: String = "", lane: UserLane = UserLane.BUSINESS): ReceiptDraft {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = lines.firstOrNull()?.take(48).orEmpty()
        val total = findBestTotal(text)
        val date = findDate(text)
        val category = inferCategory(text)
        val confidence = listOf(
            if (merchant.isNotBlank()) 35 else 0,
            if (total.isNotBlank()) 35 else 0,
            if (date.isNotBlank()) 20 else 0,
            if (text.length > 24) 10 else 0
        ).sum()
        val subtotal = total
        return ReceiptDraft(
            merchant = merchant,
            dateIso = date.ifBlank { todayIso() },
            total = total,
            tax = "",
            subtotal = subtotal,
            category = category,
            notes = "",
            lane = lane,
            reimbursable = lane.defaultReimbursable,
            billable = lane.defaultBillable,
            excluded = false,
            confidence = confidence,
            sourceUri = sourceUri,
            ocrText = text.take(5000)
        )
    }

    private fun findBestTotal(text: String): String {
        val specific = totalPattern.matcher(text)
        if (specific.find()) return specific.group(2)?.replace(',', '.') ?: ""
        val amounts = genericAmountPattern.matcher(text)
        val values = mutableListOf<Double>()
        while (amounts.find()) {
            amounts.group(1)?.replace(',', '.')?.toDoubleOrNull()?.let { values += it }
        }
        return values.maxOrNull()?.let { "%.2f".format(Locale.US, it) }.orEmpty()
    }

    private fun findDate(text: String): String {
        for (pattern in datePatterns) {
            val m = pattern.matcher(text)
            if (m.find()) return normalizeDate(m.group(1).orEmpty())
        }
        return ""
    }

    private fun normalizeDate(raw: String): String {
        val fmts = listOf("yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy", "M/d/yyyy", "MM-dd-yyyy", "M-d-yyyy")
        for (fmt in fmts) {
            runCatching {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(raw) ?: return@runCatching null
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
            }
        }
        return raw
    }

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun inferCategory(text: String): ReceiptCategory {
        val t = text.lowercase(Locale.US)
        return when {
            listOf("uber", "lyft", "taxi", "parking", "toll").any { it in t } -> ReceiptCategory.GROUND_TRAVEL
            listOf("hotel", "inn", "marriott", "hilton").any { it in t } -> ReceiptCategory.LODGING
            listOf("airlines", "delta", "alaska", "united", "southwest").any { it in t } -> ReceiptCategory.AIR_TRAVEL
            listOf("restaurant", "cafe", "coffee", "bar", "grill").any { it in t } -> ReceiptCategory.MEALS
            listOf("office depot", "staples", "supply", "hardware").any { it in t } -> ReceiptCategory.SUPPLIES
            listOf("adobe", "microsoft", "openai", "google", "software").any { it in t } -> ReceiptCategory.SOFTWARE
            listOf("shell", "chevron", "76", "exxon").any { it in t } -> ReceiptCategory.FUEL
            else -> ReceiptCategory.OTHER
        }
    }
}
