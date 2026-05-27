# Frost Staff

An ice staff whose ice ball **slows every creature in a small blast radius** on impact, on top of
direct-hit damage. Built by extending the vanilla frost staff and re-pointing its cast at a custom
projectile that triggers Inkwell's [`Inkwell_AoeEffect`](aoe-effect-interaction.md) primitive.

> **Item id:** `Inkwell_Weapon_Staff_Frost` · still displays the vanilla name/icon ("Frost Staff").

## Behavior

- **Cast:** hold-to-charge (inherited from the vanilla frost staff), then fires an ice ball. The ball
  has reduced gravity (`2.0` vs vanilla `4.4`) for a flatter, longer-range arc.
- **On impact** (a creature *or* terrain):
  - **25 Ice damage** to a directly-struck entity (charged), plus knockback.
  - **Slow** applied to every creature whose bounding box is within **2 blocks** of the impact — this
    naturally includes a directly-struck creature (its box contains the impact), so no separate
    direct-hit handling is needed. Works on players and NPCs, auto-expires (~10s, the `Slow` effect's
    own duration).
  - A **world-oriented ice explosion** (`IceBall_Explosion`) plays at the impact, identical on direct
    hits and terrain.

## How it's wired

```
Inkwell_Weapon_Staff_Frost            (item; Parent: vanilla Weapon_Staff_Frost)
  └─ cast launches ──► Inkwell_Projectile_Config_Ice_Ball   (Parent: Projectile_Config_Ice_Ball)
        ProjectileHit:  25 Ice dmg + knockback ─► Inkwell_Ice_Burst ─► despawn
        ProjectileMiss: impact sound ─► Inkwell_Ice_Burst ─► despawn
                                               │
                                               └─► Inkwell_Ice_Burst   (Type: Inkwell_AoeEffect)
                                                     Range 2 · EffectId "Slow" · Vfx "IceBall_Explosion"
```

### Files

| File | Role |
|------|------|
| `Server/Item/Items/Weapon/Staff/Inkwell_Weapon_Staff_Frost.json` | The item. Inherits vanilla; overrides only the cast launch to use the projectile below. |
| `Server/ProjectileConfigs/Weapons/Staff/Ice/Inkwell_Projectile_Config_Ice_Ball.json` | Config projectile (so it can run interactions). Gravity 2.0; hit/miss handlers do damage, run the burst, then despawn. |
| `Server/Item/Interactions/Weapons/Staff/Ice/Inkwell_Ice_Burst.json` | The AOE burst (an `Inkwell_AoeEffect` asset) — slow in radius + impact VFX. |

## Tuning

- **Blast radius / slow target effect:** edit `Inkwell_Ice_Burst.json` (`Range`, `EffectId`).
- **Explosion size / length:** `VfxScale` / `VfxDuration` in `Inkwell_Ice_Burst.json`.
- **Damage / gravity / knockback:** the `ProjectileHit` block and `Physics` in the projectile config.
- **Slow duration:** comes from the `Slow` effect asset (`Server/Entity/Effects/Status/Slow.json`,
  ~10s). To use a different duration without touching vanilla, ship an `Inkwell_`-prefixed effect and
  point `EffectId` at it.

All of the above are JSON-only and need no recompile — just `./deploy.sh`.

## Known limitations / ideas

- Still shows the vanilla **"Frost Staff"** name and icon (giving it a distinct identity needs an i18n
  translation key registered via `GenerateDefaultLanguageEvent`).
- Only the main `IceBall_Explosion` burst plays; the vanilla `Impact_Ice` shards were dropped.
- Firing mode is the vanilla hold-to-charge; a single-click cast is possible via a `FirstClick` or
  direct-cast `Primary` override.
