/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY updatedAt DESC")
    suspend fun getAllNow(): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(receipt: ReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(receipts: List<ReceiptEntity>)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM receipts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE receipts SET category = :category, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun setCategory(ids: List<Long>, category: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE receipts SET status = :status, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun setStatus(ids: List<Long>, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM receipts")
    suspend fun clearAll()
}
