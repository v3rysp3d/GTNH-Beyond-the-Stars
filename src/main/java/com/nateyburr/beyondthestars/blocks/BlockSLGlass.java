package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GlassTier;

/**
 * P5 glasses (p5-t9-t10.md §7.2, §7.3): {@code slGlass}, a see-through meta-block. GT multis that check their glass
 * read its tier through {@link GlassTier}, registered here at init:
 * <ul>
 * <li>meta 0 <b>Magnetar-Reinforced Borosilicate Glass</b>, UXV (tier 13), made with Magnetar Matter;</li>
 * <li>meta 1 <b>Dark Matter Borosilicate Glass</b>, MAX (tier 14), made with Attractor Dark Matter.</li>
 * </ul>
 * The id plan put the MAX glass at {@code slCasing4:8}; that block is opaque, so both glasses live here (question
 * B-203). Neither is a tier's "main" glass, so GT's own hologram and NEI previews keep their glass.
 */
public class BlockSLGlass extends BlockSLMeta {

    public static final int MAGNETAR_UXV = 0;
    public static final int DARK_MATTER_MAX = 1;

    static final String[] TEXTURES = { "sl_glass_magnetar", // 0
        "sl_glass_darkmatter", // 1
    };

    public BlockSLGlass() {
        super(Material.glass, "slGlass", TEXTURES);
        setStepSound(soundTypeGlass);
        setLightOpacity(0);
    }

    /** GT glass tiers (13 UXV, 14 MAX), as a non-main glass of each tier. Called once at init. */
    public void registerGlassTiers() {
        try {
            GlassTier.addCustomGlass(this, MAGNETAR_UXV, 13, 1);
            GlassTier.addCustomGlass(this, DARK_MATTER_MAX, 14, 1);
        } catch (RuntimeException failed) {
            BeyondTheStars.LOG.error("[BTS] slGlass: could not register the P5 glass tiers with GT", failed);
        }
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        final Block neighbour = world.getBlock(x, y, z);
        return neighbour != this && super.shouldSideBeRendered(world, x, y, z, side);
    }
}
