/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptRoundTripWorkflowTest {

    @Test
    fun draftToEntityToDraft_roundTrip_preservesGigWorkflowFields() {
        val draft = ReceiptDraft(
            merchant = "Saturday Market Booth",
            dateIso = "2026-04-09",
            total = "425.75",
            tax = "0.00",
            subtotal = "425.75",
            lane = UserLane.GIG_PROFIT,
            notes = "sale: card batch",
            tag = "sale",
            reportName = "Gig profit 2026-04",
            paymentMethod = "Square",
            reimbursable = false,
            billable = false,
            confidence = 82
        )

        val entity = draft.toEntity()
        val restored = entity.toDraft()

        assertEquals(UserLane.GIG_PROFIT, restored.lane)
        assertEquals("Saturday Market Booth", restored.merchant)
        assertEquals("425.75", restored.total)
        assertEquals("sale", restored.tag)
        assertEquals("Gig profit 2026-04", restored.reportName)
        assertFalse(restored.reimbursable)
        assertFalse(restored.billable)
        assertTrue(restored.notes.contains("sale"))
    }

    @Test
    fun fallbackEnumParsing_defaultsSafelyForUnknownLane() {
        val parsed = enumValueOrDefault("UNKNOWN_LANE", UserLane.BUSINESS)
        assertEquals(UserLane.BUSINESS, parsed)
    }
}
