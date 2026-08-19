package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
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

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.blocks.TileTurbineHousing;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * /btsbuild [1-5|all] [rod] [force] - builds COMPLETE, RUNNING Unit One plants where the
 * player stands.
 * <p>
 * The hologram projector can only preview one piece at a time and is height-capped, so this
 * is the way to get a whole plant on the ground for testing: the platform, silo, Spine,
 * Spiral, Turbine Generator and both Cooling Tower shells, every hatch and control block
 * fitted, rotors seated, water and fuel loaded, and the bench clock enabled so the turbine
 * spins without wiring redstone first.
 * <p>
 * `all` raises ALL FIVE tiers side by side in one go so every silo size can be walked and
 * compared. They are laid out along the plant's own RIGHT axis (which is the player's right,
 * since the controller is placed looking the way the player looks) on a fixed pitch of
 * platform + gap, so no two footprints can touch.
 */
public class CommandBTSBuild extends CommandBase {

    /** Walking corridor left between two neighbouring platforms in an `all` build. */
    private static final int SITE_GAP = 10;

    /**
     * Fraction of the sampled footprint that may already be solid before the command refuses
     * to build. Terrain gets overwritten silently, so anything above a light dusting of
     * blocks is treated as "you are about to bulldoze something" and needs `force`.
     */
    private static final double MAX_SOLID_FRACTION = 0.15;

    /** Vertical span of the site survey: the walking level and the four layers above it. */
    private static final int SURVEY_LAYERS = 5;

    /** Survey stride: every other cell in both horizontal axes (625 columns per site). */
    private static final int SURVEY_STRIDE = 2;

    @Override
    public String getCommandName() {
        return "btsbuild";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsbuild [1-5|all] [uranium|mox|thorium|naq] [force]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "1", "2", "3", "4", "5", "all");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "uranium", "mox", "thorium", "naq");
        }
        if (args.length == 3) {
            return getListOfStringsMatchingLastWord(args, "force");
        }
        return null;
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        boolean force = false;
        String tierArg = null;
        String rodArg = null;
        for (final String arg : args) {
            if (arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("-f")) {
                force = true;
            } else if (tierArg == null) {
                tierArg = arg;
            } else if (rodArg == null) {
                rodArg = arg;
            }
        }
        final boolean everyTier = tierArg != null && tierArg.equalsIgnoreCase("all");
        final int maxTier = MTEReactorCoreMk1.maxTier();
        final int singleTier = everyTier ? 0
            : tierArg == null ? 3 : Math.min(maxTier, Math.max(1, parseInt(sender, tierArg)));
        final net.minecraft.item.Item rod = rodFor(rodArg);

        final int[] tiers;
        if (everyTier) {
            tiers = new int[maxTier];
            for (int i = 0; i < maxTier; i++) tiers[i] = i + 1;
        } else {
            tiers = new int[] { singleTier };
        }

        // The controller stands where the player does, facing the way the player faces; the
        // plant then grows behind it (32 blocks) and 17 in front, 25 to either side.
        final int x = MathHelper.floor_double(player.posX);
        final int y = MathHelper.floor_double(player.posY);
        final int z = MathHelper.floor_double(player.posZ);
        final ForgeDirection facing = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
        // The alignment a freshly placed controller will take (rotation NORMAL, flip NONE),
        // so the sites can be surveyed and laid out BEFORE the first block is written.
        final ExtendedFacing alignment = ExtendedFacing.of(facing);
        final ForgeDirection right = alignment.getRelativeRightInWorld();

        // ---------------------------------------------------------------- the y limit
        int headroom = 0;
        for (final int tier : tiers) headroom = Math.max(headroom, MTEReactorCoreMk1.plantHeightFor(tier));
        if (y < 1) {
            tell(player, EnumChatFormatting.RED + "Too low: the platform's floor layer sits one block BELOW you.");
            return;
        }
        if (y + headroom > 255) {
            tell(
                player,
                EnumChatFormatting.RED
                    + (everyTier ? "Too high for an all-tier build: the tier " + maxTier + " silo"
                        : "Too high: the tier " + singleTier + " silo")
                    + " reaches "
                    + headroom
                    + " blocks up and would be clipped by the build limit. Stand at or below y="
                    + (255 - headroom)
                    + ".");
            return;
        }

        // ------------------------------------------------------------- where each one goes
        final int pitch = MTEReactorCoreMk1.platformSize() + SITE_GAP;
        final int[][] sites = new int[tiers.length][];
        for (int i = 0; i < tiers.length; i++) {
            sites[i] = new int[] { x + right.offsetX * pitch * i, y, z + right.offsetZ * pitch * i };
        }

        // ----------------------------------------------------- refuse to bulldoze a build
        if (!force) {
            final List<String> occupied = new ArrayList<>();
            for (int i = 0; i < tiers.length; i++) {
                final int percent = solidPercent(world, alignment, sites[i][0], sites[i][1], sites[i][2]);
                if (percent > MAX_SOLID_FRACTION * 100) {
                    occupied.add(
                        "tier " + tiers[i]
                            + " site at ("
                            + sites[i][0]
                            + ", "
                            + sites[i][1]
                            + ", "
                            + sites[i][2]
                            + ") is "
                            + percent
                            + "% solid");
                }
            }
            if (!occupied.isEmpty()) {
                tell(player, EnumChatFormatting.RED + "Refusing to build into occupied space:");
                for (final String line : occupied) tell(player, EnumChatFormatting.RED + "  - " + line + ".");
                tell(
                    player,
                    EnumChatFormatting.YELLOW + "Every block in a "
                        + MTEReactorCoreMk1.platformSize()
                        + "x"
                        + MTEReactorCoreMk1.platformSize()
                        + " footprint would be overwritten. Move to open ground, or append "
                        + EnumChatFormatting.WHITE
                        + "force"
                        + EnumChatFormatting.YELLOW
                        + " to build anyway.");
                return;
            }
        }

        // ------------------------------------------------------------------- build them
        final List<Site> built = new ArrayList<>();
        for (int i = 0; i < tiers.length; i++) {
            built.add(raise(world, sites[i][0], sites[i][1], sites[i][2], facing, tiers[i], rod));
        }

        // ------------------------------------------------------------------- the report
        if (everyTier) {
            tell(
                player,
                EnumChatFormatting.AQUA + "== Unit One, all "
                    + maxTier
                    + " tiers, side by side (pitch "
                    + pitch
                    + " blocks to your right) ==");
        } else {
            tell(player, EnumChatFormatting.AQUA + "== Unit One built (tier " + singleTier + ") ==");
        }
        for (final Site site : built) {
            report(player, site, everyTier);
        }
        tell(
            player,
            "Every controller faces " + facing.name()
                + "; each plant extends 32 blocks behind it, 17 in front, 25 to each side.");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Bench clock ON - the turbine cycles once a second without redstone. Wire a real clock to the Turbine Control Block (manifold back face) to control the rate, then /btsclock off.");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Infinite supplies ON - feedwater, coolant and propellant are refilled every tick and each cycle gets a perfectly timed 1L per injector, so it runs indefinitely. /btssupply off to plumb it for real.");
        tell(player, "Spine left all-CLOCKWISE (riding pattern). Right-click a housing to retune it.");
        tell(
            player,
            EnumChatFormatting.YELLOW
                + "The laser sources are UNPAINTED, so only the dynamos reach a cable. Paint each one and aim it down laser pipes at a same-tier, same-colour Laser Target Hatch to collect the rest.");
        if (everyTier) {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "/btsclock and /btssupply act on the NEAREST station only - stand next to the tier you want to retune.");
        }
    }

    /** One built plant and everything the report needs to say about it. */
    private static final class Site {

        int tier;
        int x, y, z;
        String failure;
        boolean formed;
        int rotors;
        int ports;
        int[] supplies = { 0, 0, 0 };
        long ceiling;
        long dynamos;
        long lasers;
        int laserHatches;
        String summary = "";
        String coolant = "coolant";
        String propellant = "propellant";
    }

    /**
     * Places one complete, running plant with its controller at (x, y, z). Mirrors the
     * original single-tier sequence exactly; the only addition is that the controller's own
     * build-tier setting is pinned first, because {@code tierFromTrigger} only reads the
     * trigger stack when its size is >1 and would otherwise silently fall back to the
     * setting's default for tier 1.
     */
    private static Site raise(World world, int x, int y, int z, ForgeDirection facing, int tier,
        net.minecraft.item.Item rod) {
        final Site site = new Site();
        site.tier = tier;
        site.x = x;
        site.y = y;
        site.z = z;

        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, idOf(BTSMachines.reactorCoreMk1), facing)) {
            site.failure = "could not place the main station here";
            return site;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) {
            site.failure = "main station did not initialise";
            return site;
        }
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(mte instanceof MTEReactorCoreMk1)) {
            site.failure = "unexpected machine at the build site";
            return site;
        }
        final MTEReactorCoreMk1 core = (MTEReactorCoreMk1) mte;

        // 1. Build every piece. Socket cells auto-fit their real hatch in creative placement.
        core.setBuildTierSetting(tier);
        core.construct(
            new ItemStack(BTSMachines.reactorCoreMk1.getItem(), tier, idOf(BTSMachines.reactorCoreMk1)),
            false);

        // 2. Form it, so the hatch/port/stage lists are populated for the steps below.
        core.checkStructure(true, (IGregTechTileEntity) te);

        // 3. Seat a fresh rotor in THIS plant's own housings (the Spine's 4 sockets and the
        // 8 gallery stages, as recorded by the structure walk) - never a neighbour's build.
        for (final int[] c : core.housingCells()) {
            if (!world.blockExists(c[0], c[1], c[2])) continue;
            if (world.getBlock(c[0], c[1], c[2]) != BTSBlocks.turbineHousing) continue;
            final TileEntity ht = world.getTileEntity(c[0], c[1], c[2]);
            if (ht instanceof TileTurbineHousing && !((TileTurbineHousing) ht).hasRotor()) {
                ((TileTurbineHousing) ht).installRotor(new ItemStack(BTSItems.turbineRotor, 1));
                site.rotors++;
            }
        }

        // 4. Stock each Rod Loading Port so the cascade has fuel to drop.
        site.ports = core.stockRodPorts(new ItemStack(rod, 64));

        // 5. Bench clock AND infinite supplies on, maintenance signed off (a freshly placed
        // controller has all six issues open and GT would stop it with NO_REPAIR before it
        // ever ran), then re-form.
        core.setDebugAutoCycle(true);
        core.setDebugInfiniteSupplies(true);
        core.fixAllIssues();
        core.getBaseMetaTileEntity()
            .enableWorking();
        core.checkStructure(true, (IGregTechTileEntity) te);
        core.fixAllIssues();

        // 6. Feedwater in the silo's hatches, the best coolant in the chamber's own hatch and
        // the best propellant in all eight injectors - then kept topped up every tick while
        // the flag is on, so the commanded plant reaches redline and STAYS there. Done after
        // the final re-form so it fills the hatch list the formed plant actually uses.
        site.supplies = core.refillDebugSupplies();

        final FluidStack coolant = core.bestCoolant(1);
        final FluidStack propellant = MTEReactorCoreMk1.bestPropellant(1);
        if (coolant != null) site.coolant = coolant.getLocalizedName();
        if (propellant != null) site.propellant = propellant.getLocalizedName();
        site.formed = core.mMachine;
        site.summary = core.buildSummary();
        site.ceiling = core.outputCeiling();
        site.dynamos = core.getDynamoCapacity();
        site.lasers = core.getLaserCapacity();
        site.laserHatches = core.getLaserHatchCount();
        return site;
    }

    private static void report(EntityPlayerMP player, Site site, boolean everyTier) {
        final String head = "Tier " + site.tier + " @ (" + site.x + ", " + site.y + ", " + site.z + ")";
        if (site.failure != null) {
            tell(player, EnumChatFormatting.RED + head + ": FAILED - " + site.failure + ".");
            return;
        }
        tell(
            player,
            (site.formed ? EnumChatFormatting.GREEN + head + ": FORMED"
                : EnumChatFormatting.RED + head + ": NOT FORMED") + EnumChatFormatting.RESET
                + " - "
                + site.summary
                + ".");
        tell(
            player,
            "  " + site.rotors
                + " rotors seated, "
                + site.ports
                + " rod ports stocked, "
                + site.supplies[0]
                + " feedwater + "
                + site.supplies[1]
                + " x "
                + site.coolant
                + " + "
                + site.supplies[2]
                + " x "
                + site.propellant
                + ".");
        tell(
            player,
            "  Output ceiling " + String.format("%,d", site.ceiling)
                + " EU/t (dynamos "
                + String.format("%,d", site.dynamos)
                + " + "
                + site.laserHatches
                + " laser sources "
                + String.format("%,d", site.lasers)
                + ").");
        if (!everyTier && !site.formed) {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "  Open the controller GUI: it names the missing piece. A NOT FORMED plant is a bug - please report the tier.");
        }
    }

    /**
     * Percentage of the site's sampled cells that already hold a block terrain generation
     * would not have put there loosely. Air, water, lava, grass and other replaceables read
     * as empty, so a lake or a meadow never blocks the build; stone, ores and anything the
     * player has placed do.
     */
    private static int solidPercent(World world, ExtendedFacing alignment, int x, int y, int z) {
        final int size = MTEReactorCoreMk1.platformSize();
        final int originA = MTEReactorCoreMk1.platformControllerA();
        final int originC = MTEReactorCoreMk1.platformControllerC();
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        int solid = 0;
        int total = 0;
        for (int a = 0; a < size; a += SURVEY_STRIDE) {
            for (int c = 0; c < size; c += SURVEY_STRIDE) {
                for (int b = 0; b < SURVEY_LAYERS; b++) {
                    // Structure space: +a is relative LEFT, +b is relative DOWN, +c is BACK.
                    abc[0] = a - originA;
                    abc[1] = -b;
                    abc[2] = c - originC;
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
