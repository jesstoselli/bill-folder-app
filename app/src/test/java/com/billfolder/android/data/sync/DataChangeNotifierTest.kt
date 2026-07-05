package com.billfolder.android.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataChangeNotifierTest {

    @Test
    fun `notifyDataChanged advances the version`() {
        val notifier = DataChangeNotifier()

        val before = notifier.changes.value
        notifier.notifyDataChanged()
        val after = notifier.changes.value

        assertNotEquals("version should change after a mutation", before, after)
    }

    @Test
    fun `notifyingOnSuccess bumps the version after a successful block`() = runTest {
        val notifier = DataChangeNotifier()
        val before = notifier.changes.value

        val result = notifier.notifyingOnSuccess { "created" }

        assertEquals("created", result)
        assertNotEquals(before, notifier.changes.value)
    }

    @Test
    fun `notifyingOnSuccess does not bump when the block throws`() = runTest {
        val notifier = DataChangeNotifier()
        val before = notifier.changes.value

        try {
            notifier.notifyingOnSuccess { throw IllegalStateException("boom") }
        } catch (_: IllegalStateException) {
            // esperado
        }

        assertEquals(before, notifier.changes.value)
    }

    @Test
    fun `collector that drops the initial value is notified on every change`() = runTest {
        val notifier = DataChangeNotifier()
        val received = mutableListOf<Long>()

        // UnconfinedTestDispatcher runs the collector eagerly on each emission,
        // so we don't need to advance the scheduler manually. drop(1) skips the
        // StateFlow's replayed current value (the initial subscription emit) —
        // exactly what observeDataChanges relies on so the initial load isn't
        // double-fetched.
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            notifier.changes.drop(1).collect { received.add(it) }
        }

        notifier.notifyDataChanged()
        notifier.notifyDataChanged()
        notifier.notifyDataChanged()

        job.cancel()

        assertEquals(3, received.size)
    }
}
