package com.stellar.tweak.mixin

import com.stellar.core.input.EventResult
import com.stellar.tweak.input.StellarTweakInputBridge
import net.minecraft.client.KeyboardHandler
import net.minecraft.client.input.KeyEvent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Mixin intercepting raw keyboard events to dispatch through Stellar InputManager.
 */
@Suppress("UnusedPrivateMember", "UnusedParameter")
@Mixin(KeyboardHandler::class)
class KeyboardHandlerMixin {
    @Inject(method = ["keyPress"], at = [At("HEAD")], cancellable = true)
    private fun stellarOnKeyPress(window: Long, action: Int, keyEvent: KeyEvent, ci: CallbackInfo) {
        val result = StellarTweakInputBridge.onKey(keyEvent.key(), action, keyEvent.modifiers())
        if (result == EventResult.CONSUMED) {
            ci.cancel()
        }
    }
}
