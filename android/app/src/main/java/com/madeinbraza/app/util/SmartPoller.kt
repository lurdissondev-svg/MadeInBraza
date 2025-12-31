package com.madeinbraza.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Smart polling with exponential backoff and change detection.
 * Reduces server load by increasing intervals when no changes are detected.
 */
class SmartPoller(
    private val scope: CoroutineScope,
    private val minIntervalMs: Long = 5_000L,      // 5 seconds minimum
    private val maxIntervalMs: Long = 30_000L,     // 30 seconds maximum
    private val backoffMultiplier: Float = 1.5f,   // Increase by 50% on no change
    private val resetOnChange: Boolean = true       // Reset to min interval when change detected
) {
    private var pollingJob: Job? = null
    private var currentInterval = minIntervalMs
    private var lastDataHash: Int? = null
    private var consecutiveNoChanges = 0

    companion object {
        const val MAX_CONSECUTIVE_NO_CHANGES = 5
    }

    /**
     * Start smart polling with the given fetcher and callback.
     * @param fetcher Function to fetch data from server
     * @param onData Callback when new data is received
     * @param onError Optional callback for errors
     */
    fun <T> start(
        fetcher: suspend () -> T?,
        onData: (T) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        stop()
        currentInterval = minIntervalMs
        consecutiveNoChanges = 0

        pollingJob = scope.launch {
            while (isActive) {
                delay(currentInterval)

                try {
                    val data = fetcher()
                    if (data != null) {
                        val newHash = data.hashCode()

                        if (lastDataHash != newHash) {
                            // Data changed - reset interval and notify
                            lastDataHash = newHash
                            consecutiveNoChanges = 0
                            if (resetOnChange) {
                                currentInterval = minIntervalMs
                            }
                            onData(data)
                        } else {
                            // No change - increase interval with backoff
                            consecutiveNoChanges++
                            if (consecutiveNoChanges <= MAX_CONSECUTIVE_NO_CHANGES) {
                                currentInterval = (currentInterval * backoffMultiplier)
                                    .toLong()
                                    .coerceAtMost(maxIntervalMs)
                            }
                        }
                    }
                } catch (e: Exception) {
                    onError?.invoke(e)
                    // On error, use max interval to reduce load
                    currentInterval = maxIntervalMs
                }
            }
        }
    }

    /**
     * Force immediate poll and reset interval
     */
    fun forceRefresh() {
        currentInterval = minIntervalMs
        consecutiveNoChanges = 0
        lastDataHash = null
    }

    /**
     * Stop polling
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Check if currently polling
     */
    fun isPolling(): Boolean = pollingJob?.isActive == true

    /**
     * Get current polling interval (for debugging/monitoring)
     */
    fun getCurrentInterval(): Long = currentInterval
}

/**
 * Factory for creating SmartPollers with common configurations
 */
object SmartPollerFactory {

    /**
     * Create a poller optimized for chat messages
     * Faster initial polling, slower backoff for quiet channels
     */
    fun forChat(scope: CoroutineScope) = SmartPoller(
        scope = scope,
        minIntervalMs = 5_000L,     // 5 seconds when active
        maxIntervalMs = 20_000L,    // 20 seconds when quiet
        backoffMultiplier = 1.3f
    )

    /**
     * Create a poller optimized for list updates (events, parties, members)
     * Slower polling since these change less frequently
     */
    fun forLists(scope: CoroutineScope) = SmartPoller(
        scope = scope,
        minIntervalMs = 10_000L,    // 10 seconds minimum
        maxIntervalMs = 60_000L,    // 1 minute maximum
        backoffMultiplier = 1.5f
    )

    /**
     * Create a poller for background sync
     * Very conservative to minimize battery and server impact
     */
    fun forBackground(scope: CoroutineScope) = SmartPoller(
        scope = scope,
        minIntervalMs = 30_000L,    // 30 seconds minimum
        maxIntervalMs = 120_000L,   // 2 minutes maximum
        backoffMultiplier = 2.0f
    )
}
