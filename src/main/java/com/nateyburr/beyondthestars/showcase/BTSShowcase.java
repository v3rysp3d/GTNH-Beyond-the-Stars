package com.nateyburr.beyondthestars.showcase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;

/**
 * SHOWCASE MODE, the parts every machine shares (designs/docs/showcase-worlds.md, "Code-world showcase mode").
 *
 * <h2>Chunk tickets that come back</h2>
 *
 * A showcase machine holds Forge tickets over its whole footprint ({@link ShowcaseHost#showcaseBox}), so the Eye's
 * outer bands, the quasar's lattice and a Junction 200 blocks away stay loaded with no player near. Each ticket's
 * ModData names its machine (dim, x, y, z) and box, so after a restart {@link #ticketsLoaded} forces the same chunks
 * again before any machine has ticked. The bench tickets ({@code KerrTestBench.keep}, the render test) carry no
 * showcase tag and are released there, as they always were: this class owns the mod's one loading callback (Forge
 * allows one per mod), registered in {@code CommonProxy.init}.
 * <p>
 * A ticket whose machine is gone (broken, wrenched, or switched off) is released by {@link #sweep} once its
 * controller's chunk is loaded and the controller is not a showcase host any more.
 *
 * <h2>Once a second, per machine</h2>
 *
 * {@link #second}: the tickets, {@code fixAllIssues()} (no maintenance stops), void mode (full outputs never stall),
 * and switching the machine back on after
 * a STRUCTURE_INCOMPLETE, NO_REPAIR or POWER_LOSS stop. A player who switches a showcase machine off on purpose (soft
 * mallet) is left alone.
 */
public final class BTSShowcase {

    private BTSShowcase() {}

    /** The tile NBT key every {@link ShowcaseHost} saves its flag under. */
    public static final String NBT_KEY = "btsShowcase";

    /** The GUI / scanner line a showcase machine shows, so nobody takes it for survival behaviour. */
    public static final String STATUS_LINE = "SHOWCASE MODE: fed and held by the mod (/btsshowcase off to stop)";

    private static final String TAG = "btsShowcase";

    /** Held tickets, by machine key "dim:x,y,z". */
    private static final Map<String, Held> HELD = new HashMap<>();

    /**
     * Every chunk a showcase ticket forces, by "dim:cx,cz", and the machine whose ticket forces it. A chunk is forced
     * once however many footprints cover it (a Junction stands inside its Eye's footprint), so a whole showcase world
     * stays well inside Forge's default 200 tickets per mod.
     */
    private static final Map<String, Held> FORCED = new HashMap<>();

    private static final class Held {

        final int dim, x, y, z;
        int[] box;
        /** Its box has been claimed (the chunks no other machine forces are in its own tickets). */
        boolean settled;
        final List<ForgeChunkManager.Ticket> tickets = new ArrayList<>();
        final List<String> owned = new ArrayList<>();

        Held(int dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static String key(int dim, int x, int y, int z) {
        return dim + ":" + x + "," + y + "," + z;
    }

    private static Object mod() {
        final ModContainer mc = Loader.instance()
            .getIndexedModList()
            .get(BeyondTheStars.MODID);
        return mc == null ? null : mc.getMod();
    }

    // ---------------------------------------------------------------------------------------------- the callback

    /** Called once from {@code CommonProxy.init}: the mod's only chunk-loading callback. */
    public static void registerCallback() {
        final Object mod = mod();
        if (mod == null) return;
        ForgeChunkManager.setForcedChunkLoadingCallback(mod, BTSShowcase::ticketsLoaded);
    }

    /** After a world load: showcase tickets force their chunks again, every other ticket of ours is released. */
    private static void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        int kept = 0, released = 0;
        for (ForgeChunkManager.Ticket t : tickets) {
            final NBTTagCompound d = t.getModData();
            if (d == null || !d.getBoolean(TAG)) {
                ForgeChunkManager.releaseTicket(t);
                released++;
                continue;
            }
            final int dim = world.provider.dimensionId;
            final String k = key(dim, d.getInteger("x"), d.getInteger("y"), d.getInteger("z"));
            Held h = HELD.get(k);
            if (h == null) {
                h = new Held(dim, d.getInteger("x"), d.getInteger("y"), d.getInteger("z"));
                h.box = d.getIntArray("box");
                HELD.put(k, h);
            }
            h.settled = true;
            h.tickets.add(t);
            final int[] chunks = d.getIntArray("chunks");
            // saved under a bigger per-ticket limit than this config allows: Forge would drop the oldest chunks, so the
            // machine claims its box again on its next second
            if (chunks.length / 2 > t.getMaxChunkListDepth()) h.settled = false;
            for (int i = 0; i + 1 < chunks.length; i += 2) {
                ForgeChunkManager.forceChunk(t, new ChunkCoordIntPair(chunks[i], chunks[i + 1]));
                final String ck = chunkKey(dim, chunks[i], chunks[i + 1]);
                FORCED.put(ck, h);
                h.owned.add(ck);
            }
            kept++;
        }
        if (kept + released > 0) {
            BeyondTheStars.LOG.info(
                "[BTS] showcase: dim {} restored {} chunk ticket(s), released {} bench ticket(s)",
                world.provider.dimensionId,
                kept,
                released);
        }
    }

    // ------------------------------------------------------------------------------------------ once a second

    /**
     * The shared part of a showcase machine's second. Call it from the controller's server tick once a second
     * (whatever the flag; it releases the tickets of a machine that was switched off).
     */
    public static void second(MTEMultiBlockBase machine, ShowcaseHost host) {
        final IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        if (base == null || base.isClientSide()) return;
        final World world = base.getWorld();
        final int dim = world.provider.dimensionId;
        final String k = key(dim, base.getXCoord(), base.getYCoord(), base.getZCoord());
        if (!host.isShowcase()) {
            final Held h = HELD.remove(k);
            if (h != null) release(h);
            return;
        }
        hold(world, k, base, host.showcaseBox());
        machine.fixAllIssues();
        if (host.showcaseTopsUpHatches()) topUp(machine);
        // Full outputs never stall a showcase machine (GT's default protects them).
        if (machine.supportsVoidProtection() && machine.getVoidingMode() != gregtech.api.enums.VoidingMode.VOID_ALL) {
            machine.setVoidingMode(gregtech.api.enums.VoidingMode.VOID_ALL);
        }
        if (!base.isAllowedToWork() && machine.mMachine && restartable(base.getLastShutDownReason())) {
            base.enableWorking();
        }
    }

    /**
     * Keeps every plain input hatch full of the fluid it holds and every input-bus stack at a full stack, so a machine
     * fed once (by hand or a bench fill) runs forever. Empty hatches and slots stay empty ({@code /btsshowcase fluid}
     * seeds a hatch). Debug and ME hatches feed themselves and are left alone.
     */
    private static void topUp(MTEMultiBlockBase m) {
        for (gregtech.api.metatileentity.implementations.MTEHatchInput h : m.mInputHatches) {
            if (h == null || !h.isValid() || selfFed(h)) continue;
            if (h instanceof gregtech.api.metatileentity.implementations.MTEHatchMultiInput) {
                for (net.minecraftforge.fluids.FluidStack f : ((gregtech.api.metatileentity.implementations.MTEHatchMultiInput) h)
                    .getStoredFluid()) {
                    if (f != null && f.getFluid() != null) {
                        h.fill(new net.minecraftforge.fluids.FluidStack(f.getFluid(), Integer.MAX_VALUE), true);
                    }
                }
                continue;
            }
            final net.minecraftforge.fluids.FluidStack f = h.getFluid();
            if (f != null && f.getFluid() != null && f.amount < h.getCapacity()) {
                h.fill(new net.minecraftforge.fluids.FluidStack(f.getFluid(), h.getCapacity() - f.amount), true);
            }
        }
        for (gregtech.api.metatileentity.implementations.MTEHatchInputBus b : m.mInputBusses) {
            if (b == null || !b.isValid() || selfFed(b)) continue;
            final IGregTechTileEntity t = b.getBaseMetaTileEntity();
            for (int i = 0; i < t.getSizeInventory(); i++) {
                final net.minecraft.item.ItemStack s = t.getStackInSlot(i);
                if (s != null && s.stackSize < s.getMaxStackSize()) s.stackSize = s.getMaxStackSize();
            }
        }
    }

    /** GT's debug hatches and the ME stocking hatches / buses refill themselves. */
    private static boolean selfFed(Object hatch) {
        final String n = hatch.getClass()
            .getSimpleName();
        return n.endsWith("Debug") || n.contains("InputME") || n.contains("BusME");
    }

    private static boolean restartable(ShutDownReason reason) {
        if (reason == null) return false;
        final String id = reason.getKey();
        return id.equals(ShutDownReasonRegistry.STRUCTURE_INCOMPLETE.getKey())
            || id.equals(ShutDownReasonRegistry.NO_REPAIR.getKey())
            || id.equals(ShutDownReasonRegistry.POWER_LOSS.getKey());
    }

    private static String chunkKey(int dim, int cx, int cz) {
        return dim + ":" + cx + "," + cz;
    }

    /**
     * Claims {@code box}: every chunk in it that no other showcase machine forces goes into this machine's own
     * tickets. Re-claims when the box changed (the machine grew a tier) or a neighbour it leaned on let go.
     */
    private static void hold(World world, String k, IGregTechTileEntity base, int[] box) {
        if (box == null) return;
        Held h = HELD.get(k);
        if (h != null && java.util.Arrays.equals(h.box, box) && h.settled) return;
        // a re-claim of the same box takes back the same chunks: the neighbours need not hear of it
        if (h != null) release(h, !java.util.Arrays.equals(h.box, box));
        final Object mod = mod();
        if (mod == null) return;
        h = new Held(world.provider.dimensionId, base.getXCoord(), base.getYCoord(), base.getZCoord());
        h.box = box.clone();
        h.settled = true;
        HELD.put(k, h);
        final int dim = world.provider.dimensionId;
        ForgeChunkManager.Ticket ticket = null;
        List<Integer> cells = new ArrayList<>();
        for (int cx = box[0] >> 4; cx <= box[3] >> 4; cx++) {
            for (int cz = box[2] >> 4; cz <= box[5] >> 4; cz++) {
                final String ck = chunkKey(dim, cx, cz);
                if (FORCED.containsKey(ck)) continue;
                if (ticket == null || ticket.getChunkList()
                    .size() >= ticket.getMaxChunkListDepth()) {
                    if (ticket != null) tag(ticket, h, cells);
                    ticket = ForgeChunkManager.requestTicket(mod, world, ForgeChunkManager.Type.NORMAL);
                    if (ticket == null) {
                        BeyondTheStars.LOG.warn(
                            "[BTS] showcase: out of chunk tickets at {} (raise maximumTicketCount for beyondthestars"
                                + " in config/forgeChunkLoading.cfg)",
                            k);
                        return;
                    }
                    h.tickets.add(ticket);
                    cells = new ArrayList<>();
                }
                ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(cx, cz));
                FORCED.put(ck, h);
                h.owned.add(ck);
                cells.add(cx);
                cells.add(cz);
            }
        }
        if (ticket != null) tag(ticket, h, cells);
    }

    private static void tag(ForgeChunkManager.Ticket ticket, Held h, List<Integer> cells) {
        final NBTTagCompound d = ticket.getModData();
        d.setBoolean(TAG, true);
        d.setInteger("x", h.x);
        d.setInteger("y", h.y);
        d.setInteger("z", h.z);
        d.setIntArray("box", h.box);
        final int[] chunks = new int[cells.size()];
        for (int i = 0; i < chunks.length; i++) chunks[i] = cells.get(i);
        d.setIntArray("chunks", chunks);
    }

    /**
     * Lets go of a machine's own tickets. Every other machine whose box overlaps re-claims on its next second, so a
     * chunk it leaned on is forced again by someone that still needs it.
     */
    private static void release(Held h) {
        release(h, true);
    }

    private static void release(Held h, boolean notify) {
        for (ForgeChunkManager.Ticket t : h.tickets) ForgeChunkManager.releaseTicket(t);
        h.tickets.clear();
        for (String ck : h.owned) {
            if (FORCED.get(ck) == h) FORCED.remove(ck);
        }
        h.owned.clear();
        h.settled = false;
        if (!notify) return;
        for (Held o : HELD.values()) {
            if (o != h && o.dim == h.dim && overlaps(o.box, h.box)) o.settled = false;
        }
    }

    private static boolean overlaps(int[] a, int[] b) {
        return a != null && b != null && a[0] <= b[3] && b[0] <= a[3] && a[2] <= b[5] && b[2] <= a[5];
    }

    /**
     * Releases the tickets of machines that are gone: their controller's chunk is loaded and holds no showcase host
     * with the flag on. Called once a second from the server tick (cheap: one map walk).
     */
    public static void sweep() {
        for (Iterator<Map.Entry<String, Held>> it = HELD.entrySet()
            .iterator(); it.hasNext();) {
            final Held h = it.next()
                .getValue();
            final World world = net.minecraftforge.common.DimensionManager.getWorld(h.dim);
            if (world == null || !world.blockExists(h.x, h.y, h.z)) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(h.x, h.y, h.z);
            final Object mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
                : null;
            if (mte instanceof ShowcaseHost && ((ShowcaseHost) mte).isShowcase()) continue;
            release(h);
            it.remove();
        }
    }

    private static int sweepTicks;

    /** The sweep's server tick (registered in {@code CommonProxy.init}). */
    public static final class Ticker {

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onServerTick(cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent event) {
            if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END || ++sweepTicks < 20) return;
            sweepTicks = 0;
            sweep();
        }
    }

    /** Forgets every held ticket (server stop: Forge saves them with the world and gives them back next start). */
    public static void serverStopping() {
        HELD.clear();
        FORCED.clear();
    }

    /** Lines for /btsshowcase status: every machine holding tickets. */
    public static List<String> heldLines() {
        final List<String> out = new ArrayList<>();
        int tickets = 0;
        for (Held h : HELD.values()) {
            tickets += h.tickets.size();
            out.add(
                "dim " + h.dim
                    + " at "
                    + h.x
                    + " "
                    + h.y
                    + " "
                    + h.z
                    + ": "
                    + h.owned.size()
                    + " chunk(s) of its own in "
                    + h.tickets.size()
                    + " ticket(s)");
        }
        if (!HELD.isEmpty()) out.add(FORCED.size() + " chunk(s) forced in " + tickets + " ticket(s) in all");
        return out;
    }
}
