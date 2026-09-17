package com.stellar.tweak.config

import com.stellar.core.input.Key
import org.quiltmc.config.api.ReflectiveConfig
import org.quiltmc.config.api.annotations.Comment
import org.quiltmc.config.api.values.TrackedValue

/**
 * Quilt ReflectiveConfig schema for Stellar Tweak.
 */
class StellarTweakConfig : ReflectiveConfig() {
    @Comment("Enable or disable the in-game test toasts")
    val enableToast: TrackedValue<Boolean> = value(true)

    @Comment("GLFW key code to trigger test toast (default: G = 71)")
    val toastKey: TrackedValue<Int> = value(Key.KEY_G)

    @Comment("Title displayed on the toast notification")
    val toastTitle: TrackedValue<String> = value("Stellar Tweak")

    @Comment("Message body displayed on the toast notification")
    val toastMessage: TrackedValue<String> = value("Config & Input Test Triggered!")
}
