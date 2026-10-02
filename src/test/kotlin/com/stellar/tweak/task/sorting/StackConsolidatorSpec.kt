package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class StackConsolidatorSpec : FunSpec({

    test("consolidate merges partial stacks of identical items up to maxStackSize") {
        val stack1 = SlotInfo(index = 0, itemId = "minecraft:iron_ingot", count = 30, maxStackSize = 64)
        val stack2 = SlotInfo(index = 1, itemId = "minecraft:iron_ingot", count = 40, maxStackSize = 64)
        val stack3 = SlotInfo(index = 2, itemId = "minecraft:gold_ingot", count = 10, maxStackSize = 64)

        val consolidated = StackConsolidator.consolidate(listOf(stack1, stack2, stack3))

        // stack1 receives 34 to reach 64, stack2 retains remaining 6, gold untouched
        consolidated[0].count shouldBe 64
        consolidated[0].itemId shouldBe "minecraft:iron_ingot"

        consolidated[1].count shouldBe 6
        consolidated[1].itemId shouldBe "minecraft:iron_ingot"

        consolidated[2].count shouldBe 10
        consolidated[2].itemId shouldBe "minecraft:gold_ingot"
    }

    test("consolidate completely empties fully absorbed stacks") {
        val stack1 = SlotInfo(index = 0, itemId = "minecraft:diamond", count = 20, maxStackSize = 64)
        val stack2 = SlotInfo(index = 1, itemId = "minecraft:diamond", count = 15, maxStackSize = 64)

        val consolidated = StackConsolidator.consolidate(listOf(stack1, stack2))

        consolidated[0].count shouldBe 35
        consolidated[0].itemId shouldBe "minecraft:diamond"

        consolidated[1].isEmpty shouldBe true
    }

    test("total item quantities are mathematically preserved") {
        val initial = listOf(
            SlotInfo(index = 0, itemId = "minecraft:arrow", count = 32, maxStackSize = 64),
            SlotInfo(index = 1, itemId = "minecraft:arrow", count = 32, maxStackSize = 64),
            SlotInfo(index = 2, itemId = "minecraft:arrow", count = 10, maxStackSize = 64),
            SlotInfo(index = 3, itemId = "minecraft:dirt", count = 50, maxStackSize = 64),
            SlotInfo(index = 4, itemId = "minecraft:dirt", count = 20, maxStackSize = 64),
        )

        val initialArrows = initial.filter { it.itemId == "minecraft:arrow" }.sumOf { it.count }
        val initialDirt = initial.filter { it.itemId == "minecraft:dirt" }.sumOf { it.count }

        val consolidated = StackConsolidator.consolidate(initial)

        val finalArrows = consolidated.filter { it.itemId == "minecraft:arrow" }.sumOf { it.count }
        val finalDirt = consolidated.filter { it.itemId == "minecraft:dirt" }.sumOf { it.count }

        finalArrows shouldBe initialArrows
        finalDirt shouldBe initialDirt

        val nonZeroCounts = consolidated.filterNot { it.isEmpty }.map { "${it.itemId}:${it.count}" }
        nonZeroCounts shouldContainExactly listOf(
            "minecraft:arrow:64",
            "minecraft:arrow:10",
            "minecraft:dirt:64",
            "minecraft:dirt:6",
        )
    }
})
