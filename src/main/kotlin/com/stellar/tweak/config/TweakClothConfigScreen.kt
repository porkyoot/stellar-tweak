package com.stellar.tweak.config

import com.stellar.core.config.ConfigManager
import com.stellar.tweak.StellarTweakMod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Factory for creating the Cloth Config GUI screen bound to StellarTweakConfig.
 */
object TweakClothConfigScreen {
    fun create(parent: Screen?): Screen {
        val config = ConfigManager.get<StellarTweakConfig>(StellarTweakMod.MOD_ID, "main")
            ?: ConfigManager.register(StellarTweakMod.MOD_ID, "main", StellarTweakConfig::class.java)

        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Stellar Tweak Config"))

        builder.setSavingRunnable {
            config.save()
        }

        val entryBuilder = builder.entryBuilder()
        buildSortingCategory(builder, entryBuilder, config)
        buildSafetyCategory(builder, entryBuilder, config)

        return builder.build()
    }

    private fun buildSortingCategory(builder: ConfigBuilder, entries: ConfigEntryBuilder, config: StellarTweakConfig) {
        val category = builder.getOrCreateCategory(Component.literal("Sorting"))
        val layoutOptions = arrayOf("ROW", "COLUMN")
        val currentLayout = config.sortLayout.value().uppercase().let {
            if (it in layoutOptions) it else "ROW"
        }

        val sortLayoutEntry = entries
            .startSelector(Component.literal("Sort Layout"), layoutOptions, currentLayout)
            .setDefaultValue("ROW")
            .setTooltip(Component.literal("Order items horizontally (ROW) or vertically (COLUMN)."))
            .setSaveConsumer { value -> config.sortLayout.setValue(value, true) }
            .build()

        val comparatorOrderEntry = entries
            .startStrField(Component.literal("Active Comparators (Order Matters)"), config.sortComparatorOrder.value())
            .setDefaultValue("CATEGORY,TAG,MATERIAL,MOD,COLOR,RARITY,NAME,ID,AMOUNT")
            .setTooltip(
                Component.literal(
                    "Primary criteria first. Available: CATEGORY, TAG, MATERIAL, MOD, COLOR, RARITY, NAME, ID, AMOUNT",
                ),
            )
            .setSaveConsumer { value -> config.sortComparatorOrder.setValue(value.trim(), true) }
            .build()

        val remoteSortEntry = entries
            .startBooleanToggle(Component.literal("Remote Container Sorting"), config.allowRemoteSort.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Allows sorting target containers in the crosshairs without opening GUI"))
            .setSaveConsumer { value -> config.allowRemoteSort.setValue(value, true) }
            .build()

        category.addEntry(sortLayoutEntry)
        category.addEntry(comparatorOrderEntry)
        category.addEntry(remoteSortEntry)
    }

    private fun buildSafetyCategory(builder: ConfigBuilder, entries: ConfigEntryBuilder, config: StellarTweakConfig) {
        val category = builder.getOrCreateCategory(Component.literal("Safety & Rate Limiting"))

        val cpsEntry = entries
            .startIntSlider(
                Component.literal("Click Speed (CPS)"),
                config.clickSpeedCps.value(),
                0,
                StellarTweakConfig.MAX_CPS,
            )
            .setDefaultValue(StellarTweakConfig.DEFAULT_CPS)
            .setTextGetter { value ->
                if (value == 0) Component.literal("Unlimited (Instant)") else Component.literal("$value CPS")
            }
            .setTooltip(
                Component.literal(
                    "Clicks per second for sorting operations. Higher = Faster. 0 = Unlimited (Instant).",
                ),
            )
            .setSaveConsumer { value -> config.clickSpeedCps.setValue(value, true) }
            .build()

        val burstEntry = entries
            .startIntSlider(
                Component.literal("Burst Window (Capacity)"),
                config.burstCapacity.value(),
                1,
                StellarTweakConfig.MAX_BURST_CAPACITY,
            )
            .setDefaultValue(StellarTweakConfig.DEFAULT_BURST_CAPACITY)
            .setTooltip(
                Component.literal(
                    "Token bucket capacity allowing initial micro-bursts of clicks before throttling.",
                ),
            )
            .setSaveConsumer { value -> config.burstCapacity.setValue(value, true) }
            .build()

        category.addEntry(cpsEntry)
        category.addEntry(burstEntry)
    }
}
