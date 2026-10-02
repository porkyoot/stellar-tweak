package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo
import com.stellar.tweak.task.isSameItem

/**
 * Pure mathematical consolidator that merges identical item stacks up to their maximum stack limit.
 *
 * Consolidates partial stacks of identical items into earlier slots, freeing up inventory spaces.
 */
object StackConsolidator {
    /**
     * Consolidates a list of [items], transferring quantities between compatible stacks.
     *
     * Invariants:
     * - Total count of every distinct item type remains strictly invariant.
     * - No stack exceeds its [SlotInfo.maxStackSize].
     * - Pure function without side-effects.
     *
     * @param items Initial item stacks to consolidate.
     * @return New list of item stacks with partial stacks merged.
     */
    fun consolidate(items: List<SlotInfo>): List<SlotInfo> {
        val working = items.map { it.copy() }.toMutableList()
        for (i in working.indices) {
            consolidateSlot(working, i)
        }
        return working
    }

    private fun consolidateSlot(working: MutableList<SlotInfo>, targetIdx: Int) {
        val target = working[targetIdx]
        if (target.isEmpty || target.count >= target.maxStackSize) {
            return
        }

        for (sourceIdx in targetIdx + 1 until working.size) {
            val absorbed = tryMergeSlot(working, targetIdx, sourceIdx)
            if (absorbed) {
                break
            }
        }
    }

    private fun tryMergeSlot(working: MutableList<SlotInfo>, targetIdx: Int, sourceIdx: Int): Boolean {
        val target = working[targetIdx]
        val source = working[sourceIdx]
        if (source.isEmpty || !target.isSameItem(source)) {
            return false
        }

        transferBetweenSlots(working, targetIdx, sourceIdx)
        return working[targetIdx].count >= working[targetIdx].maxStackSize
    }

    private fun transferBetweenSlots(working: MutableList<SlotInfo>, targetIdx: Int, sourceIdx: Int) {
        val target = working[targetIdx]
        val source = working[sourceIdx]
        val availableSpace = target.maxStackSize - target.count
        val transferAmount = minOf(source.count, availableSpace)
        if (transferAmount <= 0) return

        working[targetIdx] = target.copy(count = target.count + transferAmount)
        val remainingSource = source.count - transferAmount
        working[sourceIdx] = if (remainingSource > 0) {
            source.copy(count = remainingSource)
        } else {
            SlotInfo.empty(source.index)
        }
    }
}
