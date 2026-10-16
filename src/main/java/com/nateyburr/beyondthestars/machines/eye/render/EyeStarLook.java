package com.nateyburr.beyondthestars.machines.eye.render;

import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.GalacticMergerOverlay;
import com.nateyburr.beyondthestars.machines.eye.star.CoreCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
import com.nateyburr.beyondthestars.machines.eye.star.GalacticMerger;
import com.nateyburr.beyondthestars.machines.eye.star.Infall;
import com.nateyburr.beyondthestars.machines.eye.star.Inflation;
import com.nateyburr.beyondthestars.machines.eye.star.MagnetarCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.MassInfusion;
import com.nateyburr.beyondthestars.machines.eye.star.ShellIgnition;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;

/**
 * D-49 answer 3: what the Eye's centre render shows (the object, its size, colour and motion), computed on the
 * server from the star runtime once per field second and pushed to the {@link TileEyeStar} only when it moved. Pure
 * data: no world, no GL.
 * <p>
 * The colour goes through the Forge of the Gods star shader ({@code tectech:star}): it keeps the luminance of
 * TecTech's star textures and replaces their hue with {@link #rgb}, then raises it to {@code 1 / gamma}. So a hot
 * star has a high gamma (bright), and the T5 black dwarf cools by its gamma falling below 1 (dark), not by its RGB.
 * <p>
 * Sizes are in blocks (the sphere's radius). The nearest block of {@code eye_core} is 20.9 blocks from the centre (its
 * face 20.4), so the largest object (Arcturus swelling in the Core Collapse: at most {@link #SWELL_PEAK} = 20, its
 * throb
 * only shrinking it) still clears it; the voxel mockup's star is 15. Only the supernova flash (a translucent shell)
 * runs out past the bands.
 */
public final class EyeStarLook {

    /** No object (the tile is removed). */
    public static final byte NONE = 0;
    /** T1-T3 live star. */
    public static final byte STAR = 1;
    /** T1-T3 igniting: a dim red protostar swelling toward the star. */
    public static final byte PROTOSTAR = 2;
    /** T4 neutron star: small, blue-white, its beams sweeping round. */
    public static final byte NEUTRON = 3;
    /** T5 Sirius B batch: a white dwarf cooling to a black dwarf. */
    public static final byte DWARF = 4;
    /** T6 SGR 1806-20: a magnetar, violet, flaring on its event warnings. */
    public static final byte MAGNETAR = 5;
    /** Core Collapse phase 1: Arcturus fast-forwarded, swelling and reddening. */
    public static final byte SWELL = 6;
    /** Core Collapse phase 3: the proto-neutron star with the bounce's shock shell running out. */
    public static final byte BOUNCE = 7;
    /** A supernova (the Core Collapse failed at the bounce): a flash that runs out, then nothing. */
    public static final byte NOVA = 8;
    /**
     * P4 T7 Triangulum (M33): a loose, flocculent spiral seen on its 8° tilt, blue-white arms round a small bright
     * core;
     * {@link #aux} the minor merger's warning (the arms brighten).
     */
    public static final byte TRIANGULUM = 9;
    /** P4 T8 the Milky Way: a barred spiral, four arms round a warm bulge; {@link #aux} a warning or the crossing. */
    public static final byte MILKY_WAY = 10;
    /** P4 planets mode: a calm glow with the slots' planets on their orbits; {@link #aux} = planets / 9. */
    public static final byte PLANETS = 11;
    /**
     * P5 T9 the Great Attractor: a dark, lensing core with the cosmic web streaming into it and the pellet beam down
     * the collimator stack; its render-only state is {@link #p5}.
     */
    public static final byte ATTRACTOR = 12;
    /** P5 T10 the Universe: one universe inside the lifted crown; its render-only state is {@link #p5}. */
    public static final byte UNIVERSE = 13;
    /**
     * P5: the radius the two P5 kinds report (the renderer ignores it): the render tile's bounding box is 3 x it, so
     * the filaments (200 blocks out) and the crown with its spikes stay in the frustum check.
     */
    public static final float P5_RADIUS = 70f;

    public final byte kind;
    public final float radius;
    /** 0xRRGGBB. */
    public final int rgb;
    public final float gamma;
    /** Degrees per tick of the layers' rotation (scaled per layer as the Forge of the Gods does). */
    public final float spin;
    /**
     * Unrest (instability, flares), 0.02 calm to 0.18 at a lit star's worst: the renderer reads it as a severity
     * (B-251: the stars breathe by at most 4 % of the radius, the rest shows as boiling, spots and corona); on the
     * steady neutron star and magnetar (B-219) only a glow flicker of at most 4 %.
     */
    public final float pulse;
    /**
     * Kind-specific 0..1 the renderer needs: {@link #DWARF} the cooling fraction (its shells fade out and a dark
     * rim shows), {@link #MAGNETAR} the event warning (its flare shell brightens). 0 for the others.
     */
    public final float aux;

    // ------------------------------------------------------------------ renders v2 (R-04): render-only extras
    // The v2 star renders (EyeStarFx) read these; the fixed-function look ignores them. They ride in the render
    // tile's look compound only, never in the Eye's saved state.

    /** No event on the channel. */
    public static final byte EV_NONE = 0;
    /** T3 (and the end of the Shell Ignition): the helium flash. */
    public static final byte EV_HELIUM_FLASH = 1;
    /** T4: a starquake glitch (no warning). */
    public static final byte EV_GLITCH = 2;
    /** T6: a giant flare. */
    public static final byte EV_GIANT_FLARE = 3;
    /** T7 / T8 (renders v2 WP-5): a minor merger whose sign is +1 (the dwarf comes from above the disc). */
    public static final byte EV_MINOR_MERGER_UP = 4;
    /** T7 / T8: a minor merger whose sign is -1 (from below). */
    public static final byte EV_MINOR_MERGER_DOWN = 5;
    /**
     * The Galactic Merger's pericentre passes 1..3 (6..8): the next pass, or the last one for a minute after it; the
     * event's hit is the pass (the merger holds the event channel's own events back).
     */
    public static final byte EV_PERICENTRE_1 = 6;
    public static final byte EV_PERICENTRE_2 = 7;
    public static final byte EV_PERICENTRE_3 = 8;

    /** The star's tier (1..8); 0 when unknown (an old look). */
    public final byte n;
    /** The event channel's event ({@code EV_*}) while it is warned or hitting, else {@link #EV_NONE}. */
    public final byte ev;
    /**
     * Server only: seconds from now to the event's hit (negative once it hit). {@link EyeCentreRender} turns it into
     * {@link #evAt} before the look is sent.
     */
    public final float evIn;
    /** World tick of the event's hit (0: none): what the tile syncs, so the client counts down without packets. */
    public final long evAt;
    /** The running transition, {@code EyeTransition.Type.from} (1 Mass Infusion .. 7 Galactic Merger), 0 none. */
    public final byte trans;
    /** How far it is, 0..1. */
    public final float transP;
    /**
     * Renders v2 WP-5, server only: seconds to the T8 satellite crossing's hit (its 120 s countdown), negative during
     * its 300 s after the hit; NaN when none. {@link EyeCentreRender} turns it into {@link #xAt}.
     */
    public final float xIn;
    /** World tick of the satellite crossing's hit (0: none): the client counts it down and runs the ring wave. */
    public final long xAt;
    /**
     * P5: the T9 / T10 render-only snapshot (null for every other object). Set last in {@link #of}; the centre render
     * turns its clocks into world ticks ({@link EyeP5Look#at}) before the look is sent.
     */
    public EyeP5Look p5;

    public EyeStarLook(byte kind, float radius, int rgb, float gamma, float spin, float pulse) {
        this(kind, radius, rgb, gamma, spin, pulse, 0f);
    }

    public EyeStarLook(byte kind, float radius, int rgb, float gamma, float spin, float pulse, float aux) {
        this(kind, radius, rgb, gamma, spin, pulse, aux, (byte) 0, EV_NONE, 0f, 0L, (byte) 0, 0f, Float.NaN, 0L);
    }

    private EyeStarLook(byte kind, float radius, int rgb, float gamma, float spin, float pulse, float aux, byte n,
        byte ev, float evIn, long evAt, byte trans, float transP, float xIn, long xAt) {
        this.kind = kind;
        this.radius = radius;
        this.rgb = rgb;
        this.gamma = Math.min(gamma, MAX_GAMMA);
        this.spin = spin;
        this.pulse = pulse;
        this.aux = aux;
        this.n = n;
        this.ev = ev;
        this.evIn = evIn;
        this.evAt = evAt;
        this.trans = trans;
        this.transP = transP;
        this.xIn = xIn;
        this.xAt = xAt;
    }

    /** This look as tier {@code n}'s star. */
    public EyeStarLook tier(int n) {
        return new EyeStarLook(
            kind,
            radius,
            rgb,
            gamma,
            spin,
            pulse,
            aux,
            (byte) n,
            ev,
            evIn,
            evAt,
            trans,
            transP,
            xIn,
            xAt);
    }

    /** This look with an event {@code ev} hitting {@code in} seconds from now (negative: it hit that long ago). */
    public EyeStarLook event(int ev, float in) {
        return new EyeStarLook(
            kind,
            radius,
            rgb,
            gamma,
            spin,
            pulse,
            aux,
            n,
            (byte) ev,
            in,
            evAt,
            trans,
            transP,
            xIn,
            xAt);
    }

    /** This look during transition {@code trans} (1..7) at {@code p} (0..1). */
    public EyeStarLook transition(int trans, float p) {
        return new EyeStarLook(
            kind,
            radius,
            rgb,
            gamma,
            spin,
            pulse,
            aux,
            n,
            ev,
            evIn,
            evAt,
            (byte) trans,
            p,
            xIn,
            xAt);
    }

    /**
     * Renders v2 WP-5: this look with a T8 satellite crossing's hit {@code in} seconds from now (negative: it hit that
     * long ago).
     */
    public EyeStarLook crossing(float in) {
        return new EyeStarLook(kind, radius, rgb, gamma, spin, pulse, aux, n, ev, evIn, evAt, trans, transP, in, xAt);
    }

    /** Server: the look to send at world tick {@code now} (the event's and the crossing's hits as world ticks). */
    public EyeStarLook at(long now) {
        final long at = ev == EV_NONE ? 0L : now + Math.round(evIn * 20.0);
        final long xat = Float.isNaN(xIn) ? 0L : now + Math.round(xIn * 20.0);
        final EyeStarLook out = new EyeStarLook(
            kind,
            radius,
            rgb,
            gamma,
            spin,
            pulse,
            aux,
            n,
            ev,
            evIn,
            at,
            trans,
            transP,
            xIn,
            xat);
        out.p5 = p5;
        return out;
    }

    /**
     * Renders v2: the star of tier {@code from} (1 Sol in the Mass Infusion, 2 Sirius A in the Shell Ignition) at
     * {@code p} (0..1) of the way to the next one: radius, colour and gamma blended, the transition set.
     */
    public static EyeStarLook growing(int from, float p) {
        final EyeStarLook a = lit(from, 0, 0), b = lit(from + 1, 0, 0);
        final float c = Math.max(0, Math.min(1, p));
        final float e = c * c * (3 - 2 * c);
        return a
            .reshaped(a.radius + (b.radius - a.radius) * e, lerp(a.rgb, b.rgb, e), a.gamma + (b.gamma - a.gamma) * e)
            .transition(from, c);
    }

    /** A copy with another radius, colour and gamma (a transition's blend). */
    private EyeStarLook reshaped(float radius, int rgb, float gamma) {
        return new EyeStarLook(kind, radius, rgb, gamma, spin, pulse, aux, n, ev, evIn, evAt, trans, transP, xIn, xAt);
    }

    /**
     * The core's gamma ceiling for every look (render QA round 3, E-3): the star shader raises the texture's luminance
     * to {@code 1 / gamma}, and from about 4 up every look saturated to flat white with no texture left.
     */
    public static final float MAX_GAMMA = 2.5f;

    /** The flash after a supernova (the star is already out). */
    public static final EyeStarLook NOVA_FLASH = new EyeStarLook(NOVA, 15f, 0xFFF0DC, MAX_GAMMA, 1f, 0f);

    // ------------------------------------------------------------------------------ per star

    // Render QA round 3 (E-1..E-4, E-6): the star shader keeps only the tint's chroma and raises the texture's own
    // luminance to 1 / gamma, so the round-1 pale tints at gamma 4.2-6 rendered flat white discs (71-100 % of the
    // pixels pure white). The tints are now saturated and every gamma is at most MAX_GAMMA: Sol yellow-white with
    // granulation (was FFF0C8 / 4.2), Sirius A blue-white (was C8DCFF / 4.8), Arcturus orange rather than golden
    // (was FF8232 / 2.6), the neutron star and the bounce's proto-neutron star blue-white with texture (were
    // DCE8FF / 6 and C8E1FF / 6).
    private static final int SOL_RGB = 0xFFE08A;
    private static final float SOL_GAMMA = 2.0f;
    private static final int SIRIUS_A_RGB = 0x9EC2FF;
    private static final float SIRIUS_A_GAMMA = 1.8f;
    private static final int ARCTURUS_RGB = 0xFF6A20;
    // 0.9, not the review's 1.5: at 1.5 the round-3 shots still read golden (mean FEC072, median luminance 204); the
    // texture's luminance has to come down for the shader's chroma to read orange
    private static final float ARCTURUS_GAMMA = 0.9f;
    private static final int PROTO_RGB = 0xE6401A;
    private static final int NEUTRON_RGB = 0xB8D0FF;
    private static final float NEUTRON_GAMMA = 2.5f;
    private static final int BOUNCE_RGB = 0xB8D4FF;
    // 2.0, not 2.5: with the bounce's shock shell over it the proto-neutron star measured 14-34 % white at 2.5
    private static final float BOUNCE_GAMMA = 2.0f;
    // 4A30FF / 0.7 (round 3, E-6; was 6A3CFF / 1.2, which read pink-lilac): the shader's hue swap pushes a violet
    // toward magenta, so the tint leans blue to land on violet, and the low gamma keeps the core darker than the
    // flare shell's rim (the review's 0.9 still measured C597FF, lilac)
    private static final int MAGNETAR_RGB = 0x4A30FF;
    private static final float MAGNETAR_GAMMA = 0.7f;
    /**
     * Spins, degrees per tick (B-219). The compact objects turn slowly, so their beams sweep round like a lighthouse
     * instead of wobbling: the neutron star once in 4.5 s (was 12, once in 1.5 s), the magnetar once in 7.5 s, SGR
     * 1806-20's own period (was 8, once in 2.25 s). The overfed magnetar of the Magnetar Collapse spins up from
     * {@link #MAGNETAR_SPIN} to {@link #OVERFED_SPIN}.
     */
    private static final float NEUTRON_SPIN = 4f;
    public static final float MAGNETAR_SPIN = 2.4f, OVERFED_SPIN = 24f;

    /** What the centre shows now, or null for nothing (dark, IDLE, no star). Server state. */
    public static EyeStarLook of(EyeStarRuntime rt) {
        final EyeStarLook base = baseOf(rt);
        if (base == null) return null;
        // renders v2 (R-04): the tier, the event channel and the running transition, for the v2 render
        final StarDef star = rt.star();
        EyeStarLook look = base.n == 0 && star != null ? base.tier(star.n()) : base;
        final double[] ev = rt.renderEvent();
        if (ev != null) look = look.event((int) ev[0], (float) ev[1]);
        // renders v2 WP-5: the T8 satellite crossing has its own clock (it runs 420 s and may overlap a minor merger)
        final double xIn = rt.renderCrossing();
        if (!Double.isNaN(xIn)) look = look.crossing((float) xIn);
        final EyeTransition tr = rt.transition();
        // renders v2 WP-5: the Galactic Merger's pericentre passes on the event channel (its own events are held)
        if (tr instanceof GalacticMerger && look.ev == EV_NONE) {
            final GalacticMergerOverlay gm = ((GalacticMerger) tr).overlay();
            if (gm != null) look = pericentre(look, gm.st);
        }
        if (tr != null) {
            final float p = (float) Math.max(0, rt.transitionProgress());
            look = look.transition(tr.type.from, p);
            if (tr instanceof Infall || tr instanceof Inflation) look = look.tier(tr.type.from + 1);
            // the Mass Infusion and the Shell Ignition blend the star toward the next one (both render paths)
            if (tr instanceof MassInfusion || tr instanceof ShellIgnition) {
                final EyeStarLook g = growing(tr instanceof MassInfusion ? 1 : 2, p);
                look = look.reshaped(g.radius, g.rgb, g.gamma);
            }
        }
        // P5: the Great Attractor's / the Universe's render-only state
        look.p5 = EyeP5Look.of(
            rt,
            rt.galaxy()
                .bursts().paused);
        return look;
    }

    /**
     * The Galactic Merger at {@code st} seconds: the last pericentre pass for a minute after it (the starburst and the
     * bridge), else the next pass; after the last pass's minute, nothing (the client reads the progress).
     */
    public static EyeStarLook pericentre(EyeStarLook look, int st) {
        final int[] passes = GalacticMergerOverlay.PASSES;
        for (int i = passes.length - 1; i >= 0; i--) {
            if (st >= passes[i] && st - passes[i] < 60) return look.event(EV_PERICENTRE_1 + i, -(st - passes[i]));
        }
        for (int i = 0; i < passes.length; i++) {
            if (st < passes[i]) return look.event(EV_PERICENTRE_1 + i, passes[i] - st);
        }
        return look;
    }

    private static EyeStarLook baseOf(EyeStarRuntime rt) {
        final StarDef star = rt.star();
        if (star == null) return null;
        final FieldReadout r = rt.lastReadout();
        final float unrest = r != null && r.lit ? (float) Math.max(0, Math.min(1, (100 - r.stability) / 100)) : 0f;
        final float warn = r != null && r.lit && r.warn != 0 ? 1f : 0f;
        final EyeTransition tr = rt.transition();
        switch (rt.state()) {
            case IGNITING: {
                // P climbs from -80 to -20 (lights): the protostar swells and heats toward the star's look
                return protostar(star.n(), (float) Math.max(0, Math.min(1, (rt.ignitionP() + 80) / 60)));
            }
            case LIT:
            case PENDING:
            case BATCH:
                break;
            default:
                return null;
        }
        if (tr instanceof CoreCollapse) {
            final CollapseOverlay co = ((CoreCollapse) tr).co;
            if (co.phase == 1) {
                return swell((float) Math.max(0, Math.min(1, co.tach / CollapseOverlay.TACH_BOUNCE)));
            }
            return bounce(unrest);
        }
        if (tr instanceof MagnetarCollapse) {
            final MagnetarCollapse mc = (MagnetarCollapse) tr;
            if (mc.overfeed()) {
                // the overfed magnetar: it shrinks, spins up and throbs harder toward the collapse
                return overfedMagnetar((float) Math.max(0, Math.min(1, mc.mc.st / 300.0)), warn);
            }
            return galaxy(7, Math.max(unrest, 0.3f), warn);
        }
        if (tr instanceof GalacticMerger) {
            return galaxy(rt.galaxyLit() && star.n() == 8 ? 8 : 7, Math.max(unrest, 0.3f), Math.max(warn, 0.5f));
        }
        // P5: the Infall makes the Attractor at once; Inflation shows the Attractor while the regions unfold, then
        // the Universe (the field's tier says which)
        final FieldState fs = rt.fieldState();
        if (tr instanceof Infall || tr instanceof Inflation) {
            return fs != null && fs.tier.universe ? universe(unrest, warn) : attractor(unrest, warn);
        }
        if (star.tier.planets()) return planets(star.tier.slots);
        if (fs != null && fs.tier.attractor) return attractor(unrest, warn);
        if (fs != null && fs.tier.universe) return universe(unrest, warn);
        if (star.n() == 5) {
            return dwarf(rt.batchFraction());
        }
        return lit(star.n(), unrest, warn);
    }

    /**
     * T1-T3 igniting at {@code f} (0..1, P from -80 to -20): the protostar swells and heats from a dim red toward tier
     * {@code n}'s lit look.
     */
    public static EyeStarLook protostar(int n, float f) {
        final float c = Math.max(0, Math.min(1, f));
        final EyeStarLook lit = lit(n, 0, 0);
        return new EyeStarLook(
            PROTOSTAR,
            lit.radius * (0.35f + 0.45f * c),
            lerp(PROTO_RGB, lit.rgb, c),
            0.8f + (lit.gamma - 0.8f) * c,
            0.5f,
            0.05f).tier(n);
    }

    /**
     * The overfed magnetar of the T6 to T7 Magnetar Collapse at {@code f} (0..1 of the way to the collapse): it
     * shrinks,
     * spins up and throbs harder; {@code warn} an event warning (0/1).
     */
    public static EyeStarLook overfedMagnetar(float f, float warn) {
        final float c = Math.max(0, Math.min(1, f));
        return new EyeStarLook(
            MAGNETAR,
            2.6f * (1f - 0.3f * c),
            MAGNETAR_RGB,
            MAGNETAR_GAMMA,
            MAGNETAR_SPIN + (OVERFED_SPIN - MAGNETAR_SPIN) * c,
            0.05f + 0.25f * c + 0.25f * warn,
            Math.max(warn, c)).tier(6);
    }

    /**
     * Core Collapse phase 1 at {@code f} (0..1) of the way to the bounce: Arcturus swelling and reddening. Render QA
     * round 4 (D4: the round-3 swell, radius 16 and paler than Arcturus, read as Arcturus): the radius eases out from
     * Arcturus's 15 to {@link #SWELL_PEAK} (19.4 at half way), the tint reaches {@link #SWELL_RGB} at gamma 0.6 by half
     * way, the surface turns 3x Arcturus's speed at half way and faster after, and the pulse is 0.15 at half way (the
     * renderer runs the swell's pulse as a slow throb of at most 4 % of the radius, see {@code EyeStarRenderer}).
     */
    public static EyeStarLook swell(float f) {
        final float c = Math.max(0, Math.min(1, f));
        final float early = Math.min(1f, 2f * c);
        final float ease = 1f - (1f - c) * (1f - c) * (1f - c);
        return new EyeStarLook(
            SWELL,
            15f + (SWELL_PEAK - 15f) * ease,
            lerp(ARCTURUS_RGB, SWELL_RGB, early),
            ARCTURUS_GAMMA - (ARCTURUS_GAMMA - SWELL_GAMMA) * early,
            0.7f * (1f + 4f * c),
            0.05f + 0.2f * c).tier(3);
    }

    /**
     * The swell's largest radius (render QA round 4, D4): the nearest {@code eye_core} block face is 20.4 blocks from
     * the centre, and the swell's throb only ever shrinks it, so 20 never touches a block.
     */
    public static final float SWELL_PEAK = 20f;
    private static final int SWELL_RGB = 0xFF3A12;
    private static final float SWELL_GAMMA = 0.6f;

    /** Core Collapse phase 3: the proto-neutron star and the bounce's shell, with the field's unrest (0..1). */
    public static EyeStarLook bounce(float unrest) {
        return new EyeStarLook(BOUNCE, 2.6f, BOUNCE_RGB, BOUNCE_GAMMA, 10f, 0.03f + 0.1f * unrest).tier(4);
    }

    /** The lit look of a tier's star, with the field's unrest (0..1) and an event warning (0/1) as wobble. */
    public static EyeStarLook lit(int n, float unrest, float warn) {
        final EyeStarLook look = litRaw(n, unrest, warn);
        return look.n == 0 ? look.tier(n) : look;
    }

    private static EyeStarLook litRaw(int n, float unrest, float warn) {
        switch (n) {
            case 1:
                return new EyeStarLook(STAR, 9f, SOL_RGB, SOL_GAMMA, 1f, 0.02f + 0.1f * unrest + 0.06f * warn);
            case 2:
                return new EyeStarLook(
                    STAR,
                    11f,
                    SIRIUS_A_RGB,
                    SIRIUS_A_GAMMA,
                    1.2f,
                    0.02f + 0.1f * unrest + 0.06f * warn);
            case 3:
                // Arcturus, the red giant: the helium flash warning swells it
                return new EyeStarLook(
                    STAR,
                    15f,
                    ARCTURUS_RGB,
                    ARCTURUS_GAMMA,
                    0.7f,
                    0.03f + 0.1f * unrest + 0.1f * warn);
            case 4:
                return new EyeStarLook(
                    NEUTRON,
                    2.2f,
                    NEUTRON_RGB,
                    NEUTRON_GAMMA,
                    NEUTRON_SPIN,
                    0.03f + 0.08f * unrest + 0.05f * warn);
            case 5:
                return dwarf(0);
            case 7:
            case 8:
                return galaxy(n, unrest, warn);
            default:
                // the magnetar's flares: a warning brightens its flare shell (aux) and its glow flickers more
                return new EyeStarLook(
                    MAGNETAR,
                    2.6f,
                    MAGNETAR_RGB,
                    MAGNETAR_GAMMA,
                    MAGNETAR_SPIN,
                    0.05f + 0.08f * unrest + 0.25f * warn,
                    warn);
        }
    }

    // P4 galaxies: the core is the bulge (the shader's textured sphere), the arms are additive billboards drawn by the
    // renderer out to GALAXY_REACH (the core band's nearest block is 20.4 blocks out). First guesses for the render QA
    // pass (P4-64).
    private static final int TRIANGULUM_RGB = 0xA8C4FF;
    private static final float TRIANGULUM_GAMMA = 1.4f;
    private static final int MILKY_WAY_RGB = 0xFFD49A;
    private static final float MILKY_WAY_GAMMA = 1.6f;
    private static final int PLANETS_RGB = 0xFFE6B0;

    /** The arms' reach, blocks: Triangulum 16, the Milky Way 19 (inside the core band's 20.4). */
    public static float galaxyReach(byte kind) {
        return kind == MILKY_WAY ? 19f : 16f;
    }

    /** A lit galaxy (7 Triangulum, 8 the Milky Way) with the field's unrest and an event warning. */
    public static EyeStarLook galaxy(int n, float unrest, float warn) {
        if (n >= 8) {
            return new EyeStarLook(
                MILKY_WAY,
                3.0f,
                MILKY_WAY_RGB,
                MILKY_WAY_GAMMA,
                0.8f,
                0.02f + 0.06f * unrest + 0.04f * warn,
                warn);
        }
        return new EyeStarLook(
            TRIANGULUM,
            2.4f,
            TRIANGULUM_RGB,
            TRIANGULUM_GAMMA,
            1.2f,
            0.02f + 0.06f * unrest + 0.04f * warn,
            warn);
    }

    /** P5 T9: the Great Attractor (a dark core; the renderer draws it from {@link #p5}). */
    public static EyeStarLook attractor(float unrest, float warn) {
        return new EyeStarLook(ATTRACTOR, P5_RADIUS, 0x2A1A48, 0.6f, 0.4f, 0.02f + 0.06f * unrest + 0.04f * warn, warn)
            .tier(9);
    }

    /** P5 T10: the Universe (the renderer draws it from {@link #p5}). */
    public static EyeStarLook universe(float unrest, float warn) {
        return new EyeStarLook(UNIVERSE, P5_RADIUS, 0xFFB070, 1.2f, 0.2f, 0.02f + 0.04f * unrest, warn).tier(10);
    }

    /** Planets mode with {@code slots} planet slots (3 or 6). */
    public static EyeStarLook planets(int slots) {
        return new EyeStarLook(PLANETS, 1.6f, PLANETS_RGB, 1.4f, 0.6f, 0.01f, Math.max(0, Math.min(9, slots)) / 9f)
            .tier(8);
    }

    /**
     * The cooling dwarf's palette: fraction, 0xRRGGBB and gamma per stop (render QA E7). The shader keeps the texture's
     * luminance and takes only the tint's chroma, so a dark tint does not darken the ball: the gamma does (luminance
     * to the power 1 / gamma). The review's late gammas (1.2 / 0.6 / 0.4) still rendered a pink-grey Moon in round 2,
     * so the late stops go much lower: brick red at 0.45, near black at 0.15, black at 0.08 (0.2 at 90 % still showed
     * copper glints).
     */
    private static final float[] DWARF_AT = { 0f, 0.2f, 0.5f, 0.75f, 0.9f, 1f };
    private static final int[] DWARF_RGB = { 0xF2F5FF, 0xF2F5FF, 0xFFB070, 0x8A2A18, 0x2A1210, 0x0E0A0C };
    // Render QA round 4 (D5): the early dwarf stays white, F2F5FF, until 20 % of the batch at gamma 2.5 -> 2.3 (round
    // 3's B8CCFF / 1.5 measured lavender-grey 189/179/205 by day, red above green, dimmer than Sirius A); a stop at 20
    // %
    // keeps the 10 % case off the white-to-orange blend, which passes through pink
    private static final float[] DWARF_GAMMA = { 2.5f, 2.3f, 1.8f, 0.45f, 0.15f, 0.08f };

    /**
     * Sirius B through a cooling batch (fraction 0..1): white-hot blue-white, orange at half way, a dull brick red at
     * three quarters, then near black, its gamma falling under 1 (render QA E7: the old curve read as a beige Moon
     * early and rose red late). The fraction rides along as {@link #aux}: the renderer fades the shells out with it
     * and shows a faint dark-red rim so the black dwarf still reads against the casings.
     */
    public static EyeStarLook dwarf(double fraction) {
        final float c = (float) Math.max(0, Math.min(1, fraction));
        int i = 0;
        while (i < DWARF_AT.length - 2 && c > DWARF_AT[i + 1]) i++;
        final float t = Math.max(0, Math.min(1, (c - DWARF_AT[i]) / (DWARF_AT[i + 1] - DWARF_AT[i])));
        final int rgb = lerp(DWARF_RGB[i], DWARF_RGB[i + 1], t);
        final float gamma = DWARF_GAMMA[i] + (DWARF_GAMMA[i + 1] - DWARF_GAMMA[i]) * t;
        return new EyeStarLook(DWARF, 3f, rgb, gamma, 0.6f, 0.01f, c).tier(5);
    }

    /** Linear blend of two 0xRRGGBB colours. */
    static int lerp(int a, int b, float t) {
        final float u = Math.max(0, Math.min(1, t));
        final int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * u);
        final int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * u);
        final int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * u);
        return (r << 16) | (g << 8) | bl;
    }

    /** Colour channels may drift this much before a new look is sent (render QA E10). */
    private static final int RGB_TOLERANCE = 4;

    /**
     * True when the two looks differ enough to be worth a packet: another kind, a colour channel moved more than
     * {@link #RGB_TOLERANCE}, or 2 % / 0.05 / 0.02 moves (the cooling dwarf's colour changes every field second).
     */
    public boolean differs(EyeStarLook o) {
        if (o == null || o.kind != kind) return true;
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((o.rgb >> shift) & 255) - ((rgb >> shift) & 255)) > RGB_TOLERANCE) return true;
        }
        // renders v2: the extras; the event's hit may drift two seconds before it is sent again
        if (o.n != n || o.ev != ev || o.trans != trans || Math.abs(o.evAt - evAt) > 40) return true;
        if ((o.xAt == 0) != (xAt == 0) || Math.abs(o.xAt - xAt) > 40) return true;
        if (Math.abs(o.transP - transP) > 0.01f) return true;
        if ((o.p5 == null) != (p5 == null) || (p5 != null && p5.differs(o.p5))) return true;
        return Math.abs(o.radius - radius) > 0.02f * Math.max(1f, radius) || Math.abs(o.gamma - gamma) > 0.05f
            || Math.abs(o.spin - spin) > 0.05f
            || Math.abs(o.pulse - pulse) > 0.02f
            || Math.abs(o.aux - aux) > 0.02f;
    }

    /** One scanner word for the object. */
    public String label() {
        switch (kind) {
            case STAR:
                return "live star";
            case PROTOSTAR:
                return "igniting protostar";
            case NEUTRON:
                return "neutron star";
            case DWARF:
                return "cooling white dwarf";
            case MAGNETAR:
                return "magnetar";
            case SWELL:
                return "Core Collapse (fast-forward)";
            case BOUNCE:
                return "Core Collapse (bounce, proto-neutron star)";
            case NOVA:
                return "supernova";
            case TRIANGULUM:
                return "Triangulum (galaxy)";
            case MILKY_WAY:
                return "the Milky Way (galaxy)";
            case PLANETS:
                return "planets mode";
            case ATTRACTOR:
                return "the Great Attractor";
            case UNIVERSE:
                return "the Universe";
            default:
                return "nothing";
        }
    }
}
