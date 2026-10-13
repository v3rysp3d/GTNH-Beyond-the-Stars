package com.nateyburr.beyondthestars.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
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
}
