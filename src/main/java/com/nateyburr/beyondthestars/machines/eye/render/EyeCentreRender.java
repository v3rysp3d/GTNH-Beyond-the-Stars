package com.nateyburr.beyondthestars.machines.eye.render;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.google.gson.JsonArray;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * D-49 answer 3: the server half of the Eye's centre render, owned by one Eye controller. It places GT-style
 * render block ({@link BlockEyeStar}) in the empty cell at the Eye's centre, keeps its {@link EyeStarLook} up to date
 * and removes it again. Server side only; it never loads a chunk and never overwrites a block.
 * <ul>
 * <li><b>Shown</b> while the Eye is formed and its star exists (igniting, lit, pending, a T5 batch), the render is
 * switched on for this Eye (GUI) and {@code eyeRenderCentre} is on (the client also checks its own copy).</li>
 * <li><b>Removed</b> at once when the structure breaks, the star goes out, the T5 Eye goes IDLE, the render is
 * switched off, the controller turns, or the controller is broken or wrenched ({@link #remove}). After a supernova
 * the block stays {@link #NOVA_TICKS} for the flash, then goes.</li>
 * <li><b>No orphans</b>: the tile checks its owner itself ({@link TileEyeStar#updateEntity}); a block left behind by
 * an Eye broken while the centre's chunk was unloaded removes itself, and so does one whose Eye is not loaded.</li>
 * </ul>
 * The centre is the voxel origin of {@code designs/voxel.js}, where the mockup draws its star; from the controller
 * it is {@code (a, b, c) = (−x, y, z)} of the manifest's {@code machines.eye.controllerVoxel} (the exporter's
 * {@code toABC}), which is {@code (0, −40, 40)}: 40 blocks above the controller and 40 into the Eye.
 */
public final class EyeCentreRender {

    /** Ticks a supernova flash stays before the block goes. */
    public static final int NOVA_TICKS = 60;

    /** The centre from the controller, (a, b, c); null until first used. */
    private static volatile int[] centreRel;

    /** Per-machine switch (GUI); saved with the Eye's persisted state (item and tile). */
    private boolean enabled = true;
    /**
     * Where this Eye's render block stands, or null. TILE NBT only, never on the wrenched item (trap 2): an Eye
     * placed somewhere else must never think it owns a render block here.
     */
    private int[] pos;
    private EyeStarLook sent;
    /** World time until which a supernova's flash shows (0: none). */
    private long novaUntil;
    /** Why there is no render although there should be one, or "". */
    private String note = "";
    /**
     * Dev-only render QA ({@code -Dbts.rendertest=true}, {@code rendertest.RenderTest}): a look shown instead of the
     * star's while the Eye is formed, so every centre object can be photographed without driving the star through its
     * lifecycle. Null in normal play; never saved.
     */
    private EyeStarLook forced;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean on) {
        enabled = on;
    }

    public String note() {
        return note;
    }

    /** Dev-only render QA: show {@code look} instead of the star's (null: back to the star). Not saved. */
    public void debugForceLook(EyeStarLook look) {
        forced = look;
    }

    /** The object shown now ("nothing" when none). Server state. */
    public String label() {
        return pos == null || sent == null ? "nothing" : sent.label();
    }

    /** True when this Eye's render block stands at (x, y, z) (the render tile asks). */
    public boolean claims(int x, int y, int z) {
        final int[] p = pos;
        return p != null && p[0] == x && p[1] == y && p[2] == z;
    }

    /** The centre from the controller, (a, b, c), from the manifest's controller voxel. */
    public static int[] centreRel() {
        int[] rel = centreRel;
        if (rel == null) {
            final JsonArray cv = BTSShapes.manifest()
                .getAsJsonObject("machines")
                .getAsJsonObject("eye")
                .getAsJsonArray("controllerVoxel");
            rel = new int[] { -cv.get(0)
                .getAsInt(), cv.get(1)
                    .getAsInt(),
                cv.get(2)
                    .getAsInt() };
            centreRel = rel;
        }
        return rel;
    }

    /** The centre cell in the world for an Eye controller at (x, y, z) facing {@code facing}. */
    public static int[] centrePos(ExtendedFacing facing, int x, int y, int z) {
        final int[] xyz = new int[3];
        facing.getWorldOffset(centreRel(), xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }

    /**
     * Server, every tick from the controller (the world is touched every 10 ticks, or at once when the star appears
     * or goes).
     */
    public void tick(IGregTechTileEntity base, ExtendedFacing facing, boolean formed, EyeStarRuntime runtime,
        long aTick) {
        final World world = base.getWorld();
        if (world == null || world.isRemote) return;
        final long now = world.getTotalWorldTime();
        if (runtime.takeNova()) novaUntil = now + NOVA_TICKS;
        final EyeStarLook look = !formed ? null : forced != null ? forced : EyeStarLook.of(runtime);
        final EyeStarLook raw = look != null ? look : now < novaUntil && formed ? EyeStarLook.NOVA_FLASH : null;
        // renders v2 (R-04): the event's hit as a world tick, so the client counts down without a packet a second
        // P5: the Attractor's and the Universe's look also carries the voxel frame (the crown's tilt, the filaments)
        final EyeStarLook want = raw == null ? null
            : raw.at(now)
                .framed(facing);
        final boolean show = want != null && enabled && Config.eyeRenderCentre;
        final boolean changed = show != (pos != null);
        if (!changed && aTick % 10 != 0) return;
        final int[] at = centrePos(facing, base.getXCoord(), base.getYCoord(), base.getZCoord());
        if (pos != null && !Arrays.equals(pos, at)) remove(base);
        if (!show) {
            note = "";
            if (pos != null) {
                remove(base);
            } else if (at[1] >= 0 && at[1] <= 255
                && world.blockExists(at[0], at[1], at[2])
                && world.getBlock(at[0], at[1], at[2]) == BlockEyeStar.INSTANCE) {
                    // ours, but this Eye lost track of it (a reload saved before it was placed): clear it now
                    // rather than wait for the tile's own check. Only an Eye's centre cell ever holds one.
                    world.setBlockToAir(at[0], at[1], at[2]);
                }
            return;
        }
        TileEyeStar tile = pos == null ? null : tileAt(world, pos);
        if (tile == null) {
            pos = null;
            tile = place(base, world, at);
            if (tile == null) return;
        }
        if (sent == null || want.differs(sent)) {
            sent = want;
            tile.setLook(want);
        }
    }

    private static TileEyeStar tileAt(World world, int[] p) {
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof TileEyeStar ? (TileEyeStar) te : null;
    }

    /** Places (or adopts) the render block at {@code at}; null if the cell is unloaded or not air. */
    private TileEyeStar place(IGregTechTileEntity base, World world, int[] at) {
        if (BlockEyeStar.INSTANCE == null || at[1] < 0 || at[1] > 255 || !world.blockExists(at[0], at[1], at[2])) {
            return null;
        }
        final Block block = world.getBlock(at[0], at[1], at[2]);
        if (block != BlockEyeStar.INSTANCE) {
            if (!block.isAir(world, at[0], at[1], at[2])) {
                note = "Centre render blocked at " + at[0] + ", " + at[1] + ", " + at[2] + ": clear that block";
                return null;
            }
            world.setBlock(at[0], at[1], at[2], BlockEyeStar.INSTANCE, 0, 3);
        }
        final TileEyeStar tile = tileAt(world, at);
        if (tile == null) return null;
        note = "";
        pos = at;
        sent = null;
        tile.setOwner(base.getXCoord(), base.getYCoord(), base.getZCoord());
        return tile;
    }

    /**
     * Removes this Eye's render block at once if its chunk is loaded. Otherwise the position is forgotten and the tile
     * removes itself once it finds no Eye claiming it ({@link TileEyeStar}).
     */
    public void remove(IGregTechTileEntity base) {
        final int[] p = pos;
        pos = null;
        sent = null;
        if (p == null || base == null) return;
        final World world = base.getWorld();
        if (world == null || world.isRemote) return;
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return;
        if (world.getBlock(p[0], p[1], p[2]) == BlockEyeStar.INSTANCE) world.setBlockToAir(p[0], p[1], p[2]);
    }

    // ================================================================================== NBT

    /** The per-machine switch (item and tile NBT, through the Eye's one serializer). */
    public void writeSetting(NBTTagCompound nbt) {
        nbt.setBoolean("btsEyeRender", enabled);
    }

    /** Tile NBT only: where the render block stands, so a reload adopts it. */
    public void writeTile(NBTTagCompound nbt) {
        if (pos != null) nbt.setIntArray("btsEyeRenderPos", pos);
    }

    public void read(NBTTagCompound nbt) {
        enabled = !nbt.hasKey("btsEyeRender") || nbt.getBoolean("btsEyeRender");
        final int[] p = nbt.getIntArray("btsEyeRenderPos");
        pos = p.length == 3 ? p : null;
        sent = null;
        novaUntil = 0;
    }
}
