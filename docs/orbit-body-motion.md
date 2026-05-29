# `Inkwell_Orbit` — orbit-the-target NPC body motion

A custom NPC **body motion** that makes an entity continuously circle the target supplied by its
paired sensor — moving tangentially around it while correcting back toward a fixed radius, so it
holds a stable orbit and re-converges if knocked off course.

- **Java:** `inkthorne.inkwell.npc.BodyMotionOrbit` (+ `BuilderBodyMotionOrbit`)
- **Registered as:** `Inkwell_Orbit` (in `InkwellPlugin.setup()`, via `NPCPlugin.get().registerCoreComponentType(...)`)

## Why it exists

Hytale's built-in target-relative motions can't express a clean continuous orbit: `MaintainDistance`
only *strafes* intermittently (duration/frequency bursts), and `WanderInCircle` wanders within a
radius of the NPC itself, not around a target. `Inkwell_Orbit` produces a true tangential orbit each
tick. Like all body motions it integrates with the engine's pathing/collision-avoidance (it writes a
steering vector, it doesn't override velocity).

## Usage

`Inkwell_Orbit` reads its target from the **paired `Sensor`** in the same instruction node — pair it
with a `Player` (or `Target`) sensor. If the sensor finds nothing, the motion is inactive (so a later
fallback node, e.g. `WanderInCircle`, can take over).

```json
{
  "Sensor": { "Type": "Player", "Range": 30, "LockOnTarget": true },
  "BodyMotion": {
    "Type": "Inkwell_Orbit",
    "Radius": 2.5,
    "RelativeSpeed": 0.95,
    "RadiusGain": 0.6,
    "Clockwise": true
  }
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `Radius` | double | `3.0` | Distance (blocks) to orbit the target at. Must be `> 0`. |
| `RelativeSpeed` | double | `0.7` | Orbit speed as a fraction of the NPC's max speed (`0 < x ≤ 1`). |
| `RadiusGain` | double | `0.6` | How strongly the NPC corrects back toward `Radius` (`≥ 0`; `0` = pure tangent, no radius hold). Higher converges tighter but can look twitchy. |
| `Clockwise` | bool | `true` | Orbit direction, viewed from above. |

## Behavior notes

- **Target source:** the motion only moves when the node's sensor provides a position.
- **Facing:** it sets no explicit yaw — like the engine's `MoveAway`/`Find` motions, the locomotion
  controller orients the NPC to its movement direction, so it runs forwards around the orbit.
- **Reusable:** any NPC role can use it, not just the chicken — it's a library primitive like
  [`Inkwell_AoeEffect`](aoe-effect-interaction.md).

## Used by

- [`Inkwell_Role_Chicken_Annoying`](assets/inkwell-role-chicken-annoying.md) — a chicken that endlessly circles the nearest player
