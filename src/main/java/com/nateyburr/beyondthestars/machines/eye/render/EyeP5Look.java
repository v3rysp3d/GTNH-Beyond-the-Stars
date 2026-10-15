package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.AttractorField;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.InflationOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.UniverseField;
import com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.Infall;
import com.nateyburr.beyondthestars.machines.eye.star.Inflation;

/**
 * P5: the render-only snapshot of the Eye's T9 / T10 object (the Great Attractor; the Universe), riding on
 * {@link EyeStarLook#p5}. Computed on the server once per field second from the runtime, synced in the render tile's
 * look compound ({@code "p5"}), never part of the Eye's saved state. The contract with the render unit is
 * {@code p5/render-interface.md}: field names and meanings there.
 * <p>
 * The server builds it with clocks relative to now (seconds); {@link #at(long)} turns them into world ticks before the
 * look is sent, as {@link EyeStarLook#at(long)} does for the event channel, so the client counts down without packets.
 */
public final class EyeP5Look {

    // flowState
    public static final byte FLOW_NONE = 0, FLOW_WARN_ACCEPT = 1, FLOW_WARN_DECLINE = 2, FLOW_SURGE = 3,
        FLOW_PASSING = 4;
    // phase (T10)
    public static final byte PH_FORMING = 0, PH_EARLY = 1, PH_RECOMBINATION = 2, PH_TRANSPARENT = 3, PH_DAWN = 4,
        PH_LATE = 5, PH_RIP = 6, PH_CRUNCH = 7, PH_LAST_LIGHT = 8;
    /** Seconds an end (Big Rip, Big Crunch, last light) plays. */
    public static final int END_SECONDS = 3;

    // ---- T9
    public final byte beamGrade;
    public final float beamRho, beamRhoMax, beamGeV, phi;
    public final byte flowState, flowSide, flowSize;
    public final long flowAt;
    public final float flowHeld;
    public final long captureAt;
    public final byte captureN;
    public final int galaxies;
    public final float vRamp;
    /** The V gauge (the capture halo's tint). */
    public final float V;
    // ---- T10
    public final long uniStartAt;
    public final float uniAge;
    public final boolean paused;
    public final byte phase;
    public final long recAt, dawnAt;
    public final boolean dawnOk;
    public final float H;
    public final long endAt;
    public final int universes;
    public final byte infPhase;
    /** The controller's horizontal facing (ForgeDirection ordinal 2..5), −1 unknown. */
    public final byte facing;

    // ---- server-relative clocks (seconds from now; NaN none), turned into world ticks by at()
    private final float flowIn, captureAgo, uniAgeIn, recIn, dawnIn, endAgo;

    private EyeP5Look(byte beamGrade, float beamRho, float beamRhoMax, float beamGeV, float phi, byte flowState,
        byte flowSide, byte flowSize, long flowAt, float flowHeld, long captureAt, byte captureN, int galaxies,
        float vRamp, float V, long uniStartAt, float uniAge, boolean paused, byte phase, long recAt, long dawnAt,
        boolean dawnOk, float H, long endAt, int universes, byte infPhase, byte facing, float flowIn,
        float captureAgo, float uniAgeIn, float recIn, float dawnIn, float endAgo) {
        this.beamGrade = beamGrade;
        this.beamRho = beamRho;
        this.beamRhoMax = beamRhoMax;
        this.beamGeV = beamGeV;
        this.phi = phi;
        this.flowState = flowState;
        this.flowSide = flowSide;
        this.flowSize = flowSize;
        this.flowAt = flowAt;
        this.flowHeld = flowHeld;
        this.captureAt = captureAt;
        this.captureN = captureN;
        this.galaxies = galaxies;
        this.vRamp = vRamp;
        this.V = V;
        this.uniStartAt = uniStartAt;
        this.uniAge = uniAge;
        this.paused = paused;
        this.phase = phase;
        this.recAt = recAt;
        this.dawnAt = dawnAt;
        this.dawnOk = dawnOk;
        this.H = H;
        this.endAt = endAt;
        this.universes = universes;
        this.infPhase = infPhase;
        this.facing = facing;
        this.flowIn = flowIn;
        this.captureAgo = captureAgo;
        this.uniAgeIn = uniAgeIn;
        this.recIn = recIn;
        this.dawnIn = dawnIn;
        this.endAgo = endAgo;
    }

    /** The snapshot of a lit T9 / T10 Eye (or one in The Infall / Inflation), or null for anything else. */
    public static EyeP5Look of(EyeStarRuntime rt, boolean paused) {
        final FieldState fs = rt.fieldState();
        if (fs == null) return null;
        final boolean infall = rt.transition() instanceof Infall, inflation = rt.transition() instanceof Inflation;
        if (!fs.tier.attractor && !fs.tier.universe && !infall && !inflation) return null;
        final EyeAttractor a = rt.attractor();
        final long t = fs.t;
        // ---- T9: the beam and the flow
        final FieldState.Beam b = fs.beam;
        final FieldState.Flow f = fs.flow;
        byte flowState = FLOW_NONE, side = 0, size = 0;
        float flowIn = Float.NaN, held = 0;
        if (f != null && fs.tier.attractor) {
            if (f.left > 0) {
                flowState = FLOW_SURGE;
                flowIn = -f.tau;
                held = f.tau > 0 ? f.ok / (float) f.tau : 1;
            } else if (f.warned) {
                flowState = f.accepted ? FLOW_WARN_ACCEPT : FLOW_WARN_DECLINE;
                flowIn = (float) (f.next - t);
            }
            if (flowState != FLOW_NONE) {
                side = (byte) f.sign;
                size = (byte) f.size;
            } else if (a.passT() >= 0 && t - a.passT() < AttractorField.FLOW_DUR) {
                // a declined group streams past along its filament for the 240 s its surge would have taken
                flowState = FLOW_PASSING;
                flowIn = -(t - a.passT());
                side = (byte) a.passSign();
                size = (byte) a.passSize();
            }
        }
        final float vRamp = infall ? (float) ((Infall) rt.transition()).vRamp() : 1f;
        // ---- T10: the universe
        final FieldState.Universe u = fs.uni;
        final InflationOverlay inf = inflation ? ((Inflation) rt.transition()).overlay() : null;
        final boolean uniLive = fs.tier.universe && u != null && inf == null;
        byte phase = 0;
        float uniAgeIn = Float.NaN, recIn = Float.NaN, dawnIn = Float.NaN, endAgo = Float.NaN;
        if (uniLive) {
            phase = (byte) UniverseField.phase(u);
            uniAgeIn = u.t;
            recIn = UniverseField.REC_AT - u.t;
            dawnIn = u.dawn - u.t;
        }
        if (a.endKind() != 0 && a.endT() >= 0 && t - a.endT() <= END_SECONDS) {
            endAgo = t - a.endT();
            phase = a.endKind() == 1 ? PH_LAST_LIGHT : a.endKind() == 2 ? PH_RIP : PH_CRUNCH;
        }
        return new EyeP5Look(
            (byte) (a.firing() ? a.grade() : 0),
            b == null ? 0f : (float) b.rho,
            (float) a.rhoMax(),
            (float) AttractorField.gev(a.grade()),
            (float) (1 + fs.flowA),
            flowState,
            side,
            size,
            0L,
            held,
            0L,
            (byte) Math.min(127, a.captureN()),
            (int) Math.min(Integer.MAX_VALUE, rt.milestones().galaxies),
            vRamp,
            (float) fs.x[StellarField.V],
            0L,
            uniLive ? u.t : 0f,
            paused,
            phase,
            0L,
            0L,
            u != null && u.dawnOk,
            (float) fs.x[StellarField.H],
            0L,
            (int) Math.min(Integer.MAX_VALUE, rt.milestones().universes),
            (byte) (inf == null ? 0 : inf.phase),
            (byte) -1,
            flowIn,
            a.captureT() >= 0 ? (float) (t - a.captureT()) : Float.NaN,
            uniAgeIn,
            recIn,
            dawnIn,
            endAgo);
    }

    private static long tick(long now, float seconds) {
        return Float.isNaN(seconds) ? 0L : now + Math.round(seconds * 20.0);
    }

    /** Server: the snapshot to send at world tick {@code now}, with the controller's facing. */
    public EyeP5Look at(long now, int facingOrdinal) {
        return new EyeP5Look(
            beamGrade,
            beamRho,
            beamRhoMax,
            beamGeV,
            phi,
            flowState,
            flowSide,
            flowSize,
            tick(now, flowIn),
            flowHeld,
            Float.isNaN(captureAgo) ? 0L : now - Math.round(captureAgo * 20.0),
            captureN,
            galaxies,
            vRamp,
            V,
            Float.isNaN(uniAgeIn) ? 0L : now - Math.round(uniAgeIn * 20.0),
            uniAge,
            paused,
            phase,
            tick(now, recIn),
            tick(now, dawnIn),
            dawnOk,
            H,
            Float.isNaN(endAgo) ? 0L : now - Math.round(endAgo * 20.0),
            universes,
            infPhase,
            (byte) facingOrdinal,
            flowIn,
            captureAgo,
            uniAgeIn,
            recIn,
            dawnIn,
            endAgo);
    }

    /** True when worth a packet: a state change, an event's hit moved, or ρ, φ, H moved enough. */
    public boolean differs(EyeP5Look o) {
        if (o == null) return true;
        if (o.beamGrade != beamGrade || o.flowState != flowState
            || o.flowSide != flowSide
            || o.flowSize != flowSize
            || o.phase != phase
            || o.infPhase != infPhase
            || o.facing != facing
            || o.paused != paused
            || o.dawnOk != dawnOk) return true;
        if (Math.abs(o.flowAt - flowAt) > 40 || Math.abs(o.recAt - recAt) > 40
            || Math.abs(o.dawnAt - dawnAt) > 40
            || Math.abs(o.uniStartAt - uniStartAt) > 40
            || o.endAt != endAt
            || o.captureAt != captureAt) return true;
        if (o.galaxies != galaxies || o.universes != universes) return true;
        return Math.abs(o.beamRho - beamRho) > 0.05f * Math.max(1f, beamRho) || Math.abs(o.phi - phi) > 0.05f
            || Math.abs(o.H - H) > 2f
            || Math.abs(o.V - V) > 2f
            || Math.abs(o.vRamp - vRamp) > 0.02f
            || Math.abs(o.flowHeld - flowHeld) > 0.05f;
    }

    // ------------------------------------------------------------------ NBT (the render tile's look compound)

    public NBTTagCompound write() {
        final NBTTagCompound t = new NBTTagCompound();
        t.setByte("g", beamGrade);
        t.setFloat("rho", beamRho);
        t.setFloat("rhoMax", beamRhoMax);
        t.setFloat("gev", beamGeV);
        t.setFloat("phi", phi);
        t.setByte("fs", flowState);
        t.setByte("fside", flowSide);
        t.setByte("fsize", flowSize);
        t.setLong("fAt", flowAt);
        t.setFloat("fHeld", flowHeld);
        t.setLong("capAt", captureAt);
        t.setByte("capN", captureN);
        t.setInteger("gal", galaxies);
        t.setFloat("vRamp", vRamp);
        t.setFloat("V", V);
        t.setLong("uAt", uniStartAt);
        t.setFloat("uAge", uniAge);
        t.setBoolean("paused", paused);
        t.setByte("ph", phase);
        t.setLong("recAt", recAt);
        t.setLong("dawnAt", dawnAt);
        t.setBoolean("dawnOk", dawnOk);
        t.setFloat("H", H);
        t.setLong("endAt", endAt);
        t.setInteger("unis", universes);
        t.setByte("inf", infPhase);
        t.setByte("facing", facing);
        return t;
    }

    public static EyeP5Look read(NBTTagCompound t) {
        if (t == null || !t.hasKey("g")) return null;
        return new EyeP5Look(
            t.getByte("g"),
            t.getFloat("rho"),
            t.getFloat("rhoMax"),
            t.getFloat("gev"),
            t.getFloat("phi"),
            t.getByte("fs"),
            t.getByte("fside"),
            t.getByte("fsize"),
            t.getLong("fAt"),
            t.getFloat("fHeld"),
            t.getLong("capAt"),
            t.getByte("capN"),
            t.getInteger("gal"),
            t.getFloat("vRamp"),
            t.getFloat("V"),
            t.getLong("uAt"),
            t.getFloat("uAge"),
            t.getBoolean("paused"),
            t.getByte("ph"),
            t.getLong("recAt"),
            t.getLong("dawnAt"),
            t.getBoolean("dawnOk"),
            t.getFloat("H"),
            t.getLong("endAt"),
            t.getInteger("unis"),
            t.getByte("inf"),
            t.hasKey("facing") ? t.getByte("facing") : (byte) -1,
            Float.NaN,
            Float.NaN,
            Float.NaN,
            Float.NaN,
            Float.NaN,
            Float.NaN);
    }
}
