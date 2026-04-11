/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

/**
 * Extended settings for user customization and preferences
 */
data class ExtendedSettings(
    // Scanning preferences
    val autoRotateReceipts: Boolean = true,
    val autoEnhanceImages: Boolean = true,
    val saveScanCopies: Boolean = true,
    val maxImageSizeMb: Int = 50,
    
    // OCR preferences
    val ocrLanguages: String = "en",      // Comma-separated, e.g., "en,es,fr"
    val minOcrConfidence: Int = 70,
    val warnLowConfidence: Boolean = true,
    
    // Duplicate detection
    val enableDuplicateDetection: Boolean = true,
    val duplicateThreshold: Int = 70,
    val autoMergeDuplicates: Boolean = false,
    
    // Export preferences
    val defaultExportFormat: String = "csv",  // csv, pdf, json, tsv, markdown
    val includeExcludedInExport: Boolean = false,
    
    // Sorting & display
    val defaultSortBy: String = "date_desc",  // date_asc, date_desc, amount_asc, amount_desc, merchant
    val receiptsPerPage: Int = 50,
    val showConfidenceScore: Boolean = true,
    val showDuplicateScore: Boolean = true,
    
    // Notifications
    val notifyLowConfidence: Boolean = true,
    val notifyDuplicates: Boolean = true,
    val dailySummary: Boolean = false,
    
    // Performance
    val enableQueryCache: Boolean = true,
    val cacheTtlMinutes: Int = 5,
    val batchOperationSize: Int = 50,
    
    // Data management
    val autoDeleteOldScans: Boolean = false,
    val autoDeleteAfterDays: Int = 30,
    val compressOldImages: Boolean = true,
    
    // Privacy & Security
    val requireAuthForSensitive: Boolean = false,
    val maskConfidentialData: Boolean = false,
    val encryptLocalBackup: Boolean = true
)
