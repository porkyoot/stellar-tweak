package com.stellar.tweak.handler

import com.stellar.core.config.ConfigManager
import com.stellar.tweak.StellarTweakMod
import com.stellar.tweak.config.StellarTweakConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

/**
 * Manages silent remote sorting interactions with container blocks and entities in the world.
 *
 * Suppresses the GUI popup during interaction, waits for container sync, sorts items,
 * and immediately closes the container.
 */
object SilentSortCoordinator {
    private const val SILENT_SORT_TIMEOUT_MS = 2000L

    @Volatile
    private var silentSortRequested = false

    @Volatile
    private var silentSortActive = false

    @Volatile
    private var silentSortPlanStarted = false

    @Volatile
    private var silentSortRequestTime = 0L

    @Volatile
    private var capturedSilentScreen: AbstractContainerScreen<*>? = null

    fun isSilentSortRequested(): Boolean = silentSortRequested

    fun isSilentSortActive(): Boolean = silentSortActive

    fun onSilentScreenOpening(screen: AbstractContainerScreen<*>) {
        silentSortRequested = false
        silentSortActive = true
        silentSortPlanStarted = false
        capturedSilentScreen = screen
    }

    fun onSilentContainerContent(containerId: Int) {
        val screen = capturedSilentScreen ?: return
        if (screen.menu.containerId != containerId) return
        if (silentSortPlanStarted) return
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val config = ConfigManager.get<StellarTweakConfig>(StellarTweakMod.MOD_ID, "main")
            ?: StellarTweakConfig()

        val target = StellarTweakSortHandler.resolveTargetContainer(player, screen, null)
        val containerSlots = screen.menu.slots.filter { it.container == target }
        if (containerSlots.isEmpty() || containerSlots.all { it.item.isEmpty }) {
            closeSilentContainer(player)
            return
        }

        silentSortPlanStarted = true
        StellarTweakSortHandler.handleScreenSort(player, screen, null, config)
    }

    fun closeSilentContainer(player: LocalPlayer) {
        if (!shouldCloseContainer()) return
        silentSortActive = false
        silentSortRequested = false
        silentSortPlanStarted = false
        capturedSilentScreen = null
        player.closeContainer()
    }

    private fun shouldCloseContainer(): Boolean =
        silentSortActive || silentSortRequested || capturedSilentScreen != null

    fun checkSilentSortTimeout(player: LocalPlayer) {
        val now = System.currentTimeMillis()
        if (silentSortRequested && now - silentSortRequestTime > SILENT_SORT_TIMEOUT_MS) {
            silentSortRequested = false
        }
        if (silentSortActive && now - silentSortRequestTime > SILENT_SORT_TIMEOUT_MS) {
            closeSilentContainer(player)
        }
    }

    fun handleWorldRemoteSort(client: Minecraft, config: StellarTweakConfig) {
        if (!config.allowRemoteSort.value()) return
        val hit = client.hitResult ?: return
        val player = client.player ?: return
        val level = client.level ?: return

        when (hit.type) {
            HitResult.Type.BLOCK -> handleBlockRemoteSort(client, player, level, hit as BlockHitResult)
            HitResult.Type.ENTITY -> handleEntityRemoteSort(client, player, hit as EntityHitResult)
            else -> Unit
        }
    }

    private fun handleBlockRemoteSort(client: Minecraft, player: LocalPlayer, level: Level, hit: BlockHitResult) {
        if (isSortableBlock(level, hit.blockPos)) {
            triggerSilentSortInteraction {
                client.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit)
                player.swing(InteractionHand.MAIN_HAND)
            }
        }
    }

    private fun handleEntityRemoteSort(client: Minecraft, player: LocalPlayer, hit: EntityHitResult) {
        if (isSortableEntity(hit.entity)) {
            triggerSilentSortInteraction {
                client.gameMode?.interact(player, hit.entity, hit, InteractionHand.MAIN_HAND)
                player.swing(InteractionHand.MAIN_HAND)
            }
        }
    }

    private inline fun triggerSilentSortInteraction(action: () -> Unit) {
        silentSortRequested = true
        silentSortRequestTime = System.currentTimeMillis()
        action()
    }

    fun isSortableBlock(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        val block = state.block
        if (block is EnderChestBlock) return true
        val be = level.getBlockEntity(pos)
        return be is Container || be is MenuProvider
    }

    fun isSortableEntity(entity: Entity): Boolean = entity is Container || entity is MenuProvider
}
