package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * Console test bench for the older standalone multiblocks (the production halls, the Yellowcake Mill and the
 * Extraction Platform), which no other bench builds: {@code /btsbuild hall <kind> x y z [facing] [build|check|
 * stock [first|longest|n]|status|tier t]}.
 * <p>
 * {@code build} places the controller and runs the projector's survival path from an endless source that hands out
 * one of each test hatch (auto-maintenance, a creative energy hatch, MAX buses and hatches; two output hatches)
 * wherever the structure
 * takes a hatch, so a hall forms with no hand-placed sockets. Test bench only; no gameplay change.
 */
public final class HallBench {

    private HallBench() {}

    public static final String USAGE = "/btsbuild hall <yellowcake|platform|cascade|fuelfab|purex|batching> x y z "
        + "[facing] [build|check|status|stock [first|longest|n]|tier <t>|circuit <n>|place x y z <outhatch|inhatch|outbus|inbus>"
        + "|fill|empty|voiding <none|all|legacy>|feed]";

    private static ItemStack controller(String kind) {
        switch (kind) {
            case "yellowcake":
            case "mill":
                return BTSMachines.enrichmentTrain;
            case "platform":
                return BTSMachines.extractionPlatform;
            case "cascade":
                return BTSMachines.cascadeHall;
            case "fuelfab":
                return BTSMachines.fuelFabricationHall;
            case "purex":
                return BTSMachines.purexCanyon;
            case "batching":
                return BTSMachines.siteBatchingPlant;
            default:
                return null;
        }
    }

    /** Runs {@code args} = {@code hall kind x y z [facing] [sub ...]}. */
    public static List<String> run(World world, SLConsoleTest.Placement at, String[] args) {
        final List<String> out = new ArrayList<>();
        final String kind = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        final ItemStack ctrl = controller(kind);
        if (ctrl == null) {
            out.add(USAGE);
            return out;
        }
        String sub = "status";
        int subAt = -1;
        for (int i = 2; i < args.length; i++) {
            if (at.used[i]) continue;
            sub = args[i].toLowerCase(Locale.ROOT);
            subAt = i;
            break;
        }
        // forced, not just loaded: a playerless server unloads the hall between two console commands
        final int[] box = { at.x - 24, at.y - 16, at.z - 24, at.x + 24, at.y + 24, at.z + 24 };
        SLConsoleTest.loadChunks(world, box);
        com.nateyburr.beyondthestars.machines.kerr.KerrTestBench.keep(world, box);
        if ("build".equals(sub)) {
            if (!MTEReactorCoreMk1.placeGtMachine(world, at.x, at.y, at.z, (short) ctrl.getItemDamage(), at.facing)) {
                out.add("build: could not place the controller");
                return out;
            }
            final IGregTechTileEntity base = baseAt(world, at.x, at.y, at.z);
            if (base == null) {
                out.add("build: no controller tile after placing");
                return out;
            }
            final MTEMultiBlockBase m = (MTEMultiBlockBase) base.getMetaTileEntity();
            final ItemStack trigger = ctrl.copy();
            trigger.stackSize = 1;
            ChannelDataAccessor.setChannelData(trigger, "gt_hatch", 1);
            final HatchSource source = new HatchSource();
            final EntityPlayer actor = world instanceof WorldServer
                ? FakePlayerFactory.getMinecraft((WorldServer) world)
                : null;
            final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(source, actor);
            int rounds = 0, placed = 0, idle = 0;
            boolean finished = false;
            while (rounds < 400) {
                rounds++;
                final int n = ((ISurvivalConstructable) m).survivalConstruct(trigger, 4096, env);
                if (n < 0) {
                    finished = true;
                    break;
                }
                placed += n;
                idle = n == 0 ? idle + 1 : 0;
                if (idle >= 2) break;
            }
            for (MTEHatch h : m.mEnergyHatches) {
                if (h instanceof MTEHatchEnergyDebug) {
                    ((MTEHatchEnergyDebug) h).setVoltageTier(9);
                    ((MTEHatchEnergyDebug) h).setAmperage(16);
                }
            }
            m.fixAllIssues();
            base.enableWorking();
            m.checkStructure(true, base);
            out.add(
                "build " + kind
                    + ": survival path "
                    + (finished ? "finished" : "stalled")
                    + " after "
                    + rounds
                    + " round(s), "
                    + placed
                    + " block(s) placed; hatches handed out: "
                    + source.given);
        }
        final IGregTechTileEntity base = baseAt(world, at.x, at.y, at.z);
        if (base == null || !(base.getMetaTileEntity() instanceof MTEMultiBlockBase)) {
            out.add("No " + kind + " controller at (" + at.x + ", " + at.y + ", " + at.z + ").");
            return out;
        }
        final MTEMultiBlockBase m = (MTEMultiBlockBase) base.getMetaTileEntity();
        // a chunk that just (re)loaded has not been walked yet: walk it, as GT does on its first tick
        if (!m.mMachine) m.checkStructure(true, base);
        switch (sub) {
            case "check":
                m.checkStructure(true, base);
                break;
            case "tier": {
                final int t = subAt + 1 < args.length ? Integer.parseInt(args[subAt + 1]) : 9;
                for (MTEHatch h : m.mEnergyHatches) {
                    if (h instanceof MTEHatchEnergyDebug) ((MTEHatchEnergyDebug) h).setVoltageTier(t);
                }
                m.checkStructure(true, base);
                break;
            }
            case "circuit": {
                // the programmed circuit in the controller slot, as a player puts it there (GT reads the controller
                // slot as a recipe input; the Cascade Hall has no input bus)
                final int n = subAt + 1 < args.length ? Integer.parseInt(args[subAt + 1]) : 1;
                m.mInventory[m.getControllerSlotIndex()] = gregtech.api.util.GTUtility.getIntegratedCircuit(n);
                out.add("circuit: Programmed Circuit " + n + " in the controller slot");
                break;
            }
            case "place": {
                // place <x> <y> <z> <outhatch|inhatch|outbus|inbus>: swap one cell for a MAX hatch facing the
                // controller's front, as a player adds a second socket, and re-walk
                final int px = Integer.parseInt(args[subAt + 1]), py = Integer.parseInt(args[subAt + 2]),
                    pz = Integer.parseInt(args[subAt + 3]);
                final String what = args[subAt + 4].toLowerCase(Locale.ROOT);
                final ItemList hatch = "outhatch".equals(what) ? ItemList.Hatch_Output_MAX
                    : "inhatch".equals(what) ? ItemList.Hatch_Input_MAX
                        : "outbus".equals(what) ? ItemList.Hatch_Output_Bus_MAX : ItemList.Hatch_Input_Bus_MAX;
                final boolean ok = MTEReactorCoreMk1.placeGtMachine(
                    world,
                    px,
                    py,
                    pz,
                    (short) hatch.get(1L)
                        .getItemDamage(),
                    at.facing);
                m.checkStructure(true, base);
                out.add(
                    "place: " + hatch
                        .name() + " at (" + px + ", " + py + ", " + pz + ") " + (ok ? "placed" : "FAILED"));
                break;
            }
            case "stock":
                out.add(stock(m, subAt + 1 < args.length ? args[subAt + 1].toLowerCase(Locale.ROOT) : "first"));
                break;
            case "fill": {
                // B-230 void protection: every output bus slot holds a full stack of cobblestone, so no output fits
                int slots = 0;
                for (MTEHatch bus : m.mOutputBusses) {
                    final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
                    if (inv == null) continue;
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        inv.setInventorySlotContents(i, new ItemStack(net.minecraft.init.Blocks.cobblestone, 64));
                        slots++;
                    }
                }
                out.add("fill: " + slots + " output bus slot(s) hold 64 Cobblestone");
                break;
            }
            case "empty": {
                for (MTEHatch bus : m.mOutputBusses) {
                    final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
                    if (inv == null) continue;
                    for (int i = 0; i < inv.getSizeInventory(); i++) inv.setInventorySlotContents(i, null);
                }
                out.add("empty: the output buses are empty");
                break;
            }
            case "voiding": {
                // voiding <none|item|fluid|all>: GT's controller button. voiding legacy: load a save from before void
                // protection (voidingMode all, no B-230 marker) through the machine's own loadNBTData
                final String mode = subAt + 1 < args.length ? args[subAt + 1].toLowerCase(Locale.ROOT) : "none";
                if ("legacy".equals(mode)) {
                    final net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
                    m.saveNBTData(nbt);
                    nbt.removeTag(BTSVoidProtection.NBT_KEY);
                    nbt.setString("voidingMode", gregtech.api.enums.VoidingMode.VOID_ALL.name);
                    m.loadNBTData(nbt);
                    out.add("voiding legacy: a save holding VOID_ALL and no marker loads as " + m.getVoidingMode());
                    break;
                }
                m.setVoidingMode(gregtech.api.enums.VoidingMode.fromName(mode));
                out.add("voiding: " + m.getVoidingMode() + (m.supportsVoidProtection() ? "" : " (no void protection)"));
                break;
            }
            case "feed": {
                // the Yellowcake Mill's inputs: 64 Uraninite Dust in the first input bus, Hydrofluoric Acid in the
                // first input hatch
                if (!m.mInputBusses.isEmpty()) {
                    m.mInputBusses.get(0)
                        .getBaseMetaTileEntity()
                        .setInventorySlotContents(
                            0,
                            gregtech.api.util.GTOreDictUnificator
                                .get(gregtech.api.enums.OrePrefixes.dust, gregtech.api.enums.Materials.Uraninite, 64L));
                }
                if (!m.mInputHatches.isEmpty()) {
                    final MTEHatchInput hatch = m.mInputHatches.get(0);
                    final FluidStack acid = gregtech.api.enums.Materials.HydrofluoricAcid.getFluid(1L);
                    acid.amount = hatch.getCapacity();
                    hatch.setFillableStack(acid);
                }
                out.add("feed: 64 Uraninite Dust, a full hatch of Hydrofluoric Acid");
                break;
            }
            default:
                break;
        }
        out.add(status(m, base));
        if (!m.mMachine) out.addAll(SLConsoleTest.firstErrors(m, base, 3));
        return out;
    }

    private static IGregTechTileEntity baseAt(World world, int x, int y, int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IGregTechTileEntity ? (IGregTechTileEntity) te : null;
    }

    /** The localized controller name (GT's lang lookup, the way the scanner and WAILA name it), state and buses. */
    private static String status(MTEMultiBlockBase m, IGregTechTileEntity base) {
        final String key = "gt.blockmachines." + m.mName + ".name";
        return String.format(
            Locale.ROOT,
            "%s [%s = \"%s\"]: %s%s%s, progress %d/%d, max input %d EU/t, energy %d, parallel %s, voiding %s; "
                + "in: %s; fluids in: %s; out: %s; fluids out: %s; last result: %s",
            m.getLocalName(),
            key,
            StatCollector.translateToLocal(key),
            m.mMachine ? "FORMED" : "NOT FORMED",
            base.isActive() ? ", ACTIVE" : ", idle",
            base.isAllowedToWork() ? "" : " (switched off)",
            m.mProgresstime,
            m.mMaxProgresstime,
            m.getMaxInputEu(),
            base.getStoredEU(),
            m.getMaxParallelRecipes(),
            m.getVoidingMode(),
            buses(m.mInputBusses),
            tanks(m.mInputHatches),
            buses(m.mOutputBusses),
            tanks(m.mOutputHatches),
            m.getCheckRecipeResult() == null ? "none"
                : m.getCheckRecipeResult()
                    .getDisplayString());
    }

    private static String stock(MTEMultiBlockBase m, String pick) {
        final RecipeMap<?> map = m.getRecipeMap();
        if (map == null) return "stock: no recipe map";
        final long maxEu = m.getMaxInputEu();
        final List<GTRecipe> candidates = new ArrayList<>();
        for (GTRecipe r : map.getAllRecipes()) {
            if (!r.mEnabled || r.mFakeRecipe || r.mEUt > maxEu) continue;
            boolean ok = true;
            if (r.mInputs != null) {
                for (ItemStack in : r.mInputs) if (in == null || in.getItem() == null) ok = false;
            }
            if (r.mFluidInputs != null) {
                for (FluidStack f : r.mFluidInputs) if (f == null) ok = false;
            }
            if (ok) candidates.add(r);
        }
        if (candidates.isEmpty()) return "stock: no usable recipe in " + map.unlocalizedName + " at " + maxEu + " EU/t";
        final GTRecipe recipe;
        if ("longest".equals(pick)) {
            candidates.sort(
                Comparator.comparingLong((GTRecipe r) -> (long) r.mDuration)
                    .reversed());
            recipe = candidates.get(0);
        } else if ("first".equals(pick)) {
            recipe = candidates.get(0);
        } else {
            recipe = candidates.get(Math.min(candidates.size() - 1, Math.max(0, Integer.parseInt(pick))));
        }
        int items = 0;
        if (recipe.mInputs != null && !m.mInputBusses.isEmpty()) {
            final MTEHatchInputBus bus = m.mInputBusses.get(0);
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            int slot = 0;
            for (ItemStack in : recipe.mInputs) {
                final ItemStack s = in.copy();
                if (s.getItemDamage() == Short.MAX_VALUE) s.setItemDamage(0);
                s.stackSize = Math.max(s.stackSize, Math.min(64, s.getMaxStackSize()));
                while (slot < inv.getSizeInventory() && inv.getStackInSlot(slot) != null) slot++;
                if (slot >= inv.getSizeInventory()) break;
                inv.setInventorySlotContents(slot, s);
                items += s.stackSize;
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
        return String.format(
            Locale.ROOT,
            "stock: %s (%d candidates) %s -> %s, %d ticks at %d EU/t; placed %d items, %d fluid(s)",
            map.unlocalizedName,
            candidates.size(),
            names(recipe.mInputs, recipe.mFluidInputs),
            names(recipe.mOutputs, recipe.mFluidOutputs),
            recipe.mDuration,
            recipe.mEUt,
            items,
            fluids);
    }

    private static String names(ItemStack[] items, FluidStack[] fluids) {
        final List<String> parts = new ArrayList<>();
        if (items != null) for (ItemStack s : items) {
            if (s != null && s.getItem() != null) parts.add(s.stackSize + "x " + s.getDisplayName());
        }
        if (fluids != null) for (FluidStack f : fluids) {
            if (f != null) parts.add(f.amount + "L " + f.getLocalizedName());
        }
        return parts.toString();
    }

    private static String buses(List<? extends MTEHatch> buses) {
        final Map<String, Integer> totals = new LinkedHashMap<>();
        for (MTEHatch bus : buses) {
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            if (inv == null) continue;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                final ItemStack s = inv.getStackInSlot(i);
                if (s != null && s.getItem() != null) totals.merge(s.getDisplayName(), s.stackSize, Integer::sum);
            }
        }
        return buses.size() + " bus(es) " + (totals.isEmpty() ? "empty" : totals.toString());
    }

    private static String tanks(List<? extends MTEHatch> hatches) {
        final List<String> parts = new ArrayList<>();
        for (MTEHatch h : hatches) {
            if (!(h instanceof MTEBasicTank)) continue;
            final FluidStack f = ((MTEBasicTank) h).getFluid();
            if (f != null && f.amount > 0) parts.add(f.amount + "L " + f.getLocalizedName());
        }
        return hatches.size() + " hatch(es) " + (parts.isEmpty() ? "empty" : parts.toString());
    }

    /**
     * Endless for any exact stack (casings, glass), and by predicate (a hatch cell) one of each test hatch per type,
     * the type handed out least so far first, so a shared hatch element gets every type once.
     */
    private static final class HatchSource implements IItemSource {

        private static final ItemList[] HATCHES = { ItemList.Hatch_AutoMaintenance, ItemList.DebugEnergyHatch,
            ItemList.Hatch_Input_Bus_MAX, ItemList.Hatch_Input_MAX, ItemList.Hatch_Output_Bus_MAX,
            ItemList.Hatch_Output_MAX, ItemList.Hatch_Muffler_MAX };
        /** How many of each a hall gets: two output hatches, so a two-fluid recipe (the Cascade's cut + tails) fits. */
        private static final int[] LIMIT = { 1, 1, 1, 1, 1, 2, 1 };
        private final int[] count = new int[HATCHES.length];
        final Map<String, Integer> given = new LinkedHashMap<>();

        @Override
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int n) {
            int best = -1;
            for (int i = 0; i < HATCHES.length; i++) {
                final ItemStack s = HATCHES[i].get(1L);
                if (s == null || count[i] >= LIMIT[i] || !predicate.test(s)) continue;
                if (best < 0 || count[i] < count[best]) best = i;
            }
            if (best < 0) return Collections.emptyMap();
            final Map<ItemStack, Integer> m = new LinkedHashMap<>();
            m.put(HATCHES[best].get(1L), 1);
            return m;
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            if (stack == null || stack.getItem() == null) return false;
            for (int i = 0; i < HATCHES.length; i++) {
                final ItemStack h = HATCHES[i].get(1L);
                if (h != null && h.getItem() == stack.getItem() && h.getItemDamage() == stack.getItemDamage()) {
                    if (!simulate) {
                        count[i]++;
                        given.merge(HATCHES[i].name(), 1, Integer::sum);
                    }
                    return true;
                }
            }
            return true;
        }

        @Override
        public boolean takeAll(ItemStack stack, boolean simulate) {
            return stack != null && stack.getItem() != null;
        }
    }
}
