# Asset: `Inkwell_Chicken_Annoying`

**Type:** NPC role · **File:** `Server/NPC/Roles/Creature/Livestock/Inkwell_Chicken_Annoying.json`

A chicken that endlessly circles the nearest player. It reuses the vanilla chicken's appearance but
defines its own AI — a single [`Inkwell_Orbit`](../orbit-body-motion.md) instruction — instead of the
stock flee/flock/graze behavior.

It is a **self-contained `Generic` role, not a `Variant`.** A `Variant`'s `Modify` block runs each
value through the NPC expression system, which rejects a structural `Instructions` array (`Illegal
JSON value for expression`). So custom behavior must be defined at the role's top level, which is what
`Generic` roles (e.g. the engine's own `Test_*` roles) do.

## Definition

```json
{
  "Type": "Generic",
  "Appearance": "Chicken",
  "MotionControllerList": [
    { "Type": "Walk", "MaxWalkSpeed": 7, "Gravity": 10, "MaxFallSpeed": 20, "Acceleration": 10 }
  ],
  "MaxHealth": { "Compute": "MaxHealth" },
  "Parameters": { "MaxHealth": { "Value": 12, "Description": "Max health for the annoying chicken" } },
  "Instructions": [
    {
      "Sensor": { "Type": "Player", "Range": 30, "LockOnTarget": true },
      "BodyMotion": { "Type": "Inkwell_Orbit", "Radius": 2.5, "RelativeSpeed": 0.95, "RadiusGain": 0.6, "Clockwise": true }
    },
    {
      "Sensor": { "Type": "Any" },
      "BodyMotion": { "Type": "WanderInCircle", "Radius": 6, "RelativeSpeed": 0.25 }
    }
  ],
  "NameTranslationKey": "server.npcRoles.Inkwell_Chicken_Annoying.name"
}
```

## Fields

| Field | Value | Notes |
|-------|-------|-------|
| `Type` | `Generic` | Concrete, spawnable, self-contained role (vs. `Variant`, which references a template and can't carry an inline `Instructions` array). |
| `Appearance` | `Chicken` | Reuses the vanilla chicken model/texture/animations. A **referenced vanilla id keeps its name** — no `Inkwell_` prefix (the prefix is only for ids *we* define). |
| `MotionControllerList` | `Walk`, `MaxWalkSpeed 7` | Faster than a normal chicken (≈5) so it keeps up while circling. `MaxWalkSpeed` is what `Inkwell_Orbit`'s `RelativeSpeed` scales against. |
| `MaxHealth` | `12` (via `Parameters`) | Frail — it's a nuisance, not a threat. |
| `Instructions` | (top-level) | First node orbits a sensed player; the fallback wanders when none is in range. |

### Instruction nodes

1. **Orbit a player** — a `Player` sensor (`Range 30`, `LockOnTarget`) supplies the target to
   [`Inkwell_Orbit`](../orbit-body-motion.md), which circles at `Radius 2.5` and `RelativeSpeed 0.95`.
2. **Fallback wander** — `Sensor: Any` → `WanderInCircle`, so when no player is near it mills around
   instead of standing frozen.

## Spawning / testing

Role id = filename without extension = `Inkwell_Chicken_Annoying`. Spawn it in-game with the built-in
NPC spawn command (`NPCSpawnCommand`; confirm exact syntax via `/help`), e.g. spawn it a few blocks
away and watch it orbit you as you move.

## Related

- [`Inkwell_Orbit` body motion](../orbit-body-motion.md) — the reusable circling primitive it uses
