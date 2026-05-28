package inkthorne.inkwell.npc;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.BodyMotionBase;
import com.hypixel.hytale.server.npc.movement.Steering;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import org.joml.Vector3d;

/**
 * {@code Inkwell_Orbit} — a custom {@link com.hypixel.hytale.server.npc.instructions.BodyMotion}
 * that makes an NPC continuously circle the target supplied by its paired sensor.
 *
 * <p>It produces a horizontal steering vector that is tangential to a circle around the target
 * (the orbit), blended with a radial correction that pulls the NPC back toward the configured
 * {@code Radius} — so it holds a stable orbit and re-converges if knocked off. The target comes
 * from the instruction node's {@code Sensor} (e.g. {@code Player}) via the {@link InfoProvider};
 * if no target is sensed the motion is inactive (returns {@code false}).
 *
 * <p>Registered as a core component in {@code InkwellPlugin.setup()} so role JSON can reference it
 * as {@code "BodyMotion": { "Type": "Inkwell_Orbit", ... }}. See {@link BuilderBodyMotionOrbit} for
 * the JSON parameters.
 */
public class BodyMotionOrbit extends BodyMotionBase {

    private final double radius;
    private final double relativeSpeed;
    private final double radiusGain;
    private final double directionSign;

    /** The most recently orbited target, exposed for AI debug/telemetry via {@link #getDesiredTargetEntity()}. */
    private Ref<EntityStore> lastTarget;

    public BodyMotionOrbit(BuilderBodyMotionOrbit builder, BuilderSupport support) {
        super(builder);
        this.radius = builder.getRadius(support);
        this.relativeSpeed = builder.getRelativeSpeed(support);
        this.radiusGain = builder.getRadiusGain(support);
        this.directionSign = builder.isClockwise(support) ? 1.0 : -1.0;
    }

    @Override
    public boolean computeSteering(Ref<EntityStore> self, Role role, InfoProvider info, double dt,
                                   Steering out, ComponentAccessor<EntityStore> accessor) {
        // Target is provided by the paired sensor; if nothing is sensed, don't move.
        IPositionProvider positionProvider = info.getPositionProvider();
        if (positionProvider == null || !positionProvider.hasPosition()) {
            this.lastTarget = null;
            out.clear();
            return false;
        }

        TransformComponent transform = accessor.getComponent(self, TransformComponent.getComponentType());
        if (transform == null) {
            this.lastTarget = null;
            out.clear();
            return false;
        }
        this.lastTarget = positionProvider.getTarget();

        // Horizontal vector from us to the target. Read straight off the live vectors/provider —
        // no per-tick allocation (single-threaded tick, values only read).
        Vector3d selfPos = transform.getPosition();
        double dx = positionProvider.getX() - selfPos.x();
        double dz = positionProvider.getZ() - selfPos.z();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0e-3) {
            // Sitting on top of the target — nowhere meaningful to circle this tick.
            out.clear();
            return true;
        }

        // Radial unit vector (points at the target) and the perpendicular tangent (the orbit).
        double rx = dx / dist;
        double rz = dz / dist;
        double tx = -rz * directionSign;
        double tz = rx * directionSign;

        // Blend tangential motion with a correction toward the desired radius.
        double radiusError = dist - radius;
        double mx = tx + rx * (radiusError * radiusGain);
        double mz = tz + rz * (radiusError * radiusGain);
        double mlen = Math.sqrt(mx * mx + mz * mz);
        if (mlen > 1.0e-6) {
            mx /= mlen;
            mz /= mlen;
        }

        out.clear();
        out.setTranslation(mx, 0.0, mz);
        out.setTranslationRelativeSpeed(relativeSpeed);
        // No explicit yaw: like the engine's MoveAway/Find motions, the locomotion controller
        // orients the NPC to its movement direction, so the chicken runs forwards around the orbit.
        return true;
    }

    @Override
    public Ref<EntityStore> getDesiredTargetEntity() {
        return lastTarget;
    }
}
