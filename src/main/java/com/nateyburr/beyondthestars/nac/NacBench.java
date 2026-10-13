package com.nateyburr.beyondthestars.nac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.multi.nanochip.MTENanochipAssemblyComplex;
import gregtech.common.tileentities.machines.multi.nanochip.MTENanochipAssemblyModuleBase;
import gregtech.common.tileentities.machines.multi.nanochip.hatches.MTEHatchVacuumConveyorInput;
import gregtech.common.tileentities.machines.multi.nanochip.hatches.MTEHatchVacuumConveyorOutput;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponentPacket;

/**
 * CIRCUITS v2 §7.2 C5 test bench (op tooling, no gameplay change): a REAL Nanochip Assembly Complex with one Assembly
 * Matrix module, built in the world, so a NAC recipe is run by GT's own module code. Used by the degrade test ("a stock
 * NAC still forms and runs a Piko") and to run our SL-N12..N23 circuits.
 * <ul>
 * <li>{@code /btsrecipes nac build x y z [facing]}: places the complex controller and runs the projector's survival
 * path (casings endless; one Vacuum Conveyor Input hatch, one MAX input bus, one MAX output bus and one debug energy
 * hatch at MAX x 64 A for its hatch cells), then an Assembly Matrix module in the complex's module slot nearest the
 * front-left corner (shape cell a 23, b 46, c 5; same alignment as the complex; matrix casings at channel 12 = the UMV
 * Component Assembly Line casing, enough for every stock and SL-N matrix recipe), and swaps three of the module's
 * hatch cells for a Vacuum Conveyor Input and Output (both dyed white) and MAX input hatches for the fluids.</li>
 * <li>{@code /btsrecipes nac run <piko|SL-Nxx> x y z}: puts one run's inputs of that matrix recipe (the lookup
 * recipe, not the NEI page) into the module's conveyor input as circuit components, and its fluids into the input
 * hatches, then switches both machines on.</li>
 * <li>{@code /btsrecipes nac status x y z}: complex and module state, the module's progress and last result, and the
 * module's conveyor contents (the finished circuit lands in its output conveyor).</li>
 * </ul>
 */
public final class NacBench {

    private NacBench() {}

    public static final String USAGE = "/btsrecipes nac <build|run <piko|SL-N12..SL-N23>|status> x y z [facing]";

    /** The complex's controller cell in its shape (AssemblyComplexStructureString MAIN_OFFSET_*). */
    private static final int CA = 31, CB = 40, CC = 30;
    /** The module slot used: a complex 'F' cell on the front-left edge. */
    private static final int SLOT_A = 23, SLOT_B = 46, SLOT_C = 5;
    /** Module hatch cells (module base 'V' cells, which are complex 'I' cells), relative to the module controller. */
    private static final int[][] VC_IN = { { -2, 0, 0 } }, VC_OUT = { { -1, 0, 0 } };
    private static final int[][] FLUID_HATCHES = { { 1, 0, 0 }, { 2, 0, 0 }, { -2, 2, 0 }, { -1, 2, 0 }, { 0, 2, 0 },
        { 1, 2, 0 }, { 2, 2, 0 } };
    /** Matrix casing tier channel: 12 = UMV Component Assembly Line casing (meta 11). */
    private static final int MATRIX_CHANNEL = 12;
    private static final byte WHITE = 0;

    public static List<String> run(World world, SLConsoleTest.Placement at, String[] args) {
        final List<String> out = new ArrayList<>();
        String sub = "status";
        String arg = null;
        for (int i = 1; i < args.length; i++) {
            if (at.used[i]) continue;
            sub = args[i].toLowerCase(Locale.ROOT);
            if (i + 1 < args.length && !at.used[i + 1]) arg = args[i + 1];
            break;
        }
        final int[] box = { at.x - 40, at.y - 16, at.z - 40, at.x + 40, at.y + 48, at.z + 40 };
        SLConsoleTest.loadChunks(world, box);
        com.nateyburr.beyondthestars.machines.kerr.KerrTestBench.keep(world, box);
        switch (sub) {
            case "build":
                build(world, at, out);
                break;
            case "run":
                load(world, at, arg, out);
                break;
            default:
                break;
        }
        status(world, at, out);
        return out;
    }

    // ===================================================================================================== build

    private static void build(World world, SLConsoleTest.Placement at, List<String> out) {
        final ItemStack complexItem = ItemList.Machine_Multi_NanochipAssemblyComplex.get(1);
        final ItemStack moduleItem = ItemList.NanoChipModule_AssemblyMatrix.get(1);
        if (complexItem == null || moduleItem == null) {
            out.add("build: GT has no Nanochip Assembly Complex / Assembly Matrix item");
            return;
        }
        if (!MTEReactorCoreMk1
            .placeGtMachine(world, at.x, at.y, at.z, (short) complexItem.getItemDamage(), at.facing)) {
            out.add("build: could not place the complex controller");
            return;
        }
        final MTENanochipAssemblyComplex complex = complexAt(world, at);
        if (complex == null) {
            out.add("build: no complex tile after placing");
            return;
        }
        final ItemStack trigger = complexItem.copy();
        ChannelDataAccessor.setChannelData(trigger, "gt_hatch", 1);
        final HatchSource source = new HatchSource(
            new ItemList[] { ItemList.Hatch_VacuumConveyor_Input, ItemList.Hatch_Input_Bus_MAX,
                ItemList.Hatch_Output_Bus_MAX, ItemList.DebugEnergyHatch });
        final int[] complexRun = survival(complex, world, trigger, source);
        // the energy cells want an exotic hatch, which the projector does not hand a debug hatch for: put the debug
        // hatch in the first free hatch cell of the controller's back face (shape row c 28, a 28..34, b 44..48)
        if (complex.mEnergyHatches.isEmpty() && complex.getExoticEnergyHatches()
            .isEmpty()) {
            final ExtendedFacing cf = complex.getExtendedFacing();
            outer: for (int b = 44; b <= 48; b++) {
                for (int a = 28; a <= 34; a++) {
                    final int[] p = SLConsoleTest.worldPos(cf, at.x, at.y, at.z, new int[] { a - CA, b - CB, 28 - CC });
                    if (world.getTileEntity(p[0], p[1], p[2]) instanceof IGregTechTileEntity) continue;
                    final ItemStack debug = ItemList.DebugEnergyHatch.get(1);
                    if (debug != null && MTEReactorCoreMk1
                        .placeGtMachine(world, p[0], p[1], p[2], (short) debug.getItemDamage(), cf.getDirection())) {
                        source.given.merge("DebugEnergyHatch (placed at " + pos(p) + ")", 1, Integer::sum);
                    }
                    break outer;
                }
            }
            complex.checkStructure(true, complex.getBaseMetaTileEntity());
        }
        for (MTEHatch h : complex.mEnergyHatches) setDebug(h);
        for (MTEHatch h : complex.getExoticEnergyHatches()) setDebug(h);
        out.add(
            "build: complex survival path " + (complexRun[1] == 1 ? "finished" : "stalled")
                + " after "
                + complexRun[0]
                + " round(s); hatches handed out: "
                + source.given);

        // the module: same alignment as the complex, its controller in the slot cell
        final ExtendedFacing facing = complex.getExtendedFacing();
        final int[] slot = SLConsoleTest
            .worldPos(facing, at.x, at.y, at.z, new int[] { SLOT_A - CA, SLOT_B - CB, SLOT_C - CC });
        if (!MTEReactorCoreMk1.placeGtMachine(
            world,
            slot[0],
            slot[1],
            slot[2],
            (short) moduleItem.getItemDamage(),
            facing.getDirection())) {
            out.add("build: could not place the Assembly Matrix controller at " + pos(slot));
            return;
        }
        final MTENanochipAssemblyModuleBase<?> module = moduleAt(world, slot);
        if (module == null) {
            out.add("build: no Assembly Matrix tile at " + pos(slot));
            return;
        }
        final ItemStack moduleTrigger = moduleItem.copy();
        ChannelDataAccessor.setChannelData(moduleTrigger, "component_casing", MATRIX_CHANNEL);
        final int[] moduleRun = survival(module, world, moduleTrigger, new HatchSource(new ItemList[0]));
        out.add(
            "build: Assembly Matrix at " + pos(slot)
                + " ("
                + facing.getDirection()
                + "), survival path "
                + (moduleRun[1] == 1 ? "finished" : "stalled")
                + " after "
                + moduleRun[0]
                + " round(s)");
        // its sockets: conveyor in and out (white), input hatches for the fluids
        final ExtendedFacing moduleFacing = module.getExtendedFacing();
        int placed = 0;
        placed += socket(world, moduleFacing, slot, VC_IN[0], ItemList.Hatch_VacuumConveyor_Input, true);
        placed += socket(world, moduleFacing, slot, VC_OUT[0], ItemList.Hatch_VacuumConveyor_Output, true);
        for (int[] cell : FLUID_HATCHES)
            placed += socket(world, moduleFacing, slot, cell, ItemList.Hatch_Input_MAX, false);
        out.add("build: " + placed + " module socket(s) placed");

        final IGregTechTileEntity complexBase = complex.getBaseMetaTileEntity();
        complex.fixAllIssues();
        complexBase.enableWorking();
        complex.checkStructure(true, complexBase);
        module.fixAllIssues();
        module.getBaseMetaTileEntity()
            .enableWorking();
        module.checkStructure(true, module.getBaseMetaTileEntity());
    }

    private static int socket(World world, ExtendedFacing facing, int[] origin, int[] rel, ItemList hatch,
        boolean dye) {
        final int[] p = SLConsoleTest.worldPos(facing, origin[0], origin[1], origin[2], rel);
        final ItemStack item = hatch.get(1);
        if (item == null) return 0;
        if (!MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) item.getItemDamage(), facing.getDirection())) {
            return 0;
        }
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (dye && te instanceof IGregTechTileEntity) ((IGregTechTileEntity) te).setColorization(WHITE);
        return 1;
    }

    private static void setDebug(MTEHatch h) {
        if (h instanceof MTEHatchEnergyDebug) {
            ((MTEHatchEnergyDebug) h).setVoltageTier(14);
            ((MTEHatchEnergyDebug) h).setAmperage(64);
        }
    }

    /** {rounds, finished?1:0}: the projector's survival path, round after round, until done or stalled. */
    private static int[] survival(Object machine, World world, ItemStack trigger, HatchSource source) {
        final EntityPlayer actor = world instanceof WorldServer ? FakePlayerFactory.getMinecraft((WorldServer) world)
            : null;
        final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(source, actor);
        int rounds = 0, idle = 0;
        while (rounds < 400) {
            rounds++;
            final int n = ((ISurvivalConstructable) machine).survivalConstruct(trigger, 4096, env);
            if (n < 0) return new int[] { rounds, 1 };
            idle = n == 0 ? idle + 1 : 0;
            if (idle >= 2) break;
        }
        return new int[] { rounds, 0 };
    }

    // ====================================================================================================== run

    private static void load(World world, SLConsoleTest.Placement at, String which, List<String> out) {
        final MTENanochipAssemblyModuleBase<?> module = moduleOf(world, at);
        if (module == null) {
            out.add("run: no Assembly Matrix in the slot; run 'build' first");
            return;
        }
        final GTRecipe recipe = matrixRecipe(which);
        if (recipe == null) {
            out.add("run: no Assembly Matrix lookup recipe for '" + which + "' (piko or SL-N12..SL-N23)");
            return;
        }
        // components into the conveyor input, one run's worth
        final Map<CircuitComponent, Long> wanted = new LinkedHashMap<>();
        for (ItemStack in : recipe.mInputs) {
            final CircuitComponent cc = in == null ? null : CircuitComponent.tryGetFromFakeStack(in);
            if (cc == null) {
                out.add("run: input " + (in == null ? "null" : in.getDisplayName()) + " is not a circuit component");
                return;
            }
            wanted.merge(cc, (long) in.stackSize, Long::sum);
        }
        MTEHatchVacuumConveyorInput input = null;
        for (MTEHatch h : hatches(world, module, VC_IN)) {
            if (h instanceof MTEHatchVacuumConveyorInput) input = (MTEHatchVacuumConveyorInput) h;
        }
        if (input == null) {
            out.add("run: no Vacuum Conveyor Input on the module");
            return;
        }
        for (Map.Entry<CircuitComponent, Long> e : wanted.entrySet()) {
            input.unifyPacket(new CircuitComponentPacket(e.getKey(), e.getValue(), null));
        }
        // fluids, one per input hatch
        final List<MTEHatch> tanks = hatches(world, module, FLUID_HATCHES);
        int f = 0;
        if (recipe.mFluidInputs != null) {
            for (FluidStack fluid : recipe.mFluidInputs) {
                if (fluid == null) continue;
                if (f >= tanks.size() || !(tanks.get(f) instanceof MTEHatchInput)) {
                    out.add("run: not enough input hatches for the fluids");
                    return;
                }
                final MTEHatchInput tank = (MTEHatchInput) tanks.get(f++);
                final FluidStack fill = fluid.copy();
                fill.amount = Math.max(fluid.amount, tank.getCapacity());
                tank.setFillableStack(fill);
            }
        }
        final MTENanochipAssemblyComplex complex = complexAt(world, at);
        if (complex != null) complex.getBaseMetaTileEntity()
            .enableWorking();
        module.getBaseMetaTileEntity()
            .enableWorking();
        out.add(
            "run: " + which
                + " -> "
                + recipe.mOutputs[0].getDisplayName()
                + " ("
                + recipe.mDuration
                + " t at "
                + recipe.mEUt
                + " EU/t before overclocks): "
                + wanted.size()
                + " component kind(s) into the conveyor, "
                + f
                + " fluid(s)");
    }

    /** The lookup recipe (not the NEI page) of {@code which}: "piko" or an SL-N matrix id. */
    private static GTRecipe matrixRecipe(String which) {
        if (which == null) return null;
        final CircuitComponent output;
        if ("piko".equalsIgnoreCase(which)) {
            output = CircuitComponent.PicoCircuit;
        } else {
            output = com.nateyburr.beyondthestars.BTSRecipesNac.matrixOutput(which.toUpperCase(Locale.ROOT));
        }
        if (output == null) return null;
        for (GTRecipe r : RecipeMaps.nanochipAssemblyMatrixRecipes.getAllRecipes()) {
            if (r == null || r.mFakeRecipe || r.mOutputs == null || r.mOutputs.length == 0) continue;
            if (CircuitComponent.tryGetFromFakeStack(r.mOutputs[0]) == output) return r;
        }
        return null;
    }

    // =================================================================================================== status

    private static void status(World world, SLConsoleTest.Placement at, List<String> out) {
        final MTENanochipAssemblyComplex complex = complexAt(world, at);
        if (complex == null) {
            out.add("No Nanochip Assembly Complex at " + at.x + " " + at.y + " " + at.z);
            return;
        }
        final IGregTechTileEntity cb = complex.getBaseMetaTileEntity();
        if (!complex.mMachine) complex.checkStructure(true, cb);
        out.add(
            "complex: " + (complex.mMachine ? "FORMED" : "NOT FORMED")
                + (cb.isAllowedToWork() ? ", on" : ", off")
                + ", modules "
                + complex.getModules()
                    .size()
                + ", max input "
                + complex.getMaxInputEu()
                + " EU/t");
        if (!complex.mMachine) out.addAll(SLConsoleTest.firstErrors(complex, cb, 3));
        final MTENanochipAssemblyModuleBase<?> module = moduleOf(world, at);
        if (module == null) {
            out.add("module: none in the slot");
            return;
        }
        final IGregTechTileEntity mb = module.getBaseMetaTileEntity();
        out.add(
            "module: " + module.getLocalName()
                + " "
                + (module.mMachine ? "FORMED" : "NOT FORMED")
                + (mb.isAllowedToWork() ? ", on" : ", off")
                + (mb.isActive() ? ", ACTIVE" : ", idle")
                + ", linked "
                + (module.getBaseMulti() == complex)
                + ", progress "
                + module.mProgresstime
                + "/"
                + module.mMaxProgresstime
                + ", EU stored "
                + module.getCurrentEUStored()
                + ", last result "
                + (module.getCheckRecipeResult() == null ? "none"
                    : module.getCheckRecipeResult()
                        .getDisplayString()));
        if (!module.mMachine) out.addAll(SLConsoleTest.firstErrors(module, mb, 3));
        for (MTEHatch h : hatches(world, module, VC_IN)) out.add("module conveyor IN: " + contents(h));
        for (MTEHatch h : hatches(world, module, VC_OUT)) out.add("module conveyor OUT: " + contents(h));
    }

    private static String contents(MTEHatch h) {
        CircuitComponentPacket packet = null;
        if (h instanceof MTEHatchVacuumConveyorInput) packet = ((MTEHatchVacuumConveyorInput) h).contents;
        if (h instanceof MTEHatchVacuumConveyorOutput) packet = ((MTEHatchVacuumConveyorOutput) h).contents;
        if (packet == null || packet.isEmpty()) return "empty";
        final List<String> parts = new ArrayList<>();
        for (CircuitComponent cc : packet.getComponents()
            .keySet()) parts.add(packet.getAmount(cc) + " x " + cc.name());
        return parts.toString();
    }

    // ================================================================================================== helpers

    private static MTENanochipAssemblyComplex complexAt(World world, SLConsoleTest.Placement at) {
        final TileEntity te = world.getTileEntity(at.x, at.y, at.z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final Object mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTENanochipAssemblyComplex ? (MTENanochipAssemblyComplex) mte : null;
    }

    private static MTENanochipAssemblyModuleBase<?> moduleAt(World world, int[] p) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final Object mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTENanochipAssemblyModuleBase ? (MTENanochipAssemblyModuleBase<?>) mte : null;
    }

    private static MTENanochipAssemblyModuleBase<?> moduleOf(World world, SLConsoleTest.Placement at) {
        final MTENanochipAssemblyComplex complex = complexAt(world, at);
        if (complex == null) return null;
        final int[] slot = SLConsoleTest.worldPos(
            complex.getExtendedFacing(),
            at.x,
            at.y,
            at.z,
            new int[] { SLOT_A - CA, SLOT_B - CB, SLOT_C - CC });
        return moduleAt(world, slot);
    }

    private static List<MTEHatch> hatches(World world, MTENanochipAssemblyModuleBase<?> module, int[][] cells) {
        final List<MTEHatch> out = new ArrayList<>();
        final IGregTechTileEntity base = module.getBaseMetaTileEntity();
        final int[] origin = { base.getXCoord(), base.getYCoord(), base.getZCoord() };
        for (int[] rel : cells) {
            final int[] p = SLConsoleTest.worldPos(module.getExtendedFacing(), origin[0], origin[1], origin[2], rel);
            final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
            if (te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEHatch) {
                out.add((MTEHatch) ((IGregTechTileEntity) te).getMetaTileEntity());
            }
        }
        return out;
    }

    private static String pos(int[] p) {
        return "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }

    /**
     * Endless for any exact stack (casings, glass, frames), and by predicate (a hatch cell) one of each listed hatch,
     * the least handed out first.
     */
    private static final class HatchSource implements IItemSource {

        private final ItemList[] hatches;
        private final int[] count;
        final Map<String, Integer> given = new LinkedHashMap<>();

        HatchSource(ItemList[] hatches) {
            this.hatches = hatches;
            this.count = new int[hatches.length];
        }

        @Override
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int n) {
            int best = -1;
            for (int i = 0; i < hatches.length; i++) {
                final ItemStack s = hatches[i].get(1L);
                if (s == null || count[i] >= 1 || !predicate.test(s)) continue;
                if (best < 0 || count[i] < count[best]) best = i;
            }
            if (best < 0) return Collections.emptyMap();
            final Map<ItemStack, Integer> m = new LinkedHashMap<>();
            m.put(hatches[best].get(1L), 1);
            return m;
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            if (stack == null || stack.getItem() == null) return false;
            for (int i = 0; i < hatches.length; i++) {
                final ItemStack h = hatches[i].get(1L);
                if (h != null && h.getItem() == stack.getItem() && h.getItemDamage() == stack.getItemDamage()) {
                    if (!simulate) {
                        count[i]++;
                        given.merge(hatches[i].name(), 1, Integer::sum);
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
