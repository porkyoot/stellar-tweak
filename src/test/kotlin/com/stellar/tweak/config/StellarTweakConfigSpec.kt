package com.stellar.tweak.config

import com.stellar.core.config.ConfigManager
import com.stellar.core.input.EventResult
import com.stellar.core.input.GameAction
import com.stellar.core.input.GameActionKeyResolver
import com.stellar.core.input.Key
import com.stellar.core.input.Modifier
import com.stellar.core.input.ModifierMatch
import com.stellar.core.input.registerKey
import com.stellar.tweak.input.StellarTweakInputBridge
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.lwjgl.glfw.GLFW

class StellarTweakConfigSpec : FunSpec({
    test("StellarTweakConfig registers with expected defaults") {
        val testId = "test_${System.nanoTime()}"
        val config = ConfigManager.register("stellar_tweak", testId, StellarTweakConfig::class.java)
        config shouldNotBe null
        config.enableToast.value() shouldBe true
        config.toastKey.value() shouldBe Key.KEY_G
        config.toastTitle.value() shouldBe "Stellar Tweak"
        config.toastMessage.value() shouldBe "Config & Input Test Triggered!"

        config.enableToast.setValue(false, true)
        config.enableToast.value() shouldBe false

        config.toastKey.setValue(Key.KEY_H, true)
        config.toastKey.value() shouldBe Key.KEY_H
    }

    test("StellarTweakInputBridge passes unhandled keys and handles registered feature bindings") {
        StellarTweakInputBridge.initialize()
        val bridge = StellarTweakInputBridge
        val manager = bridge.inputManager

        // By default with no test interceptors, keys and mouse events pass through
        bridge.onKey(Key.KEY_C, GLFW.GLFW_PRESS, 0) shouldBe EventResult.PASS
        bridge.onMouseButton(GLFW.GLFW_MOUSE_BUTTON_1, GLFW.GLFW_PRESS, 0) shouldBe EventResult.PASS
        bridge.onMouseScroll(0.0, 1.0) shouldBe EventResult.PASS

        // Configure action resolver
        manager.actionResolver = GameActionKeyResolver { action ->
            when (action) {
                GameAction.CROUCH -> Key.KEY_C
                GameAction.SPRINT -> Key.KEY_R
                else -> null
            }
        }

        // Register a feature binding
        manager.registerKey("stellar_tweak:test_crouch_feature") {
            gameAction(GameAction.CROUCH)
            modifiers(Modifier.CTRL)
            match = ModifierMatch.EXACT
            onPress { EventResult.CONSUMED }
        }

        // Press Ctrl
        bridge.onKey(Key.KEY_LEFT_CONTROL, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL)
        manager.stateTracker.activeModifiers.contains(Modifier.CTRL) shouldBe true

        // Press C (Crouch key pressed while Ctrl is down) -> should consume feature binding
        val crouchResult = bridge.onKey(Key.KEY_C, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL)
        crouchResult shouldBe EventResult.CONSUMED

        // Release C
        bridge.onKey(Key.KEY_C, GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_CONTROL)

        // Release Ctrl
        bridge.onKey(Key.KEY_LEFT_CONTROL, GLFW.GLFW_RELEASE, 0)
        manager.stateTracker.activeModifiers.contains(Modifier.CTRL) shouldBe false

        // Unbound key press (e.g. G) passes through without toast or interception
        val unboundResult = bridge.onKey(Key.KEY_G, GLFW.GLFW_PRESS, 0)
        unboundResult shouldBe EventResult.PASS
    }
})
