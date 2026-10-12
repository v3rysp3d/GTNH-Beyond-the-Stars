package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.material.Material;

/**
 * SINGULARITY LADDER: {@code slCasing}, the Kerr and Eye-band structure casings
 * (singularity-ladder-build.md §1.2, Contract additions FC-1/FC-2).
 * <p>
 * Metas 0 (Containment Deck Plating) and 9 (Eye Shell Casing) are HOLES: I ruled
 * (2026-10-12) that the existing Eye of Harmony and Black Hole Compressor blocks play every role
 * they can, and the deck and the shell are such roles. The numbers stay reserved.
 */
public class BlockSLCasing extends BlockSLMeta {

    /** Retired: the Eye plinth is TecTech's Reinforced Temporal Structure Casing. */
    public static final int DECK_HOLE = 0;
    public static final int MIRROR = 1;
    public static final int MIRROR_BACKING = 2;
    public static final int INJECTOR_MK1 = 3;
    public static final int INJECTOR_MK2 = 4;
    public static final int INJECTOR_MK3 = 5;
    public static final int FIELD_COIL = 6;
    public static final int QUADRUPOLE = 7;
    public static final int BEAM_DUMP = 8;
    /** Retired: the Eye shell is TecTech's Reinforced Spatial Structure Casing. */
    public static final int EYE_SHELL_HOLE = 9;
    public static final int BEAM_BAND = 10;
    public static final int JET_CRESCENT = 11;
    public static final int GALACTIC_SHELL = 12;
    public static final int BASTION = 13;
    public static final int CASIMIR = 14;
    public static final int EXTRACTION = 15;

    static final String[] TEXTURES = { null, // 0 hole (deck)
        "sl_mirror", // 1
        "sl_mirrorback", // 2
        "sl_injector_mk1", // 3
        "sl_injector_mk2", // 4
        "sl_injector_mk3", // 5
        "sl_coil", // 6
        "sl_quad", // 7
        "sl_dump", // 8
        null, // 9 hole (eye shell)
        "sl_ring2", // 10
        "sl_ring3", // 11
        "sl_galactic", // 12
        "sl_bastion", // 13
        "sl_casimir", // 14
        "sl_extraction", // 15
    };

    public BlockSLCasing() {
        super(Material.iron, "slCasing", TEXTURES);
    }
}
