# Asset: `Inkwell_Role_Pack_Rat`

**Type:** NPC role · **File:** `Server/NPC/Roles/Creature/Vermin/Inkwell_Role_Pack_Rat.json`

A vanilla **Rat** that hunts as a **coordinated swarm that forms on contact**. Rats wander solo; when one
spots you it **rallies a pack** of nearby rats, and that pack attacks as a unit — the ring surrounds you and
**one rat at a time** darts in, bites once, and peels back out, then a *different* rat takes the next turn.
You always face a single, identifiable attacker (you know who to block), not a dogpile. Lose the pack and it
**disperses**, free to re-form on the next encounter.

It reuses the vanilla Rat's appearance and bite but defines its own AI. It is a **`Generic` role, not a
`Variant`** — a `Variant`'s `Modify` can't carry an inline `Instructions` array, and this role's whole point
is custom combat instructions (same reason the [Annoying Chicken](inkwell-role-chicken-annoying.md) is `Generic`).

## How it works

Two custom primitives plus the engine's flock system do the work:

1. **Form the pack — [`Inkwell_RecruitFlock`](../recruit-flock-action.md).** When a not-yet-flocked rat senses
   a player (within ~18), this action creates an engine flock, recruits up to **`FlockSize`-1** same-role rats
   within **`FlockRadius`**, and locks the player as **every** member's target — so even recruits that never
   saw you converge. It enforces **one flock per (role, target)**: other rats that aggro the same player join
   that one pack (if there's room) or stay out, rather than stacking extra packs.
2. **Take turns — [`Inkwell_FlockAttackToken`](../flock-attack-token.md).** A sensor that's `true` for exactly
   one flock member at a time (a shared attack token, held ~5 s, then rotated). The combat tree is gated on it.
3. **The engine flock** gives leader election + **succession on leader death**, the join cap, and
   dissolve-when-small for free.

Combat is keyed on the **marked target** (a `Target` sensor reading the `LockedTarget` the recruit action set),
**not** a fresh `Player` sense — so a recruit with no line of sight still chases and attacks. The role's phases:

- **Aggro (not in a flock):** `Player` within 18 → run `Inkwell_RecruitFlock`.
- **In a flock:** driven by the marked `Target`:
  - **token holder** → rush in; **bite once when in range** (`Target Range 2.4`), holding through the swing,
    then **retreat** to the 5–7 ring via a per-rat `Inkwell_PackRat_Bit` flag (hit-and-run).
  - **non-holder** → [`Inkwell_MaintainDistance`](../maintain-distance-body-motion.md) (`FaceMovementDirection`)
    at 5–7, waiting its turn (out of bite range, never swings).
  - **disperse** → a `Disband` alarm is re-armed every tick the target is within leash (35); once the target
    is lost it `Passes` ~5 s later and the rat `LeaveFlock`s. Below 2 members the engine dissolves the flock.
- **No flock, no target:** wander.

### Details that took iterating to get right

- **Gate the bite on range, not just on holding the token** — else a far holder swings on the tick it grabs
  the token, whiffs, and burns its turn approaching.
- **Hold through the swing before retreating** — `Attack` only *starts* the ~0.4 s `Rat_Bite` chain, so the
  bite branch is `ActionsBlocking` `[Attack, Timeout 0.45s, SetFlag Bit]`; without the `Timeout` it pulls back
  mid-swing and misses.
- **`createFlock`/`join` populate the member group a tick late**, so the recruit action validates a flock by
  its `Flock` component (not member count) and caps pack size with its own counter — see the
  [`Inkwell_RecruitFlock`](../recruit-flock-action.md) notes.

## Fields

| Field | Value | Notes |
|-------|-------|-------|
| `Type` | `Generic` | Self-contained role carrying inline `Instructions` (a `Variant` can't). |
| `Appearance` / bite | `Rat` / `Rat_Bite` + `Rat_Bite_Damage` | Reused vanilla ids; bite bound via top-level `InteractionVars`, 4 physical. |
| `FlockAllowedNPC` / `FlockCanLead` | `["Inkwell_Role_Pack_Rat"]` / `true` | Lets the recruit action create/join an engine flock of this role. |
| `ApplySeparation` | `true` | Crowd separation so the waiting ring doesn't stack. |
| recruit action | `{ "Type": "Inkwell_RecruitFlock", "FlockSize": 5, "FlockRadius": 16 }` | **Where pack size & recruit radius are set.** |

### Tuning knobs

- **`FlockSize` / `FlockRadius`** (on the recruit action, role JSON) — max pack size (incl. the rat that
  aggro'd) and how far it looks for pack-mates. `FlockSize` works up to the engine flock cap (~8).
- **Aggro range** — the `Player` sensor `Range` (18) on the recruit instruction.
- **Disperse grace** — the `SetAlarm … "PT5S"` duration; leash = the combat `Target` `Range` (35).
- **Turn cadence & rotation** — Java constants in [`Inkwell_FlockAttackToken`](../flock-attack-token.md#tuning-java-constants-in-sensorflockattacktoken).
- **`Timeout` `[0.45,0.45]`** — how long it commits to the strike before peeling off.
- **`Bite_Damage` `4` physical** — per-bite damage (low; it's a swarm).

## Spawning / testing

No special flock spawn needed — packs form on contact. Scatter some loose rats and walk into them:

```
/npc spawn Inkwell_Role_Pack_Rat --count=8 --radius=6
```

> Optional args use `--name=value`. Clear leftovers with `/npc clean` (all NPCs) or
> `/inkwell killrole Inkwell_Role_Pack_Rat` (just these). Spawning *with* `--flock=N` still works and just
> pre-seeds one engine flock, but it's no longer required.

Walk up to one rat: it should rally a single pack of up to `FlockSize` (including rats too far to have seen
you), swarm in the rotating one-at-a-time pattern, and disperse a few seconds after you escape — then re-form
on the next approach. The plugin's combat log (server log, `[CombatLog]` lines) shows each landed hit with its
flock id; a single shared id (rotating across members) confirms one coordinated pack.

> **Why not native flocking?** The engine's `CombatTurns` only *moves* non-attackers away; any rat left in
> range still swings, so it can't guarantee a single attacker. Hard-gating the attack decision (this role) is
> the only way to get "one rat at a time." See [`Inkwell_FlockAttackToken`](../flock-attack-token.md).

## Related

- [`Inkwell_RecruitFlock`](../recruit-flock-action.md) — the runtime pack-formation action
- [`Inkwell_FlockAttackToken`](../flock-attack-token.md) — the one-attacker-at-a-time sensor
- [`Inkwell_MaintainDistance`](../maintain-distance-body-motion.md) — the ring/retreat motion (faces its movement direction, not the target)
- [Annoying Chicken](inkwell-role-chicken-annoying.md) — the other Inkwell creature (custom AI via `Inkwell_Orbit`)
