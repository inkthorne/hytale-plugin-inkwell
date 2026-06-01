package inkthorne.inkwell.npc;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.movement.BodyMotionMaintainDistance;
import com.hypixel.hytale.server.npc.movement.Steering;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import org.joml.Vector3d;

/**
 * {@code Inkwell_MaintainDistance} — the engine's {@code MaintainDistance} body motion plus a
 * {@code FaceMovementDirection} option.
 *
 * <p>Vanilla {@link BodyMotionMaintainDistance} locks the NPC's body yaw to face the target while it strafes /
 * back-pedals to hold distance (it ends {@code computeSteering} with {@code setYaw(targetYaw)}). With
 * {@code FaceMovementDirection: true} we instead orient the body to the <i>movement</i> direction — like
 * {@code Seek}/{@code MoveAway} — so the creature runs facing where it's going rather than sidestepping while
 * staring at the target. With it {@code false} (default) behavior is identical to the engine motion.
 *
 * <p>Implemented by deferring entirely to {@code super.computeSteering} (all the distance/strafe/speed logic)
 * and only rewriting the final yaw from the resulting translation vector, using the same
 * {@link PhysicsMath#headingFromDirection}/{@link PhysicsMath#normalizeTurnAngle} the engine motion uses.
 */
public class InkwellBodyMotionMaintainDistance extends BodyMotionMaintainDistance {

    private final boolean faceMovementDirection;

    public InkwellBodyMotionMaintainDistance(InkwellBuilderBodyMotionMaintainDistance builder, BuilderSupport support) {
        super(builder, support);
        this.faceMovementDirection = builder.isFaceMovementDirection(support);
    }

    @Override
    public boolean computeSteering(Ref<EntityStore> ref, Role role, InfoProvider sensorInfo, double dt,
                                   Steering desiredSteering, ComponentAccessor<EntityStore> accessor) {
        boolean result = super.computeSteering(ref, role, sensorInfo, dt, desiredSteering, accessor);
        if (faceMovementDirection && desiredSteering.hasTranslation()) {
            Vector3d t = desiredSteering.getTranslation();
            desiredSteering.setYaw(PhysicsMath.normalizeTurnAngle(PhysicsMath.headingFromDirection(t.x(), t.z())));
        }
        return result;
    }
}
