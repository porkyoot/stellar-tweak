package com.stellar.tweak.mixin

import com.stellar.tweak.handler.StellarTweakSortHandler
import com.stellar.tweak.input.StellarTweakInputBridge
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Mixin intercepting key presses within container screens to trigger inventory sorting.
 */
@Mixin(AbstractContainerScreen::class)
abstract class AbstractContainerScreenMixin {
    @Shadow
    protected var hoveredSlot: Slot? = null

    @Suppress("UnusedPrivateMember", "UnusedParameter")
    @Inject(method = ["keyPressed"], at = [At("HEAD")], cancellable = true)
    private fun stellarTweakOnKeyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
        cir: CallbackInfoReturnable<Boolean>,
    ) {
        if (StellarTweakInputBridge.isSortKey(keyCode, scanCode)) {
            val screen = this as Any as? AbstractContainerScreen<*> ?: return
            StellarTweakSortHandler.handleSort(Minecraft.getInstance(), hoveredSlot, screen)
            cir.setReturnValue(true)
        }
    }
}
