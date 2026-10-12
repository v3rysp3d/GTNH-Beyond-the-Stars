package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.OM;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.PI;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * D-49 stability field: the prototype's sensor-hatch rigs ({@code TIERS[n].ctrl}), for the golden test, the offline
 * runner and the debug {@code /btseye star … rig} command. Never used in normal play: there the trims are whatever
 * the player's rig puts into the Field sockets.
 * <p>
 * A rig reads last second's errors and warning (1 s sensor lag) and returns trim units per second in insertion
 * order (the order the prototype sums them in). {@link Mode#BANG} is threshold ±10 at full rate; {@link Mode#FINE}
 * (two-stage) is ±5 at a reduced rate.
 */
public final class FieldRig {

    public enum Mode {

        /** No regulator: no trims. */
        OFF,
        /** Bang-bang: ±10, full rate. */
        BANG,
        /** Two-stage: ±5, reduced rate. */
        FINE;

        /** "off" / "bang" / "fine" (also the prototype's names), or null. */
        public static Mode parse(String s) {
            for (Mode m : values()) if (m.name()
                .equalsIgnoreCase(s)) return m;
            return null;
        }
    }

    private FieldRig() {}

    /** The rig's raw request, keyed by the prototype's trim keys, in insertion order (prototype {@code ctrl}). */
    public static LinkedHashMap<String, Double> ctrl(TierParams tier, double[] E, int w, Mode mode) {
        final LinkedHashMap<String, Double> u = new LinkedHashMap<>();
        if (mode == Mode.OFF) return u;
        final boolean f = mode == Mode.FINE;
        final double t = f ? 5 : 10;
        switch (tier.n) {
            case 1: {
                final double r = f ? 2 : 10;
                if (E[P] > t) u.put("He", r);
                if (E[P] < -t) u.put("H", r);
                break;
            }
            case 2: {
                final double r = f ? 4 : 20;
                if (E[P] > t) u.put("He", r);
                if (E[P] < -t) u.put("H", r);
                break;
            }
            case 3: {
                final double r = f ? 4 : 10;
                if (E[P] > t) {
                    add(u, "He", r);
                    add(u, "Cool", r);
                }
                if (E[P] < -t) u.put("H", r);
                if (E[TH] > t || (w != 0 && E[TH] > -30)) add(u, "Cool", w != 0 ? 20 : r);
                if (E[TH] < -t) add(u, "He", r);
                break;
            }
            case 4:
                ctl4(u, E, f);
                break;
            case 5: {
                final double r = f ? 0.5 : 1;
                if (E[TH] > t) u.put("DSSF", 10 * r);
                if (E[TH] < -t) u.put("RSPM", 5 * r);
                if (E[P] < -t) u.put("He", 10 * r);
                if (E[P] > t) add(u, "DSSF", 5 * r);
                break;
            }
            default: {
                ctl4(u, E, f);
                if (w != 0 || Math.abs(E[PI]) > t) u.put("Agent", 5.0);
                break;
            }
        }
        return u;
    }

    private static void ctl4(LinkedHashMap<String, Double> u, double[] E, boolean f) {
        final double t = f ? 5 : 10, r = f ? 0.5 : 1;
        if (E[P] > t) u.put("Space", 10 * r);
        if (E[P] < -t) u.put("ST", 10 * r);
        if (E[TH] > t) u.put("Cool", 10 * r);
        if (E[OM] > t) u.put("Tg", 5 * r);
    }

    /** JS {@code u[k]=(u[k]||0)+v}: an existing key keeps its position. */
    private static void add(LinkedHashMap<String, Double> u, String k, double v) {
        final Double old = u.get(k);
        u.put(k, (old == null ? 0 : old) + v);
    }

    /**
     * The prototype's filter from a request to trim units: unknown keys dropped, each clamped to [0, cap], zeros
     * dropped; insertion order kept. Fills {@code into} (cleared first).
     */
    public static void toUnits(TierParams tier, Map<String, Double> raw, LinkedHashMap<TrimDef, Double> into) {
        into.clear();
        for (Map.Entry<String, Double> e : raw.entrySet()) {
            final TrimDef.Kind kind = TrimDef.Kind.byKey(e.getKey());
            final TrimDef def = kind == null ? null : tier.trim(kind);
            if (def == null) continue;
            final double v = StellarField.clamp(e.getValue(), 0, def.cap);
            if (v == 0 || Double.isNaN(v)) continue;
            into.put(def, v);
        }
    }
}
