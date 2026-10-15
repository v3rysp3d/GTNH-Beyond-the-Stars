package com.nateyburr.beyondthestars.machines.kerr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * KERR REDESIGN test bench (console / op only, never reached in play): drives a Kerr module's
 * recipes on a dev server. {@code /btskerr module x y z <c|x> stock|status|tier|clear} acts on the
 * module standing in the KEE-at-x-y-z's compressor ({@code c}) or extractor ({@code x}) slot;
 * {@code /btskerr damage|repair x y z} knocks one plain cage block next to the KEE controller out
 * (unforming it) and puts it back. Server side only.
 */
public final class KerrTestBench {

    private KerrTestBench() {}

    /** Cells taken out by {@link #damage}, keyed by the KEE position, so {@link #repair} can restore them. */
    private static final Map<String, Object[]> DAMAGED = new HashMap<>();

    private static final int[][] NEAR_CONTROLLER = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 1, 1, 0 },
        { -1, 1, 0 }, { 0, 0, 1 } };

    /** The KEE controller at (x, y, z), or null. */
    public static MTEKerrEngine keeAt(World world, int x, int y, int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEKerrEngine ? (MTEKerrEngine) mte : null;
    }

    /** The module machine in the given slot of the KEE at (x, y, z), or null. */
    public static MTEKerrModuleBase<?> moduleOf(World world, MTEKerrEngine kee, KerrSlot slot) {
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();
        final int[] p = slot.modulePos(kee.getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord());
        final IGregTechTileEntity te = KerrSlot.gtTileAt(world, p);
        if (te == null || !(te.getMetaTileEntity() instanceof MTEKerrModuleBase)) return null;
        final MTEKerrModuleBase<?> m = (MTEKerrModuleBase<?>) te.getMetaTileEntity();
        return slot.accepts(m) ? m : null;
    }

    /**
     * A chunk the bench just (re)loaded: GT's first structure check has not run yet, so the hatch lists are empty and
     * the machine reads NOT FORMED. Walk it now (as the Eye bench's {@code benchEnsureChecked}) so a command does not
     * act on that transient state (B-71: an autorig right after {@code keep} fitted a second pair of Directive
     * Hatches; a module stock found no energy hatch).
     */
    public static void ensureChecked(gregtech.api.metatileentity.implementations.MTEMultiBlockBase multi) {
        final gregtech.api.interfaces.tileentity.IGregTechTileEntity base = multi.getBaseMetaTileEntity();
        if (multi.mStartUpCheck < 0 || base == null) return;
        multi.checkStructure(true, base);
        multi.mStartUpCheck = -1;
    }

    /**
     * {@code module ... <c|x> <action> [args]}. Actions: {@code stock [map#] [longest|first|n] [copies]}
     * picks a recipe of one of the module's maps that fits its energy and fills its input bus (and input
     * hatch for fluids) with {@code copies} times its inputs; {@code status}; {@code tier <t> [amps]} sets
     * the module's creative energy hatches; {@code clear} empties its output buses and input bus; {@code gui [info]}
     * prints the header and the kit
     * rows (or the Info page) its GUI would show now.
     */
    public static List<String> module(World world, MTEKerrEngine kee, KerrSlot slot, String[] args, int from) {
        final List<String> out = new ArrayList<>();
        final MTEKerrModuleBase<?> m = moduleOf(world, kee, slot);
        if (m == null) {
            out.add(slot.label + " module: none in its slot");
            return out;
        }
        ensureChecked(kee);
        ensureChecked(m);
        final String action = from < args.length ? args[from].toLowerCase(Locale.ROOT) : "status";
        switch (action) {
            case "stock":
                out.addAll(stock(m, args, from + 1));
                break;
            case "tier":
                out.add(tier(m, args, from + 1));
                break;
            case "clear":
                out.add(clear(m));
                break;
            case "gui":
                // controller GUI cleanup: the header and kit rows the module's GUI would show now (spec §5.3);
                // the link is re-read first (the module's own 20-tick check does the same)
                m.refreshLink();
                out.addAll(
                    new MTEKerrModuleGui(m)
                        .benchDump(from + 1 < args.length && "info".equalsIgnoreCase(args[from + 1])));
                return out;
            case "energy":
                out.add(
                    KerrKit.refitModuleEnergy(world, kee, m) ? "energy: creative hatch fitted, structure re-checked"
                        : "energy: no free arc socket");
                break;
            default:
                break;
        }
        out.add(status(m));
        return out;
    }

    private static List<String> stock(MTEKerrModuleBase<?> m, String[] args, int from) {
        final List<String> out = new ArrayList<>();
        final List<RecipeMap<?>> maps = new ArrayList<>(m.getAvailableRecipeMaps());
        int mapIndex = 0;
        String pick = "longest";
        int copies = 2;
        if (from < args.length) mapIndex = Integer.parseInt(args[from]);
        if (from + 1 < args.length) pick = args[from + 1].toLowerCase(Locale.ROOT);
        if (from + 2 < args.length) copies = Integer.parseInt(args[from + 2]);
        if (mapIndex < 0 || mapIndex >= maps.size()) {
            out.add("stock: map index " + mapIndex + " out of 0.." + (maps.size() - 1));
            return out;
        }
        final RecipeMap<?> map = maps.get(mapIndex);
        final long maxEu = m.getMaxInputEu();
        final List<GTRecipe> candidates = new ArrayList<>();
        for (GTRecipe r : map.getAllRecipes()) {
            if (!r.mEnabled || r.mFakeRecipe || r.mEUt > maxEu || r.mInputs == null) continue;
            boolean ok = r.mInputs.length > 0;
            for (ItemStack in : r.mInputs) {
                // stack size 0 = not consumed (a programmed circuit, e.g. SL-Q23's circuit 8): still stockable
                if (in == null || in.getItem() == null || in.stackSize < 0) ok = false;
            }
            if (r.mFluidInputs != null) {
                for (FluidStack f : r.mFluidInputs) {
                    if (f == null) ok = false;
                }
            }
            if (ok) candidates.add(r);
        }
        if (candidates.isEmpty()) {
            out.add("stock: no usable recipe in " + map.unlocalizedName + " at " + maxEu + " EU/t");
            return out;
        }
        final GTRecipe recipe;
        if (pick.equals("first")) {
            recipe = candidates.get(0);
        } else if (pick.equals("longest")) {
            candidates.sort(
                Comparator.comparingLong((GTRecipe r) -> (long) r.mDuration)
                    .reversed());
            recipe = candidates.get(0);
        } else {
            recipe = candidates.get(Math.min(candidates.size() - 1, Math.max(0, Integer.parseInt(pick))));
        }
        // Items into the first input bus, one stack per input per copy.
        int placed = 0;
        if (!m.mInputBusses.isEmpty()) {
            final IGregTechTileEntity inv = m.mInputBusses.get(0)
                .getBaseMetaTileEntity();
            int slot = 0;
            for (int c = 0; c < copies; c++) {
                for (ItemStack in : recipe.mInputs) {
                    final ItemStack s = in.copy();
                    if (s.getItemDamage() == Short.MAX_VALUE) s.setItemDamage(0);
                    s.stackSize = in.stackSize == 0 ? 1
                        : Math.min(s.getMaxStackSize(), Math.max(s.stackSize, s.getMaxStackSize()));
                    while (slot < inv.getSizeInventory() && inv.getStackInSlot(slot) != null) slot++;
                    if (slot >= inv.getSizeInventory()) break;
                    inv.setInventorySlotContents(slot, s);
                    placed += s.stackSize;
                }
            }
        }
        int fluids = 0;
        if (recipe.mFluidInputs != null) {
            for (FluidStack f : recipe.mFluidInputs) {
                for (MTEHatchInput hatch : m.mInputHatches) {
                    if (hatch.getFluidAmount() > 0) continue;
                    final FluidStack fill = f.copy();
                    fill.amount = hatch.getCapacity();
                    hatch.setFillableStack(fill);
                    fluids++;
                    break;
                }
            }
        }
        out.add(
            String.format(
                Locale.ROOT,
                "stock: %s recipe %s -> %s, %d ticks at %d EU/t (%d candidates); placed %d items, %d fluid(s)",
                map.unlocalizedName,
                names(recipe.mInputs, recipe.mFluidInputs),
                names(recipe.mOutputs, recipe.mFluidOutputs),
                recipe.mDuration,
                recipe.mEUt,
                candidates.size(),
                placed,
                fluids));
        return out;
    }

    private static String tier(MTEKerrModuleBase<?> m, String[] args, int from) {
        final int t = from < args.length ? Integer.parseInt(args[from]) : 12;
        final int amps = from + 1 < args.length ? Integer.parseInt(args[from + 1]) : 16;
        int n = 0;
        for (MTEHatch h : m.getExoticAndNormalEnergyHatchList()) {
            if (h instanceof MTEHatchEnergyDebug) {
                ((MTEHatchEnergyDebug) h).setVoltageTier(t);
                ((MTEHatchEnergyDebug) h).setAmperage(amps);
                n++;
            }
        }
        m.checkStructure(true, m.getBaseMetaTileEntity());
        return "tier: " + n
            + " creative hatch(es) set to tier "
            + t
            + ", "
            + amps
            + " A; max input "
            + m.getMaxInputEu()
            + " EU/t";
    }

    private static String clear(MTEKerrModuleBase<?> m) {
        int n = 0;
        for (MTEHatchOutputBus bus : m.mOutputBusses) {
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) != null) n++;
                inv.setInventorySlotContents(i, null);
            }
        }
        for (MTEHatchInputBus bus : m.mInputBusses) {
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) != null) n++;
                inv.setInventorySlotContents(i, null);
            }
        }
        int f = 0;
        for (MTEHatchInput hatch : m.mInputHatches) {
            if (hatch.getFluidAmount() > 0) f++;
            hatch.setFillableStack(null);
        }
        for (gregtech.api.metatileentity.implementations.MTEHatchOutput hatch : m.mOutputHatches) {
            if (hatch.getFluidAmount() > 0) f++;
            hatch.setDrainableStack(null);
        }
        return "clear: " + n + " stack(s) removed from the input and output buses, " + f + " fluid hatch(es) emptied";
    }

    /** One line: formed, link, activity, progress, the running recipe's outputs, buses. */
    public static String status(MTEKerrModuleBase<?> m) {
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        final StringBuilder sb = new StringBuilder();
        sb.append(m.slot().label)
            .append(" module ")
            .append(KerrKit.pos(base.getXCoord(), base.getYCoord(), base.getZCoord()))
            .append(": ")
            .append(m.mMachine ? "FORMED" : "NOT FORMED")
            .append(", link ")
            .append(m.getLink())
            .append(base.isActive() ? ", ACTIVE" : ", idle")
            .append(base.isAllowedToWork() ? "" : " (switched off)")
            .append(", progress ")
            .append(m.mProgresstime)
            .append('/')
            .append(m.mMaxProgresstime)
            .append(", lastParallel ")
            .append(m.lastParallel)
            .append(String.format(Locale.ROOT, ", bonus p%d x%.2f", m.bonusParallel(), m.bonusSpeed()))
            .append(", running -> ")
            .append(names(m.mOutputItems, null))
            .append("; in: ")
            .append(busContents(m.mInputBusses))
            .append("; out (")
            .append(m.mOutputBusses.size())
            .append(" bus(es), ")
            .append(m.mOutputHatches.size())
            .append(" hatch(es)): ")
            .append(busContents(m.mOutputBusses))
            .append("; fluids in: ")
            .append(tankContents(m.mInputHatches))
            .append("; fluids out: ")
            .append(tankContents(m.mOutputHatches))
            .append("; dropped near the controller: ")
            .append(drops(base));
        return sb.toString();
    }

    /** Item entities near the controller, down to 40 blocks below it (migration drops what does not fit). */
    private static String drops(IGregTechTileEntity base) {
        final World world = base.getWorld();
        if (world == null) return "?";
        final AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            base.getXCoord() - 6,
            base.getYCoord() - 40,
            base.getZCoord() - 6,
            base.getXCoord() + 7,
            base.getYCoord() + 4,
            base.getZCoord() + 7);
        final Map<String, Integer> totals = new LinkedHashMap<>();
        for (Object o : world.getEntitiesWithinAABB(EntityItem.class, box)) {
            final ItemStack s = ((EntityItem) o).getEntityItem();
            if (s != null && s.getItem() != null) totals.merge(s.getDisplayName(), s.stackSize, Integer::sum);
        }
        if (totals.isEmpty()) return "none";
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getValue())
                .append("x ")
                .append(e.getKey());
        }
        return sb.toString();
    }

    private static String busContents(List<? extends MTEHatch> buses) {
        final Map<String, Integer> totals = new LinkedHashMap<>();
        for (MTEHatch bus : buses) {
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            if (inv == null) continue;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                final ItemStack s = inv.getStackInSlot(i);
                if (s == null || s.getItem() == null) continue;
                totals.merge(s.getDisplayName(), s.stackSize, Integer::sum);
            }
        }
        if (totals.isEmpty()) return "empty";
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getValue())
                .append("x ")
                .append(e.getKey());
        }
        return sb.toString();
    }

    private static String tankContents(List<? extends MTEHatch> hatches) {
        final StringBuilder sb = new StringBuilder();
        for (MTEHatch h : hatches) {
            if (!(h instanceof MTEBasicTank)) continue;
            final FluidStack f = ((MTEBasicTank) h).getFluid();
            if (f == null || f.amount <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(f.amount)
                .append("L ")
                .append(f.getLocalizedName());
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    private static String names(ItemStack[] items, FluidStack[] fluids) {
        final StringBuilder sb = new StringBuilder("[");
        if (items != null) {
            for (ItemStack s : items) {
                if (s == null || s.getItem() == null) continue;
                if (sb.length() > 1) sb.append(", ");
                String name;
                try {
                    name = s.getDisplayName();
                } catch (RuntimeException e) {
                    name = String.valueOf(s.getItem());
                }
                sb.append(s.stackSize)
                    .append("x ")
                    .append(name);
            }
        }
        if (fluids != null) {
            for (FluidStack f : fluids) {
                if (f == null) continue;
                if (sb.length() > 1) sb.append(", ");
                sb.append(f.amount)
                    .append("L ")
                    .append(f.getLocalizedName());
            }
        }
        return sb.append(']')
            .toString();
    }

    // ============================================================================ damage / repair

    /** Knocks one plain (tile-less) cage block next to the KEE controller out and re-walks the KEE. */
    public static String damage(World world, MTEKerrEngine kee) {
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();
        final ExtendedFacing f = kee.getExtendedFacing();
        final String key = KerrKit.pos(base.getXCoord(), base.getYCoord(), base.getZCoord());
        if (DAMAGED.containsKey(key)) return "damage: already damaged at " + key + "; repair first";
        for (int[] rel : NEAR_CONTROLLER) {
            final int[] p = SLConsoleTest.worldPos(f, base.getXCoord(), base.getYCoord(), base.getZCoord(), rel);
            final Block block = world.getBlock(p[0], p[1], p[2]);
            if (block == null || block.isAir(world, p[0], p[1], p[2])
                || world.getTileEntity(p[0], p[1], p[2]) != null) {
                continue;
            }
            final int meta = world.getBlockMetadata(p[0], p[1], p[2]);
            DAMAGED.put(key, new Object[] { p, block, meta });
            world.setBlockToAir(p[0], p[1], p[2]);
            kee.checkStructure(true, base);
            return "damage: removed " + Block.blockRegistry.getNameForObject(block)
                + "@"
                + meta
                + " at "
                + KerrKit.pos(p)
                + "; KEE "
                + (kee.mMachine ? "STILL FORMED" : "NOT FORMED");
        }
        return "damage: no plain block next to the controller";
    }

    /** Puts back what {@link #damage} took and re-walks the KEE. */
    public static String repair(World world, MTEKerrEngine kee) {
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();
        final String key = KerrKit.pos(base.getXCoord(), base.getYCoord(), base.getZCoord());
        final Object[] d = DAMAGED.remove(key);
        if (d == null) return "repair: nothing recorded for " + key;
        final int[] p = (int[]) d[0];
        world.setBlock(p[0], p[1], p[2], (Block) d[1], (Integer) d[2], 3);
        kee.checkStructure(true, base);
        return "repair: restored " + KerrKit.pos(p) + "; KEE " + (kee.mMachine ? "FORMED" : "STILL NOT FORMED");
    }

    // ============================================================================ seeds, bolts, power

    /**
     * {@code /btskerr items x y z <seeds> <collapsers> [bolts]}: empties the KEE's bolt buses, then puts that many
     * Black
     * Hole Seeds, Collapsers and Neutronium bolts in the first one (the quest reward in a bolt bus, QA-A03 / QA-A04).
     */
    public static String items(MTEKerrEngine kee, int seeds, int collapsers, int bolts) {
        final List<MTEHatchInputBus> buses = kee.boltBuses();
        MTEHatchInputBus first = null;
        for (MTEHatchInputBus bus : buses) {
            if (bus == null || !bus.isValid()) continue;
            if (first == null) first = bus;
            for (int i = 0; i < bus.getSizeInventory(); i++) bus.getBaseMetaTileEntity()
                .setInventorySlotContents(i, null);
        }
        if (first == null) return "items: no bolt bus";
        int slot = 0;
        final IGregTechTileEntity inv = first.getBaseMetaTileEntity();
        final ItemStack seed = gregtech.api.enums.ItemList.Black_Hole_Opener.get(1L);
        final ItemStack closer = gregtech.api.enums.ItemList.Black_Hole_Closer.get(1L);
        if (seeds > 0 && seed != null)
            inv.setInventorySlotContents(slot++, gregtech.api.util.GTUtility.copyAmount(seeds, seed));
        if (collapsers > 0 && closer != null)
            inv.setInventorySlotContents(slot++, gregtech.api.util.GTUtility.copyAmount(collapsers, closer));
        final ItemStack bolt = gregtech.api.util.GTOreDictUnificator
            .get(gregtech.api.enums.OrePrefixes.bolt, gregtech.api.enums.Materials.Neutronium, 1L);
        for (int left = bolts; left > 0 && bolt != null && slot < inv.getSizeInventory(); slot++) {
            final int n = Math.min(64, left);
            inv.setInventorySlotContents(slot, gregtech.api.util.GTUtility.copyAmount(n, bolt));
            left -= n;
        }
        return "items: " + seeds
            + " Seed(s), "
            + collapsers
            + " Collapser(s), "
            + bolts
            + " Neutronium bolt(s) in the first bolt bus";
    }

    /**
     * {@code /btskerr power x y z on|off}: the KEE's creative energy hatches give their power again, or none (amperage
     * 0,
     * the stored EU emptied): containment power present or missing (QA-A04).
     */
    public static String power(MTEKerrEngine kee, boolean on) {
        int n = 0;
        for (gregtech.api.metatileentity.implementations.MTEHatch h : kee.getExoticAndNormalEnergyHatchList()) {
            if (!(h instanceof MTEHatchEnergyDebug) || !h.isValid()) continue;
            final MTEHatchEnergyDebug d = (MTEHatchEnergyDebug) h;
            d.setAmperage(on ? 2 : 0);
            if (!on) d.getBaseMetaTileEntity()
                .decreaseStoredEnergyUnits(
                    d.getBaseMetaTileEntity()
                        .getStoredEU(),
                    true);
            n++;
        }
        return "power: " + n + " creative energy hatch(es) " + (on ? "on" : "off");
    }

    // ============================================================================ chunk tickets

    /** Test-bench chunk tickets (never saved for reuse: after a restart Forge drops them unclaimed). */
    private static final List<ForgeChunkManager.Ticket> TICKETS = new ArrayList<>();
    private static boolean callbackSet;

    /**
     * Force-loads every chunk of {@code box} (a whole Kerr) with test-bench tickets, so a console test
     * with no player online keeps the KEE, its cage, both modules and the render cell loaded between
     * commands. Returns the number of chunks forced.
     */
    public static int keep(World world, int[] box) {
        final ModContainer mc = Loader.instance()
            .getIndexedModList()
            .get(BeyondTheStars.MODID);
        if (mc == null) return 0;
        final Object mod = mc.getMod();
        if (!callbackSet) {
            ForgeChunkManager.setForcedChunkLoadingCallback(
                mod,
                (tickets, w) -> { for (ForgeChunkManager.Ticket t : tickets) ForgeChunkManager.releaseTicket(t); });
            callbackSet = true;
        }
        ForgeChunkManager.Ticket ticket = null;
        int n = 0;
        for (int cx = box[0] >> 4; cx <= box[3] >> 4; cx++) {
            for (int cz = box[2] >> 4; cz <= box[5] >> 4; cz++) {
                if (ticket == null || ticket.getChunkList()
                    .size() >= ticket.getMaxChunkListDepth()) {
                    ticket = ForgeChunkManager.requestTicket(mod, world, ForgeChunkManager.Type.NORMAL);
                    if (ticket == null) return n;
                    TICKETS.add(ticket);
                }
                ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(cx, cz));
                n++;
            }
        }
        return n;
    }

    /** Releases every test-bench ticket. */
    public static int release() {
        final int n = TICKETS.size();
        for (ForgeChunkManager.Ticket t : TICKETS) ForgeChunkManager.releaseTicket(t);
        TICKETS.clear();
        return n;
    }
}
