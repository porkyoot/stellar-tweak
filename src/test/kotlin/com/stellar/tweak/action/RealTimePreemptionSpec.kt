package com.stellar.tweak.action

import com.stellar.core.action.ActionContext
import com.stellar.core.action.ActionDispatcher
import com.stellar.core.action.ActionPriority
import com.stellar.core.action.ActionResult
import com.stellar.core.action.ModAction
import com.stellar.core.ratelimit.TokenBucket
import com.stellar.tweak.task.InventoryClickAction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

private class TestCriticalAction : ModAction {
    override val priority: ActionPriority = ActionPriority.CRITICAL
    var executed: Boolean = false
        private set

    override suspend fun execute(context: ActionContext): ActionResult {
        executed = true
        return ActionResult.Success
    }
}

class RealTimePreemptionSpec : FunSpec({

    test("Critical action immediately preempts queued InventoryClickActions and bypasses empty TokenBucket") {
        runTest {
            val mockNanos = 0L
            // Rate limiter with capacity 5 tokens and 0 refill per second for manual control
            val tokenBucket = TokenBucket(
                capacity = 5.0,
                refillRatePerSecond = 0.0,
                timeSource = { mockNanos },
            )
            // Max 2 rate-limited actions processed per tick
            val dispatcher = ActionDispatcher(
                tokenBucket = tokenBucket,
                maxActionsPerTick = 2,
            )

            // 1. Queue 50 InventoryClickActions into the ActionDispatcher
            val totalClickActions = 50
            for (i in 1..totalClickActions) {
                dispatcher.enqueue(InventoryClickAction(slotId = i))
            }
            dispatcher.pendingCount shouldBe 50

            // 2. Call dispatcher.tick() a few times to verify the TokenBucket is throttling them correctly
            // Tick 1: Processes 2 actions (maxActionsPerTick = 2), 3 tokens remain in bucket
            dispatcher.tick()
            dispatcher.pendingCount shouldBe 48

            // Tick 2: Processes 2 actions, 1 token remains in bucket
            dispatcher.tick()
            dispatcher.pendingCount shouldBe 46

            // Tick 3: Only 1 token remains, processes 1 action
            dispatcher.tick()
            dispatcher.pendingCount shouldBe 45

            // Tick 4: TokenBucket is now completely exhausted (0 tokens), 0 actions executed
            dispatcher.tick()
            dispatcher.pendingCount shouldBe 45

            // 3. Simulate a sudden game event by enqueuing the critical action (CRITICAL)
            val criticalAction = TestCriticalAction()
            criticalAction.executed shouldBe false
            dispatcher.enqueue(criticalAction)
            dispatcher.pendingCount shouldBe 46

            // 4. Call dispatcher.tick() again. Assert that critical action executes immediately,
            // completely bypassing the token bucket, and effectively overtaking the ongoing inventory sort.
            dispatcher.tick()

            // Critical action executed immediately!
            criticalAction.executed shouldBe true

            // The remaining 45 LOW-priority inventory clicks remain safely buffered in the queue
            dispatcher.pendingCount shouldBe 45
        }
    }
})
