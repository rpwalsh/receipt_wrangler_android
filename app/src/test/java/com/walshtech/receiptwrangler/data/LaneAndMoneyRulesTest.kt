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

class LaneAndMoneyRulesTest {

    @Test
    fun gigProfit_laneExists_andDefaultsAreNonReimbursableNonBillable() {
        val lane = UserLane.GIG_PROFIT
        assertEquals("Gig economy profit wrangler", lane.label)
        assertFalse(lane.defaultReimbursable)
        assertFalse(lane.defaultBillable)
    }

    @Test
    fun reportName_defaultsMatchLaneSemantics() {
        val monthDate = "2026-04-09"
        assertEquals("Business 2026-04", defaultReportNameForLane(UserLane.BUSINESS, monthDate))
        assertEquals("Tax records 2026-04", defaultReportNameForLane(UserLane.TAX, monthDate))
        assertEquals("Personal 2026-04", defaultReportNameForLane(UserLane.PERSONAL, monthDate))
        assertEquals("Client billback 2026-04", defaultReportNameForLane(UserLane.FREELANCER, monthDate))
        assertEquals("Gig profit 2026-04", defaultReportNameForLane(UserLane.GIG_PROFIT, monthDate))
    }

    @Test
    fun moneyString_roundTrip_isStableForCommonValues() {
        val cents = moneyStringToCents("123.45")
        assertEquals(12_345L, cents)
        assertEquals("123.45", centsToString(cents))
    }

    @Test
    fun moneyString_handlesSymbolsAndCommas() {
        assertEquals(12_345L, moneyStringToCents("$123.45"))
        assertEquals(123_456L, moneyStringToCents("1,234.56"))
    }

    @Test
    fun laneLabels_areUnique_andUserFacing() {
        val labels = UserLane.entries.map { it.label }
        assertEquals(labels.size, labels.distinct().size)
        assertTrue(labels.all { it.isNotBlank() })
    }

    @Test
    fun gigMath_calculatesSalesCostsAndNet_fromTagsAndNotes() {
        val receipts = listOf(
            ReceiptEntity(totalCents = 25000, tag = "sale", notes = "card sale"),
            ReceiptEntity(totalCents = 5000, notes = "income from booth"),
            ReceiptEntity(totalCents = 7000, tag = "expense", notes = "fuel")
        )
        val result = calculateGigMath(receipts)
        assertEquals(30000, result.salesCents)
        assertEquals(7000, result.expenseCents)
        assertEquals(23000, result.netCents)
        assertEquals(2, result.saleCount)
        assertEquals(1, result.expenseCount)
    }
}
