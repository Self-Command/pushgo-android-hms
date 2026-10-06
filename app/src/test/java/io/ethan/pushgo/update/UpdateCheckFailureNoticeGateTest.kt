package io.ethan.pushgo.update

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckFailureNoticeGateTest {
    @Test
    fun automaticFailureOnlyNotifiesOnceAcrossRecreation() {
        val persisted = AtomicBoolean(false)
        val lock = Any()
        fun newGate() = UpdateCheckFailureNoticeGate(lock, persisted::get) {
            persisted.set(true)
            true
        }
        assertTrue(newGate().shouldNotify(manual = false))
        repeat(10) { assertFalse(newGate().shouldNotify(manual = false)) }
    }

    @Test
    fun manualChecksKeepFeedbackAndDoNotConsumeAutomaticNotice() {
        val persisted = AtomicBoolean(false)
        val gate = UpdateCheckFailureNoticeGate(Any(), persisted::get) {
            persisted.set(true)
            true
        }
        assertTrue(gate.shouldNotify(manual = true))
        assertFalse(persisted.get())
        assertTrue(gate.shouldNotify(manual = false))
        assertTrue(gate.shouldNotify(manual = true))
        assertFalse(gate.shouldNotify(manual = false))
    }

    @Test
    fun concurrentAutomaticChecksClaimOnlyOneNotice() {
        val persisted = AtomicBoolean(false)
        val writes = AtomicInteger(0)
        val lock = Any()
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = (1..50).map {
                pool.submit<Boolean> {
                    UpdateCheckFailureNoticeGate(lock, persisted::get) {
                        writes.incrementAndGet()
                        persisted.set(true)
                        true
                    }.shouldNotify(manual = false)
                }
            }
            assertEquals(1, results.count { it.get() })
            assertEquals(1, writes.get())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun failedPersistenceDoesNotEmitRepeatedUnrecordedNotices() {
        val gate = UpdateCheckFailureNoticeGate(Any(), { false }, { false })
        repeat(3) { assertFalse(gate.shouldNotify(manual = false)) }
        assertTrue(gate.shouldNotify(manual = true))
    }
}
