package com.nateyburr.beyondthestars.machines.eye;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.WirelessNetworkManager;
import tectech.thing.casing.TTCasingsContainer;

/**
 * SINGULARITY LADDER P3: the Eye's creative test path. {@code /btskit eye} hands over the parts and
 * prints each piece's bill of materials; {@code /btseye} raises a complete, formed Eye in front of the
 * player. Owned by the Eye unit.
 * <p>
 * <b>No count here is typed by hand (D-22).</b> Socket positions and bills come from the exported
 * shapes ({@link BTSShapes}) and {@code manifest.json}, the files the controller walks.
 */
public final class EyeKit {

    private EyeKit() {}

    /** Voxel centre of the Eye (the render), as a/b/c from the controller: 40 up, 40 deep. */
    private static final int[] CENTRE_REL = { 0, -40, 40 };
    /** What {@code fill} tops the buffers to, and the wireless EU it grants. */
    private static final long FILL_LITRES = 1_000_000_000_000L;
    private static final BigInteger FILL_EU = BigInteger.TEN.pow(18);

    // ===================================================================================== kit

    /** {@code /btskit eye}: controller, Galactic core, every star chart, hatches, top-tier fields. */
    public static void giveKit(EntityPlayerMP player) {
        give(player, BTSMachines.eyeOfHarmony == null ? null : BTSMachines.eyeOfHarmony.copy());
        give(player, item(SLItems.galacticUpgradeCore));
        give(player, item(SLItems.starChartSol));
        give(player, item(SLItems.starChartSiriusA));
        give(player, item(SLItems.starChartArcturus));
        give(player, item(SLItems.starChartSiriusB));
        give(player, item(SLItems.starChartSiriusBCooled));
        giveMany(player, ItemList.Hatch_Input_MAX.get(1L), sockets(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h'));
        giveMany(
            player,
            ItemList.Hatch_Input_MAX.get(1L),
            sockets(MTEEyeOfHarmonyBTS.PIECE_BEAM, 's') + sockets(MTEEyeOfHarmonyBTS.PIECE_JET, 't'));
        give(player, ItemList.Hatch_Output_Bus_ME.get(1L));
        give(player, ItemList.Hatch_Output_ME.get(1L));
        give(player, ItemList.Hatch_AutoMaintenance.get(1L));
        giveMany(
            player,
            generator(TTCasingsContainer.SpacetimeCompressionFieldGenerators, 8),
            BTSShapes.count(MTEEyeOfHarmonyBTS.PIECE_CORE, 'W'));
        giveMany(
            player,
            generator(TTCasingsContainer.TimeAccelerationFieldGenerator, 8),
            BTSShapes.count(MTEEyeOfHarmonyBTS.PIECE_CORE, 'X'));
        giveMany(
            player,
            generator(TTCasingsContainer.StabilisationFieldGenerators, 8),
            BTSShapes.count(MTEEyeOfHarmonyBTS.PIECE_CORE, 'Y'));
        player.inventoryContainer.detectAndSendChanges();

        tell(player, EnumChatFormatting.AQUA + "== Eye of Harmony test kit ==");
        tell(
            player,
            "Given: the controller, a Galactic Upgrade Core, all five star charts, input hatches for every socket, "
                + "an ME output bus + hatch, auto-maintenance and tier-9 field generators.");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        for (String piece : MTEEyeOfHarmonyBTS.PIECES) {
            tell(player, " - " + piece + ": " + bill(piece));
        }
        tell(
            player,
            EnumChatFormatting.GREEN + "Fastest look: /btseye t1..t4 [gen 1-9] [fill] raises a complete, formed Eye "
                + "in front of you. Or a hologram projector on the controller: stack 1..4 = T1..T4.");
    }

    private static ItemStack generator(Block block, int meta) {
        return block == null ? null : new ItemStack(block, 1, meta);
    }

    private static ItemStack item(Item item) {
        return item == null ? null : new ItemStack(item, 1, 0);
    }

    /** "19,616 blocks: 9,406x Reinforced Spatial ..." from the manifest's bill of materials. */
    private static String bill(String piece) {
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

    // ========================================================================= creative build

    /** Result of one {@link #build}: whether it formed, and the lines to print. */
    public static final class Result {

        public boolean formed;
        public String failure;
        public final List<String> lines = new ArrayList<>();
    }

    /**
     * Raises a complete Eye with its controller at (x, y, z) facing {@code facing}: the core and the
     * first {@code tiers - 1} bands, the field generators at {@code genMeta} (0-8), a hatch in every
     * socket, the Galactic core at T4, the top tier's star chart in the controller slot, the player as
     * owner; with {@code fill}, full input buffers and 10^18 wireless EU. Then forms it. Server only.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, int tiers, int genMeta,
        boolean fill, EntityPlayerMP player) {
        final Result result = new Result();
        final ItemStack controller = BTSMachines.eyeOfHarmony;
        if (controller == null) {
            result.failure = "the Eye controller is not registered";
            return result;
        }
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) controller.getItemDamage(), facing)) {
            result.failure = "could not place the controller";
            return result;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEEyeOfHarmonyBTS)) {
            result.failure = "the controller did not initialise";
            return result;
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final MTEEyeOfHarmonyBTS eye = (MTEEyeOfHarmonyBTS) base.getMetaTileEntity();
        base.setOwnerUuid(player.getUniqueID());
        base.setOwnerName(player.getCommandSenderName());

        // 1. Every block: the stack size picks how many bands (1 = core .. 4 = + arms).
        eye.construct(new ItemStack(controller.getItem(), tiers, controller.getItemDamage()), false);
        final ExtendedFacing alignment = eye.getExtendedFacing();

        // 2. The field generators at the asked tier (holograms place tier 1).
        final int generators = setGenerators(world, alignment, x, y, z, genMeta);

        // 3. A hatch in every socket.
        int fitted = 0;
        for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h')) {
            if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
        }
        final ItemList[] outputs = { ItemList.Hatch_Output_Bus_ME, ItemList.Hatch_Output_ME };
        final int[][] o = socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'o');
        for (int i = 0; i < o.length && i < outputs.length; i++) {
            if (fit(world, alignment, x, y, z, o[i], outputs[i])) fitted++;
        }
        for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'm')) {
            if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_AutoMaintenance)) fitted++;
        }
        if (tiers >= 2) {
            for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_BEAM, 's')) {
                if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
            }
        }
        if (tiers >= 3) {
            for (int[] rel : socketRels(MTEEyeOfHarmonyBTS.PIECE_JET, 't')) {
                if (fit(world, alignment, x, y, z, rel, ItemList.Hatch_Input_MAX)) fitted++;
            }
        }

        // 4. Upgrade, target, owner's power, then form.
        if (tiers >= 4) eye.installGalactic();
        final Item[] charts = { SLItems.starChartSol, SLItems.starChartSiriusA, SLItems.starChartArcturus,
            SLItems.starChartSiriusB };
        final Item chart = charts[Math.max(0, Math.min(charts.length, tiers) - 1)];
        if (chart != null) base.setInventorySlotContents(1, new ItemStack(chart, 1, 0));
        if (fill) {
            eye.debugFillBuffers(FILL_LITRES);
            WirelessNetworkManager.strongCheckOrAddUser(player.getUniqueID());
            WirelessNetworkManager.addEUToGlobalEnergyMap(player.getUniqueID(), FILL_EU);
        }
        eye.checkStructure(true, base);
        eye.fixAllIssues();
        base.enableWorking();

        result.formed = eye.mMachine;
        result.lines.add(
            (result.formed ? EnumChatFormatting.GREEN + "Eye T" + tiers + " FORMED"
                : EnumChatFormatting.RED + "Eye T" + tiers + " NOT FORMED") + EnumChatFormatting.RESET
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
                + " hatches fitted, "
                + generators
                + " field generators at tier "
                + (genMeta + 1)
                + ".");
        result.lines.addAll(eye.statusLines());
        return result;
    }

    /** Re-sets every W/X/Y cell of the core to generator meta {@code meta}; returns the count. */
    private static int setGenerators(World world, ExtendedFacing alignment, int x, int y, int z, int meta) {
        final String piece = MTEEyeOfHarmonyBTS.PIECE_CORE;
        final String[][] shape = BTSShapes.load(piece);
        final int[] off = BTSShapes.offset(piece);
        int n = 0;
        for (int c = 0; c < shape.length; c++) {
            for (int b = 0; b < shape[c].length; b++) {
                final String row = shape[c][b];
                for (int a = 0; a < row.length(); a++) {
                    final Block block = generatorFor(row.charAt(a));
                    if (block == null) continue;
                    final int[] p = worldPos(alignment, x, y, z, new int[] { a - off[0], b - off[1], c - off[2] });
                    world.setBlock(p[0], p[1], p[2], block, meta, 2);
                    n++;
                }
            }
        }
        return n;
    }

    private static Block generatorFor(char ch) {
        switch (ch) {
            case 'W':
                return TTCasingsContainer.SpacetimeCompressionFieldGenerators;
            case 'X':
                return TTCasingsContainer.TimeAccelerationFieldGenerator;
            case 'Y':
                return TTCasingsContainer.StabilisationFieldGenerators;
            default:
                return null;
        }
    }

    /** Places the hatch {@code item} at the socket, facing away from the Eye's centre. */
    private static boolean fit(World world, ExtendedFacing alignment, int x, int y, int z, int[] rel, ItemList item) {
        if (item == null || !item.hasBeenSet()) return false;
        final ItemStack stack = item.get(1L);
        if (stack == null) return false;
        final int[] p = worldPos(alignment, x, y, z, rel);
        return MTEReactorCoreMk1.placeGtMachine(
            world,
            p[0],
            p[1],
            p[2],
            (short) stack.getItemDamage(),
            outward(world, alignment, x, y, z, p));
    }

    private static int[] worldPos(ExtendedFacing alignment, int x, int y, int z, int[] rel) {
        final int[] xyz = new int[3];
        alignment.getWorldOffset(rel, xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }

    /** The air-facing side of a socket that points most directly away from the Eye's centre. */
    private static ForgeDirection outward(World world, ExtendedFacing alignment, int x, int y, int z, int[] p) {
        final int[] c = worldPos(alignment, x, y, z, CENTRE_REL);
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
        return best;
    }

    // ================================================================================ helpers

    private static void give(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.entityDropItem(stack, 0.5F);
        }
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

    private static String qty(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
