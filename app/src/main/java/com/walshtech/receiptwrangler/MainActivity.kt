/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import com.walshtech.receiptwrangler.camera.ReceiptCameraActivity
import com.walshtech.receiptwrangler.ui.ReceiptWranglerApp
import com.walshtech.receiptwrangler.ui.ReceiptWranglerViewModel
import com.walshtech.receiptwrangler.ui.theme.ReceiptWranglerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ReceiptWranglerViewModel by viewModels {
        ReceiptWranglerViewModel.Factory(application)
    }

    private val importFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importUris(uris)
    }

    private val launchReceiptCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val uri = IntentCompat.getParcelableExtra(data, ReceiptCameraActivity.EXTRA_CAPTURED_URI, Uri::class.java)
        if (uri != null) viewModel.importCapturedUri(uri)
    }

    private val exportCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }

    private val exportBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    private val exportPacket = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) viewModel.exportPacket(uri)
    }

    private val exportPacketPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.exportPacketPdf(uri)
    }

    private val exportSalesPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.exportGigSalesPdf(uri)
    }

    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restoreBackup(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        setContent {
            val state by viewModel.uiState.collectAsState()
            ReceiptWranglerTheme(themeMode = state.themeMode) {
                ReceiptWranglerApp(
                    viewModel = viewModel,
                    onImportClick = { importFiles.launch(arrayOf("image/*", "application/pdf")) },
                    onCameraClick = { launchReceiptCamera.launch(Intent(this, ReceiptCameraActivity::class.java)) },
                    onExportCsvClick = { exportCsv.launch(viewModel.suggestedCsvFileName()) },
                    onExportPacketClick = { exportPacket.launch(viewModel.suggestedPacketFileName()) },
                    onExportPacketPdfClick = { exportPacketPdf.launch(viewModel.suggestedPacketPdfFileName()) },
                    onExportSalesPdfClick = { exportSalesPdf.launch(viewModel.suggestedSalesPdfFileName()) },
                    onExportBackupClick = { exportBackup.launch(viewModel.suggestedBackupFileName()) },
                    onImportBackupClick = { importBackup.launch(arrayOf("application/json")) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val incoming = intent ?: return
        val action = incoming.action ?: return
        when (action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)
                val clipUris = extractClipDataUris(incoming)
                val all = listOfNotNull(uri) + clipUris
                if (all.isNotEmpty()) viewModel.importUris(all.distinct())
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                val uris = incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty() + extractClipDataUris(incoming)
                if (uris.isNotEmpty()) viewModel.importUris(uris.distinct())
            }
            Intent.ACTION_VIEW -> {
                val directUri = incoming.data
                val uris = listOfNotNull(directUri) + extractClipDataUris(incoming)
                if (uris.isNotEmpty()) viewModel.importUris(uris.distinct())
            }
        }
    }

    private fun extractClipDataUris(intent: Intent): List<Uri> {
        val clipData = intent.clipData ?: return emptyList()
        return buildList {
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i)?.uri?.let { add(it) }
            }
        }
    }
}
