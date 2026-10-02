package com.stellar.tweak.task

import com.stellar.core.action.ModAction

/**
 * Unidirectional transition intents driving the [TaskEngine] in accordance with MVI principles.
 */
sealed interface TaskIntent {
    /**
     * Initiates the task lifecycle, transitioning to [TaskState.Snapshotting].
     */
    data object Start : TaskIntent

    /**
     * Signals that the true game/container snapshot has been captured, transitioning to [TaskState.Calculating].
     */
    data object SnapshotComplete : TaskIntent

    /**
     * Supplies the calculated action sequence to the engine for execution.
     *
     * @property actions Sequence of discrete actions planned for execution.
     */
    data class PlanCalculated(val actions: List<ModAction>) : TaskIntent

    /**
     * Signals that one planned action has finished executing.
     */
    data object ActionCompleted : TaskIntent

    /**
     * Reports an action execution failure or server desync event.
     *
     * @property action The failed action instance.
     * @property reason Human-readable diagnostic explaining the failure.
     */
    data class ActionFailed(
        val action: ModAction,
        val reason: String,
    ) : TaskIntent

    /**
     * Explicitly halts and aborts the active task, resetting to [TaskState.Idle].
     */
    data object Cancel : TaskIntent
}
