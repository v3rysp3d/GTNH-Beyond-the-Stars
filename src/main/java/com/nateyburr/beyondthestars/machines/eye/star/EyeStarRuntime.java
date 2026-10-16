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
import com.nateyburr.beyondthestars.items.SLMaxItems;
import com.nateyburr.beyondthestars.items.SLP5Items;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.GalaxyDirective;
import com.nateyburr.beyondthestars.machines.directive.P5Directive;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry;
import com.nateyburr.beyondthestars.machines.eye.field.AttractorField;
import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.EventDef;
import com.nateyburr.beyondthestars.machines.eye.field.EyeTrimRule;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.eye.field.MagnetarCollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.Overlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;
import com.nateyburr.beyondthestars.machines.eye.field.UniverseField;
import com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

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

        /** S-05: why the machine is switched off, shown as the wait reason ("" while it may work). */
        default String offReason() {
            return allowedToWork() ? "" : "the machine is switched off";
        }

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

        /**
         * QA-A06: the seconds left of the GT planet run in progress (T1-T6 planets, a batch run), 0 when none: one Eye
         * runs a planet or a live star, never both.
         */
        default long planetRunSecondsLeft() {
            return 0;
        }

        /** P5: the Pellet Injector(s) and their stock this field second ({@link PelletFeed#NONE} when none). */
        default PelletFeed pellets() {
            return PelletFeed.NONE;
        }

        /**
         * P5: takes up to {@code count} pellets of {@code grade} (1..3) from the injectors and the pellet stock buses
         * (ME stocking buses through one processing window, credited only for what the network gave up). Returns how
         * many were taken.
         */
        default long takePellets(int grade, long count) {
            return 0;
        }
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
    /** P5: the T9 / T10 state beside the field (the beam's inputs, the Deep Link, the universe charge). */
    final EyeAttractor attractor = new EyeAttractor();
    /** P4 bench: a scripted burst feed replacing the receptor reading (null in play; never saved). */
    EyeBursts benchBursts;
    /**
     * B-225: the bench's standard rig on this Eye, its bench quasar's mass (SM); 0 off. Tile NBT ({@link EyeStdRig}).
     */
    double benchStdRig;
    /** P4 bench: the bench quasar's {Φ, b} for the collapse assist (its source is not a machine); null in play. */
    double[] benchFlux;

    // ---------------------------------------------------------------- transient (scanner)
    private FieldReadout last = FieldReadout.DARK;
    private StepResult lastOut;
    private double lastM = 1, lastFeed;
    int lastP = 1;
    private int lastLiveP = 1;
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
        } else if (tr instanceof Infall) {
            p = ((Infall) tr).progress();
        } else if (tr instanceof Inflation) {
            p = ((Inflation) tr).progress();
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
        return trans != null && trans.type.from >= 6;
    }

    /** P4: a galaxy-tier field burns (T7 / T8, galaxy or planets mode; the collapse's capture included). */
    public boolean galaxyLit() {
        return burning() && fs != null && fs.tier.n >= 7;
    }

    /**
     * QA-A16: true while switching galaxy / planets mode would put the live field out: a T7 / T8 star or planets field
     * is lit, igniting or in a transition (T9 and T10 have no planets mode). The GUI asks for a second click then.
     */
    public boolean modeSwitchPutsOut() {
        return live() && star != null && star.n() >= 7 && StarDef.planetsOf(star.n()) != null;
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

    /**
     * Renders v2 (R-04), read only: the event channel for the centre render while an event is warned or hitting:
     * {@code {EyeStarLook.EV_* code, seconds from now to the hit (negative once it hit)}}, or null: the helium flash,
     * the glitch, the giant flare and (renders v2 WP-5) the galaxies' minor merger with its sign. The satellite
     * crossing has its own clock ({@link #renderCrossing}); the Galactic Merger's passes are added by the look.
     */
    public double[] renderEvent() {
        final FieldState s = fs;
        if (s == null || s.ev == null || !s.ev.warned || !burning()) return null;
        final EventDef def = (trans != null ? trans : Overlay.NONE).event(s.tier, s);
        if (def == null) return null;
        final int code;
        switch (def.name) {
            case "Helium flash":
                code = EyeStarLook.EV_HELIUM_FLASH;
                break;
            case "Glitch":
                code = EyeStarLook.EV_GLITCH;
                break;
            case "Giant flare":
                code = EyeStarLook.EV_GIANT_FLARE;
                break;
            case "Minor merger":
                // renders v2 WP-5: the dwarf falls in from the side of the disc the event's sign names
                code = s.ev.sign < 0 ? EyeStarLook.EV_MINOR_MERGER_DOWN : EyeStarLook.EV_MINOR_MERGER_UP;
                break;
            default:
                return null;
        }
        return new double[] { code, s.ev.next - s.t };
    }

    /**
     * Renders v2 WP-5, read only: the T8 satellite crossing for the centre render: seconds to its hit while its 120 s
     * countdown runs, minus the seconds since the hit during its 300 s; NaN when none.
     */
    public double renderCrossing() {
        final FieldState s = fs;
        if (s == null || s.tde == null || !burning()) return Double.NaN;
        if (s.tde.warned) return s.tde.next - s.t;
        if (s.tde.left > 0) return -(GalaxyField.CROSSING_DUR - s.tde.left);
        return Double.NaN;
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
        euShort = false;
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

    /** Debug: sets the unlocked tier (1..10), lowering it too. */
    public void setUnlocked(int n) {
        unlockedTier = Math.max(1, Math.min(maxUnlock(), n));
        legacyPending = false;
        host.markDirty();
    }

    /** The highest tier an Eye unlocks (P5: T10 the Universe). */
    public static final int MAX_UNLOCK = 10;

    /** The highest tier this server lets an Eye unlock: 10, or 8 with {@code enableEyeT9T10} off. */
    public static int maxUnlock() {
        return com.nateyburr.beyondthestars.Config.enableEyeT9T10 ? MAX_UNLOCK : 8;
    }

    /** P5: the live field (render, GUI), or null. Server state, read only. */
    public FieldState fieldState() {
        return fs;
    }

    /** P5: the T9 / T10 state (the beam's inputs, the Deep Link, the universe charge). Server state. */
    public EyeAttractor attractor() {
        return attractor;
    }

    /** P5: sets the flow accept limit (0-12), or −1 for the hold rating (GUI, OpenComputers). */
    public void setAcceptLimit(int n) {
        attractor.acceptSet = n < 0 ? -1 : Math.min(AttractorField.FLOW_MAX, n);
        host.markDirty();
    }

    /** Bench {@code set mcshock}: the shock of the next Magnetar Collapse (one running keeps its own). */
    public void setCollapseShock(double v) {
        collapseShock = Math.max(0, Math.min(240, v));
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
        if (state != StarState.PENDING || star == null || star.n() < 6 || star.n() > 9) {
            return "Nothing to begin: put the next chart in the controller slot while the "
                + (star == null || star.n() <= 6 ? "magnetar" : star.n() <= 8 ? "galaxy" : "Great Attractor")
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
            waitReason = "the structure is incomplete";
            return;
        }
        if (!host.allowedToWork()) {
            if (live()) extinguish("the machine was disabled");
            else idleOff();
            waitReason = host.offReason();
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
        final long planet = host.planetRunSecondsLeft();
        if (planet > 0 && want != null) {
            // QA-A06: a planet run is in progress: the star waits for it (one live star or one planet per Eye)
            waitReason = "a planet run is in progress (" + planet + " s left): the star lights after it";
        } else {
            tryAutoStart(want);
        }
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
        // ---- B-224: a universe that has not begun (age 0, its charge paid or owed) waits for bursts on the crown: lit
        // without them its P falls at 3/s and it breaches in about 30 s, taking the next charge with it (the D-54
        // relight's rule, for every universe)
        if (T.universe && trans == null && (fs.uni == null || fs.uni.t == 0) && !galaxy.bursts.linkedAll) {
            waitReason = "the next universe waits for bursts on the universal crown's GRB Receptor Ports (route a"
                + " linked quasar's stream to the crown; lit without bursts it would collapse in about 30 s)";
            host.publish(last);
            host.markDirty();
            return;
        }
        // ---- P5 T10: a universe still owed its Dark Matter charge waits for it (no drain, no field second)
        if (T.universe && trans == null && !payUniverseCharge()) {
            waitReason = "the next universe waits for its charge: " + fmt(attractor.uniOwed)
                + " L of Attractor Dark Matter";
            host.publish(last);
            host.markDirty();
            return;
        }
        if (T.universe && waitReason.startsWith("the next universe waits")) waitReason = "";
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
        retryCycleCharge(); // S-04: a cycle still owing its EU tries again
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
                // P5 T10: the Universe rig, one 2-unit valve each way on H (F-P5-2)
                if (rig != FieldRig.Mode.OFF && table.universe) {
                    if (fs.prevE[StellarField.H] > 5) raw.put("DM", UniverseField.RIG_VALVE);
                    if (fs.prevE[StellarField.H] < -5) raw.put("ST", UniverseField.RIG_VALVE);
                }
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
        // ---- P5: the pellet beam's inputs (T9, and the T8 Deep Link test fire) and the flow's accept limit
        p5Inputs(T);
        lastM = m;
        lastP = p;
        lastFeed = in.feed;

        // D-55: no Raw Star Matter is earned in a cycle that ends inside a transition
        final boolean transBefore = trans != null;
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
        // ---- P5: the pellets fired, the Infall's and Inflation's charges
        p5Charges(out, p);
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
        // B-223: a galaxy-tier failure names its cause (the gauge that broke and what it was short of)
        if (out.fail != null) out.fail = out.fail + failHint(T, out);
        if (out.fail != null && out.recovered) {
            // ---- P5: a soft-failed Infall / Inflation (the star below is lit), or a universe that collapsed
            p5Recovered(out, p);
        }
        if (out.fail != null && (!out.recovered || out.failEarly)) {
            if (!out.recovered) {
                if (T.batch) fracture(out.fail);
                else if (trans != null) failTransition(out.fail);
                else if (T.planets()) planetsBreach(out.fail);
                else fail(out.fail);
            }
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
            // ---- P4: the satellite crossing, the countdowns, the planet runs, the links
            galaxyAfterStep(T, out, m, p);
            p5AfterStep(out, p);
            watchFlash(fs.tier.n, out);
            // ---- the cycle: product, milestones, then charge the next one (it sets the next r)
            if (out.cycleEnded) {
                final int cn = fs.tier.n;
                if (out.hasCycleProduct) {
                    emit(out.cycleProduct);
                    if (cn <= unlockedTier) {
                        if (cn <= 3) milestones.prod[cn] += out.cycleProduct;
                        else if ((cn == 4 || cn >= 6) && cycWorst <= 1) milestones.prod[cn] += out.cycleProduct;
                    }
                    if (ledger != null) ledger.product += out.cycleProduct;
                    // P4: the companions' products (the galaxy's mean Y and r, each companion's own upkeep r)
                    if (out.companionCycle) companionProducts(fs.tier, out);
                }
                // D-55: Raw Star Matter at every lit tier (T2-T10, planets mode a quarter), each Eye-minute; T5 and
                // T10 too, beside their batches (no transition, no companion earns it; T1 makes it as its product)
                if (!transBefore && !out.transitionSuccess) rawStarMatter(fs.tier, out);
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
            // QA-A37: at a high time dial a flare can be warned inside the previous one's 30 s tail: that one ends
            // here and counts if it was ridden (it used to be dropped)
            if (flashState == 2 && flashOk) countRidden(n);
            flashState = 1;
            flashOk = true;
        }
        if (flashState == 0) return;
        if (out.zone > 1) flashOk = false;
        if (flashState == 1 && fs.ev != null && !fs.ev.warned) {
            flashState = 2;
            flashTail = FLASH_TAIL;
        } else if (flashState == 2 && --flashTail <= 0) {
            if (flashOk) countRidden(n);
            flashState = 0;
        }
    }

    private void countRidden(int n) {
        if (n == 3) milestones.flashes++;
        else if (n == 6) milestones.flares++;
        else milestones.mergers++;
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
        if (trans instanceof Infall) return ((Infall) trans).demand();
        if (trans instanceof Inflation) return ((Inflation) trans).demand();
        // P5: a galaxy companion (T9 only) weighs 2 in the demand (Q-P5-7)
        if (T.n >= 9) {
            return GalaxyField.demand(T.n, T.planets(), T.galaxy ? GalaxyField.companionWeight(galaxy.companions) : 0);
        }
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
     * Companions only in galaxy mode outside a transition (D-55: the Milky Way has no feed to measure).
     */
    private void galaxyInputs(TierParams T, double m, int p) {
        final EyeBursts b = galaxy.bursts;
        final boolean bursty = T.burstDriven() || galaxyTransition();
        if (!bursty && T.n != 6) {
            in.beta = in.gHi = in.gLo = in.gHalo = in.gEye = 0;
            in.companions = 0;
            galaxy.beta = galaxy.gHi = galaxy.gLo = galaxy.gHalo = galaxy.gEye = 0;
            galaxy.demand = galaxy.normDemand = 0;
            return;
        }
        final boolean haloCounts = trans instanceof GalacticMerger || T.planets() || (T.n >= 8 && T.galaxy);
        final double dump = haloCounts ? 0 : b.halo;
        // P5: the collar ports are part of the Eye from T9 (the Infall included); at T8 they take the Deep Link's
        // test fire and move nothing (the prototype's collar share leaves β)
        final double collarDump = T.n <= 8 && !(trans instanceof Infall) ? b.collar : 0;
        final double dn = Math.max(1e-9, normDemand(T) * m * p);
        in.beta = (b.total() - dump - collarDump) / dn;
        in.gHi = b.hi / dn;
        in.gLo = b.lo / dn;
        in.gHalo = (b.halo - dump) / dn;
        if (T.universe) in.gHi = in.gLo = in.gHalo = 0; // W, Λ and V retire at T10
        in.gEye = b.total() / p;
        in.companions = T.galaxy && trans == null ? galaxy.companions.length : 0;
        galaxy.beta = in.beta;
        galaxy.gHi = in.gHi;
        galaxy.gLo = in.gLo;
        galaxy.gHalo = in.gHalo;
        galaxy.gEye = in.gEye;
        galaxy.demand = galaxyDemand(T) * p;
        galaxy.normDemand = normDemand(T);
    }

    /**
     * The Magnetar Collapse's overfeed (WDM + BDM), × p, carried. D-55: the Galactic Merger has no charge; bursts alone
     * hold it.
     */
    private void takeCharges(StepResult out, int p) {
        if (out.wdmCharged > 0) take(wdmFluid(), galaxy.owe("charge:wdm", out.wdmCharged * p));
        if (out.bdmCharged > 0) take(bdmFluid(), galaxy.owe("charge:bdm", out.bdmCharged * p));
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
                "the magnetar COLLAPSED into a black hole: shock P %s -> %s (%s on the ports cancelled %.0f of"
                    + " %.0f); the halo capture begins (Stable for 60 s in a row within 420-600 s)",
                StellarField.fmtSigned(mc.shockP0),
                StellarField.fmtSigned(mc.shockP),
                BTSFormat.buEuShort(mc.shockG),
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
     * After a lit second that did not fail: the satellite crossing's events and bonus, the
     * countdowns, planets mode's runs, the companions' set, and the two link milestones.
     */
    private void galaxyAfterStep(TierParams T, StepResult out, double m, int p) {
        if (out.crossingWarned) lastEvent = "satellite crossing in 120 s: a dwarf galaxy plunges through the disc (P is"
            + " pushed for 300 s; stay Stable to catch it)";
        if (out.crossingHit) lastEvent = "satellite crossing: the dwarf galaxy is in the disc (300 s)";
        if (out.crossingCaught) {
            if (T.n == 8 && unlockedTier >= 8) milestones.crossings++;
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
        supermassiveSecond(b);
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
        } else if (T.n == 8 && T.galaxy && trans == null) {
            deepLinkSecond(b, p);
        }
    }

    // ===================================================================== P5: T9 the Great Attractor, T10 the
    // Universe

    /**
     * P5: the pellet beam's inputs this second (p5-t9-t10.md §3.3): the injector's barrel and stock, the grade (locked,
     * or the highest in stock), the rate law's centre (auto: ρ = 1 at V = 0) and gain, the level of its Burst Control
     * Hatch (the centre 8 without one; the debug rig wires the V sensor straight to it, window ±30), and the flow's
     * accept limit (the setting, or the hold rating).
     */
    private void p5Inputs(TierParams T) {
        final EyeAttractor a = attractor;
        final PelletFeed f = host.pellets();
        a.injectors = f.injectors;
        a.barrelMk = f.barrelMk;
        a.level = f.level;
        System.arraycopy(f.stock, 0, a.stockByGrade, 0, 4);
        final int g = f.grade();
        a.grade = g;
        a.stock = g > 0 ? f.stock[g] : 0;
        a.rhoMax = AttractorField.rhoMax(f.barrelMk, g);
        a.holdRating = AttractorField.holdRating(a.rhoMax);
        in.beamGeV = AttractorField.gev(g);
        in.beamCap = AttractorField.cap(f.barrelMk);
        in.beamCentre = f.centreAuto ? AttractorField.autoCentre(f.barrelMk, g) : f.centre;
        in.beamGain = f.gain;
        if (rig != null && rig != FieldRig.Mode.OFF) {
            in.beamLevel = AttractorField.analogLevel(fs.prevE[StellarField.V], AttractorField.V_WINDOW);
        } else {
            in.beamLevel = f.level >= 0 ? f.level : 8;
        }
        in.pelletsOk = g > 0 && f.barrelMk > 0 && a.stock > 0;
        in.acceptMax = a.acceptLimit();
        a.rate = in.pelletsOk ? AttractorField.rate(in.beamCap, in.beamCentre, in.beamGain, in.beamLevel) : 0;
        a.firing = false;
        a.unwired = f.unwired && (rig == null || rig == FieldRig.Mode.OFF);
        a.why = beamIdleReason(f, g, in.beamLevel);
        // B-222: the beam needed (V above +5 last second, at T9 or inside The Infall) but firing nothing
        final boolean needed = (T.attractor || trans instanceof Infall)
            && fs.prevE[StellarField.V] > AttractorField.TWO_LEVEL_W;
        final boolean wasFault = a.fault;
        a.fault = needed && a.rate <= 1e-9;
        if (a.fault && !wasFault) {
            lastEvent = String.format(
                Locale.ROOT,
                "BEAM NOT FIRING while V climbs (V %s): %s. An idle beam takes V to a Breach in about 50 s"
                    + " (Flow bit 4 is on)",
                StellarField.fmtSigned(fs.prevE[StellarField.V]),
                a.why);
            logEvent(lastEvent);
        }
    }

    /** Why the beam fires nothing at this level ("" when it fires): the first missing part, or the cut rate law. */
    private String beamIdleReason(PelletFeed f, int g, double level) {
        if (f.injectors < 1) return "no Pellet Injector Hatch on the top collar of the collimator stack";
        if (f.barrelMk < 1) return "no Injector Barrel in the Pellet Injector Hatch";
        if (g < 1 || f.stock[g] <= 0) {
            return f.lockedGrade > 0
                ? "no " + EyeAttractor.gradeName(f.lockedGrade)
                    + " pellets in stock (the injector is locked to that grade; screwdriver to unlock)"
                : "no Gated Pellets in the injector or a pellet stock bus";
        }
        if (attractor.rate > 1e-9) return "";
        return String.format(
            Locale.ROOT,
            "the injector's Burst Control Hatch reads %.0f, so pellets/s = cap x clamp(c + k (s/15 - 0.5)) is 0"
                + " (centre %.3f, gain %.2f): %s",
            level,
            in.beamCentre,
            in.beamGain,
            f.centreAuto && f.gain >= 0.5 ? "wire the V sensor (analog, window 30) or Flow bits 1 / 2 to it"
                : "set the injector's centre to auto and its gain to 1, and wire the V sensor (analog, window 30)"
                    + " or Flow bits 1 / 2 to it");
    }

    /**
     * B-223: what broke, in words, for a T7-T10 failure: the gauge at the breach (or the worst one when Integrity ran
     * out) and the input it was short of. "" when there is nothing clear to add (the gauge symbol is in the line).
     */
    private String failHint(TierParams T, StepResult out) {
        if (out.err == null || T.n < 7 || out.worstGauge < 0 || out.worstGauge >= out.err.length) return "";
        final int k = out.worstGauge;
        final double e = out.err[k];
        if (k == StellarField.P && e < 0) {
            return galaxy.beta < 1 ? String.format(
                Locale.ROOT,
                " (P fell: the GRB Receptor Ports got %.2f of the %.2f Bu/t this object needs, beta %.2f; only bursts"
                    + " push P up, so bring a linked quasar's stream big enough)",
                galaxy.bursts.total(),
                galaxy.demand,
                galaxy.beta) : " (P fell: steer more of the stream to the Eye with J1, the P loop)";
        }
        if (k == StellarField.P)
            return " (P rose: the Space valve, Spatially Enlarged Fluid on Field bit 1, pulls it down)";
        if (k == StellarField.V && attractor.unwired) {
            return " (V " + (e > 0 ? "ran away" : "fell")
                + ": the beam is unwired and fires open loop at its centre; wire the V sensor, analog, window 30, or"
                + " Flow bits 1 / 2, to the injector's Burst Control Hatch)";
        }
        if (k == StellarField.V && e > 0) {
            return attractor.rate <= 1e-9 ? " (V ran away: the beam fired nothing; " + attractor.why + ")"
                : String.format(
                    Locale.ROOT,
                    " (V ran away: the beam fired %.2f pellets/s and did not keep up; close the V loop: the V sensor,"
                        + " analog, window 30, or Flow bits 1 / 2, to the injector's Burst Control Hatch)",
                    attractor.rate);
        }
        if (k == StellarField.V) return " (V fell: the beam pushed too hard; Flow bit 2 or the V loop cuts it)";
        if (k == StellarField.L && e > 0) {
            return " (Λ ran away: the shard halo got too few bursts; J3 halo : rail on Events bits 1 / 2)";
        }
        if (k == StellarField.W) return " (W ran away: aim the rail with J2 high : low on Field bits 3 / 4)";
        if (k == StellarField.H) {
            return e > 0 ? " (H ran high: Attractor Dark Matter pulls it down, Universe bit 1)"
                : " (H ran low: Molten SpaceTime pushes it up, Universe bit 2)";
        }
        return "";
    }

    /** P5: the pellets fired (whole pellets drawn, the rest carried) and the Infall's / Inflation's charges, × p. */
    private void p5Charges(StepResult out, int p) {
        if (out.beamFired) firePellets(out.pellets * p);
        if (out.infallCharge > 0) {
            take(SLQuasarFluids.superdenseErgomatter, galaxy.owe("charge:sde", out.infallCharge * p));
        }
        if (out.inflationCharge > 0) {
            take(EyeAttractor.darkMatter(), galaxy.owe("charge:dm", out.inflationCharge * p));
        }
    }

    /** Draws the pellets the beam fired this second (fractional pellets carried; a short stock is simply not fired). */
    private void firePellets(double pellets) {
        final EyeAttractor a = attractor;
        a.firing = pellets > 0;
        a.pelletCarry += pellets;
        final long whole = (long) Math.floor(a.pelletCarry);
        if (whole <= 0 || a.grade <= 0) return;
        final long got = host.takePellets(a.grade, whole);
        a.pelletCarry -= whole; // what could not be taken is not fired twice
        if (ledger != null) ledger.pellets += got;
    }

    /**
     * P5: the failures the field handled itself. A soft-failed Infall / Inflation (Q-P5-5): the object below is lit
     * again with Integrity 50, the charge is spent, the core stays in escrow. A collapsed universe: 25 % × the fraction
     * run is salvaged as Universium, the next universe is charged, the Eye stays at T10.
     */
    private void p5Recovered(StepResult out, int p) {
        if (out.softFail) {
            final EyeTransition tr = trans;
            final boolean infall = tr instanceof Infall;
            lastEvent = (tr == null ? "transition" : tr.type.title) + " FAILED: "
                + out.fail
                + (infall ? "; the Milky Way is thrown back out of the filament, still lit at T8 (Integrity 50)"
                    : "; the new universe never formed: the Great Attractor is relit at T9 (Integrity 50)")
                + "; the charge is spent"
                + (escrow != null ? ", the " + escrow.getDisplayName() + " stays in escrow" : "")
                + ". It may begin again once its preconditions hold.";
            if (ledger != null) ledger.fails++;
            recordFailure(lastEvent);
            trans = null;
            galaxy.beginRequested = false;
            state = StarState.LIT;
            if (out.failEarly && out.relit) {
                prodCarry = 0;
                chargeCycle();
            }
            host.markDirty();
            return;
        }
        if (out.universeCollapsed) {
            final double salvage = out.universeSalvage * p;
            final Fluid uni = EyeAttractor.universium();
            final long whole = galaxy.earn(uni == null ? "universium" : uni.getName(), salvage);
            if (whole > 0 && uni != null) host.output(uni, whole);
            if (ledger != null) {
                ledger.fails++;
                ledger.product += salvage;
            }
            attractor.endT = fs.t;
            attractor.endKind = out.universeRip ? 2 : 3;
            // B-223: Big Rip / Big Crunch only when the expansion H broke; a P breach is the burst balance
            final boolean expansion = out.worstGauge == StellarField.H;
            lastEvent = String.format(
                Locale.ROOT,
                "the universe %s: %s; %,.0f L of Universium salvaged (25 %% x %.0f %% run). The Eye stays at T10;"
                    + " the next universe is charged",
                !expansion ? "COLLAPSED (its pressure balance broke, not its expansion)"
                    : out.universeRip ? "TORE ITSELF APART (Big Rip)" : "RECOLLAPSED (Big Crunch)",
                out.fail,
                salvage,
                100 * out.universeFraction);
            recordFailure(lastEvent);
            chargeUniverse(p);
            host.markDirty();
        }
    }

    /**
     * P5: a lit second's T9 / T10 events: flows, captured galaxies, universes, the dawn, the first universe's charge.
     */
    private void p5AfterStep(StepResult out, int p) {
        final EyeAttractor a = attractor;
        // D-55: the universe's field factor for its planets (restarted when a new universe begins its clock)
        if (fs.tier.universe && trans == null && fs.uni != null) {
            if (!out.universeDone && fs.uni.t == 1) {
                a.uniZf = 0;
                a.uniZn = 0;
            }
            a.uniZf += GalaxyField.PLANET_ZONE_FACTOR[Math.max(0, Math.min(4, out.zone))];
            a.uniZn++;
        }
        if (out.flowWarned) {
            final FieldState.Flow f = fs.flow;
            lastEvent = String.format(
                Locale.ROOT,
                "Galactic Flow in %d s: a group of %d galaxies down the %s-side filament; %s",
                AttractorField.FLOW_WARN,
                f.size,
                f.sign > 0 ? "high" : "low",
                f.accepted ? "accepted (raise the beam ahead of the peak)"
                    : "declined (larger than the accept limit " + a.acceptLimit() + "): it streams past");
            logEvent(lastEvent);
        }
        if (out.flowArrived) lastEvent = "Galactic Flow: " + fs.flow.size
            + " galaxies stream into the core (240 s; hold Stable to capture them)";
        if (out.flowPassed) {
            lastEvent = "the declined group streams past along the filament: nothing captured, nothing lost";
            a.passT = fs.t;
            a.passSize = fs.flow.size;
            a.passSign = fs.flow.sign;
        }
        if (out.flowEnded) {
            final double dm = out.flowDarkMatter * p;
            emitFluid(EyeAttractor.darkMatter(), dm);
            if (ledger != null) ledger.product += dm;
            milestones.galaxies += (long) out.flowCaptured;
            if (unlockedTier >= 9 && trans == null) {
                if (out.flowHeld) milestones.flowsHeld++;
                milestones.prod[9] += dm;
            }
            a.captureT = fs.t;
            a.captureN = (int) out.flowCaptured;
            a.lastFlow = out.flowHeld
                ? String
                    .format(Locale.ROOT, "held in full: %d galaxies captured, +%,.0f L Dark Matter", out.flowSize, dm)
                : String.format(
                    Locale.ROOT,
                    "partly held (%.0f %% Stable): %.0f of %d captured, +%,.0f L Dark Matter",
                    100 * out.flowFraction,
                    out.flowCaptured,
                    out.flowSize,
                    dm);
            lastEvent = "Galactic Flow " + a.lastFlow;
            logEvent(lastEvent);
        }
        if (out.galaxyCycle) milestones.galaxies++;
        // B-218: the quest bridge's T9 counts (Twenty EeV, A Wider Pull), on the unlocked tier outside a transition
        if (fs.tier.attractor && unlockedTier >= 9 && trans == null) {
            if (a.firing && a.grade == 3) milestones.superdenseSeconds++;
            if (out.cycleEnded && galaxy.companions.length >= 3) milestones.widePulls++;
        }
        if (out.inflated) {
            lastEvent = "INFLATION: the regions unfold into a universe inside the crown; W, Λ and V retire, H appears at +"
                + Math.round(fs.x[StellarField.H])
                + " (pull it down with Dark Matter)";
            logEvent(lastEvent);
        }
        if (out.recombinationWarned) lastEvent = "Recombination in 60 s: H drifts +0.15/s for 300 s, P drops for 5 s";
        if (out.recombination)
            lastEvent = "Recombination: the universe turns transparent (the CMB appears on the crown)";
        if (out.dawnWarned) lastEvent = "Cosmic Dawn in 120 s: the first stars ignite across the crown's twelve spikes";
        if (out.dawnHit) lastEvent = "COSMIC DAWN: the starburst floods the crown (P kicked, H +0.3/s for 300 s);"
            + " stay Stable for the +12.5 % Universium";
        if (out.universeDone) {
            final double litres = (out.universeBase + out.universeBonus) * p;
            emitFluid(EyeAttractor.universium(), litres);
            if (ledger != null) ledger.product += litres;
            milestones.universes++;
            if (out.universeDawnOk) milestones.dawns++;
            if (out.universeMeanY >= 1.0) milestones.flatUniverses++; // B-218: Keep It Flat
            a.endT = fs.t;
            a.endKind = 1;
            lastEvent = String.format(
                Locale.ROOT,
                "universe %d ran to its last light: +%,.0f L Universium (mean Y %.2f)%s; the next universe is charged",
                milestones.universes,
                litres,
                out.universeMeanY,
                out.universeDawnOk ? ", Cosmic Dawn caught (+12.5 %)" : "");
            logEvent(lastEvent);
            universeYield(out, p);
            chargeUniverse(p);
        }
        if (out.uniChargedInTick) chargeUniverse(p);
    }

    /**
     * D-55: one cycle's Raw Star Matter ({@link RawStarMatterYield}): N × 50,000 L × p × the cycle's mean Y × r_min, a
     * quarter in planets mode, none at T1 (its product already is Raw Star Matter). Out of the output hatches with a
     * carry; what they cannot hold is voided, as every Eye output.
     */
    private void rawStarMatter(TierParams T, StepResult out) {
        final double litres = RawStarMatterYield.cycle(T, cycP, out.cycleMeanY, out.cycleRMin);
        if (litres <= 0) return;
        emitFluid(rspmFluid(), litres);
        lastRspm = litres;
        rspmMade += litres;
        if (ledger != null) ledger.rspm += litres;
    }

    /** D-55: the last cycle's Raw Star Matter, litres (0 none yet); the litres made since the controller loaded. */
    double lastRspm, rspmMade;

    /** D-55: the last cycle's Raw Star Matter byproduct, litres (GUI). */
    public double lastRawStarMatter() {
        return lastRspm;
    }

    /**
     * D-55: a finished universe's yield ({@link UniverseYield}): the star matter × p × mean Y × r_min, and every
     * planet the filter lets through at parallel 2^(10 − k), each parallel rolled at (1 − 0.05·R) × the batch's field
     * factor. Fluids go to the output hatches (overflow voided), items to the ME output bus. No H/He, no EU.
     */
    private void universeYield(StepResult out, int p) {
        final EyeAttractor a = attractor;
        final UniverseYield.Selection sel = UniverseYield.select(a.uniStars, a.uniPlanets, host.slotBusItems());
        final double k = Math.max(1, p) * out.universeMeanY * out.universeRMin;
        double starLitres = 0;
        for (UniverseYield.Row row : sel.stars) {
            final Fluid f = row.fluid();
            if (f == null) continue;
            final double litres = row.litres * k;
            emitFluid(f, litres);
            starLitres += litres;
        }
        final double field = a.uniZn > 0 ? a.uniZf / a.uniZn : 1;
        long runs = 0, ok = 0;
        for (EyeRecipe r : sel.planets) {
            final long par = UniverseYield.planetParallel(r);
            final double chance = UniverseYield.planetChance(r, field);
            long won = 0;
            for (long i = 0; i < par; i++) if (PLANET_RNG.nextDouble() < chance) won++;
            planetOutputs(r, won);
            runs += par;
            ok += won;
        }
        a.uniZf = 0;
        a.uniZn = 0;
        a.lastYield = String.format(
            Locale.ROOT,
            "%s: %,.0f L of star matter, %,d of %,d planet parallels succeeded (field factor %.2f)",
            UniverseYield.summary(sel),
            starLitres,
            ok,
            runs,
            field);
        if (ledger != null) {
            ledger.uniStarMatter += starLitres;
            ledger.uniPlanetRuns += ok;
        }
        logEvent("universe yield: " + a.lastYield);
    }

    /** D-55: the universe's yield toggles (GUI, OpenComputers); saved with the Eye (tile and item NBT). */
    public void setUniverseYield(boolean stars, boolean planets) {
        attractor.uniStars = stars;
        attractor.uniPlanets = planets;
        host.markDirty();
    }

    /** D-55: what the next universe yields under the toggles and the slot bus, as one line (GUI, scanner, OC). */
    public UniverseYield.Selection universeSelection() {
        return UniverseYield.select(attractor.uniStars, attractor.uniPlanets, host.slotBusItems());
    }

    /** P5: a new universe owes its Dark Matter charge (9,216 L × p); taken now if the buffer holds it. */
    private void chargeUniverse(int p) {
        attractor.uniOwed += EyeAttractor.universeCharge() * p;
        payUniverseCharge();
    }

    /** P5: pays what the universe owes from the Dark Matter buffer; true when nothing is owed. */
    private boolean payUniverseCharge() {
        if (attractor.uniOwed <= 0) return true;
        final long got = take(EyeAttractor.darkMatter(), attractor.uniOwed);
        attractor.uniOwed -= got;
        if (attractor.uniOwed <= 0) {
            attractor.uniOwed = 0;
            return true;
        }
        return false;
    }

    /** Emits litres of a fluid with a carry (whole litres out of the ME output hatch). */
    private void emitFluid(Fluid fluid, double litres) {
        if (fluid == null || litres <= 0) return;
        final long whole = galaxy.earn(fluid.getName(), litres);
        if (whole > 0) host.output(fluid, whole);
    }

    /**
     * P5: one T8 second of the Deep Link (p5-t9-t10.md §2.8; the prototype's {@code deepLinkTick}): while the test fire
     * is wanted (Slots bit 4: the link not held, the Attractor Seed Core and the charge in place) the injector fires
     * the beam (no field effect at T8); the window rolls the Eye's total with its collar part and the beam's ρ; 1,800 s
     * unbroken at 24 Bu/t, half on the collars and ρ ≥ 1 holds it.
     */
    private void deepLinkSecond(EyeBursts b, int p) {
        if (testFireWanted() && attractor.injectors > 0) {
            final double n = AttractorField.beamTick(fs, in);
            firePellets(n * p);
        }
        final com.nateyburr.beyondthestars.machines.quasar.engine.DeepLink d = attractor.deep;
        d.roll(b.total() / p, b.collar / p, fs.beam == null ? 0 : fs.beam.rho);
        if (!milestones.linkDone[8] && unlockedTier >= 8) {
            if (d.update(d.ok(b.linkedAll && b.collarPorts > 0)) || d.link.streak >= BurstLink.STREAK_SECONDS)
                linkHeld(8);
        }
        milestones.linkStreak[8] = d.link.streak;
        milestones.linkBest[8] = Math.max(milestones.linkBest[8], d.link.best);
    }

    /**
     * QA-B09: how long the Supermassive Link source may sit under 65,536 SM, its lattice in force and the link up,
     * before the streak resets: the Accretion Buffer's 60 s.
     */
    public static final int SM_DIP_GRACE = BurstLink.WINDOW;

    /**
     * SMBH (smbh-v1.md §1.3): one second of the Supermassive Link, P5's T9 → T10 machine milestone: a
     * 60 s mean of everything on the Eye's ports, and a streak while the link source (its stream record: a lattice in
     * force and at least 65,536 SM) qualifies and the mean is at least D₁₀ = 96 Bu/t. It resets when the link drops,
     * the lattice is lost, or the hole stays under 65,536 SM for longer than {@link #SM_DIP_GRACE} seconds (QA-B09: a
     * one-second supply gap dips the hole under the line for a second or two, which the 60 s Accretion Buffer is there
     * to ride, so a short dip counts as held). Held after 1,800 s, recorded in the milestones (item NBT) for P5;
     * Inflation needs it held (EyeMilestones.met(9)).
     */
    private void supermassiveSecond(EyeBursts b) {
        final BurstLink l = galaxy.link9;
        final boolean qualifies = b.supermassiveSource();
        final boolean dip = !qualifies && b.linkedAll && b.smLattice > 0 && galaxy.smDip < SM_DIP_GRACE;
        galaxy.smDip = qualifies ? 0 : dip ? galaxy.smDip + 1 : SM_DIP_GRACE;
        final boolean held = qualifies || dip;
        l.roll(b.total(), b.linkedAll && held ? b.total() : 0);
        if (l.update(l.supermassiveOk(b.linkedAll, held)) && !milestones.smLinkDone) {
            milestones.smLinkDone = true;
            lastEvent = "the " + EyeMilestones.SM_LINK_NAME
                + " is HELD: 30 min unbroken from the hole at "
                + b.source
                + " (the core of the T9 -> T10 milestones)";
            logEvent(lastEvent);
        }
        if (l.done && !milestones.smLinkDone) milestones.smLinkDone = true;
        milestones.smLinkStreak = l.streak;
        milestones.smLinkBest = Math.max(milestones.smLinkBest, l.best);
    }

    /**
     * SMBH: the Supermassive Link's line for the scanner, GUI and OpenComputers: the streak, the mean against 96 Bu/t
     * and the link source's record.
     */
    public String supermassiveLinkLine() {
        final BurstLink l = galaxy.link9;
        final EyeBursts b = galaxy.bursts;
        return String.format(
            Locale.ROOT,
            "%s (T9 -> T10 milestone): %s, 60 s mean %s / %s; source %s",
            EyeMilestones.SM_LINK_NAME,
            milestones.smLinkDone ? "HELD"
                : "streak " + EyeMilestones.clock(l.streak)
                    + " / 30:00 (best "
                    + EyeMilestones.clock(milestones.smLinkBest)
                    + ")",
            BTSFormat.buEuShort(l.mean),
            BTSFormat.buEuShort(BurstLink.supermassiveMean()),
            supermassiveSourceText(b));
    }

    /** SMBH: "a 70,000 SM hole, lattice L1 (qualifies)", "not linked", ... from the link source's stream record. */
    public static String supermassiveSourceText(EyeBursts b) {
        if (!b.linkedAll) return "not linked";
        if (b.smMass <= 0) return "no lattice record (a scripted feed)";
        return String.format(
            Locale.ROOT,
            "a %,.0f SM hole, %s (%s)",
            b.smMass,
            b.smLattice > 0 ? "lattice L" + b.smLattice : "no lattice",
            b.supermassiveSource() ? "qualifies" : "needs a lattice and 65,536 SM");
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
            if (d == null || d.n() > GalaxyField.maxCompanionTier(T.n) || d.n() > unlockedTier || out.contains(d.n()))
                continue;
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
            case "Magnetar Matter":
                return SLFluids.magnetarMatter;
            case "Ergomatter":
                return SLQuasarFluids.ergomatter;
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
        // D-54 (B-217): with T7 unlocked a dark Eye relights Triangulum at T7, the base of the intergalactic band, from
        // a new Magnetar Collapse charge (no magnetar re-spawn, no Magnetar Collapse re-run); with T10 unlocked the
        // Universe chart begins a new universe from a new Dark Matter charge
        final StarDef base = baseRelight(want);
        if (base != null) {
            tryBaseRelight(base, want);
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
        return n == 7 ? "Magnetar Collapse" : n == 8 ? "Galactic Merger" : "Infall";
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
        // QA-A09: each wanted planet keeps the run of the same planet it had (by key, in order), wherever it now
        // stands in the bus; only a planet that is new gets a new run, and only a run whose planet left is abandoned
        // (taking planet #1 out used to restart every run behind it, their inputs and EU spent)
        final List<EyeGalaxy.PlanetRun> old = new ArrayList<>();
        for (EyeGalaxy.PlanetRun r : galaxy.slots) if (r != null) old.add(r);
        final EyeGalaxy.PlanetRun[] next = new EyeGalaxy.PlanetRun[max];
        for (int i = 0; i < max && i < planets.size(); i++) {
            for (int k = 0; k < old.size(); k++) {
                if (old.get(k).key.equals(planets.get(i).key)) {
                    next[i] = old.remove(k);
                    break;
                }
            }
        }
        galaxy.slots = next;
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
            if (run == null) {
                run = new EyeGalaxy.PlanetRun(want.key); // a new planet: a new run
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
        final FieldState.Flow f = fs == null ? null : fs.flow;
        final boolean flowOn = f != null && fs.tier.attractor && (f.warned || f.left > 0);
        return r
            .withGalaxy(
                galaxy.beta,
                l == null ? 0 : l.progress(),
                l == null ? galaxy.bursts.mean60 : l.mean,
                slotProgress(),
                countdownSeconds(),
                fs != null && fs.tier.planets())
            .withP5(
                fs == null || fs.beam == null ? 0 : fs.beam.rho,
                AttractorField.gev(attractor.grade) / 1e9,
                attractor.holdRating,
                flowOn ? f.size : 0,
                milestones.galaxies,
                fs != null && fs.uni != null && fs.tier.universe ? fs.uni.t / (double) UniverseField.BATCH : 0,
                attractor.deep.link.progress());
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
     * Debits the cycle's EU and takes its upkeep (plan §6.2); r is the worst delivered fraction. T6 also spends a
     * cycle of the dwarf-matter reserve (topping it up from the buffers when low) and returns Tengam for the Agent.
     * Resets the cycle's worst zone (T4 milestone).
     * <p>
     * S-04: a failed EU debit makes r = 0 and takes NO upkeep: the cycle waits for its EU ({@link #euShort}), and
     * every second the debit is tried again ({@link #retryCycleCharge}). Once it goes through the upkeep is taken and
     * the cycle runs on at the delivered r; before, one EU dip of the wireless network put an unattended Sol out
     * about 39 s later with its upkeep spent.
     */
    void chargeCycle() {
        final int p = Math.max(1, parallel(star.n()));
        cycP = p;
        lastP = p;
        cycWorst = 0;
        if (!debit(cycleEu(p))) {
            euShort = true;
            cycR = 0;
            lastEvent = "the wireless network could not pay this cycle's " + sci(cycleEu(p))
                + " EU: no upkeep taken, P sags; the Eye tries again every second";
            host.markDirty();
            return;
        }
        euShort = false;
        cycR = takeUpkeep(p);
        host.markDirty();
    }

    /** One cycle's EU at parallel p. */
    private BigInteger cycleEu(int p) {
        return BigInteger.valueOf(star.euPerCycle)
            .multiply(BigInteger.valueOf(p));
    }

    /** S-04: the running cycle still owes its EU (the debit failed); its upkeep is not taken yet. Tile NBT. */
    boolean euShort;

    /**
     * S-04: once a second while the cycle owes its EU: the debit again; when it goes through, the upkeep is taken and
     * the rest of the cycle runs at the delivered r (the cycle's product counts from that r, not the 0 of the seconds
     * it waited: those seconds only sagged P).
     */
    private void retryCycleCharge() {
        if (!euShort || star == null) return;
        final int p = Math.max(1, cycP);
        if (!debit(cycleEu(p))) return;
        euShort = false;
        cycR = takeUpkeep(p);
        if (fs != null && fs.cyc != null) fs.cyc.rMin = cycR;
        lastEvent = "the cycle's EU was paid " + Math.round(fs == null || fs.cyc == null ? 0 : fs.cyc.eyeT)
            + " s late: upkeep taken, r "
            + f2(cycR);
        host.markDirty();
    }

    /** Takes one cycle's upkeep at parallel p (plus the companions' and T6's dwarf matter); returns the cycle's r. */
    private double takeUpkeep(int p) {
        double r = 1;
        for (EyeRecipe.FluidAmount up : star.upkeep) {
            final long need = up.amount * p;
            final long got = Math.min(host.buffer(up.fluid), need);
            if (got > 0) host.takeBuffer(up.fluid, got);
            if (up.fluid == SLFluids.antipolarizationAgent) creditAgent(got);
            r = Math.min(r, need <= 0 ? 1 : got / (double) need);
        }
        // P4: fractional upkeep, drawn with a carry (none since D-55: Triangulum's Magnetar Matter is gone)
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
        return r;
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
        // D-54 (B-217): once T4 is unlocked, a dark Eye holding the Neutron Star chart relights the neutron star itself
        // at T4, the base of its band, from a Stellar Ignition Charge and the 5,000 L HSM neutron seed (no Arcturus,
        // no Core Collapse re-run).
        final StarDef base = baseRelight(want);
        if (base != null) {
            tryBaseRelight(base, want);
            return;
        }
        // QA-A02: the chart one tier above the unlocked one (Sirius A on T1, Arcturus on T2, the Neutron Star on T3):
        // a Mass Infusion, Shell Ignition or first Core Collapse that failed or was aborted left the Eye dark for good
        // with that chart in. The star below relights, burns PENDING and the transition begins again once its
        // preconditions hold (the core stays in escrow).
        final StarDef lights = ignitable(want);
        if (lights == null) {
            waitReason = "the neutron star is born from a lit Arcturus by the Core Collapse (light Arcturus, then put the"
                + " Neutron Star chart in while it burns; once T4 is unlocked the Eye relights the neutron star itself"
                + " from a charge)";
            return;
        }
        autoIgnite(lights, lights == want ? "" : relightWhy(want));
    }

    /** Why a dark Eye lights the star below the chart's: " (to re-run the Core Collapse ...)", or a first run. */
    private String relightWhy(StarDef want) {
        return " (the " + eventFor(want.n() - 1) + " toward " + want.name + " begins once it burns)";
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
        final String to = why.isEmpty() ? "" : "; " + want.name + " relights" + why;
        if (!host.takeIgnitionCharge(true)) {
            waitReason = "no Stellar Ignition Charge in a plinth input bus" + to;
            return;
        }
        for (Map.Entry<Fluid, Long> need : ignitionNeeds(want, p).entrySet()) {
            if (host.buffer(need.getKey()) < need.getValue()) {
                waitReason = "the buffers need " + fmt(need.getValue())
                    + " L of "
                    + fluidName(need.getKey())
                    + " (10 cycles of upkeep plus the seed cloud)"
                    + to;
                return;
            }
        }
        final BigInteger eu10 = BigInteger.valueOf(want.euPerCycle)
            .multiply(BigInteger.valueOf((long) p * StarDef.IGNITION_CYCLES));
        if (networkEU().compareTo(eu10) < 0) {
            waitReason = "the wireless network needs " + sci(eu10) + " EU (10 cycles)" + to;
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

    // ===================================================================== D-54: the band's base relight

    /**
     * D-54 (B-217): the base tier of a star tier's band, where a lost star of that band relights: T4 for the galactic
     * band (T4-T6), T7 for the intergalactic band (T7-T9), T10 for the universal band. 0 for T1-T3 (they relight at
     * their own tier from a Stellar Ignition Charge, as built).
     */
    public static int bandBase(int n) {
        return n >= 10 ? 10 : n >= 7 ? 7 : n >= 4 ? 4 : 0;
    }

    /**
     * D-54 (B-217): the star a dark Eye relights straight from a new charge for this chart, at the base of the
     * chart's band, without re-running the chain below the base; null when the rule does not apply.
     * <ul>
     * <li>The Neutron Star chart, T4 unlocked: the neutron star itself (no Arcturus, no Core Collapse).</li>
     * <li>Sirius B and the magnetar (T5, T6): null. Neither needs a lit neutron star: Sirius B runs its next batch
     * from a White Dwarf Matter charge and the magnetar re-merges from the merger charge, so a lost T5 or T6 already
     * relights inside the band from a charge and never falls below T4.</li>
     * <li>A Triangulum, Milky Way or Great Attractor chart (galaxy mode), T7 unlocked and the chart at most one tier
     * past the unlocked one: Triangulum (no magnetar, no Magnetar Collapse). The Galactic Merger and The Infall then
     * re-run from it on the Begin signal, as every galaxy transition does.</li>
     * <li>The Universe chart, T10 unlocked: a new universe. Before T10 is unlocked it walks the T7 chain, as
     * above.</li>
     * </ul>
     * Planets mode lights its field at once and never needs this.
     */
    StarDef baseRelight(StarDef want) {
        if (want == null || want.tier.planets()) return null;
        final int n = want.n();
        if (n == 4) return unlockedTier >= 4 ? want : null;
        if (n == 10 && unlockedTier >= 10) return want;
        if (n < 7 || galaxy.mode == EyeGalaxy.Mode.PLANETS) return null;
        if (unlockedTier < 7 || n > unlockedTier + 1) return null;
        return StarDef.ofTier(7);
    }

    /**
     * GUI / controller: {@link #baseRelight} for the chart in the slot (the chart's star; null when it does not apply).
     */
    public StarDef baseRelightFor(StarDef chart) {
        return baseRelight(chart);
    }

    /** D-54: what a base relight takes as its charge, in words (wait lines, the failure line). */
    public static String baseChargeName(int base) {
        if (base == 4) return "Stellar Ignition Charge and the 5,000 L Hyperdense Stellar Mixture neutron seed";
        if (base == 7) return "Magnetar Collapse charge (White and Black Dwarf Matter)";
        return "universe charge (Attractor Dark Matter)";
    }

    /**
     * D-54: the buffered litres a base relight needs at parallel {@code p}: its charge (T4: the Core Collapse's HSM
     * neutron seed, 5,000 L; T7: the Magnetar Collapse's charge, WDM + BDM; T10: one universe's Dark Matter) PLUS
     * the new star's first cycle (upkeep, a fractional upkeep if any, T4's HSM feed at {@code feed} for one
     * cycle), so the relit star never starts at r = 0. All × p. T4 also takes one Stellar Ignition Charge (an item).
     */
    public static Map<Fluid, Long> baseRelightNeeds(StarDef base, int p, double feed) {
        final Map<Fluid, Long> out = new LinkedHashMap<>();
        if (base == null) return out;
        final int n = base.n();
        if (n == 4) {
            out.merge(SLFluids.hyperdenseStellarMixture, (long) Math.ceil(CollapseOverlay.HSM_SEED * p), Long::sum);
        } else if (n == 10) {
            final Fluid dm = EyeAttractor.darkMatter();
            if (dm != null) out.merge(dm, EyeAttractor.universeCharge() * p, Long::sum);
        } else {
            for (EyeRecipe.FluidAmount ch : base.charge) {
                if (ch.fluid != null) out.merge(ch.fluid, ch.amount * p, Long::sum);
            }
        }
        for (EyeRecipe.FluidAmount up : base.upkeep) {
            if (up.fluid != null) out.merge(up.fluid, up.amount * p, Long::sum);
        }
        for (Map.Entry<Fluid, Double> up : base.exactUpkeep.entrySet()) {
            if (up.getKey() != null) out.merge(up.getKey(), (long) Math.ceil(up.getValue() * p), Long::sum);
        }
        if (base.feed != null) {
            out.merge(base.feed, (long) Math.ceil(feed * StellarField.CYCLE_SECONDS * p), Long::sum);
        }
        return out;
    }

    /** {@link #baseRelightNeeds(StarDef, int, double)} at this Eye's parallel and HSM feed setpoint. */
    public Map<Fluid, Long> baseRelightNeeds(StarDef base) {
        return baseRelightNeeds(base, Math.max(1, parallel(base.n())), feedSet);
    }

    /** D-54: the wireless EU a base relight needs: the new star's first cycle, × p. */
    public BigInteger baseRelightEu(StarDef base) {
        return BigInteger.valueOf(base.euPerCycle)
            .multiply(BigInteger.valueOf(Math.max(1, parallel(base.n()))));
    }

    /**
     * D-54 (B-217): a dark Eye relights {@code base} (T4, T7 or T10) from a new charge when everything is there;
     * otherwise sets the wait reason, naming the first thing missing. The relit star is LIT at once: the gauges at
     * 0, Integrity 100, a fresh cycle charged (T10: a new universe, its charge paid). Milestones, unlocked tiers,
     * the escrowed core and the counters are untouched; with a chart above the base the star is PENDING toward it
     * and each transition above re-runs (no milestones, no core once that tier is unlocked).
     */
    private void tryBaseRelight(StarDef base, StarDef want) {
        final int n = base.n();
        final String to = base == want ? ""
            : "; then each transition above it re-runs toward " + want.name + " (Begin, no core)";
        final int p = parallel(n);
        if (p < 1) {
            waitReason = "the Eye does not reach T" + n
                + " (bands T"
                + host.structureTier()
                + ", compression T"
                + host.compressionTier()
                + ")";
            return;
        }
        final String head = (n == 10 ? "a new universe begins at T10" : base.name + " relights at T" + n)
            + ", the base of its band, from a new charge: ";
        if (base.tier.burstDriven()) {
            // a galaxy or a universe lit with no bursts on its ports would breach within a minute and take the next
            // charge with it: wait for a linked quasar's stream (any Bu/t) first
            final EyeBursts b = benchBursts != null ? benchBursts : host.bursts(galaxyDemand(base.tier) * p, 0);
            galaxy.bursts = b;
            if (b.paused) {
                waitReason = head + b.line() + to;
                return;
            }
            if (!b.linkedAll) {
                waitReason = head + "no bursts on the GRB Receptor Ports (route a linked quasar's stream to the Eye;"
                    + " lit without bursts it would breach)"
                    + to;
                return;
            }
        }
        if (n == 4 && !host.takeIgnitionCharge(true)) {
            waitReason = head + "no Stellar Ignition Charge in a plinth input bus" + to;
            return;
        }
        for (Map.Entry<Fluid, Long> need : baseRelightNeeds(base, p, feedSet).entrySet()) {
            if (host.buffer(need.getKey()) < need.getValue()) {
                waitReason = head + "the buffers need "
                    + fmt(need.getValue())
                    + " L of "
                    + fluidName(need.getKey())
                    + " (the charge plus the first cycle)"
                    + to;
                return;
            }
        }
        final BigInteger eu = baseRelightEu(base);
        if (networkEU().compareTo(eu) < 0) {
            waitReason = head + "the wireless network needs " + sci(eu) + " EU (one cycle)" + to;
            return;
        }
        // ---- consume the charge (the first cycle's upkeep and EU are taken by light()'s chargeCycle)
        if (n == 4) {
            if (!host.takeIgnitionCharge(false)) return;
            host.takeBuffer(SLFluids.hyperdenseStellarMixture, (long) Math.ceil(CollapseOverlay.HSM_SEED * p));
        } else if (n == 10) {
            host.takeBuffer(EyeAttractor.darkMatter(), EyeAttractor.universeCharge() * p);
        } else {
            for (EyeRecipe.FluidAmount ch : base.charge) {
                if (ch.fluid != null) host.takeBuffer(ch.fluid, ch.amount * p);
            }
        }
        trans = null;
        ensureField(base);
        star = base;
        fs.setX(0, 0, 0, 0);
        if (base.tier.burstDriven()) galaxy.starOut();
        if (n == 10) {
            fs.uni = UniverseField.newUniverse(fs);
            attractor.uniOwed = 0;
        }
        light();
        waitReason = "";
        lastEvent = base.name + " RELIT at T"
            + n
            + ", the base of its band (D-54): the "
            + baseChargeName(n)
            + " taken"
            + (n == 10 ? "; a new universe begins" : "")
            + to;
        logEvent(lastEvent);
    }

    /**
     * D-54: what the dark Eye does next for the chart in the slot, appended to a failure or put-out line when the
     * base relight applies; empty otherwise.
     */
    private String relightNote() {
        final StarDef want = wanted();
        final StarDef base = baseRelight(want);
        if (base == null) return "";
        if (base.n() == 10) {
            return "; with the chart still in, the Eye begins a new universe at T10 (the base of its band) from a new "
                + baseChargeName(10)
                + "; milestones, unlocked tiers and the counters are kept";
        }
        return "; with the chart still in, the Eye relights " + base.name
            + " at T"
            + base.n()
            + " (the base of its band) from a new "
            + baseChargeName(base.n())
            + (base != want ? ", then re-runs each transition above it toward " + want.name + " (no core)" : "")
            + "; milestones, unlocked tiers and the escrowed core are kept";
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
     * A star's share includes its fractional upkeep ({@link StarDef#exactUpkeep}; none since D-55) and T4's HSM feed
     * for INTAKE_CYCLES cycles at its maximum. The galaxy transitions' charges (the Magnetar Collapse's WDM + BDM
     * overfeed; D-55: the Galactic Merger has none) come in through
     * {@link #transitionNeeds}. Fluids not listed are not pulled at all (they stay in the hatch or the network). The
     * controller adds the planet in the slot. Buffers already above their cap (worlds from before B-108) keep what they
     * hold and are only drawn
     * down. Server state.
     */
    public Map<Fluid, Long> intakeCaps() {
        final Map<Fluid, Long> cap = new LinkedHashMap<>();
        final StarDef want = StarDef.byChart(host.target());
        final boolean liveStar = live() && star != null;
        final StarDef relightBase = baseRelight(wanted());
        if (liveStar) starIntake(cap, star);
        if (want != null) {
            starIntake(cap, want);
            final StarDef lights = ignitable(want);
            if (lights != null && lights != want) starIntake(cap, lights);
            // D-54: the star a dark Eye relights at its band's base, and its charge plus first cycle
            if (relightBase != null) {
                if (relightBase != want) starIntake(cap, relightBase);
                for (Map.Entry<Fluid, Long> need : baseRelightNeeds(relightBase).entrySet()) {
                    cap.merge(need.getKey(), need.getValue(), Math::max);
                }
            }
            // P4: a galaxy chart lights its planets-mode variant in planets mode
            if (want.galaxyTier()) {
                final StarDef planets = StarDef.planetsOf(want.n());
                if (planets != null && planets != want) starIntake(cap, planets);
            }
        }
        galaxyIntake(cap, want, liveStar);
        final EnumSet<EyeTransition.Type> types = EnumSet.noneOf(EyeTransition.Type.class);
        // (D-54: not for a dark Eye that relights the chart's own star at its band's base)
        if (want != null && want.n() >= 2
            && (want.n() >= 4 || liveStar && star.n() == want.n() - 1)
            && (liveStar || relightBase == null || relightBase.n() != want.n())) {
            final EyeTransition.Type t = EyeTransition.Type.leaving(want.n() - 1);
            if (t != null) types.add(t);
        }
        if (trans != null) types.add(trans.type);
        // P4: a galaxy chart in galaxy mode walks the chain from the magnetar (a dark Eye re-merges one, then the
        // Magnetar Collapse, then for the Milky Way the Galactic Merger): every step still ahead of the live object
        // may begin, and each object on the way is a star in play.
        if (want != null && want.galaxyTier() && galaxy.mode == EyeGalaxy.Mode.GALAXY) {
            final int liveN = liveStar ? star.n() : 0;
            // D-54: with T7 unlocked the chain starts at Triangulum (the band's base), not at the magnetar
            final int chainFrom = relightBase != null ? relightBase.n() : 5;
            for (int from = chainFrom; from < want.n(); from++) {
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
        // P4: fractional upkeep (none since D-55)
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
            // T4's HSM feed at FEED_MAX (feed · p litres a second, cycles at the dial)
            cap.merge(def.feed, (long) Math.ceil(FEED_MAX * StellarField.CYCLE_SECONDS * p) * INTAKE_CYCLES, Math::max);
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
            + (n == 4 ? " (collapsed to a black hole)" + relightNote()
                : n == 6
                    ? " (field decoherence: with the chart still in, the Eye re-merges the magnetar from a new"
                        + " merger charge, no core; nothing below T6 re-runs)"
                    : n <= 3 ? " (relighting needs a new Stellar Ignition Charge)" : relightNote());
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
                .append(escrow != null ? ", the core stays in escrow)" : ")")
                .append(retryNote(trans));
        }
        final boolean batch = state == StarState.BATCH && fs != null && fs.batch != null;
        if (batch) {
            sb.append("; ")
                .append(fmt(salvage()))
                .append(" L of Black Dwarf Matter salvaged")
                .append(salvageOwed ? " (owed: paid out once the Eye is formed again)" : "");
        }
        if (!reason.startsWith("the target changed") && (star == null || star.n() >= 4)) sb.append(relightNote());
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
     * QA-A02: what happens next after a Mass Infusion, Shell Ignition or Core Collapse fails or is put out: with its
     * chart still in, the Eye relights the star below from a Stellar Ignition Charge and the transition begins again.
     */
    private static String retryNote(EyeTransition tr) {
        if (tr == null || tr.type.from < 1 || tr.type.from > 3) return "";
        final StarDef from = StarDef.ofTier(tr.type.from);
        return from == null ? ""
            : "; with its chart still in, the Eye relights " + from.name
                + " from a Stellar Ignition Charge and the "
                + tr.type.title
                + " begins again";
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

    /**
     * QA-A13: the fracture salvage a pick-up mid T5 batch owes (25 % of the batch × the fraction done, whole litres),
     * as {@link #salvage()} computes it; 0 without a batch. Changes nothing.
     */
    public long pickupSalvageLitres() {
        if (state != StarState.BATCH || fs == null || fs.batch == null || star == null) return 0;
        final int p = batchP > 0 ? batchP : Math.max(1, parallel(5));
        return (long) Math
            .floor(star.productBase() * p * FRACTURE_SALVAGE * Math.min(1, fs.batch.eyeT / StellarField.BATCH_SECONDS));
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
        euShort = false;
        prodCarry = 0;
        hsmOwed = 0;
        flashState = 0;
        dwarfReserve = 0;
        lastOut = null;
        galaxy.starOut();
        galaxy.beginRequested = false;
        attractor.resetLive();
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
        if (from + 1 > maxUnlock()) out.add("Eye T9 and T10 are switched off on this server (enableEyeT9T10)");
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
                out.add(
                    from == 6 ? "the magnetar lit"
                        : from == 7 ? "Triangulum lit in galaxy mode"
                            : from == 8 ? "the Milky Way lit in galaxy mode"
                                : "the Great Attractor lit in galaxy mode");
            } else if (lastOut == null || lastOut.zone > 1) {
                out.add(
                    "the " + (from == 6 ? "magnetar" : from <= 8 ? "galaxy" : "Great Attractor") + " Stable or better");
            }
            if (from == 6 && (host.receptorPorts(0) < 1 || host.receptorPorts(1) < 1)) {
                out.add("a GRB Receptor Port on each bank of the stability rail (rail-high and rail-low)");
            }
            if (from == 7 && host.receptorPorts(2) < HALO_PORTS) {
                out.add(
                    "all " + HALO_PORTS + " GRB Receptor Ports on the shard halo (" + host.receptorPorts(2) + " now)");
            }
            if (from >= 7 && galaxy.companions.length > 0) {
                out.add("no companions (take the star charts out of the slot bus)");
            }
            if (from == 8) {
                // P5 (p5-t9-t10.md §2.8): the collimator stack with its Pellet Injector stocked, a beam that holds a
                // group of 6 (the forced flow), the collar ports for the beam's test fire
                final PelletFeed f = host.pellets();
                final int g = f.grade();
                final int rating = AttractorField.holdRating(f.barrelMk, g);
                if (f.injectors < 1) out.add("a Pellet Injector Hatch on the top collar of the collimator stack");
                else if (f.barrelMk < 1) out.add("an Injector Barrel in the Pellet Injector Hatch");
                else if (g < 1 || f.stock[g] < 1) out.add("Gated Pellets in the Pellet Injector Hatch");
                else if (rating < INFALL_HOLD_RATING) {
                    out.add(
                        "a beam with a hold rating of " + INFALL_HOLD_RATING
                            + " or more (now "
                            + rating
                            + ": "
                            + EyeAttractor.gradeName(g)
                            + " pellets on a Mk "
                            + f.barrelMk
                            + " barrel; Superdense on Mk I, or Ergomatter on Mk II, will do)");
                }
            }
            if (from == 9 && host.receptorPorts(4) < CROWN_PORTS) {
                out.add(
                    "all " + CROWN_PORTS
                        + " GRB Receptor Ports on the universal crown ("
                        + host.receptorPorts(4)
                        + " now)");
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
        return transitionNeeds(type, Math.max(1, parallel(type.from)), destParallel(type.from + 1), feedSet);
    }

    /**
     * {@link #transitionNeeds(EyeTransition.Type)} for a new star at parallel {@code pd} and an HSM feed setpoint
     * {@code feed} (the NEI transition page uses p = 1 and the default feed).
     */
    public static Map<Fluid, Long> transitionNeeds(EyeTransition.Type type, int pd, double feed) {
        return transitionNeeds(type, pd, pd, feed);
    }

    /** The Magnetar Collapse's charge per parallel: 300 s of WDM 61.44 + BDM 15.36 L/s. */
    public static final long MC_WDM = Math
        .round(MagnetarCollapseOverlay.WDM_PER_SECOND * MagnetarCollapseOverlay.OVERFEED_SECONDS),
        MC_BDM = Math.round(MagnetarCollapseOverlay.BDM_PER_SECOND * MagnetarCollapseOverlay.OVERFEED_SECONDS);
    /** P4: the shard halo's Receptor Ports the Galactic Merger needs (all of them, §5.3). */
    public static final int HALO_PORTS = 8;
    /** P5: the universal crown's Receptor Ports Inflation needs (all 12, §6.3). */
    public static final int CROWN_PORTS = 12;
    /** P5: the beam's hold rating The Infall needs (the forced flow is a group of 6, §2.8). */
    public static final int INFALL_HOLD_RATING = 6;
    /** P5: The Infall's charge per parallel (Superdense Ergomatter) and Inflation's (Attractor Dark Matter). */
    public static final long INFALL_SDE = Math
        .round(com.nateyburr.beyondthestars.machines.eye.field.InfallOverlay.CHARGE_BUFFER),
        INFLATION_DM = Math.round(com.nateyburr.beyondthestars.machines.eye.field.InflationOverlay.CHARGE_BUFFER);

    /**
     * As above with P4's two: the charge of a galaxy transition is taken per second at the parallel of the object it
     * leaves ({@code ps}); the new object's first cycle at its own ({@code pd}). D-55: the Galactic Merger needs no
     * fluid (bursts alone hold it).
     */
    public static Map<Fluid, Long> transitionNeeds(EyeTransition.Type type, int ps, int pd, double feed) {
        final Map<Fluid, Long> need = new LinkedHashMap<>();
        final StarDef dest = StarDef.byKey(type.dest);
        switch (type) {
            case MAGNETAR_COLLAPSE:
                need.merge(wdmFluid(), MC_WDM * ps, Long::sum);
                need.merge(bdmFluid(), MC_BDM * ps, Long::sum);
                break;
            case GALACTIC_MERGER:
                break; // D-55: no Ergomatter charge
            case INFALL:
                need.merge(SLQuasarFluids.superdenseErgomatter, INFALL_SDE * ps, Long::sum);
                break;
            case INFLATION:
                // 300 s of 30.72 L/s once inflated (9,216 L) and the first universe's charge (9,216 L)
                need.merge(EyeAttractor.darkMatter(), INFLATION_DM * ps, Long::sum);
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
            case 7:
                return "stability rail";
            case 8:
                return "shard halo";
            case 9:
                return "collimator stack";
            case 10:
                return "universal crown";
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
            + (escrow != null ? "; the " + escrow.getDisplayName() + " stays in escrow" : "")
            + retryNote(tr)
            + (tr != null && tr.type.from >= 4 ? relightNote() : "");
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

    /**
     * Debug {@code lose} (D-54 test): the live star is lost as a Breach loses it: a running transition fails, a T10
     * universe is put out (a T10 breach only ends that universe), every other star fails. Returns an error or null.
     */
    public String debugLose() {
        if (!live()) return "no star is live";
        if (trans != null) failTransition("Breach (bench)");
        else if (star != null && star.tier.universe) extinguish("the universe was lost on the bench");
        else fail("Breach (bench)");
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
            // ---- P5 (p5-t9-t10.md §9.3; the bit rules are P5Directive's)
            case EYE_FLOW:
                return fs == null ? 0
                    : P5Directive.flow(
                        burning() && fs.tier.attractor,
                        fs.prevE[StellarField.V],
                        AttractorField.warnedAccepted(fs),
                        attractor.beamStarved(),
                        w);
            case EYE_UNIVERSE:
                return fs == null ? 0
                    : P5Directive.universe(
                        burning() && fs.tier.universe && trans == null,
                        fs.prevE[StellarField.H],
                        UniverseField.countdown(fs.uni),
                        universeChargeShort(),
                        w);
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
        if (trans != null || state != StarState.PENDING || star == null || star.n() < 6 || star.n() > 9) {
            return false;
        }
        return missing(star.n()).isEmpty();
    }

    /** P5 Universe bit 4: the next universe's charge is short (owed now, or the buffer under one charge × p). */
    public boolean universeChargeShort() {
        if (!live()) {
            // D-54: a dark Eye with the Universe chart and T10 unlocked begins a new universe from one charge
            final StarDef base = baseRelight(wanted());
            if (base != null && base.n() == 10) {
                final Fluid dm = EyeAttractor.darkMatter();
                return dm == null || host.buffer(dm) < EyeAttractor.universeCharge() * Math.max(1, parallel(10));
            }
        }
        if (fs == null || !fs.tier.universe) return false;
        if (attractor.uniOwed > 0) return true;
        final Fluid dm = EyeAttractor.darkMatter();
        return dm == null || host.buffer(dm) < EyeAttractor.universeCharge() * Math.max(1, lastP);
    }

    /**
     * Slots bit 4: the next link milestone is not held while its core and charge are in place, on a lit magnetar (the
     * Stability Link: J1 all to the Eye) or a lit galaxy-mode Triangulum (the Heavy Link: J1 all to the Eye, J3 75 %
     * to the halo). GalaxyDirective's rule.
     */
    public boolean testFireWanted() {
        if (fs == null || star == null || trans != null) return false;
        final int n = fs.tier.n;
        if (n != 6 && n != 7 && n != 8) return false;
        final Item core = coreFor(n);
        final boolean coreIn = escrow != null && escrow.getItem() == core;
        final EyeTransition.Type type = EyeTransition.Type.leaving(n);
        boolean buffered = type != null;
        if (type != null) {
            for (Map.Entry<Fluid, Long> need : transitionNeeds(type).entrySet()) {
                if (need.getKey() == null || host.buffer(need.getKey()) < need.getValue()) buffered = false;
            }
        }
        if (n == 8) return !fs.tier.planets() && burning() && !milestones.linkDone[8] && coreIn && buffered;
        return GalaxyDirective
            .testFireWanted(n, fs.tier.planets(), burning(), false, milestones.linkDone[n], coreIn, buffered)
            && (n != 7 || heavyTestFireFits(testFireStream()));
    }

    /**
     * P4-88: what J1 at 15 puts on the Eye, Bu/t: the linked quasar's mean stream, or the out-1 limit of the Junction
     * its streams passed if that is lower (the standard rig's J1 limit, 7.5 Bu/t per parallel). The T7 test fire's
     * size: with the limit, the test fire is the same few Bu/t at any quasar size.
     */
    public double testFireStream() {
        return Math.min(linkedQuasarStream(), galaxy.bursts.limit);
    }

    /**
     * P4-88: the most mean stream the T7 halo test fire may take at J1 15 (Bu/t). The rail keeps a quarter, and above
     * about 8 Bu/t on the rail (β ≈ 5) the field's P drift outruns the binary Space valve: the prototype's T7 rig with
     * Mk II throats holds at 265 SM (33.6 Bu/t, 0 failures on 3 seeds) and starts to breach at 300 SM (43 Bu/t).
     */
    public static final double HEAVY_TEST_FIRE_MAX_STREAM = 32.0;
    /** P4-88: the Eye leg a hand-set test fire aims for on a quasar above the ceiling (rail 4, halo 12 Bu/t). */
    public static final double HEAVY_TEST_FIRE_EYE_LEG = 16.0;

    /**
     * P4-88: the standard rig's J1 out-1 limit for this Eye, Bu/t: {@link BurstRouting#STANDARD_J1_LIMIT} × parallels
     * × the time dial (the demand scales with both).
     */
    public double standardJ1Limit() {
        return j1LimitPerParallel() * Math.max(1, lastP) * Math.max(1, lastM);
    }

    /**
     * P5 (B-170): the standard J1 limit per parallel. T7 / T8 keep P4-88's 5 x D7 = 7.5 Bu/t; from the Deep Link's test
     * fire (T8, it needs 24 Bu/t on the Eye), through The Infall and at T9 / T10 it is 5 x D_T of the tier being run
     * toward (120 Bu/t at T9, 480 at T10), the same 5 : 1 ratio P4-88 gave T7.
     */
    double j1LimitPerParallel() {
        final int n = fs == null ? 0 : fs.tier.n;
        final boolean deepLink = n == 8 && trans == null && testFireWanted();
        final int t = n >= 9 ? n : trans instanceof Infall || deepLink ? 9 : 0;
        return t >= 9 ? 5 * GalaxyField.demandT(t) : BurstRouting.STANDARD_J1_LIMIT;
    }

    /** P4-79 / P4-88: the T7 test fire at J1 15 suits a mean stream from 6 to 32 Bu/t. */
    public static boolean heavyTestFireFits(double stream) {
        return stream >= GalaxyField.demandT(8) && stream <= HEAVY_TEST_FIRE_MAX_STREAM;
    }

    /**
     * P4-88: the J1 share to the Eye a test fire should use on this quasar: 1 up to the ceiling, else
     * {@link #HEAVY_TEST_FIRE_EYE_LEG} ÷ the mean stream (0 with no link).
     */
    public double heavyTestFireShare() {
        final double s = testFireStream();
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
        final double q = linkedQuasarStream(), s = testFireStream();
        if (s < GalaxyField.demandT(8)) {
            if (q >= GalaxyField.demandT(8)) {
                return String.format(
                    Locale.ROOT,
                    "Heavy Link test fire waits: J1's out 1 limit %s is under 6 Bu/t (raise it: 7.5 per parallel)",
                    BTSFormat.buEuShort(s));
            }
            return String.format(
                Locale.ROOT,
                "Heavy Link test fire waits: the quasar's mean stream %s is under 6 Bu/t (grow it)",
                BTSFormat.buEuShort(s));
        }
        if (s > HEAVY_TEST_FIRE_MAX_STREAM) {
            return String.format(
                Locale.ROOT,
                "Heavy Link test fire held: the quasar's mean stream %s is over %.0f Bu/t, and J1 at 15 would flood"
                    + " the rail. Set J1's out 1 limit to %s (the standard rig), or test-fire with J1's Eye share at"
                    + " %.2f (J3 75 %% halo)",
                BTSFormat.buEuShort(s),
                HEAVY_TEST_FIRE_MAX_STREAM,
                BTSFormat.buEuShort(standardJ1Limit()),
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

    /**
     * B-118 "Warn, allow" (2026-10-13): the most the linked quasar can put on the Eye on the Magnetar Collapse's
     * second, Bu/t: a purge at full flux, {@code QuasarParams.peakStream} at its current G0 and Y_q (3.5 × G0·Y_q;
     * the assist sends J1 all to the Eye on the cue), before any throat cap. 0 with no link. The bench's scripted
     * quasar reports its purging mean (0.8 × G0·Y_q), scaled back to the same peak.
     */
    public double linkedQuasarPeak() {
        if (benchBursts != null && benchFlux != null) {
            return benchFlux.length > 2
                ? benchFlux[2] / com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.MEAN_PURGE_40
                    * com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.PEAK_PURGE
                : 0;
        }
        final BurstSource s = galaxy.bursts.sourceId;
        final MTEQuasarEngine q = s == null ? null : s.resolve();
        if (q == null || !q.isFormed()) return 0;
        return com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.peakStream(q.state().G0, q.state().Yq);
    }

    /**
     * The Bu/t on the collapse second that cancels the next Magnetar Collapse's whole shock: the number the countdown
     * line states (13.5 at the adopted +120).
     */
    public double collapseFullCancelBu() {
        return MagnetarCollapseOverlay.fullCancelBu(collapseShock);
    }

    /**
     * B-118: a lit magnetar waiting for the Magnetar Collapse (no transition yet) whose linked quasar's peak
     * ({@link #linkedQuasarPeak}) is under {@link #collapseFullCancelBu}: even a purge at full flux timed onto the
     * collapse second leaves part of the shock. A warning only: Begin still works (my "Warn, allow").
     */
    public boolean collapseQuasarWeak() {
        if (trans != null || state != StarState.PENDING || star == null || star.n() != 6) return false;
        return linkedQuasarPeak() < collapseFullCancelBu();
    }

    /** The shock the next Magnetar Collapse would land with if the linked quasar delivered its peak (readout). */
    public double collapseShockAtPeak() {
        return MagnetarCollapseOverlay.predictedShock(collapseShock, linkedQuasarPeak());
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
            if (t.n == 3) return "Helium flash heat, Θ > +20 (more Super Coolant)";
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
                < (long) Math.ceil(feedSet * StellarField.CYCLE_SECONDS * p))
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
        // D-54: a dark Eye relighting at its band's base: the charge plus the first cycle
        final StarDef base = baseRelight(wanted());
        if (base != null) {
            if (parallel(base.n()) < 1) return false;
            for (Map.Entry<Fluid, Long> need : baseRelightNeeds(base).entrySet()) {
                if (host.buffer(need.getKey()) < need.getValue()) return true;
            }
            return false;
        }
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

    /**
     * The star a dark Eye lights from a Stellar Ignition Charge for this chart, or null: a T1-T3 chart's own star once
     * its tier is unlocked; for the chart one tier above the unlocked one (T2-T4) the star below it, which then runs
     * the transition toward the chart (QA-A02). D-54: the Neutron Star chart on an Eye that has T4 is not here: the
     * neutron star relights itself ({@link #baseRelight}).
     */
    private StarDef ignitable(StarDef want) {
        if (want == null) return null;
        final int n = want.n();
        if (want.tier.ignite && n <= unlockedTier) return want;
        if (n >= 2 && n <= 4 && n == unlockedTier + 1) {
            final StarDef progenitor = StarDef.ofTier(n - 1);
            if (progenitor != null && progenitor.tier.ignite) return progenitor;
        }
        return want.tier.ignite ? want : null;
    }

    /** Status bit 2: a dark Eye would light its chart's star now but no Stellar Ignition Charge is in a plinth bus. */
    public boolean chargeWanted() {
        if (state != StarState.DARK) return false;
        // D-54: the neutron star's relight at T4 takes a Stellar Ignition Charge too
        final StarDef base = baseRelight(wanted());
        if (base != null) return base.n() == 4 && parallel(4) >= 1 && !host.takeIgnitionCharge(true);
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

    /**
     * Status bit 4: the Eye is not formed or is switched off, or the burning star is Critical or worse, or its
     * Integrity is below 50.
     */
    public boolean alarm() {
        if (!host.formed()) return true;
        if (!host.allowedToWork()) return true; // S-05: switched off (GT's stop, a soft mallet, a cover)
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
            case 8:
                return SLP5Items.attractorSeedCore;
            case 9:
                return SLMaxItems.universalUpgradeCore;
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
        for (int n = 1; n <= 9; n++) if (coreFor(n) == item) return true;
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
        return needName + " held in escrow. It is consumed only when "
            + (eventFor(unlockedTier).startsWith("The ") ? "" : "the ")
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
        lastEvent = sb
            .append(
                "owed (a T5 salvage made while unformed or picked up, a run started on an older version, or a"
                    + " buffer the Eye no longer takes since 0.26.0)")
            .toString();
        com.nateyburr.beyondthestars.BeyondTheStars.LOG.info("[BTS] Eye {}", lastEvent);
        host.markDirty();
    }

    /**
     * D-55: a buffer the Eye no longer takes (Ergomatter, Molten Magnetar Matter in a save from before 0.26.0) is owed
     * back: paid to the output hatches with the other owed outputs once the Eye stands formed ({@link #payLegacy}).
     */
    public void oweRetired(Fluid fluid, long litres) {
        if (fluid == null || litres <= 0) return;
        legacyPay.merge(fluid.getName(), litres, Long::sum);
        com.nateyburr.beyondthestars.BeyondTheStars.LOG.info(
            "[BTS] Eye: {} L of {} in the buffers is no longer an Eye input (0.26.0); it is paid to the output hatches"
                + " once the Eye is formed",
            litres,
            fluid.getName());
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
            case DARK_MATTER:
                return EyeAttractor.darkMatter();
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
            // QA-A27: the client names the fluid in its own language
            final String name = BTSText.fluid(need.getKey());
            c.add(
                have >= need.getValue(),
                name,
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
     * D-54 GUI: every condition {@link #tryBaseRelight} checks before relighting {@code base} at the base of its
     * band, met or not: the Eye reaching that tier, T4's Stellar Ignition Charge, the charge plus the first cycle in
     * the buffers (× p) and one cycle of EU. Reads only.
     */
    public BTSChecklist baseRelightChecklist(StarDef base) {
        final BTSChecklist c = new BTSChecklist();
        if (base == null) return c;
        final int n = base.n();
        final int inst = host.installedTier();
        String why = "";
        if (host.structureTier() < n) why = "tier" + n;
        else if (host.compressionTier() < n) why = "compression" + n;
        c.add(
            parallel(n) >= 1,
            BTSText.lang("beyondthestars.gui.eye.chk.reach", n),
            BTSText.lit("T" + inst + " / T" + n),
            -1,
            why.isEmpty() ? "" : BTSText.lang(RESULT_KEY + why));
        if (base.tier.burstDriven()) {
            final EyeBursts b = galaxy.bursts;
            c.add(
                b != null && b.linkedAll && !b.paused,
                BTSText.lang("beyondthestars.gui.eye.chk.bursts"),
                BTSText.lit(b == null ? "-" : BTSFormat.buEuShort(b.total())),
                -1,
                BTSText.lang("beyondthestars.gui.eye.chk.bursts.tip"));
        }
        if (n == 4) {
            final int charges = host.countIgnitionCharges();
            c.add(
                charges >= 1,
                BTSText.lang("beyondthestars.gui.eye.chk.charge"),
                BTSText.lang("beyondthestars.gui.eye.chk.inbus", charges),
                -1,
                BTSText.lang("beyondthestars.gui.eye.chk.charge.tip"));
        }
        for (Map.Entry<Fluid, Long> need : baseRelightNeeds(base).entrySet()) {
            final long have = host.buffer(need.getKey());
            // QA-A27: the client names the fluid in its own language
            final String name = BTSText.fluid(need.getKey());
            c.add(
                have >= need.getValue(),
                name,
                BTSText.lit(BTSFormat.haveNeed(have, need.getValue()) + " L"),
                BTSChecklist.frac(have, need.getValue()),
                BTSText.lang(
                    "beyondthestars.gui.eye.fluid.tip",
                    name,
                    BTSFormat.exact(have),
                    BTSFormat.exact(need.getValue())));
        }
        final BigInteger eu = baseRelightEu(base);
        if (host.owner() == null) {
            c.add(
                false,
                BTSText.lang("beyondthestars.gui.eye.chk.eu"),
                BTSText.lang("beyondthestars.gui.eye.chk.noowner"),
                -1,
                BTSText.lang(RESULT_KEY + "noowner"));
        } else {
            final BigInteger have = networkEU();
            final boolean ok = have.compareTo(eu) >= 0;
            c.add(
                ok,
                BTSText.lang("beyondthestars.gui.eye.chk.eu"),
                BTSText.lit(ok ? BTSFormat.eu(eu) : BTSFormat.eu(have) + " / " + BTSFormat.eu(eu)),
                ok ? -1 : (float) Math.max(0, Math.min(1, have.doubleValue() / eu.doubleValue())),
                BTSText.lang("beyondthestars.gui.eye.chk.eu1.tip", BTSFormat.eu(eu)));
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
            BTSText.fluid(wdm),
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
            // QA-A27: the client names the fluid in its own language
            final String name = BTSText.fluid(need.getKey());
            c.add(
                have >= need.getValue(),
                name,
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
                    + ", companions "
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
        // B-224: a lit T10 between universes says what the next one waits for
        if (T.universe && !waitReason.isEmpty()) out.add("  waiting: " + waitReason);
        // B-221 / B-222: the beam's state when it is not doing its job
        if (T.attractor && attractor.fault) out.add("  BEAM NOT FIRING while V climbs: " + attractor.why);
        else if (T.attractor && attractor.unwired) {
            out.add(
                "  Beam: the injector's Burst Control Hatch has never carried a signal, so the beam fires at its"
                    + " centre (level 8, open loop); wire the V sensor (analog, window 30) or Flow bits 1 / 2 to it");
        }
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
        if (RawStarMatterYield.perMinute(T) > 0) {
            out.add(
                String.format(
                    Locale.ROOT,
                    "Raw Star Matter byproduct: %,.0f L x p x mean Y x r per Eye-minute%s; last cycle %,.0f L, %,.0f L"
                        + " since this controller loaded",
                    RawStarMatterYield.perMinute(T),
                    T.planets() ? " (planets mode: a quarter)" : "",
                    lastRspm,
                    rspmMade));
        }
        if (T.universe) {
            out.add("Universe yield: " + UniverseYield.summary(universeSelection()));
            if (!attractor.lastYield.isEmpty()) out.add("  last universe: " + attractor.lastYield);
        }
        if (T.n >= 6 || galaxyTransition()) galaxyLines(out, T);
        if (rig != null) out.add(
            "Debug rig: " + rig.name()
                .toLowerCase(Locale.ROOT) + " (replaces the Field sockets)");
    }

    /** P4 scanner lines: the bursts, β and the γs, the link, the T8 crossing, companions, slots. */
    private void galaxyLines(List<String> out, TierParams T) {
        out.add(galaxy.bursts.line());
        if (T.burstDriven() || galaxyTransition()) out.add(galaxy.inputLine());
        final BurstLink l = activeLink();
        if (l != null) {
            final int n = linkTier();
            out.add(
                String.format(
                    Locale.ROOT,
                    "%s: streak %s / 30:00 (best %s), 60 s mean %s, %s %.0f%%%s",
                    EyeMilestones.LINK_NAME[n],
                    EyeMilestones.clock(l.streak),
                    EyeMilestones.clock(Math.max(l.best, milestones.linkBest[n])),
                    BTSFormat.buEuShort(l.mean),
                    n == 6 ? "high bank" : "halo",
                    100 * l.frac,
                    n == 6 ? " (needs >= 1.5 Bu/t = 1.5e13 EU/t, high bank 25-75 %)"
                        : " (needs >= 6 Bu/t = 6.0e13 EU/t, halo >= 50 %)"));
        }
        if (T.n >= 8 || galaxy.bursts.supermassiveSource() || milestones.smLinkBest > 0) {
            out.add(supermassiveLinkLine());
        }
        if (T.shear) {
            final int[] c = crossings();
            out.add(
                String.format(
                    Locale.ROOT,
                    "Tidal shear drift 2.0/s (no feed since 0.26.0); satellite crossings %d (caught %d)%s",
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
                    "  The shock would land at P +%.0f with %s now; %s on the collapse second cancels" + " it all",
                    mc.predictedShock(galaxy.gEye),
                    BTSFormat.buEuShort(galaxy.gEye),
                    BTSFormat.buEuShort(mc.fullCancelBu())));
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
        /** P5: Gated Pellets drawn. */
        public double pellets;
        /** D-55: Raw Star Matter made (every tier), the universes' star matter and their planet parallels won. */
        public double rspm, uniStarMatter;
        public long uniPlanetRuns;
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
