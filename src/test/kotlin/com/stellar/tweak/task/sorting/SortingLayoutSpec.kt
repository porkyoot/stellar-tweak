package com.stellar.tweak.task.sorting

import com.stellar.tweak.task.SlotInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SortingLayoutSpec : FunSpec({

    fun createStandardGrid(rows: Int = 3, cols: Int = 9): List<SlotInfo> {
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

    test("RowLayout spaces items with line breaks and padding between distinct material groups") {
        val groupingComps = listOf(SlotComparators.MATERIAL)
        val layout = RowLayout(groupingComps)
        val availableSlots = createStandardGrid(rows = 3, cols = 9) // 27 slots

        val ironSword = SlotInfo(index = 0, itemId = "minecraft:iron_sword", count = 1)
        val ironAxe = SlotInfo(index = 1, itemId = "minecraft:iron_axe", count = 1)
        val goldPick = SlotInfo(index = 2, itemId = "minecraft:gold_pickaxe", count = 1)

        val placed = layout.layout(listOf(goldPick, ironSword, ironAxe), availableSlots)

        placed.size shouldBe 27

        // Group 1: gold (material gold) placed at slot 0 (Row 0)
        placed[0].itemId shouldBe "minecraft:gold_pickaxe"

        // With 3 rows (27 slots), computeMaxPadding leaves 1 blank padding row (Row 1: indices 9..17)
        placed[9].isEmpty shouldBe true

        // Group 2: iron placed at Row 2 (index 18)
        placed[18].itemId shouldBe "minecraft:iron_sword"
        placed[19].itemId shouldBe "minecraft:iron_axe"
    }

    test("RowLayout with tight capacity places on immediate next row without padding") {
        val groupingComps = listOf(SlotComparators.MATERIAL)
        val layout = RowLayout(groupingComps)
        val availableSlots = createStandardGrid(rows = 2, cols = 9) // 18 slots (padding = 0)

        val ironSword = SlotInfo(index = 0, itemId = "minecraft:iron_sword", count = 1)
        val goldPick = SlotInfo(index = 1, itemId = "minecraft:gold_pickaxe", count = 1)

        val placed = layout.layout(listOf(goldPick, ironSword), availableSlots)

        placed.size shouldBe 18
        placed[0].itemId shouldBe "minecraft:gold_pickaxe"
        // Immediate next row (index 9)
        placed[9].itemId shouldBe "minecraft:iron_sword"
    }

    test("ColumnLayout spaces items with column breaks and padding between distinct material groups") {
        val groupingComps = listOf(SlotComparators.MATERIAL)
        val layout = ColumnLayout(groupingComps)
        val availableSlots = createStandardGrid(rows = 3, cols = 2) // 2 columns (padding = 0)

        val ironSword = SlotInfo(index = 0, itemId = "minecraft:iron_sword", count = 1)
        val ironAxe = SlotInfo(index = 1, itemId = "minecraft:iron_axe", count = 1)
        val goldPick = SlotInfo(index = 2, itemId = "minecraft:gold_pickaxe", count = 1)

        val placed = layout.layout(listOf(goldPick, ironSword, ironAxe), availableSlots)

        placed.size shouldBe 6

        // In 3x2 grid:
        // Column 0: indices 0 (y=0), 2 (y=18), 4 (y=36)
        // Column 1: indices 1 (y=0), 3 (y=18), 5 (y=36)
        // Group 1 (goldPick) -> Column 0 top -> index 0
        placed[0].itemId shouldBe "minecraft:gold_pickaxe"

        // Column break advances to Column 1 top -> index 1
        placed[1].itemId shouldBe "minecraft:iron_sword"
        // Next in Column 1 -> index 3
        placed[3].itemId shouldBe "minecraft:iron_axe"
    }
})
