# Asset: `Inkwell_Projectile_Config_Ice_Ball`

**Type:** Projectile config · **File:** `Server/ProjectileConfigs/Weapons/Staff/Ice/Inkwell_Projectile_Config_Ice_Ball.json`

The ice ball fired by the [Frost Staff](../frost-staff.md). A **config** projectile (launched via
`Type: "Projectile"`), so it can run interactions on impact — which is how it deals damage and triggers
the AOE slow. Inherits the vanilla `Projectile_Config_Ice_Ball`.

## Top-level fields

| Field | Value | Notes |
|-------|-------|-------|
| `Parent` | `Projectile_Config_Ice_Ball` | Inherits the vanilla ice-ball config (model `Ice_Ball`, launch force, launch sound, etc.). |
| `Physics` | (restated, below) | Typed object (`Type: "Standard"`) — must be restated in full when overriding, since a partial override would drop the discriminator and other fields. |
| `Interactions` | (restated, below) | The hit/miss handlers. **Overriding `Interactions` replaces the whole handler map**, so both `ProjectileHit` and `ProjectileMiss` are restated — omitting one would make that case never despawn. |

### `Physics`

| Key | Value | Notes |
|-----|-------|-------|
| `Type` | `Standard` | Physics provider discriminator. |
| `Gravity` | `2.0` | Lowered from vanilla `4.4` for a flatter, longer arc. |
| `TerminalVelocityAir` / `TerminalVelocityWater` | `42.5` / `15` | Inherited values, restated. |
| `RotationMode` | `VelocityDamped` | Projectile faces its velocity. |
| `Bounciness` | `0.0` | No bounce. |
| `SticksVertically` | `true` | Sticks on surface contact. |

## Interactions

Both handlers end with a `Simple RunTime 0.2` **before** `RemoveEntity` — the delay lets impact
effects (sound, the burst's VFX) dispatch before the projectile entity is destroyed. Config
projectiles have no native `TimeToLive`; they despawn via this `RemoveEntity`.

### `ProjectileHit` (struck an entity)

| Step | What it does |
|------|--------------|
| `DamageEntityParent` | **25 Ice** damage (`Class: "Charged"`) + knockback (`Force 20`, upward/back) + impact sound `SFX_Ice_Ball_Death`. |
| `"Inkwell_Interaction_Ice_Burst"` | Runs the [AOE slow](../aoe-effect-interaction.md) centered on the impact. |
| `Simple` `RunTime 0.2` | Delay so effects play. |
| `RemoveEntity` `User` | Despawns the projectile. |

### `ProjectileMiss` (hit terrain / no entity)

| Step | What it does |
|------|--------------|
| `Simple` `RunTime 0.2` | Plays `SFX_Ice_Ball_Death` and acts as the despawn delay. |
| `"Inkwell_Interaction_Ice_Burst"` | Runs the AOE slow at the impact point. |
| `RemoveEntity` `User` | Despawns the projectile. |

> Note: damage lives only in `ProjectileHit` (terrain takes none), but the AOE slow runs on **both**
> paths — so hitting the ground near creatures still slows them.

## Tuning

- **Arc/feel:** `Physics.Gravity` (and the inherited launch force on the parent).
- **Damage / knockback:** the `DamageEntityParent` block in `ProjectileHit`.
- **AOE behavior / VFX:** edit `Inkwell_Interaction_Ice_Burst` (see the [interaction reference](../aoe-effect-interaction.md)).

## Related

- [Inkwell_Interaction_Ice_Burst / Inkwell_AoeEffect](../aoe-effect-interaction.md) — the AOE it triggers
- [Inkwell_Weapon_Staff_Frost](inkwell-weapon-staff-frost.md) — what launches it
