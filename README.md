# Draconic Evolution for Minecraft 26.1.2 — restjack fork (`eli/upstream-fixes`)

> **This branch** = [TasteFu1's unofficial 26.1.2 port](https://github.com/TasteFu1/Draconic-Evolution/tree/port/26.1.2-neoforge) **plus bug fixes** found while running it on a dedicated server (FTB Evolution 2).
> Our own fixes are also offered upstream as [TasteFu1/Draconic-Evolution#1](https://github.com/TasteFu1/Draconic-Evolution/pull/1).
> Not affiliated with or endorsed by brandon3055. All credit for Draconic Evolution goes to him; the port itself is TasteFu1's and MehanikTMYT's work.

## What this branch adds

| Fix | Effect in game |
| --- | --- |
| Stabilizer beam direction | Energy core stabilizer beams point into the core (were all pointing north). |
| `ModelMat` shader fix | Stabilizer orbs, pylon shells and the legacy core shell render on the block (they were floating around the camera, often seen as big cyan shapes in four directions). |
| Live machine screens | The energy core screen (and every DE machine screen) updates stored OP and I/O while open. |
| Block effects switch | Client option `blockEffectRendering` in `config/brandon3055/DraconicEvolution.cfg`, or the **eye button** on the energy core screen. Hides energy core, stabilizer, pylon and reactor visuals **for that player only**; the blocks keep working. A safety valve if they render wrong or crash your graphics. |
| Upstream 1.21 fixes (open PRs [#2044](https://github.com/Draconic-Inc/Draconic-Evolution/pull/2044), [#2046](https://github.com/Draconic-Inc/Draconic-Evolution/pull/2046), [#2050](https://github.com/Draconic-Inc/Draconic-Evolution/pull/2050), [#2051](https://github.com/Draconic-Inc/Draconic-Evolution/pull/2051), [#2052](https://github.com/Draconic-Inc/Draconic-Evolution/pull/2052), [#2053](https://github.com/Draconic-Inc/Draconic-Evolution/pull/2053)) | Stabilizer disintegration, Draconium Chest losing shift-clicked items, constant module re-saves, night-vision packet spam, crystals/pylons/reactors keeping far chunks loaded, crystal network spam, dislocator limits. #2052 is adapted to the port; #2053's arena-check commit is left out on purpose (it can stop the guardian spawning on servers with view distance < 8). |

Mod version string: `3.1.4-port.3-eli.6`. Needs NeoForge **26.1.2.126 or newer** (below 26.2), Minecraft 26.1.2, Java 25.

## Installing

There are **no prebuilt downloads**. Draconic Evolution's licence does not allow redistributing the mod, so build it yourself (below). Put the three jars in `mods/` on **every client and the server**:

| File (from the build) | Provides |
| --- | --- |
| `Draconic-Evolution-26.1.2-3.1.4-port.3-eli.6.jar` | Draconic Evolution |
| `BrandonsCore-3.2.1+26.1.2-port.1.jar` | Brandon's Core |
| `codechickenlib-4.7.0+26.1.2-port.2.jar` | CodeChickenLib |

Sharing the jars privately with your own modpack/server players is covered by brandon3055's modpack permission. Please don't upload them anywhere public.

## Building (incl. Windows traps)

Needs Git, Python 3.8+ and a **JDK 25**. Then, from this branch:

```sh
python gradle/build-port.py assemble     # or "build" to also run tests
```

On **Windows** two things break the build; both have easy workarounds:

1. **Git line endings.** With Git's default `core.autocrlf=true`, the port's `.patch` files and the library checkouts get CRLF, and `git apply` fails with "patch does not apply". Clone with `git clone -c core.autocrlf=false …`, and run the build with the override for the library checkouts:
   ```sh
   GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=core.autocrlf GIT_CONFIG_VALUE_0=false python gradle/build-port.py assemble
   ```
2. **Gradle can't download Java 25 itself** (the pinned foojay plugin is too old for Gradle 9: `JvmVendorSpec does not have member field ... IBM_SEMERU`). Point it at a local JDK 25 instead (e.g. an unpacked Temurin 25 zip):
   ```sh
   export JAVA_HOME=/path/to/jdk-25
   export GRADLE_OPTS="-Dorg.gradle.java.installations.paths=/path/to/jdk-25 -Dorg.gradle.java.installations.auto-download=false"
   ```

Output: `.gradle/port-work/Draconic-Evolution/build/libs/` (the mod) and `.gradle/port-work/deps/` (the two libraries). `assemble` is enough; the unit-test stage needs the same JDK setup.

## Adding it to an existing world

- Draconic Evolution's ore generates only in **newly generated chunks**. A world explored (or pregenerated) before you added the mod has no Draconium there.
- **Retrogen:** `extras/ftb-evolution-2/kubejs/` has a KubeJS 8 script pair (`startup_scripts/retrogen_hook.js` + `server_scripts/systems/retrogen.js`). It adds DE's own ore features to old chunks once each, at DE's own rates, and skips brand-new chunks. Copy both, then fully restart.
- **Chaos Islands** generate every `chaosIslandSeparation` blocks (default 10000) in the End, never at 0,0. With a world border smaller than that, lower it (e.g. `2000`) **before** the End generates, and restart fully; DE reads it at startup only. An already-generated End needs its terrain reset (keep each dimension's `data/` folder, which holds the world border).
- **Mystical Agriculture** ships a Draconium ore-infusion recipe with an outdated item id. `extras/ftb-evolution-2/kubejs/server_scripts/recipes/draconic_mystical_compat.js` re-adds it with the right one.

## Known issues (upstream, not fixed here)

- Chaos Guardian fight: upstream reports a crash on right-clicking a guardian crystal, an invulnerable crystal after the kill, double spawns and a stuck gravity well. Back up before fighting.
- Never move an Energy Core with AE2 Spatial IO (upstream server-crash report).
- The core screen shows **net** I/O; hold **Shift** for separate in/out (same as upstream).
- The Information Tablet has no recipe (also true in official 1.21.1; it needs Project Intelligence).

---

*TasteFu1's original README for the port follows.*

# Draconic Evolution

## Minecraft 26.1.2 · NeoForge

This branch is an unofficial port of Draconic Evolution by brandon3055. It builds on [the original 1.21 branch](https://github.com/Draconic-Inc/Draconic-Evolution/tree/1.21) and [MehanikTMYT’s 26.1.2 migration](https://github.com/MehanikTMYT/Draconic-Evolution/tree/port/26.1.2).

### Installation

Use Java 25 and Minecraft **26.1.2** with NeoForge **26.1.2.126**. Place the following three files together in the instance’s `mods` directory on the client and the dedicated server.

| File | Provides |
| --- | --- |
| `Draconic-Evolution-26.1.2-3.1.4-port.3.jar` | Draconic Evolution |
| `BrandonsCore-3.2.1+26.1.2-port.1.jar` | Brandon’s Core |
| `codechickenlib-4.7.0+26.1.2-port.2.jar` | CodeChicken Lib and its embedded Quack library |

### Classic 16×16 textures

Classic 16×16 is the default look from the first launch. The mod includes pixel-art tools in hand and in the inventory, animated bows, and classic textures for blocks and modules. The 23 modular equipment items use 16×16 sprites. Equipped chestpieces use the standard Minecraft armor shape and its 64×32 texture sheet. The build also produces a standalone ZIP in `Draconic-Evolution/build/distributions` under the build directory.

The classic sprites include artwork by enemeez1 and 5284973. The pack includes the source credits and licenses. [Original 16× texture pack](https://www.curseforge.com/minecraft/texture-packs/improved-draconic-evolution-16x-textures/files/2581395)

### Tested on 26.1.2

The client entered singleplayer and joined a dedicated NeoForge server with the three jars listed above. Both loaded all 179 Draconic Evolution recipes, including 39 fusion recipes.

Gameplay checks covered crafting a Wyvern Sword using six injectors and eight million OP, installing and charging a Wyvern energy module, using a bound Dislocator, linking energy crystals, and assembling a reactor. A server restart preserved machine inventories, energy and filters. World generation produced Draconium ore in the Overworld and a Chaos Island with its crystal in the End.

### Building from source

Install Git, Python 3.8 or later and a JDK 25. From the root of this branch run the following command.

```sh
python gradle/build-port.py build
```

The script builds the pinned library sources, applies the port’s patches and runs the Draconic Evolution Gradle build. Set `DE_BUILD_ROOT` to choose the working directory. It defaults to `.gradle/port-work` inside the checkout. The finished mod is under `Draconic-Evolution/build/libs` in that directory, and its two required libraries are under `deps`. All three projects retain their original licenses.

[![](http://cf.way2muchnoise.eu/short_draconic-evolution_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/draconic-evolution)
[![](http://cf.way2muchnoise.eu/versions/Available%20for_draconic-evolution_full.svg)](https://www.curseforge.com/minecraft/mc-mods/draconic-evolution/files)
[![](https://img.shields.io/discord/474753901451542531.svg?logo=discord&label=Discord&labelColor=2d2d2d&style=flat)](https://discord.gg/e2HBEtF)
[![](https://img.shields.io/badge/Nexus%20index-maven-blue?style=flat&labelColor=2d2d2d)](https://nexus.covers1624.net/#browse/browse:maven-hosted:com%2Fbrandon3055%2Fdraconicevolution%2FDraconic-Evolution)
[![](https://img.shields.io/badge/Patreon-brandon3055-red?style=flat&labelColor=2d2d2d&logo=patreon)](https://www.patreon.com/brandon3055)

Draconic Evolution adds powerful new tools, Items, weapons and Armor. The premise is the enigmatic ore - Draconium. As you progress you find it has many uses from the mundane such as machines and items, to the near-mystical at the top with Awakened Draconium to make extremely powerful armor and tools.

## License / Use in Modpacks
This mod is licensed under the [**Don't Be a Jerk License**](https://github.com/Draconic-Inc/Draconic-Evolution/blob/HEAD/LICENSE.md) created by CoFH.
I hereby grant permission to use this mod in any mod pack without the need to request permission from myself the owner (brandon3055).
