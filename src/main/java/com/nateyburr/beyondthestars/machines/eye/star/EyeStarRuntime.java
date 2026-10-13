package com.nateyburr.beyondthestars.machines.eye.star;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
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
import com.nateyburr.beyondthestars.fluids.SLQuasarFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.GalaxyDirective;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry;
import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.EyeTrimRule;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.GalacticMergerOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.eye.field.MagnetarCollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.Overlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;

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

        /**
         * P4-77: drains up to {@code max} litres of {@code fluid} from the stock sockets (gas and stellar hatches, ME
         * stocking hatches included): an OpenComputers trim pulse's source. Returns litres drained.
         */
        default long drainStock(Fluid fluid, long max) {
            return 0;
        }

        /** Takes (or with {@code simulate}, finds) one Stellar Ignition Charge in a plinth input bus. */
        boolean takeIgnitionCharge(boolean simulate);

        /** GUI: how many Stellar Ignition Charges the live plinth input buses hold (nothing is taken). */
        int countIgnitionCharges();

        /** The wireless network's owner, or null. */
        UUID owner();

        /** Puts litres of fluid out through the ME output hatch(es). */
        void output(Fluid fluid, long litres);

        /** Puts items out through the ME output bus(es) (the T6 Tengam return). */
        void outputItems(ItemStack stack, long count);

        /** Pushes a readout to every live Stellar Field Sensor Hatch. */
        void publish(FieldReadout readout);

        void markDirty();

        /**
         * P4: what the GRB Receptor Ports received last second, by group (rail-high, rail-low, halo). The host also
         * reports itself to every feeding quasar with this demand (Bu/t) and link streak (0..1).
         */
        EyeBursts bursts(double demandBuPerTick, double streak01);

        /** P4: Receptor Ports standing in a group: 0 rail-high, 1 rail-low, 2 halo (the last walk). */
        int receptorPorts(int group);

        /** P4: one rising edge of a Burst Control Hatch in Begin mode since the last call (false if none). */
        boolean takeBeginEdge();

        /** P4: the slot bus's items in slot order (planet items, companion star charts). Never consumed. */
        List<ItemStack> slotBusItems();

        /** P4: a planet run's length in Eye-seconds (its base time with the compression reward). */
        double planetSeconds(EyeRecipe recipe);
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
    /** P4: the shock of the next Magnetar Collapse (+120 adopted, F-2; the bench's {@code set mcshock}). */
    double collapseShock = MagnetarCollapseOverlay.DEFAULT_SHOCK;
    /** P4: the galaxy tiers' state (mode, T8 feed, links, carries, companions, planet slots). */
    final EyeGalaxy galaxy = new EyeGalaxy();
    /** P4 bench: a scripted burst feed replacing the receptor reading (null in play; never saved). */
    EyeBursts benchBursts;
    /** P4 bench: the bench quasar's {Φ, b} for the collapse assist (its source is not a machine); null in play. */
    double[] benchFlux;

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
        } else if (tr instanceof MagnetarCollapse) {
            p = ((MagnetarCollapse) tr).progress();
        } else if (tr instanceof GalacticMerger) {
            p = ((GalacticMerger) tr).progress();
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

    /** P4: the galaxy tiers' state (mode, the T8 feed, the links, companions, planet slots). */
    public EyeGalaxy galaxy() {
        return galaxy;
    }

    /** P4: the shock the next Magnetar Collapse lands with (+120 adopted). */
    public double collapseShock() {
        return collapseShock;
    }

    /** P4: a galaxy transition (the Magnetar Collapse or the Galactic Merger) runs. */
    public boolean galaxyTransition() {
        return trans instanceof MagnetarCollapse || trans instanceof GalacticMerger;
    }

    /** P4: a galaxy-tier field burns (T7 / T8, galaxy or planets mode; the collapse's capture included). */
    public boolean galaxyLit() {
        return burning() && fs != null && fs.tier.n >= 7;
    }

    /** P4: the field in planets mode (3 / 6 planet slots). */
    public boolean planetsMode() {
        return fs != null && fs.tier.planets() && burning();
    }

    /** P4: the field's current gauges (P, Θ, Ω, Π, W, Λ), last second; zeros without a field. */
    public double[] gauges() {
        return fs == null ? new double[StellarField.GAUGES] : fs.x.clone();
    }

    /** P4: the satellite crossing's countdown (seconds to the hit), or −1 when none is warned. */
    public int crossingCountdown() {
        if (fs == null || fs.tde == null || !fs.tde.warned) return -1;
        return (int) Math.max(0, Math.ceil(fs.tde.next - fs.t));
    }

    /** P4: seconds left of a satellite crossing in progress (its 300 s), or 0. */
    public int crossingLeft() {
        return fs == null || fs.tde == null ? 0 : fs.tde.left;
    }

    /** P4: crossings finished and caught (display). */
    public int[] crossings() {
        return fs == null || fs.tde == null ? new int[2] : new int[] { fs.tde.n, fs.tde.caught };
    }

    /** P4: the minor merger's warning sign (+1 / −1) while warned, else 0; the precession sense s. */
    public int mergerWarning() {
        return fs != null && fs.tier.galaxy && fs.ev != null && fs.ev.warned && trans == null ? fs.ev.sign : 0;
    }

    /** B-120: seconds to the minor merger while it is warned (its own clock, not the crossing's), or -1. */
    public int mergerCountdown() {
        return mergerWarning() == 0 ? -1 : (int) Math.max(0, Math.ceil(fs.ev.next - fs.t));
    }

    public int precession() {
        return fs == null ? 1 : fs.prec;
    }

    /** P4: the running countdown on the event channel (merger, crossing, pass, collapse) in seconds, or −1. */
    public int countdownSeconds() {
        if (trans instanceof MagnetarCollapse) {
            final int s = ((MagnetarCollapse) trans).secondsToCollapse();
            return s >= 0 && s <= MagnetarCollapseOverlay.OVERFEED_SECONDS - MagnetarCollapseOverlay.COUNTDOWN_AT ? s
                : -1;
        }
        if (trans instanceof GalacticMerger) {
            final GalacticMerger gm = (GalacticMerger) trans;
            return gm.warnedPass() >= 0 ? gm.secondsToNextPass() : -1;
        }
        final int c = crossingCountdown();
        if (c >= 0) return c;
        if (fs != null && fs.tier.galaxy && fs.ev != null && fs.ev.warned) {
            return (int) Math.max(0, Math.ceil(fs.ev.next - fs.t));
        }
        return -1;
    }

    /** P4: the link milestone the Eye is working on now (6 or 7), or 0. */
    public int linkTier() {
        if (fs == null || !burning() || trans != null) return 0;
        if (fs.tier.n == 6 && !milestones.linkDone[6]) return 6;
        if (fs.tier.n == 7 && fs.tier.galaxy && !milestones.linkDone[7]) return 7;
        return 0;
    }

    /** P4: the live link's BurstLink (the one {@link #linkTier} names), or null. */
    public BurstLink activeLink() {
        final int t = linkTier();
        return t == 6 ? galaxy.link6 : t == 7 ? galaxy.link7 : null;
    }

    /** P4: the link streak as 0..1 (sensor, quasar report). */
    public double linkProgress() {
        final BurstLink l = activeLink();
        return l == null ? 0 : l.progress();
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
        collapseShock = MagnetarCollapseOverlay.DEFAULT_SHOCK;
        galaxy.reset();
        benchBursts = null;
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

    /** Debug: sets the unlocked tier (1..8), lowering it too. */
    public void setUnlocked(int n) {
        unlockedTier = Math.max(1, Math.min(MAX_UNLOCK, n));
        legacyPending = false;
        host.markDirty();
    }

    /** The highest tier an Eye unlocks (P4: T8 Milky Way). */
    public static final int MAX_UNLOCK = 8;

    /** Bench {@code set mcshock}: the shock of the next Magnetar Collapse (one running keeps its own). */
    public void setCollapseShock(double v) {
        collapseShock = Math.max(0, Math.min(240, v));
        host.markDirty();
    }

    /** P4: the T8 Ergomatter feed setpoint, L/s per parallel (0-10). */
    public double f8Setpoint() {
        return galaxy.f8Set;
    }

    /** P4: sets the T8 feed (the GUI's and OpenComputers' clamping setter). */
    public void setF8(double v) {
        galaxy.setF8Quiet(v);
        host.markDirty();
    }

    /** P4: the mode (galaxy or planets). Changing it puts a lit T7 / T8 field out (the other one lights). */
    public EyeGalaxy.Mode mode() {
        return galaxy.mode;
    }

    public void setMode(EyeGalaxy.Mode m) {
        if (m == null || m == galaxy.mode) return;
        galaxy.mode = m;
        host.markDirty();
    }

    /**
     * P4: asks for the galaxy transition to begin (the GUI's Begin, a Burst Control Hatch rising edge in Begin mode,
     * OpenComputers {@code beginTransition()}). It begins on the next field second if Galaxy Transition bit 1 (ready)
     * is on then; otherwise it is ignored. Returns a line for the caller.
     */
    public String requestBegin() {
        if (galaxyTransition()) return "A " + trans.type.title + " is already running.";
        if (state != StarState.PENDING || star == null || (star.n() != 6 && star.n() != 7)) {
            return "Nothing to begin: put the next galaxy chart in the controller slot while the "
                + (star != null && star.n() == 7 ? "galaxy" : "magnetar")
                + " burns.";
        }
        final List<String> miss = missing(star.n());
        if (!miss.isEmpty()) return "Not ready: " + miss.get(0);
        galaxy.beginRequested = true;
        host.markDirty();
        return "The " + EyeTransition.Type.leaving(star.n()).title + " begins on the next field second.";
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
        final StarDef want = wanted();
        // P4: a Burst Control Hatch in Begin mode: a rising edge asks for the galaxy transition (ignored unless ready)
        if (host.takeBeginEdge() && state == StarState.PENDING && trans == null) requestBegin();
        if (live()) {
            if (parallel(star.n()) < 1) {
                extinguish("the Eye no longer reaches T" + star.n());
                return;
            }
            if (trans != null) {
                if (want == null || !towardDest(want, trans.type)) {
                    extinguish("the target changed during the " + trans.type.title);
                    return;
                }
            } else if (want == star) {
                if (state == StarState.PENDING) state = StarState.LIT;
                galaxy.beginRequested = false; // a begin is for the chart that was in, not a later one
            } else if (want != null
                && (state == StarState.LIT || state == StarState.PENDING || state == StarState.IGNITING)
                && ((star.n() <= 3 && want.n() == star.n() + 1) || galaxyChain(want))) {
                    // the next tier's chart: PENDING once lit (an igniting star finishes its ignition first)
                    if (state != StarState.IGNITING) state = StarState.PENDING;
                } else {
                    extinguish(
                        want != null && want.tier.planets() && !star.tier.planets() ? "planets mode was selected"
                            : want != null && star.tier.planets() && !want.tier.planets() ? "galaxy mode was selected"
                                : "the target changed");
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

    /**
     * The star the controller slot asks for: the chart's star, or (P4) with the Eye in planets mode the planets field
     * of a galaxy chart's tier (Triangulum: 3 slots, Milky Way: 6).
     */
    StarDef wanted() {
        final StarDef chart = StarDef.byChart(host.target());
        if (chart != null && chart.tier.galaxy && galaxy.mode == EyeGalaxy.Mode.PLANETS) {
            final StarDef planets = StarDef.planetsOf(chart.n());
            return planets != null ? planets : chart;
        }
        return chart;
    }

    /**
     * P4: the galaxy chain. A lit magnetar (or Triangulum) with a galaxy chart of a higher tier is PENDING: the next
     * transition re-runs (or, the first time, runs) toward it, one tier at a time, as long as the chart's tier is at
     * most one past the unlocked tier. Planets mode never chains.
     */
    private boolean galaxyChain(StarDef want) {
        return star != null && star.n() >= 6
            && !star.tier.planets()
            && !want.tier.planets()
            && want.n() > star.n()
            && want.n() <= unlockedTier + 1;
    }

    /**
     * True when the controller slot still asks for what the running transition makes, or (P4) for a galaxy beyond it
     * on the chain (the magnetar re-spawn on the way to Triangulum or the Milky Way, the Magnetar Collapse re-run on
     * the
     * way to the Milky Way).
     */
    private boolean towardDest(StarDef want, EyeTransition.Type type) {
        if (want.key.equals(type.dest)) return true;
        final int dest = type.from + 1;
        return dest >= 6 && !want.tier.planets() && want.n() > dest && want.n() <= unlockedTier + 1;
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
        // ---- P4: the GRB Receptor Ports (T6's Stability Link, the galaxy tiers and their transitions; quasar-p4.md
        // §3.1). Q-8: a quasar that fed them and is now frozen or not loaded PAUSES a burst-driven field: no step, no
        // cycle, no clock, no link streak this second (a running but dark quasar is felt in full).
        final boolean burstField = T.burstDriven() || galaxyTransition();
        if (n >= 6 || burstField) {
            galaxy.bursts = benchBursts != null ? benchBursts : host.bursts(galaxyDemand(T) * p, linkProgress());
            if (galaxy.bursts.paused && burstField) {
                waitReason = galaxy.bursts.line();
                host.publish(last);
                host.markDirty();
                return;
            }
        } else {
            galaxy.bursts = EyeBursts.NONE;
        }
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
            final LinkedHashMap<String, Double> raw;
            if (table.burstDriven()) {
                // P4: the prototype's binary Space valve (Field bit 1: P > +5 opens it at 10 units/s)
                raw = new LinkedHashMap<>();
                if (rig != FieldRig.Mode.OFF && fs.prevE[StellarField.P] > 5) raw.put("Space", SPACE_VALVE);
            } else {
                raw = FieldRig.ctrl(table, fs.prevE, fs.prevWarn, rig);
            }
            if (trans != null) trans.adjustRig(raw, fs, rig);
            FieldRig.toUnits(table, raw, in.u);
            // the prototype's rig is per parallel (p = 1 there); a socket's cap is cap × p, and the field divides by p
            if (p > 1) in.u.replaceAll((trim, units) -> units * p);
        } else {
            for (TrimDef trim : table.trims()) {
                final Fluid fluid = trimFluid(trim.kind);
                if (fluid == null) continue;
                final long max = (long) Math.floor(trim.cap * p * trim.kind.unitLitres);
                long got = host.drainField(fluid, max);
                // P4-77: an OpenComputers trim(kind, units) pulse tops it up from the stock sockets, same cap
                final double oc = ocTrim.getOrDefault(trim.kind, 0.0);
                if (oc > 0 && got < max) {
                    got += host
                        .drainStock(fluid, Math.min(max - got, (long) Math.floor(oc * p * trim.kind.unitLitres)));
                }
                if (got > 0) {
                    in.u.put(trim, got / (double) trim.kind.unitLitres);
                    lastTrims.put(trim.kind, got);
                }
            }
        }
        ocTrim.clear(); // a pulse is one second
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
        // ---- P4: the bursts as the field sees them (β, the γs, the shock's raw Bu/t), the T8 feed, the companions
        galaxyInputs(T, m, p);
        lastM = m;
        lastP = p;
        lastFeed = in.feed;

        final StepResult out = StellarField.step(fs, trans != null ? trans : Overlay.NONE, in);
        lastOut = out;
        // milestones for tier n count only once n is unlocked (plan §7.1): not during the settle or imprint that
        // makes the star of a still-locked tier
        if (out.zone == 0 && n <= unlockedTier && !T.planets()) milestones.res[n]++;
        cycWorst = Math.max(cycWorst, out.zone);
        if (ledger != null) ledger.lit(out);
        creditAgent(lastTrims.containsKey(TrimDef.Kind.AGENT) ? lastTrims.get(TrimDef.Kind.AGENT) : 0);
        // ---- P4: a galaxy transition's charge (taken whatever follows, as the prototype's transTick)
        takeCharges(out, p);
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
            else if (T.planets()) planetsBreach(out.fail);
            else fail(out.fail);
            if (ledger != null) ledger.record(this, out.zone);
            if (!burning()) return;
        } else {
            // ---- P4: the magnetar collapsed (the T7 field began) / the first pass merged the nuclei (T8 began)
            if (out.collapsed) collapsed();
            if (out.merged) merged();
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
            // ---- P4: the T8 Ergomatter feed, the satellite crossing, the countdowns, the planet runs, the links
            galaxyAfterStep(T, out, m, p);
            watchFlash(fs.tier.n, out);
            // ---- the cycle: product, milestones, then charge the next one (it sets the next r)
            if (out.cycleEnded) {
                final int cn = fs.tier.n;
                if (out.hasCycleProduct) {
                    emit(out.cycleProduct);
                    if (cn <= unlockedTier) {
                        if (cn <= 3) milestones.prod[cn] += out.cycleProduct;
                        else if ((cn == 4 || cn == 6 || cn == 7) && cycWorst <= 1)
                            milestones.prod[cn] += out.cycleProduct;
                    }
                    if (ledger != null) ledger.product += out.cycleProduct;
                    // P4: the companions' products (the galaxy's mean Y and r, each companion's own upkeep r)
                    if (out.companionCycle) companionProducts(fs.tier, out);
                }
                if (ledger != null) ledger.cycles++;
                chargeCycle();
            }
            // ---- T5: the batch end
            if (out.batchDone) batchDone(out);
        }
        last = galaxyReadout(FieldReadout.of(fs, out, in));
        host.publish(last);
        if (ledger != null) ledger.record(this, out.zone);
        host.markDirty();
    }

    /** T3 milestone: a helium flash is ridden if the zone never leaves Stable from its warning to 30 s after it. */
    private void watchFlash(int n, StepResult out) {
        // P4: the same watch counts the T6 giant flares and the T7 minor mergers ridden in Stable (the milestones count
        // once the tier is unlocked, never during a transition)
        final boolean galaxyWatch = (n == 6 || n == 7) && fs.tier.event != null
            && !fs.tier.planets()
            && trans == null
            && n <= unlockedTier;
        if (n != 3 && !galaxyWatch) {
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
            if (flashOk) {
                if (n == 3) milestones.flashes++;
                else if (n == 6) milestones.flares++;
                else milestones.mergers++;
            }
            flashState = 0;
        }
    }

    // ===================================================================== P4: the galaxy tiers

    /** The prototype's binary Space valve on the Eye (debug rig): 10 units/s while P > +5. */
    static final double SPACE_VALVE = 10;

    /**
     * The burst demand now, Bu/t per parallel (quasar-p4.md §3.1, §4.1): a galaxy transition's own (the collapse's
     * ramp, the merger's 1.5 → 6), else D_T × (planets 0.25; galaxy 1 + 0.25 per companion); T6: D₇ (the Stability
     * Link's floor, the σ the quasar shows).
     */
    double galaxyDemand(TierParams T) {
        if (trans instanceof MagnetarCollapse) return ((MagnetarCollapse) trans).demand();
        if (trans instanceof GalacticMerger) return ((GalacticMerger) trans).demand();
        if (T.n >= 7) return GalaxyField.demand(T.n, T.planets(), T.galaxy ? galaxy.companions.length : 0);
        return GalaxyField.D7;
    }

    /** β's normaliser: D₇ through the Magnetar Collapse (finding F-1), else the demand. */
    private double normDemand(TierParams T) {
        if (trans instanceof MagnetarCollapse) return ((MagnetarCollapse) trans).normDemand();
        return galaxyDemand(T);
    }

    /**
     * Fills the field's burst inputs from last second's receptor groups (the prototype's {@code conduitStep} tail): β
     * and the γs normalised by D·m·p; at T7 (and through the Magnetar Collapse) the halo is a test fire, counted for
     * the
     * Heavy Link and dumped from the field; {@code gEye} is the raw Bu/t per parallel (the collapse shock's cancel).
     * The
     * T8 feed f₈ is what the buffer can pay this second, capped by the setpoint; companions only in galaxy mode outside
     * a
     * transition.
     */
    private void galaxyInputs(TierParams T, double m, int p) {
        final EyeBursts b = galaxy.bursts;
        final boolean bursty = T.burstDriven() || galaxyTransition();
        if (!bursty && T.n != 6) {
            in.beta = in.gHi = in.gLo = in.gHalo = in.gEye = in.f8 = 0;
            in.companions = 0;
            galaxy.beta = galaxy.gHi = galaxy.gLo = galaxy.gHalo = galaxy.gEye = 0;
            galaxy.demand = galaxy.normDemand = 0;
            return;
        }
        final boolean haloCounts = trans instanceof GalacticMerger || T.planets() || (T.n >= 8 && T.galaxy);
        final double dump = haloCounts ? 0 : b.halo;
        final double dn = Math.max(1e-9, normDemand(T) * m * p);
        in.beta = (b.total() - dump) / dn;
        in.gHi = b.hi / dn;
        in.gLo = b.lo / dn;
        in.gHalo = (b.halo - dump) / dn;
        in.gEye = b.total() / p;
        in.f8 = 0;
        if (T.ergoFeed && trans == null) {
            final Fluid ergo = SLQuasarFluids.ergomatter;
            in.f8 = ergo == null ? 0 : Math.max(0, Math.min(galaxy.f8Set, host.buffer(ergo) / (m * p)));
        }
        in.companions = T.galaxy && trans == null ? galaxy.companions.length : 0;
        galaxy.beta = in.beta;
        galaxy.gHi = in.gHi;
        galaxy.gLo = in.gLo;
        galaxy.gHalo = in.gHalo;
        galaxy.gEye = in.gEye;
        galaxy.demand = galaxyDemand(T) * p;
        galaxy.normDemand = normDemand(T);
    }

    /** The Magnetar Collapse's overfeed (WDM + BDM) and the Galactic Merger's Ergomatter charge, × p, carried. */
    private void takeCharges(StepResult out, int p) {
        if (out.wdmCharged > 0) take(wdmFluid(), galaxy.owe("charge:wdm", out.wdmCharged * p));
        if (out.bdmCharged > 0) take(bdmFluid(), galaxy.owe("charge:bdm", out.bdmCharged * p));
        if (out.ergoCharged > 0) take(SLQuasarFluids.ergomatter, galaxy.owe("charge:ergo", out.ergoCharged * p));
    }

    /** Takes up to {@code litres} from the buffer; returns what came. */
    private long take(Fluid fluid, long litres) {
        if (fluid == null || litres <= 0) return 0;
        final long got = Math.min(host.buffer(fluid), litres);
        if (got > 0) host.takeBuffer(fluid, got);
        return got;
    }

    /** The magnetar collapsed this second: Triangulum's field began (T7 rules, the halo capture), charge its cycle. */
    private void collapsed() {
        final StarDef tri = StarDef.byKey("star:triangulum");
        if (tri != null) star = tri;
        prodCarry = 0;
        chargeCycle();
        if (trans instanceof MagnetarCollapse) {
            final MagnetarCollapseOverlay mc = ((MagnetarCollapse) trans).mc;
            lastEvent = String.format(
                Locale.ROOT,
                "the magnetar COLLAPSED into a black hole: shock P %s -> %s (%.2f Bu/t on the ports cancelled %.0f of"
                    + " %.0f); the halo capture begins (Stable for 60 s in a row within 420-600 s)",
                StellarField.fmtSigned(mc.shockP0),
                StellarField.fmtSigned(mc.shockP),
                mc.shockG,
                mc.shockCancel,
                mc.shock);
            logEvent(lastEvent);
        }
    }

    /** The first pericentre pass bound the second nucleus: the tier is T8 now (Λ appears). */
    private void merged() {
        final StarDef mw = StarDef.byKey("star:milkyway");
        if (mw != null) star = mw;
        lastEvent = "the first pericentre pass bound the second nucleus: the Milky Way's tidal shear Λ appears";
        logEvent(lastEvent);
    }

    /**
     * After a lit second that did not fail: the T8 feed drawn, the satellite crossing's events and bonus, the
     * countdowns, planets mode's runs, the companions' set, and the two link milestones.
     */
    private void galaxyAfterStep(TierParams T, StepResult out, double m, int p) {
        if (out.ergoFed && out.ergoToDraw > 0) {
            take(SLQuasarFluids.ergomatter, galaxy.owe("feed:ergo", out.ergoToDraw));
        }
        if (out.crossingWarned) lastEvent = "satellite crossing in 120 s: a dwarf galaxy plunges through the disc (P is"
            + " pushed for 300 s; stay Stable to catch it; lower the feed at high f8)";
        if (out.crossingHit) lastEvent = "satellite crossing: the dwarf galaxy is in the disc (300 s)";
        if (out.crossingCaught) {
            emit(out.crossingBonus);
            if (ledger != null) ledger.product += out.crossingBonus;
            lastEvent = String.format(
                Locale.ROOT,
                "satellite crossing CAUGHT: +%,.0f L Superdense Ergomatter (a bonus cycle)",
                out.crossingBonus);
            logEvent(lastEvent);
        }
        if (out.crossingMissed) lastEvent = "satellite crossing missed (the field left Stable): no bonus";
        if (out.countdown) {
            lastEvent = trans instanceof MagnetarCollapse
                ? "COUNTDOWN: the magnetar collapses in 30 s (Galaxy Transition [4] cues a purge 2 s ahead"
                    + " so the eruption lands on the collapse second)"
                : trans instanceof GalacticMerger
                    ? "COUNTDOWN: a pericentre pass in 60 s ("
                        + (((GalacticMerger) trans).nextPassSign() < 0 ? "-" : "+")
                        + ")"
                    : lastEvent;
        }
        final TierParams Tn = fs.tier;
        if (Tn.planets()) planetsSecond(Tn, out.zone, m);
        if (Tn.galaxy && trans == null) watchCompanions(Tn);
        // ---- the machine milestones: a linked quasar holding the Eye (quasar-p4.md R.1)
        final EyeBursts b = galaxy.bursts;
        if (b.paused) return;
        if (T.n == 6 && trans == null && !T.planets()) {
            final BurstLink l = galaxy.link6;
            l.roll(b.rail(), b.hi);
            if (!milestones.linkDone[6] && unlockedTier >= 6) {
                final boolean ok = l.stabilityOk(b.linkedRail) && b.hiPorts > 0 && b.loPorts > 0;
                if (l.update(ok) || l.streak >= BurstLink.STREAK_SECONDS) linkHeld(6);
            }
            milestones.linkStreak[6] = l.streak;
            milestones.linkBest[6] = Math.max(milestones.linkBest[6], l.best);
        } else if (T.n == 7 && T.galaxy && trans == null) {
            final BurstLink l = galaxy.link7;
            l.roll(b.total(), b.halo);
            if (!milestones.linkDone[7] && unlockedTier >= 7) {
                if (l.update(l.heavyOk(b.linkedAll, b.haloPorts > 0)) || l.streak >= BurstLink.STREAK_SECONDS)
                    linkHeld(7);
            }
            milestones.linkStreak[7] = l.streak;
            milestones.linkBest[7] = Math.max(milestones.linkBest[7], l.best);
        }
    }

    private void linkHeld(int n) {
        if (milestones.linkDone[n]) return;
        milestones.linkDone[n] = true;
        milestones.linkBest[n] = Math.max(milestones.linkBest[n], EyeMilestones.LINK_NEED);
        lastEvent = "the " + EyeMilestones.LINK_NAME[n]
            + " is HELD: 30 min unbroken from the quasar at "
            + galaxy.bursts.source
            + " (milestone)";
        logEvent(lastEvent);
    }

    /** The companions' products at a galaxy cycle end (the prototype's: base × 2^(N−1−k) × mean Y × r). */
    private void companionProducts(TierParams T, StepResult out) {
        final int p = Math.max(1, cycP);
        for (int i = 0; i < galaxy.companions.length; i++) {
            final int k = galaxy.companions[i];
            final GalaxyField.Companion cp = GalaxyField.Companion.of(k);
            final StarDef obj = StarDef.ofTier(k);
            if (obj == null || obj.product == null) continue;
            final double r = Math.min(out.cycleRMin, i < galaxy.companionR.length ? galaxy.companionR[i] : 1);
            final double litres = cp.productPerCycle * GalaxyField.companionParallel(T.n, k) * p * out.cycleMeanY * r;
            final long whole = galaxy.earn(obj.product.getName(), litres);
            if (whole > 0) host.output(obj.product, whole);
            if (ledger != null) ledger.product += litres;
        }
    }

    /**
     * The companions the slot bus asks for: its star charts of earlier objects (T1-T6, unlocked, one of each tier), in
     * slot order, at most 1 / 2 / 3 at T7 / T8 / T9.
     */
    int[] wantedCompanions(TierParams T) {
        final int max = GalaxyField.maxCompanions(T.n);
        final List<Integer> out = new ArrayList<>();
        for (ItemStack stack : host.slotBusItems()) {
            if (out.size() >= max) break;
            final StarDef d = StarDef.byChart(stack);
            if (d == null || d.n() > 6 || d.n() > unlockedTier || out.contains(d.n())) continue;
            out.add(d.n());
        }
        final int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    /**
     * Galaxy mode: a change of the slot bus's companions extinguishes and relights the galaxy in place (§4.8 "as a
     * target change does today", read as the field re-forming at zero with a fresh cycle; P4-60): the gauges to 0,
     * Integrity 100, the cycle in flight lost, the new set's upkeep charged.
     */
    private void watchCompanions(TierParams T) {
        final int[] want = wantedCompanions(T);
        if (java.util.Arrays.equals(want, galaxy.companions)) return;
        galaxy.companions = want;
        galaxy.companionR = new double[want.length];
        java.util.Arrays.fill(galaxy.companionR, 1);
        StellarField.resetLost(fs);
        StellarField.light(fs, dialSet, host.timeTier());
        prodCarry = 0;
        chargeCycle();
        lastEvent = "companions changed (" + companionNames() + "): the galaxy relit at zero, the cycle in flight lost";
        logEvent(lastEvent);
    }

    /** "Sol (T1), SGR 1806-20 (T6)" or "none". */
    public String companionNames() {
        if (galaxy.companions.length == 0) return "none";
        final StringBuilder sb = new StringBuilder();
        for (int k : galaxy.companions) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(GalaxyField.Companion.of(k).label);
        }
        return sb.toString();
    }

    /** True when a companion's last upkeep ran short (Slots [2]). */
    public boolean companionShort() {
        if (fs == null || !fs.tier.galaxy || !burning()) return false;
        for (double r : galaxy.companionR) if (r < 1 - 1e-9) return true;
        return false;
    }

    /** Each companion's upkeep for the next cycle, in full and with its own r (galaxy mode, no transition). */
    private void chargeCompanions(int p) {
        if (star == null || !star.tier.galaxy || trans != null || fs == null || !fs.tier.galaxy) return;
        for (int i = 0; i < galaxy.companions.length; i++) {
            final int k = galaxy.companions[i];
            final GalaxyField.Companion cp = GalaxyField.Companion.of(k);
            final double par = GalaxyField.companionParallel(fs.tier.n, k) * p;
            double r = 1;
            for (int u = 0; u < cp.upkeepCount(); u++) {
                final Fluid fluid = companionFluid(cp.upkeepName(u));
                r = Math.min(r, drawExact("comp" + k + ":", fluid, cp.upkeepAmount(u) * par));
            }
            if (i < galaxy.companionR.length) galaxy.companionR[i] = r;
        }
    }

    /**
     * Draws {@code litres} of {@code fluid} with a carry (so 7.5 L a cycle costs exactly that); returns the upkeep
     * fraction r for it (1 when paid in full).
     */
    private double drawExact(String purpose, Fluid fluid, double litres) {
        if (litres <= 0) return 1;
        if (fluid == null) return 0;
        final long whole = galaxy.owe(purpose + fluid.getName(), litres);
        if (whole <= 0) return host.buffer(fluid) > 0 || litres < 1 ? 1 : 0;
        final long got = take(fluid, whole);
        return got / (double) whole;
    }

    /** The fluid of a companion upkeep name ({@link GalaxyField.Companion}'s display names), or null. */
    public static Fluid companionFluid(String name) {
        switch (name) {
            case "Hydrogen":
                return trimFluid(TrimDef.Kind.HYDROGEN);
            case "Helium":
                return trimFluid(TrimDef.Kind.HELIUM);
            case "Raw Star Matter":
                return rspmFluid();
            case "Excited Stellar Plasma":
                return SLFluids.excitedStellarPlasma;
            case "Hyperdense Stellar Mixture":
                return SLFluids.hyperdenseStellarMixture;
            case "Molten SpaceTime":
                return spaceTimeFluid();
            case "White Dwarf Matter":
                return wdmFluid();
            case "Black Dwarf Matter":
                return bdmFluid();
            case "Antipolarization Agent":
                return SLFluids.antipolarizationAgent;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------------- planets mode

    /** A dark Eye with a galaxy chart: planets mode lights at once; galaxy mode starts the chain from the magnetar. */
    private void tryGalaxy(StarDef want) {
        final int n = want.n();
        if (want.tier.planets()) {
            if (unlockedTier < n) {
                waitReason = "planets mode at T" + n + " needs T" + n + " unlocked (the " + bandTitle(n) + ")";
                return;
            }
            if (parallel(n) < 1) {
                waitReason = "the Eye does not reach T" + n
                    + " (bands T"
                    + host.structureTier()
                    + ", compression T"
                    + host.compressionTier()
                    + ")";
                return;
            }
            final BigInteger eu = BigInteger.valueOf(want.euPerCycle)
                .multiply(BigInteger.valueOf(Math.max(1, parallel(n))));
            if (networkEU().compareTo(eu) < 0) {
                waitReason = "the wireless network needs " + sci(eu) + " EU (one cycle of the planets field)";
                return;
            }
            ensureField(want);
            star = want;
            fs.setX(0, 0, 0, 0);
            galaxy.starOut();
            light();
            lastEvent = want.name + " lit (planets mode: put planet items in the slot bus, one per slot)";
            return;
        }
        if (unlockedTier < 6) {
            waitReason = "the galaxy is born from a lit magnetar by the Magnetar Collapse: unlock T6 first";
            return;
        }
        if (n > unlockedTier + 1) {
            waitReason = "the " + want.name
                + " needs T"
                + (n - 1)
                + " first (the "
                + EyeStarRuntime.eventFor(n - 1)
                + ")";
            return;
        }
        final StarDef magnetar = StarDef.ofTier(6);
        if (magnetar == null) {
            waitReason = "the magnetar is not registered";
            return;
        }
        tryMerger(magnetar, " on the way to " + want.name);
    }

    private static String bandTitle(int n) {
        return n == 7 ? "Magnetar Collapse" : "Galactic Merger";
    }

    /**
     * Planets mode, one lit second (quasar-p4.md §4.8): the slot bus's planets fill the slots in order (3 at T7, 6 at
     * T8); a slot pays its run at the start (H/He × parallel from the buffers, its EU from the network; it waits
     * until it can), runs its base time with the compression reward, and at the end rolls each parallel at
     * (1 − 0.05·R) × the field factor averaged over the run (Resonant / Stable 1, Strained 0.5, Critical 0).
     */
    private void planetsSecond(TierParams T, int zone, double m) {
        final int max = GalaxyField.planetSlots(T.n);
        final List<EyeRecipe> planets = new ArrayList<>();
        for (ItemStack stack : host.slotBusItems()) {
            if (planets.size() >= max) break;
            final EyeRecipe r = EyeRecipeRegistry.lookup(stack);
            if (r != null && r.isPlanet()) planets.add(r);
        }
        if (galaxy.slots.length != max) galaxy.slots = java.util.Arrays.copyOf(galaxy.slots, max);
        final int inst = host.installedTier();
        final double zf = GalaxyField.PLANET_ZONE_FACTOR[Math.max(0, Math.min(4, zone))];
        final StringBuilder done = new StringBuilder();
        for (int i = 0; i < max; i++) {
            final EyeRecipe want = i < planets.size() ? planets.get(i) : null;
            EyeGalaxy.PlanetRun run = galaxy.slots[i];
            if (want == null) {
                galaxy.slots[i] = null;
                continue;
            }
            if (run == null || !run.key.equals(want.key)) {
                run = new EyeGalaxy.PlanetRun(want.key); // a changed slot abandons its run (its inputs are spent)
                galaxy.slots[i] = run;
            }
            if (run.paid <= 0 && !payPlanet(run, want, inst)) continue;
            run.t += m;
            run.zf += zf;
            run.n++;
            if (run.t >= host.planetSeconds(want)) {
                final double f = run.zf / Math.max(1, run.n);
                final long par = run.paid;
                final double chance = Math.max(0, GalaxyField.planetBase(want.rocketTier) * f);
                long ok = 0;
                for (long k = 0; k < par; k++) if (PLANET_RNG.nextDouble() < chance) ok++;
                planetOutputs(want, ok);
                galaxy.lastRunLost = f < 1 - 1e-9;
                if (done.length() > 0) done.append("; ");
                done.append(want.name)
                    .append(' ')
                    .append(ok)
                    .append('/')
                    .append(par)
                    .append(String.format(Locale.ROOT, " (field %.2f)", f));
                if ("planet:DD".equals(want.key)) milestones.deepDark++;
                run.reset();
            }
        }
        if (done.length() > 0) {
            galaxy.lastRuns = done.toString();
            lastEvent = "planet runs finished: " + galaxy.lastRuns;
        }
    }

    private static final java.util.Random PLANET_RNG = new java.util.Random();

    /** Pays one planet run at its start: H/He × parallel from the buffers, then its EU; false (it waits) if short. */
    private boolean payPlanet(EyeGalaxy.PlanetRun run, EyeRecipe recipe, int installed) {
        final long par = recipe.tier <= installed ? 1L << (installed - recipe.tier) : 0;
        if (par <= 0) return false;
        for (EyeRecipe.FluidAmount in : recipe.inputs) if (host.buffer(in.fluid) < in.amount * par) return false;
        final BigInteger eu = BigInteger.valueOf(recipe.euPerRun)
            .multiply(BigInteger.valueOf(par));
        if (networkEU().compareTo(eu) < 0 || !debit(eu)) return false;
        for (EyeRecipe.FluidAmount in : recipe.inputs) host.takeBuffer(in.fluid, in.amount * par);
        run.paid = par;
        return true;
    }

    private void planetOutputs(EyeRecipe recipe, long successes) {
        if (successes <= 0) return;
        for (EyeRecipe.ItemAmount out : recipe.itemOutputs) host.outputItems(out.stack, out.amount * successes);
        for (EyeRecipe.FluidAmount out : recipe.fluidOutputs) host.output(out.fluid, out.amount * successes);
    }

    /**
     * Planets mode's field breached (or its Integrity reached 0): the runs in flight fail (their inputs are spent) and
     * the field re-forms at zero with a fresh cycle (§4.8: "nothing else is lost").
     */
    private void planetsBreach(String reason) {
        for (EyeGalaxy.PlanetRun r : galaxy.slots) if (r != null) r.reset();
        if (ledger != null) ledger.fails++;
        StellarField.resetLost(fs);
        StellarField.light(fs, dialSet, host.timeTier());
        prodCarry = 0;
        chargeCycle();
        recordFailure("planets field BREACH (" + reason + "): the runs in flight failed; the field re-formed");
    }

    /** True when a planet slot of this tier has no planet or waits for its inputs (Slots [1]). */
    public boolean slotIdle() {
        if (fs == null || !burning()) return false;
        if (fs.tier.planets()) {
            final int max = GalaxyField.planetSlots(fs.tier.n);
            for (int i = 0; i < max; i++) {
                if (i >= galaxy.slots.length || galaxy.slots[i] == null || galaxy.slots[i].paid <= 0) return true;
            }
            return false;
        }
        return fs.tier.galaxy && trans == null
            && galaxy.companions.length < GalaxyField.maxCompanions(fs.tier.n)
            && !host.slotBusItems()
                .isEmpty();
    }

    /** Planets mode: one line per slot ("Mars 34 % x4", "empty", "waiting for H/He or EU"). */
    public List<String> slotLines() {
        final List<String> out = new ArrayList<>();
        if (fs == null || !fs.tier.planets()) return out;
        final int max = GalaxyField.planetSlots(fs.tier.n);
        for (int i = 0; i < max; i++) {
            final EyeGalaxy.PlanetRun r = i < galaxy.slots.length ? galaxy.slots[i] : null;
            final EyeRecipe rec = r == null ? null : EyeRecipeRegistry.byKey(r.key);
            if (rec == null) {
                out.add("slot " + (i + 1) + ": empty (a planet item in the slot bus)");
            } else if (r.paid <= 0) {
                out.add("slot " + (i + 1) + ": " + rec.name + " waits for its H/He and EU");
            } else {
                out.add(
                    String.format(
                        Locale.ROOT,
                        "slot %d: %s x%d, %.0f%% (field factor %.2f)",
                        i + 1,
                        rec.name,
                        r.paid,
                        100 * Math.min(1, r.t / Math.max(1, host.planetSeconds(rec))),
                        r.zf / Math.max(1, r.n)));
            }
        }
        return out;
    }

    /** Planets mode: the mean progress of the running slots, 0..1 (the sensor's slot channel). */
    public double slotProgress() {
        if (fs == null || !fs.tier.planets()) return 0;
        double sum = 0;
        int n = 0;
        for (EyeGalaxy.PlanetRun r : galaxy.slots) {
            if (r == null || r.paid <= 0) continue;
            final EyeRecipe rec = EyeRecipeRegistry.byKey(r.key);
            if (rec == null) continue;
            sum += Math.min(1, r.t / Math.max(1, host.planetSeconds(rec)));
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    /** The readout with P4's channels (β, the link streak and mean, slot progress, the countdown). */
    private FieldReadout galaxyReadout(FieldReadout r) {
        final BurstLink l = activeLink();
        return r.withGalaxy(
            galaxy.beta,
            l == null ? 0 : l.progress(),
            l == null ? galaxy.bursts.mean60 : l.mean,
            slotProgress(),
            countdownSeconds(),
            fs != null && fs.tier.planets());
    }

    /** P4 Galaxy Transition bits: the linked quasar's Φ and b (the collapse assist), or {0, 0}. */
    private double[] linkedQuasarFlux() {
        if (benchBursts != null && benchFlux != null) return benchFlux.clone();
        final BurstSource s = galaxy.bursts.sourceId;
        final MTEQuasarEngine q = s == null ? null : s.resolve();
        if (q == null || !q.isFormed()) return new double[2];
        return new double[] { q.state().Phi, q.state().b }; // the prototype reads the flux as it stands (transBits)
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
        // P4: fractional upkeep (Triangulum's Molten Magnetar Matter, 7.5 L a cycle × p), drawn with a carry
        for (Map.Entry<Fluid, Double> up : star.exactUpkeep.entrySet()) {
            r = Math.min(r, drawExact("up:", up.getKey(), up.getValue() * p));
        }
        // P4: the companions pay their own upkeep in full, each with its own r (their products use it)
        chargeCompanions(p);
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
            tryMerger(want, "");
            return;
        }
        if (n >= 7) {
            tryGalaxy(want);
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

    // ============================================================================ intake cap (B-108)

    /** B-108: the cycles of upkeep (and of the T4 feed at its maximum) a star's buffers fill to. */
    public static final int INTAKE_CYCLES = 20;
    /** B-108: the T5 batch / T6 merger charges and T6 dwarf-matter top-ups the buffers hold ahead. */
    public static final int INTAKE_CHARGES = 2;

    /**
     * B-108 THE INTAKE CAP: per fluid, the most the buffers fill to from the gas and stellar sockets now. An ME
     * stocking input hatch shows the Eye up to 2.1 G L a second, so with no cap one hatch emptied the whole network
     * into the buffers and starved everything else on it (trim export buses). The cap covers every amount the star
     * side checks the buffers against, so nothing that could start now waits on it:
     * <ul>
     * <li>each star in play (the live one, the slot's chart, and the star a dark Eye lights for it: T4 relights
     * Arcturus): {@code max(ignition need, INTAKE_CYCLES cycles of upkeep)} × p, the T4 feed for INTAKE_CYCLES cycles
     * at
     * {@link #FEED_MAX}, and INTAKE_CHARGES T5 / T6 charges and T6 top-ups;</li>
     * <li>plus each transition that may begin: the live star's toward the chart (T4-T6 from the chart alone), the
     * running one, and the next one once its core is in escrow ({@link #buffersShort}'s rule): its event fluids and
     * the new star's first cycle ({@link #transitionNeeds}), and for the Core Collapse the 90,000 L of SpaceTime the
     * bounce can absorb.</li>
     * <li>P4, T7 / T8 (the slot's chart a galaxy, or a galaxy lit): the planets-mode variant of the galaxy as a star in
     * play; in galaxy mode the chain's steps still ahead of the live object (the Magnetar Merger a dark Eye starts
     * from, the Magnetar Collapse, the Galactic Merger) as transitions that may begin, and the magnetar / Triangulum on
     * the way as stars in play; in galaxy mode the companions (running, and the slot bus's star charts that would latch
     * at the next
     * relight) for INTAKE_CYCLES cycles each at their own parallel, added to the galaxy's share (they are fed at the
     * same time); in planets mode the slot bus's planets, {@link #PLANET_INTAKE_RUNS} runs each at full parallel,
     * added up (the slots run at once).</li>
     * </ul>
     * A star's share includes its fractional upkeep (T7's Molten Magnetar Matter, {@link StarDef#exactUpkeep}) and, for
     * the T8 Ergomatter feed, INTAKE_CYCLES cycles at {@link GalaxyField#F8_MAX}. The galaxy transitions' charges
     * (the Magnetar Collapse's WDM + BDM overfeed, the Galactic Merger's Ergomatter) come in through
     * {@link #transitionNeeds}. Fluids not listed are not pulled at all (they stay in the hatch or the network). The
     * controller adds the planet in the slot. Buffers already above their cap (worlds from before B-108) keep what they
     * hold and are only drawn
     * down. Server state.
     */
    public Map<Fluid, Long> intakeCaps() {
        final Map<Fluid, Long> cap = new LinkedHashMap<>();
        final StarDef want = StarDef.byChart(host.target());
        final boolean liveStar = live() && star != null;
        if (liveStar) starIntake(cap, star);
        if (want != null) {
            starIntake(cap, want);
            final StarDef lights = ignitable(want);
            if (lights != null && lights != want) starIntake(cap, lights);
            // P4: a galaxy chart lights its planets-mode variant in planets mode
            if (want.galaxyTier()) {
                final StarDef planets = StarDef.planetsOf(want.n());
                if (planets != null && planets != want) starIntake(cap, planets);
            }
        }
        galaxyIntake(cap, want, liveStar);
        final EnumSet<EyeTransition.Type> types = EnumSet.noneOf(EyeTransition.Type.class);
        if (want != null && want.n() >= 2 && (want.n() >= 4 || liveStar && star.n() == want.n() - 1)) {
            final EyeTransition.Type t = EyeTransition.Type.leaving(want.n() - 1);
            if (t != null) types.add(t);
        }
        if (trans != null) types.add(trans.type);
        // P4: a galaxy chart in galaxy mode walks the chain from the magnetar (a dark Eye re-merges one, then the
        // Magnetar Collapse, then for the Milky Way the Galactic Merger): every step still ahead of the live object
        // may begin, and each object on the way is a star in play.
        if (want != null && want.galaxyTier() && galaxy.mode == EyeGalaxy.Mode.GALAXY) {
            final int liveN = liveStar ? star.n() : 0;
            for (int from = 5; from < want.n(); from++) {
                if (liveN > from) continue;
                final EyeTransition.Type t = EyeTransition.Type.leaving(from);
                if (t != null) types.add(t);
                final StarDef via = StarDef.ofTier(from + 1);
                if (via != null && via != want) starIntake(cap, via);
            }
        }
        final Item core = coreFor(unlockedTier);
        if (core != null && escrow != null && escrow.getItem() == core) {
            final EyeTransition.Type t = EyeTransition.Type.leaving(unlockedTier);
            if (t != null) types.add(t);
        }
        for (EyeTransition.Type t : types) {
            for (Map.Entry<Fluid, Long> need : transitionNeeds(t).entrySet()) {
                if (need.getKey() != null) cap.merge(need.getKey(), need.getValue(), Long::sum);
            }
            final Fluid st = spaceTimeFluid();
            if (t == EyeTransition.Type.CORE_COLLAPSE && st != null) {
                cap.merge(
                    st,
                    (long) (CollapseOverlay.MAX_ABSORB_POINTS * CollapseOverlay.SPACETIME_PER_POINT),
                    Long::sum);
            }
        }
        return cap;
    }

    /** B-108 / P4: the runs a planet in a planets-mode slot buffers ahead (the controller's slot uses the same). */
    public static final int PLANET_INTAKE_RUNS = 2;

    /**
     * P4's share of {@link #intakeCaps} on top of the stars in play: the galaxy's companions (galaxy mode) or the slot
     * bus's planets (planets mode). Both are fed at the same time as the galaxy itself, so they add up.
     */
    private void galaxyIntake(Map<Fluid, Long> cap, StarDef want, boolean liveStar) {
        final StarDef galaxyDef = liveStar && star.galaxyTier() ? star
            : want != null && want.galaxyTier() ? want : null;
        if (galaxyDef == null) return;
        final int n = galaxyDef.n();
        final int p = Math.max(1, parallel(n));
        if (galaxy.mode == EyeGalaxy.Mode.PLANETS || liveStar && star.tier.planets()) {
            final int inst = host.installedTier();
            int slots = GalaxyField.planetSlots(n);
            for (ItemStack stack : host.slotBusItems()) {
                if (slots <= 0) break;
                final EyeRecipe r = EyeRecipeRegistry.lookup(stack);
                if (r == null || !r.isPlanet()) continue;
                slots--;
                final long par = r.tier <= inst ? 1L << (inst - r.tier) : 0;
                for (EyeRecipe.FluidAmount in : r.inputs) {
                    if (in.fluid != null && in.amount > 0) {
                        cap.merge(in.fluid, sat((double) in.amount * par * PLANET_INTAKE_RUNS), EyeStarRuntime::satSum);
                    }
                }
            }
            return;
        }
        final java.util.Set<Integer> comps = new java.util.LinkedHashSet<>();
        for (int k : galaxy.companions) comps.add(k);
        final TierParams t = galaxyDef.tier;
        for (int k : wantedCompanions(t)) comps.add(k);
        for (int k : comps) {
            final GalaxyField.Companion cp = GalaxyField.Companion.of(k);
            final double par = GalaxyField.companionParallel(n, k) * p;
            for (int u = 0; u < cp.upkeepCount(); u++) {
                final Fluid fluid = companionFluid(cp.upkeepName(u));
                if (fluid == null) continue;
                cap.merge(fluid, sat(Math.ceil(cp.upkeepAmount(u) * par) * INTAKE_CYCLES), EyeStarRuntime::satSum);
            }
        }
    }

    /** Litres as a long, saturated (a T8 companion's hydrogen runs to 10^10 L and more). */
    private static long sat(double litres) {
        return litres >= Long.MAX_VALUE / 4 ? Long.MAX_VALUE / 4 : (long) Math.ceil(Math.max(0, litres));
    }

    private static Long satSum(Long a, Long b) {
        final long s = a + b;
        return s < 0 || s > Long.MAX_VALUE / 4 ? Long.MAX_VALUE / 4 : s;
    }

    /** One star's share of {@link #intakeCaps} (max-merged: the stars in play are never fed at once). */
    private void starIntake(Map<Fluid, Long> cap, StarDef def) {
        final int p = Math.max(1, parallel(def.n()));
        for (EyeRecipe.FluidAmount up : def.upkeep) {
            cap.merge(up.fluid, up.amount * p * INTAKE_CYCLES, Math::max);
        }
        // P4: fractional upkeep (T7's Molten Magnetar Matter 7.5 L a cycle × p)
        for (Map.Entry<Fluid, Double> up : def.exactUpkeep.entrySet()) {
            if (up.getKey() != null) {
                cap.merge(up.getKey(), sat(Math.ceil(up.getValue() * p) * INTAKE_CYCLES), Math::max);
            }
        }
        if (def.tier.ignite) {
            for (Map.Entry<Fluid, Long> need : ignitionNeeds(def, p).entrySet()) {
                cap.merge(need.getKey(), need.getValue(), Math::max);
            }
        }
        if (def.feed != null) {
            // T4's HSM feed at FEED_MAX; P4's T8 Ergomatter feed at F8_MAX (f8 · p litres a second, cycles at the dial)
            final double rate = def.tier.ergoFeed ? GalaxyField.F8_MAX : FEED_MAX;
            cap.merge(def.feed, (long) Math.ceil(rate * StellarField.CYCLE_SECONDS * p) * INTAKE_CYCLES, Math::max);
        }
        for (EyeRecipe.FluidAmount ch : def.charge) {
            cap.merge(ch.fluid, ch.amount * p * INTAKE_CHARGES, Math::max);
        }
        if (def.tier.shield) {
            final Fluid wdm = wdmFluid(), bdm = bdmFluid();
            if (wdm != null) cap.merge(wdm, TOPUP_WDM * p * INTAKE_CHARGES, Math::max);
            if (bdm != null) cap.merge(bdm, TOPUP_BDM * p * INTAKE_CHARGES, Math::max);
        }
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

    /**
     * The "Last failure" line a controller carries in its item NBT (wrench, pick-up): a live star does not survive it
     * (the star is tile-only), so that line says so; otherwise the last failure as it stood (B-71).
     */
    public String pickupFailureLine() {
        if (!live()) return lastFailure;
        return (star == null ? "star" : star.name) + " put out: the controller was picked up"
            + (trans != null ? "; the " + trans.type.title + " was ABORTED" : "");
    }

    /** Item NBT only: the line {@link #pickupFailureLine} wrote, shown as the last event and failure once placed. */
    void restorePickupFailure(String line) {
        if (line == null || line.isEmpty()) return;
        lastFailure = line;
        lastEvent = line;
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
        galaxy.starOut();
        galaxy.beginRequested = false;
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
        if (from >= 6) {
            // P4 (quasar-p4.md §5.2, §5.3): the object lit and Stable, the receptor ports built, no companions
            if (star == null || star.n() != from || !burning() || star.tier.planets()) {
                out.add(from == 6 ? "the magnetar lit" : "Triangulum lit in galaxy mode");
            } else if (lastOut == null || lastOut.zone > 1) {
                out.add("the " + (from == 6 ? "magnetar" : "galaxy") + " Stable or better");
            }
            if (from == 6 && (host.receptorPorts(0) < 1 || host.receptorPorts(1) < 1)) {
                out.add("a GRB Receptor Port on each bank of the stability rail (rail-high and rail-low)");
            }
            if (from == 7 && host.receptorPorts(2) < HALO_PORTS) {
                out.add(
                    "all " + HALO_PORTS + " GRB Receptor Ports on the shard halo (" + host.receptorPorts(2) + " now)");
            }
            if (from == 7 && galaxy.companions.length > 0) {
                out.add("no companions (take the star charts out of the slot bus)");
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
        return transitionNeeds(
            type,
            Math.max(1, parallel(type.from)),
            destParallel(type.from + 1),
            feedSet,
            galaxy.f8Set);
    }

    /**
     * {@link #transitionNeeds(EyeTransition.Type)} for a new star at parallel {@code pd} and an HSM feed setpoint
     * {@code feed} (the NEI transition page uses p = 1 and the default feed).
     */
    public static Map<Fluid, Long> transitionNeeds(EyeTransition.Type type, int pd, double feed) {
        return transitionNeeds(type, pd, pd, feed, GalaxyField.F8_DEFAULT);
    }

    /** The Magnetar Collapse's charge per parallel: 300 s of WDM 61.44 + BDM 15.36 L/s. */
    public static final long MC_WDM = Math
        .round(MagnetarCollapseOverlay.WDM_PER_SECOND * MagnetarCollapseOverlay.OVERFEED_SECONDS),
        MC_BDM = Math.round(MagnetarCollapseOverlay.BDM_PER_SECOND * MagnetarCollapseOverlay.OVERFEED_SECONDS);
    /** The Galactic Merger's Ergomatter charge per parallel: 1,800 s at 5 L/s. */
    public static final long GM_ERGO = Math
        .round(GalacticMergerOverlay.CHARGE_PER_SECOND * GalacticMergerOverlay.DURATION);
    /** P4: the shard halo's Receptor Ports the Galactic Merger needs (all of them, §5.3). */
    public static final int HALO_PORTS = 8;

    /**
     * As above with P4's two: the charge of a galaxy transition is taken per second at the parallel of the object it
     * leaves ({@code ps}); the new object's first cycle at its own ({@code pd}); {@code f8} is the T8 feed setpoint.
     */
    public static Map<Fluid, Long> transitionNeeds(EyeTransition.Type type, int ps, int pd, double feed, double f8) {
        final Map<Fluid, Long> need = new LinkedHashMap<>();
        final StarDef dest = StarDef.byKey(type.dest);
        switch (type) {
            case MAGNETAR_COLLAPSE:
                need.merge(wdmFluid(), MC_WDM * ps, Long::sum);
                need.merge(bdmFluid(), MC_BDM * ps, Long::sum);
                break;
            case GALACTIC_MERGER:
                need.merge(SLQuasarFluids.ergomatter, GM_ERGO * ps, Long::sum);
                break;
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
            for (Map.Entry<Fluid, Double> up : dest.exactUpkeep.entrySet()) {
                need.merge(up.getKey(), (long) Math.ceil(up.getValue() * pd), Long::sum);
            }
            if (dest.feed != null) {
                final double rate = dest.tier.ergoFeed ? f8 : feed;
                need.merge(dest.feed, (long) Math.ceil(rate * StellarField.CYCLE_SECONDS * pd), Long::sum);
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
            case 7:
                return "stability rail";
            case 8:
                return "shard halo";
            default:
                return "galactic rim";
        }
    }

    /** PENDING: begins the Mass Infusion, Shell Ignition or Core Collapse the moment every precondition holds. */
    private void tryBeginFromPending() {
        final List<String> miss = missing(star.n());
        if (!miss.isEmpty()) {
            waitReason = miss.get(0);
            galaxy.beginRequested = false;
            return;
        }
        final EyeTransition.Type type = EyeTransition.Type.leaving(star.n());
        if (star.n() >= 6) {
            // P4: a galaxy transition begins on a signal (Galaxy Transition bit 1 is "ready: begin"), so the player
            // or the rig times it against the quasar's flux: the GUI's Begin, a Burst Control Hatch in Begin mode,
            // OpenComputers beginTransition()
            if (!galaxy.beginRequested) {
                waitReason = "READY: begin the " + type.title
                    + " (the GUI's Begin button, a Burst Control Hatch in Begin mode in a 'd' socket, or"
                    + " OpenComputers beginTransition())";
                return;
            }
            galaxy.beginRequested = false;
            final EyeTransition tr = beginTransition(type);
            tr.onBegin(fs);
            state = StarState.LIT;
            lastEvent = type.title + " begun"
                + (tr.coreUsed ? " (the " + escrow.getDisplayName() + " is consumed only if it succeeds)"
                    : " (re-run, no core)");
            logEvent(lastEvent);
            return;
        }
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
        trans = EyeTransition.create(type, bounceShock, collapseShock);
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
        final boolean newly = unlockedTier < to;
        unlockedTier = Math.max(unlockedTier, to);
        String coreNote = "";
        if (tr.coreUsed && escrow != null) {
            coreNote = "; the " + escrow.getDisplayName() + " was consumed";
            escrow = null;
        }
        lastEvent = tr.type.title + " COMPLETE in "
            + tr.t
            + " s: "
            + (newly ? "T" + to + " unlocked" : "re-run (T" + to + " was unlocked)")
            + coreNote;
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

    /**
     * A Magnetar chart on a dark Eye: the Magnetar Merger (the first spawn, and every re-spawn). P4: also a dark Eye
     * with a galaxy chart in galaxy mode ({@code why} says so): the galaxy is re-born from a lit magnetar.
     */
    private void tryMerger(StarDef want, String why) {
        if (unlockedTier < 5) {
            waitReason = "the magnetar needs T6: unlock T5 first (the First Cooling)";
            return;
        }
        final List<String> miss = missing(5);
        if (!miss.isEmpty()) {
            waitReason = "Magnetar Merger" + why + " waits: " + miss.get(0);
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
            + (tr.coreUsed ? " (the " + escrow.getDisplayName() + " is consumed only if it succeeds)" : " (re-spawn)")
            + why;
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
        if (def.tier.burstDriven()) {
            // P4: the galaxy (or the planets field) at zero, as the prototype's relight
            fs.setX(0, 0, 0, 0);
            galaxy.starOut();
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

    /**
     * Debug {@code crossing} (B-120): the next satellite crossing is due in its warning time, so its 120 s countdown
     * starts this second (a lit T8 galaxy only).
     */
    public String debugCrossing() {
        if (fs == null || !burning()) return "no star burns";
        if (fs.tde == null) return "T" + fs.tier.n + " has no satellite crossing";
        fs.tde.next = (double) fs.t + com.nateyburr.beyondthestars.machines.eye.field.GalaxyField.CROSSING_WARN;
        fs.tde.warned = false;
        fs.tde.left = 0;
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
                if (timeWanted()) c |= 8;
                return c;
            }
            // ---- P4 (quasar-p4.md §8.1; the bit rules are GalaxyDirective's, as the rig proof reads them)
            case EYE_FIELD:
                return fs == null ? 0
                    : GalaxyDirective
                        .field(galaxyLit(), fs.tier.planets(), fs.prevE[StellarField.P], fs.prevE[StellarField.W], w);
            case EYE_EVENTS:
                return fs == null ? 0
                    : GalaxyDirective.events(
                        galaxyLit(),
                        fs.tier.planets(),
                        fs.prevE[StellarField.L],
                        fs.prevWarn,
                        fs.tde != null && fs.tde.warned,
                        w);
            case EYE_GALAXY_TRANSITION: {
                final boolean running = galaxyTransition();
                final MagnetarCollapse mc = trans instanceof MagnetarCollapse ? (MagnetarCollapse) trans : null;
                final double[] flux = mc != null && mc.overfeed() ? linkedQuasarFlux() : new double[2];
                return GalaxyDirective.transition(
                    running,
                    galaxyReady(),
                    mc != null && mc.overfeed(),
                    mc != null ? mc.mc.st : 0,
                    flux[0],
                    flux[1]);
            }
            case EYE_SLOTS:
                return GalaxyDirective.slots(slotIdle(), companionShort(), galaxy.lastRunLost, testFireWanted());
            default:
                return 0;
        }
    }

    /**
     * Galaxy Transition bit 1: the magnetar (T6) or Triangulum (T7) is PENDING with the next galaxy chart in the slot
     * and every precondition holds (milestones, the core, the receptor ports, the charge, Stable): a begin signal
     * starts
     * it.
     */
    public boolean galaxyReady() {
        if (trans != null || state != StarState.PENDING || star == null || (star.n() != 6 && star.n() != 7)) {
            return false;
        }
        return missing(star.n()).isEmpty();
    }

    /**
     * Slots bit 4: the next link milestone is not held while its core and charge are in place, on a lit magnetar (the
     * Stability Link: J1 all to the Eye) or a lit galaxy-mode Triangulum (the Heavy Link: J1 all to the Eye, J3 75 %
     * to the halo). GalaxyDirective's rule.
     */
    public boolean testFireWanted() {
        if (fs == null || star == null || trans != null) return false;
        final int n = fs.tier.n;
        if (n != 6 && n != 7) return false;
        final Item core = coreFor(n);
        final boolean coreIn = escrow != null && escrow.getItem() == core;
        final EyeTransition.Type type = EyeTransition.Type.leaving(n);
        boolean buffered = type != null;
        if (type != null) {
            for (Map.Entry<Fluid, Long> need : transitionNeeds(type).entrySet()) {
                if (need.getKey() == null || host.buffer(need.getKey()) < need.getValue()) buffered = false;
            }
        }
        return GalaxyDirective
            .testFireWanted(n, fs.tier.planets(), burning(), false, milestones.linkDone[n], coreIn, buffered)
            && (n != 7 || heavyTestFireFits(linkedQuasarStream()));
    }

    /**
     * P4-88: the most mean stream the T7 halo test fire may take at J1 15 (Bu/t). The rail keeps a quarter, and above
     * about 8 Bu/t on the rail (β ≈ 5) the field's P drift outruns the binary Space valve: the prototype's T7 rig with
     * Mk II throats holds at 265 SM (33.6 Bu/t, 0 failures on 3 seeds) and starts to breach at 300 SM (43 Bu/t).
     */
    public static final double HEAVY_TEST_FIRE_MAX_STREAM = 32.0;
    /** P4-88: the Eye leg a hand-set test fire aims for on a quasar above the ceiling (rail 4, halo 12 Bu/t). */
    public static final double HEAVY_TEST_FIRE_EYE_LEG = 16.0;

    /** P4-79 / P4-88: the T7 test fire at J1 15 suits a mean stream from 6 to 32 Bu/t. */
    public static boolean heavyTestFireFits(double stream) {
        return stream >= GalaxyField.demandT(8) && stream <= HEAVY_TEST_FIRE_MAX_STREAM;
    }

    /**
     * P4-88: the J1 share to the Eye a test fire should use on this quasar: 1 up to the ceiling, else
     * {@link #HEAVY_TEST_FIRE_EYE_LEG} ÷ the mean stream (0 with no link).
     */
    public double heavyTestFireShare() {
        final double s = linkedQuasarStream();
        if (s <= 0) return 0;
        return s <= HEAVY_TEST_FIRE_MAX_STREAM ? 1.0 : HEAVY_TEST_FIRE_EYE_LEG / s;
    }

    /**
     * The Galaxy panel's line while the T7 Heavy Link test fire is held back by the quasar's size (P4-79 / P4-88), or
     * "" when it is not. Server state.
     */
    public String heavyTestFireNote() {
        if (fs == null || star == null || trans != null || star.n() != 7 || escrow == null || milestones.linkDone[7]) {
            return "";
        }
        final double s = linkedQuasarStream();
        if (s < GalaxyField.demandT(8)) {
            return String.format(
                Locale.ROOT,
                "Heavy Link test fire waits: the quasar's mean stream %.1f Bu/t is under 6 (grow it)",
                s);
        }
        if (s > HEAVY_TEST_FIRE_MAX_STREAM) {
            return String.format(
                Locale.ROOT,
                "Heavy Link test fire held: the quasar's mean stream %.1f Bu/t is over %.0f, and J1 at 15 would flood"
                    + " the rail. Test-fire with J1's Eye share at %.2f (J3 75 %% halo)",
                s,
                HEAVY_TEST_FIRE_MAX_STREAM,
                heavyTestFireShare());
        }
        return "";
    }

    /**
     * P4-79: the linked quasar's mean stream, Bu/t ({@code QuasarParams.meanStream} at its current G0 and Y_q, purging
     * at Φ 40), 0 with no link. The T7 halo test fire sends J1 all to the Eye and J3 75 % to the halo, so the rail
     * keeps a quarter: below the Heavy Link's 6 Bu/t it would starve Triangulum (P falls, Integrity drains) and the
     * link could not be held anyway, so Slots [8] waits for a quasar big enough (about 130 SM at coils 3).
     */
    public double linkedQuasarStream() {
        if (benchBursts != null && benchFlux != null) return benchFlux.length > 2 ? benchFlux[2] : 0;
        final BurstSource s = galaxy.bursts.sourceId;
        final MTEQuasarEngine q = s == null ? null : s.resolve();
        if (q == null || !q.isFormed()) return 0;
        return com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams
            .meanStream(q.state().G0, q.state().Yq, true);
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

    /** The trim table the Trims bits name now (bench rigs, scanner), or null with no field. */
    public TierParams directiveTrimTable() {
        return fs == null ? null : trimTable();
    }

    /** With the next core in escrow, the next transition's event fluids and first cycle are not all buffered. */
    private boolean transitionFluidsShort() {
        final Item core = coreFor(unlockedTier);
        final EyeTransition.Type type = EyeTransition.Type.leaving(unlockedTier);
        if (core == null || type == null || escrow == null || escrow.getItem() != core) return false;
        for (Map.Entry<Fluid, Long> need : transitionNeeds(type).entrySet()) {
            if (need.getKey() != null && host.buffer(need.getKey()) < need.getValue()) return true;
        }
        return false;
    }

    /**
     * Status bit 1: the buffers cannot pay the next cycle's upkeep (or this cycle ran short, r below 1), a dark Eye's
     * ignition (10 cycles and the seed cloud), a waiting Sirius B batch, the magnetar's dwarf-matter top-up, or - with
     * the next core in escrow - the next transition's event fluids and its new star's first cycle.
     */
    public boolean buffersShort() {
        if (transitionFluidsShort()) return true;
        if (live() && star != null) {
            if (cycR < 1 - 1e-9) return true;
            final int p = Math.max(1, cycP > 0 ? cycP : parallel(star.n()));
            for (EyeRecipe.FluidAmount up : star.upkeep) if (host.buffer(up.fluid) < up.amount * p) return true;
            if (star.feed != null && host.buffer(star.feed)
                < (long) Math.ceil((star.tier.ergoFeed ? galaxy.f8Set : feedSet) * StellarField.CYCLE_SECONDS * p))
                return true;
            for (Map.Entry<Fluid, Double> up : star.exactUpkeep.entrySet()) {
                if (host.buffer(up.getKey()) < Math.ceil(up.getValue() * p)) return true;
            }
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
            return ((MagnetarMerger) trans).agentWanted(fs.prevE[StellarField.PI]);
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

    /**
     * Transition bit 4: Tachyon Rich Temporal Fluid is wanted in the Field sockets - the Core Collapse is
     * fast-forwarding (it doses up to 100 L/s), or the time dial is set above 1 and last second could not pay for it.
     */
    public boolean timeWanted() {
        if (trans instanceof CoreCollapse) return ((CoreCollapse) trans).co.phase == 1;
        if (trans != null || !burning() || fs == null) return false;
        final double cap = Math.min(dialSet, StellarField.mMax(fs.tier, host.timeTier()));
        return cap > 1 + 1e-9 && lastM < cap - 1e-9;
    }

    /** P4-77: OpenComputers trim pulses for the next field second, units per parallel by kind (not saved). */
    private final java.util.EnumMap<TrimDef.Kind, Double> ocTrim = new java.util.EnumMap<>(TrimDef.Kind.class);

    /**
     * OpenComputers {@code trim(kind, units)} (quasar-p4.md §8.2, P4-77): the next field second takes up to
     * {@code units} (per parallel, capped at the trim's own cap) of one of the burning star's trims - "space", or any
     * trim by its key or name - from the stock sockets, on top of what the Field sockets hold, within the same cap.
     * Returns what was queued, or why not.
     */
    public String requestTrim(String kind, double units) {
        if (fs == null || star == null || !burning()) return "no star burning: nothing to trim";
        if (rig != null) return "the debug rig replaces the Field sockets";
        final String k = kind == null ? "" : kind.trim();
        for (TrimDef trim : trimTable().trims()) {
            if (!trim.kind.key.equalsIgnoreCase(k) && !trim.kind.displayName.equalsIgnoreCase(k)
                && !trim.kind.name()
                    .equalsIgnoreCase(k)) {
                continue;
            }
            final double u = Math.max(0, Math.min(trim.cap, Double.isNaN(units) ? 0 : units));
            ocTrim.put(trim.kind, u);
            return String
                .format(Locale.ROOT, "%s: %.1f units next second (cap %.0f)", trim.kind.displayName, u, trim.cap);
        }
        return "no trim '" + k + "' on this star (its trims: " + trimTable().trims() + ")";
    }

    /**
     * Transition bit 3: this tier's milestones are met (T6 / T7: all but the link) and the next core is not in escrow
     * yet.
     */
    public boolean coreWanted() {
        final Item need = coreFor(unlockedTier);
        // T6 / T7: wanted once all but the link is met - the link's test fire needs the core in place (P4-76)
        if (need == null || !milestones.metButLink(unlockedTier)) return false;
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
            case 6:
                return SLQuasarItems.intergalacticUpgradeCore;
            case 7:
                return SLQuasarItems.supermassiveSeedCore;
            default:
                return null;
        }
    }

    /** The transition event for step n → n+1. */
    public static String eventFor(int n) {
        final EyeTransition.Type t = EyeTransition.Type.leaving(n);
        return t == null ? "" : t.title;
    }

    /** True if {@code item} is one of the seven transition cores. */
    public static boolean isCore(Item item) {
        if (item == null) return false;
        for (int n = 1; n <= 7; n++) if (coreFor(n) == item) return true;
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

    // ======================================================================== GUI (read only)

    /** The recipe-result lang key prefix of the Eye's run checks (RC-2 looks them up under this). */
    public static final String RESULT_KEY = "GT5U.gui.text.recipe_result.beyondthestars.eye.";

    /** GUI: the wireless network's EU (0 without an owner). */
    public BigInteger networkEUForGui() {
        return networkEU();
    }

    /** GUI: the star a dark Eye lights for this chart from a Stellar Ignition Charge (T4: Arcturus), or null. */
    public StarDef ignitableFor(StarDef want) {
        return ignitable(want);
    }

    /** GUI: the zone of the last solved second (0 Resonant .. 4 Breach), or −1 with none. */
    public int lastZone() {
        return lastOut == null ? -1 : lastOut.zone;
    }

    /** GUI: T5, the running batch's Eye-seconds (0 without one). */
    public double batchEyeSeconds() {
        return fs == null || fs.batch == null ? 0 : fs.batch.eyeT;
    }

    /** GUI: T5, the cooling setpoint now (NaN without a batch). */
    public double batchSetpoint() {
        return fs == null || fs.batch == null ? Double.NaN : StellarField.spAt(fs.batch.eyeT);
    }

    /** GUI: T5, the batch's mean Y so far (NaN before its first second). */
    public double batchMeanY() {
        return fs == null || fs.batch == null || fs.batch.n <= 0 ? Double.NaN : fs.batch.ySum / fs.batch.n;
    }

    /** GUI: outputs are owed (a pre-D-49 run in flight, or a salvage made while unformed). */
    public boolean hasOwed() {
        return !legacyPay.isEmpty();
    }

    /**
     * GUI: the Core Collapse's bounce prediction during its fast-forward: {SpaceTime the bounce may absorb, P after,
     * Θ after, 1 if the HSM seed is buffered else 0}; null outside a Core Collapse. The scanner's numbers.
     */
    public double[] collapsePrediction() {
        if (!(trans instanceof CoreCollapse) || fs == null) return null;
        final double st = collapseSpaceTimeAvailable();
        final double[] after = ((CoreCollapse) trans).predictedAfterBounce(fs, st);
        return new double[] { st, after[0], after[1],
            host.buffer(SLFluids.hyperdenseStellarMixture) >= CollapseOverlay.HSM_SEED ? 1 : 0 };
    }

    /** GUI: the band that makes tier {@code tier} ("beam band", "jet crescent", ...). */
    public static String bandName(int tier) {
        return bandFor(tier);
    }

    /**
     * GUI (controller-gui-spec.md §5.1.4): every condition a dark Eye's ignition of {@code want} needs, met or not
     * ({@link #autoIgnite} stops at the first missing one). Reads only: the tiers, the charges in the plinth buses,
     * the buffers (10 cycles of upkeep plus the seed cloud, × p) and the wireless network (10 cycles of EU).
     */
    public BTSChecklist ignitionChecklist(StarDef want) {
        final BTSChecklist c = new BTSChecklist();
        if (want == null) return c;
        final int n = want.n();
        final int inst = host.installedTier();
        String why = "";
        if (host.structureTier() < 1) why = "tier1";
        else if (host.structureTier() < n) why = "tier" + n;
        else if (unlockedTier < n) why = "locked" + n;
        else if (host.compressionTier() < n) why = "compression" + n;
        c.add(
            inst >= n,
            BTSText.lang("beyondthestars.gui.eye.chk.reach", n),
            BTSText.lit("T" + inst + " / T" + n),
            -1,
            why.isEmpty() ? "" : BTSText.lang(RESULT_KEY + why));
        final int charges = host.countIgnitionCharges();
        c.add(
            charges >= 1,
            BTSText.lang("beyondthestars.gui.eye.chk.charge"),
            BTSText.lang("beyondthestars.gui.eye.chk.inbus", charges),
            -1,
            BTSText.lang("beyondthestars.gui.eye.chk.charge.tip"));
        final int p = Math.max(1, parallel(n));
        for (Map.Entry<Fluid, Long> need : ignitionNeeds(want, p).entrySet()) {
            final long have = host.buffer(need.getKey());
            final String name = fluidName(need.getKey());
            c.add(
                have >= need.getValue(),
                BTSText.lit(name),
                BTSText.lit(BTSFormat.haveNeed(have, need.getValue()) + " L"),
                BTSChecklist.frac(have, need.getValue()),
                BTSText.lang(
                    "beyondthestars.gui.eye.fluid.tip",
                    name,
                    BTSFormat.exact(have),
                    BTSFormat.exact(need.getValue())));
        }
        final BigInteger eu10 = BigInteger.valueOf(want.euPerCycle)
            .multiply(BigInteger.valueOf((long) p * StarDef.IGNITION_CYCLES));
        if (host.owner() == null) {
            c.add(
                false,
                BTSText.lang("beyondthestars.gui.eye.chk.eu"),
                BTSText.lang("beyondthestars.gui.eye.chk.noowner"),
                -1,
                BTSText.lang(RESULT_KEY + "noowner"));
        } else {
            final BigInteger have = networkEU();
            final boolean ok = have.compareTo(eu10) >= 0;
            c.add(
                ok,
                BTSText.lang("beyondthestars.gui.eye.chk.eu"),
                BTSText.lit(ok ? BTSFormat.eu(eu10) : BTSFormat.eu(have) + " / " + BTSFormat.eu(eu10)),
                ok ? -1 : (float) Math.max(0, Math.min(1, have.doubleValue() / eu10.doubleValue())),
                BTSText.lang("beyondthestars.gui.eye.chk.eu.tip", BTSFormat.eu(eu10)));
        }
        return c;
    }

    /**
     * GUI (controller-gui-spec.md §5.1.4, "Next batch"): every condition {@link #tryStartCooling} checks before a T5
     * cooling batch once T5 is unlocked, met or not: the Eye reaching T5 (a live {@link #parallel} of at least 1, the
     * same test the runtime makes) and one White Dwarf Matter charge × p in the buffers. Reads only.
     */
    public BTSChecklist batchChecklist() {
        final BTSChecklist c = new BTSChecklist();
        final int inst = host.installedTier();
        final int p = parallel(5);
        String why = "";
        if (host.structureTier() < 5) why = "tier5";
        else if (unlockedTier < 5) why = "locked5";
        else if (host.compressionTier() < 5) why = "compression5";
        c.add(
            p >= 1,
            BTSText.lang("beyondthestars.gui.eye.chk.reach", 5),
            BTSText.lit("T" + inst + " / T5"),
            -1,
            why.isEmpty() ? "" : BTSText.lang(RESULT_KEY + why));
        final Fluid wdm = wdmFluid();
        if (wdm == null) {
            c.add(false, BTSText.lang("beyondthestars.gui.eye.chk.nofluid"), "", -1);
            return c;
        }
        final long need = BATCH_WDM * Math.max(1, p);
        final long have = host.buffer(wdm);
        c.add(
            have >= need,
            BTSText.lit(fluidName(wdm)),
            BTSText.lit(BTSFormat.haveNeed(have, need) + " L"),
            BTSChecklist.frac(have, need),
            BTSText.lang("beyondthestars.gui.eye.batch.charge.tip", BTSFormat.exact(have), BTSFormat.exact(need)));
        return c;
    }

    /**
     * GUI (controller-gui-spec.md §5.1.4): {@link #missing(int)} turned inside out, the met lines kept: every
     * precondition of the transition that leaves tier {@code from}. Empty when there is none. Reads only.
     */
    public BTSChecklist transitionChecklist(int from) {
        final BTSChecklist c = new BTSChecklist();
        final EyeTransition.Type type = EyeTransition.Type.leaving(from);
        if (type == null) return c;
        final boolean firstTime = unlockedTier <= from;
        if (unlockedTier < from) {
            c.add(
                false,
                BTSText.lang("beyondthestars.gui.eye.chk.unlocked", from),
                BTSText.lit("T" + unlockedTier),
                -1);
        }
        final StarDef fromStar = StarDef.ofTier(from);
        final String starName = fromStar == null ? "T" + from : fromStar.name;
        if (firstTime) {
            final boolean met = milestones.met(from);
            c.add(
                met,
                BTSText.lang("beyondthestars.gui.eye.chk.milestones", starName),
                met ? BTSText.lang("beyondthestars.gui.eye.chk.met")
                    : BTSText.lang("beyondthestars.gui.kit.of", milestones.done(from), milestones.parts(from)),
                (float) milestones.fraction(from),
                BTSText.lit(milestones.progressLine(from)));
            final Item core = coreFor(from);
            if (core != null) {
                final boolean held = escrow != null && escrow.getItem() == core;
                c.add(
                    held,
                    BTSText.lit(new ItemStack(core).getDisplayName()),
                    BTSText
                        .lang(held ? "beyondthestars.gui.eye.chk.escrow.in" : "beyondthestars.gui.eye.chk.escrow.out"),
                    -1,
                    BTSText.lang("beyondthestars.gui.eye.chk.escrow.tip"));
            }
        }
        final String band = bandFor(from + 1);
        c.add(
            host.structureTier() >= from + 1,
            BTSText.lang("beyondthestars.gui.eye.chk.band", Character.toUpperCase(band.charAt(0)) + band.substring(1)),
            BTSText.lit("T" + (from + 1)),
            -1,
            BTSText.lang("beyondthestars.gui.eye.chk.band.tip", "T" + host.structureTier()));
        c.add(
            host.compressionTier() >= from + 1,
            BTSText.lang("beyondthestars.gui.eye.chk.compression"),
            BTSText.lit("T" + host.compressionTier() + " / T" + (from + 1)),
            -1,
            BTSText.lang(RESULT_KEY + "compression" + (from + 1)));
        if (from <= 3) {
            final boolean lit = star != null && star.n() == from && burning();
            final int zone = lastOut == null ? -1 : lastOut.zone;
            c.add(
                lit && zone >= 0 && zone <= 1,
                BTSText.lang("beyondthestars.gui.eye.chk.litstable", starName),
                !lit ? BTSText.lang("beyondthestars.gui.eye.dark")
                    : zone >= 0 ? BTSText.lang("beyondthestars.gui.zone." + Math.min(4, zone))
                        : BTSText.lang("beyondthestars.gui.eye.lit"),
                -1);
        }
        if (from >= 6) {
            // P4 (quasar-p4.md §5.2, §5.3; the same rules as missing()): the object lit in galaxy mode and Stable,
            // the receptor ports built, no companions
            final boolean lit = star != null && star.n() == from && burning() && !star.tier.planets();
            final int zone = lastOut == null ? -1 : lastOut.zone;
            c.add(
                lit && zone >= 0 && zone <= 1,
                BTSText.lang(
                    from == 6 ? "beyondthestars.gui.eye.chk.magnetarstable"
                        : "beyondthestars.gui.eye.chk.galaxystable"),
                !lit ? BTSText.lang("beyondthestars.gui.eye.dark")
                    : zone >= 0 ? BTSText.lang("beyondthestars.gui.zone." + Math.min(4, zone))
                        : BTSText.lang("beyondthestars.gui.eye.lit"),
                -1);
            if (from == 6) {
                final int hi = host.receptorPorts(0), lo = host.receptorPorts(1);
                c.add(
                    hi >= 1 && lo >= 1,
                    BTSText.lang("beyondthestars.gui.eye.chk.railports"),
                    BTSText.lit(hi + " / " + lo),
                    -1,
                    BTSText.lang("beyondthestars.gui.eye.chk.railports.tip"));
            } else {
                final int halo = host.receptorPorts(2);
                c.add(
                    halo >= HALO_PORTS,
                    BTSText.lang("beyondthestars.gui.eye.chk.haloports"),
                    BTSText.lit(halo + " / " + HALO_PORTS),
                    BTSChecklist.frac(halo, HALO_PORTS),
                    BTSText.lang("beyondthestars.gui.eye.chk.haloports.tip"));
                c.add(
                    galaxy.companions.length == 0,
                    BTSText.lang("beyondthestars.gui.eye.chk.nocompanions"),
                    BTSText.lit(String.valueOf(galaxy.companions.length)),
                    -1,
                    BTSText.lang("beyondthestars.gui.eye.chk.nocompanions.tip"));
            }
        }
        for (Map.Entry<Fluid, Long> need : transitionNeeds(type).entrySet()) {
            if (need.getKey() == null) {
                c.add(false, BTSText.lang("beyondthestars.gui.eye.chk.nofluid"), "", -1);
                continue;
            }
            final long have = host.buffer(need.getKey());
            final String name = fluidName(need.getKey());
            c.add(
                have >= need.getValue(),
                BTSText.lit(name),
                BTSText.lit(BTSFormat.haveNeed(have, need.getValue()) + " L"),
                BTSChecklist.frac(have, need.getValue()),
                BTSText.lang(
                    "beyondthestars.gui.eye.fluid.tip2",
                    name,
                    BTSFormat.exact(have),
                    BTSFormat.exact(need.getValue())));
        }
        return c;
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
        if (unlockedTier <= 7) out.add(milestones.progressLine(unlockedTier));
        if (unlockedTier >= 6 || (star != null && star.n() >= 6)) {
            out.add(
                "Galaxy: mode " + galaxy.mode.key
                    + ", T8 feed setpoint "
                    + f2(galaxy.f8Set)
                    + " L/s, companions "
                    + companionNames()
                    + "; receptor ports rail-high "
                    + host.receptorPorts(0)
                    + ", rail-low "
                    + host.receptorPorts(1)
                    + ", halo "
                    + host.receptorPorts(2));
        }
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
        if (T.n >= 6 || galaxyTransition()) galaxyLines(out, T);
        if (rig != null) out.add(
            "Debug rig: " + rig.name()
                .toLowerCase(Locale.ROOT) + " (replaces the Field sockets)");
    }

    /** P4 scanner lines: the bursts, β and the γs, the link, the T8 feed and crossing, companions, slots. */
    private void galaxyLines(List<String> out, TierParams T) {
        out.add(galaxy.bursts.line());
        if (T.burstDriven() || galaxyTransition()) out.add(galaxy.inputLine());
        final BurstLink l = activeLink();
        if (l != null) {
            final int n = linkTier();
            out.add(
                String.format(
                    Locale.ROOT,
                    "%s: streak %s / 30:00 (best %s), 60 s mean %.2f Bu/t, %s %.0f%%%s",
                    EyeMilestones.LINK_NAME[n],
                    EyeMilestones.clock(l.streak),
                    EyeMilestones.clock(Math.max(l.best, milestones.linkBest[n])),
                    l.mean,
                    n == 6 ? "high bank" : "halo",
                    100 * l.frac,
                    n == 6 ? " (needs >= 1.5 Bu/t, high bank 25-75 %)" : " (needs >= 6 Bu/t, halo >= 50 %)"));
        }
        if (T.ergoFeed) {
            final int[] c = crossings();
            out.add(
                String.format(
                    Locale.ROOT,
                    "Ergomatter feed f8: setpoint %.2f L/s, this second %.2f L/s per parallel; satellite crossings %d"
                        + " (caught %d)%s",
                    galaxy.f8Set,
                    in.f8,
                    c[0],
                    c[1],
                    crossingCountdown() >= 0 ? ", NEXT IN " + crossingCountdown() + " s"
                        : crossingLeft() > 0 ? ", CROSSING " + crossingLeft() + " s left" : ""));
        }
        if (T.galaxy) {
            out.add(
                "Precession sense " + (fs.prec < 0 ? "-" : "+")
                    + (mergerWarning() != 0 ? "; MINOR MERGER WARNED (" + (mergerWarning() < 0 ? "-" : "+") + ")"
                        : ""));
        }
        if (T.planets()) out.addAll(slotLines());
        if (!galaxy.lastRuns.isEmpty() && T.planets()) out.add("Last planet runs: " + galaxy.lastRuns);
        if (trans instanceof MagnetarCollapse && ((MagnetarCollapse) trans).overfeed()) {
            final MagnetarCollapse mc = (MagnetarCollapse) trans;
            out.add(
                String.format(
                    Locale.ROOT,
                    "  The shock would land at P +%.0f with %.2f Bu/t now; %.1f Bu/t on the collapse second cancels"
                        + " it all",
                    mc.predictedShock(galaxy.gEye),
                    galaxy.gEye,
                    mc.fullCancelBu()));
        }
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
