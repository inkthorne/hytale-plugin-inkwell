# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Inkwell is a public **library plugin** for Hytale — a real, loadable mod that ships reusable content (weapons, projectiles, combat primitives, …) and the API to use it. Other plugins (e.g. Telefrag, Fluff) declare a runtime dependency on it via their `manifest.json` `Dependencies`, and the server loads Inkwell first. It is currently a skeleton: the plugin loads and logs, but no library content is wired up yet.

## Commands

```bash
./gradlew build      # produces build/libs/inkwell.jar (compiles against bundled HytaleServer.jar)
./deploy.sh          # builds if needed, then copies the jar into the launcher's UserData/Mods dir
```

On Windows use `build.bat` / `deploy.bat`. Requires **JDK 25** (source/target are `VERSION_25`).

There are no tests or linters configured.

## Modding reference

Extensive Hytale plugin documentation lives at `../../hytale-modding-handbook/docs` (relative to this repo). It covers the modding APIs in depth — combat, items/weapons, entities, components, events, networking, interactions, assets, and more — and is the canonical reference when wiring library content into `setup()`.

To inspect the game's actual assets, a cached (already-unzipped) copy of `Assets.zip` lives at `~/.cache/hytale-assets/` — ~59k files, grep/glob/read-able directly (`Common/` holds blockymodel/blockyanim/UI formats; `Server/` holds JSON asset definitions). See the handbook's `CLAUDE.md` for how it was extracted and how to refresh it after a game update.

To inspect the engine API directly, the local `HytaleServer.jar` (the `compileOnly` dependency, located by `hytale-paths.gradle`) is at `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest/Server/HytaleServer.jar` — use `javap`/`unzip -l` on it for authoritative class/method signatures. As of this install it is game version **0.5.1** (per the jar's `Implementation-Version`); the handbook docs were fact-checked against 0.5.0, so the jar is the more current source of truth for API details.

## Architecture

- **Entry point:** `src/main/java/inkthorne/inkwell/InkwellPlugin.java` extends `JavaPlugin`. Register reusable components/systems/commands/content in `setup()` against the appropriate registry, then log — mirroring the modding handbook examples. The constructor takes a `JavaPluginInit` and calls `super(init)`.
- **Manifest:** `src/main/resources/manifest.json` holds plugin metadata — `Main` class, `ServerVersion` compatibility range, and `IncludesAssetPack: true`. Because the asset pack flag is set, anything placed under `src/main/resources/Server/...` (item/weapon JSON, particles, sounds) ships inside the jar.
- **Dependency on the engine:** `HytaleServer.jar` is `compileOnly` (provided at runtime by the server). It is not vendored — `hytale-paths.gradle` locates it inside the launcher install.

## Hytale path resolution

Three files must stay in sync — they resolve the launcher's Hytale root (HytaleServer.jar to compile against + the Mods dir to deploy into) the same way across contexts:

- `hytale-paths.gradle` — build-side (Gradle)
- `hytale-paths.sh` — deploy-side (bash, sourced by `deploy.sh`)
- `hytale-paths.bat` — Windows

All three pick the root in the same priority order: `$APPDATA/Hytale` (Windows or explicit override) → Linux Flatpak path (`~/.var/app/com.hypixel.HytaleLauncher/data/Hytale`) → `~/AppData/Roaming/Hytale` fallback. If you change path logic in one, change all three.

## Conventions

- Group/package is `inkthorne`; the jar is named `inkwell.jar` with the version stripped from the filename (see `build.gradle`).
- Version lives in both `build.gradle` (`version`) and `manifest.json` (`Version`) — keep them aligned.
