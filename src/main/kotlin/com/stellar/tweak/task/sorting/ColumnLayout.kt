package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo

/**
 * Column-major sorting layout algorithm with dynamic column breaks and padding between groups.
 *
 * Items within a group are placed top-to-bottom across vertical columns. Distinct groups cause cursor
 * advances to the top of the next column, optionally skipping additional padding columns.
 *
 * @param comparators Grouping comparators used to partition item clusters.
 */
class ColumnLayout(
    comparators: List<Comparator<SlotInfo>>,
) : AbstractDepthLayout(comparators) {
    override fun fitsLayout(groups: List<Group>, availableSlots: List<SlotInfo>, padding: Int): Boolean {
        if (groups.isEmpty()) return true
        val colHeight = getColHeight(availableSlots)
        val totalColumns = getTotalColumns(availableSlots)

        val columnsForGroups = groups.sumOf { (it.size + colHeight - 1) / colHeight }
        val paddingColumns = (groups.size - 1) * padding
        val totalColumnsRequired = columnsForGroups + paddingColumns

        return totalColumnsRequired <= totalColumns
    }

    override fun emitGroups(groups: List<Group>, availableSlots: List<SlotInfo>, padding: Int): List<SlotInfo> {
        val colMajorIndices = buildColMajorIndices(availableSlots)
        val grid = arrayOfNulls<SlotInfo>(availableSlots.size)
        var cursor = 0

        for (g in groups.indices) {
            cursor = populateColumnGroup(grid, colMajorIndices, cursor, groups[g])
            if (shouldBreakColumn(g, groups.size, cursor, colMajorIndices.size)) {
                cursor = advanceToNextColumn(cursor, colMajorIndices, availableSlots, padding)
            }
        }

        val output = mutableListOf<SlotInfo>()
        for (i in availableSlots.indices) {
            val item = grid[i]
            output.add(item ?: SlotInfo.empty(availableSlots[i].index))
        }

        return fillEmpty(output, availableSlots.size)
    }

    private fun populateColumnGroup(
        grid: Array<SlotInfo?>,
        colMajorIndices: List<Int>,
        cursor: Int,
        group: Group,
    ): Int {
        var current = cursor
        for (s in group.items) {
            if (current < colMajorIndices.size) {
                grid[colMajorIndices[current]] = s
                current++
            }
        }
        return current
    }

    private fun shouldBreakColumn(groupIndex: Int, totalGroups: Int, cursor: Int, limit: Int): Boolean {
        return groupIndex < totalGroups - 1 && cursor > 0 && cursor < limit
    }

    private fun advanceToNextColumn(
        cursor: Int,
        colMajorIndices: List<Int>,
        availableSlots: List<SlotInfo>,
        padding: Int,
    ): Int {
        val currentX = availableSlots[colMajorIndices[cursor - 1]].x
        var next = cursor
        while (next < colMajorIndices.size &&
            availableSlots[colMajorIndices[next]].x == currentX
        ) {
            next++
        }
        repeat(padding) {
            if (next < colMajorIndices.size) {
                val padX = availableSlots[colMajorIndices[next]].x
                while (next < colMajorIndices.size &&
                    availableSlots[colMajorIndices[next]].x == padX
                ) {
                    next++
                }
            }
        }
        return next
    }

    private fun getColHeight(slots: List<SlotInfo>): Int {
        if (slots.isEmpty()) return 1
        val colMajor = buildColMajorIndices(slots)
        val firstX = slots[colMajor.first()].x
        var height = 0
        for (idx in colMajor) {
            if (slots[idx].x == firstX) {
                height++
            }
        }
        return maxOf(1, height)
    }

    private fun getTotalColumns(slots: List<SlotInfo>): Int {
        if (slots.isEmpty()) return 1
        return slots.map { it.x }.distinct().size.coerceAtLeast(1)
    }

    private fun buildColMajorIndices(slots: List<SlotInfo>): List<Int> {
        val indices = slots.indices.toMutableList()
        indices.sortWith { a, b ->
            val sa = slots[a]
            val sb = slots[b]
            if (sa.x != sb.x) sa.x.compareTo(sb.x) else sa.y.compareTo(sb.y)
        }
        return indices
    }
}
