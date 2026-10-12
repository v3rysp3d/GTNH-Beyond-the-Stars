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
import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
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
import gregtech.common.misc.WirelessNetworkManager;

/**
 * D-49: one Eye's live star (d49-build-plan.md §4 "The controller around step", §6, §8). Owns the state machine
 * ({@code DARK → IGNITING → LIT}, PENDING), the {@link FieldState}, cycle charging (EU and upkeep → the cycle's r),
 * ignition, the time dial and its Time payment, the trims drained from the Field sockets, the T4 HSM feed, the
 * output, the milestone counters and the core escrow. The controller calls {@link #second()} once per field second
 * (every 20 ticks, straight after its socket drain) and supplies the world through {@link Host}.
 * <p>
 * Unit 4 runs T1-T4. Transitions (Mass Infusion, Shell Ignition, Core Collapse, First Cooling, Magnetar Merger), the
 * T5 batches and the T6 shield/Agent wiring are unit 5: a T4 star is lit here only by the debug {@code lit}, and a
 * next-tier chart only parks the star in PENDING (it keeps burning).
 * <p>
 * Order of one lit second (the prototype's, so {@code /btseye star … sim N compare} matches {@code FieldSim}):
 * dial payment, trims, feed availability, {@link StellarField#step}, then failure, HSM draw, cycle end (product, then
 * the next cycle's charge), publish.
 */
public final class EyeStarRuntime {

    /** The star's state (plan §3.2). IDLE and BATCH are T5's (unit 5). */
    public enum StarState {
        DARK,
        IGNITING,
        LIT,
        /** LIT, with the next tier's chart in the slot: the transition waits on its preconditions (unit 5). */
        PENDING,
        IDLE,
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

    // ---------------------------------------------------------------- transient (scanner)
    private FieldReadout last = FieldReadout.DARK;
    private StepResult lastOut;
    private double lastM = 1, lastFeed;
    private int lastP = 1;
    private long lastTimePaid;
    private final Map<TrimDef.Kind, Long> lastTrims = new EnumMap<>(TrimDef.Kind.class);
    private String waitReason = "";
    private final StepInputs in = new StepInputs();

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

    /** True while a star exists (igniting, lit or pending). */
    public boolean live() {
        return state != StarState.DARK && state != StarState.IDLE;
    }

    /** True while the star burns (the controller's active flag; GT's Activity Detector cover). */
    public boolean burning() {
        return state == StarState.LIT || state == StarState.PENDING || state == StarState.BATCH;
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

    /** Parallel for a star of tier n on this Eye: {@code 2^(installedTier − n)}, 0 when the Eye is below it. */
    public int parallel(int n) {
        final int inst = host.installedTier();
        return inst >= n ? 1 << (inst - n) : 0;
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
        hsmOwed = prodCarry = tengam = 0;
        cycWorst = flashState = flashTail = 0;
        flashOk = false;
        rig = null;
        lastEvent = "";
        ledger = null;
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
            return;
        }
        if (!host.allowedToWork()) {
            if (live()) extinguish("the machine was disabled");
            return;
        }
        final StarDef want = StarDef.byChart(host.target());
        if (live()) {
            if (star.n() > host.installedTier()) {
                extinguish("the Eye no longer reaches T" + star.n());
                return;
            }
            if (want == star) {
                if (state == StarState.PENDING) state = StarState.LIT;
            } else if (want != null && burning() && want.n() == star.n() + 1) {
                state = StarState.PENDING; // unit 5: beginTransition when every precondition holds
            } else {
                extinguish("the target changed");
                return;
            }
        }
        switch (state) {
            case IGNITING:
                igniteSecond();
                break;
            case LIT:
            case PENDING:
                litSecond();
                break;
            default:
                darkSecond(want);
                break;
        }
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
        final int p = parallel(n);
        if (p < 1) {
            extinguish("the Eye no longer reaches T" + n);
            return;
        }
        final int tt = host.timeTier();
        // ---- the time dial (plan §6.4): pay Time from the Field sockets; a short payment slows the dial
        final double mCap = Math.min(dialSet, StellarField.mMax(T, tt));
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
        // ---- trims: the Field sockets, in the tier's table order, up to cap × p units each
        in.clearTrims();
        lastTrims.clear();
        if (rig != null) {
            FieldRig.toUnits(T, FieldRig.ctrl(T, fs.prevE, fs.prevWarn, rig), in.u);
        } else {
            for (TrimDef trim : T.trims()) {
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
        // ---- T4: the HSM feed actually available this second, L/s per parallel
        in.feed = 0;
        if (T.feed && star.feed != null) {
            final double hsm = Math.max(0, host.buffer(star.feed) - hsmOwed);
            in.feed = Math.min(feedSet, hsm / (m * p));
        }
        lastM = m;
        lastP = p;
        lastFeed = in.feed;

        final StepResult out = StellarField.step(fs, Overlay.NONE, in);
        lastOut = out;
        if (out.zone == 0) milestones.res[n]++;
        cycWorst = Math.max(cycWorst, out.zone);
        if (ledger != null) ledger.lit(out);
        if (out.fail != null) {
            fail(out.fail);
            if (ledger != null) ledger.record(this, out.zone);
            return;
        }
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
        watchFlash(n, out);
        // ---- the cycle: product, milestones, then charge the next one (it sets the next r)
        if (out.cycleEnded) {
            if (out.hasCycleProduct) {
                emit(out.cycleProduct);
                if (n <= 3) milestones.prod[n] += out.cycleProduct;
                else if (n == 4 && cycWorst <= 1) milestones.prod[4] += out.cycleProduct;
                if (ledger != null) ledger.product += out.cycleProduct;
            }
            if (ledger != null) ledger.cycles++;
            chargeCycle();
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

    // ========================================================================= cycles, ignition

    /**
     * Debits the cycle's EU and takes its upkeep (plan §6.2). A failed debit makes r = 0; the fluids are still taken
     * (upkeep is paid in full whatever the yield), and r is the worst delivered fraction. Resets the cycle's worst
     * zone (T4 milestone).
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
            r = Math.min(r, need <= 0 ? 1 : got / (double) need);
        }
        cycR = r;
        lastP = p;
        cycWorst = 0;
        host.markDirty();
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

    /** Auto-start (plan §6.3): a dark Eye with a T1-T3 chart it reaches lights itself when everything is there. */
    private void tryAutoStart(StarDef want) {
        if (want == null) {
            waitReason = "";
            return;
        }
        final int n = want.n();
        if (n > host.installedTier()) {
            waitReason = "the Eye does not reach T" + n + " (unlocked T" + unlockedTier + ")";
            return;
        }
        if (!want.tier.ignite) {
            waitReason = n == 4
                ? "the neutron star is born from a lit Arcturus by the Core Collapse (put the Neutron Star chart in"
                    + " while Arcturus burns)"
                : "the T" + n + " star is spawned by its transition, not ignited";
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
        lastEvent = "ignition: " + want.name + " (1 charge, the seed cloud and " + sci(eu10) + " EU)";
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

    /** A failed star (plan §6.6, T1-T4): DARK, the gauges zeroed; in-flight output is lost. Relight needs a charge. */
    private void fail(String reason) {
        lastEvent = (star == null ? "star" : star.name) + " LOST: "
            + reason
            + (star != null && star.n() == 4 ? " (collapsed to a black hole: relight Arcturus for a new Core Collapse)"
                : "");
        if (ledger != null) ledger.fails++;
        goDark();
    }

    /** Puts a live star out (target change, wrench, soft mallet, structure break). The core stays in escrow. */
    public void extinguish(String reason) {
        if (!live()) return;
        lastEvent = (star == null ? "star" : star.name) + " put out: " + reason;
        goDark();
    }

    private void goDark() {
        state = StarState.DARK;
        waitReason = "";
        if (fs != null) StellarField.resetLost(fs);
        cycR = 1;
        prodCarry = 0;
        hsmOwed = 0;
        flashState = 0;
        lastOut = null;
        publish(FieldReadout.DARK);
        host.markDirty();
    }

    // ============================================================================ debug (bench)

    /**
     * Debug {@code lit}: lights {@code def} at once, without a charge, seed or ignition EU (plan §6.3 "lit skips all
     * of this"); the first cycle is charged normally. Starts the bench ledger. Returns an error or null.
     */
    public String debugLight(StarDef def) {
        if (def == null) return "no star chart in the controller slot (or name one)";
        if (def.n() > 4) return "T5/T6 stars arrive with D-49 unit 5";
        if (def.n() > host.installedTier()) return "the Eye does not reach T" + def.n();
        if (live()) goDark();
        ensureField(def);
        star = def;
        light();
        lastEvent = def.name + " lit (debug)";
        ledger = new Ledger(seed);
        return null;
    }

    /** Debug {@code seed n}: a fresh field (clock 0, gauges 0) with that noise seed; a burning star relights. */
    public void debugSeed(long newSeed) {
        final TierParams t = fs != null ? fs.tier : star != null ? star.tier : TierParams.T1;
        seed = newSeed;
        fs = new FieldState(t, new FieldRng(newSeed));
        if (burning()) {
            light();
            ledger = new Ledger(seed);
        } else if (state == StarState.IGNITING) {
            goDark();
        }
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
        switch (n) {
            case 1:
                return "Mass Infusion";
            case 2:
                return "Shell Ignition";
            case 3:
                return "Core Collapse";
            case 4:
                return "First Cooling";
            case 5:
                return "Magnetar Merger";
            default:
                return "";
        }
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

    /** Sneak-punch with an empty hand: returns the escrowed core (no transition runs in unit 4), or null. */
    public ItemStack takeEscrow() {
        final ItemStack out = escrow;
        escrow = null;
        if (out != null) host.markDirty();
        return out;
    }

    // ================================================================================ migration

    /**
     * Plan §13: at the first successful walk of a pre-D-49 Eye, the bands it had built unlock their tiers under the
     * OLD rule (the jet crescent counted without the beam band): {@code max(1, min(3, legacyStructureTier))}.
     */
    public void grandfather(int legacyStructureTier) {
        if (!legacyPending) return;
        legacyPending = false;
        unlockedTier = Math.max(unlockedTier, Math.max(1, Math.min(3, legacyStructureTier)));
        lastEvent = "migrated from 0.24.x: unlocked T" + unlockedTier + " (bands grandfathered)";
        host.markDirty();
    }

    /** Pays a pre-D-49 star run in flight (plan §13 item 3) once the Eye stands with an output hatch. */
    private void payLegacy() {
        if (legacyPay.isEmpty() || !host.formed()) return;
        final StringBuilder sb = new StringBuilder("migration: credited ");
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
        lastEvent = sb.append("from the run in flight")
            .toString();
        com.nateyburr.beyondthestars.BeyondTheStars.LOG.info("[BTS] Eye {}", lastEvent);
        host.markDirty();
    }

    // ================================================================================== fluids

    /** Tachyon Rich Temporal Fluid: molten {@code Materials.Time} on RC-2 (B-35 item 6). */
    public static Fluid timeFluid() {
        return fluidOf(Materials.Time.getMolten(1L));
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
                return fluidOf(Materials.SpaceTime.getMolten(1L));
            case TENGAM:
                return fluidOf(Materials.TengamPurified.getMolten(1L));
            case DSSF:
                return fluidOf(Materials.DimensionallyShiftedSuperfluid.getFluid(1L));
            case RAW_STAR_MATTER:
                return fluidOf(Materials.RawStarMatter.getFluid(1L));
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
                + (legacyPending ? " (migration: the bands are grandfathered at the next walk)" : "")
                + "; escrow: "
                + (escrow == null ? "empty" : escrow.getDisplayName())
                + (coreFor(unlockedTier) != null ? " (next: " + new ItemStack(coreFor(unlockedTier)).getDisplayName()
                    + ", "
                    + eventFor(unlockedTier)
                    + ")" : ""));
        if (unlockedTier <= 5) out.add(milestones.progressLine(unlockedTier));
        if (state == StarState.DARK || star == null) {
            out.add("Star: DARK" + (waitReason.isEmpty() ? "" : " - waiting: " + waitReason));
        } else if (state == StarState.IGNITING) {
            out.add(
                "Star: " + star.name
                    + " IGNITING, P "
                    + StellarField.fmtSigned(fs.x[StellarField.P])
                    + " (lights at "
                    + StellarField.fmtSigned(StellarField.IGNITION_LIT)
                    + ", +1.5/s)");
        } else {
            final TierParams T = fs.tier;
            final double mMax = StellarField.mMax(T, host.timeTier());
            out.add(
                "Star: " + star.name
                    + " (T"
                    + T.n
                    + ") "
                    + state
                    + (state == StarState.PENDING ? " (next chart in the slot: transitions arrive with unit 5)" : "")
                    + ", parallel "
                    + lastP
                    + ", dial m "
                    + f2(lastM)
                    + " (set "
                    + f2(dialSet)
                    + ", max "
                    + f2(mMax)
                    + (lastTimePaid > 0 ? ", " + fmt(lastTimePaid) + " L Time/s" : "")
                    + "), q "
                    + f2(StepInputs.qFor(host.stabilisationTier())));
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
                for (int k : T.gauges()) {
                    g.append("; ")
                        .append(StellarField.GAUGE_SYM[k])
                        .append(' ')
                        .append(StellarField.fmtSigned(fs.x[k]));
                }
                out.add(g.toString());
                if (lastOut.warn != 0 && T.event != null) {
                    out.add(
                        "Event warning: " + T.event.name
                            + (T.event.signed ? " (" + (lastOut.warn > 0 ? "+" : "-") + ")" : ""));
                }
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
        if (!lastEvent.isEmpty()) out.add("Last event: " + lastEvent);
        if (!legacyPay.isEmpty()) out.add("Migration: a pre-D-49 run's output is owed (paid once formed)");
        return out;
    }

    // ================================================================================== ledger

    /**
     * The bench ledger ({@code /btseye star … sim}): the same counts and per-second hash as {@code FieldSim.runCase}
     * (zone seconds, mean Y, failures, cycles, product, and FNV-1a over x, A, I, zone, state, tier, lock), from the
     * debug {@code lit} or {@code seed} on. Saved with the star, so a restart continues it.
     */
    public static final class Ledger {

        static final int FNV_OFFSET = 0x811c9dc5, FNV_PRIME = 16777619;

        public final long seed;
        public final long[] zoneT = new long[5];
        public double ySum;
        public long yN, fails, cycles, steps;
        public double product, hsm;
        public int hash = FNV_OFFSET;

        Ledger(long seed) {
            this.seed = seed;
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
