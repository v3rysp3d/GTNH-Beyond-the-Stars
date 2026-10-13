package com.nateyburr.beyondthestars;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEProductionHallBase;
import com.nateyburr.beyondthestars.machines.MTEProductionHallGui;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.MTEReactorSiloGui;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.homogeny.HomogenyGui;
import com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.reality.HRKit;
import com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction;
import com.nateyburr.beyondthestars.machines.reality.MTERealityRefab;
import com.nateyburr.beyondthestars.machines.reality.RealityDiffractionGui;
import com.nateyburr.beyondthestars.machines.reality.RealityRefabGui;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): {@code /btshr}, the creative build and console test bench of the Homogeny,
 * Reality Diffraction and Reality Refab (the {@code /btseye} / {@code /btskerr} pattern, {@link SLConsoleTest}).
 * <ul>
 * <li><b>Player:</b> {@code /btshr <homogeny|diffraction|refab> [t1..t3] [fill] [force]} raises a complete, formed
 * machine in front of the player, its controller facing them (the Homogeny at the asked tier, installed to it).
 * {@code fill} charges the energy hatches and fills the input hatches; the footprint is surveyed first and more than
 * 15% solid refuses to build without {@code force}.</li>
 * <li><b>Console / command block:</b> {@code /btshr <machine> [t1..t3] <x> <y> <z> [facing] [fill] [force]
 * [survival] [check]} builds at the cell (loading the footprint's chunks first), prints FORMED / NOT FORMED with the
 * first wrong cells, then a creative re-projection check and the survival damage-and-repair test. {@code survival}
 * builds through the projector's survival path; {@code check} builds nothing and re-walks.</li>
 * <li>{@code /btshr keep <machine> <x> <y> <z> <facing> [t1..t3]} force-loads the footprint ({@code /btskerr
 * release} frees it).</li>
 * <li>{@code /btshr bench <x> <y> <z> <sub>} on a machine already standing: {@code status}, {@code check},
 * {@code fill}, {@code wrench}, {@code scan} (progress, the last recipe check, hatch contents), {@code on} /
 * {@code off}; Homogeny
 * {@code milestone <espm> <degenerate>}, {@code escrow}, {@code install}, {@code itick <n>}, {@code project <n>} (the
 * projector's creative build of rings 1..n over the standing machine, hatches in the new sockets, controller kept);
 * Refab {@code relink}; Diffraction {@code pending}.</li>
 * <li>{@code /btshr recipes}: the three machines' recipe maps and our Condensate Assembler / Generator recipes.</li>
 * <li>{@code /btshr gui <x> <y> <z> [info]}: the kit GUI's header and rows (or its Info page) as text, for the three
 * machines, the Reactor Silo and the production halls.</li>
 * </ul>
 */
public class CommandBTSHR extends CommandBase {

    private static final int GAP = 3;
    private static final int MAX_SOLID_PERCENT = 15;
    private static final int SURVEY_STRIDE = 4;
    private static final UUID CONSOLE_OWNER = UUID
        .nameUUIDFromBytes("beyondthestars:console-test".getBytes(StandardCharsets.UTF_8));

    @Override
    public String getCommandName() {
        return "btshr";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btshr <homogeny|diffraction|refab> [t1..t3] [fill] [force]  |  console: /btshr <machine> [t1..t3] "
            + "<x> <y> <z> [facing] [fill] [force] [survival] [check]  |  /btshr keep <machine> <x> <y> <z> <facing> "
            + "[t1..t3]  |  /btshr bench <x> <y> <z> <status|check|fill|wrench|scan|on|off|milestone e d|escrow|install|itick n|"
            + "project n|relink|pending>  |  /btshr gui <x> <y> <z> [info]  |  /btshr recipes";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(
            args,
            "homogeny",
            "diffraction",
            "refab",
            "keep",
            "bench",
            "gui",
            "recipes");
        return getListOfStringsMatchingLastWord(args, "t1", "t2", "t3", "fill", "force", "survival", "check");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            SLConsoleTest.tell(sender, getCommandUsage(sender));
            return;
        }
        if (args[0].equalsIgnoreCase("bench")) {
            for (String line : bench(sender, Arrays.copyOfRange(args, 1, args.length)))
                SLConsoleTest.tell(sender, line);
            return;
        }
        if (args[0].equalsIgnoreCase("gui")) {
            for (String line : gui(sender, Arrays.copyOfRange(args, 1, args.length))) SLConsoleTest.tell(sender, line);
            return;
        }
        if (args[0].equalsIgnoreCase("recipes")) {
            for (String line : HRKit.recipeLines()) SLConsoleTest.tell(sender, line);
            return;
        }
        if (args[0].equalsIgnoreCase("keep")) {
            keep(sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        final HRKit.Machine m = HRKit.Machine.parse(args[0]);
        if (m == null) {
            SLConsoleTest
                .tell(sender, EnumChatFormatting.RED + "Unknown machine '" + args[0] + "'. " + getCommandUsage(sender));
            return;
        }
        final String[] rest = Arrays.copyOfRange(args, 1, args.length);
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, rest);
        if (at != null) {
            processAt(sender, m, rest, at);
            return;
        }
        processPlayer(sender, m, rest);
    }

    private static int tiersArg(String[] args, boolean[] used, HRKit.Machine m) {
        int tiers = 1;
        for (int i = 0; i < args.length; i++) {
            if (used != null && used[i]) continue;
            final String a = args[i].toLowerCase(Locale.ROOT);
            if (a.matches("t[1-3]")) tiers = a.charAt(1) - '0';
        }
        return Math.max(1, Math.min(m.maxTier(), tiers));
    }

    private static boolean flag(String[] args, boolean[] used, String name) {
        for (int i = 0; i < args.length; i++) {
            if (used != null && used[i]) continue;
            if (args[i].equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    // ================================================================================ player

    private void processPlayer(ICommandSender sender, HRKit.Machine m, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;
        final int tiers = tiersArg(args, null, m);
        final boolean fill = flag(args, null, "fill");
        final boolean force = flag(args, null, "force") || flag(args, null, "-f");
        final String[] pieces = Arrays.copyOf(m.pieces, tiers);
        int front = 0;
        int below = 0;
        int above = 0;
        for (String piece : pieces) {
            final int[] off = BTSShapes.offset(piece);
            final int[] size = BTSShapes.size(piece);
            front = Math.max(front, off[2]);
            below = Math.max(below, size[1] - 1 - off[1]);
            above = Math.max(above, off[1]);
        }
        final ForgeDirection look = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
        final ForgeDirection facing = look.getOpposite();
        final int standOff = GAP + front + 1;
        final int x = MathHelper.floor_double(player.posX) + look.offsetX * standOff;
        final int z = MathHelper.floor_double(player.posZ) + look.offsetZ * standOff;
        final int y = MathHelper.floor_double(player.posY) + below;
        if (y + above > 255 || y - below < 1) {
            SLConsoleTest.tell(
                sender,
                EnumChatFormatting.RED + "No room: " + m.title() + " is " + (above + below + 1) + " blocks tall.");
            return;
        }
        final ExtendedFacing alignment = ExtendedFacing.of(facing);
        if (!force) {
            final int percent = solidPercent(world, alignment, x, y, z, pieces);
            if (percent > MAX_SOLID_PERCENT) {
                SLConsoleTest.tell(
                    sender,
                    EnumChatFormatting.RED + "Refusing to build: the footprint ahead of you is "
                        + percent
                        + "% solid and would be overwritten. Move to open ground (or the sky), or add force.");
                return;
            }
        }
        SLConsoleTest.loadChunks(world, SLConsoleTest.bounds(alignment, x, y, z, pieces));
        final HRKit.Result result = HRKit
            .build(world, x, y, z, facing, m, tiers, fill, player.getUniqueID(), player.getCommandSenderName(), false);
        if (result.failure != null) {
            SLConsoleTest.tell(sender, EnumChatFormatting.RED + m.title() + " build FAILED: " + result.failure + ".");
            return;
        }
        SLConsoleTest.tell(sender, EnumChatFormatting.AQUA + "== " + m.title() + " ==");
        for (String line : result.lines) SLConsoleTest.tell(sender, line);
        SLConsoleTest.tell(sender, "The controller is " + standOff + " blocks ahead of you; you are its owner.");
        if (m != HRKit.Machine.HOMOGENY) {
            SLConsoleTest.tell(
                sender,
                EnumChatFormatting.YELLOW + "BEC: run a BEC conduit from the BEC Hatch to a Containment Field"
                    + (m == HRKit.Machine.REFAB ? ", and stand a running Condensate Assembler within 64 blocks."
                        : "."));
        }
    }

    // =============================================================================== console

    private void processAt(ICommandSender sender, HRKit.Machine m, String[] args, SLConsoleTest.Placement at) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int tiers = tiersArg(args, at.used, m);
        final boolean fill = flag(args, at.used, "fill");
        final boolean force = flag(args, at.used, "force") || flag(args, at.used, "-f");
        final boolean survival = flag(args, at.used, "survival");
        final boolean check = flag(args, at.used, "check");
        final ExtendedFacing alignment = ExtendedFacing.of(at.facing);
        final String[] pieces = Arrays.copyOf(m.pieces, tiers);
        final int[] box = SLConsoleTest.bounds(alignment, at.x, at.y, at.z, pieces);
        final String label = m.title() + (m == HRKit.Machine.HOMOGENY ? " T" + tiers : "")
            + " facing "
            + at.facing.name()
                .toLowerCase(Locale.ROOT);
        if (box[1] < 1 || box[4] > 255) {
            SLConsoleTest.tell(
                sender,
                label + ": NOT BUILT - the footprint spans y " + box[1] + ".." + box[4] + ", outside 1..255.");
            return;
        }
        final int chunks = SLConsoleTest.loadChunks(world, box);
        if (check) {
            for (String line : HRKit.diagnose(world, at.x, at.y, at.z, m, tiers, false)) {
                SLConsoleTest.tell(sender, line);
            }
            return;
        }
        if (!force) {
            final int percent = solidPercent(world, alignment, at.x, at.y, at.z, pieces);
            if (percent > MAX_SOLID_PERCENT) {
                SLConsoleTest.tell(
                    sender,
                    label + ": NOT BUILT - the footprint is " + percent + "% solid; add force to overwrite.");
                return;
            }
        }
        final long start = System.nanoTime();
        final HRKit.Result result = HRKit
            .build(world, at.x, at.y, at.z, at.facing, m, tiers, fill, CONSOLE_OWNER, "BTS console", survival);
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
        for (String line : result.lines) SLConsoleTest.tell(sender, line);
        for (String line : HRKit.diagnose(world, at.x, at.y, at.z, m, tiers, true)) SLConsoleTest.tell(sender, line);
    }

    private void keep(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            SLConsoleTest.tell(sender, getCommandUsage(sender));
            return;
        }
        final HRKit.Machine m = HRKit.Machine.parse(args[0]);
        final String[] rest = Arrays.copyOfRange(args, 1, args.length);
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, rest);
        if (m == null || at == null) {
            SLConsoleTest.tell(sender, getCommandUsage(sender));
            return;
        }
        final int tiers = tiersArg(rest, at.used, m);
        final int[] box = SLConsoleTest
            .bounds(ExtendedFacing.of(at.facing), at.x, at.y, at.z, Arrays.copyOf(m.pieces, tiers));
        SLConsoleTest.tell(
            sender,
            m.title() + " test bench: forced " + KerrTestBench.keep(sender.getEntityWorld(), box) + " chunk(s).");
    }

    // =================================================================================== gui

    /**
     * GUI cleanup (controller-gui-spec.md 5.6-5.9): {@code /btshr gui <x> <y> <z> [info]} prints the header and the
     * status (or Info) rows the kit GUI of the controller there would show, built on the server exactly as the GUI
     * syncs them: the Homogeny, Reality Diffraction / Refab, the Reactor Silo (Nuclear Plant Foundation) and the four
     * production halls.
     */
    private static List<String> gui(ICommandSender sender, String[] args) {
        final List<String> out = new ArrayList<>();
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        if (at == null) {
            out.add("Usage: /btshr gui <x> <y> <z> [info]");
            return out;
        }
        boolean info = false;
        for (int i = 0; i < args.length; i++) if (!at.used[i] && args[i].equalsIgnoreCase("info")) info = true;
        final TileEntity te = sender.getEntityWorld()
            .getTileEntity(at.x, at.y, at.z);
        final IMetaTileEntity mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
            : null;
        final BTSControllerGui<?> g;
        if (mte instanceof MTEHomogeny) g = new HomogenyGui((MTEHomogeny) mte);
        else if (mte instanceof MTERealityDiffraction) g = new RealityDiffractionGui((MTERealityDiffraction) mte);
        else if (mte instanceof MTERealityRefab) g = new RealityRefabGui((MTERealityRefab) mte);
        else if (mte instanceof MTEReactorCoreMk1) g = new MTEReactorSiloGui((MTEReactorCoreMk1) mte);
        else if (mte instanceof MTEProductionHallBase) g = new MTEProductionHallGui((MTEProductionHallBase<?>) mte);
        else {
            out.add(
                "No Homogeny, Reality, Reactor Silo or production hall controller at " + at.x
                    + ", "
                    + at.y
                    + ", "
                    + at.z
                    + ".");
            return out;
        }
        out.addAll(g.benchDump(info));
        return out;
    }

    // ================================================================================= bench

    private List<String> bench(ICommandSender sender, String[] args) {
        final List<String> out = new ArrayList<>();
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        if (at == null) {
            out.add("Usage: /btshr bench <x> <y> <z> <sub> ...");
            return out;
        }
        final List<String> rest = new ArrayList<>();
        for (int i = 0; i < args.length; i++) if (!at.used[i]) rest.add(args[i]);
        final String sub = rest.isEmpty() ? "status"
            : rest.get(0)
                .toLowerCase(Locale.ROOT);
        final World world = sender.getEntityWorld();
        final TileEntity te = world.getTileEntity(at.x, at.y, at.z);
        final IMetaTileEntity mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
            : null;
        if (!(mte instanceof MTEExtendedPowerMultiBlockBase)
            || !(mte instanceof MTEHomogeny || mte instanceof MTERealityDiffraction
                || mte instanceof MTERealityRefab)) {
            out.add("No Homogeny / Reality controller at " + at.x + ", " + at.y + ", " + at.z + ".");
            return out;
        }
        final MTEExtendedPowerMultiBlockBase<?> machine = (MTEExtendedPowerMultiBlockBase<?>) mte;
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        switch (sub) {
            case "check":
                machine.checkStructure(true, base);
                out.add("Re-walked: " + (machine.mMachine ? "FORMED" : "NOT FORMED"));
                if (!machine.mMachine) {
                    out.addAll(SLConsoleTest.firstErrors(machine, base, 3));
                    machine.checkStructure(true, base); // firstErrors walks without clearing the hatch lists
                }
                break;
            case "fill":
                out.addAll(HRKit.fill(machine));
                break;
            case "wrench":
                if (mte instanceof MTEHomogeny) {
                    ((MTEHomogeny) mte).debugWrench();
                    out.add("Wrenched: the item NBT was written and read back.");
                } else if (mte instanceof MTERealityDiffraction) {
                    ((MTERealityDiffraction) mte).debugWrench();
                    out.add(
                        "Wrenched: the item NBT was written and read back; pending (L): "
                            + Arrays.toString(((MTERealityDiffraction) mte).debugPending()));
                } else {
                    out.add("wrench: only the Homogeny and the Diffraction keep state in item NBT.");
                }
                break;
            case "milestone":
                if (mte instanceof MTEHomogeny && rest.size() >= 3) {
                    ((MTEHomogeny) mte).debugMilestones(parseLong(rest.get(1)), parseLong(rest.get(2)));
                    out.add("Milestones set.");
                } else {
                    out.add("milestone <espm> <degenerate> (Homogeny only)");
                }
                break;
            case "escrow":
                if (mte instanceof MTEHomogeny) {
                    final MTEHomogeny h = (MTEHomogeny) mte;
                    final net.minecraft.item.Item core = MTEHomogeny.coreFor(h.installedTier() + 1);
                    h.debugEscrow(core == null ? null : new ItemStack(core));
                    out.add(core == null ? "At T3: no core." : "Core in escrow.");
                }
                break;
            case "install":
                if (mte instanceof MTEHomogeny) out.add(((MTEHomogeny) mte).beginInstall());
                break;
            case "itick":
                if (mte instanceof MTEHomogeny) {
                    final int n = rest.size() >= 2 ? (int) Math.min(MTEHomogeny.INSTALL_TICKS, parseLong(rest.get(1)))
                        : MTEHomogeny.INSTALL_TICKS;
                    out.add("Install ticks advanced: " + ((MTEHomogeny) mte).debugInstallTicks(n) + " of " + n);
                }
                break;
            case "scan":
                out.add(
                    "Progress " + machine.mProgresstime
                        + " / "
                        + machine.mMaxProgresstime
                        + " ticks, active "
                        + base.isActive()
                        + ", allowed "
                        + base.isAllowedToWork());
                out.add(
                    "Last recipe check: " + machine.getCheckRecipeResult()
                        .getID()
                        + " = \""
                        + machine.getCheckRecipeResult()
                            .getDisplayString()
                        + "\"");
                if (machine.mOutputFluids != null) {
                    for (net.minecraftforge.fluids.FluidStack f : machine.mOutputFluids) {
                        if (f != null) out.add("Running output: " + f.amount + " L " + f.getLocalizedName());
                    }
                }
                out.addAll(HRKit.hatchLines(machine));
                break;
            case "on":
                base.enableWorking();
                out.add("Enabled (as a soft mallet would).");
                break;
            case "off":
                base.disableWorking();
                out.add("Disabled (as a soft mallet would).");
                break;
            case "project":
                if (mte instanceof MTEHomogeny) {
                    final int n = rest.size() >= 2
                        ? (int) Math.max(1, Math.min(MTEHomogeny.MAX_TIER, parseLong(rest.get(1))))
                        : MTEHomogeny.MAX_TIER;
                    out.add(HRKit.project(world, machine, HRKit.Machine.HOMOGENY, n));
                } else {
                    out.add("project: Homogeny only (the Reality machines are one piece).");
                }
                break;
            case "relink":
                if (mte instanceof MTERealityRefab) {
                    out.add(((MTERealityRefab) mte).relink() ? "Linked." : "No Condensate Assembler within range.");
                }
                break;
            case "pending":
                if (mte instanceof MTERealityDiffraction) {
                    if (rest.size() >= 2) ((MTERealityDiffraction) mte).debugSeedPending(parseLong(rest.get(1)));
                    out.add("Pending (L): " + Arrays.toString(((MTERealityDiffraction) mte).debugPending()));
                }
                break;
            default:
                break;
        }
        out.addAll(HRKit.statusLines(mte));
        return out;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(
                s.replace(",", "")
                    .replace("_", ""));
        } catch (NumberFormatException bad) {
            return 0;
        }
    }

    /** Percentage of sampled cells of the pieces' bounding boxes that are already solid. */
    private static int solidPercent(World world, ExtendedFacing alignment, int x, int y, int z, String[] pieces) {
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        int solid = 0;
        int total = 0;
        for (String piece : pieces) {
            final int[] off = BTSShapes.offset(piece);
            final int[] size = BTSShapes.size(piece);
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
