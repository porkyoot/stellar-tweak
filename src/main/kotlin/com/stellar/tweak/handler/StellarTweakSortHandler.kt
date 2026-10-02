package com.stellar.tweak.handler

import com.stellar.core.action.ActionDispatcher
import com.stellar.core.config.ConfigManager
import com.stellar.core.ratelimit.TokenBucket
import com.stellar.tweak.StellarTweakMod
import com.stellar.tweak.config.StellarTweakConfig
import com.stellar.tweak.task.InventorySnapshot
import com.stellar.tweak.task.SlotInfo
import com.stellar.tweak.task.SmartSortStrategy
import com.stellar.tweak.task.TaskEngine
import com.stellar.tweak.task.TaskIntent
import com.stellar.tweak.task.sorting.SortComparator
import com.stellar.tweak.task.sorting.SortConfiguration
import com.stellar.tweak.task.sorting.SortLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/**
 * Controller orchestrating inventory sorting requests triggered via keybinds or interactions.
 */
object StellarTweakSortHandler {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val dispatcher: ActionDispatcher = ActionDispatcher(
        tokenBucket = TokenBucket.forRequestsPerSecond(
            requestsPerSecond = StellarTweakConfig.DEFAULT_CPS.toDouble(),
            burstSize = StellarTweakConfig.DEFAULT_BURST_CAPACITY.toDouble(),
        ),
        maxActionsPerTick = StellarTweakConfig.MAX_CPS,
    )

    val taskEngine: TaskEngine = TaskEngine(dispatcher, scope)

    /**
     * Ticks the action execution loop at the client TPS rate.
     */
    fun onTick(client: Minecraft) {
        if (client.player != null) {
            scope.launch {
                dispatcher.tick()
            }
        }
    }

    /**
     * Handles a sort request triggered from GUI or hotkey.
     */
    fun handleSort(client: Minecraft, hoveredSlot: Slot?, screen: AbstractContainerScreen<*>? = null) {
        val player = client.player ?: return
        val config = ConfigManager.get<StellarTweakConfig>(StellarTweakMod.MOD_ID, "main")
            ?: StellarTweakConfig()

        updateRateLimiter(config)

        if (screen != null) {
            handleScreenSort(player, screen, hoveredSlot, config)
        } else {
            handleWorldRemoteSort(client, config)
        }
    }

    private fun handleScreenSort(
        player: Player,
        screen: AbstractContainerScreen<*>,
        hoveredSlot: Slot?,
        config: StellarTweakConfig,
    ) {
        val targetContainer = resolveTargetContainer(player, screen, hoveredSlot)
        val containerSlots = screen.menu.slots.filter { it.container == targetContainer }
        if (containerSlots.isEmpty() || containerSlots.all { it.item.isEmpty }) return

        val snapshot = buildSnapshot(player, screen, containerSlots)
        val sortConfig = buildSortConfig(config)

        scope.launch {
            val strategy = SmartSortStrategy(config = sortConfig)
            val actions = strategy.calculatePlan(snapshot)
            if (actions.isNotEmpty()) {
                taskEngine.processIntent(TaskIntent.Start)
                taskEngine.processIntent(TaskIntent.SnapshotComplete)
                taskEngine.processIntent(TaskIntent.PlanCalculated(actions))

                if (config.clickSpeedCps.value() <= 0) {
                    dispatcher.tick()
                }
            }
        }
    }

    private fun resolveTargetContainer(
        player: Player,
        screen: AbstractContainerScreen<*>,
        hoveredSlot: Slot?,
    ): Container {
        if (hoveredSlot?.container != null) {
            return hoveredSlot.container
        }
        val nonPlayerSlot = screen.menu.slots.firstOrNull { it.container != player.inventory }
        return nonPlayerSlot?.container ?: player.inventory
    }

    private fun buildSnapshot(
        player: Player,
        screen: AbstractContainerScreen<*>,
        containerSlots: List<Slot>,
    ): InventorySnapshot {
        val slotInfos = containerSlots.map { slot ->
            val stack = slot.item
            val itemId = if (stack.isEmpty) "" else BuiltInRegistries.ITEM.getKey(stack.item).toString()
            SlotInfo(
                index = slot.index,
                itemId = itemId,
                count = stack.count,
                isLocked = false,
                x = slot.x,
                y = slot.y,
            )
        }

        val carried = player.containerMenu.carried
        val cursorSlot = if (!carried.isEmpty) {
            SlotInfo(
                index = -1,
                itemId = BuiltInRegistries.ITEM.getKey(carried.item).toString(),
                count = carried.count,
            )
        } else {
            null
        }

        return InventorySnapshot(
            containerId = screen.menu.containerId,
            slots = slotInfos,
            carried = cursorSlot,
        )
    }

    private fun handleWorldRemoteSort(client: Minecraft, config: StellarTweakConfig) {
        if (!config.allowRemoteSort.value()) return
        val hit = client.hitResult ?: return
        val player = client.player ?: return

        if (hit.type == HitResult.Type.BLOCK && hit is BlockHitResult) {
            client.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit)
            player.swing(InteractionHand.MAIN_HAND)
        }
    }

    private fun updateRateLimiter(config: StellarTweakConfig) {
        val cps = config.clickSpeedCps.value()
        val burst = config.burstCapacity.value()

        if (cps <= 0) {
            dispatcher.tokenBucket.reconfigure(Double.MAX_VALUE, Double.MAX_VALUE)
        } else {
            dispatcher.tokenBucket.reconfigure(burst.toDouble(), cps.toDouble())
        }
    }

    fun buildSortConfig(config: StellarTweakConfig): SortConfiguration {
        val layout = runCatching { SortLayout.valueOf(config.sortLayout.value().uppercase()) }
            .getOrDefault(SortLayout.ROW)
        val comparators = parseComparators(config.sortComparatorOrder.value())
        return SortConfiguration(layout = layout, comparatorOrder = comparators)
    }

    fun parseComparators(orderStr: String): List<SortComparator> {
        val names = orderStr.split(",").map { it.trim().uppercase() }
        val list = names.mapNotNull { name ->
            runCatching { SortComparator.valueOf(name) }.getOrNull()
        }
        return if (list.isNotEmpty()) list else SortComparator.DEFAULT_ORDER
    }
}
