package inkthorne.inkwell;

import java.util.function.Predicate;

import org.joml.Vector3d;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Inkwell's area-of-effect interaction: applies a status effect to every entity within
 * {@code Range} of the executing entity's position. Designed to run as an instant interaction
 * from a projectile's {@code ProjectileHit}/{@code ProjectileMiss}, giving a "blast radius"
 * effect (e.g. the frost staff's ice ball slows everything near where it lands).
 *
 * <p>This exists because the JSON {@code Selector} (AOECircle/AOECylinder) does <b>not</b> sweep a
 * radius when run from a projectile — it only resolves the directly-collided entity. So we do the
 * radius query in code via {@link Selector#selectNearbyEntities} and apply the effect to each hit.
 *
 * <p>Movement-modifying status effects (e.g. {@code Slow}'s {@code HorizontalSpeedMultiplier})
 * affect both players and NPCs and auto-expire on their own — no manual cleanup needed. (Note:
 * {@code DisableAll} effects only halt players; fully stopping NPC AI needs the {@code Frozen}
 * component, which is intentionally not used here since it does not auto-clear.)
 *
 * <p>JSON: {@code { "Type": "Inkwell_AoeEffect", "Range": 6.0, "EffectId": "Slow" }}
 */
public class InkwellAoeEffectInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<InkwellAoeEffectInteraction> CODEC =
        BuilderCodec.builder(InkwellAoeEffectInteraction.class, InkwellAoeEffectInteraction::new)
            .append(new KeyedCodec<>("Range", Codec.DOUBLE),
                    InkwellAoeEffectInteraction::setRange, InkwellAoeEffectInteraction::getRange)
            .add()
            .append(new KeyedCodec<>("EffectId", Codec.STRING),
                    InkwellAoeEffectInteraction::setEffectId, InkwellAoeEffectInteraction::getEffectId)
            .add()
            .append(new KeyedCodec<>("Vfx", Codec.STRING),
                    InkwellAoeEffectInteraction::setVfx, InkwellAoeEffectInteraction::getVfx)
            .add()
            .append(new KeyedCodec<>("VfxScale", Codec.DOUBLE),
                    InkwellAoeEffectInteraction::setVfxScale, InkwellAoeEffectInteraction::getVfxScale)
            .add()
            .append(new KeyedCodec<>("VfxDuration", Codec.DOUBLE),
                    InkwellAoeEffectInteraction::setVfxDuration, InkwellAoeEffectInteraction::getVfxDuration)
            .add()
            .append(new KeyedCodec<>("CandidateMargin", Codec.DOUBLE),
                    InkwellAoeEffectInteraction::setCandidateMargin, InkwellAoeEffectInteraction::getCandidateMargin)
            .add()
            .build();

    private double range = 5.0;
    private String effectId = "Slow";
    /** Optional particle system to spawn world-oriented at the impact point ("" = none). */
    private String vfx = "";
    private double vfxScale = 1.0;
    private double vfxDuration = 2.0;
    /** Extra query reach (blocks) beyond {@code range} for the broad-phase origin query, so creatures
     *  whose origin sits outside {@code range} but whose body overlaps the blast are still candidates
     *  for the bounding-box test. Must be >= the largest creature's origin-to-body-edge distance. */
    private double candidateMargin = 16.0;

    public InkwellAoeEffectInteraction() {
    }

    public double getCandidateMargin() {
        return candidateMargin;
    }

    public void setCandidateMargin(double candidateMargin) {
        this.candidateMargin = candidateMargin;
    }

    public String getVfx() {
        return vfx;
    }

    public void setVfx(String vfx) {
        this.vfx = vfx;
    }

    public double getVfxScale() {
        return vfxScale;
    }

    public void setVfxScale(double vfxScale) {
        this.vfxScale = vfxScale;
    }

    public double getVfxDuration() {
        return vfxDuration;
    }

    public void setVfxDuration(double vfxDuration) {
        this.vfxDuration = vfxDuration;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public String getEffectId() {
        return effectId;
    }

    public void setEffectId(String effectId) {
        this.effectId = effectId;
    }

    @Override
    protected void firstRun(InteractionType type, InteractionContext context, CooldownHandler cooldown) {
        Ref<EntityStore> self = context.getEntity();
        if (self == null || !self.isValid()) {
            return;
        }
        // The caster/shooter — exclude so you don't apply the effect to yourself.
        Ref<EntityStore> owner = context.getOwningEntity();
        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();

        TransformComponent transform = buffer.getComponent(self, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d center = transform.getPosition();

        // Spawn the impact VFX ourselves, world-oriented (zero rotation) at the impact point.
        // Doing it here — rather than via JSON particles — gives a consistent, untilted burst on
        // both direct hits and terrain misses (JSON ModelParticles inherit the projectile's tilt,
        // and DamageEffects particles only fire on entity hits).
        if (vfx != null && !vfx.isEmpty()) {
            ParticleUtil.spawnParticleEffect(
                vfx, center, 0.0f, 0.0f, 0.0f, (float) vfxScale, (float) vfxDuration, buffer);
        }

        var effectMap = EntityEffect.getAssetMap();
        EntityEffect effect = effectId == null ? null : effectMap.getAsset(effectMap.getIndex(effectId));
        if (effect == null) {
            return;
        }

        Predicate<Ref<EntityStore>> filter = ref -> ref != null && ref.isValid()
            && !ref.equals(self) && (owner == null || !ref.equals(owner));

        // selectNearbyEntities matches on entity ORIGIN, so a large creature whose origin sits
        // outside `range` is missed even when its body overlaps the blast. Gather a generous
        // candidate set by origin, then keep only those whose BOUNDING BOX is within `range`.
        final double candidateRadius = range + candidateMargin;
        final double rangeSq = range * range;

        Selector.selectNearbyEntities(buffer, center, candidateRadius, ref -> {
            if (!boundingBoxWithin(buffer, ref, center, rangeSq)) {
                return;
            }
            EffectControllerComponent ctrl =
                buffer.getComponent(ref, EffectControllerComponent.getComponentType());
            if (ctrl != null) {
                ctrl.addEffect(ref, effect, buffer);
            }
        }, filter);
    }

    /**
     * True if the entity's world-space bounding box comes within {@code sqrt(rangeSq)} of {@code center}
     * (squared closest-point distance from the impact to the AABB). Falls back to the entity origin if it
     * has no bounding box.
     */
    private static boolean boundingBoxWithin(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
                                             Vector3d center, double rangeSq) {
        TransformComponent transform = buffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        Vector3d pos = transform.getPosition();
        BoundingBox boundingBox = buffer.getComponent(ref, BoundingBox.getComponentType());
        Box box = boundingBox == null ? null : boundingBox.getBoundingBox();
        if (box == null) {
            return pos.distanceSquared(center) <= rangeSq;
        }
        // BoundingBox is model-relative; offset by the entity position to get the world AABB.
        double dx = center.x - clamp(center.x, pos.x + box.min.x, pos.x + box.max.x);
        double dy = center.y - clamp(center.y, pos.y + box.min.y, pos.y + box.max.y);
        double dz = center.z - clamp(center.z, pos.z + box.min.z, pos.z + box.max.z);
        return (dx * dx + dy * dy + dz * dz) <= rangeSq;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
