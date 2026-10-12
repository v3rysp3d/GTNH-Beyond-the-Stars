package com.nateyburr.beyondthestars;

import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.enums.ItemList;

/**
 * /btskit [1-5|kerr|eye|homogeny|diffraction|refab] - hands the operator a full test kit (controllers,
 * pillar frames for the chosen silo tier, every fuel grade) and prints the casing shopping list; the named
 * kits hand over that machine's controller and blocks, read from manifest.json.
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
        return "/btskit [1-" + BTSBom.MAX_TIER + "|kerr|eye|homogeny|diffraction|refab]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length != 1) return null;
        final String[] named = { "kerr", "eye", "homogeny", "diffraction", "refab" };
        final String[] options = new String[BTSBom.MAX_TIER + named.length];
        for (int t = 1; t <= BTSBom.MAX_TIER; t++) options[t - 1] = Integer.toString(t);
        System.arraycopy(named, 0, options, BTSBom.MAX_TIER, named.length);
        return getListOfStringsMatchingLastWord(args, options);
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

    private static String qty(long count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    /** "1,667x Radiant Naquadah Alloy Casing, 1,608x Reactor Fuel Pillar Frame, ..." */
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
        BTSChat.send(player, " - " + heading + ": " + materials(bill));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        // SINGULARITY LADDER P2 (Kerr unit): /btskit kerr - the Kerr's own kit, read from manifest.json.
        if (args.length > 0 && args[0].equalsIgnoreCase("kerr")) {
            com.nateyburr.beyondthestars.machines.kerr.KerrKit.giveKit(player);
            return;
        }
        // SINGULARITY LADDER P3 (Eye unit): /btskit eye - the Eye's own kit, read from manifest.json.
        if (args.length > 0 && args[0].equalsIgnoreCase("eye")) {
            com.nateyburr.beyondthestars.machines.eye.EyeKit.giveKit(player);
            return;
        }
        // H/R unit 2: /btskit homogeny | diffraction | refab - read from manifest.json.
        if (args.length > 0) {
            final com.nateyburr.beyondthestars.machines.reality.HRKit.Machine hr = com.nateyburr.beyondthestars.machines.reality.HRKit.Machine
                .parse(args[0]);
            if (hr != null) {
                com.nateyburr.beyondthestars.machines.reality.HRKit.giveKit(player, hr);
                return;
            }
        }
        final int tier = args.length > 0 ? Math.min(BTSBom.MAX_TIER, Math.max(1, parseInt(sender, args[0]))) : 1;

        final BTSBom.Bill silo = BTSBom.SILO[tier];
        final BTSBom.Bill duct = BTSBom.DUCT[tier];

        // How many of each fitted part the plant actually swallows at this tier - read off the
        // same bills the shopping list prints, so the kit can never hand out a count the
        // structure does not want.
        final int columns = silo.of(BTSBom.REACTOR_FUEL_PILLAR_FRAME);
        final int ports = silo.of(BTSBom.ROD_LOADING_PORT);
        final int housings = duct.of(BTSBom.TURBINE_HOUSING) + BTSBom.GALLERY.of(BTSBom.TURBINE_HOUSING);
        final int stages = BTSBom.GALLERY.of(BTSBom.TURBINE_HOUSING);
        final int injectors = BTSBom.SPIRAL.of(BTSBom.PRECISION_INJECTOR);

        // Controllers. A3 retired both plant-module controllers: the Cooling Towers and every
        // other building are pieces of the Nuclear Plant Foundation now, so ONE controller raises
        // the whole plant and the kit no longer hands out museum pieces. The two standalone
        // machines and the four production halls come along for testing the fuel chain.
        give(player, BTSMachines.reactorCoreMk1.copy());
        give(player, BTSMachines.extractionPlatform.copy());
        give(player, BTSMachines.enrichmentTrain.copy());
        give(player, BTSMachines.siteBatchingPlant.copy());
        give(player, BTSMachines.cascadeHall.copy());
        give(player, BTSMachines.fuelFabricationHall.copy());
        give(player, BTSMachines.purexCanyon.copy());

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

        // D-17 / D-22 module sockets - counts read off the same derived bills, never typed.
        final int moduleBuses = BTSBom.DERRICK.of(BTSBom.OUTPUT_BUS_IV) + BTSBom.ANNEX.of(BTSBom.OUTPUT_BUS_IV);
        for (int i = 0; i < moduleBuses; i++) {
            give(player, ItemList.Hatch_Output_Bus_IV.get(1L));
        }
        for (int i = 0; i < BTSBom.ANNEX.of(BTSBom.INPUT_BUS_IV); i++) {
            give(player, ItemList.Hatch_Input_Bus_IV.get(1L));
        }
        for (int i = 0; i < BTSBom.ANNEX.of(BTSBom.INPUT_HATCH_IV); i++) {
            give(player, ItemList.Hatch_Input_IV.get(1L));
        }

        // TC-4: one rotor per gallery stage (max stack 1 - wear lives on the item)
        for (int i = 0; i < stages; i++) {
            give(player, new ItemStack(BTSItems.turbineRotor, 1));
        }

        player.inventoryContainer.detectAndSendChanges();

        BTSChat.header(player, "== Beyond the Stars test kit (Reactor Silo tier " + tier + ") ==");
        BTSChat.send(
            player,
            "Kit given: Nuclear Plant Foundation (+ the standalone Extraction Platform and Enrichment Train, optional), the four production hall controllers, "
                + qty(columns)
                + " Reactor Fuel Pillar Frames, "
                + qty(ports)
                + " Rod Loading Ports (optional), "
                + qty(housings)
                + " Turbine Housings, "
                + qty(stages)
                + " Turbine Rotors, "
                + qty(injectors)
                + " Precision Injectors, both control blocks, the module hatches, fuel rods and assemblies.");
        BTSChat
            .warn(player, "Bill of materials (the same counts the Blueprint quests show) - grab via NEI in creative:");
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
        // The four plant modules, same one projection, same derived tally.
        tellBill(player, BTSBom.VENT.piece() + " (decay heat and radiation windows halved)", BTSBom.VENT);
        tellBill(
            player,
            BTSBom.SWITCHYARD.piece() + " (lifts the " + qty(MTEReactorCoreMk1.GRID_SOFT_CAP) + " EU/t output cap)",
            BTSBom.SWITCHYARD);
        tellBill(
            player,
            BTSBom.DERRICK.piece() + " (feedstock mine, "
                + MTEReactorCoreMk1.euRate(MTEReactorCoreMk1.DERRICK_EU_PER_TICK)
                + ")",
            BTSBom.DERRICK);
        tellBill(
            player,
            BTSBom.ANNEX.piece() + " (on-site yellowcake line, "
                + MTEReactorCoreMk1.euRate(MTEReactorCoreMk1.ANNEX_EU_PER_TICK)
                + ")",
            BTSBom.ANNEX);
        BTSChat.warn(player, " = Whole plant at tier " + tier + ": " + materials(BTSBom.TOTAL[tier]));
        BTSChat.send(
            player,
            " - Point-of-use sockets: Reactor Silo face = " + silo.of(BTSBom.MAINTENANCE_HATCH)
                + " Maintenance Hatch (beside the controller), "
                + silo.of(BTSBom.INPUT_HATCH_IV)
                + " Feedwater Input Hatch + "
                + silo.of(BTSBom.SILO_CONTROL_BLOCK)
                + " Silo Control Block + "
                + silo.of(BTSBom.OUTPUT_HATCH_IV)
                + " Output Hatch (row above), "
                + silo.of(BTSBom.INPUT_BUS_IV)
                + " Rod Input Bus + "
                + silo.of(BTSBom.OUTPUT_BUS_IV)
                + " Depleted Output Bus (row above that); Spiral chamber front wall = "
                + BTSBom.SPIRAL.of(BTSBom.INPUT_HATCH_IV)
                + " Input Hatches (coolant, propellant); output manifold back face = "
                + BTSBom.GALLERY.of(BTSBom.DYNAMO_HATCH_IV)
                + " Dynamo Hatches (4th row up), "
                + BTSBom.GALLERY.of(BTSBom.ENERGY_HATCH_IV)
                + " Energy Hatches (5th), "
                + BTSBom.GALLERY.of(BTSBom.LASER_SOURCE_HATCH)
                + " Laser Source Hatches (6th).");
        BTSChat.send(
            player,
            " - The projector places no Turbine Housing: the " + qty(housings)
                + " given cover the "
                + duct.of(BTSBom.TURBINE_HOUSING)
                + " Spine sockets + "
                + stages
                + " gallery stages. Seat a Turbine Rotor in every gallery stage.");
        BTSChat.send(
            player,
            " - Control blocks: Silo Control Block (silo face, directly above the controller) + Turbine Control Block (output manifold back face, above its Maintenance Hatch; each redstone pulse runs one turbine cycle).");
        BTSChat.send(
            player,
            " - Module sockets: Extraction Derrick = " + BTSBom.DERRICK.of(BTSBom.OUTPUT_BUS_IV)
                + " Output Bus; Enrichment Annex = "
                + BTSBom.ANNEX.of(BTSBom.INPUT_BUS_IV)
                + " Input Bus (Uraninite) + "
                + BTSBom.ANNEX.of(BTSBom.INPUT_HATCH_IV)
                + " Input Hatch (Hydrofluoric Acid) + "
                + BTSBom.ANNEX.of(BTSBom.OUTPUT_BUS_IV)
                + " Output Bus (yellowcake + thorium); the Vent Stack and Switchyard take none.");
        BTSChat.ok(
            player,
            "Build: place the Nuclear Plant Foundation; one hologram projection builds every piece (platform, Reactor Silo, Spine, Spiral chamber, Rotor Assembly, both Cooling Towers, the four modules). Tier: the GUI Build Tier or a 2-5 trigger stack; a standing silo is never re-tiered.");
        BTSChat.send(
            player,
            "Then: stack the pillar frames in the silo, and supply each piece where it is fed - rods and feedwater at the silo face, coolant and propellant at the Spiral chamber's front wall.");
    }
}
