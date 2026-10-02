package com.stellar.tweak.mixin

import com.stellar.tweak.handler.SilentSortCoordinator
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Mixin intercepting container content sync packets to trigger sorting for silent remote container operations.
 */
@Mixin(ClientPacketListener::class)
abstract class ClientPacketListenerMixin {
    @Suppress("UnusedPrivateMember", "UnusedParameter")
    @Inject(method = ["handleContainerContent"], at = [At("TAIL")])
    private fun stellarTweakOnContainerContent(packet: ClientboundContainerSetContentPacket, ci: CallbackInfo) {
        if (SilentSortCoordinator.isSilentSortActive()) {
            SilentSortCoordinator.onSilentContainerContent(packet.containerId())
        }
    }
}
