package inkthorne.inkwell.debug;

import java.util.concurrent.atomic.AtomicInteger;

import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * Debug command {@code /killrole <role>} — removes every NPC whose role id exactly matches the argument
 * (e.g. {@code /killrole Inkwell_Role_Pack_Rat}). The type-filtered cleaner Hytale lacks: vanilla only
 * offers {@code /npc clean} (all NPCs) and {@code /entity clean} (all entities), neither filterable by role.
 *
 * <p>Built on the same pattern as the engine's {@code NPCCleanCommand}: iterate all {@link NPCEntity}
 * entities and {@link com.hypixel.hytale.component.CommandBuffer#removeEntity remove} the matches. Gated to
 * the {@code hytale:ServerEditor} permission group, matching vanilla destructive NPC commands.
 */
public final class KillRoleCommand extends AbstractWorldCommand {

    private final RequiredArg<String> roleArg;

    public KillRoleCommand() {
        super("killrole", "Remove all NPCs with the given role id (e.g. Inkwell_Role_Pack_Rat)");
        this.roleArg = withRequiredArg("role", "The NPC role id to remove", ArgTypes.STRING);
        setPermissionGroups(new String[] { "hytale:ServerEditor" });
    }

    @Override
    protected void execute(CommandContext context, World world, Store<EntityStore> store) {
        String role = roleArg.get(context);
        AtomicInteger removed = new AtomicInteger();

        store.forEachEntityParallel(NPCEntity.getComponentType(), (index, chunk, buffer) -> {
            NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
            if (npc != null && role.equals(npc.getRoleName())) {
                buffer.removeEntity(chunk.getReferenceTo(index), RemoveReason.REMOVE);
                removed.incrementAndGet();
            }
        });

        context.sendMessage(Message.raw("Removed " + removed.get() + " NPC(s) with role '" + role + "'."));
    }
}
