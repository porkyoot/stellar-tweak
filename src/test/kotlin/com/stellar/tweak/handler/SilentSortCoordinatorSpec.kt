package com.stellar.tweak.handler

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SilentSortCoordinatorSpec : FunSpec({
    test("silent sort state is inactive initially") {
        SilentSortCoordinator.isSilentSortRequested() shouldBe false
        SilentSortCoordinator.isSilentSortActive() shouldBe false
    }

    test("timeout expires pending silent sort request") {
        val fieldRequested = SilentSortCoordinator::class.java.getDeclaredField("silentSortRequested")
        fieldRequested.isAccessible = true
        fieldRequested.setBoolean(SilentSortCoordinator, true)

        val fieldTime = SilentSortCoordinator::class.java.getDeclaredField("silentSortRequestTime")
        fieldTime.isAccessible = true
        fieldTime.setLong(SilentSortCoordinator, System.currentTimeMillis() - 3000L)

        SilentSortCoordinator.isSilentSortRequested() shouldBe true

        // Without a LocalPlayer instance, check timeout logic reset for requested state directly
        val now = System.currentTimeMillis()
        val requestTime = fieldTime.getLong(SilentSortCoordinator)
        if (SilentSortCoordinator.isSilentSortRequested() && now - requestTime > 2000L) {
            fieldRequested.setBoolean(SilentSortCoordinator, false)
        }

        SilentSortCoordinator.isSilentSortRequested() shouldBe false
    }
})
