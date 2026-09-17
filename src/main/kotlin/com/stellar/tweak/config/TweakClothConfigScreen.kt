package com.stellar.tweak.config

import com.stellar.core.config.ConfigManager
import com.stellar.core.input.Key
import com.stellar.tweak.StellarTweakMod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Factory for creating the Cloth Config GUI screen bound to StellarTweakConfig.
 */
object TweakClothConfigScreen {
    fun create(parent: Screen?): Screen {
        val config = ConfigManager.get<StellarTweakConfig>(StellarTweakMod.MOD_ID, "main")
            ?: ConfigManager.register(StellarTweakMod.MOD_ID, "main", StellarTweakConfig::class.java)

        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Stellar Tweak Config"))

        builder.setSavingRunnable {
            config.save()
        }

        val entryBuilder = builder.entryBuilder()
        val generalCategory = builder.getOrCreateCategory(Component.literal("General"))

        val enableToastEntry = entryBuilder
            .startBooleanToggle(Component.literal("Enable Toast"), config.enableToast.value())
            .setDefaultValue(true)
            .setSaveConsumer { value -> config.enableToast.setValue(value, true) }
            .build()

        val toastKeyEntry = entryBuilder
            .startIntField(Component.literal("Toast Key (GLFW)"), config.toastKey.value())
            .setDefaultValue(Key.KEY_G)
            .setSaveConsumer { value -> config.toastKey.setValue(value, true) }
            .build()

        val toastTitleEntry = entryBuilder
            .startStrField(Component.literal("Toast Title"), config.toastTitle.value())
            .setDefaultValue("Stellar Tweak")
            .setSaveConsumer { value -> config.toastTitle.setValue(value, true) }
            .build()

        val toastMessageEntry = entryBuilder
            .startStrField(Component.literal("Toast Message"), config.toastMessage.value())
            .setDefaultValue("Config & Input Test Triggered!")
            .setSaveConsumer { value -> config.toastMessage.setValue(value, true) }
            .build()

        generalCategory.addEntry(enableToastEntry)
        generalCategory.addEntry(toastKeyEntry)
        generalCategory.addEntry(toastTitleEntry)
        generalCategory.addEntry(toastMessageEntry)

        return builder.build()
    }
}
