package com.nateyburr.beyondthestars.blocks;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.items.BTSItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;

/**
 * SINGULARITY LADDER: the shared base of the tile-less structure meta-blocks (singularity-ladder-build.md
 * §1.2 and Contract additions FC-1/FC-2).
 * <p>
 * One icon per meta on all six faces. A meta whose texture name is {@code null} is a HOLE: a spec
 * meta whose role an existing EOH or BHC block now plays (user ruling 2026-10-12). Holes are never
 * listed, never crafted and never reused, so every other meta keeps the number the shape-char table
 * and the other units already reference. A hole placed by hand (a /setblock) renders as the first
 * real meta and drops nothing.
 * <p>
 * Hardness, resistance and the wrench harvest level follow GT's casings, and nothing spawns on
 * them, so a 30,000-block structure does not become a mob farm.
 * <p>
 * Like GT's own casings ({@code BlockCasingsAbstract}), every meta is a registered GT MACHINE BLOCK
 * and placing or breaking one calls {@code GregTechAPI.causeMachineUpdate}: GT's update walk only
 * travels through machine blocks, so without this a Kerr or Eye never noticed a change to these
 * casings (and they cut the walk between GT casings on either side).
 */
public abstract class BlockSLMeta extends Block {

    private final String[] textures;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    protected BlockSLMeta(Material material, String registryName, String[] textures) {
        super(material);
        this.textures = textures.clone();
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName("beyondthestars." + registryName);
        setBlockTextureName("beyondthestars:" + firstReal(textures));
        setCreativeTab(BTSItems.TAB);
        for (int meta = 0; meta < textures.length; meta++) {
            if (textures[meta] != null) {
                setHarvestLevel("wrench", 2, meta);
            }
        }
        GregTechAPI.registerMachineBlock(this, -1);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (GregTechAPI.isMachineBlock(this, world.getBlockMetadata(x, y, z))) {
            GregTechAPI.causeMachineUpdate(world, x, y, z);
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (GregTechAPI.isMachineBlock(this, meta)) {
            GregTechAPI.causeMachineUpdate(world, x, y, z);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    private static String firstReal(String[] textures) {
        for (String texture : textures) {
            if (texture != null) {
                return texture;
            }
        }
        throw new IllegalArgumentException("a meta-block needs at least one real meta");
    }

    /** True if {@code meta} is a registered, craftable variant (not a hole, not out of range). */
    public boolean isValidMeta(int meta) {
        return meta >= 0 && meta < textures.length && textures[meta] != null;
    }

    /** The texture name of a meta, or null for a hole. */
    public String textureName(int meta) {
        return isValidMeta(meta) ? textures[meta] : null;
    }

    public int metaCount() {
        return textures.length;
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return isValidMeta(meta) ? 1 : 0;
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int meta = 0; meta < textures.length; meta++) {
            if (textures[meta] != null && listed(meta)) {
                list.add(new ItemStack(item, 1, meta));
            }
        }
    }

    /**
     * False hides a still-registered meta from the creative tab and NEI's item list (a retired block
     * that must stay placeable and loadable in old worlds, e.g. the pre-redesign Kerr module cores).
     */
    protected boolean listed(int meta) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        icons = new IIcon[textures.length];
        IIcon fallback = null;
        for (int meta = 0; meta < textures.length; meta++) {
            if (textures[meta] != null) {
                icons[meta] = reg.registerIcon("beyondthestars:" + textures[meta]);
                if (fallback == null) {
                    fallback = icons[meta];
                }
            }
        }
        for (int meta = 0; meta < textures.length; meta++) {
            if (icons[meta] == null) {
                icons[meta] = fallback;
            }
        }
        blockIcon = fallback;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (icons == null || meta < 0 || meta >= icons.length) {
            return blockIcon;
        }
        return icons[meta];
    }
}
