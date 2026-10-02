package com.stellar.tweak.task

import com.stellar.core.action.ModAction
import com.stellar.tweak.task.sorting.InventoryStateComputer
import com.stellar.tweak.task.sorting.SortConfiguration
import com.stellar.tweak.task.sorting.TargetInventorySnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Algorithmic sorting strategy executing on a background thread pool via pure functional transformations.
 *
 * In accordance with client responsiveness requirements, calculation is offloaded to [dispatcher]
 * (defaults to [Dispatchers.Default]) so extensive item permutation and pathfinding never block
 * the Minecraft client render or tick loop.
 *
 * @property config Sorting configuration controlling layout and comparator priorities.
 * @property dispatcher Coroutine dispatcher utilized for background computation.
 */
class SmartSortStrategy(
    val config: SortConfiguration = SortConfiguration(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PlannerStrategy {
    override suspend fun calculatePlan(snapshot: InventorySnapshot): List<ModAction> = withContext(dispatcher) {
        computePlanPure(snapshot)
    }

    /**
     * Derives the target inventory state using the pure mathematical computation pipeline.
     *
     * @param snapshot Frozen state of the inventory grid.
     * @return Target inventory snapshot with desired final slot mappings.
     */
    fun computeTargetState(snapshot: InventorySnapshot): TargetInventorySnapshot =
        InventoryStateComputer.computeTargetState(snapshot, config)

    /**
     * Pure functional plan computation.
     *
     * Invariants:
     * - Pure function without side effects or mutable external dependencies.
     * - Always returns identical action sequences for identical inputs.
     * - Completely filters out and ignores slots where [SlotInfo.isLocked] is `true`.
     *
     * @param snapshot Frozen state of the inventory grid.
     * @return Sequence of [InventoryClickAction] commands.
     */
    fun computePlanPure(snapshot: InventorySnapshot): List<ModAction> {
        val target = computeTargetState(snapshot)
        if (target.slotTargets.isEmpty()) {
            return emptyList()
        }

        val unlockedSlots = snapshot.slots.filter { !it.isLocked }
        val targetMap = target.slotTargets

        if (isAlreadySorted(unlockedSlots, targetMap)) {
            return emptyList()
        }

        val planner = CyclePlanner(
            containerId = snapshot.containerId,
            indices = unlockedSlots.map { it.index },
            targetMap = targetMap,
            unlockedSlots = unlockedSlots,
            initialCursor = snapshot.carried,
        )
        return planner.plan()
    }

    private fun isAlreadySorted(slots: List<SlotInfo>, targetMap: Map<Int, SlotInfo>): Boolean {
        for (slot in slots) {
            val desired = targetMap[slot.index] ?: continue
            if (slot.itemId != desired.itemId || slot.count != desired.count) {
                return false
            }
        }
        return true
    }
}

private class CyclePlanner(
    private val containerId: Int,
    private val indices: List<Int>,
    private val targetMap: Map<Int, SlotInfo>,
    unlockedSlots: List<SlotInfo>,
    initialCursor: SlotInfo?,
) {
    private val virtualSlots = unlockedSlots.associateBy { it.index }.toMutableMap()
    private var cursor: SlotInfo? = initialCursor
    private val actions = mutableListOf<ModAction>()
    private val visited = mutableSetOf<Int>()

    fun plan(): List<ModAction> {
        indices.filter { shouldProcessSlot(it) }.forEach { i ->
            virtualSlots[i]?.let { current ->
                startCycle(i, current)
                runCycleSteps(i)
            }
        }
        flushCursor()
        return actions
    }

    private fun shouldProcessSlot(i: Int): Boolean {
        if (i in visited) return false
        val desired = targetMap[i] ?: return false
        val current = virtualSlots[i] ?: return false
        return current.itemId != desired.itemId || current.count != desired.count
    }

    private fun startCycle(startIdx: Int, current: SlotInfo) {
        if (cursor == null || cursor?.itemId?.isEmpty() == true) {
            actions.add(InventoryClickAction(slotId = startIdx, containerId = containerId))
            cursor = current
            virtualSlots[startIdx] = SlotInfo(index = startIdx, itemId = "", count = 0)
            visited.add(startIdx)
        }
    }

    private fun runCycleSteps(startIdx: Int) {
        val maxSteps = indices.size * 2
        var step = 0
        while (step < maxSteps) {
            step++
            if (!hasItemInCursor()) {
                break
            }
            val destIndex = findDestination(startIdx)
            actions.add(InventoryClickAction(slotId = destIndex, containerId = containerId))
            val prevInDest = virtualSlots[destIndex]
            virtualSlots[destIndex] = cursor ?: SlotInfo.empty(destIndex)
            cursor = if (prevInDest != null && prevInDest.itemId.isNotEmpty()) prevInDest else null
            visited.add(destIndex)
            if (destIndex == startIdx) {
                cursor = null
            }
        }
    }

    private fun hasItemInCursor(): Boolean = cursor?.itemId?.isNotEmpty() == true

    private fun findDestination(fallbackIdx: Int): Int {
        val cursorId = cursor?.itemId ?: return fallbackIdx
        val match = indices.firstOrNull { idx -> isSlotDemanding(idx, cursorId) }
        if (match != null) {
            return match
        }

        return indices.firstOrNull { idx -> virtualSlots[idx]?.isEmpty == true } ?: fallbackIdx
    }

    private fun isSlotDemanding(idx: Int, itemId: String): Boolean {
        val desired = targetMap[idx] ?: return false
        val current = virtualSlots[idx]
        return desired.itemId == itemId && current?.itemId != itemId
    }

    private fun flushCursor() {
        if (hasItemInCursor()) {
            val emptySlot = indices.firstOrNull { virtualSlots[it]?.isEmpty == true } ?: indices.first()
            actions.add(InventoryClickAction(slotId = emptySlot, containerId = containerId))
        }
    }
}
