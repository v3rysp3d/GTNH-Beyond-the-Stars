package com.nateyburr.beyondthestars;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Base MetaTileEntity ID. 28100+ sits in a gap unused by GT5-Unofficial as of 5.09.52.594. */
    public static int mteBaseId = 28100;

    /** Length of one Gen I reactor fuel campaign, in real-time minutes. */
    public static int genICampaignMinutes = 720;

    /** Meltdowns produce the vertical detonation column. */
    public static boolean meltdownExplosions = true;

    /** Meltdowns leave a lingering radiation field (hazmat counters it). */
    public static boolean radiationField = true;

    /**
     * D-17 GRID CONDITIONING. With this on (the default), a plant with no SWITCHYARD standing
     * may only inject {@link com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1#GRID_SOFT_CAP}
     * EU/t; everything the reactor derives above that is thrown away until the yard is built.
     * <p>
     * It is ONE setting because it is a live balance change: every plant shipped before 0.21.0
     * was uncapped, and a tier-5 build makes ten times the cap. Turn it off and the Switchyard
     * becomes pure decoration - nothing else in the plant changes, and no panel line lies about
     * it (they all read this flag).
     */
    public static boolean switchyardSoftCap = true;

    /**
     * SINGULARITY LADDER (D-44, singularity-ladder-build.md §4): remove the Black Hole Compressor
     * controller's assembly-line recipe, because the Kerr Ergosphere Engine takes its slot at UIV.
     * Placed compressors keep working; only the crafting recipe (and data sticks holding it) go.
     * GTNH 2.9.0-RC-2: every recipe that CONSUMES a BHC controller (the Spacetime Compression Field
     * Generators, the coremod's Compressed Circuit Array) is first rewritten to consume the Kerr
     * controller instead (BTSBlackHoleSwap); the BHC recipe is kept if that cannot be done safely.
     */
    public static boolean removeBlackHoleCompressorRecipe = true;

    /**
     * SINGULARITY LADDER (D-28): clear TecTech's Eye of Harmony recipes, its NEI page and its
     * controller recipe; our Eye of Harmony replaces it. Placed TecTech EOHs form but idle. Off:
     * TecTech's EOH runs as stock beside ours, and the pack's White/Black Dwarf Matter and
     * Universium sources are untouched.
     */
    public static boolean replaceTecTechEyeOfHarmony = true;

    /**
     * P5 (p5-t9-t10.md §9.6): Eye T9 (the Great Attractor) and T10 (the Universe). False caps every Eye at T8 (a server
     * that wants the MAX chapter closed); unlocked tiers above 8 stay saved and come back if it is turned on again.
     */
    public static boolean enableEyeT9T10 = true;

    /**
     * D-49 retired {@code interimBlackDwarfAtEyeT4} (the interim Eye T4 Black Dwarf Matter recipe, Q-3):
     * Black Dwarf Matter now comes from Eye T5's Sirius B cooling batches. The stale key is removed from
     * the config file on load (d49-build-plan.md §10.4).
     */
    private static final String RETIRED_INTERIM_BDM_KEY = "interimBlackDwarfAtEyeT4";

    /**
     * D-49 (answer 3, D-34): draw the Eye's current object at its centre (machines/eye/render: the live star,
     * neutron star, cooling dwarf, magnetar, Core Collapse). The client skips drawing when it is off; the server
     * places no render block when it is off. Each Eye also has a GUI switch.
     */
    public static boolean eyeRenderCentre = true;

    /**
     * KERR REDESIGN R4 (docs/kerr-redesign.md §5): the Kerr Ergosphere Engine's black hole, drawn at the
     * cage centre with GT's Black Hole Compressor renderer and scaled by mass. Read on both sides: the
     * server places the render block only when it is on, and a client with it off draws nothing. Each
     * KEE also has its own switch in its GUI.
     */
    public static boolean renderKerrHole = true;

    /** Config category of the questbook keys (docs/questbook-integration.md §4.5). */
    public static final String CATEGORY_QUESTBOOK = "questbook";

    /**
     * QUESTBOOK (§4.5, D3): on every server start, install or refresh our chapters and the pack edits in the world's
     * live BetterQuesting database when the jar's bundle differs from what the world last got, or our content is
     * missing (a {@code /bq_admin default load} leaves exactly that behind). An unchanged world pays one file read.
     */
    public static boolean questbookAutoInstall = true;

    /**
     * QUESTBOOK (§3.7, D10): apply the pack edits (the BHC quest becomes the Kerr Ergosphere Engine, the EOH quest
     * becomes the Eye, the UXV/MAX/Endgame moves). Off: the next install restores every edited pack quest from its
     * stored original, and the two fallback quests (48103 KEE, 48109 Eye) stand beside the untouched pack quests.
     */
    public static boolean questbookPackEdits = true;
    /**
     * P4 (quasar-p4.md §1.6): the Quasar Engine's hole (GT's Black Hole Compressor render, scaled by mass), its
     * accretion disc and its two jets. Read on both sides: the server places the render block only when it is on, and a
     * client with it off draws nothing. Each quasar also has its own render switch in its GUI.
     */
    public static boolean renderQuasar = true;
    /** SMBH v1 (smbh-v1.md §5.1): the Unbind button on an L4 Supermassive Lattice. */
    public static boolean smbhAllowUnbound = true;
    /**
     * P5 (p5-t9-t10.md §8.2, Q-P5-9): the Containment Node L2 on Attractor Dark Matter (Eye T9) and L3 on Universium
     * (Eye T10). False keeps D-51's recipes (reachable matter only). L4 and Overdrive are the same either way; built
     * nodes are blocks and stay whatever the flag says. One recipe per node is registered, never both.
     */
    public static boolean latticeNodesUseP5Matter = true;

    /**
     * CIRCUITS v2 (design repo designs/docs/circuits-v2.md §1.1, §7.1, revision 3): the Sidereal / Empyrean / Omega
     * circuit families. On: their recipes (SL-X), their entries in GT's circuit ore names (preInit, so a change needs a
     * restart) and the computer-rack components. Off: the items still register (stable world registry) but are hidden
     * from NEI, have no ore name and no recipe. Independent of {@link #raiseStockSpecialCircuits} (question 26, B-136):
     * with this off the stock Piko / Quantum / Planck raise stays on until that key is off too.
     */
    public static boolean stellarCircuits = true;

    /**
     * CIRCUITS v2 §4, §7.1: the SL-N Nanochip Assembly Complex recipes and our real -> fake conversion recipes
     * (server-side recipe data only). It does NOT switch the CircuitComponent enum extension, which always applies when
     * GT is present, so client and server always agree on the constants.
     */
    public static boolean nacCircuitComponents = true;

    /**
     * CIRCUITS v2 §5 (decided: "On by default"): raise the NAC Piko, Quantum, Raw Manifold Bud and Planck steps
     * (SL-G10..G13,
     * in place at load-complete). Off: stock RC-2 costs. There is no compensation key (revision 3: "No offset").
     */
    public static boolean raiseStockSpecialCircuits = true;

    /** CIRCUITS v2 §5.3: duration factor E of the raise (EU/t unchanged, so EU x E). */
    public static float stockSpecialCircuitEnergyFactor = 2.5f;

    /** CIRCUITS v2 §5.3: item and fluid factor M of the raise (rounded up). */
    public static float stockSpecialCircuitMaterialFactor = 2.0f;

    /** Renders v2 (renders-v2.md §4.1): the client render settings' config category. */
    public static final String CATEGORY_CLIENT_RENDER = "client_render";
    /** Renders v2: {@code high}, {@code medium} or {@code low} (today's fixed-function renders). Client only. */
    public static String renderQuality = "high";
    /**
     * Renders v2: multiplies the level-of-detail pixel thresholds (0.5 keeps detail further out, 2 drops it sooner).
     */
    public static float renderLodScale = 1f;
    /** Renders v2: how many objects may draw at the highest detail at once (the largest on screen win). */
    public static int renderMaxHighObjects = 2;
    /** Renders v2: bend the world behind a black hole (a framebuffer copy); off by itself under a shaderpack. */
    public static boolean renderBackgroundLensing = true;
    /** Renders v2: the few particle effects (eruption debris, supernova ejecta, blow-out fragments). */
    public static boolean renderParticles = true;
    /** Renders v2 (§11): the Homogeny's vortex, coil glow and feed streams. Client only. */
    public static boolean renderHomogeny = true;
    /** Renders v2 (§12): the Reality Diffraction's and Reality Refab's renders. Client only. */
    public static boolean renderReality = true;
    /** Renders v2 (§10): the conduit's burst streams, the Wormhole Mouths and the crowns' heat. Client only. */
    public static boolean renderConduit = true;

    /**
     * B-227: every key that changes what the recipe maps hold (and so what NEI shows), with its value as text, plus
     * {@code mteBaseId} (the machine ids every recipe and NEI page names). The server sends this map to each player at
     * login ({@link BTSConfigSync}); a client whose own values differ says so in chat. A new recipe-affecting key
     * belongs here too.
     */
    public static Map<String, String> recipeFlags() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("mteBaseId", Integer.toString(mteBaseId));
        m.put("removeBlackHoleCompressorRecipe", Boolean.toString(removeBlackHoleCompressorRecipe));
        m.put("replaceTecTechEyeOfHarmony", Boolean.toString(replaceTecTechEyeOfHarmony));
        m.put("latticeNodesUseP5Matter", Boolean.toString(latticeNodesUseP5Matter));
        m.put("stellarCircuits", Boolean.toString(stellarCircuits));
        m.put("nacCircuitComponents", Boolean.toString(nacCircuitComponents));
        m.put("raiseStockSpecialCircuits", Boolean.toString(raiseStockSpecialCircuits));
        m.put("stockSpecialCircuitEnergyFactor", Float.toString(stockSpecialCircuitEnergyFactor));
        m.put("stockSpecialCircuitMaterialFactor", Float.toString(stockSpecialCircuitMaterialFactor));
        return m;
    }

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        mteBaseId = configuration.getInt(
            "mteBaseId",
            Configuration.CATEGORY_GENERAL,
            28100,
            1,
            32765,
            "Base MetaTileEntity ID for Beyond the Stars machines (GT's registry caps at 32765)."
                + " Change only on ID collision.");

        genICampaignMinutes = configuration.getInt(
            "genICampaignMinutes",
            Configuration.CATEGORY_GENERAL,
            720,
            1,
            20160,
            "Real-time minutes one Gen I reactor fuel campaign lasts per fuel assembly load.");

        meltdownExplosions = configuration.getBoolean(
            "meltdownExplosions",
            Configuration.CATEGORY_GENERAL,
            true,
            "Reactor meltdowns produce a vertical detonation column above the core.");

        radiationField = configuration.getBoolean(
            "radiationField",
            Configuration.CATEGORY_GENERAL,
            true,
            "Reactor meltdowns leave a lingering radiation field. Full radiation hazmat counters it.");

        switchyardSoftCap = configuration.getBoolean(
            "switchyardSoftCap",
            Configuration.CATEGORY_GENERAL,
            true,
            "Nuclear plants without a Switchyard are soft-capped at 100,000,000 EU/t of output."
                + " The reactor still derives its full figure; the surplus is discarded and every panel"
                + " says so. Set false to let a plant export its full output with no Switchyard.");

        removeBlackHoleCompressorRecipe = configuration.getBoolean(
            "removeBlackHoleCompressorRecipe",
            Configuration.CATEGORY_GENERAL,
            true,
            "Remove the Black Hole Compressor controller's assembly-line recipe (the Kerr Ergosphere Engine"
                + " replaces it at UIV). Every recipe that consumes a BHC controller (Spacetime Compression Field"
                + " Generators, Compressed Circuit Array) is rewritten to consume a Kerr Ergosphere Engine controller"
                + " instead. Placed compressors keep working. Set false to keep the BHC and its recipes as stock.");

        enableEyeT9T10 = configuration.getBoolean(
            "enableEyeT9T10",
            Configuration.CATEGORY_GENERAL,
            true,
            "Eye of Harmony T9 (the Great Attractor) and T10 (the Universe). Set false to cap every Eye at T8.");

        replaceTecTechEyeOfHarmony = configuration.getBoolean(
            "replaceTecTechEyeOfHarmony",
            Configuration.CATEGORY_GENERAL,
            true,
            "Clear TecTech's Eye of Harmony recipes, NEI page and controller recipe; Beyond the Stars' Eye of"
                + " Harmony replaces it. Placed TecTech EOHs idle. Finish or cancel running EOH recipes before"
                + " enabling. Set false to keep TecTech's EOH as stock.");

        final boolean retiredKeyPresent = configuration.getCategory(Configuration.CATEGORY_GENERAL)
            .containsKey(RETIRED_INTERIM_BDM_KEY);
        if (retiredKeyPresent) {
            configuration.getCategory(Configuration.CATEGORY_GENERAL)
                .remove(RETIRED_INTERIM_BDM_KEY);
        }

        eyeRenderCentre = configuration.getBoolean(
            "eyeRenderCentre",
            Configuration.CATEGORY_GENERAL,
            true,
            "Draw the Eye of Harmony's current object at the centre of the structure while it is formed and its star"
                + " burns: the live star (T1-T3), the neutron star (T4), the cooling white dwarf (T5), the magnetar"
                + " (T6) and the Core Collapse. Uses TecTech's star shader and textures. Off on a client: nothing is"
                + " drawn there. Off on a server: no Eye places its render block (and existing ones are removed)."
                + " Each Eye also has its own switch in its GUI.");

        renderKerrHole = configuration.getBoolean(
            "renderKerrHole",
            Configuration.CATEGORY_GENERAL,
            true,
            "Draw the Kerr Ergosphere Engine's black hole at the centre of its cage, scaled by its mass (GT's Black"
                + " Hole Compressor render). Cosmetic. On a server, false places no render block at all; on a client,"
                + " false draws nothing. Each Kerr also has its own render switch in its GUI.");

        configuration.setCategoryComment(
            CATEGORY_QUESTBOOK,
            "The Singularity Ladder questbook (BetterQuesting). /btsquests status shows what this world has.");

        questbookAutoInstall = configuration.getBoolean(
            "questbookAutoInstall",
            CATEGORY_QUESTBOOK,
            true,
            "On server start, install or refresh the Beyond the Stars chapters and pack edits in the world's quest"
                + " book when the mod's bundle changed or our content is missing (for example after"
                + " /bq_admin default load). Player progress is kept. Set false to install only with /btsquests"
                + " install.");

        questbookPackEdits = configuration.getBoolean(
            "questbookPackEdits",
            CATEGORY_QUESTBOOK,
            true,
            "Edit the pack's own quests for the Singularity Ladder: the Black Hole Compressor quest becomes the"
                + " Kerr Ergosphere Engine, the Eye of Harmony quest becomes our Eye, and the UXV, MAX and endgame"
                + " quests move into our chapters. Set false and the next install restores every edited pack"
                + " quest from its stored original; two quests of ours (Kerr and Eye) then stand beside the"
                + " untouched pack quests.");
        renderQuasar = configuration.getBoolean(
            "renderQuasar",
            Configuration.CATEGORY_GENERAL,
            true,
            "Draw the Quasar Engine's black hole, accretion disc (its width shows the gas in it, its colour the disc's"
                + " zone) and two burst jets (as bright as their Bu/t). Cosmetic. On a server, false places no render"
                + " block; on a client, false draws nothing. Each quasar also has its own render switch in its GUI.");
        smbhAllowUnbound = configuration.getBoolean(
            "smbhAllowUnbound",
            Configuration.CATEGORY_GENERAL,
            true,
            "Supermassive Lattice: allow Unbind on an L4 (Constrained Eternity) lattice, so the mass setpoint can pass"
                + " 67,108,864 SM. Each doubling past it halves the Overdrive upkeep and costs a Primordial Matter"
                + " anchor of 1,024 x 4^(n^2) L/s; a short anchor only sheds mass (no terrain is ever damaged). False"
                + " hides the button, refuses OpenComputers' setUnbound(true) and turns an Unbound hole back (it sheds"
                + " to 67,108,864 SM).");
        latticeNodesUseP5Matter = configuration.getBoolean(
            "latticeNodesUseP5Matter",
            Configuration.CATEGORY_GENERAL,
            true,
            "Supermassive Lattice node recipes: true makes the L2 node from an L1 node + 4,032 L Entangled Attractor"
                + " Dark Matter (Eye T9) and the L3 node from an L2 node + 2,304 L Entangled Universium (Eye T10)."
                + " False keeps the older recipes on Superdense Ergomatter, Magnetar and Black Dwarf Matter. L4 and"
                + " Overdrive do not change. Nodes already built stay as they are.");

        stellarCircuits = configuration.getBoolean(
            "stellarCircuits",
            Configuration.CATEGORY_GENERAL,
            true,
            "The Sidereal (UMV), Empyrean (UXV) and Omega (MAX) circuit families: their recipes, their place in GT's"
                + " circuit ore names (so they fit every 'any circuit of the tier' slot) and their computer-rack"
                + " entries. Set false to keep the items registered but hidden, with no ore name and no recipe."
                + " This does not switch off the Piko / Quantum / Planck raise: that is raiseStockSpecialCircuits."
                + " Restart needed.");

        nacCircuitComponents = configuration.getBoolean(
            "nacCircuitComponents",
            Configuration.CATEGORY_GENERAL,
            true,
            "Let the Nanochip Assembly Complex take and build our circuit parts (its existing modules only; no new"
                + " module). Set false to register none of those NAC recipes; the NAC then never pulls our items in."
                + " The NAC component list itself is the same either way, so client and server always agree.");

        raiseStockSpecialCircuits = configuration.getBoolean(
            "raiseStockSpecialCircuits",
            Configuration.CATEGORY_GENERAL,
            true,
            "Make the NAC's Piko, Quantum and Planck circuits (and the Raw Manifold Bud) dearer: each step's time x the"
                + " energy factor, its items and fluids x the material factor. Set false for stock costs. Independent of"
                + " stellarCircuits: turning our circuits off does not turn this off.");

        stockSpecialCircuitEnergyFactor = configuration.getFloat(
            "stockSpecialCircuitEnergyFactor",
            Configuration.CATEGORY_GENERAL,
            2.5f,
            1.0f,
            5.0f,
            "Duration (so EU) factor of raiseStockSpecialCircuits.");

        stockSpecialCircuitMaterialFactor = configuration.getFloat(
            "stockSpecialCircuitMaterialFactor",
            Configuration.CATEGORY_GENERAL,
            2.0f,
            1.0f,
            4.0f,
            "Item and fluid factor of raiseStockSpecialCircuits (amounts rounded up).");

        renderQuality = configuration.getString(
            "renderQuality",
            CATEGORY_CLIENT_RENDER,
            "high",
            "Client only. How the black holes, the quasar, the Eye's stars and galaxies and the other big renders are"
                + " drawn: high (everything, full detail when close), medium (no background lensing, half the steps"
                + " and points) or low (the simple fixed-function renders, no shaders). Objects drop detail with"
                + " their size on screen by themselves; a shader that fails to compile falls back to low for that"
                + " object.",
            new String[] { "high", "medium", "low" });
        renderLodScale = configuration.getFloat(
            "renderLodScale",
            CATEGORY_CLIENT_RENDER,
            1f,
            0.25f,
            4f,
            "Client only. Multiplies the on-screen sizes at which the big renders drop detail: 0.5 keeps full detail"
                + " further out, 2 drops it sooner (faster).");
        renderMaxHighObjects = configuration.getInt(
            "renderMaxHighObjects",
            CATEGORY_CLIENT_RENDER,
            2,
            0,
            16,
            "Client only. How many big renders may draw at full detail at once (the largest on screen win; the rest"
                + " draw one step lower).");
        renderBackgroundLensing = configuration.getBoolean(
            "renderBackgroundLensing",
            CATEGORY_CLIENT_RENDER,
            true,
            "Client only. Bend the world seen behind a black hole (copies part of the screen). Off by itself while a"
                + " shaderpack is in use; the hole then bends a drawn starfield.");
        renderParticles = configuration.getBoolean(
            "renderParticles",
            CATEGORY_CLIENT_RENDER,
            true,
            "Client only. The few particle effects of the big renders (eruption debris, supernova ejecta, blow-out"
                + " fragments). Off: their shells and flashes alone.");
        renderHomogeny = configuration.getBoolean(
            "renderHomogeny",
            CATEGORY_CLIENT_RENDER,
            true,
            "Client only. Draw the Homogeny's plasma vortex in its glass column, the glow of its shear helix and the"
                + " feed streams from its vessels. Cosmetic.");
        renderReality = configuration.getBoolean(
            "renderReality",
            CATEGORY_CLIENT_RENDER,
            true,
            "Client only. Draw Reality Diffraction's beam, lenses, spectral fans, region clouds and field, and Reality"
                + " Refab's stream, glyph rings and reassembling voxels. Cosmetic.");
        renderConduit = configuration.getBoolean(
            "renderConduit",
            CATEGORY_CLIENT_RENDER,
            true,
            "Client only. Draw the burst streams between conduit elements, the Wormhole Mouths' portals and the heat"
                + " glow of the Burst Thermaliser Crowns. Cosmetic.");

        // Always written back: Forge does not read comments from the file and does not count a changed comment as a
        // change, so an old file would keep saying eyeRenderCentre is "not implemented" (D-49 unit 6). Saving
        // rewrites every comment from the code; values are kept.
        configuration.save();
    }
}
