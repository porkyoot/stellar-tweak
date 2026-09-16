package com.stellar.tweak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * Unit tests for Stellar Tweak using Kotest.
 */
class StellarTweakSpec : FunSpec({
    test("stellar tweak mod id should be valid") {
        StellarTweakMod.MOD_ID.shouldNotBeBlank()
        StellarTweakMod.MOD_ID shouldBe "stellar_tweak"
    }
})
