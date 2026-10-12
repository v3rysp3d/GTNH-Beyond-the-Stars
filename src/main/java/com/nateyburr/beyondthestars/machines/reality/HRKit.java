package com.nateyburr.beyondthestars.machines.reality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.items.SLMaxItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import tectech.thing.CustomItemList;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): the creative test path for the three machines. {@code /btskit homogeny |
 * diffraction | refab} hands over the parts and prints each piece's bill of materials; {@code /btshr} raises a
 * complete, formed machine (see {@code CommandBTSHR}). Nothing here is typed by hand (D-22): socket positions and
 * bills come from {@code manifest.json} and the shapes the controllers walk.
 */
public final class HRKit {

    private HRKit() {}

    /** The three machines. */
    public enum Machine {

        HOMOGENY("homogeny", MTEHomogeny.PIECES),
        DIFFRACTION("reality_diffraction", new String[] { MTERealityDiffraction.PIECE }),
        REFAB("reality_refab", new String[] { MTERealityRefab.PIECE });

        /** The manifest's machine key. */
        public final String key;
        public final String[] pieces;

        Machine(String key, String[] pieces) {
            this.key = key;
            this.pieces = pieces;
        }

        public ItemStack controller() {
            switch (this) {
                case HOMOGENY:
                    return BTSMachines.homogeny;
                case DIFFRACTION:
                    return BTSMachines.realityDiffraction;
                default:
                    return BTSMachines.realityRefab;
            }
        }

        public int maxTier() {
            return pieces.length;
        }

        public String title() {
            switch (this) {
                case HOMOGENY:
                    return "The Homogeny";
                case DIFFRACTION:
                    return "Reality Diffraction";
                default:
                    return "Reality Refab";
            }
        }

        /** "homogeny" / "diffraction" / "refab" (and a few aliases), or null. */
        public static Machine parse(String text) {
            switch (text.toLowerCase(Locale.ROOT)) {
                case "homogeny":
                case "mixer":
                case "homog":
                    return HOMOGENY;
                case "diffraction":
                case "diff":
                case "realitydiffraction":
                    return DIFFRACTION;
                case "refab":
                case "realityrefab":
                    return REFAB;
                default:
                    return null;
            }
        }
    }

    // ===================================================================================== kit

    /** {@code /btskit <machine>}: the controller, every hatch the sockets take, the cores and cells for a test. */
    public static void giveKit(EntityPlayerMP player, Machine m) {
        give(player, copy(m.controller()));
        int feeds = 0;
        for (String piece : m.pieces) feeds += sockets(piece, 'h');
        giveMany(player, ItemList.Hatch_Input_MAX.get(1L), feeds);
        giveMany(player, energyHatch(), sockets(m.pieces[0], 'e'));
        switch (m) {
            case HOMOGENY:
                giveMany(player, ItemList.Hatch_Output_MAX.get(1L), sockets(m.pieces[0], 'o'));
                give(player, item(SLMaxItems.homogenyTier2Core));
                give(player, item(SLMaxItems.homogenyTier3Core));
                give(player, cell(fluidOf(Materials.RawStarMatter.getFluid(1L))));
                break;
            case DIFFRACTION:
                giveMany(player, ItemList.Hatch_Input_MAX.get(1L), 6);
                give(player, CustomItemList.Hatch_BEC_Connector.get(1L));
                give(player, cell(SLRealityFluids.entanglementMedium));
                for (int k = 0; k < 6; k++) give(player, cell(MTERealityDiffraction.anchorFluid(k)));
                break;
            default:
                giveMany(player, ItemList.Hatch_Input_Bus_MAX.get(1L), sockets(m.pieces[0], 'i'));
                give(player, ItemList.Hatch_Output_Bus_MAX.get(1L));
                give(player, CustomItemList.Hatch_BEC_Connector.get(1L));
                break;
        }
        player.inventoryContainer.detectAndSendChanges();
        tell(player, EnumChatFormatting.AQUA + "== " + m.title() + " test kit ==");
        tell(
            player,
            "Given: the controller and a hatch for every socket"
                + (m == Machine.HOMOGENY ? ", the Tier II and III cores and a cell of Raw Star Matter"
                    : m == Machine.DIFFRACTION ? ", a BEC Hatch and cells of the medium and the six anchors"
                        : ", a BEC Hatch")
                + ".");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        for (String piece : m.pieces) tell(player, " - " + piece + ": " + bill(piece));
        tell(
            player,
            EnumChatFormatting.GREEN + "Fastest look: /btshr "
                + m.name()
                    .toLowerCase(Locale.ROOT)
                + (m == Machine.HOMOGENY ? " [t1..t3]" : "")
                + " [fill] raises a complete, formed machine in front of you. Or a hologram projector on the controller"
                + (m == Machine.HOMOGENY ? ": stack 1..3 = T1..T3." : "."));
    }

    private static ItemStack energyHatch() {
        return CustomItemList.eM_energyMulti64_UXV.get(1L);
    }

    private static ItemStack copy(ItemStack s) {
        return s == null ? null : s.copy();
    }

    private static ItemStack item(Item item) {
        return item == null ? null : new ItemStack(item, 1, 0);
    }

    private static Fluid fluidOf(FluidStack s) {
        return s == null ? null : s.getFluid();
    }

    /** A Neutronium Large Fluid Cell filled to capacity with {@code fluid}, or null. */
    private static ItemStack cell(Fluid fluid) {
        if (fluid == null) return null;
        final ItemStack cell = ItemList.Large_Fluid_Cell_Neutronium.get(1L);
        if (cell == null || !(cell.getItem() instanceof IFluidContainerItem)) return null;
        ((IFluidContainerItem) cell.getItem()).fill(cell, new FluidStack(fluid, Integer.MAX_VALUE), true);
        return cell;
    }

    /** "17,783 blocks: 5,008x Quantum Glass, ..." from the manifest. */
    public static String bill(String piece) {
        final JsonObject entry = BTSShapes.manifestPiece(piece);
        if (entry == null || !entry.has("bom")) return "(not in manifest)";
        final StringBuilder sb = new StringBuilder();
        for (JsonElement el : entry.getAsJsonArray("bom")) {
            final JsonObject row = el.getAsJsonObject();
            if ("~".equals(
                row.get("ch")
                    .getAsString()))
                continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(
                qty(
                    row.get("count")
                        .getAsInt()))
                .append("x ")
                .append(
                    shortBlock(
                        row.get("block")
                            .getAsString()));
        }
        return sb.toString();
    }

    private static String shortBlock(String text) {
        String s = text.replace("NEW ", "")
            .replace("Casings.", "");
        final int paren = s.indexOf(" (");
        if (paren > 0) s = s.substring(0, paren);
        final int comma = s.indexOf(", ");
        if (comma > 0) s = s.substring(0, comma);
        return s;
    }

    private static int sockets(String piece, char ch) {
        return socketRels(piece, ch).length;
    }

    /** Every socket of one char in a piece, as a/b/c offsets from the controller. Empty if none. */
    public static int[][] socketRels(String piece, char ch) {
        try {
            final JsonObject entry = BTSShapes.manifestPiece(piece);
            if (entry == null || !entry.has("sockets")) return new int[0][];
            final JsonObject all = entry.getAsJsonObject("sockets");
            final String key = String.valueOf(ch);
            if (!all.has(key)) return new int[0][];
            final JsonArray list = all.getAsJsonArray(key);
            final int[][] out = new int[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                final JsonArray rel = list.get(i)
                    .getAsJsonObject()
                    .getAsJsonArray("rel");
                out[i] = new int[] { rel.get(0)
                    .getAsInt(),
                    rel.get(1)
                        .getAsInt(),
                    rel.get(2)
                        .getAsInt() };
            }
            return out;
        } catch (RuntimeException bad) {
            return new int[0][];
        }
    }

    /** The machine axis (the render centre) as a/b/c from the controller, from the manifest; {0,0,0} if absent. */
    private static int[] centreRel(Machine m) {
        try {
            final JsonObject machines = BTSShapes.manifest()
                .getAsJsonObject("machines");
            final JsonArray rel = machines.getAsJsonObject(m.key)
                .getAsJsonObject("centre")
                .getAsJsonArray("rel");
            return new int[] { rel.get(0)
                .getAsInt(),
                rel.get(1)
                    .getAsInt(),
                rel.get(2)
                    .getAsInt() };
        } catch (RuntimeException missing) {
            return new int[] { 0, 0, 0 };
        }
    }

    // ========================================================================= creative build

    /** Result of one {@link #build}: whether it formed, and the lines to print. */
    public static final class Result {

        public boolean formed;
        public String failure;
        public final List<String> lines = new ArrayList<>();
    }

    private static final int SURVIVAL_BUDGET = 25;
    private static final int SURVIVAL_MAX_ROUNDS = 6000;

    /**
     * Raises machine {@code m} with its controller at (x, y, z) facing {@code facing}: the first {@code tiers} pieces
     * (the Homogeny's tiers; 1 for the Reality machines), a hatch in every socket (energy: a 64 A UXV multi-amp hatch),
     * the Homogeny installed to its tier, the player as owner; with {@code fill}, the input hatches filled (the
     * Homogeny's feeds with its tier's inputs, the Diffraction's medium and anchors) and the energy hatches charged.
     * With {@code survival} the blocks go in through {@code survivalConstruct} (the projector's survival path, from an
     * endless source). Then forms it. Server only.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, Machine m, int tiers,
        boolean fill, UUID owner, String ownerName, boolean survival) {
        final Result result = new Result();
        final ItemStack controller = m.controller();
        if (controller == null) {
            result.failure = "the controller is not registered";
            return result;
        }
        tiers = Math.max(1, Math.min(m.maxTier(), tiers));
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) controller.getItemDamage(), facing)) {
            result.failure = "could not place the controller";
            return result;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEExtendedPowerMultiBlockBase)) {
            result.failure = "the controller did not initialise";
            return result;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEExtendedPowerMultiBlockBase<?> mte = (MTEExtendedPowerMultiBlockBase<?>) base.getMetaTileEntity();
        base.setOwnerUuid(owner);
        base.setOwnerName(ownerName);

        final ItemStack trigger = new ItemStack(controller.getItem(), tiers, controller.getItemDamage());
        if (survival) {
            final SLConsoleTest.SurvivalRun run = SLConsoleTest.survivalBuild(
                (com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable) mte,
                world,
                trigger,
                SURVIVAL_BUDGET,
                SURVIVAL_MAX_ROUNDS);
            result.lines.add(
                "Survival auto-place: " + qty(run.placed)
                    + " blocks in "
                    + run.rounds
                    + " rounds, "
                    + (run.finished ? "finished (-1)" : run.stalled ? "STALLED (0 placed twice)" : "round cap hit"));
        } else {
            ((com.gtnewhorizon.structurelib.alignment.constructable.IConstructable) mte).construct(trigger, false);
        }
        final int fitted = fitHatches(world, mte, m, tiers);

        if (mte instanceof MTEHomogeny) ((MTEHomogeny) mte).debugInstall(tiers);
        mte.checkStructure(true, base);
        base.enableWorking();
        if (fill) result.lines.addAll(fill(mte));

        result.formed = mte.mMachine;
        result.lines.add(
            (result.formed ? EnumChatFormatting.GREEN + m.title() + " FORMED"
                : EnumChatFormatting.RED + m.title() + " NOT FORMED") + EnumChatFormatting.RESET
                + (m == Machine.HOMOGENY ? " (T" + tiers + ")" : "")
                + " at ("
                + x
                + ", "
                + y
                + ", "
                + z
                + "), facing "
                + facing.name()
                + "; "
                + fitted
                + " hatches fitted.");
        result.lines.addAll(statusLines(mte));
        return result;
    }

    /**
     * A hatch in every socket of the first {@code tiers} pieces (energy: a 64 A UXV multi-amp hatch); a socket that
     * already holds a tile is left alone. Returns the hatches placed. Server only.
     */
    public static int fitHatches(World world, MTEExtendedPowerMultiBlockBase<?> mte, Machine m, int tiers) {
        final ExtendedFacing alignment = mte.getExtendedFacing();
        final int[] centre = centreRel(m);
        final IGregTechTileEntity base = mte.getBaseMetaTileEntity();
        final int x = base.getXCoord();
        final int y = base.getYCoord();
        final int z = base.getZCoord();
        int fitted = 0;
        for (int i = 0; i < tiers; i++) {
            final String piece = m.pieces[i];
            for (int[] rel : socketRels(piece, 'h')) {
                if (fit(world, alignment, x, y, z, rel, centre, ItemList.Hatch_Input_MAX.get(1L))) fitted++;
            }
        }
        final String p0 = m.pieces[0];
        for (int[] rel : socketRels(p0, 'e')) {
            if (fit(world, alignment, x, y, z, rel, centre, energyHatch())) fitted++;
        }
        switch (m) {
            case HOMOGENY:
                for (int[] rel : socketRels(p0, 'o')) {
                    if (fit(world, alignment, x, y, z, rel, centre, ItemList.Hatch_Output_MAX.get(1L))) fitted++;
                }
                break;
            case DIFFRACTION:
                for (char ch : MTERealityDiffraction.ANCHOR_CHARS) {
                    for (int[] rel : socketRels(p0, ch)) {
                        if (fit(world, alignment, x, y, z, rel, centre, ItemList.Hatch_Input_MAX.get(1L))) fitted++;
                    }
                }
                for (int[] rel : socketRels(p0, 'b')) {
                    if (fit(world, alignment, x, y, z, rel, centre, CustomItemList.Hatch_BEC_Connector.get(1L))) {
                        fitted++;
                    }
                }
                break;
            default:
                for (int[] rel : socketRels(p0, 'i')) {
                    if (fit(world, alignment, x, y, z, rel, centre, ItemList.Hatch_Input_Bus_MAX.get(1L))) fitted++;
                }
                for (int[] rel : socketRels(p0, 'o')) {
                    if (fit(world, alignment, x, y, z, rel, centre, ItemList.Hatch_Output_Bus_MAX.get(1L))) fitted++;
                }
                for (int[] rel : socketRels(p0, 'b')) {
                    if (fit(world, alignment, x, y, z, rel, centre, CustomItemList.Hatch_BEC_Connector.get(1L))) {
                        fitted++;
                    }
                }
                break;
        }
        return fitted;
    }

    /** The machine's status lines (Homogeny or Reality). */
    public static List<String> statusLines(IMetaTileEntity mte) {
        if (mte instanceof MTEHomogeny) return ((MTEHomogeny) mte).statusLines();
        if (mte instanceof MTERealityBase) return ((MTERealityBase<?>) mte).statusLines();
        return new ArrayList<>();
    }

    /**
     * Bench {@code fill}: charges every energy hatch to full and fills the input hatches (the Homogeny's feeds with
     * Raw Star Matter, ESPM and the two dwarf matters; the Diffraction's medium and its six anchors, each in its own
     * socket). Returns what it did.
     */
    public static List<String> fill(MTEExtendedPowerMultiBlockBase<?> mte) {
        final List<String> out = new ArrayList<>();
        int charged = 0;
        for (MTEHatch h : mte.getExoticAndNormalEnergyHatchList()) {
            h.setEUVar(h.maxEUStore());
            charged++;
        }
        int filled = 0;
        if (mte instanceof MTEHomogeny) {
            final Fluid[] feed = { fluidOf(Materials.RawStarMatter.getFluid(1L)), SLFluids.excitedStellarPlasma,
                fluidOf(Materials.WhiteDwarfMatter.getMolten(1L)), fluidOf(Materials.BlackDwarfMatter.getMolten(1L)),
                SLRealityFluids.degenerateStellarMixture, SLRealityFluids.superdenseErgomatter };
            int i = 0;
            for (MTEHatchInput h : mte.mInputHatches) {
                if (i >= feed.length) break;
                if (feed[i] != null && h.fill(new FluidStack(feed[i], Integer.MAX_VALUE), true) > 0) filled++;
                i++;
            }
        } else if (mte instanceof MTERealityDiffraction) {
            final MTERealityDiffraction d = (MTERealityDiffraction) mte;
            for (MTEHatchInput h : mte.mInputHatches) {
                if (h.fill(new FluidStack(SLRealityFluids.entanglementMedium, Integer.MAX_VALUE), true) > 0) filled++;
            }
            filled += d.debugFillAnchors();
        }
        out.add("fill: " + charged + " energy hatches charged, " + filled + " input hatches filled.");
        return out;
    }

    /**
     * Console checks on a machine {@link #build} raised: the first wrong cells if it did not form, a creative
     * re-projection over the finished machine (it must stay formed: no hatch paved over), and the survival repair
     * test ({@link SLConsoleTest#damageAndRepair}). With {@code full} false only the walk and status (the
     * {@code check} form).
     */
    public static List<String> diagnose(World world, int x, int y, int z, Machine m, int tiers, boolean full) {
        final List<String> out = new ArrayList<>();
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEExtendedPowerMultiBlockBase)) {
            out.add("No " + m.title() + " controller at the build point.");
            return out;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEExtendedPowerMultiBlockBase<?> mte = (MTEExtendedPowerMultiBlockBase<?>) base.getMetaTileEntity();
        if (!full) mte.checkStructure(true, base);
        out.add(m.title() + " at (" + x + ", " + y + ", " + z + "): " + (mte.mMachine ? "FORMED" : "NOT FORMED"));
        if (!mte.mMachine) {
            out.add("First wrong cells:");
            for (String line : SLConsoleTest.firstErrors(mte, base, 3)) out.add("  " + line);
            mte.checkStructure(true, base);
        }
        if (full && mte.mMachine) {
            final ItemStack controller = m.controller();
            final ItemStack trigger = new ItemStack(controller.getItem(), tiers, controller.getItemDamage());
            ((com.gtnewhorizon.structurelib.alignment.constructable.IConstructable) mte).construct(trigger, false);
            mte.checkStructure(true, base);
            out.add(
                "Creative re-projection over the formed machine: " + (mte.mMachine ? "still FORMED"
                    : "BROKE IT - " + String.join("; ", SLConsoleTest.firstErrors(mte, base, 2))));
            if (mte.mMachine) {
                out.addAll(
                    damageAndRepair(
                        mte,
                        base,
                        trigger,
                        SLConsoleTest.bounds(
                            mte.getExtendedFacing(),
                            x,
                            y,
                            z,
                            Arrays.copyOf(m.pieces, Math.max(1, Math.min(m.maxTier(), tiers))))));
            }
        }
        out.addAll(statusLines(mte));
        return out;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static List<String> damageAndRepair(MTEExtendedPowerMultiBlockBase<?> mte, IGregTechTileEntity base,
        ItemStack trigger, int[] box) {
        if (mte instanceof MTEHomogeny) return SLConsoleTest.damageAndRepair((MTEHomogeny) mte, base, trigger, box);
        if (mte instanceof MTERealityDiffraction) {
            return SLConsoleTest.damageAndRepair((MTERealityDiffraction) mte, base, trigger, box);
        }
        if (mte instanceof MTERealityRefab) {
            return SLConsoleTest.damageAndRepair((MTERealityRefab) mte, base, trigger, box);
        }
        return new ArrayList<>();
    }

    /** Places the GT machine {@code stack} at the socket, facing away from the machine's axis (not over a GT tile). */
    private static boolean fit(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel, int[] centre,
        ItemStack stack) {
        if (stack == null) return false;
        final int[] p = SLConsoleTest.worldPos(alignment, x, y, z, rel);
        if (world.getTileEntity(p[0], p[1], p[2]) instanceof IGregTechTileEntity) return false;
        final int[] c = SLConsoleTest.worldPos(alignment, x, y, z, centre);
        ForgeDirection best = alignment.getDirection();
        double bestDot = Double.NEGATIVE_INFINITY;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (!world.isAirBlock(p[0] + d.offsetX, p[1] + d.offsetY, p[2] + d.offsetZ)) continue;
            final double dot = d.offsetX * (p[0] - c[0]) + d.offsetY * (p[1] - c[1]) + d.offsetZ * (p[2] - c[2]);
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return MTEReactorCoreMk1.placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), best);
    }

    // ================================================================================ bench

    /**
     * Bench {@code project <n>}: the projector's creative build of pieces 1..n over a standing machine (the controller,
     * its NBT and the hatches already in place are kept), a hatch in every new socket, then a re-walk. Server only.
     */
    public static String project(World world, MTEExtendedPowerMultiBlockBase<?> mte, Machine m, int tiers) {
        final ItemStack controller = m.controller();
        final int n = Math.max(1, Math.min(m.maxTier(), tiers));
        final IGregTechTileEntity base = mte.getBaseMetaTileEntity();
        SLConsoleTest.loadChunks(
            world,
            SLConsoleTest.bounds(
                mte.getExtendedFacing(),
                base.getXCoord(),
                base.getYCoord(),
                base.getZCoord(),
                Arrays.copyOf(m.pieces, n)));
        ((com.gtnewhorizon.structurelib.alignment.constructable.IConstructable) mte)
            .construct(new ItemStack(controller.getItem(), n, controller.getItemDamage()), false);
        final int fitted = fitHatches(world, mte, m, n);
        mte.checkStructure(true, base);
        return "Projected pieces 1.." + n
            + ", "
            + fitted
            + " new hatch(es) fitted: "
            + (mte.mMachine ? "FORMED" : "NOT FORMED");
    }

    /** Bench {@code scan}: what the input and output hatches hold, and the stored EU. */
    public static List<String> hatchLines(MTEExtendedPowerMultiBlockBase<?> mte) {
        final List<String> out = new ArrayList<>();
        final StringBuilder in = new StringBuilder("Input hatches:");
        for (MTEHatchInput h : mte.mInputHatches) {
            final FluidStack f = h.getFluid();
            in.append(' ')
                .append(f == null ? "empty" : qty(f.amount) + " L " + f.getLocalizedName())
                .append(';');
        }
        out.add(in.toString());
        final StringBuilder o = new StringBuilder("Output hatches:");
        for (gregtech.api.metatileentity.implementations.MTEHatchOutput h : mte.mOutputHatches) {
            final FluidStack f = h.getFluid();
            o.append(' ')
                .append(f == null ? "empty" : qty(f.amount) + " L " + f.getLocalizedName())
                .append(';');
        }
        out.add(o.toString());
        long eu = 0;
        for (MTEHatch h : mte.getExoticAndNormalEnergyHatchList()) eu += h.getEUVar();
        out.add("Stored EU in the energy hatches: " + qty(eu));
        return out;
    }

    /** {@code /btshr recipes}: the three process maps, and our Condensate Assembler / Generator recipes. */
    public static List<String> recipeLines() {
        final List<String> out = new ArrayList<>();
        final gregtech.api.recipe.RecipeMap<?>[] maps = { RealityRecipeMaps.HOMOGENY, RealityRecipeMaps.DIFFRACTION,
            RealityRecipeMaps.REFAB };
        for (gregtech.api.recipe.RecipeMap<?> map : maps) {
            final java.util.Collection<gregtech.api.util.GTRecipe> all = map.getAllRecipes();
            out.add(map.unlocalizedName + ": " + all.size() + " recipe(s)");
            for (gregtech.api.util.GTRecipe r : all) out.add("  " + recipeLine(r));
        }
        int assembler = 0;
        final List<String> names = new ArrayList<>();
        for (gregtech.api.util.GTRecipe r : tectech.recipe.TecTechRecipeMaps.condensateAssemblingRecipes
            .getAllRecipes()) {
            final ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
            if (o == null || !ours(o)) continue;
            assembler++;
            names.add(o.stackSize + "x " + name(o));
        }
        out.add("Condensate Assembler: " + assembler + " of ours: " + String.join(", ", names));
        int generator = 0;
        names.clear();
        for (gregtech.api.util.GTRecipe r : tectech.recipe.TecTechRecipeMaps.condensateGeneratorRecipes
            .getAllRecipes()) {
            if (r.mFluidOutputs == null) continue;
            for (FluidStack f : r.mFluidOutputs) {
                if (f == null || !f.getFluid()
                    .getName()
                    .startsWith("bts")) continue;
                generator++;
                names.add(
                    f.getFluid()
                        .getName());
            }
        }
        out.add("Condensate Generator: " + generator + " of ours: " + String.join(", ", names));
        return out;
    }

    private static boolean ours(ItemStack s) {
        final Object id = Item.itemRegistry.getNameForObject(s.getItem());
        if (id != null && String.valueOf(id)
            .startsWith("beyondthestars:")) return true;
        for (Machine m : Machine.values()) {
            final ItemStack c = m.controller();
            if (c != null && c.getItem() == s.getItem() && c.getItemDamage() == s.getItemDamage()) return true;
        }
        return false;
    }

    private static String name(ItemStack s) {
        try {
            return s.getDisplayName();
        } catch (RuntimeException e) {
            return String.valueOf(Item.itemRegistry.getNameForObject(s.getItem())) + ":" + s.getItemDamage();
        }
    }

    private static String recipeLine(gregtech.api.util.GTRecipe r) {
        final List<String> in = new ArrayList<>();
        if (r.mInputs != null) for (ItemStack s : r.mInputs) if (s != null) in.add(s.stackSize + "x " + name(s));
        if (r.mFluidInputs != null) {
            for (FluidStack f : r.mFluidInputs) if (f != null) in.add(qty(f.amount) + " L " + f.getLocalizedName());
        }
        final List<String> res = new ArrayList<>();
        if (r.mOutputs != null) for (ItemStack s : r.mOutputs) if (s != null) res.add(s.stackSize + "x " + name(s));
        if (r.mFluidOutputs != null) {
            for (FluidStack f : r.mFluidOutputs) if (f != null) res.add(qty(f.amount) + " L " + f.getLocalizedName());
        }
        return String.join(" + ", in) + " -> "
            + String.join(" + ", res)
            + " | "
            + qty(r.mEUt)
            + " EU/t, "
            + r.mDuration / 20
            + " s, special "
            + r.mSpecialValue;
    }

    // ================================================================================ helpers

    private static void give(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) player.entityDropItem(stack, 0.5F);
    }

    private static void giveMany(EntityPlayerMP player, ItemStack one, int count) {
        if (one == null || count <= 0) return;
        final int max = Math.max(1, one.getMaxStackSize());
        int left = count;
        while (left > 0) {
            final ItemStack stack = one.copy();
            stack.stackSize = Math.min(max, left);
            give(player, stack);
            left -= stack.stackSize;
        }
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    static String qty(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
