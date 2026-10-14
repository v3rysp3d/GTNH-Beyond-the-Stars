package com.nateyburr.beyondthestars.machines.reality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.NaniteTier;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchNanite;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import tectech.thing.CustomItemList;
import tectech.thing.metaTileEntity.hatch.bec.MTEHatchBEC;
import tectech.thing.metaTileEntity.multi.base.MTEBECMultiblockBase;
import tectech.thing.metaTileEntity.multi.bec.MTEBECAssembler;
import tectech.thing.metaTileEntity.multi.bec.MTEBECStorage;
import tectech.thing.metaTileEntity.pipe.MTEPipeBEC;

/**
 * Dev bench (op tooling, no gameplay change): {@code /btshr becnet}, the smallest TecTech Bose-Einstein Condensate
 * network Reality Diffraction and Reality Refab need, raised by script so the two can be seen RUNNING (the showcase
 * re-shoot, build notes item 32(f)). Everything is GT's / TecTech's own machines doing their own work afterwards:
 * <ul>
 * <li>{@code field <x> <y> <z> [strength <n>]}: a BEC Containment Field ({@link MTEBECStorage}, controller facing
 * north)
 * built by its own creative construct, one GT Debug Energy Hatch (MAX, 64 A) in a casing cell that may hold one, field
 * strength set (default 1,000,000: its capacity in litres and its EU/t), switched on. Its BEC Hatch is fitted by
 * {@code link}.</li>
 * <li>{@code assembler <x> <y> <z> [nanites <n>] [tier <name>]}: a Condensate Assembler ({@link MTEBECAssembler}) built
 * the same way, a Debug Energy Hatch and a Nanite Containment Bus holding {@code n} nanites (default 64 Black Dwarf,
 * the highest tier our Refab recipes ask for), switched on. Reality Refab links the nearest one within 64 blocks by
 * itself.</li>
 * <li>{@code link <fx> <fy> <fz> <mx> <my> <mz>}: a BEC Hatch on the field's side nearest the Reality machine at m, and
 * a BEC conduit laid through air from the machine's own BEC Hatch to it (or to a conduit already on the way).</li>
 * <li>{@code power <x> <y> <z>}: the GT multiblock's energy hatches become Debug Energy Hatches (MAX, 64 A).</li>
 * <li>{@code status <x> <y> <z>}: the BEC state of the field, assembler, Reality machine or conduit there.</li>
 * </ul>
 * Captions that film it say "set up by script".
 */
public final class BECNetBench {

    private BECNetBench() {}

    public static final String USAGE = "/btshr becnet field x y z [strength n] | assembler x y z [nanites n] [tier t]"
        + " | link fx fy fz mx my mz | power x y z | status x y z";

    private static final long DEFAULT_STRENGTH = 1_000_000L;
    private static final int DEFAULT_NANITES = 64;
    private static final int MARGIN = 10;

    public static List<String> run(World world, String[] args) {
        final List<String> out = new ArrayList<>();
        if (args.length < 4) {
            out.add(USAGE);
            return out;
        }
        final String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            final int[] p = { Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]) };
            switch (sub) {
                case "field":
                    field(world, p, longOpt(args, "strength", DEFAULT_STRENGTH), out);
                    break;
                case "assembler":
                    assembler(
                        world,
                        p,
                        (int) longOpt(args, "nanites", DEFAULT_NANITES),
                        tierOpt(args, NaniteTier.BlackDwarf),
                        out);
                    break;
                case "link": {
                    if (args.length < 7) {
                        out.add(USAGE);
                        break;
                    }
                    final int[] m = { Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]) };
                    link(world, p, m, out);
                    break;
                }
                case "status":
                    status(world, p, out);
                    break;
                case "power":
                    power(world, p, out);
                    break;
                default:
                    out.add(USAGE);
                    break;
            }
        } catch (NumberFormatException bad) {
            out.add("becnet: bad number (" + bad.getMessage() + "). " + USAGE);
        }
        return out;
    }

    private static long longOpt(String[] args, String key, long dflt) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equalsIgnoreCase(key)) return Long.parseLong(args[i + 1].replace(",", ""));
        }
        return dflt;
    }

    private static NaniteTier tierOpt(String[] args, NaniteTier dflt) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (!args[i].equalsIgnoreCase("tier")) continue;
            for (NaniteTier t : NaniteTier.values()) if (t.name()
                .equalsIgnoreCase(args[i + 1])) return t;
        }
        return dflt;
    }

    // ================================================================================= build

    private static void field(World world, int[] p, long strength, List<String> out) {
        final MTEBECStorage field = raise(
            world,
            p,
            CustomItemList.Machine_Multi_BECStorage.get(1L),
            MTEBECStorage.class,
            out);
        if (field == null) return;
        out.add("energy: " + fitEnergy(world, field, 'B'));
        field.setFieldStrength(Math.max(1, strength));
        finish(field, out);
        out.add("field strength " + field.getFieldStrength() + " (capacity in L, and its EU/t)");
    }

    private static void assembler(World world, int[] p, int nanites, NaniteTier tier, List<String> out) {
        final MTEBECAssembler a = raise(
            world,
            p,
            CustomItemList.Machine_Multi_BECAssembler.get(1L),
            MTEBECAssembler.class,
            out);
        if (a == null) return;
        out.add("energy: " + fitEnergy(world, a, '1'));
        final int[] cell = hatchCell(world, a, '1', null);
        if (cell == null) {
            out.add("nanites: no free '1' cell for a Nanite Containment Bus");
        } else if (MTEReactorCoreMk1.placeGtMachine(
            world,
            cell[0],
            cell[1],
            cell[2],
            (short) ItemList.Hatch_Nanite.get(1L)
                .getItemDamage(),
            ForgeDirection.getOrientation(cell[3]))) {
                final MTEHatchNanite bus = mteAt(world, cell, MTEHatchNanite.class);
                if (bus != null) {
                    bus.setItemStack(tier.getStack());
                    bus.setItemCount(Math.max(1, nanites));
                }
                out.add(
                    "nanites: a Nanite Containment Bus at " + pos(
                        cell) + " holding " + nanites + " " + tier.describe() + " nanites");
            }
        finish(a, out);
        out.add(
            "assembler: formed " + a.mMachine
                + ", running "
                + (a.mMaxProgresstime > 0)
                + ", nanites "
                + a.getAvailableNanites()
                + " "
                + (a.getCurrentNaniteTier() == null ? "(none yet: read next tick)"
                    : a.getCurrentNaniteTier()
                        .describe()));
    }

    /** Places the TecTech BEC controller facing north, builds its structure creatively, keeps the chunks. */
    private static <T extends MTEBECMultiblockBase<?>> T raise(World world, int[] p, ItemStack controller,
        Class<T> type, List<String> out) {
        final int[] box = { p[0] - 64, p[1] - 32, p[2] - 64, p[0] + 64, p[1] + 32, p[2] + 64 };
        SLConsoleTest.loadChunks(world, box);
        out.add("chunks kept: " + KerrTestBench.keep(world, box));
        if (!MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) controller.getItemDamage(), ForgeDirection.NORTH)) {
            out.add("could not place " + controller.getDisplayName() + " at " + pos(p));
            return null;
        }
        final T m = mteAt(world, p, type);
        if (m == null) {
            out.add("no " + type.getSimpleName() + " tile at " + pos(p));
            return null;
        }
        m.construct(controller, false);
        out.add(controller.getDisplayName() + " at " + pos(p) + ": structure built by its own creative construct");
        return m;
    }

    private static void finish(MTEMultiBlockBase m, List<String> out) {
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        m.checkStructure(true, base);
        base.enableWorking();
        out.add(
            "formed " + m.mMachine + (m.mMachine ? "" : " " + SLConsoleTest.firstErrors(m, base, 3)) + "; switched on");
    }

    /** A GT Debug Energy Hatch (MAX, 64 A) in a free cell of {@code ch}, facing out. */
    private static String fitEnergy(World world, MTEBECMultiblockBase<?> m, char ch) {
        final int[] cell = hatchCell(world, m, ch, null);
        if (cell == null) return "no free '" + ch + "' cell";
        if (!MTEReactorCoreMk1.placeGtMachine(
            world,
            cell[0],
            cell[1],
            cell[2],
            (short) ItemList.DebugEnergyHatch.get(1L)
                .getItemDamage(),
            ForgeDirection.getOrientation(cell[3]))) return "could not place a Debug Energy Hatch at " + pos(cell);
        final MTEHatchEnergyDebug d = mteAt(world, cell, MTEHatchEnergyDebug.class);
        if (d != null) {
            d.setVoltageTier(13);
            d.setAmperage(64);
        }
        return "a GT Debug Energy Hatch (MAX, 64 A) at " + pos(cell);
    }

    // ================================================================================== link

    private static void link(World world, int[] f, int[] mp, List<String> out) {
        final MTEBECStorage field = mteAt(world, f, MTEBECStorage.class);
        if (field == null) {
            out.add("link: no BEC Containment Field controller at " + pos(f));
            return;
        }
        final MTERealityBase<?> machine = mteAt(world, mp, MTERealityBase.class);
        if (machine == null) {
            out.add("link: no Reality Diffraction / Refab controller at " + pos(mp));
            return;
        }
        // the machine's own BEC Hatch (its b socket) and the cell in front of it
        final int[][] rels = BTSShapes.socketRels(machine.piece(), 'b');
        if (rels.length == 0) {
            out.add("link: the machine has no b socket in its manifest");
            return;
        }
        final int[] hp = SLConsoleTest.worldPos(machine.getExtendedFacing(), mp[0], mp[1], mp[2], rels[0]);
        final MTEHatchBEC mh = mteAt(world, hp, MTEHatchBEC.class);
        if (mh == null) {
            out.add("link: no BEC Hatch at the machine's socket " + pos(hp) + " (build it with fill first)");
            return;
        }
        final ForgeDirection mf = mh.getBaseMetaTileEntity()
            .getFrontFacing();
        final int[] start = { hp[0] + mf.offsetX, hp[1] + mf.offsetY, hp[2] + mf.offsetZ };
        // the field's BEC Hatch: the free '1' cell nearest the machine (or the one already fitted nearest it)
        int[] fh = hatchCell(world, field, '1', start);
        if (fh == null) {
            out.add("link: the field has no free BEC Hatch cell");
            return;
        }
        final IGregTechTileEntity existing = gte(world, fh);
        if (existing == null || !(existing.getMetaTileEntity() instanceof MTEHatchBEC)) {
            if (!MTEReactorCoreMk1.placeGtMachine(
                world,
                fh[0],
                fh[1],
                fh[2],
                (short) CustomItemList.Hatch_BEC_Connector.get(1L)
                    .getItemDamage(),
                ForgeDirection.getOrientation(fh[3]))) {
                out.add("link: could not place the field's BEC Hatch at " + pos(fh));
                return;
            }
            field.checkStructure(true, field.getBaseMetaTileEntity());
            out.add("field: a BEC Hatch at " + pos(fh) + " facing " + ForgeDirection.getOrientation(fh[3]));
        }
        final ForgeDirection ff = ForgeDirection.getOrientation(fh[3]);
        final int[] goal = { fh[0] + ff.offsetX, fh[1] + ff.offsetY, fh[2] + ff.offsetZ };
        final List<int[]> path = route(world, start, goal);
        if (path == null) {
            out.add("link: no air route from " + pos(start) + " to " + pos(goal));
            return;
        }
        final short pipe = (short) CustomItemList.Pipe_BEC.get(1L)
            .getItemDamage();
        // every chunk the conduit crosses stays loaded: an unloaded conduit leaves the grid and splits the network
        final int[] box = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE };
        for (int[] c : path) {
            for (int k = 0; k < 3; k++) {
                box[k] = Math.min(box[k], c[k]);
                box[k + 3] = Math.max(box[k + 3], c[k]);
            }
        }
        out.add("conduit chunks kept: " + KerrTestBench.keep(world, box));
        int laid = 0;
        for (int[] c : path) {
            if (gte(world, c) != null) continue; // joined an existing conduit
            if (MTEReactorCoreMk1.placeGtMachine(world, c[0], c[1], c[2], pipe, ForgeDirection.UNKNOWN)) laid++;
        }
        out.add(
            "link: " + laid
                + " BEC conduit blocks from the machine's BEC Hatch "
                + pos(hp)
                + " (facing "
                + mf
                + ") to "
                + (path.isEmpty() ? "?" : pos(path.get(path.size() - 1))));
        out.add("the network settles over the next second (each element registers on its first tick)");
    }

    /**
     * The cheapest air route (each step 10, each turn 6 more: long straight runs) from {@code start} to {@code goal}
     * or to any air cell touching a BEC conduit already standing, inside the box around the two plus a margin. Both
     * ends are included. Null if none.
     */
    private static List<int[]> route(World world, int[] start, int[] goal) {
        final int x0 = Math.min(start[0], goal[0]) - MARGIN, x1 = Math.max(start[0], goal[0]) + MARGIN;
        final int y0 = Math.max(1, Math.min(start[1], goal[1]) - 2),
            y1 = Math.min(254, Math.max(start[1], goal[1]) + 8);
        final int z0 = Math.min(start[2], goal[2]) - MARGIN, z1 = Math.max(start[2], goal[2]) + MARGIN;
        final int nx = x1 - x0 + 1, ny = y1 - y0 + 1, nz = z1 - z0 + 1;
        final int cells = nx * ny * nz;
        final byte[] open = new byte[cells]; // 0 unknown, 1 air, 2 solid
        final int[] dist = new int[cells * 6];
        final int[] prev = new int[cells * 6];
        Arrays.fill(dist, Integer.MAX_VALUE);
        final PriorityQueue<long[]> queue = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        final int s = (start[0] - x0) + nx * ((start[1] - y0) + ny * (start[2] - z0));
        if (!air(world, open, s, start[0], start[1], start[2])) return null;
        for (int d = 0; d < 6; d++) {
            dist[s * 6 + d] = 0;
            prev[s * 6 + d] = -1;
            queue.add(new long[] { 0, s * 6L + d });
        }
        final ForgeDirection[] dirs = ForgeDirection.VALID_DIRECTIONS;
        int found = -1;
        while (!queue.isEmpty()) {
            final long[] top = queue.poll();
            final int state = (int) top[1];
            if (top[0] > dist[state]) continue;
            final int cell = state / 6, dir = state % 6;
            final int cx = cell % nx + x0, cy = (cell / nx) % ny + y0, cz = cell / (nx * ny) + z0;
            if ((cx == goal[0] && cy == goal[1] && cz == goal[2]) || (cell != s && touchesConduit(world, cx, cy, cz))) {
                found = state;
                break;
            }
            for (int d = 0; d < 6; d++) {
                final int ax = cx + dirs[d].offsetX, ay = cy + dirs[d].offsetY, az = cz + dirs[d].offsetZ;
                if (ax < x0 || ax > x1 || ay < y0 || ay > y1 || az < z0 || az > z1) continue;
                final int n = (ax - x0) + nx * ((ay - y0) + ny * (az - z0));
                if (!air(world, open, n, ax, ay, az)) continue;
                final int cost = dist[state] + 10 + (d == dir || prev[state] == -1 ? 0 : 6);
                final int ns = n * 6 + d;
                if (cost < dist[ns]) {
                    dist[ns] = cost;
                    prev[ns] = state;
                    queue.add(new long[] { cost, ns });
                }
            }
        }
        if (found < 0) return null;
        final List<int[]> path = new ArrayList<>();
        for (int st = found; st >= 0; st = prev[st]) {
            final int cell = st / 6;
            path.add(0, new int[] { cell % nx + x0, (cell / nx) % ny + y0, cell / (nx * ny) + z0 });
        }
        return path;
    }

    private static boolean air(World world, byte[] open, int i, int x, int y, int z) {
        if (open[i] == 0) open[i] = (byte) (world.isAirBlock(x, y, z) ? 1 : 2);
        return open[i] == 1;
    }

    private static boolean touchesConduit(World world, int x, int y, int z) {
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            final IGregTechTileEntity t = gte(world, new int[] { x + d.offsetX, y + d.offsetY, z + d.offsetZ });
            if (t != null && t.getMetaTileEntity() instanceof MTEPipeBEC) return true;
        }
        return false;
    }

    // ================================================================================= power

    /**
     * Every energy hatch of the GT multiblock at {@code p} becomes a GT Debug Energy Hatch (MAX, 64 A, same face), then
     * a re-check and switch-on: the bench kit charges its hatches once, which a MAX machine empties in seconds.
     */
    private static void power(World world, int[] p, List<String> out) {
        final MTEMultiBlockBase m = mteAt(world, p, MTEMultiBlockBase.class);
        if (m == null) {
            out.add("power: no GT multiblock at " + pos(p));
            return;
        }
        m.checkStructure(true, m.getBaseMetaTileEntity()); // the hatch lists are filled by a walk
        final List<gregtech.api.metatileentity.implementations.MTEHatch> hatches = new ArrayList<>(m.mEnergyHatches);
        if (m instanceof gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase) {
            for (gregtech.api.metatileentity.implementations.MTEHatch h : ((gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase<?>) m)
                .getExoticAndNormalEnergyHatchList()) if (!hatches.contains(h)) hatches.add(h);
        }
        int n = 0;
        for (gregtech.api.metatileentity.implementations.MTEHatch h : hatches) {
            final IGregTechTileEntity t = h.getBaseMetaTileEntity();
            if (t == null) continue;
            final int[] c = { t.getXCoord(), t.getYCoord(), t.getZCoord() };
            final ForgeDirection f = t.getFrontFacing();
            world.setBlockToAir(c[0], c[1], c[2]);
            if (!MTEReactorCoreMk1.placeGtMachine(
                world,
                c[0],
                c[1],
                c[2],
                (short) ItemList.DebugEnergyHatch.get(1L)
                    .getItemDamage(),
                f)) continue;
            final MTEHatchEnergyDebug d = mteAt(world, c, MTEHatchEnergyDebug.class);
            if (d != null) {
                d.setVoltageTier(13);
                d.setAmperage(64);
            }
            n++;
        }
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        m.checkStructure(true, base);
        base.enableWorking();
        out.add(
            "power: " + n
                + " energy hatch(es) at "
                + pos(p)
                + " are now GT Debug Energy Hatches; formed "
                + m.mMachine);
    }

    // ================================================================================ status

    private static void status(World world, int[] p, List<String> out) {
        final IGregTechTileEntity t = gte(world, p);
        final IMetaTileEntity m = t == null ? null : t.getMetaTileEntity();
        if (m instanceof MTEBECStorage) {
            final MTEBECStorage f = (MTEBECStorage) m;
            out.add(
                "Containment Field at " + pos(p)
                    + ": formed "
                    + f.mMachine
                    + ", running "
                    + (f.mMaxProgresstime > 0)
                    + ", network "
                    + (f.getNetwork() == null ? "none" : f.getNetwork().id)
                    + ", strength "
                    + f.getFieldStrength()
                    + ", stored "
                    + (long) f.getAmountStored()
                    + " L");
            for (it.unimi.dsi.fastutil.Pair<String, Long> e : f.getStoredCondensate()) {
                out.add("  " + e.left() + ": " + e.right() + " L");
            }
        } else if (m instanceof MTEBECAssembler) {
            final MTEBECAssembler a = (MTEBECAssembler) m;
            out.add(
                "Condensate Assembler at " + pos(p)
                    + ": formed "
                    + a.mMachine
                    + ", running "
                    + (a.mMaxProgresstime > 0)
                    + ", nanites "
                    + a.getAvailableNanites()
                    + " "
                    + (a.getCurrentNaniteTier() == null ? "none"
                        : a.getCurrentNaniteTier()
                            .describe()));
        } else if (m instanceof MTERealityBase) {
            final MTERealityBase<?> r = (MTERealityBase<?>) m;
            out.add(r.networkLine());
            final StringBuilder sb = new StringBuilder("regions reachable:");
            for (net.minecraftforge.fluids.Fluid f : com.nateyburr.beyondthestars.fluids.SLRealityFluids.regions()) {
                sb.append(' ')
                    .append(r.stored(f));
            }
            final Object routes = r.getNetwork() == null ? null : r.getNetwork().routeTracker.getEdges(r);
            sb.append("; routed neighbours now ")
                .append(
                    r.getRoutedNeighbours()
                        .size())
                .append(", in the route table ")
                .append(routes == null ? "none" : ((Object[]) routes).length);
            out.add(sb.toString());
            out.addAll(((MTERealityBase<?>) m).statusLines());
        } else if (m instanceof tectech.mechanics.boseEinsteinCondensate.BECFactoryElement) {
            final tectech.mechanics.boseEinsteinCondensate.BECFactoryElement e = (tectech.mechanics.boseEinsteinCondensate.BECFactoryElement) m;
            final List<tectech.mechanics.boseEinsteinCondensate.BECFactoryElement> nb = new ArrayList<>();
            e.getNeighbours(nb);
            out.add(
                m.getClass()
                    .getSimpleName() + " at "
                    + pos(p)
                    + ": network "
                    + (e.getNetwork() == null ? "none" : e.getNetwork().id)
                    + ", own neighbours "
                    + nb.size()
                    + ", grid edges "
                    + tectech.mechanics.boseEinsteinCondensate.BECFactoryGrid.INSTANCE.edges.get(e)
                        .size());
        } else {
            out.add("No BEC Containment Field, Condensate Assembler or Reality controller at " + pos(p));
        }
    }

    // ================================================================================ cells

    /**
     * A cell of {@code ch} in the controller's TecTech structure definition with air on some side, as world
     * {x, y, z, facing ordinal} (facing: the air side pointing most away from the structure's centre). With
     * {@code near} set, a cell already holding a BEC Hatch counts too and the cell nearest {@code near} wins;
     * otherwise the first free one (no GT tile yet) in definition order.
     */
    private static int[] hatchCell(World world, MTEBECMultiblockBase<?> m, char ch, int[] near) {
        final String[][] def = m.getDefinition();
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        final ExtendedFacing ef = m.getExtendedFacing();
        int a0 = 0, b0 = 0, c0 = 0, w = 0, h = 0;
        for (int c = 0; c < def.length; c++) {
            h = Math.max(h, def[c].length);
            for (int b = 0; b < def[c].length; b++) {
                w = Math.max(w, def[c][b].length());
                final int a = def[c][b].indexOf('~');
                if (a >= 0) {
                    a0 = a;
                    b0 = b;
                    c0 = c;
                }
            }
        }
        final int[] centre = SLConsoleTest.worldPos(
            ef,
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord(),
            new int[] { w / 2 - a0, h / 2 - b0, def.length / 2 - c0 });
        int[] best = null;
        long bestD = Long.MAX_VALUE;
        for (int c = 0; c < def.length; c++) {
            for (int b = 0; b < def[c].length; b++) {
                final String row = def[c][b];
                for (int a = 0; a < row.length(); a++) {
                    if (row.charAt(a) != ch) continue;
                    final int[] wp = SLConsoleTest.worldPos(
                        ef,
                        base.getXCoord(),
                        base.getYCoord(),
                        base.getZCoord(),
                        new int[] { a - a0, b - b0, c - c0 });
                    final IGregTechTileEntity t = gte(world, wp);
                    final boolean becHatch = t != null && t.getMetaTileEntity() instanceof MTEHatchBEC;
                    if (t != null && !(near != null && becHatch)) continue;
                    ForgeDirection out = null;
                    double bestDot = Double.NEGATIVE_INFINITY;
                    for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                        if (!world.isAirBlock(wp[0] + d.offsetX, wp[1] + d.offsetY, wp[2] + d.offsetZ)) continue;
                        final double dot = d.offsetX * (wp[0] - centre[0]) + d.offsetY * (wp[1] - centre[1])
                            + d.offsetZ * (wp[2] - centre[2]);
                        if (dot > bestDot) {
                            bestDot = dot;
                            out = d;
                        }
                    }
                    if (becHatch) out = t.getFrontFacing();
                    if (out == null) continue;
                    final int[] cell = { wp[0], wp[1], wp[2], out.ordinal() };
                    if (near == null) return cell;
                    final long dx = wp[0] - near[0], dy = wp[1] - near[1], dz = wp[2] - near[2];
                    final long d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 < bestD) {
                        bestD = d2;
                        best = cell;
                    }
                }
            }
        }
        return best;
    }

    private static IGregTechTileEntity gte(World world, int[] p) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof IGregTechTileEntity ? (IGregTechTileEntity) te : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T mteAt(World world, int[] p, Class<T> type) {
        final IGregTechTileEntity t = gte(world, p);
        final IMetaTileEntity m = t == null ? null : t.getMetaTileEntity();
        return type.isInstance(m) ? (T) m : null;
    }

    private static String pos(int[] p) {
        return "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }
}
