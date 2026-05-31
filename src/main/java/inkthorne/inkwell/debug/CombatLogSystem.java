package inkthorne.inkwell.debug;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * Debug combat log. Hytale writes no combat/damage log of its own, so this system listens to the
 * {@link Damage} ECS event (fired on the <i>victim</i>) and writes one INFO line per hit to the server
 * log: {@code [CombatLog] <attacker> (melee|projectile) -> <victim> | <amount> <cause>}.
 *
 * <p>For NPC participants the line also carries flock membership ({@code [flock=<id8>:<TYPE>]}) — so it
 * doubles as visibility into whether the Pack Rats flocked, and (with the
 * {@code Inkwell_FlockAttackToken} gate doing the real work in the AI layer) lets us confirm only one rat
 * per flock actually lands a hit per interval.
 *
 * <p>Purely observational — it does <b>not</b> alter combat. To stay quiet on servers that merely depend on
 * Inkwell, it logs only combat involving an Inkwell creature (role id prefix {@code Inkwell_}).
 */
public final class CombatLogSystem extends DamageEventSystem {

    private static final String INKWELL_ROLE_PREFIX = "Inkwell_";

    private final HytaleLogger logger;

    public CombatLogSystem(HytaleLogger logger) {
        super();
        this.logger = logger;
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, Damage event) {
        if (!(event.getSource() instanceof Damage.EntitySource entitySource)) {
            return; // skip environment (fall/drowning) and command damage
        }
        Ref<EntityStore> attacker = entitySource.getRef();
        Ref<EntityStore> victim = chunk.getReferenceTo(index);

        if (!involvesInkwell(attacker, store) && !involvesInkwell(victim, store)) {
            return;
        }

        String kind = (event.getSource() instanceof Damage.ProjectileSource) ? "projectile" : "melee";
        String line = "[CombatLog] " + describe(attacker, store) + " (" + kind + ") -> "
            + describe(victim, store) + " | " + String.format("%.1f", event.getAmount())
            + " " + event.getCause().getId();
        logger.atInfo().log(line);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return DamageDataComponent.getComponentType();
    }

    private static boolean involvesInkwell(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null) {
            return false;
        }
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        return npc != null && npc.getRoleName() != null && npc.getRoleName().startsWith(INKWELL_ROLE_PREFIX);
    }

    private static String describe(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null) {
            return "?";
        }
        if (store.getComponent(ref, Player.getComponentType()) != null) {
            return "Player";
        }
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) {
            String role = npc.getRoleName();
            FlockMembership flock = store.getComponent(ref, FlockMembership.getComponentType());
            if (flock != null && flock.getFlockId() != null) {
                String id = flock.getFlockId().toString();
                return role + "[flock=" + id.substring(0, 8) + ":" + flock.getMembershipType() + "]";
            }
            return role + "[flock=none]";
        }
        return "entity";
    }
}
