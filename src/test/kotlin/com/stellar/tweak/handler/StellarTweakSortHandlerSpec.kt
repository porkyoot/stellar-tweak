package com.stellar.tweak.handler

import com.stellar.core.action.ActionContext
import com.stellar.core.action.ActionPriority
import com.stellar.core.action.ActionResult
import com.stellar.core.action.ModAction
import com.stellar.core.config.ConfigManager
import com.stellar.tweak.config.StellarTweakConfig
import com.stellar.tweak.task.sorting.SortComparator
import com.stellar.tweak.task.sorting.SortLayout
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class StellarTweakSortHandlerSpec : FunSpec({
    test("parseComparators parses comma-separated string correctly") {
        val parsed = StellarTweakSortHandler.parseComparators("category, id, amount")
        parsed shouldContainExactly listOf(
            SortComparator.CATEGORY,
            SortComparator.ID,
            SortComparator.AMOUNT,
        )
    }

    test("parseComparators falls back to DEFAULT_ORDER on empty/invalid string") {
        val parsed = StellarTweakSortHandler.parseComparators("invalid_name, non_existent")
        parsed shouldContainExactly SortComparator.DEFAULT_ORDER
    }

    test("buildSortConfig correctly converts config values") {
        val config = ConfigManager.register(
            "stellar_tweak",
            "sort_spec_${System.nanoTime()}",
            StellarTweakConfig::class.java,
        )
        config.sortLayout.setValue("COLUMN", false)
        config.sortComparatorOrder.setValue("NAME, RARITY", false)

        val sortConfig = StellarTweakSortHandler.buildSortConfig(config)
        sortConfig.layout shouldBe SortLayout.COLUMN
        sortConfig.comparatorOrder shouldContainExactly listOf(
            SortComparator.NAME,
            SortComparator.RARITY,
        )
    }

    test("instant mode reconfigures dispatcher to unlimited and drains actions immediately") {
        val config = ConfigManager.register(
            "stellar_tweak",
            "sort_spec_${System.nanoTime()}",
            StellarTweakConfig::class.java,
        )
        config.clickSpeedCps.setValue(0, false)

        StellarTweakSortHandler.dispatcher.tokenBucket.reconfigure(Double.MAX_VALUE, Double.MAX_VALUE)
        StellarTweakSortHandler.dispatcher.tokenBucket.isUnlimited shouldBe true

        val executed = mutableListOf<Int>()
        repeat(50) { index ->
            StellarTweakSortHandler.dispatcher.enqueue(object : ModAction {
                override val priority = ActionPriority.LOW
                override suspend fun execute(context: ActionContext): ActionResult {
                    executed.add(index)
                    return ActionResult.Success
                }
            })
        }

        StellarTweakSortHandler.dispatcher.tick()
        executed.size shouldBe 50
        StellarTweakSortHandler.dispatcher.isEmpty shouldBe true
    }
})
