package com.stellar.tweak.mixin

import com.stellar.tweak.handler.StellarTweakSortHandler
import com.stellar.tweak.input.StellarTweakInputBridge
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Mixin hooking Minecraft client tick loop to process ActionDispatcher ticks and hotkeys.
 */
@Mixin(Minecraft::class)
abstract class MinecraftTickMixin {
    @Suppress("UnusedPrivateMember", "UnusedParameter")
    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun stellarTweakOnTick(ci: CallbackInfo) {
        val mc = this as Any as? Minecraft ?: return
        if (shouldTriggerSort(mc) && StellarTweakInputBridge.sortKeyMapping.consumeClick()) {
            StellarTweakSortHandler.handleSort(mc, null, null)
        }
        StellarTweakSortHandler.onTick(mc)
    }

    private fun shouldTriggerSort(mc: Minecraft): Boolean = mc.player != null && mc.gui.screen() == null
}
