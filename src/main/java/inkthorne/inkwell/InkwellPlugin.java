package inkthorne.inkwell;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.npc.NPCPlugin;
import inkthorne.inkwell.npc.BuilderBodyMotionOrbit;

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

        getLogger().atInfo().log("Inkwell library loaded!");
    }
}
