package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.items.BTSItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Turbine Housing - a rotor socket in the Steam Duct (the Spine). Right-click to flip the
 * rotor between CLOCKWISE (meta 0) and ANTICLOCKWISE (meta 1). Backwash swirl accumulates
 * through the four housings: rotors riding the swirl add steam speed (and heat), rotors
 * set against it add EU-per-litre efficiency. The silo controller reads the pattern.
 */
public class BlockTurbineHousing extends Block {

    public static final int META_CW = 0;
    public static final int META_CCW = 1;

    @SideOnly(Side.CLIENT)
    private IIcon cwIcon;
    @SideOnly(Side.CLIENT)
    private IIcon ccwIcon;

    public BlockTurbineHousing() {
        super(Material.iron);
        setHardness(6.0F);
        setResistance(12.0F);
        setStepSound(soundTypeMetal);
        setBlockName("beyondthestars.turbineHousing");
        setBlockTextureName("beyondthestars:turbineHousingCW");
        setCreativeTab(BTSItems.TAB);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (world.isRemote) return true;
        final int flipped = world.getBlockMetadata(x, y, z) == META_CW ? META_CCW : META_CW;
        world.setBlockMetadataWithNotify(x, y, z, flipped, 3);
        player.addChatMessage(
            new ChatComponentText(
                flipped == META_CW ? "Rotor: CLOCKWISE (rides right-hand swirl)"
                    : "Rotor: ANTICLOCKWISE (rides left-hand swirl)"));
        return true;
    }

    /** Both spin directions drop the same item. */
    @Override
    public int damageDropped(int meta) {
        return META_CW;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        cwIcon = reg.registerIcon("beyondthestars:turbineHousingCW");
        ccwIcon = reg.registerIcon("beyondthestars:turbineHousingCCW");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta == META_CCW ? ccwIcon : cwIcon;
    }
}
