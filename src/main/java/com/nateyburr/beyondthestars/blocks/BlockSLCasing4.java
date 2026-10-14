package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

/**
 * SMBH v1: {@code slCasing4}, the Supermassive Lattice and the Event Horizon Crown (design repo
 * {@code designs/docs/smbh-v1.md} §1.1, §3.4, §8.1). In the quasar's Kerr / BHC family, each with its tier's colour.
 * <p>
 * The metas are the shape chars' blocks in {@code structures/manifest.json} (legend {@code quasar} chars {@code L} and
 * {@code N}, legend {@code crown} char {@code X}); the numbers are frozen once shipped. Metas 6-15 are spare.
 */
public class BlockSLCasing4 extends BlockSLMeta {

    /** Lattice Frame Casing: the lattice's band, rings, struts and ribs (quasar char {@code L}, 8,436 a lattice). */
    public static final int LATTICE_FRAME = 0;
    /** Containment Node L1 (Superdense Ergomatter): safe 16,384 SM, top 65,536. Channel {@code lattice} 1. */
    public static final int NODE_L1 = 1;
    /** Containment Node L2 (White Hole Matter): safe 65,536 SM, top 262,144. Channel {@code lattice} 2. */
    public static final int NODE_L2 = 2;
    /** Containment Node L3 (Universium): safe 262,144 SM, top 4,194,304; Overdrive. Channel {@code lattice} 3. */
    public static final int NODE_L3 = 3;
    /** Containment Node L4 (Constrained Eternity): safe 4,194,304 SM, top 67,108,864; Unbind. Channel 4. */
    public static final int NODE_L4 = 4;
    /** Event Horizon Thermaliser Casing: the Mk IV crown's core (crown char {@code X}). */
    public static final int HORIZON_THERMALISER = 5;

    /** The nodes glow in their tier colour (smbh-v1.md §7). */
    private static final int NODE_LIGHT = 7;

    static final String[] TEXTURES = { "sl_latframe", // 0
        "sl_node_l1", // 1
        "sl_node_l2", // 2
        "sl_node_l3", // 3
        "sl_node_l4", // 4
        "sl_ehthermaliser", // 5
    };

    public BlockSLCasing4() {
        super(Material.iron, "slCasing4", TEXTURES);
    }

    /** The Containment Node meta of lattice tier 1-4, or -1. */
    public static int node(int tier) {
        return tier >= 1 && tier <= 4 ? NODE_L1 + tier - 1 : -1;
    }

    /** The lattice tier (1-4) of a meta, or 0. */
    public static int nodeTier(int meta) {
        return meta >= NODE_L1 && meta <= NODE_L4 ? meta - NODE_L1 + 1 : 0;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return world.getBlock(x, y, z) == this && nodeTier(world.getBlockMetadata(x, y, z)) > 0 ? NODE_LIGHT
            : super.getLightValue(world, x, y, z);
    }
}
