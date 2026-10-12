package com.nateyburr.beyondthestars.machines.kerr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.GTOreDictUnificator;

/**
 * SINGULARITY LADDER P2 / KERR REDESIGN: the Kerr's creative test path. {@code /btskit kerr} hands
 * over the parts and prints the bills of materials; {@code /btskerr} raises a complete Kerr in front
 * of the player: the KEE (hole only) AND both module machines, each module controller placed in its
 * slot cell facing out of the cage, every machine formed and linked.
 * <p>
 * <b>No count or position here is typed by hand (D-22).</b> Rails, bills, sockets and the module slots
 * come from the exported shapes ({@link BTSShapes}) and {@code manifest.json} (the slot table, see
 * {@link KerrSlot}), the same files the machines walk.
 */
public final class KerrKit {

    private KerrKit() {}

    // ===================================================================================== kit

    /** {@code /btskit kerr}: KEE + module controllers, rails, bolts, seeds, the upgrade, hatches. */
    public static void giveKit(EntityPlayerMP player) {
        final int rails = BTSShapes.count(MTEKerrEngine.PIECE_T1, 'E');
        give(player, copy(BTSMachines.kerrEngine));
        give(player, copy(BTSMachines.kerrCompressorModule));
        give(player, copy(BTSMachines.kerrExtractorModule));
        giveMany(player, SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 1), rails);
        give(player, new ItemStack(SLItems.kerrDensityUpgrade, 1));
        give(player, ItemList.Black_Hole_Opener.get(2L));
        give(player, ItemList.Black_Hole_Closer.get(1L));
        for (int i = 0; i < 4; i++) {
            give(player, GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 64L));
            give(player, GTOreDictUnificator.get(OrePrefixes.bolt, Materials.Neutronium, 64L));
        }
        // KEE: bolt buses, energy (containment power), maintenance, a utility hatch
        giveMany(player, ItemList.Hatch_Input_Bus_MAX.get(1L), socketCount(MTEKerrEngine.PIECE_T1, 'b'));
        giveMany(player, ItemList.Hatch_Energy_UIV.get(1L), socketCount(MTEKerrEngine.PIECE_T1, 'e'));
        give(player, ItemList.Hatch_AutoMaintenance.get(1L));
        give(player, machine(MetaTileEntityIDs.HATCH_BLACK_HOLE_UTILITY.ID));
        // modules (their own hatches): compressor in bus + in hatch + out bus + out hatch + energy;
        // extractor in bus + out bus + energy
        giveMany(player, ItemList.Hatch_Input_Bus_MAX.get(1L), 2);
        give(player, ItemList.Hatch_Input_MAX.get(1L));
        giveMany(player, ItemList.Hatch_Output_Bus_MAX.get(1L), 2);
        give(player, ItemList.Hatch_Output_MAX.get(1L));
        giveMany(player, ItemList.Hatch_Energy_UIV.get(1L), 2);
        player.inventoryContainer.detectAndSendChanges();

        tell(player, EnumChatFormatting.AQUA + "== Kerr Ergosphere Engine test kit ==");
        tell(
            player,
            "Given: the KEE controller, both module controllers, " + qty(rails)
                + " Bolt Injector Rails (Mk I), a Density Upgrade, 2 seeds + a collapser, bolts, and hatches.");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        for (String piece : new String[] { MTEKerrEngine.PIECE_T1, MTEKerrEngine.PIECE_T2,
            MTEKerrEngine.PIECE_COMPRESSOR, MTEKerrEngine.PIECE_EXTRACTOR }) {
            tell(player, " - " + piece + ": " + bill(piece));
        }
        tell(
            player,
            EnumChatFormatting.GREEN + "Fastest look: /btskerr (T1) or /btskerr t2 raises a complete Kerr: the KEE "
                + "and both modules, formed and linked. By hand: place the KEE, project it (stack 1 = T1, 2 = T2); "
                + "markers 7 / 8 show where each module controller goes, facing out; then project each module.");
    }

    /** "13,899 blocks: 3,551x Mirror Backing Casing, ..." from the manifest's bill of materials. */
    private static String bill(String piece) {
        final JsonObject entry = BTSShapes.manifestPiece(piece);
        if (entry == null || !entry.has("bom")) return "(not in manifest)";
        final StringBuilder sb = new StringBuilder();
        final JsonArray bom = entry.getAsJsonArray("bom");
        for (JsonElement el : bom) {
            final JsonObject row = el.getAsJsonObject();
            final String ch = row.get("ch")
                .getAsString();
            if ("~".equals(ch)) continue;
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

    /** The manifest's block text, cut at the first explanatory parenthesis/comma. */
    private static String shortBlock(String text) {
        String s = text.replace("NEW ", "");
        final int paren = s.indexOf(" (");
        if (paren > 0) s = s.substring(0, paren);
        return s;
    }

    private static int socketCount(String piece, char ch) {
        return socketRels(piece, ch).length;
    }

    /**
     * Every socket of one char in a piece, as (a, b, c) offsets from THAT piece's controller
     * ({@code sockets.<ch>[i].rel} in {@code manifest.json}). For a module piece the controller is the
     * module's own. Empty if none.
     */
    public static int[][] socketRels(String piece, char ch) {
        try {
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
        } catch (RuntimeException bad) {
            return new int[0][];
        }
    }

    // ========================================================================= creative build

    /** Result of one {@link #build}: whether everything formed, and the lines to print. */
    public static final class Result {

        public boolean formed;
        public String failure;
        public final List<String> lines = new ArrayList<>();
    }

    public static Result build(World world, int x, int y, int z, ForgeDirection facing, boolean tier2) {
        return build(world, x, y, z, facing, tier2, false);
    }

    /**
     * Raises a complete Kerr with the KEE controller at (x, y, z) facing {@code facing}: the cage
     * (T1, or T2 with the Density Upgrade), a hatch in every KEE socket (creative energy,
     * auto-maintenance, bolt buses stocked with a seed and bolts), then each module: its controller in
     * its slot cell facing out of the cage, its arc, its own hatches (input / output and creative
     * energy). Every machine is formed; the modules link once the hole opens (about a second). With
     * {@code survival} the blocks go in through each controller's {@code survivalConstruct} (the
     * projector's survival path) instead of the creative {@code construct}. Server side only.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, boolean tier2,
        boolean survival) {
        final Result result = new Result();
        final ItemStack controller = BTSMachines.kerrEngine;
        if (controller == null) {
            result.failure = "the Kerr controller is not registered";
            return result;
        }
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) controller.getItemDamage(), facing)) {
            result.failure = "could not place the KEE controller";
            return result;
        }
        final MTEKerrEngine kee = mteAt(world, new int[] { x, y, z }, MTEKerrEngine.class);
        if (kee == null) {
            result.failure = "the KEE controller did not initialise";
            return result;
        }
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();

        // 1. The cage (stack size picks T1/T2).
        final ItemStack trigger = new ItemStack(controller.getItem(), tier2 ? 2 : 1, controller.getItemDamage());
        placeBlocks(kee, world, trigger, survival, "KEE", result);

        // 2. A hatch in every KEE socket, facing out of the cage.
        final ExtendedFacing alignment = kee.getExtendedFacing();
        final int[] centre = KerrSlot.worldPos(alignment, x, y, z, KerrSlot.centreRel());
        final String core = tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1;
        int fitted = 0;
        for (int[] rel : socketRels(core, 'e')) {
            if (fitDebugEnergy(world, alignment, x, y, z, rel, centre)) fitted++;
        }
        for (int[] rel : socketRels(core, 'm')) {
            if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_AutoMaintenance, centre)) fitted++;
        }
        boolean seeded = false;
        for (int[] rel : socketRels(core, 'b')) {
            if (!fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_Bus_MAX, centre)) continue;
            fitted++;
            final MTEHatchInputBus bus = mteAt(
                world,
                KerrSlot.worldPos(alignment, x, y, z, rel),
                MTEHatchInputBus.class);
            if (bus != null) {
                stockBoltBus(bus, !seeded);
                seeded = true;
            }
        }

        // 3. The upgrade, a sensible setpoint, then form the KEE.
        if (tier2) kee.installDensity();
        kee.debugSetpoint(tier2 ? 128 : 16);
        kee.checkStructure(true, base);
        kee.fixAllIssues();
        base.enableWorking();
        result.lines.add(
            (kee.mMachine ? EnumChatFormatting.GREEN + "KEE " + (tier2 ? "T2" : "T1") + " FORMED"
                : EnumChatFormatting.RED + "KEE " + (tier2 ? "T2" : "T1") + " NOT FORMED") + EnumChatFormatting.RESET
                + " at "
                + pos(x, y, z)
                + ", facing "
                + facing.name()
                + "; "
                + fitted
                + " hatches fitted.");

        // 4. Each module machine in its slot.
        boolean allFormed = kee.mMachine;
        for (KerrSlot slot : KerrSlot.VALUES) {
            allFormed &= buildModule(world, kee, slot, survival, centre, result);
        }
        result.formed = allFormed;
        result.lines.addAll(kee.statusLines());
        return result;
    }

    /** Places, builds, fits and forms one module machine in its slot. True if it formed. */
    private static boolean buildModule(World world, MTEKerrEngine kee, KerrSlot slot, boolean survival, int[] centre,
        Result result) {
        final IGregTechTileEntity keeBase = kee.getBaseMetaTileEntity();
        final ExtendedFacing keeFacing = kee.getExtendedFacing();
        final int[] p = slot.modulePos(keeFacing, keeBase.getXCoord(), keeBase.getYCoord(), keeBase.getZCoord());
        final ForgeDirection facing = slot.moduleFacing(keeFacing);
        final ItemStack stack = slot.controllerStack();
        if (stack == null) {
            result.lines.add(EnumChatFormatting.RED + slot.label + " module: controller not registered");
            return false;
        }
        if (!MTEReactorCoreMk1.placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), facing)) {
            result.lines
                .add(EnumChatFormatting.RED + slot.label + " module: could not place its controller at " + pos(p));
            return false;
        }
        final MTEKerrModuleBase<?> module = mteAt(world, p, MTEKerrModuleBase.class);
        if (module == null) {
            result.lines.add(EnumChatFormatting.RED + slot.label + " module: controller did not initialise");
            return false;
        }
        placeBlocks(
            module,
            world,
            new ItemStack(stack.getItem(), 1, stack.getItemDamage()),
            survival,
            slot.label,
            result);
        final ExtendedFacing alignment = module.getExtendedFacing();
        final char ch = slot == KerrSlot.COMPRESSOR ? 'c' : 'x';
        final int[][] sockets = socketRels(slot.piece, ch);
        // compressor: in bus, in hatch, out bus, out hatch, energy; extractor: in bus, out bus, energy
        final ItemList[] plan = slot == KerrSlot.COMPRESSOR
            ? new ItemList[] { ItemList.Hatch_Input_Bus_MAX, ItemList.Hatch_Input_MAX, ItemList.Hatch_Output_Bus_MAX,
                ItemList.Hatch_Output_MAX, null }
            : new ItemList[] { ItemList.Hatch_Input_Bus_MAX, ItemList.Hatch_Output_Bus_MAX, null };
        int fitted = 0;
        for (int i = 0; i < sockets.length && i < plan.length; i++) {
            final boolean ok = plan[i] == null ? fitDebugEnergy(world, alignment, p[0], p[1], p[2], sockets[i], centre)
                : fit(world, alignment, p[0], p[1], p[2], sockets[i], plan[i], centre);
            if (ok) fitted++;
        }
        final IGregTechTileEntity base = module.getBaseMetaTileEntity();
        module.checkStructure(true, base);
        module.refreshLink();
        base.enableWorking();
        result.lines.add(
            (module.mMachine ? EnumChatFormatting.GREEN + slot.label + " module FORMED"
                : EnumChatFormatting.RED + slot.label + " module NOT FORMED") + EnumChatFormatting.RESET
                + " at "
                + pos(p)
                + ", facing "
                + facing.name()
                + "; "
                + fitted
                + " hatches; link: "
                + module.getLink().text);
        return module.mMachine;
    }

    /** Creative {@code construct}, or the projector's survival path from an endless source. */
    private static void placeBlocks(ISurvivalConstructable m, World world, ItemStack trigger, boolean survival,
        String label, Result result) {
        if (survival) {
            final SLConsoleTest.SurvivalRun run = SLConsoleTest
                .survivalBuild(m, world, trigger, SURVIVAL_BUDGET, SURVIVAL_MAX_ROUNDS);
            result.lines.add(
                label + " survival auto-place: "
                    + qty(run.placed)
                    + " blocks in "
                    + run.rounds
                    + " rounds, "
                    + (run.finished ? "finished (-1)" : run.stalled ? "STALLED (0 placed twice)" : "round cap hit"));
        } else {
            m.construct(trigger, false);
        }
    }

    /** The projector's default survival round (StructureLib's 25 elements, which survivalBudget scales). */
    private static final int SURVIVAL_BUDGET = 25;
    private static final int SURVIVAL_MAX_ROUNDS = 2000;

    /**
     * World bounds {minX, minY, minZ, maxX, maxY, maxZ} of a whole Kerr (the KEE core and both module
     * arcs, each in its own frame) for a KEE controller at (x, y, z) facing {@code facing}.
     */
    public static int[] assemblyBounds(ForgeDirection facing, int x, int y, int z, boolean tier2) {
        final ExtendedFacing kee = ExtendedFacing.of(facing);
        final int[] box = SLConsoleTest.bounds(kee, x, y, z, tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1);
        for (KerrSlot slot : KerrSlot.VALUES) {
            final int[] p = slot.modulePos(kee, x, y, z);
            final int[] m = SLConsoleTest
                .bounds(ExtendedFacing.of(slot.moduleFacing(kee)), p[0], p[1], p[2], slot.piece);
            for (int i = 0; i < 3; i++) {
                box[i] = Math.min(box[i], m[i]);
                box[i + 3] = Math.max(box[i + 3], m[i + 3]);
            }
        }
        return box;
    }

    // ============================================================================ diagnostics

    /**
     * Console checks on a Kerr {@link #build} raised: the KEE walk (first wrong cells), each module's
     * walk and link, the mirror check (the extractor module must stand on the viewer's right, seen
     * from in front of the KEE), a creative re-projection over every finished machine (nothing may
     * break), then the survival repair test on the KEE. With {@code full} false only the walks and
     * the status are reported (the {@code check} form).
     */
    public static List<String> diagnose(World world, int x, int y, int z, ForgeDirection facing, boolean tier2,
        boolean full) {
        final List<String> out = new ArrayList<>();
        final MTEKerrEngine kee = mteAt(world, new int[] { x, y, z }, MTEKerrEngine.class);
        if (kee == null) {
            out.add("No Kerr controller at the build point.");
            return out;
        }
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();
        final ExtendedFacing alignment = kee.getExtendedFacing();
        kee.checkStructure(true, base);
        out.add(
            "KEE at " + pos(x, y, z)
                + ": "
                + (kee.mMachine ? "FORMED" : "NOT FORMED")
                + (kee.isDensityInstalled() ? " (Density Upgrade installed)" : ""));
        if (!kee.mMachine) {
            out.add("First wrong cells:");
            for (String line : SLConsoleTest.firstErrors(kee, base, 3)) {
                out.add("  " + line);
            }
            kee.checkStructure(true, base);
        }
        for (KerrSlot slot : KerrSlot.VALUES) {
            out.addAll(diagnoseModule(world, kee, slot, facing));
        }
        if (!full) {
            out.addAll(kee.statusLines());
            return out;
        }

        // Re-project every creative hologram over the finished machines: nothing may break.
        if (kee.mMachine) {
            final ItemStack controller = BTSMachines.kerrEngine;
            final ItemStack trigger = new ItemStack(controller.getItem(), tier2 ? 2 : 1, controller.getItemDamage());
            kee.construct(trigger, false);
            kee.checkStructure(true, base);
            out.add(
                "Creative re-projection over the formed KEE: " + (kee.mMachine ? "still FORMED"
                    : "BROKE IT - " + String.join("; ", SLConsoleTest.firstErrors(kee, base, 2))));
            for (KerrSlot slot : KerrSlot.VALUES) {
                final MTEKerrModuleBase<?> module = mteAt(
                    world,
                    slot.modulePos(alignment, x, y, z),
                    MTEKerrModuleBase.class);
                if (module == null || !module.mMachine) continue;
                final ItemStack ms = slot.controllerStack();
                module.construct(new ItemStack(ms.getItem(), 1, ms.getItemDamage()), false);
                module.checkStructure(true, module.getBaseMetaTileEntity());
                out.add(
                    "Creative re-projection over the " + slot.label
                        + " module: "
                        + (module.mMachine ? "still FORMED" : "BROKE IT"));
            }
            if (kee.mMachine) {
                out.addAll(
                    SLConsoleTest.damageAndRepair(
                        kee,
                        base,
                        trigger,
                        SLConsoleTest.bounds(alignment, x, y, z, kee.corePiece())));
            }
        }
        out.addAll(kee.statusLines());
        return out;
    }

    /** One module's walk, link and (for the extractor) the mirror check. */
    private static List<String> diagnoseModule(World world, MTEKerrEngine kee, KerrSlot slot, ForgeDirection facing) {
        final List<String> out = new ArrayList<>();
        final IGregTechTileEntity keeBase = kee.getBaseMetaTileEntity();
        final int[] p = slot
            .modulePos(kee.getExtendedFacing(), keeBase.getXCoord(), keeBase.getYCoord(), keeBase.getZCoord());
        final MTEKerrModuleBase<?> module = mteAt(world, p, MTEKerrModuleBase.class);
        if (module == null || !slot.accepts(module)) {
            out.add(
                slot.label + " module: none at "
                    + pos(p)
                    + " (holds "
                    + SLConsoleTest.blockAt(world, p[0], p[1], p[2])
                    + ")");
            return out;
        }
        final IGregTechTileEntity mb = module.getBaseMetaTileEntity();
        module.checkStructure(true, mb);
        module.refreshLink();
        out.add(
            slot.label + " module at "
                + pos(p)
                + " facing "
                + mb.getFrontFacing()
                    .name()
                + ": "
                + (module.mMachine ? "FORMED" : "NOT FORMED")
                + ", link: "
                + module.getLink().text
                + (module.getLink() == MTEKerrModuleBase.Link.LINKED ? String
                    .format(Locale.ROOT, " (parallel %,d, speed x%.2f)", module.bonusParallel(), module.bonusSpeed())
                    : ""));
        if (!module.mMachine) {
            for (String line : SLConsoleTest.firstErrors(module, mb, 3)) {
                out.add("  " + line);
            }
            module.checkStructure(true, mb);
        }
        if (slot == KerrSlot.EXTRACTOR) {
            final String side = SLConsoleTest.viewerSide(facing, keeBase.getXCoord(), keeBase.getZCoord(), p[0], p[2]);
            out.add(
                "Orientation: the extractor module stands on the viewer's " + side
                    + (side.equals("right") ? " (matches the mockup)"
                        : " (MIRRORED vs the mockup: re-export --flipH)"));
        }
        return out;
    }

    /** Fills a bolt bus: optionally a Black Hole Seed first, then SpaceTime (else Neutronium) bolts. */
    private static void stockBoltBus(MTEHatchInputBus bus, boolean seed) {
        final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
        ItemStack bolt = GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 64L);
        if (bolt == null) bolt = GTOreDictUnificator.get(OrePrefixes.bolt, Materials.Neutronium, 64L);
        boolean seedPlaced = !seed;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            if (slot == bus.getCircuitSlot() || !bus.isValidSlot(slot)) continue;
            if (!seedPlaced) {
                inv.setInventorySlotContents(slot, ItemList.Black_Hole_Opener.get(1L));
                seedPlaced = true;
                continue;
            }
            if (bolt != null) inv.setInventorySlotContents(slot, bolt.copy());
        }
    }

    /** A creative energy hatch at UMV, 16 A (DENSITY is a UMV recipe; containment is 1 A UEV). */
    private static boolean fitDebugEnergy(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel,
        int[] centre) {
        if (!fit(world, alignment, x, y, z, rel, ItemList.DebugEnergyHatch, centre)) return false;
        final MTEHatchEnergyDebug hatch = mteAt(
            world,
            KerrSlot.worldPos(alignment, x, y, z, rel),
            MTEHatchEnergyDebug.class);
        if (hatch != null) {
            hatch.setVoltageTier(12);
            hatch.setAmperage(16);
        }
        return true;
    }

    /** Places the hatch {@code item} at the socket, facing out of the cage. */
    private static boolean fit(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel, ItemList item,
        int[] centre) {
        if (item == null || !item.hasBeenSet()) return false;
        final ItemStack stack = item.get(1L);
        if (stack == null) return false;
        final int[] p = KerrSlot.worldPos(alignment, x, y, z, rel);
        return MTEReactorCoreMk1.placeGtMachine(
            world,
            p[0],
            p[1],
            p[2],
            (short) stack.getItemDamage(),
            outward(world, alignment, p, centre));
    }

    private static <T> T mteAt(World world, int[] p, Class<T> type) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(mte) ? type.cast(mte) : null;
    }

    /** The air-facing side of a socket that points most directly away from the cage centre. */
    private static ForgeDirection outward(World world, ExtendedFacing alignment, int[] p, int[] c) {
        ForgeDirection best = alignment.getDirection();
        double bestDot = Double.NEGATIVE_INFINITY;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            final int nx = p[0] + d.offsetX;
            final int ny = p[1] + d.offsetY;
            final int nz = p[2] + d.offsetZ;
            if (!world.isAirBlock(nx, ny, nz)) continue;
            final double dot = d.offsetX * (p[0] - c[0]) + d.offsetY * (p[1] - c[1]) + d.offsetZ * (p[2] - c[2]);
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }

    // ================================================================================ helpers

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private static ItemStack machine(int id) {
        if (id <= 0 || GregTechAPI.METATILEENTITIES[id] == null) return null;
        return new ItemStack(GregTechAPI.sBlockMachines, 1, id);
    }

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

    static String pos(int x, int y, int z) {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    static String pos(int[] p) {
        return pos(p[0], p[1], p[2]);
    }
}
