/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.repo

import android.content.Context
import com.walshtech.receiptwrangler.data.ReceiptDao
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup and sync management for local and cloud storage
 */
class BackupManager(
    private val context: Context,
    private val dao: ReceiptDao
) {
    
    private val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
    
    data class BackupMetadata(
        val timestamp: Long,
        val receiptCount: Int,
        val appVersion: String,
        val androidVersion: Int,
        val fileName: String
    )
    
    suspend fun createLocalBackup(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date(timestamp))
            val fileName = "receipt_backup_$dateStr.zip"
            val backupFile = File(backupDir, fileName)
            
            val receipts = dao.getAllNow()
            
            ZipOutputStream(backupFile.outputStream()).use { zos ->
                // Save receipts JSON
                val receiptsJson = JSONArray()
                for (receipt in receipts) {
                    receiptsJson.put(JSONObject().apply {
                        put("id", receipt.id)
                        put("merchant", receipt.merchant)
                        put("dateIso", receipt.dateIso)
                        put("totalCents", receipt.totalCents)
                        put("category", receipt.category)
                        put("status", receipt.status)
                        put("notes", receipt.notes)
                        put("confidence", receipt.confidence)
                    })
                }
                
                zos.putNextEntry(ZipEntry("receipts.json"))
                zos.write(receiptsJson.toString(2).toByteArray())
                zos.closeEntry()
                
                // Save metadata
                val metadata = JSONObject().apply {
                    put("timestamp", timestamp)
                    put("receiptCount", receipts.size)
                    put("appVersion", "5.0.0")
                    put("androidVersion", android.os.Build.VERSION.SDK_INT)
                    put("createdAt", dateStr)
                }
                
                zos.putNextEntry(ZipEntry("metadata.json"))
                zos.write(metadata.toString(2).toByteArray())
                zos.closeEntry()
                
                // Copy receipt files
                val existingFiles = receipts.filter { it.localCopyPath.isNotBlank() && File(it.localCopyPath).exists() }
                for (receipt in existingFiles) {
                    val sourceFile = File(receipt.localCopyPath)
                    val entryName = "files/${receipt.id}/${sourceFile.name}"
                    
                    zos.putNextEntry(ZipEntry(entryName))
                    sourceFile.inputStream().use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            
            Result.success(BackupMetadata(
                timestamp = timestamp,
                receiptCount = receipts.size,
                appVersion = "5.0.0",
                androidVersion = android.os.Build.VERSION.SDK_INT,
                fileName = fileName
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun restoreFromBackup(backupFile: File): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            var metadata: BackupMetadata? = null
            
            ZipInputStream(backupFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "metadata.json" -> {
                            val metadataJson = zis.readBytes().toString(Charsets.UTF_8)
                            val json = JSONObject(metadataJson)
                            metadata = BackupMetadata(
                                timestamp = json.getLong("timestamp"),
                                receiptCount = json.getInt("receiptCount"),
                                appVersion = json.getString("appVersion"),
                                androidVersion = json.getInt("androidVersion"),
                                fileName = backupFile.name
                            )
                        }
                        entry.name.startsWith("files/") -> {
                            val parts = entry.name.split("/")
                            if (parts.size >= 3) {
                                val receiptId = parts[1].toLongOrNull()
                                if (receiptId != null) {
                                    val destDir = File(context.filesDir, "receipt_sources").apply { mkdirs() }
                                    val destFile = File(destDir, "${receiptId}_${parts[2]}")
                                    zis.copyTo(destFile.outputStream())
                                }
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            
            if (metadata != null) {
                Result.success(metadata!!)
            } else {
                Result.failure(Exception("Invalid backup file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun listBackups(): List<BackupMetadata> = withContext(Dispatchers.IO) {
        val files = backupDir.listFiles()?.filter { it.name.endsWith(".zip") } ?: emptyList()
        val backups = mutableListOf<BackupMetadata>()
        
        for (file in files) {
            try {
                ZipInputStream(file.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "metadata.json") {
                            val json = JSONObject(zis.readBytes().toString(Charsets.UTF_8))
                            backups.add(BackupMetadata(
                                timestamp = json.getLong("timestamp"),
                                receiptCount = json.getInt("receiptCount"),
                                appVersion = json.getString("appVersion"),
                                androidVersion = json.getInt("androidVersion"),
                                fileName = file.name
                            ))
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            } catch (e: Exception) {
                // Skip invalid backup files
            }
        }
        
        backups.sortedByDescending { it.timestamp }
    }
    
    suspend fun deleteBackup(fileName: String): Boolean = withContext(Dispatchers.IO) {
        File(backupDir, fileName).delete()
    }
    
    fun getBackupSize(fileName: String): Long {
        return File(backupDir, fileName).length()
    }
    
    suspend fun getAutoBackupSize(): Long = withContext(Dispatchers.IO) {
        backupDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    data class BackupStats(
        val totalBackups: Int,
        val totalSize: Long,
        val oldestBackup: Long?,
        val newestBackup: Long?
    )
    
    suspend fun getBackupStats(): BackupStats = withContext(Dispatchers.IO) {
        val backups = listBackups()
        BackupStats(
            totalBackups = backups.size,
            totalSize = backupDir.listFiles()?.sumOf { it.length() } ?: 0L,
            oldestBackup = backups.lastOrNull()?.timestamp,
            newestBackup = backups.firstOrNull()?.timestamp
        )
    }
}
