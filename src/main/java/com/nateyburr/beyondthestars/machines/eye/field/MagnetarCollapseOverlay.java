package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.OM;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.PI;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.W;

/**
 * P4: the Magnetar Collapse (T6 → T7) as a field overlay (quasar-p4.md §5.2; the prototype's {@code beginMC},
 * {@code transDrift} / {@code transTick} kind 'mc' and {@code collapse}).
 * <ol>
 * <li><b>Overfeed</b> (phase 1, 300 s, the magnetar's T6 rules): the charge WDM 61.44 + BDM 15.36 L/s on top of the
 * upkeep ({@link StepResult#wdmCharged} / {@link StepResult#bdmCharged}); M_ns 2.0 → 2.3; Ω and Π drift
 * +2·(t/300); a 30 s countdown on the event channel from t = 270.</li>
 * <li><b>Collapse</b> (t = 300): the tier becomes T7, Θ, Ω, Π and A retire, W appears at 0; the shock P +{@link #shock}
 * less 10 per Bu/t delivered that second above D₇ (up to the whole shock), Integrity −20. P above +100 fails it.
 * A forced, warned minor merger is scheduled 450 s on.</li>
 * <li><b>Halo capture</b> (phase 3, 600 s, T7 rules): W's drift and the demand ramp in over 600 s (β is normalised by
 * D₇, finding F-1); success is Stable or better for 60 consecutive seconds within t = 420-600.</li>
 * </ol>
 * The controller supplies {@link StepInputs#gEye} (the bursts on the Eye's ports this second) and the usual β / γ.
 * The runtime owns the core (escrow), the charge buffers, the preconditions (the Stability Link) and the unlock.
 */
public final class MagnetarCollapseOverlay implements Overlay {

    public static final int OVERFEED_SECONDS = 300, COUNTDOWN_AT = 270;
    public static final double WDM_PER_SECOND = 61.44, BDM_PER_SECOND = 15.36;
    public static final double MNS_START = 2.0, MNS_RISE = 0.3;
    /** The proposal's shock (F-2 recommends +120; the prototype's default is +80). */
    public static final double DEFAULT_SHOCK = 80;
    /** Shock points cancelled per Bu/t above D₇ that second. */
    public static final double CANCEL_PER_BU = 10;
    public static final double INTEGRITY_LOSS = 20;
    /** The forced minor merger, seconds after the collapse second. */
    public static final int FORCED_MERGER_AFTER = 450;
    public static final int CAPTURE_SECONDS = 600, CAPTURE_FROM = 420, CAPTURE_STREAK = 60;

    /** The collapse shock before bursts cancel it. */
    public final double shock;
    /** 1 = overfeed (T6), 3 = halo capture (T7). */
    public int phase = 1;
    /** Seconds in the phase. */
    public int st;
    /** The halo capture's Stable-or-better streak (counted from t = 420). */
    public int streak;
    /** The magnetar's mass, M☉ (readout). */
    public double Mns = MNS_START;

    // ---- the collapse second, for readouts
    public boolean collapsedOnce;
    public double shockP0, shockP, shockG, shockCancel;

    public MagnetarCollapseOverlay(double shock) {
        this.shock = shock > 0 ? shock : DEFAULT_SHOCK;
    }

    /** The halo capture's demand ramp, Bu/t: D₇·min(1, t/600) (phase 3); D₇ in phase 1. */
    public double demand() {
        return phase == 3 ? GalaxyField.D7 * Math.min(1, st / 600.0) : GalaxyField.D7;
    }

    /** β's normaliser: D₇ throughout (finding F-1: β = G / D(t) diverges as D → 0). */
    public double normDemand() {
        return GalaxyField.D7;
    }

    /** The shock that would land with {@code gEye} Bu/t delivered on the collapse second (readout, GUI). */
    public double predictedShock(double gEye) {
        return shock - Math.min(shock, CANCEL_PER_BU * Math.max(0, gEye - GalaxyField.D7));
    }

    @Override
    public void addDrift(FieldState s, StepInputs in, double[] dp) {
        if (phase == 1) {
            final double k = 2 * (st / (double) OVERFEED_SECONDS);
            dp[OM] += k;
            dp[PI] += k;
        }
    }

    @Override
    public int addEvents(FieldState s, StepInputs in, double[] E, double q, StepResult out) {
        return phase == 1 && st >= COUNTDOWN_AT ? 1 : 0;
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs in) {
        if (phase != 3) return Overlay.super.drift(t, s, in);
        final double rho = Math.min(1, st / (double) CAPTURE_SECONDS);
        final double[] d = new double[StellarField.GAUGES];
        d[P] = GalaxyField.P_PER_BETA * (in.beta - rho);
        d[W] = GalaxyField.W_DRIFT * s.prec * rho;
        return d;
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        if (phase == 1) {
            out.wdmCharged = WDM_PER_SECOND;
            out.bdmCharged = BDM_PER_SECOND;
            st++;
            Mns = MNS_START + MNS_RISE * Math.min(1, st / (double) OVERFEED_SECONDS);
            if (st == COUNTDOWN_AT) out.countdown = true;
            if (st >= OVERFEED_SECONDS) collapse(s, in, out);
            return;
        }
        st++;
        if (st >= CAPTURE_FROM) streak = zone >= 0 && zone <= 1 ? streak + 1 : 0;
        if (streak >= CAPTURE_STREAK) out.transitionSuccess = true;
        else if (st >= CAPTURE_SECONDS) out.fail = "the halo was not captured: no 60 s Stable streak inside 420-600 s";
    }

    private void collapse(FieldState s, StepInputs in, StepResult out) {
        final double G = in.gEye;
        final double cancel = Math.min(shock, CANCEL_PER_BU * Math.max(0, G - GalaxyField.D7));
        final double sh = shock - cancel, P0 = s.x[P];
        s.tier = TierParams.T7;
        s.setX(StellarField.clamp(P0 + sh, -120, 120), 0, 0, 0);
        s.A = 0;
        s.I = Math.max(0, s.I - INTEGRITY_LOSS);
        s.lock = 0;
        collapsedOnce = true;
        shockP0 = P0;
        shockP = s.x[P];
        shockG = G;
        shockCancel = cancel;
        phase = 3;
        st = 0;
        streak = 0;
        out.collapsed = true;
        if (s.x[P] > 100) {
            out.fail = "P " + StellarField.fmtSigned(s.x[P]) + " after the collapse shock";
            return;
        }
        s.ev = new FieldState.EventState((double) (s.t + 1 + FORCED_MERGER_AFTER));
        s.prec = 1;
        s.cyc = new FieldState.Cycle(0);
    }
}
