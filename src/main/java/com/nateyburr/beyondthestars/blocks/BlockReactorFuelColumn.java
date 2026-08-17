package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.nateyburr.beyondthestars.items.BTSItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Placeable fuel column segment for the Reactor Core interior. Three stacked segments form one
 * full fuel column; the structure check counts segments and the reactor scales output by full
 * columns. AE2-driven loading manipulates these via the Core Access Hatch in a later build.
 */
public class BlockReactorFuelColumn extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;
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

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        sideIcon = reg.registerIcon("beyondthestars:reactorFuelColumnSide");
        topIcon = reg.registerIcon("beyondthestars:reactorFuelColumnTop");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return (side == 0 || side == 1) ? topIcon : sideIcon;
    }
}
