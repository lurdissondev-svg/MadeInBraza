package com.madeinbraza.app.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache with TTL support to reduce server load.
 * Prevents redundant API calls for frequently accessed data.
 */
@Singleton
class CacheManager @Inject constructor() {

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()
    private val mutex = Mutex()

    companion object {
        // Default TTL values
        const val TTL_SHORT = 30_000L      // 30 seconds - for frequently changing data
        const val TTL_MEDIUM = 60_000L     // 1 minute - for moderately changing data
        const val TTL_LONG = 300_000L      // 5 minutes - for rarely changing data
        const val TTL_USER_STATUS = 120_000L // 2 minutes - for user status
    }

    /**
     * Get cached data if not expired, otherwise return null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] as? CacheEntry<T> ?: return null
        return if (entry.isExpired()) {
            cache.remove(key)
            null
        } else {
            entry.data
        }
    }

    /**
     * Store data in cache with specified TTL
     */
    fun <T> put(key: String, data: T, ttlMs: Long = TTL_MEDIUM) {
        cache[key] = CacheEntry(data, System.currentTimeMillis(), ttlMs)
    }

    /**
     * Get cached data or fetch it using the provided loader
     */
    suspend fun <T> getOrLoad(
        key: String,
        ttlMs: Long = TTL_MEDIUM,
        loader: suspend () -> T?
    ): T? {
        // Check cache first
        get<T>(key)?.let { return it }

        // Load and cache
        return mutex.withLock {
            // Double-check after acquiring lock
            get<T>(key)?.let { return it }

            loader()?.also { data ->
                put(key, data, ttlMs)
            }
        }
    }

    /**
     * Invalidate a specific cache entry
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Invalidate all entries matching a prefix
     */
    fun invalidateByPrefix(prefix: String) {
        cache.keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    /**
     * Clear all cached data
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Clean up expired entries
     */
    fun cleanup() {
        cache.entries.removeIf { (it.value as CacheEntry<*>).isExpired() }
    }

    // Cache key builders
    object Keys {
        const val USER_STATUS = "user_status"
        fun channels() = "channels"
        fun channelMessages(channelId: String) = "channel_messages_$channelId"
        fun events() = "events"
        fun parties() = "parties"
        fun members() = "members"
        fun unreadCount(channelId: String) = "unread_$channelId"
    }
}
