package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.items.parts.ItemFacade;
import appeng.parts.automation.PartLevelEmitter;
import appeng.parts.automation.PartSharedItemBus;
import appeng.parts.automation.PartUpgradeable;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/**
 * DEV-ONLY (the automation photo rigs and the video recorder): small helpers that place real AE2 / AE2FC parts the
 * way a player would (cables, buses with cards, a level emitter, facades, the drive and its cells) and route cable
 * runs. Nothing here is loaded in normal play; AE2 and AE2FC are GT's own dependencies, so they are always on the dev
 * classpath.
 */
final class MeParts {

    private MeParts() {}

    /** Cable kinds: glass, covered, smart, dense (smart dense) and dense covered. */
    enum Kind {
        GLASS,
        COVERED,
        SMART,
        DENSE,
        DENSE_COVERED
    }

    private static ItemStack cableStack(Kind kind, AEColor color) {
        switch (kind) {
            case COVERED:
                return AEApi.instance()
                    .definitions()
                    .parts()
                    .cableCovered()
                    .stack(color, 1);
            case SMART:
                return AEApi.instance()
                    .definitions()
                    .parts()
                    .cableSmart()
                    .stack(color, 1);
            case DENSE:
                return AEApi.instance()
                    .definitions()
                    .parts()
                    .cableDense()
                    .stack(color, 1);
            case DENSE_COVERED:
                return AEApi.instance()
                    .definitions()
                    .parts()
                    .cableDenseCovered()
                    .stack(color, 1);
            default:
                return AEApi.instance()
                    .definitions()
                    .parts()
                    .cableGlass()
                    .stack(color, 1);
        }
    }

    /** The cable-bus tile at {@code p} (placed if the cell holds something else). */
    static IPartHost host(World world, int[] p) {
        final Block block = AEApi.instance()
            .definitions()
            .blocks()
            .multiPart()
            .maybeBlock()
            .orNull();
        if (block == null) return null;
        TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IPartHost)) {
            world.setBlock(p[0], p[1], p[2], block, 0, 3);
            te = world.getTileEntity(p[0], p[1], p[2]);
        }
        return te instanceof IPartHost ? (IPartHost) te : null;
    }

    /** A cable of {@code kind} and {@code color} at {@code p} (nothing if the cell already has a cable). */
    static void cable(World world, int[] p, Kind kind, AEColor color, EntityPlayer player) {
        final IPartHost h = host(world, p);
        if (h == null || h.getPart(ForgeDirection.UNKNOWN) != null) return;
        h.addPart(cableStack(kind, color), ForgeDirection.UNKNOWN, player);
    }

    /** A cable along every cell of {@code path}. */
    static void run(World world, List<int[]> path, Kind kind, AEColor color, EntityPlayer player) {
        for (int[] q : path) cable(world, q, kind, color, player);
    }

    static void block(World world, int[] p, Block block) {
        if (block != null) world.setBlock(p[0], p[1], p[2], block, 0, 3);
    }

    static Block controllerBlock() {
        return AEApi.instance()
            .definitions()
            .blocks()
            .creativeEnergyController()
            .maybeBlock()
            .orNull();
    }

    static Block driveBlock() {
        return AEApi.instance()
            .definitions()
            .blocks()
            .drive()
            .maybeBlock()
            .orNull();
    }

    static Block interfaceBlock() {
        return AEApi.instance()
            .definitions()
            .blocks()
            .iface()
            .maybeBlock()
            .orNull();
    }

    /**
     * An item export bus (or, with {@code fluid}, AE2FC's fluid export bus) on the cable at {@code p}, facing
     * {@code side}: {@code filter} in its first config slot, a redstone card set to "active with signal" and
     * {@code speedCards} acceleration cards. With {@code facade} a quartz facade goes on the top face, so redstone
     * dust can stand on the cable block and power it.
     */
    static PartSharedItemBus<?> exportBus(World world, int[] p, ForgeDirection side, IAEStack<?> filter,
        int speedCards, boolean fluid, boolean facade, Kind kind, AEColor color, EntityPlayer player,
        List<String> log, String tag) {
        cable(world, p, kind, color, player);
        final IPartHost h = host(world, p);
        if (h == null) {
            log.add(tag + "no cable bus at " + RenderTest.pos(p));
            return null;
        }
        final ItemStack busItem = fluid ? fluidExportBusItem()
            : AEApi.instance()
                .definitions()
                .parts()
                .exportBus()
                .maybeStack(1)
                .orNull();
        if (busItem == null || h.addPart(busItem, side, player) == null) {
            log.add(tag + "could not add " + (fluid ? "a fluid" : "an") + " export bus at " + RenderTest.pos(p));
            return null;
        }
        final IPart part = h.getPart(side);
        if (!(part instanceof PartSharedItemBus)) {
            log.add(tag + "the export bus at " + RenderTest.pos(p) + " is " + part);
            return null;
        }
        final PartSharedItemBus<?> bus = (PartSharedItemBus<?>) part;
        bus.getAEInventoryByName(StorageName.CONFIG)
            .putAEStackInSlot(0, filter);
        upgrades(bus, speedCards);
        if (facade) facade(h, ForgeDirection.UP, log, tag);
        bus.getConfigManager()
            .putSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.HIGH_SIGNAL);
        h.markForUpdate();
        log.add(
            tag + (fluid ? "fluid " : "") + "export bus at " + RenderTest.pos(p)
                + " facing "
                + side
                + ": "
                + describe(filter)
                + ", redstone card (active with signal)"
                + (speedCards > 0 ? ", " + speedCards + " acceleration card(s)" : ""));
        return bus;
    }

    /** AE2FC's fluid storage bus on the cable at {@code p}, facing {@code side}, read only (the network takes from it). */
    static boolean storageBus(World world, int[] p, ForgeDirection side, EntityPlayer player, List<String> log,
        String tag) {
        final IPartHost h = host(world, p);
        final Item item = ae2fc("FLUID_STORAGE_BUS");
        if (h == null || item == null || h.addPart(new ItemStack(item), side, player) == null) {
            log.add(tag + "could not add a fluid storage bus at " + RenderTest.pos(p));
            return false;
        }
        final IPart part = h.getPart(side);
        if (part instanceof PartUpgradeable) {
            try {
                ((PartUpgradeable) part).getConfigManager()
                    .putSetting(Settings.ACCESS, appeng.api.config.AccessRestriction.READ);
            } catch (RuntimeException unsupported) {
                log.add(tag + "the fluid storage bus has no access setting: " + unsupported);
            }
        }
        h.markForUpdate();
        return true;
    }

    /** AE2FC's fluid export bus item, or null (AE2FC absent). */
    static ItemStack fluidExportBusItem() {
        final Item item = ae2fc("FLUID_EXPORT_BUS");
        return item == null ? null : new ItemStack(item);
    }

    /** One of AE2FC's items by its {@code ItemAndBlockHolder} field name (reflection: AE2FC is optional). */
    static Item ae2fc(String field) {
        try {
            final Object v = Class.forName("com.glodblock.github.loader.ItemAndBlockHolder")
                .getField(field)
                .get(null);
            return v instanceof Item ? (Item) v : null;
        } catch (ReflectiveOperationException | LinkageError absent) {
            return null;
        }
    }

    static String describe(IAEStack<?> s) {
        if (s instanceof IAEItemStack) return ((IAEItemStack) s).getItemStack()
            .getDisplayName();
        if (s instanceof IAEFluidStack) return ((IAEFluidStack) s).getFluidStack()
            .getLocalizedName();
        return String.valueOf(s);
    }

    static IAEItemStack item(ItemStack stack, long count) {
        final IAEItemStack s = AEItemStack.create(stack);
        s.setStackSize(count);
        return s;
    }

    static IAEFluidStack fluid(Fluid fluid, long litres) {
        final IAEFluidStack s = AEFluidStack.create(new FluidStack(fluid, 1));
        s.setStackSize(litres);
        return s;
    }

    /** A quartz facade on {@code side}: a solid face redstone dust can stand on. */
    static void facade(IPartHost h, ForgeDirection side, List<String> log, String tag) {
        final Item item = AEApi.instance()
            .definitions()
            .items()
            .facade()
            .maybeItem()
            .orNull();
        if (!(item instanceof ItemFacade)) {
            log.add(tag + "no AE2 facade item");
            return;
        }
        final ItemStack f = ((ItemFacade) item).createFacadeForItem(new ItemStack(Blocks.quartz_block), false);
        if (f == null || !h.getFacadeContainer()
            .addFacade(((ItemFacade) item).createPartFromItemStack(f, side))) {
            log.add(tag + "could not add a facade");
            return;
        }
        h.markForUpdate();
    }

    /** A redstone card first, then {@code speedCards} acceleration cards. */
    static void upgrades(PartUpgradeable part, int speedCards) {
        final net.minecraft.inventory.IInventory up = part.getInventoryByName("upgrades");
        if (up == null) return;
        int slot = 0;
        final ItemStack red = AEApi.instance()
            .definitions()
            .materials()
            .cardRedstone()
            .maybeStack(1)
            .orNull();
        if (red != null) up.setInventorySlotContents(slot++, red);
        for (int i = 0; i < speedCards && slot < up.getSizeInventory(); i++) {
            final ItemStack sp = AEApi.instance()
                .definitions()
                .materials()
                .cardSpeed()
                .maybeStack(1)
                .orNull();
            if (sp != null) up.setInventorySlotContents(slot++, sp);
        }
    }

    /** A level emitter on the cable at {@code p}, facing {@code side}: on while the network holds {@code level}+. */
    static boolean levelEmitter(World world, int[] p, ForgeDirection side, ItemStack watch, long level, Kind kind,
        AEColor color, EntityPlayer player, List<String> log, String tag) {
        cable(world, p, kind, color, player);
        final IPartHost h = host(world, p);
        final ItemStack item = AEApi.instance()
            .definitions()
            .parts()
            .levelEmitter()
            .maybeStack(1)
            .orNull();
        if (h == null || item == null || h.addPart(item, side, player) == null) {
            log.add(tag + "could not add a level emitter at " + RenderTest.pos(p));
            return false;
        }
        final IPart part = h.getPart(side);
        if (part instanceof PartLevelEmitter) {
            final PartLevelEmitter e = (PartLevelEmitter) part;
            e.getAEInventoryByName(StorageName.CONFIG)
                .putAEStackInSlot(0, AEItemStack.create(watch));
            e.setReportingValue(level);
            e.getConfigManager()
                .putSetting(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);
            h.markForUpdate();
        }
        return true;
    }

    // ------------------------------------------------------------------------------------ cells

    /** A storage cell filled (by script) with {@code stacks} on {@code channel}; a log line says what went in. */
    static ItemStack filledCell(ItemStack cell, StorageChannel channel, List<String> log, String tag,
        IAEStack<?>... stacks) {
        if (cell == null) {
            log.add(tag + "no cell item");
            return null;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final IMEInventoryHandler inv = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(cell, null, channel);
        if (inv == null) {
            log.add(tag + cell.getDisplayName() + " has no " + channel + " inventory");
            return cell;
        }
        final BaseActionSource src = new BaseActionSource();
        final StringBuilder sb = new StringBuilder(tag + cell.getDisplayName() + ":");
        for (IAEStack<?> s : stacks) {
            @SuppressWarnings("unchecked")
            final IAEStack<?> left = (IAEStack<?>) inv.injectItems(s.copy(), Actionable.MODULATE, src);
            sb.append(" ")
                .append(s.getStackSize())
                .append(" ")
                .append(describe(s))
                .append(left == null ? "" : " (" + left.getStackSize() + " did not fit)");
        }
        log.add(sb.toString());
        return cell;
    }

    /** Puts {@code cell} into slot {@code slot} of the ME drive at {@code p}. */
    static boolean intoDrive(World world, int[] p, int slot, ItemStack cell) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof TileDrive) || cell == null) return false;
        ((TileDrive) te).getInternalInventory()
            .setInventorySlotContents(slot, cell);
        return true;
    }

    /** The storage grid behind the cable at {@code p}, or null. */
    static IStorageGrid storage(World world, int[] p) {
        if (p == null) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IPartHost)) return null;
        final IPart cablePart = ((IPartHost) te).getPart(ForgeDirection.UNKNOWN);
        if (cablePart == null) return null;
        final IGridNode node = cablePart.getGridNode();
        if (node == null) return null;
        final IGrid grid = node.getGrid();
        return grid == null ? null : grid.getCache(IStorageGrid.class);
    }

    /** How many of {@code what} the network behind the cable at {@code p} holds (-1 if no grid). */
    static long count(World world, int[] p, ItemStack what) {
        final IStorageGrid s = storage(world, p);
        if (s == null || s.getItemInventory() == null || what == null) return -1;
        final IAEItemStack found = s.getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(what));
        return found == null ? 0 : found.getStackSize();
    }

    /** Litres of {@code fluid} in the network behind the cable at {@code p} (-1 if no grid). */
    static long litres(World world, int[] p, Fluid fluid) {
        final IStorageGrid s = storage(world, p);
        if (s == null || s.getFluidInventory() == null || fluid == null) return -1;
        final IAEFluidStack found = s.getFluidInventory()
            .getStorageList()
            .findPrecise(AEFluidStack.create(new FluidStack(fluid, 1)));
        return found == null ? 0 : found.getStackSize();
    }

    /** True while the part host at {@code p} sees a redstone signal (the bus card's input). */
    static boolean powered(World world, int[] p) {
        return p != null && world.isBlockIndirectlyGettingPowered(p[0], p[1], p[2]);
    }

    // ----------------------------------------------------------------------------------- routes

    static long key(int[] p) {
        return ((long) (p[0] & 0x1fffff) << 42) | ((long) (p[1] & 0x1fffff) << 21) | (p[2] & 0x1fffff);
    }

    private static final int[][] DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    /** A straight line of cells from {@code a} to {@code b} (both included); null unless axis-aligned. */
    static List<int[]> line(int[] a, int[] b) {
        int axes = 0;
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) axes++;
        if (axes > 1) return null;
        final List<int[]> out = new ArrayList<>();
        final int n = Math.max(Math.abs(b[0] - a[0]), Math.max(Math.abs(b[1] - a[1]), Math.abs(b[2] - a[2])));
        for (int i = 0; i <= n; i++) {
            out.add(
                new int[] { a[0] + Integer.signum(b[0] - a[0]) * i, a[1] + Integer.signum(b[1] - a[1]) * i,
                    a[2] + Integer.signum(b[2] - a[2]) * i });
        }
        return out;
    }

    /** True if every cell of {@code path} is air (or already a cable bus) and not in {@code avoid}. */
    static boolean clear(World world, List<int[]> path, Set<Long> avoid) {
        if (path == null) return false;
        for (int[] q : path) {
            if (avoid != null && avoid.contains(key(q))) return false;
            if (!world.isAirBlock(q[0], q[1], q[2]) && !(world.getTileEntity(q[0], q[1], q[2]) instanceof IPartHost))
                return false;
        }
        return true;
    }

    /**
     * Turn-penalised shortest air path in 3D, both ends included ({@code turn} is the cost of a bend, so a high value
     * gives long straight runs). Cells already holding a cable bus count as free. Null if none within the box.
     */
    static List<int[]> route(World world, int[] from, int[] to, Set<Long> avoid, int turn, int minY) {
        final int r = 24;
        final int minX = Math.min(from[0], to[0]) - r, minYY = Math.max(minY, Math.min(from[1], to[1]) - 8),
            minZ = Math.min(from[2], to[2]) - r;
        final int w = Math.max(from[0], to[0]) + r - minX + 1,
            hgt = Math.min(254, Math.max(from[1], to[1]) + 12) - minYY + 1, d = Math.max(from[2], to[2]) + r - minZ + 1;
        final int n = w * hgt * d;
        final int[] dist = new int[n * 6];
        final int[] prev = new int[n * 6];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
        final PriorityQueue<int[]> q = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        final int start = ((from[0] - minX) * hgt + (from[1] - minYY)) * d + (from[2] - minZ);
        for (int k = 0; k < 6; k++) {
            dist[start * 6 + k] = 0;
            q.add(new int[] { 0, start * 6 + k });
        }
        int goal = -1;
        while (!q.isEmpty()) {
            final int[] cur = q.poll();
            if (cur[0] > dist[cur[1]]) continue;
            final int cell = cur[1] / 6, k = cur[1] % 6;
            final int cx = cell / (hgt * d) + minX, cy = (cell / d) % hgt + minYY, cz = cell % d + minZ;
            if (cx == to[0] && cy == to[1] && cz == to[2]) {
                goal = cur[1];
                break;
            }
            for (int nk = 0; nk < 6; nk++) {
                final int nx = cx + DIRS[nk][0], ny = cy + DIRS[nk][1], nz = cz + DIRS[nk][2];
                if (nx < minX || ny < minYY || nz < minZ || nx >= minX + w || ny >= minYY + hgt || nz >= minZ + d)
                    continue;
                final int[] np = { nx, ny, nz };
                final boolean isGoal = nx == to[0] && ny == to[1] && nz == to[2];
                if (!isGoal) {
                    if (avoid != null && avoid.contains(key(np))) continue;
                    if (!world.isAirBlock(nx, ny, nz) && !(world.getTileEntity(nx, ny, nz) instanceof IPartHost))
                        continue;
                }
                final int cost = cur[0] + 1 + (nk != k ? turn : 0);
                final int ncell = ((nx - minX) * hgt + (ny - minYY)) * d + (nz - minZ);
                if (cost < dist[ncell * 6 + nk]) {
                    dist[ncell * 6 + nk] = cost;
                    prev[ncell * 6 + nk] = cur[1];
                    q.add(new int[] { cost, ncell * 6 + nk });
                }
            }
        }
        if (goal < 0) return null;
        final List<int[]> path = new ArrayList<>();
        for (int idx = goal; idx >= 0; idx = prev[idx]) {
            final int cell = idx / 6;
            path.add(0, new int[] { cell / (hgt * d) + minX, (cell / d) % hgt + minYY, cell % d + minZ });
            if (prev[idx] < 0) break;
        }
        return path;
    }
}
