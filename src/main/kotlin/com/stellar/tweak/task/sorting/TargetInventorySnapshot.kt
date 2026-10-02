package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.InventorySnapshot
import com.stellar.tweak.task.SlotInfo

/**
 * Immutable target snapshot of the desired state of an inventory after a sort plan.
 *
 * Maps physical slot indices to their planned final [SlotInfo] representation, maintaining
 * mathematical consistency and preventing slot overwrites or item duplication.
 *
 * @property containerId Window / sync ID of the sorted container.
 * @property slotTargets Mapping of physical slot indices to their desired final [SlotInfo].
 * @property cursorTarget Desired item state on the player cursor.
 */
data class TargetInventorySnapshot(
    val containerId: Int,
    val slotTargets: Map<Int, SlotInfo>,
    val cursorTarget: SlotInfo = SlotInfo.empty(-1),
) {
    /**
     * Converts the target mapping to an [InventorySnapshot] containing all slots in index order.
     */
    fun toInventorySnapshot(): InventorySnapshot {
        return InventorySnapshot(
            containerId = containerId,
            slots = slotTargets.entries.sortedBy { it.key }.map { it.value },
            carried = if (cursorTarget.isEmpty) null else cursorTarget,
        )
    }
}

/**
 * User configuration options steering the sorting calculation engine.
 *
 * @property layout Spatial arrangement algorithm ([SortLayout.ROW] or [SortLayout.COLUMN]).
 * @property comparatorOrder Priority sequence of [SortComparator] criteria.
 */
data class SortConfiguration(
    val layout: SortLayout = SortLayout.ROW,
    val comparatorOrder: List<SortComparator> = SortComparator.DEFAULT_ORDER,
)
