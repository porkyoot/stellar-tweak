package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.InventorySnapshot
import com.stellar.tweak.task.SlotInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InventoryStateComputerSpec : FunSpec({

    fun createInventory(rows: Int = 3, cols: Int = 9): List<SlotInfo> {
        val slots = mutableListOf<SlotInfo>()
        var index = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                slots.add(
                    SlotInfo.empty(index = index).copy(
                        x = c * 18,
                        y = r * 18,
                    ),
                )
                index++
            }
        }
        return slots
    }

    test("computeTargetState consolidates partial stacks and sorts by priority hierarchy") {
        val baseSlots = createInventory(rows = 3, cols = 9).toMutableList()

        // Place two partial stacks of iron and one gold ingot
        baseSlots[0] = baseSlots[0].copy(itemId = "minecraft:iron_ingot", count = 30, maxStackSize = 64)
        baseSlots[1] = baseSlots[1].copy(itemId = "minecraft:iron_ingot", count = 40, maxStackSize = 64)
        baseSlots[2] = baseSlots[2].copy(itemId = "minecraft:gold_ingot", count = 10, maxStackSize = 64)

        val snapshot = InventorySnapshot(containerId = 1, slots = baseSlots)
        val config = SortConfiguration(
            layout = SortLayout.ROW,
            comparatorOrder = listOf(SortComparator.MATERIAL, SortComparator.AMOUNT),
        )

        val target = InventoryStateComputer.computeTargetState(snapshot, config)

        // Gold comes before iron alphabetically in MATERIAL
        // Group 1: gold_ingot (10) placed in Row 0 (slot 0)
        target.slotTargets[0]?.itemId shouldBe "minecraft:gold_ingot"
        target.slotTargets[0]?.count shouldBe 10

        // Iron ingots are consolidated: 30 + 40 -> 64 and 6
        // With 2 groups (gold, iron) in 3 rows, Row 1 is a padding row,
        // and Group 2 (iron) is placed in Row 2 (slots 18 and 19)
        target.slotTargets[9]?.isEmpty shouldBe true

        target.slotTargets[18]?.itemId shouldBe "minecraft:iron_ingot"
        target.slotTargets[18]?.count shouldBe 64

        target.slotTargets[19]?.itemId shouldBe "minecraft:iron_ingot"
        target.slotTargets[19]?.count shouldBe 6
    }

    test("locked slots are strictly preserved and never mutated or sorted") {
        val baseSlots = createInventory(rows = 3, cols = 9).toMutableList()

        // Lock slot 0 with a diamond sword
        val lockedSword = baseSlots[0].copy(
            itemId = "minecraft:diamond_sword",
            count = 1,
            isLocked = true,
        )
        baseSlots[0] = lockedSword

        // Unlocked slots
        baseSlots[1] = baseSlots[1].copy(itemId = "minecraft:iron_ingot", count = 64)
        baseSlots[2] = baseSlots[2].copy(itemId = "minecraft:cobblestone", count = 32)

        val snapshot = InventorySnapshot(containerId = 1, slots = baseSlots)
        val target = InventoryStateComputer.computeTargetState(snapshot)

        // Locked slot 0 remains completely identical
        target.slotTargets[0] shouldBe lockedSword
        target.slotTargets[0]?.isLocked shouldBe true

        // Unlocked items sort in remaining slots
        target.slotTargets[1]?.isEmpty shouldBe false
    }

    test("carried cursor item is absorbed into available container space") {
        val baseSlots = createInventory(rows = 3, cols = 9).toMutableList()
        baseSlots[0] = baseSlots[0].copy(itemId = "minecraft:diamond", count = 20, maxStackSize = 64)

        val carried = SlotInfo(index = -1, itemId = "minecraft:diamond", count = 10, maxStackSize = 64)
        val snapshot = InventorySnapshot(containerId = 1, slots = baseSlots, carried = carried)

        val target = InventoryStateComputer.computeTargetState(snapshot)

        // Carried 10 diamonds consolidated with container 20 diamonds -> 30 in container
        target.slotTargets[0]?.itemId shouldBe "minecraft:diamond"
        target.slotTargets[0]?.count shouldBe 30
        target.cursorTarget.isEmpty shouldBe true
    }

    test("ColumnLayout spaces distinct groups across columns with vertical stacking and padding") {
        val baseSlots = createInventory(rows = 3, cols = 9).toMutableList()
        baseSlots[0] = baseSlots[0].copy(itemId = "minecraft:diamond_sword", count = 1)
        baseSlots[1] = baseSlots[1].copy(itemId = "minecraft:diamond_pickaxe", count = 1)
        baseSlots[2] = baseSlots[2].copy(itemId = "minecraft:iron_ingot", count = 10)
        baseSlots[3] = baseSlots[3].copy(itemId = "minecraft:gold_ingot", count = 5)
        baseSlots[4] = baseSlots[4].copy(itemId = "minecraft:cobblestone", count = 64)
        baseSlots[5] = baseSlots[5].copy(itemId = "minecraft:oak_planks", count = 32)

        val snapshot = InventorySnapshot(containerId = 1, slots = baseSlots)
        val config = SortConfiguration(layout = SortLayout.COLUMN)
        val target = InventoryStateComputer.computeTargetState(snapshot, config)

        // Col 0: cobblestone
        target.slotTargets[0]?.itemId shouldBe "minecraft:cobblestone"

        // Col 1: padding spacer (empty)
        target.slotTargets[1]?.isEmpty shouldBe true

        // Col 2: diamond tools stack vertically down rows 0 and 1
        target.slotTargets[2]?.itemId shouldBe "minecraft:diamond_pickaxe"
        target.slotTargets[11]?.itemId shouldBe "minecraft:diamond_sword"

        // Col 3: padding spacer (empty)
        target.slotTargets[3]?.isEmpty shouldBe true

        // Col 4: gold ingot
        target.slotTargets[4]?.itemId shouldBe "minecraft:gold_ingot"

        // Col 5: padding spacer (empty)
        target.slotTargets[5]?.isEmpty shouldBe true

        // Col 6: iron ingot
        target.slotTargets[6]?.itemId shouldBe "minecraft:iron_ingot"

        // Col 7: padding spacer (empty)
        target.slotTargets[7]?.isEmpty shouldBe true

        // Col 8: oak planks
        target.slotTargets[8]?.itemId shouldBe "minecraft:oak_planks"
    }
})
