package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo
import kotlin.math.roundToInt

/**
 * Named item comparators implementing the inventory sorting priority system.
 *
 * Provides both precise ordering comparators and broader grouping comparators used for
 * layout depth clustering (e.g. initial letters for names, discretized hue groups for color).
 *
 * @property displayName Human-readable label for configuration UIs.
 * @property comparator Primary comparator for exact item ordering.
 * @property groupingComparator Coarser grouping comparator for spatial layout partitions.
 */
enum class SortComparator(
    val displayName: String,
    val comparator: Comparator<SlotInfo>,
    val groupingComparator: Comparator<SlotInfo>,
) {
    CATEGORY("Category", SlotComparators.CREATIVE_MENU, SlotComparators.CREATIVE_CATEGORY),
    MOD("Mod", SlotComparators.MOD, SlotComparators.MOD),
    MATERIAL("Material", SlotComparators.MATERIAL, SlotComparators.MATERIAL),
    TAG("Tag", SlotComparators.TAG, SlotComparators.TAG),
    COLOR("Color", SlotComparators.COLOR, SlotComparators.COLOR_GROUPING),
    RARITY("Rarity", SlotComparators.RARITY_DESC, SlotComparators.RARITY_DESC),
    NAME("Name", SlotComparators.NAME, SlotComparators.LETTER),
    ID("Registry ID", SlotComparators.ID, SlotComparators.NOOP),
    AMOUNT("Amount", SlotComparators.AMOUNT_DESC, SlotComparators.NOOP),
    ;

    companion object {
        /**
         * Default sorting hierarchy matching classic Piggy Inventory priority ordering.
         */
        val DEFAULT_ORDER: List<SortComparator> = listOf(
            CATEGORY,
            TAG,
            MATERIAL,
            MOD,
            COLOR,
            RARITY,
            NAME,
            ID,
            AMOUNT,
        )

        /**
         * Chained default comparator hierarchy.
         */
        val DEFAULT_HIERARCHY: Comparator<SlotInfo> = buildHierarchy(DEFAULT_ORDER)

        /**
         * Builds a chained comparator hierarchy from an ordered list of [SortComparator] tokens.
         */
        fun buildHierarchy(order: List<SortComparator>): Comparator<SlotInfo> {
            val comparators = order.map { it.comparator }
            return comparators.reduceOrNull { acc, cmp -> acc.thenComparing(cmp) }
                ?: DEFAULT_HIERARCHY
        }

        /**
         * Builds the list of coarse grouping comparators used for spatial layout partitions.
         *
         * Excludes fine-grained item identity comparators (ID, AMOUNT) so items belonging to the same
         * category or material cluster together into columns or rows rather than each occupying a single slot.
         */
        fun buildGroupingComparators(order: List<SortComparator>): List<Comparator<SlotInfo>> {
            val isNamePrimary = order.firstOrNull() == NAME
            val groupingList = mutableListOf<Comparator<SlotInfo>>()

            for (comp in order) {
                val resolved = resolveGroupingComparator(comp, isNamePrimary && groupingList.isEmpty())
                if (resolved != null) {
                    groupingList.add(resolved)
                }
            }

            return groupingList.ifEmpty {
                listOf(
                    SlotComparators.CREATIVE_CATEGORY,
                    SlotComparators.TAG,
                    SlotComparators.MATERIAL,
                    SlotComparators.MOD,
                )
            }
        }

        private fun resolveGroupingComparator(comp: SortComparator, allowNameLetter: Boolean): Comparator<SlotInfo>? =
            when (comp) {
                CATEGORY -> SlotComparators.CREATIVE_CATEGORY
                TAG -> SlotComparators.TAG
                MATERIAL -> SlotComparators.MATERIAL
                MOD -> SlotComparators.MOD
                COLOR -> SlotComparators.COLOR_GROUPING
                RARITY -> SlotComparators.RARITY_DESC
                NAME -> if (allowNameLetter) SlotComparators.LETTER else null
                ID, AMOUNT -> null
            }
    }
}

/**
 * Underlying comparator definitions and pure mathematical string transformers.
 */
internal object SlotComparators {
    private const val COLOR_CHUNKS = 12.0f
    private const val UNASSIGNED_TAG_SENTINEL = "\uFFFF"

    private val MATERIAL_AFFIXES = listOf(
        "raw_", "deepslate_", "stripped_", "weathered_", "exposed_", "oxidized_",
        "waxed_", "cut_", "chiseled_", "polished_", "smooth_", "cracked_", "block_of_",
        "_block", "_ingot", "_nugget", "_ore", "_dust", "_scrap",
        "_pickaxe", "_sword", "_axe", "_shovel", "_hoe",
        "_helmet", "_chestplate", "_leggings", "_boots",
        "_log", "_wood", "_planks", "_stairs", "_slab",
        "_door", "_trapdoor", "_fence_gate", "_fence",
        "_pressure_plate", "_button", "_chest_boat", "_boat",
        "_leaves", "_sapling", "_hanging_sign", "_sign", "_wall",
    )

    const val CREATIVE_TAB_STRIDE = 10_000

    val CREATIVE_MENU: Comparator<SlotInfo> =
        Comparator.comparingInt { it.creativeOrder }

    val CREATIVE_CATEGORY: Comparator<SlotInfo> =
        Comparator.comparingInt { it.creativeOrder / CREATIVE_TAB_STRIDE }

    val NOOP: Comparator<SlotInfo> =
        Comparator { _, _ -> 0 }

    val MOD: Comparator<SlotInfo> =
        Comparator.comparing { it.itemId.substringBefore(':', missingDelimiterValue = "minecraft") }

    val MATERIAL: Comparator<SlotInfo> =
        Comparator.comparing { getMaterial(it.itemId) }

    val TAG: Comparator<SlotInfo> =
        Comparator.comparing { it.tags.sorted().firstOrNull() ?: UNASSIGNED_TAG_SENTINEL }

    val COLOR: Comparator<SlotInfo> =
        Comparator.comparingDouble { it.hue.toDouble() }

    val COLOR_GROUPING: Comparator<SlotInfo> =
        Comparator.comparingInt { computeColorGroup(it.hue) }

    val RARITY_DESC: Comparator<SlotInfo> =
        Comparator.comparingInt<SlotInfo> { it.rarity }.reversed()

    val NAME: Comparator<SlotInfo> =
        Comparator.comparing { it.customName ?: it.itemId }

    val LETTER: Comparator<SlotInfo> =
        Comparator.comparing { extractFirstLetter(it.customName ?: it.itemId) }

    val ID: Comparator<SlotInfo> =
        Comparator.comparing { it.itemId }

    val AMOUNT_DESC: Comparator<SlotInfo> =
        Comparator.comparingInt<SlotInfo> { it.count }.reversed()

    fun computeColorGroup(hue: Float): Int = if (hue < 0f) -1 else (hue * COLOR_CHUNKS).roundToInt()

    fun getMaterial(itemId: String): String {
        val path = itemId.substringAfter(':', missingDelimiterValue = itemId)
        val normalized = normalizeMaterialPrefixes(path)
        return stripAffixes(normalized)
    }

    private fun normalizeMaterialPrefixes(path: String): String {
        val normalized = path.replace("golden_", "gold_").replace("wooden_", "wood_")
        return if (normalized == "lapis_lazuli") "lapis" else normalized
    }

    private fun stripAffixes(initialPath: String): String {
        var path = initialPath
        var changed = true
        while (changed) {
            val next = stripFirstMatchingAffix(path)
            changed = next != path
            path = next
        }
        return path
    }

    private fun stripFirstMatchingAffix(path: String): String {
        for (affix in MATERIAL_AFFIXES) {
            val stripped = tryStripSingleAffix(path, affix)
            if (stripped != path) return stripped
        }
        return path
    }

    private fun tryStripSingleAffix(path: String, affix: String): String {
        if (path.startsWith(affix) && affix.endsWith('_')) {
            return path.substring(affix.length)
        }
        if (path.endsWith(affix) && affix.startsWith('_')) {
            return path.substring(0, path.length - affix.length)
        }
        return path
    }

    private fun extractFirstLetter(name: String): String {
        val effective = name.substringAfter(':')
        return effective.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    }
}
