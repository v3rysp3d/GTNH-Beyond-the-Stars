package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.blocks.TileTurbineHousing;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.enums.Materials;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;

/**
 * /btsbuild [tier 1-5] [rod] - builds a COMPLETE, RUNNING Unit One where the player stands.
 * The hologram projector can only preview one piece at a time and is height-capped, so this
 * is the way to get a whole plant on the ground for testing: the platform, silo, Spine,
 * Spiral, Turbine Generator and both Cooling Tower shells, every hatch and control block
 * fitted, rotors seated, water and fuel loaded, and the bench clock enabled so the turbine
 * spins without wiring redstone first.
 */
public class CommandBTSBuild extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsbuild";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsbuild [tier 1-5] [uranium|mox|thorium|naq]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        final int tier = args.length > 0 ? Math.min(5, Math.max(1, parseInt(sender, args[0]))) : 3;
        final net.minecraft.item.Item rod = rodFor(args.length > 1 ? args[1] : "naq");

        // The controller stands where the player does, facing the way the player faces; the
        // plant then grows behind it (32 blocks) and 17 in front, 25 to either side.
        final int x = MathHelper.floor_double(player.posX);
        final int y = MathHelper.floor_double(player.posY);
        final int z = MathHelper.floor_double(player.posZ);
        final ForgeDirection facing = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);

        if (y + 26 > 255) {
            tell(
                player,
                EnumChatFormatting.RED
                    + "Too high: the cooling towers reach 25 blocks up and would be clipped by the build limit. Build below y=229.");
            return;
        }
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, idOf(BTSMachines.reactorCoreMk1), facing)) {
            tell(player, EnumChatFormatting.RED + "Could not place the main station here.");
            return;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) {
            tell(player, EnumChatFormatting.RED + "Main station did not initialise.");
            return;
        }
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(mte instanceof MTEReactorCoreMk1)) {
            tell(player, EnumChatFormatting.RED + "Unexpected machine at the build site.");
            return;
        }
        final MTEReactorCoreMk1 core = (MTEReactorCoreMk1) mte;

        // 1. Build every piece. Socket cells auto-fit their real hatch in creative placement.
        core.construct(
            new ItemStack(BTSMachines.reactorCoreMk1.getItem(), tier, idOf(BTSMachines.reactorCoreMk1)),
            false);

        // 2. Form it, so the hatch/port/stage lists are populated for the steps below.
        core.checkStructure(true, (IGregTechTileEntity) te);

        // 3. Seat a fresh rotor in THIS plant's own housings (the Spine's 4 sockets and the
        // 8 gallery stages, as recorded by the structure walk) - never a neighbour's build.
        int rotors = 0;
        for (final int[] c : core.housingCells()) {
            if (!world.blockExists(c[0], c[1], c[2])) continue;
            if (world.getBlock(c[0], c[1], c[2]) != BTSBlocks.turbineHousing) continue;
            final TileEntity ht = world.getTileEntity(c[0], c[1], c[2]);
            if (ht instanceof TileTurbineHousing && !((TileTurbineHousing) ht).hasRotor()) {
                ((TileTurbineHousing) ht).installRotor(new ItemStack(BTSItems.turbineRotor, 1));
                rotors++;
            }
        }

        // 4. Fill every input hatch with water (feedwater AND the chamber's coolant), and
        // stock each Rod Loading Port so the cascade has fuel to drop.
        int filled = 0;
        for (final MTEHatchInput hatch : core.mInputHatches) {
            if (hatch == null || !hatch.isValid()) continue;
            hatch.setFillableStack(
                new FluidStack(
                    Materials.Water.getFluid(1L)
                        .getFluid(),
                    hatch.getCapacity()));
            filled++;
        }
        final int ports = core.stockRodPorts(new ItemStack(rod, 64));

        // 5. Bench clock on, maintenance signed off (a freshly placed controller has all six
        // issues open and GT would stop it with NO_REPAIR before it ever ran), then re-form.
        core.setDebugAutoCycle(true);
        core.fixAllIssues();
        core.getBaseMetaTileEntity()
            .enableWorking();
        core.checkStructure(true, (IGregTechTileEntity) te);
        core.fixAllIssues();

        tell(player, EnumChatFormatting.AQUA + "== Unit One built (tier " + tier + ") ==");
        tell(
            player,
            "Main station at your feet facing " + facing.name()
                + "; plant extends 32 blocks behind it, 17 in front, 25 each side.");
        tell(
            player,
            "Fitted: all hatches + both control blocks · " + rotors
                + " rotors seated · "
                + filled
                + " input hatches watered · "
                + ports
                + " rod ports stocked.");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Bench clock ON - the turbine cycles once a second without redstone. Wire a real clock to the Turbine Control Block (manifold back face) to control the rate, then /btsclock off.");
        tell(player, "Spine left all-CLOCKWISE (riding pattern). Right-click a housing to retune it.");
    }

    private static net.minecraft.item.Item rodFor(String name) {
        if (name == null) return BTSItems.fuelRodNaqBonded;
        switch (name.toLowerCase()) {
            case "uranium":
            case "u":
                return BTSItems.fuelRodUranium;
            case "mox":
                return BTSItems.fuelRodMox;
            case "thorium":
            case "th":
                return BTSItems.fuelRodThoriumBonded;
            default:
                return BTSItems.fuelRodNaqBonded;
        }
    }

    private static short idOf(ItemStack stack) {
        return stack == null ? 0 : (short) stack.getItemDamage();
    }
}
