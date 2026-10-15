package com.nateyburr.beyondthestars.integration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

import com.nateyburr.beyondthestars.machines.directive.DirectiveHost;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.star.EyeBursts;
import com.nateyburr.beyondthestars.machines.eye.star.EyeGalaxy;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
import com.nateyburr.beyondthestars.machines.eye.star.GalacticMerger;
import com.nateyburr.beyondthestars.machines.eye.star.MagnetarCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrModuleBase;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;
import com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * AUTOMATION PASS: what a computer may read and set on the Singularity Ladder's machines, with no computer mod's types
 * in it. The OpenComputers components ({@code integration.oc}, a soft dependency) are thin wrappers over these calls,
 * and the dev-server bench drives the same calls ({@code /btskerr ocloop}, {@code /btseye star … ocloop}) to prove the
 * setter rules without OpenComputers installed.
 * <p>
 * <b>Same rules as the GUI.</b> Every setter goes through the setter the GUI uses: the Kerr setpoint clamps to the
 * tier's range, Open and Collapse need a formed KEE and a Seed / Collapser in a bolt bus (and consume it), the time
 * dial snaps to 0.25 steps in 1-3 and the HSM feed clamps to 0-1.5 L/s. There is no setter a player does not have.
 * Results are maps of numbers, strings and booleans (Lua tables on the computer). Server thread only.
 */
public final class BTSComputerApi {

    private BTSComputerApi() {}

    // ============================================================================ rate limit

    /** Setters take one call per this many ticks per machine and method: the field's own step (quasar-p4.md §8.2). */
    public static final long RATE_TICKS = 20;
    /** What a refused setter returns to the computer: nil and the reason. */
    public static final String RATE_MESSAGE = "rate limited: one call per 20 ticks (1 s, the field's step)";
    /** "dim:x,y,z#method" -> world tick of the last accepted call; the oldest entries go first past 4,096. */
    private static final Map<String, Long> LAST_SET = new LinkedHashMap<String, Long>(64, 0.75f, false) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > 4096;
        }
    };

    /**
     * True if {@code method} may run on {@code mte} now (and records the call); false within {@link #RATE_TICKS} of
     * the last accepted call, so a script cannot beat the 1 s lag. Server thread.
     */
    public static boolean rateOk(IMetaTileEntity mte, String method) {
        final IGregTechTileEntity base = mte == null ? null : mte.getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return true;
        final long now = base.getWorld()
            .getTotalWorldTime();
        final String key = base.getWorld().provider.dimensionId + ":"
            + base.getXCoord()
            + ","
            + base.getYCoord()
            + ","
            + base.getZCoord()
            + "#"
            + method;
        final Long last = LAST_SET.get(key);
        if (last != null && now - last < RATE_TICKS && now >= last) return false;
        LAST_SET.put(key, now);
        return true;
    }

    // ============================================================================ burst hosts

    /** The quasar link as a host sees it: sigma (60 s mean and last second), delivered, linked, source, paused. */
    public static Map<String, Object> burstLink(BurstHost h, double sigmaUsed, int ports) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("ports", ports);
        m.put("sigma", sigmaUsed);
        m.put("sigmaLast", h.sigma());
        m.put("delivered", h.delivered());
        m.put("deliveredEUt", h.delivered() * BTSFormat.EU_PER_BU);
        m.put("mean", h.mean());
        m.put("meanEUt", h.mean() * BTSFormat.EU_PER_BU);
        m.put("linked", h.linked());
        final BurstSource s = h.linkSource();
        m.put("source", s == null ? "" : s.label());
        m.put("paused", h.paused());
        m.put("line", h.linkLine());
        return m;
    }

    /** {@code bts_homogeny.getState()}: tier, parallels, the quasar link and what it does (P4, §3.1). */
    public static Map<String, Object> homogenyState(MTEHomogeny h) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("formed", h.isFormedForGui());
        m.put("tier", h.tier());
        m.put("installedTier", h.installedTier());
        m.put("parallels", h.maxParallel());
        m.put("speed", h.speedFactor());
        m.put("demand", h.burstDemand());
        m.put("demandEUt", h.burstDemand() * BTSFormat.EU_PER_BU);
        m.put("entanglementOpen", h.quasarSigma() >= 1.0);
        m.put("running", h.mMaxProgresstime > 0);
        m.put("progress", h.mMaxProgresstime > 0 ? (double) h.mProgresstime / h.mMaxProgresstime : 0.0);
        m.put("link", burstLink(h.burstHost(), h.quasarSigma(), h.receptorCount()));
        return m;
    }

    /** {@code bts_reality.getState()}: runs, the burst-driven regions and their speed (P4, §3.1). */
    public static Map<String, Object> realityState(MTERealityDiffraction r) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("formed", r.mMachine);
        m.put("burstDriven", r.burstDriven());
        m.put("speed", r.speedFactor());
        m.put("demand", BurstLink.REALITY);
        m.put("demandEUt", BurstLink.REALITY * BTSFormat.EU_PER_BU);
        m.put("running", r.mMaxProgresstime > 0);
        m.put("progress", r.mMaxProgresstime > 0 ? (double) r.mProgresstime / r.mMaxProgresstime : 0.0);
        m.put("anchorMask", r.anchorMask() & 63);
        m.put("link", burstLink(r.burstHost(), r.quasarSigma(), r.receptorCount()));
        return m;
    }

    // ===================================================================================== Eye

    /** {@code bts_eye.getState()}: tiers, the star, the transition, setpoints, upkeep, last event and failure. */
    public static Map<String, Object> eyeState(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("formed", eye.isFormedForGui());
        m.put("tier", eye.installedTier());
        m.put("unlockedTier", rt.unlockedTier());
        m.put("structureTier", eye.structureTier());
        m.put("compressionTier", eye.compressionTier());
        m.put(
            "state",
            rt.state()
                .name());
        final StarDef star = rt.star();
        m.put("star", star == null ? "" : star.name);
        m.put("starTier", star == null ? 0 : star.n());
        m.put("burning", rt.burning());
        m.put("parallel", rt.lastParallel());
        final ItemStack target = eye.getControllerSlot();
        m.put("target", target == null ? "" : target.getDisplayName());
        final EyeTransition tr = rt.transition();
        m.put("transition", tr == null ? "" : tr.type.title);
        m.put("transitionProgress", rt.transitionProgress());
        m.put("dial", rt.dialSetpoint());
        m.put("dialPaid", rt.dialPaid());
        m.put("dialMax", rt.dialMax());
        m.put("feed", rt.feedSetpoint());
        m.put("feedActual", rt.feedActual());
        m.put("upkeepR", rt.upkeepR());
        m.put("nextCycleSeconds", rt.nextCycleSeconds());
        m.put("waitReason", rt.waitReason());
        m.put("lastEvent", rt.lastEvent());
        m.put("lastFailure", rt.lastFailure());
        return m;
    }

    /** {@code bts_eye.getField()}: last second's stability field (the sensor hatches' readout). */
    public static Map<String, Object> eyeField(MTEEyeOfHarmonyBTS eye) {
        final FieldReadout r = eye.starRuntime()
            .lastReadout();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("lit", r.lit);
        m.put("tier", r.tier);
        m.put("P", r.value(FieldReadout.Channel.P));
        m.put("theta", r.value(FieldReadout.Channel.THETA));
        m.put("omega", r.value(FieldReadout.Channel.OMEGA));
        m.put("pi", r.value(FieldReadout.Channel.PI));
        m.put("shield", r.A);
        m.put("integrity", r.I);
        m.put("stability", r.stability);
        m.put("zone", r.zone);
        m.put("zoneName", r.zone >= 0 && r.zone < StellarField.ZONE_NAME.length ? StellarField.ZONE_NAME[r.zone] : "");
        m.put("yield", r.Y);
        m.put("lock", r.lock);
        m.put("warning", r.warn);
        m.put("setpointError", r.value(FieldReadout.Channel.SETPOINT_ERROR));
        m.put("r", r.r);
        m.put("m", r.m);
        m.put("p", r.p);
        // P4 (T7+): the galaxy gauges and the bursts as the field saw them
        final EyeGalaxy g = eye.starRuntime()
            .galaxy();
        m.put("W", r.value(FieldReadout.Channel.W));
        m.put("L", r.value(FieldReadout.Channel.LAMBDA));
        m.put("beta", g.beta());
        m.put("gammaHi", g.gammaHi());
        m.put("gammaLo", g.gammaLo());
        m.put("gammaHalo", g.gammaHalo());
        m.put("D", g.demand());
        m.put("planets", r.planets);
        return m;
    }

    /**
     * P4 {@code bts_eye.getEvent()}: the running countdown: kind ("collapse", "pass", "crossing", "merger" or ""),
     * secondsLeft (−1 none) and sign (+1 / −1, 0 unsigned).
     */
    public static Map<String, Object> eyeEvent(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final Map<String, Object> m = new LinkedHashMap<>();
        final EyeTransition tr = rt.transition();
        final int left = rt.countdownSeconds();
        String kind = "";
        int sign = 0;
        if (tr instanceof MagnetarCollapse && left >= 0) kind = "collapse";
        else if (tr instanceof GalacticMerger && left >= 0) {
            kind = "pass";
            sign = ((GalacticMerger) tr).nextPassSign();
        } else if (rt.crossingCountdown() >= 0 || rt.crossingLeft() > 0) kind = "crossing";
        else if (rt.mergerWarning() != 0) {
            kind = "merger";
            sign = rt.mergerWarning();
        }
        m.put("kind", kind);
        m.put("secondsLeft", kind.isEmpty() ? -1 : left);
        m.put("sign", sign);
        m.put("crossingLeft", rt.crossingLeft());
        m.put("precession", rt.precession());
        return m;
    }

    /**
     * P4 {@code bts_eye.getUnlock()}: the link milestone in progress: kind ("stability", "heavy" or ""), streak,
     * best, mean (Bu/t, 60 s), frac (the high bank's or the halo's share), linked, source, done, and the transition's
     * readiness.
     */
    public static Map<String, Object> eyeUnlock(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final Map<String, Object> m = new LinkedHashMap<>();
        final int n = rt.linkTier();
        final BurstLink l = rt.activeLink();
        final EyeBursts b = rt.galaxy()
            .bursts();
        m.put("kind", n == 6 ? "stability" : n == 7 ? "heavy" : "");
        m.put("streak", l == null ? 0 : l.streak);
        m.put("best", l == null ? 0 : Math.max(l.best, rt.milestones().linkBest[n]));
        m.put("mean", l == null ? b.mean60 : l.mean);
        m.put("frac", l == null ? 0 : l.frac);
        m.put("linked", n == 6 ? b.linkedRail : b.linkedAll);
        m.put("source", b.source);
        m.put("stabilityLinkHeld", rt.milestones().linkDone[6]);
        m.put("heavyLinkHeld", rt.milestones().linkDone[7]);
        m.put("ready", rt.galaxyReady());
        m.put("testFireWanted", rt.testFireWanted());
        m.put("quasarStream", rt.linkedQuasarStream());
        m.put("testFireShare", rt.heavyTestFireShare());
        // P4-88: what J1 at 15 puts on the Eye (min of the stream and J1's out-1 limit), and the standard limit
        m.put("testFireStream", rt.testFireStream());
        m.put("j1Limit", rt.standardJ1Limit());
        // SMBH (smbh-v1.md §1.3): the Supermassive Link, P5's T9 -> T10 milestone (it gates Inflation)
        final BurstLink sm = rt.galaxy()
            .supermassiveLink();
        final Map<String, Object> s = new LinkedHashMap<>();
        s.put("streak", sm.streak);
        s.put("best", rt.milestones().smLinkBest);
        s.put("held", rt.milestones().smLinkDone);
        s.put("mean", sm.mean);
        s.put("need", BurstLink.supermassiveMean());
        s.put("sourceMass", b.smMass);
        s.put("sourceLattice", b.smLattice);
        s.put("sourceQualifies", b.linkedAll && b.supermassiveSource());
        m.put("supermassiveLink", s);
        return m;
    }

    // ================================================================== P5: the beam, the flow, the universe

    /** P5 {@code bts_eye.getBeam()}. */
    public static Map<String, Object> eyeBeam(MTEEyeOfHarmonyBTS eye) {
        final com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime rt = eye.starRuntime();
        final com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor at = rt.attractor();
        final com.nateyburr.beyondthestars.machines.eye.field.FieldState fs = rt.fieldState();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("rho", fs == null || fs.beam == null ? 0 : fs.beam.rho);
        m.put("gev", com.nateyburr.beyondthestars.machines.eye.field.AttractorField.gev(at.grade()));
        m.put("rate", at.rate());
        m.put("cap", com.nateyburr.beyondthestars.machines.eye.field.AttractorField.cap(at.barrelMk()));
        m.put("grade", com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor.gradeName(at.grade()));
        m.put("holdRating", at.holdRating());
        m.put("stock", at.stock());
        m.put("level", at.level());
        m.put("rhoMax", at.rhoMax());
        m.put("acceptLimit", at.acceptLimit());
        m.put("firing", at.firing());
        return m;
    }

    /** P5 {@code bts_eye.setBeam(c, k)}: on every Pellet Injector Hatch of the Eye. */
    public static void eyeSetBeam(MTEEyeOfHarmonyBTS eye, double c, double k) {
        for (com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector i : eye.injectorHatches()) {
            if (i != null) i.setBeam(c < 0 ? Double.NaN : c, k);
        }
    }

    /** P5 {@code bts_eye.setGrade(name)}: "magnetar", "ergomatter", "superdense" or "auto"; returns it. */
    public static String eyeSetGrade(MTEEyeOfHarmonyBTS eye, String name) {
        final String n = name == null ? "" : name.trim()
            .toLowerCase(java.util.Locale.ROOT);
        final int g = n.startsWith("mag") ? 1 : n.startsWith("erg") ? 2 : n.startsWith("sup") ? 3 : 0;
        for (com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector i : eye.injectorHatches()) {
            if (i != null) i.setLockedGrade(g);
        }
        return com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor.gradeName(g)
            .replace("none", "auto");
    }

    /** P5 {@code bts_eye.getFlow()}. */
    public static Map<String, Object> eyeFlow(MTEEyeOfHarmonyBTS eye) {
        final com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime rt = eye.starRuntime();
        final com.nateyburr.beyondthestars.machines.eye.field.FieldState fs = rt.fieldState();
        final Map<String, Object> m = new LinkedHashMap<>();
        final com.nateyburr.beyondthestars.machines.eye.field.FieldState.Flow f = fs == null || !fs.tier.attractor
            ? null
            : fs.flow;
        m.put("secondsLeft", f == null ? -1 : f.left > 0 ? -f.tau : f.warned ? Math.ceil(f.next - fs.t) : -1);
        m.put("side", f == null || !(f.warned || f.left > 0) ? 0 : f.sign);
        m.put("size", f == null || !(f.warned || f.left > 0) ? 0 : f.size);
        m.put("accepted", f != null && (f.warned || f.left > 0) && f.accepted);
        m.put("surging", f != null && f.left > 0);
        m.put("held", f == null ? 0 : f.ok);
        m.put("galaxies", rt.milestones().galaxies);
        m.put("flowsHeld", rt.milestones().flowsHeld);
        m.put(
            "last",
            rt.attractor()
                .lastFlow());
        return m;
    }

    /** P5 {@code bts_eye.getUniverse()}. */
    public static Map<String, Object> eyeUniverse(MTEEyeOfHarmonyBTS eye) {
        final com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime rt = eye.starRuntime();
        final com.nateyburr.beyondthestars.machines.eye.field.FieldState fs = rt.fieldState();
        final Map<String, Object> m = new LinkedHashMap<>();
        final com.nateyburr.beyondthestars.machines.eye.field.FieldState.Universe u = fs == null || !fs.tier.universe
            ? null
            : fs.uni;
        m.put("age", u == null ? 0 : u.t);
        m.put("H", fs == null ? 0 : fs.x[com.nateyburr.beyondthestars.machines.eye.field.StellarField.H]);
        m.put("setpointPhase", u == null ? 0 : com.nateyburr.beyondthestars.machines.eye.field.UniverseField.phase(u));
        final boolean recNext = u != null && u.t < com.nateyburr.beyondthestars.machines.eye.field.UniverseField.REC_AT;
        final boolean dawnNext = u != null && !recNext && u.t < u.dawn;
        m.put("nextEvent", recNext ? "recombination" : dawnNext ? "dawn" : "none");
        m.put(
            "secondsToEvent",
            recNext ? com.nateyburr.beyondthestars.machines.eye.field.UniverseField.REC_AT - u.t
                : dawnNext ? u.dawn - u.t : -1);
        m.put("dawnOk", u != null && u.dawnOk);
        m.put(
            "chargeOwed",
            rt.attractor()
                .chargeOwed());
        m.put("chargeShort", rt.universeChargeShort());
        m.put("universes", rt.milestones().universes);
        m.put("dawns", rt.milestones().dawns);
        return m;
    }

    /** P4 {@code bts_eye.beginTransition()}: begins a ready galaxy transition; returns what happened. */
    public static String eyeBegin(MTEEyeOfHarmonyBTS eye) {
        return eye.starRuntime()
            .requestBegin();
    }

    /** P4 {@code bts_eye.setFeed8(v)}: the T8 Ergomatter feed (0-10 L/s per parallel); returns it. */
    public static double eyeSetFeed8(MTEEyeOfHarmonyBTS eye, double v) {
        eye.starRuntime()
            .setF8(v);
        return eye.starRuntime()
            .f8Setpoint();
    }

    /** P4 {@code bts_eye.setMode("galaxy" | "planets")}: returns the mode now (the GUI's switch). */
    public static String eyeSetMode(MTEEyeOfHarmonyBTS eye, String name) {
        final EyeGalaxy.Mode mode = EyeGalaxy.Mode.parse(name);
        if (mode != null) eye.starRuntime()
            .setMode(mode);
        return eye.starRuntime()
            .mode().key;
    }

    /** P4 {@code bts_eye.getSlots()}: the mode, the planet slots (one line each), the companions, idle / short. */
    public static Map<String, Object> eyeSlots(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", rt.mode().key);
        final Map<Integer, String> slots = new LinkedHashMap<>();
        final List<String> lines = rt.slotLines();
        for (int i = 0; i < lines.size(); i++) slots.put(i + 1, lines.get(i));
        m.put("slots", slots);
        m.put("progress", rt.slotProgress());
        m.put("companions", rt.companionNames());
        m.put("idle", rt.slotIdle());
        m.put("companionShort", rt.companionShort());
        m.put(
            "lastRunLostParallels",
            rt.galaxy()
                .lastRunLost());
        return m;
    }

    /** {@code bts_eye.getBuffers()}: fluid registry name → litres held. */
    public static Map<String, Object> eyeBuffers(MTEEyeOfHarmonyBTS eye) {
        return new LinkedHashMap<>(eye.bufferSnapshot());
    }

    /**
     * {@code bts_eye.getIntakeCaps()} (B-108): fluid registry name → the litres the sockets fill that buffer to now
     * (a fluid not listed is not pulled).
     */
    public static Map<String, Object> eyeIntakeCaps(MTEEyeOfHarmonyBTS eye) {
        final Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<Fluid, Long> e : eye.intakeCaps()
            .entrySet()) {
            if (e.getKey() != null) m.put(
                e.getKey()
                    .getName(),
                Math.min(MTEEyeOfHarmonyBTS.BUFFER_CAP, e.getValue()));
        }
        return m;
    }

    /** {@code bts_eye.getTransition()}: unlocking: milestones, the wanted core, escrow, ready, what is missing. */
    public static Map<String, Object> eyeTransition(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final int n = rt.unlockedTier();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("unlockedTier", n);
        m.put(
            "milestonesMet",
            rt.milestones()
                .met(n));
        m.put(
            "milestones",
            rt.milestones()
                .progressLine(n));
        final net.minecraft.item.Item core = EyeStarRuntime.coreFor(n);
        m.put("core", core == null ? "" : new ItemStack(core).getDisplayName());
        m.put("coreWanted", rt.coreWanted());
        m.put(
            "escrow",
            rt.escrow() == null ? ""
                : rt.escrow()
                    .getDisplayName());
        m.put("ready", rt.transitionReady());
        final StarDef star = rt.star();
        final List<String> missing = rt.missing(star != null && rt.live() ? star.n() : n);
        final Map<Integer, String> list = new LinkedHashMap<>();
        for (int i = 0; i < missing.size(); i++) list.put(i + 1, missing.get(i));
        m.put("missing", list);
        return m;
    }

    /** {@code bts_eye.setDial(v)}: the GUI's clamping setter (1-3, 0.25 steps). Returns the new setpoint. */
    public static double eyeSetDial(MTEEyeOfHarmonyBTS eye, double v) {
        eye.starRuntime()
            .setDial(v);
        return eye.starRuntime()
            .dialSetpoint();
    }

    /** {@code bts_eye.setFeed(v)}: the GUI's clamping setter (0-1.5 L/s per parallel). Returns the new setpoint. */
    public static double eyeSetFeed(MTEEyeOfHarmonyBTS eye, double v) {
        eye.starRuntime()
            .setFeed(v);
        return eye.starRuntime()
            .feedSetpoint();
    }

    // ============================================================================ directives

    /**
     * {@code getDirective(set [, w])} on the Eye or the KEE: the code of one of the host's output sets, with its bits
     * by name. An unknown set, another host's set or a control set gives {@code {error=...}}.
     */
    public static Map<String, Object> directive(DirectiveHost host, String setName, int window) {
        final Map<String, Object> m = new LinkedHashMap<>();
        final DirectiveSet set = DirectiveSet.parse(setName);
        if (set == null || set.host != host.directiveHost() || set.control) {
            m.put("error", "sets here: " + setNames(host.directiveHost()));
            return m;
        }
        final int w = Math.max(MTEHatchDirective.WINDOW_MIN, Math.min(MTEHatchDirective.WINDOW_MAX, window));
        final int code = host.directiveCode(set, w) & 15;
        m.put("set", set.name());
        m.put("code", code);
        final Map<String, Object> bits = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) bits.put(host.directiveBitLabel(set, i), (code & (1 << i)) != 0);
        m.put("bits", bits);
        return m;
    }

    /** The output-set names of a host, for error messages and {@code getSets()}. */
    public static String setNames(DirectiveSet.Host host) {
        final StringBuilder sb = new StringBuilder();
        for (DirectiveSet s : DirectiveSet.VALUES) {
            if (s.host != host || s.control) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.name());
        }
        return sb.toString();
    }

    // ==================================================================================== Kerr

    /** {@code bts_kerr.getState()}: the hole, the governor, bolts, power, modules. */
    public static Map<String, Object> kerrState(MTEKerrEngine kee) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("formed", kee.isFormed());
        m.put("tier", kee.isDensityInstalled() ? 2 : 1);
        m.put("open", kee.isHoleOpen());
        m.put("mass", kee.getMass());
        m.put("setpoint", kee.getSetpoint());
        m.put("setpointMax", kee.maxSetpoint());
        m.put("bleed", kee.getBleed());
        m.put("boltsPerTick", kee.getBoltsPerTick());
        m.put("boltCap", kee.boltCap());
        m.put("railTier", kee.railTier());
        m.put("stability", kee.getStability());
        m.put("powerOk", kee.isPowerOk());
        m.put("containmentEUt", kee.getContainmentEut());
        m.put("boltStockSM", kee.getBoltStockSm());
        m.put("boltStockSeconds", kee.getBoltStockSeconds());
        m.put("evaporateSeconds", kee.getEvapSeconds());
        m.put("seeds", kee.getSeeds());
        m.put("collapsers", kee.getCollapsers());
        m.put("linkedModules", kee.getLinkedModules());
        m.put("auto", kee.isAutoSeeds());
        // P4: the quasar link (a GRB Receptor Port in a utility socket)
        m.put("setpointHeld", kee.effectiveSetpoint());
        m.put("bleedFactor", kee.bleedFactor());
        m.put("link", burstLink(kee.burstHost(), kee.burstSigma(), kee.receptorCount()));
        return m;
    }

    /**
     * {@code bts_kerr.setSetpoint(n)}: the GUI's setter (whole SM, clamped to 2..64 or 2..512). Returns the new one.
     */
    public static int kerrSetSetpoint(MTEKerrEngine kee, int sm) {
        kee.setSetpoint(sm);
        return kee.getSetpoint();
    }

    /**
     * {@code bts_kerr.open()}: the GUI's Open button (a formed KEE, a closed hole, power, bolts, a Seed in a bolt bus).
     */
    public static Object[] kerrOpen(MTEKerrEngine kee) {
        if (kee.openHoleFromGui()) return new Object[] { true, "opened at " + MTEKerrEngine.OPEN_MASS + " SM" };
        return new Object[] { false,
            !kee.isFormed() ? "not formed"
                : kee.isHoleOpen() ? "already open"
                    : !kee.openRefusal()
                        .isEmpty() ? kee.openRefusal() : "no Black Hole Seed in a bolt bus" };
    }

    /** {@code bts_kerr.collapse()}: the GUI's Collapse button (an open hole, a Collapser in a bolt bus). */
    public static Object[] kerrCollapse(MTEKerrEngine kee) {
        if (kee.collapseHoleFromGui()) return new Object[] { true, "collapsed" };
        return new Object[] { false, !kee.isFormed() ? "not formed"
            : !kee.isHoleOpen() ? "no hole open" : "no Black Hole Collapser in a bolt bus" };
    }

    /** {@code bts_kerr.setAuto(b)}: the GUI's Auto switch (a Seed / Collapser in a bolt bus acts on its own). */
    public static boolean kerrSetAuto(MTEKerrEngine kee, boolean on) {
        kee.setAutoSeeds(on);
        return kee.isAutoSeeds();
    }

    /** {@code bts_kerr_module.getState()}: the link, the hole's bonus, the run. */
    public static Map<String, Object> kerrModuleState(MTEKerrModuleBase<?> module) {
        final Map<String, Object> m = new LinkedHashMap<>();
        final MTEKerrModuleBase.Link link = module.getLink();
        m.put("formed", module.mMachine);
        m.put("link", link.key);
        m.put("linkText", link.text);
        m.put("mass", module.linkedMass());
        m.put("parallel", module.bonusParallel());
        m.put("speed", module.bonusSpeed());
        final IGregTechTileEntity base = module.getBaseMetaTileEntity();
        m.put("active", base != null && base.isActive());
        m.put("progress", module.mProgresstime);
        m.put("maxProgress", module.mMaxProgresstime);
        m.put("enabled", base != null && base.isAllowedToWork());
        return m;
    }

    // ================================================================================= hatches

    /** {@code bts_sensor.getState()}: the Stellar Field Sensor Hatch's channel, comparator, reading and output. */
    public static Map<String, Object> sensorState(MTEHatchStellarFieldSensor s) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("host", s.getHost().label);
        m.put("channel", s.currentChannelName());
        m.put("window", s.getWindow());
        m.put("invert", s.isInvert());
        m.put("threshold", s.getThreshold());
        m.put("comparator", s.isLessThan() ? "lt" : "gt");
        m.put("analog", s.isAnalog());
        final double v = s.getLastValue();
        m.put("value", Double.isNaN(v) ? null : v);
        m.put("output", s.currentOutput());
        return m;
    }

    /** {@code bts_sensor.setChannel(name)}: P, THETA, OMEGA, PI, SHIELD, INTEGRITY, ... Returns the channel set. */
    public static String sensorSetChannel(MTEHatchStellarFieldSensor s, String name) {
        if (s.getHost() == MTEHatchStellarFieldSensor.Host.QUASAR) {
            final com.nateyburr.beyondthestars.machines.quasar.QuasarSensorChannel q = com.nateyburr.beyondthestars.machines.quasar.QuasarSensorChannel
                .parse(name);
            if (q != null) s.setChannelOrdinal(q.ordinal());
            return s.currentChannelName();
        }
        for (FieldReadout.Channel c : FieldReadout.Channel.values()) {
            if (c.name()
                .equalsIgnoreCase(name)) {
                s.setChannelOrdinal(c.ordinal());
                break;
            }
        }
        return s.currentChannelName();
    }

    /** {@code bts_directive.getState()}: the hatch's set, output mode, window, code, bits and output. */
    public static Map<String, Object> directiveHatchState(MTEHatchDirective h) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put(
            "set",
            h.getSet()
                .name());
        m.put("host", h.getHost().label);
        m.put(
            "output",
            h.getOutput()
                .name());
        m.put("invert", h.isInvert());
        m.put("window", h.getWindow());
        m.put("code", h.currentCode());
        m.put("redstone", h.currentOutput());
        m.put("controlIn", h.getControlIn());
        final Map<String, Object> bits = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) bits.put(h.label(i), (h.currentCode() & (1 << i)) != 0);
        m.put("bits", bits);
        return m;
    }

    /**
     * {@code bts_directive.getBits()}: the current code as four booleans, [1] [2] [4] [8] (Lua indices 1-4), with
     * their labels.
     */
    public static Map<String, Object> directiveBits(MTEHatchDirective h) {
        final Map<String, Object> m = new LinkedHashMap<>();
        final int code = h.currentCode();
        final Map<Integer, Object> on = new LinkedHashMap<>();
        final Map<Integer, Object> names = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) {
            on.put(i + 1, (code & (1 << i)) != 0);
            names.put(i + 1, h.label(i));
        }
        m.put("code", code);
        m.put("on", on);
        m.put("names", names);
        return m;
    }

    /** {@code bts_directive.setSet(name)}: any set of the hatch's host (EYE_TRIMS, KERR_HOLE, ...). */
    public static String directiveSetSet(MTEHatchDirective h, String name) {
        final DirectiveSet s = DirectiveSet.parse(name);
        final DirectiveSet.Host host = h.getHost();
        if (s != null && (host == DirectiveSet.Host.NONE || s.host == host)) h.setSet(s);
        return h.getSet()
            .name();
    }

    /** {@code bts_directive.setOutput(name)}: CODE, BIT1..BIT4, ANY. */
    public static String directiveSetOutput(MTEHatchDirective h, String name) {
        for (DirectiveSet.Output o : DirectiveSet.Output.VALUES) {
            if (o.name()
                .equalsIgnoreCase(name)) h.setOutput(o);
        }
        return h.getOutput()
            .name();
    }
}
