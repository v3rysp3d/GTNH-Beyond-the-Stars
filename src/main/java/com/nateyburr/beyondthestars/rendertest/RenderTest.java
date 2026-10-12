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
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
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
import tectech.thing.casing.TTCasingsContainer;

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
    static final String WORLD_FOLDER = "bts-rendertest-L2";
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
        TT_EOH;

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

        Case(String name, Site site, double mass, EyeStarLook look, String what) {
            this(name, site, mass, look, what, -1);
        }

        Case(String name, Site site, double mass, EyeStarLook look, String what, double eventAge) {
            this.name = name;
            this.site = site;
            this.mass = mass;
            this.look = look;
            this.what = what;
            this.eventAge = eventAge;
        }
    }

    /**
     * In order: consecutive cases on one site share its structure. {@code -Dbts.rendertest.only=a,b} keeps only the
     * cases whose name starts with one of the comma-separated prefixes (e.g. {@code kerr,ref_gt_bhc}).
     */
    static final List<Case> CASES = only(
        Arrays.asList(
            new Case("kerr_t1_16sm", Site.KERR_T1, 16, null, "Kerr T1, hole open at 16 SM"),
            new Case("kerr_t1_64sm", Site.KERR_T1, 64, null, "Kerr T1, hole open at 64 SM (the T1 cap)"),
            new Case("kerr_t2_256sm", Site.KERR_T2, 256, null, "Kerr T2 (Density Upgrade), hole open at 256 SM"),
            new Case("eye_t1_sol", Site.EYE_T1, 0, EyeStarLook.lit(1, 0, 0), "Eye T1, Sol (live star)"),
            new Case("eye_t2_sirius_a", Site.EYE_T2, 0, EyeStarLook.lit(2, 0, 0), "Eye T2, Sirius A (live star)"),
            new Case("eye_t3_arcturus", Site.EYE_T3, 0, EyeStarLook.lit(3, 0, 0), "Eye T3, Arcturus (red giant)"),
            new Case("eye_t4_neutron", Site.EYE_T4, 0, EyeStarLook.lit(4, 0, 0), "Eye T4, neutron star"),
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
                    + " 30 ticks (about 7 blocks, alpha 0.42)",
                30),
            new Case(
                "eye_t5_dwarf_early",
                Site.EYE_T5,
                0,
                EyeStarLook.dwarf(0.1),
                "Eye T5, Sirius B cooling batch at 10 % (white)"),
            new Case(
                "eye_t5_dwarf_late",
                Site.EYE_T5,
                0,
                EyeStarLook.dwarf(0.9),
                "Eye T5, Sirius B cooling batch at 90 % (near black)"),
            new Case("eye_t6_magnetar", Site.EYE_T6, 0, EyeStarLook.lit(6, 0, 0), "Eye T6, SGR 1806-20 magnetar"),
            new Case(
                "eye_t6_magnetar_warn",
                Site.EYE_T6,
                0,
                EyeStarLook.lit(6, 0, 1),
                "Eye T6, magnetar on an event warning (flare shell bright), a flare frozen at 15 ticks",
                15),
            new Case("ref_gt_bhc", Site.GT_BHC, 0, null, "REFERENCE: GT Black Hole Compressor, hole open (stable)"),
            new Case("ref_tt_eoh", Site.TT_EOH, 0, null, "REFERENCE: TecTech Eye of Harmony, star render")));

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

    private static void fly(EntityPlayerMP p) {
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
                default:
                    buildEye(world, b, site.ordinal() - Site.EYE_T1.ordinal() + 1);
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

    private static int[] loadAndKeep(World world, int[] box) {
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
                return "reference machine";
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
        if (c.site == Site.GT_BHC || c.site == Site.TT_EOH) return renderBlockAt(world, b.centre);
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

    /** The three viewpoints of every case, each shot by day and by night. */
    static final String[] VIEWS = { "front", "34", "close" };

    /**
     * Sets the time of day and moves the player to view {@code view} of the current site: {@code front} (level with
     * the structure's middle, in front of the controller, far enough that the whole box fits a 70 degree view),
     * {@code 34} (three-quarter from the front-right, raised) or {@code close} (inside the open space around the
     * render object, between it and the front, looking at it). Returns a log line.
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
            case "34": {
                target = mid;
                final double[] dir = norm(new double[] { F[0] + L[0], 0.9, F[2] + L[2] });
                eye = add(mid, dir, Math.min(MAX_VIEW_DISTANCE, far * 1.15));
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
        final float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        final float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
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
            "%s %s: eye (%.1f, %.1f, %.1f) -> (%.1f, %.1f, %.1f), %.1f blocks%s",
            view,
            night ? "night" : "day",
            eye[0],
            eye[1],
            eye[2],
            target[0],
            target[1],
            target[2],
            Math.sqrt(dx * dx + dy * dy + dz * dz),
            pushed > 0 ? ", pushed " + pushed + " out of blocks" : "");
    }

    /** About how far the render object reaches from its centre, in blocks. */
    private static double objectRadius(Built b) {
        final Case c = currentCase;
        if (c == null) return 3;
        if (c.look != null) return c.look.radius;
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
            || block == GregTechAPI.sBlackholeRender
            || block == TTCasingsContainer.eyeOfHarmonyRenderBlock);
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
