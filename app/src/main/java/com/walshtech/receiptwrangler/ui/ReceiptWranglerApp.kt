/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.walshtech.receiptwrangler.data.ReceiptCategory
import com.walshtech.receiptwrangler.data.ReceiptDraft
import com.walshtech.receiptwrangler.data.ReceiptEntity
import com.walshtech.receiptwrangler.data.ReceiptStatus
import com.walshtech.receiptwrangler.data.ReceiptValidator
import com.walshtech.receiptwrangler.data.ScanCleanupOptions
import com.walshtech.receiptwrangler.data.ThemeMode
import com.walshtech.receiptwrangler.data.UserLane
import com.walshtech.receiptwrangler.data.centsToString
import com.walshtech.receiptwrangler.data.displayMerchant
import com.walshtech.receiptwrangler.ui.theme.Emerald
import com.walshtech.receiptwrangler.ui.theme.Gold
import com.walshtech.receiptwrangler.ui.theme.GoldSoft
import com.walshtech.receiptwrangler.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main application composable for Receipt Wrangler.
 * 
 * Provides the root scaffold with:
 * - Bottom navigation for tab switching (Home, Receipts, Scan, Reports, More)
 * - Dynamic content based on selected tab
 * - Modal dialogs for settings, editing, import/export
 * - Snackbar for toast notifications
 * - Onboarding overlay for first-time users
 *
 * @param viewModel The ReceiptWranglerViewModel managing state
 * @param onImportClick Callback when user selects import action
 * @param onCameraClick Callback when user selects camera action
 * @param onExportCsvClick Callback for CSV export
 * @param onExportPacketClick Callback for packet export
 * @param onExportPacketPdfClick Callback for PDF packet export
 * @param onExportSalesPdfClick Callback for sales PDF export (conditional)
 * @param onExportBackupClick Callback for backup export
 * @param onImportBackupClick Callback for backup import
 */
@Composable
fun ReceiptWranglerApp(
    viewModel: ReceiptWranglerViewModel,
    onImportClick: () -> Unit,
    onCameraClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportPacketClick: () -> Unit,
    onExportPacketPdfClick: () -> Unit,
    onExportSalesPdfClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {},
        topBar = {
            TopBar(
                lane = state.lane,
                projectName = state.currentProject,
                onOptions = { viewModel.showOptionsDialog(true) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    modifier = Modifier,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Gold,
                        selectedTextColor = Gold,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    modifier = Modifier,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Receipt, contentDescription = "Receipts") },
                    label = { Text("Receipts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Gold,
                        selectedTextColor = Gold,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    modifier = Modifier,
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.CameraAlt, contentDescription = "Scan Receipt", modifier = Modifier.size(28.dp)) },
                    label = { Text("Scan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Gold,
                        selectedTextColor = Gold,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    modifier = Modifier,
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Outlined.Summarize, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Gold,
                        selectedTextColor = Gold,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    modifier = Modifier,
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "More Options") },
                    label = { Text("More") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Gold,
                        selectedTextColor = Gold,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedTab == 0) {
                HomeDesk(
                    state = state,
                    onPrimaryScan = onCameraClick,
                    onAdd = viewModel::openNew,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedTab == 1) {
                MainDesk(
                    state = state,
                    viewModel = viewModel,
                    onNew = viewModel::openNew,
                    onImport = onImportClick,
                    onCamera = onCameraClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedTab == 2) {
                ScanQuickStart(
                    onScan = onCameraClick,
                    onImport = onImportClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedTab == 3) {
                LeftRail(state = state, viewModel = viewModel, modifier = Modifier.fillMaxSize())
            } else {
                MoreActionsScreen(
                    lane = state.lane,
                    onImport = onImportClick,
                    onExportCsv = onExportCsvClick,
                    onExportPacket = onExportPacketClick,
                    onExportPacketPdf = onExportPacketPdfClick,
                    onExportSalesPdf = onExportSalesPdfClick,
                    onOptions = { viewModel.showOptionsDialog(true) },
                    onBackup = {
                        viewModel.showBackupDialog(true)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (state.showOptionsDialog) {
        OptionsDialog(
            currentTheme = state.themeMode,
            onDismiss = { viewModel.showOptionsDialog(false) },
            onChooseLane = {
                viewModel.showOptionsDialog(false)
                viewModel.showLaneDialog(true)
            },
            onProjects = {
                viewModel.showOptionsDialog(false)
                viewModel.showProjectDialog(true)
            },
            onBackupRestore = {
                viewModel.showOptionsDialog(false)
                viewModel.showBackupDialog(true)
            },
            onReplayTutorial = { viewModel.replayTutorial() },
            onThemeSelected = viewModel::setThemeMode
        )
    }
    if (state.showLaneDialog) {
        LaneDialog(
            current = state.lane,
            onSelect = { viewModel.setLane(it); viewModel.showLaneDialog(false) },
            onDismiss = { viewModel.showLaneDialog(false) }
        )
    }
    if (state.showProjectDialog) {
        ProjectDialog(
            currentProject = state.currentProject,
            projects = state.projects,
            onDismiss = { viewModel.showProjectDialog(false) },
            onSelect = {
                viewModel.setProject(it)
                viewModel.showProjectDialog(false)
            }
        )
    }
    if (state.showBackupDialog) {
        BackupDialog(
            onDismiss = { viewModel.showBackupDialog(false) },
            onExport = { viewModel.showBackupDialog(false); onExportBackupClick() },
            onImport = { viewModel.showBackupDialog(false); onImportBackupClick() }
        )
    }
    state.editing?.let { draft ->
        ReceiptEditorDialog(draft = draft, onDismiss = viewModel::closeEditor, onSave = viewModel::saveDraft)
    }
    state.cleanupTarget?.let { target ->
        CleanupDialog(receipt = target, onDismiss = viewModel::closeCleanup, onApply = viewModel::applyCleanup)
    }
    if (state.showMergeDialog) {
        MergeDialog(
            receipts = state.mergeCandidates,
            onDismiss = viewModel::closeMergeDialog,
            onMerge = viewModel::mergeSelected
        )
    }

    if (state.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteConfirm,
            title = { Text("Delete receipts?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permanently delete ${state.confirmActionCount} receipt${if (state.confirmActionCount == 1) "" else "s"}?", style = MaterialTheme.typography.bodyMedium)
                    Text("This action cannot be undone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteConfirm) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.showArchiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelArchiveConfirm,
            title = { Text("Archive receipts?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Archive ${state.confirmActionCount} receipt${if (state.confirmActionCount == 1) "" else "s"}?", style = MaterialTheme.typography.bodyMedium)
                    Text("Archived receipts are excluded from reports.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmArchive) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelArchiveConfirm) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.showOnboarding) {
        OnboardingOverlay(
            lane = state.lane,
            currentProject = state.currentProject,
            currentTab = selectedTab,
            onSetLane = viewModel::setLane,
            onSetProject = viewModel::setProject,
            onNavigateTab = { selectedTab = it },
            anchors = emptyMap(),
            onOpenSettings = { viewModel.showOptionsDialog(true) },
            onLaunchCamera = onCameraClick,
            onComplete = viewModel::completeTutorial,
            onSkip = viewModel::completeTutorial
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Top app bar displaying app title, current workflow lane, and project name.
 *
 * @param lane The current user workflow lane
 * @param projectName The name of the currently selected project
 * @param onOptions Callback when settings button is clicked
 */
private fun TopBar(
    lane: UserLane,
    projectName: String,
    onOptions: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = laneColor(lane).copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(laneIcon(lane), contentDescription = null, tint = laneColor(lane), modifier = Modifier.size(24.dp))
                    }
                }
                Column {
                    Text("Receipt Wrangler", style = MaterialTheme.typography.titleLarge)
                    Text("${lane.label} · $projectName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        actions = {
            IconButton(onClick = onOptions) { Icon(Icons.Outlined.Settings, contentDescription = "Options") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
/**
 * Settings and configuration dialog for the Receipt Wrangler app.
 *
 * Allows users to:
 * - Switch between light/dark/system theme modes
 * - Choose their workflow lane (business, gig, freelancer, etc.)
 * - Manage projects
 * - Replay the onboarding tutorial
 * - Access backup and restore options
 *
 * @param currentTheme The currently active theme mode
 * @param onDismiss Called when dialog should be dismissed
 * @param onChooseLane Called when user selects "Choose workflow"
 * @param onProjects Called when user selects "Projects"
 * @param onBackupRestore Called when user selects "Backup and restore"
 * @param onReplayTutorial Called when user selects "Replay first-run tutorial"
 * @param onThemeSelected Called when user selects a theme mode
 */
private fun OptionsDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onChooseLane: () -> Unit,
    onProjects: () -> Unit,
    onBackupRestore: () -> Unit,
    onReplayTutorial: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tune your workflow and controls.", style = MaterialTheme.typography.bodySmall)
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = currentTheme == mode,
                            onClick = { onThemeSelected(mode) },
                            label = { Text(themeModeLabel(mode)) }
                        )
                    }
                }
                OutlinedButton(onClick = onChooseLane, modifier = Modifier.fillMaxWidth()) { Text("Choose workflow") }
                OutlinedButton(onClick = onProjects, modifier = Modifier.fillMaxWidth()) { Text("Projects") }
                OutlinedButton(onClick = onReplayTutorial, modifier = Modifier.fillMaxWidth()) { Text("Replay first-run tutorial") }
                OutlinedButton(onClick = onBackupRestore, modifier = Modifier.fillMaxWidth()) { Text("Backup and restore") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDialog(
    currentProject: String,
    projects: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Projects") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Switch projects or create a new one.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projects.forEach { project ->
                        FilterChip(
                            selected = project == currentProject,
                            onClick = { onSelect(project) },
                            label = { Text(project) }
                        )
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New project name") }
                )
                OutlinedButton(
                    onClick = { onSelect(newName) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newName.isNotBlank()
                ) { Text("Create and switch") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
/**
 * Auto-playing onboarding tutorial overlay shown to first-time users.
 *
 * Features:
 * - 5-card walkthrough covering key app features
 * - Auto-advances every 4 seconds (2 seconds for final card)
 * - Navigates user to relevant tabs
 * - Progress indicator and animated transition
 * - Auto-completes on final card
 *
 * @param lane The current workflow lane
 * @param currentProject The current project name
 * @param currentTab The currently visible tab
 * @param onSetLane Callback to update workflow lane
 * @param onSetProject Callback to update project
 * @param onNavigateTab Callback to navigate to a specific tab
 * @param anchors Layout anchor positions for highlighting (future use)
 * @param onOpenSettings Callback to open settings
 * @param onLaunchCamera Callback to launch camera
 * @param onComplete Callback when tutorial is completed
 * @param onSkip Callback when tutorial is skipped
 */
private fun OnboardingOverlay(
    @Suppress("UNUSED_PARAMETER") lane: UserLane,
    @Suppress("UNUSED_PARAMETER") currentProject: String,
    @Suppress("UNUSED_PARAMETER") currentTab: Int,
    @Suppress("UNUSED_PARAMETER") onSetLane: (UserLane) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSetProject: (String) -> Unit,
    onNavigateTab: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") anchors: Map<String, Rect>,
    @Suppress("UNUSED_PARAMETER") onOpenSettings: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onLaunchCamera: () -> Unit,
    onComplete: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSkip: () -> Unit
) {
    // Simple 5-card walkthrough - auto-plays and auto-dismisses
    val pages = listOf(
        TutorialCard("Welcome", "Receipt Wrangler\nFast capture. Clean reports.", 0),
        TutorialCard("Scan Receipts", "Tap the camera to capture receipts instantly.\nFastest first-task success.", 2),
        TutorialCard("Review & Edit", "View all receipts and edit details.\nOrganize by category and status.", 1),
        TutorialCard("Export Reports", "Generate spend reports and analysis.\nExport as PDF or spreadsheet.", 3),
        TutorialCard("All Set!", "You're ready to go.\nStart capturing receipts now.", 0)
    )
    
    var step by remember { mutableIntStateOf(0) }
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(300), label = "tutorialAlpha")
    
    // Auto-advance every 4 seconds
    LaunchedEffect(step) {
        if (step < pages.size) {
            val delay = if (step == pages.lastIndex) 2000L else 4000L
            delay(delay)
            if (step < pages.lastIndex) {
                step += 1
            } else {
                onNavigateTab(0)
                onComplete()
            }
        }
    }
    
    // Navigate to target tab
    LaunchedEffect(pages[step].targetTab) {
        onNavigateTab(pages[step].targetTab)
    }
    
    // Overlay background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f * alpha))
    ) {
        // Card at bottom center
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.9f)
                .padding(bottom = 80.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card title
                Text(
                    pages[step].title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                
                // Card body
                Text(
                    pages[step].body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (index <= step) Gold else MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }
                
                // Auto-advance progress
                LinearProgressIndicator(
                    progress = { ((step + 1).toFloat() / pages.size.toFloat()) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Gold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

private data class TutorialCard(
    val title: String,
    val body: String,
    val targetTab: Int
)

@Composable
/**
 * Home tab screen with welcome message and recent receipts list.
 *
 * Shows:
 * - Welcome header
 * - Up to 4 most recent receipts
 * - Empty state with call-to-action if no receipts exist
 * - Gold "Add Receipt" button for quick data entry
 *
 * @param state The current UI state containing all receipts
 * @param onPrimaryScan Callback when primary scan button is clicked
 * @param onAdd Callback when add receipt button is clicked
 * @param modifier Modifier for layout control
 */
private fun HomeDesk(
    state: ReceiptUiState,
    @Suppress("UNUSED_PARAMETER") onPrimaryScan: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.EdgePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.SectionGap)
    ) {
        // Welcome header per storyboard
        Text(
            "Welcome to Receipt Wrangler",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Recent Receipts section
        Text(
            "Recent Receipts",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        
        if (state.receipts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "No receipts yet. Tap the gold button below to add your first receipt.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Recent receipts list per storyboard design
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.ListItemGap)) {
                state.receipts.take(4).forEach { receipt ->
                    ReceiptRow(receipt = receipt, onClick = {})
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // Primary CTA - Gold gradient button per storyboard
        GoldButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(AppSpacing.PrimaryButtonHeight)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add Receipt")
            Spacer(Modifier.width(AppSpacing.Base))
            Text("+ Add Receipt", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Receipt row matching storyboard: icon, merchant, date/category, amount with chevron */
@Composable
/**
 * Single receipt row component with merchant icon, details, and amount.
 *
 * Displays:
 * - Merchant initials in a colored box
 * - Merchant name (or "Unknown")
 * - Date and category
 * - Total amount in bold
 * - Navigation chevron
 *
 * @param receipt The receipt entity to display
 * @param onClick Callback when row is clicked
 * @param modifier Modifier for layout control
 */
private fun ReceiptRow(
    receipt: ReceiptEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Merchant icon placeholder
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = receipt.displayMerchant().take(2).uppercase().ifEmpty { "?" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(Modifier.weight(1f)) {
                Text(
                    receipt.displayMerchant().ifBlank { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${receipt.dateIso} · ${receipt.category.replace('_', ' ')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Text(
                "$${centsToString(receipt.totalCents)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "View Receipt Details",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Gold gradient button matching the luxury aesthetic from storyboard */
@Composable
/**
 * Themed button component with gold background and dark text.
 *
 * Used for primary CTAs throughout the app. Features:
 * - Gold background color with dark text for contrast
 * - Rounded corners matching Material 3 design
 * - Support for custom content rendering
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for layout control
 * @param content The composable content to display inside the button
 */
private fun GoldButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Color(0xFF1A1206)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
/**
 * Scan tab screen with prominent camera button and quick-start actions.
 *
 * Displays:
 * - Alignment guidance ("Align Receipt")
 * - Large circular gold camera button (primary CTA)
 * - Secondary full-width "Open camera" button
 * - Import from files button
 * - Messaging emphasizing speed and ease
 *
 * @param onScan Callback when scan/camera action is triggered
 * @param onImport Callback when import action is triggered
 * @param modifier Modifier for layout control
 */
private fun ScanQuickStart(
    onScan: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.EdgePadding, vertical = AppSpacing.Compact),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(AppSpacing.ExtraLarge))
        
        // Title section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Base)
        ) {
            Text(
                "Align Receipt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Auto-Capturing...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Large camera circle button - primary focus per storyboard
        GoldButton(
            onClick = onScan,
            modifier = Modifier.size(120.dp)
        ) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
        }
        
        // Secondary actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Base)
        ) {
            GoldButton(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(AppSpacing.PrimaryButtonHeight)
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "Open Camera")
                Spacer(Modifier.width(AppSpacing.Base))
                Text("Open camera", fontWeight = FontWeight.SemiBold)
            }
            
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().height(AppSpacing.ButtonMinHeight)
            ) {
                Icon(Icons.Outlined.FileOpen, contentDescription = "Import from Files")
                Spacer(Modifier.width(AppSpacing.Base))
                Text("Import from files")
            }
        }
        
        Spacer(Modifier.height(AppSpacing.ExtraLarge))
    }
}

@Composable
/**
 * More actions screen with import, export, and settings options.
 *
 * Organized into sections:
 * - IMPORT: Batch import receipts from files
 * - EXPORT: Multiple export formats (PDF, CSV, spreadsheet)
 *   - Sales PDF export only shown for gig_profit workflow
 * - APP: Settings and backup/restore options
 *
 * @param lane The current user workflow lane
 * @param onImport Callback for batch import
 * @param onExportCsv Callback for CSV export
 * @param onExportPacket Callback for packet export
 * @param onExportPacketPdf Callback for PDF packet export
 * @param onExportSalesPdf Callback for conditional sales PDF export
 * @param onOptions Callback for settings
 * @param onBackup Callback for backup/restore dialog
 * @param modifier Modifier for layout control
 */
private fun MoreActionsScreen(
    lane: UserLane,
    onImport: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPacket: () -> Unit,
    onExportPacketPdf: () -> Unit,
    onExportSalesPdf: () -> Unit,
    onOptions: () -> Unit,
    onBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sales-related workflows that should show sales export
    val isSalesWorkflow = lane == UserLane.GIG_PROFIT
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "More Options",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Import section
        ToolsSection(title = "IMPORT") {
            ToolsMenuItem(
                icon = Icons.Outlined.FileUpload,
                label = "Batch import",
                onClick = onImport
            )
        }
        
        // Export section matching storyboard
        ToolsSection(title = "EXPORT") {
            ToolsMenuItem(
                icon = Icons.Outlined.FileDownload,
                label = "Export report (PDF)",
                onClick = onExportPacketPdf
            )
            ToolsMenuItem(
                icon = Icons.Outlined.FileOpen,
                label = "Export report file",
                onClick = onExportPacket
            )
            ToolsMenuItem(
                icon = Icons.Outlined.Description,
                label = "Export spreadsheet (CSV)",
                onClick = onExportCsv
            )
            
            // Only show sales export for sales-related workflows
            if (isSalesWorkflow) {
                ToolsMenuItem(
                    icon = Icons.Outlined.Summarize,
                    label = "Export sales summary (PDF)",
                    onClick = onExportSalesPdf
                )
            }
        }
        
        // Settings/App section
        ToolsSection(title = "APP") {
            ToolsMenuItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = onOptions
            )
            ToolsMenuItem(
                icon = Icons.Outlined.Backup,
                label = "Backup and restore",
                onClick = onBackup
            )
        }
    }
}

@Composable
/**
 * Grouped section container with title and content items.
 *
 * Used to organize related menu items (Import, Export, App settings).
 *
 * @param title The section header text (appears as secondary text label)
 * @param modifier Modifier for layout control
 * @param content The composable content for section items
 */
private fun ToolsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

@Composable
/**
 * Single menu item with icon, label, and navigation chevron.
 *
 * Displays gold icon on left, action label in center, and chevron on right.
 * Used for all menu items in More Actions screen and dialog menus.
 *
 * @param icon The Material icon to display
 * @param label The action label text
 * @param onClick Callback when item is clicked
 * @param modifier Modifier for layout control
 */
private fun ToolsMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
/**
 * Reports and analysis tab with summary metrics and insights.
 *
 * Displays:
 * - Receipt count and total spend summary
 * - Metrics grid (Spend, Review, Ready, Dupes, Reports)
 * - Status distribution with progress bars
 * - Category breakdown
 * - Gig profit analysis (for gig_profit lane only)
 * - Action queue for recommended next steps
 * - Life admin tips
 *
 * @param state The current UI state with all receipt data
 * @param viewModel The ViewModel for handling status filters
 * @param modifier Modifier for layout control
 */
private fun LeftRail(state: ReceiptUiState, viewModel: ReceiptWranglerViewModel, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, tonalElevation = 1.dp) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.ComponentGap),
            contentPadding = PaddingValues(bottom = AppSpacing.BottomBarHeight)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.ComponentGap)) {
                    Text("Summary & Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${state.receipts.size} receipts · Total: $${centsToString(state.totalAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            item { MetricsGrid(state = state, onStatusClick = viewModel::setStatus) }
            item { SectionTitle("Status Distribution") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceiptStatus.entries.forEach { status ->
                        val count = state.receipts.count { it.status == status.name }
                        val total = state.receipts.map { it.totalCents }.sum()
                        val statusTotal = state.receipts.filter { it.status == status.name }.map { it.totalCents }.sum()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(status.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("$count items · $${centsToString(statusTotal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            LinearProgressIndicator(
                                progress = { if (total > 0) (statusTotal.toFloat() / total) else 0f },
                                modifier = Modifier.width(80.dp).height(4.dp)
                            )
                        }
                    }
                }
            }
            item { SectionTitle("Category Breakdown") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReceiptCategory.entries.forEach { category ->
                        val count = state.receipts.count { it.category == category.name }
                        if (count > 0) {
                            val categoryTotal = state.receipts.filter { it.category == category.name }.map { it.totalCents }.sum()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.label, style = MaterialTheme.typography.bodySmall)
                                Text("$count · $${centsToString(categoryTotal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
            if (state.lane == UserLane.GIG_PROFIT) {
                item { SectionTitle("Gig Profit Analysis") }
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                        Column(Modifier.padding(AppSpacing.Compact), verticalArrangement = Arrangement.spacedBy(AppSpacing.ComponentGap)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    Text("$${centsToString(state.gigSalesCents)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    Text("$${centsToString(state.gigExpenseCents)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Net Profit", style = MaterialTheme.typography.labelMedium)
                                Text("$${centsToString(state.gigNetCents)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (state.coachActions.isNotEmpty()) {
                item { SectionTitle("Action Queue") }
                items(state.coachActions) { action ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { action.statusFilter?.let(viewModel::setStatus) },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(AppSpacing.Compact), verticalArrangement = Arrangement.spacedBy(AppSpacing.Tiny)) {
                            Text(action.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(action.body, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            if (action.statusFilter != null) Text("Tap to filter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            if (state.lifeAdminTips.isNotEmpty()) {
                item { SectionTitle("Tips") }
                items(state.lifeAdminTips) { tip ->
                    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                        Column(Modifier.padding(AppSpacing.Compact), verticalArrangement = Arrangement.spacedBy(AppSpacing.Tiny)) {
                            Text(tip.title, style = MaterialTheme.typography.labelLarge)
                            Text(tip.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Receipts tab screen with search, filtering, and receipt list.
 *
 * Displays:
 * - Quick action buttons (Scan, Add, Import)
 * - Search bar for merchant/notes/tag/report lookup
 * - Filter chips for status and category
 * - Batch selection bar with actions
 * - Scrollable list of receipts with inline actions
 *
 * @param state The current UI state
 * @param viewModel The ViewModel for handling interactions
 * @param onNew Callback when add receipt is clicked
 * @param onImport Callback when import is clicked
 * @param onCamera Callback when camera/scan is clicked
 * @param modifier Modifier for layout control
 */
private fun MainDesk(
    state: ReceiptUiState,
    viewModel: ReceiptWranglerViewModel,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxSize().padding(AppSpacing.Medium), verticalArrangement = Arrangement.spacedBy(AppSpacing.SectionGap)) {
            if (state.receipts.isEmpty()) {
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.SectionGap)
                ) {
                    Text(
                        "Ready to get started?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Your first scan will take 30 seconds. Just point, tap, and review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                    Button(
                        onClick = onCamera,
                        modifier = Modifier.height(56.dp).fillMaxWidth(0.75f)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start your first scan")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNew,
                        modifier = Modifier.height(44.dp).fillMaxWidth(0.75f)
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add manually")
                    }
                }
                Spacer(Modifier.weight(1f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCamera, modifier = Modifier.height(56.dp).fillMaxWidth()) {
                        Icon(Icons.Outlined.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan receipt")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onNew, modifier = Modifier.weight(1f).height(44.dp)) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add")
                        }
                        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f).height(44.dp)) {
                            Icon(Icons.Outlined.FileOpen, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Import")
                        }
                    }
                }
                Text(
                    "Auto-saved on this device. You can leave and resume anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search merchant, notes, tag, report") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }
                )
                FilterBar(state = state, viewModel = viewModel)
                if (state.selectedIds.isNotEmpty()) {
                    BatchBar(state = state, viewModel = viewModel)
                }
                HorizontalDivider()
                ReceiptList(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
/**
 * Section header with divider and title for organizing content.
 *
 * @param text The section title to display
 */
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(2.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun MetricsGrid(state: ReceiptUiState, onStatusClick: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard("Spend", "$${centsToString(state.totalAmount)}", null, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Review", state.needsReview.toString(), ReceiptStatus.REVIEW.name, Modifier.weight(1f), onStatusClick)
            MetricCard("Ready", state.readyCount.toString(), ReceiptStatus.READY.name, Modifier.weight(1f), onStatusClick)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Dupes", state.duplicateCount.toString(), null, Modifier.weight(1f), onStatusClick)
            MetricCard("Reports", state.reportCount.toString(), null, Modifier.weight(1f), onStatusClick)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, status: String?, modifier: Modifier, onStatusClick: (String?) -> Unit = {}) {
    Surface(
        modifier = modifier.clickable(enabled = status != null) { onStatusClick(status) },
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(state: ReceiptUiState, viewModel: ReceiptWranglerViewModel) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = state.selectedStatus == null,
                onClick = { viewModel.setStatus(null) },
                label = { Text("All") },
                leadingIcon = { Icon(Icons.Outlined.FilterAlt, null) }
            )
        }
        items(ReceiptStatus.entries.toList()) {
            FilterChip(
                selected = state.selectedStatus == it.name,
                onClick = { viewModel.setStatus(if (state.selectedStatus == it.name) null else it.name) },
                label = { Text(it.label) }
            )
        }
        items(ReceiptCategory.entries.toList()) {
            FilterChip(
                selected = state.selectedCategory == it.name,
                onClick = { viewModel.setCategory(if (state.selectedCategory == it.name) null else it.name) },
                label = { Text(it.label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchBar(state: ReceiptUiState, viewModel: ReceiptWranglerViewModel) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.isBatchOperating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${state.selectedIds.size} selected · $${centsToString(state.selectionTotal)}", style = MaterialTheme.typography.labelLarge)
                AssistChip(onClick = viewModel::clearSelection, enabled = !state.isBatchOperating, label = { Text("Clear") })
                AssistChip(onClick = viewModel::selectAllVisible, enabled = !state.isBatchOperating, label = { Text("Select visible") })
                AssistChip(onClick = viewModel::openMergeDialog, enabled = !state.isBatchOperating, label = { Text("Merge") }, leadingIcon = { Icon(Icons.Outlined.Merge, null) })
                ReceiptCategory.entries.take(4).forEach { cat ->
                    AssistChip(onClick = { viewModel.markSelectedCategory(cat) }, enabled = !state.isBatchOperating, label = { Text(cat.label) })
                }
                AssistChip(onClick = { viewModel.requestArchiveConfirm() }, enabled = !state.isBatchOperating, label = { Text("Archive") }, leadingIcon = { Icon(Icons.Outlined.Archive, null) })
                AssistChip(onClick = { viewModel.requestDeleteConfirm() }, enabled = !state.isBatchOperating, label = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ReceiptList(state: ReceiptUiState, viewModel: ReceiptWranglerViewModel, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.receipts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(emptyStateTitle(state.lane), style = MaterialTheme.typography.titleSmall)
                        Text(emptyStateBody(state.lane), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        items(state.receipts, key = { it.id }) { receipt ->
            val selected = receipt.id in state.selectedIds
            var contextMenuOpen by remember { mutableStateOf(false) }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    .combinedClickable(
                        onClick = { viewModel.openEdit(receipt) },
                        onLongClick = { contextMenuOpen = true }
                    ),
                tonalElevation = if (selected) 3.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selected, onCheckedChange = { viewModel.toggleSelected(receipt.id) })
                    AsyncImage(
                        model = receipt.localCopyPath.ifBlank { receipt.sourceUri },
                        contentDescription = null,
                        modifier = Modifier.size(54.dp).border(1.dp, MaterialTheme.colorScheme.outline)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(receipt.displayMerchant().ifBlank { "Unknown merchant" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (receipt.duplicateScore >= 70) {
                                Spacer(Modifier.width(8.dp))
                                AssistChip(onClick = { viewModel.toggleSelected(receipt.id) }, label = { Text("Likely duplicate") })
                            }
                            if (receipt.confidence < 60) {
                                Spacer(Modifier.width(8.dp))
                                AssistChip(onClick = { viewModel.openEdit(receipt) }, label = { Text("Low OCR confidence") })
                            }
                        }
                        Text("${receipt.dateIso} · $${centsToString(receipt.totalCents)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(enabled = false, onClick = {}, label = { Text(receipt.category.replace('_', ' ')) })
                            AssistChip(enabled = false, onClick = {}, label = { Text(receipt.status.lowercase()) })
                            if (receipt.reportName.isNotBlank()) AssistChip(enabled = false, onClick = {}, label = { Text(receipt.reportName) })
                        }
                        LinearProgressIndicator(progress = { (receipt.confidence.coerceIn(0, 100)) / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                    Box {
                        IconButton(onClick = { contextMenuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More actions") }
                        DropdownMenu(expanded = contextMenuOpen, onDismissRequest = { contextMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit details") },
                                onClick = { contextMenuOpen = false; viewModel.openEdit(receipt) },
                                leadingIcon = { Icon(Icons.Outlined.AutoFixHigh, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cleanup scan") },
                                onClick = { contextMenuOpen = false; viewModel.openCleanup(receipt) },
                                leadingIcon = { Icon(Icons.Outlined.Tune, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun emptyStateTitle(lane: UserLane): String = when (lane) {
    UserLane.BUSINESS -> "Capture business receipts"
    UserLane.TAX -> "Track tax deductions"
    UserLane.PERSONAL -> "Track personal spending"
    UserLane.FREELANCER -> "Track billable expenses"
    UserLane.GIG_PROFIT -> "Track gig income & expenses"
    UserLane.TRUCKER -> "Track trip expenses"
    UserLane.PRACTITIONER -> "Track practice expenses"
    UserLane.TAX_ACCOUNTANT_SHOEBOX -> "Organize the shoebox"
    UserLane.GROUP_TRAVEL_SPLIT -> "Track shared expenses"
}

private fun emptyStateBody(lane: UserLane): String = when (lane) {
    UserLane.BUSINESS -> "Scan or import to get started."
    UserLane.TAX -> "Scan or import to get started."
    UserLane.PERSONAL -> "Scan or import to get started."
    UserLane.FREELANCER -> "Scan or import to get started."
    UserLane.GIG_PROFIT -> "Scan or import to get started."
    UserLane.TRUCKER -> "Scan or import to get started."
    UserLane.PRACTITIONER -> "Scan or import to get started."
    UserLane.TAX_ACCOUNTANT_SHOEBOX -> "Scan or import to get started."
    UserLane.GROUP_TRAVEL_SPLIT -> "Scan or import to get started."
}

@Composable
private fun laneIcon(lane: UserLane): ImageVector = when (lane) {
    UserLane.BUSINESS -> Icons.Outlined.Description
    UserLane.TAX -> Icons.Outlined.Rule
    UserLane.PERSONAL -> Icons.Outlined.Home
    UserLane.FREELANCER -> Icons.Outlined.ContentCopy
    UserLane.GIG_PROFIT -> Icons.Outlined.TrendingUp
    UserLane.TRUCKER -> Icons.Outlined.LocalShipping
    UserLane.PRACTITIONER -> Icons.Outlined.LocalPharmacy
    UserLane.TAX_ACCOUNTANT_SHOEBOX -> Icons.Outlined.FolderOpen
    UserLane.GROUP_TRAVEL_SPLIT -> Icons.Outlined.Flight
}

private fun laneColor(lane: UserLane): Color = when (lane) {
    UserLane.BUSINESS -> Color(0xFF4A90E2)  // Blue
    UserLane.TAX -> Color(0xFFEB6B6B)       // Red
    UserLane.PERSONAL -> Color(0xFF52C41A)  // Green
    UserLane.FREELANCER -> Color(0xFFFAD94D) // Yellow
    UserLane.GIG_PROFIT -> Color(0xFFF5A623) // Orange
    UserLane.TRUCKER -> Color(0xFF5C6BC0)   // Indigo
    UserLane.PRACTITIONER -> Color(0xFFEC407A) // Pink
    UserLane.TAX_ACCOUNTANT_SHOEBOX -> Color(0xFF7B68EE) // Purple
    UserLane.GROUP_TRAVEL_SPLIT -> Color(0xFF20B2AA) // Teal
}

@Composable
private fun LaneDialog(current: UserLane, onSelect: (UserLane) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose workflow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserLane.entries.forEach { lane ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lane) }
                            .border(2.dp, if (lane == current) laneColor(lane) else Color.Transparent, shape = RoundedCornerShape(8.dp)),
                        tonalElevation = if (lane == current) 2.dp else 0.dp,
                        color = if (lane == current) laneColor(lane).copy(alpha = 0.1f) else Color.Transparent
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(laneIcon(lane), contentDescription = null, tint = laneColor(lane), modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lane.label, fontWeight = FontWeight.SemiBold)
                                Text(lane.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            RadioButton(selected = lane == current, onClick = { onSelect(lane) })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun BackupDialog(onDismiss: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup and restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Everything stays on-device. Backup and restore use user-selected files only.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export backup") }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import backup") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun CleanupDialog(receipt: ReceiptEntity, onDismiss: () -> Unit, onApply: (ScanCleanupOptions) -> Unit) {
    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(0f) }
    var cropBottom by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableIntStateOf(0) }
    var grayscale by remember { mutableStateOf(true) }
    var contrast by remember { mutableFloatStateOf(1.15f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan cleanup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(receipt.displayMerchant().ifBlank { "Receipt" }, style = MaterialTheme.typography.titleSmall)
                Text("Use this when OCR was hurt by glare, crooked framing, or extra table/background junk.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                CropSlider("Crop left", cropLeft) { cropLeft = it }
                CropSlider("Crop top", cropTop) { cropTop = it }
                CropSlider("Crop right", cropRight) { cropRight = it }
                CropSlider("Crop bottom", cropBottom) { cropBottom = it }
                Text("Rotation: $rotation°", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-90, 0, 90, 180).forEach { deg ->
                        AssistChip(onClick = { rotation = deg }, label = { Text(if (deg >= 0) "+$deg" else "$deg") })
                    }
                }
                Text("Contrast: ${"%.2f".format(contrast)}", style = MaterialTheme.typography.labelLarge)
                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.8f..1.8f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = grayscale, onCheckedChange = { grayscale = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Grayscale cleanup")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(
                    ScanCleanupOptions(
                        cropLeftPct = cropLeft,
                        cropTopPct = cropTop,
                        cropRightPct = cropRight,
                        cropBottomPct = cropBottom,
                        rotationDegrees = rotation,
                        grayscale = grayscale,
                        contrast = contrast
                    )
                )
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CropSlider(title: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title: ${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..0.2f)
    }
}

@Composable
private fun MergeDialog(receipts: List<ReceiptEntity>, onDismiss: () -> Unit, onMerge: (Long) -> Unit) {
    var primaryId by remember(receipts) { mutableLongStateOf(receipts.firstOrNull()?.id ?: 0L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge ${receipts.size} receipts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose the receipt to keep. Duplicates will be merged into it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(receipts) { receipt ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = primaryId == receipt.id, onClick = { primaryId = receipt.id }),
                            tonalElevation = if (primaryId == receipt.id) 3.dp else 1.dp
                        ) {
                            Row(Modifier.fillMaxWidth().padding(AppSpacing.Compact), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.Compact)) {
                                RadioButton(selected = primaryId == receipt.id, onClick = { primaryId = receipt.id })
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.Base)) {
                                        Text(receipt.displayMerchant().ifBlank { "Unknown merchant" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                        AssistChip(enabled = false, onClick = {}, label = { Text("${receipt.confidence}% OCR") }, modifier = Modifier.height(24.dp))
                                    }
                                    Text("${receipt.dateIso} · $${centsToString(receipt.totalCents)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    if (receipt.duplicateScore >= 70) {
                                        Text("Duplicate score: ${receipt.duplicateScore}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (primaryId != 0L) onMerge(primaryId) }) { Text("Merge") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiptEditorDialog(draft: ReceiptDraft, onDismiss: () -> Unit, onSave: (ReceiptDraft) -> Unit) {
    var current by remember(draft) { mutableStateOf(draft) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }
    
    val validateField = { field: String, value: String ->
        when (field) {
            "merchant" -> {
                val result = ReceiptValidator.validateMerchant(value)
                if (result.isValid) null else result.errors.firstOrNull()
            }
            "amount" -> {
                val result = ReceiptValidator.validateAmount(value)
                if (result.isValid) null else result.errors.firstOrNull()
            }
            "date" -> {
                val result = ReceiptValidator.validateDate(value)
                if (result.isValid) null else result.errors.firstOrNull()
            }
            else -> null
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (draft.id == 0L) "New receipt" else "Edit receipt") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = current.projectName,
                        onValueChange = { current = current.copy(projectName = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Project") },
                        supportingText = { Text("Where this receipt belongs") }
                    )
                }
                item {
                    val merchantError = validateField("merchant", current.merchant)
                    OutlinedTextField(
                        value = current.merchant,
                        onValueChange = {
                            current = current.copy(merchant = it)
                            errors = errors.toMutableMap().apply {
                                validateField("merchant", it)?.let { err -> put("merchant", err) }
                                    ?: remove("merchant")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Merchant") },
                        isError = merchantError != null,
                        supportingText = { 
                            if (merchantError != null) Text(merchantError, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            else Text("Max 100 characters", style = MaterialTheme.typography.labelSmall)
                        },
                        trailingIcon = {
                            if (merchantError == null && current.merchant.isNotBlank()) {
                                Icon(Icons.Outlined.Add, "Valid", tint = Emerald)
                            }
                        }
                    )
                }
                item {
                    OutlinedTextField(
                        value = current.merchantAlias,
                        onValueChange = { current = current.copy(merchantAlias = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Alias (optional)") },
                        supportingText = { Text("Short name for reports") }
                    )
                }
                item {
                    val dateError = validateField("date", current.dateIso)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = current.dateIso,
                            onValueChange = {
                                current = current.copy(dateIso = it)
                                errors = errors.toMutableMap().apply {
                                    validateField("date", it)?.let { err -> put("date", err) }
                                        ?: remove("date")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Date") },
                            isError = dateError != null,
                            supportingText = { 
                                if (dateError != null) Text(dateError, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                else Text("YYYY-MM-DD", style = MaterialTheme.typography.labelSmall)
                            }
                        )
                        val amountError = validateField("amount", current.total)
                        OutlinedTextField(
                            value = current.total,
                            onValueChange = {
                                current = current.copy(total = it)
                                errors = errors.toMutableMap().apply {
                                    validateField("amount", it)?.let { err -> put("amount", err) }
                                        ?: remove("amount")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Total") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = amountError != null,
                            supportingText = { 
                                if (amountError != null) Text(amountError, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                else Text("Max: 99,999.99", style = MaterialTheme.typography.labelSmall)
                            }
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = current.tax, onValueChange = { current = current.copy(tax = it) }, modifier = Modifier.weight(1f), label = { Text("Tax") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text("Optional") })
                        OutlinedTextField(value = current.subtotal, onValueChange = { current = current.copy(subtotal = it) }, modifier = Modifier.weight(1f), label = { Text("Subtotal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text("Optional") })
                    }
                }
                item {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReceiptCategory.entries.forEach { cat ->
                            FilterChip(selected = current.category == cat, onClick = { current = current.copy(category = cat) }, label = { Text(cat.label) })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = current.reportName,
                        onValueChange = { current = current.copy(reportName = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Report / Trip / Packet") },
                        supportingText = { Text("Group related receipts together") }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = current.tag, onValueChange = { current = current.copy(tag = it) }, modifier = Modifier.weight(1f), label = { Text("Tag") }, supportingText = { Text("sale, expense, etc") })
                        OutlinedTextField(value = current.paymentMethod, onValueChange = { current = current.copy(paymentMethod = it) }, modifier = Modifier.weight(1f), label = { Text("Payment") }, supportingText = { Text("card, cash, etc") })
                    }
                }
                item {
                    Text("Quick Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { current = current.copy(tag = "sale") }, label = { Text("sale") })
                        AssistChip(onClick = { current = current.copy(tag = "expense") }, label = { Text("expense") })
                        AssistChip(onClick = {
                            val next = if (current.notes.isBlank()) "split" else current.notes + "\nsplit"
                            current = current.copy(notes = next)
                        }, label = { Text("split") })
                        AssistChip(onClick = {
                            val next = if (current.notes.isBlank()) "reimbursable" else current.notes + "\nreimbursable"
                            current = current.copy(notes = next)
                        }, label = { Text("reimbursable") })
                    }
                }
                item {
                    OutlinedTextField(value = current.notes, onValueChange = { current = current.copy(notes = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Notes") }, supportingText = { Text("Additional details or comments") })
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = current.reimbursable, onCheckedChange = { current = current.copy(reimbursable = it) })
                            Column(Modifier.weight(1f)) {
                                Text("Reimbursable", style = MaterialTheme.typography.bodySmall)
                                Text("You'll be reimbursed for this", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = current.billable, onCheckedChange = { current = current.copy(billable = it) })
                            Column(Modifier.weight(1f)) {
                                Text("Billable", style = MaterialTheme.typography.bodySmall)
                                Text("Pass cost to customer or client", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = current.excluded, onCheckedChange = { current = current.copy(excluded = it) })
                            Column(Modifier.weight(1f)) {
                                Text("Excluded", style = MaterialTheme.typography.bodySmall)
                                Text("Don't include in reports", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = ReceiptValidator.validateReceipt(current)
                    if (result.isValid) {
                        isSaving = true
                        onSave(current)
                    } else {
                        errors = result.errors.mapIndexed { idx, err ->
                            when {
                                err.contains("Merchant") -> "merchant" to err
                                err.contains("Amount") -> "amount" to err
                                err.contains("Date") -> "date" to err
                                else -> "field_$idx" to err
                            }
                        }.toMap()
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}
