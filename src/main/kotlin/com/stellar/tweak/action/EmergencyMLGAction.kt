package com.stellar.tweak.action

import com.stellar.core.action.ActionContext
import com.stellar.core.action.ActionPriority
import com.stellar.core.action.ActionResult
import com.stellar.core.action.ModAction

/**
 * High-urgency action performing an emergency water bucket placement (auto-MLG)
 * to negate lethal fall damage.
 *
 * Configured with [ActionPriority.CRITICAL] to bypass all token bucket rate limiters
 * and immediately preempt any ongoing background operations (such as inventory sorting).
 */
class EmergencyMLGAction : ModAction {
    override val priority: ActionPriority = ActionPriority.CRITICAL

    var executed: Boolean = false
        private set

    override suspend fun execute(context: ActionContext): ActionResult {
        executed = true
        return ActionResult.Success
    }
}
