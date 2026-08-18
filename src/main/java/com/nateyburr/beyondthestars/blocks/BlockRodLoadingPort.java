package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.items.BTSItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Rod Loading Port - the pillar's custom input. Placed directly above a fuel pillar inside
 * the Steam Generator; buffers rods (hopper/AE2-insertable) that the silo cascades into the
 * pillar below, lowest empty segment first. Right-click with a rod to load it by hand;
 * breaking the block drops the buffer.
 */
public class BlockRodLoadingPort extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;

    public BlockRodLoadingPort() {
        super(Material.iron);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName("beyondthestars.rodLoadingPort");
        setBlockTextureName("beyondthestars:rodLoadingPortSide");
        setCreativeTab(BTSItems.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileRodLoadingPort();
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
        if (!TileRodLoadingPort.isFuelRod(held)) return false;
        if (world.isRemote) return true;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileRodLoadingPort)) return false;
        final TileRodLoadingPort port = (TileRodLoadingPort) te;
        // Creative players load a copy - their held stack is never consumed.
        final boolean creative = player.capabilities.isCreativeMode;
        final ItemStack insert = creative ? held.copy() : held;
        for (int slot = 0; slot < TileRodLoadingPort.SLOTS && insert.stackSize > 0; slot++) {
            final ItemStack existing = port.getStackInSlot(slot);
            if (existing == null) {
                port.setInventorySlotContents(slot, insert.splitStack(insert.stackSize));
            } else if (existing.getItem() == insert.getItem() && existing.stackSize < existing.getMaxStackSize()) {
                final int move = Math.min(insert.stackSize, existing.getMaxStackSize() - existing.stackSize);
                existing.stackSize += move;
                insert.stackSize -= move;
                port.markDirty();
            }
        }
        if (!creative && held.stackSize <= 0) {
            player.setCurrentItemOrArmor(0, null);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IInventory) {
            final IInventory inv = (IInventory) te;
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                final ItemStack stack = inv.getStackInSlot(slot);
                if (stack != null) {
                    world.spawnEntityInWorld(
                        new net.minecraft.entity.item.EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack));
                    inv.setInventorySlotContents(slot, null);
                }
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        sideIcon = reg.registerIcon("beyondthestars:rodLoadingPortSide");
        topIcon = reg.registerIcon("beyondthestars:rodLoadingPortTop");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return (side == 0 || side == 1) ? topIcon : sideIcon;
    }
}
