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
import com.stellar.tweak.task.TaskState
import com.stellar.tweak.task.sorting.SortComparator
import com.stellar.tweak.task.sorting.SortConfiguration
import com.stellar.tweak.task.sorting.SortLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

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

    init {
        scope.launch {
            taskEngine.state.collect { state ->
                if (state is TaskState.Idle && SilentSortCoordinator.isSilentSortActive()) {
                    val client = Minecraft.getInstance()
                    client.execute {
                        client.player?.let { SilentSortCoordinator.closeSilentContainer(it) }
                    }
                }
            }
        }
    }

    /**
     * Ticks the action execution loop at the client TPS rate and monitors silent sort timeouts.
     */
    fun onTick(client: Minecraft) {
        val player = client.player
        if (player != null) {
            scope.launch {
                dispatcher.tick()
            }
            SilentSortCoordinator.checkSilentSortTimeout(player)
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
            SilentSortCoordinator.handleWorldRemoteSort(client, config)
        }
    }

    fun handleScreenSort(
        player: LocalPlayer,
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
            } else if (SilentSortCoordinator.isSilentSortActive()) {
                val client = Minecraft.getInstance()
                client.execute {
                    client.player?.let { SilentSortCoordinator.closeSilentContainer(it) }
                }
            }
        }
    }

    fun resolveTargetContainer(
        player: LocalPlayer,
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
        player: LocalPlayer,
        screen: AbstractContainerScreen<*>,
        containerSlots: List<Slot>,
    ): InventorySnapshot {
        val slotInfos = containerSlots.map { slot ->
            val stack = slot.item
            if (stack.isEmpty) {
                SlotInfo.empty(slot.index).copy(x = slot.x, y = slot.y)
            } else {
                createSlotInfo(slot.index, stack, x = slot.x, y = slot.y)
            }
        }

        val carried = player.containerMenu.carried
        val cursorSlot = if (!carried.isEmpty) {
            createSlotInfo(-1, carried)
        } else {
            null
        }

        return InventorySnapshot(
            containerId = screen.menu.containerId,
            slots = slotInfos,
            carried = cursorSlot,
        )
    }

    private fun createSlotInfo(
        index: Int,
        stack: ItemStack,
        isLocked: Boolean = false,
        x: Int = 0,
        y: Int = 0,
    ): SlotInfo {
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        val tags = stack.tags().map { it.location().toString() }.toList()
        return SlotInfo(
            index = index,
            itemId = itemId,
            count = stack.count,
            isLocked = isLocked,
            maxStackSize = stack.maxStackSize,
            customName = stack.hoverName.string,
            rarity = stack.rarity.ordinal,
            tags = tags,
            creativeOrder = CreativeOrderRegistry.getCreativeOrder(stack.item),
            x = x,
            y = y,
        )
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
