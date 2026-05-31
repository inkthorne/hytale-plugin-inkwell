package inkthorne.inkwell.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

/**
 * {@code Inkwell_FlockAttackToken} — a coordination sensor that is true for <b>exactly one</b> member of a
 * group at a time. It hands a shared "attack token" to one NPC for {@link #INTERVAL_MILLIS} ms, then rotates
 * it. Gate an attack branch on it (via {@code And} with a {@code Player} sensor) and only the token-holder
 * approaches + bites while the rest hold back — the hard, AI-layer guarantee that the player always faces a
 * single, identifiable attacker.
 *
 * <p>Grouping: by engine flock id when the NPC is in a flock (so each pack coordinates independently —
 * spawn with {@code /npc spawn … --flock=N}); otherwise by role id (all same-role NPCs share one token).
 * Holder identity is tracked by {@link UUIDComponent} (stable; {@code Ref}s are index handles and get
 * reused). State is process-global and atomic per group via {@link ConcurrentHashMap#compute}.
 *
 * <p>Registered in {@code InkwellPlugin.setup()} so role JSON can reference it as
 * {@code "Sensor": { "Type": "Inkwell_FlockAttackToken" }}. Pure gate: {@link #getSensorInfo()} returns null
 * (it supplies no target — the paired {@code Player} sensor does).
 */
public class SensorFlockAttackToken extends SensorBase {

    /** How long one holder keeps the token before it can rotate to another member. */
    private static final long INTERVAL_MILLIS = 5_000L;

    /** A rat that just held won't take the token again for this long — forces the turn to rotate through the
     * pack rather than the same (e.g. leader) rat re-grabbing it every window. ~2-3 turns. */
    private static final long REHOLD_COOLDOWN_MILLIS = 12_000L;

    /** Anti-deadlock: if the token has sat free this long (everyone eligible held too recently — small pack),
     * drop the cooldown and let whoever's asking take it, rather than stall with no attacker. */
    private static final long OPEN_TOO_LONG_MILLIS = 1_500L;

    /** Forget a rat's last-held time after this long (keeps the per-group history from growing unbounded). */
    private static final long LAST_HELD_PRUNE_MILLIS = 60_000L;

    /** groupKey -> current token state. Process-global so all members of a group share one token. */
    private static final ConcurrentHashMap<String, TokenState> TOKENS = new ConcurrentHashMap<>();

    public SensorFlockAttackToken(BuilderSensorFlockAttackToken builder, BuilderSupport support) {
        super(builder);
    }

    @Override
    public boolean matches(Ref<EntityStore> self, Role role, double dt, Store<EntityStore> store) {
        UUIDComponent uuid = store.getComponent(self, UUIDComponent.getComponentType());
        if (uuid == null || uuid.getUuid() == null) {
            return true; // no stable id to coordinate on — don't gate this NPC
        }
        UUID myId = uuid.getUuid();
        String key = groupKey(self, store);
        long now = System.currentTimeMillis();

        boolean[] hold = {false};
        TOKENS.compute(key, (k, prev) -> {
            TokenState state = (prev != null) ? prev : new TokenState();

            if (state.holder != null && now < state.expiry) {       // a holder is active this window
                if (myId.equals(state.holder)) {
                    hold[0] = true;
                }
                return state;
            }

            // Token is free (never held, or window expired). Skip it if I held it too recently — unless it's
            // been sitting free too long (small pack, everyone on cooldown), in which case take it anyway.
            long sinceMine = now - state.lastHeld.getOrDefault(myId, 0L);
            long freeFor = (state.holder == null) ? Long.MAX_VALUE : (now - state.expiry);
            if (sinceMine < REHOLD_COOLDOWN_MILLIS && freeFor < OPEN_TOO_LONG_MILLIS) {
                return state;                                       // defer; give a less-recent member the turn
            }

            state.holder = myId;
            state.expiry = now + INTERVAL_MILLIS;
            state.lastHeld.put(myId, now);
            state.lastHeld.values().removeIf(t -> (now - t) > LAST_HELD_PRUNE_MILLIS);
            hold[0] = true;
            return state;
        });
        return hold[0];
    }

    @Override
    public InfoProvider getSensorInfo() {
        return null; // pure gate: provides no target (pair with a Player sensor via And)
    }

    /** Coordination group: the flock if flocked, else all NPCs of the same role. */
    private static String groupKey(Ref<EntityStore> self, Store<EntityStore> store) {
        FlockMembership flock = store.getComponent(self, FlockMembership.getComponentType());
        if (flock != null && flock.getFlockId() != null) {
            return "flock:" + flock.getFlockId();
        }
        NPCEntity npc = store.getComponent(self, NPCEntity.getComponentType());
        return "role:" + (npc != null ? npc.getRoleName() : "?");
    }

    /** Mutated only inside {@link ConcurrentHashMap#compute}, so updates are atomic per group. */
    private static final class TokenState {
        UUID holder;
        long expiry;
        /** When each rat last held the token, for the re-hold cooldown that drives rotation. */
        final Map<UUID, Long> lastHeld = new HashMap<>();
    }
}
