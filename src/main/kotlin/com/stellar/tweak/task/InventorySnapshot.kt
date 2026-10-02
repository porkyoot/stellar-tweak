package com.stellar.tweak.task

/**
 * Immutable representation of a single inventory slot in the frozen grid snapshot.
 *
 * @property index Absolute index of the slot in the container grid.
 * @property itemId Namespaced item identifier (e.g. "minecraft:diamond_sword", "minecraft:air").
 * @property count Stack size or quantity present in the slot (0 for empty slots).
 * @property isLocked Whether this slot is locked from being moved or sorted.
 * @property maxStackSize Maximum allowed stack size for the item in this slot.
 * @property customName Display or custom hover name of the item.
 * @property rarity Item rarity tier ordinal (0 = Common, 1 = Uncommon, 2 = Rare, 3 = Epic).
 * @property tags Lexicographical tags attached to the item.
 * @property hue Dominant color hue in 0.0..1.0 (-1.0 if undefined or monochromatic).
 * @property creativeOrder Tab index / order in the creative inventory menu.
 * @property x Horizontal pixel / column coordinate of the slot in the UI layout.
 * @property y Vertical pixel / row coordinate of the slot in the UI layout.
 */
data class SlotInfo(
    val index: Int,
    val itemId: String,
    val count: Int,
    val isLocked: Boolean = false,
    val maxStackSize: Int = DEFAULT_MAX_STACK_SIZE,
    val customName: String? = null,
    val rarity: Int = 0,
    val tags: List<String> = emptyList(),
    val hue: Float = -1.0f,
    val creativeOrder: Int = Int.MAX_VALUE,
    val x: Int = 0,
    val y: Int = 0,
) {
    /**
     * Whether this slot is unoccupied or contains air.
     */
    val isEmpty: Boolean
        get() = count <= 0 || itemId.isEmpty() || itemId == AIR_ITEM_ID

    companion object {
        const val AIR_ITEM_ID = "minecraft:air"
        const val DEFAULT_MAX_STACK_SIZE = 64

        /**
         * Convenience factory for an empty slot.
         */
        fun empty(index: Int, isLocked: Boolean = false): SlotInfo =
            SlotInfo(index = index, itemId = AIR_ITEM_ID, count = 0, isLocked = isLocked)
    }
}

/**
 * Checks whether this slot contains the same item type and components as [other].
 */
fun SlotInfo.isSameItem(other: SlotInfo): Boolean {
    if (isEmpty && other.isEmpty) return true
    if (isEmpty != other.isEmpty) return false
    return itemId == other.itemId &&
        customName == other.customName &&
        rarity == other.rarity &&
        tags == other.tags
}

/**
 * Immutable frozen snapshot of a container or player inventory grid.
 *
 * Captures the complete state at a single point in time to allow background sorting
 * calculations off the main game loop thread.
 *
 * @property containerId Sync ID or window ID of the active container.
 * @property slots All captured slots belonging to the sortable region.
 * @property carried Floating item currently held on the player's cursor, if any.
 */
data class InventorySnapshot(
    val containerId: Int = 0,
    val slots: List<SlotInfo>,
    val carried: SlotInfo? = null,
) {
    /**
     * Total number of slots captured in this snapshot.
     */
    val size: Int
        get() = slots.size

    /**
     * Returns only the slots that are not locked.
     */
    val unlockedSlots: List<SlotInfo>
        get() = slots.filterNot { it.isLocked }
}

/**
 * Retrieves the [SlotInfo] at the given slot [index], if present.
 */
fun InventorySnapshot.getSlot(index: Int): SlotInfo? = slots.getOrNull(index)
