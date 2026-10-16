package com.nateyburr.beyondthestars.integration;

import java.util.LinkedHashMap;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.conduit.ConduitNet;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitJunction;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.conduit.MTEWormholeMouth;
import com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstReading;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;

/**
 * P4 (quasar-p4.md §8.2): what OpenComputers' conduit components return and set ({@code bts_mouth}, {@code bts_mirror},
 * {@code bts_junction}, {@code bts_crown}, {@code bts_receptor}), through the GUI's own setters (the same clamps).
 * Plain
 * JDK types only, so it does not load OpenComputers classes. Setters take effect on the element's next second.
 */
public final class ConduitComputerApi {

    private ConduitComputerApi() {}

    /** {@code getLink()}: the mouth's partner (or the mirror's routes), open, flow. */
    public static Map<String, Object> link(MTEConduitElement<?> e) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("formed", e.isFormed());
        out.put("open", e.isOpen());
        out.put("through", e.throughBu());
        out.put("clipped", e.clippedBu());
        out.put("lost", e.lostBu());
        if (e instanceof MTEWormholeMouth) {
            final MTEWormholeMouth m = (MTEWormholeMouth) e;
            final int[] p = m.getPartner();
            out.put("partner", p == null ? "" : p[1] + ", " + p[2] + ", " + p[3] + " (dim " + p[0] + ")");
            final String problem = m.partnerProblem();
            out.put("paired", problem.isEmpty());
            out.put("problem", problem);
        }
        for (int i = 0; i < e.throats().length; i++) {
            final ConduitNet.Route r = e.lastRoute(i);
            if (r != null) out.put("route" + i, r.describe());
        }
        return out;
    }

    /** {@code getThroat()}: Mk, cap. */
    public static Map<String, Object> throat(MTEConduitElement<?> e) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("mk", e.throatMk());
        out.put("cap", e.throatCap());
        return out;
    }

    /** {@code getUpkeep()}: fuel mode, the rate on each fuel, open / reopen, litres used. */
    public static Map<String, Object> upkeep(MTEConduitElement<?> e) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put(
            "mode",
            e.getFuelMode()
                .name()
                .toLowerCase(java.util.Locale.ROOT));
        out.put("line", e.getFuelLine());
        out.put("ergomatterPerSecond", e.upkeepRate(true));
        out.put("spaceTimePerSecond", e.upkeepRate(false));
        out.put("open", e.isOpen());
        out.put("reopenSeconds", e.getReopen());
        out.put("litresSpaceTime", e.getLitresSpaceTime());
        out.put("litresErgomatter", e.getLitresErgomatter());
        return out;
    }

    /** {@code setFuel(mode)}: auto / spacetime / ergomatter; returns the mode in use. */
    public static String setFuel(MTEConduitElement<?> e, String mode) {
        for (MTEConduitElement.FuelMode m : MTEConduitElement.FuelMode.VALUES) {
            if (m.name()
                .equalsIgnoreCase(mode == null ? "" : mode.trim())) e.setFuelMode(m);
        }
        return e.getFuelMode()
            .name()
            .toLowerCase(java.util.Locale.ROOT);
    }

    /** {@code bts_junction.getFlow()}: in, out1, out2, out3, clipped, mode, c, k, p3, limit (P4-88), s, share. */
    public static Map<String, Object> flow(MTEConduitJunction j) {
        final Map<String, Object> out = new LinkedHashMap<>();
        final boolean split = j.getMode() == MTEConduitJunction.Mode.SPLIT;
        out.put("mode", split ? "split" : "combine");
        out.put("in", split ? j.inBu(0) : j.inBu(1) + j.inBu(2) + j.inBu(3));
        out.put("out1", split ? j.outBu(1) : 0);
        out.put("out2", split ? j.outBu(2) : 0);
        out.put("out3", split ? j.outBu(3) : 0);
        out.put("out", split ? 0 : j.outBu(0));
        out.put("clipped", j.clippedBu());
        out.put("lost", j.lostBu());
        out.put("c", j.getRatio());
        out.put("k", j.getGain());
        out.put("p3", j.getOut3Share());
        out.put("limit", j.getLimit());
        out.put("s", j.getLastLevel());
        out.put("share", j.getLastShare());
        out.put("open", j.isOpen());
        return out;
    }

    /** {@code bts_crown.getPower()} etc.: Mk, stream, used, EU/t, clipped, stacked. */
    public static Map<String, Object> crown(MTEBurstThermaliserCrown c) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("mk", c.getMk());
        out.put("stream", c.getStream());
        out.put("used", c.getUsed());
        out.put("eut", c.getEUt());
        out.put("clipped", c.getClipped());
        out.put("stacked", c.stacked() != null);
        out.put(
            "lifetimeEU",
            c.getLifetimeEU()
                .toString());
        return out;
    }

    /**
     * {@code bts_receptor.getDelivery()}: total, bySource, linked, link source, paused, host. The port's stored copy of
     * last second (QA-B61): reading through {@link BurstHost#read} named the port's host on every call, so a script
     * polling a port kept a broken host's name alive.
     */
    public static Map<String, Object> delivery(MTEHatchGRBReceptor r) {
        final BurstReading reading = r.lastReading();
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", reading.total);
        final Map<String, Object> by = new LinkedHashMap<>();
        for (Map.Entry<BurstSource, Double> e : reading.bySource.entrySet()) by.put(
            e.getKey()
                .label(),
            e.getValue());
        out.put("bySource", by);
        out.put("linked", reading.linked());
        final BurstSource s = reading.linkSource();
        out.put("source", s == null ? "" : s.label());
        out.put("paused", reading.paused);
        out.put("host", r.getHost());
        return out;
    }
}
