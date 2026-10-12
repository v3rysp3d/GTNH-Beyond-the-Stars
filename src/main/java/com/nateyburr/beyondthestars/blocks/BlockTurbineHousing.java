package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.items.BTSItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Turbine Housing - a rotor socket in the Steam Duct (the Spine) and the rotor gallery's
 * stage seats. Right-click with a Turbine Rotor to seat it (TC-4; gallery stages only
 * convert steam while a rotor is seated); sneak-right-click with an EMPTY hand to pull the
 * rotor back out, wear preserved. A plain right-click flips the housing between CLOCKWISE
 * (meta 0) and ANTICLOCKWISE (meta 1) - the Spine's swirl toggle; gallery stages ignore
 * chirality. The silo controller reads both the pattern and the seated rotors.
 */
public class BlockTurbineHousing extends BlockContainer {

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
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileTurbineHousing();
    }

    /** Standard cube rendering despite being a container block. */
    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        final ItemStack held = player.getHeldItem();

        // Seat a rotor (TC-4): held Turbine Rotor goes into the housing, wear preserved.
        if (held != null && held.getItem() == BTSItems.turbineRotor) {
            if (world.isRemote) return true;
            final TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof TileTurbineHousing)) return false;
            final TileTurbineHousing housing = (TileTurbineHousing) te;
            if (housing.hasRotor()) {
                player.addChatMessage(
                    new ChatComponentText("A rotor is already seated - sneak-right-click empty-handed to pull it"));
                return true;
            }
            housing.installRotor(held);
            if (!player.capabilities.isCreativeMode) {
                held.stackSize--;
                if (held.stackSize <= 0) {
                    player.setCurrentItemOrArmor(0, null);
                }
            }
            player.addChatMessage(
                new ChatComponentText("Turbine Rotor seated (wear " + housing.getRotorDamage() + "/10)"));
            return true;
        }

        // Pull the rotor: sneak-right-click with an empty hand (wear stays on the item).
        if (player.isSneaking()) {
            if (held != null) return false;
            if (world.isRemote) return true;
            final TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileTurbineHousing && ((TileTurbineHousing) te).hasRotor()) {
                final ItemStack rotor = ((TileTurbineHousing) te).extractRotor();
                player.addChatMessage(
                    new ChatComponentText("Turbine Rotor extracted (wear " + rotor.getItemDamage() + "/10)"));
                if (!player.inventory.addItemStackToInventory(rotor)) {
                    player.entityDropItem(rotor, 0.5F);
                }
                player.inventoryContainer.detectAndSendChanges();
                return true;
            }
            return false;
        }

        // Spin toggle (the Spine's swirl mechanic; gallery stages ignore chirality).
        if (world.isRemote) return true;
        final int flipped = world.getBlockMetadata(x, y, z) == META_CW ? META_CCW : META_CW;
        world.setBlockMetadataWithNotify(x, y, z, flipped, 3);
        // Tidy C13: a housing cannot tell whether it rides or opposes - that depends on the
        // housings nearer the silo (computeSpine) - nor whether it sits in the Spine or the
        // gallery, so the second line states the rule in a form that holds for every housing.
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.AQUA + "Spin: " + (flipped == META_CW ? "CLOCKWISE" : "ANTICLOCKWISE")));
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GRAY
                    + "Spine: turning with the swirl of the housings nearer the silo adds speed, against it adds efficiency. Gallery stages ignore spin."));
        return true;
    }

    /** A broken housing spits its seated rotor back out, wear preserved. */
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileTurbineHousing && ((TileTurbineHousing) te).hasRotor()) {
            final ItemStack rotor = ((TileTurbineHousing) te).extractRotor();
            world.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(world, x + 0.5, y + 0.5, z + 0.5, rotor));
        }
        super.breakBlock(world, x, y, z, block, meta);
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
