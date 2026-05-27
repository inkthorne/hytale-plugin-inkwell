# `Inkwell_AoeEffect` — area-of-effect status applier

A custom interaction type that applies a status effect to **every entity within a radius** of the
executing entity's position, and optionally spawns an impact particle effect. It's most useful run
from a projectile's `ProjectileHit`/`ProjectileMiss` to give a "blast radius" effect.

- **Java:** `inkthorne.inkwell.InkwellAoeEffectInteraction` (extends `SimpleInstantInteraction`)
- **Registered as:** `Inkwell_AoeEffect` (in `InkwellPlugin.setup()`, against the interaction codec map)

## Why it exists

A JSON `Selector` (`AOECircle`/`AOECylinder`) does **not** sweep a radius when run from a projectile —
it only resolves the directly-collided entity. This interaction does the radius query in code
(`Selector.selectNearbyEntities`) so it works from a projectile impact. It also spawns the impact VFX
from code so the particle is **world-oriented** (a JSON `ModelParticle` would inherit the projectile's
tilt) and renders identically on direct hits and terrain misses.

## JSON fields

```json
{
  "Type": "Inkwell_AoeEffect",
  "Range": 2.0,
  "EffectId": "Slow",
  "Vfx": "IceBall_Explosion",
  "VfxScale": 1.0,
  "VfxDuration": 2.0
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `Range` | double | `5.0` | Blast radius (blocks). An entity is affected if its **bounding box** comes within this distance of the impact (not just its origin — so large creatures grazed by the blast are caught). |
| `EffectId` | string | `"Slow"` | Status effect asset id to apply (under `Server/Entity/Effects/`). Duration comes from the effect asset itself. |
| `Vfx` | string | `""` | Optional particle system spawned world-oriented at the impact (`""` = none). |
| `VfxScale` | double | `1.0` | Scale of the impact particle. |
| `VfxDuration` | double | `2.0` | Duration (seconds) of the impact particle. |

## Behavior notes

- **Caster is excluded** — the executing entity (e.g. the projectile) and its owner (the shooter) are
  never affected, so firing near yourself is safe.
- **Players vs NPCs:** a movement-*speed* effect like `Slow` (`HorizontalSpeedMultiplier`) slows both
  players and NPCs and auto-expires. A `DisableAll` effect (Stun/Root/Freeze) only halts *players* —
  fully stopping NPC AI needs the `Frozen` component, which this interaction does **not** use (it
  doesn't auto-clear and re-adding it throws). So prefer speed-multiplier effects here.
- The radius test uses the squared closest-point distance from the impact to each entity's world AABB.

## Used by

- [Frost Staff](frost-staff.md) → `Inkwell_Ice_Burst` (`Range 2`, `EffectId "Slow"`, `Vfx "IceBall_Explosion"`)
