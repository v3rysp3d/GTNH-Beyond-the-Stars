package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

/**
 * /btskit [tier 1-5] - hands the operator a full campus test kit (controllers, fuel columns for
 * the chosen silo tier, every fuel grade, all three fans) and prints the casing shopping list.
 * Pair with creative mode + the StructureLib Hologram Projector for instant structure builds.
 */
public class CommandBTSKit extends CommandBase {

    /** Mirrors MTEReactorCoreMk1 geometry: edge 7+2t, corner cut, interior column caps. */
    private static final int[] CUT = { 0, 2, 2, 3, 3, 4 };
    private static final int[] COLUMN_CAP = { 0, 45, 77, 109, 157, 201 };

    @Override
    public String getCommandName() {
        return "btskit";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btskit [siloTier 1-5]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static void give(EntityPlayerMP player, ItemStack stack) {
        if (stack == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.entityDropItem(stack, 0.5F);
        }
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final int tier = args.length > 0 ? Math.min(5, Math.max(1, parseInt(sender, args[0]))) : 1;

        // Controllers (main + campus + plant modules)
        give(player, BTSMachines.reactorCoreMk1.copy());
        give(player, BTSMachines.extractionPlatform.copy());
        give(player, BTSMachines.enrichmentTrain.copy());
        give(player, BTSMachines.moduleTurbineHall.copy());
        give(player, BTSMachines.moduleCoolingTower.copy());
        give(player, BTSMachines.moduleCoolingTower.copy());

        // Fuel columns for the chosen tier (6 segments per column)
        int segments = COLUMN_CAP[tier] * 6;
        while (segments > 0) {
            final int stackSize = Math.min(64, segments);
            give(player, new ItemStack(BTSBlocks.reactorFuelColumn, stackSize));
            segments -= stackSize;
        }

        // Every fuel grade + pellets
        give(player, new ItemStack(BTSItems.fuelAssemblyUranium, 64));
        give(player, new ItemStack(BTSItems.fuelAssemblyMox, 64));
        give(player, new ItemStack(BTSItems.fuelAssemblyThoriumBonded, 64));
        give(player, new ItemStack(BTSItems.fuelAssemblyNaqBonded, 64));
        give(player, new ItemStack(BTSItems.uraniumFuelPellet, 64));

        // Fans
        give(player, new ItemStack(BTSItems.turbineFanMatched, 1));
        give(player, new ItemStack(BTSItems.turbineFanTDA, 1));
        give(player, new ItemStack(BTSItems.turbineFanTDAOptimized, 1));

        player.inventoryContainer.detectAndSendChanges();

        // Shopping list for the blocks NEI provides faster than an inventory dump.
        final int n = 7 + 2 * tier;
        final int k = CUT[tier];
        final int oct = n * n - 2 * k * (k + 1);
        final int ring = oct - COLUMN_CAP[tier];
        final String wall = tier >= 5 ? "Radiant Naquadah Alloy Casing"
            : tier >= 3 ? "Europium-Reinforced Radiation Proof Casing" : "Radiation Proof Machine Casing";
        final String shield = tier >= 3 ? "Iridium Tungstensteel Block" : "Tungstensteel Reinforced Block";

        tell(player, EnumChatFormatting.AQUA + "== Beyond the Stars test kit (steam gen tier " + tier + ") ==");
        tell(
            player,
            "Kit given: 5 controllers (+ spare tower), " + COLUMN_CAP[tier] * 6 + " fuel columns, fuels, fans.");
        tell(player, EnumChatFormatting.YELLOW + "Grab via NEI (creative):");
        tell(
            player,
            " - Starter platform: 2243x Light Concrete, 249x Tungstensteel Reinforced, 8x Solid Steel (flush dock markers)");
        tell(player, " - Silo walls: ~" + ring * 10 + "x " + wall + " (hatches substitute)");
        tell(player, " - Silo dome: ~" + oct + "x " + shield);
        tell(player, " - Turbine Hall: ~90x Tungstensteel Turbine Casing, glass, Steel Pipe Casing");
        tell(player, " - Cooling Towers: ~550x Light Concrete + ~40x Solid Steel Casing each");
        tell(player, " - Hatches: energy, maintenance, water in, steam out, input bus, output bus, dynamos");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Build: place the main controller (it ends up in the silo face), project the 50x50 platform + silo. Silo tier: Build Tier in the GUI, or a 2-5 trigger stack. Then stand module controllers on the dock markers and project their buildings.");
        tell(player, "Then: columns in the core, fuel + water in, throttle in the GUI, fan in the hall's input bus.");
    }
}
