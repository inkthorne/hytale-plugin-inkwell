package inkthorne.inkwell.npc;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;

/**
 * JSON builder for {@link SensorFlockAttackToken} ({@code "Type": "Inkwell_FlockAttackToken"}). The sensor
 * takes no parameters (the token interval is a constant), so this only wires up the common sensor config.
 *
 * <p>Mirrors the engine's own sensor builders (e.g. {@code BuilderSensorFlockLeader}): the framework calls
 * {@link #readConfig(JsonElement)} at role-load time and {@link #build(BuilderSupport)} per spawned entity.
 */
public class BuilderSensorFlockAttackToken extends BuilderSensorBase {

    public BuilderSensorFlockAttackToken() {
    }

    @Override
    public Builder<Sensor> readConfig(JsonElement element) {
        // No custom params (the token interval is a constant). Do NOT call readCommonConfig here — the
        // framework applies the common "Enabled"/"Once" sensor config itself; calling it again double-registers
        // those attributes and fails role load with "FAIL: ... Once / Enabled" (matches BuilderSensorFlockLeader,
        // whose readConfig likewise does not call it).
        return this;
    }

    @Override
    public SensorFlockAttackToken build(BuilderSupport support) {
        return new SensorFlockAttackToken(this, support);
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public String getShortDescription() {
        return "True only for the flock member currently holding the attack token.";
    }

    @Override
    public String getLongDescription() {
        return "A per-flock attack token: at most one member of a flock (or, unflocked, one NPC of the same "
            + "role) holds it at a time for a fixed interval. Returns true only for the holder — gate an "
            + "attack branch on it (And with a Player sensor) so exactly one NPC approaches and attacks while "
            + "the rest hold back.";
    }
}
