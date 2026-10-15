package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

/**
 * P5 (design p5-t9-t10.md §2.1, §6.2, §9.5): the render-only look of the Eye's two cosmic objects, the Great Attractor
 * (T9, {@link EyeStarLook#ATTRACTOR}) and the Universe (T10, {@link EyeStarLook#UNIVERSE}). It rides inside the
 * {@link EyeStarLook} the centre render tile syncs ({@link TileEyeStar}, compound {@code cos}), never in the Eye's
 * saved state or an item. Pure data: no world, no GL, safe on the server.
 * <p>
 * The machine fills it once per field second with {@link Builder} (seconds relative to now, the way
 * {@link EyeStarLook#event} takes {@code in}); {@link EyeCentreRender} turns the clocks into world ticks
 * ({@link #at}) and adds the voxel frame from the controller's facing ({@link #framed}) before it sends. The client
 * then
 * counts every clock down by itself, so a packet is only needed when something moves by more than the
 * {@link #differs} tolerances.
 */
public final class EyeCosmosLook {

    // ------------------------------------------------------------------ the pellet grades (beam colour)
    /** No pellet is being fired (the beam is dark). */
    public static final byte GRADE_NONE = 0;
    /** Gated Pellet (Magnetar): violet. */
    public static final byte GRADE_MAGNETAR = 1;
    /** Gated Pellet (Ergomatter): cyan. */
    public static final byte GRADE_ERGOMATTER = 2;
    /** Gated Pellet (Superdense): white. */
    public static final byte GRADE_SUPERDENSE = 3;

    // ------------------------------------------------------------------ the Galactic Flow's state
    /** No flow announced or running. */
    public static final byte FLOW_NONE = 0;
    /** Announced (the 180 s countdown) and accepted: the group is seen far out on its filament, falling in. */
    public static final byte FLOW_WARNED = 1;
    /** Announced and declined (n above the accept limit): the group will stream past. */
    public static final byte FLOW_DECLINED = 2;
    /** The 240 s surge of an accepted flow. */
    public static final byte FLOW_SURGE = 3;
    /** A declined flow's 240 s: the group swings past the core and out again; nothing is captured. */
    public static final byte FLOW_PASSING = 4;

    // ------------------------------------------------------------------ a universe's end (T10)
    /** The universe is running. */
    public static final byte FATE_NONE = 0;
    /** Big Rip (H too high): the membrane tears outward for 3 s. */
    public static final byte FATE_RIP = 1;
    /** Big Crunch (H too low): the membrane collapses for 3 s. */
    public static final byte FATE_CRUNCH = 2;

    /** A universe's length, seconds (one 7,200 s batch). */
    public static final float UNIVERSE_SECONDS = 7200f;
    /** Recombination: seconds into each universe. */
    public static final float RECOMBINATION_AT = 1800f;
    /** A flow's warning and surge, seconds. */
    public static final float FLOW_WARNING = 180f, FLOW_SURGE_SECONDS = 240f;

    // ------------------------------------------------------------------ T9: the Great Attractor
    /** The beam's power over the Attractor's drive, rho = B / Pi9 (0 none, 1 rated; the render saturates near 3). */
    public final float rho;
    /** The pellet grade being fired ({@code GRADE_*}): the beam's colour. */
    public final byte grade;
    /** How far the beam has spooled toward its setpoint, 0..1 (the collars' glass channels pulse with it). */
    public final float spool;
    /** The flow phi = 1 + A: 1 at rest, up to 4 at a group of 12's peak (how many galaxies stream in). */
    public final float phi;
    /** The V gauge (infall against the beam), signed, as the GUI shows it: tints the capture halo. */
    public final float v;
    /** The Galactic Flow ({@code FLOW_*}). */
    public final byte flow;
    /** The flow's side: +1 the rail's high side, -1 its low side. */
    public final byte flowSide;
    /** The flow's group size n, 3..12. */
    public final byte flowN;
    /**
     * Server: seconds from now to the surge's start (negative during it). {@link #at} turns it into {@link #flowAt}.
     */
    public final float flowIn;
    /** World tick the surge starts (0: no flow); the client counts the warning down to it and the surge from it. */
    public final long flowAt;
    /** The share of the surge's seconds held Stable or better so far, 0..1 (how many of the group are captured). */
    public final float flowHeld;
    /** Galaxies held (the display counter): the halo round the core fills with them (log scale, full at 100,000). */
    public final int held;

    // ------------------------------------------------------------------ T10: the Universe
    /** Server: seconds into the running universe (NaN: none). {@link #at} turns it into {@link #uniAt}. */
    public final float uniAge;
    /** World tick the running universe began (0: none). */
    public final long uniAt;
    /** Server: seconds from now to this universe's Cosmic Dawn (NaN: not drawn yet). */
    public final float dawnIn;
    /** World tick of the Cosmic Dawn (0: none); the 120 s warning counts down to it, the starburst runs 300 s. */
    public final long dawnAt;
    /** The H gauge (expansion error), signed: the membrane breathes out (H above 0) or in. */
    public final float h;
    /** How the universe ended ({@code FATE_*}); the next one forms after the 3 s animation. */
    public final byte fate;
    /** Server: seconds since the fate (NaN: none). */
    public final float fateAge;
    /** World tick of the fate (0: none). */
    public final long fateAt;

    // ------------------------------------------------------------------ both
    /**
     * The voxel frame in the world, from the controller's facing ({@link #frameOf}): {@code ex * 6 + ez}, each a
     * {@code ForgeDirection} ordinal (2..5, horizontal) of the voxel +x and +z axes. -1: not set (the render assumes
     * the Eye faces north).
     */
    public final byte frame;

    private EyeCosmosLook(Builder b, long flowAt, long uniAt, long dawnAt, long fateAt, byte frame) {
        rho = b.rho;
        grade = b.grade;
        spool = b.spool;
        phi = b.phi;
        v = b.v;
        flow = b.flow;
        flowSide = b.flowSide;
        flowN = b.flowN;
        flowIn = b.flowIn;
        this.flowAt = flowAt;
        flowHeld = b.flowHeld;
        held = b.held;
        uniAge = b.uniAge;
        this.uniAt = uniAt;
        dawnIn = b.dawnIn;
        this.dawnAt = dawnAt;
        h = b.h;
        fate = b.fate;
        fateAge = b.fateAge;
        this.fateAt = fateAt;
        this.frame = frame;
    }

    /** A new builder (all quiet: no beam, phi 1, no flow, no universe). */
    public static Builder builder() {
        return new Builder();
    }

    /** The machine side's way in: set what is known, then {@link #build}. Seconds are relative to now. */
    public static final class Builder {

        private float rho;
        private byte grade;
        private float spool;
        private float phi = 1f;
        private float v;
        private byte flow;
        private byte flowSide = 1;
        private byte flowN;
        private float flowIn = Float.NaN;
        private float flowHeld;
        private int held;
        private float uniAge = Float.NaN;
        private float dawnIn = Float.NaN;
        private float h;
        private byte fate;
        private float fateAge = Float.NaN;

        private Builder() {}

        /** The beam: rho, the grade fired ({@code GRADE_*}) and the spool 0..1. */
        public Builder beam(float rho, int grade, float spool) {
            this.rho = Math.max(0f, rho);
            this.grade = (byte) grade;
            this.spool = clamp01(spool);
            return this;
        }

        /** The flow phi (1 at rest) and the V gauge. */
        public Builder infall(float phi, float v) {
            this.phi = Math.max(0f, phi);
            this.v = v;
            return this;
        }

        /**
         * A Galactic Flow: {@code state} ({@code FLOW_*}), side +1 / -1, group size n, seconds to the surge's start
         * ({@code in}, negative during the surge) and the share held so far.
         */
        public Builder flow(int state, int side, int n, float in, float heldShare) {
            this.flow = (byte) state;
            this.flowSide = (byte) (side < 0 ? -1 : 1);
            this.flowN = (byte) Math.max(0, Math.min(12, n));
            this.flowIn = in;
            this.flowHeld = clamp01(heldShare);
            return this;
        }

        /** Galaxies held (the display counter). */
        public Builder held(int galaxies) {
            this.held = Math.max(0, galaxies);
            return this;
        }

        /** T10: seconds into the running universe, seconds to its Cosmic Dawn (NaN: not drawn) and the H gauge. */
        public Builder universe(float age, float dawnIn, float h) {
            this.uniAge = age;
            this.dawnIn = dawnIn;
            this.h = h;
            return this;
        }

        /** T10: the universe ended ({@code FATE_*}) {@code age} seconds ago. */
        public Builder fate(int fate, float age) {
            this.fate = (byte) fate;
            this.fateAge = age;
            return this;
        }

        public EyeCosmosLook build() {
            return new EyeCosmosLook(this, 0L, 0L, 0L, 0L, (byte) -1);
        }
    }

    private static float clamp01(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    /** Server: this look with its clocks as world ticks at {@code now} (what is sent). */
    public EyeCosmosLook at(long now) {
        final long fa = flow == FLOW_NONE || Float.isNaN(flowIn) ? 0L : now + Math.round(flowIn * 20.0);
        final long ua = Float.isNaN(uniAge) ? 0L : now - Math.round(uniAge * 20.0);
        final long da = Float.isNaN(dawnIn) ? 0L : now + Math.round(dawnIn * 20.0);
        final long ta = fate == FATE_NONE || Float.isNaN(fateAge) ? 0L : now - Math.round(fateAge * 20.0);
        return new EyeCosmosLook(builderOf(this), fa, ua, da, ta, frame);
    }

    /** Server: this look in the voxel frame of a controller facing {@code facing}. */
    public EyeCosmosLook framed(ExtendedFacing facing) {
        final byte f = frameOf(facing);
        return f == frame ? this : new EyeCosmosLook(builderOf(this), flowAt, uniAt, dawnAt, fateAt, f);
    }

    private static Builder builderOf(EyeCosmosLook l) {
        final Builder b = new Builder();
        b.rho = l.rho;
        b.grade = l.grade;
        b.spool = l.spool;
        b.phi = l.phi;
        b.v = l.v;
        b.flow = l.flow;
        b.flowSide = l.flowSide;
        b.flowN = l.flowN;
        b.flowIn = l.flowIn;
        b.flowHeld = l.flowHeld;
        b.held = l.held;
        b.uniAge = l.uniAge;
        b.dawnIn = l.dawnIn;
        b.h = l.h;
        b.fate = l.fate;
        b.fateAge = l.fateAge;
        return b;
    }

    /**
     * The voxel frame of {@code facing}: the world directions of the voxel +x and +z axes as {@code ex * 6 + ez}
     * (ForgeDirection ordinals). The Eye's voxel offset (x, y, z) is {@code (a, b, c) = (x, -y, -z)} (the exporter's
     * {@code toABC}, {@link EyeCentreRender#centreRel}), so voxel +x is a and voxel +z is -c.
     */
    public static byte frameOf(ExtendedFacing facing) {
        if (facing == null) return -1;
        final int[] x = new int[3], z = new int[3];
        facing.getWorldOffset(new int[] { 1, 0, 0 }, x);
        facing.getWorldOffset(new int[] { 0, 0, -1 }, z);
        final int ex = dir(x), ez = dir(z);
        return ex < 0 || ez < 0 ? -1 : (byte) (ex * 6 + ez);
    }

    /** ForgeDirection ordinal of a unit offset (0 down, 1 up, 2 north -z, 3 south +z, 4 west -x, 5 east +x). */
    private static int dir(int[] o) {
        if (o[1] < 0) return 0;
        if (o[1] > 0) return 1;
        if (o[2] < 0) return 2;
        if (o[2] > 0) return 3;
        if (o[0] < 0) return 4;
        if (o[0] > 0) return 5;
        return -1;
    }

    /** Client: the world direction (x, y, z) of ForgeDirection ordinal {@code d} into {@code out}. */
    public static void axis(int d, double[] out) {
        out[0] = d == 4 ? -1 : d == 5 ? 1 : 0;
        out[1] = d == 0 ? -1 : d == 1 ? 1 : 0;
        out[2] = d == 2 ? -1 : d == 3 ? 1 : 0;
    }

    /** True when the two looks differ enough to be worth a packet (clocks may drift two seconds). */
    public boolean differs(EyeCosmosLook o) {
        if (o == null) return true;
        if (o.grade != grade || o.flow != flow
            || o.flowSide != flowSide
            || o.flowN != flowN
            || o.fate != fate
            || o.frame != frame) {
            return true;
        }
        if (Math.abs(o.flowAt - flowAt) > 40 || Math.abs(o.uniAt - uniAt) > 40
            || Math.abs(o.dawnAt - dawnAt) > 40
            || Math.abs(o.fateAt - fateAt) > 40) {
            return true;
        }
        if (Math.abs(o.rho - rho) > 0.03f * Math.max(1f, rho) || Math.abs(o.spool - spool) > 0.03f) return true;
        if (Math.abs(o.phi - phi) > 0.03f || Math.abs(o.flowHeld - flowHeld) > 0.02f) return true;
        if (Math.abs(o.v - v) > 1.5f || Math.abs(o.h - h) > 1.5f) return true;
        // the halo fills on a log scale: a new galaxy matters early, not at 50,000
        return o.held != held && (held < 200 || Math.abs(o.held - held) * 50 > held);
    }

    /** The seconds into the running universe at world tick {@code now} (NaN: none). Client. */
    public double universeAge(double now) {
        return uniAt == 0 ? Double.NaN : (now - uniAt) / 20.0;
    }

    /** Seconds from {@code now} to the Cosmic Dawn (negative once it began; NaN: none). Client. */
    public double dawnIn(double now) {
        return dawnAt == 0 ? Double.NaN : (dawnAt - now) / 20.0;
    }

    /** Seconds from {@code now} to the flow's surge (negative during it; NaN: none). Client. */
    public double flowIn(double now) {
        return flow == FLOW_NONE || flowAt == 0 ? Double.NaN : (flowAt - now) / 20.0;
    }

    /** Seconds since the universe's fate (NaN: none). Client. */
    public double fateAge(double now) {
        return fate == FATE_NONE || fateAt == 0 ? Double.NaN : (now - fateAt) / 20.0;
    }

    // ------------------------------------------------------------------ sync (render tile only)

    /** The synced compound (world ticks; the server-only seconds are not sent). */
    public NBTTagCompound write() {
        final NBTTagCompound c = new NBTTagCompound();
        c.setFloat("rho", rho);
        c.setByte("g", grade);
        c.setFloat("sp", spool);
        c.setFloat("phi", phi);
        c.setFloat("v", v);
        if (flow != FLOW_NONE) {
            c.setByte("fl", flow);
            c.setByte("fs", flowSide);
            c.setByte("fn", flowN);
            c.setLong("fa", flowAt);
            c.setFloat("fh", flowHeld);
        }
        c.setInteger("held", held);
        if (uniAt != 0) c.setLong("ua", uniAt);
        if (dawnAt != 0) c.setLong("da", dawnAt);
        c.setFloat("h", h);
        if (fate != FATE_NONE) {
            c.setByte("fate", fate);
            c.setLong("ta", fateAt);
        }
        c.setByte("fr", frame);
        return c;
    }

    /** Client: the look from its synced compound, or null. */
    public static EyeCosmosLook read(NBTTagCompound c) {
        if (c == null || !c.hasKey("rho")) return null;
        final Builder b = new Builder();
        b.rho = c.getFloat("rho");
        b.grade = c.getByte("g");
        b.spool = c.getFloat("sp");
        b.phi = c.getFloat("phi");
        b.v = c.getFloat("v");
        b.flow = c.getByte("fl");
        b.flowSide = c.hasKey("fs") ? c.getByte("fs") : 1;
        b.flowN = c.getByte("fn");
        b.flowHeld = c.getFloat("fh");
        b.held = c.getInteger("held");
        b.h = c.getFloat("h");
        b.fate = c.getByte("fate");
        return new EyeCosmosLook(
            b,
            c.getLong("fa"),
            c.getLong("ua"),
            c.getLong("da"),
            c.getLong("ta"),
            c.hasKey("fr") ? c.getByte("fr") : (byte) -1);
    }
}
