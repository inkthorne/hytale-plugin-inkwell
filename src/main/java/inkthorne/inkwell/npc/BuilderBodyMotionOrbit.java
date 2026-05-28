package inkthorne.inkwell.npc;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleRangeValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderBodyMotionBase;

/**
 * JSON builder for {@link BodyMotionOrbit} ({@code "Type": "Inkwell_Orbit"}). Reads its parameters
 * during NPC role load and constructs the motion per spawned entity.
 *
 * <p>Parameters (all optional, with defaults):
 * <ul>
 *   <li>{@code Radius} (default {@code 3.0}) — distance in blocks to orbit the target at.</li>
 *   <li>{@code RelativeSpeed} (default {@code 0.7}) — movement speed as a fraction of the NPC's max speed (0&lt;x&le;1).</li>
 *   <li>{@code RadiusGain} (default {@code 0.6}) — how strongly the NPC corrects back toward {@code Radius} (0 = none).</li>
 *   <li>{@code Clockwise} (default {@code true}) — orbit direction, viewed from above.</li>
 * </ul>
 *
 * <p>Mirrors the engine's own body-motion builders (e.g. {@code BuilderBodyMotionMaintainDistance}):
 * the framework invokes {@link #readConfig(JsonElement)} at load time and {@link #build(BuilderSupport)}
 * per entity.
 */
public class BuilderBodyMotionOrbit extends BuilderBodyMotionBase {

    protected final DoubleHolder radius = new DoubleHolder();
    protected final DoubleHolder relativeSpeed = new DoubleHolder();
    protected final DoubleHolder radiusGain = new DoubleHolder();
    protected final BooleanHolder clockwise = new BooleanHolder();

    public BuilderBodyMotionOrbit() {
    }

    @Override
    public BuilderBodyMotionOrbit readConfig(JsonElement element) {
        getDouble(element, "Radius", radius, 3.0, DoubleSingleValidator.greater0(),
            getBuilderDescriptorState(), "Distance in blocks to orbit the target at.", null);
        getDouble(element, "RelativeSpeed", relativeSpeed, 0.7, DoubleRangeValidator.fromExclToIncl(0.0, 1.0),
            getBuilderDescriptorState(), "Movement speed as a fraction of the NPC's max speed.", null);
        getDouble(element, "RadiusGain", radiusGain, 0.6, DoubleSingleValidator.greaterEqual0(),
            getBuilderDescriptorState(), "How strongly the NPC corrects back toward the orbit radius (0 = none).", null);
        getBoolean(element, "Clockwise", clockwise, true,
            getBuilderDescriptorState(), "Orbit direction; true = clockwise viewed from above.", null);
        return this;
    }

    @Override
    public BodyMotionOrbit build(BuilderSupport support) {
        return new BodyMotionOrbit(this, support);
    }

    public double getRadius(BuilderSupport support) {
        return radius.get(support.getExecutionContext());
    }

    public double getRelativeSpeed(BuilderSupport support) {
        return relativeSpeed.get(support.getExecutionContext());
    }

    public double getRadiusGain(BuilderSupport support) {
        return radiusGain.get(support.getExecutionContext());
    }

    public boolean isClockwise(BuilderSupport support) {
        return clockwise.get(support.getExecutionContext());
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public String getShortDescription() {
        return "Continuously circle the sensed target.";
    }

    @Override
    public String getLongDescription() {
        return "Orbits the target supplied by the paired sensor: moves tangentially around it while "
            + "correcting toward a fixed radius. Pair with a Player or Target sensor.";
    }
}
