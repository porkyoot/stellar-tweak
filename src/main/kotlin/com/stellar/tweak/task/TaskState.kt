package com.stellar.tweak.task

/**
 * Finite State Machine (FSM) states representing the lifecycle of an automated background task
 * (e.g. inventory sorting, chest compression, auto-tool swapping).
 */
sealed interface TaskState {
    /**
     * Initial resting state. No background task is running.
     */
    data object Idle : TaskState

    /**
     * Reading the authentic server game state (e.g. inventory slots, container sync, item counts).
     */
    data object Snapshotting : TaskState

    /**
     * Algorithmic planning phase computing item diffs, slot permutations, and required click actions.
     */
    data object Calculating : TaskState

    /**
     * Actively dispatching and executing actions through the scheduler.
     *
     * @property pendingActions Number of planned actions remaining to execute.
     */
    data class Executing(val pendingActions: Int) : TaskState

    /**
     * Desync, rejection, or anomaly encountered; invalidating stale actions and initiating recovery.
     *
     * @property reason Diagnostic explanation for why replanning was triggered.
     */
    data class Replanning(val reason: String) : TaskState
}
