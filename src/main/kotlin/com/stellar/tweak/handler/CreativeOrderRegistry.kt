package com.stellar.tweak.handler

import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item

/**
 * Caches creative menu display ordering and tab partitions to power category-based sorting.
 */
object CreativeOrderRegistry {
    const val TAB_STRIDE = 10_000
    private var orderMap: Map<Item, Int>? = null

    /**
     * Returns the composite tab and item order index for [item], or [Int.MAX_VALUE] if unmapped.
     */
    fun getCreativeOrder(item: Item): Int {
        val map = orderMap ?: synchronized(this) {
            orderMap ?: buildOrderMap().also { orderMap = it }
        }
        return map[item] ?: Int.MAX_VALUE
    }

    private fun buildOrderMap(): Map<Item, Int> {
        val map = HashMap<Item, Int>()
        val tabs = runCatching { CreativeModeTabs.allTabs() }.getOrNull() ?: return map

        var tabIndex = 0
        for (tab in tabs) {
            if (tab.type == CreativeModeTab.Type.SEARCH) continue
            populateTabItems(map, tab, tabIndex)
            tabIndex++
        }
        return map
    }

    private fun populateTabItems(map: MutableMap<Item, Int>, tab: CreativeModeTab, tabIndex: Int) {
        val items = runCatching { tab.displayItems }.getOrNull() ?: return
        var itemOffset = 0
        for (stack in items) {
            val item = stack.item
            if (!map.containsKey(item)) {
                map[item] = tabIndex * TAB_STRIDE + itemOffset
                itemOffset++
            }
        }
    }
}
