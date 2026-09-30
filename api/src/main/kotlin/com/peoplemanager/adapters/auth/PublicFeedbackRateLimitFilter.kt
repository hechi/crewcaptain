package com.peoplemanager.adapters.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, in-memory, per-IP+token rate limiter for public feedback submissions.
 *
 * This is the app's only anonymous write endpoint, so an unbounded public link would be a
 * spam / storage-growth vector. This adds friction only for abusive volume, not for real
 * submitters. It is intentionally simple (a sliding window of recent timestamps per key)
 * and holds IPs in memory only — nothing is persisted, honoring the privacy stance.
 *
 * Only POST requests are limited; GET (fetching the form) is unrestricted.
 */
@Component
class PublicFeedbackRateLimitFilter(
    @Value("\${app.feedback.rate-limit.max-submissions:10}") private val maxSubmissions: Int,
    @Value("\${app.feedback.rate-limit.window-seconds:600}") private val windowSeconds: Long
) : OncePerRequestFilter() {

    private val window: Duration = Duration.ofSeconds(windowSeconds)
    private val buckets = ConcurrentHashMap<String, MutableList<Instant>>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.method.equals("POST", ignoreCase = true) && isSubmission(request.requestURI)) {
            val key = keyFor(request)
            if (!allow(key)) {
                response.status = 429
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write(
                    """{"status":429,"error":"Too Many Requests","message":"Too many submissions from this device. Please try again later."}"""
                )
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun isSubmission(uri: String): Boolean = uri.startsWith("/api/v1/public/feedback/")

    private fun keyFor(request: HttpServletRequest): String {
        // Best-effort client IP; behind the Next.js proxy this may be the proxy IP, which
        // still bounds total volume. We deliberately do NOT store this anywhere.
        val forwarded = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        val ip = forwarded?.takeIf { it.isNotBlank() } ?: request.remoteAddr ?: "unknown"
        val token = request.requestURI.removePrefix("/api/v1/public/feedback/").substringBefore('/')
        return "$ip|$token"
    }

    @Synchronized
    private fun allow(key: String): Boolean {
        val now = Instant.now()
        val cutoff = now.minus(window)
        val timestamps = buckets.getOrPut(key) { mutableListOf() }
        timestamps.removeIf { it.isBefore(cutoff) }

        // Opportunistic cleanup so the map doesn't grow unbounded.
        if (buckets.size > 10_000) {
            buckets.entries.removeIf { entry -> entry.value.all { it.isBefore(cutoff) } }
        }

        if (timestamps.size >= maxSubmissions) return false
        timestamps.add(now)
        return true
    }
}
