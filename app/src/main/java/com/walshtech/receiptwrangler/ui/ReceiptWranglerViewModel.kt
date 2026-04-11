/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.walshtech.receiptwrangler.data.AppDatabase
import com.walshtech.receiptwrangler.data.ReceiptCategory
import com.walshtech.receiptwrangler.data.ReceiptDraft
import com.walshtech.receiptwrangler.data.ReceiptEntity
import com.walshtech.receiptwrangler.data.ReceiptError
import com.walshtech.receiptwrangler.data.ReceiptStatus
import com.walshtech.receiptwrangler.data.ScanCleanupOptions
import com.walshtech.receiptwrangler.data.SettingsStore
import com.walshtech.receiptwrangler.data.ThemeMode
import com.walshtech.receiptwrangler.data.UserLane
import com.walshtech.receiptwrangler.data.calculateGigMath
import com.walshtech.receiptwrangler.data.centsToString
import com.walshtech.receiptwrangler.data.defaultProjectName
import com.walshtech.receiptwrangler.data.displayMerchant
import com.walshtech.receiptwrangler.data.toDraft
import com.walshtech.receiptwrangler.ocr.ReceiptOcrEngine
import com.walshtech.receiptwrangler.repo.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptWranglerViewModel(
    private val settingsStore: SettingsStore,
    private val repository: ReceiptRepository,
    private val resolver: ContentResolver
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedStatus = MutableStateFlow<String?>(null)
    private val toast = MutableStateFlow<String?>(null)
    private val editing = MutableStateFlow<ReceiptDraft?>(null)
    private val selectedIds = MutableStateFlow(setOf<Long>())
    private val showOptionsDialog = MutableStateFlow(false)
    private val showLaneDialog = MutableStateFlow(false)
    private val showProjectDialog = MutableStateFlow(false)
    private val showBackupDialog = MutableStateFlow(false)
    private val cleanupTargetId = MutableStateFlow<Long?>(null)
    private val showMergeDialog = MutableStateFlow(false)
    private val forceOnboarding = MutableStateFlow(false)
    private val showDeleteConfirmDialog = MutableStateFlow(false)
    private val showArchiveConfirmDialog = MutableStateFlow(false)
    private val confirmActionCount = MutableStateFlow(0)  // Track count for confirmation message
    private val isBatchOperating = MutableStateFlow(false)  // Show progress during batch ops

    private val showOnboarding = combine(settingsStore.tutorialSeenFlow, forceOnboarding) { seen, forced ->
        !seen || forced
    }

    private val coreInputs = combine(
        repository.receipts,
        settingsStore.laneFlow,
        settingsStore.projectNameFlow,
        settingsStore.reportNameFlow
    ) { receipts, lane, projectName, reportName ->
        UiInputs(
            receipts = receipts,
            lane = lane,
            projectName = projectName,
            reportName = reportName
        )
    }

    private val uiInputs = coreInputs.combine(query) { current, q ->
        current.copy(query = q)
    }.combine(selectedCategory) { current, category ->
        current.copy(category = category)
    }.combine(settingsStore.themeModeFlow) { current, themeMode ->
        current.copy(themeMode = themeMode)
    }.combine(selectedStatus) { current, status ->
        current.copy(status = status)
    }.combine(toast) { current, toastText ->
        current.copy(toastText = toastText)
    }.combine(editing) { current, draft ->
        current.copy(draft = draft)
    }.combine(selectedIds) { current, selected ->
        current.copy(selected = selected)
    }.combine(showOptionsDialog) { current, optionsDialog ->
        current.copy(optionsDialog = optionsDialog)
    }.combine(showLaneDialog) { current, laneDialog ->
        current.copy(laneDialog = laneDialog)
    }.combine(showProjectDialog) { current, projectDialog ->
        current.copy(projectDialog = projectDialog)
    }.combine(showBackupDialog) { current, backupDialog ->
        current.copy(backupDialog = backupDialog)
    }.combine(cleanupTargetId) { current, cleanupId ->
        current.copy(cleanupId = cleanupId)
    }.combine(showMergeDialog) { current, mergeDialog ->
        current.copy(mergeDialog = mergeDialog)
    }.combine(showOnboarding) { current, onboarding ->
        current.copy(showOnboarding = onboarding)
    }.combine(showDeleteConfirmDialog) { current, deleteDialog ->
        current.copy(showDeleteConfirmDialog = deleteDialog)
    }.combine(showArchiveConfirmDialog) { current, archiveDialog ->
        current.copy(showArchiveConfirmDialog = archiveDialog)
    }.combine(confirmActionCount) { current, count ->
        current.copy(confirmActionCount = count)
    }.combine(isBatchOperating) { current, isBatchOp ->
        current.copy(isBatchOperating = isBatchOp)
    }

    val uiState: StateFlow<ReceiptUiState> = uiInputs.map { input ->
        val laneReceipts = input.receipts.filter {
            it.lane == input.lane.name && it.projectName == input.projectName
        }
        val projects = (input.receipts.map { it.projectName }.filter { it.isNotBlank() } + input.projectName)
            .distinct()
            .sorted()
        val visible = laneReceipts.filter { receipt ->
            (input.category == null || receipt.category == input.category) &&
                (input.status == null || receipt.status == input.status) &&
                (input.query.isBlank() || receipt.displayMerchant().contains(input.query, true) || receipt.notes.contains(input.query, true) || receipt.tag.contains(input.query, true) || receipt.reportName.contains(input.query, true))
        }
        val selectedReceipts = visible.filter { it.id in input.selected }
        val cleanupTarget = laneReceipts.firstOrNull { it.id == input.cleanupId }
        val nowMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthReceipts = laneReceipts.filter { it.dateIso.startsWith(nowMonth) && !it.excluded }
        val reimbursePending = laneReceipts.count { (it.reimbursable || it.billable) && it.status != ReceiptStatus.ARCHIVED.name && !it.excluded }
        val missingData = laneReceipts.count { it.displayMerchant().isBlank() || it.totalCents <= 0 || it.dateIso.isBlank() || it.confidence < 65 }
        val duplicateCandidates = laneReceipts.filter { it.duplicateScore >= 70 }
        val gigMath = if (input.lane == UserLane.GIG_PROFIT) calculateGigMath(laneReceipts) else null
        ReceiptUiState(
            lane = input.lane,
            currentProject = input.projectName,
            projects = projects,
            laneReportName = input.reportName,
            themeMode = input.themeMode,
            receipts = visible,
            selectedCategory = input.category,
            selectedStatus = input.status,
            query = input.query,
            toast = input.toastText,
            editing = input.draft,
            selectedIds = input.selected,
            showOptionsDialog = input.optionsDialog,
            showLaneDialog = input.laneDialog,
            showProjectDialog = input.projectDialog,
            showBackupDialog = input.backupDialog,
            cleanupTarget = cleanupTarget,
            showMergeDialog = input.mergeDialog,
            showOnboarding = input.showOnboarding,
            mergeCandidates = selectedReceipts,
            totalAmount = visible.filterNot { it.excluded }.sumOf { it.totalCents },
            needsReview = visible.count { it.status == ReceiptStatus.REVIEW.name || it.confidence < 70 },
            readyCount = visible.count { it.status == ReceiptStatus.READY.name },
            duplicateCount = duplicateCandidates.size,
            excludedCount = visible.count { it.excluded },
            reportCount = visible.map { it.reportName }.filter { it.isNotBlank() }.distinct().count(),
            monthSpend = monthReceipts.sumOf { it.totalCents },
            monthCount = monthReceipts.size,
            reimbursementPending = reimbursePending,
            taxCandidateCount = laneReceipts.count { it.category != ReceiptCategory.OTHER.name && !it.excluded },
            missingDataCount = missingData,
            coachActions = buildCoachActions(input.lane, reimbursePending, missingData, duplicateCandidates.size),
            lifeAdminTips = buildLifeAdminTips(input.lane, laneReceipts, monthReceipts, missingData, reimbursePending),
            selectionTotal = selectedReceipts.sumOf { it.totalCents },
            gigSalesCents = gigMath?.salesCents ?: 0,
            gigExpenseCents = gigMath?.expenseCents ?: 0,
            gigNetCents = gigMath?.netCents ?: 0,
            gigSaleCount = gigMath?.saleCount ?: 0,
            gigExpenseCount = gigMath?.expenseCount ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptUiState())

    private fun buildCoachActions(lane: UserLane, reimbursePending: Int, missingData: Int, duplicateCount: Int): List<CoachAction> {
        val items = mutableListOf<CoachAction>()
        if (missingData > 0) items += CoachAction("Needs review", "$missingData receipt(s) are missing key fields or have low OCR confidence.", ReceiptStatus.REVIEW.name)
        if (duplicateCount > 0) items += CoachAction("Duplicate review", "$duplicateCount likely duplicate receipt(s) need merge review.", null)
        when (lane) {
            UserLane.BUSINESS -> if (reimbursePending > 0) items += CoachAction("Ready for report", "$reimbursePending reimbursable item(s) still need packet review.", ReceiptStatus.READY.name)
            UserLane.FREELANCER -> if (reimbursePending > 0) items += CoachAction("Billback queue", "$reimbursePending billable item(s) should be grouped by client/report name.", ReceiptStatus.READY.name)
            UserLane.TAX -> items += CoachAction("Tax prep lane", "Confirm category and notes now to reduce tax-time cleanup.", null)
            UserLane.PERSONAL -> items += CoachAction("Personal tracking", "Mark personal-only receipts as excluded before export.", null)
            UserLane.GIG_PROFIT -> items += CoachAction("Profit workflow", "Tag sales vs expense receipts in notes and export monthly packets.", null)
            UserLane.TRUCKER -> items += CoachAction("Route packet", "Group route receipts by trip week before submitting reimbursement.", null)
            UserLane.PRACTITIONER -> items += CoachAction("Practice ledger", "Keep service and supply receipts categorized for monthly review.", null)
            UserLane.TAX_ACCOUNTANT_SHOEBOX -> items += CoachAction("Shoebox triage", "Move receipts from Inbox to Review, then assign category and date.", ReceiptStatus.REVIEW.name)
            UserLane.GROUP_TRAVEL_SPLIT -> items += CoachAction("Split-cost packet", "Use tags for payer/member and export one packet per trip.", null)
        }
        return items.take(4)
    }

    private fun buildLifeAdminTips(lane: UserLane, laneReceipts: List<ReceiptEntity>, monthReceipts: List<ReceiptEntity>, missingData: Int, reimbursePending: Int): List<LifeAdminTip> {
        val tips = mutableListOf<LifeAdminTip>()
        if (missingData > 0) tips += LifeAdminTip("Low confidence", "Use Scan cleanup and correct fields before moving receipts to Ready.")
        if (laneReceipts.count { it.duplicateScore >= 70 } > 0) tips += LifeAdminTip("Possible duplicates", "Review duplicates in batches and keep the cleanest source image.")
        if (monthReceipts.isEmpty()) tips += LifeAdminTip("Start capture", "Capture at purchase time to avoid missing dates and amounts.")
        if (lane != UserLane.PERSONAL && reimbursePending > 0) tips += LifeAdminTip("Packet workflow", "Set report name, verify totals, then export packet for submission.")
        if (lane == UserLane.GIG_PROFIT) tips += LifeAdminTip("Sales vs expense", "Use tags like sale or expense so monthly packet review is fast.")
        if (lane == UserLane.TRUCKER) tips += LifeAdminTip("DOT season", "Keep fuel and lodging receipts tight; audits are easier with clear trip tags.")
        if (lane == UserLane.TAX_ACCOUNTANT_SHOEBOX) tips += LifeAdminTip("Messy intake", "Start with date and total first. Merchant aliasing can happen after triage.")
        if (lane == UserLane.GROUP_TRAVEL_SPLIT) tips += LifeAdminTip("Group split", "Tag each receipt with payer and group to simplify split reconciliation.")
        return tips.take(4)
    }

    fun importUris(uris: List<Uri>) = viewModelScope.launch {
        val lane = uiState.value.lane
        val projectName = uiState.value.currentProject
        repository.importUris(uris, resolver, lane, projectName)
        toast.value = if (uris.size == 1) "Imported 1 file" else "Imported ${uris.size} files"
    }

    fun importCapturedUri(uri: Uri) = importUris(listOf(uri))

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        repository.exportCsv(uri, resolver, uiState.value.receipts)
        toast.value = "CSV exported"
    }

    fun exportPacket(uri: Uri) = viewModelScope.launch {
        repository.exportReportPacket(uri, resolver, uiState.value.lane, uiState.value.laneReportName, exportScope())
        toast.value = "Packet exported"
    }

    fun exportPacketPdf(uri: Uri) = viewModelScope.launch {
        repository.exportPacketPdf(uri, resolver, uiState.value.lane, uiState.value.laneReportName, exportScope())
        toast.value = "PDF packet exported"
    }

    fun exportGigSalesPdf(uri: Uri) = viewModelScope.launch {
        repository.exportGigSalesPdf(uri, resolver, uiState.value.laneReportName, exportScope())
        toast.value = "Sales PDF exported"
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        repository.exportBackup(uri, resolver)
        toast.value = "Backup exported"
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        repository.restoreBackup(uri, resolver)
        toast.value = "Backup restored"
    }

    // New export format methods with user-friendly error messages
    fun exportJson(uri: Uri) = viewModelScope.launch {
        try {
            repository.exportJson(uri, resolver, exportScope())
            toast.value = "JSON exported successfully"
        } catch (e: Exception) {
            toast.value = userFriendlyError(e)
        }
    }

    fun exportTsv(uri: Uri) = viewModelScope.launch {
        try {
            repository.exportTsv(uri, resolver, exportScope())
            toast.value = "TSV exported successfully"
        } catch (e: Exception) {
            toast.value = userFriendlyError(e)
        }
    }

    fun exportMarkdown(uri: Uri) = viewModelScope.launch {
        try {
            repository.exportMarkdown(uri, resolver, exportScope())
            toast.value = "Markdown exported successfully"
        } catch (e: Exception) {
            toast.value = userFriendlyError(e)
        }
    }

    private fun userFriendlyError(e: Exception): String = when (e) {
        is ReceiptError.ValidationError -> "Invalid ${e.field}: ${e.reason}"
        is ReceiptError.FileError -> "Couldn't create file. Check phone storage is available."
        is ReceiptError.DuplicateError -> "Found ${e.duplicateCount} similar receipt(s). Review before saving."
        is ReceiptError.DataNotFoundError -> "Receipt not found. It may have been deleted."
        else -> when {
            e.message?.contains("Storage", ignoreCase = true) == true -> "Phone storage is full. Delete some files and try again."
            e.message?.contains("Permission", ignoreCase = true) == true -> "Permission denied. Check app permissions in Settings."
            e.message?.contains("Network", ignoreCase = true) == true -> "Network error. Check your connection."
            else -> "Something went wrong. Please try again."
        }
    }

    fun openNew() {
        editing.value = ReceiptDraft(
            projectName = uiState.value.currentProject,
            lane = uiState.value.lane,
            reportName = uiState.value.laneReportName,
            reimbursable = uiState.value.lane.defaultReimbursable,
            billable = uiState.value.lane.defaultBillable
        )
    }

    fun openEdit(receipt: ReceiptEntity) { editing.value = receipt.toDraft() }
    fun closeEditor() { editing.value = null }

    fun saveDraft(draft: ReceiptDraft) = viewModelScope.launch {
        repository.saveDraft(draft)
        settingsStore.setReportName(draft.reportName)
        editing.value = null
        toast.value = if (draft.id == 0L) "Receipt saved" else "Receipt updated"
    }

    fun setLane(lane: UserLane) = viewModelScope.launch {
        val previousLane = uiState.value.lane
        val currentProject = uiState.value.currentProject
        val previousDefault = defaultProjectName(previousLane)
        val shouldResetProject = currentProject.isBlank() ||
            currentProject.equals("Main stack", ignoreCase = true) ||
            currentProject == previousDefault

        settingsStore.setLane(lane)
        if (shouldResetProject) {
            settingsStore.setProjectName(defaultProjectName(lane))
        }
        selectedIds.value = emptySet()
        toast.value = lane.summary
    }

    fun setProject(name: String) = viewModelScope.launch {
        val trimmed = name.trim().ifBlank { defaultProjectName(uiState.value.lane) }
        settingsStore.setProjectName(trimmed)
        selectedIds.value = emptySet()
        toast.value = "Project: $trimmed"
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsStore.setThemeMode(mode)
        toast.value = "Theme set to ${mode.name.lowercase(Locale.US)}"
    }

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: String?) { selectedCategory.value = value }
    fun setStatus(value: String?) { selectedStatus.value = value }
    fun clearToast() { toast.value = null }
    fun toggleSelected(id: Long) { selectedIds.value = selectedIds.value.toMutableSet().apply { if (!add(id)) remove(id) } }
    fun clearSelection() { selectedIds.value = emptySet() }
    fun selectAllVisible() { selectedIds.value = uiState.value.receipts.map { it.id }.toSet() }

    fun deleteSelected() = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        isBatchOperating.value = true
        try {
            repository.deleteIds(ids)
            selectedIds.value = emptySet()
            toast.value = "Deleted ${ids.size} receipt${if (ids.size == 1) "" else "s"}"
        } finally {
            isBatchOperating.value = false
        }
    }

    fun markSelectedCategory(category: ReceiptCategory) = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        isBatchOperating.value = true
        try {
            repository.changeCategory(ids, category)
            toast.value = "Updated ${ids.size} receipt${if (ids.size == 1) "" else "s"} to ${category.label}"
        } finally {
            isBatchOperating.value = false
        }
    }

    fun markSelectedStatus(status: ReceiptStatus) = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        isBatchOperating.value = true
        try {
            repository.changeStatus(ids, status)
            toast.value = "Updated status to ${status.label}"
        } finally {
            isBatchOperating.value = false
        }
    }

    // New batch operation methods for features
    fun batchSetReimbursable(reimbursable: Boolean) = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        isBatchOperating.value = true
        try {
            repository.batchSetReimbursable(ids, reimbursable)
            selectedIds.value = emptySet()
            toast.value = "Reimbursable flag updated for ${ids.size} receipt(s)"
        } finally {
            isBatchOperating.value = false
        }
    }

    fun batchSetBillable(billable: Boolean) = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        isBatchOperating.value = true
        try {
            repository.batchSetBillable(ids, billable)
            selectedIds.value = emptySet()
            toast.value = "Billable flag updated for ${ids.size} receipt(s)"
        } finally {
            isBatchOperating.value = false
        }
    }

    fun batchAddTag(tag: String) = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        if (tag.isBlank()) {
            toast.value = "Tag cannot be empty"
            return@launch
        }
        isBatchOperating.value = true
        try {
            repository.batchAddTag(ids, tag)
            selectedIds.value = emptySet()
            toast.value = "Tag '$tag' added to ${ids.size} receipt(s)"
        } finally {
            isBatchOperating.value = false
        }
    }

    fun batchAssignReport(reportName: String) = viewModelScope.launch {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        if (reportName.isBlank()) {
            toast.value = "Report name cannot be empty"
            return@launch
        }
        isBatchOperating.value = true
        try {
            repository.batchAssignReport(ids, reportName)
            selectedIds.value = emptySet()
            toast.value = "Assigned ${ids.size} receipt(s) to report '$reportName'"
        } finally {
            isBatchOperating.value = false
        }
    }

    fun openCleanup(receipt: ReceiptEntity) { cleanupTargetId.value = receipt.id }
    fun closeCleanup() { cleanupTargetId.value = null }

    fun applyCleanup(options: ScanCleanupOptions) = viewModelScope.launch {
        val id = cleanupTargetId.value ?: return@launch
        repository.applyCleanup(id, options)
        cleanupTargetId.value = null
        toast.value = "Scan cleanup applied"
    }

    fun openMergeDialog() {
        if (uiState.value.selectedIds.size < 2) {
            toast.value = "Select at least two receipts to merge"
        } else {
            showMergeDialog.value = true
        }
    }

    fun closeMergeDialog() { showMergeDialog.value = false }

    fun mergeSelected(primaryId: Long) = viewModelScope.launch {
        val ids = uiState.value.selectedIds.toList()
        repository.mergeReceipts(primaryId, ids.filterNot { it == primaryId })
        showMergeDialog.value = false
        selectedIds.value = emptySet()
        toast.value = "Receipts merged"
    }

    fun showOptionsDialog(show: Boolean) { showOptionsDialog.value = show }
    fun showLaneDialog(show: Boolean) { showLaneDialog.value = show }
    fun showProjectDialog(show: Boolean) { showProjectDialog.value = show }
    fun showBackupDialog(show: Boolean) { showBackupDialog.value = show }
    
    fun requestDeleteConfirm() {
        val count = selectedIds.value.size
        if (count > 0) {
            confirmActionCount.value = count
            showDeleteConfirmDialog.value = true
        }
    }
    
    fun cancelDeleteConfirm() { showDeleteConfirmDialog.value = false }
    
    fun confirmDelete() = viewModelScope.launch {
        showDeleteConfirmDialog.value = false
        deleteSelected()
    }
    
    fun requestArchiveConfirm() {
        val count = selectedIds.value.size
        if (count > 0) {
            confirmActionCount.value = count
            showArchiveConfirmDialog.value = true
        }
    }
    
    fun cancelArchiveConfirm() { showArchiveConfirmDialog.value = false }
    
    fun confirmArchive() = viewModelScope.launch {
        showArchiveConfirmDialog.value = false
        markSelectedStatus(ReceiptStatus.ARCHIVED)
    }

    fun replayTutorial() {
        showOptionsDialog.value = false
        forceOnboarding.value = true
    }

    fun completeTutorial() = viewModelScope.launch {
        settingsStore.setTutorialSeen(true)
        forceOnboarding.value = false
    }

    fun suggestedCsvFileName(): String = "receipt-wrangler-${todayStamp()}.csv"
    fun suggestedPacketFileName(): String = "receipt-wrangler-packet-${todayStamp()}.md"
    fun suggestedPacketPdfFileName(): String = "receipt-wrangler-packet-${todayStamp()}.pdf"
    fun suggestedSalesPdfFileName(): String = "receipt-wrangler-sales-${todayStamp()}.pdf"
    fun suggestedBackupFileName(): String = "receipt-wrangler-backup-${todayStamp()}.json"

    private fun exportScope(): List<ReceiptEntity> {
        val selected = uiState.value.selectedIds
        return if (selected.isNotEmpty()) uiState.value.receipts.filter { it.id in selected } else uiState.value.receipts
    }

    private fun todayStamp(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.get(application)
            val settings = SettingsStore(application)
            val repo = ReceiptRepository(application, db.receiptDao(), ReceiptOcrEngine(application))
            return ReceiptWranglerViewModel(settings, repo, application.contentResolver) as T
        }
    }
}

data class CoachAction(val title: String, val body: String, val statusFilter: String?)
data class LifeAdminTip(val title: String, val body: String)

data class ReceiptUiState(
    val lane: UserLane = UserLane.BUSINESS,
    val currentProject: String = defaultProjectName(),
    val projects: List<String> = listOf(defaultProjectName()),
    val laneReportName: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val receipts: List<ReceiptEntity> = emptyList(),
    val selectedCategory: String? = null,
    val selectedStatus: String? = null,
    val query: String = "",
    val toast: String? = null,
    val editing: ReceiptDraft? = null,
    val selectedIds: Set<Long> = emptySet(),
    val showOptionsDialog: Boolean = false,
    val showLaneDialog: Boolean = false,
    val showProjectDialog: Boolean = false,
    val showBackupDialog: Boolean = false,
    val cleanupTarget: ReceiptEntity? = null,
    val showMergeDialog: Boolean = false,
    val showOnboarding: Boolean = false,
    val mergeCandidates: List<ReceiptEntity> = emptyList(),
    val totalAmount: Long = 0,
    val needsReview: Int = 0,
    val readyCount: Int = 0,
    val duplicateCount: Int = 0,
    val excludedCount: Int = 0,
    val reportCount: Int = 0,
    val monthSpend: Long = 0,
    val monthCount: Int = 0,
    val reimbursementPending: Int = 0,
    val taxCandidateCount: Int = 0,
    val missingDataCount: Int = 0,
    val coachActions: List<CoachAction> = emptyList(),
    val lifeAdminTips: List<LifeAdminTip> = emptyList(),
    val showDeleteConfirmDialog: Boolean = false,
    val showArchiveConfirmDialog: Boolean = false,
    val confirmActionCount: Int = 0,
    val isBatchOperating: Boolean = false,
    val selectionTotal: Long = 0,
    val gigSalesCents: Long = 0,
    val gigExpenseCents: Long = 0,
    val gigNetCents: Long = 0,
    val gigSaleCount: Int = 0,
    val gigExpenseCount: Int = 0
)

private data class UiInputs(
    val receipts: List<ReceiptEntity> = emptyList(),
    val lane: UserLane = UserLane.BUSINESS,
    val projectName: String = defaultProjectName(),
    val reportName: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val query: String = "",
    val category: String? = null,
    val status: String? = null,
    val toastText: String? = null,
    val draft: ReceiptDraft? = null,
    val selected: Set<Long> = emptySet(),
    val optionsDialog: Boolean = false,
    val laneDialog: Boolean = false,
    val projectDialog: Boolean = false,
    val backupDialog: Boolean = false,
    val cleanupId: Long? = null,
    val mergeDialog: Boolean = false,
    val showOnboarding: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showArchiveConfirmDialog: Boolean = false,
    val confirmActionCount: Int = 0,
    val isBatchOperating: Boolean = false
)
