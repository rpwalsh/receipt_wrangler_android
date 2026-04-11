/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

/**
 * Comprehensive error handling with user-friendly messages
 */
sealed class ReceiptError(
    val userMessage: String,
    val technicalMessage: String,
    val code: String,
    cause: Throwable? = null
) : Exception(technicalMessage, cause) {

    // Validation Errors
    data class ValidationError(
        val field: String,
        val reason: String,
        val userMsg: String = "Invalid $field: $reason"
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Validation failed for field '$field': $reason",
        code = "VALIDATION_ERROR"
    )

    data class MissingFieldError(
        val field: String,
        val userMsg: String = "$field is required"
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Required field missing: $field",
        code = "MISSING_FIELD"
    )

    // Data Errors
    data class DuplicateError(
        val duplicateCount: Int = 1,
        val userMsg: String = "Found $duplicateCount potential duplicate(s). Review before saving."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Duplicate detection triggered with score >= 70",
        code = "DUPLICATE_DETECTED"
    )

    data class DataNotFoundError(
        val itemId: Long? = null,
        val userMsg: String = "Receipt not found. It may have been deleted."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Data not found for id: $itemId",
        code = "NOT_FOUND"
    )

    // File Errors
    data class FileError(
        val filePath: String,
        val operation: String = "process",
        val userMsg: String = "Unable to $operation file. Try again or choose a different file."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "File error for '$filePath' during $operation",
        code = "FILE_ERROR"
    )

    data class FileSizeError(
        val maxSizeMb: Int = 50,
        val userMsg: String = "File is too large. Maximum size is ${maxSizeMb}MB."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "File exceeds size limit of $maxSizeMb MB",
        code = "FILE_TOO_LARGE"
    )

    data class FileFormatError(
        val format: String,
        val supportedFormats: String = "JPG, PNG, PDF",
        val userMsg: String = "File format not supported. Supported: $supportedFormats"
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Unsupported file format: $format",
        code = "FILE_FORMAT_ERROR"
    )

    // OCR Errors
    data class OcrError(
        val confidence: Int = 0,
        val userMsg: String = if (confidence > 0) 
            "OCR confidence is low ($confidence%). Please review and correct the details."
            else 
            "Unable to read receipt text. Please check image quality and try again."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "OCR processing failed with confidence: $confidence%",
        code = "OCR_FAILED"
    )

    // Network Errors
    data class NetworkError(
        val operation: String = "save",
        val userMsg: String = "Network error. Changes may not be saved. Check your connection and try again."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Network failure during $operation",
        code = "NETWORK_ERROR"
    )

    // Database Errors
    data class DatabaseError(
        val operation: String = "access",
        val userMsg: String = "Database error. Please restart the app and try again."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Database error during $operation",
        code = "DATABASE_ERROR"
    )

    // Export Errors
    data class ExportError(
        val format: String,
        val userMsg: String = "Unable to export as $format. Try another format or check storage space."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Export failed for format: $format",
        code = "EXPORT_ERROR"
    )

    // Batch Operation Errors
    data class BatchOperationError(
        val successCount: Int = 0,
        val totalCount: Int = 0,
        val failureReason: String = "Unknown error",
        val userMsg: String = "Batch operation partially failed ($successCount/$totalCount succeeded)"
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Batch operation failed: $failureReason ($successCount/$totalCount)",
        code = "BATCH_OPERATION_ERROR"
    )

    // Generic Error
    data class GenericError(
        val operation: String = "operation",
        val userMsg: String = "An unexpected error occurred. Please try again."
    ) : ReceiptError(
        userMessage = userMsg,
        technicalMessage = "Unexpected error during $operation",
        code = "GENERIC_ERROR"
    )

    companion object {
        fun fromException(e: Exception): ReceiptError = when (e) {
            is ReceiptError -> e
            else -> GenericError(userMsg = "An unexpected error occurred. Please try again.")
        }
    }
}

/**
 * Error recovery strategies
 */
object ErrorRecovery {
    fun suggestAction(error: ReceiptError): String = when (error) {
        is ReceiptError.ValidationError -> "Please correct the invalid ${error.field} and try again."
        is ReceiptError.MissingFieldError -> "Fill in the missing ${error.field} field."
        is ReceiptError.DuplicateError -> "Review the potential duplicate(s) and delete if needed."
        is ReceiptError.DataNotFoundError -> "The receipt may have been deleted. Go back and refresh."
        is ReceiptError.FileError -> "Try selecting a different file or check file permissions."
        is ReceiptError.FileSizeError -> "Choose a smaller file or compress the image."
        is ReceiptError.FileFormatError -> "Convert the file to JPG or PNG and try again."
        is ReceiptError.OcrError -> "Try a clearer photo or manually enter the details."
        is ReceiptError.NetworkError -> "Check your internet connection and try again."
        is ReceiptError.DatabaseError -> "Restart the app. If problem persists, reinstall."
        is ReceiptError.ExportError -> "Check that you have storage space and permissions."
        is ReceiptError.BatchOperationError -> "Some items failed. Review and retry failed items individually."
        is ReceiptError.GenericError -> "Restart the app and try again. Contact support if it persists."
    }

    fun canRetry(error: ReceiptError): Boolean = when (error) {
        is ReceiptError.ValidationError -> true
        is ReceiptError.MissingFieldError -> true
        is ReceiptError.DuplicateError -> true
        is ReceiptError.FileError -> true
        is ReceiptError.OcrError -> true
        is ReceiptError.NetworkError -> true
        is ReceiptError.BatchOperationError -> true
        else -> false
    }
}
