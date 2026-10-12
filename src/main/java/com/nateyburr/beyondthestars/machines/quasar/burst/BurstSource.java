package com.nateyburr.beyondthestars.machines.quasar.burst;

import java.util.Locale;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * P4: the identity of a burst stream's source, a Quasar Engine controller (quasar-p4.md R.1, §3.1: "each stream now
 * carries its source: the quasar controller's position and dimension"). Immutable, comparable by value, so a GRB
 * Receptor Port can key its per-source delivery on it. Nothing about a link is saved: the conduit solver re-derives it
 * every second.
 * <p>
 * <b>Q-8 (a source that is not loaded pauses its hosts).</b> {@link #status()} tells a consumer whether the source can
 * speak at all: {@link Status#RUNNING} (its packets are live), {@link Status#FROZEN} (loaded, but formed-check or
 * structure chunks missing: its disc is frozen, it publishes nothing) or {@link Status#UNLOADED} (its chunk or world
 * is not loaded). A host whose only source is FROZEN or UNLOADED pauses (freezes its field and cycle) instead of
 * reading zero. A loaded, running but dark quasar (dormant, re-forming, no coil power) is felt in full: its packets
 * carry 0 Bu/t and {@code linkable = false}.
 */
public final class BurstSource {

    /** What a source can say right now (Q-8). */
    public enum Status {
        /** Loaded and stepping: its packets are the truth (possibly 0 Bu/t). */
        RUNNING,
        /** Loaded but frozen (unformed, or a chunk of its structure is unloaded): hosts pause. */
        FROZEN,
        /** Its world or chunk is not loaded: hosts pause. */
        UNLOADED
    }

    public final int dim, x, y, z;

    public BurstSource(int dim, int x, int y, int z) {
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** The source of a quasar controller tile. */
    public static BurstSource of(IGregTechTileEntity base) {
        return new BurstSource(
            base.getWorld().provider.dimensionId,
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord());
    }

    /** The quasar controller this id names, if its world and chunk are loaded (never loads a chunk), else null. */
    public MTEQuasarEngine resolve() {
        final World world = DimensionManager.getWorld(dim);
        if (world == null || y < 0 || y > 255 || !world.blockExists(x, y, z)) return null;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final Object mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEQuasarEngine ? (MTEQuasarEngine) mte : null;
    }

    /** Q-8: whether the source is running, frozen or not loaded. */
    public Status status() {
        final MTEQuasarEngine q = resolve();
        if (q == null) return Status.UNLOADED;
        return q.isFrozen() ? Status.FROZEN : Status.RUNNING;
    }

    /** "x, y, z (dim d)" for GUIs and the scanner: "Quasar link: 120, 80, -40 (dim 0)". */
    public String label() {
        return String.format(Locale.ROOT, "%d, %d, %d (dim %d)", x, y, z, dim);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BurstSource)) return false;
        final BurstSource s = (BurstSource) o;
        return dim == s.dim && x == s.x && y == s.y && z == s.z;
    }

    @Override
    public int hashCode() {
        return ((dim * 31 + x) * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return "Quasar " + label();
    }
}
