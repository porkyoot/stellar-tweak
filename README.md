<div align="center">

<img src="src/main/resources/assets/stellar_tweak/icon.png" alt="Stellartweak Icon" width="128" height="128" />

# 🛠️ Stellartweak

**The ultimate client-side toolkit combining advanced building aids, smart inventory management, and movement automation.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2+%20%2F%2026.2-blue.svg)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Quilt-purple.svg)](https://quiltmc.org/)
[![Language](https://img.shields.io/badge/Kotlin-2.4-orange.svg)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://adoptium.net/)
[![Environment](https://img.shields.io/badge/Environment-100%25%20Client--Side-blueviolet.svg)](#-technical-details)

</div>

---

## 📖 About

**Stellartweak** is the largest and most feature-rich mod in the Stellar suite. Originally conceived as two separate mods, it combines advanced building aids, smart inventory management, and movement automation into a single, highly optimized client-side toolkit.

Stellartweak is designed to remove the tedious aspects of gameplay while offering incredibly powerful tools for builders and technical players. Because many of its features (like fast bridging and auto-MLG) can be classified as cheats on standard multiplayer servers, Stellartweak is deeply integrated with [**Stellarlaw**](../stellar-law/).

> [!IMPORTANT]
> **Fair Play by Default:** All controversial or combat-sensitive features are disabled by default on multiplayer servers. They will only activate if the server explicitly permits them via **Stellarlaw** broadcasts (such as in verified Anarchy environments).

---

## ⚙️ Core Features

### 🏗️ Advanced Building & Overlays
* **Smart Placement:** Place blocks diagonally or adjacently without visible support blocks (Fast Bridge), coupled with an advanced cursor raycasting system for precise face-placement.
* **Shape Selection:** Quickly generate and place complex geometric shapes including spheres, rings, cylinders, lines, cubes, and solid prisms.
* **CPS Management & Fast Breaking:** Configurable clicks-per-second management for instant block placement and optimized fast-breaking in Creative mode.
* **Light Level Overlay:** Instantly view the light levels of surrounding blocks to prevent mob spawning (Default keybind: `L`).

### 🏃 Automation & Movement
* **Auto-MLG:** Automatically saves you from lethal falls or dangerous hazards by instantly deploying water buckets, slime blocks, or other safety measures.
* **Auto-Parkour:** Dynamically places blocks beneath you while jumping, allowing for seamless high-speed traversal over treacherous gaps.
* **Fluid & Block Automation:** Intelligently handles the rapid placement and retrieval of water, lava, and utility blocks during fast-paced movement.

### 🎒 Smart Inventory & Combat
* **Auto Tool & Weapon Swap:** Instantly switches to the optimal tool for the block being mined (respecting enchantments, efficiency, and durability) and selects the best weapon based on the targeted opponent.
* **Auto-Refill:** Automatically replaces broken tools, replenishes depleted hotbar block stacks from your inventory, and feeds you when hunger drops.
* **Fast Loot:** Extract items from chests instantly without needing to open the GUI, or utilize high-speed swipe looting when the UI is open.
* **Advanced Sorting:** Highly efficient, customizable one-click inventory and container organization.

---

## 🏛️ Architectural Isolation & Boundaries

Stellartweak is engineered strictly for client instances (`ClientModInitializer`). As enforced by [Detekt](../config/detekt/detekt-stellar-tweak.yml):

- **100% Client-Side:** Never imports dedicated server Minecraft classes (`net.minecraft.server.dedicated.*`).
- **No Server Lifecycle Hooks:** Prohibited from using server-side entrypoints.
- **Decoupled from Server Moderation:** Never imports or directly couples with `com.stellar.ops.*`.

---

## 🛠️ Technical Details

* **Target Minecraft Version:** 26.1.2+ (tested on 26.2)
* **Mod Loader:** Quilt Loader (`0.30.1+`)
* **Environment:** Client-side only (`client` entrypoint: `com.stellar.tweak.StellarTweakMod`)
* **Language:** Kotlin 2.4 on Java 21
* **Dependencies:**
  * **Stellarcore:** Bundled internally via Gradle Fat Jar.
  * **Stellarlaw:** Soft/optional integration (`compileOnly` & `"optional": true` in `quilt.mod.json`). Ensures automatic compliance with server policies and unlocks restricted features on permitted servers.
  * **Mod Menu:** Soft/recommended dependency (`compileOnly` & `"optional": true` in `quilt.mod.json`). Recommended for configuring client tweaks, keybinds, and feature toggles in-game.

---

## 🧪 Testing & Verification

* **Unit Tests (Kotest):**
  ```bash
  ./gradlew :stellar-tweak:test
  ```
  Runs unit specifications in [`StellarTweakSpec.kt`](file:///home/etoile/50-59_Code/50_Minecraft_Mods/stellar-mods/stellar-tweak/src/test/kotlin/com/stellar/tweak/StellarTweakSpec.kt).

* **Detekt Static Analysis:**
  ```bash
  ./gradlew :stellar-tweak:detekt
  ```

---

## 📦 Building from Source

```bash
./gradlew :stellar-tweak:build
```

The compiled mod JAR will be located at:
```
stellar-tweak/build/libs/stellar-tweak-1.0.0.jar
```
