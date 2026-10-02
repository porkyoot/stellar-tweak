package com.stellar.tweak.mixin

import com.stellar.tweak.handler.SilentSortCoordinator
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Mixin intercepting screen changes to silently handle remote container sorting without rendering the GUI.
 */
@Mixin(Gui::class)
abstract class GuiMixin {
    @Suppress("UnusedPrivateMember")
    @Inject(method = ["setScreen"], at = [At("HEAD")], cancellable = true)
    private fun stellarTweakOnSetScreen(screen: Screen?, ci: CallbackInfo) {
        if (screen is AbstractContainerScreen<*> && SilentSortCoordinator.isSilentSortRequested()) {
            SilentSortCoordinator.onSilentScreenOpening(screen)
            ci.cancel()
        }
    }
}
