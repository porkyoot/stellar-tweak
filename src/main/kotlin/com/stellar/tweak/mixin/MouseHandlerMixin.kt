package com.stellar.tweak.mixin

import com.stellar.core.input.EventResult
import com.stellar.tweak.input.StellarTweakInputBridge
import net.minecraft.client.MouseHandler
import net.minecraft.client.input.MouseButtonInfo
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Mixin intercepting mouse buttons and scroll wheel events.
 */
@Suppress("UnusedPrivateMember", "UnusedParameter")
@Mixin(MouseHandler::class)
class MouseHandlerMixin {
    @Inject(method = ["onButton"], at = [At("HEAD")], cancellable = true)
    private fun stellarOnMouseButton(window: Long, buttonInfo: MouseButtonInfo, action: Int, ci: CallbackInfo) {
        val result = StellarTweakInputBridge.onMouseButton(buttonInfo.button(), action, buttonInfo.modifiers())
        if (result == EventResult.CONSUMED) {
            ci.cancel()
        }
    }

    @Inject(method = ["onScroll"], at = [At("HEAD")], cancellable = true)
    private fun stellarOnMouseScroll(window: Long, horizontal: Double, vertical: Double, ci: CallbackInfo) {
        val result = StellarTweakInputBridge.onMouseScroll(horizontal, vertical)
        if (result == EventResult.CONSUMED) {
            ci.cancel()
        }
    }
}
