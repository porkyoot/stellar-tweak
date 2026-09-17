package com.stellar.tweak.config

import com.stellar.core.config.ConfigManager
import com.stellar.core.input.EventResult
import com.stellar.core.input.GameAction
import com.stellar.core.input.GameActionKeyResolver
import com.stellar.core.input.Key
import com.stellar.core.input.Modifier
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

    test("StellarTweakInputBridge action bindings handle crouch and sprint key pressed") {
        StellarTweakInputBridge.initialize()
        val manager = StellarTweakInputBridge.inputManager

        // Set action resolver to map Crouch -> C, Sprint -> R
        manager.actionResolver = GameActionKeyResolver { action ->
            when (action) {
                GameAction.CROUCH -> Key.KEY_C
                GameAction.SPRINT -> Key.KEY_R
                else -> null
            }
        }

        // Press Ctrl
        manager.onKey(Key.KEY_LEFT_CONTROL, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL)
        manager.stateTracker.activeModifiers.contains(Modifier.CTRL) shouldBe true

        // Press C (Crouch key pressed while Ctrl is down) -> should consume crouch_action_test
        val crouchResult = manager.onKey(Key.KEY_C, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL)
        crouchResult shouldBe EventResult.CONSUMED

        // Release C
        manager.onKey(Key.KEY_C, GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_CONTROL)

        // Press R (Sprint key pressed while Ctrl is down) -> should consume sprint_action_test
        val sprintResult = manager.onKey(Key.KEY_R, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL)
        sprintResult shouldBe EventResult.CONSUMED

        // Release R and Ctrl
        manager.onKey(Key.KEY_R, GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_CONTROL)
        manager.onKey(Key.KEY_LEFT_CONTROL, GLFW.GLFW_RELEASE, 0)
        manager.stateTracker.activeModifiers.contains(Modifier.CTRL) shouldBe false

        // Plain G press -> triggers toast
        val toastResult = manager.onKey(Key.KEY_G, GLFW.GLFW_PRESS, 0)
        toastResult shouldBe EventResult.CONSUMED
    }
})
