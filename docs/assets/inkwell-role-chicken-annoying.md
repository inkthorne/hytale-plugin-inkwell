# Asset: `Inkwell_Role_Chicken_Annoying`

**Type:** NPC role · **File:** `Server/NPC/Roles/Creature/Livestock/Inkwell_Role_Chicken_Annoying.json`

A chicken with **mood swings**: a built-in timer randomly commits it to **orbiting** the nearest player
("annoying") or ordinary **wandering**, for a random number of seconds, then re-rolls. It reuses the
vanilla chicken's appearance but defines its own AI — built from the
[`Inkwell_Orbit`](../orbit-body-motion.md) body motion — instead of the stock flee/flock/graze behavior.

The twist: **on an orbit → wander transition, it pecks once before settling down.** Leaving the orbit, it
darts in, jabs for a little damage (2 physical, with a small knockback nudge), then wanders off. The peck
is tied to that *transition* — not a cooldown — so it happens exactly once per orbit→wander switch. See
[Orbit → wander peck](#orbit--wander-peck).

The whole thing is **built-in instruction primitives only** (`Type: Random`, the `Flag` sensor /
`SetFlag` action, and `Attack`) — no custom code beyond the `Inkwell_Orbit` body motion.

It is a **self-contained `Generic` role, not a `Variant`.** A `Variant`'s `Modify` block runs each
value through the NPC expression system, which rejects a structural `Instructions` array (`Illegal
JSON value for expression`). So custom behavior must be defined at the role's top level, which is what
`Generic` roles (e.g. the engine's own `Test_*` roles) do.

## How it works

One **`Type: Random`** node with `ExecuteFor: [5, 12]` is the "mode timer": it picks one of its two
weighted branches (ORBIT / WANDER), commits to it for a random 5–12 s window, then re-rolls (~50/50).

The orbit→wander transition is detected with a **per-NPC boolean flag, `WasOrbiting`**:

- The **ORBIT branch** raises `WasOrbiting` (`SetFlag … SetTo: true`) on every tick it is *actively
  orbiting a player*. (With no player in range it wanders instead and leaves the flag down — so ending a
  player-less "orbit" window doesn't trigger a peck.)
- The **WANDER branch** checks `WasOrbiting` on the tick it takes over. If it's set **and** a player is in
  reach, it runs one peck and clears the flag. Otherwise it just clears the flag (so a player wandering
  into range mid-window can't trigger a stray peck) and wanders.

Because the peck lives in a single `ActionsBlocking` sequence that ends by clearing `WasOrbiting`, it
fires **exactly once** per transition and cannot repeat.

## Orbit → wander peck

**Damage — `InteractionVars` override.** The peck's `Attack` (`Root_NPC_Attack_Melee`) runs the stock NPC
melee chain `Melee_Start → NPC_Attack_Melee_Simple → selector → Melee_Damage`. The selector's `HitEntity`
applies whatever interaction is bound to the **`Melee_Damage`** var (default `NPC_Attack_Melee_Damage`).
We override just `Melee_Damage` at the role's top-level `InteractionVars` to drop the damage to a
peck-sized **2 physical** (vanilla is 5); the inherited `DamageEffects` (a light knockback + the
unarmed-impact sound and `Impact_Sword_Basic` particle) read perfectly as a peck. `Melee_Start` is left
at its default — the same mechanism the vanilla **undead chicken** uses to bite.

```json
"InteractionVars": {
  "Melee_Damage": {
    "Interactions": [
      { "Parent": "NPC_Attack_Melee_Damage",
        "DamageCalculator": { "Type": "Absolute", "BaseDamage": { "Physical": 2 }, "RandomPercentageModifier": 0.1 } }
    ]
  }
}
```

**Trigger — the WANDER branch's first sub-instruction**, gated on the flag and a player:

```json
{
  "Sensor": {
    "Type": "And",
    "Sensors": [
      { "Type": "Flag", "Name": "WasOrbiting", "Set": true },
      { "Type": "Player", "Range": 6, "LockOnTarget": true }
    ]
  },
  "BodyMotion": { "Type": "Seek", "StopDistance": 1.8, "SlowDownDistance": 2.5, "RelativeSpeed": 0.6 },
  "HeadMotion": { "Type": "Watch" },
  "ActionsBlocking": true,
  "Actions": [
    { "Type": "PlayAnimation", "Slot": "Status", "Animation": "Eat" },
    { "Type": "Timeout", "Delay": [ 0.6, 0.6 ] },
    { "Type": "Attack", "Attack": "Root_NPC_Attack_Melee", "AttackPauseRange": [ 0, 0 ] },
    { "Type": "PlayAnimation", "Slot": "Status" },
    { "Type": "SetFlag", "Name": "WasOrbiting", "SetTo": false }
  ]
}
```

Why the structure:

- **One peck, guaranteed.** `Attack` is one-shot (verified against the engine: it aims, strikes once,
  returns done), the blocking `ActionList` persists its index and runs through to the final `SetFlag`,
  and that `SetFlag` drops `WasOrbiting` so the branch's own sensor no longer matches. No cooldown
  bookkeeping, no repeat.
- **The `Seek` is not optional.** While orbiting, the chicken's body faces its *tangential* heading, not
  the player; the melee selector (a narrow ~30° swept arc) sweeps *in front of the body*, so without
  turning to face the player first the peck whiffs. The `Seek` rotates it onto the target during the
  windup — and the windup is **0.6 s** (not a snappier 0.35 s) specifically to give it time to swing
  ~90° around onto you; too short and it fires under-rotated and misses. It can still miss if you dodge.
- **`AttackPauseRange [0,0]`** — the pause would only matter if the action were re-entered; the flag
  guarantees it isn't, so there's nothing to throttle.

> **`Flag` sensor / `SetFlag` action.** A per-NPC named boolean. Sensor: `{ "Type": "Flag", "Name": …,
> "Set": true|false }`. Action: `{ "Type": "SetFlag", "Name": …, "SetTo": true|false }`. (Note the
> different key — `Set` to test, `SetTo` to write.)

## Fields

| Field | Value | Notes |
|-------|-------|-------|
| `Type` | `Generic` | Concrete, spawnable, self-contained role (vs. `Variant`, which references a template and can't carry an inline `Instructions` array). |
| `Appearance` | `Chicken` | Reuses the vanilla chicken model/texture/animations. A **referenced vanilla id keeps its name** — no `Inkwell_` prefix (the prefix is only for ids *we* define). |
| `MotionControllerList` | `Walk`, `MaxWalkSpeed 7` | Faster than a normal chicken (≈5) so it keeps up while circling. `MaxWalkSpeed` is what `Inkwell_Orbit`'s `RelativeSpeed` scales against. |
| `MaxHealth` | `12` (via `Parameters`) | Frail — it's a nuisance, not a threat. |
| `InteractionVars.Melee_Damage` | 2 physical | Peck damage override (see above). |
| `Instructions` | (top-level) | A single `Type: Random` "mode timer" with ORBIT and WANDER branches. |

### Tuning knobs

- **`ExecuteFor: [5, 12]`** — the random dwell time per mode before re-rolling. Use `[8, 8]` for a fixed
  cadence, or widen it for more erratic switching. Each re-roll is an independent ~50/50 pick, so
  orbit→wander transitions (and thus pecks) happen on average every couple of windows.
- **Branch `Weight`s** (both `1`) — bias toward orbiting (more pecks, more annoying) or wandering.
- **Peck `Player.Range` (`6`)** — how close the player must be when the mode flips for a peck to land.
- **`Inkwell_Orbit.Radius` (`2.5`)** — how tightly it circles; the melee selector reaches ≈3.5, so this
  keeps the player within peck distance during orbit.

> **Why each branch is wrapped in `Sensor: Any`:** the `Random` node commits to a picked branch for the
> whole `ExecuteFor` window and each tick only runs it if its (top-level) sensor matches. A bare `Player`
> sensor there would freeze the chicken for the window if no player were in range. `Any` guarantees the
> branch runs; the orbit-or-wander / peck-or-not decisions then happen in the nested `Instructions`.

## Spawning / testing

Role id = filename without extension = `Inkwell_Role_Chicken_Annoying`. Spawn it in-game with the built-in
NPC spawn command (`NPCSpawnCommand`; confirm exact syntax via `/help`), e.g. spawn it a few blocks away,
let it orbit you, and watch for a single peck each time it breaks off into a wander.

## Related

- [`Inkwell_Orbit` body motion](../orbit-body-motion.md) — the reusable circling primitive it uses
