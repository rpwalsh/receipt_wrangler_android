/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.repo

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.walshtech.receiptwrangler.data.ReceiptCategory
import com.walshtech.receiptwrangler.data.ReceiptDao
import com.walshtech.receiptwrangler.data.ReceiptDraft
import com.walshtech.receiptwrangler.data.ReceiptEntity
import com.walshtech.receiptwrangler.data.ReceiptStatus
import com.walshtech.receiptwrangler.data.ScanCleanupOptions
import com.walshtech.receiptwrangler.data.UserLane
import com.walshtech.receiptwrangler.data.calculateGigMath
import com.walshtech.receiptwrangler.data.centsToString
import com.walshtech.receiptwrangler.data.defaultProjectName
import com.walshtech.receiptwrangler.data.displayMerchant
import com.walshtech.receiptwrangler.data.inferGigEntryType
import com.walshtech.receiptwrangler.data.parseUserLane
import com.walshtech.receiptwrangler.data.toEntity
import com.walshtech.receiptwrangler.ocr.ReceiptOcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ReceiptRepository(
    private val context: Context,
    private val dao: ReceiptDao,
    private val ocrEngine: ReceiptOcrEngine
) {
    val receipts: Flow<List<ReceiptEntity>> = dao.observeAll()

    suspend fun importUris(uris: List<Uri>, resolver: ContentResolver, lane: UserLane, projectName: String) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            val draft = ocrEngine.importAndParse(uri, resolver, lane, projectName)
            val entity = scoreDuplicates(draft.toEntity())
            dao.upsert(entity)
        }
    }

    suspend fun saveDraft(draft: ReceiptDraft) = withContext(Dispatchers.IO) {
        val previous = if (draft.id != 0L) dao.getById(draft.id) else null
        dao.upsert(scoreDuplicates(draft.toEntity(previous)))
    }

    suspend fun deleteIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        val receipts = dao.getAllNow().filter { it.id in ids }
        receipts.map { it.localCopyPath }.filter { it.isNotBlank() }.forEach { runCatching { File(it).delete() } }
        dao.deleteByIds(ids)
    }

    suspend fun changeCategory(ids: List<Long>, category: ReceiptCategory) = withContext(Dispatchers.IO) {
        dao.setCategory(ids, category.name)
    }

    suspend fun changeStatus(ids: List<Long>, status: ReceiptStatus) = withContext(Dispatchers.IO) {
        dao.setStatus(ids, status.name)
    }

    suspend fun applyCleanup(id: Long, options: ScanCleanupOptions): ReceiptEntity? = withContext(Dispatchers.IO) {
        val current = dao.getById(id) ?: return@withContext null
        val sourceFile = File(current.localCopyPath)
        if (!sourceFile.exists() || sourceFile.extension.equals("pdf", true)) return@withContext current
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@withContext current
        val cropped = cropBitmap(bitmap, options)
        val rotated = rotateBitmap(cropped, options.rotationDegrees.toFloat())
        val cleaned = adjustBitmap(rotated, options)
        val outDir = File(context.filesDir, "receipt_sources").apply { mkdirs() }
        val outFile = File(outDir, "clean_${System.currentTimeMillis()}_${id}.jpg")
        FileOutputStream(outFile).use { cleaned.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        val reparsed = ocrEngine.reparseLocalFile(
            outFile,
            current.sourceUri,
            parseUserLane(current.lane),
            current.projectName
        )
        val merged = reparsed.toEntity(current).copy(
            id = current.id,
            notes = joinNotes(current.notes, "[Scan cleanup applied] " + reparsed.notes).trim(),
            reportName = if (reparsed.reportName.isBlank()) current.reportName else reparsed.reportName,
            tag = if (reparsed.tag.isBlank()) current.tag else reparsed.tag,
            localCopyPath = outFile.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        val rescored = scoreDuplicates(merged)
        dao.upsert(rescored)
        rescored
    }

    suspend fun mergeReceipts(primaryId: Long, otherIds: List<Long>): ReceiptEntity? = withContext(Dispatchers.IO) {
        val primary = dao.getById(primaryId) ?: return@withContext null
        val others = dao.getAllNow().filter { it.id in otherIds && it.id != primaryId }
        if (others.isEmpty()) return@withContext primary
        val traceHeader = buildString {
            append("Merged from receipt IDs: ")
            append(others.map { it.id }.sorted().joinToString(", "))
            append(" on ")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(System.currentTimeMillis()))
        }
        val merged = primary.copy(
            merchant = longest(primary.merchant, others.map { it.merchant }),
            merchantAlias = longest(primary.merchantAlias, others.map { it.merchantAlias }),
            dateIso = chooseDate(primary, others),
            totalCents = (listOf(primary.totalCents) + others.map { it.totalCents }).maxOrNull() ?: primary.totalCents,
            taxCents = (listOf(primary.taxCents) + others.map { it.taxCents }).maxOrNull() ?: primary.taxCents,
            subtotalCents = (listOf(primary.subtotalCents) + others.map { it.subtotalCents }).maxOrNull() ?: primary.subtotalCents,
            category = chooseString(primary.category, others.map { it.category }),
            status = chooseStatus(primary, others),
            notes = joinNotes(joinNotes(primary.notes, traceHeader), others.joinToString("\n") { it.notes }),
            tag = chooseString(primary.tag, others.map { it.tag }),
            reportName = chooseString(primary.reportName, others.map { it.reportName }),
            paymentMethod = chooseString(primary.paymentMethod, others.map { it.paymentMethod }),
            reimbursable = primary.reimbursable || others.any { it.reimbursable },
            billable = primary.billable || others.any { it.billable },
            excluded = primary.excluded && others.all { it.excluded },
            confidence = (listOf(primary.confidence) + others.map { it.confidence }).maxOrNull() ?: primary.confidence,
            duplicateScore = 0,
            sourceUri = chooseString(primary.sourceUri, others.map { it.sourceUri }),
            localCopyPath = chooseExistingPath(primary.localCopyPath, others.map { it.localCopyPath }),
            ocrText = joinNotes(primary.ocrText, others.joinToString("\n") { it.ocrText }).take(12000),
            createdAt = (listOf(primary.createdAt) + others.map { it.createdAt }).minOrNull() ?: primary.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        val rescored = scoreDuplicates(merged)
        dao.upsert(rescored)
        deleteIds(others.map { it.id })
        rescored
    }

    suspend fun exportCsv(uri: Uri, resolver: ContentResolver, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        val header = listOf("id", "merchant", "alias", "date", "total", "tax", "subtotal", "category", "lane", "status", "report", "tag", "payment", "reimbursable", "billable", "excluded", "confidence", "duplicateScore", "notes")
        val rows = visible.map { r ->
            listOf(
                r.id.toString(), r.merchant, r.merchantAlias, r.dateIso, cents(r.totalCents), cents(r.taxCents), cents(r.subtotalCents),
                r.category, r.lane, r.status, r.reportName, r.tag, r.paymentMethod,
                r.reimbursable.toString(), r.billable.toString(), r.excluded.toString(), r.confidence.toString(), r.duplicateScore.toString(), r.notes
            )
        }
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.appendLine(header.joinToString(",") { csvEscape(it) })
            rows.forEach { writer.appendLine(it.joinToString(",") { cell -> csvEscape(cell) }) }
        }
    }

    suspend fun exportReportPacket(uri: Uri, resolver: ContentResolver, lane: UserLane, reportName: String, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        val included = visible.filterNot { it.excluded }
        val excluded = visible.filter { it.excluded }
        val total = included.sumOf { it.totalCents }
        val byCategory = included.groupBy { it.category }.toSortedMap()
        val dateRange = buildDateRange(included)
        val laneGuidance = lanePacketGuidance(lane)
        val body = buildString {
            appendLine("# Receipt Wrangler Packet")
            appendLine()
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date())}")
            appendLine("Lane: ${lane.label}")
            appendLine("Report: ${reportName.ifBlank { "Untitled packet" }}")
            appendLine("Included receipts: ${included.size}")
            appendLine("Excluded receipts: ${excluded.size}")
            appendLine("Date range: $dateRange")
            appendLine("Total: $${centsToString(total)}")
            appendLine()
            appendLine("## Summary")
            appendLine(laneGuidance)
            appendLine("- Ready: ${included.count { it.status == ReceiptStatus.READY.name }}")
            appendLine("- Needs review: ${included.count { it.status == ReceiptStatus.REVIEW.name || it.confidence < 70 }}")
            appendLine("- Low OCR confidence: ${included.count { it.confidence < 60 }}")
            if (lane == UserLane.GIG_PROFIT) {
                val gm = calculateGigMath(included)
                appendLine("- Sales total: $${centsToString(gm.salesCents)} (${gm.saleCount})")
                appendLine("- Expense total: $${centsToString(gm.expenseCents)} (${gm.expenseCount})")
                appendLine("- Net: $${centsToString(gm.netCents)}")
            }
            appendLine()
            appendLine("## Category summary")
            byCategory.forEach { (category, items) ->
                appendLine("- ${category.lowercase(Locale.US).replace('_', ' ')}: ${items.size} item(s), $${centsToString(items.sumOf { it.totalCents })}")
            }
            appendLine()
            appendLine("## Receipts")
            included.sortedByDescending { it.dateIso }.forEach { r ->
                appendLine("### ${r.displayMerchant().ifBlank { "Unknown merchant" }} — $${centsToString(r.totalCents)}")
                appendLine("- Date: ${r.dateIso}")
                appendLine("- Category: ${r.category}")
                appendLine("- Status: ${r.status}")
                appendLine("- Report: ${r.reportName.ifBlank { "—" }}")
                appendLine("- Tag: ${r.tag.ifBlank { "—" }}")
                appendLine("- Payment: ${r.paymentMethod.ifBlank { "—" }}")
                appendLine("- Flags: ${buildFlags(r)}")
                appendLine("- Confidence: ${r.confidence}%")
                if (r.notes.isNotBlank()) appendLine("- Notes: ${r.notes.replace("\n", " ")}")
                appendLine()
            }
            if (excluded.isNotEmpty()) {
                appendLine("## Excluded or personal")
                excluded.sortedByDescending { it.dateIso }.forEach { r ->
                    appendLine("- ${r.dateIso} · ${r.displayMerchant().ifBlank { "Unknown merchant" }} · $${centsToString(r.totalCents)}")
                }
                appendLine()
            }
            appendLine("## Privacy")
            appendLine("This packet was generated entirely on-device. No accounts, cloud sync, or remote analytics were required.")
        }
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(body) }
    }

    suspend fun exportPacketPdf(uri: Uri, resolver: ContentResolver, lane: UserLane, reportName: String, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        val included = visible.filterNot { it.excluded }
        val gm = if (lane == UserLane.GIG_PROFIT) calculateGigMath(included) else null
        val lines = mutableListOf<String>()
        lines += "Receipt Wrangler PDF Packet"
        lines += "Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date())}"
        lines += "Lane: ${lane.label}"
        lines += "Report: ${reportName.ifBlank { "Untitled packet" }}"
        lines += "Included receipts: ${included.size}"
        lines += "Total: $${centsToString(included.sumOf { it.totalCents })}"
        if (gm != null) {
            lines += "Sales: $${centsToString(gm.salesCents)} (${gm.saleCount})"
            lines += "Costs: $${centsToString(gm.expenseCents)} (${gm.expenseCount})"
            lines += "Net: $${centsToString(gm.netCents)}"
        }
        lines += ""
        lines += "Receipts"
        included.sortedByDescending { it.dateIso }.forEach { r ->
            val gigType = if (lane == UserLane.GIG_PROFIT) " · ${inferGigEntryType(r).name.lowercase(Locale.US)}" else ""
            lines += "${r.dateIso} · ${r.displayMerchant().ifBlank { "Unknown merchant" }} · $${centsToString(r.totalCents)}$gigType"
        }
        writePdfWithReceiptImages(uri, resolver, lines, included)
    }

    suspend fun exportGigSalesPdf(uri: Uri, resolver: ContentResolver, reportName: String, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        val sales = visible
            .filterNot { it.excluded }
            .filter { inferGigEntryType(it).name == "SALE" }
            .sortedByDescending { it.dateIso }
        val lines = mutableListOf<String>()
        lines += "Receipt Wrangler Sales Receipt PDF"
        lines += "Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date())}"
        lines += "Report: ${reportName.ifBlank { "Untitled sales report" }}"
        lines += "Sale receipts: ${sales.size}"
        lines += "Gross sales: $${centsToString(sales.sumOf { it.totalCents })}"
        lines += ""
        sales.forEach { r ->
            lines += "${r.dateIso} · ${r.displayMerchant().ifBlank { "Unknown merchant" }} · $${centsToString(r.totalCents)}"
            if (r.paymentMethod.isNotBlank()) lines += "Payment: ${r.paymentMethod}"
            if (r.tag.isNotBlank()) lines += "Tag: ${r.tag}"
            lines += ""
        }
        writePdfWithReceiptImages(uri, resolver, lines, sales)
    }

    suspend fun exportBackup(uri: Uri, resolver: ContentResolver) = withContext(Dispatchers.IO) {
        val receipts = dao.getAllNow()
        val payload = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("receipts", JSONArray(receipts.map { it.toJson() }))
        }
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload.toString(2)) }
    }

    suspend fun restoreBackup(uri: Uri, resolver: ContentResolver) = withContext(Dispatchers.IO) {
        val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@withContext
        val root = JSONObject(text)
        val receipts = mutableListOf<ReceiptEntity>()
        val arr = root.optJSONArray("receipts") ?: JSONArray()
        for (i in 0 until arr.length()) receipts += arr.getJSONObject(i).toReceiptEntity()
        dao.clearAll()
        dao.upsertAll(receipts.map { scoreDuplicates(it.copy(id = 0)) })
    }

    private suspend fun scoreDuplicates(entity: ReceiptEntity): ReceiptEntity {
        val all = dao.getAllNow()
        val score = all.filter { it.id != entity.id }.maxOfOrNull { other ->
            var s = 0
            val merchantA = normalizeMerchant(other.displayMerchant())
            val merchantB = normalizeMerchant(entity.displayMerchant())
            if (merchantA == merchantB && merchantA.isNotBlank()) {
                s += 45
            } else {
                val tokenOverlap = merchantTokenOverlap(other.displayMerchant(), entity.displayMerchant())
                if (tokenOverlap >= 0.8) s += 28
                else if (tokenOverlap >= 0.6) s += 18
            }
            if (other.dateIso == entity.dateIso) {
                s += 25
            } else {
                val dayDiff = daysBetween(other.dateIso, entity.dateIso)
                if (dayDiff in 1..2) s += 12
            }
            val totalDiff = abs(other.totalCents - entity.totalCents)
            when {
                totalDiff <= 1 -> s += 25
                totalDiff <= 25 -> s += 18
                totalDiff <= 100 -> s += 10
            }
            if (other.localCopyPath.isNotBlank() && other.localCopyPath == entity.localCopyPath) s += 10
            min(s, 100)
        } ?: 0
        return entity.copy(duplicateScore = score)
    }

    private fun normalizeMerchant(value: String): String = value.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

    private fun merchantTokenOverlap(a: String, b: String): Double {
        val left = tokenizeMerchant(a)
        val right = tokenizeMerchant(b)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val overlap = left.intersect(right).size.toDouble()
        val maxSize = max(left.size, right.size).toDouble().coerceAtLeast(1.0)
        return overlap / maxSize
    }

    private fun tokenizeMerchant(value: String): Set<String> = value
        .lowercase(Locale.US)
        .split(Regex("[^a-z0-9]+"))
        .map { it.trim() }
        .filter { it.length >= 3 }
        .toSet()

    private fun daysBetween(aIso: String, bIso: String): Int {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val a = runCatching { parser.parse(aIso)?.time }.getOrNull() ?: return Int.MAX_VALUE
        val b = runCatching { parser.parse(bIso)?.time }.getOrNull() ?: return Int.MAX_VALUE
        return ((a - b).absoluteValue / (24L * 60L * 60L * 1000L)).toInt()
    }

    private fun buildDateRange(receipts: List<ReceiptEntity>): String {
        if (receipts.isEmpty()) return "—"
        val sorted = receipts.map { it.dateIso }.filter { it.isNotBlank() }.sorted()
        if (sorted.isEmpty()) return "—"
        return "${sorted.first()} to ${sorted.last()}"
    }

    private fun lanePacketGuidance(lane: UserLane): String = when (lane) {
        UserLane.BUSINESS -> "Business reimbursement packet. Verify reimbursement flags before submission."
        UserLane.TAX -> "Tax prep packet. Keep notes and categories precise for deduction support."
        UserLane.PERSONAL -> "Personal tracking packet. Excluded entries stay outside totals by design."
        UserLane.FREELANCER -> "Freelancer billback packet. Confirm client-facing notes and billable flags."
        UserLane.GIG_PROFIT -> "Gig profit packet. Capture both sales and expense receipts, then review tags for net analysis."
        UserLane.TRUCKER -> "Trucker route packet. Keep trip-tag, fuel, and lodging receipts tightly organized."
        UserLane.PRACTITIONER -> "Practice packet. Keep service, travel, and supply receipts grouped for clean records."
        UserLane.TAX_ACCOUNTANT_SHOEBOX -> "Shoebox triage packet. Clean and categorize intake receipts before filing."
        UserLane.GROUP_TRAVEL_SPLIT -> "Group split packet. Track who paid and who owes for shared travel costs."
    }
    private fun cents(value: Long): String = centsToString(value)
    private fun csvEscape(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun ReceiptEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("projectName", projectName); put("merchant", merchant); put("merchantAlias", merchantAlias); put("dateIso", dateIso)
        put("totalCents", totalCents); put("taxCents", taxCents); put("subtotalCents", subtotalCents)
        put("category", category); put("lane", lane); put("status", status); put("notes", notes); put("tag", tag)
        put("reportName", reportName); put("paymentMethod", paymentMethod); put("reimbursable", reimbursable)
        put("billable", billable); put("excluded", excluded); put("confidence", confidence)
        put("duplicateScore", duplicateScore); put("sourceUri", sourceUri); put("localCopyPath", localCopyPath)
        put("ocrText", ocrText); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toReceiptEntity(): ReceiptEntity = ReceiptEntity(
        id = optLong("id"),
        projectName = optString("projectName", defaultProjectName()),
        merchant = optString("merchant"),
        merchantAlias = optString("merchantAlias"),
        dateIso = optString("dateIso"),
        totalCents = optLong("totalCents"),
        taxCents = optLong("taxCents"),
        subtotalCents = optLong("subtotalCents"),
        category = optString("category"),
        lane = optString("lane"),
        status = optString("status"),
        notes = optString("notes"),
        tag = optString("tag"),
        reportName = optString("reportName"),
        paymentMethod = optString("paymentMethod"),
        reimbursable = optBoolean("reimbursable"),
        billable = optBoolean("billable"),
        excluded = optBoolean("excluded"),
        confidence = optInt("confidence"),
        duplicateScore = optInt("duplicateScore"),
        sourceUri = optString("sourceUri"),
        localCopyPath = optString("localCopyPath"),
        ocrText = optString("ocrText"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )

    private fun longest(current: String, others: List<String>): String =
        (listOf(current) + others).filter { it.isNotBlank() }.maxByOrNull { it.length } ?: current

    private fun chooseDate(primary: ReceiptEntity, others: List<ReceiptEntity>): String {
        return (listOf(primary.dateIso) + others.map { it.dateIso }).filter { it.isNotBlank() }.minOrNull() ?: primary.dateIso
    }

    private fun chooseString(primary: String, others: List<String>): String =
        listOf(primary).plus(others).firstOrNull { it.isNotBlank() } ?: primary

    private fun chooseExistingPath(primary: String, others: List<String>): String =
        listOf(primary).plus(others).firstOrNull { it.isNotBlank() && File(it).exists() } ?: primary

    private fun chooseStatus(primary: ReceiptEntity, others: List<ReceiptEntity>): String {
        val all = listOf(primary.status) + others.map { it.status }
        return when {
            ReceiptStatus.REVIEW.name in all -> ReceiptStatus.REVIEW.name
            ReceiptStatus.INBOX.name in all -> ReceiptStatus.INBOX.name
            ReceiptStatus.READY.name in all -> ReceiptStatus.READY.name
            else -> primary.status
        }
    }

    private fun joinNotes(a: String, b: String): String =
        listOf(a.trim(), b.trim()).filter { it.isNotBlank() }.distinct().joinToString("\n")

    private fun buildFlags(r: ReceiptEntity): String {
        val flags = mutableListOf<String>()
        if (r.reimbursable) flags += "reimbursable"
        if (r.billable) flags += "billable"
        if (r.excluded) flags += "excluded"
        return if (flags.isEmpty()) "none" else flags.joinToString(", ")
    }

    private fun writeSimplePdf(uri: Uri, resolver: ContentResolver, lines: List<String>) {
        val pdf = PdfDocument()
        val width = 595
        val height = 842
        val margin = 36f
        val lineHeight = 16f
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var y = margin

        fun newPage() {
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            y = margin
        }

        lines.forEach { rawLine ->
            val chunks = splitForPdf(rawLine, 92)
            chunks.forEach { line ->
                if (y > height - margin) newPage()
                page.canvas.drawText(line, margin, y, paint)
                y += lineHeight
            }
        }

        pdf.finishPage(page)
        resolver.openOutputStream(uri)?.use { output -> pdf.writeTo(output) }
        pdf.close()
    }

    private fun writePdfWithReceiptImages(
        uri: Uri,
        resolver: ContentResolver,
        lines: List<String>,
        receipts: List<ReceiptEntity>
    ) {
        val pdf = PdfDocument()
        val width = 595
        val height = 842
        val margin = 36f
        val lineHeight = 16f
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var y = margin

        fun newPage() {
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            y = margin
        }

        lines.forEach { rawLine ->
            val chunks = splitForPdf(rawLine, 92)
            chunks.forEach { line ->
                if (y > height - margin) newPage()
                page.canvas.drawText(line, margin, y, paint)
                y += lineHeight
            }
        }

        receipts.forEachIndexed { index, receipt ->
            val image = decodeReceiptBitmap(receipt) ?: return@forEachIndexed
            newPage()
            page.canvas.drawText(
                "Receipt image ${index + 1}/${receipts.size}: ${receipt.displayMerchant().ifBlank { "Unknown merchant" }}",
                margin,
                y,
                paint
            )
            y += lineHeight + 8f

            val maxW = width - (margin * 2)
            val maxH = height - y - margin
            val scale = minOf(maxW / image.width.toFloat(), maxH / image.height.toFloat())
            val drawW = image.width * scale
            val drawH = image.height * scale
            val left = margin
            val top = y
            val dst = android.graphics.RectF(left, top, left + drawW, top + drawH)
            page.canvas.drawBitmap(image, null, dst, null)
        }

        pdf.finishPage(page)
        resolver.openOutputStream(uri)?.use { output -> pdf.writeTo(output) }
        pdf.close()
    }

    private fun decodeReceiptBitmap(receipt: ReceiptEntity): Bitmap? {
        val path = receipt.localCopyPath
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        if (file.extension.equals("pdf", true)) {
            val pfd = runCatching { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull() ?: return null
            return try {
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    pfd.close()
                    return null
                }
                val bmp = renderer.openPage(0).use { page ->
                    val targetW = 900
                    val scale = targetW.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(out, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    out
                }
                renderer.close()
                pfd.close()
                bmp
            } catch (_: Throwable) {
                runCatching { pfd.close() }
                null
            }
        }
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun splitForPdf(line: String, maxChars: Int): List<String> {
        if (line.length <= maxChars) return listOf(line)
        val words = line.split(" ")
        val out = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (candidate.length <= maxChars) current = candidate
            else {
                if (current.isNotBlank()) out += current
                current = word
            }
        }
        if (current.isNotBlank()) out += current
        return out.ifEmpty { listOf(line.take(maxChars)) }
    }

    private fun cropBitmap(source: Bitmap, options: ScanCleanupOptions): Bitmap {
        val left = (source.width * options.cropLeftPct).toInt().coerceIn(0, source.width - 1)
        val top = (source.height * options.cropTopPct).toInt().coerceIn(0, source.height - 1)
        val right = (source.width * (1f - options.cropRightPct)).toInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * (1f - options.cropBottomPct)).toInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun adjustBitmap(source: Bitmap, options: ScanCleanupOptions): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cm = ColorMatrix()
        if (options.grayscale) cm.setSaturation(0f)
        val contrast = options.contrast
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

    // ============ BATCH OPERATIONS ============

    /** Bulk update category for multiple receipts */
    suspend fun batchUpdateCategory(ids: List<Long>, category: ReceiptCategory) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        dao.setCategory(ids, category.name)
    }

    /** Bulk update status for multiple receipts */
    suspend fun batchUpdateStatus(ids: List<Long>, status: ReceiptStatus) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        dao.setStatus(ids, status.name)
    }

    /** Bulk update reimbursable flag */
    suspend fun batchSetReimbursable(ids: List<Long>, reimbursable: Boolean) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val receipts = dao.getAllNow().filter { it.id in ids }
        receipts.forEach { 
            dao.upsert(it.copy(reimbursable = reimbursable, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Bulk update billable flag */
    suspend fun batchSetBillable(ids: List<Long>, billable: Boolean) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val receipts = dao.getAllNow().filter { it.id in ids }
        receipts.forEach { 
            dao.upsert(it.copy(billable = billable, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Bulk delete receipts and their local files */
    suspend fun batchDelete(ids: List<Long>) = withContext(Dispatchers.IO) {
        deleteIds(ids)
    }

    /** Bulk add tag to receipts */
    suspend fun batchAddTag(ids: List<Long>, tag: String) = withContext(Dispatchers.IO) {
        if (ids.isEmpty() || tag.isBlank()) return@withContext
        val receipts = dao.getAllNow().filter { it.id in ids }
        receipts.forEach { receipt ->
            val newTag = if (receipt.tag.isBlank()) tag else "${receipt.tag}, $tag"
            dao.upsert(receipt.copy(tag = newTag, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Bulk assign to report */
    suspend fun batchAssignReport(ids: List<Long>, reportName: String) = withContext(Dispatchers.IO) {
        if (ids.isEmpty() || reportName.isBlank()) return@withContext
        val receipts = dao.getAllNow().filter { it.id in ids }
        receipts.forEach { 
            dao.upsert(it.copy(reportName = reportName, updatedAt = System.currentTimeMillis()))
        }
    }

    // ============ ADVANCED FILTERING ============

    /** Filter receipts by date range */
    fun filterByDateRange(receipts: List<ReceiptEntity>, startDate: String, endDate: String): List<ReceiptEntity> {
        return receipts.filter { it.dateIso >= startDate && it.dateIso <= endDate }
    }

    /** Filter receipts by amount range in cents */
    fun filterByAmountRange(receipts: List<ReceiptEntity>, minCents: Long, maxCents: Long): List<ReceiptEntity> {
        return receipts.filter { it.totalCents >= minCents && it.totalCents <= maxCents }
    }

    /** Filter by OCR confidence threshold */
    fun filterByConfidence(receipts: List<ReceiptEntity>, minConfidence: Int): List<ReceiptEntity> {
        return receipts.filter { it.confidence >= minConfidence }
    }

    /** Filter by duplicate score threshold */
    fun filterByDuplicateScore(receipts: List<ReceiptEntity>, minScore: Int): List<ReceiptEntity> {
        return receipts.filter { it.duplicateScore >= minScore }
    }

    /** Full-text search across merchant, notes, tag, report */
    fun searchReceipts(receipts: List<ReceiptEntity>, query: String): List<ReceiptEntity> {
        if (query.isBlank()) return receipts
        val lowercaseQuery = query.lowercase()
        return receipts.filter { receipt ->
            receipt.merchant.lowercase().contains(lowercaseQuery) ||
            receipt.merchantAlias.lowercase().contains(lowercaseQuery) ||
            receipt.notes.lowercase().contains(lowercaseQuery) ||
            receipt.tag.lowercase().contains(lowercaseQuery) ||
            receipt.reportName.lowercase().contains(lowercaseQuery)
        }
    }

    /** Multi-criterion advanced filter */
    data class AdvancedFilterCriteria(
        val searchQuery: String = "",
        val startDate: String? = null,
        val endDate: String? = null,
        val minAmount: Long? = null,
        val maxAmount: Long? = null,
        val categories: List<String> = emptyList(),
        val statuses: List<String> = emptyList(),
        val minConfidence: Int? = null,
        val reimbursableOnly: Boolean = false,
        val billableOnly: Boolean = false,
        val excludeExcluded: Boolean = false
    )

    fun advancedFilter(receipts: List<ReceiptEntity>, criteria: AdvancedFilterCriteria): List<ReceiptEntity> {
        var filtered = receipts

        // Text search
        if (criteria.searchQuery.isNotBlank()) {
            filtered = searchReceipts(filtered, criteria.searchQuery)
        }

        // Date range
        if (criteria.startDate != null && criteria.endDate != null) {
            filtered = filterByDateRange(filtered, criteria.startDate, criteria.endDate)
        }

        // Amount range
        if (criteria.minAmount != null && criteria.maxAmount != null) {
            filtered = filterByAmountRange(filtered, criteria.minAmount, criteria.maxAmount)
        }

        // Categories
        if (criteria.categories.isNotEmpty()) {
            filtered = filtered.filter { it.category in criteria.categories }
        }

        // Statuses
        if (criteria.statuses.isNotEmpty()) {
            filtered = filtered.filter { it.status in criteria.statuses }
        }

        // Confidence
        if (criteria.minConfidence != null) {
            filtered = filterByConfidence(filtered, criteria.minConfidence)
        }

        // Reimbursable only
        if (criteria.reimbursableOnly) {
            filtered = filtered.filter { it.reimbursable }
        }

        // Billable only
        if (criteria.billableOnly) {
            filtered = filtered.filter { it.billable }
        }

        // Exclude marked as excluded
        if (criteria.excludeExcluded) {
            filtered = filtered.filterNot { it.excluded }
        }

        return filtered
    }

    // ============ EXPORT FORMATS ============

    /** Export as JSON format */
    suspend fun exportJson(uri: Uri, resolver: ContentResolver, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        val json = JSONArray()
        visible.forEach { receipt ->
            json.put(JSONObject().apply {
                put("id", receipt.id)
                put("merchant", receipt.merchant)
                put("date", receipt.dateIso)
                put("amount", receipt.totalCents / 100.0)
                put("category", receipt.category)
                put("status", receipt.status)
                put("confidence", receipt.confidence)
                put("notes", receipt.notes)
            })
        }
        resolver.openOutputStream(uri)?.bufferedWriter()?.use {
            it.write(json.toString(2))
        }
    }

    /** Export as TSV (tab-separated values) */
    suspend fun exportTsv(uri: Uri, resolver: ContentResolver, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        val header = listOf("id", "merchant", "date", "amount", "category", "status", "confidence", "notes")
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.appendLine(header.joinToString("\t"))
            visible.forEach { r ->
                val row = listOf(
                    r.id, r.merchant, r.dateIso, "$${centsToString(r.totalCents)}",
                    r.category, r.status, "${r.confidence}%", r.notes
                )
                writer.appendLine(row.joinToString("\t"))
            }
        }
    }

    /** Export as markdown table */
    suspend fun exportMarkdown(uri: Uri, resolver: ContentResolver, visible: List<ReceiptEntity>) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.appendLine("# Receipt Report")
            writer.appendLine()
            writer.appendLine("| Merchant | Date | Amount | Category | Status | Confidence |")
            writer.appendLine("|----------|------|--------|----------|--------|------------|")
            visible.forEach { r ->
                writer.appendLine("| ${r.merchant} | ${r.dateIso} | $${centsToString(r.totalCents)} | ${r.category} | ${r.status} | ${r.confidence}% |")
            }
            writer.appendLine()
            writer.appendLine(buildString {
                append("**Total**: $${centsToString(visible.sumOf { it.totalCents })} | ")
                append("**Count**: ${visible.size} | ")
                append("**Avg**: $${centsToString(if (visible.isEmpty()) 0 else visible.sumOf { it.totalCents } / visible.size)}")
            })
        }
    }
}
