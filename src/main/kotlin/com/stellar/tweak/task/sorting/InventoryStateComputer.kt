package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.InventorySnapshot
import com.stellar.tweak.task.SlotInfo
import com.stellar.tweak.task.isSameItem

/**
 * Pure mathematical computation engine for deriving the target inventory state from an initial snapshot.
 *
 * Implements the full sorting pipeline:
 * 1. Filtering unlocked slots from locked immutable boundaries.
 * 2. Consolidating partial item stacks via [StackConsolidator].
 * 3. Sorting items through hierarchical multi-tier [SortComparator] chains.
 * 4. Geometric distribution via [ISortingLayout] with depth-matching grouping and empty padding.
 * 5. Invariant preservation via dropped item recovery.
 */
object InventoryStateComputer {
    /**
     * Computes the final target inventory state from the frozen [snapshot] and [config].
     *
     * @param snapshot Frozen initial snapshot of the inventory.
     * @param config Sorting configuration containing layout geometry and comparator priority hierarchy.
     * @return Immutable [TargetInventorySnapshot] containing final slot mappings.
     */
    fun computeTargetState(
        snapshot: InventorySnapshot,
        config: SortConfiguration = SortConfiguration(),
    ): TargetInventorySnapshot {
        val slotsToSort = snapshot.unlockedSlots
        val lockedSlots = snapshot.slots.filter { it.isLocked }

        if (slotsToSort.isEmpty()) {
            return buildUnchangedSnapshot(snapshot)
        }

        val rawItems = collectSortableItems(slotsToSort, snapshot.carried)
        val consolidatedItems = StackConsolidator.consolidate(rawItems)
            .filterNot { it.isEmpty }
            .toMutableList()

        val hierarchy = SortComparator.buildHierarchy(config.comparatorOrder)
        consolidatedItems.sortWith(hierarchy)

        val layout = selectLayoutStrategy(config)
        val finalPositions = layout.layout(consolidatedItems, slotsToSort).toMutableList()

        val cursorTarget = resolveCursorTarget(consolidatedItems, slotsToSort.size)
        recoverDroppedItems(finalPositions, consolidatedItems)

        val slotTargets = assembleSlotTargets(slotsToSort, lockedSlots, finalPositions)
        return TargetInventorySnapshot(
            containerId = snapshot.containerId,
            slotTargets = slotTargets,
            cursorTarget = cursorTarget,
        )
    }

    private fun collectSortableItems(slotsToSort: List<SlotInfo>, carried: SlotInfo?): List<SlotInfo> {
        val items = slotsToSort.filterNot { it.isEmpty }.toMutableList()
        if (carried != null && !carried.isEmpty) {
            items.add(carried)
        }
        return items
    }

    private fun selectLayoutStrategy(config: SortConfiguration): ISortingLayout {
        val groupingComparators = SortComparator.buildGroupingComparators(config.comparatorOrder)
        return when (config.layout) {
            SortLayout.COLUMN -> ColumnLayout(groupingComparators)
            SortLayout.ROW -> RowLayout(groupingComparators)
        }
    }

    private fun resolveCursorTarget(items: List<SlotInfo>, availableSlotCount: Int): SlotInfo =
        if (items.size > availableSlotCount) {
            items.last()
        } else {
            SlotInfo.empty(-1)
        }

    private fun recoverDroppedItems(finalPositions: MutableList<SlotInfo>, items: List<SlotInfo>) {
        val trackingList = items.toMutableList()
        for (positioned in finalPositions.filterNot { it.isEmpty }) {
            removePositionedFromTracking(trackingList, positioned)
        }

        for (missing in trackingList) {
            placeRecoveredItem(finalPositions, missing)
        }
    }

    private fun removePositionedFromTracking(trackingList: MutableList<SlotInfo>, positioned: SlotInfo) {
        val idx = trackingList.indexOfFirst { it.isSameItem(positioned) && it.count == positioned.count }
        if (idx >= 0) {
            trackingList.removeAt(idx)
        }
    }

    private fun placeRecoveredItem(finalPositions: MutableList<SlotInfo>, missing: SlotInfo) {
        for (i in finalPositions.indices) {
            if (finalPositions[i].isEmpty) {
                finalPositions[i] = missing
                return
            }
        }
        if (finalPositions.isNotEmpty()) {
            finalPositions[finalPositions.lastIndex] = missing
        }
    }

    private fun assembleSlotTargets(
        slotsToSort: List<SlotInfo>,
        lockedSlots: List<SlotInfo>,
        finalPositions: List<SlotInfo>,
    ): Map<Int, SlotInfo> {
        val targets = mutableMapOf<Int, SlotInfo>()

        for (locked in lockedSlots) {
            targets[locked.index] = locked
        }

        for (i in slotsToSort.indices) {
            val slot = slotsToSort[i]
            val item = finalPositions.getOrElse(i) { SlotInfo.empty(slot.index) }
            targets[slot.index] = item.copy(
                index = slot.index,
                isLocked = false,
                x = slot.x,
                y = slot.y,
            )
        }

        return targets
    }

    private fun buildUnchangedSnapshot(snapshot: InventorySnapshot): TargetInventorySnapshot {
        val targets = snapshot.slots.associateBy { it.index }
        return TargetInventorySnapshot(
            containerId = snapshot.containerId,
            slotTargets = targets,
            cursorTarget = snapshot.carried ?: SlotInfo.empty(-1),
        )
    }
}
