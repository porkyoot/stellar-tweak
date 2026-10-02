package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SortComparatorSpec : FunSpec({

    test("MOD comparator orders by namespace") {
        val stellarItem = SlotInfo(index = 0, itemId = "stellar:core_gem", count = 1)
        val minecraftItem = SlotInfo(index = 1, itemId = "minecraft:diamond", count = 1)
        val zModItem = SlotInfo(index = 2, itemId = "zmod:item", count = 1)

        val sorted = listOf(zModItem, stellarItem, minecraftItem)
            .sortedWith(SortComparator.MOD.comparator)

        sorted.map { it.itemId } shouldContainExactly listOf(
            "minecraft:diamond",
            "stellar:core_gem",
            "zmod:item",
        )
    }

    test("MATERIAL comparator normalizes and strips affixes") {
        SlotComparators.getMaterial("minecraft:deepslate_iron_ore") shouldBe "iron"
        SlotComparators.getMaterial("minecraft:raw_iron") shouldBe "iron"
        SlotComparators.getMaterial("minecraft:iron_ingot") shouldBe "iron"
        SlotComparators.getMaterial("minecraft:golden_sword") shouldBe "gold"
        SlotComparators.getMaterial("minecraft:wooden_pickaxe") shouldBe "wood"
        SlotComparators.getMaterial("minecraft:lapis_lazuli") shouldBe "lapis"
        SlotComparators.getMaterial("minecraft:block_of_copper") shouldBe "copper"
    }

    test("TAG comparator orders by earliest alphabetical tag") {
        val weapon = SlotInfo(index = 0, itemId = "item:a", count = 1, tags = listOf("c_tag", "a_tag"))
        val tool = SlotInfo(index = 1, itemId = "item:b", count = 1, tags = listOf("b_tag"))
        val noTag = SlotInfo(index = 2, itemId = "item:c", count = 1, tags = emptyList())

        val sorted = listOf(noTag, weapon, tool).sortedWith(SortComparator.TAG.comparator)
        sorted.map { it.itemId } shouldContainExactly listOf("item:a", "item:b", "item:c")
    }

    test("COLOR and COLOR_GROUPING chunk hue into discrete bands") {
        SlotComparators.computeColorGroup(-1.0f) shouldBe -1
        SlotComparators.computeColorGroup(0.0f) shouldBe 0
        SlotComparators.computeColorGroup(0.5f) shouldBe 6
        SlotComparators.computeColorGroup(1.0f) shouldBe 12

        val red = SlotInfo(index = 0, itemId = "red", count = 1, hue = 0.0f)
        val blue = SlotInfo(index = 1, itemId = "blue", count = 1, hue = 0.6f)
        val green = SlotInfo(index = 2, itemId = "green", count = 1, hue = 0.3f)

        val sorted = listOf(blue, red, green).sortedWith(SortComparator.COLOR.comparator)
        sorted.map { it.itemId } shouldContainExactly listOf("red", "green", "blue")
    }

    test("RARITY comparator orders descending") {
        val common = SlotInfo(index = 0, itemId = "common", count = 1, rarity = 0)
        val epic = SlotInfo(index = 1, itemId = "epic", count = 1, rarity = 3)
        val rare = SlotInfo(index = 2, itemId = "rare", count = 1, rarity = 2)

        val sorted = listOf(common, rare, epic).sortedWith(SortComparator.RARITY.comparator)
        sorted.map { it.itemId } shouldContainExactly listOf("epic", "rare", "common")
    }

    test("AMOUNT comparator orders count descending") {
        val low = SlotInfo(index = 0, itemId = "gem", count = 2)
        val max = SlotInfo(index = 1, itemId = "gem", count = 64)
        val mid = SlotInfo(index = 2, itemId = "gem", count = 16)

        val sorted = listOf(low, mid, max).sortedWith(SortComparator.AMOUNT.comparator)
        sorted.map { it.count } shouldContainExactly listOf(64, 16, 2)
    }

    test("buildHierarchy chains multi-tier priority comparators") {
        val itemA = SlotInfo(index = 0, itemId = "minecraft:iron_ingot", count = 10, rarity = 0)
        val itemB = SlotInfo(index = 1, itemId = "minecraft:iron_ingot", count = 64, rarity = 0)
        val itemC = SlotInfo(index = 2, itemId = "minecraft:gold_ingot", count = 5, rarity = 0)

        val hierarchy = SortComparator.buildHierarchy(
            listOf(SortComparator.MATERIAL, SortComparator.AMOUNT),
        )

        val sorted = listOf(itemA, itemC, itemB).sortedWith(hierarchy)
        // Material gold before iron, then iron 64 before iron 10
        sorted.map { "${it.itemId}:${it.count}" } shouldContainExactly listOf(
            "minecraft:gold_ingot:5",
            "minecraft:iron_ingot:64",
            "minecraft:iron_ingot:10",
        )
    }
})
