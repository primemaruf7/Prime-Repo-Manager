package com.example.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RateLimitInfo(
    val limit: Int = 5000,
    val remaining: Int = 5000,
    val resetEpochSeconds: Long = 0L,
    val used: Int = 0
) {
    val percentageRemaining: Float
        get() = if (limit > 0) (remaining.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 1f
        
    val isNearExhaustion: Boolean
        get() = remaining < 50
}

object RateLimitManager {
    private val _rateLimit = MutableStateFlow(RateLimitInfo())
    val rateLimit: StateFlow<RateLimitInfo> = _rateLimit.asStateFlow()

    fun updateFromHeaders(limitStr: String?, remainingStr: String?, resetStr: String?, usedStr: String?) {
        val limit = limitStr?.toIntOrNull() ?: _rateLimit.value.limit
        val remaining = remainingStr?.toIntOrNull() ?: _rateLimit.value.remaining
        val reset = resetStr?.toLongOrNull() ?: _rateLimit.value.resetEpochSeconds
        val used = usedStr?.toIntOrNull() ?: _rateLimit.value.used

        _rateLimit.value = RateLimitInfo(
            limit = limit,
            remaining = remaining,
            resetEpochSeconds = reset,
            used = used
        )
    }
}
