package com.nateyburr.beyondthestars.machines.kerr;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;

import gregtech.api.casing.Casings;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * KERR REDESIGN: one module slot of the Kerr Ergosphere Engine's cage (docs/kerr-redesign.md §2.2).
 * <p>
 * A slot is a fixed cell relative to the KEE controller where one module machine's controller
 * stands, facing out of the cage. The geometry is not typed here: it is the exporter's slot table
 * ({@code manifest.json} {@code machines.kerr.slots}), so it can never disagree with the shapes.
 * <ul>
 * <li>{@code rel}: the module controller from the KEE controller, in the KEE's (a, b, c) frame;</li>
 * <li>{@code keeRel}: the KEE controller from the module controller, in the module's frame;</li>
 * <li>{@code facingAbc}: the module controller's front, as a unit vector in the KEE's frame;</li>
 * <li>{@code legacyCoreRel}: where the pre-redesign module core block stood (migration).</li>
 * </ul>
 * Nothing about a link is stored: both sides recompute the other's cell from this table and their
 * own {@link ExtendedFacing} (pull, never push), so a link cannot go stale across chunk unloads and
 * two KEEs side by side can never be confused.
 * <p>
 * <b>Adding a module</b> (the KEE does not change): a new arc anchor and slot in the exporter; one
 * entry here; a subclass of {@link MTEKerrModuleBase} naming its recipe maps, with an MTE id from
 * 28119 up; a controller recipe, textures and lang.
 */
public enum KerrSlot {

    /** The south arc: GT's compressor + neutronium-compressor maps, and DENSITY at Kerr T2. */
    COMPRESSOR("compressor", "Compressor", MTEKerrEngine.PIECE_COMPRESSOR, 7, BlockSLCasing2.COMPRESSOR_MODULE),
    /** The east arc: GT's item extractor map only (Q-9/Q-22). */
    EXTRACTOR("extractor", "Extractor", MTEKerrEngine.PIECE_EXTRACTOR, 8, BlockSLCasing2.EXTRACTOR_MODULE);

    public static final KerrSlot[] VALUES = values();

    /** The key in the manifest's slot table. */
    public final String key;
    /** Shown in GUIs and chat: "Compressor". */
    public final String label;
    /** The module machine's own structure piece. */
    public final String piece;
    /** The hint-dot count the KEE's hologram uses for this slot's marker. */
    public final int hintDots;
    /** The pre-redesign core block's {@code slCasing2} meta (migration). */
    public final int legacyCoreMeta;

    private volatile int[] rel;
    private volatile int[] keeRel;
    private volatile int[] facingAbc;
    private volatile int[] legacyCoreRel;

    KerrSlot(String key, String label, String piece, int hintDots, int legacyCoreMeta) {
        this.key = key;
        this.label = label;
        this.piece = piece;
        this.hintDots = hintDots;
        this.legacyCoreMeta = legacyCoreMeta;
    }

    /** The module controller item (null before machine registration). */
    public ItemStack controllerStack() {
        switch (this) {
            case COMPRESSOR:
                return BTSMachines.kerrCompressorModule;
            case EXTRACTOR:
                return BTSMachines.kerrExtractorModule;
            default:
                return null;
        }
    }

    /** True when {@code mte} is this slot's module machine. */
    public boolean accepts(IMetaTileEntity mte) {
        switch (this) {
            case COMPRESSOR:
                return mte instanceof MTEKerrCompressorModule;
            case EXTRACTOR:
                return mte instanceof MTEKerrExtractorModule;
            default:
                return false;
        }
    }

    /** The block the legacy core cell holds once migrated (it is a socket now; this is its fallback). */
    public void placeLegacyCellCasing(World world, int x, int y, int z) {
        if (this == COMPRESSOR) {
            final Block block = Casings.BackgroundRadiationAbsorbentCasing.getBlock();
            world.setBlock(x, y, z, block, Casings.BackgroundRadiationAbsorbentCasing.getBlockMeta(), 3);
        } else {
            world.setBlock(x, y, z, SLBlocks.slCasing, BlockSLCasing.EXTRACTION, 3);
        }
    }

    /** The plain casing a module controller cell held before the redesign (migration may replace it). */
    public boolean isPlainShell(World world, int x, int y, int z) {
        final Block block = world.getBlock(x, y, z);
        final int meta = world.getBlockMetadata(x, y, z);
        if (this == COMPRESSOR) {
            return block == Casings.BackgroundRadiationAbsorbentCasing.getBlock()
                && meta == Casings.BackgroundRadiationAbsorbentCasing.getBlockMeta();
        }
        return block == SLBlocks.slCasing && meta == BlockSLCasing.EXTRACTION;
    }

    // ============================================================================ geometry

    public int[] rel() {
        load();
        return rel.clone();
    }

    public int[] keeRel() {
        load();
        return keeRel.clone();
    }

    public int[] facingAbc() {
        load();
        return facingAbc.clone();
    }

    public int[] legacyCoreRel() {
        load();
        return legacyCoreRel.clone();
    }

    private void load() {
        if (legacyCoreRel != null) return;
        synchronized (KerrSlot.class) {
            if (legacyCoreRel != null) return;
            final JsonObject slots = BTSShapes.manifest()
                .getAsJsonObject("machines")
                .getAsJsonObject("kerr")
                .getAsJsonObject("slots");
            if (slots == null || !slots.has(key)) {
                throw new IllegalStateException("[BTS] manifest.json has no Kerr slot '" + key + "': re-export");
            }
            final JsonObject s = slots.getAsJsonObject(key);
            rel = vec(s, "rel");
            keeRel = vec(s, "keeRel");
            facingAbc = vec(s, "facingAbc");
            legacyCoreRel = vec(s, "legacyCoreRel");
        }
    }

    /** The hole centre from the KEE controller, KEE (a, b, c) ({@code machines.kerr.centre.rel}). */
    public static int[] centreRel() {
        return vec(
            BTSShapes.manifest()
                .getAsJsonObject("machines")
                .getAsJsonObject("kerr")
                .getAsJsonObject("centre"),
            "rel");
    }

    private static int[] vec(JsonObject o, String name) {
        final JsonArray a = o.getAsJsonArray(name);
        if (a == null || a.size() != 3) throw new IllegalStateException("[BTS] Kerr slot table: bad '" + name + "'");
        return new int[] { a.get(0)
            .getAsInt(),
            a.get(1)
                .getAsInt(),
            a.get(2)
                .getAsInt() };
    }

    // =========================================================================== world math

    /** {@code base + facing.worldOffset(abc)}. */
    public static int[] worldPos(ExtendedFacing facing, int x, int y, int z, int[] abc) {
        final int[] xyz = new int[3];
        facing.getWorldOffset(abc, xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }

    /** Where this slot's module controller stands for a KEE at (x, y, z) facing {@code kee}. */
    public int[] modulePos(ExtendedFacing kee, int x, int y, int z) {
        return worldPos(kee, x, y, z, rel());
    }

    /** The world direction the slot's module controller must face, for a KEE facing {@code kee}. */
    public ForgeDirection moduleFacing(ExtendedFacing kee) {
        final int[] d = new int[3];
        kee.getWorldOffset(facingAbc(), d);
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (dir.offsetX == d[0] && dir.offsetY == d[1] && dir.offsetZ == d[2]) return dir;
        }
        return ForgeDirection.UNKNOWN;
    }

    /** Where the KEE controller stands for this slot's module at (x, y, z) facing {@code module}. */
    public int[] keePos(ExtendedFacing module, int x, int y, int z) {
        return worldPos(module, x, y, z, keeRel());
    }

    /** The GT tile at a loaded cell, or null (never loads a chunk). */
    public static IGregTechTileEntity gtTileAt(World world, int[] p) {
        if (world == null || p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof IGregTechTileEntity ? (IGregTechTileEntity) te : null;
    }
}
