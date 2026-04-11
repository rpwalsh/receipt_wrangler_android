/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ReceiptValidatorTest {

    @Before
    fun setup() {
        // No setup needed for object
    }

    @Test
    fun validateMerchant_validName_returnsValid() {
        val result = ReceiptValidator.validateMerchant("Starbucks")
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateMerchant_empty_returnsInvalid() {
        val result = ReceiptValidator.validateMerchant("")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("required") })
    }

    @Test
    fun validateMerchant_whitespaceOnly_returnsInvalid() {
        val result = ReceiptValidator.validateMerchant("   ")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("whitespace") })
    }

    @Test
    fun validateMerchant_tooLong_returnsInvalid() {
        val longName = "A".repeat(101)
        val result = ReceiptValidator.validateMerchant(longName)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("100") })
    }

    @Test
    fun validateAmount_validAmount_returnsValid() {
        val result = ReceiptValidator.validateAmount("19.99")
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateAmount_validLargeAmount_returnsValid() {
        val result = ReceiptValidator.validateAmount("99999.99")
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateAmount_negative_returnsInvalid() {
        val result = ReceiptValidator.validateAmount("-10.00")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("negative") })
    }

    @Test
    fun validateAmount_exceedsMax_returnsInvalid() {
        val result = ReceiptValidator.validateAmount("100000.00")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("exceeds") })
    }

    @Test
    fun validateAmount_invalidFormat_returnsInvalid() {
        val result = ReceiptValidator.validateAmount("abc")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid") })
    }

    @Test
    fun validateAmount_empty_returnsInvalid() {
        val result = ReceiptValidator.validateAmount("")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("required") })
    }

    @Test
    fun validateDate_validDate_returnsValid() {
        val result = ReceiptValidator.validateDate("2024-12-25")
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateDate_invalidFormat_returnsInvalid() {
        val result = ReceiptValidator.validateDate("12/25/2024")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("format") })
    }

    @Test
    fun validateDate_empty_returnsInvalid() {
        val result = ReceiptValidator.validateDate("")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("required") })
    }

    @Test
    fun validateCategory_validCategory_returnsValid() {
        val result = ReceiptValidator.validateCategory(ReceiptCategory.MEALS)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateStatus_validStatus_returnsValid() {
        val result = ReceiptValidator.validateStatus(ReceiptStatus.INBOX)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateReceipt_validDraft_returnsValid() {
        val draft = ReceiptDraft(
            merchant = "Starbucks",
            total = "19.99",
            dateIso = "2024-12-25",
            category = ReceiptCategory.MEALS,
            status = ReceiptStatus.INBOX
        )
        val result = ReceiptValidator.validateReceipt(draft)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateReceipt_multipleErrors_returnsAllErrors() {
        val draft = ReceiptDraft(
            merchant = "",
            total = "invalid",
            dateIso = "invalid-date",
            category = ReceiptCategory.MEALS,
            status = ReceiptStatus.INBOX
        )
        val result = ReceiptValidator.validateReceipt(draft)
        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 3) // merchant, total, date
    }
}
