package com.nateyburr.beyondthestars.machines.conduit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.QuasarGeometry;
import com.nateyburr.beyondthestars.machines.quasar.QuasarKit;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstReading;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * P4: the Conduit's and the crowns' creative kit and console bench ({@code /btskit conduit}, {@code /btsconduit}).
 * Every position here comes from {@code manifest.json} (throats, sockets, crown intakes, the quasar's ports) and the
 * exported shapes, the same files the machines walk (D-22): nothing is typed by hand.
 * <p>
 * {@link #rig} raises the standard test rig of the design (quasar-p4.md §3): a quasar, a Mk I crown on its top port, a
 * Wormhole Mouth on its bottom port paired to a second mouth, a Mirror, a split Junction (out 3 at 10 %), a second
 * Mirror turning out 1 up into a remote crown's GRB Receptor Port, and standalone receptors on out 2 and out 3.
 */
public final class ConduitKit {

    private ConduitKit() {}

    /** The bench crowns' owner (a fixed team on the wireless network). */
    public static final UUID BENCH_OWNER = UUID.nameUUIDFromBytes("beyondthestars-conduit-bench".getBytes());

    // ===================================================================================== kit

    public static void giveKit(EntityPlayerMP player) {
        give(player, copy(BTSMachines.wormholeMouth, 2));
        give(player, copy(BTSMachines.conduitMirror, 1));
        give(player, copy(BTSMachines.conduitJunction, 1));
        give(player, copy(BTSMachines.burstThermaliserCrown, 2));
        give(player, copy(BTSMachines.grbReceptorPort, 4));
        give(player, copy(BTSMachines.burstControlHatch, 2));
        for (int i = 0; i < 4; i++) give(player, SLQuasarItems.throat(1));
        give(player, SLQuasarItems.throat(2));
        give(player, ItemList.Tool_DataStick.get(1L));
        final ItemStack hatch = ItemList.Hatch_Input_MAX.get(1L);
        if (hatch != null) {
            hatch.stackSize = 8;
            give(player, hatch);
        }
        player.inventoryContainer.detectAndSendChanges();
        tell(player, EnumChatFormatting.AQUA + "== Einstein-Rosen Conduit + crowns test kit ==");
        tell(
            player,
            "Given: 2 Wormhole Mouths, a Mirror, a Junction, 2 crowns, 4 GRB Receptor Ports, 2 Burst Control"
                + " Hatches, 4 Throats Mk I + 1 Mk II, a Data Stick, 8 input hatches (throat fuel).");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        for (String piece : new String[] { MTEWormholeMouth.PIECE, MTEConduitMirror.PIECE, MTEConduitJunction.PIECE,
            MTEBurstThermaliserCrown.PIECES[0], MTEBurstThermaliserCrown.PIECES[1],
            MTEBurstThermaliserCrown.PIECES[2] }) {
            tell(player, " - " + piece + ": " + bill(piece));
        }
        tell(
            player,
            EnumChatFormatting.GREEN + "Dock: a crown's base centre (or a mouth's front-face centre) goes on the"
                + " quasar's port cell, the cell just past each cap on the jet axis; /btsconduit dock <quasar x y z>"
                + " prints both cells. Pair two mouths with the Data Stick. A Throat in each controller slot, fuel"
                + " (Molten SpaceTime, Ergomatter after T7) in the input hatches. /btsconduit rig x y z raises a"
                + " whole test rig.");
    }

    /** "1,404 blocks: 413x Reinforced Temporal Structure Casing, ..." from the manifest. */
    public static String bill(String piece) {
        final JsonObject entry = BTSShapes.manifestPiece(piece);
        if (entry == null || !entry.has("bom")) return "(not in manifest)";
        final StringBuilder sb = new StringBuilder();
        for (JsonElement el : entry.getAsJsonArray("bom")) {
            final JsonObject row = el.getAsJsonObject();
            final String ch = row.get("ch")
                .getAsString();
            if ("~".equals(ch) || Character.isLowerCase(ch.charAt(0))) continue;
            if (sb.length() > 0) sb.append(", ");
            String block = row.get("block")
                .getAsString()
                .replace("NEW ", "")
                .replace("Casings.", "");
            final int paren = block.indexOf(" (");
            if (paren > 0) block = block.substring(0, paren);
            sb.append(
                String.format(
                    Locale.ROOT,
                    "%,dx ",
                    row.get("count")
                        .getAsInt()))
                .append(block);
        }
        return sb.toString();
    }

    /** Every socket of one char of a piece, as offsets from the controller ({@code sockets.<ch>[i].rel}). */
    public static int[][] socketRels(String piece, char ch) {
        final JsonObject entry = BTSShapes.manifestPiece(piece);
        if (entry == null || !entry.has("sockets")) return new int[0][];
        final JsonObject sockets = entry.getAsJsonObject("sockets");
        final String key = String.valueOf(ch);
        if (!sockets.has(key)) return new int[0][];
        final JsonArray list = sockets.getAsJsonArray(key);
        final int[][] out = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            final JsonArray rel = list.get(i)
                .getAsJsonObject()
                .getAsJsonArray("rel");
            out[i] = new int[] { rel.get(0)
                .getAsInt(),
                rel.get(1)
                    .getAsInt(),
                rel.get(2)
                    .getAsInt() };
        }
        return out;
    }

    /** The two dock cells of a quasar and how a crown stands on each ({@code /btsconduit dock}). */
    public static List<String> dockCells(MTEQuasarEngine q) {
        final List<String> out = new ArrayList<>();
        final IGregTechTileEntity base = q.getBaseMetaTileEntity();
        for (JetPort port : JetPort.VALUES) {
            final int[] cell = q.portCell(base, port);
            final int[] crown = QuasarGeometry.world(
                q.getExtendedFacing(),
                base.getXCoord(),
                base.getYCoord(),
                base.getZCoord(),
                QuasarGeometry.crownControllerRel(port));
            final ExtendedFacing f = MTEBurstThermaliserCrown.dockFacing(q.getExtendedFacing(), port);
            out.add(
                port.label + " port cell "
                    + pos(cell)
                    + ": a crown's controller at "
                    + pos(crown)
                    + " facing "
                    + (f == null ? "?"
                        : f.getDirection() + (f.getRotation()
                            .isUpsideDown() ? ", upside down" : ""))
                    + "; or a Wormhole Mouth with its front-face centre on the cell, facing the quasar");
        }
        return out;
    }

    // ================================================================================== bench

    /**
     * {@code /btsconduit crowns}: a Mk {@code mk} crown docked on each of the quasar's two jet ports (bench owner,
     * formed), and with {@code stack} a second crown of the same Mk stacked on each (it takes the first one's
     * overflow). Returns what it did.
     */
    public static List<String> crowns(World world, MTEQuasarEngine q, int mk, boolean stack) {
        final List<String> out = new ArrayList<>();
        final IGregTechTileEntity base = q.getBaseMetaTileEntity();
        final ExtendedFacing qf = q.getExtendedFacing();
        final int m = Math.max(1, Math.min(MTEBurstThermaliserCrown.MK_MAX, mk));
        for (JetPort port : JetPort.VALUES) {
            final ExtendedFacing f = MTEBurstThermaliserCrown.dockFacing(qf, port);
            final int[] c = QuasarGeometry.world(
                qf,
                base.getXCoord(),
                base.getYCoord(),
                base.getZCoord(),
                QuasarGeometry.crownControllerRel(port));
            final MTEBurstThermaliserCrown crown = placeCrown(world, c, f, m, out, port.label + " crown");
            if (crown == null || !stack) continue;
            final int[] s = QuasarGeometry.world(f, c[0], c[1], c[2], MTEBurstThermaliserCrown.stackRel(m));
            placeCrown(world, s, f, m, out, port.label + " crown, stacked");
        }
        return out;
    }

    /** The last rig raised (positions), for {@code /btsconduit rig status}. */
    public static final class Rig {

        public World world;
        public int[] quasar, crownTop, mouthA, mouthB, mirror1, junction, mirror2, crownRemote, r2, r3;
    }

    private static Rig last;

    public static Rig lastRig() {
        return last;
    }

    /** Raises the standard rig around a quasar at (x, y, z) facing north. Returns what it did. */
    public static List<String> rig(World world, int x, int y, int z) {
        final List<String> out = new ArrayList<>();
        final Rig rig = new Rig();
        rig.world = world;
        // 1. the quasar: coils T3, helium, seeded at 64 SM with spin 0.5 so it makes bursts at once
        final int[] qbox = QuasarKit.bounds(ForgeDirection.NORTH, x, y, z);
        SLConsoleTest.loadChunks(world, qbox);
        KerrTestBench.keep(world, qbox);
        final QuasarKit.Result qr = QuasarKit.build(world, x, y, z, ForgeDirection.NORTH, 3, false, false);
        if (qr.failure != null) {
            out.add("Quasar: BUILD FAILED - " + qr.failure);
            return out;
        }
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, x, y, z);
        if (q == null || !q.isFormed()) {
            out.add("Quasar: NOT FORMED");
            return out;
        }
        q.debugSeed(64, 0.5);
        q.debugFillBuffer();
        rig.quasar = new int[] { x, y, z };
        out.add("Quasar FORMED at " + pos(rig.quasar) + ", seeded 64 SM, spin 0.5 (debug)");
        final ExtendedFacing qf = q.getExtendedFacing();
        final IGregTechTileEntity qb = q.getBaseMetaTileEntity();

        // 2. a Mk I crown on the top port
        {
            final ExtendedFacing f = MTEBurstThermaliserCrown.dockFacing(qf, JetPort.TOP);
            final int[] c = QuasarGeometry.world(qf, x, y, z, QuasarGeometry.crownControllerRel(JetPort.TOP));
            final MTEBurstThermaliserCrown crown = placeCrown(world, c, f, 1, out, "Crown (top port)");
            if (crown != null) rig.crownTop = c;
        }
        // 3. mouth A on the bottom port: its axis (+c) along the jet, its front-face centre on the port cell
        final ConduitThroats.Throat[] mt = ConduitThroats.of(MTEWormholeMouth.PIECE);
        final int[] bottom = q.portCell(qb, JetPort.BOTTOM);
        final int[] ax = QuasarGeometry.axisAbc(JetPort.BOTTOM);
        final double[] jetDown = ConduitThroats.worldDir(qf, new double[] { ax[0], ax[1], ax[2] });
        final ExtendedFacing fa = MTEConduitElement.facingFor(mt, 0, jetDown, -1, null);
        final int[] dockRel = mt[0].centreRel.clone();
        dockRel[2] = 0;
        rig.mouthA = minus(bottom, offset(fa, dockRel));
        final MTEWormholeMouth a = placeElement(
            world,
            BTSMachines.wormholeMouth,
            MTEWormholeMouth.class,
            rig.mouthA,
            fa,
            1,
            out,
            "Mouth A (bottom port)");

        // 4. mouth B east of the quasar, emitting south (+z)
        final double[] south = { 0, 0, 1 };
        final ExtendedFacing fb = MTEConduitElement.facingFor(mt, 0, south, -1, null);
        final int[] bThroat = { x + 100, y - 40, z + 72 };
        rig.mouthB = minus(bThroat, offset(fb, mt[0].centreRel));
        final MTEWormholeMouth b = placeElement(
            world,
            BTSMachines.wormholeMouth,
            MTEWormholeMouth.class,
            rig.mouthB,
            fb,
            1,
            out,
            "Mouth B");
        if (a != null && b != null) {
            MTEWormholeMouth.pair(a, b);
            out.add("Mouths paired: " + pos(rig.mouthA) + " <-> " + pos(rig.mouthB));
        }

        // 5. mirror 1: in from the north (normal -z), out to the east (+x), 40 blocks south of B's ring
        final ConduitThroats.Throat[] mr = ConduitThroats.of(MTEConduitMirror.PIECE);
        final double[] east = { 1, 0, 0 };
        final ExtendedFacing f1 = MTEConduitElement.facingFor(mr, 0, new double[] { 0, 0, -1 }, 1, east);
        final int[] m1In = { bThroat[0], bThroat[1], bThroat[2] + 40 };
        rig.mirror1 = minus(m1In, offset(f1, mr[0].centreRel));
        placeElement(world, BTSMachines.conduitMirror, MTEConduitMirror.class, rig.mirror1, f1, 1, out, "Mirror 1");
        final int[] m1Out = plus(rig.mirror1, offset(f1, mr[1].centreRel));

        // 6. the junction, split: big throat facing west, out 1 east; 30 blocks east of mirror 1's out
        final ConduitThroats.Throat[] jt = ConduitThroats.of(MTEConduitJunction.PIECE);
        final ExtendedFacing fj = MTEConduitElement.facingFor(jt, 0, new double[] { -1, 0, 0 }, 1, east);
        final int[] jBig = { m1Out[0] + 30, m1Out[1], m1Out[2] };
        rig.junction = minus(jBig, offset(fj, jt[0].centreRel));
        final MTEConduitJunction j = placeElement(
            world,
            BTSMachines.conduitJunction,
            MTEConduitJunction.class,
            rig.junction,
            fj,
            1,
            out,
            "Junction");
        if (j != null) {
            j.setMode(MTEConduitJunction.Mode.SPLIT);
            j.setRatio(0.5);
            j.setGainIndex(0);
            j.setOut3Share(0.1);
            final int[][] cs = socketRels(MTEConduitJunction.PIECE, 'c');
            if (cs.length > 0) {
                final int[] p = plus(rig.junction, offset(fj, cs[0]));
                if (world.getTileEntity(p[0], p[1], p[2]) == null) {
                    place(world, p, BTSMachines.burstControlHatch, openSide(world, p));
                    final MTEHatchBurstControl h = mteAt(world, p, MTEHatchBurstControl.class);
                    if (h != null) h.setMode(MTEHatchBurstControl.Mode.SPLIT);
                }
                j.checkStructure(true, j.getBaseMetaTileEntity());
            }
        }
        // 7. mirror 2 on out 1: in from the west, out up; 20 blocks east of out 1
        final ConduitThroats.WorldThroat out1 = ConduitThroats
            .world(jt[1], fj, rig.junction[0], rig.junction[1], rig.junction[2]);
        final ExtendedFacing f2 = MTEConduitElement
            .facingFor(mr, 0, new double[] { -1, 0, 0 }, 1, new double[] { 0, 1, 0 });
        final int[] m2In = { out1.cell[0] + 20, out1.cell[1], out1.cell[2] };
        rig.mirror2 = minus(m2In, offset(f2, mr[0].centreRel));
        placeElement(world, BTSMachines.conduitMirror, MTEConduitMirror.class, rig.mirror2, f2, 1, out, "Mirror 2");
        final int[] m2Out = plus(rig.mirror2, offset(f2, mr[1].centreRel));

        // 8. the remote crown: its receptor socket 20 blocks above mirror 2's out
        final int[] rCell = { m2Out[0], m2Out[1] + 20, m2Out[2] };
        final ExtendedFacing fc = ExtendedFacing.of(ForgeDirection.NORTH);
        final int[] rRel = socketRels(MTEBurstThermaliserCrown.PIECES[0], 'r')[0];
        rig.crownRemote = minus(rCell, offset(fc, rRel));
        final MTEBurstThermaliserCrown remote = placeCrown(world, rig.crownRemote, fc, 1, out, "Crown (remote)");
        if (remote != null) {
            if (world.getTileEntity(rCell[0], rCell[1], rCell[2]) == null) {
                place(world, rCell, BTSMachines.grbReceptorPort, ForgeDirection.DOWN);
            }
            remote.checkStructure(true, remote.getBaseMetaTileEntity());
            out.add(
                "Remote crown receptor at " + pos(rCell) + ": " + (remote.receptorCount() > 0 ? "fitted" : "MISSING"));
        }
        // 9. standalone receptors on out 2 and out 3
        if (j != null) {
            rig.r2 = receptorOnRay(
                world,
                ConduitThroats.world(jt[2], fj, rig.junction[0], rig.junction[1], rig.junction[2]),
                20);
            rig.r3 = receptorOnRay(
                world,
                ConduitThroats.world(jt[3], fj, rig.junction[0], rig.junction[1], rig.junction[2]),
                15);
            out.add("Receptors: out 2 at " + pos(rig.r2) + ", out 3 at " + pos(rig.r3));
        }
        last = rig;
        out.add("Rig raised. Wait a few seconds (elements open after 10 s of upkeep), then /btsconduit rig status.");
        return out;
    }

    /** A standalone receptor on a ray, {@code t} blocks out of a throat. */
    private static int[] receptorOnRay(World world, ConduitThroats.WorldThroat th, double t) {
        final int[] p = { (int) Math.floor(th.centre[0] + t * th.normal[0]),
            (int) Math.floor(th.centre[1] + t * th.normal[1]), (int) Math.floor(th.centre[2] + t * th.normal[2]) };
        SLConsoleTest.loadChunks(world, new int[] { p[0], p[1], p[2], p[0], p[1], p[2] });
        KerrTestBench.keep(world, new int[] { p[0], p[1], p[2], p[0], p[1], p[2] });
        place(world, p, BTSMachines.grbReceptorPort, ForgeDirection.UP);
        return p;
    }

    /**
     * Places, builds (creative) and fits an element: a throat Mk {@code throat}, every fuel socket a MAX input hatch
     * full of Molten SpaceTime.
     */
    static <T extends MTEConduitElement<T>> T placeElement(World world, ItemStack stack, Class<T> type, int[] p,
        ExtendedFacing f, int throat, List<String> out, String name) {
        if (stack == null || f == null) {
            out.add(name + ": NOT PLACED (" + (stack == null ? "not registered" : "no facing fits") + ")");
            return null;
        }
        final T e = placeController(world, stack, type, p, f);
        if (e == null) {
            out.add(name + ": NOT PLACED at " + pos(p));
            return null;
        }
        final ItemStack trigger = stack.copy();
        trigger.stackSize = 1;
        e.construct(trigger, false);
        final Fluid st = MTEConduitElement.spaceTimeFluid();
        int fuel = 0;
        for (int[] rel : socketRels(e.piece(), 'u')) {
            final int[] h = plus(p, offset(f, rel));
            if (world.getTileEntity(h[0], h[1], h[2]) == null) {
                place(world, h, ItemList.Hatch_Input_MAX.get(1L), openSide(world, h));
            }
            final MTEHatchInput hatch = mteAt(world, h, MTEHatchInput.class);
            if (hatch != null && st != null) {
                hatch.fill(new FluidStack(st, Integer.MAX_VALUE), true);
                fuel++;
            }
        }
        e.mInventory[e.getControllerSlotIndex()] = SLQuasarItems.throat(throat);
        e.checkStructure(true, e.getBaseMetaTileEntity());
        e.getBaseMetaTileEntity()
            .enableWorking();
        out.add(
            name + ": "
                + (e.isFormed() ? "FORMED" : "NOT FORMED")
                + " at "
                + pos(p)
                + ", facing "
                + f.getName2()
                + ", throat Mk "
                + throat
                + ", "
                + fuel
                + " fuel hatch(es) of Molten SpaceTime");
        if (!e.isFormed()) {
            for (String line : SLConsoleTest.firstErrors(e, e.getBaseMetaTileEntity(), 3)) out.add("  " + line);
        }
        return e;
    }

    static MTEBurstThermaliserCrown placeCrown(World world, int[] p, ExtendedFacing f, int mk, List<String> out,
        String name) {
        if (BTSMachines.burstThermaliserCrown == null || f == null) {
            out.add(name + ": NOT PLACED");
            return null;
        }
        final MTEBurstThermaliserCrown c = placeController(
            world,
            BTSMachines.burstThermaliserCrown,
            MTEBurstThermaliserCrown.class,
            p,
            f);
        if (c == null) {
            out.add(name + ": NOT PLACED at " + pos(p));
            return null;
        }
        final ItemStack trigger = BTSMachines.burstThermaliserCrown.copy();
        trigger.stackSize = mk;
        c.construct(trigger, false);
        c.getBaseMetaTileEntity()
            .setOwnerUuid(BENCH_OWNER);
        c.checkStructure(true, c.getBaseMetaTileEntity());
        c.getBaseMetaTileEntity()
            .enableWorking();
        out.add(
            name + ": "
                + (c.mMachine ? "FORMED Mk " + c.getMk() : "NOT FORMED")
                + " at "
                + pos(p)
                + ", facing "
                + f.getName2());
        if (!c.mMachine) {
            for (String line : SLConsoleTest.firstErrors(c, c.getBaseMetaTileEntity(), 3)) out.add("  " + line);
        }
        return c;
    }

    private static <T extends MTEEnhancedMultiBlockBase<?>> T placeController(World world, ItemStack stack,
        Class<T> type, int[] p, ExtendedFacing f) {
        final int[] box = { p[0] - 64, p[1], p[2] - 64, p[0] + 64, p[1], p[2] + 64 };
        SLConsoleTest.loadChunks(world, box);
        KerrTestBench.keep(world, box);
        if (!MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), f.getDirection())) {
            return null;
        }
        final T m = mteAt(world, p, type);
        if (m == null) return null;
        m.setExtendedFacing(f);
        return m;
    }

    /**
     * The hologram's survival path on one element or crown ({@code /btsconduit survival}): a bare controller facing
     * {@code dir} at (x, y, z), StructureLib's survival auto-place from an endless source until it reports done (or,
     * with {@code creative}, the projector's creative {@code construct}), a hatch in the first fuel socket (survival
     * places hatches only with the projector's hatch channel), a creative re-projection over it (nothing may break)
     * and the damage-and-repair check.
     */
    public static List<String> survivalTest(World world, int x, int y, int z, String kind) {
        return survivalTest(world, x, y, z, kind, ForgeDirection.NORTH, false);
    }

    public static List<String> survivalTest(World world, int x, int y, int z, String kind, ForgeDirection dir,
        boolean creative) {
        final List<String> out = new ArrayList<>();
        final ExtendedFacing f = ExtendedFacing.of(dir);
        out.add(
            kind + " facing "
                + dir.name()
                    .toLowerCase(Locale.ROOT)
                + (creative ? " (creative path)" : " (survival path)"));
        final String k = kind.toLowerCase(Locale.ROOT);
        final MTEEnhancedMultiBlockBase<?> m;
        final ItemStack stack;
        int tier = 1;
        if (k.startsWith("crown")) {
            tier = k.length() > 5 ? Math.max(1, Math.min(MTEBurstThermaliserCrown.MK_MAX, k.charAt(5) - '0')) : 1;
            stack = BTSMachines.burstThermaliserCrown;
            m = stack == null ? null
                : placeController(world, stack, MTEBurstThermaliserCrown.class, new int[] { x, y, z }, f);
        } else {
            stack = k.equals("mouth") ? BTSMachines.wormholeMouth
                : k.equals("mirror") ? BTSMachines.conduitMirror : BTSMachines.conduitJunction;
            m = stack == null ? null : placeController(world, stack, MTEConduitElement.class, new int[] { x, y, z }, f);
        }
        if (m == null) {
            out.add(kind + ": controller NOT PLACED");
            return out;
        }
        final ItemStack trigger = stack.copy();
        trigger.stackSize = tier;
        if (creative) {
            ((com.gtnewhorizon.structurelib.alignment.constructable.IConstructable) m).construct(trigger, false);
            out.add(kind + " creative construct: done");
        } else {
            final SLConsoleTest.SurvivalRun run = SLConsoleTest.survivalBuild(
                (com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable) m,
                world,
                trigger,
                25,
                4000);
            out.add(
                kind + " survival auto-place: "
                    + run.placed
                    + " blocks in "
                    + run.rounds
                    + " rounds, "
                    + (run.finished ? "finished (-1)" : run.stalled ? "STALLED" : "round cap hit"));
        }
        if (m instanceof MTEConduitElement) {
            final MTEConduitElement<?> e = (MTEConduitElement<?>) m;
            final int[][] u = socketRels(e.piece(), 'u');
            if (u.length > 0) {
                final int[] p = plus(new int[] { x, y, z }, offset(f, u[0]));
                place(world, p, ItemList.Hatch_Input_MAX.get(1L), openSide(world, p));
            }
        }
        m.checkStructure(true, m.getBaseMetaTileEntity());
        out.add(kind + ": " + (m.mMachine ? "FORMED" : "NOT FORMED"));
        if (!m.mMachine) {
            for (String line : SLConsoleTest.firstErrors(m, m.getBaseMetaTileEntity(), 3)) out.add("  " + line);
            return out;
        }
        ((com.gtnewhorizon.structurelib.alignment.constructable.IConstructable) m).construct(trigger, false);
        m.checkStructure(true, m.getBaseMetaTileEntity());
        out.add("Creative re-projection over it: " + (m.mMachine ? "still FORMED" : "BROKE IT"));
        if (m.mMachine && m instanceof MTEConduitElement) {
            final MTEConduitElement<?> e = (MTEConduitElement<?>) m;
            out.addAll(
                SLConsoleTest.damageAndRepair(
                    e,
                    e.getBaseMetaTileEntity(),
                    trigger,
                    SLConsoleTest.bounds(f, x, y, z, e.piece())));
        } else if (m.mMachine && m instanceof MTEBurstThermaliserCrown) {
            final MTEBurstThermaliserCrown c = (MTEBurstThermaliserCrown) m;
            out.add("Crown Mk " + c.getMk());
            out.addAll(
                SLConsoleTest.damageAndRepair(
                    c,
                    c.getBaseMetaTileEntity(),
                    trigger,
                    SLConsoleTest.bounds(f, x, y, z, MTEBurstThermaliserCrown.PIECES[tier - 1])));
        }
        return out;
    }

    /** The whole rig's state ({@code /btsconduit rig status}). */
    public static List<String> rigStatus() {
        final List<String> out = new ArrayList<>();
        final Rig rig = last;
        if (rig == null) {
            out.add("No rig raised since the server started: /btsconduit rig x y z");
            return out;
        }
        final World w = rig.world;
        final MTEQuasarEngine q = rig.quasar == null ? null
            : QuasarKit.quasarAt(w, rig.quasar[0], rig.quasar[1], rig.quasar[2]);
        if (q != null) out.addAll(q.statusLines());
        for (int[] p : new int[][] { rig.crownTop, rig.mouthA, rig.mouthB, rig.mirror1, rig.junction, rig.mirror2,
            rig.crownRemote, rig.r2, rig.r3 }) {
            if (p == null) continue;
            out.add(EnumChatFormatting.AQUA + "-- " + pos(p));
            out.addAll(status(w, p[0], p[1], p[2]));
        }
        out.add(
            "Wireless network of the bench owner: "
                + String.format(Locale.ROOT, "%,d", WirelessNetworkManager.getUserEU(BENCH_OWNER))
                + " EU");
        return out;
    }

    /** The scanner lines of whatever conduit machine stands at (x, y, z). */
    public static List<String> status(World world, int x, int y, int z) {
        final List<String> out = new ArrayList<>();
        final IMetaTileEntity m = mteAt(world, new int[] { x, y, z }, IMetaTileEntity.class);
        if (m instanceof MTEConduitElement) out.addAll(((MTEConduitElement<?>) m).statusLines());
        else if (m instanceof MTEBurstThermaliserCrown) out.addAll(((MTEBurstThermaliserCrown) m).statusLines());
        else if (m instanceof MTEHatchGRBReceptor) {
            final MTEHatchGRBReceptor r = (MTEHatchGRBReceptor) m;
            final BurstReading reading = BurstHost.read(java.util.Collections.singletonList(r), r.getHost());
            out.add(
                String.format(
                    Locale.ROOT,
                    "GRB Receptor Port: %s%s%s",
                    com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(reading.total),
                    reading.linked() ? ", LINKED to " + reading.linkSource()
                        .label() : ", not linked",
                    reading.paused ? ", PAUSED" : ""));
            out.add(reading.line());
        } else if (m instanceof MTEQuasarEngine) out.addAll(((MTEQuasarEngine) m).statusLines());
        else out.add("No conduit machine at " + pos(new int[] { x, y, z }));
        return out;
    }

    /** Wireless EU of the bench owner (a BigInteger, for the bench's before / after check). */
    public static BigInteger benchEU() {
        return WirelessNetworkManager.getUserEU(BENCH_OWNER);
    }

    // ================================================================================ helpers

    static <T> T mteAt(World world, int[] p, Class<T> type) {
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(mte) ? type.cast(mte) : null;
    }

    private static boolean place(World world, int[] p, ItemStack stack, ForgeDirection facing) {
        if (stack == null) return false;
        return MTEReactorCoreMk1.placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), facing);
    }

    /** The first air-facing side of a cell (a hatch's front). */
    private static ForgeDirection openSide(World world, int[] p) {
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (world.isAirBlock(p[0] + d.offsetX, p[1] + d.offsetY, p[2] + d.offsetZ)) return d;
        }
        return ForgeDirection.UP;
    }

    static int[] offset(ExtendedFacing f, int[] rel) {
        final int[] o = new int[3];
        f.getWorldOffset(rel, o);
        return o;
    }

    static int[] plus(int[] a, int[] b) {
        return new int[] { a[0] + b[0], a[1] + b[1], a[2] + b[2] };
    }

    static int[] minus(int[] a, int[] b) {
        return new int[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    static String pos(int[] p) {
        return p == null ? "-" : "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }

    private static ItemStack copy(ItemStack stack, int n) {
        if (stack == null) return null;
        final ItemStack s = stack.copy();
        s.stackSize = n;
        return s;
    }

    private static void give(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) player.entityDropItem(stack, 0.5F);
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }
}
