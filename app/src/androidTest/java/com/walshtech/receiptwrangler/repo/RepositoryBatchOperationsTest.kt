/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.walshtech.receiptwrangler.data.AppDatabase
import com.walshtech.receiptwrangler.data.ReceiptCategory
import com.walshtech.receiptwrangler.data.ReceiptDao
import com.walshtech.receiptwrangler.data.ReceiptEntity
import com.walshtech.receiptwrangler.data.ReceiptStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class RepositoryBatchOperationsTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ReceiptDao
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.receiptDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun batchUpdateCategory_updatesMultipleReceipts() = runBlocking {
        // Create test receipts
        val receipt1 = ReceiptEntity(
            id = 1,
            merchant = "Starbucks",
            dateIso = "2024-01-01",
            totalCents = 1999,
            category = ReceiptCategory.OTHER.name,
            status = ReceiptStatus.INBOX.name
        )
        val receipt2 = ReceiptEntity(
            id = 2,
            merchant = "Chipotle",
            dateIso = "2024-01-02",
            totalCents = 2499,
            category = ReceiptCategory.OTHER.name,
            status = ReceiptStatus.INBOX.name
        )

        dao.upsertAll(listOf(receipt1, receipt2))

        // Update both to MEALS
        dao.setCategory(listOf(1L, 2L), ReceiptCategory.MEALS.name)

        // Verify
        val updated1 = dao.getById(1)
        val updated2 = dao.getById(2)

        assertEquals(ReceiptCategory.MEALS.name, updated1?.category)
        assertEquals(ReceiptCategory.MEALS.name, updated2?.category)
    }

    @Test
    fun batchUpdateStatus_updatesMultipleReceipts() = runBlocking {
        val receipt1 = ReceiptEntity(
            id = 1,
            merchant = "Store 1",
            dateIso = "2024-01-01",
            totalCents = 1000,
            status = ReceiptStatus.INBOX.name
        )
        val receipt2 = ReceiptEntity(
            id = 2,
            merchant = "Store 2",
            dateIso = "2024-01-02",
            totalCents = 2000,
            status = ReceiptStatus.INBOX.name
        )

        dao.upsertAll(listOf(receipt1, receipt2))

        // Update both to READY
        dao.setStatus(listOf(1L, 2L), ReceiptStatus.READY.name)

        // Verify
        val updated1 = dao.getById(1)
        val updated2 = dao.getById(2)

        assertEquals(ReceiptStatus.READY.name, updated1?.status)
        assertEquals(ReceiptStatus.READY.name, updated2?.status)
    }

    @Test
    fun batchDelete_deletesMultipleReceipts() = runBlocking {
        val receipt1 = ReceiptEntity(id = 1, merchant = "Store 1", dateIso = "2024-01-01", totalCents = 1000)
        val receipt2 = ReceiptEntity(id = 2, merchant = "Store 2", dateIso = "2024-01-02", totalCents = 2000)
        val receipt3 = ReceiptEntity(id = 3, merchant = "Store 3", dateIso = "2024-01-03", totalCents = 3000)

        dao.upsertAll(listOf(receipt1, receipt2, receipt3))

        // Delete first two
        dao.deleteByIds(listOf(1L, 2L))

        // Verify
        assertNull(dao.getById(1))
        assertNull(dao.getById(2))
        assertNotNull(dao.getById(3))
    }

    @Test
    fun batchDelete_emptyList_doesNothing() = runBlocking {
        val receipt = ReceiptEntity(id = 1, merchant = "Store 1", dateIso = "2024-01-01", totalCents = 1000)
        dao.upsert(receipt)

        // Delete empty list
        dao.deleteByIds(emptyList())

        // Verify receipt still exists
        assertNotNull(dao.getById(1))
    }

    @Test
    fun batchUpdateCategory_partialIds_onlyUpdatesMatching() = runBlocking {
        val receipt1 = ReceiptEntity(
            id = 1,
            merchant = "Store 1",
            dateIso = "2024-01-01",
            totalCents = 1000,
            category = ReceiptCategory.OTHER.name
        )
        val receipt2 = ReceiptEntity(
            id = 2,
            merchant = "Store 2",
            dateIso = "2024-01-02",
            totalCents = 2000,
            category = ReceiptCategory.OTHER.name
        )

        dao.upsertAll(listOf(receipt1, receipt2))

        // Update only receipt 1
        dao.setCategory(listOf(1L), ReceiptCategory.MEALS.name)

        // Verify
        val updated1 = dao.getById(1)
        val updated2 = dao.getById(2)

        assertEquals(ReceiptCategory.MEALS.name, updated1?.category)
        assertEquals(ReceiptCategory.OTHER.name, updated2?.category)
    }

    @Test
    fun batchUpdateCategory_nonexistentIds_doesNothing() = runBlocking {
        val receipt = ReceiptEntity(
            id = 1,
            merchant = "Store 1",
            dateIso = "2024-01-01",
            totalCents = 1000,
            category = ReceiptCategory.OTHER.name
        )
        dao.upsert(receipt)

        // Update non-existent IDs
        dao.setCategory(listOf(99L, 100L), ReceiptCategory.MEALS.name)

        // Verify existing receipt unchanged
        val receipt1 = dao.getById(1)
        assertEquals(ReceiptCategory.OTHER.name, receipt1?.category)
    }

    @Test
    fun batchUpdate_maintainsUpdatedAtTimestamp() = runBlocking {
        val receipt = ReceiptEntity(
            id = 1,
            merchant = "Store 1",
            dateIso = "2024-01-01",
            totalCents = 1000,
            updatedAt = 1000L
        )
        dao.upsert(receipt)

        val originalUpdatedAt = dao.getById(1)?.updatedAt

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10)

        // Update
        dao.setCategory(listOf(1L), ReceiptCategory.MEALS.name)

        val newUpdatedAt = dao.getById(1)?.updatedAt

        // Verify timestamp was updated
        assertTrue(newUpdatedAt!! > originalUpdatedAt!!)
    }
}
