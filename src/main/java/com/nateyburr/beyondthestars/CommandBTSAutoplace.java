package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitJunction;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.misc.GTStructureChannels;

/**
 * Test bench for the optional hatch sockets: runs a machine's survival auto-place the way a player's projector or the
 * NEI multiblock preview does, then says what stands in every socket and whether the machine formed.
 * <p>
 * {@code /btsautoplace <kerr|eye|homogeny|quasar|crown|junction> x y z [tier n] [mode survival|bare|nei|optional]}
 * (console or op; tier is the Kerr / Homogeny tier, the quasar's coil tier or the crown's Mk):
 * <ul>
 * <li>{@code survival} (default): GT's hatch channel ON and an inventory that holds every block of the structure and
 * only the hatches the machine REQUIRES (no Directive, Sensor, Burst Control, GRB Receptor Port or BHC Utility
 * Hatch). The machine must form.</li>
 * <li>{@code bare}: the same with no hatches at all. Auto-place must finish (casing in every socket), never stall.</li>
 * <li>{@code nei}: what BlockRenderer6343's preview does: the hatch channel ON and a creative source that hands out
 * any hatch. No optional hatch may appear.</li>
 * <li>{@code optional}: {@code nei} plus the {@code bts_optional} channel: the optional sockets take their
 * hatches.</li>
 * </ul>
 * The controller faces north; the footprint's chunks are loaded first. Use a cleared superflat area.
 */
public class CommandBTSAutoplace extends CommandBase {

    static final String MACHINES = "kerr, eye, homogeny, quasar, crown, junction";

    @Override
    public String getCommandName() {
        return "btsautoplace";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsautoplace <" + MACHINES.replace(", ", "|") + "> x y z [tier n] [mode survival|bare|nei|optional]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 4) throw new WrongUsageException(getCommandUsage(sender));
        final String machine = args[0].toLowerCase();
        final World world = sender.getEntityWorld() != null ? sender.getEntityWorld() : DimensionManager.getWorld(0);
        final int x = parseInt(sender, args[1]);
        final int y = parseInt(sender, args[2]);
        final int z = parseInt(sender, args[3]);
        int tier = 1;
        String mode = "survival";
        for (int i = 4; i + 1 < args.length; i += 2) {
            if (args[i].equalsIgnoreCase("tier")) tier = parseIntBounded(sender, args[i + 1], 1, 9);
            else if (args[i].equalsIgnoreCase("mode")) mode = args[i + 1].toLowerCase();
        }
        for (String line : run(world, machine, x, y, z, tier, mode)) {
            BTSChat.send(sender, line);
            BeyondTheStars.LOG.info("[autoplace] " + line);
        }
    }

    /** One audited auto-place; the lines say what was placed in each socket and whether it formed. */
    static List<String> run(World world, String machine, int x, int y, int z, int tier, String mode) {
        final List<String> out = new ArrayList<>();
        final ItemStack controller;
        final String[] pieces;
        final List<ItemStack> required = new ArrayList<>();
        switch (machine) {
            case "kerr":
                controller = BTSMachines.kerrEngine;
                pieces = new String[] { tier >= 2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1 };
                add(required, ItemList.Hatch_Energy_UIV, ItemList.Hatch_Input_Bus_MAX);
                break;
            case "eye":
                controller = BTSMachines.eyeOfHarmony;
                pieces = new String[] { MTEEyeOfHarmonyBTS.PIECE_CORE };
                add(
                    required,
                    ItemList.Hatch_Input_UMV,
                    ItemList.Hatch_Output_Bus_ME,
                    ItemList.Hatch_Output_ME,
                    ItemList.Hatch_AutoMaintenance);
                break;
            case "homogeny":
                controller = BTSMachines.homogeny;
                pieces = tier >= 2 ? new String[] { MTEHomogeny.PIECE_T1, MTEHomogeny.PIECE_T2 }
                    : new String[] { MTEHomogeny.PIECE_T1 };
                add(required, ItemList.Hatch_Input_UMV, ItemList.Hatch_Output_UMV, ItemList.Hatch_Energy_UMV);
                break;
            // B-229: the quasar (tier = coil tier; the lattice piece is left out), the crown (tier = Mk) and the
            // junction
            case "quasar":
                controller = BTSMachines.quasarEngine;
                pieces = new String[] { MTEQuasarEngine.PIECE };
                add(required, ItemList.Hatch_Input_UXV, ItemList.Hatch_Energy_UXV);
                break;
            case "crown":
                controller = BTSMachines.burstThermaliserCrown;
                pieces = new String[] {
                    MTEBurstThermaliserCrown.PIECES[Math.min(tier, MTEBurstThermaliserCrown.PIECES.length) - 1] };
                break;
            case "junction":
                controller = BTSMachines.conduitJunction;
                pieces = new String[] { MTEConduitJunction.PIECE };
                add(required, ItemList.Hatch_Input_UMV);
                break;
            default:
                out.add("unknown machine '" + machine + "' (" + MACHINES + ")");
                return out;
        }
        if (controller == null) {
            out.add(machine + " is not registered");
            return out;
        }
        final ExtendedFacing facing = ExtendedFacing.of(ForgeDirection.NORTH);
        out.add(
            "chunks loaded: " + SLConsoleTest.loadChunks(world, SLConsoleTest.bounds(facing, x, y, z, pieces))
                + "; mode "
                + mode
                + ", tier "
                + tier);
        if (!MTEReactorCoreMk1
            .placeGtMachine(world, x, y, z, (short) controller.getItemDamage(), ForgeDirection.NORTH)) {
            out.add("could not place the controller");
            return out;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) {
            out.add("the controller did not initialise");
            return out;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        if (base.getOwnerUuid() == null) {
            base.setOwnerUuid(UUID.nameUUIDFromBytes("BTS console test".getBytes()));
            base.setOwnerName("BTS console");
        }
        final IMetaTileEntity mte = base.getMetaTileEntity();
        if (!(mte instanceof ISurvivalConstructable)) {
            out.add("not survival-constructable");
            return out;
        }

        // The trigger: a projector of stack size = tier, with the channels the mode asks for.
        final ItemStack trigger = new ItemStack(StructureLibAPI.getDefaultHologramItem(), tier);
        ChannelDataAccessor.setChannelData(trigger, GTStructureChannels.HATCH.get(), 1);
        if (mode.equals("optional"))
            ChannelDataAccessor.setChannelData(trigger, BTSStructures.OPTIONAL_HATCH_CHANNEL, 1);

        final IItemSource source;
        switch (mode) {
            case "bare":
                source = source(Collections.emptyList(), false);
                break;
            case "nei":
            case "optional":
                source = source(Collections.emptyList(), true);
                break;
            default:
                source = source(required, false);
        }
        final EntityPlayer actor = world instanceof WorldServer ? FakePlayerFactory.getMinecraft((WorldServer) world)
            : null;
        final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(source, actor);
        int rounds = 0, placed = 0, idle = 0;
        String end = "round cap hit";
        while (rounds < 2000) {
            rounds++;
            final int n = ((ISurvivalConstructable) mte).survivalConstruct(trigger, 25, env);
            if (n < 0) {
                end = "finished (-1)";
                break;
            }
            placed += n;
            idle = n == 0 ? idle + 1 : 0;
            if (idle >= 2) {
                end = "STALLED (0 placed twice)";
                break;
            }
        }
        out.add("auto-place: " + placed + " elements in " + rounds + " rounds, " + end);

        // What stands in each socket.
        for (String piece : pieces) {
            final Map<Character, int[][]> sockets = sockets(piece);
            for (Map.Entry<Character, int[][]> e : sockets.entrySet()) {
                final Map<String, Integer> what = new LinkedHashMap<>();
                for (int[] rel : e.getValue()) {
                    final int[] xyz = new int[3];
                    facing.getWorldOffset(rel, xyz);
                    final int px = x + xyz[0], py = y + xyz[1], pz = z + xyz[2];
                    final TileEntity t = world.getTileEntity(px, py, pz);
                    final String name;
                    if (t instanceof IGregTechTileEntity && ((IGregTechTileEntity) t).getMetaTileEntity() != null) {
                        name = ((IGregTechTileEntity) t).getMetaTileEntity()
                            .getLocalName();
                    } else {
                        final ItemStack s = new ItemStack(
                            world.getBlock(px, py, pz),
                            1,
                            world.getBlockMetadata(px, py, pz));
                        String n;
                        try {
                            n = s.getItem() != null ? s.getDisplayName()
                                : world.getBlock(px, py, pz)
                                    .getUnlocalizedName();
                        } catch (RuntimeException ex) {
                            n = world.getBlock(px, py, pz)
                                .getUnlocalizedName();
                        }
                        name = n;
                    }
                    what.merge(name, 1, Integer::sum);
                }
                out.add(piece + " socket '" + e.getKey() + "' (" + e.getValue().length + "): " + what);
            }
        }

        if (mte instanceof MTEMultiBlockBase) {
            final MTEMultiBlockBase multi = (MTEMultiBlockBase) mte;
            multi.checkStructure(true, base);
            out.add(multi.mMachine ? "FORMED" : "NOT FORMED");
            if (!multi.mMachine) {
                for (String err : SLConsoleTest.firstErrors(multi, base, 3)) out.add("  " + err);
            }
        }
        return out;
    }

    private static void add(List<ItemStack> list, ItemList... items) {
        for (ItemList item : items) {
            if (item != null && item.hasBeenSet()) list.add(item.get(1L));
        }
    }

    /** The lower-case socket chars of a piece and their offsets (sockets.ch in manifest.json). */
    private static Map<Character, int[][]> sockets(String piece) {
        final Map<Character, int[][]> m = new LinkedHashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            final int[][] rels = BTSShapes.socketRels(piece, c);
            if (rels.length > 0) m.put(c, rels);
        }
        return m;
    }

    /**
     * An item source with every exact stack (the structure's blocks) endlessly, and by predicate (hatches) either
     * only {@code hatches}, endlessly, or with {@code creative} any registered GT machine, the first that matches.
     */
    private static IItemSource source(List<ItemStack> hatches, boolean creative) {
        return new IItemSource() {

            @Override
            public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
                for (ItemStack h : hatches) {
                    if (predicate.test(h)) return Collections.singletonMap(h.copy(), count);
                }
                if (creative) {
                    for (IMetaTileEntity m : GregTechAPI.METATILEENTITIES) {
                        if (m == null) continue;
                        final ItemStack s = m.getStackForm(1L);
                        if (s != null && predicate.test(s)) return Collections.singletonMap(s, count);
                    }
                }
                return Collections.emptyMap();
            }

            @Override
            public boolean takeOne(ItemStack stack, boolean simulate) {
                return stack != null && stack.getItem() != null;
            }

            @Override
            public boolean takeAll(ItemStack stack, boolean simulate) {
                return stack != null && stack.getItem() != null;
            }
        };
    }
}
