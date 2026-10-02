package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo

/**
 * Base abstract layout utilizing hierarchical comparator depth matching to group similar items.
 *
 * Implements an iterative agglomerative clustering strategy: items are initially partitioned into
 * groups by strict comparator depth. If the resulting grouped layout exceeds available slot constraints,
 * adjacent groups with the highest common prefix depth are merged until the layout fits.
 *
 * @property comparators Sequence of grouping comparators used to evaluate similarity depth.
 */
abstract class AbstractDepthLayout(
    protected val comparators: List<Comparator<SlotInfo>>,
) : ISortingLayout {
    override fun layout(items: List<SlotInfo>, availableSlots: List<SlotInfo>): List<SlotInfo> {
        if (items.isEmpty() || availableSlots.isEmpty()) {
            return fillEmpty(mutableListOf(), availableSlots.size)
        }

        val normalizedSlots = normalizeSlotCoordinates(availableSlots)
        val groups = buildInitialGroups(items)
        mergeGroupsUntilFit(groups, normalizedSlots)
        val padding = computeMaxPadding(groups, normalizedSlots)

        return emitGroups(groups, normalizedSlots, padding)
    }

    protected fun buildInitialGroups(items: List<SlotInfo>): MutableList<Group> {
        val groups = mutableListOf<Group>()
        var currentGroup = Group()
        var previous: SlotInfo? = null

        for (item in items.filterNot { it.isEmpty }) {
            if (shouldStartNewGroup(previous, item)) {
                groups.add(currentGroup)
                currentGroup = Group()
            }
            currentGroup.items.add(item)
            previous = item
        }
        if (currentGroup.items.isNotEmpty()) {
            groups.add(currentGroup)
        }
        return groups
    }

    private fun shouldStartNewGroup(previous: SlotInfo?, item: SlotInfo): Boolean =
        previous != null && differsAtDepth(previous, item, comparators.size)

    protected fun mergeGroupsUntilFit(groups: MutableList<Group>, availableSlots: List<SlotInfo>) {
        while (groups.size > 1 && !fitsLayout(groups, availableSlots, padding = 0)) {
            val bestIndex = findBestMergeIndex(groups)
            val targetGroup = groups[bestIndex]
            val mergedGroup = groups.removeAt(bestIndex + 1)
            targetGroup.items.addAll(mergedGroup.items)
        }
    }

    private fun findBestMergeIndex(groups: List<Group>): Int {
        var bestIndex = 0
        var maxMatchDepth = -1
        for (i in 0 until groups.size - 1) {
            val depth = findMatchDepth(groups[i].representative, groups[i + 1].representative)
            if (depth > maxMatchDepth) {
                maxMatchDepth = depth
                bestIndex = i
            }
        }
        return bestIndex
    }

    protected fun computeMaxPadding(groups: List<Group>, availableSlots: List<SlotInfo>): Int {
        var padding = 0
        if (groups.size > 1) {
            while (fitsLayout(groups, availableSlots, padding)) {
                padding++
            }
            padding = maxOf(0, padding - 1)
        }
        return padding
    }

    protected fun findMatchDepth(a: SlotInfo, b: SlotInfo): Int {
        for (i in comparators.indices) {
            if (comparators[i].compare(a, b) != 0) {
                return i
            }
        }
        return comparators.size
    }

    protected fun differsAtDepth(a: SlotInfo, b: SlotInfo, depthLimit: Int): Boolean {
        val limit = minOf(depthLimit, comparators.size)
        for (i in 0 until limit) {
            if (comparators[i].compare(a, b) != 0) {
                return true
            }
        }
        return false
    }

    protected abstract fun fitsLayout(groups: List<Group>, availableSlots: List<SlotInfo>, padding: Int): Boolean

    protected abstract fun emitGroups(groups: List<Group>, availableSlots: List<SlotInfo>, padding: Int): List<SlotInfo>

    protected fun fillEmpty(list: MutableList<SlotInfo>, targetSize: Int): List<SlotInfo> {
        while (list.size < targetSize) {
            list.add(SlotInfo.empty(list.size))
        }
        return list
    }

    protected fun normalizeSlotCoordinates(slots: List<SlotInfo>): List<SlotInfo> {
        val hasExplicitCoordinates = slots.any { it.x != 0 || it.y != 0 }
        if (hasExplicitCoordinates) return slots

        return slots.mapIndexed { idx, slot ->
            val col = idx % DEFAULT_CONTAINER_WIDTH
            val row = idx / DEFAULT_CONTAINER_WIDTH
            slot.copy(x = col * SLOT_STRIDE, y = row * SLOT_STRIDE)
        }
    }

    /**
     * Inner cluster of item stacks forming a cohesive sorting grouping.
     */
    data class Group(
        val items: MutableList<SlotInfo> = mutableListOf(),
    ) {
        val representative: SlotInfo
            get() = items.firstOrNull() ?: SlotInfo.empty(-1)

        val size: Int
            get() = items.size
    }

    companion object {
        const val DEFAULT_CONTAINER_WIDTH = 9
        const val SLOT_STRIDE = 18
    }
}
