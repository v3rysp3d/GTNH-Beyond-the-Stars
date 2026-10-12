package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

/**
 * SINGULARITY LADDER: {@code slCasing3}, the casings of the Homogeny (MTE 28130), Reality Diffraction (28131) and
 * Reality Refab (28132), from my sketches (design repo {@code designs/docs/homogeny-reality.md} §4.2,
 * PROPOSED). Owned by the Homogeny/Reality work, so P4's casings (expected in {@code slCasing2:3+}) never collide
 * (question H/R-1).
 * <p>
 * The metas are the shape chars' blocks in {@code structures/manifest.json} (legends {@code homogeny},
 * {@code reality_diffraction}, {@code reality_refab}); the numbers are frozen once shipped. Metas 12-15 are spare.
 * Every block is made in the Condensate Assembler (ruling 16).
 */
public class BlockSLCasing3 extends BlockSLMeta {

    /** Homogeny volute, discharge throat and throat tip shell (Homogeny char {@code A}). */
    public static final int STIRRING_BAND = 0;
    /** Homogeny shear helix round the mixing column ({@code B}). */
    public static final int SHEAR_HELIX = 1;
    /** Homogeny feed vessels ({@code C}); each vessel's top carries one feed Input Hatch. */
    public static final int FEED_VESSEL = 2;
    /** Homogeny vortex inlets, vessel to volute ({@code D}). */
    public static final int VORTEX_INLET = 3;
    /** Reality Diffraction deck bands ({@code A}). */
    public static final int REGION_DECK_BAND = 4;
    /**
     * Every orange diamond: Diffraction blades and apex, Refab receivers, anchor and focus, Homogeny tips ({@code E}).
     */
    public static final int REGION_CRYSTAL = 5;
    /** Reality Diffraction phase rings ({@code H}). */
    public static final int PHASE_RING = 6;
    /** Reality Refab stem, ribs, belt, cap and clamps ({@code R}). */
    public static final int REASSEMBLY_HULL = 7;
    /** Reality Refab trays ({@code S}). */
    public static final int REASSEMBLY_TRAY = 8;
    /** Reality Refab claws, 8 per tray ({@code T}). */
    public static final int REASSEMBLY_CLAW = 9;
    /** Homogeny T3 outer ring and tail ({@code F}); also reserved for the Eye's T10 crown (P5). */
    public static final int ETERNITY_SHELL = 10;
    /** The red lens either side of each Reality Diffraction anchor socket ({@code I}). */
    public static final int REGION_ANCHOR = 11;

    /** Light level of the Region Crystal: the sketches' orange diamonds glow. */
    private static final int CRYSTAL_LIGHT = 9;

    static final String[] TEXTURES = { "sl_stir", // 0
        "sl_helix", // 1
        "sl_vessel", // 2
        "sl_inlet", // 3
        "sl_deckband", // 4
        "sl_region", // 5
        "sl_phase", // 6
        "sl_hull", // 7
        "sl_tray", // 8
        "sl_claw", // 9
        "sl_eternity", // 10
        "sl_alens", // 11
    };

    public BlockSLCasing3() {
        super(Material.iron, "slCasing3", TEXTURES);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return world.getBlock(x, y, z) == this && world.getBlockMetadata(x, y, z) == REGION_CRYSTAL ? CRYSTAL_LIGHT
            : super.getLightValue(world, x, y, z);
    }
}
