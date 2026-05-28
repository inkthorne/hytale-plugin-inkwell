# Asset: `Inkwell_Chicken_Annoying`

**Type:** NPC role · **File:** `Server/NPC/Roles/Creature/Livestock/Inkwell_Chicken_Annoying.json`

A chicken with **mood swings**: it randomly alternates between circling the nearest player ("annoying")
and ordinary wandering ("default"), holding each mode for a random number of seconds before switching.
It reuses the vanilla chicken's appearance but defines its own AI — built from the
[`Inkwell_Orbit`](../orbit-body-motion.md) body motion — instead of the stock flee/flock/graze behavior.

The alternation is driven entirely by a built-in **`"Type": "Random"`** instruction with an
`ExecuteFor: [min, max]` window (the same primitive the engine's `Test_Random_Instruction.json` uses):
it picks one of its weighted sub-instructions, runs it for a random duration in that range, then
re-rolls. No custom code is needed beyond the `Inkwell_Orbit` body motion.

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
      "Type": "Random",
      "Continue": true,
      "ExecuteFor": [ 5, 12 ],
      "Instructions": [
        {
          "Weight": 1,
          "Sensor": { "Type": "Any" },
          "Instructions": [
            {
              "Sensor": { "Type": "Player", "Range": 30, "LockOnTarget": true },
              "BodyMotion": { "Type": "Inkwell_Orbit", "Radius": 2.5, "RelativeSpeed": 0.95, "RadiusGain": 0.6, "Clockwise": true }
            },
            {
              "Sensor": { "Type": "Any" },
              "BodyMotion": { "Type": "WanderInCircle", "Radius": 6, "RelativeSpeed": 0.25 }
            }
          ]
        },
        {
          "Weight": 1,
          "Sensor": { "Type": "Any" },
          "BodyMotion": { "Type": "WanderInCircle", "Radius": 6, "RelativeSpeed": 0.25 }
        }
      ]
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
| `Instructions` | (top-level) | A single `Type: Random` node that re-rolls between the orbit and wander sub-instructions every `ExecuteFor` seconds. |

### Instruction nodes

A single **`Type: Random`** node owns the behavior. Each time its timer expires it picks one
sub-instruction (weighted) and commits to it for the next random window:

- **`ExecuteFor: [5, 12]`** — the random dwell time: the chosen mode runs for a random number of
  seconds in `[min, max]` before the node re-rolls. Use `[8, 8]` for a fixed dwell, or widen the
  range for more erratic switching.
- **`Continue: true`** — lets later top-level instructions (if any are added) still run in the same
  tick; harmless here with a single node.

The two sub-instructions (equal `Weight: 1`, so a ~50/50 split — bias the annoying branch's weight up to
make it annoying more often):

1. **Annoying** — `Sensor: Any` (always matches; see the note below) wrapping a nested fallthrough:
   a `Player` sensor (`Range 30`, `LockOnTarget`) feeds [`Inkwell_Orbit`](../orbit-body-motion.md)
   (circles at `Radius 2.5`, `RelativeSpeed 0.95`); if no player is in range, it falls through to
   `WanderInCircle` instead of standing still.
2. **Default (wander)** — `Sensor: Any` → `WanderInCircle`, the ordinary livestock-style milling about.

> **Why the annoying branch is wrapped in `Sensor: Any`:** `InstructionRandomized` commits to a picked
> branch for the whole `ExecuteFor` window, and each tick it only runs that branch if its (top-level)
> sensor matches. A bare `Player` sensor there would mean a window rolled while no player is in range
> does *nothing* — the chicken freezes until the next re-roll. Making the branch's own sensor `Any`
> guarantees it always runs; the orbit-or-wander decision then happens in the nested `Instructions`
> fallthrough, so the chicken wanders (never idles) when there's no one to pester.

## Spawning / testing

Role id = filename without extension = `Inkwell_Chicken_Annoying`. Spawn it in-game with the built-in
NPC spawn command (`NPCSpawnCommand`; confirm exact syntax via `/help`), e.g. spawn it a few blocks
away and watch it orbit you as you move.

## Related

- [`Inkwell_Orbit` body motion](../orbit-body-motion.md) — the reusable circling primitive it uses
