package com.stellar.tweak.task

import com.stellar.core.action.ModAction

/**
 * Strategy Pattern contract for computing inventory sorting or reorganization plans.
 *
 * Allows interchangeable sorting algorithms (e.g. alphabetical sorting, category-based
 * grouping, slot compaction, or heuristic restock) without modifying the task orchestrator.
 */
interface PlannerStrategy {
    /**
     * Computes a planned sequence of [ModAction] commands from the frozen [snapshot].
     *
     * As a suspending function, implementations can offload heavy computations to background
     * coroutine dispatchers (such as [kotlinx.coroutines.Dispatchers.Default]) to prevent UI thread stutter.
     *
     * @param snapshot Frozen immutable snapshot of the inventory state.
     * @return Sequence of actions to dispatch.
     */
    suspend fun calculatePlan(snapshot: InventorySnapshot): List<ModAction>
}
