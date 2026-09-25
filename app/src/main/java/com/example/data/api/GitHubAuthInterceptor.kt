package com.example.data.api

import okhttp3.Interceptor
import okhttp3.Response

class GitHubAuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")

        val token = tokenProvider()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer ${token.trim()}")
        }

        val request = builder.build()
        val response = chain.proceed(request)

        // Capture rate limit headers
        val limit = response.header("x-ratelimit-limit")
        val remaining = response.header("x-ratelimit-remaining")
        val reset = response.header("x-ratelimit-reset")
        val used = response.header("x-ratelimit-used")
        RateLimitManager.updateFromHeaders(limit, remaining, reset, used)

        return response
    }
}
