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
import com.nateyburr.beyondthestars.machines.SLGateDump;
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarBench;

/**
 * /btseye [t1..t8] [gen 1-9] [fill] [force] - SINGULARITY LADDER P3 creative build: raises a
 * complete, formed Eye of Harmony (the core plus the bands for the asked tier, every hatch, the
 * Galactic core at T4, the tier's star chart in the controller slot) in front of the player, its
 * controller facing them. Owned by the Eye unit.
 * <p>
 * {@code gen n} sets all three field-generator types to tier n (default 9, the Gallifreyan tier);
 * {@code fill} tops every input buffer to 10^12 L and grants the player 10^18 wireless EU, so a run
 * starts at once. The plinth's bottom row lands at the player's feet; the T4 arms reach 28 blocks in
 * front of the controller, so the stand-off grows with the pieces built. The footprint is surveyed
 * first; more than 15% solid refuses to build without {@code force}.
 * <p>
 * <b>Console / command-block form:</b> {@code /btseye [t1..t8] <x> <y> <z> [north|south|east|west]
 * [gen 1-9] [fill] [force] [survival]} puts the controller at x y z ("~" is relative to a command
 * block) with its front facing the given way (default north), owned by a fixed test UUID; it loads
 * the footprint's chunks first and prints FORMED or NOT FORMED with the first wrong cells, the mirror
 * check and a re-projection check. {@code survival} builds through the hologram projector's survival
 * path instead of the creative one; {@code check} builds nothing and re-walks the Eye already standing
 * there (e.g. after a restart). D-49 test bench: {@code legacy} leaves the Field/Sensor/rim sockets and
 * the plinth input bus without hatches (a pre-D-49 Eye must still form); {@code nobeam} clears the beam
 * band after the build (bands are cumulative: the Eye must drop to structural T1).
 * <p>
 * {@code /btseye gates} dumps the Spacetime Compression Field Generator recipes the server has and
 * checks the compression-gate rule against them ({@link SLGateDump}).
 * <p>
 * {@code /btseye star <x> <y> <z> <sub> …} is the D-49 live-star bench ({@link EyeStarBench}).
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
        return "/btseye [t1..t8] [gen 1-9] [fill] [force]  |  console: /btseye [t1..t8] <x> <y> <z> [facing] "
            + "[gen 1-9] [fill] [force] [survival] [legacy] [nobeam] [oldwindows] | [check]  |  /btseye keep <x> <y> <z> <facing> [t1..t8]  |  /btseye gates  |  /btseye bec  |  "
            + EyeStarBench.USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return getListOfStringsMatchingLastWord(
            args,
            "t1",
            "t2",
            "t3",
            "t4",
            "t5",
            "t6",
            "t7",
            "t8",
            "gen",
            "fill",
            "force");
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("star")) {
            // D-49 test bench: the live star of the Eye at x y z (EyeStarBench).
            for (String line : EyeStarBench.run(sender, args)) {
                SLConsoleTest.tell(sender, line);
            }
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("bec")) {
            // Test bench: our Condensate Assembler / Generator recipes as the server holds them (B-71).
            for (String line : com.nateyburr.beyondthestars.BTSRecipesBEC.benchLines()) {
                SLConsoleTest.tell(sender, line);
            }
            for (String line : com.nateyburr.beyondthestars.BTSRecipesGeneratorUpgrade.benchLines()) {
                SLConsoleTest.tell(sender, line);
            }
            // the compression gate chambers as NEI lists them (SL-G01..G03 rewrite GT's own recipes in place)
            for (String line : com.nateyburr.beyondthestars.BTSRecipesSingularity.chamberLines()) {
                SLConsoleTest.tell(sender, line);
            }
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("gates")) {
            // Test bench: the compression-gate audit against the recipes this server actually has.
            SLGateDump.run(sender);
            return;
        }
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        if (at != null && args[0].equalsIgnoreCase("keep")) {
            // Test bench: force-load the Eye's footprint (as /btskerr keep), so a console test is not confounded
            // by the dev server unloading the centre-render cell's chunk between commands. /btskerr release frees.
            int tiers = 6;
            for (String arg : args) {
                if (arg.toLowerCase(java.util.Locale.ROOT)
                    .matches("t[1-8]")) tiers = arg.charAt(1) - '0';
            }
            final int[] box = SLConsoleTest.bounds(
                ExtendedFacing.of(at.facing),
                at.x,
                at.y,
                at.z,
                java.util.Arrays.copyOf(MTEEyeOfHarmonyBTS.PIECES, tiers));
            SLConsoleTest.tell(
                sender,
                "Eye test bench: forced " + com.nateyburr.beyondthestars.machines.kerr.KerrTestBench.keep(
                    sender.getEntityWorld(),
                    box) + " chunk(s) around the Eye T" + tiers + " at " + at.x + ", " + at.y + ", " + at.z);
            return;
        }
        if (at != null) {
            processAt(sender, args, at);
            return;
        }
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;
        int tiers = 1;
        int genMeta = 8;
        boolean fill = false;
        boolean force = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i].toLowerCase(java.util.Locale.ROOT);
            if (arg.matches("t[1-8]")) tiers = arg.charAt(1) - '0';
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
                + (fill
                    ? "Buffers filled and 10^18 EU granted. A T1-T3 star lights from a Stellar Ignition Charge in a"
                        + " plinth input bus (or '/btseye star <x> <y> <z> lit'); planets run at once. "
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

    /** Owner of console-built Eyes (its wireless EU network): a fixed, recognisable test identity. */
    private static final java.util.UUID CONSOLE_OWNER = java.util.UUID
        .nameUUIDFromBytes("beyondthestars:console-test".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /** The console / command-block form: build at the given cell and facing, then report. */
    private void processAt(ICommandSender sender, String[] args, SLConsoleTest.Placement at) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        int tiers = 1;
        int genMeta = 8;
        boolean fill = false;
        boolean force = false;
        boolean survival = false;
        boolean check = false;
        boolean legacy = false;
        boolean nobeam = false;
        boolean oldWindows = false;
        for (int i = 0; i < args.length; i++) {
            if (at.used[i]) continue;
            final String arg = args[i].toLowerCase(java.util.Locale.ROOT);
            if (arg.matches("t[1-8]")) tiers = arg.charAt(1) - '0';
            if (arg.equals("gen") && i + 1 < args.length && !at.used[i + 1]) {
                genMeta = parseIntBounded(sender, args[++i], 1, 9) - 1;
            }
            if (arg.equals("fill")) fill = true;
            if (arg.equals("force") || arg.equals("-f")) force = true;
            if (arg.equals("survival")) survival = true;
            if (arg.equals("check")) check = true;
            if (arg.equals("legacy")) legacy = true;
            if (arg.equals("nobeam")) nobeam = true;
            if (arg.equals("oldwindows")) oldWindows = true;
        }
        final ExtendedFacing alignment = ExtendedFacing.of(at.facing);
        final String[] pieces = java.util.Arrays.copyOf(MTEEyeOfHarmonyBTS.PIECES, tiers);
        final int[] box = SLConsoleTest.bounds(alignment, at.x, at.y, at.z, pieces);
        final String label = "Eye T" + tiers
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
            final long walkStart = System.nanoTime();
            final List<String> lines = EyeKit.diagnose(world, at.x, at.y, at.z, at.facing, tiers, false);
            final long walkUs = (System.nanoTime() - walkStart) / 1_000L;
            for (String line : lines) {
                SLConsoleTest.tell(sender, line);
            }
            SLConsoleTest.tell(sender, "check: walk + status in " + walkUs / 1000 + "." + walkUs % 1000 / 100 + " ms");
            return;
        }
        if (!force) {
            final int percent = solidPercent(world, alignment, at.x, at.y, at.z, tiers);
            if (percent > MAX_SOLID_FRACTION * 100) {
                SLConsoleTest.tell(
                    sender,
                    label + ": NOT BUILT - the footprint is " + percent + "% solid; add force to overwrite.");
                return;
            }
        }
        final long start = System.nanoTime();
        final EyeKit.Result result = EyeKit.build(
            world,
            at.x,
            at.y,
            at.z,
            at.facing,
            tiers,
            genMeta,
            fill,
            CONSOLE_OWNER,
            "BTS console",
            survival,
            legacy);
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
        for (String line : EyeKit.diagnose(world, at.x, at.y, at.z, at.facing, tiers, true)) {
            SLConsoleTest.tell(sender, line);
        }
        if (oldWindows) {
            // window-band bench: an Eye built before the windows has its old blocks in those cells; it must form
            int n = 0;
            for (String piece : pieces) {
                n += SLConsoleTest
                    .revertWindows(world, alignment, at.x, at.y, at.z, piece, MTEEyeOfHarmonyBTS.windowBefore());
            }
            SLConsoleTest.tell(sender, "oldwindows: put the pre-window blocks back into " + n + " cells; re-walk:");
            for (String line : EyeKit.diagnose(world, at.x, at.y, at.z, at.facing, tiers, false)) {
                SLConsoleTest.tell(sender, line);
            }
        }
        if (nobeam && tiers >= 2) {
            final int cleared = EyeKit.stripPiece(world, at.x, at.y, at.z, MTEEyeOfHarmonyBTS.PIECE_BEAM);
            SLConsoleTest.tell(sender, "nobeam: cleared " + cleared + " beam band cells; re-walk:");
            for (String line : EyeKit.diagnose(world, at.x, at.y, at.z, at.facing, tiers, false)) {
                SLConsoleTest.tell(sender, line);
            }
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
