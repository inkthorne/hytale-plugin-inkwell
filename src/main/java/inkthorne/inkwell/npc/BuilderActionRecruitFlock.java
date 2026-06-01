package inkthorne.inkwell.npc;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

/**
 * JSON builder for {@link ActionRecruitFlock} ({@code "Type": "Inkwell_RecruitFlock"}).
 *
 * <p>Params: {@code FlockSize} (default 5; max pack size incl. the recruiter) and {@code FlockRadius}
 * (default 16; recruit search radius in blocks). Does NOT call {@code readCommonConfig} — the framework
 * applies the common {@code Enabled}/{@code Once} action config itself, and calling it again double-registers
 * those attributes and fails role load (same trap as the sensor builder).
 */
public class BuilderActionRecruitFlock extends BuilderActionBase {

    protected final DoubleHolder flockSize = new DoubleHolder();
    protected final DoubleHolder flockRadius = new DoubleHolder();

    public BuilderActionRecruitFlock() {
    }

    @Override
    public Builder<Action> readConfig(JsonElement element) {
        getDouble(element, "FlockSize", flockSize, 5.0, DoubleSingleValidator.greater0(),
            getBuilderDescriptorState(), "Max NPCs in a dynamically-formed pack (including the recruiter).", null);
        getDouble(element, "FlockRadius", flockRadius, 16.0, DoubleSingleValidator.greater0(),
            getBuilderDescriptorState(), "How far the aggro'd NPC looks to recruit pack-mates, in blocks.", null);
        return this;
    }

    @Override
    public ActionRecruitFlock build(BuilderSupport support) {
        return new ActionRecruitFlock(this, support);
    }

    public double getFlockSize(BuilderSupport support) {
        return flockSize.get(support.getExecutionContext());
    }

    public double getFlockRadius(BuilderSupport support) {
        return flockRadius.get(support.getExecutionContext());
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public String getShortDescription() {
        return "Form a flock from nearby same-role NPCs and rally them on the aggro target.";
    }

    @Override
    public String getLongDescription() {
        return "When run (gate it on a Player sensor + not-already-in-a-flock), creates an engine flock, "
            + "recruits up to FlockSize-1 same-role NPCs within FlockRadius, and locks the sensed target as "
            + "every member's LockedTarget so recruits that never sensed the target still converge and attack.";
    }
}
