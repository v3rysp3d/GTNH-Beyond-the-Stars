package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.items.SLCircuitItems;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import tectech.thing.CustomItemList;

/**
 * CIRCUITS v2 §7.2 C7 test bench (op tooling, no gameplay change): the circuit chain SL-X01..X32 run end to end in
 * REAL stock GT machines, so one Mainframe of each family is made through GT's own processing code.
 * <ul>
 * <li>{@code /btsrecipes chain build x y z}: lays out the machines (facing north): an Electric Blast Furnace (Hypogen
 * coils) and a Hyper-Intensity Laser Engraver (UMV laser source, two input hatches) built through the projector's
 * survival path, the single-block UMV Cutting Machine, Assembling Machine and Circuit Assembler, a 16-slice Assembly
 * Line, and a chest for the finished Mainframes. Multiblocks get a debug energy hatch (MAX, 64 A); the single blocks
 * are kept charged by the bench every tick. The two-fluid engravings (SL-X05 / X06) need the multiblock engraver: a
 * single-block engraver has one fluid slot.</li>
 * <li>{@code /btsrecipes chain plan <sidereal|empyrean|omega> x y z}: the steps a Mainframe needs from an empty store,
 * by machine.</li>
 * <li>{@code /btsrecipes chain make <sidereal|empyrean|omega> x y z}: runs those steps one at a time, one recipe run
 * per step. Our own items (the 28 Circuits v2 items) only ever come from the bench's store, filled by the machines'
 * outputs; every other input (stock parts, Eye fluids, lenses, water) is supplied by the bench ("creative", counted).
 * The finished Mainframe goes into the chest, out of the store, so the next {@code make} builds its own.</li>
 * <li>{@code /btsrecipes chain gt <umv|uxv|max> x y z}: a STOCK Assembler recipe with a {@code circuitExotic} /
 * {@code circuitCosmic} / {@code circuitTranscendent} input run in the Assembling Machine with our Mainframe(s) in that
 * slot (from the store, else supplied).</li>
 * <li>{@code /btsrecipes chain status|stop x y z}.</li>
 * </ul>
 * Data sticks for the Assembly Line are written with GT's own {@link AssemblyLineUtils} (what TecTech's Research
 * Station writes); the Research Station itself is not run here. Single blocks need GT's dev cleanroom bypass
 * ({@code /gt cleanroom_bypass on cleanroom}), which {@code build} reports.
 */
public final class CircuitChainBench {

    private CircuitChainBench() {}

    public static final String USAGE = "/btsrecipes chain <build | plan <family> | make <family> | gt <umv|uxv|max>"
        + " | status | stop> x y z";

    // ======================================================================================================= layout

    /** Offsets from the bench origin (all controllers face north; structures extend south / sideways). */
    private static final int[] EBF = { 0, 0, 0 }, HILE = { 8, 0, 0 }, CUTTER = { 14, 0, 0 }, ASSEMBLER = { 16, 0, 0 },
        CIRCUIT_ASSEMBLER = { 18, 0, 0 }, CHEST = { 20, 0, 0 }, ASSEMBLY_LINE = { 30, 2, 14 };

    private enum Machine {

        EBF("Electric Blast Furnace"),
        ENGRAVER("Hyper-Intensity Laser Engraver"),
        CUTTER("Cutting Machine UMV"),
        ASSEMBLER("Assembling Machine UMV"),
        CIRCUIT_ASSEMBLER("Circuit Assembler UMV"),
        ASSEMBLY_LINE("Assembly Line");

        final String label;

        Machine(String label) {
            this.label = label;
        }
    }

    private static int[] at(int[] origin, int[] off) {
        return new int[] { origin[0] + off[0], origin[1] + off[1], origin[2] + off[2] };
    }

    private static int[] posOf(int[] origin, Machine m) {
        switch (m) {
            case EBF:
                return at(origin, EBF);
            case ENGRAVER:
                return at(origin, HILE);
            case CUTTER:
                return at(origin, CUTTER);
            case ASSEMBLER:
                return at(origin, ASSEMBLER);
            case CIRCUIT_ASSEMBLER:
                return at(origin, CIRCUIT_ASSEMBLER);
            default:
                return at(origin, ASSEMBLY_LINE);
        }
    }

    // ===================================================================================================== command

    public static List<String> run(World world, SLConsoleTest.Placement at, String[] args) {
        final List<String> out = new ArrayList<>();
        final List<String> words = new ArrayList<>();
        for (int i = 1; i < args.length; i++) if (!at.used[i]) words.add(args[i].toLowerCase(Locale.ROOT));
        final String sub = words.isEmpty() ? "status" : words.get(0);
        final String arg = words.size() > 1 ? words.get(1) : null;
        final int[] origin = { at.x, at.y, at.z };
        keepLoaded(world, origin);
        switch (sub) {
            case "build":
                build(world, origin, out);
                break;
            case "plan": {
                final Item target = mainframe(arg);
                if (target == null) {
                    out.add("plan: family must be sidereal, empyrean or omega");
                    break;
                }
                final List<Step> steps = plan(target, new LinkedHashMap<>(), out);
                if (steps != null) describePlan(target, steps, out);
                break;
            }
            case "make": {
                final Item target = mainframe(arg);
                if (target == null) {
                    out.add("make: family must be sidereal, empyrean or omega");
                    break;
                }
                start(world, origin, target, out);
                break;
            }
            case "gt":
                gtRecipe(world, origin, arg, out);
                break;
            case "stop":
                if (job != null) {
                    out.add("stopped: " + job.describe());
                    job = null;
                } else out.add("nothing running");
                break;
            default:
                break;
        }
        status(world, origin, out);
        return out;
    }

    private static Item mainframe(String family) {
        if (family == null) return null;
        switch (family) {
            case "sidereal":
                return SLCircuitItems.siderealMainframe;
            case "empyrean":
                return SLCircuitItems.empyreanMainframe;
            case "omega":
                return SLCircuitItems.omegaMainframe;
            default:
                return null;
        }
    }

    private static void keepLoaded(World world, int[] o) {
        final int[] box = { o[0] - 8, o[1] - 6, o[2] - 8, o[0] + 52, o[1] + 12, o[2] + 24 };
        SLConsoleTest.loadChunks(world, box);
        com.nateyburr.beyondthestars.machines.kerr.KerrTestBench.keep(world, box);
    }

    // ======================================================================================================= build

    private static void build(World world, int[] o, List<String> out) {
        // the Electric Blast Furnace: Hypogen coils (12,601 K) for SL-X01's 12,600 K
        final ItemStack ebf = ItemList.Machine_Multi_BlastFurnace.get(1);
        final ItemStack ebfTrigger = ebf.copy();
        ChannelDataAccessor.setChannelData(ebfTrigger, "gt_hatch", 1);
        ChannelDataAccessor.setChannelData(ebfTrigger, "coil", 13);
        out.add(
            multi(
                world,
                at(o, EBF),
                ebf,
                ebfTrigger,
                new Source(
                    ItemList.Hatch_Input_Bus_MAX.get(1),
                    1,
                    ItemList.Hatch_Input_MAX.get(1),
                    1,
                    ItemList.Hatch_Output_Bus_MAX.get(1),
                    1,
                    ItemList.Hatch_Output_MAX.get(1),
                    1,
                    ItemList.Hatch_Maintenance.get(1),
                    1,
                    ItemList.DebugEnergyHatch.get(1),
                    1,
                    ItemList.Hatch_Muffler_MAX.get(1),
                    1)));
        // the Hyper-Intensity Laser Engraver: UMV laser source (256 A), glass at least UMV, two input hatches
        final ItemStack hile = ItemList.Machine_Multi_IndustrialLaserEngraver.get(1);
        final ItemStack hileTrigger = hile.copy();
        ChannelDataAccessor.setChannelData(hileTrigger, "gt_hatch", 1);
        ChannelDataAccessor.setChannelData(hileTrigger, "glass", 14);
        out.add(
            multi(
                world,
                at(o, HILE),
                hile,
                hileTrigger,
                new Source(
                    CustomItemList.eM_dynamoTunnel1_UMV.get(1),
                    1,
                    ItemList.Hatch_Input_Bus_MAX.get(1),
                    1,
                    ItemList.Hatch_Input_MAX.get(1),
                    2,
                    ItemList.Hatch_Output_Bus_MAX.get(1),
                    1,
                    ItemList.Hatch_Maintenance.get(1),
                    1,
                    ItemList.DebugEnergyHatch.get(1),
                    1)));
        out.add(fixEngraver(world, at(o, HILE)));
        // the single blocks
        out.add(single(world, at(o, CUTTER), ItemList.CuttingMachineUMV.get(1)));
        out.add(single(world, at(o, ASSEMBLER), ItemList.AssemblingMachineUMV.get(1)));
        out.add(single(world, at(o, CIRCUIT_ASSEMBLER), ItemList.CircuitAssemblerUMV.get(1)));
        // the Assembly Line: 16 slices, an input bus under each, four input hatches
        final ItemStack line = ItemList.Machine_Multi_Assemblyline.get(1);
        final ItemStack lineTrigger = line.copy();
        lineTrigger.stackSize = 16;
        ChannelDataAccessor.setChannelData(lineTrigger, "gt_hatch", 1);
        ChannelDataAccessor.setChannelData(lineTrigger, "length", 16);
        out.add(
            multi(
                world,
                at(o, ASSEMBLY_LINE),
                line,
                lineTrigger,
                new Source(
                    ItemList.Hatch_Input_Bus_MAX.get(1),
                    16,
                    ItemList.Hatch_Input_MAX.get(1),
                    4,
                    ItemList.Hatch_Output_Bus_MAX.get(1),
                    1,
                    ItemList.Hatch_Maintenance.get(1),
                    1,
                    ItemList.DebugEnergyHatch.get(1),
                    1)));
        out.add(fixLine(world, at(o, ASSEMBLY_LINE)));
        // the chest for the finished Mainframes
        final int[] c = at(o, CHEST);
        world.setBlock(c[0], c[1], c[2], Blocks.chest, 2, 3);
        out.add("build: chest for the finished Mainframes at " + pos(c));
        out.add(
            "build: single blocks need GT's dev cleanroom bypass for the cleanroom recipes: "
                + (gregtech.api.util.FakeCleanroom.isCleanroomBypassEnabled() ? "ON"
                    : "OFF (run /gt cleanroom_bypass on cleanroom)"));
        ensureTicker();
    }

    private static String single(World world, int[] p, ItemStack item) {
        if (item == null) return "build: GT has no such machine item";
        final boolean ok = MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) item.getItemDamage(), ForgeDirection.NORTH);
        final MTEBasicMachine m = basicAt(world, p);
        return "build: " + item.getDisplayName() + " at " + pos(p) + (ok && m != null ? " placed" : " FAILED");
    }

    private static String multi(World world, int[] p, ItemStack controller, ItemStack trigger, Source source) {
        if (!MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) controller.getItemDamage(), ForgeDirection.NORTH)) {
            return "build: could not place " + controller.getDisplayName() + " at " + pos(p);
        }
        final MTEMultiBlockBase m = multiAt(world, p);
        if (!(m instanceof ISurvivalConstructable)) return "build: no multiblock tile at " + pos(p);
        final EntityPlayer actor = world instanceof WorldServer ? FakePlayerFactory.getMinecraft((WorldServer) world)
            : null;
        final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(source, actor);
        int rounds = 0, idle = 0;
        boolean finished = false;
        while (rounds < 400) {
            rounds++;
            final int n = ((ISurvivalConstructable) m).survivalConstruct(trigger, 4096, env);
            if (n < 0) {
                finished = true;
                break;
            }
            idle = n == 0 ? idle + 1 : 0;
            if (idle >= 2) break;
        }
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        m.checkStructure(true, base);
        for (MTEHatchEnergy h : m.mEnergyHatches) setDebug(h);
        m.fixAllIssues();
        base.enableWorking();
        m.checkStructure(true, base);
        final StringBuilder sb = new StringBuilder("build: ").append(controller.getDisplayName())
            .append(" at ")
            .append(pos(p))
            .append(", survival path ")
            .append(finished ? "finished" : "stalled")
            .append(" after ")
            .append(rounds)
            .append(" round(s), ")
            .append(m.mMachine ? "FORMED" : "NOT FORMED")
            .append("; hatches ")
            .append(source.given);
        if (!m.mMachine) sb.append(" ")
            .append(SLConsoleTest.firstErrors(m, base, 3));
        return sb.toString();
    }

    /**
     * The projector's survival path stops short of what the engraver needs here: it places the lowest glass (the HILE
     * wants glass at least as high as the laser source, UMV) and one input hatch (SL-X05 / X06 take two fluids). So:
     * every glass block becomes the highest-tier glass GT knows, and one casing becomes a second input hatch.
     */
    private static String fixEngraver(World world, int[] p) {
        final MTEMultiBlockBase m = multiAt(world, p);
        if (m == null) return "fix: no engraver";
        net.minecraft.block.Block best = null;
        int bestMeta = 0, bestTier = -1;
        for (org.apache.commons.lang3.tuple.Pair<net.minecraft.block.Block, Integer> g : gregtech.api.util.GlassTier
            .getGlassList()) {
            final Integer tier = gregtech.api.util.GlassTier.getGlassBlockTier(g.getLeft(), g.getRight());
            if (tier != null && tier > bestTier) {
                bestTier = tier;
                best = g.getLeft();
                bestMeta = g.getRight();
            }
        }
        int glass = 0;
        for (int x = p[0] - 2; x <= p[0] + 2; x++)
            for (int y = p[1]; y <= p[1] + 4; y++) for (int z = p[2]; z <= p[2] + 4; z++) {
                final net.minecraft.block.Block b = world.getBlock(x, y, z);
                if (best != null
                    && gregtech.api.util.GlassTier.getGlassBlockTier(b, world.getBlockMetadata(x, y, z)) != null) {
                    world.setBlock(x, y, z, best, bestMeta, 3);
                    glass++;
                }
            }
        m.checkStructure(true, m.getBaseMetaTileEntity());
        final String hatch = addHatch(
            world,
            m,
            new int[] { p[0] - 2, p[1], p[2] + 1, p[0] + 2, p[1], p[2] + 4 },
            gregtech.api.GregTechAPI.sBlockCasings10,
            1,
            ItemList.Hatch_Input_MAX.get(1),
            mm -> mm.mInputHatches.size() >= 2);
        for (MTEHatchEnergy h : m.mEnergyHatches) setDebug(h);
        m.fixAllIssues();
        return "fix: engraver glass " + glass
            + " block(s) -> "
            + new ItemStack(best, 1, bestMeta).getDisplayName()
            + " (tier "
            + bestTier
            + "); second input hatch "
            + hatch
            + "; "
            + (m.mMachine ? "FORMED" : "NOT FORMED " + SLConsoleTest.firstErrors(m, m.getBaseMetaTileEntity(), 3));
    }

    /**
     * The Assembly Line's last slice overlaps the 15th (GT builds both at the same offset), so the projector's casing
     * stands where the last slice's output bus goes: one bottom-front casing becomes the output bus.
     */
    private static String fixLine(World world, int[] p) {
        final MTEMultiBlockBase m = multiAt(world, p);
        if (m == null) return "fix: no assembly line";
        if (m.mMachine) return "fix: assembly line already formed";
        final String bus = addHatch(
            world,
            m,
            new int[] { p[0] - 16, p[1] - 2, p[2] - 1, p[0] + 16, p[1] - 2, p[2] + 2 },
            gregtech.api.GregTechAPI.sBlockCasings2,
            0,
            ItemList.Hatch_Output_Bus_MAX.get(1),
            mm -> mm.mMachine);
        for (MTEHatchEnergy h : m.mEnergyHatches) setDebug(h);
        m.fixAllIssues();
        m.checkStructure(true, m.getBaseMetaTileEntity());
        return "fix: assembly line output bus " + bus
            + "; "
            + (m.mMachine ? "FORMED" : "NOT FORMED " + SLConsoleTest.firstErrors(m, m.getBaseMetaTileEntity(), 3))
            + ", "
            + m.mInputBusses.size()
            + " input buses, "
            + m.mInputHatches.size()
            + " input hatches";
    }

    /** Tries each casing cell in the box: a hatch there, kept if {@code ok} holds after a re-check, else put back. */
    private static String addHatch(World world, MTEMultiBlockBase m, int[] box, net.minecraft.block.Block casing,
        int meta, ItemStack hatch, Predicate<MTEMultiBlockBase> ok) {
        if (ok.test(m)) return "not needed";
        int tries = 0;
        for (int y = box[1]; y <= box[4]; y++)
            for (int x = box[0]; x <= box[3]; x++) for (int z = box[2]; z <= box[5]; z++) {
                if (world.getBlock(x, y, z) != casing || world.getBlockMetadata(x, y, z) != meta) continue;
                tries++;
                MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) hatch.getItemDamage(), ForgeDirection.DOWN);
                m.checkStructure(true, m.getBaseMetaTileEntity());
                if (ok.test(m)) return "placed at " + pos(new int[] { x, y, z }) + " (" + tries + " cell(s) tried)";
                world.setBlock(x, y, z, casing, meta, 3);
            }
        m.checkStructure(true, m.getBaseMetaTileEntity());
        return "NOT placed (" + tries + " cell(s) tried)";
    }

    private static void setDebug(MTEHatch h) {
        if (h instanceof MTEHatchEnergyDebug) {
            ((MTEHatchEnergyDebug) h).setVoltageTier(14);
            ((MTEHatchEnergyDebug) h).setAmperage(64);
        }
    }

    // ====================================================================================================== recipes

    private enum Kind {
        GT,
        LINE
    }

    /** One recipe run: an SL-X id, its machine and its live recipe object. */
    private static final class Step {

        final String id;
        final Machine machine;
        final GTRecipe gt;
        final GTRecipe.RecipeAssemblyLine line;
        final Item output;
        final int outputCount;

        Step(String id, Machine machine, GTRecipe gt, GTRecipe.RecipeAssemblyLine line) {
            this.id = id;
            this.machine = machine;
            this.gt = gt;
            this.line = line;
            final ItemStack o = line != null ? line.mOutput : gt.mOutputs[0];
            this.output = o.getItem();
            this.outputCount = o.stackSize;
        }

        Kind kind() {
            return line != null ? Kind.LINE : Kind.GT;
        }

        /** The inputs a run takes (lenses at count 1); for an ore-dict slot its first alternative. */
        List<ItemStack> inputs() {
            final List<ItemStack> in = new ArrayList<>();
            if (line != null) {
                for (int i = 0; i < line.mInputs.length; i++) {
                    ItemStack s = line.mInputs[i];
                    if (line.mOreDictAlt != null && i < line.mOreDictAlt.length
                        && line.mOreDictAlt[i] != null
                        && line.mOreDictAlt[i].length > 0
                        && line.mOreDictAlt[i][0] != null) {
                        final ItemStack alt = line.mOreDictAlt[i][0].copy();
                        alt.stackSize = s == null ? alt.stackSize : s.stackSize;
                        s = alt;
                    }
                    in.add(s == null ? null : fix(s.copy()));
                }
            } else {
                for (ItemStack s : gt.mInputs) if (s != null) in.add(fix(s.copy()));
            }
            return in;
        }

        FluidStack[] fluids() {
            final FluidStack[] f = line != null ? line.mFluidInputs : gt.mFluidInputs;
            return f == null ? new FluidStack[0] : f;
        }

        int duration() {
            return line != null ? line.mDuration : gt.mDuration;
        }
    }

    private static ItemStack fix(ItemStack s) {
        if (s.getItemDamage() == GTValues.W) s.setItemDamage(0);
        if (s.stackSize <= 0) s.stackSize = 1; // a lens: present, not consumed
        return s;
    }

    /** The first SL-X recipe that makes each of our items (SL-X02 before X03: Grade-7 water). */
    private static Map<Item, Step> producers(List<String> out) {
        final Map<Item, Step> producers = new LinkedHashMap<>();
        for (int i = 1; i <= BTSRecipesCircuits.RECIPE_COUNT; i++) {
            final String id = String.format(Locale.ROOT, "SL-X%02d", i);
            Step step = null;
            for (BTSRecipeLedger.Added a : BTSRecipeLedger.allAdded()) {
                if (!id.equals(a.id)) continue;
                if (a.assemblyLine != null) {
                    step = new Step(id, Machine.ASSEMBLY_LINE, null, a.assemblyLine);
                    break;
                }
                if (a.gt == null || a.gt.mFakeRecipe || a.gt.mOutputs == null || a.gt.mOutputs.length == 0) continue;
                final Machine m = machineFor(a.map);
                if (m != null) step = new Step(id, m, a.gt, null);
            }
            if (step == null) {
                if (out != null) out.add("plan: " + id + " has no live recipe");
                continue;
            }
            producers.putIfAbsent(step.output, step);
        }
        return producers;
    }

    private static Machine machineFor(RecipeMap<?> map) {
        if (map == RecipeMaps.blastFurnaceRecipes) return Machine.EBF;
        if (map == RecipeMaps.laserEngraverRecipes) return Machine.ENGRAVER;
        if (map == RecipeMaps.cutterRecipes) return Machine.CUTTER;
        if (map == RecipeMaps.assemblerRecipes) return Machine.ASSEMBLER;
        if (map == RecipeMaps.circuitAssemblerRecipes) return Machine.CIRCUIT_ASSEMBLER;
        return null;
    }

    private static boolean ours(ItemStack s) {
        return s != null && BTSCircuitChecks.isOurs(s.getItem());
    }

    /** Steps, in run order, that make one {@code target} from {@code store} (which the plan consumes virtually). */
    private static List<Step> plan(Item target, Map<Item, Long> store, List<String> out) {
        final Map<Item, Step> producers = producers(out);
        final List<Step> steps = new ArrayList<>();
        try {
            ensure(target, 1, store, producers, steps, 0);
        } catch (IllegalStateException bad) {
            out.add("plan: " + bad.getMessage());
            return null;
        }
        return steps;
    }

    private static void ensure(Item item, long count, Map<Item, Long> store, Map<Item, Step> producers,
        List<Step> steps, int depth) {
        if (depth > 40) throw new IllegalStateException("the chain is too deep at " + name(item));
        while (store.getOrDefault(item, 0L) < count) {
            final Step r = producers.get(item);
            if (r == null) throw new IllegalStateException("no SL-X recipe makes " + name(item));
            for (ItemStack in : r.inputs()) {
                if (ours(in)) ensure(in.getItem(), in.stackSize, store, producers, steps, depth + 1);
            }
            steps.add(r);
            store.merge(item, (long) r.outputCount, Long::sum);
        }
        store.merge(item, -count, Long::sum);
    }

    private static void describePlan(Item target, List<Step> steps, List<String> out) {
        final Map<String, Integer> byMachine = new LinkedHashMap<>();
        final Map<String, Integer> byId = new LinkedHashMap<>();
        long ticks = 0;
        for (Step s : steps) {
            byMachine.merge(s.machine.label, 1, Integer::sum);
            byId.merge(s.id, 1, Integer::sum);
            ticks += s.duration();
        }
        out.add(
            "plan: one " + name(target)
                + " from an empty store = "
                + steps.size()
                + " recipe runs, "
                + byId.size()
                + " distinct recipes, "
                + (ticks / 20)
                + " s of recipe time before overclocks");
        out.add("plan: by machine " + byMachine);
        out.add("plan: by recipe " + byId);
    }

    // ========================================================================================================== job

    private static Job job;
    private static boolean ticker;

    /** True while a {@code make} runs (the recorder's {@code chainbusy} / {@code chainidle} conditions). */
    public static boolean busy() {
        return job != null;
    }

    /** The recipe id of the step a running {@code make} is on ("SL-X24"), or "" (the recorder's {@code chainstep}). */
    public static String currentStep() {
        final Job j = job;
        final Step s = j == null ? null : j.step();
        return s == null ? "" : s.id;
    }

    /** Our items between machines; only machine outputs fill it. */
    private static final Map<Item, Long> STORE = new LinkedHashMap<>();
    /** Everything the bench supplied (not our items), for the report. */
    private static final Map<String, Long> SUPPLIED = new LinkedHashMap<>();
    private static final List<int[]> CHARGED = new ArrayList<>();

    private enum Phase {
        LOAD,
        RUN,
        DONE
    }

    private static final class Job {

        final World world;
        final int[] origin;
        final Item target;
        final List<Step> steps;
        int index;
        Phase phase = Phase.LOAD;
        int ticks;
        boolean started;
        long gameTicks;
        final long realStart = System.currentTimeMillis();
        final Map<String, Long> machineTicks = new LinkedHashMap<>();

        Job(World world, int[] origin, Item target, List<Step> steps) {
            this.world = world;
            this.origin = origin;
            this.target = target;
            this.steps = steps;
        }

        Step step() {
            return index < steps.size() ? steps.get(index) : null;
        }

        String describe() {
            final Step s = step();
            return "making " + name(target)
                + ": step "
                + Math.min(index + 1, steps.size())
                + " of "
                + steps.size()
                + (s == null ? "" : " (" + s.id + " -> " + name(s.output) + " in the " + s.machine.label + ")")
                + ", "
                + gameTicks
                + " game ticks, "
                + (System.currentTimeMillis() - realStart) / 1000
                + " s real";
        }
    }

    private static void start(World world, int[] origin, Item target, List<String> out) {
        if (job != null) {
            out.add("make: already " + job.describe());
            return;
        }
        final Map<Item, Long> virtual = new LinkedHashMap<>(STORE);
        final List<Step> steps = plan(target, virtual, out);
        if (steps == null) return;
        describePlan(target, steps, out);
        job = new Job(world, origin, target, steps);
        ensureTicker();
        out.add("make: started; progress in the server log ([BTS chain]) and in chat at each Mainframe-class step");
    }

    private static void ensureTicker() {
        if (ticker) return;
        ticker = true;
        FMLCommonHandler.instance()
            .bus()
            .register(new Ticker());
    }

    public static final class Ticker {

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            try {
                charge();
                if (job != null) advance(job);
            } catch (RuntimeException bad) {
                BeyondTheStars.LOG.warn("[BTS chain] stopped by an exception", bad);
                say("[BTS chain] stopped: " + bad);
                job = null;
            }
        }
    }

    /**
     * The single blocks have no energy hatch: keep them full (they draw up to 4 A x UMV a tick). And keep the weather
     * clear: GT machines open to the sky can catch fire or explode in the rain (a warped run sees a lot of rain).
     */
    private static void charge() {
        if (job == null && CHARGED.isEmpty()) return;
        final World world = job != null ? job.world : MinecraftServer.getServer().worldServers[0];
        if (world.getWorldInfo()
            .isRaining()
            || world.getWorldInfo()
                .isThundering()) {
            world.getWorldInfo()
                .setRaining(false);
            world.getWorldInfo()
                .setThundering(false);
        }
        world.getWorldInfo()
            .setRainTime(12000);
        world.getWorldInfo()
            .setThunderTime(12000);
        for (int[] p : CHARGED) {
            final MTEBasicMachine m = basicAt(world, p);
            if (m == null) continue;
            final IGregTechTileEntity b = m.getBaseMetaTileEntity();
            final long gap = b.getEUCapacity() - b.getStoredEU();
            if (gap > 0) b.increaseStoredEnergyUnits(gap, true);
        }
    }

    private static void advance(Job j) {
        j.gameTicks++;
        final Step s = j.step();
        if (s == null) {
            finishJob(j);
            return;
        }
        final int[] p = posOf(j.origin, s.machine);
        switch (j.phase) {
            case LOAD: {
                final String why = load(j.world, p, s);
                if (why != null) {
                    say("[BTS chain] " + s.id + " could not be loaded: " + why + " (" + j.describe() + ")");
                    job = null;
                    return;
                }
                j.phase = Phase.RUN;
                j.ticks = 0;
                j.started = false;
                return;
            }
            case RUN: {
                j.ticks++;
                final int progress = maxProgress(j.world, p, s.machine);
                if (progress > 0) j.started = true;
                final long made = outputCount(j.world, p, s.machine, s.output);
                if (made >= s.outputCount && progress <= 0) {
                    collect(j.world, p, s.machine);
                    j.machineTicks.merge(s.machine.label, (long) j.ticks, Long::sum);
                    BeyondTheStars.LOG.info(
                        "[BTS chain] {} done: {} x{} from the {} in {} game ticks (recipe {} t); store {}",
                        s.id,
                        name(s.output),
                        made,
                        s.machine.label,
                        j.ticks,
                        s.duration(),
                        describeStore());
                    if (s.output == SLCircuitItems.siderealMainframe || s.output == SLCircuitItems.empyreanMainframe
                        || s.output == SLCircuitItems.omegaMainframe) {
                        say("[BTS chain] " + s.id + ": " + name(s.output) + " made in the " + s.machine.label);
                    }
                    j.index++;
                    j.phase = Phase.LOAD;
                    return;
                }
                final int limit = j.started ? s.duration() * 2 + 400 : 400;
                if (j.ticks > limit) {
                    say(
                        "[BTS chain] " + s.id
                            + " in the "
                            + s.machine.label
                            + (j.started ? " did not finish" : " never started")
                            + " after "
                            + j.ticks
                            + " game ticks: "
                            + reason(j.world, p, s));
                    job = null;
                }
                return;
            }
            default:
                finishJob(j);
        }
    }

    private static void finishJob(Job j) {
        final long have = STORE.getOrDefault(j.target, 0L);
        final int[] c = at(j.origin, CHEST);
        final TileEntity te = j.world.getTileEntity(c[0], c[1], c[2]);
        String where = "left in the store (no chest)";
        if (have > 0 && te instanceof TileEntityChest) {
            final TileEntityChest chest = (TileEntityChest) te;
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                if (chest.getStackInSlot(i) == null) {
                    chest.setInventorySlotContents(i, new ItemStack(j.target, 1, 0));
                    STORE.merge(j.target, -1L, Long::sum);
                    where = "put in the chest at " + pos(c) + " slot " + i;
                    break;
                }
            }
        }
        say(
            "[BTS chain] DONE: " + name(j.target)
                + " "
                + where
                + "; "
                + j.steps.size()
                + " recipe runs in "
                + j.gameTicks
                + " game ticks ("
                + (System.currentTimeMillis() - j.realStart) / 1000
                + " s real); machine ticks "
                + j.machineTicks);
        BeyondTheStars.LOG.info("[BTS chain] store after the job: {}", describeStore());
        BeyondTheStars.LOG.info("[BTS chain] supplied so far (not our items): {}", SUPPLIED);
        job = null;
    }

    // ================================================================================================ load / collect

    /** Clears the machine's inputs, then loads one run of {@code s}; returns null, or why it could not. */
    private static String load(World world, int[] p, Step s) {
        final List<ItemStack> inputs = s.inputs();
        // our items must all be in the store before anything is moved
        final Map<Item, Long> want = new LinkedHashMap<>();
        for (ItemStack in : inputs) if (ours(in)) want.merge(in.getItem(), (long) in.stackSize, Long::sum);
        for (Map.Entry<Item, Long> e : want.entrySet()) {
            if (STORE.getOrDefault(e.getKey(), 0L) < e.getValue()) {
                return "the store has " + STORE
                    .getOrDefault(e.getKey(), 0L) + " " + name(e.getKey()) + ", the run needs " + e.getValue();
            }
        }
        final List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack in : inputs) {
            if (in == null) {
                stacks.add(null);
                continue;
            }
            if (ours(in)) {
                STORE.merge(in.getItem(), (long) -in.stackSize, Long::sum);
            } else {
                SUPPLIED.merge(in.getDisplayName(), (long) in.stackSize, Long::sum);
            }
            stacks.add(in);
        }
        // a previous run's outputs (ours go back to the store) must not block this one
        collect(world, p, s.machine);
        final FluidStack[] fluids = s.fluids();
        for (FluidStack f : fluids)
            if (f != null) SUPPLIED.merge(f.getLocalizedName() + " (L)", (long) f.amount, Long::sum);

        if (s.machine == Machine.CUTTER || s.machine == Machine.ASSEMBLER || s.machine == Machine.CIRCUIT_ASSEMBLER) {
            final MTEBasicMachine m = basicAt(world, p);
            if (m == null) return "no " + s.machine.label + " at " + pos(p) + " (run build)";
            if (!contains(CHARGED, p)) CHARGED.add(p);
            clearBasic(m);
            int slot = m.getInputSlot();
            for (ItemStack st : stacks) {
                if (st == null) continue;
                if (slot >= m.getInputSlot() + m.mInputSlotCount) return "more item inputs than the machine's slots";
                m.mInventory[slot++] = st;
            }
            if (fluids.length > 1) return "two fluids: a single-block machine has one fluid slot";
            m.setFillableStack(fluids.length == 1 && fluids[0] != null ? fluids[0].copy() : null);
            m.getBaseMetaTileEntity()
                .markInventoryBeenModified();
            m.getBaseMetaTileEntity()
                .enableWorking();
            return null;
        }
        final MTEMultiBlockBase m = multiAt(world, p);
        if (m == null) return "no " + s.machine.label + " at " + pos(p) + " (run build)";
        if (!m.mMachine) m.checkStructure(true, m.getBaseMetaTileEntity());
        if (!m.mMachine) return s.machine.label + " is not formed";
        clearMulti(m);
        if (s.kind() == Kind.LINE) {
            if (m.mInputBusses.size() < stacks.size()) return "the line has " + m.mInputBusses.size() + " input buses";
            for (int i = 0; i < stacks.size(); i++) {
                if (stacks.get(i) != null) m.mInputBusses.get(i).mInventory[0] = stacks.get(i);
            }
            final ItemStack stick = ItemList.Tool_DataStick.get(1);
            AssemblyLineUtils.setAssemblyLineRecipeOnDataStick(stick, s.line);
            m.mInventory[1] = stick;
            SUPPLIED.merge("Data Stick (written by script)", 1L, Long::sum);
        } else {
            final MTEHatchInputBus bus = m.mInputBusses.isEmpty() ? null : m.mInputBusses.get(0);
            if (bus == null) return "no input bus";
            int slot = 0;
            for (ItemStack st : stacks) {
                if (st == null) continue;
                if (slot >= bus.mInventory.length - 1) return "more item inputs than the bus holds";
                bus.mInventory[slot++] = st;
            }
            bus.getBaseMetaTileEntity()
                .markInventoryBeenModified();
        }
        int h = 0;
        for (FluidStack f : fluids) {
            if (f == null) continue;
            if (h >= m.mInputHatches.size()) return "more fluids than input hatches";
            m.mInputHatches.get(h++)
                .setFillableStack(f.copy());
        }
        m.getBaseMetaTileEntity()
            .enableWorking();
        // GT's multis check recipes only when pushed (a hatch's insert does it; these writes are direct)
        m.scheduleRecipeCheck(gregtech.common.tileentities.machines.RecipeCheckReason.IMMEDIATE);
        return null;
    }

    private static void clearBasic(MTEBasicMachine m) {
        for (int i = m.getInputSlot(); i < m.getInputSlot() + m.mInputSlotCount; i++) {
            leftover(m.mInventory[i], "a single block's input slot");
            giveBack(m.mInventory[i]);
            m.mInventory[i] = null;
        }
        m.setFillableStack(null);
    }

    private static void clearMulti(MTEMultiBlockBase m) {
        for (MTEHatchInputBus bus : m.mInputBusses) {
            for (int i = 0; i < bus.mInventory.length; i++) {
                leftover(bus.mInventory[i], "an input bus");
                giveBack(bus.mInventory[i]);
                bus.mInventory[i] = null;
            }
        }
        for (MTEHatchInput hatch : m.mInputHatches) hatch.setFillableStack(null);
        m.mInventory[1] = null;
    }

    /** Logs an item of ours still sitting in a machine's input when the next run is loaded (should never happen). */
    private static void leftover(ItemStack s, String where) {
        if (s != null && ours(s) && s.stackSize > 0) {
            BeyondTheStars.LOG.warn("[BTS chain] leftover input: {}x {} in {}", s.stackSize, s.getDisplayName(), where);
        }
    }

    /** Leftovers of ours go back to the store; anything else was supplied and is dropped. */
    private static void giveBack(ItemStack s) {
        if (s != null && ours(s) && s.stackSize > 0) STORE.merge(s.getItem(), (long) s.stackSize, Long::sum);
    }

    private static int maxProgress(World world, int[] p, Machine machine) {
        final MTEBasicMachine b = basicAt(world, p);
        if (b != null) return b.mMaxProgresstime;
        final MTEMultiBlockBase m = multiAt(world, p);
        return m == null ? 0 : m.mMaxProgresstime;
    }

    private static List<ItemStack> outputs(World world, int[] p) {
        final List<ItemStack> out = new ArrayList<>();
        final MTEBasicMachine b = basicAt(world, p);
        if (b != null) {
            for (int i = 0; i < b.mOutputItems.length; i++) out.add(b.mInventory[b.getOutputSlot() + i]);
            return out;
        }
        final MTEMultiBlockBase m = multiAt(world, p);
        if (m == null) return out;
        for (MTEHatchOutputBus bus : m.mOutputBusses) Collections.addAll(out, bus.mInventory);
        return out;
    }

    private static long outputCount(World world, int[] p, Machine machine, Item item) {
        long n = 0;
        for (ItemStack s : outputs(world, p)) if (s != null && s.getItem() == item) n += s.stackSize;
        return n;
    }

    private static void collect(World world, int[] p, Machine machine) {
        final MTEBasicMachine b = basicAt(world, p);
        if (b != null) {
            for (int i = 0; i < b.mOutputItems.length; i++) {
                giveBack(b.mInventory[b.getOutputSlot() + i]);
                b.mInventory[b.getOutputSlot() + i] = null;
            }
            return;
        }
        final MTEMultiBlockBase m = multiAt(world, p);
        if (m == null) return;
        for (MTEHatchOutputBus bus : m.mOutputBusses) {
            for (int i = 0; i < bus.mInventory.length; i++) {
                giveBack(bus.mInventory[i]);
                bus.mInventory[i] = null;
            }
        }
    }

    private static String reason(World world, int[] p, Step s) {
        final MTEMultiBlockBase m = multiAt(world, p);
        if (m != null) {
            return (m.mMachine ? "formed" : "NOT formed") + ", last result "
                + (m.getCheckRecipeResult() == null ? "none"
                    : m.getCheckRecipeResult()
                        .getDisplayString());
        }
        final MTEBasicMachine b = basicAt(world, p);
        if (b == null) return "no machine";
        final RecipeMap<?> map = b.getRecipeMap();
        GTRecipe found = null;
        try {
            found = map.findRecipeQuery()
                .items(
                    java.util.Arrays.copyOfRange(b.mInventory, b.getInputSlot(), b.getInputSlot() + b.mInputSlotCount))
                .fluids(b.getFillableStack())
                .voltage(GTValues.V[12])
                .find();
        } catch (RuntimeException odd) {
            // reported below as not found
        }
        return "basic machine: EU " + b.getBaseMetaTileEntity()
            .getStoredEU()
            + ", lookup at UMV "
            + (found == null ? "finds nothing" : "finds " + name(found.mOutputs[0].getItem()))
            + ", output blocked "
            + b.mOutputBlocked
            + ", cleanroom bypass "
            + gregtech.api.util.FakeCleanroom.isCleanroomBypassEnabled();
    }

    // ======================================================================================================= gt run

    /**
     * A stock Assembler recipe with an any-of-tier circuit slot, run in the Assembling Machine UMV with our Mainframe
     * in
     * that slot. Prefers the recipe with the fewest circuits, within UMV voltage, nine items and one fluid.
     */
    private static void gtRecipe(World world, int[] o, String tier, List<String> out) {
        if (job != null) {
            out.add("gt: wait for the job: " + job.describe());
            return;
        }
        final Materials material;
        final Item ours;
        if ("uxv".equals(tier)) {
            material = Materials.UXV;
            ours = SLCircuitItems.empyreanMainframe;
        } else if ("max".equals(tier)) {
            material = Materials.MAX;
            ours = SLCircuitItems.omegaMainframe;
        } else {
            material = Materials.UMV;
            ours = SLCircuitItems.siderealMainframe;
        }
        GTRecipe best = null;
        int bestSlot = -1;
        for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            if (r == null || r.mFakeRecipe || !r.mEnabled || r.mInputs == null || r.mEUt > GTValues.V[12]) continue;
            if (r.mInputs.length > 9 || (r.mFluidInputs != null && r.mFluidInputs.length > 1)) continue;
            // stock only: not one of ours (a ledger recipe), not a circuit wrap (packaging)
            if (BTSRecipeLedger.added(r) != null || r.mOutputs == null
                || r.mOutputs.length == 0
                || r.mOutputs[0] == null
                || r.mOutputs[0].getDisplayName()
                    .toLowerCase(Locale.ROOT)
                    .contains("wrap"))
                continue;
            final int slot = circuitSlot(r.mInputs, material);
            if (slot < 0) continue;
            if (best == null || r.mInputs[slot].stackSize < best.mInputs[bestSlot].stackSize) {
                best = r;
                bestSlot = slot;
            }
        }
        if (best == null) {
            out.add("gt: no stock Assembler recipe within UMV voltage takes " + OrePrefixes.circuit.get(material));
            return;
        }
        final int[] p = at(o, ASSEMBLER);
        final MTEBasicMachine m = basicAt(world, p);
        if (m == null) {
            out.add("gt: no Assembling Machine (run build)");
            return;
        }
        if (!contains(CHARGED, p)) CHARGED.add(p);
        ensureTicker();
        clearBasic(m);
        collect(world, p, Machine.ASSEMBLER);
        int fromStore = 0, supplied = 0;
        int slot = m.getInputSlot();
        for (int i = 0; i < best.mInputs.length; i++) {
            final ItemStack in = best.mInputs[i];
            if (in == null) continue;
            final ItemStack s;
            if (i == bestSlot) {
                final long have = STORE.getOrDefault(ours, 0L);
                fromStore = (int) Math.min(have, in.stackSize);
                supplied = in.stackSize - fromStore;
                STORE.merge(ours, (long) -fromStore, Long::sum);
                s = new ItemStack(ours, in.stackSize, 0);
            } else {
                s = fix(in.copy());
            }
            m.mInventory[slot++] = s;
        }
        m.setFillableStack(
            best.mFluidInputs != null && best.mFluidInputs.length == 1 ? best.mFluidInputs[0].copy() : null);
        m.getBaseMetaTileEntity()
            .markInventoryBeenModified();
        m.getBaseMetaTileEntity()
            .enableWorking();
        out.add(
            "gt: stock recipe -> " + best.mOutputs[0].getDisplayName()
                + " ("
                + best.mDuration
                + " t at "
                + best.mEUt
                + " EU/t), its "
                + OrePrefixes.circuit.get(material)
                + " slot ("
                + best.mInputs[bestSlot].stackSize
                + "x "
                + best.mInputs[bestSlot].getDisplayName()
                + ") filled with "
                + best.mInputs[bestSlot].stackSize
                + "x "
                + name(ours)
                + " ("
                + fromStore
                + " from the store, "
                + supplied
                + " supplied); the other inputs supplied. Check with 'chain status'.");
    }

    private static int circuitSlot(ItemStack[] inputs, Materials tier) {
        for (int i = 0; i < inputs.length; i++) {
            final ItemStack in = inputs[i];
            if (in == null || in.stackSize <= 0 || ours(in)) continue;
            final ItemData data = GTOreDictUnificator.getAssociation(in);
            if (data != null && data.mPrefix == OrePrefixes.circuit
                && data.mMaterial != null
                && data.mMaterial.mMaterial == tier) return i;
        }
        return -1;
    }

    // ======================================================================================================= status

    private static void status(World world, int[] o, List<String> out) {
        out.add(job == null ? "job: none" : "job: " + job.describe());
        for (Machine m : Machine.values()) {
            final int[] p = posOf(o, m);
            final MTEBasicMachine b = basicAt(world, p);
            final MTEMultiBlockBase multi = b == null ? multiAt(world, p) : null;
            if (b == null && multi == null) {
                out.add(m.label + ": none at " + pos(p));
                continue;
            }
            final List<String> made = new ArrayList<>();
            for (ItemStack s : outputs(world, p)) if (s != null) made.add(s.stackSize + "x " + s.getDisplayName());
            if (b != null) {
                out.add(
                    m.label + ": progress "
                        + b.mProgresstime
                        + "/"
                        + b.mMaxProgresstime
                        + ", EU "
                        + b.getBaseMetaTileEntity()
                            .getStoredEU()
                        + ", outputs "
                        + made);
            } else {
                out.add(
                    m.label + ": "
                        + (multi.mMachine ? "FORMED" : "NOT FORMED")
                        + ", progress "
                        + multi.mProgresstime
                        + "/"
                        + multi.mMaxProgresstime
                        + ", outputs "
                        + made);
            }
        }
        final int[] c = at(o, CHEST);
        final TileEntity te = world.getTileEntity(c[0], c[1], c[2]);
        if (te instanceof TileEntityChest) {
            final List<String> held = new ArrayList<>();
            for (int i = 0; i < ((TileEntityChest) te).getSizeInventory(); i++) {
                final ItemStack s = ((TileEntityChest) te).getStackInSlot(i);
                if (s != null) held.add(s.stackSize + "x " + s.getDisplayName());
            }
            out.add("chest: " + held);
        }
        out.add("store: " + describeStore());
    }

    private static String describeStore() {
        final List<String> parts = new ArrayList<>();
        for (Map.Entry<Item, Long> e : STORE.entrySet())
            if (e.getValue() != 0) parts.add(e.getValue() + "x " + name(e.getKey()));
        return parts.toString();
    }

    // ===================================================================================================== helpers

    private static void say(String line) {
        BeyondTheStars.LOG.info(line);
        final MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.getConfigurationManager() != null) {
            server.getConfigurationManager()
                .sendChatMsg(new ChatComponentText(line));
        }
    }

    private static String name(Item item) {
        try {
            return new ItemStack(item, 1, 0).getDisplayName();
        } catch (RuntimeException odd) {
            return String.valueOf(Item.itemRegistry.getNameForObject(item));
        }
    }

    private static boolean contains(List<int[]> list, int[] p) {
        for (int[] q : list) if (q[0] == p[0] && q[1] == p[1] && q[2] == p[2]) return true;
        return false;
    }

    private static MTEBasicMachine basicAt(World world, int[] p) {
        final IMetaTileEntity mte = mteAt(world, p);
        return mte instanceof MTEBasicMachine ? (MTEBasicMachine) mte : null;
    }

    private static MTEMultiBlockBase multiAt(World world, int[] p) {
        final IMetaTileEntity mte = mteAt(world, p);
        return mte instanceof MTEMultiBlockBase ? (MTEMultiBlockBase) mte : null;
    }

    private static IMetaTileEntity mteAt(World world, int[] p) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity() : null;
    }

    private static String pos(int[] p) {
        return "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }

    /**
     * Endless for any exact stack (casings, coils, glass, frames); by predicate (a hatch cell) the listed hatches, up
     * to
     * each one's count, the least handed out first.
     */
    private static final class Source implements IItemSource {

        private final ItemStack[] hatches;
        private final int[] max;
        private final int[] count;
        final Map<String, Integer> given = new LinkedHashMap<>();

        /** Pairs of (hatch stack, how many). */
        Source(Object... pairs) {
            hatches = new ItemStack[pairs.length / 2];
            max = new int[hatches.length];
            count = new int[hatches.length];
            for (int i = 0; i < hatches.length; i++) {
                hatches[i] = (ItemStack) pairs[2 * i];
                max[i] = (Integer) pairs[2 * i + 1];
            }
        }

        @Override
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int n) {
            int best = -1;
            for (int i = 0; i < hatches.length; i++) {
                if (hatches[i] == null || count[i] >= max[i] || !predicate.test(hatches[i])) continue;
                if (best < 0 || count[i] < count[best]) best = i;
            }
            if (best < 0) return Collections.emptyMap();
            final Map<ItemStack, Integer> m = new LinkedHashMap<>();
            m.put(hatches[best].copy(), 1);
            return m;
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            if (stack == null || stack.getItem() == null) return false;
            for (int i = 0; i < hatches.length; i++) {
                final ItemStack h = hatches[i];
                if (h != null && h.getItem() == stack.getItem() && h.getItemDamage() == stack.getItemDamage()) {
                    if (count[i] >= max[i]) return false;
                    if (!simulate) {
                        count[i]++;
                        given.merge(h.getDisplayName(), 1, Integer::sum);
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
