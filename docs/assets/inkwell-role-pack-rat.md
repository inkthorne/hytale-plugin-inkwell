# Asset: `Inkwell_Role_Pack_Rat`

**Type:** NPC role · **File:** `Server/NPC/Roles/Creature/Vermin/Inkwell_Role_Pack_Rat.json`

A vanilla **Rat** that hunts as a **coordinated swarm**: the pack surrounds you, and **one rat at a
time** darts in, bites once, and peels back out — then a *different* rat takes the next turn. The player
always faces a single, identifiable attacker (you know who to block), not a dogpile.

It reuses the vanilla Rat's appearance and bite, but defines its own AI. It is a **`Generic` role, not a
`Variant`** — a `Variant`'s `Modify` block can't carry an inline `Instructions` array, and this role's
whole point is custom combat instructions (same reason the [Annoying Chicken](inkwell-role-chicken-annoying.md)
is `Generic`).

## How it works

The coordination is a custom sensor, [`Inkwell_FlockAttackToken`](../flock-attack-token.md), that is
`true` for exactly **one** flock member at a time (a shared "attack token", held ~5 s, then rotated). The
combat instruction tree is gated on it:

1. **Token holder** (`And[Player, Inkwell_FlockAttackToken]`):
   - **not yet in bite range** → `Seek` rushes in;
   - **in bite range** (`Player Range 2.4`) → face the target, **bite once**, then raise a per-rat
     `Inkwell_PackRat_Bit` flag;
   - **already bit this turn** (flag set) → `MaintainDistance` **retreats** to the 5–7 block ring
     (hit-and-run). It stays the token-holder for the rest of the window, so no one else engages early.
2. **Non-holders** → `MaintainDistance` hold/strafe at 5–7 blocks (out of bite range, so they never
   swing) and clear the `Bit` flag so their next turn bites fresh.
3. **No player** → wander.

### Two details that took iterating to get right

- **Gate the attack on bite range, not just on holding the token.** If the holder fires `Attack` the
  instant it grabs the token, a far holder swings at air and burns its turn. The in-range (`Player 2.4`)
  sub-gate makes it approach first.
- **Hold position through the swing before retreating.** The `Attack` action only *starts* the
  `Rat_Bite` chain (~0.4 s; damage lands mid-way). The bite branch is `ActionsBlocking` with
  `[Attack, Timeout 0.45s, SetFlag Bit]` — the `Timeout` keeps it in range/facing until the hit
  connects, *then* it flips to retreat. Without it the rat pulls back mid-swing and whiffs.

## Fields

| Field | Value | Notes |
|-------|-------|-------|
| `Type` | `Generic` | Self-contained role carrying inline `Instructions` (a `Variant` can't). |
| `Appearance` / bite | `Rat` / `Rat_Bite` + `Rat_Bite_Damage` | Reused vanilla ids; bite bound via top-level `InteractionVars` (`Melee_Start`/`Bite_Damage`), 4 physical. |
| `FlockAllowedNPC` / `FlockCanLead` | `["Inkwell_Role_Pack_Rat"]` / `true` | Lets `/npc spawn … --flock=N` form a shared flock so each pack coordinates on its own token. |
| `ApplySeparation` | `true` | Crowd separation so the waiting ring doesn't stack. |
| combat `Instructions` | (inline) | Token-gated approach → bite → retreat; non-holders hold; else wander. |

### Tuning knobs

- **Turn cadence & rotation** live in the sensor's Java constants — see
  [`Inkwell_FlockAttackToken` → Tuning](../flock-attack-token.md#tuning-java-constants-in-sensorflockattacktoken)
  (`INTERVAL_MILLIS`, `REHOLD_COOLDOWN_MILLIS`).
- **`Timeout` `[0.45,0.45]`** in the bite branch — how long it commits to the strike before peeling off.
  Longer = more "stays to bite"; too short = whiffs.
- **`MaintainDistance.DesiredDistanceRange` `[5,7]`** — how far the ring (and the post-bite retreat) sits.
- **`Bite_Damage` `4` physical** — per-bite damage (low; it's a swarm).

## Spawning / testing

**Spawn pre-flocked** so the pack shares one token (see [server command notes](#) below):

```
/npc spawn Inkwell_Role_Pack_Rat --count=1 --flock=5
```

> **`--count` × `--flock` multiply.** `--count` is the number of *flocks*; each spawns `--flock` members.
> So `--count=1 --flock=5` = one pack of 5. (`--count=5 --flock=5` = 25, in 5 packs.) Optional args use
> `--name=value`; clear leftovers with `/npc clean` (no role filter) or `/inkwell killrole Inkwell_Role_Pack_Rat`.

Then let a pack swarm you and confirm: one rat darts in, bites, retreats; a *different* rat takes the next
turn (~5 s); the rest circle at distance. The plugin's combat-log (server log, `[CombatLog]` lines) shows
each landed hit with the attacker's flock membership — handy for confirming the cadence and rotation.

> **Why not native flocking?** The engine's `CombatTurns` only *moves* non-attackers away; any rat left in
> range still swings, so it can't guarantee a single attacker. Hard-gating the attack decision (this role)
> is the only way to get "one rat at a time." See [`Inkwell_FlockAttackToken`](../flock-attack-token.md).

## Related

- [`Inkwell_FlockAttackToken`](../flock-attack-token.md) — the one-attacker-at-a-time sensor this role is built on
- [Annoying Chicken](inkwell-role-chicken-annoying.md) — the other Inkwell creature (custom AI via `Inkwell_Orbit`)
