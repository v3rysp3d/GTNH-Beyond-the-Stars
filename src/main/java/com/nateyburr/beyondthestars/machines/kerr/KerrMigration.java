package com.nateyburr.beyondthestars.machines.kerr;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * KERR REDESIGN: converts a pre-redesign Kerr (one controller, module CORE BLOCKS in the arcs) into
 * the KEE + module machines (docs/kerr-redesign.md §7). Server side, idempotent: it acts only where a
 * legacy core block still stands.
 * <ul>
 * <li><b>Compressor:</b> the core block's cell is an I/O socket now, so it becomes Background
 * Radiation Absorbent Casing; the module controller goes into the arc's outer shell cell below it
 * (the slot cell), facing out of the cage.</li>
 * <li><b>Extractor:</b> the core block's cell (inner face) becomes Ergosphere Extraction Casing; the
 * module controller goes into the outer shell cell on the 340 degree ray.</li>
 * </ul>
 * A controller cell is replaced only if it is air or the plain arc shell it held before the redesign
 * (never a tile). The hatches already in the arc sockets stay and become the module's own on its
 * first walk. A recipe the old KEE had in flight is handed to the module it was running on.
 * <p>
 * Runs automatically once per load on the KEE (after its first structure check), and on demand from
 * {@code /btskerr migrate}.
 */
public final class KerrMigration {

    private KerrMigration() {}

    /**
     * Converts the Kerr whose controller is {@code kee}. Returns a one-line summary for the GUI and
     * chat, or null when there was nothing to convert.
     */
    public static String convert(MTEKerrEngine kee) {
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null || !base.isServerSide()) return null;
        final World world = base.getWorld();
        final int x = base.getXCoord();
        final int y = base.getYCoord();
        final int z = base.getZCoord();
        final ExtendedFacing facing = kee.getExtendedFacing();
        final List<String> notes = new ArrayList<>();
        int converted = 0;
        for (KerrSlot slot : KerrSlot.VALUES) {
            final int[] core = KerrSlot.worldPos(facing, x, y, z, slot.legacyCoreRel());
            if (!world.blockExists(core[0], core[1], core[2])) continue;
            if (world.getBlock(core[0], core[1], core[2]) != SLBlocks.slCasing2
                || world.getBlockMetadata(core[0], core[1], core[2]) != slot.legacyCoreMeta) continue;
            final int[] ctrl = slot.modulePos(facing, x, y, z);
            final IGregTechTileEntity there = KerrSlot.gtTileAt(world, ctrl);
            final boolean already = there != null && slot.accepts(there.getMetaTileEntity());
            if (!already) {
                if (!world.isAirBlock(ctrl[0], ctrl[1], ctrl[2])
                    && !slot.isPlainShell(world, ctrl[0], ctrl[1], ctrl[2])) {
                    notes.add(slot.label + " NOT converted: its controller cell " + pos(ctrl) + " is occupied");
                    continue;
                }
                final ItemStack controller = slot.controllerStack();
                if (controller == null || !MTEReactorCoreMk1.placeGtMachine(
                    world,
                    ctrl[0],
                    ctrl[1],
                    ctrl[2],
                    (short) controller.getItemDamage(),
                    slot.moduleFacing(facing))) {
                    notes.add(slot.label + " NOT converted: the controller could not be placed at " + pos(ctrl));
                    continue;
                }
            }
            slot.placeLegacyCellCasing(world, core[0], core[1], core[2]);
            converted++;
            BeyondTheStars.LOG.info(
                "[BTS] Kerr migration: {} module core block at {} -> {} module controller at {}",
                slot.label,
                pos(core),
                slot.label,
                pos(ctrl));
        }
        final String handOff = handOffLegacyRecipe(kee, world, facing, x, y, z);
        if (handOff != null) notes.add(handOff);
        if (converted == 0 && notes.isEmpty()) return null;
        final String summary = "Kerr migration: converted " + converted
            + " module"
            + (converted == 1 ? "" : "s")
            + (notes.isEmpty() ? "" : "; " + String.join("; ", notes));
        BeyondTheStars.LOG.info("[BTS] {} (KEE at {})", summary, pos(new int[] { x, y, z }));
        return summary;
    }

    /** True once every slot's legacy core cell and controller cell is in a loaded chunk. */
    static boolean cellsLoaded(MTEKerrEngine kee) {
        final IGregTechTileEntity base = kee.getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return false;
        final World world = base.getWorld();
        final ExtendedFacing facing = kee.getExtendedFacing();
        for (KerrSlot slot : KerrSlot.VALUES) {
            final int[] core = KerrSlot
                .worldPos(facing, base.getXCoord(), base.getYCoord(), base.getZCoord(), slot.legacyCoreRel());
            final int[] ctrl = slot.modulePos(facing, base.getXCoord(), base.getYCoord(), base.getZCoord());
            if (!world.blockExists(core[0], core[1], core[2]) || !world.blockExists(ctrl[0], ctrl[1], ctrl[2])) {
                return false;
            }
        }
        return true;
    }

    /** The old KEE's in-flight outputs go to the module they were made on (else drop at the KEE). */
    private static String handOffLegacyRecipe(MTEKerrEngine kee, World world, ExtendedFacing facing, int x, int y,
        int z) {
        final int mode = kee.legacyBoundMode();
        final Object[] legacy = kee.takeLegacyRecipe();
        if (legacy == null) return null;
        final KerrSlot slot = mode == 1 ? KerrSlot.EXTRACTOR : KerrSlot.COMPRESSOR;
        final IGregTechTileEntity te = KerrSlot.gtTileAt(world, slot.modulePos(facing, x, y, z));
        if (te != null && slot.accepts(te.getMetaTileEntity())) {
            final MTEKerrModuleBase<?> module = (MTEKerrModuleBase<?>) te.getMetaTileEntity();
            module.checkStructure(true, te);
            return "the old in-flight recipe went to the " + slot.label
                + " module ("
                + module.acceptLegacyOutputs(MTEKerrEngine.items(legacy), MTEKerrEngine.fluids(legacy))
                + ")";
        }
        int dropped = 0;
        final ItemStack[] items = MTEKerrEngine.items(legacy);
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack == null || stack.stackSize <= 0) continue;
                world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 1.5, z + 0.5, stack.copy()));
                dropped++;
            }
        }
        return "the old in-flight recipe had no module to go to: " + dropped + " stack(s) dropped at the KEE";
    }

    private static String pos(int[] p) {
        return "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }
}
