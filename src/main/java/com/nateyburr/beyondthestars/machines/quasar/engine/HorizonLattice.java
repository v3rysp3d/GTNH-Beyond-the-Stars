package com.nateyburr.beyondthestars.machines.quasar.engine;

/**
 * SMBH v1: the Supermassive Lattice's second (smbh-v1.md §2-§5, §10.1). Pure (no Minecraft or GT types). Mirrors the
 * reference block {@code tools/fixtures/smbh-engine.js} ({@code smbhPlan} / {@code horizonStep}) line for line, so
 * tools/smbh-golden.js can prove the Java equals it bit for bit.
 * <p>
 * The second, in order (the MTE, {@link SmbhSim} and the reference all do exactly this):
 * <ol>
 * <li>{@link #plan}: from the start-of-second state, the lattice in force, the band, the segments' needs, the
 * Overdrive upkeep and the anchor. The controller reads it to know what to have on hand.</li>
 * <li>{@link QuasarDisc#step}: the disc (its own growth capped at the mass setpoint; G0 with last second's o_eff).</li>
 * <li>{@link #step}: the intake split (Disc, then Hold-up, then Growth), the Growth segment's mass, the Overdrive
 * cover (o_eff for the next second), the anchor, then a shed or the leak. It reports the litres to drain.</li>
 * </ol>
 *
 * <pre>
 * Ṁ_Edd = 40·M/κ;  cap I_max = 5·Ṁ_Edd;  x = log(M/safe)/log(top/safe) (0..1)
 * Disc  = the disc's own draw (f·Ṁ_Edd, QuasarInputs.gasDrawn; the only part that moves E)
 * Hold  = min(on hand, 3·x·Ṁ_Edd);  c_h = Hold/(3·x·Ṁ_Edd) (1 when none needed; 0 when the disc is dark)
 * Growth = min(on hand − Hold, I_max − Disc − 3·x·Ṁ_Edd);  M ← min(setpoint, M + 6.4e-8·(1 − η(a))·Growth)
 * Anchor (L4, Unbound, n &gt; 0): need 1,024·4^(n²) L/s; c_a = used/need. The Primordial Matter on hand caps Unbound
 *     mass at 67,108,864·2^n' (n' = what it pays for): growth (the disc's too) stops there, a bigger hole sheds to it
 * Shed: target = min(setpoint, structure top, anchor cap); M ← max(target, 0.98·M); the stream is 0 (no power,
 *     no Overdrive burnt, no hold-up or growth drawn)
 * else Overdrive (L3+, disc lit): need 100,000·o²·2^(−n) L/s each; c_o = min(MHDCSM, Universium on hand)/need;
 *     o_eff(next second) = o·c_o
 * and the leak: ln(M/safe) ← ln(M/safe)·2^(−(1 − c_h)/3,600), never below safe
 * </pre>
 *
 * A hole that is not open or re-forming is frozen here too. Without a lattice (lattice 0, or coils under MagMatter)
 * every segment is 0 and nothing changes, except a hole above the coil cap (a coil downgrade) sheds to it.
 */
public final class HorizonLattice {

    private HorizonLattice() {}

    /** What a second will ask for, from the start-of-second state (read-only; the controller sizes its draws on it). */
    public static final class Plan {

        /** The hole runs (open or re-forming); the disc is lit (open). */
        public boolean active, open;
        /** Mass at the start of the second, SM. */
        public double M;
        /** The lattice in force (0-4), safe limit, band top, the structure's top, the setpoint in force, SM. */
        public int lattice;
        public double safe, top, structureTop, setpoint;
        /** x (0..1) and n (Unbound doublings). */
        public double x, n;
        /** Ṁ_Edd, the intake cap, the hold-up need, and the room the Growth segment has, L/s. */
        public double medd, intakeCap, holdUpNeed, growthRoom;
        /** The setpoint or structure top is under M: the second sheds (an anchor shed is decided in the step). */
        public boolean shedPlanned;
        /** Overdrive: the level asked for (0 when it cannot run) and its upkeep each, L/s. */
        public double overdrive, overdriveNeed;
        /** Anchor need U(n), L/s, and the most mass the Primordial Matter on hand holds (+∞ when not Unbound). */
        public double anchorNeed, anchorCap = Double.POSITIVE_INFINITY;
        /** The growth cap: the setpoint, and the anchor's cap when Unbound ({@link QuasarParams#growthCap}). */
        public double growthCap;
        /** The shed target: the growth cap and the structure's top. */
        public double target;
        /** The o_eff this second's G0 uses (last second's cover). */
        public double oEffUsed;
    }

    /**
     * The second's plan (step 1). {@code in.gasDrawn} must already hold the disc's draw, and
     * {@code in.anchorAvailable} the Primordial Matter on hand (it caps Unbound growth); the other supplies may be
     * filled in after it.
     */
    public static Plan plan(QuasarState q, QuasarInputs in) {
        final Plan p = new Plan();
        p.active = q.hole == QuasarState.Hole.OPEN || q.hole == QuasarState.Hole.REFORM;
        p.open = q.hole == QuasarState.Hole.OPEN;
        p.M = q.M;
        p.oEffUsed = q.oEff;
        p.lattice = QuasarParams.latticeInForce(in.coilTier, in.lattice);
        p.safe = QuasarParams.safeLimit(in.coilTier, in.lattice);
        p.top = QuasarParams.bandTop(in.coilTier, in.lattice);
        p.structureTop = QuasarParams.structureTop(in.coilTier, in.lattice, in.unbound);
        p.setpoint = QuasarParams.massSetpoint(in.coilTier, in.lattice, in.unbound, in.massSetpoint);
        p.x = QuasarParams.bandX(q.M, in.coilTier, in.lattice);
        p.n = p.lattice == QuasarParams.LATTICE_MAX && in.unbound ? QuasarParams.unboundN(q.M) : 0;
        p.medd = QuasarParams.eddingtonFeed(q.M, in.gas);
        p.intakeCap = QuasarParams.INTAKE_CAP * p.medd;
        p.growthCap = QuasarParams.growthCap(in);
        if (in.unbound && p.lattice == QuasarParams.LATTICE_MAX)
            p.anchorCap = QuasarParams.anchorCap(in.anchorAvailable);
        p.target = Math.min(p.growthCap, p.structureTop);
        p.shedPlanned = p.active && q.M > p.target;
        if (p.open && p.lattice > 0 && !p.shedPlanned) {
            p.holdUpNeed = QuasarParams.holdUpNeed(p.x, p.medd);
            if (q.M < p.growthCap) p.growthRoom = Math.max(0, p.intakeCap - in.gasDrawn - p.holdUpNeed);
        }
        final double o = Math.max(0, Math.min(1, in.overdrive));
        if (p.open && p.lattice >= QuasarParams.LATTICE_OVERDRIVE && o > 0) {
            p.overdrive = o;
            p.overdriveNeed = QuasarParams.overdriveUpkeep(o, p.n);
        }
        if (p.active && p.n > 0) p.anchorNeed = QuasarParams.anchorUpkeep(p.n);
        return p;
    }

    /**
     * The lattice's part of the second (step 3), after {@link QuasarDisc#step} produced {@code out}. Fills the SMBH
     * fields of {@code out} (the litres to drain among them) and updates M, o_eff and the shed flag.
     */
    public static void step(QuasarState q, QuasarInputs in, Plan p, QuasarResult out) {
        out.lattice = p.lattice;
        out.safe = p.safe;
        out.top = p.top;
        out.structureTop = p.structureTop;
        out.setpoint = p.setpoint;
        out.x = p.x;
        out.n = p.n;
        out.intakeCap = p.intakeCap;
        out.outputX = QuasarParams.outputX(p.M, p.oEffUsed);
        if (!p.active) {
            q.oEff = 0;
            q.shed = false;
            out.oEff = 0;
            return;
        }
        final boolean lit = q.hole == QuasarState.Hole.OPEN;

        // ---- the intake: Hold-up, then Growth (the disc took its own share already)
        final double avail = Math.max(0, in.intakeAvailable);
        out.holdUpNeed = p.holdUpNeed;
        double hold = 0, grow = 0, ch = 1;
        if (lit) {
            hold = Math.min(avail, p.holdUpNeed);
            grow = Math.min(avail - hold, p.growthRoom);
            if (p.holdUpNeed > 0) ch = hold / p.holdUpNeed;
        } else if (p.lattice > 0 && q.M > p.safe) ch = 0;
        out.holdUpCoverage = ch;

        // ---- the anchor: what it is paid; short, the hole sheds to the mass the supply holds (the plan's anchor cap)
        if (p.anchorNeed > 0) {
            final double used = Math.min(Math.max(0, in.anchorAvailable), p.anchorNeed);
            out.anchorNeed = p.anchorNeed;
            out.anchorCoverage = used / p.anchorNeed;
            out.anchorLitres = used;
            out.anchorPaysN = Math.min(p.n, QuasarParams.anchorDoublings(Math.max(0, in.anchorAvailable)));
            out.anchorShort = out.anchorCoverage < 1 - QuasarParams.ANCHOR_SLACK;
        }
        final double target = p.target;
        final String why = q.M > p.structureTop ? "structure"
            : p.anchorCap < Math.min(p.setpoint, p.structureTop) && q.M > p.anchorCap ? "anchor" : "setpoint";

        // ---- a shed (always safe: no power while it lasts), else growth and the leak
        if (q.M > target) {
            q.M = Math.max(target, q.M * (1 - QuasarParams.SHED_RATE));
            q.oEff = 0;
            q.shed = true;
            out.shedding = true;
            out.shedTarget = target;
            out.shedReason = why;
            q.G = 0;
            out.G = 0;
            out.jet = 0;
            return;
        }
        q.shed = false;

        // ---- Overdrive: the cover of the lower fluid, both burnt at that cover; o_eff for the next second
        double oEff = 0;
        if (lit && p.overdriveNeed > 0) {
            final double have = Math.min(Math.max(0, in.mhdcsmAvailable), Math.max(0, in.universiumAvailable));
            final double co = Math.min(1, have / p.overdriveNeed);
            out.overdriveNeed = p.overdriveNeed;
            out.overdriveCoverage = co;
            out.mhdcsmLitres = co * p.overdriveNeed;
            out.universiumLitres = out.mhdcsmLitres;
            oEff = p.overdrive * co;
        }
        q.oEff = oEff;
        out.oEff = oEff;

        out.holdUpLitres = hold;
        out.growthLitres = grow;
        if (grow > 0 && q.M < p.growthCap) {
            final double m0 = q.M;
            q.M = Math.min(
                p.growthCap,
                q.M + QuasarParams.GROWTH * (1 - QuasarParams.iscoEta(QuasarDisc.clamp(q.a, 0, QuasarParams.A_MAX)))
                    * grow);
            out.grownMass = q.M - m0;
        }
        if (p.lattice > 0 && ch < 1 && q.M > p.safe) {
            final double m0 = q.M;
            final double l = StrictMath.log(q.M / p.safe)
                * QuasarParams.powE(2, -(1 - ch) / QuasarParams.LEAK_HALF_LIFE);
            q.M = Math.max(p.safe, p.safe * StrictMath.exp(l));
            out.leaking = true;
            out.leakedMass = m0 - q.M;
        }
    }
}
