package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;

/**
 * D-49 stability field: the Core Collapse (T3 → T4) as a field overlay (d49-build-plan.md §7.4; the prototype's
 * {@code beginCollapse} / {@code bounce} / settle).
 * <ol>
 * <li><b>Fast-forward</b> (phase 1): each second the controller sets {@link #dose} (Tachyon Rich Temporal Fluid
 * dosed, ≤ 100 L); {@code Mc = 0.5 + tach/100,000}; the swelling {@code 2(Mc − 0.5)/0.94} is added to the P and Θ
 * drift.</li>
 * <li><b>Bounce</b> (at tach ≥ 94,000): {@code s = min(90, floor(SpaceTime/1,000))} points absorbed;
 * {@code shock = bounceShock − s} added to P and Θ; Integrity −20. Above +100 on either: supernova. Otherwise the
 * proto-neutron star lights under T4 rules at (0, +60, +30, 0).</li>
 * <li><b>Settle</b> (phase 3): Stable or better for 60 consecutive seconds within 180 s, or it fails.</li>
 * </ol>
 * The controller sets {@link #dose}, {@link #spaceTimeBuffer} and {@link #hsmSeedPresent} before each step, and takes
 * {@link StepResult#spaceTimeTaken} and {@link StepResult#hsmSeedTaken} when {@link StepResult#bounced}.
 */
public final class CollapseOverlay implements Overlay {

    /** The adopted bounce (B-25 recommendation, plan §1.2). */
    public static final double BOUNCE_SHOCK = 150;
    /** The prototype's bounce, for the golden test. */
    public static final double PROTOTYPE_BOUNCE_SHOCK = 90;
    public static final double TACH_BOUNCE = 94000;
    public static final double MC_BOUNCE = 1.44;
    public static final double MAX_ABSORB_POINTS = 90;
    public static final double SPACETIME_PER_POINT = 1000;
    public static final double HSM_SEED = 5000;
    public static final double BOUNCE_INTEGRITY_LOSS = 20;
    public static final int SETTLE_STREAK = 60;
    public static final int SETTLE_LIMIT = 180;

    public final double bounceShock;

    /** 1 = fast-forward, 3 = settle. */
    public int phase = 1;
    public double tach;
    public double Mc = 0.5;
    /** Settle seconds and the current Stable-or-better streak. */
    public int st;
    public int streak;

    // ---- per-second inputs from the controller
    /** Time fluid dosed this second (phase 1), litres. */
    public double dose;
    /** Molten SpaceTime buffered, litres (read at the bounce). */
    public double spaceTimeBuffer;
    /** False when the 5,000 L HSM neutron seed is gone at the bounce: supernova ("no neutron seed"). */
    public boolean hsmSeedPresent = true;

    // ---- the bounce, for readouts
    public boolean bounced;
    public double shockP0, shockTh0, shockP, shockTh, absorbed;

    public CollapseOverlay(double bounceShock) {
        this.bounceShock = bounceShock;
    }

    /** The shock the bounce would apply with this SpaceTime buffer (readout). */
    public double predictedShock(double spaceTime) {
        return bounceShock - Math.min(MAX_ABSORB_POINTS, Math.floor(spaceTime / SPACETIME_PER_POINT));
    }

    @Override
    public void addDrift(FieldState s, StepInputs in, double[] dp) {
        if (phase == 1) {
            final double sw = 2 * (Mc - 0.5) / 0.94;
            dp[P] += sw;
            dp[TH] += sw;
        }
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        if (phase == 1) {
            tach += dose;
            out.timeDosed = dose;
            Mc = 0.5 + tach / 100000;
            if (tach >= TACH_BOUNCE) {
                Mc = MC_BOUNCE;
                bounce(s, in, out);
            }
        } else if (phase == 3) {
            st++;
            streak = zone <= 1 ? streak + 1 : 0;
            if (streak >= SETTLE_STREAK) out.transitionSuccess = true;
            else if (st >= SETTLE_LIMIT) out.fail = "not Stable for 60 s within 180 s";
        }
    }

    private void bounce(FieldState s, StepInputs in, StepResult out) {
        if (!hsmSeedPresent) { // plan §7.4; never hit by the prototype (it requires the HSM to begin)
            out.supernova = true;
            out.supernovaReason = "no neutron seed";
            s.setX(0, 0, 0, 0);
            return;
        }
        final double sAbs = Math.min(MAX_ABSORB_POINTS, Math.floor(spaceTimeBuffer / SPACETIME_PER_POINT));
        final double shock = bounceShock - sAbs;
        out.bounced = true;
        out.spaceTimeTaken = sAbs * SPACETIME_PER_POINT;
        out.hsmSeedTaken = HSM_SEED;
        bounced = true;
        absorbed = sAbs;
        shockP0 = s.x[P];
        shockTh0 = s.x[TH];
        s.x[P] = StellarField.clamp(s.x[P] + shock, -120, 120);
        s.x[TH] = StellarField.clamp(s.x[TH] + shock, -120, 120);
        s.I = Math.max(0, s.I - BOUNCE_INTEGRITY_LOSS);
        shockP = s.x[P];
        shockTh = s.x[TH];
        if (s.x[P] > 100 || s.x[TH] > 100) {
            out.supernova = true;
            out.supernovaReason = (s.x[P] > 100 ? "P" : StellarField.GAUGE_SYM[TH])
                + " ended above +100 after the bounce";
            s.setX(0, 0, 0, 0);
            return;
        }
        s.tier = TierParams.T4;
        s.setX(0, 60, 30, 0);
        s.lock = 0;
        phase = 3;
        st = 0;
        streak = 0;
        StellarField.light(s, in.dialSetpoint, in.tt);
        out.relit = true;
    }
}
