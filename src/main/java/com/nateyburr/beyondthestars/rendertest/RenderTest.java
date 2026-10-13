package com.nateyburr.beyondthestars.rendertest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.ConduitKit;
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.render.BlockEyeStar;
import com.nateyburr.beyondthestars.machines.eye.render.EyeCentreRender;
import com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook;
import com.nateyburr.beyondthestars.machines.kerr.BlockKerrHole;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrSlot;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.QuasarGeometry;
import com.nateyburr.beyondthestars.machines.quasar.QuasarKit;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.quasar.render.BlockQuasarHole;
import com.nateyburr.beyondthestars.machines.reality.HRKit;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.common.tileentities.machines.multi.compressor.MTEBlackHoleCompressor;
import tectech.thing.CustomItemList;
import tectech.thing.block.TileEntityEyeOfHarmony;
import tectech.thing.block.TileEntityForgeOfGods;
import tectech.thing.casing.TTCasingsContainer;
import tectech.thing.metaTileEntity.multi.godforge.color.ForgeOfGodsStarColor;

/**
 * DEV-ONLY REAL-CLIENT RENDER QA harness, part 1 (the server half). Active only when the JVM runs with
 * {@code -Dbts.rendertest=true} (and only in a client, see {@link RenderTestClient}); in normal play nothing here is
 * registered or called.
 * <p>
 * Every case is one picture subject: a Kerr with its hole open at a mass, an Eye showing one centre-render object, or a
 * reference machine (GT's Black Hole Compressor with an open hole, TecTech's Eye of Harmony with its star). A
 * {@link Site} is one structure in the superflat test world; consecutive cases on the same site reuse it (the Kerr T1
 * at
 * 16 and 64 SM, the Eye T4 neutron star and both Core Collapse looks, the T5 dwarf early and late). The structures are
 * raised through the existing kits ({@link KerrKit#build}, {@link EyeKit#build}), the same code as {@code /btskerr} and
 * {@code /btseye}; the Eye's object is set through {@link MTEEyeOfHarmonyBTS#debugForceLook} with the very looks
 * {@link EyeStarLook#of} computes for those states, so no star has to be driven through its lifecycle.
 * <p>
 * Everything here runs on the integrated server's thread: the client queues work with {@link #onServer} and waits on
 * the returned future.
 */
public final class RenderTest {

    private RenderTest() {}

    /** The switch: {@code -Dbts.rendertest=true}. */
    public static final boolean ENABLED = Boolean.getBoolean("bts.rendertest");

    /**
     * Save folder of the test world under {@code run/client/saves}. It names the site layout (L2: x 1000 + 500 n,
     * z 4000): a run reuses the world of its own layout and rebuilds each site in place, and a layout change MUST bump
     * the suffix, or the old layout's structures stand inside the new sites.
     */
    // L3 (window bands, B-86): same site layout as L2, but the builds never clear a site and the windows left cells
    // open that L2 had filled, so a fresh world
    // L4 (renders v2, R-01): L3's sites plus the Homogeny, the two Reality machines and the conduit rig appended
    static final String WORLD_FOLDER = "bts-rendertest-L4";
    /** Owner of the test Eyes (their wireless network). */
    private static final UUID OWNER = UUID
        .nameUUIDFromBytes("beyondthestars:render-test".getBytes(StandardCharsets.UTF_8));
    /** First air row of the superflat preset {@code 2;7,2x3,2;1;} (bedrock, 2 dirt, grass at y 3). */
    private static final int GROUND = 4;
    private static final int SITE_X0 = 1000;
    private static final int SITE_Z = 4000;
    private static final int SITE_SPACING = 500;
    /** Every structure faces north: the front views look south at it. */
    private static final ForgeDirection FACING = ForgeDirection.NORTH;
    /** Furthest a far view may stand from what it looks at (render distance is 16 chunks). */
    private static final double MAX_VIEW_DISTANCE = 230;

    // =============================================================================== cases

    /** One structure in the test world. */
    enum Site {

        KERR_T1,
        KERR_T2,
        EYE_T1,
        EYE_T2,
        EYE_T3,
        EYE_T4,
        EYE_T5,
        EYE_T6,
        GT_BHC,
        TT_EOH,
        GT_GODFORGE,
        /** P4: the Quasar Engine (coils T3), appended so every older site keeps its place. */
        QUASAR,
        /** P4: the Eye at T7 (the stability rail) and T8 (the shard halo), appended. */
        EYE_T7,
        EYE_T8,
        /** Renders v2 (R-01): the Homogeny at its top tier, the vortex's future home (no visual yet). */
        HOMOGENY,
        /** Renders v2: the Reality Diffraction (no visual yet). */
        REALITY_DIFFRACTION,
        /** Renders v2: the Reality Refab (no visual yet). */
        REALITY_REFAB,
        /**
         * Renders v2: the conduit rig (ConduitKit.rig): a quasar with a Mk I crown on its top port and mouths below.
         */
        CONDUIT;

        /** The Eye tier this site builds (1..8), or 0 for a non-Eye site. */
        int eyeTier() {
            if (this == EYE_T7) return 7;
            if (this == EYE_T8) return 8;
            return ordinal() >= EYE_T1.ordinal() && ordinal() <= EYE_T6.ordinal() ? ordinal() - EYE_T1.ordinal() + 1
                : 0;
        }

        int originX() {
            return SITE_X0 + ordinal() * SITE_SPACING;
        }
    }

    /** One picture subject. */
    static final class Case {

        final String name;
        final Site site;
        /** Kerr: the hole's mass, SM (0 otherwise). */
        final double mass;
        /** Eye: the centre object (null otherwise). */
        final EyeStarLook look;
        final String what;
        /**
         * Eye: the age (ticks) the bounce shell / magnetar flare is frozen at for this case's shots
         * ({@link com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar#debugEventAge}), or -1 (live).
         */
        final double eventAge;
        /**
         * Renders v2: a client-side look override for the shots, or null (live): Kerr {stability} into
         * {@code TileKerrHole.debugStability}; quasar the {@code TileQuasarHole.debugLook} array.
         */
        final float[] clientLook;

        Case(String name, Site site, double mass, EyeStarLook look, String what) {
            this(name, site, mass, look, what, -1);
        }

        Case(String name, Site site, double mass, EyeStarLook look, String what, double eventAge) {
            this(name, site, mass, look, what, eventAge, null);
        }

        Case(String name, Site site, double mass, EyeStarLook look, String what, double eventAge, float[] clientLook) {
            this.name = name;
            this.site = site;
            this.mass = mass;
            this.look = look;
            this.what = what;
            this.eventAge = eventAge;
            this.clientLook = clientLook;
        }
    }

    /**
     * Renders v2: an Eye case with its frozen clocks: {@code eventAge} the kind / flare / transition age (ticks, -1
     * live), {@code eventIn} seconds to the event's hit (negative after it, NaN live), {@code prevKind} the kind shown
     * before (-1 live).
     */
    private static Case eyeCase(String name, Site site, EyeStarLook look, String what, double eventAge, double eventIn,
        int prevKind) {
        return new Case(name, site, 0, look, what, eventAge, new float[] { (float) eventIn, prevKind });
    }

    /**
     * Renders v2 WP-5: a galaxy case with its frozen clocks, as {@link #eyeCase}, plus {@code crossingIn} seconds to
     * the satellite crossing's hit (negative after it, NaN live).
     */
    private static Case galaxyCase(String name, Site site, EyeStarLook look, String what, double eventAge,
        double eventIn, int prevKind, double crossingIn) {
        return new Case(
            name,
            site,
            0,
            look,
            what,
            eventAge,
            new float[] { (float) eventIn, prevKind, (float) crossingIn });
    }

    /** Renders v2 WP-5: the Galactic Merger at {@code st} seconds, as the runtime shows it (Triangulum + the pass). */
    private static EyeStarLook mergerAt(int st) {
        return EyeStarLook.pericentre(
            EyeStarLook.galaxy(7, 0.3f, 0.5f)
                .transition(7, st / 2100f),
            st);
    }

    /** The event clock {@link #mergerAt} puts on the channel (NaN after the last pass's minute). */
    private static double mergerIn(int st) {
        final EyeStarLook l = mergerAt(st);
        return l.ev == 0 ? Double.NaN : l.evIn;
    }

    /** A Kerr case's client look: GT's stability input (0.5 and up calm, 0 spinning). */
    private static float[] kerrLook(float stability) {
        return new float[] { stability };
    }

    /**
     * A quasar case's client look ({@code TileQuasarHole.debugLook}); flash age negative: no flash. Renders v2: a flash
     * is an eruption, the spin 0.5 (the T3 coils' equilibrium), the flux 30, no lock.
     */
    private static float[] quasarLook(int zone, float lc, float e, float jetTop, float jetBottom, float flashAge,
        float flashSize) {
        return quasarLook(
            zone,
            lc,
            e,
            jetTop,
            jetBottom,
            flashAge,
            flashSize,
            flashAge >= 0 ? 1 : 0,
            0.5f,
            30f,
            false,
            0f);
    }

    /**
     * Renders v2 (R-03): the full quasar look: event kind (1 eruption, 2 purge, 3 blow-out, 4 re-formed, 5 First
     * Light), spin, flux, Harmonic Lock, re-forming fraction.
     */
    private static float[] quasarLook(int zone, float lc, float e, float jetTop, float jetBottom, float flashAge,
        float flashSize, int event, float spin, float phi, boolean lock, float reform) {
        return new float[] { zone, lc, e, jetTop, jetBottom, flashAge, flashSize, event, spin, phi, lock ? 1 : 0,
            reform };
    }

    /** Renders v2: one frame of an event sequence on the 1,024 SM quasar (Stable, jets 600 Bu/t). */
    private static Case quasarEvent(String name, int event, float ageTicks, float size, float phi, String what) {
        return new Case(
            name,
            Site.QUASAR,
            1024,
            null,
            what,
            -1,
            quasarLook(1, 0.85f, 10f, 600f, 600f, ageTicks, size, event, 0.6f, phi, false, 0f));
    }

    /**
     * In order: consecutive cases on one site share its structure. {@code -Dbts.rendertest.only=a,b} keeps only the
     * cases whose name starts with one of the comma-separated prefixes (e.g. {@code kerr,ref_gt_bhc}).
     */
    static final List<Case> CASES = only(
        Arrays.asList(
            // ---- the Kerr (GT's BlackholeRenderer, scaled by mass)
            new Case("kerr_t1_16sm", Site.KERR_T1, 16, null, "Kerr T1, hole open at 16 SM"),
            new Case("kerr_t1_64sm", Site.KERR_T1, 64, null, "Kerr T1, hole open at 64 SM (the T1 cap)"),
            new Case(
                "kerr_t1_64sm_strained",
                Site.KERR_T1,
                64,
                null,
                "Kerr T1 at 64 SM, governor coverage 0.3 (GT stability input 0.15: the disc shrinks and reddens)",
                -1,
                kerrLook(0.15f)),
            new Case(
                "kerr_t1_64sm_failing",
                Site.KERR_T1,
                64,
                null,
                "Kerr T1 at 64 SM, no coverage (GT stability 0: the disc spins, deep red)",
                -1,
                kerrLook(0f)),
            new Case("kerr_t2_256sm", Site.KERR_T2, 256, null, "Kerr T2 (Density Upgrade), hole open at 256 SM"),
            new Case("kerr_t2_512sm", Site.KERR_T2, 512, null, "Kerr T2 at 512 SM (the T2 cap)"),
            // ---- the Eye T1-T6 stars
            new Case(
                "eye_t1_protostar_early",
                Site.EYE_T1,
                0,
                EyeStarLook.protostar(1, 0.1f),
                "Eye T1 igniting, 10 % of the way (P -74): a small dim red protostar"),
            new Case(
                "eye_t1_protostar_late",
                Site.EYE_T1,
                0,
                EyeStarLook.protostar(1, 0.8f),
                "Eye T1 igniting, 80 % of the way (P -32): swollen, heating toward Sol"),
            new Case("eye_t1_sol", Site.EYE_T1, 0, EyeStarLook.lit(1, 0, 0), "Eye T1, Sol (live star)"),
            new Case(
                "eye_t1_sol_unstable",
                Site.EYE_T1,
                0,
                EyeStarLook.lit(1, 0.8f, 1),
                "Eye T1, Sol at stability 20 on an event warning (the wobble)"),
            // renders v2: the ignition flash (the protostar lit, frozen at 15 of its 60 ticks) and the Mass Infusion
            eyeCase(
                "eye_t1_ignition",
                Site.EYE_T1,
                EyeStarLook.lit(1, 0, 0),
                "Eye T1, the protostar just lit: the ignition flash, its cocoon blown away (15 of 60 ticks)",
                15,
                Double.NaN,
                EyeStarLook.PROTOSTAR),
            eyeCase(
                "eye_t1_mass_infusion",
                Site.EYE_T1,
                EyeStarLook.growing(1, 0.5f),
                "Eye T1 -> T2, the Mass Infusion half way: Sol heating toward Sirius A, matter raining in",
                -1,
                Double.NaN,
                -1),
            new Case("eye_t2_sirius_a", Site.EYE_T2, 0, EyeStarLook.lit(2, 0, 0), "Eye T2, Sirius A (live star)"),
            eyeCase(
                "eye_t2_shell_ignition",
                Site.EYE_T2,
                EyeStarLook.growing(2, 0.5f),
                "Eye T2 -> T3, the Shell Ignition half way: Sirius A swelling and cooling toward Arcturus",
                -1,
                Double.NaN,
                -1),
            new Case("eye_t3_arcturus", Site.EYE_T3, 0, EyeStarLook.lit(3, 0, 0), "Eye T3, Arcturus (red giant)"),
            new Case(
                "eye_t3_helium_flash_warn",
                Site.EYE_T3,
                0,
                EyeStarLook.lit(3, 0.3f, 1),
                "Eye T3, Arcturus on a helium-flash warning (stability 70)"),
            eyeCase(
                "eye_t3_helium_flash_t05",
                Site.EYE_T3,
                EyeStarLook.lit(3, 0.3f, 1)
                    .event(EyeStarLook.EV_HELIUM_FLASH, 5f),
                "Eye T3, a helium flash 5 s away: the cells boil, the star throbs",
                -1,
                5,
                -1),
            eyeCase(
                "eye_t3_helium_flash_hit",
                Site.EYE_T3,
                EyeStarLook.lit(3, 0.3f, 1)
                    .event(EyeStarLook.EV_HELIUM_FLASH, -1f),
                "Eye T3, a helium flash 1 s after its hit: the bright front across the surface, the puff",
                -1,
                -1,
                -1),
            new Case("eye_t4_neutron", Site.EYE_T4, 0, EyeStarLook.lit(4, 0, 0), "Eye T4, neutron star"),
            eyeCase(
                "eye_t4_glitch",
                Site.EYE_T4,
                EyeStarLook.lit(4, 0, 0)
                    .event(EyeStarLook.EV_GLITCH, -0.4f),
                "Eye T4, a starquake glitch 0.4 s after it hit: the crust flash, the beams stutter",
                -1,
                -0.4,
                -1),
            new Case(
                "eye_t4_collapse_swell",
                Site.EYE_T4,
                0,
                EyeStarLook.swell(0.5f),
                "Eye T4 structure, Core Collapse phase 1 mid-swell (half way to the bounce)"),
            new Case(
                "eye_t4_collapse_bounce",
                Site.EYE_T4,
                0,
                EyeStarLook.bounce(0f),
                "Eye T4 structure, Core Collapse phase 3, the bounce shell (proto-neutron star), shell frozen at"
                    + " 30 ticks (about 7.4 blocks, rim alpha 0.04 -> 0.64 x 0.7)",
                30),
            new Case(
                "eye_t4_collapse_bounce_t08",
                Site.EYE_T4,
                0,
                EyeStarLook.bounce(0f),
                "Eye T4 structure, the bounce 8 ticks in: the shock leaving the proto-neutron star, the flash",
                8),
            new Case(
                "eye_t4_collapse_bounce_t70",
                Site.EYE_T4,
                0,
                EyeStarLook.bounce(0f),
                "Eye T4 structure, the bounce 70 ticks in: the shock near the bands, fading",
                70),
            new Case(
                "eye_t4_supernova_t05",
                Site.EYE_T4,
                0,
                EyeStarLook.NOVA_FLASH,
                "Eye T4 structure, a failed Core Collapse: the supernova 5 ticks in (the blinding flash)",
                5),
            new Case(
                "eye_t4_supernova_t45",
                Site.EYE_T4,
                0,
                EyeStarLook.NOVA_FLASH,
                "Eye T4 structure, the supernova 45 ticks in: the remnant's filaments fading",
                45),
            new Case(
                "eye_t4_supernova",
                Site.EYE_T4,
                0,
                EyeStarLook.NOVA_FLASH,
                "Eye T4 structure, a failed Core Collapse: the supernova flash frozen at 20 of its 60 ticks",
                20),
            new Case(
                "eye_t5_dwarf_early",
                Site.EYE_T5,
                0,
                EyeStarLook.dwarf(0.1),
                "Eye T5, Sirius B cooling batch at 10 % (white)"),
            new Case(
                "eye_t5_dwarf_mid",
                Site.EYE_T5,
                0,
                EyeStarLook.dwarf(0.5),
                "Eye T5, Sirius B cooling batch at 50 % (orange)"),
            new Case(
                "eye_t5_dwarf_late",
                Site.EYE_T5,
                0,
                EyeStarLook.dwarf(0.9),
                "Eye T5, Sirius B cooling batch at 90 % (near black)"),
            eyeCase(
                "eye_t5_first_cooling",
                Site.EYE_T5,
                EyeStarLook.dwarf(0.01)
                    .transition(4, 0.01f),
                "Eye T4 -> T5, the First Cooling's start: the white dwarf assembling (40 of 80 ticks)",
                40,
                Double.NaN,
                -1),
            new Case(
                "eye_t5_black_dwarf",
                Site.EYE_T5,
                0,
                EyeStarLook.dwarf(1.0),
                "Eye T5, Sirius B at the end of a batch: the black dwarf (its crystal sheen)"),
            new Case("eye_t6_magnetar", Site.EYE_T6, 0, EyeStarLook.lit(6, 0, 0), "Eye T6, SGR 1806-20 magnetar"),
            eyeCase(
                "eye_t6_giant_flare_t06",
                Site.EYE_T6,
                EyeStarLook.lit(6, 0, 1)
                    .event(EyeStarLook.EV_GIANT_FLARE, 6f),
                "Eye T6, a giant flare 6 s away: the field winding up and crackling, the crust faults glowing",
                200,
                6,
                -1),
            eyeCase(
                "eye_t6_giant_flare_hit",
                Site.EYE_T6,
                EyeStarLook.lit(6, 0, 1)
                    .event(EyeStarLook.EV_GIANT_FLARE, -0.8f),
                "Eye T6, a giant flare 0.8 s after its hit: the flash, the field torn open into a fan",
                200,
                -0.8,
                -1),
            eyeCase(
                "eye_t6_merger_start",
                Site.EYE_T6,
                EyeStarLook.lit(6, 0, 0)
                    .transition(5, 0.02f),
                "Eye T5 -> T6, the Magnetar Merger's start: the merger flash (20 of 60 ticks)",
                20,
                Double.NaN,
                -1),
            eyeCase(
                "eye_t6_merger_imprint",
                Site.EYE_T6,
                EyeStarLook.lit(6, 0, 0)
                    .transition(5, 0.6f),
                "Eye T5 -> T6, the Magnetar Merger's imprint (60 %): the dynamo winding the field up",
                200,
                Double.NaN,
                -1),
            new Case(
                "eye_t6_magnetar_warn",
                Site.EYE_T6,
                0,
                EyeStarLook.lit(6, 0, 1),
                "Eye T6, magnetar on an event warning (flare shell bright), a flare frozen at 15 ticks",
                15),
            new Case(
                "eye_t6_overfed",
                Site.EYE_T6,
                0,
                EyeStarLook.overfedMagnetar(0.8f, 0),
                "Eye T6 -> T7 Magnetar Collapse, the overfed magnetar at 80 % (shrunk, spun up, throbbing), a flare"
                    + " frozen at 15 ticks",
                15),
            // ---- the reference renders of the pack
            new Case("ref_gt_bhc", Site.GT_BHC, 0, null, "REFERENCE: GT Black Hole Compressor, hole open (stable)"),
            new Case("ref_tt_eoh", Site.TT_EOH, 0, null, "REFERENCE: TecTech Eye of Harmony, star render"),
            new Case(
                "ref_gt_godforge",
                Site.GT_GODFORGE,
                0,
                null,
                "REFERENCE: TecTech Forge of the Gods star render (default: radius 20, one ring, default colour)"),
            // ---- the quasar (GT's hole + our disc and jets)
            new Case(
                "quasar_64sm",
                Site.QUASAR,
                64,
                null,
                "Quasar Engine (coils T3), a fresh 64 SM hole at f 0.85: hole scale 3.1, the disc band r 40 to about"
                    + " 59, Stable, both jets venting (no dock); live look"),
            new Case(
                "quasar_256sm_resonant",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM, disc Resonant (ice-white), lambda_c 0.9, E -10, jets 40 Bu/t each",
                -1,
                quasarLook(0, 0.9f, -10f, 40f, 40f, -1f, 0f)),
            new Case(
                "quasar_256sm_critical",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM, disc Critical (red), lambda_c 1, E +80 (puffed up), jets 120 Bu/t each",
                -1,
                quasarLook(3, 1f, 80f, 120f, 120f, -1f, 0f)),
            new Case(
                "quasar_256sm_reforming",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM after a blow-out: the disc re-forming (dark), no jets",
                -1,
                quasarLook(-1, 0f, 0f, 0f, 0f, -1f, 0f)),
            new Case(
                "quasar_1024sm_eruption",
                Site.QUASAR,
                1024,
                null,
                "Quasar Engine, a 1,024 SM hole (D-32's good build): hole scale 5.1, Stable, jets 600 Bu/t; an"
                    + " eruption flash (size 1) frozen at 15 of its 60 ticks",
                -1,
                quasarLook(1, 0.85f, 10f, 600f, 600f, 15f, 1f)),
            new Case(
                "quasar_1024sm_purge",
                Site.QUASAR,
                1024,
                null,
                "Quasar 1,024 SM, a purge flash (size 0.3) frozen at 30 of its 60 ticks, jets 300 Bu/t",
                -1,
                quasarLook(1, 0.85f, 0f, 300f, 300f, 30f, 0.3f, 2, 0.5f, 20f, false, 0f)),
            // ---- renders v2 (WP-2): the quasar's other looks and the event sequences
            new Case(
                "quasar_256sm_lock",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM, Resonant under the Harmonic Lock (600 s Resonant): the standing ripples, spin 0.9",
                -1,
                quasarLook(0, 0.9f, -5f, 60f, 60f, -1f, 0f, 0, 0.9f, 40f, true, 0f)),
            new Case(
                "quasar_256sm_flux95",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM, Stable, flux 95 (past the eruption warning: the field lines crackle), spin 0.9",
                -1,
                quasarLook(1, 0.85f, 5f, 120f, 120f, -1f, 0f, 0, 0.9f, 95f, false, 0f)),
            new Case(
                "quasar_256sm_slowspin",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM, Stable, spin 0.05 (the inner disc's edge at 6 r_g: a dark gap round the shadow)",
                -1,
                quasarLook(1, 0.85f, 0f, 60f, 60f, -1f, 0f, 0, 0.05f, 10f, false, 0f)),
            new Case(
                "quasar_256sm_strained",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM, Strained (orange, thicker turbulence), E +40 (puffed), jets 200 Bu/t",
                -1,
                quasarLook(2, 0.95f, 40f, 200f, 200f, -1f, 0f, 0, 0.6f, 50f, false, 0f)),
            new Case(
                "quasar_256sm_reforming_half",
                Site.QUASAR,
                256,
                null,
                "Quasar 256 SM re-forming, half way (150 of 300 s): violet-red embers filling in from the inner wall",
                -1,
                quasarLook(-1, 0f, 0f, 0f, 0f, -1f, 0f, 0, 0.6f, 0f, false, 0.5f)),
            quasarEvent("quasar_1024sm_erupt_t03", 1, 3f, 1f, 2f, "Eruption, 3 ticks in: the field lines snap"),
            quasarEvent("quasar_1024sm_erupt_t10", 1, 10f, 1f, 2f, "Eruption, 10 ticks: the shell crosses the ring"),
            quasarEvent(
                "quasar_1024sm_erupt_t25",
                1,
                25f,
                1f,
                2f,
                "Eruption, 25 ticks: the ring flares, the giant knots"),
            quasarEvent("quasar_1024sm_erupt_t45", 1, 45f, 1f, 2f, "Eruption, 45 ticks: debris, the shell fading"),
            quasarEvent("quasar_1024sm_erupt_t70", 1, 70f, 1f, 2f, "Eruption, 70 ticks: settling"),
            quasarEvent(
                "quasar_1024sm_purge_t04",
                2,
                4f,
                0.3f,
                15f,
                "Purge, 4 ticks: the field lines unwind, jets pulse"),
            quasarEvent("quasar_1024sm_purge_t15", 2, 15f, 0.3f, 15f, "Purge, 15 ticks: the spiral flash up the jets"),
            quasarEvent("quasar_1024sm_purge_t40", 2, 40f, 0.3f, 15f, "Purge, 40 ticks: the ring dimmed"),
            quasarEvent("quasar_1024sm_purge_t65", 2, 65f, 0.3f, 15f, "Purge, 65 ticks: recovering"),
            new Case(
                "quasar_256sm_blowout_t08",
                Site.QUASAR,
                256,
                null,
                "Blow-out, 8 ticks: the ring breaks up (zone already dark, jets off)",
                -1,
                quasarLook(-1, 0f, 0f, 0f, 0f, 8f, 0.5f, 3, 0.6f, 0f, false, 0f)),
            new Case(
                "quasar_256sm_blowout_t40",
                Site.QUASAR,
                256,
                null,
                "Blow-out, 40 ticks: chunks flying outward, fading",
                -1,
                quasarLook(-1, 0f, 0f, 0f, 0f, 40f, 0.5f, 3, 0.6f, 0f, false, 0f)),
            new Case(
                "quasar_256sm_reformed_t20",
                Site.QUASAR,
                256,
                null,
                "Re-formed, 20 ticks: the inner wall kindles, the gas fills outward (Stable)",
                -1,
                quasarLook(1, 0.85f, 0f, 30f, 30f, 20f, 0.5f, 4, 0.6f, 0f, false, 0f)),
            // ---- P4 Eye T7 / T8 (the centre render's galaxies)
            new Case(
                "eye_t7_triangulum",
                Site.EYE_T7,
                0,
                EyeStarLook.galaxy(7, 0, 0),
                "Eye T7, Triangulum: three loose blue-white arms of dots to r 16 round a small bright core, tilted 8"
                    + " deg, turning once in about 2.6 min"),
            new Case(
                "eye_t7_forming",
                Site.EYE_T7,
                0,
                EyeStarLook.galaxy(7, 0.3f, 0),
                "Eye T7, the Magnetar Collapse's galaxy look (unrest 0.3): what shows while T6 becomes T7"),
            new Case(
                "eye_t7_merger_warn",
                Site.EYE_T7,
                0,
                EyeStarLook.galaxy(7, 0.3f, 1),
                "Eye T7, Triangulum on a minor merger's warning (the arms 60 % brighter, the core throbbing)"),
            new Case(
                "eye_t8_milky_way",
                Site.EYE_T8,
                0,
                EyeStarLook.galaxy(8, 0, 0),
                "Eye T8, the Milky Way: a warm bar and four arms to r 19 round a 3-block bulge, under the shard halo"),
            new Case(
                "eye_t8_merger",
                Site.EYE_T8,
                0,
                EyeStarLook.galaxy(8, 0.3f, 0.5f),
                "Eye T8, the Galactic Merger in progress (pericentre passes; the runtime shows warn 0.5)"),
            new Case(
                "eye_t8_crossing_warn",
                Site.EYE_T8,
                0,
                EyeStarLook.galaxy(8, 0, 1),
                "Eye T8, the Milky Way on a satellite crossing's warning (warn 1)"),
            new Case(
                "eye_t8_planets",
                Site.EYE_T8,
                0,
                EyeStarLook.planets(6),
                "Eye T8 in planets mode: a calm glow with six planets on their orbits (r 5 to 13)"),
            new Case(
                "eye_t8_planets3",
                Site.EYE_T8,
                0,
                EyeStarLook.planets(3),
                "Eye T8 in planets mode with three slots"),
            // ---- renders v2 WP-5: the galaxies' transitions and events
            galaxyCase(
                "eye_t7_collapse_flash",
                Site.EYE_T7,
                EyeStarLook.galaxy(7, 0.3f, 0)
                    .transition(6, 0.334f),
                "Eye T6 -> T7 Magnetar Collapse, 1.5 s after the collapse: the core flash, the stars bursting out, the"
                    + " shell running to the bands",
                30,
                Double.NaN,
                EyeStarLook.MAGNETAR,
                Double.NaN),
            galaxyCase(
                "eye_t7_capture_25",
                Site.EYE_T7,
                EyeStarLook.galaxy(7, 0.3f, 0)
                    .transition(6, 1 / 3f + 2 / 3f * 0.25f),
                "Eye T6 -> T7, the halo capture at 25 %: a round halo of stars beginning to settle into a disc",
                -1,
                Double.NaN,
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t7_capture_70",
                Site.EYE_T7,
                EyeStarLook.galaxy(7, 0.3f, 0)
                    .transition(6, 1 / 3f + 2 / 3f * 0.7f),
                "Eye T6 -> T7, the halo capture at 70 %: the disc formed, arms and dust coming in",
                -1,
                Double.NaN,
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t7_minor_warn",
                Site.EYE_T7,
                EyeStarLook.galaxy(7, 0, 1)
                    .event(EyeStarLook.EV_MINOR_MERGER_UP, 5f),
                "Eye T7, a minor merger 5 s away (sign +): the dwarf falling in from above the disc",
                -1,
                5,
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t7_minor_hit",
                Site.EYE_T7,
                EyeStarLook.galaxy(7, 0, 1)
                    .event(EyeStarLook.EV_MINOR_MERGER_DOWN, -1f),
                "Eye T7, a minor merger 1 s after its hit (sign -): the impact flash, the dwarf torn apart",
                -1,
                -1,
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_gm_approach",
                Site.EYE_T8,
                mergerAt(400),
                "Eye T7 -> T8 Galactic Merger at 400 s: the companion falling toward the first pericentre",
                -1,
                mergerIn(400),
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_gm_pass1",
                Site.EYE_T8,
                mergerAt(601),
                "Eye T7 -> T8, 1 s after the first pericentre pass: the starburst, the bridge",
                -1,
                mergerIn(601),
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_gm_apo",
                Site.EYE_T8,
                mergerAt(900),
                "Eye T7 -> T8 at 900 s: the companion at its apocentre, the tidal tails growing",
                -1,
                mergerIn(900),
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_gm_pass2",
                Site.EYE_T8,
                mergerAt(1215),
                "Eye T7 -> T8, 15 s after the second pass: long tails, the bridge, the core glowing",
                -1,
                mergerIn(1215),
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_gm_pass3",
                Site.EYE_T8,
                mergerAt(1830),
                "Eye T7 -> T8, 30 s after the last pass: the companion swallowed, the Milky Way taking shape",
                -1,
                mergerIn(1830),
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_gm_settle",
                Site.EYE_T8,
                mergerAt(2000),
                "Eye T7 -> T8, the settle at 2,000 s: the tails fading, a barred spiral",
                -1,
                mergerIn(2000),
                -1,
                Double.NaN),
            galaxyCase(
                "eye_t8_crossing_t60",
                Site.EYE_T8,
                EyeStarLook.galaxy(8, 0, 1)
                    .crossing(60f),
                "Eye T8, a satellite crossing 60 s away: the dwarf falling toward the disc",
                -1,
                Double.NaN,
                -1,
                60),
            galaxyCase(
                "eye_t8_crossing_t03",
                Site.EYE_T8,
                EyeStarLook.galaxy(8, 0, 1)
                    .crossing(3f),
                "Eye T8, a satellite crossing 3 s away: the dwarf at the disc, stretched by the tide",
                -1,
                Double.NaN,
                -1,
                3),
            galaxyCase(
                "eye_t8_crossing_wave60",
                Site.EYE_T8,
                EyeStarLook.galaxy(8, 0, 0)
                    .crossing(-60f),
                "Eye T8, 60 s after a satellite crossing: the ring wave of star formation running out",
                -1,
                Double.NaN,
                -1,
                -60),
            galaxyCase(
                "eye_t8_crossing_wave200",
                Site.EYE_T8,
                EyeStarLook.galaxy(8, 0, 0)
                    .crossing(-200f),
                "Eye T8, 200 s after a satellite crossing: the ring wave near the edge",
                -1,
                Double.NaN,
                -1,
                -200),
            galaxyCase(
                "eye_t8_minor_warn",
                Site.EYE_T8,
                EyeStarLook.galaxy(8, 0, 1)
                    .event(EyeStarLook.EV_MINOR_MERGER_DOWN, 4f),
                "Eye T8, a minor merger 4 s away (sign -): the dwarf rising toward the disc from below",
                -1,
                4,
                -1,
                Double.NaN),
            // ---- renders v2: machines with no visual yet (the poses for their future renders)
            new Case(
                "homogeny_top",
                Site.HOMOGENY,
                0,
                null,
                "The Homogeny at its top tier, formed, idle: where the vortex would turn (no render yet)"),
            new Case(
                "reality_diffraction",
                Site.REALITY_DIFFRACTION,
                0,
                null,
                "Reality Diffraction, formed, idle (no render yet)"),
            new Case("reality_refab", Site.REALITY_REFAB, 0, null, "Reality Refab, formed, idle (no render yet)"),
            new Case(
                "conduit_crown",
                Site.CONDUIT,
                64,
                null,
                "The conduit rig: a 64 SM quasar with a Mk I Burst Thermaliser Crown docked on its top jet port"
                    + " (the crown and the burst stream have no render yet)")));

    private static List<Case> only(List<Case> all) {
        final String filter = System.getProperty("bts.rendertest.only", "")
            .trim();
        if (filter.isEmpty()) return Collections.unmodifiableList(all);
        final List<Case> kept = new ArrayList<>();
        for (Case c : all) {
            for (String prefix : filter.split(",")) {
                if (!prefix.trim()
                    .isEmpty() && c.name.startsWith(prefix.trim())) {
                    kept.add(c);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(kept);
    }

    /** What the client needs to frame a built site. */
    static final class Built {

        Site site;
        String error;
        int[] controller;
        ForgeDirection facing = FACING;
        /** {minX, minY, minZ, maxX, maxY, maxZ}. */
        int[] box;
        /** The render object's centre (block centre). */
        double[] centre;
        final List<String> lines = new ArrayList<>();
    }

    // ======================================================================== server thread

    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();
    /** The site and case being photographed (server thread state, kept alive once a second). */
    private static volatile Built current;
    private static volatile Case currentCase;
    private static boolean registered;

    /** Registers the server-tick hook (idempotent). */
    static synchronized void registerServerHook() {
        if (registered) return;
        registered = true;
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerHook());
    }

    /** Runs {@code work} on the integrated server's thread at the end of its next tick. */
    static <T> CompletableFuture<T> onServer(Supplier<T> work) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        TASKS.add(() -> {
            try {
                future.complete(work.get());
            } catch (Throwable t) {
                BeyondTheStars.LOG.error("[BTS render test] server task failed", t);
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public static final class ServerHook {

        private int ticks;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Runnable task;
            while ((task = TASKS.poll()) != null) task.run();
            retireOldTickets();
            if (++ticks % 20 == 0) {
                final Built b = current;
                final Case c = currentCase;
                if (b != null && c != null && b.error == null) {
                    try {
                        maintain(world(), b, c);
                    } catch (RuntimeException bad) {
                        BeyondTheStars.LOG.warn("[BTS render test] keep-alive failed: " + bad);
                    }
                }
            }
        }
    }

    // ============================================================================ chunk tickets

    /** Tickets holding the current site (and its camera spots) loaded. */
    private static final List<ForgeChunkManager.Ticket> HELD = new ArrayList<>();
    /** The previous site's tickets, released only once the player has left it. */
    private static final List<ForgeChunkManager.Ticket> RETIRING = new ArrayList<>();
    /** Server tick at which RETIRING goes (-1: not scheduled). */
    private static int retireAt = -1;
    private static int serverTicks;
    private static boolean callbackSet;
    /** Ticks between the first teleport onto a new site and the release of the old site's tickets. */
    private static final int RETIRE_DELAY = 100;

    /**
     * Force-loads every chunk of {@code box} with the harness's own tickets. Forge queues a chunk for unloading the
     * moment its ticket is released, even with a player standing in it, and unloading that chunk takes the player
     * entity out of the world (it stops ticking, so the server never sends it another chunk and the client draws only
     * sky). So a site's tickets are never released while the player may still be there: see {@link #retireOldTickets}.
     */
    private static void hold(World world, int[] box) {
        final ModContainer mc = Loader.instance()
            .getIndexedModList()
            .get(BeyondTheStars.MODID);
        if (mc == null) return;
        final Object mod = mc.getMod();
        if (!callbackSet) {
            ForgeChunkManager.setForcedChunkLoadingCallback(
                mod,
                (tickets, w) -> { for (ForgeChunkManager.Ticket t : tickets) ForgeChunkManager.releaseTicket(t); });
            callbackSet = true;
        }
        ForgeChunkManager.Ticket ticket = null;
        for (int cx = box[0] >> 4; cx <= box[3] >> 4; cx++) {
            for (int cz = box[2] >> 4; cz <= box[5] >> 4; cz++) {
                if (ticket == null || ticket.getChunkList()
                    .size() >= ticket.getMaxChunkListDepth()) {
                    ticket = ForgeChunkManager.requestTicket(mod, world, ForgeChunkManager.Type.NORMAL);
                    if (ticket == null) return;
                    HELD.add(ticket);
                }
                ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(cx, cz));
            }
        }
    }

    /** A new site: the old site's tickets retire once the player has been moved off it ({@link #view}). */
    private static void retireCurrentSite() {
        RETIRING.addAll(HELD);
        HELD.clear();
        retireAt = -1;
    }

    private static void retireOldTickets() {
        serverTicks++;
        if (retireAt < 0 || serverTicks < retireAt) return;
        for (ForgeChunkManager.Ticket t : RETIRING) ForgeChunkManager.releaseTicket(t);
        RETIRING.clear();
        retireAt = -1;
    }

    /** Releases every harness ticket (end of the run). */
    static String releaseAll() {
        final int n = HELD.size() + RETIRING.size();
        for (ForgeChunkManager.Ticket t : RETIRING) ForgeChunkManager.releaseTicket(t);
        for (ForgeChunkManager.Ticket t : HELD) ForgeChunkManager.releaseTicket(t);
        RETIRING.clear();
        HELD.clear();
        return n + " chunk tickets released";
    }

    static WorldServer world() {
        return MinecraftServer.getServer()
            .worldServerForDimension(0);
    }

    static EntityPlayerMP player() {
        final List<?> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        return players.isEmpty() ? null : (EntityPlayerMP) players.get(0);
    }

    /** Once, after the player is in: no daylight cycle, clear weather, the player flying in creative. */
    static String prepareWorld() {
        final WorldServer world = world();
        world.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        world.getWorldInfo()
            .setRaining(false);
        world.getWorldInfo()
            .setThundering(false);
        world.getWorldInfo()
            .setRainTime(1_000_000);
        world.setWorldTime(6000);
        final EntityPlayerMP p = player();
        if (p != null) fly(p);
        return "world prepared (no daylight cycle, clear, " + (p == null ? "no player" : p.getCommandSenderName())
            + ")";
    }

    static void fly(EntityPlayerMP p) {
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        p.fallDistance = 0;
    }

    /** Builds (or rebuilds over) one site and force-loads its chunks; the previous site's tickets are released. */
    static Built buildSite(Site site) {
        final WorldServer world = world();
        retireCurrentSite();
        current = null;
        currentCase = null;
        final Built b = new Built();
        b.site = site;
        final long start = System.nanoTime();
        try {
            switch (site) {
                case KERR_T1:
                case KERR_T2:
                    buildKerr(world, b, site == Site.KERR_T2);
                    break;
                case GT_BHC:
                    buildBhc(world, b);
                    break;
                case TT_EOH:
                    buildTecTechEye(world, b);
                    break;
                case GT_GODFORGE:
                    buildGodforge(world, b);
                    break;
                case QUASAR:
                    buildQuasar(world, b);
                    break;
                case HOMOGENY:
                    buildHR(world, b, HRKit.Machine.HOMOGENY);
                    break;
                case REALITY_DIFFRACTION:
                    buildHR(world, b, HRKit.Machine.DIFFRACTION);
                    break;
                case REALITY_REFAB:
                    buildHR(world, b, HRKit.Machine.REFAB);
                    break;
                case CONDUIT:
                    buildConduit(world, b);
                    break;
                default:
                    buildEye(world, b, site.eyeTier());
                    break;
            }
        } catch (RuntimeException bad) {
            BeyondTheStars.LOG.error("[BTS render test] building " + site + " failed", bad);
            b.error = "build threw " + bad;
        }
        b.lines.add("built " + site + " in " + (System.nanoTime() - start) / 1_000_000L + " ms");
        current = b;
        return b;
    }

    static int[] loadAndKeep(World world, int[] box) {
        SLConsoleTest.loadChunks(world, box);
        hold(world, box);
        return box;
    }

    private static void buildKerr(WorldServer world, Built b, boolean tier2) {
        final int x = b.site.originX();
        final int z = SITE_Z;
        final int[] probe = KerrKit.assemblyBounds(FACING, x, 100, z, tier2);
        final int y = 100 + GROUND - probe[1];
        b.box = loadAndKeep(world, KerrKit.assemblyBounds(FACING, x, y, z, tier2));
        b.controller = new int[] { x, y, z };
        final KerrKit.Result r = KerrKit.build(world, x, y, z, FACING, tier2);
        if (r.failure != null) {
            b.error = "Kerr build failed: " + r.failure;
            return;
        }
        for (String line : r.lines) b.lines.add(strip(line));
        final MTEKerrEngine kee = KerrTestBench.keeAt(world, x, y, z);
        if (kee == null) {
            b.error = "no KEE after the build";
            return;
        }
        if (!kee.mMachine) b.lines.add("WARNING: KEE NOT FORMED");
        final int[] c = KerrSlot.worldPos(kee.getExtendedFacing(), x, y, z, KerrSlot.centreRel());
        b.centre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
    }

    /** P4: a formed Quasar Engine (coils T3, creative hatches, Helium) standing on the ground, facing north. */
    private static void buildQuasar(WorldServer world, Built b) {
        final int x = b.site.originX();
        final int z = SITE_Z;
        final int[] probe = QuasarKit.bounds(FACING, x, 100, z);
        final int y = 100 + GROUND - probe[1];
        b.box = loadAndKeep(world, QuasarKit.bounds(FACING, x, y, z));
        b.controller = new int[] { x, y, z };
        final QuasarKit.Result r = QuasarKit.build(world, x, y, z, FACING, 3, false, false);
        if (r.failure != null) {
            b.error = "Quasar build failed: " + r.failure;
            return;
        }
        for (String line : r.lines) b.lines.add(strip(line));
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, x, y, z);
        if (q == null) {
            b.error = "no Quasar Engine after the build";
            return;
        }
        if (!q.mMachine) b.lines.add("WARNING: Quasar NOT FORMED");
        final int[] c = QuasarGeometry.world(q.getExtendedFacing(), x, y, z, QuasarGeometry.centreRel());
        b.centre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
    }

    /**
     * Renders v2 (R-01): a Homogeny (top tier) or a Reality machine raised by {@link HRKit#build} (no fill), standing
     * on
     * the ground facing north; the render centre is its bounding box's centre (none of them draws anything yet).
     */
    private static void buildHR(WorldServer world, Built b, HRKit.Machine m) {
        final int x = b.site.originX();
        final int z = SITE_Z;
        final int tiers = m.maxTier();
        final String[] pieces = Arrays.copyOf(m.pieces, tiers);
        final int[] probe = SLConsoleTest.bounds(ExtendedFacing.of(FACING), x, 100, z, pieces);
        final int y = 100 + GROUND - probe[1];
        b.box = loadAndKeep(world, SLConsoleTest.bounds(ExtendedFacing.of(FACING), x, y, z, pieces));
        b.controller = new int[] { x, y, z };
        if (b.box[4] > 255) {
            b.error = m.title() + " does not fit under y 255 (top " + b.box[4] + ")";
            return;
        }
        final HRKit.Result r = HRKit.build(world, x, y, z, FACING, m, tiers, false, OWNER, "BTS render test", false);
        if (r.failure != null) {
            b.error = m.title() + " build failed: " + r.failure;
            return;
        }
        for (String line : r.lines) b.lines.add(strip(line));
        if (!r.formed) b.lines.add("WARNING: " + m.title() + " NOT FORMED");
        b.centre = new double[] { (b.box[0] + b.box[3] + 1) / 2.0, (b.box[1] + b.box[4] + 1) / 2.0,
            (b.box[2] + b.box[5] + 1) / 2.0 };
    }

    /**
     * Renders v2 (R-01): the conduit rig ({@link ConduitKit#rig}, the conduit bench): a quasar seeded at 64 SM with a
     * Mk I crown on its top port, mouths, mirrors and a junction. The render centre is the top crown's controller; the
     * front views frame the quasar.
     */
    private static void buildConduit(WorldServer world, Built b) {
        final int x = b.site.originX();
        final int z = SITE_Z;
        final int[] probe = QuasarKit.bounds(FACING, x, 100, z);
        final int y = 100 + GROUND - probe[1];
        b.box = loadAndKeep(world, QuasarKit.bounds(FACING, x, y, z));
        b.controller = new int[] { x, y, z };
        for (String line : ConduitKit.rig(world, x, y, z)) b.lines.add(strip(line));
        final ConduitKit.Rig rig = ConduitKit.lastRig();
        if (rig == null || rig.quasar == null) {
            b.error = "the conduit rig did not build its quasar";
            return;
        }
        final int[] c = rig.crownTop != null ? rig.crownTop
            : QuasarGeometry.world(ExtendedFacing.of(FACING), x, y, z, QuasarGeometry.centreRel());
        b.centre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
    }

    /**
     * P4: the quasar case's hole: the mass, spun up to its equilibrium, 30 disc seconds run; the eruption case erupts.
     */
    private static String applyQuasar(WorldServer world, Built b, Case c) {
        final int[] p = b.controller;
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, p[0], p[1], p[2]);
        if (q == null) return "no Quasar Engine";
        q.debugSeed(c.mass, QuasarParams.aEq(1, 3));
        q.benchRun(30, true);
        if (c.name.contains("eruption")) {
            q.state().Phi = 99.9;
            q.benchRun(1, true);
        }
        return String.format(
            Locale.ROOT,
            "Quasar %s: M %.0f SM, hole scale %.2f, lambda_c %.2f, zone %s, G %.2f Bu/t (%.2f per jet)%s",
            q.holeName(),
            q.getMass(),
            MTEQuasarEngine.renderScaleFor(q.getMass()),
            q.state().lc,
            MTEQuasarEngine.zoneName(q.state().zone),
            q.state().G,
            q.jetBu(JetPort.TOP),
            q.mMachine ? "" : ", NOT FORMED");
    }

    private static void buildEye(WorldServer world, Built b, int tiers) {
        final int x = b.site.originX();
        final int z = SITE_Z;
        final String[] pieces = Arrays.copyOf(MTEEyeOfHarmonyBTS.PIECES, tiers);
        final int[] probe = SLConsoleTest.bounds(ExtendedFacing.of(FACING), x, 100, z, pieces);
        final int y = 100 + GROUND - probe[1];
        b.box = loadAndKeep(world, SLConsoleTest.bounds(ExtendedFacing.of(FACING), x, y, z, pieces));
        b.controller = new int[] { x, y, z };
        if (b.box[4] > 255) {
            b.error = "the Eye T" + tiers + " does not fit under y 255 (top " + b.box[4] + ")";
            return;
        }
        final EyeKit.Result r = EyeKit.build(world, x, y, z, FACING, tiers, 8, false, OWNER, "BTS render test", false);
        if (r.failure != null) {
            b.error = "Eye build failed: " + r.failure;
            return;
        }
        for (String line : r.lines) b.lines.add(strip(line));
        final MTEEyeOfHarmonyBTS eye = eyeAt(world, x, y, z);
        if (eye == null) {
            b.error = "no Eye controller after the build";
            return;
        }
        if (!eye.mMachine) b.lines.add("WARNING: Eye NOT FORMED");
        final int[] c = EyeCentreRender.centrePos(eye.getExtendedFacing(), x, y, z);
        b.centre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
    }

    /**
     * GT's Black Hole Compressor as built by its own {@code construct}: three of its hatch-capable casings become a
     * creative energy hatch, an input bus and an output bus (its minimum to form), then the hole is opened as a Black
     * Hole Seed would (status 2, GT's own {@code createRenderBlock}); the keep-alive holds its stability at 100.
     */
    private static void buildBhc(WorldServer world, Built b) {
        final ItemStack stack = ItemList.Machine_Multi_BlackHoleCompressor.get(1L);
        if (stack == null) {
            b.error = "GT's Black Hole Compressor is not registered";
            return;
        }
        final int x = b.site.originX();
        final int y = 40;
        final int z = SITE_Z;
        loadAndKeep(world, new int[] { x - 48, 0, z - 48, x + 48, 255, z + 48 });
        b.controller = new int[] { x, y, z };
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) stack.getItemDamage(), FACING)) {
            b.error = "could not place the BHC controller";
            return;
        }
        final MTEBlackHoleCompressor bhc = mteAt(world, x, y, z, MTEBlackHoleCompressor.class);
        if (bhc == null) {
            b.error = "the BHC controller did not initialise";
            return;
        }
        bhc.construct(new ItemStack(stack.getItem(), 1, stack.getItemDamage()), false);
        b.box = scanBox(world, x, y, z, 40);
        // its minimum hatches, on hatch-capable casings (sBlockCasings10:12) nearest the controller
        final ItemList[] hatches = { ItemList.DebugEnergyHatch, ItemList.Hatch_Input_Bus_MAX,
            ItemList.Hatch_Output_Bus_MAX };
        final List<int[]> cells = new ArrayList<>();
        for (int bx = b.box[0]; bx <= b.box[3]; bx++) {
            for (int by = b.box[1]; by <= b.box[4]; by++) {
                for (int bz = b.box[2]; bz <= b.box[5]; bz++) {
                    if (world.getBlock(bx, by, bz) != GregTechAPI.sBlockCasings10
                        || world.getBlockMetadata(bx, by, bz) != 12) continue;
                    if (airSide(world, bx, by, bz) == null) continue;
                    cells.add(new int[] { bx, by, bz });
                }
            }
        }
        cells.sort((p, q) -> Integer.compare(dist2(p, x, y, z), dist2(q, x, y, z)));
        int fitted = 0;
        for (int i = 0; i < cells.size() && fitted < hatches.length; i++) {
            final int[] p = cells.get(i);
            final ItemStack h = hatches[fitted].get(1L);
            if (h == null) break;
            final ForgeDirection side = airSide(world, p[0], p[1], p[2]);
            if (MTEReactorCoreMk1.placeGtMachine(world, p[0], p[1], p[2], (short) h.getItemDamage(), side)) {
                if (fitted == 0) {
                    final MTEHatchEnergyDebug e = mteAt(world, p[0], p[1], p[2], MTEHatchEnergyDebug.class);
                    if (e != null) {
                        e.setVoltageTier(12);
                        e.setAmperage(16);
                    }
                }
                fitted++;
            }
        }
        final IGregTechTileEntity base = bhc.getBaseMetaTileEntity();
        bhc.checkStructure(true, base);
        base.enableWorking();
        b.lines.add(
            "GT BHC " + (bhc.mMachine ? "FORMED" : "NOT FORMED")
                + " at "
                + pos(b.controller)
                + ", "
                + fitted
                + " hatches fitted on casings nearest the controller");
        if (!bhc.mMachine) {
            for (String line : SLConsoleTest.firstErrors(bhc, base, 3)) b.lines.add("  " + strip(line));
            b.error = "the BHC did not form";
            return;
        }
        // GT: the render block stands 7 behind the controller and 11 up (MTEBlackHoleCompressor.createRenderBlock)
        final ForgeDirection back = FACING.getOpposite();
        b.centre = new double[] { x + 7 * back.offsetX + 0.5, y + 11 + 0.5, z + 7 * back.offsetZ + 0.5 };
        b.lines.add(openBhc(bhc));
    }

    /** Opens the BHC's hole exactly as its seed does: status 2 and GT's private createRenderBlock. */
    private static String openBhc(MTEBlackHoleCompressor bhc) {
        try {
            final Field status = MTEBlackHoleCompressor.class.getDeclaredField("blackHoleStatus");
            status.setAccessible(true);
            status.setByte(bhc, (byte) 2);
            setBhcStability(bhc);
            final Method create = MTEBlackHoleCompressor.class.getDeclaredMethod("createRenderBlock");
            create.setAccessible(true);
            final boolean made = (Boolean) create.invoke(bhc);
            return "GT BHC hole opened (status 2, stable), render block " + (made ? "created" : "NOT created");
        } catch (ReflectiveOperationException | RuntimeException bad) {
            return "GT BHC: could not open the hole by reflection: " + bad;
        }
    }

    private static void setBhcStability(MTEBlackHoleCompressor bhc) throws ReflectiveOperationException {
        final Field stability = MTEBlackHoleCompressor.class.getDeclaredField("blackHoleStability");
        stability.setAccessible(true);
        stability.setFloat(bhc, 100f);
    }

    /**
     * TecTech's Eye of Harmony as built by its own {@code construct} (generators at tier 9). It does not form without
     * its hatches and a recipe, and TecTech only places its star while a recipe runs, so the star is placed the way
     * its private {@code createRenderBlock} does: TecTech's render block 16 behind the controller, tier and size as
     * for a top recipe (rocket tier 8, spacetime tier 8: size 0.4 + 8/8).
     */
    private static void buildTecTechEye(WorldServer world, Built b) {
        final ItemStack stack = CustomItemList.Machine_Multi_EyeOfHarmony.get(1L);
        if (stack == null) {
            b.error = "TecTech's Eye of Harmony is not registered";
            return;
        }
        final int x = b.site.originX();
        final int y = 40;
        final int z = SITE_Z;
        loadAndKeep(world, new int[] { x - 48, 0, z - 48, x + 48, 255, z + 48 });
        b.controller = new int[] { x, y, z };
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) stack.getItemDamage(), FACING)) {
            b.error = "could not place the TecTech EOH controller";
            return;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        final IMetaTileEntity mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
            : null;
        if (!(mte instanceof ISurvivalConstructable)) {
            b.error = "the TecTech EOH controller did not initialise";
            return;
        }
        ((ISurvivalConstructable) mte).construct(new ItemStack(stack.getItem(), 9, stack.getItemDamage()), false);
        b.box = scanBox(world, x, y, z, 40);
        // an unformed TT multi that is allowed to work calls stopMachine every check, which removes its star
        ((IGregTechTileEntity) te).disableWorking();
        final ForgeDirection back = FACING.getOpposite();
        final int cx = x + 16 * back.offsetX;
        final int cz = z + 16 * back.offsetZ;
        b.centre = new double[] { cx + 0.5, y + 0.5, cz + 0.5 };
        if (TTCasingsContainer.eyeOfHarmonyRenderBlock == null) {
            b.error = "TecTech's EOH render block is not registered";
            return;
        }
        final TileEntity star = placeTecTechStar(world, cx, y, cz);
        b.lines.add(
            "TecTech EOH built by its construct (tier-9 generators) at " + pos(b.controller)
                + "; not formed (no hatches, no recipe); star placed as its createRenderBlock does: "
                + (star instanceof TileEntityEyeOfHarmony ? "rocket tier 8, size 1.4" : "NO RENDER TILE"));
    }

    /** TecTech's star at (x, y, z): its render block, rocket tier 8, size 0.4 + 8 / 8 (a top recipe). */
    private static TileEntity placeTecTechStar(World world, int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, TTCasingsContainer.eyeOfHarmonyRenderBlock);
        final TileEntity star = world.getTileEntity(x, y, z);
        if (star instanceof TileEntityEyeOfHarmony) {
            ((TileEntityEyeOfHarmony) star).setTier(8);
            ((TileEntityEyeOfHarmony) star).setStarSize(0.4 + 8 / 8.0);
            world.markBlockForUpdate(x, y, z);
        }
        return star;
    }

    /** The Forge of the Gods' default star size (TecTech's {@code ForgeOfGodsData.DEFAULT_STAR_SIZE}). */
    private static final int GODFORGE_STAR_RADIUS = 20;

    /**
     * Render QA round 4: TecTech's Forge of the Gods star as the reference for "as good as the Godforge star". Only its
     * render block stands on the site (the whole Godforge would be some 250 blocks across and needs its modules to
     * form); it is set up as {@code MTEForgeOfGods.createRenderer} / {@code updateRenderer} do with a fresh Godforge's
     * data: star size 20, rotation speed 5, one ring, the default colour, the controller facing north. Its rings and
     * beam are drawn too, as in game.
     */
    private static void buildGodforge(WorldServer world, Built b) {
        if (TTCasingsContainer.forgeOfGodsRenderBlock == null) {
            b.error = "TecTech's Forge of the Gods render block is not registered";
            return;
        }
        final int x = b.site.originX();
        final int y = 100;
        final int z = SITE_Z;
        loadAndKeep(world, new int[] { x - 80, 0, z - 80, x + 80, 255, z + 80 });
        b.controller = new int[] { x, y, z };
        b.centre = new double[] { x + 0.5, y + 0.5, z + 0.5 };
        final int r = GODFORGE_STAR_RADIUS + 4;
        b.box = new int[] { x - r, y - r, z - r, x + r, y + r, z + r };
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, TTCasingsContainer.forgeOfGodsRenderBlock);
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityForgeOfGods)) {
            b.error = "the Forge of the Gods render block has no TileEntityForgeOfGods";
            return;
        }
        final TileEntityForgeOfGods fog = (TileEntityForgeOfGods) te;
        fog.setRenderRotation(Rotation.NORMAL, FACING);
        fog.setRingCount(1);
        fog.setStarRadius(GODFORGE_STAR_RADIUS);
        fog.setRotationSpeed(5);
        fog.setColor(ForgeOfGodsStarColor.DEFAULT);
        fog.updateToClient();
        b.lines.add(
            "TecTech Forge of the Gods render block at " + pos(b.controller)
                + ": star radius "
                + GODFORGE_STAR_RADIUS
                + ", rotation speed 5, 1 ring, default colour (no structure)");
    }

    /** Sets one case's state on its built site. */
    static String applyCase(Case c) {
        final Built b = current;
        if (b == null || b.error != null) return "site not built";
        final WorldServer world = world();
        currentCase = c;
        final int[] p = b.controller;
        switch (c.site) {
            case KERR_T1:
            case KERR_T2: {
                final MTEKerrEngine kee = KerrTestBench.keeAt(world, p[0], p[1], p[2]);
                if (kee == null) return "no KEE";
                kee.debugOpenHole(c.mass);
                return "KEE hole " + (kee.isHoleOpen() ? "OPEN" : "closed")
                    + " at "
                    + MTEKerrEngine.fmt(kee.mass())
                    + " SM (setpoint "
                    + kee.getSetpoint()
                    + String.format(
                        Locale.ROOT,
                        "), core scale %.2f, halo radius %.1f blocks, GL scale %.2f",
                        MTEKerrEngine.coreScaleFor(kee.mass()),
                        MTEKerrEngine.haloRadiusFor(kee.mass()),
                        MTEKerrEngine.renderScaleFor(kee.mass()))
                    + (kee.mMachine ? "" : ", NOT FORMED");
            }
            case GT_BHC:
            case TT_EOH:
            case GT_GODFORGE:
                return "reference machine";
            case HOMOGENY:
            case REALITY_DIFFRACTION:
            case REALITY_REFAB:
                return "no render yet (renders v2 target)";
            case CONDUIT:
            case QUASAR:
                return applyQuasar(world, b, c);
            default: {
                final MTEEyeOfHarmonyBTS eye = eyeAt(world, p[0], p[1], p[2]);
                if (eye == null) return "no Eye";
                eye.debugForceLook(c.look);
                return "Eye centre look: " + c.look.label()
                    + String.format(
                        Locale.ROOT,
                        " (radius %.1f, rgb %06X, gamma %.2f, spin %.1f, pulse %.2f, aux %.2f%s)",
                        c.look.radius,
                        c.look.rgb,
                        c.look.gamma,
                        c.look.spin,
                        c.look.pulse,
                        c.look.aux,
                        c.eventAge >= 0 ? ", event age frozen at " + c.eventAge : "")
                    + (eye.mMachine ? "" : ", NOT FORMED");
            }
        }
    }

    /** Once a second while a case is photographed: holds the Kerr's mass, the BHC's stability, TecTech's star. */
    private static void maintain(WorldServer world, Built b, Case c) {
        final int[] p = b.controller;
        if (c.site == Site.KERR_T1 || c.site == Site.KERR_T2) {
            final MTEKerrEngine kee = KerrTestBench.keeAt(world, p[0], p[1], p[2]);
            if (kee != null && Math.abs(kee.mass() - c.mass) > 0.5) kee.debugOpenHole(c.mass);
        } else if (c.site == Site.GT_BHC) {
            final MTEBlackHoleCompressor bhc = mteAt(world, p[0], p[1], p[2], MTEBlackHoleCompressor.class);
            if (bhc != null) {
                try {
                    setBhcStability(bhc);
                } catch (ReflectiveOperationException ignored) {
                    // reported at the build
                }
            }
        } else if (c.site == Site.QUASAR || c.site == Site.CONDUIT) {
            // the disc keeps running on its own: keep its gas stocked and its mass where the case wants it
            final MTEQuasarEngine q = QuasarKit.quasarAt(world, p[0], p[1], p[2]);
            if (q != null) {
                q.debugFillBuffer();
                if (Math.abs(q.getMass() - c.mass) > 1) q.debugMass(c.mass);
            }
        } else if (c.site == Site.TT_EOH) {
            final int x = (int) Math.floor(b.centre[0]);
            final int y = (int) Math.floor(b.centre[1]);
            final int z = (int) Math.floor(b.centre[2]);
            if (world.getBlock(x, y, z) != TTCasingsContainer.eyeOfHarmonyRenderBlock) {
                BeyondTheStars.LOG.info("[BTS render test] TecTech's star was removed; placing it again");
                placeTecTechStar(world, x, y, z);
            }
        }
        final EntityPlayerMP player = player();
        if (player != null && !player.capabilities.isFlying) fly(player);
    }

    /** Live status for the log, read at the moment of a shot. */
    static String status() {
        return serverView() + "; " + siteStatus() + serverDirt();
    }

    /** Server-side block-update requests (they send the block and its tile to the watching clients), render QA P-1. */
    private static RenderDirtTracker serverDirt;

    /** What asked the server to resend a block since the last shot (then resets), or "" if nothing. */
    private static String serverDirt() {
        final WorldServer world = world();
        if (serverDirt == null || serverDirt.world() != world) {
            serverDirt = new RenderDirtTracker(world);
            world.addWorldAccess(serverDirt);
            return "";
        }
        final String top = serverDirt.top(4);
        final String paths = serverDirt.stacks();
        serverDirt.reset();
        final EntityPlayerMP p = player();
        final String sent = p == null ? "no player"
            : PacketSpy.install(p)
                .drain();
        return "; server block updates since the last shot: " + top
            + (paths.isEmpty() ? "" : "; path: " + paths)
            + "; machine description packets sent: "
            + sent;
    }

    /** The server's side of the camera (diagnostics): where it has the player, what it still owes it, chunk state. */
    private static String serverView() {
        final EntityPlayerMP p = player();
        if (p == null) return "server: no player";
        final WorldServer world = world();
        final int x = (int) Math.floor(p.posX);
        final int z = (int) Math.floor(p.posZ);
        String chunk = "unloaded";
        if (world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) {
            final net.minecraft.world.chunk.Chunk ch = world.getChunkFromBlockCoords(x, z);
            chunk = "ticked " + ch.field_150815_m
                + " populated "
                + ch.isTerrainPopulated
                + " lit "
                + ch.isLightPopulated;
        }
        boolean moved = false;
        try {
            final Field f = cpw.mods.fml.relauncher.ReflectionHelper
                .findField(net.minecraft.network.NetHandlerPlayServer.class, "hasMoved", "field_147380_r");
            moved = f.getBoolean(p.playerNetServerHandler);
        } catch (Exception ignored) {
            // diagnostics only
        }
        return String.format(
            Locale.ROOT,
            "server player (%.1f, %.1f, %.1f) managed (%.1f, %.1f), owed %d chunks, hasMoved %s, in world %s, dead %s, chunk %s, ticksExisted %d, in entity list %s, added to chunk %s (%d, %d), 32-block area loaded %s",
            p.posX,
            p.posY,
            p.posZ,
            p.managedPosX,
            p.managedPosZ,
            p.loadedChunks.size(),
            moved,
            world.playerEntities.contains(p),
            p.isDead,
            chunk,
            p.ticksExisted,
            world.loadedEntityList.contains(p),
            p.addedToChunk,
            p.chunkCoordX,
            p.chunkCoordZ,
            world.checkChunksExist(x - 32, 0, z - 32, x + 32, 0, z + 32));
    }

    private static String siteStatus() {
        final Built b = current;
        final Case c = currentCase;
        if (b == null || c == null || b.error != null) return "";
        final WorldServer world = world();
        final int[] p = b.controller;
        if (c.site == Site.KERR_T1 || c.site == Site.KERR_T2) {
            final MTEKerrEngine kee = KerrTestBench.keeAt(world, p[0], p[1], p[2]);
            return kee == null ? "no KEE"
                : "mass " + MTEKerrEngine.fmt(kee.mass())
                    + " SM, "
                    + (kee.mMachine ? "formed" : "NOT FORMED")
                    + (kee.getRenderNote()
                        .isEmpty() ? "" : ", " + kee.getRenderNote());
        }
        if (c.site == Site.GT_BHC || c.site == Site.TT_EOH || c.site == Site.GT_GODFORGE) {
            return renderBlockAt(world, b.centre);
        }
        if (c.site == Site.HOMOGENY || c.site == Site.REALITY_DIFFRACTION || c.site == Site.REALITY_REFAB) {
            return "no render yet";
        }
        if (c.site == Site.QUASAR || c.site == Site.CONDUIT) {
            final MTEQuasarEngine q = QuasarKit.quasarAt(world, p[0], p[1], p[2]);
            return q == null ? "no Quasar Engine"
                : String.join(
                    "; ",
                    q.statusLines()
                        .subList(
                            0,
                            Math.min(
                                3,
                                q.statusLines()
                                    .size())))
                    + ", "
                    + renderBlockAt(world, b.centre);
        }
        final MTEEyeOfHarmonyBTS eye = eyeAt(world, p[0], p[1], p[2]);
        return eye == null ? "no Eye"
            : (eye.mMachine ? "formed" : "NOT FORMED") + ", centre: "
                + renderBlockAt(world, b.centre)
                + (eye.getRenderNote()
                    .isEmpty() ? "" : ", " + eye.getRenderNote());
    }

    private static String renderBlockAt(World world, double[] c) {
        final int x = (int) Math.floor(c[0]);
        final int y = (int) Math.floor(c[1]);
        final int z = (int) Math.floor(c[2]);
        final Block block = world.getBlock(x, y, z);
        return isRenderBlock(block) ? "render block present"
            : "NO render block (" + Block.blockRegistry.getNameForObject(block) + ")";
    }

    // =============================================================================== views

    /**
     * The viewpoints of every case, each shot by day and by night: by default {@code front, 34, close, level};
     * {@code -Dbts.rendertest.views=a,b} picks others, e.g. {@code hero} (renders v2, R-01: the detail close-up).
     */
    static final String[] VIEWS = System.getProperty("bts.rendertest.views", "front,34,close,level")
        .trim()
        .split("\\s*,\\s*");

    /**
     * Sets the time of day and moves the player to view {@code view} of the current site: {@code front} (level with
     * the structure's middle, in front of the controller, far enough that the whole box fits a 70 degree view),
     * {@code 34} (three-quarter from the front-right, raised) or {@code close} (inside the open space around the
     * render object, between it and the front, looking at it) or {@code level} (the window-band view, render K2 / E-9:
     * level with the render object, 24 blocks in front of the structure's front face, looking straight at it, as a
     * player standing at its height sees it). Returns a log line.
     */
    static String view(String view, boolean night) {
        final Built b = current;
        if (b == null || b.error != null) return "no site";
        final WorldServer world = world();
        world.setWorldTime(night ? 18000 : 6000);
        final EntityPlayerMP player = player();
        if (player == null) return "no player";
        final ForgeDirection f = b.facing;
        final double[] F = { f.offsetX, 0, f.offsetZ };
        final double[] L = { -f.offsetZ, 0, f.offsetX };
        final int[] box = b.box;
        final double[] mid = { (box[0] + box[3] + 1) / 2.0, (box[1] + box[4] + 1) / 2.0, (box[2] + box[5] + 1) / 2.0 };
        final double hx = (box[3] - box[0] + 1) / 2.0;
        final double hy = (box[4] - box[1] + 1) / 2.0;
        final double hz = (box[5] - box[2] + 1) / 2.0;
        final double depth = Math.abs(F[0]) * hx + Math.abs(F[2]) * hz;
        final double lateral = Math.abs(F[0]) * hz + Math.abs(F[2]) * hx;
        final double far = Math.min(
            MAX_VIEW_DISTANCE,
            depth + 1.1 * Math.max(hy / Math.tan(Math.toRadians(35)), lateral / Math.tan(Math.toRadians(50))) + 3);
        double[] target;
        double[] eye;
        switch (view) {
            case "front":
                target = mid;
                eye = add(mid, F, far);
                break;
            case "level": {
                target = b.centre;
                final double ext = F[0] != 0
                    ? Math.max((box[0] - b.centre[0]) * F[0], (box[3] + 1 - b.centre[0]) * F[0])
                    : Math.max((box[2] - b.centre[2]) * F[2], (box[5] + 1 - b.centre[2]) * F[2]);
                eye = add(b.centre, F, Math.min(MAX_VIEW_DISTANCE, ext + 24));
                break;
            }
            case "34": {
                target = mid;
                final double[] dir = norm(new double[] { F[0] + L[0], 0.9, F[2] + L[2] });
                eye = add(mid, dir, Math.min(MAX_VIEW_DISTANCE, far * 1.15));
                break;
            }
            case "ring":
            case "cap": {
                // renders v2 (WP-2): the quasar's ring from inside the shell, high over its near side looking in at
                // the hole and the far side ("ring"), and its top cap from outside ("cap"); other sites use the
                // close view
                if (b.site == Site.QUASAR || b.site == Site.CONDUIT) {
                    final double[] up = { 0, 1, 0 };
                    if (view.equals("ring")) {
                        eye = add(add(b.centre, F, 52), up, 46);
                        target = add(b.centre, F, -12);
                    } else {
                        eye = add(add(add(b.centre, F, 70), L, 30), up, 112);
                        target = add(b.centre, up, 78);
                    }
                    break;
                }
                target = b.centre;
                eye = add(b.centre, norm(new double[] { F[0], 0.35, F[2] }), objectRadius(b) * 1.9 + 1.5);
                break;
            }
            case "hero": {
                // renders v2 (R-01): the detail close-up, 3 object radii out, 30 degrees to the right of the front and
                // 12 degrees above the disc plane (so a disc shows its lensed far side), never past a block
                target = b.centre;
                final double radius = heroRadius(b);
                final double[] dir = norm(
                    new double[] { F[0] * Math.cos(Math.toRadians(30)) + L[0] * Math.sin(Math.toRadians(30)),
                        Math.tan(Math.toRadians(12)),
                        F[2] * Math.cos(Math.toRadians(30)) + L[2] * Math.sin(Math.toRadians(30)) });
                final double free = freeDistance(world, b.centre, dir, 120);
                eye = add(b.centre, dir, Math.max(2.5, Math.min(free - 2, 3 * radius)));
                break;
            }
            default: {
                target = b.centre;
                final double radius = objectRadius(b);
                double[] dir = norm(new double[] { F[0], 0.35, F[2] });
                double free = freeDistance(world, b.centre, dir, 80);
                double want = Math.max(free * 0.55, radius * 1.9 + 1.5);
                if (radius >= 9) {
                    // render QA E5: a big object needs the camera at 2.2 radii + 4 or the frame is one blurred
                    // texture; that is outside the Eye's inner space from the front, so climb until a clear line
                    // (over the crown) is found, else take the clearest. Round 3 (H-1): from radius 9, not over 12,
                    // so Sol (23.8 blocks) and Sirius A (28.2) no longer fill the frame
                    want = 2.2 * radius + 4;
                    double[] best = dir;
                    double bestFree = free;
                    for (double up : new double[] { 0.35, 0.6, 0.9, 1.3, 2, 3.5, 8 }) {
                        final double[] d = norm(new double[] { F[0], up, F[2] });
                        final double fr = freeDistance(world, b.centre, d, 80);
                        if (fr > bestFree) {
                            best = d;
                            bestFree = fr;
                        }
                        if (fr >= want + 2) break;
                    }
                    dir = best;
                    free = bestFree;
                }
                final double d = Math.max(2.5, Math.min(free - 2, want));
                eye = add(b.centre, dir, d);
                break;
            }
        }
        // never inside a block: walk back along the view ray until the player's cells are air
        final double[] back = norm(sub(eye, target));
        int pushed = 0;
        while (pushed < 60 && !clear(world, eye)) {
            eye = add(eye, back, 1);
            pushed++;
        }
        final double dx = target[0] - eye[0];
        final double dy = target[1] - eye[1];
        final double dz = target[2] - eye[2];
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        // renders v2 (R-01): a pose from the committed poses file wins, so the AFTER shots stand exactly where the
        // BEFORE shots stood even when an object's size (and so the computed close view) changed
        final Case cc = currentCase;
        final String key = (cc == null ? "?" : cc.name) + "/" + view;
        final double[] fixed = POSES.get(key);
        String from = "computed";
        if (fixed != null) {
            eye = new double[] { b.centre[0] + fixed[0], b.centre[1] + fixed[1], b.centre[2] + fixed[2] };
            yaw = (float) fixed[3];
            pitch = (float) fixed[4];
            from = "pose file";
        } else if (!night) {
            POSE_LOG.add(
                String.format(
                    Locale.ROOT,
                    "%s\t%s\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                    cc == null ? "?" : cc.name,
                    view,
                    eye[0] - b.centre[0],
                    eye[1] - b.centre[1],
                    eye[2] - b.centre[2],
                    yaw,
                    pitch));
        }
        fly(player);
        // The server ticks a player (and so sends it chunks, EntityPlayerMP.onUpdate) only while the chunks within 32
        // blocks of it are loaded; a camera far outside the site box would stand in unloaded chunks and the client
        // would never get any terrain. Load and hold them first.
        final int ex = (int) Math.floor(eye[0]);
        final int ez = (int) Math.floor(eye[2]);
        loadAndKeep(world, new int[] { ex - 48, 0, ez - 48, ex + 48, 255, ez + 48 });
        player.playerNetServerHandler.setPlayerLocation(eye[0], eye[1] - 1.62, eye[2], yaw, pitch);
        player.motionX = 0;
        player.motionY = 0;
        player.motionZ = 0;
        if (!RETIRING.isEmpty() && retireAt < 0) retireAt = serverTicks + RETIRE_DELAY;
        // re-register the chunks the player watches now rather than on its next movement packet
        world.getPlayerManager()
            .updatePlayerPertinentChunks(player);
        return String.format(
            Locale.ROOT,
            "%s %s: eye (%.1f, %.1f, %.1f) yaw %.1f pitch %.1f (%s) -> (%.1f, %.1f, %.1f), %.1f blocks%s",
            view,
            night ? "night" : "day",
            eye[0],
            eye[1],
            eye[2],
            yaw,
            pitch,
            from,
            target[0],
            target[1],
            target[2],
            Math.sqrt(dx * dx + dy * dy + dz * dz),
            pushed > 0 ? ", pushed " + pushed + " out of blocks" : "");
    }

    // ============================================================================ poses (renders v2)

    /**
     * Renders v2 (R-01): fixed camera poses, {@code case/view -> {dx, dy, dz, yaw, pitch}} with the eye relative to the
     * case's render centre, read from {@code -Dbts.rendertest.poses=<file>} (default: none, every pose computed). The
     * file is tab-separated {@code case view dx dy dz yaw pitch}, {@code #} comments; a relative path is looked up from
     * the client's working directory and from the worktree root ({@code ../..}). Committed as
     * {@code tools/rendertest/poses.tsv}.
     */
    static final java.util.Map<String, double[]> POSES = loadPoses(System.getProperty("bts.rendertest.poses", ""));

    /** Every computed day pose of this run, in the poses-file format; the client writes them to {@code poses.tsv}. */
    static final ConcurrentLinkedQueue<String> POSE_LOG = new ConcurrentLinkedQueue<>();

    private static java.util.Map<String, double[]> loadPoses(String path) {
        final java.util.Map<String, double[]> poses = new java.util.HashMap<>();
        if (path == null || path.trim()
            .isEmpty()) return poses;
        java.io.File file = new java.io.File(path.trim());
        if (!file.isFile() && !file.isAbsolute()) file = new java.io.File("../..", path.trim());
        if (!file.isFile()) {
            BeyondTheStars.LOG.warn("[BTS render test] poses file not found: " + path);
            return poses;
        }
        try {
            for (String line : java.nio.file.Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                final String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                final String[] f = t.split("\t");
                if (f.length < 7) continue;
                poses.put(
                    f[0] + "/" + f[1],
                    new double[] { Double.parseDouble(f[2]), Double.parseDouble(f[3]), Double.parseDouble(f[4]),
                        Double.parseDouble(f[5]), Double.parseDouble(f[6]) });
            }
            BeyondTheStars.LOG.info("[BTS render test] " + poses.size() + " fixed poses from " + file);
        } catch (java.io.IOException | RuntimeException bad) {
            BeyondTheStars.LOG.error("[BTS render test] cannot read the poses file " + file, bad);
        }
        return poses;
    }

    /**
     * The radius the {@code hero} close-up frames: the object itself rather than its whole reach (the quasar's hole and
     * inner disc, not its r 40-62 ring; a star's photosphere; a galaxy's inner arms).
     */
    private static double heroRadius(Built b) {
        final Case c = currentCase;
        if (c != null && (c.site == Site.QUASAR || c.site == Site.CONDUIT) && c.mass > 0) {
            return 1.8 * MTEQuasarEngine.renderScaleFor(c.mass);
        }
        if (c != null && c.look != null
            && (c.look.kind == EyeStarLook.TRIANGULUM || c.look.kind == EyeStarLook.MILKY_WAY)) {
            return 0.6 * EyeStarLook.galaxyReach(c.look.kind);
        }
        return objectRadius(b);
    }

    /** About how far the render object reaches from its centre, in blocks. */
    private static double objectRadius(Built b) {
        final Case c = currentCase;
        if (c == null) return 3;
        if (c.look != null && (c.look.kind == EyeStarLook.TRIANGULUM || c.look.kind == EyeStarLook.MILKY_WAY)) {
            return EyeStarLook.galaxyReach(c.look.kind); // P4: the arms reach past the bulge
        }
        if (c.look != null && c.look.kind == EyeStarLook.PLANETS) return 14;
        if (c.look != null) return c.look.radius;
        if (c.site == Site.GT_GODFORGE) return GODFORGE_STAR_RADIUS;
        // the disc band reaches r 40 + 22 lambda_c
        if (c.site == Site.QUASAR) return 62;
        if (c.site == Site.CONDUIT) return 12;
        if (c.site == Site.HOMOGENY || c.site == Site.REALITY_DIFFRACTION || c.site == Site.REALITY_REFAB) return 6;
        if (c.mass > 0) return MTEKerrEngine.haloRadiusFor(c.mass);
        return 3;
    }

    /** Distance from {@code c} along {@code dir} to the first solid block (render blocks do not count). */
    private static double freeDistance(World world, double[] c, double[] dir, int max) {
        for (double t = 1.5; t < max; t += 0.5) {
            final int x = (int) Math.floor(c[0] + dir[0] * t);
            final int y = (int) Math.floor(c[1] + dir[1] * t);
            final int z = (int) Math.floor(c[2] + dir[2] * t);
            if (y < 0 || y > 255) return t;
            final Block block = world.getBlock(x, y, z);
            if (!block.isAir(world, x, y, z) && !isRenderBlock(block)) return t;
        }
        return max;
    }

    /** True when the player's two cells and the eye cell at {@code eye} are air. */
    private static boolean clear(World world, double[] eye) {
        final int x = (int) Math.floor(eye[0]);
        final int z = (int) Math.floor(eye[2]);
        final int feet = (int) Math.floor(eye[1] - 1.62);
        for (int y = feet; y <= (int) Math.floor(eye[1]) + 1; y++) {
            if (y < 0 || y > 255) continue;
            if (!world.isAirBlock(x, y, z)) return false;
        }
        return true;
    }

    private static boolean isRenderBlock(Block block) {
        return block != null && (block == BlockEyeStar.INSTANCE || block == BlockKerrHole.INSTANCE
            || block == BlockQuasarHole.INSTANCE
            || block == GregTechAPI.sBlackholeRender
            || block == TTCasingsContainer.eyeOfHarmonyRenderBlock
            || block == TTCasingsContainer.forgeOfGodsRenderBlock);
    }

    // ============================================================================= helpers

    /** Bounds of every non-air block within {@code r} of (x, y, z). */
    private static int[] scanBox(World world, int x, int y, int z, int r) {
        final int[] box = { x, y, z, x, y, z };
        for (int bx = x - r; bx <= x + r; bx++) {
            for (int bz = z - r; bz <= z + r; bz++) {
                for (int by = Math.max(GROUND, y - r); by <= Math.min(255, y + r); by++) {
                    if (world.isAirBlock(bx, by, bz)) continue;
                    box[0] = Math.min(box[0], bx);
                    box[1] = Math.min(box[1], by);
                    box[2] = Math.min(box[2], bz);
                    box[3] = Math.max(box[3], bx);
                    box[4] = Math.max(box[4], by);
                    box[5] = Math.max(box[5], bz);
                }
            }
        }
        return box;
    }

    private static ForgeDirection airSide(World world, int x, int y, int z) {
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (d == ForgeDirection.UP || d == ForgeDirection.DOWN) continue;
            if (world.isAirBlock(x + d.offsetX, y + d.offsetY, z + d.offsetZ)) return d;
        }
        return null;
    }

    private static int dist2(int[] p, int x, int y, int z) {
        return (p[0] - x) * (p[0] - x) + (p[1] - y) * (p[1] - y) + (p[2] - z) * (p[2] - z);
    }

    private static MTEEyeOfHarmonyBTS eyeAt(World world, int x, int y, int z) {
        return mteAt(world, x, y, z, MTEEyeOfHarmonyBTS.class);
    }

    private static <T> T mteAt(World world, int x, int y, int z, Class<T> type) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(mte) ? type.cast(mte) : null;
    }

    private static double[] add(double[] a, double[] d, double t) {
        return new double[] { a[0] + d[0] * t, a[1] + d[1] * t, a[2] + d[2] * t };
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    private static double[] norm(double[] v) {
        final double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1e-9 ? new double[] { 0, 0, -1 } : new double[] { v[0] / l, v[1] / l, v[2] / l };
    }

    static String pos(int[] p) {
        return "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }

    /** Chat colour codes out. */
    static String strip(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }
}
