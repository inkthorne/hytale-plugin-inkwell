# Inkwell

A public **library plugin** for Hytale — the shared reservoir other plugins draw
from. Inkwell ships reusable content (weapons, projectiles, combat primitives, …)
and the API to use it. It's a real, loadable mod: dependent plugins declare a
runtime dependency on it and the server loads Inkwell first.

> **Status:** skeleton. The plugin loads and logs; no library content is wired up
> yet.

## Depending on Inkwell

A dependent plugin declares Inkwell in its `manifest.json` so the loader resolves
load order (Inkwell loads before its dependents):

```json
{
  "Dependencies": {
    "inkthorne:Inkwell": "^0.1.0"
  }
}
```

Use `OptionalDependencies` instead if the dependent should still load when Inkwell
is absent.

## Building

```bash
./gradlew build      # produces build/libs/inkwell.jar
```

Compiles against the bundled `HytaleServer.jar` located by `hytale-paths.gradle`.
Requires JDK 25.

## Deploying

```bash
./deploy.sh          # builds if needed, then copies the jar into the launcher's Mods dir
```

On Windows use `deploy.bat`.

## Layout

```
src/main/java/inkthorne/inkwell/
  InkwellPlugin.java      plugin entry point — register reusable pieces in setup()
src/main/resources/
  manifest.json           plugin metadata (Main class, server version, asset pack)
  Server/...              (planned) bundled item/weapon/particle/sound assets
```

The manifest sets `IncludesAssetPack`, so any assets added under
`src/main/resources/Server` ship inside the jar.
