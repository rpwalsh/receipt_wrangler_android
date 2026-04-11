/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * In-memory cache for receipt data with TTL and query result caching
 */
class ReceiptCache {
    private val lock = ReentrantReadWriteLock()
    private val queryCache = ConcurrentHashMap<String, CacheEntry<List<ReceiptEntity>>>()
    private val statisticsCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    data class CacheEntry<T>(
        val value: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMillis: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMillis
        }
    }

    // TTL in milliseconds - 5 minutes for query results, 1 minute for statistics
    private val DEFAULT_QUERY_TTL = 5 * 60 * 1000L
    private val DEFAULT_STAT_TTL = 60 * 1000L

    fun cacheQueryResult(key: String, result: List<ReceiptEntity>) {
        lock.write {
            queryCache[key] = CacheEntry(result)
        }
    }

    fun getQueryResult(key: String): List<ReceiptEntity>? {
        lock.read {
            val entry = queryCache[key] ?: return null
            if (entry.isExpired(DEFAULT_QUERY_TTL)) {
                lock.write { queryCache.remove(key) }
                return null
            }
            return entry.value
        }
    }

    fun cacheStatistic(key: String, value: Any) {
        lock.write {
            statisticsCache[key] = CacheEntry(value)
        }
    }

    fun <T> getStatistic(key: String, type: Class<T>): T? {
        lock.read {
            val entry = statisticsCache[key] ?: return null
            if (entry.isExpired(DEFAULT_STAT_TTL)) {
                lock.write { statisticsCache.remove(key) }
                return null
            }
            return type.cast(entry.value)
        }
    }

    fun invalidateAll() {
        lock.write {
            queryCache.clear()
            statisticsCache.clear()
        }
    }

    fun invalidateQueries() {
        lock.write {
            queryCache.clear()
        }
    }

    fun invalidateStatistics() {
        lock.write {
            statisticsCache.clear()
        }
    }

    fun getCacheStats(): CacheStats {
        lock.read {
            return CacheStats(
                queryCacheSize = queryCache.size,
                statisticsCacheSize = statisticsCache.size,
                totalEntries = queryCache.size + statisticsCache.size
            )
        }
    }

    data class CacheStats(
        val queryCacheSize: Int,
        val statisticsCacheSize: Int,
        val totalEntries: Int
    )
}
