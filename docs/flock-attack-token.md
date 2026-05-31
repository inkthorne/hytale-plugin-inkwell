# `Inkwell_FlockAttackToken` — one-attacker-at-a-time NPC sensor

A custom NPC **sensor** that is `true` for **exactly one member of a group at a time**. It hands a
shared "attack token" to one NPC for a fixed interval, then rotates it to a different member. Gate an
attack branch on it and only the token-holder engages while the rest hold back — a hard, AI-layer
guarantee that a swarm presents a single, identifiable attacker rather than a dogpile.

- **Java:** `inkthorne.inkwell.npc.SensorFlockAttackToken` (+ `BuilderSensorFlockAttackToken`)
- **Registered as:** `Inkwell_FlockAttackToken` (in `InkwellPlugin.setup()`, via `NPCPlugin.get().registerCoreComponentType(...)`)

## Why it exists

The engine's native `CombatTurns` system *moves* non-attackers away but can't **guarantee** one
attacker: the predator template's attack instruction is gated on range only, so any NPC that ends up in
range still swings. Cancelling the `Damage` event can't help either — by then the swing already
happened. The only place to hard-gate "who may attack" is the **attack decision** in the role's own
instructions, which needs a shared, cross-NPC signal. This sensor is that signal.

## Usage

Pair it (via `And`) with the sensor that supplies the target (e.g. `Player`), so the holder's branch
both knows it may attack *and* has a target to chase. Non-holders fall through to a hold-back branch.

```jsonc
{
  "Sensor": {
    "Type": "And",
    "Sensors": [
      { "Type": "Player", "Range": 30, "LockOnTarget": true },
      { "Type": "Inkwell_FlockAttackToken" }
    ]
  },
  "Instructions": [ /* approach + attack (token holder only) */ ]
}
```

It takes **no parameters** — the interval and rotation are constants in the Java (see Tuning).

## Behavior

- **Grouping:** by **engine flock id** when the NPC is in a flock (so each pack coordinates
  independently — spawn packs with `/npc spawn … --flock=N`); otherwise by **role id** (all NPCs of the
  same role share one token). So flocked or not, a swarm always has one attacker.
- **Holder identity** is tracked by `UUIDComponent` (stable); `Ref`s are index handles and get reused,
  so they can't be compared across ticks.
- **Rotation:** a rat that just held can't re-take the token for a cooldown (`REHOLD_COOLDOWN_MILLIS`,
  ~2–3 turns), which forces the turn through the pack instead of one rat (e.g. the flock leader)
  re-grabbing it every window. An anti-deadlock fallback (`OPEN_TOO_LONG_MILLIS`) drops the cooldown if
  the token sits unclaimed too long (small pack, everyone on cooldown).
- **State** is process-global and atomic per group (`ConcurrentHashMap.compute`).
- **Pure gate:** `getSensorInfo()` returns `null` — it supplies no target of its own (the paired
  `Player`/`Target` sensor does).

### Tuning (Java constants in `SensorFlockAttackToken`)

| Constant | Default | Effect |
|---|---|---|
| `INTERVAL_MILLIS` | `5000` | How long one member holds the turn before it can rotate. |
| `REHOLD_COOLDOWN_MILLIS` | `12000` | How long a member must wait before holding again (drives rotation breadth). |
| `OPEN_TOO_LONG_MILLIS` | `1500` | If the token sits free this long, ignore the cooldown (anti-stall). |

## Used by

- [`Inkwell_Role_Pack_Rat`](assets/inkwell-role-pack-rat.md) — a rat that swarms in coordinated,
  rotating hit-and-run turns.
