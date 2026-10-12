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
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
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
 * SINGULARITY LADDER P2: the Kerr's creative test path. {@code /btskit kerr} hands over the parts
 * and prints the bill of materials; {@code /btskerr} raises a complete, formed, running Kerr in front
 * of the player. Owned by the Kerr unit.
 * <p>
 * <b>No count here is typed by hand (D-22).</b> The rails, the bill of materials and every socket
 * position come from the exported shapes ({@link BTSShapes}) and {@code manifest.json}, the same
 * files the controller walks, so the kit and the machine cannot disagree.
 */
public final class KerrKit {

    private KerrKit() {}

    /** Voxel-space centre of the cage, as a/b/c offset from the controller (controller at z 38). */
    private static final int[] CAGE_CENTRE_REL = { 0, 0, 38 };

    // ===================================================================================== kit

    /** {@code /btskit kerr}: controller, module cores, rails, bolts, seeds, the upgrade, hatches. */
    public static void giveKit(EntityPlayerMP player) {
        final int rails = BTSShapes.count(MTEKerrEngine.PIECE_T1, 'E');
        give(player, BTSMachines.kerrEngine == null ? null : BTSMachines.kerrEngine.copy());
        give(player, SLBlocks.casing2(BlockSLCasing2.COMPRESSOR_MODULE, 1));
        give(player, SLBlocks.casing2(BlockSLCasing2.EXTRACTOR_MODULE, 1));
        giveMany(player, SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 1), rails);
        give(player, new ItemStack(SLItems.kerrDensityUpgrade, 1));
        give(player, ItemList.Black_Hole_Opener.get(2L));
        give(player, ItemList.Black_Hole_Closer.get(1L));
        for (int i = 0; i < 4; i++) {
            give(player, GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 64L));
            give(player, GTOreDictUnificator.get(OrePrefixes.bolt, Materials.Neutronium, 64L));
        }
        giveMany(player, ItemList.Hatch_Input_Bus_MAX.get(1L), socketCount(MTEKerrEngine.PIECE_T1, 'b'));
        giveMany(player, ItemList.Hatch_Energy_UIV.get(1L), socketCount(MTEKerrEngine.PIECE_T1, 'e'));
        give(player, ItemList.Hatch_AutoMaintenance.get(1L));
        // compressor arc: 2 input buses, 2 input hatches, 1 output bus, 1 output hatch;
        // extractor arc (item buses only): 1 input bus, 1 output bus
        giveMany(player, ItemList.Hatch_Input_Bus_MAX.get(1L), 3);
        giveMany(player, ItemList.Hatch_Input_MAX.get(1L), 2);
        giveMany(player, ItemList.Hatch_Output_Bus_MAX.get(1L), 2);
        give(player, ItemList.Hatch_Output_MAX.get(1L));
        give(player, machine(MetaTileEntityIDs.HATCH_BLACK_HOLE_UTILITY.ID));
        player.inventoryContainer.detectAndSendChanges();

        tell(player, EnumChatFormatting.AQUA + "== Kerr Ergosphere Engine test kit ==");
        tell(
            player,
            "Given: the controller, both module cores, " + qty(rails)
                + " Bolt Injector Rails (Mk I), a Density Upgrade, 2 seeds + a collapser, bolts, and hatches.");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        for (String piece : new String[] { MTEKerrEngine.PIECE_T1, MTEKerrEngine.PIECE_T2,
            MTEKerrEngine.PIECE_COMPRESSOR, MTEKerrEngine.PIECE_EXTRACTOR }) {
            tell(player, " - " + piece + ": " + bill(piece));
        }
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Fastest look: /btskerr (T1) or /btskerr t2 raises a complete, formed Kerr in front of you. "
                + "Or place the controller and use a hologram projector: stack 1 = T1, 2 = T2 (both modules always).");
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
        return MTEKerrEngine.socketRels(piece, ch).length;
    }

    // ========================================================================= creative build

    /** Result of one {@link #build}: whether it formed, and the lines to print. */
    public static final class Result {

        public boolean formed;
        public String failure;
        public final List<String> lines = new ArrayList<>();
    }

    /**
     * Raises a complete Kerr with its controller at (x, y, z) facing {@code facing}: every block of
     * the core piece and both modules, a hatch in every socket the machine needs (creative energy,
     * auto-maintenance, bolt buses stocked with a seed and bolts, module I/O), the Density Upgrade
     * when {@code tier2}, then forms it. Server side only.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, boolean tier2) {
        return build(world, x, y, z, facing, tier2, false);
    }

    /**
     * As {@link #build(World, int, int, int, ForgeDirection, boolean)}; with {@code survival} the blocks
     * go in through the controller's {@code survivalConstruct} (the hologram projector's survival
     * path, projector-sized rounds, from an endless item source) instead of the creative
     * {@code construct}. Either way the hatches are then fitted and the machine formed.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, boolean tier2,
        boolean survival) {
        final Result result = new Result();
        final ItemStack controller = BTSMachines.kerrEngine;
        if (controller == null) {
            result.failure = "the Kerr controller is not registered";
            return result;
        }
        final short controllerId = (short) controller.getItemDamage();
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, controllerId, facing)) {
            result.failure = "could not place the controller";
            return result;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEKerrEngine)) {
            result.failure = "the controller did not initialise";
            return result;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEKerrEngine kerr = (MTEKerrEngine) base.getMetaTileEntity();

        // 1. Every block: the core (stack size picks T1/T2) and both modules.
        final ItemStack trigger = new ItemStack(controller.getItem(), tier2 ? 2 : 1, controller.getItemDamage());
        if (survival) {
            final SLConsoleTest.SurvivalRun run = SLConsoleTest
                .survivalBuild(kerr, world, trigger, SURVIVAL_BUDGET, SURVIVAL_MAX_ROUNDS);
            result.lines.add(
                "Survival auto-place: " + qty(run.placed)
                    + " blocks in "
                    + run.rounds
                    + " rounds, "
                    + (run.finished ? "finished (-1)" : run.stalled ? "STALLED (0 placed twice)" : "round cap hit"));
        } else {
            kerr.construct(trigger, false);
        }

        // 2. A hatch in every socket the walk demands, facing out of the cage.
        final ExtendedFacing alignment = kerr.getExtendedFacing();
        final String core = tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1;
        int fitted = 0;
        for (int[] rel : MTEKerrEngine.socketRels(core, 'e')) {
            if (fit(world, alignment, x, y, z, rel, ItemList.DebugEnergyHatch)) {
                fitted++;
                final MTEHatchEnergyDebug hatch = mteAt(world, alignment, x, y, z, rel, MTEHatchEnergyDebug.class);
                if (hatch != null) {
                    hatch.setVoltageTier(12); // UMV: DENSIFY is a UMV recipe
                    hatch.setAmperage(16);
                }
            }
        }
        for (int[] rel : MTEKerrEngine.socketRels(core, 'm')) {
            if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_AutoMaintenance)) fitted++;
        }
        boolean seeded = false;
        for (int[] rel : MTEKerrEngine.socketRels(core, 'b')) {
            if (!fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_Bus_MAX)) continue;
            fitted++;
            final MTEHatchInputBus bus = mteAt(world, alignment, x, y, z, rel, MTEHatchInputBus.class);
            if (bus != null) {
                stockBoltBus(bus, !seeded);
                seeded = true;
            }
        }
        final ItemList[] compressorIO = { ItemList.Hatch_Input_Bus_MAX, ItemList.Hatch_Input_Bus_MAX,
            ItemList.Hatch_Input_MAX, ItemList.Hatch_Input_MAX, ItemList.Hatch_Output_Bus_MAX,
            ItemList.Hatch_Output_MAX };
        final int[][] cSockets = MTEKerrEngine.socketRels(MTEKerrEngine.PIECE_COMPRESSOR, 'c');
        for (int i = 0; i < cSockets.length && i < compressorIO.length; i++) {
            if (fit(world, alignment, x, y, z, cSockets[i], compressorIO[i])) fitted++;
        }
        final ItemList[] extractorIO = { ItemList.Hatch_Input_Bus_MAX, ItemList.Hatch_Output_Bus_MAX };
        final int[][] xSockets = MTEKerrEngine.socketRels(MTEKerrEngine.PIECE_EXTRACTOR, 'x');
        for (int i = 0; i < xSockets.length && i < extractorIO.length; i++) {
            if (fit(world, alignment, x, y, z, xSockets[i], extractorIO[i])) fitted++;
        }

        // 3. The upgrade, a sensible setpoint, then form.
        if (tier2) kerr.installDensity();
        kerr.debugSetpoint(tier2 ? 128 : 16);
        kerr.setMachineMode(MTEKerrEngine.MODE_COMPRESS);
        kerr.checkStructure(true, base);
        kerr.fixAllIssues();
        base.enableWorking();

        result.formed = kerr.mMachine;
        result.lines.add(
            (result.formed ? EnumChatFormatting.GREEN + "Kerr " + (tier2 ? "T2" : "T1") + " FORMED"
                : EnumChatFormatting.RED + "Kerr " + (tier2 ? "T2" : "T1") + " NOT FORMED") + EnumChatFormatting.RESET
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
                + " hatches fitted.");
        result.lines.addAll(kerr.statusLines());
        return result;
    }

    /** The projector's default survival round (StructureLib's 25 elements, which survivalBudget scales). */
    private static final int SURVIVAL_BUDGET = 25;
    private static final int SURVIVAL_MAX_ROUNDS = 2000;

    /**
     * Console/command-block checks on a Kerr {@link #build} raised: the first wrong cells if it did
     * not form, the mirror check (the extractor arc is the mockup's +x side, so it must stand on the
     * viewer's right, seen from in front of the controller), and a creative re-projection over the
     * finished machine, which must leave it formed (no hatch paved over).
     */
    public static List<String> diagnose(World world, int x, int y, int z, ForgeDirection facing, boolean tier2) {
        final List<String> out = new ArrayList<>();
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEKerrEngine)) {
            out.add("No Kerr controller at the build point.");
            return out;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEKerrEngine kerr = (MTEKerrEngine) base.getMetaTileEntity();
        final ExtendedFacing alignment = kerr.getExtendedFacing();
        if (!kerr.mMachine) {
            out.add("First wrong cells:");
            for (String line : SLConsoleTest.firstErrors(kerr, base, 3)) {
                out.add("  " + line);
            }
            kerr.checkStructure(true, base);
        }

        // Mirror check, read off the world: where the extractor core block actually stands.
        final int[] rel = MTEKerrEngine.socketRel(MTEKerrEngine.PIECE_EXTRACTOR, 'k', 0);
        if (rel != null) {
            final int[] p = SLConsoleTest.worldPos(alignment, x, y, z, rel);
            final boolean there = world.getBlock(p[0], p[1], p[2]) == SLBlocks.slCasing2
                && world.getBlockMetadata(p[0], p[1], p[2]) == BlockSLCasing2.EXTRACTOR_MODULE;
            final String side = SLConsoleTest.viewerSide(facing, x, z, p[0], p[2]);
            out.add(
                "Orientation: extractor core at (" + p[0]
                    + ", "
                    + p[1]
                    + ", "
                    + p[2]
                    + ") "
                    + (there ? "present" : "MISSING (holds " + SLConsoleTest.blockAt(world, p[0], p[1], p[2]) + ")")
                    + ", on the viewer's "
                    + side
                    + (side.equals("right") ? " (matches the mockup)"
                        : " (MIRRORED vs the mockup: re-export --flipH)"));
        }

        // Re-project the creative hologram over the formed machine: nothing may break.
        if (kerr.mMachine) {
            final ItemStack controller = BTSMachines.kerrEngine;
            kerr.construct(new ItemStack(controller.getItem(), tier2 ? 2 : 1, controller.getItemDamage()), false);
            kerr.checkStructure(true, base);
            out.add(
                "Creative re-projection over the formed Kerr: " + (kerr.mMachine ? "still FORMED"
                    : "BROKE IT - " + String.join("; ", SLConsoleTest.firstErrors(kerr, base, 2))));
            final int again = SLConsoleTest.survivalBuild(
                kerr,
                world,
                new ItemStack(controller.getItem(), tier2 ? 2 : 1, controller.getItemDamage()),
                SURVIVAL_BUDGET,
                5).placed;
            kerr.checkStructure(true, base);
            out.add(
                "Survival re-projection: placed " + again
                    + " blocks, "
                    + (kerr.mMachine ? "still FORMED" : "BROKE IT"));
        }
        out.addAll(kerr.statusLines());
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

    /** Places the hatch {@code item} at the socket, facing out of the cage. */
    private static boolean fit(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel, ItemList item) {
        if (item == null || !item.hasBeenSet()) return false;
        final ItemStack stack = item.get(1L);
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

    private static <T> T mteAt(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel, Class<T> type) {
        final int[] p = worldPos(alignment, x, y, z, rel);
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(mte) ? type.cast(mte) : null;
    }

    private static int[] worldPos(ExtendedFacing alignment, int x, int y, int z, int[] rel) {
        final int[] xyz = new int[3];
        alignment.getWorldOffset(rel, xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }

    /** The air-facing side of a socket that points most directly away from the cage centre. */
    private static ForgeDirection outward(World world, ExtendedFacing alignment, int x, int y, int z, int[] p) {
        final int[] c = worldPos(alignment, x, y, z, CAGE_CENTRE_REL);
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
}
