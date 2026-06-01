package inkthorne.inkwell.npc;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;

/**
 * When a flock member dies, free its reserved slot in the owning {@link ActionRecruitFlock} pack so the pack
 * refills on the next roaming same-role aggro instead of dwindling. This is the core counterpart to the
 * join-time reservation in {@code ActionRecruitFlock#tryJoin}: that counter is the sole source of truth for
 * pack capacity (the engine's flock group count lags a tick), so departures have to be reported explicitly.
 *
 * <p>Distinct from the debug {@code DeathLogSystem} (which only writes a DIED log line): this is real mechanic
 * and must always run, so it lives here in core rather than under {@code debug}. {@link #noteMemberDeparted}
 * no-ops for any flock Inkwell didn't form, so observing every flock-member death is harmless.
 */
public final class FlockMemberDeathSystem extends DeathSystems.OnDeathSystem {

    @Override
    public Query<EntityStore> getQuery() {
        // Only flock members carry a FlockMembership, so this fires exactly for the deaths we care about.
        return FlockMembership.getComponentType();
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> dead, DeathComponent death, Store<EntityStore> store,
                                 CommandBuffer<EntityStore> buffer) {
        FlockMembership membership = store.getComponent(dead, FlockMembership.getComponentType());
        if (membership != null) {
            ActionRecruitFlock.noteMemberDeparted(membership.getFlockId());
        }
    }
}
