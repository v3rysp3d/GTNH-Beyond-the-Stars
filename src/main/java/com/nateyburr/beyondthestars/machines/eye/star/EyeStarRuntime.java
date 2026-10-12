package com.nateyburr.beyondthestars.machines.eye.star;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.EyeTrimRule;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.Overlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * D-49: one Eye's live star (d49-build-plan.md §4 "The controller around step", §6, §7, §8). Owns the state machine
 * ({@code DARK → IGNITING → LIT}, PENDING, and T5's {@code IDLE → BATCH}), the {@link FieldState}, cycle charging (EU
 * and upkeep → the cycle's r), ignition, the time dial and its Time payment, the trims drained from the Field sockets,
 * the T4 HSM feed, the transitions ({@link EyeTransition}), the T5 cooling batches, the T6 magnetar (its Agent feed,
 * the dwarf-matter top-ups and the Tengam return), the output, the milestone counters and the core escrow. The
 * controller calls {@link #second()} once per field second (every 20 ticks, straight after its socket drain) and
 * supplies the world through {@link Host}.
 * <p>
 * <b>Unlocking</b> (plan §7.1, §8): LIT(N) with the chart of N+1 in the slot is PENDING; the transition begins the
 * moment every precondition holds (milestones and the core in escrow the first time, the next band, compression at
 * N+1, the star Stable or better, the event fluids buffered). T4 → T5 (First Cooling) and T5 → T6 (Magnetar Merger)
 * start from a dark Eye with the Sirius B / Magnetar chart in the slot. Success unlocks N+1 for good and consumes the
 * core if one was used; failure or an abort (wrench, soft mallet, structure, target change) loses the star and the
 * event materials and keeps the core.
 * <p>
 * Order of one lit second (the prototype's, so {@code /btseye star … sim N compare} matches {@code FieldSim}): dial
 * payment, trims, feed availability, the transition's inputs, {@link StellarField#step}, then (the order of
 * {@link StepResult}) the bounce's materials, failure or supernova, a relit star's first cycle, transition success,
 * the HSM draw, the cycle end (product, then the next cycle's charge), the batch end, publish.
 */
public final class EyeStarRuntime {

    /** The star's state (plan §3.2). */
    public enum StarState {
        DARK,
        IGNITING,
        LIT,
        /** LIT, with the next tier's chart in the slot: the transition waits on its preconditions. */
        PENDING,
        /** T5: Sirius B selected, no batch running (waiting for a White Dwarf Matter charge). */
        IDLE,
        /** T5: a cooling batch runs. */
        BATCH
    }

    /** What the runtime needs from the controller. Server side only. */
    public interface Host {

        /** False until the controller's first structure check after load: the star freezes, it does not go out. */
        boolean structureChecked();

        boolean formed();

        boolean allowedToWork();

        /** {@code min(unlockedTier, structureTier, compressionTier)}. */
        int installedTier();

        /** The cumulative bands' tier (0 unformed). Transitions test it directly (plan §6.5). */
        int structureTier();

        /** The highest tier every compression generator meets (0 none). Transitions test it directly. */
        int compressionTier();

        /** Lowest Time Acceleration generator tier (1..9), 1 if none. */
        int timeTier();

        /** Lowest Stabilisation generator tier (1..9), 1 if none. */
        int stabilisationTier();

        /** The controller-slot item. */
        ItemStack target();

        long buffer(Fluid fluid);

        void takeBuffer(Fluid fluid, long litres);

        /** Drains up to {@code max} litres of {@code fluid} from the live Field sockets; returns litres drained. */
        long drainField(Fluid fluid, long max);

        /** Takes (or with {@code simulate}, finds) one Stellar Ignition Charge in a plinth input bus. */
        boolean takeIgnitionCharge(boolean simulate);

        /** The wireless network's owner, or null. */
        UUID owner();

        /** Puts litres of fluid out through the ME output hatch(es). */
        void output(Fluid fluid, long litres);

        /** Puts items out through the ME output bus(es) (the T6 Tengam return). */
        void outputItems(ItemStack stack, long count);

        /** Pushes a readout to every live Stellar Field Sensor Hatch. */
        void publish(FieldReadout readout);

        void markDirty();
    }

    /** The T4 HSM feed setpoint range and the default (today's rate, plan §6.1). */
    public static final double FEED_MAX = 1.5, FEED_DEFAULT = 0.694, FEED_STEP = 0.1;
    /** The dial setpoint range (plan §3.3, §6.4). */
    public static final double DIAL_MAX = 3.0, DIAL_STEP = 0.25;
    /** Helium-flash ride window after the last hit second (plan §8). */
    public static final int FLASH_TAIL = 30;

    /** T5: the White Dwarf Matter charge per batch, per parallel (plan §6.1). */
    public static final long BATCH_WDM = 18_432L;
    /** T5: the fracture salvage, a share of the full batch × the fraction completed (plan §6.6). */
    public static final double FRACTURE_SALVAGE = 0.25;

    /**
     * T6 (my answer: the Agent is the CONSTANT feed; "the White + Black Dwarf Matter merger spawns the
     * magnetar and tops it up to keep it stable"): the dwarf-matter reserve, in cycles. The merger charge fills
     * {@link #MERGER_RESERVE_CYCLES}; each cycle spends one; at {@link #TOPUP_AT} cycles or fewer the Eye tops it up
     * from the buffers with White Dwarf Matter {@link #TOPUP_WDM} + Black Dwarf Matter {@link #TOPUP_BDM} (× p) for
     * {@link #TOPUP_CYCLES} more. A magnetar with an empty reserve starves: that cycle's r is 0 (no output, P sags).
     * PROPOSED (B-63).
     */
    public static final int MERGER_RESERVE_CYCLES = 60, TOPUP_CYCLES = 60, TOPUP_AT = 10;
    public static final long TOPUP_WDM = 1_800L, TOPUP_BDM = 450L;
    /** T6: the Magnetar Merger charge besides {@link #BATCH_WDM} (per parallel, plan §6.1). */
    public static final long MERGER_BDM = 4_608L, MERGER_AGENT = 100_000L;
    /** T6: Purified Tengam dust returned per 1,000 L of Agent consumed (plan §10.1). */
    public static final double TENGAM_PER_KL = 12;

    final Host host;

    // ---------------------------------------------------------------- persistent (item NBT, EyeNbt)
    /** The permanent unlocked tier, 1..6. */
    int unlockedTier = 1;
    final EyeMilestones milestones = new EyeMilestones();
    /** The core waiting for its transition, or null. */
    ItemStack escrow;
    double dialSet = 1;
    double feedSet = FEED_DEFAULT;
    /** Migration: grandfather the bands at the first successful walk (plan §13). */
    boolean legacyPending;
    /** Migration: outputs owed for a pre-D-49 star run in flight (fluid name → litres). */
    final Map<String, Long> legacyPay = new LinkedHashMap<>();

    // ---------------------------------------------------------------- tile NBT (the live star)
    StarState state = StarState.DARK;
    StarDef star;
    FieldState fs;
    long seed;
    double cycR = 1;
    /**
     * The parallel the running cycle's EU and upkeep were paid for ({@link #chargeCycle}), and the T5 batch charge's
     * ({@link #startCooling}, {@link #nextBatch}); 0 = not known (a save from before this field), no cap. A lit second
     * runs at {@code min(parallel, cycP[, batchP])}, so a parallel raised mid-cycle (a band put back) counts from the
     * next charge, never for output that was not paid for.
     */
    int cycP, batchP;
    double hsmOwed;
    double prodCarry;
    double tengam;
    int cycWorst;
    /** Helium-flash ride watch: 0 idle, 1 warned / hitting, 2 the 30 s tail. */
    int flashState;
    int flashTail;
    boolean flashOk;
    FieldRig.Mode rig;
    String lastEvent = "";
    Ledger ledger;
    /** The transition running, or null. */
    EyeTransition trans;
    /** The bounce of the next Core Collapse (150 adopted; the bench's {@code set shock 90} for the golden cases). */
    double bounceShock = CollapseOverlay.BOUNCE_SHOCK;
    /** T6: the dwarf-matter reserve in cycles, and the top-ups taken. */
    double dwarfReserve;
    long topUps;
    /** T6: Purified Tengam dust returned so far (display). */
    long tengamOut;

    // ---------------------------------------------------------------- transient (scanner)
    private FieldReadout last = FieldReadout.DARK;
    private StepResult lastOut;
    private double lastM = 1, lastFeed;
    private int lastP = 1, lastLiveP = 1;
    private long lastTimePaid;
    private final Map<TrimDef.Kind, Long> lastTrims = new EnumMap<>(TrimDef.Kind.class);
    private String waitReason = "";
    private final StepInputs in = new StepInputs();
    /** The last way a star was lost or put out (GUI, scanner); tile NBT with the star. */
    String lastFailure = "";
    /** Set when a Core Collapse ends in a supernova; the centre render takes it for its flash. Not saved. */
    private boolean novaPending;
    /** The last {@link #salvage()} was owed (the Eye was unformed), not emitted. */
    private boolean salvageOwed;

    public EyeStarRuntime(Host host) {
        this.host = host;
    }

    // =========================================================================== accessors

    public StarState state() {
        return state;
    }

    public StarDef star() {
        return star;
    }

    /** True while a star exists (igniting, lit, pending or a batch running). */
    public boolean live() {
        return state != StarState.DARK && state != StarState.IDLE;
    }

    /** True while the star burns (the controller's active flag; GT's Activity Detector cover). */
    public boolean burning() {
        return state == StarState.LIT || state == StarState.PENDING || state == StarState.BATCH;
    }

    /** The transition running, or null. */
    public EyeTransition transition() {
        return trans;
    }

    public int unlockedTier() {
        return unlockedTier;
    }

    public EyeMilestones milestones() {
        return milestones;
    }

    public ItemStack escrow() {
        return escrow;
    }

    public double dialSetpoint() {
        return dialSet;
    }

    public double feedSetpoint() {
        return feedSet;
    }

    public double bounceShock() {
        return bounceShock;
    }

    public FieldReadout lastReadout() {
        return last;
    }

    public boolean legacyPending() {
        return legacyPending;
    }

    public FieldRig.Mode rig() {
        return rig;
    }

    public Ledger ledger() {
        return ledger;
    }

    /** T6: the dwarf-matter reserve in cycles. */
    public double dwarfReserve() {
        return dwarfReserve;
    }

    /** The last way a star was lost or put out, or "" (GUI, scanner). */
    public String lastFailure() {
        return lastFailure;
    }

    /** IGNITING: the protostar's P (−80 → −20 lights); 0 when there is no field. Centre render. */
    public double ignitionP() {
        return fs == null ? 0 : fs.x[StellarField.P];
    }

    /** T5: how far the running cooling batch is (0..1), 0 without one. Centre render, GUI. */
    public double batchFraction() {
        return fs == null || fs.batch == null ? 0 : Math.min(1, fs.batch.eyeT / StellarField.BATCH_SECONDS);
    }

    /** The upkeep fraction r of the running cycle. GUI. */
    public double upkeepR() {
        return cycR;
    }

    /** Seconds to the next cycle at the current dial (0 when no star is lit). GUI. */
    public double nextCycleSeconds() {
        if (fs == null || fs.cyc == null || !burning()) return 0;
        return Math.max(0, (StellarField.CYCLE_SECONDS - fs.cyc.eyeT) / Math.max(1e-9, lastM));
    }

    /** The dial m actually paid this second (1 when none). GUI. */
    public double dialPaid() {
        return lastM;
    }

    /** The time dial's cap {@code 1 + 0.25·max(0, tt − N)} for the star lit now (1 when none). GUI. */
    public double dialMax() {
        return fs == null ? 1 : StellarField.mMax(fs.tier, host.timeTier());
    }

    /** The T4 HSM feed actually drawn this second, L/s per parallel. GUI. */
    public double feedActual() {
        return lastFeed;
    }

    /** Parallel of the star this second. GUI. */
    public int lastParallel() {
        return lastP;
    }

    /** Why a dark (or IDLE) star waits, or "". GUI. */
    public String waitReason() {
        return waitReason;
    }

    /** The last event line, or "". GUI. */
    public String lastEvent() {
        return lastEvent;
    }

    /**
     * How far the running transition is, 0..1 (GUI bar), or −1 with none. Mass Infusion and Shell Ignition by their
     * clocks; the Core Collapse is half fast-forward (Time dosed) and half settle (the Stable streak); the First
     * Cooling by its batch; the Magnetar Merger is 80 % imprint (seconds held) and 20 % release.
     */
    public double transitionProgress() {
        final EyeTransition tr = trans;
        if (tr == null) return -1;
        final double p;
        if (tr instanceof MassInfusion) {
            p = tr.t / (double) MassInfusion.DURATION;
        } else if (tr instanceof ShellIgnition) {
            p = tr.t / (double) (ShellIgnition.END - 1);
        } else if (tr instanceof CoreCollapse) {
            final CollapseOverlay co = ((CoreCollapse) tr).co;
            p = co.phase == 1 ? 0.5 * co.tach / CollapseOverlay.TACH_BOUNCE
                : 0.5 + 0.5 * co.streak / (double) CollapseOverlay.SETTLE_STREAK;
        } else if (tr instanceof FirstCooling) {
            p = batchFraction();
        } else if (tr instanceof MagnetarMerger) {
            final MagnetarMerger mm = (MagnetarMerger) tr;
            p = mm.phase == 1 ? 0.8 * mm.hold / MagnetarMerger.HOLD
                : 0.8 + 0.2 * mm.t2 / (double) MagnetarMerger.RELEASE_TIMEOUT;
        } else {
            p = 0;
        }
        return Math.max(0, Math.min(1, p));
    }

    /** The running transition's phase line, or "" (GUI). */
    public String transitionLine() {
        final EyeTransition tr = trans;
        if (tr == null) return "";
        return tr instanceof FirstCooling ? ((FirstCooling) tr).line(fs) : tr.phaseLine();
    }

    /** The gauges the field shows now (a transition's own set, else the tier's); empty when no star burns. GUI. */
    public int[] activeGauges() {
        if (fs == null || !burning()) return new int[0];
        return trans != null ? trans.gauges(fs.tier) : fs.tier.gauges();
    }

    /** The trims drained from the Field sockets this second, display name → litres (GUI). */
    public Map<String, Long> lastTrimsByName() {
        final Map<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<TrimDef.Kind, Long> e : lastTrims.entrySet()) out.put(e.getKey().displayName, e.getValue());
        return out;
    }

    /** True once after a supernova: the centre render shows its flash. */
    public boolean takeNova() {
        final boolean n = novaPending;
        novaPending = false;
        return n;
    }

    /**
     * Parallel for a star of tier n on this Eye: {@code 2^(installedTier − n)}; 1 for the star a running transition
     * is making before its tier is unlocked (the First Cooling batch, the merger, the settle phase), as long as the
     * band and compression reach it; else 0.
     */
    public int parallel(int n) {
        final int inst = host.installedTier();
        if (inst >= n) return 1 << (inst - n);
        if (trans != null && n == trans.type.from + 1 && n <= Math.min(host.structureTier(), host.compressionTier()))
            return 1;
        return 0;
    }

    /**
     * The parallel a lit second runs at: the live {@link #parallel}, capped by what the cycle paid for and, on a T5
     * batch, by what the batch charge paid for. Product, the HSM feed, the trims' caps and the field's per-parallel
     * division all use it, so the output always matches the upkeep taken.
     */
    int paidParallel(int live) {
        int p = live;
        if (cycP > 0) p = Math.min(p, cycP);
        if (batchP > 0 && fs != null && fs.tier.batch) p = Math.min(p, batchP);
        return Math.max(1, p);
    }

    /** The parallel the star of tier {@code n} will run at once its transition makes it (1 before it unlocks). */
    int destParallel(int n) {
        final int inst = host.installedTier();
        return inst >= n ? 1 << (inst - n) : 1;
    }

    // ============================================================================ settings

    /** Screwdriver: dial +0.25, wrapping 3.00 → 1.00. Returns the new setpoint. */
    public double stepDial() {
        dialSet = dialSet + DIAL_STEP > DIAL_MAX + 1e-9 ? 1 : dialSet + DIAL_STEP;
        host.markDirty();
        return dialSet;
    }

    /** Sneak-screwdriver: HSM feed +0.1 L/s, wrapping above 1.5 → 0. Returns the new setpoint. */
    public double stepFeed() {
        final double next = Math.round((feedSet + FEED_STEP) * 1000) / 1000.0;
        feedSet = next > FEED_MAX + 1e-9 ? 0 : next;
        host.markDirty();
        return feedSet;
    }

    public void setDial(double v) {
        setDialQuiet(v);
        host.markDirty();
    }

    public void setFeed(double v) {
        setFeedQuiet(v);
        host.markDirty();
    }

    /** Bench {@code set shock}: the bounce of the next Core Collapse (one running keeps its own). */
    public void setBounceShock(double v) {
        bounceShock = Math.max(0, Math.min(240, v));
        host.markDirty();
    }

    void setDialQuiet(double v) {
        dialSet = Math.max(1, Math.min(DIAL_MAX, Math.round(v / DIAL_STEP) * DIAL_STEP));
    }

    void setFeedQuiet(double v) {
        feedSet = Double.isNaN(v) ? FEED_DEFAULT : Math.max(0, Math.min(FEED_MAX, v));
    }

    /** Every field back to a new controller's defaults (before an NBT read, and the bench's wrench). */
    public void reset() {
        unlockedTier = 1;
        milestones.read(null);
        escrow = null;
        dialSet = 1;
        feedSet = FEED_DEFAULT;
        legacyPending = false;
        legacyPay.clear();
        state = StarState.DARK;
        star = null;
        fs = null;
        seed = 0;
        cycR = 1;
        cycP = batchP = 0;
        hsmOwed = prodCarry = tengam = 0;
        cycWorst = flashState = flashTail = 0;
        flashOk = false;
        rig = null;
        lastEvent = "";
        lastFailure = "";
        novaPending = false;
        ledger = null;
        trans = null;
        bounceShock = CollapseOverlay.BOUNCE_SHOCK;
        dwarfReserve = 0;
        topUps = 0;
        tengamOut = 0;
        last = FieldReadout.DARK;
        lastOut = null;
        lastTrims.clear();
        waitReason = "";
    }

    public void setRig(FieldRig.Mode mode) {
        rig = mode;
        host.markDirty();
    }

    /** Debug: sets the unlocked tier (1..6), lowering it too. */
    public void setUnlocked(int n) {
        unlockedTier = Math.max(1, Math.min(6, n));
        legacyPending = false;
        host.markDirty();
    }

    // ===================================================================== the field second

    /** One field second (every 20 ticks, server side). */
    public void second() {
        if (!host.structureChecked()) return; // freshly loaded: freeze until the first walk
        payLegacy();
        if (!host.formed()) {
            if (live()) extinguish("the structure is incomplete");
            else idleOff();
            return;
        }
        if (!host.allowedToWork()) {
            if (live()) extinguish("the machine was disabled");
            else idleOff();
            return;
        }
        final StarDef want = StarDef.byChart(host.target());
        if (live()) {
            if (parallel(star.n()) < 1) {
                extinguish("the Eye no longer reaches T" + star.n());
                return;
            }
            if (trans != null) {
                if (want == null || !want.key.equals(trans.type.dest)) {
                    extinguish("the target changed during the " + trans.type.title);
                    return;
                }
            } else if (want == star) {
                if (state == StarState.PENDING) state = StarState.LIT;
            } else if (want != null
                && (state == StarState.LIT || state == StarState.PENDING || state == StarState.IGNITING)
                && star.n() <= 3
                && want.n() == star.n() + 1) {
                    // the next tier's chart: PENDING once lit (an igniting star finishes its ignition first)
                    if (state != StarState.IGNITING) state = StarState.PENDING;
                } else {
                    extinguish("the target changed");
                    return;
                }
        } else if (state == StarState.IDLE && want != star) {
            idleOff();
        }
        if (state == StarState.PENDING) tryBeginFromPending();
        switch (state) {
            case IGNITING:
                igniteSecond();
                break;
            case LIT:
            case PENDING:
            case BATCH:
                litSecond();
                break;
            default:
                darkSecond(want);
                break;
        }
    }

    /** T5 IDLE → DARK (the chart went, or the Eye stopped). */
    private void idleOff() {
        if (state != StarState.IDLE) return;
        state = StarState.DARK;
        waitReason = "";
        host.markDirty();
    }

    private void darkSecond(StarDef want) {
        if (fs != null) StellarField.stepDark(fs);
        if (ledger != null) ledger.record(this, -1);
        tryAutoStart(want);
        publish(FieldReadout.DARK);
    }

    private void igniteSecond() {
        if (StellarField.stepIgniting(fs, dialSet, host.timeTier())) {
            state = StarState.LIT;
            lastEvent = star.name + " lit";
            chargeCycle();
        }
        if (ledger != null) ledger.record(this, -1);
        publish(FieldReadout.DARK);
        host.markDirty();
    }

    private void litSecond() {
        final TierParams T = fs.tier;
        final int n = T.n;
        final int live = parallel(n);
        if (live < 1) {
            extinguish("the Eye no longer reaches T" + n);
            return;
        }
        // the parallel paid for: a rise counts from the next charge, a fall at once (D-49 review fix)
        final int p = paidParallel(live);
        lastLiveP = live;
        final int tt = host.timeTier();
        // ---- the time dial (plan §6.4): pay Time from the Field sockets; a short payment slows the dial.
        // Transitions hold the dial at m = 1.
        final double mCap = trans != null ? 1 : Math.min(dialSet, StellarField.mMax(T, tt));
        double m = 1;
        lastTimePaid = 0;
        if (mCap > 1 + 1e-9) {
            final double perStep = 10 * Math.pow(2, n - 1);
            final long want = Math.round((mCap - 1) / DIAL_STEP * perStep);
            final long paid = host.drainField(timeFluid(), want);
            lastTimePaid = paid;
            m = paid >= want ? mCap : 1 + DIAL_STEP * paid / perStep;
        }
        in.m = m;
        in.q = StepInputs.qFor(host.stabilisationTier());
        in.r = cycR;
        in.p = p;
        in.dialSetpoint = dialSet;
        in.tt = tt;
        // ---- trims: the Field sockets, in the trim table's order, up to cap × p units each (Shell Ignition
        // drains by T3's table)
        final TierParams table = trans != null ? trans.trimTable(T) : T;
        in.clearTrims();
        lastTrims.clear();
        if (rig != null) {
            final LinkedHashMap<String, Double> raw = FieldRig.ctrl(table, fs.prevE, fs.prevWarn, rig);
            if (trans != null) trans.adjustRig(raw, fs, rig);
            FieldRig.toUnits(table, raw, in.u);
        } else {
            for (TrimDef trim : table.trims()) {
                final Fluid fluid = trimFluid(trim.kind);
                if (fluid == null) continue;
                final long max = (long) Math.floor(trim.cap * p * trim.kind.unitLitres);
                final long got = host.drainField(fluid, max);
                if (got > 0) {
                    in.u.put(trim, got / (double) trim.kind.unitLitres);
                    lastTrims.put(trim.kind, got);
                }
            }
        }
        // ---- T4: the HSM feed actually available this second, L/s per parallel. A Core Collapse computes it
        // too, so the bounce second feeds the proto-neutron star exactly as the prototype does.
        in.feed = 0;
        final Fluid feedFluid = T.feed && star.feed != null ? star.feed
            : trans instanceof CoreCollapse ? SLFluids.hyperdenseStellarMixture : null;
        if (feedFluid != null) {
            final double hsm = Math.max(0, host.buffer(feedFluid) - hsmOwed);
            in.feed = Math.min(feedSet, hsm / (m * p));
        }
        // ---- the Core Collapse's per-second inputs: the Time dose (≤ 100 L from the Field sockets; the debug rig
        // doses the prototype's 100 L/s), the SpaceTime buffer and the HSM seed for the bounce
        if (trans instanceof CoreCollapse) {
            final CollapseOverlay co = ((CoreCollapse) trans).co;
            co.dose = co.phase != 1 ? 0
                : rig != null ? CoreCollapse.DOSE_CAP : host.drainField(timeFluid(), (long) CoreCollapse.DOSE_CAP);
            co.spaceTimeBuffer = collapseSpaceTimeAvailable(); // the neutron star's first cycle held back
            co.hsmSeedPresent = host.buffer(SLFluids.hyperdenseStellarMixture) >= CollapseOverlay.HSM_SEED;
        }
        lastM = m;
        lastP = p;
        lastFeed = in.feed;

        final StepResult out = StellarField.step(fs, trans != null ? trans : Overlay.NONE, in);
        lastOut = out;
        // milestones for tier n count only once n is unlocked (plan §7.1): not during the settle or imprint that
        // makes the star of a still-locked tier
        if (out.zone == 0 && n <= unlockedTier) milestones.res[n]++;
        cycWorst = Math.max(cycWorst, out.zone);
        if (ledger != null) ledger.lit(out);
        creditAgent(lastTrims.containsKey(TrimDef.Kind.AGENT) ? lastTrims.get(TrimDef.Kind.AGENT) : 0);
        // ---- the bounce's materials (taken whatever follows: a supernova spends them too)
        if (out.bounced) {
            final Fluid st = spaceTimeFluid();
            host.takeBuffer(st, Math.min(host.buffer(st), (long) out.spaceTimeTaken));
            final Fluid hsm = SLFluids.hyperdenseStellarMixture;
            host.takeBuffer(hsm, Math.min(host.buffer(hsm), (long) out.hsmSeedTaken));
            if (ledger != null) ledger.stAtBounce = out.spaceTimeTaken;
        }
        if (out.supernova) {
            novaPending = true;
            failTransition(
                "SUPERNOVA: " + out.supernovaReason
                    + ". Arcturus is lost; the Time fluid, HSM and SpaceTime are spent");
            if (ledger != null) ledger.record(this, out.zone);
            return;
        }
        if (out.fail != null) {
            if (T.batch) fracture(out.fail);
            else if (trans != null) failTransition(out.fail);
            else fail(out.fail);
            if (ledger != null) ledger.record(this, out.zone);
            if (!burning()) return;
        } else {
            // ---- a transition made the next star mid-step: its first cycle (R-5)
            if (out.relit) relit();
            if (out.transitionSuccess) completeTransition();
            // ---- the HSM drawn this second (fractional litres carried)
            if (out.hsmToDraw > 0 && star.feed != null) {
                hsmOwed += out.hsmToDraw;
                final long take = Math.min((long) Math.floor(hsmOwed), host.buffer(star.feed));
                if (take > 0) {
                    host.takeBuffer(star.feed, take);
                    hsmOwed -= take;
                }
                if (ledger != null) ledger.hsm += out.hsmToDraw;
            }
            watchFlash(fs.tier.n, out);
            // ---- the cycle: product, milestones, then charge the next one (it sets the next r)
            if (out.cycleEnded) {
                final int cn = fs.tier.n;
                if (out.hasCycleProduct) {
                    emit(out.cycleProduct);
                    if (cn <= unlockedTier) {
                        if (cn <= 3) milestones.prod[cn] += out.cycleProduct;
                        else if (cn == 4 && cycWorst <= 1) milestones.prod[4] += out.cycleProduct;
                    }
                    if (ledger != null) ledger.product += out.cycleProduct;
                }
                if (ledger != null) ledger.cycles++;
                chargeCycle();
            }
            // ---- T5: the batch end
            if (out.batchDone) batchDone(out);
        }
        last = FieldReadout.of(fs, out, in);
        host.publish(last);
        if (ledger != null) ledger.record(this, out.zone);
        host.markDirty();
    }

    /** T3 milestone: a helium flash is ridden if the zone never leaves Stable from its warning to 30 s after it. */
    private void watchFlash(int n, StepResult out) {
        if (n != 3) {
            flashState = 0;
            return;
        }
        if (out.eventWarned) {
            flashState = 1;
            flashOk = true;
        }
        if (flashState == 0) return;
        if (out.zone > 1) flashOk = false;
        if (flashState == 1 && fs.ev != null && !fs.ev.warned) {
            flashState = 2;
            flashTail = FLASH_TAIL;
        } else if (flashState == 2 && --flashTail <= 0) {
            if (flashOk) milestones.flashes++;
            flashState = 0;
        }
    }

    private void emit(double litres) {
        prodCarry += litres;
        final long whole = (long) Math.floor(prodCarry);
        if (whole > 0 && star.product != null) {
            host.output(star.product, whole);
            prodCarry -= whole;
        }
    }

    private void publish(FieldReadout r) {
        last = r;
        host.publish(r);
    }

    /** T6: 12 Purified Tengam dust back per 1,000 L of Agent consumed (whole dust out, the rest carried). */
    private void creditAgent(long litres) {
        if (litres <= 0) return;
        tengam += litres * TENGAM_PER_KL / 1000.0;
        final long whole = (long) Math.floor(tengam);
        if (whole <= 0) return;
        final ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dust, Materials.TengamPurified, 1L);
        if (dust == null) return;
        host.outputItems(dust, whole);
        tengam -= whole;
        tengamOut += whole;
    }

    // ========================================================================= cycles, ignition

    /**
     * Debits the cycle's EU and takes its upkeep (plan §6.2). A failed debit makes r = 0; the fluids are still taken
     * (upkeep is paid in full whatever the yield), and r is the worst delivered fraction. T6 also spends a cycle of
     * the dwarf-matter reserve (topping it up from the buffers when low) and returns Tengam for the Agent. Resets the
     * cycle's worst zone (T4 milestone).
     */
    void chargeCycle() {
        final int p = Math.max(1, parallel(star.n()));
        double r = debit(
            BigInteger.valueOf(star.euPerCycle)
                .multiply(BigInteger.valueOf(p))) ? 1 : 0;
        for (EyeRecipe.FluidAmount up : star.upkeep) {
            final long need = up.amount * p;
            final long got = Math.min(host.buffer(up.fluid), need);
            if (got > 0) host.takeBuffer(up.fluid, got);
            if (up.fluid == SLFluids.antipolarizationAgent) creditAgent(got);
            r = Math.min(r, need <= 0 ? 1 : got / (double) need);
        }
        if (star.tier.shield) {
            if (dwarfReserve <= TOPUP_AT) topUp(p);
            if (dwarfReserve >= 1) dwarfReserve -= 1;
            else r = 0; // starving: no dwarf matter left to hold it together
        }
        cycR = r;
        cycP = p;
        lastP = p;
        cycWorst = 0;
        host.markDirty();
    }

    /** T6: one dwarf-matter top-up from the buffers if both fluids are there in full. */
    private void topUp(int p) {
        final Fluid wdm = wdmFluid(), bdm = bdmFluid();
        if (wdm == null || bdm == null) return;
        if (host.buffer(wdm) < TOPUP_WDM * p || host.buffer(bdm) < TOPUP_BDM * p) return;
        host.takeBuffer(wdm, TOPUP_WDM * p);
        host.takeBuffer(bdm, TOPUP_BDM * p);
        dwarfReserve += TOPUP_CYCLES;
        topUps++;
    }

    private boolean debit(BigInteger eu) {
        final UUID owner = host.owner();
        if (owner == null) return false;
        return WirelessNetworkManager.addEUToGlobalEnergyMap(owner, eu.negate());
    }

    private BigInteger networkEU() {
        final UUID owner = host.owner();
        return owner == null ? BigInteger.ZERO : WirelessNetworkManager.getUserEU(owner);
    }

    /**
     * Auto-start (plan §6.3, §7.5, §7.6): a dark Eye with a T1-T3 chart it reaches lights itself when everything is
     * there; a Sirius B chart starts the cooling batches (or the First Cooling); a Magnetar chart starts the merger.
     */
    private void tryAutoStart(StarDef want) {
        if (want == null) {
            waitReason = "";
            return;
        }
        final int n = want.n();
        if (n == 5) {
            tryStartCooling(want);
            return;
        }
        if (n == 6) {
            tryMerger(want);
            return;
        }
        if (!want.tier.ignite) {
            // T4 (D-49 review): once T4 is unlocked, a dark Eye holding the Neutron Star chart relights Arcturus by
            // itself from a Stellar Ignition Charge, exactly as a T1-T3 star relights; Arcturus then goes PENDING
            // and re-runs the Core Collapse (no core) once its fluids are buffered. So a lost neutron star, or one
            // put out for a Deep Dark run, comes back with no hand on the controller slot.
            final StarDef progenitor = StarDef.ofTier(n - 1);
            if (n == 4 && unlockedTier >= 4 && progenitor != null && progenitor.tier.ignite) {
                autoIgnite(progenitor, " (to re-run the Core Collapse for the neutron star)");
                return;
            }
            waitReason = "the neutron star is born from a lit Arcturus by the Core Collapse (light Arcturus, then put the"
                + " Neutron Star chart in while it burns; once T4 is unlocked the Eye relights Arcturus by itself)";
            return;
        }
        autoIgnite(want, "");
    }

    /**
     * Ignites {@code want} (T1-T3) from one Stellar Ignition Charge, the seed cloud and 10 cycles of EU when the Eye
     * reaches it and everything is there; otherwise sets the wait reason.
     */
    private void autoIgnite(StarDef want, String why) {
        final int n = want.n();
        if (n > host.installedTier()) {
            // name every limit: a migrated jet-without-beam Eye is unlocked T3 but its bands give T1
            waitReason = "the Eye does not reach T" + n
                + " (unlocked T"
                + unlockedTier
                + ", bands T"
                + host.structureTier()
                + ", compression T"
                + host.compressionTier()
                + ")";
            return;
        }
        final int p = parallel(n);
        if (!host.takeIgnitionCharge(true)) {
            waitReason = "no Stellar Ignition Charge in a plinth input bus";
            return;
        }
        for (Map.Entry<Fluid, Long> need : ignitionNeeds(want, p).entrySet()) {
            if (host.buffer(need.getKey()) < need.getValue()) {
                waitReason = "the buffers need " + fmt(need.getValue())
                    + " L of "
                    + fluidName(need.getKey())
                    + " (10 cycles of upkeep plus the seed cloud)";
                return;
            }
        }
        final BigInteger eu10 = BigInteger.valueOf(want.euPerCycle)
            .multiply(BigInteger.valueOf((long) p * StarDef.IGNITION_CYCLES));
        if (networkEU().compareTo(eu10) < 0) {
            waitReason = "the wireless network needs " + sci(eu10) + " EU (10 cycles)";
            return;
        }
        // ---- consume: one charge, the seed cloud, 10 cycles of EU
        if (!host.takeIgnitionCharge(false) || !debit(eu10)) return;
        for (EyeRecipe.FluidAmount s : want.seed) host.takeBuffer(s.fluid, s.amount * p);
        ignite(want);
        lastEvent = "ignition: " + want.name + " (1 charge, the seed cloud and " + sci(eu10) + " EU)" + why;
    }

    /** The buffered litres an ignition needs: 10 cycles of upkeep plus the seed cloud, × p. */
    public static Map<Fluid, Long> ignitionNeeds(StarDef def, int p) {
        final Map<Fluid, Long> out = new LinkedHashMap<>();
        for (EyeRecipe.FluidAmount up : def.upkeep) {
            out.merge(up.fluid, up.amount * p * StarDef.IGNITION_CYCLES, Long::sum);
        }
        for (EyeRecipe.FluidAmount s : def.seed) out.merge(s.fluid, s.amount * p, Long::sum);
        return out;
    }

    private void ignite(StarDef def) {
        ensureField(def);
        star = def;
        StellarField.beginIgnition(fs);
        state = StarState.IGNITING;
        flashState = 0;
        waitReason = "";
        host.markDirty();
    }

    /** The field for a new star: the existing state (clock and noise continue) at the new tier, or a fresh one. */
    private void ensureField(StarDef def) {
        if (fs == null) {
            seed = (long) (Math.random() * 3_000_000L) + 1;
            fs = new FieldState(def.tier, new FieldRng(seed));
        } else {
            fs.tier = def.tier;
        }
        fs.batch = null;
    }

    /**
     * Prototype {@code light()} plus the first cycle's charge (plan §4): Integrity 100, lock 0, a fresh cycle, the
     * first event scheduled. The gauges are kept.
     */
    private void light() {
        StellarField.light(fs, dialSet, host.timeTier());
        state = StarState.LIT;
        prodCarry = 0;
        hsmOwed = 0;
        flashState = 0;
        chargeCycle();
    }

    /** A transition lit the next star mid-step (its field part already ran): switch the star, charge its cycle. */
    private void relit() {
        final StarDef next = StarDef.byKey(trans != null ? trans.type.dest : null);
        if (next != null) star = next;
        prodCarry = 0;
        hsmOwed = 0;
        flashState = 0;
        chargeCycle();
    }

    /** A failed star (plan §6.6, T1-T4, T6): DARK, the gauges zeroed; in-flight output is lost. */
    private void fail(String reason) {
        final int n = star == null ? 0 : star.n();
        lastEvent = (star == null ? "star" : star.name) + " LOST: "
            + reason
            + (n == 4
                ? " (collapsed to a black hole: with the Neutron Star chart still in the slot the Eye relights Arcturus"
                    + " from a Stellar Ignition Charge and re-runs the Core Collapse, no core needed)"
                : n == 6 ? " (field decoherence: a re-merge needs the merger charge, no core)"
                    : n <= 3 ? " (relighting needs a new Stellar Ignition Charge)" : "");
        if (ledger != null) ledger.fails++;
        recordFailure(lastEvent);
        goDark();
    }

    /** Puts a live star out (target change, wrench, soft mallet, structure break). The core stays in escrow. */
    public void extinguish(String reason) {
        if (!live()) return;
        final StringBuilder sb = new StringBuilder(star == null ? "star" : star.name).append(" put out: ")
            .append(reason);
        if (trans != null) {
            sb.append("; the ")
                .append(trans.type.title)
                .append(" was ABORTED (its materials are lost")
                .append(escrow != null ? ", the core stays in escrow)" : ")");
        }
        final boolean batch = state == StarState.BATCH && fs != null && fs.batch != null;
        if (batch) {
            sb.append("; ")
                .append(fmt(salvage()))
                .append(" L of Black Dwarf Matter salvaged")
                .append(salvageOwed ? " (owed: paid out once the Eye is formed again)" : "");
        }
        lastEvent = sb.toString();
        // A plain target change (no transition, no batch) is the player's own choice, not a failure: it must not
        // replace a more useful "Last failure" line (B-59).
        final boolean failure = trans != null || batch
            || !(reason.startsWith("the target changed") || reason.startsWith("a planet was selected"));
        if (failure) recordFailure(lastEvent);
        else logEvent(lastEvent);
        goDark();
    }

    /**
     * The persistent "Last failure" line (GUI, scanner; tile NBT). Only failures write it, so the auto-start that
     * follows one (a T1-T3 relight, the next T5 batch, a re-merge) changes the event line but never this one. Every
     * failure is logged, as a completion is.
     */
    private void recordFailure(String line) {
        lastFailure = line;
        logEvent(line);
    }

    private void logEvent(String line) {
        com.nateyburr.beyondthestars.BeyondTheStars.LOG.info("[BTS] Eye {}", line);
    }

    private void goDark() {
        state = StarState.DARK;
        waitReason = "";
        trans = null;
        if (fs != null) {
            StellarField.resetLost(fs);
            fs.batch = null;
        }
        cycR = 1;
        cycP = batchP = 0;
        prodCarry = 0;
        hsmOwed = 0;
        flashState = 0;
        dwarfReserve = 0;
        lastOut = null;
        publish(FieldReadout.DARK);
        host.markDirty();
    }

    // ============================================================================ transitions

    /**
     * What still blocks the transition that leaves tier {@code from} (plan §7.2-§7.6; empty = it may begin). The
     * milestones and the core are needed only the first time (the tier above is not unlocked yet).
     */
    public List<String> missing(int from) {
        final List<String> out = new ArrayList<>();
        final EyeTransition.Type type = EyeTransition.Type.leaving(from);
        if (type == null) return out;
        final boolean firstTime = unlockedTier <= from;
        if (unlockedTier < from) out.add("unlock T" + from + " first");
        if (firstTime) {
            if (!milestones.met(from)) out.add(milestones.progressLine(from));
            final Item core = coreFor(from);
            if (escrow == null || escrow.getItem() != core) {
                out.add(
                    "the " + new ItemStack(core).getDisplayName() + " in escrow (right-click the controller with it)");
            }
        }
        if (host.structureTier() < from + 1) out.add("the " + bandFor(from + 1) + " built (bands are cumulative)");
        if (host.compressionTier() < from + 1) {
            out.add("every Spacetime Compression Field Generator at tier " + (from + 1) + " or higher");
        }
        if (from <= 3) {
            if (star == null || star.n() != from || !burning()) {
                out.add("the T" + from + " star lit");
            } else if (lastOut == null || lastOut.zone > 1) {
                out.add("the star Stable or better");
            }
        }
        for (Map.Entry<Fluid, Long> need : transitionNeeds(type).entrySet()) {
            needBuffer(out, need.getKey(), need.getValue());
        }
        return out;
    }

    /**
     * The buffered litres a transition needs to begin (plan §7.2-§7.6): its event fluids (Mass Infusion RSPM, Shell
     * Ignition ESPM, the Core Collapse's HSM seed, the First Cooling's WDM charge, the merger charge), PLUS the first
     * cycle of the star it makes (its upkeep × its parallel, and for the neutron star one cycle of the HSM feed at the
     * setpoint). The event takes its fluids first and the new star is charged straight after, so without that reserve
     * an exact buffer left the new star at r = 0 during the settle or imprint that decides the transition (D-49
     * review). The Core Collapse's SpaceTime is not an event fluid (the bounce absorbs what is there), but the
     * neutron star's 144 L × p is reserved from it ({@link #collapseSpaceTimeReserve}).
     */
    public Map<Fluid, Long> transitionNeeds(EyeTransition.Type type) {
        return transitionNeeds(type, destParallel(type.from + 1), feedSet);
    }

    /**
     * {@link #transitionNeeds(EyeTransition.Type)} for a new star at parallel {@code pd} and an HSM feed setpoint
     * {@code feed} (the NEI transition page uses p = 1 and the default feed).
     */
    public static Map<Fluid, Long> transitionNeeds(EyeTransition.Type type, int pd, double feed) {
        final Map<Fluid, Long> need = new LinkedHashMap<>();
        final StarDef dest = StarDef.byKey(type.dest);
        switch (type) {
            case MASS_INFUSION:
                need.merge(rspmFluid(), MassInfusion.RSPM, Long::sum);
                break;
            case SHELL_IGNITION:
                need.merge(SLFluids.excitedStellarPlasma, ShellIgnition.ESPM, Long::sum);
                break;
            case CORE_COLLAPSE:
                need.merge(SLFluids.hyperdenseStellarMixture, (long) CollapseOverlay.HSM_SEED, Long::sum);
                break;
            case FIRST_COOLING:
                need.merge(wdmFluid(), BATCH_WDM, Long::sum);
                break;
            default:
                need.merge(wdmFluid(), BATCH_WDM * pd, Long::sum);
                need.merge(bdmFluid(), MERGER_BDM * pd, Long::sum);
                need.merge(SLFluids.antipolarizationAgent, MERGER_AGENT * pd, Long::sum);
                break;
        }
        if (dest != null) {
            for (EyeRecipe.FluidAmount up : dest.upkeep) need.merge(up.fluid, up.amount * pd, Long::sum);
            if (dest.feed != null) {
                need.merge(dest.feed, (long) Math.ceil(feed * StellarField.CYCLE_SECONDS * pd), Long::sum);
            }
        }
        return need;
    }

    /** The neutron star's first cycle of SpaceTime, held back from what the Core Collapse's bounce may absorb. */
    long collapseSpaceTimeReserve() {
        final StarDef ns = StarDef.ofTier(4);
        if (ns == null) return 0;
        final Fluid st = spaceTimeFluid();
        long litres = 0;
        for (EyeRecipe.FluidAmount up : ns.upkeep) if (up.fluid == st) litres += up.amount;
        return litres * destParallel(4);
    }

    /** The SpaceTime the bounce may absorb: the buffer minus the neutron star's first cycle. */
    public long collapseSpaceTimeAvailable() {
        return Math.max(0, host.buffer(spaceTimeFluid()) - collapseSpaceTimeReserve());
    }

    private void needBuffer(List<String> out, Fluid fluid, long litres) {
        if (fluid == null) {
            out.add("a fluid this pack does not have");
            return;
        }
        if (host.buffer(fluid) < litres) {
            out.add(fmt(litres) + " L of " + fluidName(fluid) + " buffered (" + fmt(host.buffer(fluid)) + " L now)");
        }
    }

    private static String bandFor(int tier) {
        switch (tier) {
            case 2:
                return "beam band";
            case 3:
                return "jet crescent";
            case 4:
                return "spiral arms";
            case 5:
                return "second arms";
            default:
                return "galactic rim";
        }
    }

    /** PENDING: begins the Mass Infusion, Shell Ignition or Core Collapse the moment every precondition holds. */
    private void tryBeginFromPending() {
        final List<String> miss = missing(star.n());
        if (!miss.isEmpty()) {
            waitReason = miss.get(0);
            return;
        }
        final EyeTransition.Type type = EyeTransition.Type.leaving(star.n());
        final EyeTransition tr = beginTransition(type);
        if (type == EyeTransition.Type.MASS_INFUSION) host.takeBuffer(rspmFluid(), MassInfusion.RSPM);
        if (type == EyeTransition.Type.SHELL_IGNITION) {
            host.takeBuffer(SLFluids.excitedStellarPlasma, ShellIgnition.ESPM);
        }
        tr.onBegin(fs);
        state = StarState.LIT;
        lastEvent = type.title + " begun"
            + (tr.coreUsed ? " (the " + escrow.getDisplayName() + " is consumed only if it succeeds)"
                : " (re-run, no core)");
    }

    /**
     * The one entry point (plan §7.1): the transition object, its begin clock, and whether the escrowed core is
     * consumed on success (first time only). The caller takes the event materials and sets the field up.
     */
    private EyeTransition beginTransition(EyeTransition.Type type) {
        trans = EyeTransition.create(type, bounceShock);
        trans.beginT = fs != null ? fs.t : 0;
        trans.coreUsed = unlockedTier <= type.from && escrow != null && escrow.getItem() == coreFor(type.from);
        waitReason = "";
        if (ledger != null && type == EyeTransition.Type.CORE_COLLAPSE) {
            ledger.collapseBeginT = trans.beginT;
            ledger.collapseShock = bounceShock;
            ledger.collapseStBuf = collapseSpaceTimeAvailable();
        }
        host.markDirty();
        return trans;
    }

    /** Success (plan §7.1): N+1 unlocked for good, the core consumed if one was used. */
    private void completeTransition() {
        final EyeTransition tr = trans;
        if (tr == null) return;
        final int to = tr.type.from + 1;
        unlockedTier = Math.max(unlockedTier, to);
        String coreNote = "";
        if (tr.coreUsed && escrow != null) {
            coreNote = "; the " + escrow.getDisplayName() + " was consumed";
            escrow = null;
        }
        lastEvent = tr.type.title + " COMPLETE in " + tr.t + " s: T" + to + " unlocked" + coreNote;
        com.nateyburr.beyondthestars.BeyondTheStars.LOG.info("[BTS] Eye {}", lastEvent);
        trans = null;
        host.markDirty();
    }

    /** A transition failed (plan §7): the star is lost, the event materials too; the core stays in escrow. */
    private void failTransition(String reason) {
        final EyeTransition tr = trans;
        lastEvent = (tr == null ? "transition" : tr.type.title) + " FAILED: "
            + reason
            + (escrow != null ? "; the " + escrow.getDisplayName() + " stays in escrow" : "");
        if (ledger != null) ledger.fails++;
        recordFailure(lastEvent);
        goDark();
    }

    // ============================================================================== T5 cooling

    /** A Sirius B chart on a dark Eye: the cooling batches (T5 unlocked), or the First Cooling (T4 unlocked). */
    private void tryStartCooling(StarDef want) {
        star = want; // IDLE keeps Sirius B selected (DARK when the chart goes)
        if (unlockedTier >= 5) {
            if (parallel(5) < 1) {
                waitReason = "the Eye does not reach T5 (bands, compression)";
                return;
            }
            if (!startCooling(want)) idle(
                "Sirius B waits for a White Dwarf Matter charge (" + fmt(BATCH_WDM * parallel(5)) + " L buffered)");
            return;
        }
        if (unlockedTier < 4) {
            waitReason = "Sirius B needs T5: unlock T4 first (the Core Collapse)";
            return;
        }
        final List<String> miss = missing(4);
        if (!miss.isEmpty()) {
            idle("First Cooling waits: " + miss.get(0));
            return;
        }
        final EyeTransition tr = beginTransition(EyeTransition.Type.FIRST_COOLING);
        if (!startCooling(want)) {
            trans = null;
            idle("First Cooling waits for a White Dwarf Matter charge");
            return;
        }
        tr.onBegin(fs);
        lastEvent = "First Cooling begun: the first white dwarf assembled at Θ +90"
            + (tr.coreUsed ? " (the " + escrow.getDisplayName() + " is consumed only if it succeeds)" : "");
    }

    /** A fresh Sirius B: takes a batch charge, lights the field (I 100, a fresh cycle) and starts the batch. */
    private boolean startCooling(StarDef def) {
        final int p = Math.max(1, parallel(5));
        final Fluid wdm = wdmFluid();
        if (wdm == null || host.buffer(wdm) < BATCH_WDM * p) return false;
        host.takeBuffer(wdm, BATCH_WDM * p);
        batchP = p;
        ensureField(def);
        star = def;
        StellarField.startBatch(fs);
        light();
        state = StarState.BATCH;
        waitReason = "";
        return true;
    }

    /** The next batch of a running Sirius B (T5 unlocked): a new charge, or IDLE. */
    private void nextBatch() {
        if (unlockedTier < 5) {
            idle("the First Cooling needs another attempt (a new White Dwarf Matter charge starts it)");
            return;
        }
        final int p = Math.max(1, parallel(5));
        final Fluid wdm = wdmFluid();
        if (wdm == null || host.buffer(wdm) < BATCH_WDM * p) {
            idle("Sirius B waits for a White Dwarf Matter charge (" + fmt(BATCH_WDM * p) + " L)");
            return;
        }
        host.takeBuffer(wdm, BATCH_WDM * p);
        batchP = p;
        StellarField.startBatch(fs);
        state = StarState.BATCH;
    }

    private void idle(String reason) {
        state = StarState.IDLE;
        waitReason = reason;
        if (fs != null) fs.batch = null;
        host.markDirty();
    }

    /** A batch completed: its BDM, the T5 → T6 milestone, the First Cooling's verdict, then the next batch. */
    private void batchDone(StepResult out) {
        emit(out.batchProduct);
        if (ledger != null) {
            ledger.product += out.batchProduct;
            ledger.batches++;
        }
        final boolean good = out.batchMeanY >= FirstCooling.MEAN_Y;
        // T5 -> T6 counts from the moment T5 unlocks (plan §7.1): the First Cooling batch itself does not count
        if (good && unlockedTier >= 5) milestones.goodBatches++;
        String note = String.format(
            Locale.ROOT,
            "batch cooled to a black dwarf: %,.0f L Black Dwarf Matter (mean Y %.3f)",
            out.batchProduct,
            out.batchMeanY);
        boolean failed = false;
        if (trans instanceof FirstCooling) {
            if (good) {
                completeTransition();
                note += "; " + lastEvent;
            } else {
                note += "; the First Cooling FAILED (mean Y below " + FirstCooling.MEAN_Y
                    + ")"
                    + (escrow != null ? ", the core stays in escrow" : "");
                trans = null;
                failed = true;
            }
        }
        lastEvent = note;
        if (failed) recordFailure(note);
        nextBatch();
    }

    /** T5 fracture (plan §6.6): 25 % × the fraction done is salvaged; I 100, lock 0; the next batch (or IDLE). */
    private void fracture(String reason) {
        final long salv = salvage();
        fs.I = 100;
        fs.lock = 0;
        if (ledger != null) ledger.fails++;
        final StringBuilder sb = new StringBuilder("FRACTURE (").append(reason)
            .append("): ")
            .append(fmt(salv))
            .append(" L of Black Dwarf Matter salvaged, the rest of the charge lost");
        if (trans instanceof FirstCooling) {
            sb.append("; the First Cooling FAILED")
                .append(escrow != null ? ", the core stays in escrow" : "");
            trans = null;
        }
        lastEvent = sb.toString();
        recordFailure(lastEvent);
        nextBatch();
    }

    /**
     * The fracture salvage of the batch in progress: 25 % of the batch its charge paid for ({@link #batchP}) × the
     * fraction done. While formed it goes out through the output hatches. While unformed GT has already cleared the
     * hatch lists (and would void the fluid), so it is OWED instead: kept with the other owed outputs
     * ({@link #legacyPay}, item NBT, survives a wrench) and paid the next second the Eye is formed. Returns the whole
     * litres; {@link #salvageOwed} says which way they went.
     */
    private long salvage() {
        salvageOwed = false;
        if (fs == null || fs.batch == null || star == null) return 0;
        final int p = batchP > 0 ? batchP : Math.max(1, parallel(5));
        final double litres = star.productBase() * p
            * FRACTURE_SALVAGE
            * Math.min(1, fs.batch.eyeT / StellarField.BATCH_SECONDS);
        if (ledger != null) ledger.product += litres;
        final double before = prodCarry;
        if (!host.formed()) {
            final long whole = (long) Math.floor(before + litres);
            prodCarry = before + litres - whole;
            if (whole > 0 && star.product != null) {
                legacyPay.merge(star.product.getName(), whole, Long::sum);
                salvageOwed = true;
            }
            return whole;
        }
        emit(litres);
        return (long) Math.floor(before + litres);
    }

    // ============================================================================== T6 magnetar

    /** A Magnetar chart on a dark Eye: the Magnetar Merger (the first spawn, and every re-spawn). */
    private void tryMerger(StarDef want) {
        if (unlockedTier < 5) {
            waitReason = "the magnetar needs T6: unlock T5 first (the First Cooling)";
            return;
        }
        final List<String> miss = missing(5);
        if (!miss.isEmpty()) {
            waitReason = "Magnetar Merger waits: " + miss.get(0);
            return;
        }
        final EyeTransition tr = beginTransition(EyeTransition.Type.MAGNETAR_MERGER);
        final int p = Math.max(1, parallel(6));
        host.takeBuffer(wdmFluid(), BATCH_WDM * p);
        host.takeBuffer(bdmFluid(), MERGER_BDM * p);
        host.takeBuffer(SLFluids.antipolarizationAgent, MERGER_AGENT * p);
        creditAgent(MERGER_AGENT * p);
        ensureField(want);
        star = want;
        fs.setX(0, 0, 0, 0);
        dwarfReserve = MERGER_RESERVE_CYCLES;
        light();
        tr.onBegin(fs);
        lastEvent = "Magnetar Merger begun: the merger charge taken, the dynamo winds Π up"
            + (tr.coreUsed ? " (the " + escrow.getDisplayName() + " is consumed only if it succeeds)" : " (re-spawn)");
    }

    // ============================================================================ debug (bench)

    /**
     * Debug {@code lit}: lights {@code def} at once (plan §6.3 "lit skips all of this"): T1-T4 without a charge,
     * seed or ignition EU; T5 a batch without its charge; T6 the magnetar without the merger. The first cycle is
     * charged normally. Starts the bench ledger. Returns an error or null.
     */
    public String debugLight(StarDef def) {
        if (def == null) return "no star chart in the controller slot (or name one)";
        if (def.n() > host.installedTier()) return "the Eye does not reach T" + def.n();
        if (live()) goDark();
        trans = null;
        batchP = 0; // no charge taken: only the cycle's paid parallel caps it
        ensureField(def);
        star = def;
        if (def.tier.batch) StellarField.startBatch(fs);
        if (def.tier.shield) {
            fs.setX(0, 0, 0, 0);
            dwarfReserve = MERGER_RESERVE_CYCLES;
        }
        light();
        if (def.tier.batch) state = StarState.BATCH;
        lastEvent = def.name + " lit (debug)";
        ledger = new Ledger(seed, def.n());
        return null;
    }

    /** Debug {@code seed n}: a fresh field (clock 0, gauges 0) with that noise seed; a burning star relights. */
    public void debugSeed(long newSeed) {
        final TierParams t = fs != null ? fs.tier : star != null ? star.tier : TierParams.T1;
        seed = newSeed;
        fs = new FieldState(t, new FieldRng(newSeed));
        if (burning()) {
            trans = null;
            if (t.batch) StellarField.startBatch(fs);
            light();
            if (t.batch) state = StarState.BATCH;
            ledger = new Ledger(seed, t.n);
        } else if (state == StarState.IGNITING) {
            goDark();
        }
        host.markDirty();
    }

    /** Debug {@code event}: the tier's next event comes now (its warning this second). Returns an error or null. */
    public String debugEvent() {
        if (fs == null || !burning()) return "no star burns";
        if (fs.tier.event == null) return "T" + fs.tier.n + " has no event";
        if (fs.ev == null) fs.ev = new FieldState.EventState(fs.t);
        fs.ev.next = (double) fs.t + fs.tier.event.warn;
        fs.ev.warned = false;
        fs.ev.left = 0;
        host.markDirty();
        return null;
    }

    /** Debug {@code transition abort}: as a wrench would. */
    public void debugAbort() {
        if (trans != null) extinguish("aborted on the bench");
    }

    // ============================================================================ automation

    /**
     * AUTOMATION PASS: the Directive Hatch code of an Eye output set (docs/automation.md; {@link DirectiveSet}), from
     * the last solved second (the 1 s sensor lag the prototype's rigs see). {@code w} is the hatch's window: the
     * gauge band ±w of the Trims set.
     */
    public int directiveCode(DirectiveSet set, double w) {
        switch (set) {
            case EYE_TRIMS:
                return burning() && fs != null ? EyeTrimRule.trimBits(trimTable(), fs.prevE, fs.prevWarn, w) : 0;
            case EYE_STATUS: {
                int c = 0;
                if (buffersShort()) c |= 1;
                if (chargeWanted()) c |= 2;
                if (extraWanted(w)) c |= 4;
                if (alarm()) c |= 8;
                return c;
            }
            case EYE_TRANSITION: {
                int c = 0;
                if (transitionReady()) c |= 1;
                if (trans != null || state == StarState.PENDING) c |= 2;
                if (coreWanted()) c |= 4;
                if ((state == StarState.DARK || state == StarState.IDLE) && !waitReason.isEmpty()) c |= 8;
                return c;
            }
            default:
                return 0;
        }
    }

    /** The Trims set's bit names on the star burning now (its trim table), else the set's defaults. */
    public String directiveBitLabel(DirectiveSet set, int i) {
        if (set == DirectiveSet.EYE_TRIMS && fs != null && star != null) {
            return EyeTrimRule.label(trimTable(), i);
        }
        if (set == DirectiveSet.EYE_STATUS && i == 2 && fs != null && star != null) {
            final TierParams t = trimTable();
            if (t.trims()
                .size() > 4) {
                return t.trims()
                    .get(4).kind.displayName + " wanted / event warned";
            }
            if (t.n == 3) return "Helium flash warned (more Super Coolant)";
        }
        return set.bit(i);
    }

    /** The trim table in force: a transition's own (Shell Ignition drains by T3's), else the star's. */
    private TierParams trimTable() {
        return trans != null ? trans.trimTable(fs.tier) : fs.tier;
    }

    /**
     * Status bit 1: the buffers cannot pay the next cycle's upkeep (or this cycle ran short, r below 1), a dark Eye's
     * ignition (10 cycles and the seed cloud), a waiting Sirius B batch or the magnetar's dwarf-matter top-up.
     */
    public boolean buffersShort() {
        if (live() && star != null) {
            if (cycR < 1 - 1e-9) return true;
            final int p = Math.max(1, cycP > 0 ? cycP : parallel(star.n()));
            for (EyeRecipe.FluidAmount up : star.upkeep) if (host.buffer(up.fluid) < up.amount * p) return true;
            if (star.feed != null
                && host.buffer(star.feed) < (long) Math.ceil(feedSet * StellarField.CYCLE_SECONDS * p)) return true;
            if (star.tier.shield && dwarfReserve <= TOPUP_AT) {
                final Fluid wdm = wdmFluid(), bdm = bdmFluid();
                if (wdm != null && host.buffer(wdm) < TOPUP_WDM * p) return true;
                if (bdm != null && host.buffer(bdm) < TOPUP_BDM * p) return true;
            }
            return false;
        }
        final StarDef want = StarDef.byChart(host.target());
        if (want == null) return false;
        if (want.n() == 5) {
            final Fluid wdm = wdmFluid();
            return wdm != null && host.buffer(wdm) < BATCH_WDM * Math.max(1, parallel(5));
        }
        final StarDef def = ignitable(want);
        if (def == null || def.n() > host.installedTier()) return false;
        for (Map.Entry<Fluid, Long> need : ignitionNeeds(def, Math.max(1, parallel(def.n()))).entrySet()) {
            if (host.buffer(need.getKey()) < need.getValue()) return true;
        }
        return false;
    }

    /** The star a dark Eye lights from a Stellar Ignition Charge for this chart (T4 relights Arcturus), or null. */
    private StarDef ignitable(StarDef want) {
        if (want == null) return null;
        if (want.tier.ignite) return want;
        if (want.n() == 4 && unlockedTier >= 4) {
            final StarDef progenitor = StarDef.ofTier(3);
            return progenitor != null && progenitor.tier.ignite ? progenitor : null;
        }
        return null;
    }

    /** Status bit 2: a dark Eye would light its chart's star now but no Stellar Ignition Charge is in a plinth bus. */
    public boolean chargeWanted() {
        if (state != StarState.DARK) return false;
        final StarDef def = ignitable(StarDef.byChart(host.target()));
        return def != null && def.n() <= host.installedTier() && !host.takeIgnitionCharge(true);
    }

    /** Status bit 3: the fifth trim is wanted (T6: the Agent; merger thresholds during the merger), else a warning. */
    public boolean extraWanted(double w) {
        if (!burning() || fs == null) return false;
        if (trans instanceof MagnetarMerger) {
            final MagnetarMerger mm = (MagnetarMerger) trans;
            final double e = fs.prevE[StellarField.PI];
            return mm.phase == 1 ? e > 75 : e > 30;
        }
        return EyeTrimRule.extra(trimTable(), fs.prevE, fs.prevWarn, w);
    }

    /** Status bit 4: the Eye is not formed, or the burning star is Critical or worse, or its Integrity is below 50. */
    public boolean alarm() {
        if (!host.formed()) return true;
        if (!burning() || fs == null) return false;
        return (lastOut != null && lastOut.zone >= 3) || fs.I < 50;
    }

    /**
     * Transition bit 1: everything the next transition needs is in place but the chart; loading the next tier's chart
     * (into the controller slot, or a plinth input bus) begins it.
     */
    public boolean transitionReady() {
        if (trans != null || state == StarState.PENDING) return false;
        final int from = star != null && live() ? star.n() : unlockedTier;
        if (EyeTransition.Type.leaving(from) == null) return false;
        final StarDef want = StarDef.byChart(host.target());
        if (want != null && want.n() == from + 1) return false;
        return missing(from).isEmpty();
    }

    /** Transition bit 3: this tier's milestones are met and the next core is not in escrow yet. */
    public boolean coreWanted() {
        final Item need = coreFor(unlockedTier);
        if (need == null || !milestones.met(unlockedTier)) return false;
        return escrow == null || escrow.getItem() != need;
    }

    /** AUTOMATION: the last-event line (the plinth-bus chart and core pickups). */
    public void noteEvent(String line) {
        lastEvent = line;
        host.markDirty();
    }

    // ================================================================================ escrow

    /** The core for step n → n+1 (index n), or null. */
    public static Item coreFor(int n) {
        switch (n) {
            case 1:
                return SLItems.stellarMassCore;
            case 2:
                return SLItems.shellBurningCore;
            case 3:
                return SLItems.galacticUpgradeCore;
            case 4:
                return SLItems.subspaceCoolingCore;
            case 5:
                return SLItems.magnetarSeedCore;
            default:
                return null;
        }
    }

    /** The transition event for step n → n+1. */
    public static String eventFor(int n) {
        final EyeTransition.Type t = EyeTransition.Type.leaving(n);
        return t == null ? "" : t.title;
    }

    /** True if {@code item} is one of the five transition cores. */
    public static boolean isCore(Item item) {
        if (item == null) return false;
        for (int n = 1; n <= 5; n++) if (coreFor(n) == item) return true;
        return false;
    }

    /**
     * Right-click with a transition core (plan §8): the core for {@code unlockedTier → unlockedTier+1} goes into an
     * empty escrow (one item); any other core is refused, naming the one needed. Returns the chat line; sets
     * {@code taken[0]} when one item was taken from the stack.
     */
    public String offerCore(ItemStack held, boolean[] taken) {
        taken[0] = false;
        final Item need = coreFor(unlockedTier);
        if (need == null) return "This Eye is fully unlocked (T" + unlockedTier + "): it needs no more cores.";
        final String needName = new ItemStack(need).getDisplayName();
        if (held == null || held.getItem() != need) {
            return "This Eye is unlocked to T" + unlockedTier
                + ": the next step needs the "
                + needName
                + " ("
                + eventFor(unlockedTier)
                + ", T"
                + unlockedTier
                + " -> T"
                + (unlockedTier + 1)
                + ").";
        }
        if (escrow != null) return "The escrow already holds a " + escrow.getDisplayName() + ".";
        escrow = held.copy();
        escrow.stackSize = 1;
        taken[0] = true;
        host.markDirty();
        return needName + " held in escrow. It is consumed only when the "
            + eventFor(unlockedTier)
            + " succeeds; until then sneak-punch the controller with an empty hand to take it back. "
            + milestones.progressLine(unlockedTier);
    }

    /** Sneak-punch with an empty hand: returns the escrowed core, or null (none, or a transition is running). */
    public ItemStack takeEscrow() {
        if (trans != null) return null;
        final ItemStack out = escrow;
        escrow = null;
        if (out != null) host.markDirty();
        return out;
    }

    // ================================================================================ migration

    /**
     * Plan §13: at each successful walk of a pre-D-49 Eye, the bands it had built unlock their tiers under the OLD
     * rule (the jet crescent counted without the beam band): {@code max(1, min(3, legacyStructureTier))}, never
     * lowering. {@code settle} (the walk saw the whole T1-T3 footprint loaded) ends the migration; until then a later
     * walk with more chunks loaded can still raise it (D-49 review: a first walk across unloaded band chunks used to
     * lock an old T3 Eye at T1 for good).
     */
    public void grandfather(int legacyStructureTier, boolean settle) {
        if (!legacyPending) return;
        final int before = unlockedTier;
        unlockedTier = Math.max(unlockedTier, Math.max(1, Math.min(3, legacyStructureTier)));
        if (settle) legacyPending = false;
        if (unlockedTier != before || settle) {
            lastEvent = "migrated from 0.24.x: unlocked T" + unlockedTier
                + " (bands grandfathered"
                + (settle ? ")" : "; checked again once the whole footprint is loaded)");
            logEvent(lastEvent);
            host.markDirty();
        }
    }

    /**
     * Pays the owed outputs once the Eye stands with its output hatches: a pre-D-49 star run in flight (plan §13 item
     * 3) and a T5 salvage made while the Eye was unformed ({@link #salvage()}).
     */
    private void payLegacy() {
        if (legacyPay.isEmpty() || !host.formed()) return;
        final StringBuilder sb = new StringBuilder("credited ");
        for (Map.Entry<String, Long> e : legacyPay.entrySet()) {
            final Fluid fluid = net.minecraftforge.fluids.FluidRegistry.getFluid(e.getKey());
            if (fluid == null || e.getValue() <= 0) continue;
            host.output(fluid, e.getValue());
            sb.append(fmt(e.getValue()))
                .append(" L ")
                .append(fluidName(fluid))
                .append(' ');
        }
        legacyPay.clear();
        lastEvent = sb.append("owed (a pre-D-49 run in flight, or a salvage made while unformed)")
            .toString();
        com.nateyburr.beyondthestars.BeyondTheStars.LOG.info("[BTS] Eye {}", lastEvent);
        host.markDirty();
    }

    /** The owed outputs, e.g. "960 L Black Dwarf Matter". */
    public String owedLine() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : legacyPay.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(fmt(e.getValue()))
                .append(" L ")
                .append(fluidName(net.minecraftforge.fluids.FluidRegistry.getFluid(e.getKey())));
        }
        return sb.toString();
    }

    // ================================================================================== fluids

    /** Tachyon Rich Temporal Fluid: molten {@code Materials.Time} on RC-2 (B-35 item 6). */
    public static Fluid timeFluid() {
        return fluidOf(Materials.Time.getMolten(1L));
    }

    public static Fluid spaceTimeFluid() {
        return fluidOf(Materials.SpaceTime.getMolten(1L));
    }

    public static Fluid rspmFluid() {
        return fluidOf(Materials.RawStarMatter.getFluid(1L));
    }

    public static Fluid wdmFluid() {
        return fluidOf(Materials.WhiteDwarfMatter.getMolten(1L));
    }

    public static Fluid bdmFluid() {
        return fluidOf(Materials.BlackDwarfMatter.getMolten(1L));
    }

    /** The real fluid of a trim kind (plan §5), or null if the pack lacks it. */
    public static Fluid trimFluid(TrimDef.Kind kind) {
        switch (kind) {
            case HELIUM:
                return fluidOf(Materials.Helium.getGas(1L));
            case HYDROGEN:
                return fluidOf(Materials.Hydrogen.getGas(1L));
            case SUPER_COOLANT:
                return fluidOf(Materials.SuperCoolant.getFluid(1L));
            case SPACE:
                return fluidOf(Materials.Space.getMolten(1L));
            case SPACETIME:
                return spaceTimeFluid();
            case TENGAM:
                return fluidOf(Materials.TengamPurified.getMolten(1L));
            case DSSF:
                return fluidOf(Materials.DimensionallyShiftedSuperfluid.getFluid(1L));
            case RAW_STAR_MATTER:
                return rspmFluid();
            case AGENT:
                return SLFluids.antipolarizationAgent;
            default:
                return null;
        }
    }

    private static Fluid fluidOf(FluidStack stack) {
        return stack == null ? null : stack.getFluid();
    }

    static String fluidName(Fluid fluid) {
        return fluid == null ? "?" : fluid.getLocalizedName(new FluidStack(fluid, 1));
    }

    static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    static String sci(BigInteger v) {
        return v.bitLength() < 20 ? v.toString() : String.format(Locale.ROOT, "%.2e", v.doubleValue());
    }

    static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    // ================================================================================== scanner

    /** The star's scanner lines (plan §12). Server state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add(
            "Unlocked T" + unlockedTier
                + (legacyPending
                    ? " (migration: the bands are grandfathered once a walk sees the whole T1-T3" + " footprint loaded)"
                    : "")
                + "; escrow: "
                + (escrow == null ? "empty" : escrow.getDisplayName())
                + (coreFor(unlockedTier) != null ? " (next: " + new ItemStack(coreFor(unlockedTier)).getDisplayName()
                    + ", "
                    + eventFor(unlockedTier)
                    + ")" : ""));
        if (unlockedTier <= 5) out.add(milestones.progressLine(unlockedTier));
        if (state == StarState.DARK || star == null) {
            out.add("Star: DARK" + (waitReason.isEmpty() ? "" : " - waiting: " + waitReason));
        } else if (state == StarState.IDLE) {
            out.add("Star: " + star.name + " IDLE" + (waitReason.isEmpty() ? "" : " - " + waitReason));
        } else if (state == StarState.IGNITING) {
            out.add(
                "Star: " + star.name
                    + " IGNITING, P "
                    + StellarField.fmtSigned(fs.x[StellarField.P])
                    + " (lights at "
                    + StellarField.fmtSigned(StellarField.IGNITION_LIT)
                    + ", +1.5/s)");
        } else {
            litLines(out);
        }
        if (!lastEvent.isEmpty()) out.add("Last event: " + lastEvent);
        if (!legacyPay.isEmpty()) out.add("Owed output (paid once the Eye is formed): " + owedLine());
        return out;
    }

    private void litLines(List<String> out) {
        final TierParams T = fs.tier;
        final double mMax = StellarField.mMax(T, host.timeTier());
        out.add(
            "Star: " + star.name
                + " (T"
                + T.n
                + ") "
                + (trans != null ? "TRANSITION" : state.toString())
                + ", parallel "
                + lastP
                + (lastLiveP > lastP ? " (the structure gives " + lastLiveP + " from the next charge)" : "")
                + ", dial m "
                + f2(lastM)
                + (trans != null ? " (held at 1 by the transition)"
                    : " (set " + f2(dialSet)
                        + ", max "
                        + f2(mMax)
                        + (lastTimePaid > 0 ? ", " + fmt(lastTimePaid) + " L Time/s" : "")
                        + ")")
                + ", q "
                + f2(StepInputs.qFor(host.stabilisationTier())));
        if (trans != null) transitionLines(out);
        else if (state == StarState.PENDING) {
            final List<String> miss = missing(T.n);
            out.add(
                "PENDING: the " + eventFor(T.n)
                    + (miss.isEmpty() ? " begins now" : " waits for " + miss.size() + " thing(s):"));
            for (String m : miss) out.add("  - " + m);
        }
        if (lastOut != null) {
            final StringBuilder g = new StringBuilder("Field: Stability ").append(Math.round(lastOut.stability))
                .append(" (")
                .append(StellarField.ZONE_NAME[lastOut.zone])
                .append("), Y ")
                .append(f2(lastOut.Y))
                .append(", Lock ")
                .append(fs.lock)
                .append('/')
                .append(StellarField.LOCK_SECONDS)
                .append(", Integrity ")
                .append(Math.round(fs.I));
            final int[] gauges = trans != null ? trans.gauges(T) : T.gauges();
            for (int k : gauges) {
                g.append("; ")
                    .append(StellarField.GAUGE_SYM[k])
                    .append(' ')
                    .append(StellarField.fmtSigned(fs.x[k]));
            }
            if (T.shield) g.append("; shield A ")
                .append(Math.round(fs.A));
            out.add(g.toString());
            if (lastOut.warn != 0) {
                final String name = T.batch ? "crystallisation plateau"
                    : trans instanceof ShellIgnition ? "forced helium flash" : T.event != null ? T.event.name : "event";
                final boolean signed = T.event != null && T.event.signed && !T.batch;
                out.add("Event warning: " + name + (signed ? " (" + (lastOut.warn > 0 ? "+" : "-") + ")" : ""));
            }
        }
        if (T.batch && fs.batch != null) {
            final double sp = StellarField.spAt(fs.batch.eyeT);
            out.add(
                String.format(
                    Locale.ROOT,
                    "Batch: %.0f / 7,200 Eye-s, setpoint %s, Θ error %s, mean Y so far %.3f",
                    fs.batch.eyeT,
                    StellarField.fmtSigned(sp),
                    StellarField.fmtSigned(fs.x[StellarField.TH] - sp),
                    fs.batch.n > 0 ? fs.batch.ySum / fs.batch.n : 0));
        }
        final double nextIn = Math.max(0, (StellarField.CYCLE_SECONDS - fs.cyc.eyeT) / Math.max(1e-9, lastM));
        out.add(
            "Upkeep r " + f2(cycR)
                + (cycR < 1 ? " (SHORT: P sags, output x r)" : "")
                + ", next cycle in "
                + Math.round(nextIn)
                + " s, "
                + sci(
                    BigInteger.valueOf(star.euPerCycle)
                        .multiply(BigInteger.valueOf(Math.max(1, lastP))))
                + " EU/cycle");
        if (T.feed) {
            out.add(
                "HSM feed: setpoint " + String.format(Locale.ROOT, "%.3f", feedSet)
                    + " L/s, actual "
                    + String.format(Locale.ROOT, "%.3f", lastFeed)
                    + " L/s per parallel (sneak-screwdriver steps it)");
        }
        if (T.shield) {
            out.add(
                String.format(
                    Locale.ROOT,
                    "Dwarf-matter reserve: %.0f cycles (%d top-ups; at %d or fewer the Eye tops it up with WDM %,d +"
                        + " BDM %,d L x p%s); Purified Tengam returned %,d dust",
                    dwarfReserve,
                    topUps,
                    TOPUP_AT,
                    TOPUP_WDM,
                    TOPUP_BDM,
                    dwarfReserve < 1 ? "; EMPTY: the magnetar starves, r 0" : "",
                    tengamOut));
        }
        if (!lastTrims.isEmpty()) {
            final StringBuilder t = new StringBuilder("Trims this second:");
            for (Map.Entry<TrimDef.Kind, Long> e : lastTrims.entrySet()) {
                t.append(' ')
                    .append(e.getKey().displayName)
                    .append(' ')
                    .append(fmt(e.getValue()))
                    .append(" L;");
            }
            out.add(t.toString());
        }
        if (rig != null) out.add(
            "Debug rig: " + rig.name()
                .toLowerCase(Locale.ROOT) + " (replaces the Field sockets)");
    }

    private void transitionLines(List<String> out) {
        out.add(trans instanceof FirstCooling ? ((FirstCooling) trans).line(fs) : trans.phaseLine());
        if (trans instanceof CoreCollapse) {
            final CoreCollapse cc = (CoreCollapse) trans;
            if (cc.co.phase == 1) {
                final double st = collapseSpaceTimeAvailable();
                final double[] after = cc.predictedAfterBounce(fs, st);
                out.add(
                    String.format(
                        Locale.ROOT,
                        "  SpaceTime for the bounce %,.0f L (%,d L kept for the neutron star's first cycle): predicted"
                            + " shock +%.0f (bounce %.0f), P/Θ after the bounce %s / %s%s; HSM seed %s",
                        st,
                        collapseSpaceTimeReserve(),
                        cc.co.predictedShock(st),
                        cc.co.bounceShock,
                        StellarField.fmtSigned(after[0]),
                        StellarField.fmtSigned(after[1]),
                        after[0] > 100 || after[1] > 100 ? " (SUPERNOVA)" : "",
                        host.buffer(SLFluids.hyperdenseStellarMixture) >= CollapseOverlay.HSM_SEED ? "ready"
                            : "MISSING (supernova at the bounce)"));
            }
        }
        out.add(
            "  The core is " + (trans.coreUsed ? "consumed only if it succeeds" : "not needed (re-run)")
                + "; a wrench, soft mallet, broken band or another chart aborts it");
    }

    // ================================================================================== ledger

    /**
     * The bench ledger ({@code /btseye star … sim}): the same counts and per-second hash as {@code FieldSim.runCase}
     * (zone seconds, mean Y, failures, cycles, product, and FNV-1a over x, A, I, zone, state, tier, lock), from the
     * debug {@code lit} or {@code seed} on. Saved with the star, so a restart continues it. A Core Collapse records
     * its begin second, bounce and SpaceTime buffer, so the offline compare can replay it.
     */
    public static final class Ledger {

        static final int FNV_OFFSET = 0x811c9dc5, FNV_PRIME = 16777619;

        public final long seed;
        /** The star tier the ledger started on (a collapse ends at T4). */
        public final int startTier;
        public final long[] zoneT = new long[5];
        public double ySum;
        public long yN, fails, cycles, steps, batches;
        public double product, hsm;
        public int hash = FNV_OFFSET;
        /** Core Collapse: begin second (−1 none), the bounce, the SpaceTime buffer at the begin and at the bounce. */
        public long collapseBeginT = -1;
        public double collapseShock, collapseStBuf, stAtBounce;

        Ledger(long seed, int startTier) {
            this.seed = seed;
            this.startTier = startTier;
        }

        void lit(StepResult out) {
            zoneT[out.zone]++;
            ySum += out.Y;
            yN++;
        }

        void record(EyeStarRuntime rt, int zone) {
            final FieldState s = rt.fs;
            if (s == null) return;
            final int st = rt.burning() ? 0 : rt.state == StarState.IGNITING ? 1 : 2;
            int h = hash;
            for (int k = 0; k < 4; k++) h = fnv(h, q6(s.x[k]));
            h = fnv(h, q6(s.A));
            h = fnv(h, q6(s.I));
            h = fnv(h, zone);
            h = fnv(h, st);
            h = fnv(h, s.tier.n);
            h = fnv(h, s.lock);
            hash = h;
            steps++;
        }

        static int fnv(int h, int v) {
            for (int i = 0; i < 4; i++) {
                h ^= (v >>> (8 * i)) & 255;
                h *= FNV_PRIME;
            }
            return h;
        }

        static int q6(double v) {
            return (int) Math.round(v * 1e6);
        }
    }
}
