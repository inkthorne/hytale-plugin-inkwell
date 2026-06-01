package inkthorne.inkwell;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.npc.NPCPlugin;
import inkthorne.inkwell.debug.CombatLogSystem;
import inkthorne.inkwell.debug.InkwellCommand;
import inkthorne.inkwell.npc.BuilderActionRecruitFlock;
import inkthorne.inkwell.npc.BuilderBodyMotionOrbit;
import inkthorne.inkwell.npc.BuilderSensorFlockAttackToken;

/**
 * Entry point for Inkwell — a public <b>library plugin</b> for Hytale: the shared
 * reservoir that other plugins draw from. It ships reusable content (weapons,
 * projectiles, combat primitives, …) and the API to use it; dependents declare a
 * runtime dependency on it via their manifest {@code Dependencies}, and the server
 * loads Inkwell first.
 *
 * <p>This is an empty skeleton. Wire library pieces into {@link #setup()} as they
 * land — register the relevant components/systems/commands on the appropriate
 * registry, then log — mirroring the pattern used throughout the modding handbook
 * examples. Bundled assets (item/weapon JSON, particles, sounds) go under
 * {@code src/main/resources/Server/...}; {@code IncludesAssetPack} is already set in
 * the manifest, so they ship inside the jar.
 *
 * <p>Reusable combat primitives slated to live here (per Telefrag's notes): player
 * and item respawners, the weapon/projectile framework, pickup spawners, and
 * damage/scoring scaffolding — extracted so arena-specific plugins (Telefrag) and
 * juice plugins (Fluff) can share them rather than re-implement.
 */
public class InkwellPlugin extends JavaPlugin {

    public InkwellPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Register Inkwell's custom interaction types so JSON assets can reference them by Type.
        // Must happen before assets load so referencing documents decode against the right codec.
        getCodecRegistry(Interaction.CODEC).register(
            "Inkwell_AoeEffect",
            InteractionAoeEffect.class,
            InteractionAoeEffect.CODEC);

        // Register Inkwell's custom NPC body-motion types so role JSON can reference them by Type.
        // The NPC plugin owns the core-component registry; we depend on it (manifest Dependencies)
        // so it loads first, and register before our own NPC role assets are parsed.
        NPCPlugin.get().registerCoreComponentType("Inkwell_Orbit", BuilderBodyMotionOrbit::new);

        // Coordination sensor: true for only one member of a flock at a time (the attack-token holder), so a
        // role can gate its attack on it and exactly one creature approaches+swings while the rest hang back.
        NPCPlugin.get().registerCoreComponentType("Inkwell_FlockAttackToken", BuilderSensorFlockAttackToken::new);

        // Runtime flock formation: when an aggro'd NPC runs this action it recruits nearby same-role NPCs into
        // a flock and rallies them on the target — so packs form dynamically on contact, not at spawn time.
        NPCPlugin.get().registerCoreComponentType("Inkwell_RecruitFlock", BuilderActionRecruitFlock::new);

        // Debug combat log: one server-log line per hit involving an Inkwell creature (attacker -> victim,
        // amount, cause, and the NPC's flock membership). Hytale has no built-in combat log; this fills that
        // gap and lets us confirm flocking + attack timing by grepping the server log for "[CombatLog]".
        getEntityStoreRegistry().registerSystem(new CombatLogSystem(getLogger()));

        // Debug commands under the /inkwell namespace. Currently: /inkwell killrole <role> — remove all NPCs
        // of a given role (type-filtered cleanup the vanilla /npc clean lacks), e.g.
        // /inkwell killrole Inkwell_Role_Pack_Rat.
        getCommandRegistry().registerCommand(new InkwellCommand());

        getLogger().atInfo().log("Inkwell library loaded!");
    }
}
