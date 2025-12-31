package com.madeinbraza.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Debounce utility to prevent rapid repeated API calls.
 * Useful for pull-to-refresh and search operations.
 */
class Debouncer {

    private val jobs = ConcurrentHashMap<String, Job>()
    private val lastExecutionTimes = ConcurrentHashMap<String, Long>()

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 2000L  // 2 seconds default
        const val REFRESH_DEBOUNCE_MS = 3000L  // 3 seconds for refresh
        const val SEARCH_DEBOUNCE_MS = 500L    // 500ms for search
    }

    /**
     * Debounce an action - only executes after delay if no new calls
     */
    fun debounce(
        scope: CoroutineScope,
        key: String = "default",
        delayMs: Long = DEFAULT_DEBOUNCE_MS,
        action: suspend () -> Unit
    ) {
        jobs[key]?.cancel()
        jobs[key] = scope.launch {
            delay(delayMs)
            action()
            jobs.remove(key)
        }
    }

    /**
     * Throttle an action - executes immediately but prevents re-execution within window
     * Returns true if action was executed, false if throttled
     */
    fun throttle(
        scope: CoroutineScope,
        key: String = "default",
        windowMs: Long = DEFAULT_DEBOUNCE_MS,
        action: suspend () -> Unit
    ): Boolean {
        val now = System.currentTimeMillis()
        val lastExecution = lastExecutionTimes[key] ?: 0L

        if (now - lastExecution < windowMs) {
            return false // Throttled
        }

        lastExecutionTimes[key] = now
        scope.launch { action() }
        return true
    }

    /**
     * Check if an action can be executed (not throttled)
     */
    fun canExecute(key: String, windowMs: Long = DEFAULT_DEBOUNCE_MS): Boolean {
        val now = System.currentTimeMillis()
        val lastExecution = lastExecutionTimes[key] ?: 0L
        return now - lastExecution >= windowMs
    }

    /**
     * Reset throttle for a specific key
     */
    fun reset(key: String) {
        jobs[key]?.cancel()
        jobs.remove(key)
        lastExecutionTimes.remove(key)
    }

    /**
     * Cancel all pending debounced actions
     */
    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
