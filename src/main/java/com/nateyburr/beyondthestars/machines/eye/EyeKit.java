package com.nateyburr.beyondthestars.machines.eye;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.WirelessNetworkManager;
import tectech.thing.casing.TTCasingsContainer;

/**
 * SINGULARITY LADDER P3: the Eye's creative test path. {@code /btskit eye} hands over the parts and
 * prints each piece's bill of materials; {@code /btseye} raises a complete, formed Eye in front of the
 * player. Owned by the Eye unit.
 * <p>
 * <b>No count here is typed by hand (D-22).</b> Socket positions and bills come from the exported
 * shapes ({@link BTSShapes}) and {@code manifest.json}, the files the controller walks.
 */
public final class EyeKit {

    private EyeKit() {}

    /** Voxel centre of the Eye (the render), as a/b/c from the controller: 40 up, 40 deep. */
    private static final int[] CENTRE_REL = { 0, -40, 40 };
    /** What {@code fill} tops the buffers to, and the wireless EU it grants. */
    private static final long FILL_LITRES = 1_000_000_000_000L;
    private static final BigInteger FILL_EU = BigInteger.TEN.pow(18);

    // ===================================================================================== kit

    /**
     * {@code /btskit eye}: controller, every transition core, every star chart, 16 ignition charges, hatches
     * (D-49: 8 Stellar Field Sensor Hatches, 8 plain input hatches for the Field sockets, 1 input bus for the
     * charges), Neutronium cells of Antipolarization Agent, DSSF and Tachyon Rich Temporal Fluid, top-tier fields.
     */
    public static void giveKit(EntityPlayerMP player) {
        give(player, BTSMachines.eyeOfHarmony == null ? null : BTSMachines.eyeOfHarmony.copy());
        // D-49 (d49-build-plan.md §11.2): the cores in transition order, the T1-T6 charts, the ignition charges.
        give(player, item(SLItems.stellarMassCore));
        give(player, item(SLItems.shellBurningCore));
        give(player, item(SLItems.galacticUpgradeCore));
        give(player, item(SLItems.subspaceCoolingCore));
        give(player, item(SLItems.magnetarSeedCore));
        give(player, item(SLItems.starChartSol));
        give(player, item(SLItems.starChartSiriusA));
        give(player, item(SLItems.starChartArcturus));
        give(player, item(SLItems.starChartNeutronStar));
        give(player, item(SLItems.starChartSiriusB));
        give(player, item(SLItems.starChartMagnetar));
        giveMany(player, item(SLItems.stellarIgnitionCharge), 16);
        giveMany(player, BTSMachines.stellarFieldSensor == null ? null : BTSMachines.stellarFieldSensor.copy(), 8);
        giveMany(player, BTSMachines.directiveHatch == null ? null : BTSMachines.directiveHatch.copy(), 4);
        giveMany(player, ItemList.Hatch_Input_MAX.get(1L), 8);
        give(player, ItemList.Hatch_Input_Bus_MAX.get(1L));
        give(player, fluidCell(SLFluids.antipolarizationAgent));
        give(player, fluidCell(fluidOf(Materials.DimensionallyShiftedSuperfluid.getFluid(1L))));
        give(player, fluidCell(fluidOf(Materials.Time.getMolten(1L))));
        giveMany(player, ItemList.Hatch_Input_MAX.get(1L), sockets(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h'));
        giveMany(
            player,
            ItemList.Hatch_Input_MAX.get(1L),
            sockets(MTEEyeOfHarmonyBTS.PIECE_BEAM, 's') + sockets(MTEEyeOfHarmonyBTS.PIECE_JET, 't')
                + sockets(MTEEyeOfHarmonyBTS.PIECE_RIM, 'g'));
        give(player, ItemList.Hatch_Output_Bus_ME.get(1L));
        give(player, ItemList.Hatch_Output_ME.get(1L));
        give(player, ItemList.Hatch_AutoMaintenance.get(1L));
        giveMany(
            player,
            generator(TTCasingsContainer.SpacetimeCompressionFieldGenerators, 8),
            BTSShapes.count(MTEEyeOfHarmonyBTS.PIECE_CORE, 'W'));
        giveMany(
            player,
            generator(TTCasingsContainer.TimeAccelerationFieldGenerator, 8),
            BTSShapes.count(MTEEyeOfHarmonyBTS.PIECE_CORE, 'X'));
        giveMany(
            player,
            generator(TTCasingsContainer.StabilisationFieldGenerators, 8),
            BTSShapes.count(MTEEyeOfHarmonyBTS.PIECE_CORE, 'Y'));
        player.inventoryContainer.detectAndSendChanges();

        tell(player, EnumChatFormatting.AQUA + "== Eye of Harmony test kit ==");
        tell(
            player,
            "Given: the controller, the five transition cores, the six star charts, 16 Stellar Ignition Charges, "
                + "input hatches for every socket (plus 8 for the Field sockets and an input bus for the charges), "
                + "8 Stellar Field Sensor Hatches, 4 Directive Hatches, cells of Antipolarization Agent, DSSF and Tachyon Rich Temporal "
                + "Fluid, an ME output bus + hatch, auto-maintenance and tier-9 field generators.");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        for (String piece : MTEEyeOfHarmonyBTS.PIECES) {
            tell(player, " - " + piece + ": " + bill(piece));
        }
        tell(
            player,
            EnumChatFormatting.GREEN + "Fastest look: /btseye t1..t6 [gen 1-9] [fill] raises a complete, formed Eye "
                + "in front of you. Or a hologram projector on the controller: stack 1..6 = T1..T6 (bands are "
                + "cumulative).");
    }

    /** A Neutronium Large Fluid Cell filled to capacity with {@code fluid}, or null. */
    private static ItemStack fluidCell(Fluid fluid) {
        if (fluid == null) return null;
        final ItemStack cell = ItemList.Large_Fluid_Cell_Neutronium.get(1L);
        if (cell == null || !(cell.getItem() instanceof IFluidContainerItem)) return null;
        ((IFluidContainerItem) cell.getItem()).fill(cell, new FluidStack(fluid, Integer.MAX_VALUE), true);
        return cell;
    }

    private static Fluid fluidOf(FluidStack stack) {
        return stack == null ? null : stack.getFluid();
    }

    private static ItemStack generator(Block block, int meta) {
        return block == null ? null : new ItemStack(block, 1, meta);
    }

    private static ItemStack item(Item item) {
        return item == null ? null : new ItemStack(item, 1, 0);
    }

    /** "19,616 blocks: 9,406x Reinforced Spatial ..." from the manifest's bill of materials. */
    private static String bill(String piece) {
        final JsonObject entry = BTSShapes.manifestPiece(piece);
        if (entry == null || !entry.has("bom")) return "(not in manifest)";
        final StringBuilder sb = new StringBuilder();
        for (JsonElement el : entry.getAsJsonArray("bom")) {
            final JsonObject row = el.getAsJsonObject();
            if ("~".equals(
                row.get("ch")
                    .getAsString()))
                continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(
                qty(
                    row.get("count")
                        .getAsInt()))
                .append("x ")
                .append(
                    shortBlock(
                        row.get("block")
                            .getAsString()));
        }
        return sb.toString();
    }

    private static String shortBlock(String text) {
        String s = text.replace("NEW ", "")
            .replace("Casings.", "");
        final int paren = s.indexOf(" (");
        if (paren > 0) s = s.substring(0, paren);
        final int comma = s.indexOf(", ");
        if (comma > 0) s = s.substring(0, comma);
        return s;
    }

    private static int sockets(String piece, char ch) {
        return socketRels(piece, ch).length;
    }

    /** Every socket of one char in a piece, as a/b/c offsets from the controller. Empty if none. */
    public static int[][] socketRels(String piece, char ch) {
        try {
            final JsonObject entry = BTSShapes.manifestPiece(piece);
            if (entry == null || !entry.has("sockets")) return new int[0][];
            final JsonObject all = entry.getAsJsonObject("sockets");
            final String key = String.valueOf(ch);
            if (!all.has(key)) return new int[0][];
            final JsonArray list = all.getAsJsonArray(key);
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
        } catch (RuntimeException bad) {
            return new int[0][];
        }
    }

    // ========================================================================= creative build

    /** Result of one {@link #build}: whether it formed, and the lines to print. */
    public static final class Result {

        public boolean formed;
        public String failure;
        public final List<String> lines = new ArrayList<>();
    }

    /**
     * Raises a complete Eye with its controller at (x, y, z) facing {@code facing}: the core and the
     * first {@code tiers - 1} bands (1..6), the field generators at {@code genMeta} (0-8), a hatch in every
     * socket (D-49: the last plinth gas socket takes an input bus for the ignition charges, every Field
     * socket a plain input hatch, every Sensor socket a Stellar Field Sensor Hatch, the T6 rim's sockets
     * input hatches), unlocked to its tier (D-49), the top tier's chart in the controller slot, the player as
     * owner; with {@code fill}, full input buffers and 10^18 wireless EU. Then forms it. Server only.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, int tiers, int genMeta,
        boolean fill, EntityPlayerMP player) {
        return build(
            world,
            x,
            y,
            z,
            facing,
            tiers,
            genMeta,
            fill,
            player.getUniqueID(),
            player.getCommandSenderName(),
            false);
    }

    /**
     * As above, owned by {@code owner} (the console form passes a fixed test UUID). With
     * {@code survival} the blocks go in through {@code survivalConstruct} (the hologram projector's
     * survival path, projector-sized rounds, from an endless item source) instead of the creative
     * {@code construct}; the field generators, hatches and forming follow either way.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, int tiers, int genMeta,
        boolean fill, UUID owner, String ownerName, boolean survival) {
        return build(world, x, y, z, facing, tiers, genMeta, fill, owner, ownerName, survival, false);
    }

    /**
     * As above; with {@code legacy} the D-49 sockets get no hatch (no Field, Sensor or rim hatch, and
     * no input bus on the plinth), so their cells keep the casing a pre-D-49 Eye had there: the
     * migration check that an Eye built before the Field/Sensor sockets still forms.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, int tiers, int genMeta,
        boolean fill, UUID owner, String ownerName, boolean survival, boolean legacy) {
        final Result result = new Result();
        final ItemStack controller = BTSMachines.eyeOfHarmony;
        if (controller == null) {
            result.failure = "the Eye controller is not registered";
            return result;
        }
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) controller.getItemDamage(), facing)) {
            result.failure = "could not place the controller";
            return result;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEEyeOfHarmonyBTS)) {
            result.failure = "the controller did not initialise";
            return result;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEEyeOfHarmonyBTS eye = (MTEEyeOfHarmonyBTS) base.getMetaTileEntity();
        base.setOwnerUuid(owner);
        base.setOwnerName(ownerName);

        // 1. Every block: the stack size picks how many bands (1 = core .. 6 = + galactic rim).
        final ItemStack trigger = new ItemStack(controller.getItem(), tiers, controller.getItemDamage());
        if (survival) {
            final SLConsoleTest.SurvivalRun run = SLConsoleTest
                .survivalBuild(eye, world, trigger, SURVIVAL_BUDGET, SURVIVAL_MAX_ROUNDS);
            result.lines.add(
                "Survival auto-place: " + qty(run.placed)
                    + " blocks in "
                    + run.rounds
                    + " rounds, "
                    + (run.finished ? "finished (-1)" : run.stalled ? "STALLED (0 placed twice)" : "round cap hit"));
        } else {
            eye.construct(trigger, false);
        }
        final ExtendedFacing alignment = eye.getExtendedFacing();

        // 2. The field generators at the asked tier. Compression is raised to the tier's gate (Q-20,
        // MTEEyeOfHarmonyBTS.COMPRESSION_META_FOR_TIER) so a low "gen" never leaves the Eye below "tiers".
        final int compressionMeta = Math.max(genMeta, MTEEyeOfHarmonyBTS.COMPRESSION_META_FOR_TIER[tiers]);
        final int generators = setGenerators(world, alignment, x, y, z, genMeta, compressionMeta);

        // 3. A hatch in every socket.
        int fitted = 0;
        final int[][] gas = socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h');
        for (int i = 0; i < gas.length; i++) {
            // D-49 R-1: the last gas socket takes an input bus (the ignition charges); the rest gas hatches
            final ItemList hatch = !legacy && i == gas.length - 1 && gas.length > 1 ? ItemList.Hatch_Input_Bus_MAX
                : ItemList.Hatch_Input_MAX;
            if (fit(world, alignment, x, y, z, gas[i], hatch)) fitted++;
        }
        if (!legacy) {
            for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'f')) {
                if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
            }
            // AUTOMATION PASS: the first three Sensor sockets get Directive Hatches (Trims, Status, Transition, code
            // mode), the rest Stellar Field Sensor Hatches
            final int[][] n = socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'n');
            for (int i = 0; i < n.length; i++) {
                if (i < DIRECTIVE_SETS.length) {
                    if (fitDirective(world, alignment, x, y, z, n[i], DIRECTIVE_SETS[i])) fitted++;
                } else if (fitStack(world, alignment, x, y, z, n[i], BTSMachines.stellarFieldSensor)) {
                    fitted++;
                }
            }
        }
        final ItemList[] outputs = { ItemList.Hatch_Output_Bus_ME, ItemList.Hatch_Output_ME };
        final int[][] o = socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'o');
        for (int i = 0; i < o.length && i < outputs.length; i++) {
            if (fit(world, alignment, x, y, z, o[i], outputs[i])) fitted++;
        }
        for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'm')) {
            if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_AutoMaintenance)) fitted++;
        }
        if (tiers >= 2) {
            for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_BEAM, 's')) {
                if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
            }
        }
        if (tiers >= 3) {
            for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_JET, 't')) {
                if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
            }
        }
        if (tiers >= 6 && !legacy) {
            for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_RIM, 'g')) {
                if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
            }
        }

        // 4. Upgrade, target, owner's power, then form.
        // D-49: a creative test build is unlocked to its tier (the milestones, cores and transitions are skipped).
        eye.debugUnlock(tiers);
        final Item[] charts = { SLItems.starChartSol, SLItems.starChartSiriusA, SLItems.starChartArcturus,
            SLItems.starChartNeutronStar, SLItems.starChartSiriusB, SLItems.starChartMagnetar };
        final Item chart = charts[Math.max(0, Math.min(charts.length, tiers) - 1)];
        if (chart != null) base.setInventorySlotContents(eye.getControllerSlotIndex(), new ItemStack(chart, 1, 0));
        if (fill) {
            eye.debugFillBuffers(FILL_LITRES);
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            WirelessNetworkManager.addEUToGlobalEnergyMap(owner, FILL_EU);
        }
        eye.checkStructure(true, base);
        eye.fixAllIssues();
        base.enableWorking();

        result.formed = eye.mMachine;
        result.lines.add(
            (result.formed ? EnumChatFormatting.GREEN + "Eye T" + tiers + " FORMED"
                : EnumChatFormatting.RED + "Eye T" + tiers + " NOT FORMED") + EnumChatFormatting.RESET
                + " at ("
                + x
                + ", "
                + y
                + ", "
                + z
                + "), facing "
                + facing.name()
                + "; "
                + fitted
                + " hatches fitted, "
                + generators
                + " field generators at tier "
                + (genMeta + 1)
                + (compressionMeta != genMeta
                    ? " (compression raised to tier " + (compressionMeta + 1) + ", the T" + tiers + " gate)"
                    : "")
                + ".");
        result.lines.addAll(eye.statusLines());
        return result;
    }

    /** The projector's default survival round (StructureLib's 25 elements, which survivalBudget scales). */
    private static final int SURVIVAL_BUDGET = 25;
    private static final int SURVIVAL_MAX_ROUNDS = 4000;

    /**
     * Console/command-block checks on an Eye {@link #build} raised: the first wrong cells if it did
     * not form, the mirror check (the core's lone east gas hatch, and at T3+ the jet's lone east
     * socket, are the mockup's +x side, so they must stand on the viewer's right, seen from in front
     * of the controller), and a creative re-projection over the finished machine, which must leave it
     * formed (no hatch paved over, no field generator downgraded), then the survival repair test
     * ({@link SLConsoleTest#damageAndRepair}). With {@code full} false only the walk and status are
     * reported (the {@code check} form).
     */
    public static List<String> diagnose(World world, int x, int y, int z, ForgeDirection facing, int tiers,
        boolean full) {
        final List<String> out = new ArrayList<>();
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEEyeOfHarmonyBTS)) {
            out.add("No Eye controller at the build point.");
            return out;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEEyeOfHarmonyBTS eye = (MTEEyeOfHarmonyBTS) base.getMetaTileEntity();
        final ExtendedFacing alignment = eye.getExtendedFacing();
        if (!full) eye.checkStructure(true, base);
        out.add("Eye at (" + x + ", " + y + ", " + z + "): " + (eye.mMachine ? "FORMED" : "NOT FORMED"));
        if (!eye.mMachine) {
            out.add("First wrong cells:");
            for (String line : SLConsoleTest.firstErrors(eye, base, 3)) {
                out.add("  " + line);
            }
            eye.checkStructure(true, base);
        }
        if (!full) {
            out.addAll(eye.statusLines());
            return out;
        }
        out.add(probe(world, alignment, facing, x, y, z, "core east gas hatch", MTEEyeOfHarmonyBTS.PIECE_CORE, 'h'));
        if (tiers >= 3) {
            out.add(probe(world, alignment, facing, x, y, z, "jet east socket", MTEEyeOfHarmonyBTS.PIECE_JET, 't'));
        }

        if (eye.mMachine) {
            final ItemStack controller = BTSMachines.eyeOfHarmony;
            final int before = fieldSignature(world, alignment, x, y, z);
            eye.construct(new ItemStack(controller.getItem(), tiers, controller.getItemDamage()), false);
            eye.checkStructure(true, base);
            final int after = fieldSignature(world, alignment, x, y, z);
            out.add(
                "Creative re-projection over the formed Eye: "
                    + (eye.mMachine ? "still FORMED"
                        : "BROKE IT - " + String.join("; ", SLConsoleTest.firstErrors(eye, base, 2)))
                    + (before == after ? ", field generators unchanged" : ", FIELD GENERATORS CHANGED"));
            if (eye.mMachine) {
                out.addAll(
                    SLConsoleTest.damageAndRepair(
                        eye,
                        base,
                        new ItemStack(controller.getItem(), tiers, controller.getItemDamage()),
                        SLConsoleTest
                            .bounds(alignment, x, y, z, java.util.Arrays.copyOf(MTEEyeOfHarmonyBTS.PIECES, tiers))));
            }
        }
        out.addAll(eye.statusLines());
        return out;
    }

    /**
     * Test bench: clears every block of one band of the Eye at (x, y, z) to air, hatches included, then
     * re-walks it; returns the number of cells cleared. Used to show the D-49 cumulative rule (an Eye
     * whose beam band is gone runs as T1 even with its jet crescent standing).
     */
    public static int stripPiece(World world, int x, int y, int z, String piece) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEEyeOfHarmonyBTS)) {
            return 0;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEEyeOfHarmonyBTS eye = (MTEEyeOfHarmonyBTS) base.getMetaTileEntity();
        final ExtendedFacing alignment = eye.getExtendedFacing();
        final String[][] shape = BTSShapes.load(piece);
        final int[] off = BTSShapes.offset(piece);
        int n = 0;
        for (int c = 0; c < shape.length; c++) {
            for (int b = 0; b < shape[c].length; b++) {
                final String row = shape[c][b];
                for (int a = 0; a < row.length(); a++) {
                    if (row.charAt(a) == ' ') continue;
                    final int[] p = worldPos(alignment, x, y, z, new int[] { a - off[0], b - off[1], c - off[2] });
                    world.setBlockToAir(p[0], p[1], p[2]);
                    n++;
                }
            }
        }
        eye.checkStructure(true, base);
        return n;
    }

    /** The socket of {@code ch} with the largest a (the mockup's +x side): what stands there, which side. */
    private static String probe(World world, ExtendedFacing alignment, ForgeDirection facing, int x, int y, int z,
        String label, String piece, char ch) {
        int[] best = null;
        for (int[] rel : socketRels(piece, ch)) {
            if (best == null || rel[0] > best[0]) best = rel;
        }
        if (best == null || best[0] <= 0) return "Orientation: no +x socket '" + ch + "' in " + piece;
        final int[] p = SLConsoleTest.worldPos(alignment, x, y, z, best);
        final String side = SLConsoleTest.viewerSide(facing, x, z, p[0], p[2]);
        return "Orientation: " + label
            + " (a="
            + best[0]
            + ") at ("
            + p[0]
            + ", "
            + p[1]
            + ", "
            + p[2]
            + ") holds "
            + SLConsoleTest.blockAt(world, p[0], p[1], p[2])
            + ", on the viewer's "
            + side
            + (side.equals("right") ? " (matches the mockup)" : " (MIRRORED vs the mockup: re-export --flipH)");
    }

    /** A cheap hash of every field-generator cell's meta, to spot a downgrade by a re-projection. */
    private static int fieldSignature(World world, ExtendedFacing alignment, int x, int y, int z) {
        final String piece = MTEEyeOfHarmonyBTS.PIECE_CORE;
        final String[][] shape = BTSShapes.load(piece);
        final int[] off = BTSShapes.offset(piece);
        int h = 1;
        for (int c = 0; c < shape.length; c++) {
            for (int b = 0; b < shape[c].length; b++) {
                final String row = shape[c][b];
                for (int a = 0; a < row.length(); a++) {
                    if (generatorFor(row.charAt(a)) == null) continue;
                    final int[] p = worldPos(alignment, x, y, z, new int[] { a - off[0], b - off[1], c - off[2] });
                    h = 31 * h + world.getBlockMetadata(p[0], p[1], p[2]);
                }
            }
        }
        return h;
    }

    /**
     * Re-sets every X/Y cell of the core to generator meta {@code meta} and every W (compression) cell
     * to {@code compressionMeta}; returns the count.
     */
    private static int setGenerators(World world, ExtendedFacing alignment, int x, int y, int z, int meta,
        int compressionMeta) {
        final String piece = MTEEyeOfHarmonyBTS.PIECE_CORE;
        final String[][] shape = BTSShapes.load(piece);
        final int[] off = BTSShapes.offset(piece);
        int n = 0;
        for (int c = 0; c < shape.length; c++) {
            for (int b = 0; b < shape[c].length; b++) {
                final String row = shape[c][b];
                for (int a = 0; a < row.length(); a++) {
                    final Block block = generatorFor(row.charAt(a));
                    if (block == null) continue;
                    final int[] p = worldPos(alignment, x, y, z, new int[] { a - off[0], b - off[1], c - off[2] });
                    world.setBlock(p[0], p[1], p[2], block, row.charAt(a) == 'W' ? compressionMeta : meta, 2);
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Test bench (the 1:1 compression gate): sets the core's first Spacetime Compression Field Generator - or every
     * one,
     * with {@code all} - to {@code meta} (0-8 = TecTech tiers 1-9) on the Eye standing at its controller, as a player
     * swapping a generator would, and re-walks it. Returns the cells changed.
     */
    public static int setCompression(World world, MTEEyeOfHarmonyBTS eye, int meta, boolean all) {
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        final ExtendedFacing alignment = eye.getExtendedFacing();
        final String piece = MTEEyeOfHarmonyBTS.PIECE_CORE;
        final String[][] shape = BTSShapes.load(piece);
        final int[] off = BTSShapes.offset(piece);
        int n = 0;
        for (int c = 0; c < shape.length; c++) {
            for (int b = 0; b < shape[c].length; b++) {
                final String row = shape[c][b];
                for (int a = 0; a < row.length(); a++) {
                    if (row.charAt(a) != 'W') continue;
                    final int[] p = worldPos(
                        alignment,
                        base.getXCoord(),
                        base.getYCoord(),
                        base.getZCoord(),
                        new int[] { a - off[0], b - off[1], c - off[2] });
                    world.setBlock(p[0], p[1], p[2], TTCasingsContainer.SpacetimeCompressionFieldGenerators, meta, 2);
                    n++;
                    if (!all) {
                        eye.checkStructure(true, base);
                        return n;
                    }
                }
            }
        }
        eye.checkStructure(true, base);
        return n;
    }

    private static Block generatorFor(char ch) {
        switch (ch) {
            case 'W':
                return TTCasingsContainer.SpacetimeCompressionFieldGenerators;
            case 'X':
                return TTCasingsContainer.TimeAccelerationFieldGenerator;
            case 'Y':
                return TTCasingsContainer.StabilisationFieldGenerators;
            default:
                return null;
        }
    }

    /** Places the hatch {@code item} at the socket, facing away from the Eye's centre. */
    private static boolean fit(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel, ItemList item) {
        if (item == null || !item.hasBeenSet()) return false;
        return fitStack(world, alignment, x, y, z, rel, item.get(1L));
    }

    /** Places the GT machine {@code stack} (a hatch's item) at the socket, facing away from the Eye's centre. */
    /** The kit's Directive Hatches, in Sensor-socket order (code output). */
    private static final DirectiveSet[] DIRECTIVE_SETS = { DirectiveSet.EYE_TRIMS, DirectiveSet.EYE_STATUS,
        DirectiveSet.EYE_TRANSITION };

    /** A Directive Hatch in the socket, set to {@code set} with code output. */
    private static boolean fitDirective(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel,
        DirectiveSet set) {
        if (!fitStack(world, alignment, x, y, z, rel, BTSMachines.directiveHatch)) return false;
        final int[] p = worldPos(alignment, x, y, z, rel);
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (te instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEHatchDirective) {
            final MTEHatchDirective h = (MTEHatchDirective) ((IGregTechTileEntity) te).getMetaTileEntity();
            h.setSet(set);
            h.setOutput(DirectiveSet.Output.CODE);
        }
        return true;
    }

    private static boolean fitStack(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel,
        ItemStack stack) {
        if (stack == null) return false;
        final int[] p = worldPos(alignment, x, y, z, rel);
        return MTEReactorCoreMk1.placeGtMachine(
            world,
            p[0],
            p[1],
            p[2],
            (short) stack.getItemDamage(),
            outward(world, alignment, x, y, z, p));
    }

    private static int[] worldPos(ExtendedFacing alignment, int x, int y, int z, int[] rel) {
        final int[] xyz = new int[3];
        alignment.getWorldOffset(rel, xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }

    /** The air-facing side of a socket that points most directly away from the Eye's centre. */
    private static ForgeDirection outward(World world, ExtendedFacing alignment, int x, int y, int z, int[] p) {
        final int[] c = worldPos(alignment, x, y, z, CENTRE_REL);
        ForgeDirection best = alignment.getDirection();
        double bestDot = Double.NEGATIVE_INFINITY;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (!world.isAirBlock(p[0] + d.offsetX, p[1] + d.offsetY, p[2] + d.offsetZ)) continue;
            final double dot = d.offsetX * (p[0] - c[0]) + d.offsetY * (p[1] - c[1]) + d.offsetZ * (p[2] - c[2]);
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }

    // ================================================================================ helpers

    private static void give(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.entityDropItem(stack, 0.5F);
        }
    }

    private static void giveMany(EntityPlayerMP player, ItemStack one, int count) {
        if (one == null || count <= 0) return;
        final int max = Math.max(1, one.getMaxStackSize());
        int left = count;
        while (left > 0) {
            final ItemStack stack = one.copy();
            stack.stackSize = Math.min(max, left);
            give(player, stack);
            left -= stack.stackSize;
        }
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    private static String qty(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
