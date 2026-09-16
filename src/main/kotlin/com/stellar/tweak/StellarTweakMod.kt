package com.stellar.tweak

import com.stellar.core.StellarCore
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
    }

    companion object {
        const val MOD_ID: String = "stellar_tweak"
    }
}
