# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Inkwell is a public **library plugin** for Hytale — a real, loadable mod that ships reusable content (weapons, projectiles, combat primitives, …) and the API to use it. Other plugins (e.g. Telefrag, Fluff) declare a runtime dependency on it via their `manifest.json` `Dependencies`, and the server loads Inkwell first.

Feature/usage documentation lives in [`docs/`](docs/README.md) (the Frost Staff, the `Inkwell_AoeEffect` interaction primitive, etc.) — keep it updated as content is added.

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

## Versioning & releases

The plugin version is **SemVer** (`MAJOR.MINOR.PATCH`). Bump on a **release** (a build other plugins/users actually consume), not per commit — most commits don't change what a consumer gets.

The "public API" for this library plugin is concrete: the **registered string keys** (`Inkwell_AoeEffect`, `Inkwell_Orbit`, …), the **JSON schema** of bundled assets, and any **Java signatures** dependent plugins (Telefrag, Fluff) compile against. Java *class names* are private — rename them freely (the compiler enforces internal consistency); the one exception is `InkwellPlugin`, named by string in `manifest.json` `Main`.

While at `0.x` (initial development, API not yet stable), everything shifts down one slot from stable SemVer:

| Change | `0.x` (now) | `1.x+` (after stable release) |
|---|---|---|
| Breaking (rename/remove a registered key, remove/rename a JSON param, change a param's meaning/default, change a consumed Java signature) | **MINOR** (`0.2.0`) | MAJOR (`2.0.0`) |
| New backwards-compatible feature (new type, new *optional* param with a safe default) | **PATCH** (`0.1.1`) | MINOR (`1.1.0`) |
| Backwards-compatible bug fix | **PATCH** (`0.1.1`) | PATCH (`1.0.1`) |

Cut `1.0.0` only when ready to commit to a stable public API. MAJOR is about **breaking compatibility**, not how big or exciting a feature is.

`ServerVersion` in `manifest.json` is **independent** of this — it declares the compatible Hytale *engine* range, not the plugin's own SemVer.

### Release model: trunk-based

`main` is the **trunk** — it must stay releasable — but dependents (Telefrag, Fluff) consume **tagged releases, not raw `main` HEAD** (building HEAD is unsafe; see the pre-release gotcha below).

- **WIP never goes directly to `main`.** Develop on short-lived **feature branches** (e.g. `feat/orbit-chicken`) and merge via PR only when green and coherent. No long-lived `dev` branch — `main` is the trunk.
- **Between releases, `main` carries a `-dev` pre-release suffix** (e.g. `0.2.0-dev`) in both `build.gradle` and `manifest.json`, so any jar built off the trunk self-identifies as not-a-release.
- **A release is a single commit on `main`:** drop the `-dev` suffix → set the concrete version (per the SemVer table above) → commit → **tag it** (`v0.1.1`). Immediately after, bump the trunk to the **next _patch_** as a `-dev` placeholder (release `0.1.0` → trunk `0.1.1-dev`).
- **Why next-patch for the placeholder:** you don't yet know if the next release is a fix or a feature — you decide that at release time. The `-dev` number just has to sort *above* the last release and *below* whatever comes next, and next-patch is the smallest value that always does (verified against 0.5.1: `0.1.0 < 0.1.1-dev < 0.1.1 < 0.2.0 < 1.0.0`). Do **not** reuse the just-released number (`0.1.0-dev < 0.1.0` — sorts *below* the release), and do **not** placeholder the next *minor* (`0.2.0-dev`): if you then ship a `0.1.1` patch it sorts *below* the trunk you were carrying (`0.2.0-dev > 0.1.1`), a backwards jump.

Tags are what make releases **immutable and retrievable**: `main` HEAD only ever means "latest WIP," but a tag permanently pins a specific version's bytes. Dependents build/pull a tag (or its release jar), never `main`.

**Engine gotcha — pre-releases fail dependency ranges.** Hytale parses `manifest.json` `Version` into a real `Semver` (`com.hypixel.hytale.common.semver`, full pre-release support) and resolves dependents' `Dependencies` as `SemverRange`s with node-semver semantics: **a `-dev` build satisfies _no_ normal range.** Verified against HytaleServer.jar 0.5.1 — `0.2.0-dev` does *not* satisfy `^0.2.0`, `^0.1.0`, or `>=0.1.0`; only `*` or a range that explicitly names the same pre-release (`^0.2.0-dev`) accepts it. So a dependent declaring e.g. `Inkwell: "^0.1.0"` that builds a `-dev` trunk sees the dependency as **unmet and fails to load**. This is *why* dependents must consume tagged (suffix-free) releases — and it's a feature: it stops a half-built trunk from ever satisfying a release pin.

Reach for a separate long-lived release branch only when a concrete need appears — namely patching a stable released line *while* doing risky, long-running next-version work. That doesn't exist at solo `0.x`, so don't add the ceremony preemptively.
