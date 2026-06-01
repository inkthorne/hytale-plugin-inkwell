package inkthorne.inkwell.npc;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.corecomponents.movement.builders.BuilderBodyMotionMaintainDistance;

/**
 * JSON builder for {@link InkwellBodyMotionMaintainDistance} ({@code "Type": "Inkwell_MaintainDistance"}).
 *
 * <p>Adds one param on top of the engine {@code MaintainDistance} fields (DesiredDistanceRange, speeds,
 * strafing, …): {@code FaceMovementDirection} (boolean, default {@code false}). {@code readConfig} calls
 * {@code super.readConfig} to read all the inherited params (it does not touch {@code readCommonConfig}, so
 * there's no double-registration of the common Enabled/Once attributes), then reads the extra flag.
 */
public class InkwellBuilderBodyMotionMaintainDistance extends BuilderBodyMotionMaintainDistance {

    protected final BooleanHolder faceMovementDirection = new BooleanHolder();

    public InkwellBuilderBodyMotionMaintainDistance() {
    }

    @Override
    public BuilderBodyMotionMaintainDistance readConfig(JsonElement data) {
        super.readConfig(data);
        getBoolean(data, "FaceMovementDirection", faceMovementDirection, false,
            getBuilderDescriptorState(),
            "Face the movement direction instead of the target (no target-facing strafe/back-pedal).", null);
        return this;
    }

    @Override
    public InkwellBodyMotionMaintainDistance build(BuilderSupport support) {
        return new InkwellBodyMotionMaintainDistance(this, support);
    }

    public boolean isFaceMovementDirection(BuilderSupport support) {
        return faceMovementDirection.get(support.getExecutionContext());
    }

    @Override
    public String getShortDescription() {
        return "Maintain distance from the target, optionally facing the movement direction.";
    }

    @Override
    public String getLongDescription() {
        return "Like the engine MaintainDistance, plus FaceMovementDirection: when true the NPC orients to its "
            + "movement direction (Seek/MoveAway-style) instead of locking its body to face the target.";
    }
}
