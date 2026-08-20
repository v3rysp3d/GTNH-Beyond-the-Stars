package com.nateyburr.beyondthestars;

import java.util.Locale;

import net.minecraft.block.Block;
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
 * <p>
 * <b>Not one number below is typed by hand.</b> Review finding C5/A4 caught this command keeping
 * its own estimates of what the projection raises. They had drifted by up to 3.7x - 240 wall
 * casings quoted where a tier-5 silo actually takes 1,808 - and one of them named "Iridium
 * Tungstensteel Block", which is the GregTech {@code ItemList} enum constant
 * {@code Block_IridiumTungstensteel}, not a registered display name; searching NEI for it returns
 * nothing. The registered name of {@code sBlockReinforced} meta 1 is "Iridium Reinforced Block".
 * <p>
 * Every count and every material name printed here now comes from {@link BTSBom}, which
 * {@code questbook/gen-bom.ps1} renders straight out of the structure shape code in the same pass
 * that fills in the five Blueprint quests. This command and the questbook are two readers of one
 * tally, so they cannot disagree; and because a material may only be named through a
 * {@code BTSBom} constant, an item that is renamed or drops out of the element alphabet breaks
 * {@code compileJava} instead of quietly printing a block that does not exist.
 */
public class CommandBTSKit extends CommandBase {

    @Override
    public String getCommandName() {
        return "btskit";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btskit [siloTier 1-" + BTSBom.MAX_TIER + "]";
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

    /** Hands over a derived quantity of a block, chunked into legal stacks. */
    private static void giveBlocks(EntityPlayerMP player, Block block, int count) {
        int left = count;
        while (left > 0) {
            final int stackSize = Math.min(64, left);
            give(player, new ItemStack(block, stackSize));
            left -= stackSize;
        }
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    private static String qty(int count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    /** "1,667x Radiant Naquadah Alloy Casing, 1,608x Reactor Fuel Column, ..." */
    private static String materials(BTSBom.Bill bill) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bill.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(qty(bill.count(i)))
                .append("x ")
                .append(bill.item(i));
        }
        return sb.toString();
    }

    private static void tellBill(EntityPlayerMP player, String heading, BTSBom.Bill bill) {
        tell(player, " - " + heading + ": " + materials(bill));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final int tier = args.length > 0 ? Math.min(BTSBom.MAX_TIER, Math.max(1, parseInt(sender, args[0]))) : 1;

        final BTSBom.Bill silo = BTSBom.SILO[tier];
        final BTSBom.Bill duct = BTSBom.DUCT[tier];

        // How many of each fitted part the plant actually swallows at this tier - read off the
        // same bills the shopping list prints, so the kit can never hand out a count the
        // structure does not want.
        final int columns = silo.of(BTSBom.REACTOR_FUEL_COLUMN);
        final int ports = silo.of(BTSBom.ROD_LOADING_PORT);
        final int housings = duct.of(BTSBom.TURBINE_HOUSING) + BTSBom.GALLERY.of(BTSBom.TURBINE_HOUSING);
        final int stages = BTSBom.GALLERY.of(BTSBom.TURBINE_HOUSING);
        final int injectors = BTSBom.SPIRAL.of(BTSBom.PRECISION_INJECTOR);

        // Controllers. A3 retired both plant-module controllers: the Cooling Towers and every
        // other building are pieces of the main station now, so ONE controller raises the
        // whole plant and the kit no longer hands out museum pieces.
        give(player, BTSMachines.reactorCoreMk1.copy());
        give(player, BTSMachines.extractionPlatform.copy());
        give(player, BTSMachines.enrichmentTrain.copy());

        // Pillar frames (8 segments per pillar) + a loading port per pillar
        giveBlocks(player, BTSBlocks.reactorFuelColumn, columns);
        giveBlocks(player, BTSBlocks.rodLoadingPort, ports);

        // Rods for the cascade, assemblies for the fallback, pellets for crafting
        give(player, new ItemStack(BTSItems.fuelRodUranium, 64));
        give(player, new ItemStack(BTSItems.fuelRodMox, 64));
        give(player, new ItemStack(BTSItems.fuelRodThoriumBonded, 64));
        give(player, new ItemStack(BTSItems.fuelRodNaqBonded, 64));
        give(player, new ItemStack(BTSItems.fuelAssemblyUranium, 64));
        give(player, new ItemStack(BTSItems.fuelAssemblyNaqBonded, 64));
        give(player, new ItemStack(BTSItems.uraniumFuelPellet, 64));

        // Spine rotor sockets + gallery stages + Spiral chamber hatches + TC control blocks
        giveBlocks(player, BTSBlocks.turbineHousing, housings);
        for (int i = 0; i < injectors; i++) {
            give(player, BTSMachines.precisionInjector.copy());
        }
        give(player, BTSMachines.spineSensor.copy());
        give(player, BTSMachines.siloControlBlock.copy());
        give(player, BTSMachines.turbineControlBlock.copy());

        // TC-4: one rotor per gallery stage (max stack 1 - wear lives on the item)
        for (int i = 0; i < stages; i++) {
            give(player, new ItemStack(BTSItems.turbineRotor, 1));
        }

        // Fans
        give(player, new ItemStack(BTSItems.turbineFanMatched, 1));
        give(player, new ItemStack(BTSItems.turbineFanTDA, 1));
        give(player, new ItemStack(BTSItems.turbineFanTDAOptimized, 1));

        player.inventoryContainer.detectAndSendChanges();

        tell(player, EnumChatFormatting.AQUA + "== Beyond the Stars test kit (silo tier " + tier + ") ==");
        tell(
            player,
            "Kit given: 3 controllers, " + qty(columns)
                + " pillar frames, "
                + qty(ports)
                + " loading ports (optional now), "
                + qty(housings)
                + " turbine housings, "
                + qty(stages)
                + " rotors, "
                + qty(injectors)
                + " precision injectors, rods, fuels.");
        tell(
            player,
            EnumChatFormatting.YELLOW
                + "Grab via NEI (creative). Every count below is DERIVED from the structure shape code by questbook/gen-bom.ps1 - the same tally the Blueprint quests print, so the two can never disagree:");
        tellBill(player, BTSBom.PLATFORM.piece(), BTSBom.PLATFORM);
        tellBill(
            player,
            silo.piece() + " ("
                + BTSBom.EDGE[tier]
                + "x"
                + BTSBom.EDGE[tier]
                + " octagon, "
                + BTSBom.PILLARS[tier]
                + " pillars)",
            silo);
        tellBill(player, duct.piece() + " (length " + BTSBom.DUCT_LENGTH[tier] + ")", duct);
        tellBill(player, BTSBom.SPIRAL.piece(), BTSBom.SPIRAL);
        tellBill(player, BTSBom.GALLERY.piece(), BTSBom.GALLERY);
        tellBill(player, BTSBom.TOWER.piece() + ", x" + BTSBom.TOWERS, BTSBom.TOWER);
        tell(
            player,
            EnumChatFormatting.YELLOW + " = WHOLE PLANT at tier " + tier + ": " + materials(BTSBom.TOTAL[tier]));
        tell(
            player,
            " - Point-of-use sockets (D-20): SILO face = " + silo.of(BTSBom.MAINTENANCE_HATCH)
                + " maintenance (y0), "
                + silo.of(BTSBom.INPUT_HATCH_IV)
                + " input hatch + "
                + silo.of(BTSBom.SILO_CONTROL_BLOCK)
                + " Silo Control Block + "
                + silo.of(BTSBom.OUTPUT_HATCH_IV)
                + " output hatch (y1), "
                + silo.of(BTSBom.INPUT_BUS_IV)
                + " input bus + "
                + silo.of(BTSBom.OUTPUT_BUS_IV)
                + " output bus (y2); SPIRAL front wall = "
                + BTSBom.SPIRAL.of(BTSBom.INPUT_HATCH_IV)
                + " input hatches, coolant + propellant (y1); MANIFOLD back face = "
                + BTSBom.GALLERY.of(BTSBom.DYNAMO_HATCH_IV)
                + " dynamos (y3), "
                + BTSBom.GALLERY.of(BTSBom.ENERGY_HATCH_IV)
                + " ENERGY hatches (y4), "
                + BTSBom.GALLERY.of(BTSBom.LASER_SOURCE_HATCH)
                + " laser sources (y5)");
        tell(
            player,
            " - The projector places no Turbine Housing: the " + qty(housings)
                + " given cover the "
                + duct.of(BTSBom.TURBINE_HOUSING)
                + " spine sockets + "
                + stages
                + " gallery stages - seat a Turbine Rotor ("
                + qty(stages)
                + " given) in every stage.");
        tell(
            player,
            " - Control blocks given: Silo Control Block (silo face, above the station) + Turbine Control Block (manifold back face; feed it a redstone clock to spin the fan)");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Build: place the main controller, ONE hologram projection builds EVERYTHING (platform + silo + duct + spiral + gallery + both cooling towers; tier from GUI Build Tier or a 2-5 trigger stack, and a silo that already stands is never re-tiered). There is no second controller to place.");
        tell(
            player,
            "Then: pillar frames in the core, and plumb each building where it is fed - rods and water into the SILO face, coolant and propellant into the SPIRAL chamber's front wall. The plant distributes all of it internally from there.");
    }
}
