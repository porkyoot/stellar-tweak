package com.stellar.tweak.toast

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component

/**
 * In-game toast helper for triggering test notifications.
 */
object StellarTweakToastHelper {
    private val TOAST_ID by lazy {
        runCatching { SystemToast.SystemToastId() }.getOrNull()
    }

    fun showToast(title: String, message: String) {
        runCatching {
            val mc = Minecraft.getInstance() ?: return
            mc.execute {
                val toastManager = mc.gui?.toastManager()
                val id = TOAST_ID
                if (toastManager != null && id != null) {
                    SystemToast.addOrUpdate(
                        toastManager,
                        id,
                        Component.literal(title),
                        Component.literal(message),
                    )
                }
            }
        }
    }
}
