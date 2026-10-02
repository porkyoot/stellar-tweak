package com.stellar.tweak.config

import org.quiltmc.config.api.ReflectiveConfig
import org.quiltmc.config.api.annotations.Comment
import org.quiltmc.config.api.values.TrackedValue

/**
 * Quilt ReflectiveConfig schema for Stellar Tweak inventory sorting and client safety.
 */
class StellarTweakConfig : ReflectiveConfig() {
    @Comment("Sort layout algorithm: ROW (horizontal) or COLUMN (vertical)")
    val sortLayout: TrackedValue<String> = value("ROW")

    @Comment("Comma-separated active comparator priority list")
    val sortComparatorOrder: TrackedValue<String> = value(
        "CATEGORY,TAG,MATERIAL,MOD,COLOR,RARITY,NAME,ID,AMOUNT",
    )

    @Comment("Clicks per second (CPS) rate limit for sorting clicks (0 = unlimited)")
    val clickSpeedCps: TrackedValue<Int> = value(DEFAULT_CPS)

    @Comment("Token bucket burst capacity for initial clicks batch")
    val burstCapacity: TrackedValue<Int> = value(DEFAULT_BURST_CAPACITY)

    @Comment("Enable remote sorting of containers targeted in the crosshairs")
    val allowRemoteSort: TrackedValue<Boolean> = value(true)

    companion object {
        const val DEFAULT_CPS: Int = 10
        const val MAX_CPS: Int = 20
        const val DEFAULT_BURST_CAPACITY: Int = 5
        const val MAX_BURST_CAPACITY: Int = 50
    }
}
