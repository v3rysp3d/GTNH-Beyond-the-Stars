package com.nateyburr.beyondthestars;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;

/**
 * /btskerr [t1|t2] [force] - SINGULARITY LADDER P2 creative build: raises a complete, formed,
 * running Kerr Ergosphere Engine (both modules, every hatch, bolt buses stocked with a Black Hole
 * Seed and bolts, creative energy) a few blocks in front of the player, its controller facing the
 * player and the cage stretching away from them. Owned by the Kerr unit.
 * <p>
 * The cage is 65 blocks tall with the controller on its middle row, so the controller goes 32 blocks
 * above the player's feet and the cage's bottom row lands at foot level. The footprint is surveyed
 * first; more than 15% solid refuses to build without {@code force}.
 * <p>
 * <b>Console / command-block form:</b> {@code /btskerr [t1|t2] <x> <y> <z> [north|south|east|west]
 * [force] [survival]} puts the controller at x y z ("~" is relative to a command block) with its
 * front facing the given way (default north), loads the footprint's chunks first, and prints FORMED
 * or NOT FORMED with the first wrong cells, the mirror check and a re-projection check.
 * {@code survival} builds through the hologram projector's survival path instead of the creative one;
 * {@code check} builds nothing and re-walks the Kerr already standing there (e.g. after a restart).
 */
public class CommandBTSKerr extends CommandBase {

    /** Gap between the player and the controller's face. */
    private static final int STAND_OFF = 3;
    private static final double MAX_SOLID_FRACTION = 0.15;
    private static final int SURVEY_STRIDE = 4;

    @Override
    public String getCommandName() {
        return "btskerr";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btskerr [t1|t2] [force]  |  console: /btskerr [t1|t2] <x> <y> <z> [facing] [force] [survival] | [check]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "t1", "t2");
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, "force");
        return null;
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        if (at != null) {
            processAt(sender, args, at);
            return;
        }
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;
        boolean tier2 = false;
        boolean force = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("t2")) tier2 = true; // no bare "2": it reads as an x coordinate
            if (arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("-f")) force = true;
        }
        final String core = tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1;
        final int[] off = BTSShapes.offset(core);
        final int[] size = BTSShapes.size(core);
        final int below = size[1] - 1 - off[1];

        final ForgeDirection look = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
        final ForgeDirection facing = look.getOpposite();
        final int px = MathHelper.floor_double(player.posX);
        final int py = MathHelper.floor_double(player.posY);
        final int pz = MathHelper.floor_double(player.posZ);
        final int x = px + look.offsetX * STAND_OFF;
        final int z = pz + look.offsetZ * STAND_OFF;
        final int y = py + below;
        if (y + off[1] > 255 || py < 1) {
            tell(
                player,
                EnumChatFormatting.RED + "No room: the cage is "
                    + size[1]
                    + " blocks tall and its bottom row sits at your feet. Stand between y=1 and y="
                    + (255 - off[1] - below)
                    + ".");
            return;
        }

        final ExtendedFacing alignment = ExtendedFacing.of(facing);
        if (!force) {
            final int percent = solidPercent(world, alignment, x, y, z, core);
            if (percent > MAX_SOLID_FRACTION * 100) {
                tell(
                    player,
                    EnumChatFormatting.RED + "Refusing to build: the "
                        + size[0]
                        + "x"
                        + size[1]
                        + "x"
                        + size[2]
                        + " footprint ahead of you is "
                        + percent
                        + "% solid and would be overwritten. Move to open ground (or the sky), or add "
                        + EnumChatFormatting.WHITE
                        + "force"
                        + EnumChatFormatting.RED
                        + ".");
                return;
            }
        }

        final KerrKit.Result result = KerrKit.build(world, x, y, z, facing, tier2);
        if (result.failure != null) {
            tell(player, EnumChatFormatting.RED + "Kerr build FAILED: " + result.failure + ".");
            return;
        }
        tell(player, EnumChatFormatting.AQUA + "== Kerr Ergosphere Engine (" + (tier2 ? "T2" : "T1") + ") ==");
        for (String line : result.lines) {
            tell(player, line);
        }
        tell(
            player,
            "The controller is " + below
                + " blocks above your feet on the cage's front (governor arc); the cage runs "
                + (size[2] + 8)
                + " blocks away from you.");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Creative energy hatches (UMV, 16 A) and an auto-maintenance hatch are fitted. Bolt buses hold a "
                + "Black Hole Seed and SpaceTime bolts: the hole opens within a second. Feed the compressor arc's "
                + "input bus to compress; screwdriver = setpoint, sneak + screwdriver = mode.");
        if (!result.formed) {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "Open the controller GUI: it names the first wrong cell. A NOT FORMED creative build is a bug - "
                    + "please report it (and check the orientation note in the build spec).");
        }
    }

    /** The console / command-block form: build at the given cell and facing, then report. */
    private void processAt(ICommandSender sender, String[] args, SLConsoleTest.Placement at) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        boolean tier2 = false;
        boolean force = false;
        boolean survival = false;
        boolean check = false;
        for (int i = 0; i < args.length; i++) {
            if (at.used[i]) continue;
            final String arg = args[i];
            if (arg.equalsIgnoreCase("t2")) tier2 = true; // no bare "2": it reads as an x coordinate
            if (arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("-f")) force = true;
            if (arg.equalsIgnoreCase("survival")) survival = true;
            if (arg.equalsIgnoreCase("check")) check = true;
        }
        final String core = tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1;
        final ExtendedFacing alignment = ExtendedFacing.of(at.facing);
        final int[] box = SLConsoleTest
            .bounds(alignment, at.x, at.y, at.z, core, MTEKerrEngine.PIECE_COMPRESSOR, MTEKerrEngine.PIECE_EXTRACTOR);
        final String label = "Kerr " + (tier2 ? "T2" : "T1")
            + " facing "
            + at.facing.name()
                .toLowerCase();
        if (box[1] < 1 || box[4] > 255) {
            SLConsoleTest.tell(
                sender,
                label + ": NOT BUILT - the footprint spans y " + box[1] + ".." + box[4] + ", outside 1..255.");
            return;
        }
        final int chunks = SLConsoleTest.loadChunks(world, box);
        if (check) {
            for (String line : KerrKit.diagnose(world, at.x, at.y, at.z, at.facing, tier2, false)) {
                SLConsoleTest.tell(sender, line);
            }
            return;
        }
        if (!force) {
            final int percent = solidPercent(world, alignment, at.x, at.y, at.z, core);
            if (percent > MAX_SOLID_FRACTION * 100) {
                SLConsoleTest.tell(
                    sender,
                    label + ": NOT BUILT - the footprint is " + percent + "% solid; add force to overwrite.");
                return;
            }
        }
        final long start = System.nanoTime();
        final KerrKit.Result result = KerrKit.build(world, at.x, at.y, at.z, at.facing, tier2, survival);
        if (result.failure != null) {
            SLConsoleTest.tell(sender, label + ": BUILD FAILED - " + result.failure);
            return;
        }
        final long ms = (System.nanoTime() - start) / 1_000_000L;
        SLConsoleTest.tell(
            sender,
            "== " + label
                + (survival ? " (survival path)" : " (creative path)")
                + ": "
                + (result.formed ? "FORMED" : "NOT FORMED")
                + " == box ("
                + box[0]
                + ", "
                + box[1]
                + ", "
                + box[2]
                + ")..("
                + box[3]
                + ", "
                + box[4]
                + ", "
                + box[5]
                + "), "
                + chunks
                + " chunks loaded, built in "
                + ms
                + " ms");
        for (String line : result.lines) {
            SLConsoleTest.tell(sender, line);
        }
        for (String line : KerrKit.diagnose(world, at.x, at.y, at.z, at.facing, tier2, true)) {
            SLConsoleTest.tell(sender, line);
        }
    }

    /** Percentage of sampled cells of the piece's bounding box that are already solid. */
    private static int solidPercent(World world, ExtendedFacing alignment, int x, int y, int z, String core) {
        final int[] off = BTSShapes.offset(core);
        final int[] size = BTSShapes.size(core);
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        int solid = 0;
        int total = 0;
        for (int a = 0; a < size[0]; a += SURVEY_STRIDE) {
            for (int b = 0; b < size[1]; b += SURVEY_STRIDE) {
                for (int c = 0; c < size[2] + 8; c += SURVEY_STRIDE) {
                    abc[0] = a - off[0];
                    abc[1] = b - off[1];
                    abc[2] = c - off[2];
                    alignment.getWorldOffset(abc, xyz);
                    final int wx = x + xyz[0];
                    final int wy = y + xyz[1];
                    final int wz = z + xyz[2];
                    if (wy < 0 || wy > 255) continue;
                    total++;
                    final Block block = world.getBlock(wx, wy, wz);
                    if (block == null || block.isAir(world, wx, wy, wz)) continue;
                    if (block.isReplaceable(world, wx, wy, wz)) continue;
                    solid++;
                }
            }
        }
        return total == 0 ? 0 : solid * 100 / total;
    }
}
