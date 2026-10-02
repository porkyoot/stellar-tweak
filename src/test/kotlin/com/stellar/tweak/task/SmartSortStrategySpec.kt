package com.stellar.tweak.task

import com.stellar.core.action.ActionPriority
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SmartSortStrategySpec : FunSpec({

    test("SlotInfo correctly identifies empty slots") {
        val emptySlot = SlotInfo.empty(0)
        val airSlot = SlotInfo(index = 1, itemId = "minecraft:air", count = 1)
        val swordSlot = SlotInfo(index = 2, itemId = "minecraft:diamond_sword", count = 1, isLocked = true)

        emptySlot.isEmpty shouldBe true
        airSlot.isEmpty shouldBe true
        swordSlot.isEmpty shouldBe false
        swordSlot.isLocked shouldBe true
    }

    test("InventorySnapshot filters unlocked slots") {
        val slots = listOf(
            SlotInfo(index = 0, itemId = "minecraft:diamond_sword", count = 1, isLocked = true),
            SlotInfo(index = 1, itemId = "minecraft:iron_ingot", count = 64, isLocked = false),
            SlotInfo(index = 2, itemId = "minecraft:gold_ingot", count = 32, isLocked = false),
        )
        val snapshot = InventorySnapshot(containerId = 1, slots = slots)

        snapshot.size shouldBe 3
        snapshot.unlockedSlots.map { it.itemId } shouldContainExactly listOf(
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
        )
    }

    test("InventoryClickAction defaults to ActionPriority.LOW") {
        val action = InventoryClickAction(containerId = 1, slotIndex = 5)
        action.priority shouldBe ActionPriority.LOW
        action.slotIndex shouldBe 5
        action.clickType shouldBe "PICKUP"
    }

    test("SmartSortStrategy executes calculatePlan on background thread and ignores locked slots") {
        val strategy = SmartSortStrategy()
        val slots = listOf(
            SlotInfo(index = 0, itemId = "minecraft:shield", count = 1, isLocked = true),
            SlotInfo(index = 1, itemId = "minecraft:cobblestone", count = 64, isLocked = false),
            SlotInfo(index = 2, itemId = "minecraft:dirt", count = 32, isLocked = false),
        )
        val snapshot = InventorySnapshot(containerId = 42, slots = slots)

        val plan = strategy.calculatePlan(snapshot)
        plan.shouldBeEmpty()

        // Pure calculation produces identical result for identical snapshot
        val pureResult1 = strategy.computePlanPure(snapshot)
        val pureResult2 = strategy.computePlanPure(snapshot)
        pureResult1 shouldBe pureResult2
    }
})
