package inkthorne.inkwell.debug;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * Debug death log: writes a {@code [CombatLog] <role>[flock=…] DIED} line when an Inkwell creature dies, so the
 * combat log carries deaths alongside the hits ({@link CombatLogSystem} logs the damage; this marks the death).
 * Purely observational — a dev aid, like {@link CombatLogSystem} (candidate to gate behind a flag before a
 * stable release).
 */
public final class DeathLogSystem extends DeathSystems.OnDeathSystem {

    private static final String INKWELL_ROLE_PREFIX = "Inkwell_";

    private final HytaleLogger logger;

    public DeathLogSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> dead, DeathComponent death, Store<EntityStore> store,
                                 CommandBuffer<EntityStore> buffer) {
        NPCEntity npc = store.getComponent(dead, NPCEntity.getComponentType());
        if (npc == null || npc.getRoleName() == null || !npc.getRoleName().startsWith(INKWELL_ROLE_PREFIX)) {
            return;
        }
        FlockMembership membership = store.getComponent(dead, FlockMembership.getComponentType());
        String flockTag = (membership != null && membership.getFlockId() != null)
            ? "flock=" + membership.getFlockId().toString().substring(0, 8) : "flock=none";
        logger.atInfo().log("[CombatLog] " + npc.getRoleName() + "[" + flockTag + "] DIED");
    }
}
