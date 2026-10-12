package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.material.Material;

/**
 * SINGULARITY LADDER: {@code slCasing2}, the Kerr module core blocks (singularity-ladder-build.md
 * §1.2, Contract additions FC-1/FC-2). One core sits in each optional module arc; its presence is
 * what the Kerr controller checks for.
 * <p>
 * Meta 2 (Harmonic Lining) is a HOLE: the lining role is played by existing blocks (the Eye's
 * Infinite Spacetime Energy Boundary Casing, the Kerr's Background Radiation Absorbent Casing).
 */
public class BlockSLCasing2 extends BlockSLMeta {

    public static final int COMPRESSOR_MODULE = 0;
    public static final int EXTRACTOR_MODULE = 1;
    /** Retired: see the class comment. */
    public static final int LINING_HOLE = 2;

    static final String[] TEXTURES = { "sl_module_compressor", // 0
        "sl_module_extractor", // 1
        null, // 2 hole (harmonic lining)
    };

    public BlockSLCasing2() {
        super(Material.iron, "slCasing2", TEXTURES);
    }
}
