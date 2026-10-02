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
        // Because AMOUNT is in the comparator order, 64 and 6 form distinct amount groups:
        // Group 2: iron_ingot (64) placed in Row 1 (slot 9)
        target.slotTargets[9]?.itemId shouldBe "minecraft:iron_ingot"
        target.slotTargets[9]?.count shouldBe 64

        // Group 3: iron_ingot (6) placed in Row 2 (slot 18)
        target.slotTargets[18]?.itemId shouldBe "minecraft:iron_ingot"
        target.slotTargets[18]?.count shouldBe 6
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
})
