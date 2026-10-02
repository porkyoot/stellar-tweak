package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo

/**
 * Row-major sorting layout algorithm with dynamic line breaks and padding between groups.
 *
 * Items within a group are placed left-to-right across rows. Distinct groups cause cursor advances
 * to the start of the next row, optionally skipping additional padding rows.
 *
 * @param comparators Grouping comparators used to partition item clusters.
 */
class RowLayout(
    comparators: List<Comparator<SlotInfo>>,
) : AbstractDepthLayout(comparators) {
    override fun fitsLayout(groups: List<Group>, availableSlots: List<SlotInfo>, padding: Int): Boolean {
        val rowWidth = getRowWidth(availableSlots)
        val totalSpacesRequired = groups.sumOf { it.size }
        val breaks = groups.size - 1
        val breakSpace = rowWidth - 1 + padding * rowWidth
        val worstCaseSize = totalSpacesRequired + breaks * breakSpace

        return worstCaseSize <= availableSlots.size
    }

    override fun emitGroups(groups: List<Group>, availableSlots: List<SlotInfo>, padding: Int): List<SlotInfo> {
        val grid = arrayOfNulls<SlotInfo>(availableSlots.size)
        var cursor = 0

        for (g in groups.indices) {
            cursor = populateGroupItems(grid, cursor, groups[g], availableSlots.size)
            if (shouldBreakRow(g, groups.size, cursor, availableSlots.size)) {
                cursor = advanceToNextRow(cursor, availableSlots, padding)
            }
        }

        val output = mutableListOf<SlotInfo>()
        for (i in availableSlots.indices) {
            val item = grid[i]
            output.add(item ?: SlotInfo.empty(availableSlots[i].index))
        }

        return fillEmpty(output, availableSlots.size)
    }

    private fun populateGroupItems(grid: Array<SlotInfo?>, cursor: Int, group: Group, limit: Int): Int {
        var currentCursor = cursor
        for (s in group.items) {
            if (currentCursor < limit) {
                grid[currentCursor] = s
                currentCursor++
            }
        }
        return currentCursor
    }

    private fun shouldBreakRow(groupIndex: Int, totalGroups: Int, cursor: Int, limit: Int): Boolean {
        return groupIndex < totalGroups - 1 && cursor > 0 && cursor < limit
    }

    private fun advanceToNextRow(cursor: Int, availableSlots: List<SlotInfo>, padding: Int): Int {
        val currentY = availableSlots[cursor - 1].y
        var next = cursor
        while (next < availableSlots.size && availableSlots[next].y == currentY) {
            next++
        }
        repeat(padding) {
            if (next < availableSlots.size) {
                val padY = availableSlots[next].y
                while (next < availableSlots.size && availableSlots[next].y == padY) {
                    next++
                }
            }
        }
        return next
    }

    private fun getRowWidth(slots: List<SlotInfo>): Int {
        if (slots.isEmpty()) return 1
        val firstY = slots.first().y
        var count = 0
        for (s in slots) {
            if (s.y == firstY) {
                count++
            } else {
                break
            }
        }
        return maxOf(1, count)
    }
}
