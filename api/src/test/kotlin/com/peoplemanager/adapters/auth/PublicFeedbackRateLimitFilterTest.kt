package com.peoplemanager.adapters.auth

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicInteger

class PublicFeedbackRateLimitFilterTest {

    private fun request(method: String, uri: String, ip: String = "1.2.3.4"): HttpServletRequest {
        val req = mockk<HttpServletRequest>(relaxed = true)
        every { req.method } returns method
        every { req.requestURI } returns uri
        every { req.remoteAddr } returns ip
        every { req.getHeader("X-Forwarded-For") } returns null
        // OncePerRequestFilter inspects the dispatcher type; keep it a normal request.
        every { req.dispatcherType } returns DispatcherType.REQUEST
        every { req.getAttribute(any()) } returns null
        return req
    }

    /** A response mock that records the last status set into [statusHolder]. */
    private fun response(statusHolder: AtomicInteger): HttpServletResponse {
        val resp = mockk<HttpServletResponse>(relaxed = true)
        every { resp.writer } returns PrintWriter(StringWriter())
        every { resp.status = any() } answers { statusHolder.set(firstArg()) }
        return resp
    }

    private fun chain(counter: AtomicInteger): FilterChain {
        val c = mockk<FilterChain>(relaxed = true)
        every { c.doFilter(any(), any()) } answers { counter.incrementAndGet(); Unit }
        return c
    }

    @Test
    fun `allows submissions under the limit`() {
        val filter = PublicFeedbackRateLimitFilter(maxSubmissions = 3, windowSeconds = 600)
        val calls = AtomicInteger(0)
        val c = chain(calls)

        repeat(3) {
            filter.doFilter(request("POST", "/api/v1/public/feedback/tok"), response(AtomicInteger()), c)
        }
        calls.get() shouldBe 3
    }

    @Test
    fun `blocks submissions over the limit with 429`() {
        val filter = PublicFeedbackRateLimitFilter(maxSubmissions = 2, windowSeconds = 600)
        val calls = AtomicInteger(0)
        val c = chain(calls)

        repeat(2) {
            filter.doFilter(request("POST", "/api/v1/public/feedback/tok"), response(AtomicInteger()), c)
        }
        val status = AtomicInteger(0)
        filter.doFilter(request("POST", "/api/v1/public/feedback/tok"), response(status), c)

        status.get() shouldBe 429
        calls.get() shouldBe 2 // the blocked request never reached the chain
    }

    @Test
    fun `separate tokens have separate buckets`() {
        val filter = PublicFeedbackRateLimitFilter(maxSubmissions = 1, windowSeconds = 600)
        val calls = AtomicInteger(0)
        val c = chain(calls)

        filter.doFilter(request("POST", "/api/v1/public/feedback/tokA"), response(AtomicInteger()), c)
        filter.doFilter(request("POST", "/api/v1/public/feedback/tokB"), response(AtomicInteger()), c)

        calls.get() shouldBe 2
    }

    @Test
    fun `GET requests are never limited`() {
        val filter = PublicFeedbackRateLimitFilter(maxSubmissions = 1, windowSeconds = 600)
        val calls = AtomicInteger(0)
        val c = chain(calls)

        repeat(5) {
            filter.doFilter(request("GET", "/api/v1/public/feedback/tok"), response(AtomicInteger()), c)
        }
        calls.get() shouldBe 5
    }
}
