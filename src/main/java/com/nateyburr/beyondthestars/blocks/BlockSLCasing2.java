package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.material.Material;

/**
 * SINGULARITY LADDER: {@code slCasing2}, the (retired) Kerr module core blocks and the Kerr's lining
 * (singularity-ladder-build.md §1.2, Contract additions FC-1/FC-2). Before the Kerr redesign one core
 * sat in each optional module arc; the modules are separate machines now (docs/kerr-redesign.md).
 * <p>
 * Meta 2 (Harmonic Lining) was a hole while the Kerr's lining role was played by the Black Hole
 * Compressor's Background Radiation Absorbent Casing. I ruled (2026-10-12, round 4) that the
 * Kerr gets ONE new white lining block in the BHC visual family (the mockup's bright inner face), so
 * the reserved lining meta is live again, for the same role it was reserved for. The Eye keeps
 * TecTech's Infinite Spacetime Energy Boundary Casing as its lining.
 * <p>
 * Metas 3-11 are P4's new structure blocks (the quasar, its crowns and the conduit), in the Kerr / BHC family: the
 * quasar's cage is the BHC's. §6.2's "Casimir-lined conduit casing" is not a block: the existing Casimir Plate
 * ({@code slCasing:14}) plays it (P4-08).
 */
public class BlockSLCasing2 extends BlockSLMeta {

    public static final int COMPRESSOR_MODULE = 0;
    public static final int EXTRACTOR_MODULE = 1;
    /** Harmonic Lining: the white inner face of every Kerr arc and band (Kerr shape char {@code J}). */
    public static final int LINING = 2;

    // ---- P4 (quasar-p4.md §6.2; P4-05 / P4-08). Frozen once shipped. The quasar's chars in manifest.json.
    /** Disc Field Coil T1 (Infinity): b <= 1, hole cap 256 SM. Quasar char {@code H}, channel {@code coil} 1. */
    public static final int DISC_COIL_T1 = 3;
    /** Disc Field Coil T2 (Transcendent Metal): b <= 2, cap 1,024 SM. Channel {@code coil} 2. */
    public static final int DISC_COIL_T2 = 4;
    /** Disc Field Coil T3 (SpaceTime): b <= 3, cap 2,048 SM. Channel {@code coil} 3. */
    public static final int DISC_COIL_T3 = 5;
    /** Disc Field Coil T4 (MagMatter): b <= 4, cap 4,096 SM. Channel {@code coil} 4. */
    public static final int DISC_COIL_T4 = 6;
    /** Eddington Wall Casing: the quasar's walls and collars (char {@code E}, 27,815 per quasar). */
    public static final int EDDINGTON_WALL = 7;
    /** Rim Gas Injector: the quasar's disc rim (char {@code I}) and the Wormhole Mouth's fuel ring. */
    public static final int RIM_GAS_INJECTOR = 8;
    /** Jet Collimator Ring: the quasar's jet towers (char {@code C}), the crowns' and every conduit element's rings. */
    public static final int JET_COLLIMATOR = 9;
    /** Burst Thermaliser Casing: the crowns' core (crown char {@code F}). */
    public static final int BURST_THERMALISER = 10;
    /** Radiator Petal: the crowns' petals (crown char {@code R}). */
    public static final int RADIATOR_PETAL = 11;

    static final String[] TEXTURES = { "sl_module_compressor", // 0
        "sl_module_extractor", // 1
        "sl_lining", // 2
        "sl_disccoil_t1", // 3
        "sl_disccoil_t2", // 4
        "sl_disccoil_t3", // 5
        "sl_disccoil_t4", // 6
        "sl_eddington", // 7
        "sl_rimgas", // 8
        "sl_collimator", // 9
        "sl_thermaliser", // 10
        "sl_petal", // 11
    };

    /** The Disc Field Coil meta of tier 1-4 (the quasar's {@code coil} channel), or -1. */
    public static int discCoil(int tier) {
        return tier >= 1 && tier <= 4 ? DISC_COIL_T1 + tier - 1 : -1;
    }

    /** The Disc Field Coil tier (1-4) of a meta, or 0. */
    public static int discCoilTier(int meta) {
        return meta >= DISC_COIL_T1 && meta <= DISC_COIL_T4 ? meta - DISC_COIL_T1 + 1 : 0;
    }

    public BlockSLCasing2() {
        super(Material.iron, "slCasing2", TEXTURES);
    }

    /**
     * KERR REDESIGN: the two module CORE blocks are retired (the modules are machines with their own
     * controllers now). The metas stay registered, because a live block must never lose a meta, but
     * they are hidden from NEI and the creative tab; a shapeless recipe turns one into its module
     * controller, and the KEE converts placed ones (KerrMigration).
     */
    @Override
    protected boolean listed(int meta) {
        return meta != COMPRESSOR_MODULE && meta != EXTRACTOR_MODULE;
    }
}
