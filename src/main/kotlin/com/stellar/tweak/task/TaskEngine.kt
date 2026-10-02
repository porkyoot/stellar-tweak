package com.stellar.tweak.task

import com.stellar.core.action.ActionDispatcher
import com.stellar.core.action.ActionPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Background task orchestrator managing complex client-side routines (such as inventory sorting)
 * using an MVI Finite State Machine.
 *
 * Integrates directly with [ActionDispatcher]:
 * - Collects [ActionDispatcher.failureFlow] in a background coroutine.
 * - If in [TaskState.Executing] and a dispatched [ActionPriority.LOW] action fails (e.g. server desync),
 *   it immediately transitions to [TaskState.Replanning].
 * - Automatically calls [ActionDispatcher.clearQueue] with `{ it.priority == ActionPriority.LOW }`
 *   to flush obsolete clicks.
 * - Transitions back to [TaskState.Snapshotting] to re-read server state and recover cleanly.
 *
 * @property dispatcher The action execution engine used to queue and clear actions.
 * @property scope Coroutine scope in which failure observation and async intents execute.
 * @property onSnapshot Optional hook invoked when transitioning to [TaskState.Snapshotting].
 */
class TaskEngine(
    val dispatcher: ActionDispatcher,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val onSnapshot: (suspend () -> Unit)? = null,
) : AutoCloseable {
    private val _state = MutableStateFlow<TaskState>(TaskState.Idle)

    /**
     * Reactive state stream emitting current FSM states.
     */
    val state: StateFlow<TaskState> = _state.asStateFlow()

    private val mutex = Mutex()

    private val collectorJob = scope.launch {
        dispatcher.failureFlow.collect { event ->
            processIntent(
                TaskIntent.ActionFailed(
                    action = event.action,
                    reason = event.result.reason,
                ),
            )
        }
    }

    /**
     * Stops the background failure flow observer and cleans up coroutine jobs.
     */
    fun stop() {
        collectorJob.cancel()
    }

    override fun close() {
        stop()
    }

    /**
     * Processes a transition intent according to MVI unidirectional data flow rules.
     *
     * @param intent The transition intent to reduce against the current state.
     */
    suspend fun processIntent(intent: TaskIntent) {
        mutex.withLock {
            reduce(intent)
        }
    }

    private suspend fun reduce(intent: TaskIntent) {
        when (intent) {
            is TaskIntent.Start -> handleStart()
            is TaskIntent.SnapshotComplete -> handleSnapshotComplete()
            is TaskIntent.PlanCalculated -> handlePlanCalculated(intent)
            is TaskIntent.ActionCompleted -> handleActionCompleted()
            is TaskIntent.ActionFailed -> handleActionFailed(intent)
            is TaskIntent.Cancel -> handleCancel()
        }
    }

    private suspend fun handleStart() {
        if (_state.value is TaskState.Idle || _state.value is TaskState.Replanning) {
            _state.value = TaskState.Snapshotting
            onSnapshot?.invoke()
        }
    }

    private fun handleSnapshotComplete() {
        if (_state.value is TaskState.Snapshotting) {
            _state.value = TaskState.Calculating
        }
    }

    private fun handlePlanCalculated(intent: TaskIntent.PlanCalculated) {
        if (_state.value is TaskState.Calculating) {
            val actions = intent.actions
            if (actions.isEmpty()) {
                _state.value = TaskState.Idle
                return
            }
            for (action in actions) {
                dispatcher.enqueue(action)
            }
            _state.value = TaskState.Executing(pendingActions = actions.size)
        }
    }

    private fun handleActionCompleted() {
        val current = _state.value
        if (current is TaskState.Executing) {
            val remaining = current.pendingActions - 1
            if (remaining <= 0) {
                _state.value = TaskState.Idle
            } else {
                _state.value = TaskState.Executing(pendingActions = remaining)
            }
        }
    }

    private suspend fun handleActionFailed(intent: TaskIntent.ActionFailed) {
        if (_state.value is TaskState.Executing && intent.action.priority == ActionPriority.LOW) {
            // 1. Immediately transition to Replanning
            _state.value = TaskState.Replanning(reason = intent.reason)

            // 2. Flush obsolete clicks from the dispatcher queue
            dispatcher.clearQueue { it.priority == ActionPriority.LOW }

            // 3. Automatically transition back to Snapshotting to restart based on true server state
            _state.value = TaskState.Snapshotting
            onSnapshot?.invoke()
        }
    }

    private fun handleCancel() {
        dispatcher.clearQueue { it.priority == ActionPriority.LOW }
        _state.value = TaskState.Idle
    }
}
