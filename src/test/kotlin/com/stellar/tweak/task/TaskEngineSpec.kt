package com.stellar.tweak.task

import com.stellar.core.action.ActionContext
import com.stellar.core.action.ActionDispatcher
import com.stellar.core.action.ActionPriority
import com.stellar.core.action.ActionResult
import com.stellar.core.action.ModAction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first

class TaskEngineSpec : FunSpec({

    test("normal lifecycle transitions: Idle -> Snapshotting -> Calculating -> Executing -> Idle") {
        val dispatcher = ActionDispatcher()
        val engine = TaskEngine(dispatcher = dispatcher)

        try {
            engine.state.value shouldBe TaskState.Idle

            engine.processIntent(TaskIntent.Start)
            engine.state.value shouldBe TaskState.Snapshotting

            engine.processIntent(TaskIntent.SnapshotComplete)
            engine.state.value shouldBe TaskState.Calculating

            val dummyAction = object : ModAction {
                override val priority = ActionPriority.LOW
                override suspend fun execute(context: ActionContext) = ActionResult.Success
            }

            engine.processIntent(TaskIntent.PlanCalculated(listOf(dummyAction, dummyAction)))
            val executing = engine.state.value
            executing.shouldBeInstanceOf<TaskState.Executing>()
            executing.pendingActions shouldBe 2
            dispatcher.pendingCount shouldBe 2

            engine.processIntent(TaskIntent.ActionCompleted)
            (engine.state.value as TaskState.Executing).pendingActions shouldBe 1

            engine.processIntent(TaskIntent.ActionCompleted)
            engine.state.value shouldBe TaskState.Idle
        } finally {
            engine.close()
        }
    }

    test("failure integration: low priority action failure flushes queue and transitions to Snapshotting") {
        val dispatcher = ActionDispatcher()
        var snapshotTriggered = false
        val engine = TaskEngine(
            dispatcher = dispatcher,
            onSnapshot = { snapshotTriggered = true },
        )

        try {
            val failingAction = object : ModAction {
                override val priority = ActionPriority.LOW
                override suspend fun execute(context: ActionContext): ActionResult {
                    return ActionResult.Failure("Inventory slot mismatch: expected diamond_sword")
                }
            }

            val subsequentAction = object : ModAction {
                override val priority = ActionPriority.LOW
                override suspend fun execute(context: ActionContext) = ActionResult.Success
            }

            // Transition to Executing state
            engine.processIntent(TaskIntent.Start)
            engine.processIntent(TaskIntent.SnapshotComplete)
            engine.processIntent(TaskIntent.PlanCalculated(listOf(failingAction, subsequentAction)))

            engine.state.value.shouldBeInstanceOf<TaskState.Executing>()
            dispatcher.pendingCount shouldBe 2

            // Execute tick on dispatcher -> failingAction executes, returns Failure, emits to failureFlow
            dispatcher.tick()

            // Wait for collector to process failure intent and transition to Snapshotting
            val finalState = engine.state.first { it is TaskState.Snapshotting }

            // The engine must have flushed all LOW priority actions from the queue
            dispatcher.pendingCount shouldBe 0

            // Automatically transitioned back to Snapshotting
            finalState shouldBe TaskState.Snapshotting
            snapshotTriggered shouldBe true
        } finally {
            engine.close()
        }
    }

    test("non-LOW priority failure does not trigger replanning in sorting engine") {
        val dispatcher = ActionDispatcher()
        val engine = TaskEngine(dispatcher = dispatcher)

        try {
            val highPriorityAction = object : ModAction {
                override val priority = ActionPriority.HIGH
                override suspend fun execute(context: ActionContext): ActionResult {
                    return ActionResult.Failure("Combat interrupt")
                }
            }

            val lowAction = object : ModAction {
                override val priority = ActionPriority.LOW
                override suspend fun execute(context: ActionContext) = ActionResult.Success
            }

            engine.processIntent(TaskIntent.Start)
            engine.processIntent(TaskIntent.SnapshotComplete)
            engine.processIntent(TaskIntent.PlanCalculated(listOf(lowAction)))

            dispatcher.enqueue(highPriorityAction)
            dispatcher.tick()

            // State remains Executing because failure was not LOW priority
            engine.state.value.shouldBeInstanceOf<TaskState.Executing>()
        } finally {
            engine.close()
        }
    }

    test("Cancel intent flushes LOW priority queue and returns to Idle") {
        val dispatcher = ActionDispatcher()
        val engine = TaskEngine(dispatcher = dispatcher)

        try {
            val dummyAction = object : ModAction {
                override val priority = ActionPriority.LOW
                override suspend fun execute(context: ActionContext) = ActionResult.Success
            }

            engine.processIntent(TaskIntent.Start)
            engine.processIntent(TaskIntent.SnapshotComplete)
            engine.processIntent(TaskIntent.PlanCalculated(listOf(dummyAction, dummyAction)))

            dispatcher.pendingCount shouldBe 2

            engine.processIntent(TaskIntent.Cancel)

            engine.state.value shouldBe TaskState.Idle
            dispatcher.pendingCount shouldBe 0
        } finally {
            engine.close()
        }
    }
})
