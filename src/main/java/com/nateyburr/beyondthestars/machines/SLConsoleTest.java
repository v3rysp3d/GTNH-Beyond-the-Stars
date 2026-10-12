package com.nateyburr.beyondthestars.machines;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.StructureError;

/**
 * SINGULARITY LADDER test bench: what the console (and command-block) forms of {@code /btskerr} and
 * {@code /btseye} need to run a build without a player - a facing parsed from text, the footprint's
 * chunks loaded before anything is placed or walked, a survival (hologram projector) build from an
 * endless item source, the first wrong cell of a failed walk, and which side of the viewer a cell is
 * on (the mirror check). Server side only; creative/op tooling, never reached in normal play.
 */
public final class SLConsoleTest {

    private SLConsoleTest() {}

    /** A horizontal facing from "north"/"n"/..., or null if the text is not one. */
    public static ForgeDirection parseFacing(String text) {
        switch (text.toLowerCase(Locale.ROOT)) {
            case "north":
            case "n":
                return ForgeDirection.NORTH;
            case "south":
            case "s":
                return ForgeDirection.SOUTH;
            case "east":
            case "e":
                return ForgeDirection.EAST;
            case "west":
            case "w":
                return ForgeDirection.WEST;
            default:
                return null;
        }
    }

    /** True if the text is a coordinate: an int, or "~" / "~n" relative to the sender. */
    public static boolean isCoord(String text) {
        final String rest = text.startsWith("~") ? text.substring(1) : text;
        if (rest.isEmpty()) return text.startsWith("~");
        try {
            Integer.parseInt(rest);
            return true;
        } catch (NumberFormatException no) {
            return false;
        }
    }

    private static int coord(String text, int base) {
        if (!text.startsWith("~")) return Integer.parseInt(text);
        return base + (text.length() == 1 ? 0 : Integer.parseInt(text.substring(1)));
    }

    /** A console / command-block placement: the controller cell and the way its front faces. */
    public static final class Placement {

        public int x;
        public int y;
        public int z;
        public ForgeDirection facing = ForgeDirection.NORTH;
        /** Which args the placement used, so the caller's flag parsing skips them. */
        public boolean[] used;
    }

    /**
     * Finds "x y z [facing]" among the args (the first three coordinates in a row; "~" is relative to
     * the sender, e.g. a command block), or null if there is none (the player form).
     */
    public static Placement parsePlacement(ICommandSender sender, String[] args) {
        for (int i = 0; i + 2 < args.length; i++) {
            if (i > 0 && args[i - 1].equalsIgnoreCase("gen")) continue; // "gen 3" is not an x
            if (!isCoord(args[i]) || !isCoord(args[i + 1]) || !isCoord(args[i + 2])) continue;
            final ChunkCoordinates at = sender.getPlayerCoordinates();
            final Placement p = new Placement();
            p.used = new boolean[args.length];
            p.x = coord(args[i], at == null ? 0 : at.posX);
            p.y = coord(args[i + 1], at == null ? 0 : at.posY);
            p.z = coord(args[i + 2], at == null ? 0 : at.posZ);
            p.used[i] = p.used[i + 1] = p.used[i + 2] = true;
            if (i + 3 < args.length && parseFacing(args[i + 3]) != null) {
                p.facing = parseFacing(args[i + 3]);
                p.used[i + 3] = true;
            }
            return p;
        }
        return null;
    }

    /** Chat to any sender (player, console, command block). */
    public static void tell(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    // ================================================================================ chunks

    /**
     * World bounds {minX, minY, minZ, maxX, maxY, maxZ} of the given pieces placed with their
     * controller at (x, y, z), from the shapes' sizes and offsets.
     */
    public static int[] bounds(ExtendedFacing alignment, int x, int y, int z, String... pieces) {
        final int[] box = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE };
        final int[] xyz = new int[3];
        for (String piece : pieces) {
            final int[] off = BTSShapes.offset(piece);
            final int[] size = BTSShapes.size(piece);
            for (int corner = 0; corner < 8; corner++) {
                final int[] abc = { ((corner & 1) == 0 ? 0 : size[0] - 1) - off[0],
                    ((corner & 2) == 0 ? 0 : size[1] - 1) - off[1], ((corner & 4) == 0 ? 0 : size[2] - 1) - off[2] };
                alignment.getWorldOffset(abc, xyz);
                for (int i = 0; i < 3; i++) {
                    final int w = (i == 0 ? x : i == 1 ? y : z) + xyz[i];
                    box[i] = Math.min(box[i], w);
                    box[i + 3] = Math.max(box[i + 3], w);
                }
            }
        }
        return box;
    }

    /**
     * Loads (generating if needed) every chunk under the box plus a one-chunk margin, so world
     * population has already run on the footprint before a block is placed and the structure walk
     * never meets an unloaded cell. Returns the number of chunks touched.
     */
    public static int loadChunks(World world, int[] box) {
        int n = 0;
        for (int cx = (box[0] >> 4) - 1; cx <= (box[3] >> 4) + 1; cx++) {
            for (int cz = (box[2] >> 4) - 1; cz <= (box[5] >> 4) + 1; cz++) {
                if (world instanceof WorldServer) {
                    ((WorldServer) world).theChunkProviderServer.loadChunk(cx, cz);
                } else {
                    world.getChunkFromChunkCoords(cx, cz);
                }
                n++;
            }
        }
        return n;
    }

    // ============================================================================ survival

    /** An item source that supplies any one exact stack, endlessly, and nothing by predicate. */
    public static IItemSource endlessSource() {
        return new IItemSource() {

            @Override
            public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
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

    /** What one survival auto-place run did. */
    public static final class SurvivalRun {

        public int rounds;
        public int placed;
        public boolean finished;
        public boolean stalled;
    }

    /**
     * Drives {@code survivalConstruct} the way the Multiblock Structure Projector does (round after
     * round at {@code budget} elements, here back to back), from an endless source with a fake
     * player as the actor, until it reports -1 (all standing), stalls (0 placed twice) or
     * {@code maxRounds} pass.
     */
    public static SurvivalRun survivalBuild(ISurvivalConstructable machine, World world, ItemStack trigger, int budget,
        int maxRounds) {
        final SurvivalRun run = new SurvivalRun();
        final EntityPlayer actor = world instanceof WorldServer ? FakePlayerFactory.getMinecraft((WorldServer) world)
            : null;
        final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(endlessSource(), actor);
        int idle = 0;
        while (run.rounds < maxRounds) {
            run.rounds++;
            final int n = machine.survivalConstruct(trigger, budget, env);
            if (n < 0) {
                run.finished = true;
                break;
            }
            run.placed += n;
            idle = n == 0 ? idle + 1 : 0;
            if (idle >= 2) {
                run.stalled = true;
                break;
            }
        }
        return run;
    }

    // ======================================================================== diagnostics

    /**
     * Re-walks the controller into a fresh error list and describes the first error: its text, and
     * for a positioned error the cell and what actually stands there. Empty when the walk is clean.
     */
    public static List<String> firstErrors(MTEMultiBlockBase machine, IGregTechTileEntity base, int max) {
        final List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        final List<String> out = new ArrayList<>();
        for (int i = 0; i < errors.size() && i < max; i++) {
            out.add(describe(errors.get(i), base.getWorld()));
        }
        if (errors.size() > max) out.add("... and " + (errors.size() - max) + " more");
        return out;
    }

    private static String describe(StructureError error, World world) {
        final StringBuilder sb = new StringBuilder();
        sb.append(
            error.getClass()
                .getSimpleName())
            .append(": ")
            .append(BTSStructures.oneLine(error.getDisplayString()));
        final int[] p = position(error);
        if (p != null) {
            sb.append(" -> (")
                .append(p[0])
                .append(", ")
                .append(p[1])
                .append(", ")
                .append(p[2])
                .append(") holds ")
                .append(blockAt(world, p[0], p[1], p[2]));
        }
        return sb.toString();
    }

    /** x/y/z of a PositionedStructureError (private fields), or null for any other error. */
    private static int[] position(StructureError error) {
        try {
            final Field fx = error.getClass()
                .getDeclaredField("x");
            final Field fy = error.getClass()
                .getDeclaredField("y");
            final Field fz = error.getClass()
                .getDeclaredField("z");
            fx.setAccessible(true);
            fy.setAccessible(true);
            fz.setAccessible(true);
            return new int[] { fx.getInt(error), fy.getInt(error), fz.getInt(error) };
        } catch (ReflectiveOperationException | RuntimeException none) {
            return null;
        }
    }

    /** "registry:name@meta" (with the tile's class if any) of the block in a cell. */
    public static String blockAt(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return "<unloaded>";
        final Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) return "air";
        final Object name = Block.blockRegistry.getNameForObject(block);
        final StringBuilder sb = new StringBuilder(String.valueOf(name)).append('@')
            .append(world.getBlockMetadata(x, y, z));
        final Object te = world.getTileEntity(x, y, z);
        if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te).getMetaTileEntity() != null) {
            sb.append(" [")
                .append(
                    ((IGregTechTileEntity) te).getMetaTileEntity()
                        .getClass()
                        .getSimpleName())
                .append(']');
        }
        return sb.toString();
    }

    // ======================================================================== orientation

    /**
     * Which side of a viewer standing in front of the controller (looking at its face) a cell lies
     * on: "right", "left" or "centre". The mockup's +x (shape a > 0) must come out "right".
     */
    public static String viewerSide(ForgeDirection facing, int cx, int cz, int x, int z) {
        // The viewer looks along l = -facing; right = l x up = (-lz, 0, lx) = (fz, 0, -fx).
        final int rightX = facing.offsetZ;
        final int rightZ = -facing.offsetX;
        final int dot = (x - cx) * rightX + (z - cz) * rightZ;
        return dot > 0 ? "right" : dot < 0 ? "left" : "centre";
    }

    // ================================================================== damage and repair

    /** Cells next to the controller, as a/b/c offsets, tried in order for the damage test. */
    private static final int[][] NEAR_CONTROLLER = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 1, 1, 0 },
        { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 0, 0, 1 } };

    /**
     * The hologram's survival REPAIR path on a formed machine: knocks out up to two plain blocks next
     * to the controller (never a tile), checks that the walk now fails and names the cell, then runs
     * the survival auto-place and checks that it puts back exactly those blocks, leaves every tile
     * entity (hatch) in the box alone, and the machine forms again.
     */
    public static <M extends MTEEnhancedMultiBlockBase<?> & ISurvivalConstructable> List<String> damageAndRepair(
        M machine, IGregTechTileEntity base, ItemStack trigger, int[] box) {
        final List<String> out = new ArrayList<>();
        final World world = base.getWorld();
        final ExtendedFacing alignment = machine.getExtendedFacing();
        final int x = base.getXCoord();
        final int y = base.getYCoord();
        final int z = base.getZCoord();
        final int tilesBefore = countTiles(world, box);
        final List<int[]> removed = new ArrayList<>();
        final List<Block> blocks = new ArrayList<>();
        final List<Integer> metas = new ArrayList<>();
        for (int[] rel : NEAR_CONTROLLER) {
            final int[] p = worldPos(alignment, x, y, z, rel);
            final Block block = world.getBlock(p[0], p[1], p[2]);
            if (block == null || block.isAir(world, p[0], p[1], p[2])
                || world.getTileEntity(p[0], p[1], p[2]) != null) {
                continue;
            }
            removed.add(p);
            blocks.add(block);
            metas.add(world.getBlockMetadata(p[0], p[1], p[2]));
            world.setBlockToAir(p[0], p[1], p[2]);
            if (removed.size() == 2) break;
        }
        if (removed.isEmpty()) {
            out.add("Damage test: no plain block next to the controller to remove; skipped.");
            return out;
        }
        final List<String> errors = firstErrors(machine, base, 1);
        machine.checkStructure(true, base);
        out.add(
            "Damage test: removed " + removed.size()
                + " block(s) next to the controller -> "
                + (machine.mMachine ? "STILL FORMED (the walk missed it)"
                    : "NOT FORMED; " + (errors.isEmpty() ? "no error reported" : errors.get(0))));
        final SurvivalRun run = survivalBuild(machine, world, trigger, 25, 50);
        machine.checkStructure(true, base);
        int restored = 0;
        for (int i = 0; i < removed.size(); i++) {
            final int[] p = removed.get(i);
            if (world.getBlock(p[0], p[1], p[2]) == blocks.get(i)
                && world.getBlockMetadata(p[0], p[1], p[2]) == metas.get(i)) {
                restored++;
            }
        }
        final int tilesAfter = countTiles(world, box);
        out.add(
            "Survival repair: placed " + run.placed
                + " in "
                + run.rounds
                + " round(s), "
                + restored
                + "/"
                + removed.size()
                + " cells restored as before, tile entities in the box "
                + tilesBefore
                + " -> "
                + tilesAfter
                + ", "
                + (machine.mMachine ? "FORMED again" : "STILL NOT FORMED"));
        return out;
    }

    /** Live tile entities inside the box (hatches, the controller). */
    public static int countTiles(World world, int[] box) {
        int n = 0;
        for (Object o : world.loadedTileEntityList) {
            final TileEntity te = (TileEntity) o;
            if (te.isInvalid()) continue;
            if (te.xCoord >= box[0] && te.xCoord <= box[3]
                && te.yCoord >= box[1]
                && te.yCoord <= box[4]
                && te.zCoord >= box[2]
                && te.zCoord <= box[5]) n++;
        }
        return n;
    }

    /** World position of a shape-relative offset (a, b, c) from the controller at (x, y, z). */
    public static int[] worldPos(ExtendedFacing alignment, int x, int y, int z, int[] rel) {
        final int[] xyz = new int[3];
        alignment.getWorldOffset(rel, xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }
}
