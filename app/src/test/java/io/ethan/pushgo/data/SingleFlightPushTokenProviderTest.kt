package io.ethan.pushgo.data

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SingleFlightPushTokenProviderTest {
    @Test
    fun timeout_does_not_cancel_or_duplicate_request_and_late_token_is_kept() = runBlocking {
        val calls = AtomicInteger()
        val persisted = AtomicReference<String?>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val provider = SingleFlightPushTokenProvider(fetch = {
            calls.incrementAndGet()
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            " token "
        }, onToken = { persisted.set(it) })
        val first = async(start = CoroutineStart.UNDISPATCHED) { provider.fetchToken(100) }
        check(entered.await(5, TimeUnit.SECONDS))
        assertNull(first.await())
        val waiters = List(8) { async(start = CoroutineStart.UNDISPATCHED) { provider.fetchToken(5_000) } }
        release.countDown()
        assertEquals(List(8) { "token" }, waiters.awaitAll())
        assertEquals(1, calls.get())
        assertEquals("token", persisted.get())
    }

    @Test
    fun empty_result_waits_for_callback_and_failure_allows_a_new_attempt() = runBlocking {
        val calls = AtomicInteger()
        val provider = SingleFlightPushTokenProvider(fetch = {
            when (calls.incrementAndGet()) {
                1 -> throw IllegalStateException("temporary failure")
                2 -> "  "
                else -> "recovered"
            }
        }, onToken = {})
        assertTrue(runCatching { provider.fetchToken(5_000) }.isFailure)
        assertNull(provider.fetchToken(5_000))
        assertEquals("recovered", provider.fetchToken(5_000))
    }
}
