# Inkwell

A public **library plugin** for Hytale — the shared reservoir other plugins draw from. Inkwell ships
reusable content (weapons, projectiles, combat primitives, …) and the API to use it. It's a real,
loadable mod: dependent plugins declare a runtime dependency on it and the server loads Inkwell first.

## Features

- **Frost Staff** — an ice staff whose ice ball deals damage and **slows every creature in a small
  blast radius** on impact, with a world-oriented ice explosion. See [docs/frost-staff.md](docs/frost-staff.md).
- **`Inkwell_AoeEffect`** — a reusable custom interaction (Java) that applies a status effect to every
  entity within a radius (by **bounding-box overlap**, so large creatures are handled) and spawns an
  impact particle. Drop it into any interaction list and configure it from JSON.
  See [docs/aoe-effect-interaction.md](docs/aoe-effect-interaction.md).

📖 Full documentation lives in **[docs/](docs/README.md)**.

## Depending on Inkwell

A dependent plugin declares Inkwell in its `manifest.json` so the loader resolves load order (Inkwell
loads before its dependents):

```json
{
  "Dependencies": {
    "inkthorne:Inkwell": "^0.1.0"
  }
}
```

Use `OptionalDependencies` instead if the dependent should still load when Inkwell is absent.

## Building

```bash
./gradlew build      # produces build/libs/inkwell.jar
```

Compiles against the bundled `HytaleServer.jar` located by `hytale-paths.gradle`. Requires **JDK 25**;
targets Hytale server **0.5.x**.

## Deploying

```bash
./deploy.sh          # builds, then copies the jar into the launcher's UserData/Mods dir
```

On Windows use `deploy.bat`. (Both always build first, so editing source and deploying never ships a
stale jar.)

## Layout

```
src/main/java/inkthorne/inkwell/
  InkwellPlugin.java                plugin entry point; registers custom types in setup()
  InkwellAoeEffectInteraction.java  the Inkwell_AoeEffect interaction primitive
src/main/resources/
  manifest.json                     plugin metadata (Main class, server version, asset pack)
  Server/...                        bundled JSON assets (items, projectiles, interactions)
docs/                               feature & asset documentation
```

The manifest sets `IncludesAssetPack`, so anything under `src/main/resources/Server` ships inside the jar.
Every custom asset id and interaction `Type` is prefixed with `Inkwell_` to avoid clobbering base-game
or other plugins' content (Hytale resolves asset ids globally per type).

## Contributing

See [CLAUDE.md](CLAUDE.md) for build/architecture notes and the conventions used in this repo.
