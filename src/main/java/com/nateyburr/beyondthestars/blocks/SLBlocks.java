package com.nateyburr.beyondthestars.blocks;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * SINGULARITY LADDER block registrar (singularity-ladder-build.md §1.2, Contract additions FC-1).
 * Owned by the Foundation unit; called from one appended line in {@link BTSBlocks#register()}.
 * <p>
 * Two meta-blocks, no tile entities. {@code BlockSLGlass} from the spec is NOT created: the
 * Spatial Glass Channel role is played by existing glass (TecTech's Quantum Glass on the Eye, the
 * BHC's Hawking Radiation Realignment Focus on the Kerr).
 */
public final class SLBlocks {

    public static BlockSLCasing slCasing;
    public static BlockSLCasing2 slCasing2;

    private SLBlocks() {}

    public static void register() {
        slCasing = new BlockSLCasing();
        GameRegistry.registerBlock(slCasing, ItemBlockSL.class, "slCasing");
        slCasing2 = new BlockSLCasing2();
        GameRegistry.registerBlock(slCasing2, ItemBlockSL.class, "slCasing2");
    }

    /** A stack of {@code slCasing:meta}. Throws on a hole, so a retired meta is never crafted. */
    public static ItemStack casing(int meta, int count) {
        return stack(slCasing, meta, count);
    }

    /** A stack of {@code slCasing2:meta}. Throws on a hole. */
    public static ItemStack casing2(int meta, int count) {
        return stack(slCasing2, meta, count);
    }

    private static ItemStack stack(BlockSLMeta block, int meta, int count) {
        if (block == null || !block.isValidMeta(meta)) {
            throw new IllegalArgumentException(
                "no Singularity Ladder block " + (block == null ? "(unregistered)" : block.getUnlocalizedName())
                    + " meta "
                    + meta);
        }
        return new ItemStack(block, count, meta);
    }
}
