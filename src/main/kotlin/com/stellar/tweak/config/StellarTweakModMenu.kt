package com.stellar.tweak.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

/**
 * ModMenu integration for Stellar Tweak.
 */
class StellarTweakModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            TweakClothConfigScreen.create(parent)
        }
    }
}
