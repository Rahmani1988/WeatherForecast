package com.common.util

/**
 * Represents a cached entry with a timestamp for expiration.
 *
 * @param T The type of the cached data.
 * @property data The cached data.
 */
data class CachedEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(timeoutMs: Long): Boolean =
        System.currentTimeMillis() - timestamp > timeoutMs
}