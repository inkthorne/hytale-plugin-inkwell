# Inkwell documentation

Inkwell is a public **library plugin** for Hytale — a shared reservoir of reusable content
(weapons, projectiles, combat primitives) and the API to use it. Dependent plugins declare a
runtime dependency on it; the server loads Inkwell first.

This directory documents how the plugin works and what it provides. (For build/deploy and
architecture aimed at contributors, see [`../CLAUDE.md`](../CLAUDE.md).)

## Conventions

- **Asset naming:** every custom asset id and custom interaction `Type` is prefixed with
  `Inkwell_` (e.g. `Inkwell_Weapon_Staff_Frost`, `Inkwell_AoeEffect`). Hytale resolves asset ids
  globally and case-sensitively, and a file sharing a vanilla id silently *replaces* it — the prefix
  prevents clobbering base-game or other plugins' content.
- **Assets** live under `src/main/resources/Server/...`, mirroring the game's layout. They ship
  inside the jar because the manifest sets `IncludesAssetPack`.
- **Java** lives under `src/main/java/inkthorne/inkwell/`.

## Reusable primitives (Java)

Building blocks other content can reference by `Type` in JSON.

- [`Inkwell_AoeEffect` — area-of-effect status applier](aoe-effect-interaction.md)
- [`Inkwell_Orbit` — orbit-the-target NPC body motion](orbit-body-motion.md)

## Content

- [Frost Staff](frost-staff.md) — ice staff whose projectile slows everything in a small blast radius
- [Annoying Chicken](assets/inkwell-role-chicken-annoying.md) — a chicken NPC that endlessly circles the nearest player

## Asset reference

Per-asset field references (the [`Inkwell_AoeEffect`](aoe-effect-interaction.md) page covers the
`Inkwell_Interaction_Ice_Burst` asset):

- [`Inkwell_Weapon_Staff_Frost`](assets/inkwell-weapon-staff-frost.md) — the frost staff item
- [`Inkwell_Projectile_Config_Ice_Ball`](assets/inkwell-projectile-config-ice-ball.md) — its ice ball projectile
- [`Inkwell_Role_Chicken_Annoying`](assets/inkwell-role-chicken-annoying.md) — the orbiting chicken NPC role
