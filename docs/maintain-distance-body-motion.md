# `Inkwell_MaintainDistance` — MaintainDistance that can face its movement direction

The engine's `MaintainDistance` body motion **plus a `FaceMovementDirection` option**. Vanilla
`MaintainDistance` locks the NPC's body to **face the target** while it strafes / back-pedals to hold range —
which reads as sidestepping while staring at you. With `FaceMovementDirection: true` the NPC instead orients
to the **direction it's moving** (like `Seek`/`MoveAway`), so it runs/circles facing its travel.

- **Java:** `inkthorne.inkwell.npc.InkwellBodyMotionMaintainDistance` (+ `InkwellBuilderBodyMotionMaintainDistance`)
- **Registered as:** `Inkwell_MaintainDistance` (in `InkwellPlugin.setup()`, via `NPCPlugin.get().registerCoreComponentType(...)`)

## Why it exists

Body facing in Hytale is set by the `BodyMotion`: `MaintainDistance` ends its steering with
`setYaw(targetYaw)` (face the target), whereas `Seek`/`MoveAway`/`WanderInCircle`/`Inkwell_Orbit` leave the
yaw to the locomotion controller (face movement). There's no vanilla toggle to keep `MaintainDistance`'s
distance-holding + strafing behavior *without* the target-locked facing. This subclass adds exactly that one
switch and changes nothing else.

## How it works

It **extends** the engine `BodyMotionMaintainDistance` and defers to `super.computeSteering` for all the
distance / strafing / speed logic, then — only when `FaceMovementDirection` is `true` — rewrites the final
body yaw from the resulting translation vector (using the engine's own
`PhysicsMath.headingFromDirection`/`normalizeTurnAngle`, so the convention matches). With the flag `false`
(default) it is byte-for-byte the vanilla behavior.

## Usage

Drop-in replacement for `MaintainDistance` — same fields, plus `FaceMovementDirection`:

```jsonc
{
  "Sensor": { "Type": "Target", "TargetSlot": "LockedTarget", "Range": 35 },
  "BodyMotion": {
    "Type": "Inkwell_MaintainDistance",
    "FaceMovementDirection": true,
    "DesiredDistanceRange": [ 5, 7 ],
    "StrafingDurationRange": [ 3, 5 ],
    "StrafingFrequencyRange": [ 2, 4 ],
    "RelativeForwardsSpeed": 0.6,
    "RelativeBackwardsSpeed": 0.6
  }
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `FaceMovementDirection` | bool | `false` | `false` = vanilla (body faces the target while strafing). `true` = body faces the movement direction. |
| *(all engine `MaintainDistance` fields)* | — | — | `DesiredDistanceRange`, `RelativeForwardsSpeed`/`RelativeBackwardsSpeed`, `StrafingDurationRange`/`StrafingFrequencyRange`, `TargetDistanceFactor`, `MoveThreshold`, … — unchanged. |

> A paired `HeadMotion: Watch` still turns only the **head** toward the target, independent of body yaw — so
> `FaceMovementDirection: true` + `Watch` reads as "runs facing its path, glancing at you."

## Used by

- [`Inkwell_Role_Pack_Rat`](assets/inkwell-role-pack-rat.md) — the waiting ring and post-bite retreat use it
  with `FaceMovementDirection: true`, so the pack prowls/peels off facing its movement rather than strafing.
