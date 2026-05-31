package inkthorne.inkwell.debug;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Root {@code /inkwell} command — a namespace for Inkwell's debug/admin subcommands, so they don't
 * pollute the global command space. Subcommands are added in the constructor via {@code addSubCommand}
 * (mirroring the engine's own {@code NPCDebugCommand} collection).
 *
 * <p>Current subcommands:
 * <ul>
 *   <li>{@code /inkwell killrole <role>} — remove all NPCs of a given role ({@link KillRoleCommand}).</li>
 * </ul>
 */
public final class InkwellCommand extends AbstractCommandCollection {

    public InkwellCommand() {
        super("inkwell", "Inkwell debug/admin commands");
        setPermissionGroups(new String[] { "hytale:ServerEditor" });
        addSubCommand(new KillRoleCommand());
    }
}
