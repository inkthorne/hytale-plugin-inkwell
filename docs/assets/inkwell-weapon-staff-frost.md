# Asset: `Inkwell_Weapon_Staff_Frost`

**Type:** Item (weapon) · **File:** `Server/Item/Items/Weapon/Staff/Inkwell_Weapon_Staff_Frost.json`

The frost staff item. See [Frost Staff](../frost-staff.md) for the feature-level behavior; this page is
the asset reference.

## Definition

```json
{
  "Parent": "Weapon_Staff_Frost",
  "InteractionVars": {
    "Staff_Cast_Summon_Launch": {
      "Interactions": [
        {
          "Type": "Projectile",
          "RunTime": 0.25,
          "Config": "Inkwell_Projectile_Config_Ice_Ball",
          "Effects": { "ItemAnimationId": "CastSummonCharged" }
        }
      ]
    }
  }
}
```

## Fields

| Field | Value | Notes |
|-------|-------|-------|
| `Parent` | `Weapon_Staff_Frost` | Inherits the entire vanilla frost staff — model, texture, icon, name, cast/charge interactions, mana cost, melee swing, etc. We override **only** the launch step below. |
| `InteractionVars.Staff_Cast_Summon_Launch` | (override) | The vanilla cast resolves this var to decide what the charged cast fires. We replace it so the staff launches our config projectile instead of the vanilla simple `Ice_Ball`. |

### The launch override

| Key | Value | Notes |
|-----|-------|-------|
| `Type` | `Projectile` | Launches a **config** projectile (vs `LaunchProjectile`, which fires a simple `Server/Projectiles/*`). Config projectiles can run interactions — required for the on-impact AOE. |
| `Config` | `Inkwell_Projectile_Config_Ice_Ball` | The projectile to fire — see [its reference](inkwell-projectile-config-ice-ball.md). |
| `RunTime` | `0.25` | Cast animation timing (matches the vanilla pattern). |
| `Effects.ItemAnimationId` | `CastSummonCharged` | Plays the staff's charged-cast animation. |

## Everything inherited (not overridden)

Because `Parent` is the vanilla frost staff and we override only the launch var, all of these come
straight from vanilla:
- **Identity:** display name ("Frost Staff") and icon — *not* yet customized.
- **Firing:** the hold-to-charge `Primary`/`Secondary`, mana cost, stamina cost, melee swing fallback.
- **Visuals:** model, texture, held particles, light.

## Related

- [Inkwell_Projectile_Config_Ice_Ball](inkwell-projectile-config-ice-ball.md) — what it fires
- [Frost Staff feature](../frost-staff.md) — behavior, tuning, limitations
