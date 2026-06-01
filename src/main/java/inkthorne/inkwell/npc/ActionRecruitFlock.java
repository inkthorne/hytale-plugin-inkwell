package inkthorne.inkwell.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.group.EntityGroup;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.Flock;
import com.hypixel.hytale.server.flock.FlockMembershipSystems;
import com.hypixel.hytale.server.flock.FlockPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import org.joml.Vector3d;

/**
 * {@code Inkwell_RecruitFlock} — a custom NPC action that forms a flock <b>at runtime</b> when an NPC aggros,
 * with <b>exactly one flock per (role, target)</b>.
 *
 * <p>Gate it (in role JSON) on a {@code Player} sensor (supplies the target) AND "not already in a flock". On
 * execute, a process-global registry maps each (role, target) to its one owning flock:
 * <ul>
 *   <li>The first aggro'd NPC creates an engine flock and recruits up to {@code FlockSize-1} same-role NPCs
 *       within {@code FlockRadius}.</li>
 *   <li>Any later/other aggro'd NPC joins that same flock if there's room, or stays out — it never forms a
 *       second pack on the same target.</li>
 * </ul>
 * Every member gets the target locked as its {@code LockedTarget} so recruits that never sensed it still
 * converge. The engine handles leader election/succession, the hard join cap, and dissolve-when-small;
 * {@link SensorFlockAttackToken} (groups by flock id) serialises the attack.
 *
 * <p><b>Why a registry + logical counter rather than reading the flock's member group:</b> the engine
 * populates a flock's {@code EntityGroup} via a deferred system, so right after {@code createFlock}/{@code join}
 * the group still reports 0 members for the rest of the tick. Relying on it made every same-tick aggro create
 * its own flock (and overshoot the size). So we (a) treat a just-created flock as alive without requiring
 * members yet, (b) get-or-create atomically via {@link ConcurrentHashMap#compute}, and (c) cap size with our
 * own {@link AtomicInteger}.
 */
public class ActionRecruitFlock extends ActionBase {

    /** (roleName | targetUuid) -> the one pack currently attacking that target. Self-heals: a dissolved flock
     * fails {@link #isFlockAlive} on lookup and is replaced. Process-global. */
    private static final ConcurrentHashMap<String, Pack> PACKS = new ConcurrentHashMap<>();

    private final int flockSize;
    private final double flockRadius;

    public ActionRecruitFlock(BuilderActionRecruitFlock builder, BuilderSupport support) {
        super(builder);
        this.flockSize = Math.max(1, (int) Math.round(builder.getFlockSize(support)));
        this.flockRadius = builder.getFlockRadius(support);
    }

    @Override
    public boolean execute(Ref<EntityStore> self, Role role, InfoProvider sensorInfo, double dt,
                           Store<EntityStore> store) {
        if (FlockPlugin.isFlockMember(self, store)) {
            return true; // already in a pack
        }
        Ref<EntityStore> target = sensedTarget(sensorInfo);
        if (target == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(self, TransformComponent.getComponentType());
        NPCEntity selfNpc = store.getComponent(self, NPCEntity.getComponentType());
        if (transform == null || selfNpc == null || selfNpc.getRoleName() == null) {
            return false;
        }
        String myRole = selfNpc.getRoleName();
        String key = targetKey(myRole, target, store);
        if (key == null) {
            return true; // no stable target id; skip rather than risk uncoordinated flocks
        }

        // Atomically get-or-create the ONE flock for this (role, target). compute() is atomic per key, so even
        // if several rats aggro the same tick only one flock is created; the rest get the same Ref.
        boolean[] created = {false};
        Pack pack = PACKS.compute(key, (k, prev) -> {
            if (prev != null && isFlockAlive(prev.flockRef, store)) {
                return prev;
            }
            created[0] = true;
            return new Pack(FlockPlugin.createFlock(store, role));
        });

        // Join this pack (counter-capped at FlockSize), then — only if we created it — recruit nearby rats.
        tryJoin(self, pack, target, store);
        if (created[0]) {
            recruitNearby(self, myRole, transform.getPosition(), pack, target, store);
        }
        return true;
    }

    private void recruitNearby(Ref<EntityStore> self, String myRole, Vector3d center, Pack pack,
                               Ref<EntityStore> target, Store<EntityStore> store) {
        // Collect candidates first (don't mutate the store inside the spatial query callback).
        List<Ref<EntityStore>> candidates = new ArrayList<>();
        Predicate<Ref<EntityStore>> filter = ref -> ref != null && ref.isValid() && !ref.equals(self)
            && !FlockPlugin.isFlockMember(ref, store)
            && isSameRole(ref, myRole, store);
        Selector.selectNearbyEntities(store, center, flockRadius, candidates::add, filter);
        for (Ref<EntityStore> ref : candidates) {
            if (!tryJoin(ref, pack, target, store)) {
                break; // pack full
            }
        }
    }

    /** Join {@code ref} to the pack if it's under {@code flockSize}; returns false if the pack is already full. */
    private boolean tryJoin(Ref<EntityStore> ref, Pack pack, Ref<EntityStore> target, Store<EntityStore> store) {
        int n = pack.count.incrementAndGet();
        if (n > flockSize) {
            pack.count.decrementAndGet();
            return false;
        }
        if (FlockPlugin.isFlockMember(ref, store) || !FlockMembershipSystems.canJoinFlock(ref, pack.flockRef, store)) {
            pack.count.decrementAndGet(); // couldn't actually join — give the slot back
            return true;                  // not "full"; keep trying other candidates
        }
        FlockMembershipSystems.join(ref, pack.flockRef, store);
        setTarget(ref, target, store);
        return true;
    }

    private static String targetKey(String roleName, Ref<EntityStore> target, Store<EntityStore> store) {
        UUIDComponent uuid = store.getComponent(target, UUIDComponent.getComponentType());
        return (uuid != null && uuid.getUuid() != null) ? roleName + "|" + uuid.getUuid() : null;
    }

    /**
     * Is {@code flockRef} a live flock ENTITY? Check its {@link Flock} component directly — NOT
     * {@code FlockPlugin.getFlock}, which expects a flock <i>member</i> ref (it reads the member's
     * FlockMembership) and always returns null for the flock entity itself. {@code createFlock} adds the
     * Flock + EntityGroup components synchronously, so a just-created flock passes immediately even though its
     * member group is still empty (members are added by a deferred system).
     */
    private static boolean isFlockAlive(Ref<EntityStore> flockRef, Store<EntityStore> store) {
        if (flockRef == null || !flockRef.isValid()
                || store.getComponent(flockRef, Flock.getComponentType()) == null) {
            return false;
        }
        EntityGroup group = store.getComponent(flockRef, EntityGroup.getComponentType());
        return group == null || !group.isDissolved();
    }

    private static Ref<EntityStore> sensedTarget(InfoProvider sensorInfo) {
        if (sensorInfo == null) {
            return null;
        }
        IPositionProvider pp = sensorInfo.getPositionProvider();
        return (pp != null) ? pp.getTarget() : null;
    }

    private static boolean isSameRole(Ref<EntityStore> ref, String roleName, Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        return npc != null && roleName.equals(npc.getRoleName());
    }

    private static void setTarget(Ref<EntityStore> ref, Ref<EntityStore> target, Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        Role role = (npc != null) ? npc.getRole() : null;
        if (role != null) {
            role.getMarkedEntitySupport().setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, target);
        }
    }

    /** Registry value: the owning flock + a logical member count (the engine's group count lags a tick). */
    private static final class Pack {
        final Ref<EntityStore> flockRef;
        final AtomicInteger count = new AtomicInteger(0);

        Pack(Ref<EntityStore> flockRef) {
            this.flockRef = flockRef;
        }
    }
}
