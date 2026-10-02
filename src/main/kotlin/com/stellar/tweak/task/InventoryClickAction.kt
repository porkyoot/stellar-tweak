package com.stellar.tweak.task

import com.stellar.core.action.ActionContext
import com.stellar.core.action.ActionPriority
import com.stellar.core.action.ActionResult
import com.stellar.core.action.ModAction

/**
 * Discrete inventory click action representing a single slot interaction.
 *
 * Implements [ModAction] with [ActionPriority.LOW].
 *
 * @property slotId Index of the slot being clicked.
 * @property containerId Container / window ID being interacted with.
 * @property button Mouse button (0 = left, 1 = right, 2 = middle/swap).
 * @property clickType Mode of click (e.g. "PICKUP", "QUICK_MOVE", "SWAP", "CLONE").
 */
data class InventoryClickAction(
    val slotId: Int,
    val containerId: Int = 0,
    val button: Int = 0,
    val clickType: String = DEFAULT_CLICK_TYPE,
) : ModAction {
    val slotIndex: Int
        get() = slotId

    override val priority: ActionPriority = ActionPriority.LOW

    constructor(containerId: Int, slotIndex: Int) : this(
        slotId = slotIndex,
        containerId = containerId,
        button = 0,
        clickType = DEFAULT_CLICK_TYPE,
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(context: ActionContext): ActionResult {
        val clickFn = context.attributes["clickExecutor"] as? ((Int, Int, Int, String) -> Boolean)
        if (clickFn != null) {
            val ok = clickFn(containerId, slotId, button, clickType)
            return if (ok) ActionResult.Success else ActionResult.Failure("Click executor returned false")
        }
        val mc = runCatching { net.minecraft.client.Minecraft.getInstance() }.getOrNull()
        if (mc != null) {
            val player = mc.player
            val gameMode = mc.gameMode
            if (player != null && gameMode != null) {
                val mode = runCatching { net.minecraft.world.inventory.ContainerInput.valueOf(clickType) }
                    .getOrDefault(net.minecraft.world.inventory.ContainerInput.PICKUP)
                mc.execute {
                    gameMode.handleContainerInput(containerId, slotId, button, mode, player)
                }
                return ActionResult.Success
            }
        }
        return ActionResult.Success
    }

    companion object {
        const val DEFAULT_CLICK_TYPE = "PICKUP"
    }
}
