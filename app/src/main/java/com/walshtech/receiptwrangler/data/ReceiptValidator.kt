/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Input validation for receipts with detailed error messages
 */
object ReceiptValidator {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    )
    
    fun validateMerchant(merchant: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (merchant.isBlank()) {
            errors.add("Merchant name is required")
        }
        if (merchant.length > 100) {
            errors.add("Merchant name must be 100 characters or less")
        }
        if (merchant.matches(Regex("^\\s+$"))) {
            errors.add("Merchant name cannot be only whitespace")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    fun validateAmount(amountString: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (amountString.isBlank()) {
            errors.add("Amount is required")
            return ValidationResult(false, errors)
        }
        
        try {
            val cents = (amountString.toDouble() * 100).toLong()
            if (cents < 0) {
                errors.add("Amount cannot be negative")
            }
            if (cents > 9999999) { // $99,999.99
                errors.add("Amount exceeds maximum (99999.99)")
            }
        } catch (e: Exception) {
            errors.add("Invalid amount format")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    fun validateDate(dateIso: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (dateIso.isBlank()) {
            errors.add("Date is required")
        }
        
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso)
        } catch (e: Exception) {
            errors.add("Invalid date format. Use YYYY-MM-DD")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    fun validateCategory(@Suppress("UNUSED_PARAMETER") category: ReceiptCategory): ValidationResult {
        return ValidationResult(true, emptyList())
    }
    
    fun validateStatus(@Suppress("UNUSED_PARAMETER") status: ReceiptStatus): ValidationResult {
        return ValidationResult(true, emptyList())
    }
    
    fun validateReceipt(draft: ReceiptDraft): ValidationResult {
        val allErrors = mutableListOf<String>()
        
        // Validate merchant
        val merchantResult = validateMerchant(draft.merchant)
        allErrors.addAll(merchantResult.errors)
        
        // Validate amount
        val amountResult = validateAmount(draft.total)
        allErrors.addAll(amountResult.errors)
        
        // Validate date
        val dateResult = validateDate(draft.dateIso)
        allErrors.addAll(dateResult.errors)
        
        // Category and status are enums, so they're always valid
        
        return ValidationResult(allErrors.isEmpty(), allErrors)
    }
}
