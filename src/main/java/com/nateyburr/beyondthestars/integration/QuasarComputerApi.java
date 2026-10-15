package com.nateyburr.beyondthestars.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarState;

/**
 * P4 (quasar-p4.md §8.2): what OpenComputers' {@code bts_quasar} component returns and sets, through the GUI's own
 * setters (the same clamps). Plain JDK types only, so it does not load OpenComputers classes. Setters are applied at
 * once and the disc reads them on its next second (the field's own 1 s step: a script cannot beat the lag).
 */
public final class QuasarComputerApi {

    private QuasarComputerApi() {}

    /** {@code getDisc()}: E, phi, lambda, a, M, cap, integrity, zone, Yq, stability, state. */
    public static Map<String, Object> disc(MTEQuasarEngine m) {
        final QuasarState q = m.state();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("state", m.holeName());
        out.put("formed", m.isFormed());
        out.put("frozen", m.isFrozen());
        out.put("E", q.E);
        out.put("phi", q.Phi);
        out.put("lambda", q.lam);
        out.put("a", q.a);
        out.put("M", m.getMass());
        out.put("cap", m.getMassCap());
        out.put("integrity", q.I);
        out.put("zone", MTEQuasarEngine.zoneName(q.zone));
        out.put("Yq", q.Yq);
        out.put("stability", q.stability());
        out.put("b", m.getBInUse());
        out.put("coilTier", m.getCoilTier());
        out.put("powerOk", m.isPowerOk());
        out.put("lastEvent", m.getLastEvent());
        return out;
    }

    /** {@code getBurst()}: total, top, bottom, docks. */
    public static Map<String, Object> burst(MTEQuasarEngine m) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", m.state().G);
        out.put("totalEUt", QuasarParams.euPerTick(m.state().G));
        out.put("totalText", BTSFormat.buEu(m.state().G));
        final Map<String, Object> docks = new LinkedHashMap<>();
        for (JetPort port : JetPort.VALUES) {
            out.put(port.key, m.jetBu(port));
            final String d = m.jetDock(port);
            docks.put(port.key, d == null || m.jetRefused(port) ? "venting" : d);
        }
        out.put("docks", docks);
        out.put("vented", m.getVented());
        return out;
    }

    /** {@code getLinks()}: the hosts fed, as the conduit reports them. */
    public static List<Map<String, Object>> links(MTEQuasarEngine m) {
        final List<Map<String, Object>> out = new ArrayList<>();
        for (MTEQuasarEngine.HostReport h : m.hostReports()) {
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("host", h.label);
            row.put("bu", h.bu);
            row.put("sigma", h.sigma);
            row.put("streak", h.streak);
            out.add(row);
        }
        return out;
    }

    /** {@code getMilestones()}. */
    public static Map<String, Object> milestones(MTEQuasarEngine m) {
        final QuasarState q = m.state();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("firstLight", q.firstLight);
        out.put("firstEruption", q.firstErupt);
        out.put("eddingtonHour", q.eddDone);
        for (int i = 0; i < QuasarParams.MASS_MILESTONES.length; i++) {
            out.put("mass" + (int) QuasarParams.MASS_MILESTONES[i], (q.massMilestones & (1 << i)) != 0);
        }
        out.put("eruptions", m.getEruptions());
        out.put("purges", m.getPurges());
        out.put("blowOuts", m.getBlowOuts());
        return out;
    }

    /** {@code setFeed(f)}: returns the feed set (0..1.5). */
    public static double setFeed(MTEQuasarEngine m, double f) {
        m.setFeed(f);
        return m.getFeed();
    }

    /** {@code setGas(name)}: H / He / RSM (or the names); returns the gas in use. */
    public static String setGas(MTEQuasarEngine m, String name) {
        final QuasarGas g = QuasarGas.parse(name);
        if (g != null) m.setGas(g);
        return m.getGas().key;
    }

    /** {@code setField(b)}: the GUI's b; returns it (a FIELD-mode Burst Control Hatch still overrides it). */
    public static double setField(MTEQuasarEngine m, double b) {
        m.setBSetting(b);
        return m.getBSetting();
    }

    /** {@code getDirective(set, w)}: the Disc set's code and bits. */
    public static Map<String, Object> directive(MTEQuasarEngine m, int w) {
        final int code = m.directiveCode(DirectiveSet.QUASAR_DISC, w);
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        for (int i = 0; i < 4; i++) out.put(DirectiveSet.QUASAR_DISC.bit(i), (code & (1 << i)) != 0);
        return out;
    }

    // ------------------------------------------------------------------ SMBH (smbh-v1.md §6): the Supermassive Lattice

    /** {@code getBand()}: the lattice and where the hole sits in its band. */
    public static Map<String, Object> band(MTEQuasarEngine m) {
        final QuasarResult r = m.lastResult();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("lattice", m.getLatticeInForce());
        out.put("latticeBuilt", m.getLatticeBuilt());
        out.put("saturated", m.isSaturated());
        out.put("saturationSeconds", m.getSaturationSeconds());
        out.put("M", m.getMass());
        out.put("safe", r.safe);
        out.put("top", r.top);
        out.put("structureTop", m.getStructureTop());
        out.put("setpoint", m.getMassCap());
        out.put("setpointFromHatch", m.holdFromHatch());
        out.put("x", r.x);
        out.put("unbound", m.isUnbound());
        out.put("n", r.n);
        out.put("shedding", m.state().shed);
        out.put("shedTarget", r.shedTarget);
        out.put("shedReason", r.shedReason);
        out.put("massPerHourPct", 100 * m.getMassRatePerHour());
        out.put("supermassiveLinkSource", m.supermassiveLinkSource());
        return out;
    }

    /** {@code getIntake()}: last second's Disc / Hold-up / Growth (L/s), the 5x cap and the share of it used. */
    public static Map<String, Object> intake(MTEQuasarEngine m) {
        final QuasarResult r = m.lastResult();
        final double total = m.getGasDrawn() + r.holdUpLitres + r.growthLitres;
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("gas", m.getGas().key);
        out.put("disc", m.getGasDrawn());
        out.put("holdUp", r.holdUpLitres);
        out.put("growth", r.growthLitres);
        out.put("total", total);
        out.put("cap", r.intakeCap);
        out.put("capPct", r.intakeCap > 0 ? 100 * total / r.intakeCap : 0);
        out.put("massPerHourPct", 100 * m.getMassRatePerHour());
        return out;
    }

    /** {@code getHoldUp()}: what the band needs, what was paid, the cover and the leak. */
    public static Map<String, Object> holdUp(MTEQuasarEngine m) {
        final QuasarResult r = m.lastResult();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("need", r.holdUpNeed);
        out.put("paid", r.holdUpLitres);
        out.put("coverage", r.holdUpCoverage);
        out.put("leaking", r.leaking);
        out.put("leakPerHourPct", 100 * m.getLeakRatePerHour());
        return out;
    }

    /** {@code getBuffer()}: the Accretion Buffer (litres, cap, seconds of Disc + Hold-up, the feed-short flag). */
    public static Map<String, Object> buffer(MTEQuasarEngine m) {
        final double s = m.getBufferSeconds();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("litres", m.bufferShown());
        out.put("stored", m.buffer(MTEQuasarEngine.gasFluid(m.getGas())));
        out.put("cap", m.bufferCap());
        out.put("seconds", Double.isInfinite(s) ? -1 : s);
        out.put("feedShort", s < QuasarParams.FEED_SHORT_SECONDS);
        return out;
    }

    /** {@code getOverdrive()}: the level, its upkeep and cover, and the output it gives (Bu/t and EU/t). */
    public static Map<String, Object> overdrive(MTEQuasarEngine m) {
        final QuasarResult r = m.lastResult();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("level", m.getOverdrive());
        out.put("inUse", m.getOverdriveInUse());
        out.put("fromHatch", m.overdriveFromHatch());
        out.put("available", m.getLatticeInForce() >= QuasarParams.LATTICE_OVERDRIVE);
        out.put("upkeepEach", r.overdriveNeed);
        out.put("coverage", r.overdriveCoverage);
        out.put("espm", r.mhdcsmLitres);
        out.put("hsm", r.universiumLitres);
        out.put("oEff", r.oEff);
        out.put("outputX", r.outputX);
        out.put("bursts", m.state().G);
        out.put("burstsEUt", QuasarParams.euPerTick(m.state().G));
        out.put("burstsText", BTSFormat.buEu(m.state().G));
        return out;
    }

    /** {@code getAnchor()}: Unbound, n, the Primordial Matter anchor's need, cover and the doublings it pays for. */
    public static Map<String, Object> anchor(MTEQuasarEngine m) {
        final QuasarResult r = m.lastResult();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("allowed", Config.smbhAllowUnbound);
        out.put("unbound", m.isUnbound());
        out.put("n", r.n);
        out.put("need", r.anchorNeed);
        out.put("paid", r.anchorLitres);
        out.put("coverage", r.anchorCoverage);
        out.put("short", r.anchorShort);
        out.put("paysN", r.anchorPaysN);
        return out;
    }

    /** {@code setMassSetpoint(M)}: SM (0 = the safe limit); returns the setpoint in force. */
    public static double setMassSetpoint(MTEQuasarEngine m, double mass) {
        m.setMassSetpoint(mass);
        return m.getMassCap();
    }

    /** {@code setOverdrive(o)}: 0-1 in 5 % steps; returns the level set. */
    public static double setOverdrive(MTEQuasarEngine m, double o) {
        m.setOverdrive(o);
        return m.getOverdrive();
    }

    /** {@code getHorizon()}: the Horizon set's code and bits. */
    public static Map<String, Object> horizon(MTEQuasarEngine m) {
        final int code = m.directiveCode(DirectiveSet.QUASAR_HORIZON, 0);
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        for (int i = 0; i < 4; i++) out.put(DirectiveSet.QUASAR_HORIZON.bit(i), (code & (1 << i)) != 0);
        return out;
    }
}
