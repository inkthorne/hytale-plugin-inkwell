# `Inkwell_RecruitFlock` — runtime aggro-triggered flock formation

A custom NPC **action** that forms a flock **at runtime** when an NPC aggros: the NPC that spots a target
rallies nearby same-role NPCs into a single pack and locks the target on all of them. This is how a swarm
forms *on contact* instead of being fixed at spawn time.

- **Java:** `inkthorne.inkwell.npc.ActionRecruitFlock` (+ `BuilderActionRecruitFlock`)
- **Registered as:** `Inkwell_RecruitFlock` (in `InkwellPlugin.setup()`, via `NPCPlugin.get().registerCoreComponentType(...)`)

## Why it exists

The engine forms flocks at **spawn time** (worldgen / `/npc spawn --flock=N`); hand-placed or lone NPCs never
auto-flock. This action lets a lone NPC build a pack dynamically the moment it aggros — and reuses the engine
flock so leader election, succession-on-death, the join cap, target propagation, and dissolve-when-small all
come for free.

## Usage

Gate it (in role JSON) on a `Player` sensor (which supplies the target) **and** "not already in a flock", so it
fires once when a lone NPC first spots a target:

```jsonc
{
  "Continue": true,
  "Sensor": {
    "Type": "And",
    "Sensors": [
      { "Type": "Player", "Range": 18, "LockOnTarget": true },
      { "Type": "Self", "Filters": [ { "Type": "Flock", "FlockStatus": "NotMember" } ] }
    ]
  },
  "Actions": [ { "Type": "Inkwell_RecruitFlock", "FlockSize": 5, "FlockRadius": 16 } ]
}
```

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `FlockSize` | int | `5` | Max pack size, **including** the NPC that aggro'd. Capped by the engine flock max (~8). |
| `FlockRadius` | double | `16` | How far (blocks) the aggro'd NPC looks to recruit pack-mates. |

After it runs, drive combat off the marked target (a `Target` sensor on `LockedTarget`), not a fresh `Player`
sense — so recruits with no line of sight still engage. Pair with
[`Inkwell_FlockAttackToken`](flock-attack-token.md) to serialise the attack.

## Behavior

- **One flock per (role, target).** A process-global registry maps `(roleName, targetUuid)` → its owning
  flock. The first aggro'd NPC creates the flock and recruits; later NPCs that aggro the same target **join
  that one pack** (if there's room) or stay out — independent aggros never stack a second pack on the same
  target. (Different targets, or different roles, get their own flocks.)
- **Recruiting** uses `Selector.selectNearbyEntities` to find same-role, not-yet-flocked NPCs within
  `FlockRadius`, then `FlockMembershipSystems.join`s up to `FlockSize-1` of them.
- **Target lock** is set on every member via `MarkedEntitySupport.setMarkedEntity("LockedTarget", target)` —
  per-rat (not `flockSetTarget`), because a just-joined member isn't in the flock's `EntityGroup` yet.

### Two engine gotchas it works around (worth knowing for any flock code)

- **`createFlock`/`join` populate the flock's `EntityGroup` a tick late** (a deferred system). So a
  just-created flock reports 0 members for the rest of the tick. The action therefore (a) validates a flock by
  its **`Flock` component**, not member count, and (b) caps pack size with its own `AtomicInteger`. Without
  this, every same-tick aggro created its own flock (and size overshot).
- **`FlockPlugin.getFlock(accessor, ref)` expects a flock _member_ ref**, not the flock entity — it reads the
  member's `FlockMembership`. Passing the flock entity always returns null. To test the flock entity itself,
  read its `Flock` component directly.

### Tuning (Java constants in `ActionRecruitFlock`)

Pack size/radius are JSON params (above). The disperse behavior lives in the **role** (a re-armed `Disband`
alarm → `LeaveFlock`), not here.

### Limitation: the size counter only counts up

The per-pack `AtomicInteger` increments on join but **never decrements** (a member dying/leaving is handled by
the engine's deferred systems, which this action can't cheaply observe in-tick). Consequence: **a pack won't
refill after a member dies** — once it hits `FlockSize` it stays "full" to this action until the whole flock
dissolves (registry entry goes stale → next aggro forms a fresh pack). Refill-on-death would require hooking
`FlockMembership` removal to decrement the counter.

## Used by

- [`Inkwell_Role_Pack_Rat`](assets/inkwell-role-pack-rat.md) — rats that rally into a pack on contact.
