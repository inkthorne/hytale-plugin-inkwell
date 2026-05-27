package inkthorne.inkwell;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

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
        // TODO: register reusable components/systems/content as they are built.

        getLogger().atInfo().log("Inkwell library loaded!");
    }
}
