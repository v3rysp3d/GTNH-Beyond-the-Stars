package com.nateyburr.beyondthestars.machines.quasar.engine;

import com.nateyburr.beyondthestars.machines.eye.field.StellarField;

/**
 * P4: the Quasar Engine's accretion disc, one second (quasar-p4.md §1.3-§1.4). Mirrors the prototype's
 * {@code quasarStep} line for line (designs/quasar-p4-prototype.html, the P4 block), RNG draw included (one per
 * open second, for ξ), so tools/quasar-golden.js can prove the Java equals the prototype bit for bit. The prototype's
 * own automation (the disc rig, the purge regulator) is not here: in the mod it is the player's Directive rig, and
 * the golden harness ({@link QuasarSim}) supplies it as inputs.
 *
 * <pre>
 * Ṁ_Edd = 40·M/κ;  f_eff = gas drawn / Ṁ_Edd;  b = min(b set, coil tier), 0 without coil power
 * E ← clamp(E + (100(f_eff − 1) − E)/30 + 0.04·max(E, 0) + ξ(±2) + 2·u_gas − 2·u_space, ±120)
 * λ = max(0, 1 + E/100), λc = min(λ, 1);  a ← a + (a_eq(λc, b) − a)/1,800
 * zone on |E| (D-49's table); Integrity; Breach or I = 0 → blow-out (Φ 0, jets dark, re-form 300 s at f ≥ 0.3)
 * Y_q = 1.25 / 1 / 0.5 / 0 (1.5 after 600 s Resonant);  G0 = λc·a²·b²·(M/64)²
 * Φ ← min(100, Φ + b·λc/2.4);  at Φ = 100, or a purge at Φ ≥ 20: +3·G0·Y_q·Φ/100 for 3 s, E −30·Φ/100,
 *     a −0.002·Φ/100, Φ → 0
 * G = G0·Y_q·(0.5 + Φ/100) (+ the eruption);  M ← min(cap, M + 6.4×10⁻⁸·(1 − η(a))·λc·Ṁ_Edd)
 * </pre>
 *
 * SMBH (smbh-v1.md §10.1): G0 takes {@link QuasarParams#g0(double, double, double, double, double)} with the state's
 * o_eff, and the growth cap is {@link QuasarParams#growthCap} (the coil cap without a lattice). With no lattice, M
 * at or under 4,096 SM and o_eff 0 both are today's expressions, so this step is unchanged bit for bit there.
 * {@link HorizonLattice#step} runs after it.
 */
public final class QuasarDisc {

    private QuasarDisc() {}

    /** D-49's zone table (star-lifecycle.md §3.3), on |E|: yield and Integrity change per second. */
    public static final double[] ZONE_Y = { 1.25, 1.0, 0.5, 0, 0 };
    public static final double[] ZONE_DI = { 1, 0.2, -0.5, -2, 0 };
    public static final int LOCK_SECONDS = 600;
    public static final double LOCK_Y = 1.5;

    static double clamp(double v, double a, double b) {
        return v < a ? a : v > b ? b : v;
    }

    /**
     * One disc second. A hole that is not open or re-forming (none, dormant) is frozen: nothing changes, no output.
     */
    public static QuasarResult step(QuasarState q, QuasarInputs in) {
        final QuasarResult out = new QuasarResult();
        if (q.hole != QuasarState.Hole.OPEN && q.hole != QuasarState.Hole.REFORM) {
            q.G = 0;
            q.G0 = 0;
            return out;
        }
        final double b = in.powerOk ? Math.min(in.bSetpoint, in.coilTier) : 0;
        final double Medd = QuasarParams.eddingtonFeed(q.M, in.gas);
        final double fEff = in.gasDrawn / Medd;
        q.fEff = fEff;
        q.b = b;
        q.Medd = Medd;
        out.b = b;
        out.medd = Medd;
        out.fEff = fEff;
        out.coilEUt = QuasarParams.coilEUt(b);
        if (q.flash > 0) q.flash--;

        // ---- re-forming after a blow-out
        if (q.hole == QuasarState.Hole.REFORM) {
            if (fEff >= QuasarParams.REFORM_MIN_FEED) q.reform++;
            q.G = 0;
            q.G0 = 0;
            q.erupt = 0;
            q.zone = -1;
            q.Yq = 0;
            out.reforming = true;
            if (q.reform >= QuasarParams.REFORM_SECONDS) {
                q.hole = QuasarState.Hole.OPEN;
                q.E = 0;
                q.I = 100;
                q.Phi = 0;
                q.lock = 0;
                q.reform = 0;
                out.reformed = true;
            }
            q.lagE = q.E;
            q.lagPhi = q.Phi;
            return out;
        }

        // ---- the Eddington balance E
        final double uG = clamp(in.gasTrimUnits, 0, QuasarParams.TRIM_CAP);
        final double uS = clamp(in.spaceTrimUnits, 0, QuasarParams.TRIM_CAP);
        out.gasTrimUnits = uG;
        out.spaceTrimUnits = uS;
        out.gasTrimLitres = uG * 1000 * q.M / 64;
        out.spaceLitres = uS * 100 * q.M / 64;
        q.E = clamp(
            q.E + (100 * (fEff - 1) - q.E) / 30
                + 0.04 * Math.max(q.E, 0)
                + (q.rng.next() * 2 - 1) * 2
                + 2 * uG
                - 2 * uS,
            -120,
            120);
        final double lam = Math.max(0, 1 + q.E / 100), lc = Math.min(lam, 1);
        q.lam = lam;
        q.lc = lc;
        q.a += (QuasarParams.aEq(lc, b) - q.a) / 1800;

        // ---- zone, lock, Integrity, blow-out
        final int z = StellarField.zoneOf(Math.abs(q.E));
        q.zone = z;
        out.zone = z;
        q.lock = z == 0 ? q.lock + 1 : 0;
        q.I = clamp(q.I + ZONE_DI[z], 0, 100);
        if (z == 4 || q.I <= 0) {
            out.blowOut = true;
            out.blowOutReason = z == 4
                ? (q.E > 0 ? "over-Eddington runaway, E " : "starved, E ") + StellarField.fmtSigned(q.E)
                : "disc Integrity reached 0";
            q.hole = QuasarState.Hole.REFORM;
            q.reform = 0;
            q.Phi = 0;
            q.G = 0;
            q.G0 = 0;
            q.erupt = 0;
            q.zone = -1;
            q.Yq = 0;
            q.lagE = q.E;
            q.lagPhi = 0;
            q.flash = 0;
            return out;
        }
        final double Yq = (z == 0 && q.lock >= LOCK_SECONDS) ? LOCK_Y : ZONE_Y[z];
        q.Yq = Yq;
        out.yieldCounted = true;

        // ---- the stream, Φ, eruptions and purges
        // SMBH: X(M, o_eff) past 4,096 SM or under Overdrive; today's expression otherwise (bit for bit)
        final double G0 = QuasarParams.g0(lc, q.a, b, q.M, q.oEff);
        q.G0 = G0;
        out.G0 = G0;
        q.Phi = Math.min(100, q.Phi + b * lc / 2.4);
        boolean purge = in.regulatorPurge;
        if (q.purgeLatch) {
            purge = true;
            q.purgeLatch = false;
        }
        if (q.Phi >= 100 || (purge && q.Phi >= QuasarParams.PURGE_MIN)) {
            final boolean full = q.Phi >= 100;
            q.lastWasEruption = full;
            q.erupt = QuasarParams.ERUPT_SECONDS;
            q.eruptG = G0 * Yq * 3 * (q.Phi / 100);
            q.E = clamp(q.E - 30 * q.Phi / 100, -120, 120);
            q.a -= 0.002 * q.Phi / 100;
            q.Phi = 0;
            q.flash = 3;
            if (full) out.eruption = true;
            else out.purge = true;
            if (!q.firstErupt) {
                q.firstErupt = true;
                out.firstErupt = true;
            }
        } else if (purge) out.purgeRefused = true;
        double G = G0 * Yq * (0.5 + q.Phi / 100);
        if (q.erupt > 0) {
            G += q.eruptG;
            q.erupt--;
        }
        q.G = G;
        out.G = G;
        out.jet = G / 2;

        // ---- hole growth, to the coil cap (SMBH: with a lattice, to the mass setpoint; HorizonLattice adds the
        // intake's Growth segment)
        final double cap = QuasarParams.growthCap(in);
        if (q.M < cap) q.M = Math
            .min(cap, q.M + 6.4e-8 * (1 - QuasarParams.iscoEta(clamp(q.a, 0, QuasarParams.A_MAX))) * lc * Medd);
        out.massCapped = q.M >= cap;

        // ---- milestones (display only)
        if (!q.firstLight && G > 0) {
            q.firstLight = true;
            out.firstLight = true;
        }
        if (z == 0 && lam >= QuasarParams.EDD_MILESTONE_LAMBDA) q.edd++;
        else q.edd = 0;
        if (!q.eddDone && q.edd >= QuasarParams.EDD_MILESTONE_SECONDS) {
            q.eddDone = true;
            out.eddington = true;
        }
        for (int i = 0; i < QuasarParams.MASS_MILESTONES.length; i++) {
            if ((q.massMilestones & (1 << i)) == 0 && q.M >= QuasarParams.MASS_MILESTONES[i]) {
                q.massMilestones |= 1 << i;
                out.massMilestone = i;
            }
        }
        q.lagE = q.E;
        q.lagPhi = q.Phi;
        return out;
    }
}
