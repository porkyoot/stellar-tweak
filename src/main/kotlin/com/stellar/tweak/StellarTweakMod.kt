package com.stellar.tweak

import com.stellar.core.StellarCore
import com.stellar.core.config.ConfigManager
import com.stellar.tweak.config.StellarTweakConfig
import com.stellar.tweak.input.StellarTweakInputBridge
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Dedicated client entry point for the Stellar Tweak mod (100% client-side).
 */
class StellarTweakMod : ClientModInitializer {
    private val logger: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitializeClient() {
        StellarCore.logInfo("Initializing Stellar Tweak under namespace ${StellarCore.NAMESPACE}")
        logger.info("Initializing client mod Stellar Tweak")

        ConfigManager.register(MOD_ID, "main", StellarTweakConfig::class.java)
        StellarTweakInputBridge.initialize()
    }

    companion object {
        const val MOD_ID: String = "stellar_tweak"
    }
}
