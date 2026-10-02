package com.stellar.tweak.input

import com.stellar.core.config.ConfigManager
import com.stellar.core.input.EventResult
import com.stellar.core.input.GameAction
import com.stellar.core.input.GameActionKeyResolver
import com.stellar.core.input.InputManager
import com.stellar.core.input.KeyMappingRegistry
import com.stellar.tweak.StellarTweakMod
import com.stellar.tweak.config.StellarTweakConfig
import com.stellar.tweak.mixin.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * Client-side input bridge connecting Minecraft raw input events to the Stellar InputManager.
 */
object StellarTweakInputBridge {
    val sortKeyMapping: KeyMapping = KeyMappingRegistry.register(
        name = "key.stellar_tweak.sort",
        keyCode = GLFW.GLFW_KEY_R,
        category = KeyMappingRegistry.CATEGORY_STELLAR,
    )

    val inputManager: InputManager = InputManager(
        screenContextProvider = {
            runCatching { Minecraft.getInstance().gui.screen() != null }.getOrDefault(false)
        },
    )

    @Suppress("UnusedParameter")
    fun isSortKey(keyCode: Int, scanCode: Int): Boolean {
        return KeyMappingRegistry.matches(sortKeyMapping, keyCode)
    }

    fun initialize() {
        ConfigManager.get<StellarTweakConfig>(StellarTweakMod.MOD_ID, "main")
            ?: ConfigManager.register(StellarTweakMod.MOD_ID, "main", StellarTweakConfig::class.java)

        inputManager.actionResolver = GameActionKeyResolver { action -> resolveGameActionKey(action) }
    }

    fun onKey(key: Int, action: Int, modifiers: Int): EventResult {
        return inputManager.onKey(key, action, modifiers)
    }

    fun onMouseButton(button: Int, action: Int, modifiers: Int): EventResult {
        return inputManager.onMouseButton(button, action, modifiers)
    }

    fun onMouseScroll(horizontal: Double, vertical: Double): EventResult {
        return inputManager.onMouseScroll(horizontal, vertical)
    }

    private fun resolveGameActionKey(action: GameAction): Int? {
        val mc = runCatching { Minecraft.getInstance() }.getOrNull() ?: return null
        val options = mc.options
        return when (action) {
            GameAction.CROUCH -> getBoundKey(options.keyShift)
            GameAction.SPRINT -> getBoundKey(options.keySprint)
            GameAction.JUMP -> getBoundKey(options.keyJump)
            else -> null
        }
    }

    private fun getBoundKey(keyMapping: KeyMapping?): Int? {
        if (keyMapping == null) return null
        return runCatching {
            (keyMapping as? KeyMappingAccessor)?.stellarGetKey()?.value
        }.getOrNull()
    }
}
