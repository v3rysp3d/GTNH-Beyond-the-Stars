package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.nateyburr.beyondthestars.items.BTSItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Fuel pillar frame segment for the Steam Generator interior. Eight stacked segments form one
 * pillar; the pillar frame is permanent structure, and the rods CASCADING through it are
 * items tracked by the silo controller. Meta 0 = empty frame, meta 1 = a rod occupies this
 * segment (set by the controller for the fill-level visual).
 */
public class BlockReactorFuelColumn extends Block {

    public static final int META_EMPTY = 0;
    public static final int META_FUELED = 1;

    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;
    @SideOnly(Side.CLIENT)
    private IIcon sideFueledIcon;
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;

    public BlockReactorFuelColumn() {
        super(Material.iron);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName("beyondthestars.reactorFuelColumn");
        setBlockTextureName("beyondthestars:reactorFuelColumnSide");
        setCreativeTab(BTSItems.TAB);
    }

    /** Fueled frames still drop the plain frame; the rod inside is the controller's problem. */
    @Override
    public int damageDropped(int meta) {
        return META_EMPTY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        sideIcon = reg.registerIcon("beyondthestars:reactorFuelColumnSide");
        sideFueledIcon = reg.registerIcon("beyondthestars:reactorFuelColumnSideFueled");
        topIcon = reg.registerIcon("beyondthestars:reactorFuelColumnTop");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 0 || side == 1) return topIcon;
        return meta == META_FUELED ? sideFueledIcon : sideIcon;
    }
}
