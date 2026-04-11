/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UserLane(val label: String, val summary: String, val defaultReimbursable: Boolean, val defaultBillable: Boolean) {
    BUSINESS("Business expense", "Company spending that may need review and submission.", true, false),
    TAX("Tax prep", "Keep documentation for deductions and tax-time cleanup.", false, false),
    PERSONAL("Personal", "Private household tracking with no submission workflow.", false, false),
    FREELANCER("Freelancer billback", "Client-billable purchases with proof and notes.", true, true),
    GIG_PROFIT("Gig economy profit wrangler", "Track booth/trailer sales receipts and expense receipts fully offline.", false, false),
    TRUCKER("I'm a trucker", "Track route fuel, repairs, meals, and overnight receipts.", true, false),
    PRACTITIONER("Independent practitioner", "Track practice supplies, travel, and client-session receipts.", false, false),
    TAX_ACCOUNTANT_SHOEBOX("Tax shoebox triage", "Triage messy receipt piles into clean monthly packets.", false, false),
    GROUP_TRAVEL_SPLIT("Group travel split", "Track shared group-travel receipts and split-cost evidence packets.", false, false)
}

enum class ReceiptCategory(val label: String) {
    MEALS("Meals"),
    LODGING("Lodging"),
    GROUND_TRAVEL("Ground travel"),
    AIR_TRAVEL("Air travel"),
    SUPPLIES("Supplies"),
    SOFTWARE("Software"),
    UTILITIES("Utilities"),
    HEALTH("Health"),
    FUEL("Fuel"),
    TAX("Tax / fees"),
    OTHER("Other")
}

enum class ReceiptStatus(val label: String) {
    INBOX("Inbox"),
    REVIEW("Needs review"),
    READY("Ready"),
    ARCHIVED("Archived")
}

enum class GigEntryType { SALE, EXPENSE }

data class GigMathSummary(
    val salesCents: Long = 0,
    val expenseCents: Long = 0,
    val netCents: Long = 0,
    val saleCount: Int = 0,
    val expenseCount: Int = 0
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectName: String = defaultProjectName(),
    val merchant: String = "",
    val merchantAlias: String = "",
    val dateIso: String = todayIso(),
    val totalCents: Long = 0,
    val taxCents: Long = 0,
    val subtotalCents: Long = 0,
    val category: String = ReceiptCategory.OTHER.name,
    val lane: String = UserLane.BUSINESS.name,
    val status: String = ReceiptStatus.INBOX.name,
    val notes: String = "",
    val tag: String = "",
    val reportName: String = "",
    val paymentMethod: String = "",
    val reimbursable: Boolean = true,
    val billable: Boolean = false,
    val excluded: Boolean = false,
    val confidence: Int = 0,
    val duplicateScore: Int = 0,
    val sourceUri: String = "",
    val localCopyPath: String = "",
    val ocrText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ReceiptDraft(
    val id: Long = 0,
    val projectName: String = defaultProjectName(),
    val merchant: String = "",
    val merchantAlias: String = "",
    val dateIso: String = todayIso(),
    val total: String = "",
    val tax: String = "",
    val subtotal: String = "",
    val category: ReceiptCategory = ReceiptCategory.OTHER,
    val lane: UserLane = UserLane.BUSINESS,
    val status: ReceiptStatus = ReceiptStatus.INBOX,
    val notes: String = "",
    val tag: String = "",
    val reportName: String = "",
    val paymentMethod: String = "",
    val reimbursable: Boolean = lane.defaultReimbursable,
    val billable: Boolean = lane.defaultBillable,
    val excluded: Boolean = false,
    val confidence: Int = 0,
    val duplicateScore: Int = 0,
    val sourceUri: String = "",
    val localCopyPath: String = "",
    val ocrText: String = ""
)

data class BackupPayload(
    val exportedAt: Long,
    val receipts: List<ReceiptEntity>
)

data class ScanCleanupOptions(
    val cropLeftPct: Float = 0f,
    val cropTopPct: Float = 0f,
    val cropRightPct: Float = 0f,
    val cropBottomPct: Float = 0f,
    val rotationDegrees: Int = 0,
    val grayscale: Boolean = true,
    val contrast: Float = 1.15f
)

fun ReceiptEntity.displayMerchant(): String = merchantAlias.ifBlank { merchant }

fun ReceiptEntity.toDraft(): ReceiptDraft = ReceiptDraft(
    id = id,
    projectName = projectName,
    merchant = merchant,
    merchantAlias = merchantAlias,
    dateIso = dateIso,
    total = centsToString(totalCents),
    tax = centsToString(taxCents),
    subtotal = centsToString(subtotalCents),
    category = enumValueOrDefault(category, ReceiptCategory.OTHER),
    lane = parseUserLane(lane),
    status = enumValueOrDefault(status, ReceiptStatus.INBOX),
    notes = notes,
    tag = tag,
    reportName = reportName,
    paymentMethod = paymentMethod,
    reimbursable = reimbursable,
    billable = billable,
    excluded = excluded,
    confidence = confidence,
    duplicateScore = duplicateScore,
    sourceUri = sourceUri,
    localCopyPath = localCopyPath,
    ocrText = ocrText
)

fun ReceiptDraft.toEntity(previous: ReceiptEntity? = null): ReceiptEntity = ReceiptEntity(
    id = id,
    projectName = projectName.trim().ifBlank { defaultProjectName() },
    merchant = merchant.trim(),
    merchantAlias = merchantAlias.trim(),
    dateIso = dateIso.trim().ifBlank { todayIso() },
    totalCents = moneyStringToCents(total),
    taxCents = moneyStringToCents(tax),
    subtotalCents = moneyStringToCents(subtotal),
    category = category.name,
    lane = lane.name,
    status = status.name,
    notes = notes.trim(),
    tag = tag.trim(),
    reportName = reportName.trim(),
    paymentMethod = paymentMethod.trim(),
    reimbursable = reimbursable,
    billable = billable,
    excluded = excluded,
    confidence = confidence,
    duplicateScore = duplicateScore,
    sourceUri = sourceUri,
    localCopyPath = localCopyPath,
    ocrText = ocrText,
    createdAt = previous?.createdAt ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

private val legacyPractitionerLaneToken = "WITCH" + "_" + "DOCTOR"

fun parseUserLane(value: String): UserLane = when (value) {
    legacyPractitionerLaneToken -> UserLane.PRACTITIONER
    else -> enumValueOrDefault(value, UserLane.BUSINESS)
}

fun moneyStringToCents(value: String): Long {
    val cleaned = value.trim().replace("$", "").replace(",", "")
    if (cleaned.isBlank()) return 0
    return ((cleaned.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
}

fun centsToString(cents: Long): String = "%.2f".format(Locale.US, cents / 100.0)

fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun defaultProjectName(lane: UserLane = UserLane.BUSINESS, dateIso: String = todayIso()): String {
    val stampedDate = dateIso.ifBlank { todayIso() }
    return when (lane) {
        UserLane.BUSINESS -> "Expense report ($stampedDate)"
        UserLane.TAX -> "Tax deductions ($stampedDate)"
        UserLane.PERSONAL -> "Personal expenses ($stampedDate)"
        UserLane.FREELANCER -> "Client expenses ($stampedDate)"
        UserLane.GIG_PROFIT -> "Sales ($stampedDate)"
        UserLane.TRUCKER -> "Route expenses ($stampedDate)"
        UserLane.PRACTITIONER -> "Practice expenses ($stampedDate)"
        UserLane.TAX_ACCOUNTANT_SHOEBOX -> "Shoebox cleanup ($stampedDate)"
        UserLane.GROUP_TRAVEL_SPLIT -> "Trip split ($stampedDate)"
    }
}

fun defaultReportNameForLane(lane: UserLane, dateIso: String): String {
    val month = dateIso.take(7).ifBlank { todayIso().take(7) }
    return when (lane) {
        UserLane.BUSINESS -> "Business $month"
        UserLane.TAX -> "Tax records $month"
        UserLane.PERSONAL -> "Personal $month"
        UserLane.FREELANCER -> "Client billback $month"
        UserLane.GIG_PROFIT -> "Gig profit $month"
        UserLane.TRUCKER -> "Trucker route $month"
        UserLane.PRACTITIONER -> "Practice ledger $month"
        UserLane.TAX_ACCOUNTANT_SHOEBOX -> "Shoebox cleanup $month"
        UserLane.GROUP_TRAVEL_SPLIT -> "Group travel split $month"
    }
}

fun inferGigEntryType(receipt: ReceiptEntity): GigEntryType {
    val text = listOf(receipt.tag, receipt.notes, receipt.reportName)
        .joinToString(" ")
        .lowercase(Locale.US)
    val saleSignals = listOf("sale", "sales", "income", "revenue", "gross", "collected", "ticket")
    return if (saleSignals.any { it in text }) GigEntryType.SALE else GigEntryType.EXPENSE
}

fun calculateGigMath(receipts: List<ReceiptEntity>): GigMathSummary {
    val included = receipts.filterNot { it.excluded }
    val saleItems = included.filter { inferGigEntryType(it) == GigEntryType.SALE }
    val expenseItems = included.filter { inferGigEntryType(it) == GigEntryType.EXPENSE }
    val sales = saleItems.sumOf { it.totalCents }
    val expenses = expenseItems.sumOf { it.totalCents }
    return GigMathSummary(
        salesCents = sales,
        expenseCents = expenses,
        netCents = sales - expenses,
        saleCount = saleItems.size,
        expenseCount = expenseItems.size
    )
}
