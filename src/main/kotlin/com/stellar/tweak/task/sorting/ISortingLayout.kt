package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo

/**
 * Strategy interface defining an inventory spatial layout algorithm.
 */
fun interface ISortingLayout {
    /**
     * Positions the pre-sorted list of [items] into the specified [availableSlots],
     * utilizing empty padding for group separation.
     *
     * @param items Pre-sorted list of items to place.
     * @param availableSlots Sequence of strictly unlocked physical slots available in the container.
     * @return Flat list of exact size matching availableSlots.size containing placed items and empty padding.
     */
    fun layout(items: List<SlotInfo>, availableSlots: List<SlotInfo>): List<SlotInfo>
}
