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
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;

/**
 * /btseye [t1|t2|t3|t4] [gen 1-9] [fill] [force] - SINGULARITY LADDER P3 creative build: raises a
 * complete, formed Eye of Harmony (the core plus the bands for the asked tier, every hatch, the
 * Galactic core at T4, the tier's star chart in the controller slot) in front of the player, its
 * controller facing them. Owned by the Eye unit.
 * <p>
 * {@code gen n} sets all three field-generator types to tier n (default 9, the Gallifreyan tier);
 * {@code fill} tops every input buffer to 10^12 L and grants the player 10^18 wireless EU, so a run
 * starts at once. The plinth's bottom row lands at the player's feet; the T4 arms reach 28 blocks in
 * front of the controller, so the stand-off grows with the pieces built. The footprint is surveyed
 * first; more than 15% solid refuses to build without {@code force}.
 */
public class CommandBTSEye extends CommandBase {

    private static final int GAP = 3;
    private static final double MAX_SOLID_FRACTION = 0.15;
    private static final int SURVEY_STRIDE = 4;

    @Override
    public String getCommandName() {
        return "btseye";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btseye [t1|t2|t3|t4] [gen 1-9] [fill] [force]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return getListOfStringsMatchingLastWord(args, "t1", "t2", "t3", "t4", "gen", "fill", "force");
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;
        int tiers = 1;
        int genMeta = 8;
        boolean fill = false;
        boolean force = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i].toLowerCase(java.util.Locale.ROOT);
            if (arg.matches("t[1-4]")) tiers = arg.charAt(1) - '0';
            if (arg.equals("gen") && i + 1 < args.length) genMeta = parseIntBounded(sender, args[++i], 1, 9) - 1;
            if (arg.equals("fill")) fill = true;
            if (arg.equals("force") || arg.equals("-f")) force = true;
        }

        // How far the built pieces reach in front of and below the controller.
        int front = 0;
        int below = 0;
        for (int i = 0; i < tiers; i++) {
            final int[] off = BTSShapes.offset(MTEEyeOfHarmonyBTS.PIECES[i]);
            final int[] size = BTSShapes.size(MTEEyeOfHarmonyBTS.PIECES[i]);
            front = Math.max(front, off[2]);
            below = Math.max(below, size[1] - 1 - off[1]);
        }

        final ForgeDirection look = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
        final ForgeDirection facing = look.getOpposite();
        final int px = MathHelper.floor_double(player.posX);
        final int py = MathHelper.floor_double(player.posY);
        final int pz = MathHelper.floor_double(player.posZ);
        final int standOff = GAP + front + 1;
        final int x = px + look.offsetX * standOff;
        final int z = pz + look.offsetZ * standOff;
        final int y = py + below;
        if (y + 60 > 255 || py < 1) {
            tell(player, EnumChatFormatting.RED + "No room: the Eye is up to 57 blocks tall above your feet.");
            return;
        }

        final ExtendedFacing alignment = ExtendedFacing.of(facing);
        if (!force) {
            final int percent = solidPercent(world, alignment, x, y, z, tiers);
            if (percent > MAX_SOLID_FRACTION * 100) {
                tell(
                    player,
                    EnumChatFormatting.RED + "Refusing to build: the footprint ahead of you is "
                        + percent
                        + "% solid and would be overwritten. Move to open ground (or the sky), or add "
                        + EnumChatFormatting.WHITE
                        + "force"
                        + EnumChatFormatting.RED
                        + ".");
                return;
            }
        }

        final EyeKit.Result result = EyeKit.build(world, x, y, z, facing, tiers, genMeta, fill, player);
        if (result.failure != null) {
            tell(player, EnumChatFormatting.RED + "Eye build FAILED: " + result.failure + ".");
            return;
        }
        tell(player, EnumChatFormatting.AQUA + "== Eye of Harmony (T" + tiers + ") ==");
        for (String line : result.lines) {
            tell(player, line);
        }
        tell(
            player,
            "The controller is " + standOff
                + " blocks ahead of you on the plinth's front; you are its owner (wireless EU is debited from your "
                + "network).");
        tell(
            player,
            EnumChatFormatting.GREEN
                + (fill ? "Buffers filled and 10^18 EU granted: the star chart in the controller slot runs at once. "
                    : "Feed H/He into the plinth's input hatches, or re-run with 'fill'. ")
                + "Outputs need an ME network on the ME output bus / hatch.");
        if (!result.formed) {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "Open the controller GUI: it names the first wrong cell. A NOT FORMED creative build is a bug - "
                    + "please report it (and check the orientation note in the build spec).");
        }
    }

    /** Percentage of sampled cells of the built pieces' bounding boxes that are already solid. */
    private static int solidPercent(World world, ExtendedFacing alignment, int x, int y, int z, int tiers) {
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        int solid = 0;
        int total = 0;
        for (int i = 0; i < tiers; i++) {
            final int[] off = BTSShapes.offset(MTEEyeOfHarmonyBTS.PIECES[i]);
            final int[] size = BTSShapes.size(MTEEyeOfHarmonyBTS.PIECES[i]);
            for (int a = 0; a < size[0]; a += SURVEY_STRIDE) {
                for (int b = 0; b < size[1]; b += SURVEY_STRIDE) {
                    for (int c = 0; c < size[2]; c += SURVEY_STRIDE) {
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
        }
        return total == 0 ? 0 : solid * 100 / total;
    }
}
