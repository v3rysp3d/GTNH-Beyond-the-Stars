package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.material.Material;

/**
 * SINGULARITY LADDER: {@code slCasing2}, the Kerr module core blocks and the Kerr's lining
 * (singularity-ladder-build.md §1.2, Contract additions FC-1/FC-2). One core sits in each optional
 * module arc; its presence is what the Kerr controller checks for.
 * <p>
 * Meta 2 (Harmonic Lining) was a hole while the Kerr's lining role was played by the Black Hole
 * Compressor's Background Radiation Absorbent Casing. I ruled (2026-10-12, round 4) that the
 * Kerr gets ONE new white lining block in the BHC visual family (the mockup's bright inner face), so
 * the reserved lining meta is live again, for the same role it was reserved for. The Eye keeps
 * TecTech's Infinite Spacetime Energy Boundary Casing as its lining.
 */
public class BlockSLCasing2 extends BlockSLMeta {

    public static final int COMPRESSOR_MODULE = 0;
    public static final int EXTRACTOR_MODULE = 1;
    /** Harmonic Lining: the white inner face of every Kerr arc and band (Kerr shape char {@code J}). */
    public static final int LINING = 2;

    static final String[] TEXTURES = { "sl_module_compressor", // 0
        "sl_module_extractor", // 1
        "sl_lining", // 2
    };

    public BlockSLCasing2() {
        super(Material.iron, "slCasing2", TEXTURES);
    }
}
